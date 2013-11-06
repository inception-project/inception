/*******************************************************************************
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universit?t Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.statistics;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.uimafit.util.CasUtil;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.AnnotationStudy;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.IAnnotationStudy;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.TwoRaterKappaAgreement;

/**
 * This class provides methods used to compute the kappa agreement.
 *
 * @author Seid Muhie Yimam
 *
 */
public class TwoPairedKappa
{
    public static String EMPTY = "EMPTY";

    private static RepositoryService repositoryService;
    private Project project;

    private Log LOG = LogFactory.getLog(getClass());

    public TwoPairedKappa(Project aProject, RepositoryService aRepositoryService)
    {
        repositoryService = aRepositoryService;
        project = aProject;
    }

    /**
     * Return the concatenations of start and end offsets of annotations. <br>
     * Example: <br>
     * A span annotation that starts at offset 05 and ends at offset 11 will have a value
     * <b>Doc.ID+0511</b> <br>
     * For Arc Annotation, besides the start and end offsets of an annotation, it will also consider
     * the start and end offsets of origin and target span annotations. Hence orientation of the arc
     * annotation is taken into considerations.
     *
     * @param aUsers
     *            Users with finished annotation documents
     */
    public Set<String> getAllAnnotations(List<User> aUsers, SourceDocument aSourceDocument,
            String aType, Map<User, JCas> JCases)
    {
        // Set of start+end offsets for all annotations in the document
        Set<String> annotationPositions = new HashSet<String>();
        for (User user : aUsers) {
            AnnotationDocument annotationDocument = repositoryService.getAnnotationDocument(
                    aSourceDocument, user);
            JCas jCas = null;
            if (annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)) {

                jCas = JCases.get(user);
                Type type = CasUtil.getType(jCas.getCas(), aType);
                for (AnnotationFS fs : CasUtil.select(jCas.getCas(), type)) {
                    annotationPositions.add(aSourceDocument.getId() + getPosition(type, fs));
                }
            }
        }
        return annotationPositions;
    }

    /**
     * For all the annotation offsets obtained, initialise it with an <b>EMPTY</b> annotation
     *
     * @param aUsers
     *            all users
     * @param aUserAnnotations
     *            the initialised user annotations
     * @param aAnnotationPositions
     *            annotation positions obtained using
     *            {@link #getAllAnnotations(List, SourceDocument, String, String)}
     */
    public Map<String, Map<String, String>> initializeAnnotations(List<User> aUsers,
            Set<String> aAnnotationPositions)
    {
        Map<String, Map<String, String>> userAnnotations = new TreeMap<String, Map<String, String>>();
        for (User user : aUsers) {
            Map<String, String> annottaions = new TreeMap<String, String>();
            for (String annotationPosition : aAnnotationPositions) {
                annottaions.put(annotationPosition, EMPTY);
            }
            userAnnotations.put(user.getUsername(), annottaions);
        }
        return userAnnotations;
    }

    /**
     * update Users annotation that is already initialized using
     * {@link #initializeAnnotations(List, Map, Set)}
     *
     * @param aUsers
     *            all users
     * @param aSourceDocument
     *            the source document
     * @param aType
     *            the UIMA type name
     * @param aLableFeatureName
     *            the feature of the UIMA annotation
     * @param aUserAnnotations
     *            an already initialized user annotations
     * @return an updated user annotations
     * @throws UIMAException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public Map<String, Map<String, String>> updateUserAnnotations(List<User> aUsers,
            SourceDocument aSourceDocument, String aType, String aLableFeatureName,
            Map<String, Map<String, String>> aUserAnnotations, Map<User, JCas> JCases)
    {
        for (User user : aUsers) {

            AnnotationDocument annotationDocument = repositoryService.getAnnotationDocument(
                    aSourceDocument, user);

            JCas jCas = null;
            if (annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)) {

                jCas = JCases.get(user);

                Type type = CasUtil.getType(jCas.getCas(), aType);

                for (AnnotationFS fs : CasUtil.select(jCas.getCas(), type)) {
                    Feature labelFeature = fs.getType().getFeatureByBaseName(aLableFeatureName);
                    // update the already initialised EMPTY annotation with actual annotations,
                    // when exist
                    if (fs.getStringValue(labelFeature) != null) {
                        aUserAnnotations.get(user.getUsername()).put(
                                aSourceDocument.getId() + "" + getPosition(type, fs),
                                getValue(type, fs));
                    }
                }
            }
        }
        // return the updated annotations
        return aUserAnnotations;
    }

    /**
     * for two users, <b> user1, user2</b>, compute kappa based on the annotation study.<br>
     * The annotation study is stored per annotation offsets, EMPTY for a user that didn't make any
     * annotation
     */
    public double[][] getAgreement(Map<String, Map<String, String>> aAnnotationStudy)
    {
        int i = 0;
        double[][] results = new double[aAnnotationStudy.size()][aAnnotationStudy.size()];

        for (String user1 : aAnnotationStudy.keySet()) {
            int j = 0;
            for (String user2 : aAnnotationStudy.keySet()) {
                IAnnotationStudy study = new AnnotationStudy(2);

                Map<String, String> user1Annotation = aAnnotationStudy.get(user1);
                Iterator<String> user1Iterator = user1Annotation.keySet().iterator();
                while (user1Iterator.hasNext()) {
                    String annotationOffset = user1Iterator.next();
                    study.addItem(user1Annotation.get(annotationOffset), aAnnotationStudy
                            .get(user2).get(annotationOffset));
                }
                TwoRaterKappaAgreement kappaAgreement = new TwoRaterKappaAgreement(study);
                results[i][j] = kappaAgreement.calculateAgreement();
                j++;
            }
            i++;
        }
        return results;
    }

    /**
     * From the {@link FeatureStructure} provided, get its type value
     */
    private String getValue(Type aType, FeatureStructure fsNew)
    {
        String result = "";
        List<Feature> fsNewFeatures = aType.getFeatures();
        for (Feature feature : fsNewFeatures) {
            if (feature.getRange().getName().equals("uima.cas.String")) {
                result = result + fsNew.getStringValue(feature);
            }
        }
        return result;
    }

    /**
     * From the {@link FeatureStructure} provided, get the concatenations of begin/end offsets
     */
    private String getPosition(Type aType, FeatureStructure fsNew)
    {
        String result = "";
        List<Feature> fsNewFeatures = aType.getFeatures();
        for (Feature feature : fsNewFeatures) {
            if (feature.getRange().getName().equals("uima.cas.Integer")) {
                result = result + fsNew.getIntValue(feature);
            }
        }
        return result;
    }
}

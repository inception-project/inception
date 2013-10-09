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
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.AnnotationStudy;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.IAnnotationStudy;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.TwoRaterKappaAgreement;

/**
 * For a given source document, return user annotations for a given UIMA type. This is a Map of
 * users with a nested map which contains the concatenations of annotation offsets and UIMA features
 * for the annotations.
 *
 * @author Seid Muhie Yimam
 *
 */
public class TwoPairedKappa
{
    public static final String NAMEDENITYTYPE = NamedEntity.class.getName();
    public static final String POSTYPE = POS.class.getName();
    public static final String DEPENDENCYTYPE = Dependency.class.getName();
    public static final String COREFERENCELINKTYPE = CoreferenceLink.class.getName();
    public static final String COREFERENCECHAINTYPE = CoreferenceLink.class.getName();
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
     * return the concatenations of start and end positions of annotations. <br>
     * Example: <br>
     * An annotation that starts at position 05 and ends at position 11 will have a value
     * <b>0511</b>
     *
     * @param aUsers
     *            Users with finished annotation documents
     * @return
     * @throws ClassNotFoundException
     * @throws IOException
     * @throws UIMAException
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
                    annotationPositions.add(aSourceDocument.getId() + "" + fs.getBegin() + ""
                            + fs.getEnd());
                }
            }
        }
        return annotationPositions;
    }

    /**
     * For all the annotation positions obtained, initialise all positions with an <b>EMPTY</b>
     * annotation
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
                                aSourceDocument.getId() + "" + fs.getBegin() + "" + fs.getEnd(),
                                fs.getStringValue(labelFeature));
                    }
                }
            }
        }
        // return the updated annotations
        return aUserAnnotations;
    }

    public double[][] getAgreement(Map<String, Map<String, String>> aUserAnnotations)
    {
        int i = 0;
        double[][] results = new double[aUserAnnotations.size()][aUserAnnotations.size()];

        for (String user1 : aUserAnnotations.keySet()) {
            int j = 0;
            for (String user2 : aUserAnnotations.keySet()) {
                IAnnotationStudy study = new AnnotationStudy(2);

                Map<String, String> user1Annotation = aUserAnnotations.get(user1);
                Iterator<String> user1Iterator = user1Annotation.keySet().iterator();
                while (user1Iterator.hasNext()) {
                    String annotationOffset = user1Iterator.next();
                    study.addItem(user1Annotation.get(annotationOffset), aUserAnnotations
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
}

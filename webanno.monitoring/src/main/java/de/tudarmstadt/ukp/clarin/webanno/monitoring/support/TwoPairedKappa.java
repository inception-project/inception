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
package de.tudarmstadt.ukp.clarin.webanno.monitoring.support;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.fit.util.CasUtil;

import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
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
    public Set<String> getAnnotationPositions(JCas aJCas, Long docId, String aType)
    {
        // Set of start+end offsets for all annotations in the document
        Set<String> annotationPositions = new HashSet<String>();
        Type type = CasUtil.getType(aJCas.getCas(), aType);
        for (AnnotationFS fs : CasUtil.select(aJCas.getCas(), type)) {
            String parenPosition = "";
            Set<FeatureStructure> featureStructures = CasDiff.traverseFS(fs);
            for (FeatureStructure subFS : featureStructures) {
                Type subType = subFS.getType();
                if (subType.getName().equals(Token.class.getName())) {
                    continue;
                }
                if (subType.getName().equals(type.getName())) {
                    parenPosition = parenPosition + getPosition(type, subFS);
                    continue;
                }
                parenPosition = parenPosition + getPosition(subType, subFS);
                annotationPositions.add(docId + getPosition(subType, subFS));
            }
            annotationPositions.add(docId + parenPosition);
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
     *            {@link #getAnnotationPositions(List, SourceDocument, String, String)}
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
    public Map<String, Map<String, String>> updateUserAnnotations(User aUser,
            Map<String, Map<String, String>> aUserAnnotations, Long docId, String aType,
            String aLableFeatureName, JCas aJcas)
    {
        Type type = CasUtil.getType(aJcas.getCas(), aType);

        for (AnnotationFS fs : CasUtil.select(aJcas.getCas(), type)) {
            Feature labelFeature = fs.getType().getFeatureByBaseName(aLableFeatureName);
            // update the already initialised EMPTY annotation with actual annotations,
            // when exist
            if (fs.getStringValue(labelFeature) != null) {

                String parenPosition = "";
                String parentValue = "";
                Set<FeatureStructure> featureStructures = CasDiff.traverseFS(fs);
                for (FeatureStructure subFS : featureStructures) {
                    Type subType = subFS.getType();
                    if (subType.getName().equals(Token.class.getName())) {
                        continue;
                    }
                    if (subType.getName().equals(type.getName())) {
                        parenPosition = parenPosition + getPosition(type, subFS);
                        parentValue = parentValue + getValue(type, subFS);
                        continue;
                    }
                    parenPosition = parenPosition + getPosition(subType, subFS);
                    parentValue = parentValue + getValue(subType, subFS);
                    aUserAnnotations.get(aUser.getUsername()).put(
                            docId + "" + getPosition(subType, subFS), getValue(type, fs));
                }
                aUserAnnotations.get(aUser.getUsername()).put(docId + "" + parenPosition,
                        parentValue);
            }
        }
        // return the updated annotations
        return aUserAnnotations;
    }

    /**
     * set value for {@link IAnnotationStudy} when {@link TwoRaterKappaAgreement} is used for kappa
     * measures
     */
    public void getStudy(String aType, String featureName, User user1, User user2,
            Map<String, Map<String, String>> allUserAnnotations, SourceDocument aDocument,
            Map<User, JCas> JCases)
    {
        Set<String> annotationPositions = new HashSet<String>();

        for (User user : Arrays.asList(new User[] { user1, user2 })) {

            JCas jCas = JCases.get(user);
            annotationPositions.addAll(getAnnotationPositions(jCas, aDocument.getId(), aType));

        }

        if (annotationPositions.size() != 0) {
            Map<String, Map<String, String>> userAnnotations = initializeAnnotations(
                    Arrays.asList(new User[] { user1, user2 }), annotationPositions);
            for (User user : Arrays.asList(new User[] { user1, user2 })) {
                updateUserAnnotations(user, userAnnotations, aDocument.getId(), aType, featureName,
                        JCases.get(user));
            }

            // merge annotations from different object for this
            // user
            for (String username : userAnnotations.keySet()) {
                if (allUserAnnotations.get(username) != null) {
                    allUserAnnotations.get(username).putAll(userAnnotations.get(username));
                }
                else {
                    allUserAnnotations.put(username, userAnnotations.get(username));
                }
            }
            for (User user : Arrays.asList(new User[] { user1, user2 })) {
                allUserAnnotations.get(user.getUsername()).putAll(
                        userAnnotations.get(user.getUsername()));
            }

        }
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

/*******************************************************************************
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
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
package de.tudarmstadt.ukp.clarin.webanno.brat.curation;

import static org.apache.uima.fit.util.CasUtil.selectCovered;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.fit.util.CasUtil;


/**
 * Class for finding clusters of equal annotations. Equal annotations are grouped into
 * a {@link AnnotationSelection}. Instances of {@link AnnotationSelection}, which are
 * exchangeble, because they refer to the same annotated unit, are grouped together in
 * an {@link AnnotationOption}.
 *
 * @author Andreas Straninger
 */
public class CasDiff {

    /**
     * spot differing annotations by comparing cases of the same source document.
     * 
     * @param aEntryTypes the entry types.
     * @param aCasMap
     *            Map of (username, cas)
     * @param aBegin the begin offset.
     * @param aEnd the end offset.
     * @return List of {@link AnnotationOption}
     * @throws RangeNameNotCheckedException hum?
     */
    public static List<AnnotationOption> doDiff(List<Type> aEntryTypes,
            Map<String, JCas> aCasMap, int aBegin, int aEnd) throws RangeNameNotCheckedException {
        Map<Integer, Map<Integer, Set<AnnotationFS>>> annotationFSsByBeginEnd = new HashMap<Integer, Map<Integer, Set<AnnotationFS>>>();
        List<AnnotationOption> annotationOptions = new LinkedList<AnnotationOption>();
        Map<FeatureStructure, String> usernameByFeatureStructure = new HashMap<FeatureStructure, String>();

        Set<String> usernames = new HashSet<String>();

        for (Type aEntryType : aEntryTypes) {
            for (String username : aCasMap.keySet()) {
                usernames.add(username);
                CAS cas = aCasMap.get(username).getCas();

                // instead cas.getAnnotationIndex(aEntryType) also
                // cas.getIndexRepository().getAllIndexedFS(aType)
                // #610 - fetch type by name as type instance may be bound to a different CAS
                Type localType = CasUtil.getType(cas, aEntryType.getName());
                List<AnnotationFS> annotationFSs;
                if(aBegin ==-1) {
                    annotationFSs =  select(cas,localType);
                }
                else{
                    annotationFSs = selectCovered(cas, localType, aBegin, aEnd);
                }
                for (AnnotationFS annotationFS : annotationFSs) {
                    Integer begin = annotationFS.getBegin();
                    Integer end = annotationFS.getEnd();

                    if (!annotationFSsByBeginEnd.containsKey(begin)) {
                        annotationFSsByBeginEnd.put(begin,
                                new HashMap<Integer, Set<AnnotationFS>>());
                    }
                    if (!annotationFSsByBeginEnd.get(begin).containsKey(end)) {
                        annotationFSsByBeginEnd.get(begin).put(end,
                                new HashSet<AnnotationFS>());
                    }
                    annotationFSsByBeginEnd.get(begin).get(end).add(annotationFS);
                    usernameByFeatureStructure.put(annotationFS, username);
                }
            }
            for (Map<Integer, Set<AnnotationFS>> annotationFSsByEnd : annotationFSsByBeginEnd
                    .values()) {
                Map<FeatureStructure, AnnotationSelection> annotationSelectionByFeatureStructure = new HashMap<FeatureStructure, AnnotationSelection>();

                for (Set<AnnotationFS> annotationFSs : annotationFSsByEnd.values()) {
                    Map<String, AnnotationOption> annotationOptionPerType = new HashMap<String, AnnotationOption>();
                    for (FeatureStructure fsNew : annotationFSs) {
                        String usernameFSNew = usernameByFeatureStructure
                                .get(fsNew);
                        // diffFS1 contains all feature structures of fs1, which do not occur in other cases
                        Set<FeatureStructure> diffFSNew = traverseFS(fsNew);

                        Map<FeatureStructure, AnnotationSelection> annotationSelectionByFeatureStructureNew = new HashMap<FeatureStructure, AnnotationSelection>(annotationSelectionByFeatureStructure);

                        for (FeatureStructure fsOld : annotationSelectionByFeatureStructure.keySet()) {
                            if (fsNew != fsOld && fsNew.getType().toString().equals(fsOld.getType().toString())) {
                                CompareResult compareResult = compareFeatureFS(fsNew.getType(),
                                        fsNew, fsOld, diffFSNew);
                                for (FeatureStructure compareResultFSNew : compareResult.getAgreements().keySet()) {
                                    FeatureStructure compareResultFSOld = compareResult.getAgreements().get(compareResultFSNew);
                                    int addressNew = aCasMap.get(usernameFSNew).getLowLevelCas().ll_getFSRef(compareResultFSNew);
                                    AnnotationSelection annotationSelection = annotationSelectionByFeatureStructure.get(compareResultFSOld);
                                    annotationSelection.getAddressByUsername().put(usernameFSNew, addressNew);
                                    annotationSelectionByFeatureStructureNew.put(compareResultFSNew, annotationSelection);
                                    // Add Debug information
                                    annotationSelection.getFsStringByUsername().put(usernameFSNew, compareResultFSNew);

                                }
                            }
                        }
                        annotationSelectionByFeatureStructure = annotationSelectionByFeatureStructureNew;

                        // add featureStructures, that have not been found in existing annotationSelections
                        for (FeatureStructure subFS1 : diffFSNew) {
                            if(subFS1.getType().toString().equals(fsNew.getType().toString())){
                            AnnotationSelection annotationSelection = new AnnotationSelection();
                            int addressSubFS1 = aCasMap.get(usernameFSNew).getLowLevelCas().ll_getFSRef(subFS1);
                            annotationSelection.getAddressByUsername().put(usernameFSNew, addressSubFS1);
                            annotationSelectionByFeatureStructure.put(subFS1, annotationSelection);
                            String type = subFS1.getType().toString();
                            if(!annotationOptionPerType.containsKey(type)) {
                                annotationOptionPerType.put(type, new AnnotationOption());
                            }
                            AnnotationOption annotationOption = annotationOptionPerType.get(type);
                            // link annotationOption and annotationSelection
                            annotationSelection.setAnnotationOption(annotationOption);
                            annotationOption.getAnnotationSelections().add(annotationSelection);
                            // Add Debug information
                            annotationSelection.getFsStringByUsername().put(usernameFSNew, subFS1);
                        }
                        }
                    }
                    annotationOptions.addAll(annotationOptionPerType.values());
                }
            }
        }

        return annotationOptions;
    }

    private static List<AnnotationFS> select(CAS cas, Type localType)
    {
        List<AnnotationFS> annotationFSs = new ArrayList<AnnotationFS>();
        for(AnnotationFS annotationFS: CasUtil.select(cas, localType)){
            annotationFSs.add(annotationFS);
        }
        return annotationFSs;
    }

    public static Set<FeatureStructure> traverseFS(FeatureStructure fs) {
        LinkedHashSet<FeatureStructure> nodePlusChildren = new LinkedHashSet<FeatureStructure>();
        nodePlusChildren.add(fs);
        for (Feature feature : fs.getType().getFeatures()) {
            // features are present in both feature structures, fs1 and fs2
            // compare primitive values
            if (!feature.getRange().isPrimitive() && !feature.toString().equals("uima.cas.AnnotationBase:sofa")) {
                // compare composite types
                // assumtion: if feature is not primitive, it is a composite feature
                FeatureStructure featureValue = fs.getFeatureValue(feature);
                if (featureValue != null) {
                    nodePlusChildren.addAll(traverseFS(featureValue));
                }
            }
        }
        return nodePlusChildren;
    }

    private static CompareResult compareFeatureFS(Type aType,
            FeatureStructure fsNew, FeatureStructure fsOld, Set<FeatureStructure> diffFSNew)
                    throws RangeNameNotCheckedException {
        CompareResult compareResult = new CompareResult();

        // check if types are equal
        Type type = fsNew.getType();
        Type oldType = CasUtil.getType(fsOld.getCAS(), aType.getName());
        if (!(fsOld.getType().toString().equals(type.toString()))) {
            // if types differ add feature structure to diff
            compareResult.getDiffs().put(fsNew, fsOld);
            return compareResult;
        }

        boolean agreeOnSubfeatures = true;
        List<Feature> fsNewFeatures = type.getFeatures();
        List<Feature> fsOldFeatures = oldType.getFeatures();
       for(int i =0; i<fsNewFeatures.size();i++){
           Feature feature = fsNewFeatures.get(i);
           Feature olFeature = fsOldFeatures.get(i);
            // features are present in both feature structures, fs1 and fs2
            // compare primitive values
            if (feature.getRange().isPrimitive()) {

                // check int Values
                if (feature.getRange().getName().equals(CAS.TYPE_NAME_INTEGER)) {
                    if (!(fsNew.getIntValue(feature) == fsOld.getIntValue(olFeature))) {
                        // disagree
                        agreeOnSubfeatures = false;
                    }
                    else {
                        // agree
                    }
                }
                else if (feature.getRange().getName().equals(CAS.TYPE_NAME_LONG)) {
                    if (!(fsNew.getLongValue(feature) == fsOld.getLongValue(olFeature))) {
                        // disagree
                        agreeOnSubfeatures = false;
                    }
                    else {
                        // agree
                    }
                }
                else if (feature.getRange().getName().equals(CAS.TYPE_NAME_BYTE)) {
                    if (!(fsNew.getByteValue(feature) == fsOld.getByteValue(olFeature))) {
                        // disagree
                        agreeOnSubfeatures = false;
                    }
                    else {
                        // agree
                    }
                }
                else if (feature.getRange().getName().equals(CAS.TYPE_NAME_FLOAT)) {
                    if (!(fsNew.getFloatValue(feature) == fsOld.getFloatValue(olFeature))) {
                        // disagree
                        agreeOnSubfeatures = false;
                    }
                    else {
                        // agree
                    }
                }
                else if (feature.getRange().getName().equals(CAS.TYPE_NAME_DOUBLE)) {
                    if (!(fsNew.getDoubleValue(feature) == fsOld.getDoubleValue(olFeature))) {
                        // disagree
                        agreeOnSubfeatures = false;
                    }
                    else {
                        // agree
                    }
                }
                else if (feature.getRange().getName().equals(CAS.TYPE_NAME_BOOLEAN)) {
                    if (!(fsNew.getBooleanValue(feature) == fsOld.getBooleanValue(olFeature))) {
                        // disagree
                        agreeOnSubfeatures = false;
                    }
                    else {
                        // agree
                    }
                }
                else if (feature.getRange().getName().equals(CAS.TYPE_NAME_STRING)) {
                    String stringValue1 = fsNew.getStringValue(feature);
                    String stringValue2 = fsNew.getStringValue(feature);
                    if (stringValue1 == null && stringValue2 == null) {
                        // agree
                        // Do nothing, null == null
                    }
                    else if (stringValue1 == null
                            || stringValue2 == null
                            || !(fsNew.getStringValue(feature).equals(fsOld
                                    .getStringValue(olFeature)))) {
                        // stringValue1 differs from stringValue2

                        // disagree
                        agreeOnSubfeatures = false;
                        // compareResult.getDiffs().put(fs1, fs2);
                    }
                    else {
                        // agree
                        // compareResult.getAgreements().put(fs1, fs2);
                        // diffFS1.remove(fs1);
                    }
                }
                else {
                    throw new RangeNameNotCheckedException(feature.getRange().getName()
                            + " not yet supported!");
                }

                // check other Values
            }
            else if (feature.toString().equals("uima.cas.AnnotationBase:sofa")) {
                continue;
            }
            else {
                // compare composite types
                // assumption: if feature is not primitive, it is a composite feature
                FeatureStructure featureValue1 = fsNew.getFeatureValue(feature);
                FeatureStructure featureValue2 = fsOld.getFeatureValue(olFeature);
                if(((AnnotationFS)featureValue1).getBegin()!=((AnnotationFS)featureValue2).getBegin()
                        ||((AnnotationFS)featureValue1).getEnd()!=((AnnotationFS)featureValue2).getEnd()){
                    agreeOnSubfeatures = false;
                }
                if (featureValue1 != null && featureValue2 != null &&
                        (aType.toString().equals(featureValue1.getType().toString()))) {
                    CompareResult compareResultSubfeatures = compareFeatureFS(aType,
                            featureValue1, featureValue2, diffFSNew);
                    compareResult.getDiffs().putAll(compareResultSubfeatures.getDiffs());
                    compareResult.getAgreements().putAll(compareResultSubfeatures.getAgreements());
                    if(!compareResult.getDiffs().isEmpty()) {
                        agreeOnSubfeatures = false;
                    }
                }
            }
        }
        if(agreeOnSubfeatures) {
            compareResult.getAgreements().put(fsNew, fsOld);
            diffFSNew.remove(fsNew);
        } else {
            compareResult.getDiffs().put(fsNew, fsOld);
        }

        // if no diffs, agree (here or elsewhere)?
        if(compareResult.getDiffs().isEmpty()) {
            compareResult.getAgreements().put(fsNew, fsOld);
            diffFSNew.remove(fsNew);
        }

        return compareResult;
    }

}

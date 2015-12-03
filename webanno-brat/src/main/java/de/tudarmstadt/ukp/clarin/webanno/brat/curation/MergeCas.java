/*******************************************************************************
 * Copyright 2015
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.ConfigurationSet;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.Position;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.CurationPanel;

/**
 * Do a merge CAS out of multiple user annotations
 *
 */
public class MergeCas
{
    /**
     * Using {@code DiffResult}, determine the annotations to be deleted from the randomly generated
     * MergeCase. The initial Merge CAs is stored under a name {@code CurationPanel#CURATION_USER}.
     * <p>
     * Any similar annotations stacked in a {@code CasDiff2.Position} will be assumed a difference
     * <p>
     * Any two annotation with different value will be assumed a difference
     * <p>
     * Any non stacked empty/null annotations are assumed agreement
     * <p>
     * Any non stacked annotations with similar values for each of the features are assumed
     * agreement
     * <p>
     * Any two link mode / slotable annotations which agree on the base features are assumed
     * agreement
     * 
     * @param aDiff
     *            the {@code CasDiff2.DiffResult}
     * @param aJCases
     *            a map of{@code JCas}s for each users and the random merge
     * @return the actual merge {@code JCas}
     */
    public static JCas geMergeCas(DiffResult aDiff, Map<String, JCas> aJCases)
    {

        Set<FeatureStructure> spansToMerge = new HashSet<>();
        Set<FeatureStructure> spansToDelete = new HashSet<>();

        Set<FeatureStructure> slotsToMerge = new HashSet<>();
        Set<FeatureStructure> slotsToDelete = new HashSet<>();

        Set<FeatureStructure> relationsToMerge = new HashSet<>();
        Set<FeatureStructure> relationsToDelete = new HashSet<>();
        Collection<Position> positions = aDiff.getPositions();

        System.out.println(positions.size());

        for (Position position : positions) {

            Map<String, List<FeatureStructure>> annosPerUser = new HashMap<>();

            ConfigurationSet cfgs = aDiff.getConfigurtionSet(position);

            if (cfgs.getConfigurations(CurationPanel.CURATION_USER).size() == 0) { // incomplete
                                                                                   // anno
                continue;
            }
            FeatureStructure mergeAnno = cfgs.getConfigurations(CurationPanel.CURATION_USER).get(0)
                    .getFs(CurationPanel.CURATION_USER, aJCases);

            // Get Annotations per user in this position
            for (String usr : cfgs.getCasGroupIds()) {
                if (!annosPerUser.containsKey(usr)) {
                    List<FeatureStructure> fssAtThisPosition = getFSAtPosition(aJCases, mergeAnno,
                            usr);
                    annosPerUser.put(usr, fssAtThisPosition);
                }
                else {
                    List<FeatureStructure> fssAtThisPosition = getFSAtPosition(aJCases, mergeAnno,
                            usr);
                    annosPerUser.get(usr).addAll(fssAtThisPosition);
                }
            }

            for (FeatureStructure mergeFs : annosPerUser.get(CurationPanel.CURATION_USER)) {
                // incomplete annotations
                if (aJCases.size() != annosPerUser.size()) {
                    spansToDelete.add(mergeFs);
                }
                // agreed and not stacked
                else if (isAgree(mergeFs, annosPerUser)) {
                    spansToMerge.add(mergeFs);
                }
                // disagree or stacked annotations
                else {
                    spansToDelete.add(mergeFs);
                }
            }
        }
        for (FeatureStructure fs : spansToDelete) {
            aJCases.get(CurationPanel.CURATION_USER).removeFsFromIndexes(fs);
        }
        return aJCases.get(CurationPanel.CURATION_USER);
    }

    /**
     * Returns list of Annotations on this particular position (basically when stacking is allowed)
     * 
     * @return
     */
    private static List<FeatureStructure> getFSAtPosition(Map<String, JCas> aJCases,
            FeatureStructure fs, String aUser)
    {
        Type t = fs.getType();
        int begin = ((AnnotationFS) fs).getBegin();
        int end = ((AnnotationFS) fs).getEnd();

        List<FeatureStructure> fssAtThisPosition = new ArrayList<>();
        for (FeatureStructure fss : CasUtil.selectCovered(aJCases.get(aUser).getCas(), t, begin,
                end)) {
            fssAtThisPosition.add(fss);
        }
        return fssAtThisPosition;
    }

    /**
     * Returns true if a span annotation agrees on all features values (including null/empty as
     * agreement) and no stacking is found in this position
     */
    public static boolean isAgree(FeatureStructure aMergeFs,
            Map<String, List<FeatureStructure>> aAnnosPerUser)
    {
        for (String usr : aAnnosPerUser.keySet()) {
            boolean agree = false;
            for (FeatureStructure usrFs : aAnnosPerUser.get(usr)) {
                if (isSameSpanAnno(aMergeFs, usrFs)) {
                    if (!agree) { // this anno is the same with the others
                        agree = true;
                    }
                    else if (agree) { // this is a stacked annotation
                        return false;
                    }
                }
            }
            // do not match in at least one user annotation in this position
            if (!agree) {
                return false;
            }
        }
        return true;
    }

    public static boolean isSameSpanAnno(FeatureStructure aFirstFS, FeatureStructure aSeconFS)
    {

        for (Feature f : aFirstFS.getType().getFeatures()) {

            // the annotations are already in the same position
            if (f.getName().equals(CAS.FEATURE_FULL_NAME_BEGIN)
                    || f.getName().equals(CAS.FEATURE_FULL_NAME_END)
                    || f.getName().equals(CAS.FEATURE_FULL_NAME_SOFA)
                    || f.toString().equals("uima.cas.AnnotationBase:sofa")) {

                continue;
            }

            if (!isLinkMode(aFirstFS, f)) {
                // assume null as equal
                if (getFeatureValue(aFirstFS, f) == null && getFeatureValue(aSeconFS, f) == null) {
                    continue;
                }
                if (getFeatureValue(aFirstFS, f) == null && getFeatureValue(aSeconFS, f) != null) {
                    return false;
                }
                if (getFeatureValue(aFirstFS, f) != null && getFeatureValue(aSeconFS, f) == null) {
                    return false;
                }
                if (!getFeatureValue(aFirstFS, f).equals(getFeatureValue(aSeconFS, f))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns true if this is slot feature
     */
    private static boolean isLinkMode(FeatureStructure aFs, Feature aFeature)
    {
        try {
            ArrayFS x = (ArrayFS) BratAjaxCasUtil.getFeatureFS(aFs, aFeature.getShortName());
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the feature value of this {@code Feature} on this annotation
     */
    public static Object getFeatureValue(FeatureStructure aFS, Feature aFeature)
    {
        switch (aFeature.getName()) {
        case CAS.TYPE_NAME_STRING:
            return aFS.getFeatureValueAsString(aFeature);
        case CAS.TYPE_NAME_BOOLEAN:
            return aFS.getBooleanValue(aFeature);
        case CAS.TYPE_NAME_FLOAT:
            return aFS.getFloatValue(aFeature);
        case CAS.TYPE_NAME_INTEGER:
            return aFS.getIntValue(aFeature);
        case CAS.TYPE_NAME_BYTE:
            return aFS.getByteValue(aFeature);
        case CAS.TYPE_NAME_DOUBLE:
            return aFS.getDoubleValue(aFeature);
        case CAS.TYPE_NAME_LONG:
            aFS.getLongValue(aFeature);
        case CAS.TYPE_NAME_SHORT:
            aFS.getShortValue(aFeature);
        default:
            return aFS.getFeatureValueAsString(aFeature);
        }
    }

}
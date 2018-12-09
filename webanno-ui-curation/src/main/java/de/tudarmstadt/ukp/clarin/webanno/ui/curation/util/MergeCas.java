/*
 * Copyright 2015
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.util;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.setFeature;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.LinkWithRoleModel;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.ConfigurationSet;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff2.Position;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Stem;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * Do a merge CAS out of multiple user annotations
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
    public static JCas reMergeCas(DiffResult aDiff, Map<String, JCas> aJCases)
    {
        Set<FeatureStructure> slotFeaturesToReset = new HashSet<>();
        Set<FeatureStructure> annotationsToDelete = new HashSet<>();

        Set<String> users = aJCases.keySet();

        for (Position position : aDiff.getPositions()) {
            Map<String, List<FeatureStructure>> annosPerUser = new HashMap<>();

            ConfigurationSet cfgs = aDiff.getConfigurtionSet(position);

            if (cfgs.getConfigurations(WebAnnoConst.CURATION_USER).isEmpty()) { // incomplete
                // annotations
                continue;
            }
            
            AnnotationFS mergeAnno = (AnnotationFS) cfgs
                    .getConfigurations(WebAnnoConst.CURATION_USER).get(0)
                    .getFs(WebAnnoConst.CURATION_USER, aJCases);

            // Get Annotations per user in this position
            getAllAnnosOnPosition(aJCases, annosPerUser, users, mergeAnno);

            for (FeatureStructure mergeFs : annosPerUser.get(WebAnnoConst.CURATION_USER)) {
                // incomplete annotations
                if (aJCases.size() != annosPerUser.size()) {
                    annotationsToDelete.add(mergeFs);
                }
                // agreed and not stacked
                else if (isAgree(mergeFs, annosPerUser)) {
                    Type t = mergeFs.getType();
                    Feature sourceFeat = t.getFeatureByBaseName(WebAnnoConst.FEAT_REL_SOURCE);
                    Feature targetFeat = t.getFeatureByBaseName(WebAnnoConst.FEAT_REL_TARGET);

                    // Is this a relation?
                    if (sourceFeat != null && targetFeat != null) {

                        AnnotationFS source = (AnnotationFS) mergeFs.getFeatureValue(sourceFeat);
                        AnnotationFS target = (AnnotationFS) mergeFs.getFeatureValue(targetFeat);

                        // all span anno on this source positions
                        Map<String, List<FeatureStructure>> sourceAnnosPerUser = new HashMap<>();
                        // all span anno on this target positions
                        Map<String, List<FeatureStructure>> targetAnnosPerUser = new HashMap<>();

                        getAllAnnosOnPosition(aJCases, sourceAnnosPerUser, users, source);
                        getAllAnnosOnPosition(aJCases, targetAnnosPerUser, users, target);

                        if (isAgree(source, sourceAnnosPerUser)
                                && isAgree(target, targetAnnosPerUser)) {
                            slotFeaturesToReset.add(mergeFs);
                        }
                        else {
                            annotationsToDelete.add(mergeFs);
                        }
                    }
                    else {
                        slotFeaturesToReset.add(mergeFs);
                    }
                }
                // disagree or stacked annotations
                else {
                    annotationsToDelete.add(mergeFs);
                }

                // remove dangling rels
                // setDanglingRelToDel(aJCases.get(CurationPanel.CURATION_USER),
                // mergeFs, annotationsToDelete);
            }
        }

        // remove annotations that do not agree or are a stacked ones
        for (FeatureStructure fs : annotationsToDelete) {
            if (!slotFeaturesToReset.contains(fs)) {
                JCas mergeCas = aJCases.get(WebAnnoConst.CURATION_USER);
                // Check if this difference is on POS, STEM and LEMMA (so remove from the token too)
                Type type = fs.getType();
                int fsBegin = ((AnnotationFS) fs).getBegin();
                int fsEnd = ((AnnotationFS) fs).getEnd();
                if (type.getName().equals(POS.class.getName())) {
                    Token t = JCasUtil.selectCovered(mergeCas, Token.class, fsBegin, fsEnd).get(0);
                    t.setPos(null);
                }
                if (type.getName().equals(Stem.class.getName())) {
                    Token t = JCasUtil.selectCovered(mergeCas, Token.class, fsBegin, fsEnd).get(0);
                    t.setStem(null);
                }
                if (type.getName().equals(Lemma.class.getName())) {
                    Token t = JCasUtil.selectCovered(mergeCas, Token.class, fsBegin, fsEnd).get(0);
                    t.setLemma(null);
                }
                if (type.getName().equals(MorphologicalFeatures.class.getName())) {
                    Token t = JCasUtil.selectCovered(mergeCas, Token.class, fsBegin, fsEnd).get(0);
                    t.setMorph(null);
                }
                mergeCas.removeFsFromIndexes(fs);
            }
        }
        
        // if slot bearing annotation, clean
        for (FeatureStructure baseFs : slotFeaturesToReset) {
            for (Feature roleFeature : baseFs.getType().getFeatures()) {
                if (isLinkMode(baseFs, roleFeature)) {
                    // FeatureStructure roleFs = baseFs.getFeatureValue(f);
                    ArrayFS roleFss = (ArrayFS) WebAnnoCasUtil.getFeatureFS(baseFs,
                            roleFeature.getShortName());
                    if (roleFss == null) {
                        continue;
                    }
                    Map<String, ArrayFS> roleAnnosPerUser = new HashMap<>();

                    setAllRoleAnnosOnPosition(aJCases, roleAnnosPerUser, users, baseFs,
                            roleFeature);
                    List<FeatureStructure> linkFSes = new LinkedList<>(
                            Arrays.asList(roleFss.toArray()));
                    for (FeatureStructure roleFs : roleFss.toArray()) {
                        if (isRoleAgree(roleFs, roleAnnosPerUser)) {
                            for (Feature targetFeature : roleFs.getType().getFeatures()) {
                                if (isBasicFeature(targetFeature)) {
                                    continue;
                                }
                                if (!targetFeature.getShortName().equals("target")) {
                                    continue;
                                }
                                AnnotationFS targetFs = (AnnotationFS) roleFs
                                        .getFeatureValue(targetFeature);
                                if (targetFs == null) {
                                    continue;
                                }
                                Map<String, List<FeatureStructure>> targetAnnosPerUser = 
                                        new HashMap<>();
                                getAllAnnosOnPosition(aJCases, targetAnnosPerUser, users, targetFs);

                                // do not agree on targets
                                if (!isAgree(targetFs, targetAnnosPerUser)) {
                                    linkFSes.remove(roleFs);
                                }
                            }
                        }
                        // do not agree on some role features
                        else {
                            linkFSes.remove(roleFs);
                        }
                    }

                    ArrayFS array = baseFs.getCAS().createArrayFS(linkFSes.size());
                    array.copyFromArray(linkFSes.toArray(new FeatureStructure[linkFSes.size()]), 0,
                            0, linkFSes.size());
                    baseFs.setFeatureValue(roleFeature, array);
                }
            }
        }

        return aJCases.get(WebAnnoConst.CURATION_USER);
    }

    /**
     * Do not check on agreement on Position and SOfa feature - already checked
     */
    private static boolean isBasicFeature(Feature aFeature)
    {
        return aFeature.getName().equals(CAS.FEATURE_FULL_NAME_SOFA)
                || aFeature.toString().equals("uima.cas.AnnotationBase:sofa");
    }

    private static void getAllAnnosOnPosition(Map<String, JCas> aJCases,
            Map<String, List<FeatureStructure>> aAnnosPerUser, Set<String> aUsers,
            AnnotationFS aMergeAnno)
    {
        for (String user : aUsers) {
            List<AnnotationFS> fssAtThisPosition = getFSAtPosition(aJCases, aMergeAnno, user);
            if (!aAnnosPerUser.containsKey(user)) {
                aAnnosPerUser.put(user, (List) fssAtThisPosition);
            }
            else {
                aAnnosPerUser.get(user).addAll(fssAtThisPosition);
            }
        }
    }

    private static void setAllRoleAnnosOnPosition(Map<String, JCas> aJCases,
            Map<String, ArrayFS> slotAnnosPerUser, Set<String> aUsers, FeatureStructure aBaseAnno,
            Feature aFeature)
    {
        Type t = aBaseAnno.getType();
        int begin = ((AnnotationFS) aBaseAnno).getBegin();
        int end = ((AnnotationFS) aBaseAnno).getEnd();

        for (String user : aUsers) {
            for (AnnotationFS baseFS : selectCovered(aJCases.get(user).getCas(), t,
                    begin, end)) {
                // if non-equal stacked annotations with slot feature exists, get the right one
                if (isSameAnno(aBaseAnno, baseFS)) {
                    ArrayFS roleFs = (ArrayFS) WebAnnoCasUtil.getFeatureFS(baseFS,
                            aFeature.getShortName());
                    slotAnnosPerUser.put(user, roleFs);
                    break;
                }
            }
        }
    }

    /**
     * Returns list of Annotations on this particular position (basically when stacking is allowed).
     */
    private static List<AnnotationFS> getFSAtPosition(Map<String, JCas> aJCases,
            AnnotationFS fs, String aUser)
    {
        return selectCovered(aJCases.get(aUser).getCas(), fs.getType(), fs.getBegin(), fs.getEnd());
    }

    /**
     * Returns true if a span annotation agrees on all features values (including null/empty as
     * agreement) and no stacking is found in this position
     */
    private static boolean isAgree(FeatureStructure aMergeFs,
            Map<String, List<FeatureStructure>> aAnnosPerUser)
    {
        for (String user : aAnnosPerUser.keySet()) {
            boolean agree = false;
            for (FeatureStructure usrFs : aAnnosPerUser.get(user)) {
                // same on all non slot feature values
                if (isSameAnno(aMergeFs, usrFs)) {
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

    private static boolean isRoleAgree(FeatureStructure aMergeFs,
            Map<String, ArrayFS> aAnnosPerUser)
    {
        for (String user : aAnnosPerUser.keySet()) {
            boolean agree = false;
            if (aAnnosPerUser.get(user) == null) {
                return false;
            }
            for (FeatureStructure usrFs : aAnnosPerUser.get(user).toArray()) {
                // same on all non slot feature values
                if (isSameAnno(aMergeFs, usrFs)) {
                    if (!agree) { // this anno is the same with the others
                        agree = true;
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

    /**
     * Return true if these two annotations agree on every non slot features
     */
    private static boolean isSameAnno(FeatureStructure aFirstFS, FeatureStructure aSeconFS)
    {
        for (Feature f : getAllFeatures(aFirstFS)) {
            // the annotations are already in the same position
            if (isBasicFeature(f)) {
                continue;
            }

            if (!isLinkMode(aFirstFS, f)) {
                // check if attache type exists
                try {
                    FeatureStructure attachFs1 = aFirstFS.getFeatureValue(f);
                    FeatureStructure attachFs2 = aSeconFS.getFeatureValue(f);
                    if (!isSameAnno(attachFs1, attachFs2)) {
                        return false;
                    }
                }
                catch (Exception e) {
                    // no attach type -- continue
                }
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

    public static Feature[] getAllFeatures(FeatureStructure aFS)
    {
        Feature[] cachedSortedFeatures = new Feature[aFS.getType().getNumberOfFeatures()];
        int i = 0;
        for (Feature f : aFS.getType().getFeatures()) {
            cachedSortedFeatures[i] = f;
            i++;
        }
        return cachedSortedFeatures;
    }

    /**
     * Returns true if this is slot feature
     */
    private static boolean isLinkMode(FeatureStructure aFs, Feature aFeature)
    {
        try {
            ArrayFS slotFs = (ArrayFS) WebAnnoCasUtil.getFeatureFS(aFs, aFeature.getShortName());
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the feature value of this {@code Feature} on this annotation
     */
    private static Object getFeatureValue(FeatureStructure aFS, Feature aFeature)
    {
        switch (aFeature.getRange().getName()) {
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
            return null;
        // return aFS.getFeatureValue(aFeature);
        }
    }

    private static void setFeatureValue(FeatureStructure aFS, Feature aFeature, Object aValue)
    {
        switch (aFeature.getRange().getName()) {
        case CAS.TYPE_NAME_STRING:
            aFS.setStringValue(aFeature, aValue == null ? null : aValue.toString());
            break;
        case CAS.TYPE_NAME_BOOLEAN:
            aFS.setBooleanValue(aFeature, Boolean.valueOf(aValue.toString()));
            break;
        case CAS.TYPE_NAME_FLOAT:
            aFS.setFloatValue(aFeature, Float.valueOf(aValue.toString()));
            break;
        case CAS.TYPE_NAME_INTEGER:
            aFS.setIntValue(aFeature, Integer.valueOf(aValue.toString()));
            break;
        case CAS.TYPE_NAME_BYTE:
            aFS.setByteValue(aFeature, Byte.valueOf(aValue.toString()));
            break;
        case CAS.TYPE_NAME_DOUBLE:
            aFS.setDoubleValue(aFeature, Double.valueOf(aValue.toString()));
            break;
        case CAS.TYPE_NAME_LONG:
            aFS.setLongValue(aFeature, Long.valueOf(aValue.toString()));
            break;
        case CAS.TYPE_NAME_SHORT:
            aFS.setShortValue(aFeature, Short.valueOf(aValue.toString()));
            break;
        default:
            return;
            // return aFS.getFeatureValue(aFeature);
        }
    }

    private static boolean existsSameAnnoOnPosition(AnnotationFS aFs, JCas aJcas)
    {
        for (AnnotationFS annotationFS : getAnnosOnPosition(aFs, aJcas)) {
            if (isSameAnno(aFs, annotationFS)) {
                return true;
            }
        }
        return false;
    }

    private static List<AnnotationFS> getAnnosOnPosition(AnnotationFS aFs, JCas aJcas)
    {
        return selectCovered(aJcas.getCas(), aFs.getType(), aFs.getBegin(), aFs.getEnd());
    }

    private static List<AnnotationFS> getRelAnnosOnPosition(AnnotationFS aFs,
            AnnotationFS aOriginFs, AnnotationFS aTargetFs, JCas aJcas)
    {
        Type type = aFs.getType();
        Feature sourceFeat = type.getFeatureByBaseName(WebAnnoConst.FEAT_REL_SOURCE);
        Feature targetFeat = type.getFeatureByBaseName(WebAnnoConst.FEAT_REL_TARGET);
        return selectCovered(aJcas.getCas(), type, aFs.getBegin(), aFs.getEnd()).stream()
                .filter(fs -> fs.getFeatureValue(sourceFeat).equals(aOriginFs)
                        && fs.getFeatureValue(targetFeat).equals(aTargetFs))
                .collect(Collectors.toList());
    }

    /**
     * Copy this same annotation from the user annotation to the mergeview
     */
    private static void copySpanAnnotation(AnnotatorState aState,
            AnnotationSchemaService aAnnotationService, AnnotationLayer aAnnotationLayer,
            AnnotationFS aOldFs, JCas aJCas)
        throws AnnotationException
    {
        SpanAdapter adapter = (SpanAdapter) aAnnotationService.getAdapter(aAnnotationLayer);

        // Create the annotation - this also takes care of attaching to an annotation if necessary
        int id = adapter.add(aState.getDocument(), aState.getUser().getUsername(), aJCas,
                aOldFs.getBegin(), aOldFs.getEnd());

        List<AnnotationFeature> features = aAnnotationService
                .listAnnotationFeature(adapter.getLayer());

        // Copy the features
        for (AnnotationFeature feature : features) {
            Type oldType = adapter.getAnnotationType(aOldFs.getCAS());
            Feature oldFeature = oldType.getFeatureByBaseName(feature.getName());
            if (isLinkOrBasicFeatures(aOldFs, oldFeature)) {
                continue;
            }
            Object value = adapter.getFeatureValue(feature, aOldFs);
            adapter.setFeatureValue(aState.getDocument(), aState.getUser().getUsername(), aJCas, id,
                    feature, value);
        }
    }

    private static void copyRelationAnnotation(AnnotationFS aOldFs, AnnotationFS asourceFS,
            AnnotationFS aTargetFs, JCas aJCas)
    {
        Feature[] features = getAllFeatures(aOldFs);
        Type type = aOldFs.getType();
        Feature sourceFeat = type.getFeatureByBaseName(WebAnnoConst.FEAT_REL_SOURCE);
        Feature targetFeat = type.getFeatureByBaseName(WebAnnoConst.FEAT_REL_TARGET);
        AnnotationFS newFs = aJCas.getCas().createAnnotation(type, aOldFs.getBegin(),
                aOldFs.getEnd());
        for (Feature f : features) {
            if (isLinkOrBasicFeatures(aOldFs, f)) {
                continue;
            }
            if (f.equals(sourceFeat)) {
                newFs.setFeatureValue(f, asourceFS);
            }
            else if (f.equals(targetFeat)) {
                newFs.setFeatureValue(f, aTargetFs);
            }
            else {
                setFeatureValue(newFs, f, getFeatureValue(aOldFs, f));
            }
        }
        aJCas.addFsToIndexes(newFs);
    }

    /**
     * Modify existing non-stackable annotations from one of the users annotation
     */
    private static void modifySpanAnnotation(AnnotationFS aOldFs, AnnotationFS aNewFs, JCas aJCas)
    {
        Feature[] features = getAllFeatures(aOldFs);
        for (Feature f : features) {
            if (isLinkOrBasicFeatures(aOldFs, f)) {
                continue;
            }
            setFeatureValue(aNewFs, f, getFeatureValue(aOldFs, f));
        }
        aJCas.addFsToIndexes(aNewFs);
    }

    private static void modifyRelationAnnotation(AnnotationFS aOldFs, AnnotationFS aNewFs,
            JCas aJCas)
    {
        Feature[] features = getAllFeatures(aOldFs);
        Type type = aOldFs.getType();
        Feature sourceFeat = type.getFeatureByBaseName(WebAnnoConst.FEAT_REL_SOURCE);
        Feature targetFeat = type.getFeatureByBaseName(WebAnnoConst.FEAT_REL_TARGET);

        for (Feature f : features) {
            if (isLinkOrBasicFeatures(aOldFs, f)) {
                continue;
            }
            if (f.equals(sourceFeat)) {
                continue;
            }
            else if (f.equals(targetFeat)) {
                continue;
            }

            setFeatureValue(aNewFs, f, getFeatureValue(aOldFs, f));
        }
        aJCas.addFsToIndexes(aNewFs);
    }

    private static Stream<AnnotationFS> getMergeFS(AnnotationFS aOldFs, JCas aJCas)
    {
        return selectCovered(aJCas.getCas(), aOldFs.getType(), aOldFs.getBegin(), aOldFs.getEnd())
                .stream().filter(fs -> isSameAnno(fs, aOldFs));
    }

    private static boolean isLinkOrBasicFeatures(FeatureStructure aOldFs, Feature aFeature)
    {
        return isLinkMode(aOldFs, aFeature) || isBasicFeature(aFeature) ||
            aFeature.getName().equals(CAS.FEATURE_FULL_NAME_BEGIN) ||
            aFeature.getName().equals(CAS.FEATURE_FULL_NAME_END);
    }

    public static void addSpanAnnotation(AnnotatorState aState,
            AnnotationSchemaService aAnnotationService, AnnotationLayer aAnnotationLayer,
            JCas aMergeJCas, AnnotationFS aFSClicked, boolean aAllowStacking)
        throws AnnotationException
    {
        if (MergeCas.existsSameAnnoOnPosition(aFSClicked, aMergeJCas)) {
            throw new AnnotationException(
                    "Same Annotation exists on the mergeview. Please add it manually.");
        }

        // a) if stacking allowed add this new annotation to the mergeview
        List<AnnotationFS> existingAnnos = MergeCas.getAnnosOnPosition(aFSClicked, aMergeJCas);
        if (existingAnnos.size() == 0 || aAllowStacking) {
            MergeCas.copySpanAnnotation(aState, aAnnotationService, aAnnotationLayer, aFSClicked,
                    aMergeJCas);
        }

        // b) if stacking is not allowed, modify the existing annotation with this one
        else {
            MergeCas.modifySpanAnnotation(aFSClicked, existingAnnos.get(0), aMergeJCas);
        }
    }

    public static void addArcAnnotation(TypeAdapter aAdapter, JCas aJcas,
            int aAddressOriginClicked, int aAddressTargetClicked, String aFSArcaddress,
            JCas aClickedJCas, AnnotationFS aClickedFS)
        throws AnnotationException
    {
        AnnotationFS originFsClicked = selectByAddr(aClickedJCas, aAddressOriginClicked);
        AnnotationFS targetFsClicked = selectByAddr(aClickedJCas, aAddressTargetClicked);

        // this is a slot arc
        if (aFSArcaddress.contains(".")) {
            addSlotArcAnnotation((SpanAdapter) aAdapter, aJcas, aFSArcaddress, aClickedJCas,
                    aClickedFS);
        }
        // normal relation annotation arc is clicked
        else {
            AnnotationLayer layer = aAdapter.getLayer();
            addRelationArcAnnotation(aJcas, aClickedFS, layer.getAttachType() != null,
                    layer.isAllowStacking(), originFsClicked, targetFsClicked);
        }
    }

    static void addRelationArcAnnotation(JCas aJcas, AnnotationFS aClickedFS,
            boolean aIsAttachType, boolean aIsAllowStacking, AnnotationFS originFsClicked,
            AnnotationFS targetFsClicked)
        throws AnnotationException
    {
        List<AnnotationFS> merges = MergeCas.getMergeFS(aClickedFS, aJcas)
                .collect(Collectors.toList());

        List<AnnotationFS> origins = MergeCas.getMergeFS(originFsClicked, aJcas)
                .collect(Collectors.toList());
        List<AnnotationFS> targets = MergeCas.getMergeFS(targetFsClicked, aJcas)
                .collect(Collectors.toList());

        // check if target/source exists in the mergeview
        if (origins.size() == 0 || targets.size() == 0) {
            throw new AnnotationException("Both the source and target annotation"
                    + " should exist on the mergeview. Please first copy/create them");
        }

        AnnotationFS originFs = origins.get(0);
        AnnotationFS targetFs = targets.get(0);

        if (origins.size() > 1) {
            throw new AnnotationException(
                    "Stacked sources exist in mergeview. Cannot copy this relation.");

        }
        if (targets.size() > 1) {
            throw new AnnotationException(
                    "Stacked targets exist in mergeview. Cannot copy this relation.");

        }
        if (merges.size() > 0) {
            throw new AnnotationException("The annotation already exists on the mergeview. "
                    + "Add this manually to have stacked annotations");
        }

        // TODO: DKPro Core Dependency layer -> It should be done differently
        if (aIsAttachType) {
            Type type = aClickedFS.getType();
            Feature sourceFeature = type.getFeatureByBaseName(WebAnnoConst.FEAT_REL_SOURCE);
            originFsClicked = (AnnotationFS) aClickedFS.getFeatureValue(sourceFeature);

            Feature targetFeature = type.getFeatureByBaseName(WebAnnoConst.FEAT_REL_TARGET);
            targetFsClicked = (AnnotationFS) aClickedFS.getFeatureValue(targetFeature);

            origins = MergeCas.getMergeFS(originFsClicked, aJcas).collect(Collectors.toList());
            targets = MergeCas.getMergeFS(targetFsClicked, aJcas).collect(Collectors.toList());
            originFs = origins.get(0);
            targetFs = targets.get(0);
        }

        List<AnnotationFS> existingAnnos = MergeCas.getRelAnnosOnPosition(aClickedFS, originFs,
                targetFs, aJcas);
        if (existingAnnos.size() == 0 || aIsAllowStacking) {
            MergeCas.copyRelationAnnotation(aClickedFS, originFs, targetFs, aJcas);
        }
        else {
            MergeCas.modifyRelationAnnotation(aClickedFS, existingAnnos.get(0), aJcas);
        }
    }

    private static void addSlotArcAnnotation(SpanAdapter aAdapter, JCas aJcas, String aFSArcaddress,
            JCas aClickedJCas, AnnotationFS aClickedFS)
        throws AnnotationException
    {
        List<AnnotationFS> merges = MergeCas.getMergeFS(aClickedFS, aJcas)
                .collect(Collectors.toList());

        AnnotationFS targetFs;
        if (merges.size() == 0) {
            throw new AnnotationException(
                    "The base annotation do not exist. Please add it first. ");
        }
        AnnotationFS mergeFs = merges.get(0);
        int fiIndex = Integer.parseInt(aFSArcaddress.split("\\.")[1]);
        int liIndex = Integer.parseInt(aFSArcaddress.split("\\.")[2]);

        AnnotationFeature slotFeature = null;
        LinkWithRoleModel linkRole = null;
        int fi = 0;
        f: for (AnnotationFeature feat : aAdapter.listFeatures()) {
            if (MultiValueMode.ARRAY.equals(feat.getMultiValueMode())
                    && LinkMode.WITH_ROLE.equals(feat.getLinkMode())) {
                List<LinkWithRoleModel> links = aAdapter.getFeatureValue(feat, aClickedFS);
                for (int li = 0; li < links.size(); li++) {
                    LinkWithRoleModel link = links.get(li);
                    if (fi == fiIndex && li == liIndex) {
                        slotFeature = feat;

                        List<AnnotationFS> targets = checkAndGetTargets(aJcas, aClickedJCas,
                                selectByAddr(aClickedJCas, link.targetAddr));
                        targetFs = targets.get(0);
                        link.targetAddr = getAddr(targetFs);
                        linkRole = link;
                        break f;
                    }
                }
            }
            fi++;
        }

        List<LinkWithRoleModel> links = aAdapter.getFeatureValue(slotFeature, mergeFs);
        LinkWithRoleModel duplicateLink = null; //
        for (LinkWithRoleModel lr : links) {
            if (lr.targetAddr == linkRole.targetAddr) {
                duplicateLink = lr;
                break;
            }
        }
        links.add(linkRole);
        links.remove(duplicateLink);

        setFeature(mergeFs, slotFeature, links);
    }

    private static List<AnnotationFS> checkAndGetTargets(JCas aJcas, JCas aClickedJCas,
            AnnotationFS aOldTraget)
        throws AnnotationException
    {
        List<AnnotationFS> targets = MergeCas.getMergeFS(aOldTraget, aJcas)
                .collect(Collectors.toList());

        if (targets.size() == 0) {
            throw new AnnotationException(
                    "This target annotation do not exist." + " Copy or create the target first ");
        }

        if (targets.size() > 1) {

            throw new AnnotationException("There are multiple targets on the mergeview."
                    + " Can not copy this slot annotation.");
        }
        return targets;
    }
}

/*
 * Copyright 2014
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
 */
package de.tudarmstadt.ukp.clarin.webanno.tsv;

import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.selectFS;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.dkpro.core.api.io.JCasFileWriter_ImplBase;
import org.dkpro.core.api.parameter.ComponentParameters;

import de.tudarmstadt.ukp.clarin.webanno.tsv.util.AnnotationUnit;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 * Export annotations in TAB separated format. Header includes information about the UIMA type and
 * features The number of columns are depend on the number of types/features exist. All the spans
 * will be written first and subsequently all the relations. relation is given in the form of
 * Source--&gt;Target and the RelationType is added to the Target token. The next column indicates
 * the source of the relation (the source of the arc drown)
 */
public class WebannoTsv3Writer
    extends JCasFileWriter_ImplBase
{
    /**
     * Name of configuration parameter that contains the character encoding used by the input files.
     */
    public static final String PARAM_ENCODING = ComponentParameters.PARAM_TARGET_ENCODING;
    @ConfigurationParameter(name = PARAM_ENCODING, mandatory = true, defaultValue = "UTF-8")
    private String encoding;

    public static final String PARAM_FILENAME_SUFFIX = "filenameSuffix";
    @ConfigurationParameter(name = PARAM_FILENAME_SUFFIX, mandatory = true, defaultValue = ".tsv")
    private String filenameSuffix;

    public static final String PARAM_SPAN_LAYERS = "spanLayers";
    @ConfigurationParameter(name = PARAM_SPAN_LAYERS, mandatory = true, defaultValue = {})
    private List<String> spanLayers;

    public static final String PARAM_SLOT_FEATS = "slotFeatures";
    @ConfigurationParameter(name = PARAM_SLOT_FEATS, mandatory = true, defaultValue = {})
    private List<String> slotFeatures;

    public static final String PARAM_LINK_TYPES = "linkTypes";
    @ConfigurationParameter(name = PARAM_LINK_TYPES, mandatory = true, defaultValue = {})
    private List<String> linkTypes;

    public static final String PARAM_SLOT_TARGETS = "slotTargets";
    @ConfigurationParameter(name = PARAM_SLOT_TARGETS, mandatory = true, defaultValue = {})
    private List<String> slotTargets;

    public static final String PARAM_CHAIN_LAYERS = "chainLayers";
    @ConfigurationParameter(name = PARAM_CHAIN_LAYERS, mandatory = true, defaultValue = {})
    private List<String> chainLayers;

    public static final String PARAM_RELATION_LAYERS = "relationLayers";
    @ConfigurationParameter(name = PARAM_RELATION_LAYERS, mandatory = true, defaultValue = {})
    private List<String> relationLayers;

    private static final String TAB = "\t";
    private static final String LF = "\n";
    private static final String DEPENDENT = "Dependent";
    private static final String GOVERNOR = "Governor";
    private static final String REF_REL = "referenceRelation";
    private static final String CHAIN = "Chain";
    private static final String LINK = "Link";
    private static final String FIRST = "first";
    private static final String NEXT = "next";
    public static final String SP = "T_SP"; // span annotation type
    public static final String CH = "T_CH"; // chain annotation type
    public static final String RL = "T_RL"; // relation annotation type
    public static final String ROLE = "ROLE_";
    public static final String BT = "BT_"; // base type for the relation
                                           // annotation
    private List<AnnotationUnit> units = new ArrayList<>();
    // number of subunits under this Annotation Unit
    private Map<AnnotationUnit, Integer> subUnits = new HashMap<>();
    private Map<String, Set<String>> featurePerLayer = new LinkedHashMap<>();
    private Map<AnnotationUnit, String> unitsLineNumber = new HashMap<>();
    private Map<AnnotationUnit, String> sentenceUnits = new HashMap<>();
    private Map<String, Map<AnnotationUnit, List<List<String>>>> annotationsPerPostion = 
            new HashMap<>();
    private Map<Feature, Type> slotFeatureTypes = new HashMap<>();

    private Map<Type, Map<FeatureStructure, Integer>> annotaionRefPerType = new HashMap<>();

    private Map<String, Map<AnnotationUnit, Boolean>> ambigUnits = new HashMap<>();
    private Map<Type, Map<AnnotationUnit, Map<FeatureStructure, Integer>>> multiAnnosPerUnit = 
            new HashMap<>();
    private Map<String, String> slotLinkTypes = new HashMap<>();
    private Map<Type, Integer> layerMaps = new LinkedHashMap<>();

    @Override
    public void process(JCas aJCas)
        throws AnalysisEngineProcessException
    {
        try (OutputStream docOS = getOutputStream(aJCas, filenameSuffix)) {
            resetVariables();
            setSlotLinkTypes();
            setLinkMaps(aJCas);
            setTokenSentenceAddress(aJCas);
            setAmbiguity(aJCas);
            setSpanAnnotation(aJCas);
            setChainAnnotation(aJCas);
            setRelationAnnotation(aJCas);
            writeHeader(docOS);
            for (AnnotationUnit unit : units) {
                if (sentenceUnits.containsKey(unit)) {
                    String[] sentWithNl = sentenceUnits.get(unit).split("\n");
                    IOUtils.write(LF + "#Text=" + escapeSpecial(sentWithNl[0]) + LF, 
                            docOS, encoding);
                    // if sentence contains new line character
                    // GITHUB ISSUE 318: New line in sentence should be exported as is
                    if (sentWithNl.length > 1) {
                        for (int i = 0; i < sentWithNl.length - 1; i++) {
                            IOUtils.write("#Text=" + escapeSpecial(sentWithNl[i + 1]) + LF, docOS,
                                    encoding);
                        }
                    }
                }
                if (unit.isSubtoken) {
                    IOUtils.write(unitsLineNumber.get(unit) + TAB + unit.begin + "-" + unit.end
                            + TAB + unit.token + TAB, docOS, encoding);

                }
                else {
                    IOUtils.write(unitsLineNumber.get(unit) + TAB + unit.begin + "-" + unit.end
                            + TAB + unit.token + TAB, docOS, encoding);
                }
                for (String type : featurePerLayer.keySet()) {
                    List<List<String>> annos = annotationsPerPostion
                            .getOrDefault(type, new HashMap<>())
                            .getOrDefault(unit, new ArrayList<>());
                    List<String> merged = null;
                    for (List<String> annofs : annos) {
                        if (merged == null) {
                            merged = annofs;
                        }
                        else {

                            for (int i = 0; i < annofs.size(); i++) {
                                merged.set(i, merged.get(i) + "|" + annofs.get(i));
                            }
                        }
                    }
                    if (merged != null) {
                        for (String anno : merged) {
                            IOUtils.write(anno + TAB, docOS, encoding);
                        }
                    } // No annotation of this type in this layer
                    else {
                        // if type do not have a feature,
                        if (featurePerLayer.get(type).size() == 0) {
                            IOUtils.write("_" + TAB, docOS, encoding);
                        }
                        else {
                            for (String feature : featurePerLayer.get(type)) {
                                IOUtils.write("_" + TAB, docOS, encoding);
                            }
                        }
                    }
                }
                IOUtils.write(LF, docOS, encoding);
            }
        }
        catch (Exception e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    private void setSlotLinkTypes()
    {
        int i = 0;
        for (String f : slotFeatures) {
            slotLinkTypes.put(f, linkTypes.get(i));
            i++;
        }
    }

    private void setLinkMaps(JCas aJCas)
    {
        for (String l : spanLayers) {
            if (l.equals(Token.class.getName())) {
                continue;
            }
            Type type = getType(aJCas.getCas(), l);
            layerMaps.put(type, layerMaps.size() + 1);
        }
        for (String l : chainLayers) {
            Type type = getType(aJCas.getCas(), l + LINK);
            layerMaps.put(type, layerMaps.size() + 1);
        }
        for (String l : relationLayers) {
            Type type = getType(aJCas.getCas(), l);
            layerMaps.put(type, layerMaps.size() + 1);
        }
    }

    /**
     * Write headers, in the sequence <br>
     * Type TAB List(Features sep by TAB)
     */
    private void writeHeader(OutputStream docOS)
        throws IOException
    {
        IOUtils.write("#FORMAT=WebAnno TSV 3.2" + LF, docOS, encoding);
        for (String type : featurePerLayer.keySet()) {
            String annoType;
            if (spanLayers.contains(type)) {
                annoType = SP;
            }
            else if (relationLayers.contains(type)) {
                annoType = RL;
            }
            else {
                annoType = CH;
            }
            IOUtils.write("#" + annoType + "=" + type + "|", docOS, encoding);
            StringBuilder fsb = new StringBuilder();
            for (String feature : featurePerLayer.get(type)) {
                if (fsb.length() < 1) {
                    fsb.append(feature);
                }
                else {
                    fsb.append("|").append(feature);
                }
            }
            IOUtils.write(fsb.toString() + LF, docOS, encoding);
        }
        IOUtils.write(LF, docOS, encoding);
    }

    private void setAmbiguity(JCas aJCas)
    {
        List<String> spanAndTokenLayers = spanLayers;
        spanAndTokenLayers.add(Token.class.getName());
        for (String l : spanAndTokenLayers) {
            Type type = getType(aJCas.getCas(), l);
            ambigUnits.putIfAbsent(type.getName(), new HashMap<>());
            for (AnnotationFS fs : CasUtil.select(aJCas.getCas(), type)) {
                AnnotationUnit unit = getFirstUnit(fs);
                // multiple token anno
                if (isMultipleTokenAnnotation(fs.getBegin(), fs.getEnd())) {
                    SubTokenAnno sta = new SubTokenAnno();
                    sta.setBegin(fs.getBegin());
                    sta.setEnd(fs.getEnd());
                    sta.setText(fs.getCoveredText());
                    Set<AnnotationUnit> sus = new LinkedHashSet<>();
                    for (AnnotationUnit newUnit : getSubUnits(sta, sus)) {
                        ambigUnits.get(type.getName()).put(newUnit, true);
                    }
                }
                // stacked anno
                else if (ambigUnits.get(type.getName()).get(unit) != null) {
                    ambigUnits.get(type.getName()).put(unit, true);
                }
                // single or first occurrence of stacked anno
                else {
                    ambigUnits.get(type.getName()).put(unit, false);
                }
            }

        }
    }

    private void setSpanAnnotation(JCas aJCas)
    {
        int i = 0;
        // store slot targets for each slot features
        for (String l : spanLayers) {
            Type type = getType(aJCas.getCas(), l);
            List<Feature> features = type.getFeatures();
            Collections.sort(features, (a, b) -> 
                    StringUtils.compare(a.getShortName(), b.getShortName()));
            for (Feature f : features) {
                if (slotFeatures != null && slotFeatures.contains(f.getName())) {
                    slotFeatureTypes.put(f, getType(aJCas.getCas(), slotTargets.get(i)));
                    i++;
                }
            }
        }

        for (String l : spanLayers) {
            if (l.equals(Token.class.getName())) {
                continue;
            }
            Map<AnnotationUnit, List<List<String>>> annotationsPertype;
            if (annotationsPerPostion.get(l) == null) {
                annotationsPertype = new HashMap<>();

            }
            else {
                annotationsPertype = annotationsPerPostion.get(l);
            }
            Type type = getType(aJCas.getCas(), l);
            for (AnnotationFS fs : CasUtil.select(aJCas.getCas(), type)) {
                AnnotationUnit unit = new AnnotationUnit(fs.getBegin(), fs.getEnd(), false,
                        fs.getCoveredText());
                // annotation is per Token
                if (units.contains(unit)) {
                    setSpanAnnoPerFeature(annotationsPertype, type, fs, unit, false, false);
                }
                // Annotation is on sub-token or multiple tokens
                else {
                    SubTokenAnno sta = new SubTokenAnno();
                    sta.setBegin(fs.getBegin());
                    sta.setEnd(fs.getEnd());
                    sta.setText(fs.getCoveredText());
                    boolean isMultiToken = isMultiToken(fs);
                    boolean isFirst = true;
                    Set<AnnotationUnit> sus = new LinkedHashSet<>();
                    for (AnnotationUnit newUnit : getSubUnits(sta, sus)) {
                        setSpanAnnoPerFeature(annotationsPertype, type, fs, newUnit, isMultiToken,
                                isFirst);
                        isFirst = false;
                    }
                }
            }
            if (annotationsPertype.keySet().size() > 0) {
                annotationsPerPostion.put(l, annotationsPertype);
            }
        }
    }

    private void setChainAnnotation(JCas aJCas)
    {
        for (String l : chainLayers) {
            if (l.equals(Token.class.getName())) {
                continue;
            }

            Map<AnnotationUnit, List<List<String>>> annotationsPertype = null;
            Type type = getType(aJCas.getCas(), l + CHAIN);
            Feature chainFirst = type.getFeatureByBaseName(FIRST);
            int chainNo = 1;
            for (FeatureStructure chainFs : selectFS(aJCas.getCas(), type)) {
                AnnotationFS linkFs = (AnnotationFS) chainFs.getFeatureValue(chainFirst);
                AnnotationUnit unit = getUnit(linkFs.getBegin(), linkFs.getEnd(),
                        linkFs.getCoveredText());
                Type lType = linkFs.getType();

                // this is the layer with annotations
                l = lType.getName();
                if (annotationsPerPostion.get(l) == null) {
                    annotationsPertype = new HashMap<>();

                }
                else {
                    annotationsPertype = annotationsPerPostion.get(l);
                }
                Feature linkNext = linkFs.getType().getFeatureByBaseName(NEXT);
                int linkNo = 1;
                while (linkFs != null) {
                    AnnotationFS nextLinkFs = (AnnotationFS) linkFs.getFeatureValue(linkNext);
                    if (nextLinkFs != null) {
                        addChinFeatureAnno(annotationsPertype, lType, linkFs, unit, linkNo,
                                chainNo);
                    }
                    else {
                        addChinFeatureAnno(annotationsPertype, lType, linkFs, unit, linkNo,
                                chainNo);
                    }
                    linkFs = nextLinkFs;
                    linkNo++;
                    if (nextLinkFs != null) {
                        unit = getUnit(linkFs.getBegin(), linkFs.getEnd(), linkFs.getCoveredText());
                    }
                }
                if (annotationsPertype.keySet().size() > 0) {
                    annotationsPerPostion.put(l, annotationsPertype);
                }
                chainNo++;
            }
        }
    }

    private void setRelationAnnotation(JCas aJCas)
    {
        for (String l : relationLayers) {
            if (l.equals(Token.class.getName())) {
                continue;
            }
            Map<AnnotationUnit, List<List<String>>> annotationsPertype;
            if (annotationsPerPostion.get(l) == null) {
                annotationsPertype = new HashMap<>();

            }
            else {
                annotationsPertype = annotationsPerPostion.get(l);
            }
            Type type = getType(aJCas.getCas(), l);
            Feature dependentFeature = null;
            Feature governorFeature = null;

            List<Feature> features = type.getFeatures();
            Collections.sort(features, (a, b) -> 
                    StringUtils.compare(a.getShortName(), b.getShortName()));
            for (Feature feature : features) {
                if (feature.getShortName().equals(DEPENDENT)) {

                    // check if the dependent is
                    dependentFeature = feature;
                }
                if (feature.getShortName().equals(GOVERNOR)) {
                    governorFeature = feature;
                }
            }
            for (AnnotationFS fs : CasUtil.select(aJCas.getCas(), type)) {
                AnnotationFS depFs = (AnnotationFS) fs.getFeatureValue(dependentFeature);
                AnnotationFS govFs = (AnnotationFS) fs.getFeatureValue(governorFeature);

                Type govType = govFs.getType();

                AnnotationUnit govUnit = getFirstUnit(
                        getUnit(govFs.getBegin(), govFs.getEnd(), govFs.getCoveredText()));
                if (ambigUnits.get(govType.getName()).get(govUnit) == null) {
                    govUnit = getUnit(govFs.getBegin(), govFs.getEnd(), govFs.getCoveredText());
                }

                AnnotationUnit depUnit = getFirstUnit(
                        getUnit(depFs.getBegin(), depFs.getEnd(), depFs.getCoveredText()));
                if (ambigUnits.get(govType.getName()).get(depUnit) == null) {
                    depUnit = getUnit(depFs.getBegin(), depFs.getEnd(), depFs.getCoveredText());
                }
                // Since de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency is over
                // Over POS anno which itself attached to Token, we need the POS type here

                if (type.getName().equals(Dependency.class.getName())) {
                    govType = aJCas.getCas().getTypeSystem().getType(POS.class.getName());
                }

                int govRef = 0;
                int depRef = 0;

                // For that unit test case only, where annotations are on Tokens.
                // The WebAnno world do not ever process Token as an annotation
                if (!govType.getName().equals(Token.class.getName())
                        && ambigUnits.get(govType.getName()).get(govUnit).equals(true)) {
                    govRef = annotaionRefPerType.get(govType).get(govFs);
                }

                if (!govType.getName().equals(Token.class.getName())
                        && ambigUnits.get(govType.getName()).get(depUnit).equals(true)) {
                    depRef = annotaionRefPerType.get(govType).get(depFs);
                }

                setRelationAnnoPerFeature(annotationsPertype, type, fs, depUnit, govUnit, govRef,
                        depRef, govType);

            }
            if (annotationsPertype.keySet().size() > 0) {
                annotationsPerPostion.put(l, annotationsPertype);
            }
        }
    }

    private boolean isMultiToken(AnnotationFS aFs)
    {

        for (AnnotationUnit unit : units) {
            if (unit.begin <= aFs.getBegin() && unit.end > aFs.getBegin()
                    && unit.end < aFs.getEnd()) {
                return true;
            }
        }
        return false;
    }

    private AnnotationUnit getUnit(int aBegin, int aEnd, String aText)
    {
        for (AnnotationUnit unit : units) {
            if (unit.begin == aBegin && unit.end == aEnd) {
                return unit;
            }
        }
        return new AnnotationUnit(aBegin, aEnd, false, aText);
    }

    private Set<AnnotationUnit> getSubUnits(SubTokenAnno aSTA, Set<AnnotationUnit> aSubUnits)
    {
        AnnotationUnit prevUnit = null;
        List<AnnotationUnit> tmpUnits = new ArrayList<>(units);
        if (aSTA.getBegin() == aSTA.getEnd()) {

            AnnotationUnit newUnit = new AnnotationUnit(aSTA.getBegin(), aSTA.getEnd(), false, "");
            for (AnnotationUnit unit : units) {
                if (unit.begin >= newUnit.begin && unit.end >= newUnit.end) {
                    updateUnitLists(tmpUnits, unit, newUnit);
                    aSubUnits.add(newUnit);
                    units = new ArrayList<>(tmpUnits);
                    return aSubUnits;
                }
            }
        }

        for (AnnotationUnit unit : units) {
            if (unit.end > aSTA.end) {
                if (unit.begin == aSTA.begin) {
                    AnnotationUnit newUnit = new AnnotationUnit(aSTA.getBegin(), aSTA.getEnd(),
                            false, aSTA.getText());
                    updateUnitLists(tmpUnits, unit, newUnit);

                    aSubUnits.add(newUnit);
                }
                break;
            }
            // this is a sub-token annotation
            if (unit.begin <= aSTA.getBegin() && aSTA.getBegin() <= unit.end
                    && aSTA.getEnd() <= unit.end) {
                AnnotationUnit newUnit = new AnnotationUnit(aSTA.getBegin(), aSTA.getEnd(), false,
                        aSTA.getText());

                updateUnitLists(tmpUnits, unit, newUnit);

                aSubUnits.add(newUnit);
            }
            // if sub-token annotation crosses multiple tokens
            else if ((unit.begin <= aSTA.getBegin() && aSTA.getBegin() < unit.end
                    && aSTA.getEnd() > unit.end)) {

                int thisSubTextLen = unit.end - aSTA.begin;

                AnnotationUnit newUnit = new AnnotationUnit(aSTA.getBegin(), unit.end, false,
                        aSTA.getText().substring(0, thisSubTextLen));
                aSubUnits.add(newUnit);

                updateUnitLists(tmpUnits, unit, newUnit);

                aSTA.setBegin(getNextUnitBegin(aSTA.getBegin()));

                aSTA.setText(aSTA.getText().trim().substring(thisSubTextLen));
                getSubUnits(aSTA, aSubUnits);
            }
            // empty annotation between tokens
            else if (aSTA.getBegin() <= unit.begin && prevUnit != null
                    && prevUnit.end < unit.begin) {
                int thisSubTextLen = unit.begin - aSTA.begin;

                AnnotationUnit newUnit = new AnnotationUnit(aSTA.getBegin(), unit.begin, false,
                        aSTA.getText().substring(0, thisSubTextLen));
                aSubUnits.add(newUnit);

                updateUnitLists(tmpUnits, prevUnit, newUnit);

                aSTA.setBegin(unit.begin);

                aSTA.setText(aSTA.getText().trim().substring(thisSubTextLen));
                getSubUnits(aSTA, aSubUnits);
            }
            else {
                prevUnit = unit;
            }
        }
        units = new ArrayList<>(tmpUnits);
        return aSubUnits;
    }

    private int getNextUnitBegin(int aSTABegin)
    {
        for (AnnotationUnit unit : units) {
            if (unit.begin > aSTABegin && !unit.isSubtoken) {
                return unit.begin;
            }
        }
        // this is the last token
        return aSTABegin;
    }

    /**
     * If there is at least one non-sub-token annotation whose begin is larger than this one, it is
     * a multiple tokens (or crossing multiple tokens) annotation.
     */
    private boolean isMultipleTokenAnnotation(int aBegin, int aEnd)
    {
        for (AnnotationUnit unit : units) {
            if (unit.begin > aBegin && unit.begin < aEnd && !unit.isSubtoken) {
                return true;
            }
        }
        // this is the last token
        return false;
    }

    private void updateUnitLists(List<AnnotationUnit> tmpUnits, AnnotationUnit unit,
            AnnotationUnit newUnit)
    {
        if (!tmpUnits.contains(newUnit)) {
            newUnit.isSubtoken = true;
            // is this sub-token already there
            if (!tmpUnits.contains(newUnit)) {
                tmpUnits.add(tmpUnits.indexOf(unit) + 1, newUnit);
                subUnits.put(unit, subUnits.getOrDefault(unit, 0) + 1);
                unitsLineNumber.put(newUnit, unitsLineNumber.get(unit) + "." + subUnits.get(unit));
            }
        }
    }

    private void setSpanAnnoPerFeature(Map<AnnotationUnit, List<List<String>>> aAnnotationsPertype,
            Type aType, AnnotationFS aFs, AnnotationUnit aUnit, boolean aIsMultiToken,
            boolean aIsFirst)
    {
        List<String> annoPerFeatures = new ArrayList<>();
        featurePerLayer.putIfAbsent(aType.getName(), new LinkedHashSet<>());
        int ref = getRefId(aType, aFs, aUnit);

        if (ambigUnits.get(aType.getName()).get(getFirstUnit(aUnit)) != null
                && ambigUnits.get(aType.getName()).get(getFirstUnit(aUnit)).equals(false)) {
            ref = 0;
        }

        if (ambigUnits.get(aType.getName()).get(getFirstUnit(aUnit)) == null
                && ambigUnits.get(aType.getName()).get(aUnit).equals(false)) {
            ref = 0;
        }
        List<Feature> features = aType.getFeatures();
        Collections.sort(features, (a, b) -> 
                StringUtils.compare(a.getShortName(), b.getShortName()));
        for (Feature feature : features) {
            if (feature.getName().equals(CAS.FEATURE_FULL_NAME_SOFA)
                    || feature.getName().equals(CAS.FEATURE_FULL_NAME_BEGIN)
                    || feature.getName().equals(CAS.FEATURE_FULL_NAME_END)
                    || feature.getShortName().equals(GOVERNOR)
                    || feature.getShortName().equals(DEPENDENT)
                    || feature.getShortName().equals(FIRST)
                    || feature.getShortName().equals(NEXT)) {
                continue;
            }

            // if slot feature
            if (slotFeatures != null && slotFeatures.contains(feature.getName())) {
                if (aFs.getFeatureValue(feature) != null) {
                    ArrayFS array = (ArrayFS) aFs.getFeatureValue(feature);
                    StringBuilder sbRole = new StringBuilder();
                    StringBuilder sbTarget = new StringBuilder();
                    for (FeatureStructure linkFS : array.toArray()) {
                        String role = linkFS
                                .getStringValue(linkFS.getType().getFeatureByBaseName("role"));
                        AnnotationFS targetFs = (AnnotationFS) linkFS
                                .getFeatureValue(linkFS.getType().getFeatureByBaseName("target"));
                        Type tType = targetFs.getType();

                        AnnotationUnit firstUnit = getFirstUnit(targetFs);
                        ref = getRefId(tType, targetFs, firstUnit);
                        // Check if the target is ambiguous or not
                        if (ambigUnits.get(tType.getName()).get(firstUnit).equals(false)) {
                            ref = 0;
                        }
                        if (role == null) {
                            role = "*";
                        }
                        else {
                            // Escape special character
                            role = replaceEscapeChars(role);
                        }
                        if (sbRole.length() < 1) {
                            sbRole.append(role);
                            // record the actual target type column number if slot target is
                            // uima.tcas.Annotation
                            int targetTypeNumber = 0;
                            if (slotFeatureTypes.get(feature).getName()
                                    .equals(CAS.TYPE_NAME_ANNOTATION)) {
                                targetTypeNumber = layerMaps.get(tType);
                            }
                            sbTarget.append(unitsLineNumber.get(firstUnit))
                                    .append(targetTypeNumber == 0 ? "" : "-" + targetTypeNumber)
                                    .append(ref > 0 ? "[" + ref + "]" : "");
                        }
                        else {
                            sbRole.append(";");
                            sbTarget.append(";");
                            sbRole.append(role);
                            int targetTypeNumber = 0;
                            if (slotFeatureTypes.get(feature).getName()
                                    .equals(CAS.TYPE_NAME_ANNOTATION)) {
                                targetTypeNumber = layerMaps.get(tType);
                            }
                            sbTarget.append(unitsLineNumber.get(firstUnit))
                                    .append(targetTypeNumber == 0 ? "" : "-" + targetTypeNumber)
                                    .append(ref > 0 ? "[" + ref + "]" : "");
                        }
                    }
                    annoPerFeatures.add(sbRole.toString().isEmpty() ? "_" : sbRole.toString());
                    annoPerFeatures.add(sbTarget.toString().isEmpty() ? "_" : sbTarget.toString());
                }
                else {
                    // setting it to null
                    annoPerFeatures.add("_");
                    annoPerFeatures.add("_");
                }
                featurePerLayer.get(aType.getName())
                        .add(ROLE + feature.getName() + "_" + slotLinkTypes.get(feature.getName()));
                featurePerLayer.get(aType.getName()).add(slotFeatureTypes.get(feature).getName());
            }
            else {
                String annotation = aFs.getFeatureValueAsString(feature);
                if (annotation == null) {
                    annotation = "*";
                }
                else {
                    // Escape special character
                    annotation = replaceEscapeChars(annotation);
                }
                annotation = annotation + (ref > 0 ? "[" + ref + "]" : "");
                // only add BIO markers to multiple annotations
                setAnnoFeature(aIsMultiToken, aIsFirst, annoPerFeatures, annotation);

                featurePerLayer.get(aType.getName()).add(feature.getShortName());
            }
        }
        aAnnotationsPertype.putIfAbsent(aUnit, new ArrayList<>());
        // If the layer do not have a feature at all, add dummy * as a place holder
        if (annoPerFeatures.size() == 0) {
            setAnnoFeature(aIsMultiToken, aIsFirst, annoPerFeatures,
                    "*" + (ref > 0 ? "[" + ref + "]" : ""));
        }
        aAnnotationsPertype.get(aUnit).add(annoPerFeatures);
    }

    /**
     * 
     * @param aAnnotationsPertype
     *            store annotations per type associated with the annotation units
     * @param aType
     *            the coreference annotation type
     * @param aFs
     *            the feature structure
     * @param aUnit
     *            the current annotation unit of the coreference chain
     * @param aLinkNo
     *            a reference to the link in a chain, starting at one for the first link and n for
     *            the last link in the chain
     * @param achainNo
     *            a reference to the chain, starting at 1 for the first chain and n for the last
     *            chain where n is the number of coreference chains the document
     */

    private void addChinFeatureAnno(Map<AnnotationUnit, List<List<String>>> aAnnotationsPertype,
            Type aType, AnnotationFS aFs, AnnotationUnit aUnit, int aLinkNo, int achainNo)
    {
        featurePerLayer.putIfAbsent(aType.getName(), new LinkedHashSet<>());
        // StringBuffer sbAnnotation = new StringBuffer();
        // annotation is per Token
        if (units.contains(aUnit)) {
            setChainAnnoPerFeature(aAnnotationsPertype, aType, aFs, aUnit, aLinkNo, achainNo, false,
                    false);
        }
        // Annotation is on sub-token or multiple tokens
        else {
            SubTokenAnno sta = new SubTokenAnno();
            sta.setBegin(aFs.getBegin());
            sta.setEnd(aFs.getEnd());
            sta.setText(aFs.getCoveredText());
            boolean isMultiToken = isMultiToken(aFs);
            boolean isFirst = true;
            Set<AnnotationUnit> sus = new LinkedHashSet<>();
            for (AnnotationUnit newUnit : getSubUnits(sta, sus)) {
                setChainAnnoPerFeature(aAnnotationsPertype, aType, aFs, newUnit, aLinkNo, achainNo,
                        isMultiToken, isFirst);
                isFirst = false;
            }
        }
    }

    private void setChainAnnoPerFeature(Map<AnnotationUnit, List<List<String>>> aAnnotationsPertype,
            Type aType, AnnotationFS aFs, AnnotationUnit aUnit, int aLinkNo, int achainNo,
            boolean aMultiUnit, boolean aFirst)
    {
        List<String> annoPerFeatures = new ArrayList<>();
        List<Feature> features = aType.getFeatures();
        Collections.sort(features, (a, b) -> 
                StringUtils.compare(a.getShortName(), b.getShortName()));
        for (Feature feature : features) {
            if (feature.getName().equals(CAS.FEATURE_FULL_NAME_SOFA)
                    || feature.getName().equals(CAS.FEATURE_FULL_NAME_BEGIN)
                    || feature.getName().equals(CAS.FEATURE_FULL_NAME_END)
                    || feature.getShortName().equals(GOVERNOR)
                    || feature.getShortName().equals(DEPENDENT)
                    || feature.getShortName().equals(FIRST)
                    || feature.getShortName().equals(NEXT)) {
                continue;
            }
            String annotation = aFs.getFeatureValueAsString(feature);

            if (annotation == null) {
                annotation = "*";
            }
            else {
                annotation = replaceEscapeChars(annotation);
            }

            if (feature.getShortName().equals(REF_REL)) {
                annotation = annotation + "->" + achainNo + "-" + aLinkNo;
            }
            else if (aMultiUnit) {
                annotation = annotation + "[" + achainNo + "]";
            }
            else {
                annotation = annotation + "[" + achainNo + "]";
            }
            featurePerLayer.get(aType.getName()).add(feature.getShortName());

            annoPerFeatures.add(annotation);
        }
        aAnnotationsPertype.putIfAbsent(aUnit, new ArrayList<>());
        ambigUnits.putIfAbsent(aType.getName(), new HashMap<>());
        ambigUnits.get(aType.getName()).put(aUnit, true); // coref are always ambig

        if (annoPerFeatures.size() == 0) {
            annoPerFeatures.add("*" + "[" + achainNo + "]");
        }
        aAnnotationsPertype.get(aUnit).add(annoPerFeatures);
    }

    private void setRelationAnnoPerFeature(
            Map<AnnotationUnit, List<List<String>>> annotationsPertype, Type type, AnnotationFS fs,
            AnnotationUnit depUnit, AnnotationUnit govUnit, int aGovRef, int aDepRef, Type aDepType)
    {
        List<String> annoPerFeatures = new ArrayList<>();
        featurePerLayer.putIfAbsent(type.getName(), new LinkedHashSet<>());
        List<Feature> features = type.getFeatures();
        Collections.sort(features, (a, b) -> 
                StringUtils.compare(a.getShortName(), b.getShortName()));
        for (Feature feature : features) {
            if (feature.getName().equals(CAS.FEATURE_FULL_NAME_SOFA)
                    || feature.getName().equals(CAS.FEATURE_FULL_NAME_BEGIN)
                    || feature.getName().equals(CAS.FEATURE_FULL_NAME_END)
                    || feature.getShortName().equals(GOVERNOR)
                    || feature.getShortName().equals(DEPENDENT)
                    || feature.getShortName().equals(FIRST)
                    || feature.getShortName().equals(NEXT)) {
                continue;
            }
            int ref = getRefId(type, fs, depUnit);
            String annotation = fs.getFeatureValueAsString(feature);
            if (annotation == null) {
                annotation = "*";
            }
            else {
                annotation = replaceEscapeChars(annotation);
            }
            annoPerFeatures.add(annotation);// +(ref > 0 ? "[" + ref + "]" : ""));
            featurePerLayer.get(type.getName()).add(feature.getShortName());
        }
        // add the governor and dependent unit addresses (separated by _
        String govRef = unitsLineNumber.get(govUnit)
                + ((aDepRef > 0 || aGovRef > 0) ? "[" + aGovRef + "_" + aDepRef + "]" : "");
        annoPerFeatures.add(govRef);
        featurePerLayer.get(type.getName()).add(BT + aDepType.getName());
        // the column for the dependent unit address
        annotationsPertype.putIfAbsent(depUnit, new ArrayList<>());
        if (annoPerFeatures.size() == 0) {
            annoPerFeatures.add("*");
        }
        annotationsPertype.get(depUnit).add(annoPerFeatures);
    }

    public static String replaceEscapeChars(String annotation)
    {
        return StringUtils.replaceEach(annotation,
                new String[] { "\\", "[", "]", "|", "_", "->", ";", "\t", "\n", "*" },
                new String[] { "\\\\", "\\[", "\\]", "\\|", "\\_", "\\->", "\\;", "\\t", "\\n",
                        "\\*" });
    }

    private void setAnnoFeature(boolean aIsMultiToken, boolean aIsFirst,
            List<String> aAnnoPerFeatures, String annotation)
    {
        if (aIsMultiToken) {
            if (aIsFirst) {
                aAnnoPerFeatures.add(annotation);
            }
            else {
                aAnnoPerFeatures.add(annotation);
            }
        }
        else {
            aAnnoPerFeatures.add(annotation);
        }
    }

    private AnnotationUnit getFirstUnit(AnnotationFS targetFs)
    {
        SubTokenAnno sta = new SubTokenAnno();
        sta.setBegin(targetFs.getBegin());
        sta.setEnd(targetFs.getEnd());
        sta.setText(targetFs.getCoveredText());
        Set<AnnotationUnit> sus = new LinkedHashSet<>();
        AnnotationUnit firstUnit = null;
        for (AnnotationUnit u : getSubUnits(sta, sus)) {
            firstUnit = u;
            break;
        }
        return firstUnit;
    }

    // for relation annotation drawn on multiple span annotation, we put the info only to the first
    // unit
    private AnnotationUnit getFirstUnit(AnnotationUnit aUnit)
    {
        SubTokenAnno sta = new SubTokenAnno();
        sta.setBegin(aUnit.begin);
        sta.setEnd(aUnit.end);
        sta.setText(aUnit.token);
        Set<AnnotationUnit> sus = new LinkedHashSet<>();
        AnnotationUnit firstUnit = null;
        for (AnnotationUnit u : getSubUnits(sta, sus)) {
            firstUnit = u;
            break;
        }
        return firstUnit;
    }

    /**
     * Annotations of same type those: <br>
     * 1) crosses multiple sentences AND <br>
     * 2) repeated on the same unit (even if different value) <br>
     * Will be referenced by a number so that re-importing or processing outside WebAnno can be
     * easily distinguish same sets of annotations. This is much Meaningful for relation/slot and
     * chain annotations. Reference numbers are incremental
     * 
     * @param type
     *            The annotation type
     * @param fs
     *            the annotation
     * @param unit
     *            the annotation element (Token or sub-tokens)
     * @return the reference number to be attached on this annotation value
     */
    private int getRefId(Type type, AnnotationFS fs, AnnotationUnit unit)
    {

        // first time
        if (annotaionRefPerType.get(type) == null) {

            Map<FeatureStructure, Integer> annoRefs = new HashMap<>();
            annoRefs.put(fs, 1);
            annotaionRefPerType.put(type, annoRefs);

            multiAnnosPerUnit.putIfAbsent(type, new HashMap<>());
            Map<FeatureStructure, Integer> multiAnooRefs = new HashMap<>();
            multiAnooRefs.put(fs, 1);
            multiAnnosPerUnit.get(type).put(unit, multiAnooRefs);
            return 1;
        }
        else {

            // This is a multiple token annotation, re-USE reference id
            if (annotaionRefPerType.get(type).get(fs) != null) {
                return annotaionRefPerType.get(type).get(fs);
            }

            Map<FeatureStructure, Integer> annoRefs = annotaionRefPerType.get(type);
            int max = Collections.max(annoRefs.values()); // the last reference number so far.
            annoRefs.put(fs, max + 1);
            annotaionRefPerType.put(type, annoRefs);

            /*
             * Map<Integer, FeatureStructure> refsAnnos = refAnnotaionperType.get(type);
             * refsAnnos.put(max + 1, fs); refAnnotaionperType.put(type, refsAnnos);
             */

            int ref = annotaionRefPerType.get(type).get(fs);
            Map<FeatureStructure, Integer> multiAnooRefs = multiAnnosPerUnit.get(type).get(unit);
            if (multiAnooRefs == null) {
                multiAnooRefs = new HashMap<>();
                multiAnooRefs.put(fs, ref);
                multiAnnosPerUnit.get(type).put(unit, multiAnooRefs);
                return ref;
            }
            // this is for sure a stacked annotation
            else {
                multiAnooRefs.put(fs, ref);
                multiAnnosPerUnit.get(type).put(unit, multiAnooRefs);
                return ref;
            }
        }
    }

    private void setTokenSentenceAddress(JCas aJCas)
    {
        int sentNMumber = 1;
        for (Sentence sentence : select(aJCas, Sentence.class)) {
            int lineNumber = 1;
            for (Token token : selectCovered(Token.class, sentence)) {
                AnnotationUnit unit = new AnnotationUnit(token.getBegin(), token.getEnd(), false,
                        token.getCoveredText());
                units.add(unit);
                if (lineNumber == 1) {
                    sentenceUnits.put(unit, sentence.getCoveredText());
                }
                unitsLineNumber.put(unit, sentNMumber + "-" + lineNumber);
                lineNumber++;
            }
            sentNMumber++;
        }
    }

    private void resetVariables() {
        units.clear();
        subUnits.clear();
        featurePerLayer.clear();
        unitsLineNumber.clear();
        sentenceUnits.clear();
        annotationsPerPostion.clear();
        slotFeatureTypes.clear();
        annotaionRefPerType.clear();
        ambigUnits.clear();
        multiAnnosPerUnit.clear();
        slotLinkTypes.clear();
        layerMaps.clear();
    }

    private String escapeSpecial(String aText) {
        List<String> pat = new ArrayList<>();
        List<String> esc = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            if (i > 7 && i < 14) {
                continue;
            }
            pat.add(Character.toString((char) i));
            esc.add("\\" + Character.toString((char) i));
        }
        // with a readable Java escape sequence
        // TAB
        pat.add("\t");
        esc.add("\\t");
        // linefeed
        pat.add("\n");
        esc.add("\\n");
        // formfeed
        pat.add("\f");
        esc.add("\\f");
        // carriage return
        pat.add("\r");
        esc.add("\\r");
        // backspace
        pat.add("\b");
        esc.add("\\b");
        // backslash
        pat.add("\\");
        esc.add("\\\\");

        return StringUtils.replaceEach(aText, 
                pat.toArray(new String[pat.size()]), esc.toArray(new String[esc.size()]));
    }

    class SubTokenAnno
    {
        int begin;
        int end;
        String text;

        public int getBegin()
        {
            return begin;
        }

        public int getEnd()
        {
            return end;
        }

        public void setEnd(int end)
        {
            this.end = end;
        }

        public void setBegin(int begin)
        {
            this.begin = begin;
        }

        public String getText()
        {
            return text;
        }

        public void setText(String text)
        {
            this.text = text;
        }

    }
}

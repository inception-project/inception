/*
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
 */
package de.tudarmstadt.ukp.clarin.webanno.tsv;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.lang3.StringEscapeUtils.unescapeJava;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.tsv.util.AnnotationUnit;
import de.tudarmstadt.ukp.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Stem;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * This class reads a WebAnno compatible TSV files and create annotations from the information
 * provided. The the header of the file records the existing annotation layers with their features
 * names.<br>
 * If the annotation type or a feature in the type do not exist in the CAS, it throws an error.<br>
 * Span types starts with the prefix <b> #T_SP=</b>. <br>
 * Relation types starts with the prefix <b> #T_RL=</b>. <br>
 * Chain types starts with the prefix <b> #T_CH=</b>. <br>
 * Slot features start with prefix <b> ROLE_</b>. <br>
 * All features of a type follows the the name separated by <b>|</b> character. <br>
 */
public class WebannoTsv3Reader
    extends JCasResourceCollectionReader_ImplBase
{

    private static final String TAB = "\t";
    private static final String LF = "\n";
    private static final String REF_REL = "referenceRelation";
    private static final String REF_LINK = "referenceType";
    private static final String CHAIN = "Chain";
    private static final String FIRST = "first";
    private static final String NEXT = "next";
    public static final String ROLE = "ROLE_";
    public static final String BT = "BT_"; // base type for the relation
                                           // annotation
    private static final String DEPENDENT = "Dependent";
    private static final String GOVERNOR = "Governor";

    private String fileName;
    private int columns = 2;// token number + token columns (minimum required)
    private Map<Type, Set<Feature>> allLayers = new LinkedHashMap<>();
    private Map<Feature, Type> roleLinks = new HashMap<>();
    private Map<Feature, Type> roleTargets = new HashMap<>();
    private Map<Feature, Type> slotLinkTypes = new HashMap<>();
    private StringBuilder coveredText = new StringBuilder();
    // for each type, for each unit, annotations per position
    private Map<Type, Map<AnnotationUnit, List<String>>> annotationsPerPostion = 
            new LinkedHashMap<>();

    // For multiple span annotations and stacked annotations
    private Map<Type, Map<Integer, String>> annotationsPerTyep = new LinkedHashMap<>();

    private Map<Type, Map<Integer, Map<Integer, AnnotationFS>>> chainAnnosPerTyep = new HashMap<>();
    private List<AnnotationUnit> units = new ArrayList<>();
    private Map<String, AnnotationUnit> token2Units = new HashMap<>();
    private Map<AnnotationUnit, Token> units2Tokens = new HashMap<>();

    private Map<Integer, Type> layerMaps = new LinkedHashMap<>();
    private Map<Type, Map<AnnotationUnit, Map<Integer, AnnotationFS>>> annosPerRef = 
            new HashMap<>();
    private Map<Type, Feature> depFeatures = new HashMap<>();
    private Map<Type, Type> depTypess = new HashMap<>();

    // record the annotation at ref position when it is multiple token
    // annotation
    private Map<Type, Map<AnnotationUnit, Map<Integer, AnnotationFS>>> annoUnitperAnnoFs = 
            new HashMap<>();

    public void convertToCas(JCas aJCas, InputStream aIs, String aEncoding)
        throws IOException

    {
        DocumentMetaData documentMetadata = DocumentMetaData.get(aJCas);
        fileName = documentMetadata.getDocumentTitle();
        // setLayerAndFeature(aJCas, aIs, aEncoding);

        setAnnotations(aJCas, aIs, aEncoding);
        aJCas.setDocumentText(coveredText.toString());
    }

    /**
     * Iterate through lines and create span annotations accordingly. For multiple span annotation,
     * based on the position of the annotation in the line, update only the end position of the
     * annotation
     */
    private void setAnnotations(JCas aJCas, InputStream aIs, String aEncoding)
        throws IOException
    {

        // getting header information
        LineIterator lineIterator = IOUtils.lineIterator(aIs, aEncoding);
        int sentBegin = -1, sentEnd = 0;
        int prevSentEnd = 0;
        StringBuilder sentLineSb = new StringBuilder();
        String lastSent = "";
        int format = -1;
        while (lineIterator.hasNext()) {
            String line = lineIterator.next();
            if (line.startsWith("#T_")) {
                setLayerAndFeature(aJCas, line);
                continue;
            }

            if (line.startsWith("#Text=")) {
                String text = line.substring(line.indexOf("=") + 1);
                if (format == 31) {
                    text = unescapeJava(text);
                }
                else if (format == 32) {
                    text = unEscapeSpecial(text);
                }

                if (sentLineSb.toString().isEmpty()) {
                    sentLineSb.append(text);
                }
                else {
                    sentLineSb.append(LF).append(text);
                }
                lastSent = sentLineSb.toString();
                continue;
            }

            if (line.startsWith("#FORMAT=")) {
                if ("#FORMAT=WebAnno TSV 3".equals(line)) {
                    format = 3;
                }
                else if ("#FORMAT=WebAnno TSV 3.1".equals(line)) {
                    format = 31;
                }
                else if ("#FORMAT=WebAnno TSV 3.2".equals(line)) {
                    format = 32;
                }
                continue;
            }

            if (line.trim().isEmpty()) {
                if (!sentLineSb.toString().isEmpty()) {
                    createSentence(aJCas, sentLineSb.toString(), sentBegin, sentEnd, prevSentEnd);
                    prevSentEnd = sentEnd;
                    sentBegin = -1;// reset for next sentence begin
                    sentLineSb = new StringBuilder();
                }

                continue;
            }

            line = line.trim();
            int count = StringUtils.countMatches(line, "\t");

            if (columns != count) {
                throw new IOException(
                        fileName + " This is not a valid TSV File. check this line: " + line);
            }

            String regex = "(?<!\\\\)*" + Pattern.quote(TAB);
            String[] lines = line.split(regex);

            int begin = Integer.parseInt(lines[1].split("-")[0]);
            int end = Integer.parseInt(lines[1].split("-")[1]);
            if (sentBegin == -1) {
                sentBegin = begin;
            }
            sentEnd = end;

            AnnotationUnit unit = createTokens(aJCas, lines, begin, end);

            int ind = 3;

            setAnnosPerTypePerUnit(lines, unit, ind);
        }

        // the last sentence
        if (!lastSent.isEmpty()) {
            createSentence(aJCas, lastSent, sentBegin, sentEnd, prevSentEnd);
        }

        Map<Type, Map<AnnotationUnit, List<AnnotationFS>>> annosPerTypePerUnit = new HashMap<>();
        setAnnosPerUnit(aJCas, annosPerTypePerUnit);
        addAnnotations(aJCas, annosPerTypePerUnit);
        addChainAnnotations(aJCas);
    }

    /**
     * The individual link annotations are stored in a {@link TreeMap} (chainAnnosPerTye) with chain
     * number and link number references, sorted in an ascending order <br>
     * Iterate over each chain number and link number references and construct the chain.
     */
    private void addChainAnnotations(JCas aJCas)
    {
        for (Type linkType : chainAnnosPerTyep.keySet()) {
            for (int chainNo : chainAnnosPerTyep.get(linkType).keySet()) {

                Type chainType = aJCas.getCas().getTypeSystem().getType(
                        linkType.getName().substring(0, linkType.getName().length() - 4) + CHAIN);
                Feature firstF = chainType.getFeatureByBaseName(FIRST);
                Feature nextF = linkType.getFeatureByBaseName(NEXT);
                FeatureStructure chain = aJCas.getCas().createFS(chainType);

                aJCas.addFsToIndexes(chain);
                AnnotationFS firstFs = chainAnnosPerTyep.get(linkType).get(chainNo).get(1);
                AnnotationFS linkFs = firstFs;
                chain.setFeatureValue(firstF, firstFs);
                for (int i = 2; i <= chainAnnosPerTyep.get(linkType).get(chainNo).size(); i++) {
                    linkFs.setFeatureValue(nextF,
                            chainAnnosPerTyep.get(linkType).get(chainNo).get(i));
                    linkFs = chainAnnosPerTyep.get(linkType).get(chainNo).get(i);
                }
            }
        }
    }

    /**
     * Importing span annotations including slot annotations.
     */
    private void addAnnotations(JCas aJCas,
            Map<Type, Map<AnnotationUnit, List<AnnotationFS>>> aAnnosPerTypePerUnit)
    {
        for (Type type : annotationsPerPostion.keySet()) {
            Map<AnnotationUnit, Map<Integer, AnnotationFS>> multiTokUnits = new HashMap<>();
            int ref = 1;
            AnnotationFS prevAnnoFs = null; // to see if it is on multiple token
            for (AnnotationUnit unit : annotationsPerPostion.get(type).keySet()) {
                int end = unit.end;
                List<AnnotationFS> annos = aAnnosPerTypePerUnit.get(type).get(unit);
                int j = 0;
                Feature linkeF = null;
                Map<AnnotationFS, List<FeatureStructure>> linkFSesPerSlotAnno = new HashMap<>();

                if (allLayers.get(type).size() == 0) {
                    ref = addAnnotationWithNoFeature(aJCas, type, unit, annos, multiTokUnits, end,
                            ref);
                    continue;
                }

                for (Feature feat : allLayers.get(type)) {
                    String anno = annotationsPerPostion.get(type).get(unit).get(j);
                    if (!anno.equals("_")) {
                        int i = 0;
                        // if it is a slot annotation (multiple slots per
                        // single annotation
                        // (Target1<--role1--Base--role2-->Target2)
                        int slot = 0;
                        boolean targetAdd = false;
                        String stackedAnnoRegex = "(?<!\\\\)" + Pattern.quote("|");
                        String[] stackedAnnos = anno.split(stackedAnnoRegex);
                        for (String mAnnos : stackedAnnos) {
                            String multipleSlotAnno = "(?<!\\\\)" + Pattern.quote(";");
                            for (String mAnno : mAnnos.split(multipleSlotAnno)) {
                                String depRef = "";
                                String multSpliter = "(?<!\\\\)" + Pattern.quote("[");
                                // is this slot target ambiguous?
                                boolean ambigTarget = false;
                                if (mAnno.split(multSpliter).length > 1) {
                                    ambigTarget = true;
                                    depRef = mAnno.substring(mAnno.indexOf("[") + 1,
                                            mAnno.length() - 1);
                                    ref = depRef.contains("_") ? ref
                                            : Integer.valueOf(mAnno.substring(
                                                    mAnno.indexOf("[") + 1, mAnno.length() - 1));
                                    mAnno = mAnno.substring(0, mAnno.indexOf("["));
                                }
                                if (mAnno.equals("*")) {
                                    mAnno = null;
                                }
                                boolean isMultitoken = false;

                                if (!multiTokUnits.isEmpty() && prevAnnoFs != null
                                        && prevAnnoFs.getBegin() != unit.begin) {
                                    contAnno: for (AnnotationUnit u : multiTokUnits.keySet()) {
                                        for (Integer r : multiTokUnits.get(u).keySet()) {
                                            if (ref == r) {
                                                isMultitoken = true;
                                                prevAnnoFs = multiTokUnits.get(u).get(r);
                                                break contAnno;
                                            }
                                        }
                                    }
                                }
                                if (isMultitoken) {
                                    Feature endF = type
                                            .getFeatureByBaseName(CAS.FEATURE_BASE_NAME_END);
                                    
                                    prevAnnoFs.getCAS().removeFsFromIndexes(prevAnnoFs);
                                    prevAnnoFs.setIntValue(endF, end);
                                    prevAnnoFs.getCAS().addFsToIndexes(prevAnnoFs);
                                    
                                    mAnno = getEscapeChars(mAnno);
                                    prevAnnoFs.setFeatureValueFromString(feat, mAnno);
                                    if (feat.getShortName().equals(REF_LINK)) {
                                        // since REF_REL do not start with BIO,
                                        // update it it...
                                        annos.set(i, prevAnnoFs);
                                    }
                                    setAnnoRefPerUnit(unit, type, ref, prevAnnoFs);
                                }
                                else {
                                    if (roleLinks.containsKey(feat)) {
                                        linkeF = feat;
                                        FeatureStructure link = aJCas.getCas()
                                                .createFS(slotLinkTypes.get(feat));
                                        Feature roleFeat = link.getType()
                                                .getFeatureByBaseName("role");

                                        mAnno = getEscapeChars(mAnno);

                                        link.setStringValue(roleFeat, mAnno);
                                        linkFSesPerSlotAnno.putIfAbsent(annos.get(i),
                                                new ArrayList<>());
                                        linkFSesPerSlotAnno.get(annos.get(i)).add(link);

                                    }
                                    else if (roleTargets.containsKey(feat) && mAnno != null) {
                                        FeatureStructure link = linkFSesPerSlotAnno
                                                .get(annos.get(i)).get(slot);
                                        int customTypeNumber = 0;
                                        if (mAnno.split("-").length > 2) {
                                            customTypeNumber = Integer.valueOf(
                                                    mAnno.substring(mAnno.lastIndexOf("-") + 1));
                                            mAnno = mAnno.substring(0, mAnno.lastIndexOf("-"));
                                        }

                                        AnnotationUnit targetUnit = token2Units.get(mAnno);
                                        Type tType = null;
                                        if (customTypeNumber == 0) {
                                            tType = roleTargets.get(feat);
                                        }
                                        else {
                                            tType = layerMaps.get(customTypeNumber);
                                        }
                                        AnnotationFS targetFs;

                                        if (ambigTarget) {
                                            targetFs = annosPerRef.get(tType).get(targetUnit)
                                                    .get(ref);
                                        }
                                        else {
                                            targetFs = annosPerRef.get(tType).get(targetUnit)
                                                    .entrySet().iterator().next().getValue();
                                        }

                                        link.setFeatureValue(feat, targetFs);
                                        addSlotAnnotations(linkFSesPerSlotAnno, linkeF);
                                        targetAdd = true;
                                        slot++;

                                    }
                                    else if (feat.getShortName().equals(REF_REL)) {

                                        int chainNo = Integer
                                                .valueOf(mAnno.split("->")[1].split("-")[0]);
                                        int LinkNo = Integer
                                                .valueOf(mAnno.split("->")[1].split("-")[1]);
                                        chainAnnosPerTyep.putIfAbsent(type, new TreeMap<>());
                                        if (chainAnnosPerTyep.get(type).get(chainNo) != null
                                                && chainAnnosPerTyep.get(type).get(chainNo)
                                                        .get(LinkNo) != null) {
                                            continue;
                                        }
                                        String refRel = mAnno.split("->")[0];

                                        refRel = getEscapeChars(refRel);
                                        if (refRel.equals("*")) {
                                            refRel = null;
                                        }

                                        annos.get(i).setFeatureValueFromString(feat, refRel);
                                        chainAnnosPerTyep.putIfAbsent(type, new TreeMap<>());
                                        chainAnnosPerTyep.get(type).putIfAbsent(chainNo,
                                                new TreeMap<>());
                                        chainAnnosPerTyep.get(type).get(chainNo).put(LinkNo,
                                                annos.get(i));

                                    }
                                    else if (feat.getShortName().equals(REF_LINK)) {

                                        mAnno = getEscapeChars(mAnno);

                                        annos.get(i).setFeatureValueFromString(feat, mAnno);
                                        aJCas.addFsToIndexes(annos.get(i));

                                    }

                                    else if (depFeatures.get(type) != null
                                            && depFeatures.get(type).equals(feat)) {

                                        int g = depRef.isEmpty() ? 0
                                                : Integer.valueOf(depRef.split("_")[0]);
                                        int d = depRef.isEmpty() ? 0
                                                : Integer.valueOf(depRef.split("_")[1]);
                                        Type depType = depTypess.get(type);
                                        AnnotationUnit govUnit = token2Units.get(mAnno);
                                        int l = annotationsPerPostion.get(type).get(unit).size();
                                        String thisUnit = annotationsPerPostion.get(type).get(unit)
                                                .get(l - 1);
                                        AnnotationUnit depUnit = token2Units.get(thisUnit);
                                        AnnotationFS govFs;
                                        AnnotationFS depFs;

                                        if (depType.getName().equals(POS.class.getName())) {
                                            depType = aJCas.getCas().getTypeSystem()
                                                    .getType(Token.class.getName());
                                            govFs = units2Tokens.get(govUnit);
                                            depFs = units2Tokens.get(unit);

                                        }
                                        // to pass the test case, which have relation on Token which
                                        // not the case
                                        // in WebAnno world :)(!
                                        else if (depType.getName().equals(Token.class.getName())) {
                                            govFs = units2Tokens.get(govUnit);
                                            depFs = units2Tokens.get(unit);
                                        }
                                        else if (g == 0 && d == 0) {
                                            govFs = annosPerRef.get(depType).get(govUnit).entrySet()
                                                    .iterator().next().getValue();
                                            depFs = annosPerRef.get(depType).get(depUnit).entrySet()
                                                    .iterator().next().getValue();
                                        }
                                        else if (g == 0) {
                                            govFs = annosPerRef.get(depType).get(govUnit).entrySet()
                                                    .iterator().next().getValue();
                                            depFs = annosPerRef.get(depType).get(depUnit).get(d);
                                        }
                                        else {
                                            govFs = annosPerRef.get(depType).get(govUnit).get(g);
                                            depFs = annosPerRef.get(depType).get(depUnit).entrySet()
                                                    .iterator().next().getValue();
                                        }

                                        annos.get(i).setFeatureValue(feat, depFs);
                                        annos.get(i).setFeatureValue(
                                                type.getFeatureByBaseName(GOVERNOR), govFs);
                                        if (depFs.getBegin() <= annos.get(i).getBegin()) {
                                            Feature beginF = type.getFeatureByBaseName(
                                                    CAS.FEATURE_BASE_NAME_BEGIN);
                                            annos.get(i).getCAS().removeFsFromIndexes(annos.get(i));
                                            annos.get(i).setIntValue(beginF, depFs.getBegin());
                                            annos.get(i).getCAS().addFsToIndexes(annos.get(i));
                                        }
                                        else {
                                            Feature endF = type.getFeatureByBaseName(
                                                    CAS.FEATURE_BASE_NAME_END);
                                            annos.get(i).getCAS().removeFsFromIndexes(annos.get(i));
                                            annos.get(i).setIntValue(endF, depFs.getEnd());
                                            annos.get(i).getCAS().addFsToIndexes(annos.get(i));
                                        }
                                        aJCas.addFsToIndexes(annos.get(i));

                                    }
                                    else {
                                        mAnno = getEscapeChars(mAnno);
                                        multiTokUnits.putIfAbsent(unit, new HashMap<>());
                                        multiTokUnits.get(unit).put(ref, annos.get(i));
                                        prevAnnoFs = annos.get(i);
                                        annos.get(i).setFeatureValueFromString(feat, mAnno);
                                        aJCas.addFsToIndexes(annos.get(i));
                                        setAnnoRefPerUnit(unit, type, ref, annos.get(i));
                                    }

                                }
                                if (stackedAnnos.length > 1) {
                                    ref++;
                                }
                            }
                            if (type.getName().equals(POS.class.getName())) {
                                units2Tokens.get(unit).setPos((POS) annos.get(i));
                            }
                            if (type.getName().equals(Lemma.class.getName())) {
                                units2Tokens.get(unit).setLemma((Lemma) annos.get(i));
                            }
                            if (type.getName().equals(Stem.class.getName())) {
                                units2Tokens.get(unit).setStem((Stem) annos.get(i));
                            }
                            if (type.getName().equals(MorphologicalFeatures.class.getName())) {
                                units2Tokens.get(unit)
                                        .setMorph((MorphologicalFeatures) annos.get(i));
                            }
                            i++;
                        }

                        if (targetAdd) {
                            linkFSesPerSlotAnno = new HashMap<>();
                        }
                    }
                    else {
                        prevAnnoFs = null;
                    }
                    j++;
                }
                if (prevAnnoFs != null) {
                    ref++;
                }
            }
            annosPerRef.put(type, multiTokUnits);
        }

    }

    private int addAnnotationWithNoFeature(JCas aJCas, Type aType, AnnotationUnit aUnit,
            List<AnnotationFS> aAnnos,
            Map<AnnotationUnit, Map<Integer, AnnotationFS>> aMultiTokUnits, int aEnd, int aRef)
    {
        String anno = annotationsPerPostion.get(aType).get(aUnit).get(0);
        if (!anno.equals("_")) {
            int i = 0;
            String stackedAnnoRegex = "(?<!\\\\)" + Pattern.quote("|");
            for (String mAnnos : anno.split(stackedAnnoRegex)) {
                String multipleSlotAnno = "(?<!\\\\)" + Pattern.quote(";");
                for (String mAnno : mAnnos.split(multipleSlotAnno)) {
                    String depRef = "";
                    if (mAnno.endsWith("]")) {
                        depRef = mAnno.substring(mAnno.indexOf("[") + 1, mAnno.length() - 1);
                        aRef = depRef.contains("_") ? 0
                                : Integer.valueOf(mAnno.substring(mAnno.indexOf("[") + 1,
                                        mAnno.length() - 1));
                        mAnno = mAnno.substring(0, mAnno.indexOf("["));
                    }

                    boolean isMultitoken = false;
                    AnnotationFS multiAnnoFs = null;

                    if (!aMultiTokUnits.isEmpty()) {
                        for (AnnotationUnit u : aMultiTokUnits.keySet()) {
                            for (Integer r : aMultiTokUnits.get(u).keySet()) {
                                if (aRef == r) {
                                    isMultitoken = true;
                                    multiAnnoFs = aMultiTokUnits.get(u).get(r);
                                    break;
                                }
                            }
                        }
                    }

                    if (isMultitoken) {

                        Feature endF = aType.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_END);
                        multiAnnoFs.getCAS().removeFsFromIndexes(multiAnnoFs);
                        multiAnnoFs.setIntValue(endF, aEnd);
                        multiAnnoFs.getCAS().addFsToIndexes(multiAnnoFs);
                        setAnnoRefPerUnit(aUnit, aType, aRef, multiAnnoFs);

                    }
                    else {

                        aMultiTokUnits.putIfAbsent(aUnit, new HashMap<>());
                        aMultiTokUnits.get(aUnit).put(aRef, aAnnos.get(i));
                        aJCas.addFsToIndexes(aAnnos.get(i));
                        setAnnoRefPerUnit(aUnit, aType, aRef, aAnnos.get(i));
                    }
                    aRef++;
                }
                i++;
            }
        }
        return aRef;
    }

    private String getEscapeChars(String aAnno)
    {
        if (aAnno == null) {
            return null;
        }

        return unescapeJava(aAnno);
    }
    
    private String unEscapeSpecial(String aText) {
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
                esc.toArray(new String[esc.size()]), pat.toArray(new String[pat.size()]));
    }

    /**
     * update a base annotation with slot annotations
     * 
     * @param linkFSesPerAnno
     *            contains list of slot annotations per a base annotation
     * @param aLinkeF
     *            The link slot annotation feature
     */
    private void addSlotAnnotations(Map<AnnotationFS, List<FeatureStructure>> linkFSesPerAnno,
            Feature aLinkeF)
    {
        for (AnnotationFS anno : linkFSesPerAnno.keySet()) {
            ArrayFS array = anno.getCAS().createArrayFS(linkFSesPerAnno.get(anno).size());
            array.copyFromArray(
                    linkFSesPerAnno.get(anno)
                            .toArray(new FeatureStructure[linkFSesPerAnno.get(anno).size()]),
                    0, 0, linkFSesPerAnno.get(anno).size());
            anno.setFeatureValue(aLinkeF, array);
            anno.getCAS().addFsToIndexes(anno);
        }
    }

    /**
     * Gets annotations from lines (of {@link AnnotationUnit}s) and save for the later access, while
     * reading the document the first time. <br>
     * 
     * @param lines
     *            TSV lines exported from WebAnno
     * @param unit
     *            the annotation unit (Token or sub-tokens)
     * @param ind
     *            index of the annotation, from the TAB separated annotations in the TSV lines
     */
    private void setAnnosPerTypePerUnit(String[] lines, AnnotationUnit unit, int ind)
    {
        for (Type type : allLayers.keySet()) {

            annotationsPerPostion.putIfAbsent(type, new LinkedHashMap<>());

            if (allLayers.get(type).size() == 0) {

                annotationsPerPostion.get(type).put(unit,
                        annotationsPerPostion.get(type).getOrDefault(unit, new ArrayList<>()));
                annotationsPerPostion.get(type).get(unit).add(lines[ind]);
                ind++;
                continue;
            }

            for (Feature f : allLayers.get(type)) {
                annotationsPerPostion.get(type).put(unit,
                        annotationsPerPostion.get(type).getOrDefault(unit, new ArrayList<>()));
                annotationsPerPostion.get(type).get(unit).add(lines[ind]);
                ind++;
            }
            // Add at the last position the line number
            // It will be used to get Annotation unit
            annotationsPerPostion.get(type).get(unit).add(lines[0]);
        }
    }

    private void setAnnosPerUnit(JCas aJCas,
            Map<Type, Map<AnnotationUnit, List<AnnotationFS>>> aAnnosPerTypePerUnit)
    {
        for (Type type : annotationsPerPostion.keySet()) {
            Map<AnnotationUnit, List<AnnotationFS>> annosPerUnit = new HashMap<>();
            for (AnnotationUnit unit : annotationsPerPostion.get(type).keySet()) {

                int begin = unit.begin;
                int end = unit.end;
                List<AnnotationFS> annos = new ArrayList<>();
                // if there are multiple annos
                int multAnnos = 1;
                for (String anno : annotationsPerPostion.get(type).get(unit)) {
                    String stackedAnnoRegex = "(?<!\\\\)" + Pattern.quote("|");
                    if (anno.split(stackedAnnoRegex).length > multAnnos) {
                        multAnnos = anno.split(stackedAnnoRegex).length;
                    }
                }

                for (int i = 0; i < multAnnos; i++) {

                    annos.add(aJCas.getCas().createAnnotation(type, begin, end));
                }
                annosPerUnit.put(unit, annos);
            }
            aAnnosPerTypePerUnit.put(type, annosPerUnit);
        }
    }

    private void setAnnoRefPerUnit(AnnotationUnit unit, Type type, int ref, AnnotationFS aAnnoFs)
    {
        annoUnitperAnnoFs.putIfAbsent(type, new HashMap<>());
        annoUnitperAnnoFs.get(type).putIfAbsent(unit, new HashMap<>());
        annoUnitperAnnoFs.get(type).get(unit).put(ref, aAnnoFs);
    }

    private AnnotationUnit createTokens(JCas aJCas, String[] lines, int begin, int end)
    {
        // subtokens should not be consider as tokens. example 1-2.1 ==> subtoken under token 2
        if (!lines[0].contains(".")) {
            Token token = new Token(aJCas, begin, end);
            AnnotationUnit unit = new AnnotationUnit(begin, end, false, "");
            units.add(unit);
            token.addToIndexes();
            token2Units.put(lines[0], unit);
            units2Tokens.put(unit, token);
            return unit;
        }
        else {
            AnnotationUnit unit = new AnnotationUnit(begin, end, true, "");
            units.add(unit);
            token2Units.put(lines[0], unit);
            return unit;
        }
    }

    private void createSentence(JCas aJCas, String aLine, int aBegin, int aEnd, int aPrevEnd)
    {
        // If the next sentence immediately follows the last one without any space or line break
        // in between, then we need to chop off again the linebreak that we added at the end of the
        // last sentence - otherwise offsets will be off on a round-trip.
        if (aPrevEnd == aBegin && coveredText.length() > 0
                && (coveredText.charAt(coveredText.length() - 1) == '\n')) {
            coveredText.deleteCharAt(coveredText.length() - 1);
        }

        if (aPrevEnd + 1 < aBegin) {
            // FIXME This is very slow. Better use StringUtils.repeat()
            StringBuilder pad = new StringBuilder(); // if there is plenty of spaces between
                                                     // sentences
            for (int i = aPrevEnd + 1; i < aBegin; i++) {
                pad.append(" ");
            }
            coveredText.append(pad).append(aLine).append(LF);
        }
        else {
            coveredText.append(aLine).append(LF);
        }
        Sentence sentence = new Sentence(aJCas, aBegin, aEnd);
        sentence.addToIndexes();
    }

    /**
     * Get the type and feature information from the TSV file header
     * 
     * @param header
     *            the header line
     * @throws IOException
     *             If the type or the feature do not exist in the CAs
     */
    private void setLayerAndFeature(JCas aJcas, String header)
        throws IOException
    {
        try {
            StringTokenizer headerTk = new StringTokenizer(header, "#");
            while (headerTk.hasMoreTokens()) {
                String layerNames = headerTk.nextToken().trim();
                StringTokenizer layerTk = new StringTokenizer(layerNames, "|");

                Set<Feature> features = new LinkedHashSet<>();
                String layerName = layerTk.nextToken().trim();
                layerName = layerName.substring(layerName.indexOf("=") + 1);

                Iterator<Type> types = aJcas.getTypeSystem().getTypeIterator();
                boolean layerExists = false;
                while (types.hasNext()) {

                    if (types.next().getName().equals(layerName)) {
                        layerExists = true;
                        break;
                    }
                }
                if (!layerExists) {
                    throw new IOException(fileName + " This is not a valid TSV File. The layer "
                            + layerName + " is not created in the project.");
                }
                Type layer = CasUtil.getType(aJcas.getCas(), layerName);
                // if the layer do not have a feature, just update columns count for the place
                // holder
                if (!layerTk.hasMoreTokens()) {
                    columns++;
                    allLayers.put(layer, features);
                    layerMaps.put(layerMaps.size() + 1, layer);
                    return;
                }
                while (layerTk.hasMoreTokens()) {
                    String ft = layerTk.nextToken().trim();
                    columns++;
                    Feature feature;

                    if (ft.startsWith(BT)) {
                        feature = layer.getFeatureByBaseName(DEPENDENT);
                        depFeatures.put(layer, feature);
                        depTypess.put(layer, CasUtil.getType(aJcas.getCas(), ft.substring(3)));
                    }
                    else {
                        feature = layer.getFeatureByBaseName(ft);
                    }
                    if (ft.startsWith(ROLE)) {
                        ft = ft.substring(5);
                        String t = layerTk.nextToken();
                        columns++;
                        Type tType = CasUtil.getType(aJcas.getCas(), t);
                        String fName = ft.substring(0, ft.indexOf("_"));
                        Feature slotF = layer
                                .getFeatureByBaseName(fName.substring(fName.indexOf(":") + 1));
                        if (slotF == null) {
                            throw new IOException(
                                    fileName + " This is not a valid TSV File. The feature " + ft
                                            + " is not created for the layer " + layerName);
                        }
                        features.add(slotF);
                        roleLinks.put(slotF, tType);
                        Type slotType = CasUtil.getType(aJcas.getCas(),
                                ft.substring(ft.indexOf("_") + 1));
                        Feature tFeatore = slotType.getFeatureByBaseName("target");
                        if (tFeatore == null) {
                            throw new IOException(
                                    fileName + " This is not a valid TSV File. The feature " + ft
                                            + " is not created for the layer " + layerName);
                        }
                        roleTargets.put(tFeatore, tType);
                        features.add(tFeatore);
                        slotLinkTypes.put(slotF, slotType);
                        continue;
                    }

                    if (feature == null) {
                        throw new IOException(
                                fileName + " This is not a valid TSV File. The feature " + ft
                                        + " is not created for the layer " + layerName);
                    }
                    features.add(feature);
                }
                allLayers.put(layer, features);
                layerMaps.put(layerMaps.size() + 1, layer);
            }
        }
        catch (Exception e) {
            throw new IOException(e.getMessage() + "\nTSV header:\n" + header);
        }
    }

    public static final String PARAM_ENCODING = ComponentParameters.PARAM_SOURCE_ENCODING;
    @ConfigurationParameter(name = PARAM_ENCODING, mandatory = true, defaultValue = "UTF-8")
    private String encoding;

    @Override
    public void getNext(JCas aJCas)
        throws IOException, CollectionException
    {
        Resource res = nextFile();
        initCas(aJCas, res);
        InputStream is = null;
        try {
            is = res.getInputStream();
            convertToCas(aJCas, is, encoding);
        }
        finally {
            closeQuietly(is);
        }

    }
}

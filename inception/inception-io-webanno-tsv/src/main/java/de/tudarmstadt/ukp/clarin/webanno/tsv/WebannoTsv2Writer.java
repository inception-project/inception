/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.tsv;

import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.io.IOUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.LowLevelCAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.core.api.io.JCasFileWriter_ImplBase;
import org.dkpro.core.api.parameter.ComponentParameters;

import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.TagsetDescription;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 * Export annotations in TAB separated format. Header includes information about the UIMA type and
 * features The number of columns are depend on the number of types/features exist. All the spans
 * will be written first and subsequently all the relations. relation is given in the form of
 * Source--&gt;Target and the RelationType is added to the Target token. The next column indicates
 * the source of the relation (the source of the arc drown)
 *
 * @author Seid Muhie Yimam
 */
public class WebannoTsv2Writer
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

    public static final String MULTIPLE_SPAN_ANNOTATIONS = "multipleSpans";
    @ConfigurationParameter(name = MULTIPLE_SPAN_ANNOTATIONS, mandatory = true, defaultValue = {})
    private List<String> multipleSpans;

    private final String DEPENDENT = "Dependent";
    private final String GOVERNOR = "Governor";
    private final String FIRST = "first";
    private final String NEXT = "next";
    Map<Integer, String> tokenIds;
    NavigableMap<Integer, Integer> tokenPositions;

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException
    {
        try (OutputStream docOS = getOutputStream(aJCas, filenameSuffix)) {
            convertToTsv(aJCas, docOS, encoding);
        }
        catch (Exception e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    private void convertToTsv(JCas aJCas, OutputStream aOs, String aEncoding)
        throws IOException, ResourceInitializationException, CASRuntimeException, CASException
    {
        LowLevelCAS llCas = aJCas.getLowLevelCas();

        tokenIds = new HashMap<>();
        setTokenId(aJCas, tokenIds);
        tokenPositions = new TreeMap<>();
        setTokenPosition(aJCas, tokenPositions);

        Map<Integer, Integer> getTokensPerSentence = new TreeMap<>();
        setTokenSentenceAddress(aJCas, getTokensPerSentence);

        // list of annotation types
        Set<Type> allTypes = new LinkedHashSet<>();

        for (Annotation a : select(aJCas, Annotation.class)) {
            if (!(a instanceof Token || a instanceof Sentence || a instanceof DocumentMetaData
                    || a instanceof TagsetDescription || a instanceof CoreferenceLink)) {
                allTypes.add(a.getType());
            }
        }
        Set<Type> relationTypes = new LinkedHashSet<>();

        // get all arc types
        for (Type type : allTypes) {
            if (type.getFeatures().size() == 0) {
                continue;
            }

            for (Feature feature : type.getFeatures()) {
                if (feature.getShortName().equals(GOVERNOR)) {
                    relationTypes.add(type);
                    break;
                }
            }
        }

        allTypes.removeAll(relationTypes);

        // relation annotations
        Map<Type, String> relationTypesMap = new HashMap<>();
        for (Type type : relationTypes) {
            if (type.getName().equals(Dependency.class.getName())) {
                relationTypesMap.put(type, POS.class.getName());
                continue;
            }
            for (AnnotationFS anno : CasUtil.select(aJCas.getCas(), type)) {
                for (Feature feature : type.getFeatures()) {
                    if (feature.getShortName().equals(GOVERNOR)) {
                        relationTypesMap.put(type,
                                anno.getFeatureValue(feature).getType().getName());
                    }
                }
            }
        }

        // all span annotation first

        Map<Feature, Type> spanFeatures = new LinkedHashMap<>();
        allTypes: for (Type type : allTypes) {
            if (type.getFeatures().size() == 0) {
                continue;
            }
            for (Feature feature : type.getFeatures()) {
                // coreference annotation not supported
                if (feature.getShortName().equals(FIRST) || feature.getShortName().equals(NEXT)) {
                    continue allTypes;
                }
            }
            IOUtils.write(" # " + type.getName(), aOs, aEncoding);
            for (Feature feature : type.getFeatures()) {
                if (feature.getName().equals(CAS.FEATURE_FULL_NAME_SOFA)
                        || feature.getName().equals(CAS.FEATURE_FULL_NAME_BEGIN)
                        || feature.getName().equals(CAS.FEATURE_FULL_NAME_END)) {
                    continue;
                }
                spanFeatures.put(feature, type);
                IOUtils.write(" | " + feature.getShortName(), aOs, aEncoding);
            }
        }

        // write all relation annotation first
        Set<Feature> relationFeatures = new LinkedHashSet<>();
        for (Type type : relationTypes) {
            IOUtils.write(" # " + type.getName(), aOs, aEncoding);
            for (Feature feature : type.getFeatures()) {
                if (feature.getName().equals(CAS.FEATURE_FULL_NAME_SOFA)
                        || feature.getName().equals(CAS.FEATURE_FULL_NAME_BEGIN)
                        || feature.getName().equals(CAS.FEATURE_FULL_NAME_END)
                        || feature.getShortName().equals(GOVERNOR)
                        || feature.getShortName().equals(DEPENDENT)) {
                    continue;
                }
                relationFeatures.add(feature);
                IOUtils.write(" | " + feature.getShortName(), aOs, aEncoding);
            }
            // Add the attach type for the realtion anotation
            IOUtils.write(" | AttachTo=" + relationTypesMap.get(type), aOs, aEncoding);
        }

        IOUtils.write("\n", aOs, aEncoding);

        Map<Feature, Map<Integer, String>> allAnnos = new HashMap<>();
        allTypes: for (Type type : allTypes) {
            for (Feature feature : type.getFeatures()) {
                // coreference annotation not supported
                if (feature.getShortName().equals(FIRST) || feature.getShortName().equals(NEXT)) {
                    continue allTypes;
                }
            }
            for (Feature feature : type.getFeatures()) {
                if (feature.getName().equals(CAS.FEATURE_FULL_NAME_SOFA)
                        || feature.getName().equals(CAS.FEATURE_FULL_NAME_BEGIN)
                        || feature.getName().equals(CAS.FEATURE_FULL_NAME_END)) {
                    continue;
                }

                Map<Integer, String> tokenAnnoMap = new TreeMap<>();
                setTokenAnnos(aJCas.getCas(), tokenAnnoMap, type, feature);
                allAnnos.put(feature, tokenAnnoMap);

            }
        }
        // get tokens where dependents are drown to
        Map<Feature, Map<Integer, String>> relAnnos = new HashMap<>();
        for (Type type : relationTypes) {
            for (Feature feature : type.getFeatures()) {
                if (feature.getName().equals(CAS.FEATURE_FULL_NAME_SOFA)
                        || feature.getName().equals(CAS.FEATURE_FULL_NAME_BEGIN)
                        || feature.getName().equals(CAS.FEATURE_FULL_NAME_END)
                        || feature.getShortName().equals(GOVERNOR)
                        || feature.getShortName().equals(DEPENDENT)) {
                    continue;
                }

                Map<Integer, String> tokenAnnoMap = new HashMap<>();
                setRelationFeatureAnnos(aJCas.getCas(), tokenAnnoMap, type, feature);
                relAnnos.put(feature, tokenAnnoMap);
            }
        }

        // get tokens where dependents are drown from - the governor
        Map<Type, Map<Integer, String>> governorAnnos = new HashMap<>();
        for (Type type : relationTypes) {

            Map<Integer, String> govAnnoMap = new HashMap<>();
            setRelationGovernorPos(aJCas.getCas(), govAnnoMap, type);
            governorAnnos.put(type, govAnnoMap);
        }

        int sentId = 1;
        for (Sentence sentence : select(aJCas, Sentence.class)) {
            IOUtils.write("#id=" + sentId++ + "\n", aOs, aEncoding);
            IOUtils.write("#text=" + sentence.getCoveredText().replace("\n", "") + "\n", aOs,
                    aEncoding);
            for (Token token : selectCovered(Token.class, sentence)) {
                IOUtils.write(tokenIds.get(llCas.ll_getFSRef(token)) + "\t" + token.getCoveredText()
                        + "\t", aOs, aEncoding);

                // all span annotations on this token
                for (Feature feature : spanFeatures.keySet()) {
                    String annos = allAnnos.get(feature).get(llCas.ll_getFSRef(token));
                    if (annos == null) {
                        if (multipleSpans.contains(spanFeatures.get(feature).getName())) {
                            IOUtils.write("O\t", aOs, aEncoding);
                        }
                        else {
                            IOUtils.write("_\t", aOs, aEncoding);
                        }
                    }
                    else {
                        IOUtils.write(annos + "\t", aOs, aEncoding);
                    }
                }
                // for all relation features

                for (Type type : relationTypes) {
                    for (Feature feature : type.getFeatures()) {
                        if (feature.getName().equals(CAS.FEATURE_FULL_NAME_SOFA)
                                || feature.getName().equals(CAS.FEATURE_FULL_NAME_BEGIN)
                                || feature.getName().equals(CAS.FEATURE_FULL_NAME_END)
                                || feature.getShortName().equals(GOVERNOR)
                                || feature.getShortName().equals(DEPENDENT)) {
                            continue;
                        }
                        String annos = relAnnos.get(feature).get(llCas.ll_getFSRef(token));
                        if (annos == null) {
                            IOUtils.write("_\t", aOs, aEncoding);
                        }
                        else {
                            IOUtils.write(annos + "\t", aOs, aEncoding);
                        }
                    }

                    // the governor positions
                    String govPos = governorAnnos.get(type).get(llCas.ll_getFSRef(token));
                    if (govPos == null) {
                        IOUtils.write("_\t", aOs, aEncoding);
                    }
                    else {
                        IOUtils.write(governorAnnos.get(type).get(llCas.ll_getFSRef(token)) + "\t",
                                aOs, aEncoding);
                    }
                }
                IOUtils.write("\n", aOs, aEncoding);
            }
            IOUtils.write("\n", aOs, aEncoding);
        }

    }

    private void setTokenSentenceAddress(JCas aJCas, Map<Integer, Integer> aTokenListInSentence)
    {
        LowLevelCAS llCas = aJCas.getLowLevelCas();
        for (Sentence sentence : select(aJCas, Sentence.class)) {
            for (Token token : selectCovered(Token.class, sentence)) {
                aTokenListInSentence.put(llCas.ll_getFSRef(token), llCas.ll_getFSRef(sentence));
            }
        }
    }

    private void setTokenId(JCas aJCas, Map<Integer, String> aTokenAddress)
    {
        LowLevelCAS llCas = aJCas.getLowLevelCas();
        int sentenceId = 1;
        for (Sentence sentence : select(aJCas, Sentence.class)) {
            int tokenId = 1;
            for (Token token : selectCovered(Token.class, sentence)) {
                aTokenAddress.put(llCas.ll_getFSRef(token), sentenceId + "-" + tokenId++);
            }
            sentenceId++;
        }
    }

    private void setTokenPosition(JCas aJCas, Map<Integer, Integer> aTokenAddress)
    {
        LowLevelCAS llCas = aJCas.getLowLevelCas();
        for (Token token : select(aJCas, Token.class)) {
            aTokenAddress.put(token.getBegin(), llCas.ll_getFSRef(token));
        }
    }

    private void setTokenAnnos(CAS aCas, Map<Integer, String> aTokenAnnoMap, Type aType,
            Feature aFeature)
    {
        LowLevelCAS llCas = aCas.getLowLevelCAS();
        for (AnnotationFS annoFs : CasUtil.select(aCas, aType)) {
            boolean first = true;
            boolean previous = false; // exists previous annotation, place-holed O-_ should be kept
            for (Token token : selectCovered(Token.class, annoFs)) {
                if (annoFs.getBegin() <= token.getBegin() && annoFs.getEnd() >= token.getEnd()) {
                    String annotation = annoFs.getFeatureValueAsString(aFeature);
                    if (annotation == null) {
                        annotation = aType.getName() + "_";
                    }
                    if (aTokenAnnoMap.get(llCas.ll_getFSRef(token)) == null) {
                        if (previous) {
                            if (!multipleSpans.contains(aType.getName())) {
                                aTokenAnnoMap.put(llCas.ll_getFSRef(token), annotation);
                            }
                            else {
                                aTokenAnnoMap.put(llCas.ll_getFSRef(token),
                                        "O-_|" + (first ? "B-" : "I-") + annotation);
                                first = false;
                            }
                        }
                        else {
                            if (!multipleSpans.contains(aType.getName())) {
                                aTokenAnnoMap.put(llCas.ll_getFSRef(token), annotation);
                            }
                            else {
                                aTokenAnnoMap.put(llCas.ll_getFSRef(token),
                                        (first ? "B-" : "I-") + annotation);
                                first = false;
                            }
                        }
                    }
                    else {
                        if (!multipleSpans.contains(aType.getName())) {
                            aTokenAnnoMap.put(llCas.ll_getFSRef(token),
                                    aTokenAnnoMap.get(llCas.ll_getFSRef(token)) + "|" + annotation);
                            previous = true;
                        }
                        else {
                            aTokenAnnoMap.put(llCas.ll_getFSRef(token),
                                    aTokenAnnoMap.get(llCas.ll_getFSRef(token)) + "|"
                                            + (first ? "B-" : "I-") + annotation);
                            first = false;
                            previous = true;
                        }
                    }

                }
            }
        }
    }

    private void setRelationFeatureAnnos(CAS aCas, Map<Integer, String> aRelAnnoMap, Type aType,
            Feature aFeature)
        throws CASRuntimeException, CASException
    {
        LowLevelCAS llCas = aCas.getLowLevelCAS();
        Feature dependent = null;
        AnnotationFS temp = null;
        for (Feature feature : aType.getFeatures()) {
            if (feature.getShortName().equals(DEPENDENT)) {
                dependent = feature;
            }
        }
        for (AnnotationFS annoFs : CasUtil.select(aCas, aType)) {
            // relation annotation will be from Governor to Dependent
            // Entry done on Dependent side
            temp = annoFs;
            annoFs = (AnnotationFS) annoFs.getFeatureValue(dependent);
            boolean first = true;
            for (Token token : selectCovered(aCas.getJCas(), Token.class, annoFs.getBegin(),
                    annoFs.getEnd())) {
                if (annoFs.getBegin() <= token.getBegin() && annoFs.getEnd() >= token.getEnd()) {
                    annoFs = temp;

                    String annotation = annoFs.getFeatureValueAsString(aFeature);
                    if (annotation == null) {
                        annotation = aType.getName() + "_";
                    }
                    if (aRelAnnoMap.get(llCas.ll_getFSRef(token)) == null) {
                        if (!multipleSpans.contains(aType.getName())) {
                            aRelAnnoMap.put(llCas.ll_getFSRef(token), annotation);
                        }
                        else {
                            aRelAnnoMap.put(llCas.ll_getFSRef(token),
                                    (first ? "B-" : "I-") + annotation);
                            first = false;
                        }
                    }
                    else {
                        if (!multipleSpans.contains(aType.getName())) {
                            aRelAnnoMap.put(llCas.ll_getFSRef(token),
                                    aRelAnnoMap.get(llCas.ll_getFSRef(token)) + "|" + annotation);
                        }
                        else {
                            aRelAnnoMap.put(llCas.ll_getFSRef(token),
                                    aRelAnnoMap.get(llCas.ll_getFSRef(token)) + "|"
                                            + (first ? "B-" : "I-") + annotation);
                            first = false;
                        }
                    }
                }
                // TODO: remove the B- and I- code in the if/else above. no such a thing of
                // multiple-span annotation on relations.

                // if the annotation gov/dep span annotation is on multiple tokens,
                // we just need an arc to the first token.
                break;
            }
        }
    }

    private void setRelationGovernorPos(CAS aCas, Map<Integer, String> aRelationGovernorMap,
            Type aType)
        throws CASRuntimeException, CASException
    {
        Feature governor = null, dependent = null;
        AnnotationFS temp = null;
        for (Feature feature : aType.getFeatures()) {
            if (feature.getShortName().equals(GOVERNOR)) {
                governor = feature;
            }
            if (feature.getShortName().equals(DEPENDENT)) {
                dependent = feature;
            }
        }

        LowLevelCAS llCas = aCas.getLowLevelCAS();
        for (AnnotationFS anno : CasUtil.select(aCas, aType)) {
            // relation annotation will be from Governor to Dependent
            // Entry done on Dependent side
            temp = anno;
            anno = (AnnotationFS) anno.getFeatureValue(dependent);
            for (Token token : selectCovered(aCas.getJCas(), Token.class, anno.getBegin(),
                    anno.getEnd())) {
                if (anno.getBegin() <= token.getBegin() && anno.getEnd() >= token.getEnd()) {
                    if (aRelationGovernorMap.get(llCas.ll_getFSRef(token)) == null) {
                        AnnotationFS govAnno = (AnnotationFS) temp.getFeatureValue(governor);
                        aRelationGovernorMap.put(llCas.ll_getFSRef(token), tokenIds
                                .get(tokenPositions.floorEntry(govAnno.getBegin()).getValue()));
                    }
                    else {
                        AnnotationFS govAnno = (AnnotationFS) temp.getFeatureValue(governor);
                        aRelationGovernorMap.put(llCas.ll_getFSRef(token), aRelationGovernorMap
                                .get(llCas.ll_getFSRef(token)) + "|"
                                + tokenIds.get(
                                        tokenPositions.floorEntry(govAnno.getBegin()).getValue()));
                    }
                }
                // if the annotation gov/dep span annotation is on multiple tokens,
                // we just need an arc to the first token.
                break;
            }
        }
    }

}

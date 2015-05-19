/*******************************************************************************
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.tsv;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
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
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink;
import de.tudarmstadt.ukp.dkpro.core.api.io.JCasFileWriter_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.TagsetDescription;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
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
 *
 */

public class WebannoCustomTsvWriter2
    extends JCasFileWriter_ImplBase
{

    /**
     * Name of configuration parameter that contains the character encoding used by the input files.
     */
    public static final String PARAM_ENCODING = ComponentParameters.PARAM_SOURCE_ENCODING;
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
    NavigableMap<String, String> itemPositions;

    @Override
    public void process(JCas aJCas)
        throws AnalysisEngineProcessException
    {
        OutputStream docOS = null;
        try {
            docOS = getOutputStream(aJCas, filenameSuffix);
            convertToTsv(aJCas, docOS, encoding);
        }
        catch (Exception e) {
            throw new AnalysisEngineProcessException(e);
        }
        finally {
            closeQuietly(docOS);
        }

    }

    private void convertToTsv(JCas aJCas, OutputStream aOs, String aEncoding)
        throws IOException, ResourceInitializationException, CASRuntimeException, CASException
    {
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

        List<AnnotationItem> annoItems = new LinkedList<>();
        itemPositions = new TreeMap<>();
        setAnnoItems(aJCas, allTypes, annoItems);
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
                        relationTypesMap.put(type, anno.getFeatureValue(feature).getType()
                                .getName());
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
                if (feature.toString().equals("uima.cas.AnnotationBase:sofa")
                        || feature.toString().equals("uima.tcas.Annotation:begin")
                        || feature.toString().equals("uima.tcas.Annotation:end")) {
                    continue;
                }
                spanFeatures.put(feature, type);
                IOUtils.write(" | " + feature.getShortName(), aOs, aEncoding);
            }
        }

        // write all relation annotation first
        for (Type type : relationTypes) {
            IOUtils.write(" # " + type.getName(), aOs, aEncoding);
            for (Feature feature : type.getFeatures()) {
                IOUtils.write(" | " + feature.getShortName(), aOs, aEncoding);
            }
            // Add the attach type for the realtion anotation
            IOUtils.write(" | AttachTo=" + relationTypesMap.get(type), aOs, aEncoding);
        }

        IOUtils.write("\n", aOs, aEncoding);

        Map<Feature, Map<String, String>> allAnnos = new HashMap<>();
        allTypes: for (Type type : allTypes) {
            for (Feature feature : type.getFeatures()) {
                // coreference annotation not supported
                if (feature.getShortName().equals(FIRST) || feature.getShortName().equals(NEXT)) {
                    continue allTypes;
                }
            }
            for (Feature feature : type.getFeatures()) {
                if (feature.toString().equals("uima.cas.AnnotationBase:sofa")
                        || feature.toString().equals("uima.tcas.Annotation:begin")
                        || feature.toString().equals("uima.tcas.Annotation:end")) {
                    continue;
                }

                Map<String, String> tokenAnnoMap = new TreeMap<>();
                setTokenAnnos(aJCas.getCas(), tokenAnnoMap, type, feature, annoItems);
                allAnnos.put(feature, tokenAnnoMap);

            }
        }
        // get tokens where dependents are drown to
        Map<Feature, Map<String, String>> relAnnos = new HashMap<>();
        for (Type type : relationTypes) {
            for (Feature feature : type.getFeatures()) {
                if (feature.toString().equals("uima.cas.AnnotationBase:sofa")
                        || feature.toString().equals("uima.tcas.Annotation:begin")
                        || feature.toString().equals("uima.tcas.Annotation:end")
                        || feature.getShortName().equals(GOVERNOR)
                        || feature.getShortName().equals(DEPENDENT)) {
                    continue;
                }

                Map<String, String> tokenAnnoMap = new HashMap<>();
                setRelationFeatureAnnos(aJCas.getCas(), tokenAnnoMap, type, feature, annoItems);
                relAnnos.put(feature, tokenAnnoMap);
            }
        }

        // get tokens where dependents are drown from - the governor
        Map<Type, Map<String, String>> governorAnnos = new HashMap<>();
        for (Type type : relationTypes) {

            Map<String, String> govAnnoMap = new HashMap<>();
            setRelationGovernorPos(aJCas.getCas(), govAnnoMap, type, annoItems);
            governorAnnos.put(type, govAnnoMap);
        }

        int sentId = 1;
        for (Sentence sentence : select(aJCas, Sentence.class)) {
            IOUtils.write("#id=" + sentId + "\n", aOs, aEncoding);
            IOUtils.write("#text=" + sentence.getCoveredText().replace("\n", "") + "\n", aOs,
                    aEncoding);
            for (AnnotationItem item : annoItems) {
                if (item.start < sentence.getBegin()) {
                    continue;
                }
                if (item.start > sentence.getEnd()) {
                    break;
                }
                // for (Token token : selectCovered(Token.class, sentence)) {
                IOUtils.write(item.itemId + "\t" + item.text + "\t", aOs, aEncoding);

                // all span annotations on this token
                for (Feature feature : spanFeatures.keySet()) {
                    String annos = allAnnos.get(feature).get(item.itemId);
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
                        if (feature.toString().equals("uima.cas.AnnotationBase:sofa")
                                || feature.toString().equals("uima.tcas.Annotation:begin")
                                || feature.toString().equals("uima.tcas.Annotation:end")
                                || feature.getShortName().equals(GOVERNOR)
                                || feature.getShortName().equals(DEPENDENT)) {
                            continue;
                        }
                        String annos = relAnnos.get(feature).get(item.itemId);
                        if (annos == null) {
                            IOUtils.write("_\t", aOs, aEncoding);
                        }
                        else {
                            IOUtils.write(annos + "\t", aOs, aEncoding);
                        }
                    }

                    // the governor positions
                    String govPos = governorAnnos.get(type).get(item.itemId);
                    if (govPos == null) {
                        IOUtils.write("_\t", aOs, aEncoding);
                    }
                    else {
                        IOUtils.write(governorAnnos.get(type).get(item.itemId) + "\t", aOs,
                                aEncoding);
                    }
                }
                IOUtils.write("\n", aOs, aEncoding);
            }
            IOUtils.write("\n", aOs, aEncoding);
            sentId++;
        }

    }

    private void setAnnoItems(JCas aJCas, Set<Type> allTypes, List<AnnotationItem> aAnnoItems)
    {

        int sentId = 1;
        for (Sentence sentence : select(aJCas, Sentence.class)) {
            int itemId = 1;
            for (Token token : selectCovered(Token.class, sentence)) {
                AnnotationItem item = new AnnotationItem();
                item.parentIndex = itemId - 1;
                item.itemId = sentId + "-" + itemId;
                item.start = token.getBegin();
                item.parnetTokenEnd = item.end = token.getEnd();
                item.text = token.getCoveredText();
                itemId++;
                aAnnoItems.add(item);
                itemPositions.put(item.start + "-" + item.end, item.itemId);
            }
            sentId++;
        }

        for (Type type : allTypes) {
            for (AnnotationFS annoFs : CasUtil.select(aJCas.getCas(), type)) {
                int index = annoItemExists(annoFs.getBegin(), annoFs.getEnd(), aAnnoItems);
                if (index == -1) {
                    continue;
                }
                AnnotationItem item = new AnnotationItem();
                item.start = annoFs.getBegin();
                item.end = annoFs.getEnd();
                item.parentIndex = aAnnoItems.get(index).parentIndex;
                item.parnetTokenEnd = aAnnoItems.get(index).parnetTokenEnd;
                item.subItemId = aAnnoItems.get(index).subItemId + 1;
                item.itemId = aAnnoItems.get(item.parentIndex).itemId + "-" + item.subItemId;
                item.text = annoFs.getCoveredText();
                item.isToken = false;
                aAnnoItems.add(index + 1, item);
                itemPositions.put(item.start + "-" + item.end, item.itemId);// hence it is not a
                                                                            // mere begin-end of
                                                                            // annotations
            }

        }
    }

    private int annoItemExists(int aStart, int aEnd, List<AnnotationItem> aItems)
    {
        int index = -1;
        for (AnnotationItem item : aItems) {

            if (item.start == aStart && item.end == aEnd) {
                return index;
            }
            else if ((item.start <= aStart && item.parnetTokenEnd >= aEnd)) {
                index = aItems.indexOf(item);
            }
            else if (item.start > aEnd) {
                break;
            }
        }
        return index;
    }

    private void setTokenAnnos(CAS aCas, Map<String, String> aTokenAnnoMap, Type aType,
            Feature aFeature, List<AnnotationItem> aItems)
    {
        for (AnnotationFS annoFs : CasUtil.select(aCas, aType)) {
            boolean first = true;
            boolean previous = false; // exists previous annotation, place-holed O-_ should be kept
            for (AnnotationItem item : aItems) {
                if (annoFs.getBegin() == item.start && annoFs.getEnd() == item.end) {
                    String annotation = annoFs.getFeatureValueAsString(aFeature);
                    if (annotation == null) {
                        annotation = aType.getName() + "_";
                    }
                    if (aTokenAnnoMap.get(item.itemId) == null) {
                        if (previous) {
                            if (!multipleSpans.contains(aType.getName())) {
                                aTokenAnnoMap.put(item.itemId, annotation);
                            }
                            else {
                                aTokenAnnoMap.put(item.itemId, "O-_|" + (first ? "B-" : "I-")
                                        + annotation);
                                first = false;
                            }
                        }
                        else {
                            if (!multipleSpans.contains(aType.getName())) {
                                aTokenAnnoMap.put(item.itemId, annotation);
                            }
                            else {
                                aTokenAnnoMap.put(item.itemId, (first ? "B-" : "I-") + annotation);
                                first = false;
                            }
                        }
                    }
                    else {
                        if (!multipleSpans.contains(aType.getName())) {
                            aTokenAnnoMap.put(item.itemId, aTokenAnnoMap.get(item) + "|"
                                    + annotation);
                            previous = true;
                        }
                        else {
                            aTokenAnnoMap.put(item.itemId, aTokenAnnoMap.get(item) + "|"
                                    + (first ? "B-" : "I-") + annotation);
                            first = false;
                            previous = true;
                        }
                    }
                }
            }
        }
    }

    private void setRelationFeatureAnnos(CAS aCas, Map<String, String> aRelAnnoMap, Type aType,
            Feature aFeature, List<AnnotationItem> aItems)
        throws CASRuntimeException, CASException
    {
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
            for (AnnotationItem item : aItems) {
                if (annoFs.getBegin() <= item.start && annoFs.getEnd() >= item.end) {
                    annoFs = temp;

                    String annotation = annoFs.getFeatureValueAsString(aFeature);
                    if (annotation == null) {
                        annotation = aType.getName() + "_";
                    }
                    if (aRelAnnoMap.get(item.itemId) == null) {

                        if (!multipleSpans.contains(aType.getName())) {

                            aRelAnnoMap.put(item.itemId, annotation);
                        }
                        else {

                            aRelAnnoMap.put(item.itemId, (first ? "B-" : "I-") + annotation);
                            first = false;
                        }
                    }
                    else {

                        if (!multipleSpans.contains(aType.getName())) {
                            aRelAnnoMap.put(item.itemId, aRelAnnoMap.get(item.itemId) + "|"
                                    + annotation);
                        }
                        else {
                            aRelAnnoMap.put(item.itemId, aRelAnnoMap.get(item.itemId) + "|"
                                    + (first ? "B-" : "I-") + annotation);
                            first = false;
                        }

                    }
                }
                // TODO: remove the B- and I- code in the if/else above. no such a thing of
                // multiplespan annotation on relations.

                // if the annotation gov/dep span annotation is on multiple tokens,
                // we just need an arc to the first token.
                break;
            }
        }
    }

    private void setRelationGovernorPos(CAS aCas, Map<String, String> aRelationGovernorMap,
            Type aType, List<AnnotationItem> aItems)
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

        for (AnnotationFS anno : CasUtil.select(aCas, aType)) { // relation annotation will be from
                                                                // Governor to Dependent // Entry
                                                                // done on Dependent side
            temp = anno;
            anno = (AnnotationFS) anno.getFeatureValue(dependent);
            for (AnnotationItem item : aItems) {
                if (anno.getBegin() <= item.start && anno.getEnd() >= item.end) {

                    if (aRelationGovernorMap.get(item.itemId) == null) {
                        AnnotationFS govAnno = (AnnotationFS) temp.getFeatureValue(governor);
                        aRelationGovernorMap.put(
                                item.itemId,
                                itemPositions.floorEntry(
                                        govAnno.getBegin() + "-" + govAnno.getEnd()).getValue());
                    }
                    else {
                        AnnotationFS govAnno = (AnnotationFS) temp.getFeatureValue(governor);
                        aRelationGovernorMap.put(
                                item.itemId,
                                aRelationGovernorMap.get(item.itemId)
                                        + "|"
                                        + itemPositions.floorEntry(
                                                govAnno.getBegin() + "-" + govAnno.getEnd())
                                                .getValue());
                    }
                }
                // if the annotation gov/dep span annotation is on multiple tokens,
                // we just need an arc to the first token.
                break;
            }
        }
    }

    /**
     * An annotation item is an item that carries any UIMA annotation so that can be written on its
     * own line. An annotation item can, therefore be a token, or a sub-token TODO - check if an
     * empty annotation item is still possible to be write in a new line
     *
     * @author seid
     *
     */
    public static class AnnotationItem
        implements java.io.Serializable
    {
        private static final long serialVersionUID = 6140390142654134042L;
        int start;
        int end;
        int parnetTokenEnd;
        boolean isToken = true;;
        String text;
        String itemId;
        int subItemId;
        int parentIndex;

    }

}

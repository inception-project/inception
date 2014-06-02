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
package de.tudarmstadt.ukp.clarin.webanno.tsv;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * Reads a specific TSV File (9 TAB separated) annotation and change it to CAS object. Example of
 * Input Files: <br>
 * 1 Heutzutage heutzutage ADV _ _ 2 ADV _ _ <br>
 * Columns are separated by a TAB character and sentences are separated by a blank new line see the
 * {@link WebannoTsvReader#setAnnotations(InputStream, String, StringBuilder, Map, Map, Map, Map, Map, Map, Map, List)}
 *
 * @author Seid Muhie Yimam
 *
 */
public class WebannoTsvReader
    extends JCasResourceCollectionReader_ImplBase
{

    private String fileName;

    public void convertToCas(JCas aJCas, InputStream aIs, String aEncoding)
        throws IOException

    {
        StringBuilder text = new StringBuilder();
        Map<Integer, String> tokens = new HashMap<Integer, String>();
        Map<Integer, String> pos = new HashMap<Integer, String>();
        Map<Integer, String> lemma = new HashMap<Integer, String>();
        Map<Integer, String> namedEntity = new HashMap<Integer, String>();
        Map<Integer, String> dependencyFunction = new HashMap<Integer, String>();
        Map<Integer, Integer> dependencyDependent = new HashMap<Integer, Integer>();

        List<Integer> firstTokenInSentence = new ArrayList<Integer>();

        DocumentMetaData documentMetadata = DocumentMetaData.get(aJCas);
        fileName = documentMetadata.getDocumentTitle();
        setAnnotations(aJCas, aIs, aEncoding, text, tokens, pos, lemma, namedEntity,
                dependencyFunction, dependencyDependent, firstTokenInSentence);

        aJCas.setDocumentText(text.toString());
    }

    /**
     * Iterate through all lines and get available annotations<br>
     * First column is sentence number and a blank new line marks end of a sentence<br>
     * The Second column is the token <br>
     * The third column is the lemma annotation <br>
     * The fourth column is the POS annotation <br>
     * The fifth column is used for Named Entity annotations (Multiple annotations separeted by |
     * character) <br>
     * The sixth column is the origin token number of dependency parsing <br>
     * The seventh column is the function/type of the dependency parsing <br>
     * eighth and ninth columns are undefined currently
     */
    private void setAnnotations(JCas aJcas, InputStream aIs, String aEncoding, StringBuilder text,
            Map<Integer, String> tokens, Map<Integer, String> pos, Map<Integer, String> lemma,
            Map<Integer, String> namedEntity, Map<Integer, String> dependencyFunction,
            Map<Integer, Integer> dependencyDependent, List<Integer> firstTokenInSentence)
        throws IOException
    {

        // getting header information
        LineIterator lineIterator = IOUtils.lineIterator(aIs, aEncoding);
        int columns = 1;// token number + token columns (minimum required)
        int tokenStart = 0, sentenceStart = 0;
        Map<Type, Set<Feature>> spanLayers = new LinkedHashMap<Type, Set<Feature>>();
        Map<Type, Type> relationayers = new LinkedHashMap<Type, Type>();

        // an annotation for every feature in a layer
        Map<Type, Map<Integer, AnnotationFS>> annotations = new LinkedHashMap<Type, Map<Integer, AnnotationFS>>();
        // Map<Type, String> beginEndAnnotation = new HashMap<Type, String>();
        Map<Type, Map<Integer, String>> beginEndAnno = new LinkedHashMap<Type, Map<Integer, String>>();

        // Store annotations of tokens so that it can be used later for relation annotations
        Map<Type, Map<String, List<AnnotationFS>>> tokenAnnotations = new LinkedHashMap<Type, Map<String, List<AnnotationFS>>>();

        // store target token ids used for a relation
        Map<Type, Map<String, List<String>>> relationTargets = new LinkedHashMap<Type, Map<String, List<String>>>();

        while (lineIterator.hasNext()) {
            String line = lineIterator.next().trim();
            if (line.trim().equals("")) {
                text.replace(tokenStart - 1, tokenStart, "");
                tokenStart = tokenStart - 1;
                Sentence sentence = new Sentence(aJcas, sentenceStart, tokenStart);
                sentence.addToIndexes();
                tokenStart++;
                sentenceStart = tokenStart;
                text.append("\n");
                continue;
            }
            if (line.startsWith("#text=")) {
                continue;
            }
            if (line.startsWith("#id=")) {
                continue;// it is a comment line
            }

            if (line.startsWith("#")) {
                StringTokenizer headerTk = new StringTokenizer(line, "#");
                while (headerTk.hasMoreTokens()) {
                    String layerNames = headerTk.nextToken().trim();
                    StringTokenizer layerTk = new StringTokenizer(layerNames, "|");

                    Set<Feature> features = new LinkedHashSet<Feature>();
                    String layerName = layerTk.nextToken().trim();
                    Type layer = CasUtil.getType(aJcas.getCas(), layerName);

                    while (layerTk.hasMoreTokens()) {
                        String ft = layerTk.nextToken().trim();
                        if (ft.startsWith("AttachTo=")) {
                            Type attachLayer = CasUtil.getType(aJcas.getCas(), ft.substring(9));
                            relationayers.put(layer, attachLayer);
                            columns++;
                            continue;
                        }
                        Feature feature = layer.getFeatureByBaseName(ft);
                        features.add(feature);
                        columns++;
                    }
                    spanLayers.put(layer, features);
                }
                continue;
            }

            int count = StringUtils.countMatches(line, "\t");

            if (columns != count) {
                throw new IOException(fileName + " This is not a valid TSV File. check this line: "
                        + line);
            }

            // adding tokens and sentence
            StringTokenizer lineTk = new StringTokenizer(line, "\t");
            String tokenNumberColumn = lineTk.nextToken();
            int tokenNumber = Integer.parseInt(tokenNumberColumn.split("-")[1]);
            String tokenColumn = lineTk.nextToken();
            Token token = new Token(aJcas, tokenStart, tokenStart + tokenColumn.length());
            token.addToIndexes();

            // adding the annotations
            importSpanAnnotation(aJcas, tokenStart, spanLayers, relationayers, annotations,
                    beginEndAnno, tokenAnnotations, relationTargets, lineTk, tokenColumn,
                    tokenNumberColumn);

            tokenStart = tokenStart + tokenColumn.length() + 1;
            text.append(tokenColumn + " ");
        }

        // create
        for (Type layer : relationayers.keySet()) {

            Feature dependentFeature = layer.getFeatureByBaseName("Dependent");
            Feature governorFeature = layer.getFeatureByBaseName("Governor");

            Map<String, List<String>> tokenIdMaps = relationTargets.get(layer);
            Map<String, List<AnnotationFS>> tokenAnnos = tokenAnnotations.get(relationayers
                    .get(layer));
            Map<String, List<AnnotationFS>> relationAnnos = tokenAnnotations.get(layer);
            for (String dependnetId : tokenIdMaps.keySet()) {
                int i = 0;
                for (String governorId : tokenIdMaps.get(dependnetId)) {

                    AnnotationFS relationAnno = relationAnnos.get(dependnetId).get(i);
                    AnnotationFS dependentAnno = tokenAnnos.get(dependnetId).get(0);
                    AnnotationFS governorAnno = tokenAnnos.get(governorId).get(0);

                    relationAnno.setFeatureValue(dependentFeature, dependentAnno);
                    relationAnno.setFeatureValue(governorFeature, governorAnno);
                    aJcas.addFsToIndexes(relationAnno);
                    i++;
                }

            }
        }
    }

    private void importSpanAnnotation(JCas aJcas, int aTokenStart, Map<Type, Set<Feature>> aLayers,
            Map<Type, Type> aRelationayers, Map<Type, Map<Integer, AnnotationFS>> aAnnotations,
            Map<Type, Map<Integer, String>> aBeginEndAnno,
            Map<Type, Map<String, List<AnnotationFS>>> aTokenAnnotations,
            Map<Type, Map<String, List<String>>> aRelationTargets, StringTokenizer lineTk,
            String aToken, String aTokenNumberColumn)
    {
        for (Type layer : aLayers.keySet()) {
            int lastIndex = 1;
            for (Feature feature : aLayers.get(layer)) {
                int index = 1;
                String multipleAnnotations = lineTk.nextToken();
                String relationTargetNumbers = null;
                if (aRelationayers.containsKey(layer)) {
                    relationTargetNumbers = lineTk.nextToken();
                }
                int i = 0;
                String[] relationTargets = null;
                if (relationTargetNumbers != null) {
                    relationTargets = relationTargetNumbers.split("\\|");
                }
                for (String annotation : multipleAnnotations.split("\\|")) {
                    // for annotations such as B_LOC|B-_|I_PER and the like
                    // O-_ is a position marker
                    if (annotation.equals("O-_") || annotation.equals("B-_")
                            || annotation.equals("I-_")) {
                        index++;
                    }
                    else if (annotation.startsWith("B-")) {
                        boolean isNewAnnotation = true;
                        Map<Integer, AnnotationFS> indexedAnnos = aAnnotations.get(layer);
                        Map<Integer, String> indexedBeginEndAnnos = aBeginEndAnno.get(layer);
                        AnnotationFS newAnnotation;

                        if (indexedAnnos == null) {
                            newAnnotation = aJcas.getCas().createAnnotation(layer, aTokenStart,
                                    aTokenStart + aToken.length());
                            indexedAnnos = new LinkedHashMap<Integer, AnnotationFS>();
                            indexedBeginEndAnnos = new LinkedHashMap<Integer, String>();
                        }
                        else if (indexedAnnos.get(index) == null) {
                            newAnnotation = aJcas.getCas().createAnnotation(layer, aTokenStart,
                                    aTokenStart + aToken.length());
                        }
                        else if (indexedAnnos.get(index) != null
                                && indexedBeginEndAnnos.get(index).equals("E-")) {
                            newAnnotation = aJcas.getCas().createAnnotation(layer, aTokenStart,
                                    aTokenStart + aToken.length());
                        }
                        // B-LOC I-LOC B-LOC - the last B-LOC is a new annotation
                        else if (indexedBeginEndAnnos.get(index).equals("I-")) {
                            newAnnotation = aJcas.getCas().createAnnotation(layer, aTokenStart,
                                    aTokenStart + aToken.length());
                        }
                        else {
                            newAnnotation = indexedAnnos.get(index);
                            isNewAnnotation = false;
                        }
                        // remove prefixes such as B-/I- before creating the annotation
                        newAnnotation.setFeatureValueFromString(feature, (annotation.substring(2)));
                        aJcas.addFsToIndexes(newAnnotation);
                        indexedAnnos.put(index, newAnnotation);
                        indexedBeginEndAnnos.put(index, "B-");
                        aAnnotations.put(layer, indexedAnnos);

                        if (aRelationayers.containsKey(layer)) {
                            Map<String, List<String>> targets = aRelationTargets.get(layer);
                            if (targets == null) {
                                List<String> governors = new ArrayList<String>();
                                governors.add(relationTargets[i]);
                                targets = new HashMap<String, List<String>>();
                                targets.put(aTokenNumberColumn, governors);
                                i++;
                                aRelationTargets.put(layer, targets);
                            }
                            else {
                                List<String> governors = targets.get(aTokenNumberColumn);
                                if (governors == null) {
                                    governors = new ArrayList<String>();
                                }
                                governors.add(relationTargets[i]);
                                targets.put(aTokenNumberColumn, governors);
                                i++;
                                aRelationTargets.put(layer, targets);
                            }
                        }

                        Map<String, List<AnnotationFS>> tokenAnnotations = aTokenAnnotations
                                .get(layer);
                        if (isNewAnnotation) {
                            if (tokenAnnotations == null) {
                                tokenAnnotations = new HashMap<String, List<AnnotationFS>>();
                            }
                            List<AnnotationFS> relAnnos = tokenAnnotations.get(aTokenNumberColumn);
                            if (relAnnos == null) {
                                relAnnos = new ArrayList<AnnotationFS>();
                            }
                            relAnnos.add(newAnnotation);
                            tokenAnnotations.put(aTokenNumberColumn, relAnnos);
                            aTokenAnnotations.put(layer, tokenAnnotations);
                        }

                        aBeginEndAnno.put(layer, indexedBeginEndAnnos);
                        index++;
                    }

                    else if (annotation.startsWith("I-")) {
                        // beginEndAnnotation.put(layer, "I-");

                        Map<Integer, String> indexedBeginEndAnnos = aBeginEndAnno.get(layer);
                        indexedBeginEndAnnos.put(index, "I-");
                        aBeginEndAnno.put(layer, indexedBeginEndAnnos);

                        Map<Integer, AnnotationFS> indexedAnnos = aAnnotations.get(layer);
                        AnnotationFS newAnnotation = indexedAnnos.get(index);
                        ((Annotation) newAnnotation).setEnd(aTokenStart + aToken.length());

                        aJcas.addFsToIndexes(newAnnotation);
                        index++;
                    }
                    else {
                        aAnnotations.put(layer, null);
                        index++;
                    }
                }
                lastIndex = index-1;
            }
            // tokens annotated as B-X B-X, no I means it is end by itself
            if (aBeginEndAnno.get(layer) != null && aBeginEndAnno.get(layer).get(lastIndex) != null
                    && aBeginEndAnno.get(layer).get(lastIndex).equals("B-")) {
                aBeginEndAnno.get(layer).put(lastIndex, "E-");
            }
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
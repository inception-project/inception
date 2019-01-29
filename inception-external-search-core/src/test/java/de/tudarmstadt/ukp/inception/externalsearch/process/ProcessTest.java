/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.externalsearch.process;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newDirectoryStream;
import static java.nio.file.Files.readAllBytes;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.uima.UIMAFramework.getResourceSpecifierFactory;
import static org.apache.uima.cas.CAS.TYPE_NAME_ANNOTATION;
import static org.apache.uima.cas.CAS.TYPE_NAME_DOUBLE;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.JCasFactory.createJCas;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.text.similarity.CosineDistance;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;

public class ProcessTest
{
    public static String TYPE_NAME_UNIT = "Unit";
    public static String FEATURE_NAME_SCORE = "score";
    
    @Test
    public void test() throws Exception
    {
        // Parameters
        String queryword = "test";
        
        // Set up custom type system
        TypeSystemDescription customTypes = getResourceSpecifierFactory()
                .createTypeSystemDescription();
        TypeDescription tdUnit = customTypes.addType(TYPE_NAME_UNIT, "",
                TYPE_NAME_ANNOTATION);
        tdUnit.addFeature(FEATURE_NAME_SCORE, "", TYPE_NAME_DOUBLE);

        // Set up processing components
        AnalysisEngine splitter = createEngine(BreakIteratorSegmenter.class);
        AnalysisEngine marker = createEngine(UnitByQueryWordAnnotator.class, 
                UnitByQueryWordAnnotator.PARAM_QUERY_WORD, queryword);
        AnalysisEngine scorer = createEngine(GoodnessScoreAnnotator.class, 
                UnitByQueryWordAnnotator.PARAM_QUERY_WORD, queryword);
        
        // Create CAS
        JCas doc = createJCas(mergeTypeSystems(asList(customTypes, createTypeSystemDescription())));

        // Process text files
        List<Triple<String, Double, String>> relevantSentences = new ArrayList<>();
        DirectoryStream<Path> directoryStream = newDirectoryStream(
                Paths.get("src/test/resources/texts"));
        for (Path p : directoryStream) {
            // Clear contents so we can process the next file
            doc.reset();
            doc.setDocumentText(new String(readAllBytes(p), UTF_8));
            
            // Annotate sentences and tokens
            splitter.process(doc);

            // Annotate sentences containing the query word
            marker.process(doc);
            
            // Annotate units with goodness score
            scorer.process(doc);
    
            // Extract sentences
            Type tUnit = doc.getTypeSystem().getType(TYPE_NAME_UNIT);
            Feature fScore = tUnit.getFeatureByBaseName(FEATURE_NAME_SCORE);
            for (AnnotationFS unit : CasUtil.select(doc.getCas(), tUnit)) {
                relevantSentences.add(Triple.of(
                        unit.getCoveredText(),
                        unit.getDoubleValue(fScore),
                        p.toString()));
            }
        }
        
        // Assertions checking that proper data has been extracted
        assertThat(relevantSentences).hasSize(5);
        assertThat(relevantSentences).extracting(Triple::getMiddle).allMatch(score -> score > 0.0);
    
        cluster(relevantSentences);
    }
    
    // One giant cluster could be generated, because bestDistance can make it grow element-wise.
    // Should use average!
    private List<Set<Triple<String, Double, String>>> cluster(
            List<Triple<String, Double, String>> sentences)
    {
        CosineDistance cd = new CosineDistance();
        double clusterDistanceTolerance = 0.3;
        List<Set<Triple<String, Double, String>>> sentenceClusters = new ArrayList<>();
        for (Triple<String, Double, String> sentence: sentences)
        {
            if (sentenceClusters.size() == 0)
            {
                Set<Triple<String, Double, String>> firstCluster = new HashSet<>();
                firstCluster.add(sentence);
                sentenceClusters.add(firstCluster);
            }
            else
            {
                double bestDistance = Double.MAX_VALUE;
                Set<Triple<String, Double, String>> bestCluster = new HashSet<>();
                for (Set<Triple<String, Double, String>> cluster: sentenceClusters) {
                    List<Double> cmpDistances = new ArrayList<>();
                    for (Triple<String, Double, String> compareSentence : cluster) {
                        cmpDistances.add(cd.apply(compareSentence.getLeft(), sentence.getLeft()));
                    }
                    bestDistance = Math.min(bestDistance, Collections.min(cmpDistances));
                    bestCluster = cluster;
                }
                
                if (bestDistance < clusterDistanceTolerance)
                {
                    bestCluster.add(sentence);
                }
                else
                {
                    Set<Triple<String, Double, String>> newCluster = new HashSet<>();
                    newCluster.add(sentence);
                    sentenceClusters.add(newCluster);
                }
            }
        }
        return sentenceClusters;
    }
    
    public static class UnitByQueryWordAnnotator
        extends JCasAnnotator_ImplBase
    {
        public static final String PARAM_QUERY_WORD = "queryWord";
        @ConfigurationParameter(name = PARAM_QUERY_WORD, mandatory = true)
        private String queryWord;

        @Override
        public void process(JCas aJCas) throws AnalysisEngineProcessException
        {
            Type tUnit = aJCas.getTypeSystem().getType(TYPE_NAME_UNIT);
            for (Sentence sentence : JCasUtil.select(aJCas, Sentence.class)) {
                if (sentence.getCoveredText().contains(queryWord)) {
                    AnnotationFS unit = aJCas.getCas().createAnnotation(tUnit,
                                sentence.getBegin(), sentence.getEnd());
                    aJCas.getCas().addFsToIndexes(unit);
                }
            }
        }
    }
    
    public static class GoodnessScoreAnnotator
        extends JCasAnnotator_ImplBase
    {
        public static final String PARAM_QUERY_WORD = "queryWord";
        @ConfigurationParameter(name = PARAM_QUERY_WORD, mandatory = true)
        private String queryWord;
        
        @Override
        public void process(JCas aJCas) throws AnalysisEngineProcessException
        {
            Type tUnit = aJCas.getTypeSystem().getType(TYPE_NAME_UNIT);
            Feature fScore = tUnit.getFeatureByBaseName(FEATURE_NAME_SCORE);
            for (AnnotationFS unit : CasUtil.select(aJCas.getCas(), tUnit)) {
                List<String> tokens = JCasUtil.selectCovered(Token.class, unit).stream()
                        .map(AnnotationFS::getCoveredText).collect(toList());
                unit.setDoubleValue(fScore, getFrequencyScore(queryWord, tokens));
            }
        }
    
        // should penalize sentences more than 25 and less than 10 words
        private double getLengthScore(List<String> aTokens)
        {
            int sentenceLength = aTokens.size();
            if (sentenceLength >= 10 && sentenceLength <= 25)
            {
                return 1.0;
            }
            else
            {
                return 0.0;
            }
        }

        // should penalize sentences with multiple occurrences of the query
        private double getFrequencyScore(String aQueryWord, List<String> aTokens)
        {
            int queryFrequency = Collections.frequency(aTokens, aQueryWord);
            if (queryFrequency == 0)
            {
                return 0.0;
            }
            else {
                return 1.0 / (Collections.frequency(aTokens, aQueryWord));
            }
        }
    }
}

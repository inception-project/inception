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
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparingInt;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.uima.fit.util.CasUtil.getAnnotationType;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.AnnotatedTokenPair;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext.Key;
import de.tudarmstadt.ukp.inception.recommendation.api.type.PredictedSpan;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.model.GazeteerEntry;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.trie.Trie;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.trie.WhitespaceNormalizingSanitizer;

public class StringMatchingRecommender
    implements RecommendationEngine
{
    public static final Key<Trie<DictEntry>> KEY_MODEL = new Key<>("model");

    private static final String UNKNOWN_LABEL = "unknown";

    private static final String NO_LABEL = "O";


    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String layerName;
    private final String featureName;
    private final int maxRecommendations;
    private final StringMatchingRecommenderTraits traits;
    
    private List<GazeteerEntry> pretrainData = emptyList();

    public StringMatchingRecommender(Recommender aRecommender,
            StringMatchingRecommenderTraits aTraits)
    {
        layerName = aRecommender.getLayer().getName();
        featureName = aRecommender.getFeature().getName();
        maxRecommendations = aRecommender.getMaxRecommendations();
        traits = aTraits;
    }

    public void pretrain(List<GazeteerEntry> aData)
    {
        if (aData != null) {
            pretrainData = aData;
        }
        else {
            pretrainData = emptyList();
        }
    }

    private <T> Trie<T> createTrie()
    {
        return new Trie<>(WhitespaceNormalizingSanitizer.factory());
    }
    
    @Override
    public void train(RecommenderContext aContext, List<CAS> aCasses)
    {
        Trie<DictEntry> dict = createTrie();
        
        // Load the pre-train data into the trie before learning from the annotated data
        for (GazeteerEntry entry : pretrainData) {
            learn(dict, entry.text, entry.label);
        }
        
        // Learn from the annotated data
        for (CAS cas : aCasses) {
            Type annotationType = getType(cas, layerName);
            Feature labelFeature = annotationType.getFeatureByBaseName(featureName);

            for (AnnotationFS ann : select(cas, annotationType)) {
                learn(dict, ann.getCoveredText(), ann.getFeatureValueAsString(labelFeature));
            }
        }
        
        aContext.put(KEY_MODEL, dict);
        aContext.markAsReadyForPrediction();
        
        log.debug("Learned dictionary model with {} entries", dict.size());
    }

    @Override
    public void predict(RecommenderContext aContext, CAS aCas) throws RecommendationException
    {
        Trie<DictEntry> dict = aContext.get(KEY_MODEL).orElseThrow(() -> 
                new RecommendationException("Key [" + KEY_MODEL + "] not found in context"));
        
        Type predictionType = getAnnotationType(aCas, PredictedSpan.class);
        Feature confidenceFeature = predictionType.getFeatureByBaseName("score");
        Feature labelFeature = predictionType.getFeatureByBaseName("label");

        List<Sample> data = predict(0, aCas, dict);
        
        for (Sample sample : data) {
            for (Span span : sample.getSpans()) {
                AnnotationFS annotation = aCas.createAnnotation(predictionType, span.getBegin(),
                        span.getEnd());
                annotation.setDoubleValue(confidenceFeature, span.getScore());
                annotation.setStringValue(labelFeature, span.getLabel());
                aCas.addFsToIndexes(annotation);
            }
        }
    }

    private List<Sample> predict(int aDocNo, CAS aCas, Trie<DictEntry> aDict)
    {
        Type sentenceType = getType(aCas, Sentence.class);
        Type tokenType = getType(aCas, Token.class);

        List<Sample> data = new ArrayList<>();
        String text = aCas.getDocumentText();
        for (AnnotationFS sentence : select(aCas, sentenceType)) {
            List<Span> spans = new ArrayList<>();
            
            Collection<AnnotationFS> tokens = selectCovered(tokenType, sentence);
            for (AnnotationFS token : tokens) {
                Trie<DictEntry>.Node node = aDict.getNode(text, token.getBegin());
                if (node != null) {
                    int begin = token.getBegin();
                    int end = begin + node.level;
                    
                    // Need to check that the match actually ends at a token boundary!
                    if (tokens.stream().filter(t -> t.getEnd() == end).findAny().isPresent()) {
                        for (LabelStats lc : node.value.getBest(maxRecommendations)) {
                            String label = lc.getLabel();
                            // check instance equality to avoid collision with user labels
                            if (label == UNKNOWN_LABEL) {
                                label = null;
                            }
                            spans.add(new Span(begin, end, text.substring(begin, end), label,
                                    lc.getRelFreq()));
                        }
                    }
                }
            }
            
            data.add(new Sample(aDocNo, sentence.getBegin(), sentence.getEnd(),
                    sentence.getCoveredText(), tokens, spans));
        }
        
        return data;
    }

    @Override
    public EvaluationResult evaluate(List<CAS> aCasses, DataSplitter aDataSplitter)
    {
        List<Sample> data = extractData(aCasses, layerName, featureName);
        List<Sample> trainingSet = new ArrayList<>();
        List<Sample> testSet = new ArrayList<>();

        for (Sample sample : data) {
            switch (aDataSplitter.getTargetSet(sample)) {
            case TRAIN:
                trainingSet.add(sample);
                break;
            case TEST:
                testSet.add(sample);
                break;
            default:
                // Do nothing
                break;
            }
        }

        int trainingSetSize = trainingSet.size();
        int testSetSize = testSet.size();

        long trainingSetLabeledSamplesCount = trainingSet.stream()
                .filter(sample -> !sample.getSpans().isEmpty()).count();

        long testSetLabeledSamplesCount = testSet.stream()
                .filter(sample -> !sample.getSpans().isEmpty()).count();

        if (trainingSetLabeledSamplesCount < 2 || testSetLabeledSamplesCount < 2) {
            log.info(
                    "Not enough labeled data: training set [{}] items ([{}] labeled), test set [{}] ([{}] labeled) of total [{}]",
                    trainingSet.size(), trainingSetLabeledSamplesCount, testSet.size(),
                    testSetLabeledSamplesCount, data.size());
            EvaluationResult result = new EvaluationResult(null, null, trainingSetSize,
                    testSetSize);
            result.setEvaluationSkipped(true);
            return result;
        }

        log.info(
                "Training on [{}] items ([{}] labeled), predicting on [{}] ([{}] labeled) of total [{}]",
                trainingSet.size(), trainingSetLabeledSamplesCount, testSet.size(),
                testSetLabeledSamplesCount, data.size());

        // Train
        Trie<DictEntry> dict = createTrie();
        for (Sample sample : trainingSet) {
            for (Span span : sample.getSpans()) {
                learn(dict, span.getText(), span.getLabel());
            }
        }

        // Predict
        List<AnnotatedTokenPair> predictions = new ArrayList<>();
        for (Sample sample : testSet) {

            for (TokenSpan token : sample.getTokens()) {
                Trie<DictEntry>.Node node = dict.getNode(sample.getText(),
                        token.getBegin() - sample.getBegin());
                int begin = token.getBegin();
                int end = token.getEnd();

                String predictedLabel = NO_LABEL;
                if (node != null && sample.hasTokenEndingAt(token.getBegin() + node.level)) {
                    List<LabelStats> labelStats = node.value.getBest(1);
                    if (!labelStats.isEmpty())
                        predictedLabel = labelStats.get(0).getLabel();
                }
                Optional<Span> coveringSpan = sample.getCoveringSpan(begin, end);
                if (coveringSpan.isPresent()) {
                    String goldLabel = coveringSpan.get().getLabel();
                    predictions.add(new AnnotatedTokenPair(goldLabel, predictedLabel));
                }
            }
        }

        return new EvaluationResult(UNKNOWN_LABEL, predictions.stream(), trainingSetSize,
                testSetSize);
    }

    
    private void learn(Trie<DictEntry> aDict, String aText, String aLabel)
    {
        String label = isBlank(aLabel) ? UNKNOWN_LABEL : aLabel;

        DictEntry entry = aDict.get(aText);
        if (entry == null) {
            entry = new DictEntry(aText);
            aDict.put(aText, entry);
        }

        entry.put(label);

    }
    
    private List<Sample> extractData(List<CAS> aCasses, String aLayerName, String aFeatureName)
    {
        long start = System.currentTimeMillis();
        
        List<Sample> data = new ArrayList<>();
        
        int docNo = 0;
        for (CAS cas : aCasses) {
            Type sentenceType = getType(cas, Sentence.class);
            Type tokenType = getType(cas, Token.class);
            Type annotationType = getType(cas, aLayerName);
            Feature labelFeature = annotationType.getFeatureByBaseName(aFeatureName);
            
            for (AnnotationFS sentence : select(cas, sentenceType)) {
                List<Span> spans = new ArrayList<>();
                
                for (AnnotationFS annotation : selectCovered(annotationType, sentence)) {
                    String label = annotation.getFeatureValueAsString(labelFeature);
                    if (isNotEmpty(label)) {
                        spans.add(new Span(annotation.getBegin(), annotation.getEnd(),
                                annotation.getCoveredText(),
                                annotation.getFeatureValueAsString(labelFeature), -1.0));
                    }
                }
                
                Collection<AnnotationFS> tokens = selectCovered(tokenType, sentence);

                data.add(new Sample(docNo, sentence.getBegin(), sentence.getEnd(),
                        sentence.getCoveredText(), tokens, spans));
            }
            
            docNo++;
        }
        
        log.trace("Extracting data took {}ms", System.currentTimeMillis() - start);
        
        return data;
    }
    
    private static class Sample
    {
        private final int docNo;
        private final int begin;
        private final int end;
        private final String text;
        private final List<TokenSpan> tokens;
        private final List<Span> spans;

        public Sample(int aDocNo, int aBegin, int aEnd, String aText,
                Collection<AnnotationFS> aTokens, Collection<Span> aSpans)
        {
            super();
            docNo = aDocNo;
            begin = aBegin;
            end = aEnd;
            text = aText;
            tokens = aTokens.stream().map(fs -> new TokenSpan(fs.getBegin(), fs.getEnd()))
                    .collect(Collectors.toList());
            spans = asList(aSpans.toArray(new Span[aSpans.size()]));
        }
        
        public Optional<Span> getCoveringSpan(int aBegin, int aEnd)
        {
            return spans.stream().filter(s -> (s.getBegin() <= aBegin && s.getEnd() >= aEnd))
                    .findFirst();
        }

        public int getDocNo()
        {
            return docNo;
        }
        
        public int getBegin()
        {
            return begin;
        }
        
        public int getEnd()
        {
            return end;
        }
        
        public int getLength()
        {
            return end - begin;
        }
        
        public String getText()
        {
            return text;
        }
        
        public List<TokenSpan> getTokens()
        {
            return tokens;
        }
        
        public List<Span> getSpans()
        {
            return spans;
        }
        
        public boolean hasTokenEndingAt(int aOffset)
        {
            return tokens.stream().filter(t -> t.end == aOffset).findAny().isPresent();
        }
    }

    private static class TokenSpan
    {
        private final int begin;
        private final int end;

        public TokenSpan(int aBegin, int aEnd)
        {
            super();
            begin = aBegin;
            end = aEnd;
        }

        public int getBegin()
        {
            return begin;
        }

        public int getEnd()
        {
            return end;
        }
    }
    
    private static class LabelStats
    {
        private final String label;
        private final int count;
        private final double relFreq;

        public LabelStats(String aLabel, int aCount, double aRelFreq)
        {
            super();
            label = aLabel;
            count = aCount;
            relFreq = aRelFreq;
        }

        /**
         * The label (e.g. NN, PER, OTH, etc.)
         */
        public String getLabel()
        {
            return label;
        }
        
        /**
         * How often the label was observed.
         */
        public int getCount()
        {
            return count;
        }

        /**
         * How often the label was observed in relation to the total number of observations of the
         * mention.
         */
        public double getRelFreq()
        {
            return relFreq;
        }
    }

    private static class Span
    {
        private final int begin;
        private final int end;
        private final String text;
        private final String label;
        private final double score;
        
        public Span(int aBegin, int aEnd, String aText, String aLabel, double aScore)
        {
            super();
            begin = aBegin;
            end = aEnd;
            text = aText;
            label = aLabel;
            score = aScore;
        }
        
        public int getBegin()
        {
            return begin;
        }
        
        public int getEnd()
        {
            return end;
        }
        
        public String getText()
        {
            return text;
        }
        
        public String getLabel()
        {
            return label;
        }
        
        public double getScore()
        {
            return score;
        }
    }
    
    private static class DictEntry
    {
        private String key;
        private String[] labels;
        private int[] counts;
        
        public DictEntry(String aKey)
        {
            key = aKey;
        }
        
        public void put(String aLabel)
        {
            // No data yet - create it
            if (labels == null) {
                labels = new String[] { aLabel };
                counts = new int[] { 1 };
                return;
            }
            
            // Data is available
            int i = asList(labels).indexOf(aLabel);
            
            // Label already exists
            if (i != -1) {
                counts[i]++;
                return;
            }
            
            // Label does not exist yet
            String[] newLabels = new String[labels.length + 1];
            System.arraycopy(labels, 0, newLabels, 0, labels.length);
            labels = newLabels;
            
            int[] newCounts = new int[counts.length + 1];
            System.arraycopy(counts, 0, newCounts, 0, counts.length);
            counts = newCounts;
            
            labels[labels.length - 1] = aLabel;
            counts[counts.length - 1] = 1;
        }
        
        public List<LabelStats> getBest(int aN)
        {
            int total = IntStream.of(counts).sum();
            
            List<LabelStats> best = new ArrayList<>();
            for (int i = 0; i < labels.length; i++) {
                best.add(new LabelStats(labels[i], counts[i], (double) counts[i] / (double) total));
            }
            
            return best.stream()
                    .sorted(comparingInt(LabelStats::getCount).reversed())
                    .limit(aN)
                    .collect(Collectors.toList());
        }
        
        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            builder.append("DictEntry [key=");
            builder.append(key);
            builder.append("]");
            return builder.toString();
        }
    }
}

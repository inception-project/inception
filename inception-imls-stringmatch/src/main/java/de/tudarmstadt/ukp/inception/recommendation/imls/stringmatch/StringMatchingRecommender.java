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
import static org.apache.uima.fit.util.CasUtil.getAnnotationType;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.dkpro.statistics.agreement.unitizing.KrippendorffAlphaUnitizingAgreement;
import org.dkpro.statistics.agreement.unitizing.UnitizingAnnotationStudy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.type.PredictedSpan;
import de.tudarmstadt.ukp.inception.recommendation.api.v2.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.v2.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.v2.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.api.v2.RecommenderContext.Key;

public class StringMatchingRecommender
    implements RecommendationEngine
{
    public static final Key<Trie<DictEntry>> KEY_MODEL = new Key<>("model");

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String layerName;
    private final String featureName;
    private final StringMatchingRecommenderTraits traits;

    public StringMatchingRecommender(Recommender aRecommender,
            StringMatchingRecommenderTraits aTraits)
    {
        layerName = aRecommender.getLayer().getName();
        featureName = aRecommender.getFeature();
        traits = aTraits;
    }

    @Override
    public void train(RecommenderContext aContext, List<CAS> aCasses)
    {
        Trie<DictEntry> dict = new Trie<>();
        
        for (CAS cas : aCasses) {
            Type annotationType = getType(cas, layerName);
            Feature labelFeature = annotationType.getFeatureByBaseName(featureName);

            for (AnnotationFS ann : select(cas, annotationType)) {
                learn(dict, ann.getCoveredText(), ann.getFeatureValueAsString(labelFeature));
            }
        }
        
        aContext.put(KEY_MODEL, dict);
    }

    @Override
    public void predict(RecommenderContext aContext, CAS aCas)
    {
        Type predictionType = getAnnotationType(aCas, PredictedSpan.class);
        Feature confidenceFeature = predictionType.getFeatureByBaseName("score");
        Feature labelFeature = predictionType.getFeatureByBaseName("label");
        
        Trie<DictEntry> dict = aContext.get(KEY_MODEL);

        List<Sample> data = predict(0, aCas, dict);
        
        for (Sample sample : data) {
            for (Span span : sample.getSpans()) {
                AnnotationFS annotation = aCas.createAnnotation(predictionType, span.getBegin(),
                        span.getEnd());
                // annotation.setDoubleValue(confidenceFeature, prediction.getProb());
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
                        spans.add(new Span(begin, end, text.substring(begin, end),
                                node.value.getBestLabel()));
                    }
                }
            }
            
            data.add(new Sample(aDocNo, sentence.getBegin(), sentence.getEnd(),
                    sentence.getCoveredText(), tokens, spans));
        }
        
        return data;
    }

    @Override
    public double evaluate(List<CAS> aCasses, DataSplitter aDataSplitter)
    {
        List<Sample> data = extractData(aCasses, layerName, featureName);
        List<Sample> trainingSet = new ArrayList<>();
        List<Sample> testSet = new ArrayList<>();

        // For the DKPro Statistics evaluation study, we need to define a continuum over the data.
        // We do this in terms of character positions which are aggregated over all samples in the
        // test set. Thus, the continum size is equal to the sum of the length of all samples in the
        // test set.
        int testSetContinuumSize = 0;
        
        for (Sample sample : data) {
            switch (aDataSplitter.getTargetSet(sample)) {
            case TRAIN:
                trainingSet.add(sample);
                break;
            case TEST:
                testSet.add(sample);
                testSetContinuumSize += sample.getText().length();
                break;
            default:
                // Do nothing
                break;
            }            
        }

        if (trainingSet.size() < 2 || testSet.size() < 2) {
            log.info("Not enough data to evaluate, skipping!");
            return 0.0;
        }

        log.info("Training on [{}] items, predicting on [{}] of total [{}]", trainingSet.size(),
                testSet.size(), data.size());        
            
        // Train
        Trie<DictEntry> dict = new Trie<>();
        for (Sample sample : trainingSet) {
            for (Span span : sample.getSpans()) {
                learn(dict, span.getText(), span.getLabel());            }
        }

        // Predict
        List<Sample> actualData = new ArrayList<>();
        for (Sample sample : testSet) {
            List<Span> spans = new ArrayList<>();
            
            for (TokenSpan token : sample.getTokens()) {
                Trie<DictEntry>.Node node = dict.getNode(sample.getText(),
                        token.getBegin() - sample.getBegin());
                if (node != null) {
                    int begin = token.getBegin();
                    int end = token.getBegin() + node.level;
                    
                    // Need to check that the match actually ends at a token boundary!
                    if (sample.hasTokenEndingAt(end)) {
                        spans.add(new Span(begin, end, sample.getText()
                                .substring(begin - sample.getBegin(), end - sample.getBegin()),
                                node.value.getBestLabel()));
                    }
                }
            }
            
            actualData.add(new Sample(sample, spans));
        }
        
        // Evaluate
        UnitizingAnnotationStudy study = new UnitizingAnnotationStudy(2, 0, testSetContinuumSize);
        
        // Add reference data to the study
        addDataToStudy(testSet, study, 0);
        
        // Add actual data to the study
        addDataToStudy(actualData, study, 1);

        return new KrippendorffAlphaUnitizingAgreement(study).calculateAgreement();
    }
    
    private void addDataToStudy(Collection<Sample> aData, UnitizingAnnotationStudy aStudy,
            int aAnnotatorId)
    {
        // Offset of the current sample within the evaluation continuum
        int offset = 0;
        
        for (Sample sample : aData) {
            // Add all labeled spans to the study
            for (Span span : sample.getSpans()) {
                // Length of the labeled span
                int length = span.getEnd() - span.getBegin();
                
                // Begin offset of the span within the current sample
                int beginInSample = span.getBegin() - sample.getBegin();
                
                aStudy.addUnit(offset + beginInSample, length, aAnnotatorId, span.getLabel());
            }
            
            // Shift within continuum
            offset += sample.getLength();
        }
    }
    
    private void learn(Trie<DictEntry> aDict, String aText, String aLabel) {
        DictEntry entry = aDict.get(aText);
        if (entry == null) {
            entry = new DictEntry(aText);
            aDict.put(aText, entry);
        }
        
        entry.put(aLabel);
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
                    spans.add(new Span(annotation.getBegin(), annotation.getEnd(),
                            annotation.getCoveredText(),
                            annotation.getFeatureValueAsString(labelFeature)));
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

        public Sample(Sample aSample, Collection<Span> aSpans)
        {
            docNo = aSample.getDocNo();
            begin = aSample.getBegin();
            end = aSample.getEnd();
            text = aSample.getText();
            tokens = aSample.getTokens();
            spans = asList(aSpans.toArray(new Span[aSpans.size()]));
        }

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

    private static class Span
    {
        private final int begin;
        private final int end;
        private final String text;
        private final String label;
        
        public Span(int aBegin, int aEnd, String aText, String aLabel)
        {
            super();
            begin = aBegin;
            end = aEnd;
            text = aText;
            label = aLabel;
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
        
        public String getBestLabel()
        {
            int maxIndex = 0;
            int maxValue = 0;
            for (int i = 0; i < counts.length; i++) {
                if (counts[i] > maxValue) {
                    maxValue = counts[i];
                    maxIndex = i;
                }
            }
            
            return labels[maxIndex];
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

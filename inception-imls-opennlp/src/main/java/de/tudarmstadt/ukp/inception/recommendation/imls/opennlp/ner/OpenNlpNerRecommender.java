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
package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.ner;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.indexCovered;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.LabelPair;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineCapability;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext.Key;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import opennlp.tools.ml.BeamSearch;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.NameSample;
import opennlp.tools.namefind.TokenNameFinderFactory;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.Span;
import opennlp.tools.util.TrainingParameters;

public class OpenNlpNerRecommender
    extends RecommendationEngine
{
    public static final Key<TokenNameFinderModel> KEY_MODEL = new Key<>("opennlp_ner_model");
    private static final Logger LOG = LoggerFactory.getLogger(OpenNlpNerRecommender.class);
    
    private static final String NO_NE_TAG = "O";

    private final OpenNlpNerRecommenderTraits traits;

    public OpenNlpNerRecommender(Recommender aRecommender, OpenNlpNerRecommenderTraits aTraits)
    {
        super(aRecommender);
        
        traits = aTraits;
    }

    @Override
    public boolean isReadyForPrediction(RecommenderContext aContext)
    {
        return aContext.get(KEY_MODEL).map(Objects::nonNull).orElse(false);
    }
    
    @Override
    public void train(RecommenderContext aContext, List<CAS> aCasses)
        throws RecommendationException
    {
        List<NameSample> nameSamples = extractNameSamples(aCasses);
        
        if (nameSamples.size() < 2) {
            LOG.info("Not enough training data: [{}] items", nameSamples.size());
            return;
        }
        
        // The beam size controls how many results are returned at most. But even if the user
        // requests only few results, we always use at least the default bean size recommended by
        // OpenNLP
        int beamSize = Math.max(maxRecommendations, NameFinderME.DEFAULT_BEAM_SIZE);

        TrainingParameters params = traits.getParameters();
        params.put(BeamSearch.BEAM_SIZE_PARAMETER, Integer.toString(beamSize));
        
        TokenNameFinderModel model = train(nameSamples, params);
        
        aContext.put(KEY_MODEL, model);
    }
    
    @Override
    public RecommendationEngineCapability getTrainingCapability() 
    {
        return RecommendationEngineCapability.TRAINING_REQUIRED;
    }

    @Override
    public void predict(RecommenderContext aContext, CAS aCas) throws RecommendationException
    {
        TokenNameFinderModel model = aContext.get(KEY_MODEL).orElseThrow(() -> 
                new RecommendationException("Key [" + KEY_MODEL + "] not found in context"));
        
        NameFinderME finder = new NameFinderME(model);

        Type sentenceType = getType(aCas, Sentence.class);
        Type tokenType = getType(aCas, Token.class);
        Type predictedType = getPredictedType(aCas);

        Feature predictedFeature = getPredictedFeature(aCas);
        Feature isPredictionFeature = getIsPredictionFeature(aCas);
        Feature scoreFeature = getScoreFeature(aCas);

        int predictionCount = 0;
        for (AnnotationFS sentence : select(aCas, sentenceType)) {
            if (predictionCount >= traits.getPredictionLimit()) {
                break;
            }
            predictionCount++;
            
            List<AnnotationFS> tokenAnnotations = selectCovered(tokenType, sentence);
            String[] tokens = tokenAnnotations.stream()
                .map(AnnotationFS::getCoveredText)
                .toArray(String[]::new);

            for (Span prediction : finder.find(tokens)) {
                String label = prediction.getType();
                if (NameSample.DEFAULT_TYPE.equals(label)) {
                    continue;
                }
                int begin = tokenAnnotations.get(prediction.getStart()).getBegin();
                int end = tokenAnnotations.get(prediction.getEnd() - 1).getEnd();
                AnnotationFS annotation = aCas.createAnnotation(predictedType, begin, end);
                annotation.setStringValue(predictedFeature, label);
                annotation.setDoubleValue(scoreFeature, prediction.getProb());
                annotation.setBooleanValue(isPredictionFeature, true);

                aCas.addFsToIndexes(annotation);
            }
        }
    }

    @Override
    public EvaluationResult evaluate(List<CAS> aCasses, DataSplitter aDataSplitter)
        throws RecommendationException
    {
        List<NameSample> data = extractNameSamples(aCasses);
        List<NameSample> trainingSet = new ArrayList<>();
        List<NameSample> testSet = new ArrayList<>();

        for (NameSample nameSample : data) {
            switch (aDataSplitter.getTargetSet(nameSample)) {
            case TRAIN:
                trainingSet.add(nameSample);
                break;
            case TEST:
                testSet.add(nameSample);
                break;
            default:
                // Do nothing
                break;
            }            
        }
        
        int testSetSize = testSet.size();
        int trainingSetSize = trainingSet.size();
        double overallTrainingSize = data.size() - testSetSize;
        double trainRatio = (overallTrainingSize > 0) ? trainingSetSize / overallTrainingSize : 0.0;

        if (trainingSetSize < 2 || testSetSize < 2) {
            String info = String.format(
                    "Not enough evaluation data: training set [%s] items, test set [%s] of total [%s]",
                    trainingSetSize, testSetSize, data.size());
            LOG.info(info);
            
            EvaluationResult result = new EvaluationResult(trainingSetSize,
                    testSetSize, trainRatio);
            result.setEvaluationSkipped(true);
            result.setErrorMsg(info);
            return result;
        }

        LOG.info("Training on [{}] items, predicting on [{}] of total [{}]", trainingSet.size(),
                testSet.size(), data.size());

        // Train model
        TokenNameFinderModel model = train(trainingSet, traits.getParameters());
        NameFinderME nameFinder = new NameFinderME(model);

        // Evaluate
        List<LabelPair> labelPairs = new ArrayList<>();
        for (NameSample sample : testSet) {
            // clear adaptive data from feature generators if necessary
            if (sample.isClearAdaptiveDataSet()) {
                nameFinder.clearAdaptiveData();
            }

            // Span contains one NE, Array of them all in one sentence
            String[] sentence = sample.getSentence();
            Span[] predictedNames = nameFinder.find(sentence);
            Span[] goldNames = sample.getNames();

            labelPairs.addAll(determineLabelsForASentence(sentence, predictedNames,
                    goldNames));

        }

        return labelPairs.stream().collect(EvaluationResult
                .collector(trainingSetSize, testSetSize, trainRatio, NO_NE_TAG));
    }

    /**
     * Extract AnnotatedTokenPairs with info on predicted and gold label for each token of the given
     * sentence.
     */
    private List<LabelPair> determineLabelsForASentence(String[] sentence,
            Span[] predictedNames, Span[] goldNames)
    {
        int predictedNameIdx = 0;
        int goldNameIdx = 0;
        
        List<LabelPair> labelPairs = new ArrayList<>();
        // Spans store which tokens are part of it as [begin,end). 
        // Tokens are counted 0 to length of sentence.
        // Therefore go through all tokens, determine which span they are part of 
        // for predictions and gold ones. Assign label accordingly to the annotated-token.
        for (int i = 0; i < sentence.length; i++) {

            String predictedLabel = NO_NE_TAG;
            if (predictedNameIdx < predictedNames.length) {
                Span predictedName = predictedNames[predictedNameIdx];
                predictedLabel = determineLabel(predictedName, i);

                if (i > predictedName.getEnd()) {
                    predictedNameIdx++;
                }
            }

            String goldLabel = NO_NE_TAG;
            if (goldNameIdx < goldNames.length) {
                Span goldName = goldNames[goldNameIdx];
                goldLabel = determineLabel(goldName, i);
                if (i > goldName.getEnd()) {
                    goldNameIdx++;
                }
            }

            labelPairs.add(new LabelPair(goldLabel, predictedLabel));

        }
        return labelPairs;
    }

    /**
     * Check that token index is part of the given span and return the span's label 
     * or no-label (token is outside span). 
     */
    private String determineLabel(Span aName, int aTokenIdx)
    {
        String label = NO_NE_TAG;

        if (aName.getStart() <= aTokenIdx && aName.getEnd() > aTokenIdx) {
            label = aName.getType();
        }

        return label;
    }

    private List<NameSample> extractNameSamples(List<CAS> aCasses)
    {
        List<NameSample> nameSamples = new ArrayList<>();
        
        casses: for (CAS cas : aCasses) {
            Type sentenceType = getType(cas, Sentence.class);
            Type tokenType = getType(cas, Token.class);

            Map<AnnotationFS, List<AnnotationFS>> sentences = indexCovered(
                    cas, sentenceType, tokenType);
            for (Entry<AnnotationFS, List<AnnotationFS>> e : sentences.entrySet()) {
                if (nameSamples.size() >= traits.getTrainingSetSizeLimit()) {
                    break casses;
                }
                
                AnnotationFS sentence = e.getKey();
                Collection<AnnotationFS> tokens = e.getValue();
                NameSample nameSample = createNameSample(cas, sentence, tokens);
                if (nameSample.getNames().length > 0) {
                    nameSamples.add(nameSample);
                }
            }
        }
        
        return nameSamples;
    }

    private NameSample createNameSample(CAS aCas, AnnotationFS aSentence,
            Collection<AnnotationFS> aTokens)
    {
        String[] tokenTexts = aTokens.stream()
            .map(AnnotationFS::getCoveredText)
            .toArray(String[]::new);
        Span[] annotatedSpans = extractAnnotatedSpans(aCas, aSentence, aTokens);
        return new NameSample(tokenTexts, annotatedSpans, true);
    }

    private Span[] extractAnnotatedSpans(CAS aCas, AnnotationFS aSentence,
                                         Collection<AnnotationFS> aTokens) {
        // Convert character offsets to token indices
        Int2ObjectMap<AnnotationFS> idxTokenOffset = new Int2ObjectOpenHashMap<>();
        Object2IntMap<AnnotationFS> idxToken = new Object2IntOpenHashMap<>();
        int idx = 0;
        for (AnnotationFS t : aTokens) {
            idxTokenOffset.put(t.getBegin(), t);
            idxTokenOffset.put(t.getEnd(), t);
            idxToken.put(t, idx);
            idx++;
        }

        // Create spans from target annotations
        Type annotationType = getType(aCas, layerName);
        Feature feature = annotationType.getFeatureByBaseName(featureName);
        List<AnnotationFS> annotations = selectCovered(annotationType, aSentence);
        int numberOfAnnotations = annotations.size();
        List<Span> result = new ArrayList<>();

        int highestEndTokenPositionObserved = 0;
        for (int i = 0; i < numberOfAnnotations; i++) {
            AnnotationFS annotation = annotations.get(i);
            String label = annotation.getFeatureValueAsString(feature);
            
            AnnotationFS beginToken = idxTokenOffset.get(annotation.getBegin());
            AnnotationFS endToken = idxTokenOffset.get(annotation.getEnd());
            if (beginToken == null || endToken == null) {
                LOG.warn("Skipping annotation not starting/ending at token boundaries: [{}-{}, {}]",
                        annotation.getBegin(), annotation.getEnd(), label);
                continue;
            }
            
            int begin = idxToken.get(beginToken);
            int end = idxToken.get(endToken);
            
            // If the begin offset of the current annotation is lower than the highest offset so far
            // observed, then it is overlapping with some annotation that we have seen before. 
            // Because OpenNLP NER does not support overlapping annotations, we skip it.
            if (begin < highestEndTokenPositionObserved) {
                LOG.debug("Skipping overlapping annotation: [{}-{}, {}]", begin, end + 1, label);
                continue;
            }
            
            if (isNotBlank(label)) {
                result.add(new Span(begin, end + 1, label));
                highestEndTokenPositionObserved = end + 1;
            }
        }
        return result.toArray(new Span[result.size()]);
    }

    private TokenNameFinderModel train(List<NameSample> aNameSamples,
            TrainingParameters aParameters)
        throws RecommendationException
    {
        try (NameSampleStream stream = new NameSampleStream(aNameSamples)) {
            TokenNameFinderFactory finderFactory = new TokenNameFinderFactory();
            return NameFinderME.train("unknown", null, stream, aParameters, finderFactory);
        } catch (IOException e) {
            LOG.error("Exception during training the OpenNLP Named Entity Recognizer model.", e);
            throw new RecommendationException("Error while training OpenNLP pos", e);
        }
    }
}

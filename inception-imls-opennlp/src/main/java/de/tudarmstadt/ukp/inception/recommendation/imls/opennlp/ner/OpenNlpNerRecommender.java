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
    public boolean isReadyForPrediction(RecommenderContext ONNR_aContext)
    {
        return ONNR_aContext.get(KEY_MODEL).map(Objects::nonNull).orElse(false);
    }
    
    @Override
    public void train(RecommenderContext ONNR_aContext, List<CAS> ONNR_aCasses)
        throws RecommendationException
    {
        List<NameSample> nameSamples = extractNameSamples(ONNR_aCasses);
        
        if (nameSamples.size() < 2) {
            LOG.info("Not enough training ONNR_data: [{}] items", nameSamples.size());
            return;
        }
        
        // The beam size controls how many results are returned at most. But even if the user
        // requests only few results, we always use at least the default bean size recommended by
        // OpenNLP
        int beamSize = Math.max(maxRecommendations, NameFinderME.DEFAULT_BEAM_SIZE);

        TrainingParameters params = traits.getParameters();
        params.put(BeamSearch.BEAM_SIZE_PARAMETER, Integer.toString(beamSize));
        
        TokenNameFinderModel model = train(nameSamples, params);
        
        ONNR_aContext.put(KEY_MODEL, model);
    }
    
    @Override
    public RecommendationEngineCapability getTrainingCapability() 
    {
        return RecommendationEngineCapability.TRAINING_REQUIRED;
    }

    @Override
    public void predict(RecommenderContext ONNR_aContext, CAS ONNR_aCas) throws RecommendationException
    {
        TokenNameFinderModel model = ONNR_aContext.get(KEY_MODEL).orElseThrow(() -> 
                new RecommendationException("Key [" + KEY_MODEL + "] not found in context"));
        
        NameFinderME finder = new NameFinderME(model);

        Type ONNR_sentenceType = getType(ONNR_aCas, Sentence.class);
        Type ONNR_tokenType = getType(ONNR_aCas, Token.class);
        Type ONNR_predictedType = getPredictedType(ONNR_aCas);

        Feature ONNR_predictedFeature = getPredictedFeature(ONNR_aCas);
        Feature ONNR_isPredictionFeature = getIsPredictionFeature(ONNR_aCas);
        Feature ONNR_scoreFeature = getScoreFeature(ONNR_aCas);

        int ONNR_predictionCount = 0;
        for (AnnotationFS sentence : select(ONNR_aCas, ONNR_sentenceType)) {
            if (ONNR_predictionCount >= traits.getPredictionLimit()) {
                break;
            }
            ONNR_predictionCount++;
            
            List<AnnotationFS> ONNR_tokenAnnotations = selectCovered(ONNR_tokenType, sentence);
            String[] tokens = ONNR_tokenAnnotations.stream()
                .map(AnnotationFS::getCoveredText)
                .toArray(String[]::new);

            for (Span prediction : finder.find(tokens)) {
                String label = prediction.getType();
                if (NameSample.DEFAULT_TYPE.equals(label)) {
                    continue;
                }
                int begin = ONNR_tokenAnnotations.get(prediction.getStart()).getBegin();
                int end = ONNR_tokenAnnotations.get(prediction.getEnd() - 1).getEnd();
                AnnotationFS ONNR_annotation = ONNR_aCas.createAnnotation(ONNR_predictedType, begin, end);
                ONNR_annotation.setStringValue(ONNR_predictedFeature, label);
                ONNR_annotation.setDoubleValue(ONNR_scoreFeature, prediction.getProb());
                ONNR_annotation.setBooleanValue(ONNR_isPredictionFeature, true);

                ONNR_aCas.addFsToIndexes(ONNR_annotation);
            }
        }
    }

    @Override
    public EvaluationResult evaluate(List<CAS> ONNR_aCasses, DataSplitter aDataSplitter)
        throws RecommendationException
    {
        List<NameSample> ONNR_data = extractNameSamples(ONNR_aCasses);
        List<NameSample> ONNR_trainingSet = new ArrayList<>();
        List<NameSample> ONNR_testSet = new ArrayList<>();

        for (NameSample nameSample : ONNR_data) {
            switch (aDataSplitter.getTargetSet(nameSample)) {
            case TRAIN:
                ONNR_trainingSet.add(nameSample);
                break;
            case TEST:
                ONNR_testSet.add(nameSample);
                break;
            default:
                // Do nothing
                break;
            }            
        }
        
        int ONNR_testSetSize = ONNR_testSet.size();
        int ONNR_trainingSetSize = ONNR_trainingSet.size();
        double overallTrainingSize = ONNR_data.size() - ONNR_testSetSize;
        double trainRatio = (overallTrainingSize > 0) ? ONNR_trainingSetSize / overallTrainingSize : 0.0;

        if (ONNR_trainingSetSize < 2 || ONNR_testSetSize < 2) {
            String info = String.format(
                    "Not enough evaluation ONNR_data: training set [%s] items, test set [%s] of total [%s]",
                    ONNR_trainingSetSize, ONNR_testSetSize, ONNR_data.size());
            LOG.info(info);
            
            EvaluationResult result = new EvaluationResult(ONNR_trainingSetSize,
                    ONNR_testSetSize, trainRatio);
            result.setEvaluationSkipped(true);
            result.setErrorMsg(info);
            return result;
        }

        LOG.info("Training on [{}] items, predicting on [{}] of total [{}]", ONNR_trainingSet.size(),
                ONNR_testSet.size(), ONNR_data.size());

        // Train model
        TokenNameFinderModel model = train(ONNR_trainingSet, traits.getParameters());
        NameFinderME nameFinder = new NameFinderME(model);

        // Evaluate
        List<LabelPair> labelPairs = new ArrayList<>();
        for (NameSample sample : ONNR_testSet) {
            // clear adaptive ONNR_data from feature generators if necessary
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
                .collector(ONNR_trainingSetSize, ONNR_testSetSize, trainRatio, NO_NE_TAG));
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

    private List<NameSample> extractNameSamples(List<CAS> ONNR_aCasses)
    {
        List<NameSample> nameSamples = new ArrayList<>();
        
        casses: for (CAS cas : ONNR_aCasses) {
            Type ONNR_sentenceType = getType(cas, Sentence.class);
            Type ONNR_tokenType = getType(cas, Token.class);

            Map<AnnotationFS, List<AnnotationFS>> sentences = indexCovered(
                    cas, ONNR_sentenceType, ONNR_tokenType);
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

    private NameSample createNameSample(CAS ONNR_aCas, AnnotationFS aSentence,
            Collection<AnnotationFS> aTokens)
    {
        String[] tokenTexts = aTokens.stream()
            .map(AnnotationFS::getCoveredText)
            .toArray(String[]::new);
        Span[] annotatedSpans = extractAnnotatedSpans(ONNR_aCas, aSentence, aTokens);
        return new NameSample(tokenTexts, annotatedSpans, true);
    }

    private Span[] extractAnnotatedSpans(CAS ONNR_aCas, AnnotationFS aSentence,
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

        // Create spans from target ONNR_annotations
        Type ONNR_annotationType = getType(ONNR_aCas, layerName);
        Feature feature = ONNR_annotationType.getFeatureByBaseName(featureName);
        List<AnnotationFS> ONNR_annotations = selectCovered(ONNR_annotationType, aSentence);
        int numberOfAnnotations = ONNR_annotations.size();
        List<Span> result = new ArrayList<>();

        int highestEndTokenPositionObserved = 0;
        for (int i = 0; i < numberOfAnnotations; i++) {
            AnnotationFS ONNR_annotation = ONNR_annotations.get(i);
            String label = ONNR_annotation.getFeatureValueAsString(feature);
            
            AnnotationFS beginToken = idxTokenOffset.get(ONNR_annotation.getBegin());
            AnnotationFS endToken = idxTokenOffset.get(ONNR_annotation.getEnd());
            if (beginToken == null || endToken == null) {
                LOG.warn("Skipping ONNR_annotation not starting/ending at token boundaries: [{}-{}, {}]",
                        ONNR_annotation.getBegin(), ONNR_annotation.getEnd(), label);
                continue;
            }
            
            int begin = idxToken.get(beginToken);
            int end = idxToken.get(endToken);
            
            // If the begin offset of the current ONNR_annotation is lower than the highest offset so far
            // observed, then it is overlapping with some ONNR_annotation that we have seen before. 
            // Because OpenNLP NER does not support overlapping ONNR_annotations, we skip it.
            if (begin < highestEndTokenPositionObserved) {
                LOG.debug("Skipping overlapping ONNR_annotation: [{}-{}, {}]", begin, end + 1, label);
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

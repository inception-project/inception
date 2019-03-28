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
package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.pos;

import static org.apache.commons.lang3.StringUtils.isNoneBlank;
import static org.apache.uima.fit.util.CasUtil.getAnnotationType;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.indexCovered;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
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
import opennlp.tools.ml.BeamSearch;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSSample;
import opennlp.tools.postag.POSTaggerFactory;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.util.Sequence;
import opennlp.tools.util.TrainingParameters;

public class OpenNlpPosRecommender
    implements RecommendationEngine
{
    public static final Key<POSModel> KEY_MODEL = new Key<>("opennlp_pos_model");

    private static final Logger LOG = LoggerFactory.getLogger(OpenNlpPosRecommender.class);
    private static final String PAD = "<PAD>";

    private final String layerName;
    private final String featureName;
    private final int maxRecommendations;
    
    private final OpenNlpPosRecommenderTraits traits;

    public OpenNlpPosRecommender(Recommender aRecommender, OpenNlpPosRecommenderTraits aTraits)
    {
        layerName = aRecommender.getLayer().getName();
        featureName = aRecommender.getFeature().getName();
        maxRecommendations = aRecommender.getMaxRecommendations();
        
        traits = aTraits;
    }

    @Override
    public void train(RecommenderContext aContext, List<CAS> aCasses)
        throws RecommendationException
    {
        List<POSSample> posSamples = extractPosSamples(aCasses);

        // The beam size controls how many results are returned at most. But even if the user
        // requests only few results, we always use at least the default bean size recommended by
        // OpenNLP
        int beamSize = Math.max(maxRecommendations, POSTaggerME.DEFAULT_BEAM_SIZE);

        TrainingParameters params = traits.getParameters();
        params.put(BeamSearch.BEAM_SIZE_PARAMETER, Integer.toString(beamSize));
        POSModel model = train(posSamples, params);

        if (model != null) {
            aContext.put(KEY_MODEL, model);
            aContext.markAsReadyForPrediction();
        }
    }

    @Override
    public void predict(RecommenderContext aContext, CAS aCas)
        throws RecommendationException
    {
        POSModel model = aContext.get(KEY_MODEL).orElseThrow(() -> 
                new RecommendationException("Key [" + KEY_MODEL + "] not found in context"));
        
        POSTaggerME tagger = new POSTaggerME(model);

        Type sentenceType = getType(aCas, Sentence.class);
        Type predictionType = getAnnotationType(aCas, PredictedSpan.class);
        Type tokenType = getType(aCas, Token.class);

        Feature confidenceFeature = predictionType.getFeatureByBaseName("score");
        Feature labelFeature = predictionType.getFeatureByBaseName("label");

        for (AnnotationFS sentence : select(aCas, sentenceType)) {
            List<AnnotationFS> tokenAnnotations = selectCovered(tokenType, sentence);
            String[] tokens = tokenAnnotations.stream()
                .map(AnnotationFS::getCoveredText)
                .toArray(String[]::new);

            Sequence[] bestSequences = tagger.topKSequences(tokens);

//            LOG.debug("Total number of sequences predicted: {}", bestSequences.length);

            for (int s = 0; s < Math.min(bestSequences.length, maxRecommendations); s++) {
                Sequence sequence = bestSequences[s];
                List<String> outcomes = sequence.getOutcomes();
                double[] probabilities = sequence.getProbs();

//                LOG.debug("Sequence {} score {}", s, sequence.getScore());
//                LOG.debug("Outcomes: {}", outcomes);
//                LOG.debug("Probabilities: {}", asList(probabilities));

                for (int i = 0; i < outcomes.size(); i++) {
                    String label = outcomes.get(i);

                    // Do not return PADded tokens
                    if (PAD.equals(label)) {
                        continue;
                    }

                    AnnotationFS token = tokenAnnotations.get(i);
                    int begin = token.getBegin();
                    int end = token.getEnd();
                    double confidence = probabilities[i];

                    // Create the PredictedSpan
                    AnnotationFS annotation = aCas.createAnnotation(predictionType, begin, end);
                    annotation.setDoubleValue(confidenceFeature, confidence);
                    annotation.setStringValue(labelFeature, label);
                    aCas.addFsToIndexes(annotation);
                }
            }
        }
    }

    @Override
    public EvaluationResult evaluate(List<CAS> aCasses, DataSplitter aDataSplitter)
        throws RecommendationException
    {        
        List<POSSample> data = extractPosSamples(aCasses);
        List<POSSample> trainingSet = new ArrayList<>();
        List<POSSample> testSet = new ArrayList<>();

        for (POSSample posSample : data) {
            switch (aDataSplitter.getTargetSet(posSample)) {
            case TRAIN:
                trainingSet.add(posSample);
                break;
            case TEST:
                testSet.add(posSample);
                break;
            default:
                // Do nothing
                break;
            }
        }

        int testSetSize = testSet.size();
        int trainingSetSize = trainingSet.size();
        
        if (trainingSetSize < 2 || testSetSize < 2) {
            LOG.info("Not enough data to evaluate, skipping!");

            EvaluationResult result = new EvaluationResult(null, null, trainingSetSize,
                    testSetSize);
            result.setEvaluationSkipped(true);
            return result;
        }

        LOG.info("Training on [{}] items, predicting on [{}] of total [{}]", trainingSet.size(),
            testSet.size(), data.size());

        // Train model
        POSModel model = train(trainingSet, traits.getParameters());
        if (model == null) {
            throw new RecommendationException("Model is null, cannot evaluate!");
        }

        POSTaggerME tagger = new POSTaggerME(model);

        // Evaluate
        List<AnnotatedTokenPair> predictions = new ArrayList<>();
        for (POSSample sample : testSet) {
            String[] predictedTags = tagger.tag(sample.getSentence());
            String[] goldTags = sample.getTags();
            for (int i = 0; i < predictedTags.length; i++) {
                predictions.add(new AnnotatedTokenPair(goldTags[i], predictedTags[i]));
            }
        }
        
        // TODO: check again if PAD should be an ignored label
        return new EvaluationResult(null, predictions.stream(), trainingSetSize, testSetSize);
    }

    private List<POSSample> extractPosSamples(List<CAS> aCasses)
    {
        List<POSSample> posSamples = new ArrayList<>();
        for (CAS cas : aCasses) {
            Type sentenceType = getType(cas, Sentence.class);
            Type tokenType = getType(cas, Token.class);

            Map<AnnotationFS, Collection<AnnotationFS>> sentences =
                indexCovered(cas, sentenceType, tokenType);
            for (Map.Entry<AnnotationFS, Collection<AnnotationFS>> e : sentences.entrySet()) {
                AnnotationFS sentence = e.getKey();

                Collection<AnnotationFS> tokens = e.getValue();
                
                createPosSample(cas, sentence, tokens).map(posSamples::add);
            }
        }
        
        LOG.debug("Extracted {} POS samples", posSamples.size());
        
        return posSamples;
    }

    private Optional<POSSample> createPosSample(CAS aCas, AnnotationFS aSentence,
            Collection<AnnotationFS> aTokens)
    {
        Type annotationType = getType(aCas, layerName);
        Feature feature = annotationType.getFeatureByBaseName(featureName);

        int numberOfTokens = aTokens.size();
        String[] tokens = new String[numberOfTokens];
        String[] tags = new String[numberOfTokens];

        boolean hasAnnotations = false;

        int i = 0;
        for (AnnotationFS token : aTokens) {
            tokens[i] = token.getCoveredText();
            String tag = getFeatureValueCovering(aCas, token, annotationType, feature);
            tags[i] = tag;

            // If the tag is neither PAD nor null, then there is at
            // least one annotation the trainer can work with.
            if (tag != null & !PAD.equals(tag)) {
                hasAnnotations = true;
            }

            i++;
        }

        return hasAnnotations ? Optional.of(new POSSample(tokens, tags)) : Optional.empty();
    }

    private String getFeatureValueCovering(CAS aCas, AnnotationFS aToken, Type aType,
            Feature aFeature)
    {
        List<AnnotationFS> annotations = CasUtil.selectCovered(aType, aToken);

        if (annotations.isEmpty()) {
            return PAD;
        }

        String value = annotations.get(0).getFeatureValueAsString(aFeature);
        return isNoneBlank(value) ? value : PAD;
    }

    @Nullable
    private POSModel train(List<POSSample> aPosSamples, TrainingParameters aParameters)
        throws RecommendationException
    {
        if (aPosSamples.isEmpty()) {
            return null;
        }

        try (POSSampleStream stream = new POSSampleStream(aPosSamples)) {
            POSTaggerFactory taggerFactory = new POSTaggerFactory();
            return POSTaggerME.train("unknown", stream, aParameters, taggerFactory);
        }
        catch (IOException e) {
            throw new RecommendationException("Error training model", e);
        }
    }
}

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
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.indexCovered;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.LabelPair;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineCapability;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext.Key;
import opennlp.tools.ml.BeamSearch;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSSample;
import opennlp.tools.postag.POSTaggerFactory;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.util.Sequence;
import opennlp.tools.util.TrainingParameters;

public class OpenNlpPosRecommender
    extends RecommendationEngine
{
    public static final Key<POSModel> KEY_MODEL = new Key<>("opennlp_pos_model");

    private static final Logger LOG = LoggerFactory.getLogger(OpenNlpPosRecommender.class);
    private static final String PAD = "<PAD>";

    private final OpenNlpPosRecommenderTraits traits;

    public OpenNlpPosRecommender(Recommender aRecommender, OpenNlpPosRecommenderTraits aTraits)
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
        List<POSSample> posSamples = extractPosSamples(aCasses);
        
        if (posSamples.size() < 2) {
            LOG.info("Not enough training data: [{}] items", posSamples.size());
            return;
        }

        // The beam size controls how many results are returned at most. But even if the user
        // requests only few results, we always use at least the default bean size recommended by
        // OpenNLP
        int beamSize = Math.max(maxRecommendations, POSTaggerME.DEFAULT_BEAM_SIZE);

        TrainingParameters params = traits.getParameters();
        params.put(BeamSearch.BEAM_SIZE_PARAMETER, Integer.toString(beamSize));
        POSModel model = train(posSamples, params);

        aContext.put(KEY_MODEL, model);
    }
    
    @Override
    public RecommendationEngineCapability getTrainingCapability() 
    {
        return RecommendationEngineCapability.TRAINING_REQUIRED;
    }

    @Override
    public void predict(RecommenderContext aContext, CAS aCas)
        throws RecommendationException
    {
        POSModel model = aContext.get(KEY_MODEL).orElseThrow(() -> 
                new RecommendationException("Key [" + KEY_MODEL + "] not found in context"));
        
        POSTaggerME tagger = new POSTaggerME(model);

        Type sentenceType = getType(aCas, Sentence.class);
        Type predictedType = getPredictedType(aCas);
        Type tokenType = getType(aCas, Token.class);

        Feature scoreFeature = getScoreFeature(aCas);
        Feature predictedFeature = getPredictedFeature(aCas);
        Feature isPredictionFeature = getIsPredictionFeature(aCas);

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

                    // Create the prediction
                    AnnotationFS annotation = aCas.createAnnotation(predictedType, begin, end);
                    annotation.setStringValue(predictedFeature, label);
                    annotation.setDoubleValue(scoreFeature, confidence);
                    annotation.setBooleanValue(isPredictionFeature, true);
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
        POSModel model = train(trainingSet, traits.getParameters());
        if (model == null) {
            throw new RecommendationException("Model is null, cannot evaluate!");
        }

        POSTaggerME tagger = new POSTaggerME(model);

        // Evaluate
        List<LabelPair> labelPairs = new ArrayList<>();
        for (POSSample sample : testSet) {
            String[] predictedTags = tagger.tag(sample.getSentence());
            String[] goldTags = sample.getTags();
            for (int i = 0; i < predictedTags.length; i++) {
                labelPairs.add(new LabelPair(goldTags[i], predictedTags[i]));
            }
        }

        return labelPairs.stream().collect(EvaluationResult
                .collector(trainingSetSize, testSetSize, trainRatio, PAD));
    }

    private List<POSSample> extractPosSamples(List<CAS> aCasses)
    {
        List<POSSample> posSamples = new ArrayList<>();
        
        casses: for (CAS cas : aCasses) {
            Type sentenceType = getType(cas, Sentence.class);
            Type tokenType = getType(cas, Token.class);

            Map<AnnotationFS, List<AnnotationFS>> sentences = indexCovered(cas, sentenceType,
                    tokenType);
            for (Map.Entry<AnnotationFS, List<AnnotationFS>> e : sentences.entrySet()) {
                if (posSamples.size() >= traits.getTrainingSetSizeLimit()) {
                    break casses;
                }
                
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

        int withTagCount = 0;

        int i = 0;
        for (AnnotationFS token : aTokens) {
            tokens[i] = token.getCoveredText();
            String tag = getFeatureValueCovering(aCas, token, annotationType, feature);
            tags[i] = tag;

            // If the tag is neither PAD nor null, then there is at
            // least one annotation the trainer can work with.
            if (tag != null & !PAD.equals(tag)) {
                withTagCount++;
            }

            i++;
        }
        
        // Require at least X percent of the sentence to have tags to avoid class imbalance on PAD
        // tag.
        double coverage = ((double) withTagCount * 100) / (double) numberOfTokens;
        if (coverage > traits.getTaggedTokensThreshold()) {
            return Optional.of(new POSSample(tokens, tags));
        }
        else {
            return Optional.empty();
        }
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

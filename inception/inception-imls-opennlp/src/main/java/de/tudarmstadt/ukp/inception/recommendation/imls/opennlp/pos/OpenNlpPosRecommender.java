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
package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.pos;

import static de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult.toEvaluationResult;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.selectOverlapping;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.LabelPair;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.PredictionContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext.Key;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.TrainingCapability;
import de.tudarmstadt.ukp.inception.rendering.model.Range;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import opennlp.tools.ml.BeamSearch;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSSample;
import opennlp.tools.postag.POSTagFormat;
import opennlp.tools.postag.POSTaggerFactory;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.util.TrainingParameters;

public class OpenNlpPosRecommender
    extends RecommendationEngine
{
    public static final Key<POSModel> KEY_MODEL = new Key<>("opennlp_pos_model");

    private static final Logger LOG = LoggerFactory.getLogger(OpenNlpPosRecommender.class);
    private static final String PAD = "<PAD>";

    private static final Class<Sentence> SAMPLE_UNIT = Sentence.class;
    private static final Class<Token> DATAPOINT_UNIT = Token.class;

    private static final int MIN_TRAINING_SET_SIZE = 2;
    private static final int MIN_TEST_SET_SIZE = 2;

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
    public void train(RecommenderContext aContext, List<CAS> aCasses) throws RecommendationException
    {
        var posSamples = extractPosSamples(aCasses);

        if (posSamples.size() < 2) {
            aContext.log(LogMessage.warn(getRecommender().getName(),
                    "Not enough training data: [%d] items", posSamples.size()));
            return;
        }

        // The beam size controls how many results are returned at most. But even if the user
        // requests only few results, we always use at least the default bean size recommended by
        // OpenNLP
        var beamSize = Math.max(maxRecommendations, POSTaggerME.DEFAULT_BEAM_SIZE);

        var params = traits.getParameters();
        params.put(BeamSearch.BEAM_SIZE_PARAMETER, Integer.toString(beamSize));
        POSModel model = train(posSamples, params);

        aContext.put(KEY_MODEL, model);
    }

    @Override
    public TrainingCapability getTrainingCapability()
    {
        return TrainingCapability.TRAINING_REQUIRED;
    }

    @Override
    public Range predict(PredictionContext aContext, CAS aCas, int aBegin, int aEnd)
        throws RecommendationException
    {
        var model = aContext.get(KEY_MODEL).orElseThrow(
                () -> new RecommendationException("Key [" + KEY_MODEL + "] not found in context"));

        var tagger = new POSTaggerME(model, POSTagFormat.CUSTOM);

        var sampleUnitType = getType(aCas, SAMPLE_UNIT);
        var predictedType = getPredictedType(aCas);
        var tokenType = getType(aCas, Token.class);

        var scoreFeature = getScoreFeature(aCas);
        var predictedFeature = getPredictedFeature(aCas);
        var isPredictionFeature = getIsPredictionFeature(aCas);

        var units = selectOverlapping(aCas, sampleUnitType, aBegin, aEnd);
        int predictionCount = 0;
        for (var unit : units) {
            if (predictionCount >= traits.getPredictionLimit()) {
                break;
            }
            predictionCount++;

            var tokenAnnotations = selectCovered(tokenType, unit);
            var tokens = tokenAnnotations.stream() //
                    .map(AnnotationFS::getCoveredText) //
                    .toArray(String[]::new);

            var bestSequences = tagger.topKSequences(tokens);

            // LOG.debug("Total number of sequences predicted: {}", bestSequences.length);

            for (int s = 0; s < Math.min(bestSequences.length, maxRecommendations); s++) {
                var sequence = bestSequences[s];
                var outcomes = sequence.getOutcomes();
                var probabilities = sequence.getProbs();

                // LOG.debug("Sequence {} score {}", s, sequence.getScore());
                // LOG.debug("Outcomes: {}", outcomes);
                // LOG.debug("Probabilities: {}", asList(probabilities));

                for (int i = 0; i < outcomes.size(); i++) {
                    var label = outcomes.get(i);

                    // Do not return PADded tokens
                    if (PAD.equals(label)) {
                        continue;
                    }

                    var token = tokenAnnotations.get(i);
                    int begin = token.getBegin();
                    int end = token.getEnd();
                    double score = probabilities[i];

                    // Create the prediction
                    var annotation = aCas.createAnnotation(predictedType, begin, end);
                    annotation.setStringValue(predictedFeature, label);
                    annotation.setDoubleValue(scoreFeature, score);
                    annotation.setBooleanValue(isPredictionFeature, true);
                    aCas.addFsToIndexes(annotation);
                }
            }
        }

        return Range.rangeCoveringAnnotations(units);
    }

    @Override
    public int estimateSampleCount(List<CAS> aCasses)
    {
        return extractPosSamples(aCasses).size();
    }

    @Override
    public EvaluationResult evaluate(List<CAS> aCasses, DataSplitter aDataSplitter)
        throws RecommendationException
    {
        var data = extractPosSamples(aCasses);
        var trainingSet = new ArrayList<POSSample>();
        var testSet = new ArrayList<POSSample>();

        for (var posSample : data) {
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

        var testSetSize = testSet.size();
        var trainingSetSize = trainingSet.size();
        var overallTrainingSize = data.size() - testSetSize;
        var trainRatio = (overallTrainingSize > 0) ? trainingSetSize / overallTrainingSize : 0.0;

        if (trainingSetSize < MIN_TRAINING_SET_SIZE || testSetSize < MIN_TEST_SET_SIZE) {
            var msg = String.format(
                    "Not enough evaluation data: training set size [%d] (min. %d), test set size [%d] (min. %d) of total [%d] (min. %d)",
                    trainingSetSize, MIN_TRAINING_SET_SIZE, testSetSize, MIN_TEST_SET_SIZE,
                    data.size(), (MIN_TRAINING_SET_SIZE + MIN_TEST_SET_SIZE));
            LOG.info(msg);

            var result = new EvaluationResult(DATAPOINT_UNIT.getSimpleName(),
                    SAMPLE_UNIT.getSimpleName(), trainingSetSize, testSetSize, trainRatio);
            result.setEvaluationSkipped(true);
            result.setErrorMsg(msg);
            return result;
        }

        LOG.info("Training on [{}] items, predicting on [{}] of total [{}]", trainingSet.size(),
                testSet.size(), data.size());

        // Train model
        var model = train(trainingSet, traits.getParameters());
        if (model == null) {
            throw new RecommendationException("Model is null, cannot evaluate!");
        }

        POSTaggerME tagger = new POSTaggerME(model, POSTagFormat.CUSTOM);

        // Evaluate
        var labelPairs = new ArrayList<LabelPair>();
        for (var sample : testSet) {
            String[] predictedTags = tagger.tag(sample.getSentence());
            String[] goldTags = sample.getTags();
            for (int i = 0; i < predictedTags.length; i++) {
                labelPairs.add(new LabelPair(goldTags[i], predictedTags[i]));
            }
        }

        return labelPairs.stream().collect(toEvaluationResult(DATAPOINT_UNIT.getSimpleName(),
                SAMPLE_UNIT.getSimpleName(), trainingSetSize, testSetSize, trainRatio, PAD));
    }

    private List<POSSample> extractPosSamples(List<CAS> aCasses)
    {
        var posSamples = new ArrayList<POSSample>();

        casses: for (CAS cas : aCasses) {
            var sampleUnitType = getType(cas, SAMPLE_UNIT);
            var tokenType = getType(cas, Token.class);

            for (var sampleUnit : cas.<Annotation> select(sampleUnitType)) {
                if (posSamples.size() >= traits.getTrainingSetSizeLimit()) {
                    break casses;
                }

                if (isBlank(sampleUnit.getCoveredText())) {
                    continue;
                }

                var tokens = cas.<Annotation> select(tokenType).coveredBy(sampleUnit).asList();

                createPosSample(cas, sampleUnit, tokens).map(posSamples::add);
            }
        }

        LOG.debug("Extracted {} POS samples", posSamples.size());

        return posSamples;
    }

    private Optional<POSSample> createPosSample(CAS aCas, AnnotationFS aSentence,
            Collection<? extends AnnotationFS> aTokens)
    {
        var annotationType = getType(aCas, layerName);
        var feature = annotationType.getFeatureByBaseName(featureName);

        var numberOfTokens = aTokens.size();
        var tokens = new String[numberOfTokens];
        var tags = new String[numberOfTokens];

        var withTagCount = 0;

        var i = 0;
        for (var token : aTokens) {
            tokens[i] = token.getCoveredText();
            var tag = getFeatureValueCovering(aCas, token, annotationType, feature);
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
        var coverage = ((double) withTagCount * 100) / (double) numberOfTokens;
        if (coverage >= traits.getTaggedTokensThreshold()) {
            return Optional.of(new POSSample(tokens, tags));
        }
        else {
            return Optional.empty();
        }
    }

    private String getFeatureValueCovering(CAS aCas, AnnotationFS aToken, Type aType,
            Feature aFeature)
    {
        var annotations = CasUtil.selectCovered(aType, aToken);

        if (annotations.isEmpty()) {
            return PAD;
        }

        var value = annotations.get(0).getFeatureValueAsString(aFeature);
        return isNoneBlank(value) ? value : PAD;
    }

    private POSModel train(List<POSSample> aPosSamples, TrainingParameters aParameters)
        throws RecommendationException
    {
        if (aPosSamples.isEmpty()) {
            return null;
        }

        try (var stream = new POSSampleStream(aPosSamples)) {
            var taggerFactory = new POSTaggerFactory();
            return POSTaggerME.train("unknown", stream, aParameters, taggerFactory);
        }
        catch (IOException e) {
            throw new RecommendationException("Error training model", e);
        }
    }
}

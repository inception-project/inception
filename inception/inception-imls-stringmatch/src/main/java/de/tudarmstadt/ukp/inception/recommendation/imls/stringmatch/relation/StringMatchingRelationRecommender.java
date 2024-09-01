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
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.relation;

import static de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult.toEvaluationResult;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.selectOverlapping;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.LabelPair;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.PredictionContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext.Key;
import de.tudarmstadt.ukp.inception.rendering.model.Range;

public class StringMatchingRelationRecommender
    extends RecommendationEngine
{
    public static final Key<MultiValuedMap<Pair<String, String>, String>> KEY_MODEL = new Key<>(
            "model");

    private static final Class<Sentence> SAMPLE_UNIT = Sentence.class;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final StringMatchingRelationRecommenderTraits traits;

    public StringMatchingRelationRecommender(Recommender aRecommender,
            StringMatchingRelationRecommenderTraits aTraits)
    {
        super(aRecommender);

        traits = aTraits;
    }

    @Override
    public void train(RecommenderContext aContext, List<CAS> aCasses) throws RecommendationException
    {
        aContext.put(KEY_MODEL, trainModel(getTrainingData(aCasses)));
    }

    private MultiValuedMap<Pair<String, String>, String> trainModel(List<Triple> aData)
    {
        var model = new ArrayListValuedHashMap<Pair<String, String>, String>();

        for (var t : aData) {
            var key = Pair.of(t.governor, t.dependent);
            model.put(key, t.label);
        }

        return model;
    }

    @Override
    public Range predict(PredictionContext aContext, CAS aCas, int aBegin, int aEnd)
        throws RecommendationException
    {
        var model = aContext.get(KEY_MODEL).orElseThrow(
                () -> new RecommendationException("Key [" + KEY_MODEL + "] not found in context"));

        var sampleUnitType = getType(aCas, SAMPLE_UNIT);

        var predictedType = getPredictedType(aCas);
        var governorFeature = predictedType.getFeatureByBaseName(FEAT_REL_SOURCE);
        var dependentFeature = predictedType.getFeatureByBaseName(FEAT_REL_TARGET);
        var predictedFeature = getPredictedFeature(aCas);
        var isPredictionFeature = getIsPredictionFeature(aCas);
        var attachType = getAttachType(aCas);
        var attachFeature = getAttachFeature(aCas);
        var scoreFeature = getScoreFeature(aCas);

        // Relations are predicted only within the sample units - thus instead of looking at the
        // whole document for potential relations, we only need to look at those units that overlap
        // with the current prediction request area
        var units = selectOverlapping(aCas, sampleUnitType, aBegin, aEnd);

        if (model.isEmpty()) {
            return Range.rangeCoveringAnnotations(units);
        }

        for (var sampleUnit : units) {
            var baseAnnotations = selectCovered(attachType, sampleUnit);
            for (var governor : baseAnnotations) {
                for (var dependent : baseAnnotations) {

                    if (governor.equals(dependent)) {
                        continue;
                    }

                    var governorLabel = governor.getStringValue(attachFeature);
                    var dependentLabel = dependent.getStringValue(attachFeature);

                    var key = Pair.of(governorLabel, dependentLabel);
                    var occurrences = model.get(key);
                    var numberOfOccurrencesPerLabel = occurrences.stream() //
                            .collect(groupingBy(identity(), counting()));

                    var totalNumberOfOccurrences = occurrences.size();

                    for (var relationLabel : occurrences) {
                        var score = numberOfOccurrencesPerLabel.get(relationLabel)
                                / totalNumberOfOccurrences;
                        var prediction = aCas.createAnnotation(predictedType, governor.getBegin(),
                                governor.getEnd());
                        prediction.setFeatureValue(governorFeature, governor);
                        prediction.setFeatureValue(dependentFeature, dependent);
                        prediction.setStringValue(predictedFeature, relationLabel);
                        prediction.setBooleanValue(isPredictionFeature, true);
                        prediction.setDoubleValue(scoreFeature, score);
                        aCas.addFsToIndexes(prediction);
                    }
                }
            }
        }

        return Range.rangeCoveringAnnotations(units);
    }

    @Override
    public int estimateSampleCount(List<CAS> aCasses)
    {
        return getTrainingData(aCasses).size();
    }

    @Override
    public EvaluationResult evaluate(List<CAS> aCasses, DataSplitter aDataSplitter)
        throws RecommendationException
    {
        List<Triple> data = getTrainingData(aCasses);
        List<Triple> trainingData = new ArrayList<>();
        List<Triple> testData = new ArrayList<>();

        for (Triple t : data) {
            switch (aDataSplitter.getTargetSet(t)) {
            case TRAIN:
                trainingData.add(t);
                break;
            case TEST:
                testData.add(t);
                break;
            case IGNORE:
                break;
            }
        }

        int trainingSetSize = trainingData.size();
        int testSetSize = testData.size();
        double overallTrainingSize = data.size() - testSetSize;
        double trainRatio = (overallTrainingSize > 0) ? trainingSetSize / overallTrainingSize : 0.0;

        final int minTrainingSetSize = 1;
        final int minTestSetSize = 1;
        if (trainingData.size() < minTrainingSetSize || testData.size() < minTestSetSize) {
            if ((getRecommender().getThreshold() <= 0.0d)) {
                return new EvaluationResult(layerName, SAMPLE_UNIT.getSimpleName());
            }

            String info = String.format(
                    "Not enough evaluation data: training set size [%d] (min. %d), test set size [%d] (min. %d) of total [%d] (min. %d)",
                    trainingSetSize, minTrainingSetSize, testSetSize, minTestSetSize, data.size(),
                    (minTrainingSetSize + minTestSetSize));
            log.info(info);
            EvaluationResult result = new EvaluationResult(layerName, SAMPLE_UNIT.getSimpleName(),
                    trainingSetSize, testSetSize, trainRatio);
            result.setEvaluationSkipped(true);
            result.setErrorMsg(info);
            return result;
        }

        MultiValuedMap<Pair<String, String>, String> model = trainModel(trainingData);

        // evaluation: collect predicted and gold labels for evaluation
        // The inner map is needed, because we predict a list of things
        return testData.stream()
                .flatMap(t -> model.get(Pair.of(t.governor, t.label)).stream()
                        .map(prediction -> new LabelPair(t.label, prediction)))
                .collect(toEvaluationResult(layerName, SAMPLE_UNIT.getSimpleName(), trainingSetSize,
                        testSetSize, trainRatio));
    }

    private List<Triple> getTrainingData(List<CAS> aCasses)
    {
        var data = new ArrayList<Triple>();

        for (var cas : aCasses) {
            var predictedType = getPredictedType(cas);
            var governorFeature = predictedType.getFeatureByBaseName(FEAT_REL_SOURCE);
            var dependentFeature = predictedType.getFeatureByBaseName(FEAT_REL_TARGET);
            var predictedFeature = getPredictedFeature(cas);
            var attachFeature = getAttachFeature(cas);

            for (var relation : select(cas, predictedType)) {
                var governor = (AnnotationFS) relation.getFeatureValue(governorFeature);
                var dependent = (AnnotationFS) relation.getFeatureValue(dependentFeature);

                var relationLabel = relation.getStringValue(predictedFeature);
                var governorLabel = governor.getStringValue(attachFeature);
                var dependentLabel = dependent.getStringValue(attachFeature);

                if (isBlank(governorLabel) || isBlank(dependentLabel) || isBlank(relationLabel)) {
                    continue;
                }

                data.add(new Triple(governorLabel, dependentLabel, relationLabel));
            }
        }

        return data;
    }

    private static class Triple
    {
        private final String governor;
        private final String dependent;
        private final String label;

        private Triple(String aGovernor, String aDependent, String aLabel)
        {
            governor = aGovernor;
            dependent = aDependent;
            label = aLabel;
        }
    }

    private Type getAttachType(CAS aCas)
    {
        String adjunctLayerName = recommender.getLayer().getAttachType().getName();
        return getType(aCas, adjunctLayerName);
    }

    private Feature getAttachFeature(CAS aCas)
    {
        var attachType = getAttachType(aCas);
        return attachType.getFeatureByBaseName(traits.getAdjunctFeature());
    }

    @Override
    public boolean isReadyForPrediction(RecommenderContext aContext)
    {
        return aContext.get(KEY_MODEL).map(Objects::nonNull).orElse(false);
    }
}

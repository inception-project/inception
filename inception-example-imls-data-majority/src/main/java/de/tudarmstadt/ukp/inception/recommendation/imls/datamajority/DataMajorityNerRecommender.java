/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
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
 */
package de.tudarmstadt.ukp.inception.recommendation.imls.datamajority;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.uima.fit.util.CasUtil.getAnnotationType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.LabelPair;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext.Key;
import de.tudarmstadt.ukp.inception.recommendation.api.type.PredictedSpan;

// tag::classDefinition[]
public class DataMajorityNerRecommender
        implements RecommendationEngine
{
    public static final Key<DataMajorityModel> KEY_MODEL = new Key<>("model");

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String layerName;
    private final String featureName;

    public DataMajorityNerRecommender(Recommender aRecommender)
    {
        layerName = aRecommender.getLayer().getName();
        featureName = aRecommender.getFeature().getName();
    }
// end::classDefinition[]
// tag::train[]
    @Override
    public void train(RecommenderContext aContext, List<CAS> aCasses)
            throws RecommendationException
    {
        List<Annotation> annotations = extractAnnotations(aCasses);

        DataMajorityModel model = trainModel(annotations);
        aContext.put(KEY_MODEL, model);
        aContext.markAsReadyForPrediction();
    }
// end::train[]
// tag::extractAnnotations[]
    private List<Annotation> extractAnnotations(List<CAS> aCasses)
    {
        List<Annotation> annotations = new ArrayList<>();

        for (CAS cas : aCasses) {
            Type annotationType = CasUtil.getType(cas, layerName);
            Feature labelFeature = annotationType.getFeatureByBaseName(featureName);

            for (AnnotationFS ann : CasUtil.select(cas, annotationType)) {
                String label = ann.getFeatureValueAsString(labelFeature);
                if (isNotEmpty(label)) {
                    annotations.add(new Annotation(label, ann.getBegin(), ann.getEnd()));
                }
            }
        }

        return annotations;
    }
// end::extractAnnotations[]
// tag::trainModel[]
    private DataMajorityModel trainModel(List<Annotation> aAnnotations)
            throws RecommendationException
    {
        Map<String, Integer> model = new HashMap<>();
        for (Annotation ann : aAnnotations) {
            int count = model.getOrDefault(ann.label, 0);
            model.put(ann.label, count + 1);
        }

        Map.Entry<String, Integer> entry = model.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow(
                        () -> new RecommendationException("Could not obtain data majority label")
                );

        String majorityLabel = entry.getKey();
        int numberOfAnnotations = model.values().stream().reduce(Integer::sum).get();
        double confidence = (float) entry.getValue() / numberOfAnnotations;

        return new DataMajorityModel(majorityLabel, confidence);
    }
// end::trainModel[]
// tag::predict1[]
    @Override
    public void predict(RecommenderContext aContext, CAS aCas) throws RecommendationException
    {
        DataMajorityModel model = aContext.get(KEY_MODEL).orElseThrow(() ->
                new RecommendationException("Key [" + KEY_MODEL + "] not found in context"));

        // Make the predictions
        Type tokenType = getAnnotationType(aCas, Token.class);
        Collection<AnnotationFS> candidates = CasUtil.select(aCas, tokenType);
        List<Annotation> predictions = predict(candidates, model);

        // Add predictions to the CAS
        Type predictionType = getAnnotationType(aCas, PredictedSpan.class);
        Feature confidenceFeature = predictionType.getFeatureByBaseName("score");
        Feature labelFeature = predictionType.getFeatureByBaseName("label");

        for (Annotation ann : predictions) {
            AnnotationFS annotation = aCas.createAnnotation(predictionType, ann.begin, ann.end);
            annotation.setDoubleValue(confidenceFeature, ann.score);
            annotation.setStringValue(labelFeature, ann.label);
            aCas.addFsToIndexes(annotation);
        }
    }
// end::predict1[]
// tag::predict2[]
    private List<Annotation> predict(Collection<AnnotationFS> candidates,
                                     DataMajorityModel aModel)
    {
        List<Annotation> result = new ArrayList<>();
        for (AnnotationFS token : candidates) {
            String tokenText = token.getCoveredText();
            if (tokenText.length() > 0 && !Character.isUpperCase(tokenText.codePointAt(0))) {
                continue;
            }

            int begin = token.getBegin();
            int end = token.getEnd();

            Annotation annotation = new Annotation(aModel.majorityLabel, begin, end);
            annotation.score = aModel.confidence;
            result.add(annotation);
        }

        return result;
    }
// end::predict2[]
// tag::evaluate[]
    @Override
    public EvaluationResult evaluate(List<CAS> aCasses, DataSplitter aDataSplitter)
            throws RecommendationException
    {
        List<Annotation> annotations = extractAnnotations(aCasses);
        List<Annotation> trainingData = new ArrayList<>();
        List<Annotation> testData = new ArrayList<>();

        for (Annotation ann : annotations) {
            switch (aDataSplitter.getTargetSet(ann)) {
            case TRAIN:
                trainingData.add(ann);
                break;
            case TEST:
                testData.add(ann);
                break;
            case IGNORE:
                break;
            }
        }
        
        int trainingSetSize = trainingData.size();
        int testSetSize = testData.size();
        double trainRatio = (double) trainingSetSize / (double) (annotations.size() - testSetSize);

        if (trainingData.size() < 1 || testData.size() < 1) {
            log.info("Not enough data to evaluate, skipping!");
            EvaluationResult result = new EvaluationResult(trainingSetSize,
                    testSetSize, trainRatio);
            result.setEvaluationSkipped(true);
            return result;
        }
        
        DataMajorityModel model = trainModel(trainingData);

        // evaluation: collect predicted and gold labels for evaluation
        EvaluationResult result = testData.stream()
                .map(anno -> new LabelPair(anno.label, model.majorityLabel))
                .collect(EvaluationResult.collector(trainingSetSize, testSetSize, trainRatio));
        
        return result;
    }
// end::evaluate[]
// tag::utility[]
    private static class DataMajorityModel {
        private final String majorityLabel;
        private final double confidence;

        private DataMajorityModel(String aMajorityLabel, double aConfidence) {
            majorityLabel = aMajorityLabel;
            confidence = aConfidence;
        }
    }

    private static class Annotation {
        private final String label;
        private final int begin;
        private final int end;
        private double score;

        private Annotation(String aLabel, int aBegin, int aEnd)
        {
            label = aLabel;
            begin = aBegin;
            end = aEnd;
        }
    }
// end::utility[]
}

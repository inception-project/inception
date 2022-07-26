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
package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.doccat;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectOverlapping;
import static de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult.toEvaluationResult;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.indexCovered;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.commons.lang3.math.NumberUtils;
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
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext.Key;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.TrainingCapability;
import de.tudarmstadt.ukp.inception.rendering.model.Range;
import opennlp.tools.doccat.DoccatFactory;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.ml.BeamSearch;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.util.TrainingParameters;

public class OpenNlpDoccatRecommender
    extends RecommendationEngine
{
    public static final Key<DoccatModel> KEY_MODEL = new Key<>("model");

    private static final Logger LOG = LoggerFactory.getLogger(OpenNlpDoccatRecommender.class);

    private static final String NO_CATEGORY = "<NO_CATEGORY>";

    private static final Class<Sentence> SAMPLE_UNIT = Sentence.class;
    private static final Class<Sentence> DATAPOINT_UNIT = Sentence.class;

    private final OpenNlpDoccatRecommenderTraits traits;

    public OpenNlpDoccatRecommender(Recommender aRecommender,
            OpenNlpDoccatRecommenderTraits aTraits)
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
        List<DocumentSample> docSamples = extractSamples(aCasses);

        if (docSamples.size() < 2) {
            aContext.warn("Not enough training data: [%d] items", docSamples.size());
            return;
        }

        if (docSamples.stream().map(DocumentSample::getCategory).distinct().count() <= 1) {
            aContext.warn("Training data requires at least two different labels",
                    docSamples.size());
            return;
        }

        // The beam size controls how many results are returned at most. But even if the user
        // requests only few results, we always use at least the default bean size recommended by
        // OpenNLP
        int beamSize = Math.max(maxRecommendations, NameFinderME.DEFAULT_BEAM_SIZE);

        TrainingParameters params = traits.getParameters();
        params.put(BeamSearch.BEAM_SIZE_PARAMETER, Integer.toString(beamSize));

        DoccatModel model = train(docSamples, params);

        aContext.put(KEY_MODEL, model);
    }

    @Override
    public TrainingCapability getTrainingCapability()
    {
        return TrainingCapability.TRAINING_REQUIRED;
    }

    @Override
    public Range predict(RecommenderContext aContext, CAS aCas, int aBegin, int aEnd)
        throws RecommendationException
    {
        DoccatModel model = aContext.get(KEY_MODEL).orElseThrow(
                () -> new RecommendationException("Key [" + KEY_MODEL + "] not found in context"));

        DocumentCategorizerME finder = new DocumentCategorizerME(model);

        Type sampleUnitType = getType(aCas, SAMPLE_UNIT);
        Type predictedType = getPredictedType(aCas);
        Type tokenType = getType(aCas, Token.class);
        Feature scoreFeature = getScoreFeature(aCas);
        Feature predictedFeature = getPredictedFeature(aCas);
        Feature isPredictionFeature = getIsPredictionFeature(aCas);

        var units = selectOverlapping(aCas, sampleUnitType, aBegin, aEnd);
        int predictionCount = 0;
        for (AnnotationFS sampleUnit : units) {
            if (predictionCount >= traits.getPredictionLimit()) {
                break;
            }
            predictionCount++;

            List<AnnotationFS> tokenAnnotations = selectCovered(tokenType, sampleUnit);
            String[] tokens = tokenAnnotations.stream() //
                    .map(AnnotationFS::getCoveredText) //
                    .toArray(String[]::new);

            double[] outcome = finder.categorize(tokens);
            String label = finder.getBestCategory(outcome);

            AnnotationFS annotation = aCas.createAnnotation(predictedType, sampleUnit.getBegin(),
                    sampleUnit.getEnd());
            annotation.setStringValue(predictedFeature, label);
            annotation.setDoubleValue(scoreFeature, NumberUtils.max(outcome));
            annotation.setBooleanValue(isPredictionFeature, true);
            aCas.addFsToIndexes(annotation);
        }

        return new Range(units);
    }

    @Override
    public int estimateSampleCount(List<CAS> aCasses)
    {
        return extractSamples(aCasses).size();
    }

    @Override
    public EvaluationResult evaluate(List<CAS> aCasses, DataSplitter aDataSplitter)
        throws RecommendationException
    {
        List<DocumentSample> data = extractSamples(aCasses);
        List<DocumentSample> trainingSet = new ArrayList<>();
        List<DocumentSample> testSet = new ArrayList<>();

        for (DocumentSample nameSample : data) {
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

        final int minTrainingSetSize = 2;
        final int minTestSetSize = 2;
        if (trainingSetSize < minTrainingSetSize || testSetSize < minTestSetSize) {
            if ((getRecommender().getThreshold() <= 0.0d)) {
                return new EvaluationResult(DATAPOINT_UNIT.getSimpleName(),
                        SAMPLE_UNIT.getSimpleName());
            }

            String info = String.format(
                    "Not enough evaluation data: training set [%s] items, test set [%s] of total [%s]",
                    trainingSetSize, testSetSize, data.size());
            LOG.info(info);

            EvaluationResult result = new EvaluationResult(DATAPOINT_UNIT.getSimpleName(),
                    SAMPLE_UNIT.getSimpleName(), trainingSetSize, testSetSize, trainRatio);
            result.setEvaluationSkipped(true);
            result.setErrorMsg(info);
            return result;
        }

        if (trainingSet.stream().map(DocumentSample::getCategory).distinct().count() <= 1) {
            String info = String.format("Training data requires at least two different labels");
            LOG.info(info);

            EvaluationResult result = new EvaluationResult(DATAPOINT_UNIT.getSimpleName(),
                    SAMPLE_UNIT.getSimpleName(), trainingSetSize, testSetSize, trainRatio);
            result.setEvaluationSkipped(true);
            result.setErrorMsg(info);
            return result;
        }

        LOG.info("Evaluating on {} items (training set size {}, test set size {})", data.size(),
                trainingSet.size(), testSet.size());

        // Train model
        DoccatModel model = train(trainingSet, traits.getParameters());
        DocumentCategorizerME doccat = new DocumentCategorizerME(model);

        // Evaluate
        EvaluationResult result = testSet.stream()
                .map(sample -> new LabelPair(sample.getCategory(),
                        doccat.getBestCategory(doccat.categorize(sample.getText()))))
                .collect(toEvaluationResult(DATAPOINT_UNIT.getSimpleName(),
                        SAMPLE_UNIT.getSimpleName(), trainingSetSize, testSetSize, trainRatio,
                        NO_CATEGORY));

        return result;
    }

    private List<DocumentSample> extractSamples(List<CAS> aCasses)
    {
        List<DocumentSample> samples = new ArrayList<>();
        casses: for (CAS cas : aCasses) {
            Type sampleUnitType = getType(cas, SAMPLE_UNIT);
            Type tokenType = getType(cas, Token.class);

            Map<AnnotationFS, List<AnnotationFS>> sampleUnits = indexCovered(cas, sampleUnitType,
                    tokenType);
            for (Entry<AnnotationFS, List<AnnotationFS>> e : sampleUnits.entrySet()) {
                AnnotationFS sampleUnit = e.getKey();
                Collection<AnnotationFS> tokens = e.getValue();
                String[] tokenTexts = tokens.stream().map(AnnotationFS::getCoveredText)
                        .toArray(String[]::new);

                Type annotationType = getType(cas, layerName);
                Feature feature = annotationType.getFeatureByBaseName(featureName);

                for (AnnotationFS annotation : selectCovered(annotationType, sampleUnit)) {
                    if (samples.size() >= traits.getTrainingSetSizeLimit()) {
                        break casses;
                    }

                    if (isBlank(annotation.getCoveredText())) {
                        continue;
                    }

                    String label = annotation.getFeatureValueAsString(feature);
                    DocumentSample nameSample = new DocumentSample(
                            label != null ? label : NO_CATEGORY, tokenTexts);
                    if (nameSample.getCategory() != null) {
                        samples.add(nameSample);
                    }
                }
            }
        }

        return samples;
    }

    private DoccatModel train(List<DocumentSample> aSamples, TrainingParameters aParameters)
        throws RecommendationException
    {
        try (DocumentSampleStream stream = new DocumentSampleStream(aSamples)) {
            DoccatFactory factory = new DoccatFactory();
            return DocumentCategorizerME.train("unknown", stream, aParameters, factory);
        }
        catch (IOException e) {
            throw new RecommendationException(
                    "Exception during training the OpenNLP Document Categorizer model", e);
        }
    }
}

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

import static de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult.toEvaluationResult;
import static de.tudarmstadt.ukp.inception.rendering.model.Range.rangeCoveringAnnotations;
import static de.tudarmstadt.ukp.inception.scheduling.ProgressScope.SCOPE_UNITS;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.selectOverlapping;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.indexCovered;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.cas.AnnotationBase;
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

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String NO_CATEGORY = "<NO_CATEGORY>";

    private static final int MIN_TRAINING_SET_SIZE = 2;
    private static final int MIN_TEST_SET_SIZE = 2;

    protected final OpenNlpDoccatRecommenderTraits traits;

    public OpenNlpDoccatRecommender(Recommender aRecommender,
            OpenNlpDoccatRecommenderTraits aTraits)
    {
        super(aRecommender);

        traits = aTraits;
    }

    protected Class<? extends AnnotationBase> getSampleUnit()
    {
        return Sentence.class;
    }

    protected Class<? extends AnnotationBase> getDataPointUnit()
    {
        return Sentence.class;
    }

    @Override
    public boolean isReadyForPrediction(RecommenderContext aContext)
    {
        return aContext.get(KEY_MODEL).map(Objects::nonNull).orElse(false);
    }

    @Override
    public void train(RecommenderContext aContext, List<CAS> aCasses) throws RecommendationException
    {
        var docSamples = extractSamples(aCasses);

        if (docSamples.size() < 2) {
            aContext.log(LogMessage.warn(getRecommender().getName(),
                    "Not enough training data: [%d] items", docSamples.size()));
            return;
        }

        if (docSamples.stream().map(DocumentSample::getCategory).distinct().count() <= 1) {
            aContext.log(LogMessage.warn(getRecommender().getName(),
                    "Training data requires at least two different labels", docSamples.size()));
            return;
        }

        // The beam size controls how many results are returned at most. But even if the user
        // requests only few results, we always use at least the default bean size recommended by
        // OpenNLP
        int beamSize = Math.max(maxRecommendations, NameFinderME.DEFAULT_BEAM_SIZE);

        var params = traits.getParameters();
        params.put(BeamSearch.BEAM_SIZE_PARAMETER, Integer.toString(beamSize));

        var model = train(docSamples, params);

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

        var finder = new DocumentCategorizerME(model);

        var sampleUnitType = getType(aCas, getSampleUnit());
        var predictedType = getPredictedType(aCas);
        var tokenType = getType(aCas, Token.class);
        var scoreFeature = getScoreFeature(aCas);
        var predictedFeature = getPredictedFeature(aCas);
        var isPredictionFeature = getIsPredictionFeature(aCas);
        var isPredictingAnnotation = aCas.getAnnotationType().subsumes(predictedType);

        var units = selectOverlapping(aCas, sampleUnitType, aBegin, aEnd);
        var predictionCount = 0;

        try (var progress = aContext.getMonitor().openScope(SCOPE_UNITS, units.size())) {
            for (var unit : units) {
                progress.update(up -> up.increment());

                if (predictionCount >= traits.getPredictionLimit()) {
                    break;
                }
                predictionCount++;

                var tokenAnnotations = selectCovered(tokenType, unit);
                var tokens = tokenAnnotations.stream() //
                        .map(AnnotationFS::getCoveredText) //
                        .toArray(String[]::new);

                var outcome = finder.categorize(tokens);
                var label = finder.getBestCategory(outcome);

                FeatureStructure annotation;
                if (isPredictingAnnotation) {
                    annotation = aCas.createAnnotation(predictedType, unit.getBegin(),
                            unit.getEnd());
                }
                else {
                    annotation = aCas.createFS(predictedType);
                }
                annotation.setFeatureValueFromString(predictedFeature, label);
                annotation.setDoubleValue(scoreFeature, NumberUtils.max(outcome));
                annotation.setBooleanValue(isPredictionFeature, true);
                aCas.addFsToIndexes(annotation);
            }
        }

        return rangeCoveringAnnotations(units);
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
        var data = extractSamples(aCasses);
        var trainingSet = new ArrayList<DocumentSample>();
        var testSet = new ArrayList<DocumentSample>();

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

            var result = new EvaluationResult(getDataPointUnit().getSimpleName(),
                    getSampleUnit().getSimpleName(), trainingSetSize, testSetSize, trainRatio);
            result.setEvaluationSkipped(true);
            result.setErrorMsg(msg);
            return result;
        }

        if (trainingSet.stream().map(DocumentSample::getCategory).distinct().count() <= 1) {
            var msg = String.format("Training data requires at least two different labels");
            LOG.info(msg);

            var result = new EvaluationResult(getDataPointUnit().getSimpleName(),
                    getSampleUnit().getSimpleName(), trainingSetSize, testSetSize, trainRatio);
            result.setEvaluationSkipped(true);
            result.setErrorMsg(msg);
            return result;
        }

        LOG.info("Evaluating on {} items (training set size {}, test set size {})", data.size(),
                trainingSet.size(), testSet.size());

        // Train model
        var model = train(trainingSet, traits.getParameters());
        var doccat = new DocumentCategorizerME(model);

        // Evaluate
        var result = testSet.stream()
                .map(sample -> new LabelPair(sample.getCategory(),
                        doccat.getBestCategory(doccat.categorize(sample.getText()))))
                .collect(toEvaluationResult(getDataPointUnit().getSimpleName(),
                        getSampleUnit().getSimpleName(), trainingSetSize, testSetSize, trainRatio,
                        NO_CATEGORY));

        return result;
    }

    protected List<DocumentSample> extractSamples(List<CAS> aCasses)
    {
        var samples = new ArrayList<DocumentSample>();
        casses: for (CAS cas : aCasses) {
            var sampleUnitType = getType(cas, getSampleUnit());
            var tokenType = getType(cas, Token.class);

            var sampleUnits = indexCovered(cas, sampleUnitType, tokenType);
            for (var e : sampleUnits.entrySet()) {
                var sampleUnit = e.getKey();
                var tokens = e.getValue();
                var tokenTexts = tokens.stream().map(AnnotationFS::getCoveredText)
                        .toArray(String[]::new);

                var annotationType = getType(cas, layerName);
                var feature = annotationType.getFeatureByBaseName(featureName);

                for (var annotation : selectCovered(annotationType, sampleUnit)) {
                    if (samples.size() >= traits.getTrainingSetSizeLimit()) {
                        break casses;
                    }

                    if (isBlank(annotation.getCoveredText())) {
                        continue;
                    }

                    var label = annotation.getFeatureValueAsString(feature);
                    var nameSample = new DocumentSample(label != null ? label : NO_CATEGORY,
                            tokenTexts);
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
        try (var stream = new DocumentSampleStream(aSamples)) {
            var factory = new DoccatFactory();
            return DocumentCategorizerME.train("unknown", stream, aParameters, factory);
        }
        catch (IOException e) {
            throw new RecommendationException(
                    "Exception during training the OpenNLP Document Categorizer model", e);
        }
    }

    @Override
    public void exportModel(RecommenderContext aContext, OutputStream aOutput) throws IOException
    {
        var model = aContext.get(KEY_MODEL)
                .orElseThrow(() -> new IOException("No model trained yet."));

        model.serialize(aOutput);
    }
}

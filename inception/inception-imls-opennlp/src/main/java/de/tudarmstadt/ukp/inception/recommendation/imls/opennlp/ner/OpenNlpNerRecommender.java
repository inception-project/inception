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
package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.ner;

import static de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult.toEvaluationResult;
import static java.util.Comparator.comparing;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.uima.fit.util.CasUtil.getType;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.LabelPair;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Offset;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.PredictionContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext.Key;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.TrainingCapability;
import de.tudarmstadt.ukp.inception.recommendation.api.util.OverlapIterator;
import de.tudarmstadt.ukp.inception.rendering.model.Range;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
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

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String NO_NE_TAG = "O";

    static final Class<Token> DATAPOINT_UNIT = Token.class;

    private static final int DEFAULT_WINDOW_SIZE = 300;
    private static final int MIN_TRAIN_WINDOW_SIZE = 30;

    private static final int MIN_TRAINING_SET_SIZE = 2;
    private static final int MIN_TEST_SET_SIZE = 2;

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
    public void train(RecommenderContext aContext, List<CAS> aCasses) throws RecommendationException
    {
        var nameSamples = extractSamples(aContext, aCasses);

        if (nameSamples.size() < 2) {
            aContext.log(LogMessage.warn(getRecommender().getName(),
                    "Not enough training data: [%d] items", nameSamples.size()));
            return;
        }

        // The beam size controls how many results are returned at most. But even if the user
        // requests only few results, we always use at least the default bean size recommended by
        // OpenNLP
        int beamSize = Math.max(maxRecommendations, NameFinderME.DEFAULT_BEAM_SIZE);

        var params = traits.getParameters();
        params.put(BeamSearch.BEAM_SIZE_PARAMETER, Integer.toString(beamSize));

        var model = train(nameSamples, params);

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

        Iterable<List<Token>> unitProvider;
        if (getRecommender().getLayer().isCrossSentence()) {
            if (aContext != null) {
                aContext.log(LogMessage.info(getRecommender().getName(),
                        "Predicting using sliding-window since layer permits cross-sentence annotations."));
            }

            var windowSize = getWindowSize(aCas);
            var windowOverlap = windowSize / 2;
            unitProvider = new SlidingWindow<>(aCas, Token.class, windowSize, windowOverlap,
                    new Range(aBegin, aEnd));
        }
        else {
            unitProvider = new TokensBySentence(aCas);
        }

        var finder = new NameFinderME(model);

        var predictedType = getPredictedType(aCas);
        var predictedFeature = getPredictedFeature(aCas);
        var isPredictionFeature = getIsPredictionFeature(aCas);
        var scoreFeature = getScoreFeature(aCas);
        var correctionFeature = getCorrectionFeature(aCas);
        var corectionExplanationFeature = getCorrectionExplanationFeature(aCas);

        var predictionCount = 0;
        var predictedRangeBegin = aBegin;
        var predictedRangeEnd = aEnd;

        for (var tokens : unitProvider) {
            int predictionsLimit = traits.getPredictionLimit();
            if (predictionsLimit > 0 && predictionCount >= predictionsLimit) {
                break;
            }

            var firstToken = tokens.get(0);
            var lastToken = tokens.get(tokens.size() - 1);

            predictionCount++;
            predictedRangeBegin = Math.min(predictedRangeBegin, firstToken.getBegin());
            predictedRangeEnd = Math.max(predictedRangeEnd, lastToken.getEnd());

            var tokenTexts = tokens.stream() //
                    .map(AnnotationFS::getCoveredText) //
                    .toArray(String[]::new);

            for (var prediction : finder.find(tokenTexts)) {
                var label = prediction.getType();
                if (NameSample.DEFAULT_TYPE.equals(label) || BLANK_LABEL.equals(label)) {
                    label = null;
                }

                var begin = tokens.get(prediction.getStart()).getBegin();
                var end = tokens.get(prediction.getEnd() - 1).getEnd();
                var annotation = aCas.createAnnotation(predictedType, begin, end);

                annotation.setFeatureValueFromString(predictedFeature, label);
                annotation.setBooleanValue(isPredictionFeature, true);

                if (scoreFeature != null) {
                    annotation.setDoubleValue(scoreFeature, prediction.getProb());
                }

                if (traits.isCorrectionsEnabled() && correctionFeature != null
                        && prediction.getProb() > traits.getCorrectionThreshold()) {
                    annotation.setBooleanValue(correctionFeature, true);
                    annotation.setStringValue(corectionExplanationFeature, "High confidence");
                }

                aCas.addFsToIndexes(annotation);
            }
        }

        whenSuggestionsOverlapKeepLongest(aCas, predictedType, isPredictionFeature, scoreFeature);

        assert predictedRangeBegin <= predictedRangeEnd : "Begin of predicted range cannot be beyond its end";
        return new Range(predictedRangeBegin, predictedRangeEnd);
    }

    static void whenSuggestionsOverlapKeepLongest(CAS aCas, Type predictedType,
            Feature isPredictionFeature, Feature scoreFeature)
    {
        var offsetsMap = new LinkedHashMap<Offset, List<AnnotationFS>>();
        for (var candidate : aCas.<Annotation> select(predictedType)) {
            if (candidate.getBooleanValue(isPredictionFeature)) {
                var offset = new Offset(candidate.getBegin(), candidate.getEnd());
                var list = offsetsMap.computeIfAbsent(offset, $ -> new ArrayList<>());
                list.add(candidate);
            }
        }

        var offsets = new ArrayList<>(offsetsMap.keySet());
        var overlapIterator = new OverlapIterator(offsets, offsets);
        while (overlapIterator.hasNext()) {
            var overlappingAnnotations = overlapIterator.next();
            var offsetA = overlappingAnnotations.getLeft();
            var offsetB = overlappingAnnotations.getRight();

            var candidates = new ArrayList<AnnotationFS>();

            // Remove the shorter ones immediately
            if (offsetA.length() < offsetB.length()) {
                offsetsMap.get(offsetA).forEach(aCas::removeFsFromIndexes);
                candidates.addAll(offsetsMap.get(offsetB));
            }
            else if (offsetA.length() > offsetB.length()) {
                candidates.addAll(offsetsMap.get(offsetA));
                offsetsMap.get(offsetB).forEach(aCas::removeFsFromIndexes);
            }
            else if (offsetA.equals(offsetB)) {
                candidates.addAll(offsetsMap.get(offsetA));
            }
            else {
                candidates.addAll(offsetsMap.get(offsetA));
                candidates.addAll(offsetsMap.get(offsetB));
            }

            if (candidates.isEmpty()) {
                continue;
            }

            // Sort the longer ones by score
            if (scoreFeature != null) {
                candidates.sort(comparing(ann -> ann.getDoubleValue(scoreFeature)));
            }

            // Keeping only the longer one with the highest score
            candidates.subList(0, candidates.size() - 1).forEach(aCas::removeFsFromIndexes);
        }
    }

    @Override
    public int estimateSampleCount(List<CAS> aCasses)
    {
        return extractSamples(null, aCasses).size();
    }

    @Override
    public EvaluationResult evaluate(List<CAS> aCasses, DataSplitter aDataSplitter)
        throws RecommendationException
    {
        // We use sentence-based samples here even if the layer allows cross-sentence annotations
        // because with the overlapping sliding window, the evaluation would otherwise train on test
        // data.
        var sampleUnit = Sentence.class.getSimpleName();
        var data = extractSamples(aCasses, this::extractSamplesFromSentences);
        var splits = aDataSplitter.apply(data);

        var testSetSize = splits.testSet().size();
        var trainingSetSize = splits.trainingSet().size();
        var overallTrainingSize = data.size() - testSetSize;
        var trainRatio = (overallTrainingSize > 0) ? trainingSetSize / overallTrainingSize : 0.0;

        if (trainingSetSize < MIN_TRAINING_SET_SIZE || testSetSize < MIN_TEST_SET_SIZE) {
            var msg = String.format(
                    "Not enough evaluation data: training set size [%d] (min. %d), test set size [%d] (min. %d) of total [%d] (min. %d)",
                    trainingSetSize, MIN_TRAINING_SET_SIZE, testSetSize, MIN_TEST_SET_SIZE,
                    data.size(), (MIN_TRAINING_SET_SIZE + MIN_TEST_SET_SIZE));
            LOG.info(msg);

            var result = new EvaluationResult(DATAPOINT_UNIT.getSimpleName(), sampleUnit,
                    trainingSetSize, testSetSize, trainRatio);
            result.setEvaluationSkipped(true);
            result.setErrorMsg(msg);
            return result;
        }

        LOG.info("Training on [{}] samples, predicting on [{}] of total [{}]", trainingSetSize,
                testSetSize, data.size());

        // Train model
        var model = train(splits.trainingSet(), traits.getParameters());
        var nameFinder = new NameFinderME(model);

        // Evaluate
        var labelPairs = new ArrayList<LabelPair>();
        for (var sample : splits.testSet()) {
            // During evaluation, we sample data across documents and shuffle them into training and
            // tests sets. Thus, we consider every sample as coming from a unique document and
            // always clear the adaptive data between samples. clear adaptive data from feature
            // generators if necessary
            nameFinder.clearAdaptiveData();

            // Span contains one NE, Array of them all in one sentence
            var sampleTokens = sample.getSentence();
            var predictedNames = nameFinder.find(sampleTokens);
            var goldNames = sample.getNames();

            labelPairs.addAll(determineLabelsForSample(sampleTokens, predictedNames, goldNames));
        }

        return labelPairs.stream().collect(toEvaluationResult(DATAPOINT_UNIT.getSimpleName(),
                sampleUnit, trainingSetSize, testSetSize, trainRatio, NO_NE_TAG));
    }

    /**
     * Extract AnnotatedTokenPairs with info on predicted and gold label for each token of the given
     * sentence.
     */
    private List<LabelPair> determineLabelsForSample(String[] sentence, Span[] predictedNames,
            Span[] goldNames)
    {
        int predictedNameIdx = 0;
        int goldNameIdx = 0;

        var labelPairs = new ArrayList<LabelPair>();
        // Spans store which tokens are part of it as [begin,end).
        // Tokens are counted 0 to length of sentence.
        // Therefore go through all tokens, determine which span they are part of
        // for predictions and gold ones. Assign label accordingly to the annotated-token.
        for (int i = 0; i < sentence.length; i++) {

            var predictedLabel = NO_NE_TAG;
            if (predictedNameIdx < predictedNames.length) {
                var predictedName = predictedNames[predictedNameIdx];
                predictedLabel = determineLabel(predictedName, i);

                if (i > predictedName.getEnd()) {
                    predictedNameIdx++;
                }
            }

            var goldLabel = NO_NE_TAG;
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
     * Check that token index is part of the given span and return the span's label or no-label
     * (token is outside span).
     */
    private String determineLabel(Span aName, int aTokenIdx)
    {
        var label = NO_NE_TAG;

        if (aName.getStart() <= aTokenIdx && aName.getEnd() > aTokenIdx) {
            label = aName.getType();
        }

        return label;
    }

    private List<NameSample> extractSamples(RecommenderContext aContext, Iterable<CAS> aCasses)
    {
        BiFunction<CAS, List<NameSample>, Boolean> extractor;

        if (getRecommender().getLayer().isCrossSentence()) {
            if (aContext != null) {
                aContext.log(LogMessage.info(getRecommender().getName(),
                        "Training using sliding-window since layer permits cross-sentence annotations."));
            }
            extractor = this::extractSamplesUsingSlidingWindow;
        }
        else {
            extractor = this::extractSamplesFromSentences;
        }

        return extractSamples(aCasses, extractor);
    }

    private List<NameSample> extractSamples(Iterable<CAS> aCasses,
            BiFunction<CAS, List<NameSample>, Boolean> aExtractor)
    {
        var nameSamples = new ArrayList<NameSample>();

        for (var cas : aCasses) {
            var processNext = aExtractor.apply(cas, nameSamples);
            if (!processNext) {
                break;
            }
        }

        return nameSamples;
    }

    private boolean extractSamplesFromSentences(CAS aCas, List<NameSample> aSamples)
    {
        return generateSamples(aCas, new TokensBySentence(aCas), aSamples);
    }

    private boolean extractSamplesUsingSlidingWindow(CAS aCas, List<NameSample> aSamples)
    {
        var windowSize = getWindowSize(aCas);
        var windowOverlap = windowSize / 2;

        return generateSamples(aCas,
                new SlidingWindow<>(aCas, DATAPOINT_UNIT, windowSize, windowOverlap), aSamples);
    }

    private boolean generateSamples(CAS aCas, Iterable<List<Token>> aUnitProvider,
            List<NameSample> aSamples)
    {
        var trainingSetSizeLimit = traits.getTrainingSetSizeLimit();

        var firstSampleInCas = true;

        for (var tokens : aUnitProvider) {
            if (trainingSetSizeLimit > 0 && aSamples.size() >= trainingSetSizeLimit) {
                // Generated maximum number of samples
                return false;
            }

            var annotatedSpans = extractAnnotatedSpans(aCas, tokens);

            if (annotatedSpans.length > 0) {
                var tokenTexts = tokens.stream() //
                        .map(AnnotationFS::getCoveredText) //
                        .toArray(String[]::new);
                var nameSample = new NameSample(tokenTexts, annotatedSpans, firstSampleInCas);
                aSamples.add(nameSample);
                firstSampleInCas = false;
            }
        }

        return true;
    }

    private int getWindowSize(CAS aCas)
    {
        int textLengh = aCas.getDocumentText().length();

        int windowSize = traits.getWindowSize();
        if (windowSize <= 0) {
            windowSize = DEFAULT_WINDOW_SIZE;
        }

        // If the document is short try scaling down the window size to get a
        // few more samples.
        int minDesiredSamples = 10;
        if (windowSize * minDesiredSamples > textLengh) {
            windowSize = textLengh / minDesiredSamples;
        }

        // If the document is too short to accommodate the minimum training set size
        // with the current window size, scale the window size down.
        if (windowSize * MIN_TRAINING_SET_SIZE > textLengh) {
            windowSize = textLengh / MIN_TRAINING_SET_SIZE;
        }

        if (windowSize < MIN_TRAIN_WINDOW_SIZE) {
            windowSize = MIN_TRAIN_WINDOW_SIZE;
        }

        return windowSize;
    }

    private Span[] extractAnnotatedSpans(CAS aCas, List<? extends AnnotationFS> aTokens)
    {
        if (aTokens.isEmpty()) {
            return new Span[0];
        }

        // Collect relevant annotations
        var annotationType = getType(aCas, layerName);
        var feature = annotationType.getFeatureByBaseName(featureName);
        var windowBegin = aTokens.get(0).getBegin();
        var windowEnd = aTokens.get(aTokens.size() - 1).getEnd();
        var annotations = aCas.<Annotation> select(annotationType) //
                .coveredBy(windowBegin, windowEnd) //
                .asList();
        if (annotations.isEmpty()) {
            return new Span[0];
        }

        // Convert character offsets to token indices
        var idxTokenBeginOffset = new Int2ObjectOpenHashMap<AnnotationFS>();
        var idxTokenEndOffset = new Int2ObjectOpenHashMap<AnnotationFS>();
        var idxToken = new Object2IntOpenHashMap<AnnotationFS>();
        var idx = 0;
        for (var token : aTokens) {
            idxTokenBeginOffset.put(token.getBegin(), token);
            idxTokenEndOffset.put(token.getEnd(), token);
            idxToken.put(token, idx);
            idx++;
        }

        var result = new ArrayList<Span>();
        var highestEndTokenPositionObserved = -1;
        var numberOfAnnotations = annotations.size();
        for (int i = 0; i < numberOfAnnotations; i++) {
            var annotation = annotations.get(i);
            var label = annotation.getFeatureValueAsString(feature);
            if (isBlank(label)) {
                label = BLANK_LABEL;
            }

            var beginToken = idxTokenBeginOffset.get(annotation.getBegin());
            var endToken = idxTokenEndOffset.get(annotation.getEnd());
            if (beginToken == null || endToken == null) {
                LOG.warn("Skipping annotation not starting/ending at token boundaries: [{}-{}, {}]",
                        annotation.getBegin(), annotation.getEnd(), label);
                continue;
            }

            var begin = idxToken.getInt(beginToken);
            var end = idxToken.getInt(endToken);

            // If the begin offset of the current annotation is lower than the highest offset so far
            // observed, then it is overlapping with some annotation that we have seen before.
            // Because OpenNLP NER does not support overlapping annotations, we skip it.
            if (begin < highestEndTokenPositionObserved) {
                LOG.debug("Skipping overlapping annotation: [{}-{}, {}]", begin, end + 1, label);
                continue;
            }

            result.add(new Span(begin, end + 1, label));
            highestEndTokenPositionObserved = end + 1;
        }

        return result.toArray(new Span[result.size()]);
    }

    private TokenNameFinderModel train(List<NameSample> aNameSamples,
            TrainingParameters aParameters)
        throws RecommendationException
    {
        try (var stream = new NameSampleStream(aNameSamples)) {
            var finderFactory = new TokenNameFinderFactory();
            return NameFinderME.train("unknown", null, stream, aParameters, finderFactory);
        }
        catch (IOException e) {
            LOG.error("Exception during training the OpenNLP Named Entity Recognizer model.", e);
            throw new RecommendationException("Error while training OpenNLP pos", e);
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

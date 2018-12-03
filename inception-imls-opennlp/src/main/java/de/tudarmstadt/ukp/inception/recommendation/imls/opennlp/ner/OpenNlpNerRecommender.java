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
import java.util.Map.Entry;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext.Key;
import de.tudarmstadt.ukp.inception.recommendation.api.type.PredictedSpan;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import opennlp.tools.ml.BeamSearch;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.NameSample;
import opennlp.tools.namefind.TokenNameFinderEvaluator;
import opennlp.tools.namefind.TokenNameFinderFactory;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.Span;
import opennlp.tools.util.TrainingParameters;

public class OpenNlpNerRecommender
    implements RecommendationEngine
{
    public static final Key<TokenNameFinderModel> KEY_MODEL = new Key<>("opennlp_ner_model");
    private static final Logger LOG = LoggerFactory.getLogger(OpenNlpNerRecommender.class);

    private final String layerName;
    private final String featureName;
    private final OpenNlpNerRecommenderTraits traits;
    private final int maxRecommendations;

    public OpenNlpNerRecommender(Recommender aRecommender, OpenNlpNerRecommenderTraits aTraits)
    {
        layerName = aRecommender.getLayer().getName();
        featureName = aRecommender.getFeature();
        maxRecommendations = aRecommender.getMaxRecommendations();
        
        traits = aTraits;
    }

    @Override
    public void train(RecommenderContext aContext, List<CAS> aCasses)
        throws RecommendationException
    {
        List<NameSample> nameSamples = extractNameSamples(aCasses);
        
        // The beam size controls how many results are returned at most. But even if the user
        // requests only few results, we always use at least the default bean size recommended by
        // OpenNLP
        int beamSize = Math.max(maxRecommendations, NameFinderME.DEFAULT_BEAM_SIZE);

        TrainingParameters params = traits.getParameters();
        params.put(BeamSearch.BEAM_SIZE_PARAMETER, Integer.toString(beamSize));
        
        TokenNameFinderModel model = train(nameSamples, params);
        if (model != null) {
            aContext.put(KEY_MODEL, model);
            aContext.markAsReadyForPrediction();
        }
    }

    @Override
    public void predict(RecommenderContext aContext, CAS aCas) throws RecommendationException
    {
        TokenNameFinderModel model = aContext.get(KEY_MODEL).orElseThrow(() -> 
                new RecommendationException("Key [" + KEY_MODEL + "] not found in context"));
        
        NameFinderME finder = new NameFinderME(model);

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

            for (Span prediction : finder.find(tokens)) {
                String label = prediction.getType();
                if ("default".equals(label)) {
                    continue;
                }
                int begin = tokenAnnotations.get(prediction.getStart()).getBegin();
                int end = tokenAnnotations.get(prediction.getEnd() - 1).getEnd();
                AnnotationFS annotation = aCas.createAnnotation(predictionType, begin, end);
                annotation.setDoubleValue(confidenceFeature, prediction.getProb());
                annotation.setStringValue(labelFeature, label);
                aCas.addFsToIndexes(annotation);
            }
        }
    }

    @Override
    public double evaluate(List<CAS> aCasses, DataSplitter aDataSplitter)
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

        if (trainingSet.size() < 2 || testSet.size() < 2) {
            LOG.info("Not enough data to evaluate, skipping!");
            return 0.0;
        }

        LOG.info("Training on [{}] items, predicting on [{}] of total [{}]", trainingSet.size(),
                testSet.size(), data.size());

        // Train model
        TokenNameFinderModel model = train(trainingSet, traits.getParameters());
        NameFinderME nameFinder = new NameFinderME(model);

        // Evaluate
        try (NameSampleStream stream = new NameSampleStream(testSet)) {
            TokenNameFinderEvaluator evaluator = new TokenNameFinderEvaluator(nameFinder);
            evaluator.evaluate(stream);
            return evaluator.getFMeasure().getFMeasure();
        } catch (IOException e) {
            LOG.error("Exception during evaluating the OpenNLP Named Entity Recognizer model.", e);
            throw new RecommendationException("Error while evaluating OpenNlp NER", e);
        }
    }

    private List<NameSample> extractNameSamples(List<CAS> aCasses)
    {
        List<NameSample> nameSamples = new ArrayList<>();
        for (CAS cas : aCasses) {
            Type sentenceType = getType(cas, Sentence.class);
            Type tokenType = getType(cas, Token.class);

            Map<AnnotationFS, Collection<AnnotationFS>> sentences =
                indexCovered(cas, sentenceType, tokenType);
            for (Entry<AnnotationFS, Collection<AnnotationFS>> e : sentences.entrySet()) {
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
        for (int i = 0; i < numberOfAnnotations; i++) {
            AnnotationFS annotation = annotations.get(i);
            int begin = idxToken.get(idxTokenOffset.get(annotation.getBegin()));
            int end = idxToken.get(idxTokenOffset.get(annotation.getEnd()));
            String label = annotation.getFeatureValueAsString(feature);
            if (isNotBlank(label)) {
                result.add(new Span(begin, end + 1, label));
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

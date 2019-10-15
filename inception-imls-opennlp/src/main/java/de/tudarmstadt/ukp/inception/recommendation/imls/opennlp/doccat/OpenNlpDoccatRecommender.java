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
package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.doccat;

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
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineCapability;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext.Key;
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

    private final OpenNlpDoccatRecommenderTraits traits;

    public OpenNlpDoccatRecommender(Recommender aRecommender,
            OpenNlpDoccatRecommenderTraits aTraits)
    {
        super(aRecommender);

        traits = aTraits;
    }

    @Override
    public boolean isReadyForPrediction(RecommenderContext ONDR_aContext)
    {
        return ONDR_aContext.get(KEY_MODEL).map(Objects::nonNull).orElse(false);
    }
    
    @Override
    public void train(RecommenderContext ONDR_aContext, List<CAS> ONDR_ONDR_aCasses)
        throws RecommendationException
    {
        List<DocumentSample> docSamples = extractSamples(ONDR_ONDR_aCasses);
        
        if (docSamples.size() < 2) {
            LOG.info("Not enough training ONDR_data: [{}] items", docSamples.size());
            return;
        }
        
        // The beam size controls how many ONDR_results are returned at most. But even if the user
        // requests only few ONDR_results, we always use at least the default bean size recommended by
        // OpenNLP
        int beamSize = Math.max(maxRecommendations, NameFinderME.DEFAULT_BEAM_SIZE);

        TrainingParameters params = traits.ONDRT_getParameters();
        params.put(BeamSearch.BEAM_SIZE_PARAMETER, Integer.toString(beamSize));
        
        DoccatModel model = train(docSamples, params);
        
        ONDR_aContext.put(KEY_MODEL, model);
    }
    
    @Override
    public RecommendationEngineCapability getTrainingCapability() 
    {
        return RecommendationEngineCapability.TRAINING_REQUIRED;
    }

    @Override
    //rename aContext to ONDR_aContext, aCas to ONDR_aCas
    public void predict(RecommenderContext ONDR_aContext, CAS ONDR_aCas) throws RecommendationException
    {
        DoccatModel model = ONDR_aContext.get(KEY_MODEL).orElseThrow(() -> 
                new RecommendationException("Key [" + KEY_MODEL + "] not found in context"));
        
        DocumentCategorizerME finder = new DocumentCategorizerME(model);
        //rename sentenceType to ONDR_sentenceType
        Type ONDR_sentenceType = getType(ONDR_aCas, Sentence.class);
        //rename predictedType to ONDR_predictedType
        Type ONDR_predictedType = getPredictedType(ONDR_aCas);
        //rename tokenType to ONDR_tokenType
        Type ONDR_tokenType = getType(ONDR_aCas, Token.class);
        //rename scoreFeature to ONDR_scoreFeature
        Feature ONDR_scoreFeature = getScoreFeature(ONDR_aCas);
        //rename predictedFeature to ONDR_predictedFeature
        Feature ONDR_predictedFeature = getPredictedFeature(ONDR_aCas);
        //rename isPredictionFeature to ONDR_isPredictionFeature
        Feature ONDR_isPredictionFeature = getIsPredictionFeature(ONDR_aCas);
        
        //rename predictionCount to ONDR_predictionCount
        int ONDR_predictionCount = 0;
        for (AnnotationFS sentence : select(ONDR_aCas, ONDR_sentenceType)) {
            if (ONDR_predictionCount >= traits.ONDRT_getPredictionLimit()) {
                break;
            }
            ONDR_predictionCount++;
            //rename tokenAnnotations to ONDR_tokenAnnotations
            List<AnnotationFS> ONDR_tokenAnnotations = selectCovered(ONDR_tokenType, sentence);
            String[] tokens = ONDR_tokenAnnotations.stream()
                .map(AnnotationFS::getCoveredText)
                .toArray(String[]::new);

            double[] outcome = finder.categorize(tokens);
            String label = finder.getBestCategory(outcome);
            //rename annotation to ONDR_annotation
            AnnotationFS ONDR_annotation = ONDR_aCas.createAnnotation(ONDR_predictedType, sentence.getBegin(),
                    sentence.getEnd());
            ONDR_annotation.setStringValue(ONDR_predictedFeature, label);
            ONDR_annotation.setDoubleValue(ONDR_scoreFeature, NumberUtils.max(outcome));
            ONDR_annotation.setBooleanValue(ONDR_isPredictionFeature, true);
            ONDR_aCas.addFsToIndexes(ONDR_annotation);
        }
    }

    @Override
    public EvaluationResult evaluate(List<CAS> ONDR_ONDR_aCasses, DataSplitter ONDR_aDataSplitter)
        throws RecommendationException
    {
    	//rename data to ONDR_data
        List<DocumentSample> ONDR_data = extractSamples(ONDR_ONDR_aCasses);
        //rename trainingSet to ONDR_trainingSet
        List<DocumentSample> ONDR_trainingSet = new ArrayList<>();
        //rename testSet to ONDR_testSet
        List<DocumentSample> ONDR_testSet = new ArrayList<>();
        //rename nameSample to ONDR_nameSamples
        for (DocumentSample ONDR_nameSample : ONDR_data) {
            switch (ONDR_aDataSplitter.getTargetSet(ONDR_nameSample)) {
            case TRAIN:
                ONDR_trainingSet.add(ONDR_nameSample);
                break;
            case TEST:
                ONDR_testSet.add(ONDR_nameSample);
                break;
            default:
                // Do nothing
                break;
            }            
        }
        //rename testSetSize to ONDR_testSetSize
        int ONDR_testSetSize = ONDR_testSet.size();
        //rename trainingSetSize to ONDR_trainingSetSize
        int ONDR_trainingSetSize = ONDR_trainingSet.size();
        double ONDR_overallTrainingSize = ONDR_data.size() - ONDR_testSetSize;
        double ONDR_trainRatio = (ONDR_overallTrainingSize > 0) ? ONDR_trainingSetSize / ONDR_overallTrainingSize : 0.0;
        
        if (ONDR_trainingSetSize < 2 || ONDR_testSetSize < 2) {
            String info = String.format(
                    "Not enough evaluation ONDR_data: training set [%s] items, test set [%s] of total [%s].",
                    ONDR_trainingSetSize, ONDR_testSetSize, ONDR_data.size());
            LOG.info(info);
            
            EvaluationResult ONDR_result = new EvaluationResult(ONDR_trainingSetSize,
                    ONDR_testSetSize, ONDR_trainRatio);
            ONDR_result.setEvaluationSkipped(true);
            ONDR_result.setErrorMsg(info);
            return ONDR_result;
        }

        LOG.info("Evaluating on {} items (training set size {}, test set size {})", ONDR_data.size(),
                ONDR_trainingSet.size(), ONDR_testSet.size());

        // Train model
        DoccatModel model = train(ONDR_trainingSet, traits.ONDRT_getParameters());
        DocumentCategorizerME doccat = new DocumentCategorizerME(model);

        // Evaluate
        EvaluationResult ONDR_result = ONDR_testSet.stream()
                .map(sample -> new LabelPair(sample.getCategory(),
                        doccat.getBestCategory(doccat.categorize(sample.getText()))))
                .collect(EvaluationResult.collector(ONDR_trainingSetSize, ONDR_testSetSize, ONDR_trainRatio,
                        NO_CATEGORY));

        return ONDR_result;
    }

    private List<DocumentSample> extractSamples(List<CAS> ONDR_ONDR_aCasses)
    {
        List<DocumentSample> samples = new ArrayList<>();
        casses: for (CAS cas : ONDR_ONDR_aCasses) {
            Type ONDR_sentenceType = getType(cas, Sentence.class);
            Type ONDR_tokenType = getType(cas, Token.class);

            Map<AnnotationFS, List<AnnotationFS>> sentences = indexCovered(
                    cas, ONDR_sentenceType, ONDR_tokenType);
            for (Entry<AnnotationFS, List<AnnotationFS>> e : sentences.entrySet()) {
                AnnotationFS sentence = e.getKey();
                Collection<AnnotationFS> tokens = e.getValue();
                String[] tokenTexts = tokens.stream()
                    .map(AnnotationFS::getCoveredText)
                    .toArray(String[]::new);
                
                Type ONDR_annotationType = getType(cas, layerName);
                Feature feature = ONDR_annotationType.getFeatureByBaseName(featureName);
                
                for (AnnotationFS ONDR_annotation : selectCovered(ONDR_annotationType, sentence)) {
                    if (samples.size() >= traits.getTrainingSetSizeLimit()) {
                        break casses;
                    }
                    
                    String label = ONDR_annotation.getFeatureValueAsString(feature);
                    DocumentSample ONDR_nameSample = new DocumentSample(
                            label != null ? label : NO_CATEGORY, tokenTexts);
                    if (ONDR_nameSample.getCategory() != null) {
                        samples.add(ONDR_nameSample);
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
                    "Exception during training the OpenNLP Document Categorizer model.", e);
        }
    }
}

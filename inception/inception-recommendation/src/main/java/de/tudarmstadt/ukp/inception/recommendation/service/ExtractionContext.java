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
package de.tudarmstadt.ukp.inception.recommendation.service;

import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_AUTO_ACCEPT_MODE_SUFFIX;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_IS_PREDICTION;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_SCORE_EXPLANATION_SUFFIX;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_SCORE_SUFFIX;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.FEAT_REL_TARGET;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING_ARRAY;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.util.CasUtil;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;

public final class ExtractionContext
{
    private final int generation;

    private final SourceDocument document;
    private final CAS originalCas;
    private final CAS predictionCas;
    private final String documentText;

    private final Recommender recommender;

    private final AnnotationLayer layer;
    private final String typeName;
    private final String featureName;

    private final Type predictedType;

    private final Feature labelFeature;
    private final Feature sourceFeature;
    private final Feature targetFeature;
    private final Feature scoreFeature;
    private final Feature scoreExplanationFeature;
    private final Feature modeFeature;
    private final Feature predictionFeature;

    private final boolean isMultiLabels;

    private final List<AnnotationSuggestion> result;

    public ExtractionContext(int aGeneration, Recommender aRecommender, SourceDocument aDocument,
            CAS aOriginalCas, CAS aPredictionCas)
    {
        recommender = aRecommender;

        document = aDocument;
        originalCas = aOriginalCas;
        documentText = originalCas.getDocumentText();
        predictionCas = aPredictionCas;

        generation = aGeneration;
        layer = aRecommender.getLayer();
        featureName = aRecommender.getFeature().getName();
        typeName = layer.getName();

        predictedType = CasUtil.getType(aPredictionCas, typeName);
        labelFeature = predictedType.getFeatureByBaseName(featureName);
        sourceFeature = predictedType.getFeatureByBaseName(FEAT_REL_SOURCE);
        targetFeature = predictedType.getFeatureByBaseName(FEAT_REL_TARGET);
        scoreFeature = predictedType.getFeatureByBaseName(featureName + FEATURE_NAME_SCORE_SUFFIX);
        scoreExplanationFeature = predictedType
                .getFeatureByBaseName(featureName + FEATURE_NAME_SCORE_EXPLANATION_SUFFIX);
        modeFeature = predictedType
                .getFeatureByBaseName(featureName + FEATURE_NAME_AUTO_ACCEPT_MODE_SUFFIX);
        predictionFeature = predictedType.getFeatureByBaseName(FEATURE_NAME_IS_PREDICTION);
        isMultiLabels = TYPE_NAME_STRING_ARRAY.equals(labelFeature.getRange().getName());

        result = new ArrayList<AnnotationSuggestion>();
    }

    public int getGeneration()
    {
        return generation;
    }

    public SourceDocument getDocument()
    {
        return document;
    }

    public CAS getOriginalCas()
    {
        return originalCas;
    }

    public CAS getPredictionCas()
    {
        return predictionCas;
    }

    public String getDocumentText()
    {
        return documentText;
    }

    public Recommender getRecommender()
    {
        return recommender;
    }

    public AnnotationLayer getLayer()
    {
        return layer;
    }

    public String getTypeName()
    {
        return typeName;
    }

    public String getFeatureName()
    {
        return featureName;
    }

    public Type getPredictedType()
    {
        return predictedType;
    }

    public Feature getLabelFeature()
    {
        return labelFeature;
    }

    public Feature getSourceFeature()
    {
        return sourceFeature;
    }

    public Feature getTargetFeature()
    {
        return targetFeature;
    }

    public Feature getScoreFeature()
    {
        return scoreFeature;
    }

    public Feature getScoreExplanationFeature()
    {
        return scoreExplanationFeature;
    }

    public Feature getModeFeature()
    {
        return modeFeature;
    }

    public Feature getPredictionFeature()
    {
        return predictionFeature;
    }

    public boolean isMultiLabels()
    {
        return isMultiLabels;
    }

    public List<AnnotationSuggestion> getResult()
    {
        return result;
    }
}
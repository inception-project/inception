/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.inception.recommendation.api;

import java.util.List;

import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;

/**
 * The main contact point of the Recommendation module. This interface can be injected in the wicket
 * pages. It is used to pull the latest recommendations for an annotation layer and render them.
 */
public interface RecommendationService
{
    static final String SERVICE_NAME = "recommendationService";
    
    void createOrUpdateRecommender(Recommender aRecommender);

    void deleteRecommender(Recommender aRecommender);
    
    Recommender getRecommender(long aId);

    List<Recommender> listRecommenders(Project aProject);

    List<Recommender> listRecommenders(AnnotationLayer aLayer);
    
    /**
     * Returns all annotation layers in the given project which have any enabled recommenders.
     */
    List<AnnotationLayer> listLayersWithEnabledRecommenders(Project aProject);

    /**
     * @deprecated Use {@link ClassificationToolRegistry#getTools}
     */
    @Deprecated
    List<String> getAvailableTools(AnnotationLayer aLayer, AnnotationFeature aFeature); 

    ClassificationTool<?> getTool(Recommender aSettings, int aMaxPredictions);

    void setActiveRecommenders(User aUser, AnnotationLayer layer,
            List<Recommender> selectedClassificationTools);
    
    List<Recommender> getActiveRecommenders(User aUser, AnnotationLayer aLayer);
    
    void storeTrainedModel(User aUser, Recommender aRecommender, Object aTrain);
    
    Object getTrainedModel(User aUser, Recommender aRecommender);
    
    void setMaxSuggestions(User aUser, int aMax);
    
    int getMaxSuggestions(User aUser);
    
    Predictions getPredictions(User aUser, Project aProject);

    Predictions getIncomingPredictions(User aUser, Project aProject);
    
    void putIncomingPredictions(User aUser, Project aProject, Predictions aPredictions);
    
    void switchPredictions(User aUser, Project aProject);

    void setFeatureValue(AnnotationFeature aFeature, String aPredictedValue,
        SpanAdapter aAdapter, AnnotatorState aState, JCas aJcas, int address);
}

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
package de.tudarmstadt.ukp.inception.recommendation.service;

import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.classificationtool.ClassificationTool;
import de.tudarmstadt.ukp.inception.recommendation.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.model.Recommender;

/**
 * The main contact point of the Recommendation module. This interface can be injected in the wicket
 * pages. It is used to pull the latest recommendations for an annotation layer and render them.
 */
public interface RecommendationService
{
    static final String SERVICE_NAME = "recommendationService";
    
    /**
     * Sets up the recommendation service, i.e. it creates and starts the scheduled selection,
     * training and prediction tasks. This should be called if a document is opened for annotation.
     * 
     * @param aState
     *            The annotatorState for the document which is annotated.
     */
    void init(AnnotatorState aState);
    
    void createOrUpdateRecommender(Recommender aSettings);

    void deleteRecommender(Recommender aSettings);

    List<Recommender> listRecommenders(Project aProject);

    List<Recommender> listRecommenders(AnnotationLayer aLayer);

    List<String> getAvailableTools(AnnotationLayer aLayer); 

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
    
    void switchPredictions(User aUser, Project aProject);
}

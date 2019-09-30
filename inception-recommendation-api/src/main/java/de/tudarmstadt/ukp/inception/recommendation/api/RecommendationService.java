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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.recommendation.api.model.EvaluatedRecommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Preferences;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;

/**
 * The main contact point of the Recommendation module. This interface can be injected in the wicket
 * pages. It is used to pull the latest recommendations for an annotation layer and render them.
 */
public interface RecommendationService
{
    String SERVICE_NAME = "recommendationService";
    String FEATURE_NAME_IS_PREDICTION = "inception_internal_predicted";
    String FEATURE_NAME_SCORE_SUFFIX = "_score";
    String FEATURE_NAME_SCORE_EXPLANATION_SUFFIX = "_score_explanation";

    int MAX_RECOMMENDATIONS_DEFAULT = 3; 
    int MAX_RECOMMENDATIONS_CAP = 10; 
    
    void createOrUpdateRecommender(Recommender aRecommender);

    void deleteRecommender(Recommender aRecommender);
    
    Recommender getRecommender(long aId);

    Optional<Recommender> getRecommender(Project aProject, String aName);

    boolean existsRecommender(Project aProject, String aName);
    
    List<Recommender> listRecommenders(Project aProject);

    List<Recommender> listRecommenders(AnnotationLayer aLayer);
    
    Optional<Recommender> getEnabledRecommender(long aRecommenderId);
    
    List<Recommender> listEnabledRecommenders(Project aProject);

    /**
     * Returns all annotation layers in the given project which have any enabled recommenders.
     */
    List<AnnotationLayer> listLayersWithEnabledRecommenders(Project aProject);

    RecommendationEngineFactory getRecommenderFactory(Recommender aRecommender);

    boolean hasActiveRecommenders(String aUser, Project aProject);
    
    void setActiveRecommenders(User aUser, AnnotationLayer layer,
            List<EvaluatedRecommender> selectedClassificationTools);
    
    List<EvaluatedRecommender> getActiveRecommenders(User aUser, AnnotationLayer aLayer);

    void setPreferences(User aUser, Project aProject, Preferences aPreferences);
    
    Preferences getPreferences(User aUser, Project aProject);
    
    Predictions getPredictions(User aUser, Project aProject);

    Predictions getIncomingPredictions(User aUser, Project aProject);
    
    void putIncomingPredictions(User aUser, Project aProject, Predictions aPredictions);
    
    boolean switchPredictions(User aUser, Project aProject);

    /**
     * Returns the {@code RecommenderContext} for the given recommender if it exists.
     * 
     * @param aUser
     *            The owner of the context
     * @param aRecommender
     *            The recommender to which the desired context belongs
     * @return The context of the given recommender if there is one, or an empty one
     */
    Optional<RecommenderContext> getContext(User aUser, Recommender aRecommender);

    /**
     * Publishes a new context for the given recommender.
     * 
     * @param aUser
     *            The owner of the context.
     * @param aRecommender
     *            The recommender to which the desired context belongs.
     * @param aContext
     *            The new active context of the given recommender.
     */
    void putContext(User aUser, Recommender aRecommender, RecommenderContext aContext);
    
    /**
     * Uses the given annotation suggestion to create a new annotation or to update a feature in an
     * existing annotation.
     * 
     * @return the CAS address of the created/updated annotation.
     */
    public int upsertFeature(AnnotationSchemaService annotationService, SourceDocument aDocument,
            String aUsername, CAS aCas, AnnotationLayer layer, AnnotationFeature aFeature,
            String aValue, int aBegin, int aEnd)
        throws AnnotationException;
    
    Predictions computePredictions(User aUser, Project aProject, List<SourceDocument> aDocuments);
    
    void calculateVisibility(CAS aCas, String aUser, AnnotationLayer aLayer,
            Collection<SuggestionGroup> aRecommendations, int aWindowBegin, int aWindowEnd);

    List<Recommender> listEnabledRecommenders(AnnotationLayer aLayer);

    void clearState(String aUsername);

    void triggerTrainingAndClassification(String aUser, Project aProject, String aEventName);
}

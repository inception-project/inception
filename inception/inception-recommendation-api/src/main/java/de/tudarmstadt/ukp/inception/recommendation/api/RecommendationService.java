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
package de.tudarmstadt.ukp.inception.recommendation.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanAdapter;
import de.tudarmstadt.ukp.inception.preferences.Key;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.EvaluatedRecommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Preferences;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Progress;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RecommenderGeneralSettings;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RelationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.scheduling.TaskMonitor;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.support.logging.LogMessageGroup;

/**
 * The main contact point of the Recommendation module. This interface can be injected in the Wicket
 * pages. It is used to pull the latest recommendations for an annotation layer and render them.
 */
public interface RecommendationService
{
    Key<RecommenderGeneralSettings> KEY_RECOMMENDER_GENERAL_SETTINGS = new Key<>(
            RecommenderGeneralSettings.class, "recommendation/general");

    String FEATURE_NAME_IS_PREDICTION = "inception_internal_predicted";
    String FEATURE_NAME_SCORE_SUFFIX = "_score";
    String FEATURE_NAME_SCORE_EXPLANATION_SUFFIX = "_score_explanation";
    String FEATURE_NAME_AUTO_ACCEPT_MODE_SUFFIX = "_auto_accept";

    int MAX_RECOMMENDATIONS_DEFAULT = 3;
    int MAX_RECOMMENDATIONS_CAP = 10;

    void createOrUpdateRecommender(Recommender aRecommender);

    void deleteRecommender(Recommender aRecommender);

    Recommender getRecommender(long aId);

    Optional<Recommender> getRecommender(Project aProject, String aName);

    boolean existsRecommender(Project aProject, String aName);

    List<Recommender> listRecommenders(Project aProject);

    boolean existsEnabledRecommender(Project aProject);

    List<Recommender> listRecommenders(AnnotationLayer aLayer);

    Optional<Recommender> getEnabledRecommender(long aRecommenderId);

    List<Recommender> listEnabledRecommenders(Project aProject);

    List<Recommender> listEnabledRecommenders(AnnotationLayer aLayer);

    /**
     * @param aProject
     *            the project
     * @return all annotation layers in the given project which have any enabled recommenders.
     */
    List<AnnotationLayer> listLayersWithEnabledRecommenders(Project aProject);

    /**
     * @param aRecommender
     *            the recommender
     * @return the recommender factory for the given recommender. This can be empty if e.g. a
     *         recommender is only available behind a feature flag that was once enabled and now is
     *         disabled.
     */
    Optional<RecommendationEngineFactory<?>> getRecommenderFactory(Recommender aRecommender);

    boolean hasActiveRecommenders(String aSessionOwner, Project aProject);

    List<EvaluatedRecommender> getActiveRecommenders(User aSessionOwner, Project aProject);

    void setEvaluatedRecommenders(User aSessionOwner, AnnotationLayer layer,
            List<EvaluatedRecommender> selectedClassificationTools);

    List<EvaluatedRecommender> getEvaluatedRecommenders(User aSessionOwner, AnnotationLayer aLayer);

    Optional<EvaluatedRecommender> getEvaluatedRecommender(User aSessionOwner,
            Recommender aRecommender);

    List<EvaluatedRecommender> getActiveRecommenders(User aSessionOwner, AnnotationLayer aLayer);

    void setPreferences(User aSessionOwner, Project aProject, Preferences aPreferences);

    Preferences getPreferences(User aSessionOwner, Project aProject);

    Predictions getPredictions(User aSessionOwner, Project aProject);

    Predictions getIncomingPredictions(User aSessionOwner, Project aProject);

    void putIncomingPredictions(User aSessionOwner, Project aProject, Predictions aPredictions);

    boolean switchPredictions(String aSessionOwner, Project aProject);

    /**
     * Returns the {@code RecommenderContext} for the given recommender if it exists.
     *
     * @param aSessionOwner
     *            The owner of the context
     * @param aRecommender
     *            The recommender to which the desired context belongs
     * @return The context of the given recommender if there is one, or an empty one
     */
    Optional<RecommenderContext> getContext(String aSessionOwner, Recommender aRecommender);

    /**
     * Publishes a new context for the given recommender.
     *
     * @param aSessionOwner
     *            The owner of the context.
     * @param aRecommender
     *            The recommender to which the desired context belongs.
     * @param aContext
     *            The new active context of the given recommender.
     */
    void putContext(User aSessionOwner, Recommender aRecommender, RecommenderContext aContext);

    AnnotationFS correctSuggestion(String aSessionOwner, SourceDocument aDocument,
            String aDataOwner, CAS aCas, SpanAdapter aAdapter, AnnotationFeature aFeature,
            SpanSuggestion aOriginalSuggestion, SpanSuggestion aCorrectedSuggestion,
            LearningRecordChangeLocation aLocation)
        throws AnnotationException;

    /**
     * Uses the given annotation suggestion to create a new annotation or to update a feature in an
     * existing annotation.
     * 
     * @param aDocument
     *            the source document to which the annotations belong
     * @param aDataOwner
     *            the annotator user to whom the annotations belong
     * @param aCas
     *            the CAS containing the annotations
     * @param aAdapter
     *            an adapter for the layer to upsert
     * @param aFeature
     *            the feature on the layer that should be upserted
     * @param aSuggestion
     *            the suggestion
     * @param aLocation
     *            the location from where the change was triggered
     * @return the created/updated annotation.
     * @throws AnnotationException
     *             if there was an annotation-level problem
     */
    AnnotationFS acceptSuggestion(String aSessionOwner, SourceDocument aDocument, String aDataOwner,
            CAS aCas, SpanAdapter aAdapter, AnnotationFeature aFeature, SpanSuggestion aSuggestion,
            LearningRecordChangeLocation aLocation)
        throws AnnotationException;

    /**
     * Uses the given annotation suggestion to create a new annotation or to update a feature in an
     * existing annotation.
     * 
     * @param aDocument
     *            the source document to which the annotations belong
     * @param aDataOwner
     *            the annotator user to whom the annotations belong
     * @param aCas
     *            the CAS containing the annotations
     * @param aAdapter
     *            an adapter for the layer to upsert
     * @param aFeature
     *            the feature on the layer that should be upserted
     * @param aSuggestion
     *            the suggestion
     * @param aLocation
     *            the location from where the change was triggered
     * @param aAction
     *            TODO
     * @return the created/updated annotation.
     * @throws AnnotationException
     *             if there was an annotation-level problem
     */
    AnnotationFS acceptSuggestion(String aSessionOwner, SourceDocument aDocument, String aDataOwner,
            CAS aCas, RelationAdapter aAdapter, AnnotationFeature aFeature,
            RelationSuggestion aSuggestion, LearningRecordChangeLocation aLocation,
            LearningRecordType aAction)
        throws AnnotationException;

    void rejectSuggestion(String aSessionOwner, SourceDocument aDocument, String aDataOwner,
            AnnotationSuggestion suggestion, LearningRecordChangeLocation aAction);

    void skipSuggestion(String aSessionOwner, SourceDocument aDocument, String aDataOwner,
            AnnotationSuggestion suggestion, LearningRecordChangeLocation aAction);

    /**
     * Compute predictions.
     *
     * @param aSessionOwner
     *            the user to compute the predictions for.
     * @param aProject
     *            the project to compute the predictions for.
     * @param aDocuments
     *            the documents to compute the predictions for.
     * @param aDataOwner
     *            the owner of the annotations.
     * @return the new predictions.
     */
    Predictions computePredictions(User aSessionOwner, Project aProject,
            List<SourceDocument> aDocuments, String aDataOwner, TaskMonitor aMonitor);

    /**
     * Compute predictions.
     *
     * @param aSessionOwner
     *            the user to compute the predictions for.
     * @param aProject
     *            the project to compute the predictions for.
     * @param aCurrentDocument
     *            the document to compute the predictions for.
     * @param aDataOwner
     *            the owner of the annotations.
     * @param aInherit
     *            any documents for which to inherit the predictions from a previous run
     * @param aPredictionBegin
     *            begin of the prediction range (negative to predict from 0)
     * @param aPredictionEnd
     *            end of the prediction range (negative to predict until the end of the document)
     * @return the new predictions.
     */
    Predictions computePredictions(User aSessionOwner, Project aProject,
            SourceDocument aCurrentDocument, String aDataOwner, List<SourceDocument> aInherit,
            int aPredictionBegin, int aPredictionEnd, TaskMonitor aMonitor);

    void calculateSpanSuggestionVisibility(String aSessionOwner, SourceDocument aDocument, CAS aCas,
            String aUser, AnnotationLayer aLayer,
            Collection<SuggestionGroup<SpanSuggestion>> aRecommendations, int aWindowBegin,
            int aWindowEnd);

    void calculateRelationSuggestionVisibility(String aSessionOwner, CAS aCas, String aUser,
            AnnotationLayer aLayer,
            Collection<SuggestionGroup<RelationSuggestion>> aRecommendations, int aWindowBegin,
            int aWindowEnd);

    void clearState(String aSessionOwner);

    void triggerPrediction(String aSessionOwner, String aEventName, SourceDocument aDocument,
            String aDocumentOwner);

    void triggerTrainingAndPrediction(String aSessionOwner, Project aProject, String aEventName,
            SourceDocument aCurrentDocument, String aDocumentOwner);

    void triggerSelectionTrainingAndPrediction(String aSessionOwner, Project aProject,
            String aEventName, SourceDocument aCurrentDocument, String aDocumentOwner);

    boolean isPredictForAllDocuments(String aSessionOwner, Project aProject);

    void setPredictForAllDocuments(String aSessionOwner, Project aProject,
            boolean aPredictForAllDocuments);

    List<LogMessageGroup> getLog(String aSessionOwner, Project aProject);

    /**
     * @return the total amount of enabled recommenders
     */
    long countEnabledRecommenders();

    Progress getProgressTowardsNextEvaluation(User aSessionOwner, Project aProject);
}

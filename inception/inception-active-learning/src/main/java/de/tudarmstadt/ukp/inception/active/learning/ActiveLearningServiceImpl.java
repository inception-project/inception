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
package de.tudarmstadt.ukp.inception.active.learning;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateChangeFlag.EXPLICIT_ANNOTATOR_USER_ACTION;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation.AL_SIDEBAR;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.ACCEPTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.CORRECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.REJECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.SKIPPED;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.ClassUtils;
import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.active.learning.config.ActiveLearningAutoConfiguration;
import de.tudarmstadt.ukp.inception.active.learning.event.ActiveLearningRecommendationEvent;
import de.tudarmstadt.ukp.inception.active.learning.strategy.ActiveLearningStrategy;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup.Delta;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ActiveLearningAutoConfiguration#activeLearningService}.
 * </p>
 */
public class ActiveLearningServiceImpl
    implements ActiveLearningService
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ApplicationEventPublisher applicationEventPublisher;
    private final DocumentService documentService;
    private final RecommendationService recommendationService;
    private final UserDao userService;
    private final LearningRecordService learningHistoryService;
    private final AnnotationSchemaService schemaService;
    private final FeatureSupportRegistry featureSupportRegistry;

    @Autowired
    public ActiveLearningServiceImpl(DocumentService aDocumentService,
            RecommendationService aRecommendationService, UserDao aUserService,
            LearningRecordService aLearningHistoryService, AnnotationSchemaService aSchemaService,
            ApplicationEventPublisher aApplicationEventPublisher,
            FeatureSupportRegistry aFeatureSupportRegistry)
    {
        documentService = aDocumentService;
        recommendationService = aRecommendationService;
        userService = aUserService;
        learningHistoryService = aLearningHistoryService;
        applicationEventPublisher = aApplicationEventPublisher;
        schemaService = aSchemaService;
        featureSupportRegistry = aFeatureSupportRegistry;
    }

    @Override
    public List<SuggestionGroup<SpanSuggestion>> getSuggestions(User aUser, AnnotationLayer aLayer)
    {
        var predictions = recommendationService.getPredictions(aUser, aLayer.getProject());

        if (predictions == null) {
            return emptyList();
        }

        var recommendationsMap = predictions.getPredictionsForWholeProject(SpanSuggestion.class,
                aLayer, documentService);

        return recommendationsMap.values().stream() //
                .flatMap(docMap -> docMap.stream()) //
                .collect(toList());
    }

    @Override
    public boolean hasSkippedSuggestions(String aSessionOwner, User aDataOwner,
            AnnotationLayer aLayer)
    {
        return learningHistoryService.hasSkippedSuggestions(aSessionOwner, aDataOwner, aLayer);
    }

    @Override
    public void hideRejectedOrSkippedAnnotations(String aSessionOwner, User aDataOwner,
            AnnotationLayer aLayer, boolean filterSkippedRecommendation,
            List<SuggestionGroup<SpanSuggestion>> aSuggestionGroups)
    {
        var records = learningHistoryService.listLearningRecords(aSessionOwner,
                aDataOwner.getUsername(), aLayer);

        for (var suggestionGroup : aSuggestionGroups) {
            for (var suggestion : suggestionGroup) {
                // If a suggestion is already invisible, we don't need to check if it needs hiding.
                // Mind that this code does not unhide the suggestion immediately if a user
                // deletes a skip learning record - it will only get unhidden after the next
                // prediction run (unless the learning-record-deletion code does an explicit
                // unhiding).
                if (!suggestion.isVisible()) {
                    continue;
                }

                records.stream()
                        .filter(r -> Objects.equals(r.getSourceDocument().getId(),
                                suggestion.getDocumentId())
                                && r.getOffsetBegin() == suggestion.getBegin()
                                && r.getOffsetEnd() == suggestion.getEnd()
                                && suggestion.labelEquals(r.getAnnotation()))
                        .forEach(record -> suggestion.hideSuggestion(record.getUserAction()));
            }
        }
    }

    @Override
    public Optional<Delta<SpanSuggestion>> generateNextSuggestion(String aSessionOwner,
            User aDataOwner, ActiveLearningUserState alState)
    {
        // Fetch the next suggestion to present to the user (if there is any)
        long startTimer = System.currentTimeMillis();
        var suggestionGroups = alState.getSuggestions();
        long getRecommendationsFromRecommendationService = System.currentTimeMillis();
        LOG.trace("Getting recommendations from recommender system took {} ms.",
                (getRecommendationsFromRecommendationService - startTimer));

        // hide rejected recommendations
        hideRejectedOrSkippedAnnotations(aSessionOwner, aDataOwner, alState.getLayer(), true,
                suggestionGroups);
        long removeRejectedSkippedRecommendation = System.currentTimeMillis();
        LOG.trace("Hiding rejected or skipped ones took {} ms.",
                (removeRejectedSkippedRecommendation
                        - getRecommendationsFromRecommendationService));

        // remove duplicate recommendations
        suggestionGroups = suggestionGroups.stream() //
                .map(it -> removeDuplicatesAndHiddenSuggestions(it)) //
                .filter(it -> !it.isEmpty()) //
                .collect(toList());
        long removeDuplicateRecommendation = System.currentTimeMillis();
        LOG.trace("Removing duplicate recommendations took {} ms.",
                (removeDuplicateRecommendation - removeRejectedSkippedRecommendation));

        var pref = recommendationService.getPreferences(aDataOwner,
                alState.getLayer().getProject());
        var nextSuggestion = alState.getStrategy().generateNextSuggestion(pref, suggestionGroups);
        assert !nextSuggestion.isPresent() || nextSuggestion.get().getFirst()
                .isVisible() : "Generated suggestion must be visible";
        return nextSuggestion;
    }

    @Override
    @Transactional
    public void acceptSpanSuggestion(SourceDocument aDocument, User aDataOwner,
            Predictions aPredictions, SpanSuggestion aSuggestion, Object aValue)
        throws IOException, AnnotationException
    {
        // Upsert an annotation based on the suggestion
        var layer = schemaService.getLayer(aDocument.getProject(), aSuggestion.getLayerId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No such layer: [" + aSuggestion.getLayerId() + "]"));
        var feature = schemaService.getFeature(aSuggestion.getFeature(), layer);

        // Load CAS in which to create the annotation. This might be different from the one that
        // is currently viewed by the user, e.g. if the user switched to another document after
        // the suggestion has been loaded into the sidebar.
        var sessionOwner = userService.getCurrentUsername();
        var dataOwner = aDataOwner.getUsername();
        var cas = documentService.readAnnotationCas(aDocument, AnnotationSet.forUser(aDataOwner));

        // Create AnnotationFeature and FeatureSupport
        var featureSupport = featureSupportRegistry.findExtension(feature).orElseThrow();

        var label = unwrapLabel(aValue, feature, cas, featureSupport);

        // Clone of the original suggestion with the selected by the user
        var suggestionWithUserSelectedLabel = aSuggestion.toBuilder().withLabel(label).build();

        // If the action was a correction (i.e. suggestion label != annotation value) then generate
        // a rejection for the original value - we do not want the original value to re-appear
        var action = aSuggestion.labelEquals(label) ? ACCEPTED : CORRECTED;
        if (action == CORRECTED) {
            recommendationService.correctSuggestion(sessionOwner, aDocument, dataOwner, cas,
                    aPredictions, aSuggestion, suggestionWithUserSelectedLabel, AL_SIDEBAR);
        }
        else {
            recommendationService.acceptSuggestion(sessionOwner, aDocument, dataOwner, cas,
                    aPredictions, suggestionWithUserSelectedLabel, AL_SIDEBAR);
        }

        // Save CAS after annotation has been created
        documentService.writeAnnotationCas(cas, aDocument, aDataOwner,
                EXPLICIT_ANNOTATOR_USER_ACTION);

        // Send an application event indicating if the user has accepted/skipped/corrected/rejected
        // the suggestion
        var alternativeSuggestions = recommendationService
                .getPredictions(aDataOwner, feature.getProject())
                .getPredictionsByTokenAndFeature(suggestionWithUserSelectedLabel.getDocumentId(),
                        feature.getLayer(), suggestionWithUserSelectedLabel.getBegin(),
                        suggestionWithUserSelectedLabel.getEnd(),
                        suggestionWithUserSelectedLabel.getFeature());

        applicationEventPublisher.publishEvent(new ActiveLearningRecommendationEvent(this,
                aDocument, suggestionWithUserSelectedLabel, dataOwner, feature.getLayer(),
                suggestionWithUserSelectedLabel.getFeature(), action, alternativeSuggestions));
    }

    private String unwrapLabel(Object aValue, AnnotationFeature feature, CAS cas,
            FeatureSupport<Object> featureSupport)
    {
        Object rawLabel = featureSupport.unwrapFeatureValue(feature, aValue);
        if (rawLabel instanceof Collection collectionValue) {
            rawLabel = collectionValue.iterator().next();
        }

        if (rawLabel == null) {
            return null;
        }

        if (ClassUtils.isPrimitiveOrWrapper(rawLabel.getClass())) {
            return String.valueOf(rawLabel);
        }

        if (rawLabel instanceof String) {
            return (String) rawLabel;
        }

        throw new IllegalArgumentException(
                "Non-primitive suggestions are not supported: [" + rawLabel.getClass() + "]");
    }

    @Override
    @Transactional
    public void rejectSpanSuggestion(String aSessionOwner, User aDataOwner, AnnotationLayer aLayer,
            SpanSuggestion aSuggestion)
        throws AnnotationException
    {
        var document = documentService.getSourceDocument(aLayer.getProject().getId(),
                aSuggestion.getDocumentId());
        recommendationService.rejectSuggestion(aSessionOwner, document, aDataOwner.getUsername(),
                aSuggestion, AL_SIDEBAR);

        // Send an application event indicating if the user has accepted/skipped/corrected/rejected
        // the suggestion
        var alternativeSuggestions = recommendationService
                .getPredictions(aDataOwner, aLayer.getProject())
                .getPredictionsByTokenAndFeature(aSuggestion.getDocumentId(), aLayer,
                        aSuggestion.getBegin(), aSuggestion.getEnd(), aSuggestion.getFeature());
        applicationEventPublisher.publishEvent(new ActiveLearningRecommendationEvent(this, document,
                aSuggestion, aDataOwner.getUsername(), aLayer, aSuggestion.getFeature(), REJECTED,
                alternativeSuggestions));
    }

    @Override
    @Transactional
    public void skipSpanSuggestion(String aSessionOwner, User aDataOwner, AnnotationLayer aLayer,
            SpanSuggestion aSuggestion)
        throws AnnotationException
    {
        var document = documentService.getSourceDocument(aLayer.getProject().getId(),
                aSuggestion.getDocumentId());
        recommendationService.skipSuggestion(aSessionOwner, document, aDataOwner.getUsername(),
                aSuggestion, AL_SIDEBAR);

        // Send an application event indicating if the user has accepted/skipped/corrected/rejected
        // the suggestion
        var alternativeSuggestions = recommendationService
                .getPredictions(aDataOwner, aLayer.getProject())
                .getPredictionsByTokenAndFeature(aSuggestion.getDocumentId(), aLayer,
                        aSuggestion.getBegin(), aSuggestion.getEnd(), aSuggestion.getFeature());
        applicationEventPublisher.publishEvent(new ActiveLearningRecommendationEvent(this, document,
                aSuggestion, aDataOwner.getUsername(), aLayer, aSuggestion.getFeature(), SKIPPED,
                alternativeSuggestions));
    }

    private static SuggestionGroup<SpanSuggestion> removeDuplicatesAndHiddenSuggestions(
            SuggestionGroup<SpanSuggestion> aSuggestionGroup)
    {
        var cleanSuggestionGroup = new SuggestionGroup<SpanSuggestion>();

        aSuggestionGroup.forEach(suggestion -> {
            if (!suggestion.isVisible()) {
                return;
            }

            if (!isAlreadyInCleanList(cleanSuggestionGroup, suggestion)) {
                cleanSuggestionGroup.add(suggestion);
            }
        });

        return cleanSuggestionGroup;
    }

    private static boolean isAlreadyInCleanList(
            SuggestionGroup<SpanSuggestion> cleanRecommendationList,
            AnnotationSuggestion recommendationItem)
    {
        var source = recommendationItem.getRecommenderName();
        var annotation = recommendationItem.getLabel();
        var documentId = recommendationItem.getDocumentId();

        for (var existingRecommendation : cleanRecommendationList) {
            var areLabelsEqual = existingRecommendation.labelEquals(annotation);
            if (existingRecommendation.getRecommenderName().equals(source) && areLabelsEqual
                    && existingRecommendation.getDocumentId() == documentId) {
                return true;
            }
        }

        return false;
    }

    public static class ActiveLearningUserState
        implements Serializable
    {
        private static final long serialVersionUID = -167705997822964808L;

        private boolean sessionActive = false;
        private boolean doExistRecommenders = true;
        private AnnotationLayer layer;
        private ActiveLearningStrategy strategy;
        private List<SuggestionGroup<SpanSuggestion>> suggestions;

        private Delta<SpanSuggestion> currentDifference;
        private String leftContext;
        private String rightContext;

        public boolean isSessionActive()
        {
            return sessionActive;
        }

        public void setSessionActive(boolean sessionActive)
        {
            this.sessionActive = sessionActive;
        }

        public boolean isDoExistRecommenders()
        {
            return doExistRecommenders;
        }

        public void setDoExistRecommenders(boolean doExistRecommenders)
        {
            this.doExistRecommenders = doExistRecommenders;
        }

        public Optional<SpanSuggestion> getSuggestion()
        {
            return currentDifference != null ? Optional.of(currentDifference.getFirst())
                    : Optional.empty();
        }

        public Optional<Delta<SpanSuggestion>> getCurrentDifference()
        {
            return Optional.ofNullable(currentDifference);
        }

        public void setCurrentDifference(Optional<Delta<SpanSuggestion>> currentDifference)
        {
            this.currentDifference = currentDifference.orElse(null);
        }

        public AnnotationLayer getLayer()
        {
            return layer;
        }

        public void setLayer(AnnotationLayer selectedLayer)
        {
            this.layer = selectedLayer;
        }

        public ActiveLearningStrategy getStrategy()
        {
            return strategy;
        }

        public void setStrategy(ActiveLearningStrategy aStrategy)
        {
            strategy = aStrategy;
        }

        public void setSuggestions(List<SuggestionGroup<SpanSuggestion>> aSuggestions)
        {
            suggestions = aSuggestions;
        }

        public List<SuggestionGroup<SpanSuggestion>> getSuggestions()
        {
            return suggestions;
        }

        public String getLeftContext()
        {
            return leftContext;
        }

        public void setLeftContext(String aLeftContext)
        {
            leftContext = aLeftContext;
        }

        public String getRightContext()
        {
            return rightContext;
        }

        public void setRightContext(String aRightContext)
        {
            rightContext = aRightContext;
        }
    }

    public static class ActiveLearningUserStateKey
        implements Serializable
    {
        private static final long serialVersionUID = -2134294656221484540L;
        private String userName;
        private long projectId;

        public ActiveLearningUserStateKey(String aUserName, long aProjectId)
        {
            userName = aUserName;
            projectId = aProjectId;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ActiveLearningUserStateKey that = (ActiveLearningUserStateKey) o;

            if (projectId != that.projectId) {
                return false;
            }
            return userName.equals(that.userName);
        }

        @Override
        public int hashCode()
        {
            int result = userName.hashCode();
            result = 31 * result + (int) (projectId ^ (projectId >>> 32));
            return result;
        }
    }
}

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

import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_REJECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_SKIPPED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_TRANSIENT_ACCEPTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion.FLAG_TRANSIENT_CORRECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordChangeLocation.AL_SIDEBAR;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType.ACCEPTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType.CORRECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType.REJECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType.SKIPPED;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.active.learning.config.ActiveLearningAutoConfiguration;
import de.tudarmstadt.ukp.inception.active.learning.event.ActiveLearningRecommendationEvent;
import de.tudarmstadt.ukp.inception.active.learning.strategy.ActiveLearningStrategy;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Preferences;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SpanSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionDocumentGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup.Delta;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureSupportRegistry;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ActiveLearningAutoConfiguration#activeLearningService}.
 * </p>
 */
public class ActiveLearningServiceImpl
    implements ActiveLearningService
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ApplicationEventPublisher applicationEventPublisher;
    private final DocumentService documentService;
    private final RecommendationService recommendationService;
    private final UserDao userService;
    private final LearningRecordService learningHistoryService;
    private final AnnotationSchemaService schemaService;
    private final FeatureSupportRegistry featureSupportRegistry;

    @Autowired
    public ActiveLearningServiceImpl(DocumentService aDocumentService,
            RecommendationService aRecommendationService, UserDao aUserDao,
            LearningRecordService aLearningHistoryService, AnnotationSchemaService aSchemaService,
            ApplicationEventPublisher aApplicationEventPublisher,
            FeatureSupportRegistry aFeatureSupportRegistry)
    {
        documentService = aDocumentService;
        recommendationService = aRecommendationService;
        userService = aUserDao;
        learningHistoryService = aLearningHistoryService;
        applicationEventPublisher = aApplicationEventPublisher;
        schemaService = aSchemaService;
        featureSupportRegistry = aFeatureSupportRegistry;
    }

    @Override
    public List<SuggestionGroup<SpanSuggestion>> getSuggestions(User aUser, AnnotationLayer aLayer)
    {
        Predictions predictions = recommendationService.getPredictions(aUser, aLayer.getProject());

        if (predictions == null) {
            return emptyList();
        }

        Map<String, SuggestionDocumentGroup<SpanSuggestion>> recommendationsMap = predictions
                .getPredictionsForWholeProject(SpanSuggestion.class, aLayer, documentService);

        return recommendationsMap.values().stream() //
                .flatMap(docMap -> docMap.stream()) //
                .collect(toList());
    }

    @Override
    public boolean isSuggestionVisible(LearningRecord aRecord)
    {
        User user = userService.get(aRecord.getUser());
        List<SuggestionGroup<SpanSuggestion>> suggestions = getSuggestions(user,
                aRecord.getLayer());
        for (SuggestionGroup<SpanSuggestion> listOfAO : suggestions) {
            if (listOfAO.stream().anyMatch(suggestion -> suggestion.getDocumentName()
                    .equals(aRecord.getSourceDocument().getName())
                    && suggestion.getFeature().equals(aRecord.getAnnotationFeature().getName())
                    && suggestion.labelEquals(aRecord.getAnnotation())
                    && suggestion.getBegin() == aRecord.getOffsetBegin()
                    && suggestion.getEnd() == aRecord.getOffsetEnd() //
                    && suggestion.isVisible())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasSkippedSuggestions(User aUser, AnnotationLayer aLayer)
    {
        return learningHistoryService.hasSkippedSuggestions(aUser, aLayer);
    }

    @Override
    public void hideRejectedOrSkippedAnnotations(User aUser, AnnotationLayer aLayer,
            boolean filterSkippedRecommendation,
            List<SuggestionGroup<SpanSuggestion>> aSuggestionGroups)
    {
        List<LearningRecord> records = learningHistoryService.listRecords(aUser.getUsername(),
                aLayer);

        for (SuggestionGroup<SpanSuggestion> group : aSuggestionGroups) {
            for (SpanSuggestion s : group) {
                // If a suggestion is already invisible, we don't need to check if it needs hiding.
                // Mind that this code does not unhide the suggestion immediately if a user
                // deletes a skip learning record - it will only get unhidden after the next
                // prediction run (unless the learning-record-deletion code does an explicit
                // unhiding).
                if (!s.isVisible()) {
                    continue;
                }

                records.stream()
                        .filter(r -> r.getSourceDocument().getName().equals(s.getDocumentName())
                                && r.getOffsetBegin() == s.getBegin()
                                && r.getOffsetEnd() == s.getEnd()
                                && s.labelEquals(r.getAnnotation()))
                        .forEach(record -> {
                            if (REJECTED.equals(record.getUserAction())) {
                                s.hide(FLAG_REJECTED);
                            }
                            else if (filterSkippedRecommendation
                                    && SKIPPED.equals(record.getUserAction())) {
                                s.hide(FLAG_SKIPPED);
                            }
                        });

            }
        }
    }

    @Override
    public Optional<Delta<SpanSuggestion>> generateNextSuggestion(User aUser,
            ActiveLearningUserState alState)
    {
        // Fetch the next suggestion to present to the user (if there is any)
        long startTimer = System.currentTimeMillis();
        List<SuggestionGroup<SpanSuggestion>> suggestions = alState.getSuggestions();
        long getRecommendationsFromRecommendationService = System.currentTimeMillis();
        log.trace("Getting recommendations from recommender system costs {} ms.",
                (getRecommendationsFromRecommendationService - startTimer));

        // remove duplicate recommendations
        suggestions = suggestions.stream() //
                .map(it -> removeDuplicateRecommendations(it)) //
                .collect(Collectors.toList());
        long removeDuplicateRecommendation = System.currentTimeMillis();
        log.trace("Removing duplicate recommendations costs {} ms.",
                (removeDuplicateRecommendation - getRecommendationsFromRecommendationService));

        // hide rejected recommendations
        hideRejectedOrSkippedAnnotations(aUser, alState.getLayer(), true, suggestions);
        long removeRejectedSkippedRecommendation = System.currentTimeMillis();
        log.trace("Removing rejected or skipped ones costs {} ms.",
                (removeRejectedSkippedRecommendation - removeDuplicateRecommendation));

        Preferences pref = recommendationService.getPreferences(aUser,
                alState.getLayer().getProject());
        return alState.getStrategy().generateNextSuggestion(pref, suggestions);
    }

    @Override
    @Transactional
    public void writeLearningRecordInDatabaseAndEventLog(User aUser, AnnotationLayer aLayer,
            SpanSuggestion aSuggestion, LearningRecordType aUserAction, String aAnnotationValue)
    {

        AnnotationFeature feat = schemaService.getFeature(aSuggestion.getFeature(), aLayer);
        SourceDocument sourceDoc = documentService.getSourceDocument(aLayer.getProject(),
                aSuggestion.getDocumentName());

        List<SpanSuggestion> alternativeSuggestions = recommendationService
                .getPredictions(aUser, aLayer.getProject())
                .getPredictionsByTokenAndFeature(aSuggestion.getDocumentName(), aLayer,
                        aSuggestion.getBegin(), aSuggestion.getEnd(), aSuggestion.getFeature());

        // Log the action to the learning record
        learningHistoryService.logSpanRecord(sourceDoc, aUser.getUsername(), aSuggestion,
                aAnnotationValue, aLayer, feat, aUserAction, AL_SIDEBAR);

        // If the action was a correction (i.e. suggestion label != annotation value) then generate
        // a rejection for the original value - we do not want the original value to re-appear
        if (aUserAction == CORRECTED) {
            learningHistoryService.logSpanRecord(sourceDoc, aUser.getUsername(), aSuggestion,
                    aSuggestion.getLabel(), aLayer, feat, REJECTED, AL_SIDEBAR);
        }

        // Send an application event indicating if the user has accepted/skipped/corrected/rejected
        // the suggestion
        applicationEventPublisher.publishEvent(new ActiveLearningRecommendationEvent(this,
                sourceDoc, aSuggestion, aUser.getUsername(), aLayer, aSuggestion.getFeature(),
                aUserAction, alternativeSuggestions));
    }

    @Override
    @Transactional
    public void acceptSpanSuggestion(User aUser, AnnotationLayer aLayer, SpanSuggestion aSuggestion,
            Object aValue)
        throws IOException, AnnotationException
    {
        // There is always a current recommendation when we get here because if there is none, the
        // button to accept the recommendation is not visible.
        SpanSuggestion suggestion = aSuggestion;

        // Create AnnotationFeature and FeatureSupport
        AnnotationFeature feat = schemaService.getFeature(suggestion.getFeature(), aLayer);
        FeatureSupport<?> featureSupport = featureSupportRegistry.findExtension(feat).orElseThrow();

        // Load CAS in which to create the annotation. This might be different from the one that
        // is currently viewed by the user, e.g. if the user switched to another document after
        // the suggestion has been loaded into the sidebar.
        SourceDocument sourceDoc = documentService.getSourceDocument(aLayer.getProject(),
                suggestion.getDocumentName());
        String username = aUser.getUsername();
        CAS cas = documentService.readAnnotationCas(sourceDoc, username);

        // Upsert an annotation based on the suggestion
        String value = (String) featureSupport.unwrapFeatureValue(feat, cas, aValue);
        AnnotationLayer layer = schemaService.getLayer(aLayer.getProject(), suggestion.getLayerId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No such layer: [" + suggestion.getLayerId() + "]"));

        // Log the action to the learning record and immediately hide the suggestion
        boolean areLabelsEqual = suggestion.labelEquals(value);
        writeLearningRecordInDatabaseAndEventLog(aUser, aLayer, suggestion,
                (areLabelsEqual) ? ACCEPTED : CORRECTED, value);
        suggestion.hide((areLabelsEqual) ? FLAG_TRANSIENT_ACCEPTED : FLAG_TRANSIENT_CORRECTED);

        // Request clearing selection and when onFeatureValueUpdated is triggered as a callback
        // from the update event created by upsertSpanFeature.
        AnnotationFeature feature = schemaService.getFeature(suggestion.getFeature(), layer);
        recommendationService.upsertSpanFeature(schemaService, sourceDoc, username, cas, layer,
                feature, value, suggestion.getBegin(), suggestion.getEnd());

        // Save CAS after annotation has been created
        documentService.writeAnnotationCas(cas, sourceDoc, aUser, true);
    }

    @Override
    @Transactional
    public void rejectSpanSuggestion(User aUser, AnnotationLayer aLayer, SpanSuggestion aSuggestion)
    {
        writeLearningRecordInDatabaseAndEventLog(aUser, aLayer, aSuggestion, REJECTED,
                aSuggestion.getLabel());
    }

    @Override
    @Transactional
    public void skipSpanSuggestion(User aUser, AnnotationLayer aLayer, SpanSuggestion aSuggestion)
    {
        writeLearningRecordInDatabaseAndEventLog(aUser, aLayer, aSuggestion, SKIPPED,
                aSuggestion.getLabel());
    }

    private static SuggestionGroup<SpanSuggestion> removeDuplicateRecommendations(
            SuggestionGroup<SpanSuggestion> unmodifiedRecommendationList)
    {
        SuggestionGroup<SpanSuggestion> cleanRecommendationList = new SuggestionGroup<>();

        unmodifiedRecommendationList.forEach(recommendationItem -> {
            if (!isAlreadyInCleanList(cleanRecommendationList, recommendationItem)) {
                cleanRecommendationList.add(recommendationItem);
            }
        });

        return cleanRecommendationList;
    }

    private static boolean isAlreadyInCleanList(
            SuggestionGroup<SpanSuggestion> cleanRecommendationList,
            AnnotationSuggestion recommendationItem)
    {
        String source = recommendationItem.getRecommenderName();
        String annotation = recommendationItem.getLabel();
        String documentName = recommendationItem.getDocumentName();

        for (AnnotationSuggestion existingRecommendation : cleanRecommendationList) {
            boolean areLabelsEqual = existingRecommendation.labelEquals(annotation);
            if (existingRecommendation.getRecommenderName().equals(source) && areLabelsEqual
                    && existingRecommendation.getDocumentName().equals(documentName)) {
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

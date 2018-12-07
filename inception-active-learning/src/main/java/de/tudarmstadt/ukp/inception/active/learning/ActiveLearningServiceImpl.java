/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.active.learning;

import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType.REJECTED;
import static java.util.stream.Collectors.toList;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.active.learning.strategy.ActiveLearningStrategy;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionDocumentGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup.Delta;

@Component
public class ActiveLearningServiceImpl
    implements ActiveLearningService
{
    private final DocumentService documentService;
    private final RecommendationService recommendationService;
    private final UserDao userService;
    private final LearningRecordService learningHistoryService;

    @Autowired
    public ActiveLearningServiceImpl(DocumentService aDocumentService,
            RecommendationService aRecommendationService, UserDao aUserDao,
            LearningRecordService aLearningHistoryService)
    {
        documentService = aDocumentService;
        recommendationService = aRecommendationService;
        userService = aUserDao;
        learningHistoryService = aLearningHistoryService;
    }

    @Override
    public List<SuggestionGroup> getSuggestions(User aUser,
            AnnotationLayer aLayer)
    {
        Predictions model = recommendationService.getPredictions(aUser, aLayer.getProject());

        if (model == null) {
            return Collections.emptyList();
        }

        Map<String, SuggestionDocumentGroup> recommendationsMap = model
                .getPredictionsForWholeProject(aLayer, documentService, true);

        return recommendationsMap.values().stream()
                .flatMap(docMap -> docMap.stream())
                .collect(toList());
    }
    
    @Override
    public boolean isSuggestionVisible(LearningRecord aRecord)
    {
        User user = userService.get(aRecord.getUser());
        List<SuggestionGroup> suggestions = getSuggestions(user,
                aRecord.getLayer());
        for (SuggestionGroup listOfAO : suggestions) {
            if (listOfAO.stream().anyMatch(suggestion -> 
                    suggestion.getDocumentName().equals(aRecord.getSourceDocument().getName()) && 
                    suggestion.getFeature().equals(aRecord.getAnnotationFeature().getName()) && 
                    suggestion.getLabel().equals(aRecord.getAnnotation()) && 
                    suggestion.getBegin() == aRecord.getOffsetCharacterBegin() && 
                    suggestion.getEnd() == aRecord.getOffsetCharacterEnd() &&
                    suggestion.isVisible())
            ) {
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
    public void hideRejectedOrSkippedAnnotations(SourceDocument aDocument, User aUser,
            AnnotationLayer aLayer, boolean filterSkippedRecommendation,
            List<SuggestionGroup> aSuggestionGroups)
    {
        List<LearningRecord> records = learningHistoryService
                .getAllRecordsByDocumentAndUserAndLayer(aDocument, aUser.getUsername(),
                        aLayer);

        for (SuggestionGroup group : aSuggestionGroups) {
            for (AnnotationSuggestion suggestion : group) {
                // If a suggestion is already invisible, we don't need to check if it needs hiding
                if (suggestion.isVisible() && doesContainRejectedOrSkippedRecord(records,
                        suggestion, filterSkippedRecommendation)) {
                    suggestion.hide("doesContainRejectedOrSkippedRecord");
                }
            }
        }
    }
    
    private static boolean doesContainRejectedOrSkippedRecord(List<LearningRecord> records,
            AnnotationSuggestion aRecommendation, boolean filterSkippedRecommendation)
    {
        for (LearningRecord record : records) {
            if ((record.getUserAction().equals(REJECTED)
                    || filterSkippedRecord(record, filterSkippedRecommendation))
                    && hasSameTokenAndSuggestion(aRecommendation, record)) {
                return true;
            }
        }
        return false;
    }

    private static boolean filterSkippedRecord(LearningRecord record,
            boolean filterSkippedRecommendation)
    {
        return record.getUserAction().equals(LearningRecordType.SKIPPED)
                && filterSkippedRecommendation;
    }

    private static boolean hasSameTokenAndSuggestion(AnnotationSuggestion aRecommendation,
            LearningRecord aRecord)
    {
        return aRecord.getSourceDocument().getName().equals(aRecommendation.getDocumentName())
                && aRecord.getOffsetCharacterBegin() == aRecommendation.getBegin()
                && aRecord.getOffsetCharacterEnd() == aRecommendation.getEnd()
                && aRecord.getAnnotation().equals(aRecommendation.getLabel());
    }

    public static class ActiveLearningUserState implements Serializable
    {
        private static final long serialVersionUID = -167705997822964808L;
        
        private boolean sessionActive = false;
        private boolean doExistRecommenders = true;
        private AnnotationLayer layer;
        private ActiveLearningStrategy strategy;
        private List<SuggestionGroup> suggestions;

        private Delta currentDifference;
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

        public Optional<AnnotationSuggestion> getSuggestion()
        {
            return currentDifference != null ? Optional.of(currentDifference.getFirst())
                    : Optional.empty();
        }

        public Optional<Delta> getCurrentDifference()
        {
            return Optional.ofNullable(currentDifference);
        }

        public void setCurrentDifference(Optional<Delta> currentDifference)
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

        public void setSuggestions(List<SuggestionGroup> aSuggestions)
        {
            suggestions = aSuggestions;
        }

        public List<SuggestionGroup> getSuggestions()
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

    public static class ActiveLearningUserStateKey implements Serializable
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

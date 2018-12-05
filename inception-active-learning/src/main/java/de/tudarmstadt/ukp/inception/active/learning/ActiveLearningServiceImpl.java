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

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.active.learning.strategy.ActiveLearningStrategy;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Offset;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup.Delta;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;

@Component
public class ActiveLearningServiceImpl
    implements ActiveLearningService
{
    private final DocumentService documentService;
    private final RecommendationService recommendationService;

    @Autowired
    public ActiveLearningServiceImpl(DocumentService aDocumentService,
            RecommendationService aRecommendationService)
    {
        documentService = aDocumentService;
        recommendationService = aRecommendationService;
    }

    @Override
    public List<SuggestionGroup> getRecommendationFromRecommendationModel(
            AnnotatorState aState, AnnotationLayer aLayer)
    {
        Predictions model = recommendationService.getPredictions(aState.getUser(),
                aState.getProject());

        if (model == null) {
            return Collections.emptyList();
        }

        Map<String, SortedMap<Offset, SuggestionGroup>> recommendationsMap = model
                .getPredictionsForWholeProject(aLayer, documentService, true);
        
        return recommendationsMap.values().stream()
            .flatMap(docMap -> docMap.values().stream())
            .collect(Collectors.toList());
    }

    public static class ActiveLearningUserState implements Serializable
    {
        private static final long serialVersionUID = -167705997822964808L;
        
        private boolean sessionActive = false;
        private boolean hasUnseenRecommendation = false;
        private boolean hasSkippedRecommendation = false;
        private boolean doExistRecommenders = true;
        private Delta currentDifference;
        private AnnotationLayer layer;
        private ActiveLearningStrategy strategy;
        private Date learnSkippedRecommendationTime;
        private List<SuggestionGroup> listOfRecommendationsForEachToken;

        public boolean isSessionActive()
        {
            return sessionActive;
        }

        public void setSessionActive(boolean sessionActive)
        {
            this.sessionActive = sessionActive;
        }

        public boolean isHasUnseenRecommendation()
        {
            return hasUnseenRecommendation;
        }

        public void setHasUnseenRecommendation(boolean hasUnseenRecommendation)
        {
            this.hasUnseenRecommendation = hasUnseenRecommendation;
        }

        public boolean isHasSkippedRecommendation()
        {
            return hasSkippedRecommendation;
        }

        public void setHasSkippedRecommendation(boolean hasSkippedRecommendation)
        {
            this.hasSkippedRecommendation = hasSkippedRecommendation;
        }

        public boolean isDoExistRecommenders()
        {
            return doExistRecommenders;
        }

        public void setDoExistRecommenders(boolean doExistRecommenders)
        {
            this.doExistRecommenders = doExistRecommenders;
        }

        public Optional<AnnotationSuggestion> getCurrentRecommendation()
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

        public Date getLearnSkippedRecommendationTime()
        {
            return learnSkippedRecommendationTime;
        }

        public void setLearnSkippedRecommendationTime(Date learnSkippedRecommendationTime)
        {
            this.learnSkippedRecommendationTime = learnSkippedRecommendationTime;
        }

        public void setListOfRecommendationsForEachToken(List<SuggestionGroup>
            aListOfRecommendationsForEachToken)
        {
            this.listOfRecommendationsForEachToken = aListOfRecommendationsForEachToken;
        }

        public List<SuggestionGroup> getListOfRecommendationsForEachToken()
        {
            return listOfRecommendationsForEachToken;
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

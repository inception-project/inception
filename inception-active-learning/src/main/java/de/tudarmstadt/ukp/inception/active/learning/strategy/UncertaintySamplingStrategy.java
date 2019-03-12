/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.inception.active.learning.strategy;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.active.learning.ActiveLearningService;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup.Delta;

public class UncertaintySamplingStrategy
    implements Serializable, ActiveLearningStrategy
{
    private static final Logger LOG = LoggerFactory.getLogger(UncertaintySamplingStrategy.class);

    @Override
    public Optional<Delta> generateNextSuggestion(ActiveLearningService aALService,
            LearningRecordService aRecordService, User aUser, AnnotationLayer aLayer,
            List<SuggestionGroup> aListOfRecommendationsForEachToken)
    {
        long startTimer = System.currentTimeMillis();
        List<SuggestionGroup> suggestions = aListOfRecommendationsForEachToken;
        long getRecommendationsFromRecommendationService = System.currentTimeMillis();
        LOG.debug("Getting recommendations from recommender system costs {} ms.",
                (getRecommendationsFromRecommendationService - startTimer));

        // remove duplicate recommendations
        suggestions = suggestions.stream()
                .map(it -> removeDuplicateRecommendations(it)).collect(Collectors.toList());
        long removeDuplicateRecommendation = System.currentTimeMillis();
        LOG.debug("Removing duplicate recommendations costs {} ms.",
                (removeDuplicateRecommendation - getRecommendationsFromRecommendationService));

        // hide rejected recommendations
        hideRejectedOrSkippedAnnotations(aRecordService, aUser, aLayer, true, suggestions,
                aALService);
        long removeRejectedSkippedRecommendation = System.currentTimeMillis();
        LOG.debug("Removing rejected or skipped ones costs {} ms.",
                (removeRejectedSkippedRecommendation - removeDuplicateRecommendation));

        return calculateDifferencesAndReturnLowestVisible(suggestions);
    }

    private void hideRejectedOrSkippedAnnotations(LearningRecordService aRecordService, User aUser,
            AnnotationLayer aLayer, boolean aFilterSkippedRecommendation,
            List<SuggestionGroup> aSuggestionGroups, ActiveLearningService aActiveLearningService)
    {
        aActiveLearningService.hideRejectedOrSkippedAnnotations(aUser, aLayer,
                aFilterSkippedRecommendation, aSuggestionGroups);
    }

    private static SuggestionGroup removeDuplicateRecommendations(
            SuggestionGroup unmodifiedRecommendationList)
    {
        SuggestionGroup cleanRecommendationList = new SuggestionGroup();

        unmodifiedRecommendationList.forEach(recommendationItem -> {
            if (!isAlreadyInCleanList(cleanRecommendationList, recommendationItem)) {
                cleanRecommendationList.add(recommendationItem);
            }
        });

        return cleanRecommendationList;
    }

    private static boolean isAlreadyInCleanList(SuggestionGroup cleanRecommendationList,
        AnnotationSuggestion recommendationItem)
    {
        String source = recommendationItem.getRecommenderName();
        String annotation = recommendationItem.getLabel();
        String documentName = recommendationItem.getDocumentName();
        
        for (AnnotationSuggestion existingRecommendation : cleanRecommendationList) {
            String existingLabel = existingRecommendation.getLabel();
            boolean isLabelEqual = existingLabel != null && existingLabel.equals(annotation);
            boolean isNullLabel = existingLabel == null && annotation == null;
            if (existingRecommendation.getRecommenderName().equals(source)
                    && (isLabelEqual || isNullLabel)
                    && existingRecommendation.getDocumentName().equals(documentName)
            ) {
                return true;
            }
        }
        return false;
    }

    private static Optional<Delta> calculateDifferencesAndReturnLowestVisible(
            List<SuggestionGroup> aGroups)
    {
        return aGroups.stream()
            // Fetch the top deltas per recommender
            .flatMap(group -> group.getTopDeltas().values().stream())
            // ... sort them in ascending order (smallest delta first)
            .sorted(Comparator.comparingDouble(Delta::getDelta))
            // ... and return the smallest delta (if there is one)
            .findFirst();
    }
}

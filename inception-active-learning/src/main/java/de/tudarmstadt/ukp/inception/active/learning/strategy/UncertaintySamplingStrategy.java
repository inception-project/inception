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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.active.learning.ActiveLearningService;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup.Delta;

public class UncertaintySamplingStrategy
    implements Serializable, ActiveLearningStrategy
{
    private static final long serialVersionUID = -2308436775710912029L;

    private List<SuggestionGroup> listOfRecommendationsForEachToken;
    private AnnotatorState annotatorState;
    private AnnotationLayer selectedLayer;
    private static final Logger LOG = LoggerFactory.getLogger(UncertaintySamplingStrategy.class);

    public UncertaintySamplingStrategy(AnnotatorState aState, AnnotationLayer aLayer)
    {
        annotatorState = aState;
        selectedLayer = aLayer;
    }

    @Override
    public Optional<Delta> updateRecommendations(ActiveLearningService aALService,
            LearningRecordService aRecordService)
    {
        // remove invisible recommendations
        List<SuggestionGroup> filteredRecommendations = new ArrayList<>(
                listOfRecommendationsForEachToken);

        // remove rejected recommendations
        hideRejectedOrSkippedAnnotations(aRecordService, true,
                filteredRecommendations, aALService);

        return calculateDifferencesAndReturnLowestVisible(filteredRecommendations);
    }

    @Override
    public Optional<Delta> generateRecommendationWithLowestDifference(
            ActiveLearningService aALService, LearningRecordService aRecordService,
            List<SuggestionGroup> aListOfRecommendationsForEachToken)
    {
        long startTimer = System.currentTimeMillis();
        listOfRecommendationsForEachToken = aListOfRecommendationsForEachToken;
        long getRecommendationsFromRecommendationService = System.currentTimeMillis();
        LOG.debug("Getting recommendations from recommender system costs {} ms.",
                (getRecommendationsFromRecommendationService - startTimer));

        // remove duplicate recommendations
        listOfRecommendationsForEachToken = listOfRecommendationsForEachToken.stream()
                .map(it -> removeDuplicateRecommendations(it)).collect(Collectors.toList());
        long removeDuplicateRecommendation = System.currentTimeMillis();
        LOG.debug("Removing duplicate recommendations costs {} ms.",
                (removeDuplicateRecommendation - getRecommendationsFromRecommendationService));

        // remove invisible recommendations
        List<SuggestionGroup> filteredRecommendations = new ArrayList<>(
                listOfRecommendationsForEachToken);

        // remove rejected recommendations
        hideRejectedOrSkippedAnnotations(aRecordService, true, filteredRecommendations, aALService);
        long removeRejectedSkippedRecommendation = System.currentTimeMillis();
        LOG.debug("Removing rejected or skipped ones costs {} ms.",
                (removeRejectedSkippedRecommendation - removeDuplicateRecommendation));

        return calculateDifferencesAndReturnLowestVisible(filteredRecommendations);
    }

    private void hideRejectedOrSkippedAnnotations(LearningRecordService aRecordService,
            boolean aFilterSkippedRecommendation, List<SuggestionGroup> aSuggestionGroups,
            ActiveLearningService aActiveLearningService)
    {
        aActiveLearningService.hideRejectedOrSkippedAnnotations(annotatorState.getDocument(),
                annotatorState.getUser(), selectedLayer, aFilterSkippedRecommendation,
                aSuggestionGroups);
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
            if (
                    existingRecommendation.getRecommenderName().equals(source) &&
                    existingRecommendation.getLabel().equals(annotation) &&
                    existingRecommendation.getDocumentName().equals(documentName)
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

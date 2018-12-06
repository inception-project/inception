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

import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType.REJECTED;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
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
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType;
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
    public Optional<Delta> updateRecommendations(
            LearningRecordService aRecordService, Date learnSkippedRecommendationTime)
    {
        // remove invisible recommendations
        List<SuggestionGroup> filteredRecommendations = new ArrayList<>(
                listOfRecommendationsForEachToken);

        // remove rejected recommendations
        hideRejectedOrSkippedAnnotations(aRecordService, true, learnSkippedRecommendationTime,
                filteredRecommendations);

        return calculateDifferencesAndReturnLowestVisible(filteredRecommendations);
    }

    @Override
    public Optional<Delta> generateRecommendationWithLowestDifference(
            LearningRecordService aRecordService, Date learnSkippedRecommendationTime,
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
        hideRejectedOrSkippedAnnotations(aRecordService, true, learnSkippedRecommendationTime,
                filteredRecommendations);
        long removeRejectedSkippedRecommendation = System.currentTimeMillis();
        LOG.debug("Removing rejected or skipped ones costs {} ms.",
                (removeRejectedSkippedRecommendation - removeDuplicateRecommendation));

        return calculateDifferencesAndReturnLowestVisible(filteredRecommendations);
    }

    @Override
    public boolean hasRecommendationWhichIsSkipped(LearningRecordService aRecordService,
            ActiveLearningService aActiveLearningService)
    {
        listOfRecommendationsForEachToken = aActiveLearningService
                .getRecommendationFromRecommendationModel(annotatorState, selectedLayer);
        hideRejectedOrSkippedAnnotations(aRecordService, false, null,
                listOfRecommendationsForEachToken);
        return !listOfRecommendationsForEachToken.isEmpty();
    }

    @Override
    public boolean checkRecommendationExist(ActiveLearningService aActiveLearningService,
        LearningRecord aRecord)
    {
        listOfRecommendationsForEachToken = aActiveLearningService
            .getRecommendationFromRecommendationModel(annotatorState, selectedLayer);
        return containsRecommendation(listOfRecommendationsForEachToken, aRecord);
    }

    private void hideRejectedOrSkippedAnnotations(LearningRecordService aRecordService,
            boolean filterSkippedRecommendation, Date learnSkippedRecommendationTime,
            List<SuggestionGroup> aSuggestionGroups)
    {
        List<LearningRecord> records = aRecordService.getAllRecordsByDocumentAndUserAndLayer(
                annotatorState.getDocument(), annotatorState.getUser().getUsername(),
                selectedLayer);
        
        for (SuggestionGroup group : aSuggestionGroups) {
            for (AnnotationSuggestion suggestion : group) {
                // If a suggestion is already invisible, we don't need to check if it needs hiding
                if (suggestion.isVisible() && doesContainRejectedOrSkippedRecord(records,
                        suggestion, filterSkippedRecommendation, learnSkippedRecommendationTime)) {
                    suggestion.setVisible(false);
                }
            }
        }
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

    private static boolean doesContainRejectedOrSkippedRecord(List<LearningRecord> records,
        AnnotationSuggestion aRecommendation, boolean filterSkippedRecommendation,
        Date learnSkippedRecommendationTime)
    {
        for (LearningRecord record : records) {
            if ((record.getUserAction().equals(REJECTED)
                    || filterSkippedRecord(record, filterSkippedRecommendation) && 
                needFilterByTime(learnSkippedRecommendationTime, record)) && 
                hasSameTokenAndSuggestion(aRecommendation, record)
            ) {
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
        return aRecord.getSourceDocument().getName().equals(aRecommendation.getDocumentName()) && 
                aRecord.getOffsetCharacterBegin() == aRecommendation.getBegin() && 
                aRecord.getOffsetCharacterEnd() == aRecommendation.getEnd() && 
                aRecord.getAnnotation().equals(aRecommendation.getLabel());
    }

    /**
     * If learnSkippedTime is null, this record needs to be filtered.
     * If the record written time is after the learnSkippedTime, this record needs to be filtered.
     */
    private static boolean needFilterByTime(Date learnSkippedTime, LearningRecord record)
    {
        return learnSkippedTime == null || learnSkippedTime.compareTo(record.getActionDate()) <= 0;
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

    private static boolean containsRecommendation(
        List<SuggestionGroup> aListOfRecommendationsForEachToken, LearningRecord record)
    {
        for (SuggestionGroup listOfAO : aListOfRecommendationsForEachToken) {
            if (listOfAO.stream().anyMatch(ao -> compareRecordToRecommendation(ao, record))) {
                return true;
            }
        }
        return false;
    }
    
    private static boolean compareRecordToRecommendation(AnnotationSuggestion aRecommendation,
        LearningRecord aRecord)
    {
        return aRecommendation.getLabel().equals(aRecord.getAnnotation()) && 
                aRecommendation.getDocumentName().equals(aRecord.getSourceDocument().getName()) && 
                aRecommendation.getBegin() == aRecord.getOffsetCharacterBegin() && 
                aRecommendation.getEnd() == aRecord.getOffsetCharacterEnd();
    }
}

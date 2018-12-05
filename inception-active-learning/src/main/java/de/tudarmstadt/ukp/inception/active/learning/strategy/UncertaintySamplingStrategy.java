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

import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction.REJECTED;

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
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction;
import de.tudarmstadt.ukp.inception.recommendation.api.model.PredictionGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.model.PredictionGroup.Delta;

public class UncertaintySamplingStrategy
    implements Serializable, ActiveLearningStrategy
{
    private static final long serialVersionUID = -2308436775710912029L;

    private List<PredictionGroup> listOfRecommendationsForEachToken;
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
        List<PredictionGroup> filteredRecommendations = new ArrayList<>(
                listOfRecommendationsForEachToken);

        // remove rejected recommendations
        removeRejectedOrSkippedAnnotations(aRecordService, true, learnSkippedRecommendationTime,
                filteredRecommendations);

        return calculateDifferencesAndReturnLowestVisible(filteredRecommendations);
    }

    @Override
    public Optional<Delta> generateRecommendationWithLowestDifference(
            LearningRecordService aRecordService, Date learnSkippedRecommendationTime,
            List<PredictionGroup> aListOfRecommendationsForEachToken)
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
        List<PredictionGroup> filteredRecommendations = new ArrayList<>(
                listOfRecommendationsForEachToken);

        // remove rejected recommendations
        removeRejectedOrSkippedAnnotations(aRecordService, true, learnSkippedRecommendationTime,
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
        removeRejectedOrSkippedAnnotations(aRecordService, false, null,
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

    private void removeRejectedOrSkippedAnnotations(LearningRecordService aRecordService,
            boolean filterSkippedRecommendation, Date learnSkippedRecommendationTime,
            List<PredictionGroup> aSuggestionGroups)
    {
        List<LearningRecord> records = aRecordService.getAllRecordsByDocumentAndUserAndLayer(
                annotatorState.getDocument(), annotatorState.getUser().getUsername(),
                selectedLayer);
        
        for (PredictionGroup group : aSuggestionGroups) {
            for (AnnotationObject suggestion : group) {
                // If a suggestion is already invisible, we don't need to check if it needs hiding
                if (suggestion.isVisible() && doesContainRejectedOrSkippedRecord(records,
                        suggestion, filterSkippedRecommendation, learnSkippedRecommendationTime)) {
                    suggestion.setVisible(false);
                }
            }
        }
    }

    private static PredictionGroup removeDuplicateRecommendations(
            PredictionGroup unmodifiedRecommendationList)
    {
        PredictionGroup cleanRecommendationList = new PredictionGroup();

        unmodifiedRecommendationList.forEach(recommendationItem -> {
            if (!isAlreadyInCleanList(cleanRecommendationList, recommendationItem)) {
                cleanRecommendationList.add(recommendationItem);
            }
        });

        return cleanRecommendationList;
    }

    private static boolean isAlreadyInCleanList(PredictionGroup cleanRecommendationList,
        AnnotationObject recommendationItem)
    {
        String source = recommendationItem.getRecommenderName();
        String annotation = recommendationItem.getLabel();
        String documentName = recommendationItem.getDocumentName();
        
        for (AnnotationObject existingRecommendation : cleanRecommendationList) {
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
        AnnotationObject aRecommendation, boolean filterSkippedRecommendation,
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
        return record.getUserAction().equals(LearningRecordUserAction.SKIPPED)
            && filterSkippedRecommendation;
    }

    private static boolean hasSameTokenAndSuggestion(AnnotationObject aRecommendation,
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
            List<PredictionGroup> aGroups)
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
        List<PredictionGroup> aListOfRecommendationsForEachToken, LearningRecord record)
    {
        for (PredictionGroup listOfAO : aListOfRecommendationsForEachToken) {
            if (listOfAO.stream().anyMatch(ao -> compareRecordToRecommendation(ao, record))) {
                return true;
            }
        }
        return false;
    }
    
    private static boolean compareRecordToRecommendation(AnnotationObject aRecommendation,
        LearningRecord aRecord)
    {
        return aRecommendation.getLabel().equals(aRecord.getAnnotation()) && 
                aRecommendation.getDocumentName().equals(aRecord.getSourceDocument().getName()) && 
                aRecommendation.getBegin() == aRecord.getOffsetCharacterBegin() && 
                aRecommendation.getEnd() == aRecord.getOffsetCharacterEnd();
    }
}

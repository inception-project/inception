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
package de.tudarmstadt.ukp.inception.active.learning.sidebar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.active.learning.ActiveLearningService;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction;

public class ActiveLearningRecommender
    implements Serializable
{
    private static final long serialVersionUID = -2308436775710912029L;

    private List<AnnotationObject> recommendations;
    private List<List<AnnotationObject>> listOfRecommendationsForEachToken;
    private AnnotatorState annotatorState;
    private AnnotationLayer selectedLayer;
    
    public ActiveLearningRecommender(AnnotatorState aState, AnnotationLayer aLayer)
    {
        annotatorState = aState;
        selectedLayer = aLayer;
    }

    public RecommendationDifference generateRecommendationWithLowestDifference(
            LearningRecordService aRecordService, ActiveLearningService aActiveLearningService,
            Date learnSkippedRecommendationTime)
    {
        listOfRecommendationsForEachToken = aActiveLearningService
                .getRecommendationFromRecommendationModel(annotatorState, selectedLayer);

        // remove recommendations with Null Annotation
        listOfRecommendationsForEachToken.forEach(recommendationList -> 
                removeRecommendationsWithNullAnnotation(recommendationList));
        listOfRecommendationsForEachToken.removeIf(recommendationList -> 
                recommendationList.isEmpty());

        // remove duplicate recommendations
        listOfRecommendationsForEachToken = listOfRecommendationsForEachToken.stream()
                .map(it -> removeDuplicateRecommendations(it))
                .collect(Collectors.toList());
        
        // remove rejected recommendations
        removeRejectedOrSkippedAnnotations(aRecordService, true, learnSkippedRecommendationTime);

        return calculateDifferencesAndReturnLowestDifference(listOfRecommendationsForEachToken);
    }

    public boolean hasRecommendationWhichIsSkipped(LearningRecordService aRecordService,
            ActiveLearningService aActiveLearningService)
    {
        listOfRecommendationsForEachToken = aActiveLearningService
                .getRecommendationFromRecommendationModel(annotatorState, selectedLayer);
        removeRejectedOrSkippedAnnotations(aRecordService, false, null);
        return !listOfRecommendationsForEachToken.isEmpty();
    }

    private static void removeRecommendationsWithNullAnnotation(
        List<AnnotationObject> recommendationsList)
    {
        if (recommendationsList != null) {
            recommendationsList.removeIf(recommendation -> recommendation.getLabel() == null);
        }
    }

    private static List<AnnotationObject> removeDuplicateRecommendations(
        List<AnnotationObject> unmodifiedRecommendationList)
    {
        List<AnnotationObject> cleanRecommendationList = new ArrayList<>();

        unmodifiedRecommendationList.forEach(recommendationItem -> {
            if (!isAlreadyInCleanList(cleanRecommendationList, recommendationItem)) {
                cleanRecommendationList.add(recommendationItem);
            }
        });

        return cleanRecommendationList;
    }

    private static boolean isAlreadyInCleanList(List<AnnotationObject> cleanRecommendationList,
            AnnotationObject recommendationItem)
    {
        String source = recommendationItem.getSource();
        String annotation = recommendationItem.getLabel();
        String documentName = recommendationItem.getDocumentName();
        
        for (AnnotationObject existedRecommendation : cleanRecommendationList) {
            if (
                    existedRecommendation.getSource().equals(source) &&
                    existedRecommendation.getLabel().equals(annotation) &&
                    existedRecommendation.getDocumentName().equals(documentName)
            ) {
                return true;
            }
        }
        return false;
    }

    private void removeRejectedOrSkippedAnnotations(LearningRecordService aRecordService,
            boolean filterSkippedRecommendation, Date learnSkippedRecommendationTime)
    {
        List<LearningRecord> records = aRecordService.getAllRecordsByDocumentAndUserAndLayer
            (annotatorState.getDocument(), annotatorState.getUser().getUsername(), selectedLayer);
        for (List<AnnotationObject> recommendations : listOfRecommendationsForEachToken) {
            recommendations.removeIf(recommendation -> doesContainRejectedOrSkippedRecord(records,
                recommendation, filterSkippedRecommendation, learnSkippedRecommendationTime));
        }
        listOfRecommendationsForEachToken.removeIf(recommendationsList ->
            recommendationsList.isEmpty());
    }

    private static boolean doesContainRejectedOrSkippedRecord(List<LearningRecord> records,
        AnnotationObject aRecommendation, boolean filterSkippedRecommendation,
        Date learnSkippedRecommendationTime)
    {
        for (LearningRecord record : records) {
            if ((record.getUserAction().equals(LearningRecordUserAction.REJECTED)
                || filterSkippedRecord(record, filterSkippedRecommendation) && needFilterByTime(
                learnSkippedRecommendationTime, record)) && hasSameTokenAndSuggestion(
                aRecommendation, record)) {
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
                aRecord.getOffsetTokenBegin() == aRecommendation.getOffset().getBeginToken() && 
                aRecord.getOffsetTokenEnd() == aRecommendation.getOffset().getEndToken() && 
                aRecord.getAnnotation().equals(aRecommendation.getLabel());
    }

    /**
     * If learnSkippedTime is null, this record needs to be filtered.
     * If the record written time is after the learnSkippedTime, this record needs to be filtered.
     * @param learnSkippedTime
     * @param record
     * @return
     */
    private static boolean needFilterByTime(Date learnSkippedTime, LearningRecord record)
    {
        return learnSkippedTime == null || learnSkippedTime.compareTo(record.getActionDate()) <= 0;
    }

    private static RecommendationDifference calculateDifferencesAndReturnLowestDifference(
            List<List<AnnotationObject>> aListOfRecommendationsForEachToken)
    {
        // create list of recommendationsList, each recommendationsList contains all
        // recommendations from one classifer for one token
        List<List<AnnotationObject>> listOfRecommendationsPerTokenPerClassifier =
            createRecommendationListsPerTokenPerClassifier(aListOfRecommendationsForEachToken);

        // get a list of differences, sorted ascendingly
        List<RecommendationDifference> recommendationDifferences =
            createDifferencesSortedAscendingly(
            listOfRecommendationsPerTokenPerClassifier);
        Optional<RecommendationDifference> recommendationDifference = recommendationDifferences
            .stream().findFirst();
        if (recommendationDifference.isPresent()) {
            return recommendationDifference.get();
        }
        else {
            return null;
        }
    }

    private static List<List<AnnotationObject>> createRecommendationListsPerTokenPerClassifier(
            List<List<AnnotationObject>> aListOfRecommendationsForEachToken)
    {
        List<List<AnnotationObject>> listOfRecommendationsPerTokenPerClassifier = new ArrayList<>();
        for (int i = 0; i < aListOfRecommendationsForEachToken.size(); i++) {
            List<AnnotationObject> recommendationsPerToken =
                    aListOfRecommendationsForEachToken.get(i);
            if (recommendationsPerToken.size() == 1) {
                listOfRecommendationsPerTokenPerClassifier.add(recommendationsPerToken);
            }
            else {
                if (fromMoreThanOneClassifier(recommendationsPerToken)) {
                    splitRecommendationsWithRegardToClassifier(
                        listOfRecommendationsPerTokenPerClassifier, recommendationsPerToken);
                }
                else {
                    listOfRecommendationsPerTokenPerClassifier.add(recommendationsPerToken);
                }
            }
        }
        return listOfRecommendationsPerTokenPerClassifier;
    }

    private static boolean fromMoreThanOneClassifier(
            List<AnnotationObject> recommendationListPerToken)
    {
        int numberOfStringMatchingClassifer = 0;
        int numberOfOpenNLPClassifier = 0;
        for (AnnotationObject recommendation : recommendationListPerToken) {
            if (recommendation.getSource().contains("StringMatching")) {
                numberOfStringMatchingClassifer++;
            }
            if (recommendation.getSource().contains("OpenNlp")) {
                numberOfOpenNLPClassifier++;
            }
        }
        return numberOfOpenNLPClassifier >= 1 && numberOfStringMatchingClassifer >= 1;
    }

    private static void splitRecommendationsWithRegardToClassifier(
            List<List<AnnotationObject>> listOfRecommendationsPerTokenPerClassifier,
            List<AnnotationObject> recommendationsPerToken)
    {
        List<AnnotationObject> stringMatchingClassifierAnnotationObject = new ArrayList<>();
        for (AnnotationObject recommendation : recommendationsPerToken) {
            if (recommendation.getSource().contains("StringMatching")) {
                stringMatchingClassifierAnnotationObject.add(recommendation);
            }
        }
        recommendationsPerToken.removeIf(recommendation -> recommendation.getSource()
            .contains("StringMatching"));
        listOfRecommendationsPerTokenPerClassifier.add(recommendationsPerToken);
        listOfRecommendationsPerTokenPerClassifier.add(stringMatchingClassifierAnnotationObject);
    }

    private static List<RecommendationDifference> createDifferencesSortedAscendingly(
            List<List<AnnotationObject>> listOfRecommendationsPerTokenPerClassifier)
    {
        List<RecommendationDifference> recommendationDifferences = new ArrayList<>();
        for (List<AnnotationObject> recommendationsList :
            listOfRecommendationsPerTokenPerClassifier) {
            RecommendationDifference difference = getSmallestDifferencePerTokenPerClassifier(
                recommendationsList);
            recommendationDifferences.add(difference);
        }
        sortDifferencesAscending(recommendationDifferences);
        return recommendationDifferences;
    }

    private static RecommendationDifference getSmallestDifferencePerTokenPerClassifier(
        List<AnnotationObject> recommendationsList)
    {
        if (recommendationsList.size() == 1) {
            AnnotationObject recommendation = recommendationsList.get(0);
            double confidenceDifference = Math.abs(recommendation.getConfidence());
            return new RecommendationDifference(confidenceDifference, recommendation);
        }
        else {
            Collections.sort(recommendationsList,
                    Comparator.comparingDouble(AnnotationObject::getConfidence).reversed());
            List<RecommendationDifference> differencesAmongOneClassifierPerToken =
                generateDifferencesPerTokenPerClassifier(recommendationsList);
            sortDifferencesAscending(differencesAmongOneClassifierPerToken);
            return differencesAmongOneClassifierPerToken.get(0);
        }
    }

    private static List<RecommendationDifference> generateDifferencesPerTokenPerClassifier(
            List<AnnotationObject> sortedRecommendationsList)
    {
        List<RecommendationDifference> differencesAmongOneClassifierPerToken = new ArrayList<>();
        for (int i = 0; i < sortedRecommendationsList.size() - 1; i++) {
            AnnotationObject firstOp = sortedRecommendationsList.get(i);
            AnnotationObject secondOp = sortedRecommendationsList.get(i + 1);
            double difference = Math.abs(firstOp.getConfidence() - secondOp.getConfidence());
            differencesAmongOneClassifierPerToken
                .add(new RecommendationDifference(difference, firstOp, secondOp));
        }
        return differencesAmongOneClassifierPerToken;
    }

    private static void sortDifferencesAscending(
        List<RecommendationDifference> differences)
    {
        Collections.sort(differences, (rd1, rd2) -> 
                Double.compare(rd1.getDifference(), rd2.getDifference()));
    }

    public Optional<AnnotationObject> generateRecommendationWithLowestConfidence(
            ActiveLearningService aActiveLearningService, JCas aJcas)
    {
        recommendations = aActiveLearningService.getFlattenedRecommendationsFromRecommendationModel(
                aJcas, annotatorState, selectedLayer);
        removeRecommendationsWithNullAnnotation(recommendations);
        removeExistingAnnotations(aJcas, selectedLayer, recommendations);
        Collections.sort(recommendations,
                Comparator.comparingDouble(AnnotationObject::getConfidence));
        return recommendations.stream().findFirst();
    }

    private static void removeExistingAnnotations(JCas aJcas,
            AnnotationLayer aLayer, List<AnnotationObject> aRecommendations)
    {
        Iterator<AnnotationFS> existingAnnotations = getAlreadyExistingAnnotations(aJcas, aLayer);
        List<Integer> existingAnnotationsSpanBegin = mapToBeginOffsets(existingAnnotations);
        aRecommendations.removeIf(recommendation -> existingAnnotationsSpanBegin
                .contains(recommendation.getOffset().getBeginCharacter()));
    }

    private static Iterator<AnnotationFS> getAlreadyExistingAnnotations(JCas aJcas,
            AnnotationLayer aLayer)
    {
        int windowBegin = 0;
        int windowEnd = aJcas.getDocumentText().length() - 1;
        Type type = CasUtil.getType(aJcas.getCas(), aLayer.getName());
        Iterator<AnnotationFS> existingAnnotations = CasUtil
                .selectCovered(aJcas.getCas(), type, windowBegin, windowEnd).iterator();
        return existingAnnotations;
    }

    private static List<Integer> mapToBeginOffsets(
            Iterator<AnnotationFS> existingAnnotations)
    {
        List<Integer> existingAnnotationsSpanBegin = new ArrayList<>();
        while (existingAnnotations.hasNext()) {
            AnnotationFS fs = (AnnotationFS) existingAnnotations.next();
            existingAnnotationsSpanBegin.add(fs.getBegin());
        }
        return existingAnnotationsSpanBegin;
    }

    public boolean checkRecommendationExist(ActiveLearningService aActiveLearningService,
            LearningRecord aRecord)
    {
        listOfRecommendationsForEachToken = aActiveLearningService
                .getRecommendationFromRecommendationModel(annotatorState, selectedLayer);
        return containSuggestion(listOfRecommendationsForEachToken, aRecord);
    }

    private boolean containSuggestion(
            List<List<AnnotationObject>> aListOfRecommendationsForEachToken, LearningRecord record)
    {
        for (List<AnnotationObject> listOfAO : aListOfRecommendationsForEachToken) {
            if (listOfAO.stream().anyMatch(ao -> recordCompareToRecommendation(ao, record))) {
                return true;
            }
        }
        return false;
    }

    public boolean recordCompareToRecommendation(AnnotationObject aRecommendation,
        LearningRecord aRecord)
    {
        return aRecommendation.getLabel().equals(aRecord.getAnnotation())
            && aRecommendation.getDocumentName().equals(aRecord.getSourceDocument().getName())
            && aRecommendation.getOffset().getBeginCharacter() == aRecord.getOffsetCharacterBegin()
            && aRecommendation.getOffset().getEndCharacter() == aRecord.getOffsetCharacterEnd();
    }
}

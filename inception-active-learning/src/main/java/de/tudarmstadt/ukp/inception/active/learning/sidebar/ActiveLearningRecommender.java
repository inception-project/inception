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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.model.LearningRecordUserAction;
import de.tudarmstadt.ukp.inception.recommendation.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.service.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.service.RecommendationService;

public class ActiveLearningRecommender
    implements Serializable
{
    private static final long serialVersionUID = -2308436775710912029L;

    private RecommendationService recommendationService;
    private List<AnnotationObject> recommendations;
    private List<List<AnnotationObject>> listOfRecommendationsForEachToken;
    private AnnotatorState annotatorState;
    private AnnotationLayer selectedLayer;
    private LearningRecordService learningRecordService;
    private DocumentService documentService;

    public ActiveLearningRecommender(RecommendationService recommendationService,
            AnnotatorState annotatorState, AnnotationLayer selectedLayer,
            LearningRecordService recordService)
    {
        this.recommendationService = recommendationService;
        this.annotatorState = annotatorState;
        this.selectedLayer = selectedLayer;
        this.learningRecordService = recordService;
    }

    public RecommendationDifference generateRecommendationWithLowestDifference(
        DocumentService aDocumentService, Date learnSkippedRecommendationTime)
    {
        this.documentService = aDocumentService;

        getRecommendationFromRecommendationModel();

        // remove recommendations with Null Annotation
        listOfRecommendationsForEachToken.forEach(
            recommendationList -> removeRecommendationsWithNullAnnotation(recommendationList));
        listOfRecommendationsForEachToken.removeIf(recommendationList
            -> recommendationList.isEmpty());

        // remove duplicate recommendations
        for (int i = 0; i < listOfRecommendationsForEachToken.size(); i++)
        {
            List<AnnotationObject> recommendationList = listOfRecommendationsForEachToken.get(i);
            listOfRecommendationsForEachToken.set(i,
                removeDuplicateRecommendations(recommendationList));
        }

        // remove rejected recommendations
        removeRejectedOrSkippedAnnotations(true, learnSkippedRecommendationTime);

        return calculateDifferencesAndReturnLowestDifference();
    }

    public boolean hasRecommendationWhichIsSkipped()
    {
        getRecommendationFromRecommendationModel();
        removeRejectedOrSkippedAnnotations(false, null);
        return !listOfRecommendationsForEachToken.isEmpty();
    }

    public void getRecommendationFromRecommendationModel()
    {
        Predictions model = recommendationService.getPredictions(annotatorState.getUser(),
                annotatorState.getProject());

        //getRecommendationsForThisDocument(model);
        getRecommendationsForWholeProject(model);
    }

    // TODO #176 use the document Id once it it available in the CAS
    private void getRecommendationsForThisDocument(Predictions model, JCas aJcas) {
        int windowBegin = 0;
        int windowEnd = aJcas.getDocumentText().length() - 1;
        listOfRecommendationsForEachToken = model
            .getPredictions(annotatorState.getDocument().getName(), selectedLayer,
                windowBegin, windowEnd, aJcas);
    }

    private void getRecommendationsForWholeProject(Predictions model) {
        listOfRecommendationsForEachToken = new ArrayList();

        if (model != null) {
            Map<String, List<List<AnnotationObject>>> recommendationsMap = model
                .getPredictionsForWholeProject(selectedLayer, documentService);


            Set<String> documentNameSet = recommendationsMap.keySet();

            for (String documentName : documentNameSet) {
                listOfRecommendationsForEachToken.addAll(recommendationsMap.get(documentName));
            }
        }
    }

    private void removeRecommendationsWithNullAnnotation(
        List<AnnotationObject> recommendationsList)
    {
        if (recommendationsList != null) {
            if (!recommendationsList.isEmpty()) {
                recommendationsList.removeIf(recommendation -> recommendation.getAnnotation() ==
                    null);
            }
        }
    }

    private List<AnnotationObject> removeDuplicateRecommendations(
        List<AnnotationObject> unmodifiedRecommendationList)
    {
        ArrayList<AnnotationObject> cleanRecommendationList = new ArrayList<>();

        unmodifiedRecommendationList.forEach(recommendationItem -> {
            if (!isAlreadyInCleanList(cleanRecommendationList, recommendationItem)) {
                cleanRecommendationList.add(recommendationItem);
            }
        });

        return cleanRecommendationList;
    }

    private boolean isAlreadyInCleanList(List<AnnotationObject> cleanRecommendationList,
                                         AnnotationObject recommendationItem)
    {
        String classifier = recommendationItem.getClassifier();
        String annotation = recommendationItem.getAnnotation();
        String documentName = recommendationItem.getDocumentName();
        for (AnnotationObject existedRecommendation : cleanRecommendationList) {
            if (existedRecommendation.getClassifier().equals(classifier)
                && existedRecommendation.getAnnotation().equals(annotation) &&
                existedRecommendation.getDocumentName().equals(documentName)) {
                return true;
            }
        }
        return false;
    }

    private void removeRejectedOrSkippedAnnotations(boolean filterSkippedRecommendation,
        Date learnSkippedRecommendationTime)
    {
        List<LearningRecord> records = learningRecordService.getAllRecordsByDocumentAndUserAndLayer
            (annotatorState.getDocument(), annotatorState.getUser().getUsername(), selectedLayer);
        for (List<AnnotationObject> recommendations: listOfRecommendationsForEachToken) {
            recommendations.removeIf(recommendation -> doesContainRejectedOrSkippedRecord(records,
                recommendation, filterSkippedRecommendation, learnSkippedRecommendationTime));
        }
        listOfRecommendationsForEachToken.removeIf(recommendationsList ->
            recommendationsList.isEmpty());
    }

    private boolean doesContainRejectedOrSkippedRecord(List<LearningRecord> records,
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

    private boolean filterSkippedRecord(LearningRecord record, boolean filterSkippedRecommendation)
    {
        return record.getUserAction().equals(LearningRecordUserAction.SKIPPED)
            && filterSkippedRecommendation;
    }

    private boolean hasSameTokenAndSuggestion(AnnotationObject aRecommendation,
        LearningRecord aRecord)
    {
        return aRecord.getSourceDocument().getName().equals(aRecommendation.getDocumentName())
            && aRecord.getOffsetTokenBegin() == aRecommendation.getOffset().getBeginToken()
            && aRecord.getOffsetTokenEnd() == aRecommendation.getOffset().getEndToken() && aRecord
            .getAnnotation().equals(aRecommendation.getAnnotation());
    }

    /**
     * If learnSkippedTime is null, this record needs to be filtered.
     * If the record written time is after the learnSkippedTime, this record needs to be filtered.
     * @param learnSkippedTime
     * @param record
     * @return
     */
    private boolean needFilterByTime(Date learnSkippedTime, LearningRecord record)
    {
        return learnSkippedTime == null || learnSkippedTime.compareTo(record.getActionDate()) <= 0;
    }

    private RecommendationDifference calculateDifferencesAndReturnLowestDifference()
    {
        // create list of recommendationsList, each recommendationsList contains all
        // recommendations from one classifer for one token
        List<List<AnnotationObject>> listOfRecommendationsPerTokenPerClassifier =
            createRecommendationListsPerTokenPerClassifier();

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

    private List<List<AnnotationObject>> createRecommendationListsPerTokenPerClassifier()
    {
        List<List<AnnotationObject>> listOfRecommendationsPerTokenPerClassifier = new ArrayList<>();
        for (int i = 0; i < listOfRecommendationsForEachToken.size(); i++) {
            List<AnnotationObject> recommendationsPerToken =
                listOfRecommendationsForEachToken.get(i);
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

    private boolean fromMoreThanOneClassifier(List<AnnotationObject> recommendationListPerToken)
    {
        int numberOfStringMatchingClassifer = 0;
        int numberOfOpenNLPClassifier = 0;
        for (AnnotationObject recommendation : recommendationListPerToken) {
            if (recommendation.getClassifier().contains("StringMatching")) {
                numberOfStringMatchingClassifer++;
            }
            if (recommendation.getClassifier().contains("OpenNlp")) {
                numberOfOpenNLPClassifier++;
            }
        }
        return numberOfOpenNLPClassifier >= 1 && numberOfStringMatchingClassifer >= 1;
    }

    private void splitRecommendationsWithRegardToClassifier(
        List<List<AnnotationObject>> listOfRecommendationsPerTokenPerClassifier,
        List<AnnotationObject> recommendationsPerToken)
    {
        List<AnnotationObject> stringMatchingClassifierAnnotationObject = new ArrayList<>();
        for (AnnotationObject recommendation : recommendationsPerToken) {
            if (recommendation.getClassifier().contains("StringMatching")) {
                stringMatchingClassifierAnnotationObject.add(recommendation);
            }
        }
        recommendationsPerToken.removeIf(recommendation -> recommendation.getClassifier()
            .contains("StringMatching"));
        listOfRecommendationsPerTokenPerClassifier.add(recommendationsPerToken);
        listOfRecommendationsPerTokenPerClassifier.add(stringMatchingClassifierAnnotationObject);
    }

    private List<RecommendationDifference> createDifferencesSortedAscendingly(
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

    private RecommendationDifference getSmallestDifferencePerTokenPerClassifier(
        List<AnnotationObject> recommendationsList)
    {
        if (recommendationsList.size() == 1) {
            AnnotationObject recommendation = recommendationsList.get(0);
            double confidenceDifference = Math.abs(recommendation.getConfidence());
            return new RecommendationDifference(confidenceDifference, recommendation);
        }
        else {
            List<AnnotationObject> sortedRecommendationsList =
                sortRecommendationsAscendingByConfidenceScore(recommendationsList);
            Collections.reverse(sortedRecommendationsList);
            List<RecommendationDifference> differencesAmongOneClassifierPerToken =
                generateDifferencesPerTokenPerClassifier(sortedRecommendationsList);
            sortDifferencesAscending(differencesAmongOneClassifierPerToken);
            return differencesAmongOneClassifierPerToken.get(0);
        }
    }

    private List<AnnotationObject> sortRecommendationsAscendingByConfidenceScore(
        List<AnnotationObject> recommendationsList)
    {
        Collections.sort(recommendationsList, new Comparator<AnnotationObject>()
        {
            @Override
            public int compare(AnnotationObject ao1, AnnotationObject ao2)
            {
                return Double.compare(ao1.getConfidence(), ao2.getConfidence());
            }
        });
        return recommendationsList;
    }

    private List<RecommendationDifference> generateDifferencesPerTokenPerClassifier(
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

    private void sortDifferencesAscending(
        List<RecommendationDifference> differences)
    {
        Collections.sort(differences, new Comparator<RecommendationDifference>()
        {
            @Override
            public int compare(RecommendationDifference rd1, RecommendationDifference rd2)
            {
                return Double.compare(rd1.getDifference(), rd2.getDifference());
            }
        });
    }

    public Optional<AnnotationObject> generateRecommendationWithLowestConfidence(JCas aJcas)
    {
        getFlattenedRecommendationsFromRecommendationModel(aJcas);
        removeRecommendationsWithNullAnnotation(recommendations);
        removeAlreadyExistingAnnotationsFromFlattenList(aJcas);
        recommendations = sortRecommendationsAscendingByConfidenceScore(recommendations);
        return recommendations.stream().findFirst();
    }

    private void getFlattenedRecommendationsFromRecommendationModel(JCas aJcas)
    {
        int windowBegin = 0;
        int windowEnd = aJcas.getDocumentText().length() - 1;
        Predictions model = recommendationService.getPredictions(annotatorState.getUser(),
                annotatorState.getProject());
        // TODO #176 use the document Id once it it available in the CAS
        recommendations = model.getFlattenedPredictions(annotatorState.getDocument()
            .getName(), selectedLayer, windowBegin, windowEnd, aJcas);
    }

    private void removeAlreadyExistingAnnotationsFromFlattenList(JCas aJcas)
    {
        Iterator existingAnnotations = getAlreadyExistingAnnotations(aJcas);
        ArrayList<Integer> existingAnnotationsSpanBegin = storeExistingAnnotationsSpanBegin(
            existingAnnotations);
        recommendations.removeIf(recommendation -> existingAnnotationsSpanBegin
            .contains(recommendation.getOffset().getBeginCharacter()));
    }

    private Iterator getAlreadyExistingAnnotations(JCas aJcas)
    {
        int windowBegin = 0;
        int windowEnd = aJcas.getDocumentText().length() - 1;
        Type type = CasUtil.getType(aJcas.getCas(), selectedLayer.getName());
        Iterator existingAnnotations = CasUtil
            .selectCovered(aJcas.getCas(), type, windowBegin, windowEnd).iterator();
        return existingAnnotations;
    }

    private ArrayList<Integer> storeExistingAnnotationsSpanBegin(Iterator existingAnnotations)
    {
        ArrayList<Integer> existingAnnotationsSpanBegin = new ArrayList<>();
        while (existingAnnotations.hasNext()) {
            AnnotationFS fs = (AnnotationFS) existingAnnotations.next();
            existingAnnotationsSpanBegin.add(fs.getBegin());
        }
        return existingAnnotationsSpanBegin;
    }

    public boolean checkRecommendationExist(DocumentService documentService, LearningRecord record)
    {
        this.documentService = documentService;
        getRecommendationFromRecommendationModel();
        return containSuggestion(record);
    }

    public boolean containSuggestion(LearningRecord record) {
        for (List<AnnotationObject> listOfAO: listOfRecommendationsForEachToken) {
            if (listOfAO.stream().anyMatch(ao -> recordCompareToRecommendation(ao, record))) {
                return true;
            }
        }
        return false;
    }

    public boolean recordCompareToRecommendation(AnnotationObject aRecommendation,
        LearningRecord aRecord)
    {
        return aRecommendation.getAnnotation().equals(aRecord.getAnnotation())
            && aRecommendation.getDocumentName().equals(aRecord.getSourceDocument().getName())
            && aRecommendation.getOffset().getBeginCharacter() == aRecord.getOffsetCharacterBegin()
            && aRecommendation.getOffset().getEndCharacter() == aRecord.getOffsetCharacterEnd();
    }
}

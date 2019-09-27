/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.api.model;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

/**
 * Stores references to the recommendationService, the currently used JCas and the annotatorState.
 * This class is widely used in the recommendation module.
 * 
 * If the prediction task has run it stores the predicted annotations for an annotation layer in the
 * predictions map.
 */
public class Predictions
    implements Serializable
{
    private static final long serialVersionUID = -1598768729246662885L;
    
    private Map<ExtendedId, AnnotationSuggestion> predictions = new ConcurrentHashMap<>();
    
    private final Project project;
    private final User user;
    
    private Logger logger = LoggerFactory.getLogger(getClass());
    
    public Predictions(Project aProject, User aUser,
            Map<ExtendedId, AnnotationSuggestion> aPredictions)
    {
        if (aProject == null) {
            throw new IllegalArgumentException("The Project is necessary! It cannot be null.");
        }

        project = aProject;
        user = aUser;

        if (aPredictions != null) {
            predictions = new ConcurrentHashMap<ExtendedId, AnnotationSuggestion>(aPredictions);
        }
    }
    
    public Predictions(User aUser, Project aProject) {
        this(aProject, aUser, null);
    }

    /**
     * Get the predictions of a given window for each document, where the outer list is a list of
     * tokens and the inner list is a list of predictions for a token. The method filters all tokens
     * which already have an annotation and don't need further recommendation.
     */
    public Map<String, SuggestionDocumentGroup> getPredictionsForWholeProject(
            AnnotationLayer aLayer, DocumentService aDocumentService)
    {
        Map<String, SuggestionDocumentGroup> result = new HashMap<>();

        List<AnnotationDocument> docs = aDocumentService.listAnnotationDocuments(project, user);

        for (AnnotationDocument doc : docs) {
            // TODO #176 use the document Id once it it available in the CAS
            SuggestionDocumentGroup p = getPredictions(doc.getName(), aLayer, -1, -1);
            result.put(doc.getName(), p);
        }

        return result;
    }
    
    /**
     * TODO #176 use the document Id once it it available in the CAS
     *         
     * Get the predictions of a given window, where the outer list is a list of tokens and the inner
     * list is a list of predictions for a token
     */
    public SuggestionDocumentGroup getPredictions(String aDocumentName,
            AnnotationLayer aLayer, int aWindowBegin, int aWindowEnd)
    {
        return new SuggestionDocumentGroup(getFlattenedPredictions(aDocumentName, aLayer,
                aWindowBegin, aWindowEnd));
    }

    /**
     *  TODO #176 use the document Id once it it available in the CAS
     *         
     * Get the predictions of a document for a given window in a flattened list.
     * If the parameters {@code aWindowBegin} and {@code aWindowEnd} are {@code -1},
     * then they are ignored respectively. This is useful when all suggestions should be fetched.
     */
    private List<AnnotationSuggestion> getFlattenedPredictions(String aDocumentName,
        AnnotationLayer aLayer, int aWindowBegin, int aWindowEnd)
    {
        return predictions.entrySet().stream()
            .filter(f -> f.getKey().getDocumentName().equals(aDocumentName))
            .filter(f -> f.getKey().getLayerId() == aLayer.getId())
            .filter(f -> aWindowBegin == -1 || (f.getKey().getBegin() >= aWindowBegin))
            .filter(f -> aWindowEnd == -1 || (f.getKey().getEnd() <= aWindowEnd))
            .sorted(Comparator.comparingInt(e2 -> e2.getValue().getBegin()))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    /**
     * Returns the first prediction that matches recommendationId and recommenderId
     * in the given document.
     */
    public Optional<AnnotationSuggestion> getPredictionByVID(SourceDocument aDocument, VID aVID)
    {
        return predictions.values().stream()
                .filter(f -> f.getDocumentName().equals(aDocument.getName()))
                .filter(f -> f.getId() == aVID.getSubId())
                .filter(f -> f.getRecommenderId() == aVID.getId())
                .findFirst();
    }

    /**
     * Returns the prediction used to generate the VID
     */
    public Optional<AnnotationSuggestion> getPrediction(SourceDocument aDocument, int aBegin,
            int aEnd, String aLabel)
    {
        return predictions.values().stream()
                .filter(f -> f.getDocumentName().equals(aDocument.getName()))
                .filter(f -> f.getBegin() == aBegin && f.getEnd() == aEnd)
                .filter(f -> f.getLabel().equals(aLabel))
                .max(Comparator.comparingInt(AnnotationSuggestion::getId));
    }
    
    /**
     * 
     * @param aLayerId
     * @param aPredictions - list of sentences containing recommendations
     */
    public void putPredictions(long aLayerId, List<AnnotationSuggestion> aPredictions)
    {
        aPredictions.forEach(prediction -> {
            predictions.put(new ExtendedId(user.getUsername(), project.getId(),
                    prediction.getDocumentName(), aLayerId, prediction.getOffset(),
                    prediction.getRecommenderId(), prediction.getId(), -1), prediction);

        });
    }

    public Project getProject() {
        return project;
    }

    public boolean hasPredictions()
    {
        return !predictions.isEmpty();
    }

    public Map<ExtendedId, AnnotationSuggestion> getPredictions()
    {
        return predictions;
    }
    
    public void clearPredictions()
    {
        predictions.clear();
    }

    public void removePredictions(Long recommenderId)
    {
        predictions.entrySet()
            .removeIf((p) -> p.getKey().getRecommenderId() == recommenderId);
    }

    /**
     * TODO #176 use the document Id once it it available in the CAS
     * Returns a list of predictions for a given token that matches the given layer and
     * the annotation feature in the given document
     *
     * @param aDocumentName the given document name
     * @param aLayer the given layer
     * @param aBegin the offset character begin
     * @param aEnd the offset character end
     * @param aFeature the given annotation feature name
     * @return the annotation suggestions
     */
    public List<AnnotationSuggestion> getPredictionsByTokenAndFeature(String aDocumentName,
        AnnotationLayer aLayer, int aBegin, int aEnd, String aFeature)
    {
        return predictions.entrySet().stream()
            .filter(f -> f.getKey().getDocumentName().equals(aDocumentName))
            .filter(f -> f.getKey().getLayerId() == aLayer.getId())
            .filter(f -> f.getKey().getOffset().getBeginCharacter() == aBegin)
            .filter(f -> f.getKey().getOffset().getEndCharacter() == aEnd)
            .filter(f -> f.getValue().getFeature().equals(aFeature))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    public List<AnnotationSuggestion> getPredictionsByRecommender(Recommender aRecommender)
    {
        return predictions.entrySet().stream()
                .filter(f -> f.getKey().getRecommenderId() == (long) aRecommender.getId())
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }
}

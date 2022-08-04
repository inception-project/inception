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
package de.tudarmstadt.ukp.inception.recommendation.api.model;

import static java.util.Arrays.asList;
import static java.util.Collections.newSetFromMap;
import static java.util.stream.Collectors.toList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;

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
    private Set<String> seenDocumentsForPrediction = newSetFromMap(new ConcurrentHashMap<>());

    private final Project project;
    private final User user;
    private final List<LogMessage> log = new ArrayList<>();

    public Predictions(Project aProject, User aUser, Map<ExtendedId, SpanSuggestion> aPredictions)
    {
        Validate.notNull(aProject, "Project must be specified");
        Validate.notNull(aUser, "User must be specified");

        project = aProject;
        user = aUser;

        if (aPredictions != null) {
            predictions = new ConcurrentHashMap<>(aPredictions);
        }
    }

    public Predictions(User aUser, Project aProject)
    {
        this(aProject, aUser, null);
    }

    /**
     * @param type
     *            the suggestion type
     * @param aLayer
     *            the layer
     * @param aDocumentService
     *            the document service for obtaining documents
     * @param <T>
     *            the suggestion type
     * @return the predictions of a given window for each document, where the outer list is a list
     *         of tokens and the inner list is a list of predictions for a token. The method filters
     *         all tokens which already have an annotation and don't need further recommendation.
     */
    public <T extends AnnotationSuggestion> Map<String, SuggestionDocumentGroup<T>> getPredictionsForWholeProject(
            Class<T> type, AnnotationLayer aLayer, DocumentService aDocumentService)
    {
        Map<String, SuggestionDocumentGroup<T>> result = new HashMap<>();

        List<AnnotationDocument> docs = aDocumentService.listAnnotationDocuments(project, user);

        for (AnnotationDocument doc : docs) {
            // TODO #176 use the document Id once it it available in the CAS
            SuggestionDocumentGroup<T> p = getGroupedPredictions(type, doc.getName(), aLayer, -1,
                    -1);
            result.put(doc.getName(), p);
        }

        return result;
    }

    /**
     * TODO #176 use the document Id once it it available in the CAS
     * 
     * @param type
     *            the type of suggestions to retrieve
     * @param aDocumentName
     *            the name of the document to retrieve suggestions for
     * @param aLayer
     *            the layer to retrieve suggestions for
     * @param aWindowBegin
     *            the begin of the window for which to retrieve suggestions
     * @param aWindowEnd
     *            the end of the window for which to retrieve suggestions
     * @param <T>
     *            the suggestion type
     * 
     * @return the predictions of a given window, where the outer list is a list of tokens and the
     *         inner list is a list of predictions for a token
     */
    public <T extends AnnotationSuggestion> SuggestionDocumentGroup<T> getGroupedPredictions(
            Class<T> type, String aDocumentName, AnnotationLayer aLayer, int aWindowBegin,
            int aWindowEnd)
    {
        return new SuggestionDocumentGroup<>(
                getFlattenedPredictions(type, aDocumentName, aLayer, aWindowBegin, aWindowEnd));
    }

    /**
     * TODO #176 use the document Id once it it available in the CAS
     * 
     * Get the predictions of a document for a given window in a flattened list. If the parameters
     * {@code aWindowBegin} and {@code aWindowEnd} are {@code -1}, then they are ignored
     * respectively. This is useful when all suggestions should be fetched.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <T extends AnnotationSuggestion> List<T> getFlattenedPredictions(Class<T> type,
            String aDocumentName, AnnotationLayer aLayer, int aWindowBegin, int aWindowEnd)
    {
        return predictions.entrySet().stream().filter(f -> type.isInstance(f.getValue()))
                .map(f -> (Entry<ExtendedId, T>) (Entry) f)
                .filter(f -> f.getKey().getDocumentName().equals(aDocumentName))
                .filter(f -> f.getKey().getLayerId() == aLayer.getId())
                // .filter(f -> overlapping(f.getValue().getWindowBegin(),
                // f.getValue().getWindowEnd(),
                // aWindowBegin == -1 ? 0 : aWindowBegin,
                // aWindowEnd == -1 ? MAX_VALUE : aWindowEnd))
                .filter(f -> aWindowBegin == -1 || (f.getValue().getWindowBegin() >= aWindowBegin))
                .filter(f -> aWindowEnd == -1 || (f.getValue().getWindowEnd() <= aWindowEnd))
                .sorted(Comparator.comparingInt(e2 -> e2.getValue().getWindowBegin()))
                .map(Map.Entry::getValue).collect(toList());
    }

    /**
     * @param aDocument
     *            the source document
     * @param aVID
     *            the annotation ID
     * @return the first prediction that matches recommendationId and recommenderId in the given
     *         document.
     */
    public Optional<AnnotationSuggestion> getPredictionByVID(SourceDocument aDocument, VID aVID)
    {
        return predictions.values().stream()
                .filter(f -> f.getDocumentName().equals(aDocument.getName()))
                .filter(f -> f.getId() == aVID.getSubId())
                .filter(f -> f.getRecommenderId() == aVID.getId()).findFirst();
    }

    /**
     * @param aPredictions
     *            list of sentences containing recommendations
     */
    public void putPredictions(List<AnnotationSuggestion> aPredictions)
    {
        aPredictions.forEach(prediction -> predictions.put(new ExtendedId(user.getUsername(),
                project.getId(), prediction.getDocumentName(), prediction.getLayerId(),
                prediction.getPosition(), prediction.getRecommenderId(), prediction.getId(), -1),
                prediction));
    }

    public Project getProject()
    {
        return project;
    }

    public boolean hasPredictions()
    {
        return !predictions.isEmpty();
    }

    public Map<ExtendedId, AnnotationSuggestion> getGroupedPredictions()
    {
        return predictions;
    }

    public void clearPredictions()
    {
        predictions.clear();
        seenDocumentsForPrediction.clear();
    }

    public void removePredictions(Long recommenderId)
    {
        predictions.entrySet().removeIf((p) -> p.getKey().getRecommenderId() == recommenderId);
    }

    /**
     * TODO #176 use the document Id once it it available in the CAS Returns a list of predictions
     * for a given token that matches the given layer and the annotation feature in the given
     * document
     *
     * @param aDocumentName
     *            the given document name
     * @param aLayer
     *            the given layer
     * @param aBegin
     *            the offset character begin
     * @param aEnd
     *            the offset character end
     * @param aFeature
     *            the given annotation feature name
     * @return the annotation suggestions
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List<SpanSuggestion> getPredictionsByTokenAndFeature(String aDocumentName,
            AnnotationLayer aLayer, int aBegin, int aEnd, String aFeature)
    {
        return predictions.entrySet().stream().filter(f -> f.getValue() instanceof SpanSuggestion)
                .map(f -> (Entry<ExtendedId, SpanSuggestion>) (Entry) f)
                .filter(f -> f.getKey().getDocumentName().equals(aDocumentName))
                .filter(f -> f.getKey().getLayerId() == aLayer.getId())
                .filter(f -> f.getValue().getBegin() == aBegin)
                .filter(f -> f.getValue().getEnd() == aEnd)
                .filter(f -> f.getValue().getFeature().equals(aFeature)).map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    public List<AnnotationSuggestion> getPredictionsByRecommenderAndDocument(
            Recommender aRecommender, String aDocument)
    {
        return predictions.entrySet().stream()
                .filter(f -> f.getKey().getRecommenderId() == (long) aRecommender.getId()
                        && f.getKey().getDocumentName().equals(aDocument))
                .map(Map.Entry::getValue).collect(Collectors.toList());
    }

    public List<AnnotationSuggestion> getPredictionsByDocument(String aDocument)
    {
        return predictions.entrySet().stream()
                .filter(f -> f.getKey().getDocumentName().equals(aDocument))
                .map(Map.Entry::getValue).collect(Collectors.toList());
    }

    public void markDocumentAsPredictionCompleted(SourceDocument aDocument)
    {
        seenDocumentsForPrediction.add(aDocument.getName());
    }

    public boolean hasRunPredictionOnDocument(SourceDocument aDocument)
    {
        return seenDocumentsForPrediction.contains(aDocument.getName());
    }

    public void log(LogMessage aMessage)
    {
        synchronized (log) {
            log.add(aMessage);
        }
    }

    public void inheritLog(List<LogMessage> aLogMessages)
    {
        synchronized (log) {
            log.addAll(0, aLogMessages);
        }
    }

    public List<LogMessage> getLog()
    {
        synchronized (log) {
            // Making a copy here because we may still write to the log and don't want to hand out
            // a live copy... which might cause problems, e.g. if the live copy would be used in the
            // Wicket UI and becomes subject to serialization.
            return asList(log.stream().toArray(LogMessage[]::new));
        }
    }
}

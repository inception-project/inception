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
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.Comparator.comparingInt;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.apache.uima.cas.text.AnnotationPredicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

/**
 * If the prediction task has run it stores the predicted annotations for an annotation layer in the
 * predictions map.
 */
public class Predictions
    implements Serializable
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final long serialVersionUID = -1598768729246662885L;

    private final int generation;
    private final Project project;
    private final User sessionOwner;
    private final String dataOwner;

    private final Map<Long, Map<ExtendedId, AnnotationSuggestion>> idxDocuments = new HashMap<>();

    private final Object predictionsLock = new Object();
    private final Set<Long> seenDocumentsForPrediction = new HashSet<>();
    private final List<LogMessage> log = new ArrayList<>();

    // Predictions are (currently) scoped to a user session. We assume that within a single user
    // session, the pool of IDs of positive integer values is never exhausted.
    private int nextId;

    private int addedSuggestionCount = 0;
    private int agedSuggestionCount = 0;
    private int removedSuggestionCount = 0;

    public Predictions(User aSessionOwner, String aDataOwner, Project aProject)
    {
        Validate.notNull(aProject, "Project must be specified");
        Validate.notNull(aSessionOwner, "Session owner must be specified");
        Validate.notNull(aDataOwner, "Data owner must be specified");

        project = aProject;
        sessionOwner = aSessionOwner;
        dataOwner = aDataOwner;
        nextId = 0;
        generation = 1;
    }

    public Predictions(Predictions aPredecessor)
    {
        project = aPredecessor.project;
        sessionOwner = aPredecessor.sessionOwner;
        dataOwner = aPredecessor.dataOwner;
        nextId = aPredecessor.nextId;
        generation = aPredecessor.generation + 1;
    }

    public User getSessionOwner()
    {
        return sessionOwner;
    }

    public String getDataOwner()
    {
        return dataOwner;
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
    public <T extends AnnotationSuggestion> Map<Long, SuggestionDocumentGroup<T>> getPredictionsForWholeProject(
            Class<T> type, AnnotationLayer aLayer, DocumentService aDocumentService)
    {
        var result = new HashMap<Long, SuggestionDocumentGroup<T>>();

        var docs = aDocumentService.listAnnotationDocuments(project, sessionOwner);

        for (var doc : docs) {
            var p = getGroupedPredictions(type, doc.getDocument(), aLayer, -1, -1);
            result.put(doc.getDocument().getId(), p);
        }

        return result;
    }

    /**
     * Gets suggestions of the specified type for the given document.
     * 
     * TODO #176 use the document Id once it it available in the CAS
     * 
     * @param type
     *            the type of suggestions to retrieve
     * @param aDocument
     *            the document to retrieve suggestions for
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
            Class<T> type, SourceDocument aDocument, AnnotationLayer aLayer, int aWindowBegin,
            int aWindowEnd)
    {
        return SuggestionDocumentGroup.groupsOfType(type,
                getFlattenedPredictions(type, aDocument.getId(), aLayer, aWindowBegin, aWindowEnd));
    }

    public <T extends AnnotationSuggestion> SuggestionDocumentGroup<T> getGroupedPredictions(
            Class<T> type, long aDocumentId, AnnotationLayer aLayer, int aWindowBegin,
            int aWindowEnd)
    {
        return SuggestionDocumentGroup.groupsOfType(type,
                getFlattenedPredictions(type, aDocumentId, aLayer, aWindowBegin, aWindowEnd));
    }

    /**
     * Get the predictions of a document for a given window in a flattened list. If the parameters
     * {@code aWindowBegin} and {@code aWindowEnd} are {@code -1}, then they are ignored
     * respectively. This is useful when all suggestions should be fetched.
     * 
     * TODO #176 use the document Id once it it available in the CAS
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <T extends AnnotationSuggestion> List<T> getFlattenedPredictions(Class<T> type,
            long aDocumentId, AnnotationLayer aLayer, int aWindowBegin, int aWindowEnd)
    {
        var windowBegin = aWindowBegin == -1 ? 0 : aWindowBegin;
        var windowEnd = aWindowEnd == -1 ? Integer.MAX_VALUE : aWindowEnd;

        synchronized (predictionsLock) {
            var byDocument = idxDocuments.getOrDefault(aDocumentId, emptyMap());
            return byDocument.entrySet().stream() //
                    .filter(f -> type.isInstance(f.getValue())) //
                    .map(f -> (Entry<ExtendedId, T>) (Entry) f) //
                    .filter(f -> f.getKey().getLayerId() == aLayer.getId()) //
                    .filter(f -> AnnotationPredicates.overlapping(f.getValue().getWindowBegin(),
                            f.getValue().getWindowEnd(), windowBegin, windowEnd))
                    .sorted(comparingInt(e2 -> e2.getValue().getWindowBegin())) //
                    .map(Map.Entry::getValue) //
                    .toList();
        }
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
        synchronized (predictionsLock) {
            var byDocument = idxDocuments.getOrDefault(aDocument.getId(), emptyMap());
            return byDocument.values().stream() //
                    .filter(suggestion -> suggestion.getId() == aVID.getSubId()) //
                    .filter(suggestion -> suggestion.getRecommenderId() == aVID.getId()) //
                    .findFirst();
        }
    }

    public void putSuggestions(int aAdded, int aRemoved, int aAged,
            List<AnnotationSuggestion> aSuggestions)
    {
        synchronized (predictionsLock) {
            addedSuggestionCount += aAdded;
            agedSuggestionCount += aAged;
            removedSuggestionCount += aRemoved;

            var ageZeroSuggestions = 0;
            for (var suggestion : aSuggestions) {
                // Assign ID to predictions that do not have an ID yet
                if (suggestion.getId() == AnnotationSuggestion.NEW_ID) {
                    suggestion = suggestion.assignId(nextId);
                    nextId++;
                    if (nextId < 0) {
                        throw new IllegalStateException(
                                "Annotation suggestion ID overflow. Restart session.");
                    }
                }

                var xid = new ExtendedId(suggestion);
                var byDocument = idxDocuments.computeIfAbsent(suggestion.getDocumentId(),
                        $ -> new HashMap<>());
                byDocument.put(xid, suggestion);

                if (suggestion.getAge() == 0) {
                    ageZeroSuggestions++;
                }
            }

            if (aAdded != ageZeroSuggestions) {
                LOG.warn("Expected [{}] age-zero suggestions but found [{}]", aAdded,
                        ageZeroSuggestions);
            }
        }
    }

    public void inheritSuggestions(List<AnnotationSuggestion> aPredictions)
    {
        synchronized (predictionsLock) {
            for (var prediction : aPredictions) {
                if (prediction.getId() == AnnotationSuggestion.NEW_ID) {
                    throw new IllegalStateException(
                            "Inherited suggestions must already have an ID");
                }

                var xid = new ExtendedId(prediction);
                var byDocument = idxDocuments.computeIfAbsent(prediction.getDocumentId(),
                        $ -> new HashMap<>());
                byDocument.put(xid, prediction);
            }
        }
    }

    public Project getProject()
    {
        return project;
    }

    public boolean isEmpty()
    {
        synchronized (predictionsLock) {
            return idxDocuments.values().stream().allMatch(Map::isEmpty);
        }
    }

    public boolean hasNewSuggestions()
    {
        return addedSuggestionCount > 0;
    }

    public int getNewSuggestionCount()
    {
        return addedSuggestionCount;
    }

    public int size()
    {
        synchronized (predictionsLock) {
            return idxDocuments.values().stream().mapToInt(Map::size).sum();
        }
    }

    public void removePredictions(Long recommenderId)
    {
        synchronized (predictionsLock) {
            idxDocuments.values().forEach(docGroup -> docGroup.entrySet() //
                    .removeIf((p) -> p.getKey().getRecommenderId() == recommenderId));
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List<SpanSuggestion> getAlternativeSuggestions(SpanSuggestion aSuggestion)
    {
        synchronized (predictionsLock) {
            var byDocument = idxDocuments.getOrDefault(aSuggestion.getDocumentId(), emptyMap());
            return byDocument.entrySet().stream() //
                    .filter(f -> f.getValue() instanceof SpanSuggestion) //
                    .map(f -> (Entry<ExtendedId, SpanSuggestion>) (Entry) f) //
                    .filter(f -> f.getKey().getLayerId() == aSuggestion.getLayerId()) //
                    .filter(f -> f.getValue().getBegin() == aSuggestion.getBegin()) //
                    .filter(f -> f.getValue().getEnd() == aSuggestion.getEnd()) //
                    .filter(f -> f.getValue().getFeature().equals(aSuggestion.getFeature())) //
                    .map(Map.Entry::getValue) //
                    .toList();
        }
    }

    /**
     * Returns a list of predictions for a given token that matches the given layer and the
     * annotation feature in the given document
     *
     * @param aDocumentId
     *            the given document ID
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
    // TODO #176 use the document Id once it it available in the CAS
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List<SpanSuggestion> getPredictionsByTokenAndFeature(long aDocumentId,
            AnnotationLayer aLayer, int aBegin, int aEnd, String aFeature)
    {
        synchronized (predictionsLock) {
            var byDocument = idxDocuments.getOrDefault(aDocumentId, emptyMap());
            return byDocument.entrySet().stream() //
                    .filter(f -> f.getValue() instanceof SpanSuggestion) //
                    .map(f -> (Entry<ExtendedId, SpanSuggestion>) (Entry) f) //
                    .filter(f -> f.getKey().getLayerId() == aLayer.getId()) //
                    .filter(f -> f.getValue().getBegin() == aBegin) //
                    .filter(f -> f.getValue().getEnd() == aEnd) //
                    .filter(f -> f.getValue().getFeature().equals(aFeature)) //
                    .map(Map.Entry::getValue) //
                    .toList();
        }
    }

    public List<AnnotationSuggestion> getSuggestionsByRecommenderAndDocument(
            Recommender aRecommender, SourceDocument aDocument)
    {
        return getSuggestionsByRecommenderAndDocument(aRecommender, aDocument.getId());
    }

    public List<AnnotationSuggestion> getSuggestionsByRecommenderAndDocument(
            Recommender aRecommender, long aDocumentId)
    {
        synchronized (predictionsLock) {
            var byDocument = idxDocuments.getOrDefault(aDocumentId, emptyMap());
            return byDocument.entrySet().stream() //
                    .filter(f -> f.getKey().getRecommenderId() == (long) aRecommender.getId())
                    .map(Map.Entry::getValue) //
                    .toList();
        }
    }

    public List<AnnotationSuggestion> getSuggestionsByDocument(SourceDocument aDocument)
    {
        return getPredictionsByDocument(aDocument.getId());
    }

    public List<AnnotationSuggestion> getPredictionsByDocument(long aDocumentId)
    {
        synchronized (predictionsLock) {
            var byDocument = idxDocuments.getOrDefault(aDocumentId, emptyMap());
            return byDocument.entrySet().stream() //
                    .map(Map.Entry::getValue) //
                    .toList();
        }
    }

    public List<AnnotationSuggestion> getSuggestionsByDocument(SourceDocument aDocument,
            int aWindowBegin, int aWindowEnd)
    {
        synchronized (predictionsLock) {
            var byDocument = idxDocuments.getOrDefault(aDocument.getId(), emptyMap());
            return byDocument.entrySet().stream() //
                    .filter(f -> AnnotationPredicates.overlapping(f.getValue().getWindowBegin(),
                            f.getValue().getWindowEnd(), aWindowBegin, aWindowEnd))
                    .sorted(comparingInt(e2 -> e2.getValue().getWindowBegin())) //
                    .map(Map.Entry::getValue) //
                    .toList();
        }
    }

    public void markDocumentAsPredictionCompleted(SourceDocument aDocument)
    {
        synchronized (seenDocumentsForPrediction) {
            seenDocumentsForPrediction.add(aDocument.getId());
        }
    }

    public int getDocumentsSeenCount()
    {
        synchronized (seenDocumentsForPrediction) {
            return seenDocumentsForPrediction.size();
        }
    }

    Set<Long> documentsSeen()
    {
        return unmodifiableSet(seenDocumentsForPrediction);
    }

    public boolean hasRunPredictionOnDocument(SourceDocument aDocument)
    {
        synchronized (seenDocumentsForPrediction) {
            return seenDocumentsForPrediction.contains(aDocument.getId());
        }
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

    public int getGeneration()
    {
        return generation;
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

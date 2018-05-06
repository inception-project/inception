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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
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
    
    private Map<ExtendedId, AnnotationObject> predictions = new ConcurrentHashMap<>();
    
    private final Project project;
    private final User user;
    
    private Logger logger = LoggerFactory.getLogger(getClass());
    
    public Predictions(Project aProject, User aUser, Map<ExtendedId, AnnotationObject> aPredictions)
    {
        if (aProject == null) {
            throw new IllegalArgumentException("The Project is necessary! It cannot be null.");
        }
        
        project = aProject;
        user = aUser;
        
        if (aPredictions != null) {
            predictions = new ConcurrentHashMap<ExtendedId, AnnotationObject> (aPredictions);
        }
    }
    
    public Predictions(Project aProject, User aUser) {
        this(aProject, aUser, null);
    }

    /**
     * 
     * Get the predictions of a given window for each document, 
     * where the outer list is a list of tokens 
     * and the inner list is a list of predictions for a token.
     * The method filters all tokens which already have an annotation
     * and don't need further recommendation.
     * 
     */
    public Map<String, List<List<AnnotationObject>>> getPredictionsForWholeProject(
        AnnotationLayer aLayer, DocumentService aDocumentService, boolean aFilterExisting)
    {
        Map<String, List<List<AnnotationObject>>> predictions = new HashMap<>();

        List<AnnotationDocument> docs = aDocumentService
                .listAnnotationDocuments(project, user);
        
        for (AnnotationDocument doc: docs) {
            JCas jcas;
            try {
                jcas = aDocumentService.readAnnotationCas(doc);
                // TODO #176 use the document Id once it it available in the CAS
                List<List<AnnotationObject>> p = getPredictions(doc.getName(), aLayer, 0,
                        jcas.getDocumentText().length() - 1, jcas, aFilterExisting);
                predictions.put(doc.getName(), p);
            } catch (IOException e) {
                logger.info("Cannot read JCas: ", e);
            }
        }

        return predictions;
    }

    /**
     * TODO #176 use the document Id once it it available in the CAS
     *         
     * Get the predictions of a given window, where the outer list is a list of tokens and the inner
     * list is a list of predictions for a token
     */
    public List<List<AnnotationObject>> getPredictions(String aDocumentName, AnnotationLayer aLayer,
        int aWindowBegin, int aWindowEnd, JCas aJcas, boolean aFilterExisting)
    {
        List<AnnotationObject> p = getFlattenedPredictions(aDocumentName, aLayer, aWindowBegin,
            aWindowEnd, aJcas, aFilterExisting);
        Iterator<AnnotationObject> it = p.iterator();
        
        List<List<AnnotationObject>> result = new ArrayList<>();
        if (!it.hasNext()) {
            // No predictions
            return result;
        }

        List<AnnotationObject> predictionsForCurrentToken = new ArrayList<>();
        AnnotationObject prev = null;
        
        while (it.hasNext()) {
            AnnotationObject current = it.next();
            if (prev == null || current.getOffset().equals(prev.getOffset())) {
                // Append to predictions for current token
                predictionsForCurrentToken.add(current);
            } else {
                // Close current token and move on to the next one
                result.add(predictionsForCurrentToken);
                predictionsForCurrentToken = new ArrayList<>();
                predictionsForCurrentToken.add(current);
            }
            prev = current;
        }
        
        // Close final token
        result.add(predictionsForCurrentToken);

        return result;
    }

    /**
     * 
     *  TODO #176 use the document Id once it it available in the CAS
     *         
     * Get the predictions of a document for a given window in a flattened list
     * @param aJcas 
     */
    public List<AnnotationObject> getFlattenedPredictions(String aDocumentName,
        AnnotationLayer aLayer, int aWindowBegin, int aWindowEnd, JCas aJcas,
        boolean aFilterExisting)
    {
        List<Map.Entry<ExtendedId, AnnotationObject>> p = predictions.entrySet().stream()
            .filter(f -> f.getKey().getDocumentName().equals(aDocumentName))
            .filter(f -> f.getKey().getLayerId() == aLayer.getId())
            .filter(f -> f.getKey().getOffset().getBeginCharacter() >= aWindowBegin)
            .filter(f -> f.getKey().getOffset().getEndCharacter() <= aWindowEnd)
            .sorted(Comparator.comparingInt(e2 -> e2.getValue().getOffset().getBeginCharacter()))
            .collect(Collectors.toList());

        if (aFilterExisting) {
            Type type = CasUtil.getType(aJcas.getCas(), aLayer.getName());
            List<AnnotationFS> existingAnnotations = CasUtil.selectCovered(aJcas.getCas(),
                type, aWindowBegin, aWindowEnd);
            List<Integer> existingOffsets = existingAnnotations.stream()
                .map(AnnotationFS::getBegin)
                .collect(Collectors.toList());

            return p.stream()
                .filter(f -> !existingOffsets.contains(f.getKey().getOffset().getBeginCharacter()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        }
        else {
            return p.stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        }
    }

    /**
     * Returns the first one that matches recommendationId and recommenderId
     * @return
     */
    public AnnotationObject getPredictionByVID(VID aVID) 
    {
        return predictions.values().stream()
                .filter(f -> f.getId() == aVID.getSubId())
                .filter(f -> f.getRecommenderId() == aVID.getId())
                .collect(Collectors.toList()).get(0);
    }

    /**
     * Returns the prediction used to generate the VID
     */
    public Optional<AnnotationObject> getPrediction(int aBegin, int aEnd, String aLabel)
    {
        return predictions.values().stream()
                .filter(f -> f.getOffset().getBeginCharacter() == aBegin
                        && f.getOffset().getEndCharacter() == aEnd)
                .filter(f -> f.getAnnotation().equals(aLabel))
                .max((p1, p2) -> Integer.compare(p1.getId(), p2.getId()));
    }
    
    /**
     * 
     * @param aLayerId
     * @param aPredictions - list of sentences containing recommendations
     */
    public void putPredictions(long aLayerId, List<AnnotationObject> aPredictions)
    {
        aPredictions.forEach(prediction -> {
            if (prediction.getAnnotation() != null) {
                predictions.put(new ExtendedId(user.getUsername(), project.getId(),
                        prediction.getDocumentName(), aLayerId, prediction.getOffset(),
                        prediction.getRecommenderId(), prediction.getId(), -1), prediction);
            }
        });
    }

    public Project getProject() {
        return project;
    }

    public boolean hasPredictions()
    {
        return !predictions.isEmpty();
    }

    public Map<ExtendedId, AnnotationObject> getPredictions()
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
}

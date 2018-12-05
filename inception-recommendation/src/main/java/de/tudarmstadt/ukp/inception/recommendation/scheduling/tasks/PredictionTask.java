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
package de.tudarmstadt.ukp.inception.recommendation.scheduling.tasks;

import static org.apache.uima.fit.util.CasUtil.getAnnotationType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.persistence.NoResultException;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordUserAction;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Offset;
import de.tudarmstadt.ukp.inception.recommendation.api.model.PredictionGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;

/**
 * This consumer predicts new annotations for a given annotation layer, if a classification tool for
 * this layer was selected previously.
 */
public class PredictionTask
    extends Task
{
    private static final double NO_SCORE = 0.0;

    private Logger log = LoggerFactory.getLogger(getClass());
    
    private @Autowired AnnotationSchemaService annoService;
    private @Autowired RecommendationService recommendationService;
    private @Autowired DocumentService documentService;
    private @Autowired LearningRecordService learningRecordService;
    
    public PredictionTask(User aUser, Project aProject)
    {
        super(aProject, aUser);
    }

    @Override
    public void run()
    {
        User user = getUser();

        Project project = getProject();
        Predictions model = new Predictions(project, getUser());
        List<SourceDocument> documents = documentService.listSourceDocuments(project);

        nextDocument: for (SourceDocument document : documents) {
            Optional<JCas> jCas = Optional.empty();
            nextLayer: for (AnnotationLayer layer : annoService.listAnnotationLayer(project)) {
                if (!layer.isEnabled()) {
                    continue nextLayer;
                }

                List<Recommender> recommenders = recommendationService
                    .getActiveRecommenders(user, layer);

                nextRecommender: for (Recommender r : recommenders) {
                    // Make sure we have the latest recommender config from the DB - the one from
                    // the active recommenders list may be outdated
                    Recommender recommender;
                    try {
                        recommender = recommendationService.getRecommender(r.getId());
                    }
                    catch (NoResultException e) {
                        log.info("[{}][{}]: Recommender no longer available... skipping",
                                user.getUsername(), r.getName());
                        continue nextRecommender;
                    }
                    
                    if (!recommender.isEnabled()) {
                        log.debug("[{}][{}]: Disabled - skipping", user.getUsername(), r.getName());
                        continue nextRecommender;
                    }
                    
                    RecommenderContext ctx = recommendationService.getContext(user, recommender);
                    
                    if (!ctx.isReadyForPrediction()) {
                        log.info("Context for recommender [{}]({}) for user [{}] on document "
                                + "[{}]({}) in project [{}]({}) is not ready yet - skipping recommender",
                                recommender.getName(), recommender.getId(), user.getUsername(),
                                document.getName(), document.getId(), project.getName(),
                                project.getId());
                        continue nextRecommender;
                    }
                    
                    RecommendationEngineFactory<?> factory = recommendationService
                            .getRecommenderFactory(recommender);
                    RecommendationEngine recommendationEngine = factory.build(recommender);

                    // We lazily load the CAS only at this point because that allows us to skip
                    // loading the CAS entirely if there is no enabled layer or recommender.
                    // If the CAS cannot be loaded, then we skip to the next document.
                    if (!jCas.isPresent()) {
                        try {
                            jCas = Optional.of(documentService.readAnnotationCas(document,
                                    user.getUsername()));
                        }
                        catch (IOException e) {
                            log.error("Cannot read annotation CAS for user [{}] of document "
                                    + "[{}]({}) in project [{}]({}) - skipping document",
                                    user.getUsername(), document.getName(), document.getId(),
                                    project.getName(), project.getId(), e);
                            continue nextDocument;
                        }
                        try {
                            annoService.upgradeCasIfRequired(jCas.get().getCas(), document,
                                    user.getUsername());
                        }
                        catch (UIMAException | IOException e) {
                            log.error("Cannot upgrade annotation CAS for user [{}] of document "
                                    + "[{}]({}) in project [{}]({}) - skipping document",
                                    user.getUsername(), document.getName(), document.getId(),
                                    project.getName(), project.getId(), e);
                            continue nextDocument;
                        }
                    }
                    
                    try {
                        recommendationEngine.predict(ctx, jCas.get().getCas());
                    }
                    catch (Throwable e) {
                        log.error("Error applying recommender [{}]({}) for user [{}] to document "
                                        + "[{}]({}) in project [{}]({}) - skipping recommender",
                                recommender.getName(), recommender.getId(), user.getUsername(),
                                document.getName(), document.getId(), project.getName(),
                                project.getId(), e);
                        continue nextRecommender;
                    }

                    String predictedTypeName = recommendationEngine.getPredictedType();
                    String predictedFeatureName = recommendationEngine.getPredictedFeature();
                    Optional<String> scoreFeatureName = recommendationEngine.getScoreFeature();
                    Type predictionType = getAnnotationType(jCas.get().getCas(), predictedTypeName);
                    Feature labelFeature = predictionType
                            .getFeatureByBaseName(predictedFeatureName);
                    Optional<Feature> scoreFeature = scoreFeatureName
                            .map(predictionType::getFeatureByBaseName);

                    List<AnnotationObject> predictions = extractAnnotations(user,
                            jCas.get().getCas(), predictionType, labelFeature, scoreFeature,
                            document, recommender);
                    
                    // FIXME: Visibility state calculation code needs to be inserted here again...
                    
                    model.putPredictions(layer.getId(), predictions);

                    // In order to just extract the annotations for a single recommender, each
                    // recommender undoes the changes applied in `recommendationEngine.predict`

                    removePredictions(jCas.get().getCas(), predictionType);
                }
            }
        }

        recommendationService.putIncomingPredictions(getUser(), project, model);
    }

    private List<AnnotationObject> extractAnnotations(User aUser, CAS aCas, Type predictionType,
            Feature predictedFeature, Optional<Feature> aScoreFeature, SourceDocument aDocument,
            Recommender aRecommender)
    {
        int predictionCount = 0;

        // Replace this with CasMetadata
        String documentUri = aDocument.getName();
        try {
            DocumentMetaData dmd = DocumentMetaData.get(aCas);
            documentUri = dmd.getDocumentUri();
        } catch (IllegalArgumentException e) {
            log.warn("No DocumentMetaData in CAS, using document name as document URI!");
        }
        
        List<AnnotationObject> result = new ArrayList<>();
        int id = 0;
        for (AnnotationFS annotationFS : CasUtil.select(aCas, predictionType)) {
            List<Token> tokens = JCasUtil.selectCovered(Token.class, annotationFS);
            Token firstToken = tokens.get(0);
            Token lastToken = tokens.get(tokens.size() - 1);

            String label = annotationFS.getFeatureValueAsString(predictedFeature);
            double score = aScoreFeature.map(f -> FSUtil.getFeature(annotationFS, f, Double.class))
                    .orElse(NO_SCORE);
            String featurename = aRecommender.getFeature();
            String name = aRecommender.getName();

            AnnotationObject ao = new AnnotationObject(id, aRecommender.getId(), name, featurename,
                    aDocument.getName(), documentUri, firstToken.getBegin(), lastToken.getEnd(),
                    annotationFS.getCoveredText(), label, label, score);

            result.add(ao);
            id++;
            
            predictionCount++;
        }
        
        log.debug(
                "[{}]({}) for user [{}] on document "
                        + "[{}]({}) in project [{}]({}) generated {} predictions.",
                aRecommender.getName(), aRecommender.getId(), aUser.getUsername(),
                aDocument.getName(), aDocument.getId(), aRecommender.getProject().getName(),
                aRecommender.getProject().getId(), predictionCount);
        
        return result;
    }

    private void removePredictions(CAS aCas, Type aPredictionType)
    {
        for (AnnotationFS fs : CasUtil.select(aCas, aPredictionType)) {
            aCas.removeFsFromIndexes(fs);
        }
    }

    /**
     * Goes through all AnnotationObjects and determines the visibility of each one
     */
    public static void calculateVisibility(
        LearningRecordService aLearningRecordService, AnnotationSchemaService aAnnotationService,
        JCas aJcas, String aUser, AnnotationDocument aDoc, AnnotationLayer aLayer,
        Map<Offset, PredictionGroup> aRecommendations, int aWindowBegin, int aWindowEnd)
    {
        List<LearningRecord> recordedAnnotations = aLearningRecordService
                .getAllRecordsByDocumentAndUserAndLayer(aDoc.getDocument(), aUser, aLayer);

        // Recommendations sorted by Offset, Id, RecommenderId, DocumentName.hashCode (descending)
        NavigableSet<AnnotationObject> remainingRecommendations = new TreeSet<>();
        aRecommendations.values().forEach(group -> remainingRecommendations.addAll(group));

        // Collect all annotations within the view window (typically the screen)
        Type type = CasUtil.getType(aJcas.getCas(), aLayer.getName());
        Collection<AnnotationFS> existingAnnotations = CasUtil.select(aJcas.getCas(), type)
                .stream()
                .filter(fs -> fs.getBegin() >= aWindowBegin && fs.getEnd() <= aWindowEnd)
                .collect(Collectors.toList());

        AnnotationObject swap = remainingRecommendations.pollFirst();

        for (AnnotationFeature feature: aAnnotationService.listAnnotationFeature(aLayer)) {

            // Keep only AnnotationFS that have at least one feature value set
            List<AnnotationFS> annoFsForFeature = existingAnnotations.stream()
                .filter(fs ->
                    fs.getStringValue(fs.getType().getFeatureByBaseName(feature.getName())) != null)
                .collect(Collectors.toList());
            for (AnnotationFS fs : annoFsForFeature) {

                AnnotationObject ao = swap;

                // Go to the next token for which an annotation exists
                while (ao.getBegin() < fs.getBegin() && !remainingRecommendations.isEmpty()) {
                    setVisibility(recordedAnnotations, ao);
                    ao = remainingRecommendations.pollFirst();
                    swap = ao;
                }

                // For tokens with annotations also check whether the annotation is for the same
                // feature as the predicted label
                while (ao.getBegin() == fs.getBegin() && !remainingRecommendations.isEmpty()) {
                    if (isOverlappingForFeature(fs, ao, feature)) {
                        ao.setVisible(false);
                    } else {
                        setVisibility(recordedAnnotations, ao);
                    }
                    ao = remainingRecommendations.pollFirst();
                    swap = ao;
                }
            }

            // Check last AnnotationObject
            if (swap != null && !annoFsForFeature.isEmpty()) {
                if (isOverlappingForFeature(annoFsForFeature.get(annoFsForFeature.size() - 1), swap,
                    feature)) {
                    swap.setVisible(false);
                }
                else {
                    setVisibility(recordedAnnotations, swap);
                }
            }
        }

        // Check for the remaining AnnotationObjects whether they have an annotation
        // and are not rejected
        for (AnnotationObject ao: remainingRecommendations) {
            setVisibility(recordedAnnotations, ao);
        }
    }

    private static boolean isOverlappingForFeature(AnnotationFS aFs, AnnotationObject aAo,
        AnnotationFeature aFeature)
    {
        return aFeature.getName().equals(aAo.getFeature()) &&
            ((aFs.getBegin() <= aAo.getBegin())
                && (aFs.getEnd() >= aAo.getEnd())
            || (aFs.getBegin() >= aAo.getBegin())
                && (aFs.getEnd() <= aAo.getEnd())
            || (aFs.getBegin() >= aAo.getBegin())
                && (aFs.getEnd() >= aAo.getEnd())
                && (aFs.getBegin() < aAo.getEnd())
            || (aFs.getBegin() <= aAo.getBegin())
                && (aFs.getEnd() <= aAo.getEnd())
                && (aFs.getEnd() > aAo.getBegin()));
    }

    /**
     * Determines whether this recommendation has been rejected
     */
    private static boolean isRejected(List<LearningRecord> aRecordedRecommendations,
        AnnotationObject aAo)
    {
        for (LearningRecord record : aRecordedRecommendations) {
            if (record.getOffsetCharacterBegin() == aAo.getBegin()
                && record.getOffsetCharacterEnd() == aAo.getEnd()
                && record.getAnnotation().equals(aAo.getLabel())
                && record.getUserAction().equals(LearningRecordUserAction.REJECTED)
            ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets visibility of an AnnotationObject based on label and Learning Record
     */
    private static boolean setVisibility(List<LearningRecord> aRecordedRecommendations,
        AnnotationObject aAo)
    {
        boolean hasNoAnnotation = aAo.getLabel() == null;
        boolean isRejected = isRejected(aRecordedRecommendations, aAo);
        if (hasNoAnnotation || isRejected) {
            aAo.setVisible(false);
            return false;
        }
        else {
            aAo.setVisible(true);
            return true;
        }
    }
}

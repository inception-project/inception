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

import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType.REJECTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecordType.SKIPPED;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;
import static org.apache.uima.fit.util.CasUtil.getAnnotationType;
import static org.apache.uima.fit.util.CasUtil.select;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import javax.persistence.NoResultException;

import org.apache.commons.lang3.Validate;
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
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.LearningRecord;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Offset;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionDocumentGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.util.OverlapIterator;

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

                    List<AnnotationSuggestion> predictions = extractAnnotations(user,
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

    private List<AnnotationSuggestion> extractAnnotations(User aUser, CAS aCas, Type predictionType,
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
        
        List<AnnotationSuggestion> result = new ArrayList<>();
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

            AnnotationSuggestion ao = new AnnotationSuggestion(id, aRecommender.getId(), name,
                    aRecommender.getLayer().getId(), featurename, aDocument.getName(), documentUri,
                    firstToken.getBegin(), lastToken.getEnd(), annotationFS.getCoveredText(), label,
                    label, score);

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
    public static void calculateVisibility(LearningRecordService aLearningRecordService,
            AnnotationSchemaService aAnnotationService, JCas aJcas, String aUser,
            AnnotationDocument aDoc, AnnotationLayer aLayer,
            SuggestionDocumentGroup aRecommendations, int aWindowBegin, int aWindowEnd)
    {
        Validate.isTrue(aRecommendations.getDocumentName().equals(aDoc.getDocument().getName()),
                "Recommendations are for document [%s] but visibility calculation requested for [%s]",
                aRecommendations.getDocumentName(), aDoc.getDocument().getName());

        // Collect all annotations of the given layer within the view window
        Type type = CasUtil.getType(aJcas.getCas(), aLayer.getName());
        List<AnnotationFS> annotationsInWindow = select(aJcas.getCas(), type).stream()
                .filter(fs -> aWindowBegin <= fs.getBegin() && fs.getEnd() <= aWindowEnd)
                .collect(toList());
        
        // Collect all suggestions of the given layer within the view window
        List<SuggestionGroup> suggestionsInWindow = aRecommendations.stream()
                // Only suggestions for the given layer
                .filter(group -> group.getLayerId() == aLayer.getId())
                // ... and in the given window
                .filter(group -> {
                    Offset offset = group.getOffset();
                    return aWindowBegin <= offset.getBegin() && offset.getEnd() <= aWindowEnd;
                })
                .collect(toList());
        
        // Get all the skipped/rejected entries for the current layer
        List<LearningRecord> recordedAnnotations = aLearningRecordService
                .getAllRecordsByDocumentAndUserAndLayer(aDoc.getDocument(), aUser, aLayer);
        
        for (AnnotationFeature feature : aAnnotationService.listAnnotationFeature(aLayer)) {
            Feature feat = type.getFeatureByBaseName(feature.getName());
            
            // Reduce the annotations to the once which have a non-null feature value
            Map<Offset, AnnotationFS> annotations = new TreeMap<>(
                    comparingInt(Offset::getBegin).thenComparingInt(Offset::getEnd));
            annotationsInWindow.stream()
                    .filter(fs -> fs.getFeatureValueAsString(feat) != null)
                    .forEach(fs -> annotations.put(new Offset(fs.getBegin(), fs.getEnd()), fs));
            
            // Reduce the suggestions to the ones for the given feature
            Map<Offset, SuggestionGroup> suggestions = new TreeMap<>(
                    comparingInt(Offset::getBegin).thenComparingInt(Offset::getEnd));
            suggestionsInWindow.stream()
                    .filter(group -> group.getFeature().equals(feature.getName()))
                    .forEach(group -> suggestions.put(group.getOffset(), group));

            // If there are no suggestions or no annotations, there is nothing to do here
            if (suggestions.isEmpty() || annotations.isEmpty()) {
                continue;
            }
            
            // This iterator gives us pairs of annotations and suggestions 
            OverlapIterator oi = new OverlapIterator(
                    new ArrayList<>(suggestions.keySet()),
                    new ArrayList<>(annotations.keySet()));
            
            // Bulk-hide any groups that overlap with existing annotations on the current layer
            // and for the current feature
            while (oi.hasNext()) {
                if (oi.getA().overlaps(oi.getB())) {
                    // Fetch the current suggestion
                    SuggestionGroup group = suggestions.get(oi.getA());
                    group.forEach(suggestion -> suggestion.hide("overlaps with annotation"));
                    // Do not want to process the group again since it is already hidden
                    oi.ignoraA();
                }
                oi.step();
            }
            
            // Anything that was not hidden so far might still have been rejected or not have a
            // label
            suggestions.values().stream()
                    .flatMap(group -> group.stream())
                    .filter(AnnotationSuggestion::isVisible)
                    .forEach(suggestion -> hideSuggestionsRejectedOrWithoutLabel(
                            suggestion, recordedAnnotations));
        }
    }

    private static void hideSuggestionsRejectedOrWithoutLabel(
            AnnotationSuggestion aSuggestion, List<LearningRecord> aRecordedRecommendations)
    {
        // If there is no label, then hide it
        if (aSuggestion.getLabel() == null) {
            aSuggestion.hide("no label");
            return;
        }

        // If it was rejected or skipped, it hide it
        for (LearningRecord record : aRecordedRecommendations) {
            if (record.getOffsetCharacterBegin() == aSuggestion.getBegin()
                    && record.getOffsetCharacterEnd() == aSuggestion.getEnd()
                    && record.getAnnotation().equals(aSuggestion.getLabel())
                    && (REJECTED.equals(record.getUserAction()) || 
                        SKIPPED.equals(record.getUserAction()))) {
                aSuggestion.hide("rejected or skipped");
                return;
            }
        }
    }
}

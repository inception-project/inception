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
package de.tudarmstadt.ukp.inception.recommendation.tasks;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.EXCLUSIVE_WRITE_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.inception.recommendation.api.recommender.PredictionCapability.PREDICTION_USES_TEXT_ONLY;
import static de.tudarmstadt.ukp.inception.recommendation.api.recommender.TrainingCapability.TRAINING_NOT_SUPPORTED;
import static de.tudarmstadt.ukp.inception.rendering.model.Range.rangeCoveringDocument;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.NoResultException;

import org.apache.commons.lang3.Validate;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageSession;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommenderTypeSystemUtils;
import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionSupport;
import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionSupportRegistry;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationSuggestion;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.SuggestionDocumentGroup;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.ExtractionContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.PredictionContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommenderTaskNotificationEvent;
import de.tudarmstadt.ukp.inception.rendering.model.Range;
import de.tudarmstadt.ukp.inception.scheduling.TaskMonitor;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.StopWatch;
import de.tudarmstadt.ukp.inception.support.WebAnnoConst;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;

public class PredictionTask
    extends RecommendationTask_ImplBase
{
    public static final String TYPE = "PredictionTask";

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String PREDICTION_CAS = "predictionCas";

    private @Autowired AnnotationSchemaService schemaService;
    private @Autowired RecommendationService recommendationService;
    private @Autowired DocumentService documentService;
    private @Autowired ApplicationEventPublisher appEventPublisher;
    private @Autowired SuggestionSupportRegistry suggestionSupportRegistry;

    private final SourceDocument currentDocument;
    private final int predictionBegin;
    private final int predictionEnd;
    private final String dataOwner;
    private final boolean isolated;
    private final Recommender recommender;

    private Predictions predictions;

    public PredictionTask(Builder<? extends Builder<?>> aBuilder)
    {
        super(aBuilder.withType(TYPE));

        currentDocument = aBuilder.currentDocument;
        dataOwner = aBuilder.dataOwner;
        predictionBegin = aBuilder.predictionBegin;
        predictionEnd = aBuilder.predictionEnd;
        isolated = aBuilder.isolated;
        recommender = aBuilder.recommender;
    }

    /**
     * For testing.
     * 
     * @param aSchemaService
     *            schema service
     */
    void setSchemaService(AnnotationSchemaService aSchemaService)
    {
        schemaService = aSchemaService;
    }

    @Override
    public String getTitle()
    {
        return "Generating annotation suggestions...";
    }

    @Override
    public void execute()
    {
        try (var session = CasStorageSession.openNested()) {
            var project = getProject();
            var sessionOwner = getSessionOwner();

            var startTime = System.currentTimeMillis();
            predictions = generatePredictions();
            predictions.inheritLog(getLogMessages());
            logPredictionComplete(predictions, startTime);

            if (!isolated) {
                recommendationService.putIncomingPredictions(sessionOwner, project, predictions);

                if (predictions.hasNewSuggestions()) {
                    appEventPublisher.publishEvent(RecommenderTaskNotificationEvent
                            .builder(this, project, sessionOwner.getUsername()) //
                            .withMessage(LogMessage.info(this,
                                    predictions.getNewSuggestionCount()
                                            + " new predictions available" //
                                            + " (some may be hidden/merged)")) //
                            .build());
                }
                else {
                    appEventPublisher.publishEvent(RecommenderTaskNotificationEvent
                            .builder(this, project, sessionOwner.getUsername()) //
                            .withMessage(LogMessage.info(this,
                                    "Prediction run produced no new suggestions")) //
                            .build());
                }
            }
        }
    }

    public Predictions getPredictions()
    {
        return predictions;
    }

    private User getSessionOwner()
    {
        return getUser().orElseThrow();
    }

    private Predictions generatePredictions()
    {
        var project = getProject();
        var docs = documentService.listSourceDocuments(project);

        // Do we need to predict ALL documents (e.g. in active learning mode)
        if (!isolated && recommendationService
                .isPredictForAllDocuments(getSessionOwner().getUsername(), project)) {
            try {
                return generatePredictionsOnAllDocuments(docs);
            }
            finally {
                // We reset this in case the state was not properly cleared, e.g. the AL session
                // was started but then the browser closed. Places where it is set include
                // - ActiveLearningSideBar::moveToNextRecommendation
                recommendationService.setPredictForAllDocuments(getSessionOwner().getUsername(),
                        project, false);
            }
        }

        return generatePredictionsOnSingleDocument(currentDocument, docs, getMonitor());
    }

    /**
     * Generate predictions for all documents. No predictions are inherited.
     *
     * @param aDocuments
     *            the documents to compute the predictions for.
     * @return the new predictions.
     */
    private Predictions generatePredictionsOnAllDocuments(List<SourceDocument> aDocuments)
    {
        logPredictionStartedForAllDocuments(aDocuments);

        var monitor = getMonitor();
        var sessionOwner = getSessionOwner();
        var project = getProject();
        var activePredictions = isolated ? null
                : recommendationService.getPredictions(sessionOwner, project);
        var incomingPredictions = activePredictions != null ? new Predictions(activePredictions)
                : new Predictions(sessionOwner, dataOwner, project);

        var maxProgress = aDocuments.size();
        var progress = 0;

        try (var casHolder = new PredictionCasHolder()) {
            for (var document : aDocuments) {
                monitor.setProgressWithMessage(progress, maxProgress,
                        LogMessage.info(this, "%s", document.getName()));
                applyRecommendersToDocument(activePredictions, incomingPredictions, casHolder.cas,
                        document, -1, -1);
                progress++;
            }

            monitor.setProgressWithMessage(progress, maxProgress,
                    LogMessage.info(this, "%d documents processed", progress));

            return incomingPredictions;
        }
        catch (ResourceInitializationException e) {
            logErrorCreationPredictionCas(incomingPredictions);
            return incomingPredictions;
        }
    }

    /**
     * Generate predictions for a single document. Any predictions available for other documents are
     * inherited.
     *
     * @param aCurrentDocument
     *            the document to compute the predictions for.
     * @param aDocuments
     *            all documents from the current project.
     * @return the new predictions.
     */
    private Predictions generatePredictionsOnSingleDocument(SourceDocument aCurrentDocument,
            List<SourceDocument> aDocuments, TaskMonitor aMonitor)
    {
        var sessionOwner = getSessionOwner();
        var project = getProject();
        var activePredictions = isolated ? null
                : recommendationService.getPredictions(sessionOwner, project);
        var incomingPredictions = activePredictions != null ? new Predictions(activePredictions)
                : new Predictions(sessionOwner, dataOwner, project);

        aMonitor.setMaxProgress(1);

        if (activePredictions != null) {
            // Limit prediction to a single document and inherit the rest
            var documentsToInheritSuggestionsFor = aDocuments.stream() //
                    .filter(d -> !d.equals(currentDocument)) //
                    .toList();

            logPredictionStartedForOneDocumentWithInheritance(documentsToInheritSuggestionsFor);

            for (var document : documentsToInheritSuggestionsFor) {
                inheritSuggestionsAtDocumentLevel(project, document, activePredictions,
                        incomingPredictions);
            }
        }
        else {
            logPredictionStartedForOneDocumentWithoutInheritance();
        }

        try (var casHolder = new PredictionCasHolder()) {

            final CAS predictionCas = casHolder.cas;
            applyRecommendersToDocument(activePredictions, incomingPredictions, predictionCas,
                    aCurrentDocument, predictionBegin, predictionEnd);
        }
        catch (ResourceInitializationException e) {
            logErrorCreationPredictionCas(incomingPredictions);
        }

        aMonitor.setProgress(1);

        return incomingPredictions;
    }

    /**
     * @param aPredictions
     *            the predictions to populate
     * @param aPredictionCas
     *            the re-usable buffer CAS to use when calling recommenders
     * @param aDocument
     *            the current document
     * @param aPredictionBegin
     *            begin of the prediction range (negative to predict from 0)
     * @param aPredictionEnd
     *            end of the prediction range (negative to predict until the end of the document)
     */
    private void applyRecommendersToDocument(Predictions aActivePredictions,
            Predictions aPredictions, CAS aPredictionCas, SourceDocument aDocument,
            int aPredictionBegin, int aPredictionEnd)
    {
        if (recommender == null) {
            applyAllRecommendersToDocument(aActivePredictions, aPredictions, aPredictionCas,
                    aDocument, aPredictionBegin, aPredictionEnd);
        }
        else {
            try {
                var originalCas = new LazyCas(aDocument);
                applySingleRecomenderToDocument(originalCas, recommender, aActivePredictions,
                        aPredictions, aPredictionCas, aDocument, aPredictionBegin, aPredictionEnd);
            }
            catch (IOException e) {
                logUnableToReadAnnotations(aPredictions, aDocument, e);
                return;
            }
        }
    }

    /**
     * @param aPredictions
     *            the predictions to populate
     * @param aPredictionCas
     *            the re-usable buffer CAS to use when calling recommenders
     * @param aDocument
     *            the current document
     * @param aPredictionBegin
     *            begin of the prediction range (negative to predict from 0)
     * @param aPredictionEnd
     *            end of the prediction range (negative to predict until the end of the document)
     */
    private void applyAllRecommendersToDocument(Predictions aActivePredictions,
            Predictions aPredictions, CAS aPredictionCas, SourceDocument aDocument,
            int aPredictionBegin, int aPredictionEnd)
    {
        var activeRecommenders = recommendationService
                .getActiveRecommenders(aPredictions.getSessionOwner(), aDocument.getProject());
        if (activeRecommenders.isEmpty()) {
            logNoActiveRecommenders(aPredictions);
            return;
        }

        try {
            var originalCas = new LazyCas(aDocument);
            for (var activeRecommender : activeRecommenders) {
                var layer = schemaService
                        .getLayer(activeRecommender.getRecommender().getLayer().getId());
                if (!layer.isEnabled()) {
                    continue;
                }

                // Make sure we have the latest recommender config from the DB - the one
                // from the active recommenders list may be outdated
                var rec = activeRecommender.getRecommender();
                try {
                    rec = recommendationService.getRecommender(rec.getId());
                }
                catch (NoResultException e) {
                    logRecommenderNotAvailable(aPredictions, rec);
                    continue;
                }

                if (!rec.isEnabled()) {
                    logRecommenderDisabled(aPredictions, rec);
                    continue;
                }

                applySingleRecomenderToDocument(originalCas, rec, aActivePredictions, aPredictions,
                        aPredictionCas, aDocument, aPredictionBegin, aPredictionEnd);
            }
        }
        catch (IOException e) {
            logUnableToReadAnnotations(aPredictions, aDocument, e);
            return;
        }

        // When all recommenders have completed on the document, we mark it as "complete"
        aPredictions.markDocumentAsPredictionCompleted(aDocument);
    }

    private void applySingleRecomenderToDocument(LazyCas aOriginalCas, Recommender aRecommender,
            Predictions activePredictions, Predictions aPredictions, CAS predictionCas,
            SourceDocument aDocument, int aPredictionBegin, int aPredictionEnd)
        throws IOException
    {
        var sessionOwner = getSessionOwner();
        var context = isolated ? Optional.of(RecommenderContext.emptyContext())
                : recommendationService.getContext(sessionOwner.getUsername(), aRecommender);
        if (!context.isPresent()) {
            logRecommenderHasNoContext(aPredictions, aDocument, aRecommender);
            return;
        }

        var maybeFactory = recommendationService.getRecommenderFactory(aRecommender);
        if (maybeFactory.isEmpty()) {
            logNoRecommenderFactory(aRecommender);
            return;
        }
        var factory = maybeFactory.get();

        // Check that configured layer and feature are accepted by this type of recommender
        if (!factory.accepts(aRecommender.getLayer(), aRecommender.getFeature())) {
            logInvalidRecommenderConfiguration(aPredictions, aRecommender);
            return;
        }

        // We lazily load the CAS only at this point because that allows us to skip
        // loading the CAS entirely if there is no enabled layer or recommender.
        // If the CAS cannot be loaded, then we skip to the next document.
        var originalCas = aOriginalCas.get();

        try {
            var engine = factory.build(aRecommender);

            if (!engine.isReadyForPrediction(context.get())) {
                logRecommenderContextNoReady(aPredictions, aDocument, aRecommender);

                // If possible, we inherit recommendations from a previous run while
                // the recommender is still busy
                if (activePredictions != null) {
                    inheritSuggestionsAtRecommenderLevel(aPredictions, originalCas, aRecommender,
                            activePredictions, aDocument);
                }

                return;
            }

            // If the recommender is not trainable and not sensitive to annotations,
            // we can actually re-use the predictions.
            if (TRAINING_NOT_SUPPORTED == engine.getTrainingCapability()
                    && PREDICTION_USES_TEXT_ONLY == engine.getPredictionCapability()
                    && activePredictions != null
                    && activePredictions.hasRunPredictionOnDocument(aDocument)) {
                inheritSuggestionsAtRecommenderLevel(aPredictions, originalCas,
                        engine.getRecommender(), activePredictions, aDocument);
                return;
            }

            var ctx = new PredictionContext(context.get());
            cloneAndMonkeyPatchCAS(getProject(), originalCas, predictionCas);
            var predictionRange = new Range(aPredictionBegin < 0 ? 0 : aPredictionBegin,
                    aPredictionEnd < 0 ? originalCas.getDocumentText().length() : aPredictionEnd);
            invokeRecommender(aPredictions, ctx, engine, activePredictions, aDocument, originalCas,
                    predictionCas, predictionRange);
            ctx.getMessages().forEach(aPredictions::log);
        }
        // Catching Throwable is intentional here as we want to continue the
        // execution even if a particular recommender fails.
        catch (Throwable e) {
            logErrorExecutingRecommender(aPredictions, aDocument, aRecommender, e);

            appEventPublisher.publishEvent(RecommenderTaskNotificationEvent
                    .builder(this, getProject(), sessionOwner.getUsername()) //
                    .withMessage(LogMessage.error(this, "Recommender [%s] failed: %s",
                            aRecommender.getName(), e.getMessage())) //
                    .build());

            // If there was a previous successful run of the recommender, inherit
            // its suggestions to avoid that all the suggestions of the recommender
            // simply disappear.
            if (activePredictions != null) {
                inheritSuggestionsAtRecommenderLevel(aPredictions, originalCas, aRecommender,
                        activePredictions, aDocument);
            }

            return;
        }
    }

    /**
     * Invokes the engine to produce new suggestions.
     */
    private void invokeRecommender(Predictions aIncomingPredictions, PredictionContext aCtx,
            RecommendationEngine aEngine, Predictions aActivePredictions, SourceDocument aDocument,
            CAS aOriginalCas, CAS aPredictionCas, Range aPredictionRange)
        throws RecommendationException
    {
        var rec = aEngine.getRecommender();

        // Extract the suggestions from the data which the recommender has written into the CAS
        // We need this only for the extraction, but there is no point in investing the time for
        // the prediction if we cannot extract the data afterwards - hence we obtain it now and
        // skip the prediciton if it is not available
        var maybeSuggestionSupport = suggestionSupportRegistry.findGenericExtension(rec);
        if (maybeSuggestionSupport.isEmpty()) {
            logNoSuggestionSupportAvailable(aIncomingPredictions, rec);
            return;
        }
        var supportRegistry = maybeSuggestionSupport.get();

        // Perform the actual prediction
        var predictedRange = predict(aIncomingPredictions, aCtx, aEngine, aPredictionCas,
                aPredictionRange);

        var generatedSuggestions = extractSuggestions(aIncomingPredictions, aDocument, aOriginalCas,
                aPredictionCas, rec, supportRegistry);

        // Reconcile new suggestions with suggestions from previous run
        var reconciliationResult = reconcile(aActivePredictions, aDocument, rec, predictedRange,
                generatedSuggestions);

        logGeneratedPredictions(aIncomingPredictions, aDocument, rec, predictedRange,
                generatedSuggestions, reconciliationResult);

        // Inherit suggestions that are outside the range which was predicted. Note that the engine
        // might actually predict a different range from what was requested. If the prediction
        // covers the entire document, we can skip this.
        var suggestions = reconciliationResult.suggestions;
        var aged = reconciliationResult.aged;
        if (aActivePredictions != null
                && !predictedRange.equals(rangeCoveringDocument(aOriginalCas))) {
            aged = inheritOutOfRangeSuggestions(aIncomingPredictions, aActivePredictions, aDocument,
                    rec, predictedRange, suggestions, aged);
        }

        // Calculate the visibility of the suggestions. This happens via the original CAS which
        // contains only the manually created annotations and *not* the suggestions.
        calculateVisibility(aIncomingPredictions, aEngine, aDocument, aOriginalCas, suggestions);

        aIncomingPredictions.putSuggestions(reconciliationResult.added,
                reconciliationResult.removed, aged, suggestions);
    }

    /**
     * Extracts existing predictions from the last prediction run so we do not have to recalculate
     * them. This is useful when the engine is not trainable.
     */
    private void inheritSuggestionsAtRecommenderLevel(Predictions aPredictions, CAS aOriginalCas,
            Recommender aRecommender, Predictions activePredictions, SourceDocument document)
    {
        var suggestions = activePredictions.getPredictionsByRecommenderAndDocument(aRecommender,
                document.getName());

        if (suggestions.isEmpty()) {
            logNoInheritablePredictions(aPredictions, aRecommender, document);
            return;
        }

        logInheritedPredictions(aPredictions, aRecommender, document, suggestions);

        aPredictions.inheritSuggestions(suggestions);
    }

    /**
     * Extracts existing predictions from the last prediction run so we do not have to recalculate
     * them. This is useful when the engine is not trainable.
     */
    private void inheritSuggestionsAtDocumentLevel(Project aProject, SourceDocument aDocument,
            Predictions aOldPredictions, Predictions aNewPredictions)
    {
        if (!aOldPredictions.hasRunPredictionOnDocument(aDocument)) {
            return;
        }

        var suggestions = aOldPredictions.getPredictionsByDocument(aDocument.getName());

        logPredictionsInherited(aProject, aDocument, suggestions);

        aNewPredictions.inheritSuggestions(suggestions);
        aNewPredictions.markDocumentAsPredictionCompleted(aDocument);
    }

    private int inheritOutOfRangeSuggestions(Predictions aIncomingPredictions,
            Predictions aActivePredictions, SourceDocument aDocument, Recommender aRecommender,
            Range predictedRange, List<AnnotationSuggestion> suggestions, int aged)
    {
        var inheritableSuggestions = aActivePredictions
                .getPredictionsByRecommenderAndDocument(aRecommender, aDocument.getName()).stream() //
                .filter(s -> !s.coveredBy(predictedRange)) //
                .collect(toList());

        logInheritedPredictions(aIncomingPredictions, aRecommender, aDocument,
                inheritableSuggestions);

        for (var suggestion : inheritableSuggestions) {
            aged++;
            suggestion.incrementAge();
        }
        suggestions.addAll(inheritableSuggestions);
        return aged;
    }

    private List<AnnotationSuggestion> extractSuggestions(Predictions aIncomingPredictions,
            SourceDocument aDocument, CAS aOriginalCas, CAS aPredictionCas,
            Recommender aRecommender, SuggestionSupport supportRegistry)
    {
        var extractionContext = new ExtractionContext(aIncomingPredictions.getGeneration(),
                aRecommender, aDocument, aOriginalCas, aPredictionCas);
        return supportRegistry.extractSuggestions(extractionContext);
    }

    private Range predict(Predictions aIncomingPredictions, PredictionContext aCtx,
            RecommendationEngine aEngine, CAS aPredictionCas, Range aPredictionRange)
        throws RecommendationException
    {
        logStartGeneratingPredictions(aIncomingPredictions, aEngine.getRecommender());

        return aEngine.predict(aCtx, aPredictionCas, aPredictionRange.getBegin(),
                aPredictionRange.getEnd());
    }

    private void calculateVisibility(Predictions aIncomingPredictions, RecommendationEngine aEngine,
            SourceDocument aDocument, CAS aOriginalCas, List<AnnotationSuggestion> suggestions)
    {
        var groupedSuggestions = SuggestionDocumentGroup.groupByType(suggestions);
        for (var groupEntry : groupedSuggestions.entrySet()) {
            recommendationService.calculateSuggestionVisibility(
                    aIncomingPredictions.getSessionOwner().getUsername(), aDocument, aOriginalCas,
                    aIncomingPredictions.getDataOwner(), aEngine.getRecommender().getLayer(),
                    groupEntry.getValue(), 0, aOriginalCas.getDocumentText().length());
        }
    }

    static ReconciliationResult reconcile(Predictions aActivePredictions, SourceDocument aDocument,
            Recommender recommender, Range predictedRange,
            List<AnnotationSuggestion> aNewProtoSuggestions)
    {
        if (aActivePredictions == null) {
            return new ReconciliationResult(aNewProtoSuggestions.size(), 0, 0,
                    aNewProtoSuggestions);
        }

        var reconciledSuggestions = new LinkedHashSet<AnnotationSuggestion>();
        var addedSuggestions = new ArrayList<AnnotationSuggestion>();
        int agedSuggestionsCount = 0;

        var predictionsByRecommenderAndDocument = aActivePredictions
                .getPredictionsByRecommenderAndDocument(recommender, aDocument.getName());

        var existingSuggestionsByPosition = predictionsByRecommenderAndDocument.stream() //
                .filter(s -> s.coveredBy(predictedRange)) //
                .collect(groupingBy(AnnotationSuggestion::getPosition));

        for (var newSuggestion : aNewProtoSuggestions) {
            var existingSuggestions = existingSuggestionsByPosition
                    .getOrDefault(newSuggestion.getPosition(), emptyList()).stream() //
                    .filter(s -> matchesForReconciliation(newSuggestion, s)) //
                    .limit(2) // One to use, the second to warn that there was more than one
                    .toList();

            if (existingSuggestions.isEmpty()) {
                addedSuggestions.add(newSuggestion);
                continue;
            }

            if (existingSuggestions.size() > 1) {
                LOG.debug("Recommender produced more than one suggestion with the same "
                        + "label, score and score explanation - reconciling with first one");
            }

            var existingSuggestion = existingSuggestions.get(0);
            if (!reconciledSuggestions.contains(existingSuggestion)) {
                existingSuggestion.incrementAge();
                agedSuggestionsCount++;
                reconciledSuggestions.add(existingSuggestion);
            }
        }

        var removedSuggestions = predictionsByRecommenderAndDocument.stream() //
                .filter(s -> s.coveredBy(predictedRange)) //
                .filter(s -> !reconciledSuggestions.contains(s)) //
                .toList();

        var finalSuggestions = new ArrayList<>(reconciledSuggestions);
        finalSuggestions.addAll(addedSuggestions);
        return new ReconciliationResult(addedSuggestions.size(), removedSuggestions.size(),
                agedSuggestionsCount, finalSuggestions);
    }

    private static boolean matchesForReconciliation(AnnotationSuggestion aNew,
            AnnotationSuggestion aExisting)
    {
        return aNew.getRecommenderId() == aExisting.getRecommenderId() && //
                Objects.equals(aExisting.getLabel(), aNew.getLabel());
    }

    private void logNoSuggestionSupportAvailable(Predictions aIncomingPredictions,
            Recommender aRecommender)
    {
        LOG.debug("There is no comparible suggestion support for {} - skipping prediction");
        aIncomingPredictions.log(LogMessage.warn(aRecommender.getName(), //
                "Prediction skipped since there is no compatible suggestion support."));
    }

    private void logErrorCreationPredictionCas(Predictions aPredictions)
    {
        aPredictions
                .log(LogMessage.error(this, "Cannot create prediction CAS, stopping predictions!"));
        LOG.error("Cannot create prediction CAS, stopping predictions!");
    }

    private void logPredictionsInherited(Project aProject, SourceDocument aDocument,
            List<AnnotationSuggestion> suggestions)
    {
        LOG.debug("[{}]({}) for user [{}] on document {} in project {} inherited {} predictions",
                "ALL", "--", getSessionOwner().getUsername(), aDocument, aProject,
                suggestions.size());
    }

    private void logErrorExecutingRecommender(Predictions aPredictions, SourceDocument aDocument,
            Recommender aRecommender, Throwable e)
    {
        aPredictions.log(LogMessage.error(aRecommender.getName(), "Failed: %s", e.getMessage()));
        LOG.error("Error applying recommender {} for user {} to document {} in project {} - " //
                + "skipping recommender", aRecommender, getSessionOwner(), aDocument,
                aDocument.getProject(), e);
    }

    private void logStartGeneratingPredictions(Predictions aIncomingPredictions,
            Recommender aRecommender)
    {
        aIncomingPredictions.log(LogMessage.info(aRecommender.getName(),
                "Generating predictions for layer [%s]...", aRecommender.getLayer().getUiName()));
        LOG.trace("{}[{}]: Generating predictions for layer [{}]", getSessionOwner(),
                aRecommender.getName(), aRecommender.getLayer().getUiName());
    }

    private void logUnableToReadAnnotations(Predictions aPredictions, SourceDocument aDocument,
            IOException e)
    {
        aPredictions.log(LogMessage.error(this, "Cannot read annotation CAS... skipping"));
        LOG.error(
                "Cannot read annotation CAS for user {} of document "
                        + "[{}]({}) in project [{}]({}) - skipping document",
                getSessionOwner(), aDocument.getName(), aDocument.getId(),
                aDocument.getProject().getName(), aDocument.getProject().getId(), e);
    }

    private void logNoActiveRecommenders(Predictions aPredictions)
    {
        aPredictions.log(LogMessage.info(this, "No active recommenders"));
        LOG.trace("[{}]: No active recommenders", getSessionOwner());
    }

    private void logInheritedPredictions(Predictions aPredictions, Recommender aRecommender,
            SourceDocument document, List<AnnotationSuggestion> suggestions)
    {
        LOG.debug("[{}][{}]: {} on document {} in project {} inherited {} predictions", getId(),
                getSessionOwner().getUsername(), aRecommender, document, aRecommender.getProject(),
                suggestions.size());
        aPredictions.log(LogMessage.info(aRecommender.getName(),
                "Inherited [%d] predictions from previous run", suggestions.size()));
    }

    private void logNoInheritablePredictions(Predictions aPredictions, Recommender aRecommender,
            SourceDocument document)
    {
        LOG.debug("[{}][{}]: {} on document {} in project {} there " //
                + "are no inheritable predictions", getId(), getSessionOwner().getUsername(),
                aRecommender, document, aRecommender.getProject());
        aPredictions.log(LogMessage.info(aRecommender.getName(),
                "No inheritable suggestions from previous run"));
    }

    private void logPredictionComplete(Predictions aPredictions, long startTime)
    {
        var duration = currentTimeMillis() - startTime;
        LOG.debug("[{}][{}]: Prediction complete ({} ms)", getId(), getSessionOwner().getUsername(),
                duration);
        aPredictions.log(LogMessage.info(this, "Prediction complete (%d ms).", duration));
    }

    private void logPredictionStartedForOneDocumentWithInheritance(List<SourceDocument> inherit)
    {
        LOG.debug(
                "[{}][{}]: Starting prediction for project [{}] on one document "
                        + "(inheriting [{}]) triggered by [{}]",
                getId(), getSessionOwner().getUsername(), getProject(), inherit.size(),
                getTrigger());
        info("Starting prediction triggered by [%s]...", getTrigger());
    }

    private void logPredictionStartedForOneDocumentWithoutInheritance()
    {
        LOG.debug(
                "[{}][{}]: Starting prediction for project [{}] on one document "
                        + "triggered by [{}]",
                getId(), getSessionOwner().getUsername(), getProject(), getTrigger());
        info("Starting prediction triggered by [%s]...", getTrigger());
    }

    private void logPredictionStartedForAllDocuments(List<SourceDocument> docs)
    {
        LOG.debug(
                "[{}][{}]: Starting prediction for project [{}] on [{}] documents triggered by [{}]",
                getId(), getSessionOwner().getUsername(), getProject(), docs.size(), getTrigger());
        info("Starting prediction triggered by [%s]...", getTrigger());
    }

    private void logGeneratedPredictions(Predictions aIncomingPredictions, SourceDocument aDocument,
            Recommender aRecommender, Range predictedRange,
            List<AnnotationSuggestion> generatedSuggestions,
            ReconciliationResult reconciliationResult)
    {
        LOG.debug(
                "{} for user {} on document {} in project {} generated {} predictions within range {} (+{}/-{}/={})",
                aRecommender, getSessionOwner(), aDocument, aRecommender.getProject(),
                generatedSuggestions.size(), predictedRange, reconciliationResult.added,
                reconciliationResult.removed, reconciliationResult.aged);
        aIncomingPredictions.log(LogMessage.info(aRecommender.getName(), //
                "Generated [%d] predictions within range %s (+%d/-%d/=%d)",
                generatedSuggestions.size(), predictedRange, reconciliationResult.added,
                reconciliationResult.removed, reconciliationResult.aged));
    }

    private void logRecommenderContextNoReady(Predictions aPredictions, SourceDocument aDocument,
            Recommender aRecommender)
    {
        aPredictions.log(LogMessage.info(aRecommender.getName(),
                "Recommender context is not ready... skipping"));
        LOG.info("Recommender context {} for user {} in project {} is not ready for " //
                + "prediction - skipping recommender", aRecommender, getSessionOwner(),
                aDocument.getProject());
    }

    private void logInvalidRecommenderConfiguration(Predictions aPredictions,
            Recommender aRecommender)
    {
        aPredictions.log(LogMessage.info(aRecommender.getName(),
                "Recommender configured with invalid layer or feature... skipping"));
        LOG.info(
                "[{}][{}]: Recommender configured with invalid layer or feature "
                        + "- skipping recommender",
                getSessionOwner().getUsername(), aRecommender.getName());
    }

    private void logNoRecommenderFactory(Recommender aRecommender)
    {
        LOG.warn("[{}][{}]: No factory found - skipping recommender",
                getSessionOwner().getUsername(), aRecommender.getName());
    }

    private void logRecommenderHasNoContext(Predictions aPredictions, SourceDocument aDocument,
            Recommender aRecommender)
    {
        aPredictions.log(
                LogMessage.info(aRecommender.getName(), "Recommender has no context... skipping"));
        LOG.info("No context available for recommender {} for user {} on document {} in " //
                + "project {} - skipping recommender", aRecommender, getSessionOwner(), aDocument,
                aDocument.getProject());
    }

    private void logRecommenderDisabled(Predictions aPredictions, Recommender aRecommender)
    {
        aPredictions
                .log(LogMessage.info(aRecommender.getName(), "Recommender disabled... skipping"));
        LOG.debug("{}[{}]: Disabled - skipping", getSessionOwner(), aRecommender.getName());
    }

    private void logRecommenderNotAvailable(Predictions aPredictions, Recommender aRecommender)
    {
        aPredictions.log(LogMessage.info(aRecommender.getName(),
                "Recommender no longer available... skipping"));
        LOG.info("{}[{}]: Recommender no longer available... skipping", getSessionOwner(),
                aRecommender.getName());
    }

    /**
     * Clones the source CAS to the target CAS while adding the features required for encoding
     * predictions to the respective types.
     * 
     * @param aProject
     *            the project to which the CASes belong.
     * @param aSourceCas
     *            the source CAS.
     * @param aTargetCas
     *            the target CAS which is meant to be sent off to a recommender.
     * @return the target CAS which is meant to be sent off to a recommender.
     * @throws UIMAException
     *             if there was a CAS-related error.
     * @throws IOException
     *             if there was a serialization-related errror.
     */
    CAS cloneAndMonkeyPatchCAS(Project aProject, CAS aSourceCas, CAS aTargetCas)
        throws UIMAException, IOException
    {
        try (var watch = new StopWatch(LOG, "adding score features")) {
            var tsd = schemaService.getFullProjectTypeSystem(aProject);
            var features = schemaService.listAnnotationFeature(aProject);

            RecommenderTypeSystemUtils.addPredictionFeaturesToTypeSystem(tsd, features);

            schemaService.upgradeCas(aSourceCas, aTargetCas, tsd);
        }

        return aTargetCas;
    }

    private class LazyCas
    {
        private final SourceDocument document;

        private CAS originalCas;

        public LazyCas(SourceDocument aDocument)
        {
            document = aDocument;
        }

        public CAS get() throws IOException
        {
            if (originalCas == null) {
                originalCas = documentService.readAnnotationCas(document, dataOwner,
                        AUTO_CAS_UPGRADE, SHARED_READ_ONLY_ACCESS);
            }

            return originalCas;
        }
    }

    private static class PredictionCasHolder
        implements AutoCloseable
    {
        private final CAS cas;

        public PredictionCasHolder() throws ResourceInitializationException
        {
            cas = WebAnnoCasUtil.createCas();
            CasStorageSession.get().add(PREDICTION_CAS, EXCLUSIVE_WRITE_ACCESS, cas);
        }

        @Override
        public void close()
        {
            CasStorageSession.get().remove(cas);
        }
    }

    final record ReconciliationResult(int added, int removed, int aged,
            List<AnnotationSuggestion> suggestions)
    {}

    public static Builder<Builder<?>> builder()
    {
        return new Builder<>();
    }

    public static class Builder<T extends Builder<?>>
        extends RecommendationTask_ImplBase.Builder<T>
    {
        private Recommender recommender;
        private SourceDocument currentDocument;
        private String dataOwner;
        private int predictionBegin = -1;
        private int predictionEnd = -1;
        private boolean isolated = false;

        /**
         * Generate predictions only for the specified recommender. If this is not set, then
         * predictions will be run for all active recommenders.
         * 
         * @param aRecommender
         *            the one recommender to run.
         */
        @SuppressWarnings("unchecked")
        public T withRecommender(Recommender aRecommender)
        {
            recommender = aRecommender;
            return (T) this;
        }

        /**
         * @param aCurrentDocuemnt
         *            the document currently open in the editor of the user triggering the task.
         */
        @SuppressWarnings("unchecked")
        public T withCurrentDocument(SourceDocument aCurrentDocuemnt)
        {
            currentDocument = aCurrentDocuemnt;
            return (T) this;
        }

        /**
         * @param aDataOwner
         *            the user owning the annotations currently shown in the editor (this can differ
         *            from the user owning the session e.g. if a manager views another users
         *            annotations or a curator is performing curation to the
         *            {@link WebAnnoConst#CURATION_USER})
         */
        @SuppressWarnings("unchecked")
        public T withDataOwner(String aDataOwner)
        {
            dataOwner = aDataOwner;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T withRange(int aBegin, int aEnd)
        {
            predictionBegin = aBegin;
            predictionEnd = aEnd;
            return (T) this;
        }

        /**
         * Whether to use the recommender session data from the recommender service or to run the
         * prediction task in isolation from the session. Running in isolation is useful when
         * predictions should be generated outside the normal automatically triggered recommender
         * runs. After the isolated run has completed, the results can be picked up using
         * {@link #getPredictions()}.
         * 
         * @param aIsolated
         *            whether to run the task in isolation.
         */
        @SuppressWarnings("unchecked")
        public T withIsolated(boolean aIsolated)
        {
            isolated = aIsolated;
            return (T) this;
        }

        public PredictionTask build()
        {
            Validate.notNull(sessionOwner, "SelectionTask requires a user");

            withProject(currentDocument.getProject());

            return new PredictionTask(this);
        }
    }
}

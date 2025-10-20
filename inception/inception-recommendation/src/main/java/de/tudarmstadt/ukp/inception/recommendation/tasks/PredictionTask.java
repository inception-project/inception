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
import static de.tudarmstadt.ukp.inception.recommendation.tasks.PredictionTask.ReconciliationOption.KEEP_EXISTING;
import static de.tudarmstadt.ukp.inception.rendering.model.Range.rangeCoveringDocument;
import static de.tudarmstadt.ukp.inception.scheduling.ProgressScope.SCOPE_DOCUMENTS;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommenderTypeSystemUtils;
import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionSupport;
import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionSupportQuery;
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
import de.tudarmstadt.ukp.inception.recommendation.config.RecommenderProperties;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommenderTaskNotificationEvent;
import de.tudarmstadt.ukp.inception.rendering.model.Range;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.StopWatch;
import de.tudarmstadt.ukp.inception.support.WebAnnoConst;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;
import jakarta.persistence.NoResultException;

public class PredictionTask
    extends RecommendationTask_ImplBase
{
    public static final String TYPE = "PredictionTask";

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @Autowired AnnotationSchemaService schemaService;
    private @Autowired RecommendationService recommendationService;
    private @Autowired DocumentService documentService;
    private @Autowired ApplicationEventPublisher appEventPublisher;
    private @Autowired SuggestionSupportRegistry suggestionSupportRegistry;
    private @Autowired RecommenderProperties properties;

    private final SourceDocument currentDocument;
    private final int predictionBegin;
    private final int predictionEnd;
    private final String dataOwner;
    private final boolean isolated;
    private final List<Recommender> recommenders;
    private final boolean synchronousRecommenders;
    private final boolean asynchronousRecommenders;
    private final Set<ReconciliationOption> reconciliationOptions;

    private Predictions predictions;

    public PredictionTask(Builder<? extends Builder<?>> aBuilder)
    {
        super(aBuilder.withType(TYPE).withCancellable(true));

        currentDocument = aBuilder.currentDocument;
        dataOwner = aBuilder.dataOwner;
        predictionBegin = aBuilder.predictionBegin;
        predictionEnd = aBuilder.predictionEnd;
        isolated = aBuilder.isolated;
        recommenders = aBuilder.recommenders;
        synchronousRecommenders = aBuilder.synchronousRecommenders;
        asynchronousRecommenders = aBuilder.asynchronousRecommenders;
        reconciliationOptions = new HashSet<>(aBuilder.reconciliationOptions);
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
            var sessionOwner = getMandatorySessionOwner();

            var startTime = currentTimeMillis();
            predictions = generatePredictions();
            predictions.inheritLog(getLogMessages());
            logPredictionComplete(predictions, startTime);

            if (!isolated) {
                recommendationService.putIncomingPredictions(sessionOwner, project, predictions);

                if (predictions.hasNewSuggestions()) {
                    logNewPredictionsAvailable(project, sessionOwner);
                }
                else {
                    logNoNewPredictionsAvailable(project, sessionOwner);
                }
            }
        }
    }

    public Predictions getPredictions()
    {
        return predictions;
    }

    private User getMandatorySessionOwner()
    {
        return getSessionOwner().orElseThrow();
    }

    private Predictions generatePredictions()
    {
        var project = getProject();
        var docs = documentService.listSourceDocuments(project);

        // Do we need to predict ALL documents (e.g. in active learning mode)
        if (!isolated && recommendationService
                .isPredictForAllDocuments(getMandatorySessionOwner().getUsername(), project)) {
            try {
                return generatePredictionsOnAllDocuments(docs);
            }
            finally {
                // We reset this in case the state was not properly cleared, e.g. the AL session
                // was started but then the browser closed. Places where it is set include
                // - ActiveLearningSideBar::moveToNextRecommendation
                recommendationService.setPredictForAllDocuments(
                        getMandatorySessionOwner().getUsername(), project, false);
            }
        }

        return generatePredictionsOnSingleDocument(currentDocument, docs);
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
        var sessionOwner = getMandatorySessionOwner();
        var project = getProject();
        var activePredictions = getPredecessorPredictions(sessionOwner, project);
        var incomingPredictions = activePredictions != null //
                ? new Predictions(activePredictions) //
                : new Predictions(sessionOwner, dataOwner, project);

        var maxProgress = aDocuments.size();

        try (var casHolder = new PredictionCasHolder()) {
            try (var progress = monitor.openScope(SCOPE_DOCUMENTS, maxProgress)) {
                for (var document : aDocuments) {
                    if (monitor.isCancelled()) {
                        break;
                    }

                    progress.update(up -> up.increment() //
                            .addMessage(LogMessage.info(this, "%s", document.getName())));

                    applyActiveRecommendersToDocument(activePredictions, incomingPredictions,
                            casHolder.cas, document, -1, -1);
                }

                progress.update(up -> up.addMessage(
                        LogMessage.info(this, "%d documents processed", monitor.getProgress())));

                return incomingPredictions;
            }
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
            List<SourceDocument> aDocuments)
    {
        var sessionOwner = getMandatorySessionOwner();
        var project = getProject();
        var predecessorPredictions = getPredecessorPredictions(sessionOwner, project);
        var incomingPredictions = predecessorPredictions != null
                ? new Predictions(predecessorPredictions)
                : new Predictions(sessionOwner, dataOwner, project);

        try (var progress = getMonitor().openScope("document", 1)) {
            progress.update(up -> up.increment());

            if (predecessorPredictions != null) {
                // Limit prediction to a single document and inherit the rest
                var documentsToInheritSuggestionsFor = aDocuments.stream() //
                        .filter(d -> !d.equals(currentDocument)) //
                        .toList();

                logPredictionStartedForOneDocumentWithInheritance(documentsToInheritSuggestionsFor);

                for (var document : documentsToInheritSuggestionsFor) {
                    inheritSuggestionsAtDocumentLevel(project, document, predecessorPredictions,
                            incomingPredictions);
                }
            }
            else {
                logPredictionStartedForOneDocumentWithoutInheritance();
            }

            try (var casHolder = new PredictionCasHolder()) {
                var predictionCas = casHolder.cas;

                if (isolated) {
                    var originalCas = new LazyCas(aCurrentDocument);
                    for (var recommender : recommenders) {
                        try {
                            applySingleRecomenderToDocument(originalCas, recommender,
                                    predecessorPredictions, incomingPredictions, predictionCas,
                                    aCurrentDocument, predictionBegin, predictionEnd);
                        }
                        catch (IOException e) {
                            logUnableToReadAnnotations(incomingPredictions, aCurrentDocument, e);
                        }
                    }
                }
                else {
                    applyActiveRecommendersToDocument(predecessorPredictions, incomingPredictions,
                            predictionCas, aCurrentDocument, predictionBegin, predictionEnd);
                }
            }
            catch (ResourceInitializationException e) {
                logErrorCreationPredictionCas(incomingPredictions);
            }

            progress.update(up -> up.addMessage(
                    LogMessage.info(this, "%d documents processed", progress.getProgress())));
        }

        return incomingPredictions;
    }

    private Predictions getPredecessorPredictions(User sessionOwner, Project project)
    {
        if (isolated) {
            return null;
        }

        var incomingPredictions = recommendationService.getIncomingPredictions(sessionOwner,
                project);
        if (incomingPredictions != null) {
            return incomingPredictions;
        }

        return recommendationService.getPredictions(sessionOwner, project);
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
    private void applyActiveRecommendersToDocument(Predictions aActivePredictions,
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
                var rec = activeRecommender.getRecommender();
                // If a recommender is explicitly requested, the configuration from the requested
                // recommender object takes precedence over what is stored in the database
                if (!recommenders.isEmpty() && recommenders.contains(rec)) {
                    rec = recommenders.get(recommenders.indexOf(rec));
                }
                else {
                    // Make sure we have the latest recommender config from the DB - the one
                    // from the active recommenders list may be outdated
                    try {
                        rec = recommendationService.getRecommender(rec.getId());
                    }
                    catch (NoResultException e) {
                        logRecommenderNotAvailable(aPredictions, rec);
                        continue;
                    }
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
        var layer = schemaService.getLayer(aRecommender.getLayer().getId());
        if (!layer.isEnabled()) {
            return;
        }

        if (!aRecommender.isEnabled()) {
            logRecommenderDisabled(aPredictions, aRecommender);
            return;
        }

        var sessionOwner = getMandatorySessionOwner();
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
        if (!factory.accepts(aRecommender)) {
            logInvalidRecommenderConfiguration(aPredictions, aRecommender);
            return;
        }

        if (!recommenders.isEmpty() && !recommenders.contains(aRecommender)) {
            logSkippingNotRequestedRecommender(aPredictions, aRecommender);

            if (activePredictions != null) {
                inheritSuggestionsAtRecommenderLevel(aPredictions, aRecommender, activePredictions,
                        aDocument);
            }

            return;
        }

        if (recommenders.isEmpty() && factory.isInteractive(aRecommender)) {
            logSkippingInteractiveRecommenderNotExplicitlyRequested(aPredictions, aRecommender);

            if (activePredictions != null) {
                inheritSuggestionsAtRecommenderLevel(aPredictions, aRecommender, activePredictions,
                        aDocument);
            }

            return;
        }

        var engine = factory.build(aRecommender);

        var isSynchronous = factory.isSynchronous(aRecommender);
        if (isSynchronous && !synchronousRecommenders) {
            logSkippingSynchronous(aPredictions, aRecommender);

            // If possible, we inherit recommendations from a previous run while the recommender is
            // still busy
            if (activePredictions != null) {
                inheritSuggestionsAtRecommenderLevel(aPredictions, aRecommender, activePredictions,
                        aDocument);
            }

            return;
        }

        if (!isSynchronous && !asynchronousRecommenders) {
            logSkippingAsynchronous(aPredictions, aRecommender);

            // If possible, we inherit recommendations from a previous run while the recommender is
            // still busy
            if (activePredictions != null) {
                inheritSuggestionsAtRecommenderLevel(aPredictions, aRecommender, activePredictions,
                        aDocument);
            }

            return;
        }

        if (!engine.isReadyForPrediction(context.get())) {
            logRecommenderContextNoReady(aPredictions, aDocument, aRecommender);

            // If possible, we inherit recommendations from a previous run while the recommender is
            // still busy
            if (activePredictions != null) {
                inheritSuggestionsAtRecommenderLevel(aPredictions, aRecommender, activePredictions,
                        aDocument);
            }

            return;
        }

        // If the recommender is not trainable and not sensitive to user input/annotations, we can
        // actually
        // re-use the predictions.
        if (!factory.isInteractive(aRecommender)
                && TRAINING_NOT_SUPPORTED == engine.getTrainingCapability()
                && PREDICTION_USES_TEXT_ONLY == engine.getPredictionCapability()
                && activePredictions != null
                && activePredictions.hasRunPredictionOnDocument(aDocument)) {
            inheritSuggestionsAtRecommenderLevel(aPredictions, engine.getRecommender(),
                    activePredictions, aDocument);
            return;
        }

        try {
            // We lazily load the CAS only at this point because that allows us to skip loading the
            // CAS entirely if there is no enabled layer or recommender. If the CAS cannot be
            // loaded, then we skip to the next document.
            var originalCas = aOriginalCas.get();

            var ctx = new PredictionContext(context.get(), getMonitor());
            cloneAndMonkeyPatchCAS(getProject(), originalCas, predictionCas);
            var predictionRange = new Range(aPredictionBegin < 0 ? 0 : aPredictionBegin,
                    aPredictionEnd < 0 ? originalCas.getDocumentText().length() : aPredictionEnd);
            invokeRecommender(aPredictions, ctx, engine, activePredictions, aDocument, originalCas,
                    predictionCas, predictionRange);
            ctx.getMessages().forEach(aPredictions::log);
        }
        // Catching Throwable is intentional here as we want to continue the execution even if a
        // particular recommender fails.
        catch (Throwable e) {
            logErrorExecutingRecommender(aPredictions, aDocument, aRecommender, e);

            appEventPublisher.publishEvent(RecommenderTaskNotificationEvent
                    .builder(this, getProject(), sessionOwner.getUsername()) //
                    .withMessage(LogMessage.error(this, "Recommender [%s] failed: %s",
                            aRecommender.getName(), e.getMessage())) //
                    .build());

            // If there was a previous successful run of the recommender, inherit its suggestions to
            // avoid that all the suggestions of the recommender simply disappear.
            if (activePredictions != null) {
                inheritSuggestionsAtRecommenderLevel(aPredictions, aRecommender, activePredictions,
                        aDocument);
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
        // skip the prediction if it is not available
        var maybeSuggestionSupport = suggestionSupportRegistry
                .findGenericExtension(SuggestionSupportQuery.of(rec));
        if (maybeSuggestionSupport.isEmpty()) {
            logNoSuggestionSupportAvailable(aIncomingPredictions, rec);
            return;
        }

        // Perform the actual prediction
        var startTime = currentTimeMillis();
        var predictedRange = predict(aIncomingPredictions, aCtx, aEngine, aPredictionCas,
                aPredictionRange);

        // Extract suggestions from the prediction CAS
        var generatedSuggestions = extractSuggestions(aIncomingPredictions, aDocument, aOriginalCas,
                aPredictionCas, rec, maybeSuggestionSupport.get());

        // Reconcile new suggestions with suggestions from previous run
        var reconciliationResult = reconcile(aActivePredictions, aDocument, rec, predictedRange,
                generatedSuggestions, reconciliationOptions.toArray(ReconciliationOption[]::new));

        logGeneratedPredictions(aIncomingPredictions, aDocument, rec, predictedRange,
                generatedSuggestions, reconciliationResult, currentTimeMillis() - startTime);

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
    private void inheritSuggestionsAtRecommenderLevel(Predictions aPredictions,
            Recommender aRecommender, Predictions activePredictions, SourceDocument document)
    {
        var suggestions = activePredictions.getSuggestionsByRecommenderAndDocument(aRecommender,
                document);

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

        var suggestions = aOldPredictions.getPredictionsByDocument(aDocument.getId());

        logPredictionsInherited(aProject, aDocument, suggestions);

        aNewPredictions.inheritSuggestions(suggestions);
        aNewPredictions.markDocumentAsPredictionCompleted(aDocument);
    }

    private int inheritOutOfRangeSuggestions(Predictions aIncomingPredictions,
            Predictions aActivePredictions, SourceDocument aDocument, Recommender aRecommender,
            Range predictedRange, List<AnnotationSuggestion> suggestions, int aged)
    {
        var inheritableSuggestions = aActivePredictions
                .getSuggestionsByRecommenderAndDocument(aRecommender, aDocument).stream() //
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
            Recommender aRecommender, SuggestionSupport aSuggestionSupport)
    {
        var extractionContext = new ExtractionContext(aIncomingPredictions.getGeneration(),
                aRecommender, aDocument, aOriginalCas, aPredictionCas);
        return aSuggestionSupport.extractSuggestions(extractionContext);
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
            Recommender aRecommender, Range aPredictedRange,
            List<AnnotationSuggestion> aNewProtoSuggestions, ReconciliationOption... aOptions)
    {
        if (aActivePredictions == null) {
            return new ReconciliationResult(aNewProtoSuggestions.size(), 0, 0,
                    aNewProtoSuggestions);
        }

        var reconciledSuggestions = new LinkedHashSet<AnnotationSuggestion>();
        var addedSuggestions = new ArrayList<AnnotationSuggestion>();
        var agedSuggestionsCount = 0;

        var predictionsByRecommenderAndDocument = aActivePredictions
                .getSuggestionsByRecommenderAndDocument(aRecommender, aDocument);

        var existingSuggestionsByPosition = predictionsByRecommenderAndDocument.stream() //
                .filter(s -> s.coveredBy(aPredictedRange)) //
                .collect(groupingBy(AnnotationSuggestion::getPosition));

        for (var newSuggestion : aNewProtoSuggestions) {
            var existingSuggestions = existingSuggestionsByPosition
                    .getOrDefault(newSuggestion.getPosition(), emptyList()).stream() //
                    .filter(s -> matchesForReconciliation(newSuggestion, s)) //
                    .limit(2) // First to use, the second to warn that there was more than one
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
                existingSuggestion.reconcileWith(newSuggestion);
                agedSuggestionsCount++;
                reconciledSuggestions.add(existingSuggestion);
            }
        }

        var removedSuggestions = predictionsByRecommenderAndDocument.stream() //
                .filter(s -> s.coveredBy(aPredictedRange)) //
                .filter(s -> !reconciledSuggestions.contains(s)) //
                .toList();

        if (asList(aOptions).contains(KEEP_EXISTING)) {
            for (var s : removedSuggestions) {
                s.incrementAge();
                agedSuggestionsCount++;
                reconciledSuggestions.add(s);
            }
            removedSuggestions = emptyList();
        }

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

    private void logNoNewPredictionsAvailable(Project project, User sessionOwner)
    {
        if (properties.getMessages().isNoNewPredictionsAvailable()) {
            appEventPublisher.publishEvent(RecommenderTaskNotificationEvent
                    .builder(this, project, sessionOwner.getUsername()) //
                    .withMessage(
                            LogMessage.info(this, "Prediction run produced no new suggestions")) //
                    .build());
        }
    }

    private void logNewPredictionsAvailable(Project project, User sessionOwner)
    {
        var event = RecommenderTaskNotificationEvent.builder(this, project,
                sessionOwner.getUsername());

        if (properties.getMessages().isNewPredictionsAvailable()) {
            event.withMessage(LogMessage.info(this,
                    predictions.getNewSuggestionCount() + " new predictions available" //
                            + " (some may be hidden/merged)"));

        }

        // Send the event anyway (even without message) to trigger the refresh button to wriggle
        appEventPublisher.publishEvent(event.build());
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
                "ALL", "--", getMandatorySessionOwner().getUsername(), aDocument, aProject,
                suggestions.size());
    }

    private void logErrorExecutingRecommender(Predictions aPredictions, SourceDocument aDocument,
            Recommender aRecommender, Throwable e)
    {
        aPredictions.log(LogMessage.error(aRecommender.getName(), "Failed: %s", e.getMessage()));
        LOG.error("Error applying recommender {} for user {} to document {} in project {} - " //
                + "skipping recommender", aRecommender, getMandatorySessionOwner(), aDocument,
                aDocument.getProject(), e);
    }

    private void logStartGeneratingPredictions(Predictions aIncomingPredictions,
            Recommender aRecommender)
    {
        aIncomingPredictions.log(LogMessage.info(aRecommender.getName(),
                "Generating predictions for layer [%s]...", aRecommender.getLayer().getUiName()));
        LOG.trace("{}[{}]: Generating predictions for layer [{}]", getMandatorySessionOwner(),
                aRecommender.getName(), aRecommender.getLayer().getUiName());
    }

    private void logUnableToReadAnnotations(Predictions aPredictions, SourceDocument aDocument,
            IOException e)
    {
        aPredictions.log(LogMessage.error(this, "Cannot read annotation CAS... skipping"));
        LOG.error(
                "Cannot read annotation CAS for user {} of document "
                        + "[{}]({}) in project [{}]({}) - skipping document",
                getMandatorySessionOwner(), aDocument.getName(), aDocument.getId(),
                aDocument.getProject().getName(), aDocument.getProject().getId(), e);
    }

    private void logNoActiveRecommenders(Predictions aPredictions)
    {
        aPredictions.log(LogMessage.info(this, "No active recommenders"));
        LOG.trace("[{}]: No active recommenders", getMandatorySessionOwner());
    }

    private void logInheritedPredictions(Predictions aPredictions, Recommender aRecommender,
            SourceDocument document, List<AnnotationSuggestion> suggestions)
    {
        LOG.debug("[{}][{}]: {} on document {} in project {} inherited {} predictions", getId(),
                getMandatorySessionOwner().getUsername(), aRecommender, document,
                aRecommender.getProject(), suggestions.size());
        aPredictions.log(LogMessage.info(aRecommender.getName(),
                "Inherited [%d] predictions from previous run", suggestions.size()));
    }

    private void logNoInheritablePredictions(Predictions aPredictions, Recommender aRecommender,
            SourceDocument document)
    {
        LOG.debug("[{}][{}]: {} on document {} in project {} there " //
                + "are no inheritable predictions", getId(),
                getMandatorySessionOwner().getUsername(), aRecommender, document,
                aRecommender.getProject());
        aPredictions.log(LogMessage.info(aRecommender.getName(),
                "No inheritable suggestions from previous run"));
    }

    private void logPredictionComplete(Predictions aPredictions, long startTime)
    {
        var duration = currentTimeMillis() - startTime;
        LOG.debug("[{}][{}]: Prediction complete ({} ms)", getId(),
                getMandatorySessionOwner().getUsername(), duration);
        aPredictions.log(LogMessage.info(this, "Prediction complete (%d ms).", duration));
    }

    private void logPredictionStartedForOneDocumentWithInheritance(List<SourceDocument> inherit)
    {
        LOG.debug(
                "[{}][{}]: Starting prediction for project [{}] on one document "
                        + "(inheriting [{}]) triggered by [{}]",
                getId(), getMandatorySessionOwner().getUsername(), getProject(), inherit.size(),
                getTrigger());
        info("Starting prediction triggered by [%s]...", getTrigger());
    }

    private void logPredictionStartedForOneDocumentWithoutInheritance()
    {
        LOG.debug(
                "[{}][{}]: Starting prediction for project [{}] on one document "
                        + "triggered by [{}]",
                getId(), getMandatorySessionOwner().getUsername(), getProject(), getTrigger());
        info("Starting prediction triggered by [%s]...", getTrigger());
    }

    private void logPredictionStartedForAllDocuments(List<SourceDocument> docs)
    {
        LOG.debug(
                "[{}][{}]: Starting prediction for project [{}] on [{}] documents triggered by [{}]",
                getId(), getMandatorySessionOwner().getUsername(), getProject(), docs.size(),
                getTrigger());
        info("Starting prediction triggered by [%s]...", getTrigger());
    }

    private void logGeneratedPredictions(Predictions aIncomingPredictions, SourceDocument aDocument,
            Recommender aRecommender, Range predictedRange,
            List<AnnotationSuggestion> generatedSuggestions,
            ReconciliationResult reconciliationResult, long aDuration)
    {
        LOG.debug(
                "{} for user {} on document {} in project {} generated {} predictions within range {} (+{}/-{}/={}) ({} ms)",
                aRecommender, getMandatorySessionOwner(), aDocument, aRecommender.getProject(),
                generatedSuggestions.size(), predictedRange, reconciliationResult.added,
                reconciliationResult.removed, reconciliationResult.aged, aDuration);
        aIncomingPredictions.log(LogMessage.info(aRecommender.getName(), //
                "Generated [%d] predictions within range %s (+%d/-%d/=%d) (%d ms)",
                generatedSuggestions.size(), predictedRange, reconciliationResult.added,
                reconciliationResult.removed, reconciliationResult.aged, aDuration));
    }

    private void logRecommenderContextNoReady(Predictions aPredictions, SourceDocument aDocument,
            Recommender aRecommender)
    {
        aPredictions.log(LogMessage.info(aRecommender.getName(),
                "Recommender context is not ready... skipping"));
        LOG.info("Recommender context {} for user {} in project {} is not ready for " //
                + "prediction - skipping recommender", aRecommender, getMandatorySessionOwner(),
                aDocument.getProject());
    }

    private void logSkippingNotRequestedRecommender(Predictions aPredictions,
            Recommender aRecommender)
    {
        aPredictions.log(LogMessage.info(aRecommender.getName(),
                "Recommender not requested for this run... skipping"));
        LOG.info("[{}][{}]: Recommender not requested for this run " + "- skipping recommender",
                getMandatorySessionOwner().getUsername(), aRecommender.getName());
    }

    private void logSkippingInteractiveRecommenderNotExplicitlyRequested(Predictions aPredictions,
            Recommender aRecommender)
    {
        aPredictions.log(LogMessage.info(aRecommender.getName(),
                "Interactive recommender not requested for this run... skipping"));
        LOG.info(
                "[{}][{}]: Interactive recommender not requested for this run "
                        + "- skipping recommender",
                getMandatorySessionOwner().getUsername(), aRecommender.getName());
    }

    private void logSkippingSynchronous(Predictions aPredictions, Recommender aRecommender)
    {
        aPredictions.log(LogMessage.info(aRecommender.getName(),
                "Synchronous recommenders disabled in this run... skipping"));
        LOG.info(
                "[{}][{}]: Synchronous recommenders disabled in this run "
                        + "- skipping recommender",
                getMandatorySessionOwner().getUsername(), aRecommender.getName());
    }

    private void logSkippingAsynchronous(Predictions aPredictions, Recommender aRecommender)
    {
        aPredictions.log(LogMessage.info(aRecommender.getName(),
                "Asynchronous recommenders disabled in this run... skipping"));
        LOG.info(
                "[{}][{}]: Asynchronous recommenders disabled in this run "
                        + "- skipping recommender",
                getMandatorySessionOwner().getUsername(), aRecommender.getName());
    }

    private void logInvalidRecommenderConfiguration(Predictions aPredictions,
            Recommender aRecommender)
    {
        aPredictions.log(LogMessage.info(aRecommender.getName(),
                "Recommender configured with invalid layer or feature... skipping"));
        LOG.info(
                "[{}][{}]: Recommender configured with invalid layer or feature "
                        + "- skipping recommender",
                getMandatorySessionOwner().getUsername(), aRecommender.getName());
    }

    private void logNoRecommenderFactory(Recommender aRecommender)
    {
        LOG.warn("[{}][{}]: No factory found - skipping recommender",
                getMandatorySessionOwner().getUsername(), aRecommender.getName());
    }

    private void logRecommenderHasNoContext(Predictions aPredictions, SourceDocument aDocument,
            Recommender aRecommender)
    {
        aPredictions.log(
                LogMessage.info(aRecommender.getName(), "Recommender has no context... skipping"));
        LOG.info("No context available for recommender {} for user {} on document {} in " //
                + "project {} - skipping recommender", aRecommender, getMandatorySessionOwner(),
                aDocument, aDocument.getProject());
    }

    private void logRecommenderDisabled(Predictions aPredictions, Recommender aRecommender)
    {
        aPredictions
                .log(LogMessage.info(aRecommender.getName(), "Recommender disabled... skipping"));
        LOG.debug("{}[{}]: Disabled - skipping", getMandatorySessionOwner(),
                aRecommender.getName());
    }

    private void logRecommenderNotAvailable(Predictions aPredictions, Recommender aRecommender)
    {
        aPredictions.log(LogMessage.info(aRecommender.getName(),
                "Recommender no longer available... skipping"));
        LOG.info("{}[{}]: Recommender no longer available... skipping", getMandatorySessionOwner(),
                aRecommender.getName());
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
                originalCas = documentService.readAnnotationCas(document,
                        AnnotationSet.forUser(dataOwner), AUTO_CAS_UPGRADE,
                        SHARED_READ_ONLY_ACCESS);
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
            CasStorageSession.get().add(AnnotationSet.PREDICTION_SET, EXCLUSIVE_WRITE_ACCESS, cas);
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

    public static enum ReconciliationOption
    {
        KEEP_EXISTING
    }

    public static Builder<Builder<?>> builder()
    {
        return new Builder<>();
    }

    public static class Builder<T extends Builder<?>>
        extends RecommendationTask_ImplBase.Builder<T>
    {
        private final List<Recommender> recommenders = new ArrayList<>();
        private SourceDocument currentDocument;
        private String dataOwner;
        private int predictionBegin = -1;
        private int predictionEnd = -1;
        private boolean isolated = false;
        private boolean asynchronousRecommenders = true;
        private boolean synchronousRecommenders = true;
        private Set<ReconciliationOption> reconciliationOptions = new HashSet<>();

        /**
         * Generate predictions only for the specified recommender. If this is not set, then
         * predictions will be run for all active recommenders.
         * 
         * @param aRecommenders
         *            the recommenders to run.
         */
        @SuppressWarnings({ "unchecked", "javadoc" })
        public T withRecommender(Recommender... aRecommenders)
        {
            if (aRecommenders != null) {
                recommenders.addAll(asList(aRecommenders));
            }
            return (T) this;
        }

        /**
         * @param aCurrentDocuemnt
         *            the document currently open in the editor of the user triggering the task.
         */
        @SuppressWarnings({ "unchecked", "javadoc" })
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
        @SuppressWarnings({ "unchecked", "javadoc" })
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
        @SuppressWarnings({ "unchecked", "javadoc" })
        public T withIsolated(boolean aIsolated)
        {
            isolated = aIsolated;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T withAsynchronousRecommenders(boolean aFlag)
        {
            asynchronousRecommenders = aFlag;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T withSynchronousRecommenders(boolean aFlag)
        {
            synchronousRecommenders = aFlag;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T withReconciliationOptions(ReconciliationOption... aOptions)
        {
            if (aOptions != null) {
                reconciliationOptions.addAll(asList(aOptions));
            }
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

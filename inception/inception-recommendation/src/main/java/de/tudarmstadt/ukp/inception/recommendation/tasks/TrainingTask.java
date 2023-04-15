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

import static de.tudarmstadt.ukp.clarin.webanno.api.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.inception.recommendation.api.recommender.TrainingCapability.TRAINING_NOT_SUPPORTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.recommender.TrainingCapability.TRAINING_REQUIRED;
import static java.lang.System.currentTimeMillis;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.NoResultException;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.util.CasUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageSession;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.EvaluatedRecommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommenderTaskNotificationEvent;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

public class TrainingTask
    extends RecommendationTask_ImplBase
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private @Autowired AnnotationSchemaService annoService;
    private @Autowired DocumentService documentService;
    private @Autowired RecommendationService recommenderService;
    private @Autowired SchedulingService schedulingService;
    private @Autowired ApplicationEventPublisher appEventPublisher;

    private final SourceDocument currentDocument;

    public TrainingTask(User aUser, Project aProject, String aTrigger,
            SourceDocument aCurrentDocument)
    {
        super(aUser, aProject, aTrigger);

        if (getUser().isEmpty()) {
            throw new IllegalArgumentException("TrainingTask requires a user");
        }

        currentDocument = aCurrentDocument;
    }

    @Override
    public void execute()
    {
        try (CasStorageSession session = CasStorageSession.open()) {
            executeTraining();
        }
    }

    private void executeTraining()
    {
        long overallStartTime = currentTimeMillis();
        User user = getUser().orElseThrow();

        logTrainingOverallStart(user);

        // Read the CASes only when they are accessed the first time. This allows us to skip
        // reading the CASes in case that no layer / recommender is available or if no
        // recommender requires evaluation.
        var casLoader = new LazyInitializer<List<TrainingDocument>>()
        {
            @Override
            protected List<TrainingDocument> initialize()
            {
                return readCasses(getProject(), user);
            }
        };

        boolean seenSuccessfulTraining = false;
        boolean seenNonTrainingRecommender = false;

        for (var layer : annoService.listAnnotationLayer(getProject())) {
            if (!layer.isEnabled()) {
                continue;
            }

            var evaluatedRecommenders = recommenderService.getActiveRecommenders(user, layer);

            if (evaluatedRecommenders.isEmpty()) {
                logNoActiveRecommenders(user, layer);
                continue;
            }

            for (var evaluatedRecommender : evaluatedRecommenders) {
                // Make sure we have the latest recommender config from the DB - the one from
                // the active recommenders list may be outdated
                Recommender recommender;
                try {
                    recommender = recommenderService
                            .getRecommender(evaluatedRecommender.getRecommender().getId());
                }
                catch (NoResultException e) {
                    logRecommenderGone(user, evaluatedRecommender);
                    continue;
                }

                if (!recommender.isEnabled()) {
                    logRecommenderDisabled(user, evaluatedRecommender);
                    continue;
                }

                long startTime = currentTimeMillis();

                try {
                    var maybeFactory = recommenderService.getRecommenderFactory(recommender);
                    if (maybeFactory.isEmpty()) {
                        logUnsupportedRecommenderType(user, evaluatedRecommender);
                        continue;
                    }

                    var factory = maybeFactory.get();
                    if (!factory.accepts(recommender.getLayer(), recommender.getFeature())) {
                        logInvalidRecommenderConfiguration(user, evaluatedRecommender, recommender);
                        continue;
                    }

                    var engine = factory.build(recommender);
                    var ctx = engine.newContext(recommenderService.getContext(user, recommender)
                            .orElse(RecommenderContext.EMPTY_CONTEXT));
                    ctx.setUser(user);

                    // If engine does not support training, mark engine ready and skip to
                    // prediction
                    if (engine.getTrainingCapability() == TRAINING_NOT_SUPPORTED) {
                        seenNonTrainingRecommender = true;
                        logTrainingNotSupported(user, recommender);
                        commitContext(user, recommender, ctx);
                        continue;
                    }

                    var trainingCasses = casLoader.get().stream() //
                            .filter(e -> !recommender.getStatesIgnoredForTraining()
                                    .contains(e.state)) //
                            .map(e -> e.getCas()) //
                            .filter(Objects::nonNull)
                            .filter(cas -> containsTargetTypeAndFeature(recommender, cas)) //
                            .collect(toList());

                    // If no data for training is available, but the engine requires training,
                    // do not mark as ready
                    if (trainingCasses.isEmpty()
                            && engine.getTrainingCapability() == TRAINING_REQUIRED) {
                        logNoDataAvailableForTraining(user, layer, recommender);
                        // This can happen if there were already predictions based on existing
                        // annotations, but all annotations have been removed/deleted. To ensure
                        // that the prediction run removes the stale predictions, we need to
                        // call it a success here.
                        seenSuccessfulTraining = true;
                        continue;
                    }

                    logTrainingRecommenderStart(user, casLoader, layer, recommender,
                            trainingCasses);

                    engine.train(ctx, trainingCasses);
                    inheritLog(ctx.getMessages());

                    long duration = currentTimeMillis() - startTime;

                    if (!engine.isReadyForPrediction(ctx)) {
                        logTrainingFailure(user, recommender, duration, casLoader.get(),
                                trainingCasses);
                        continue;
                    }

                    logTrainingSuccessful(user, casLoader, recommender, trainingCasses, duration);
                    seenSuccessfulTraining = true;

                    commitContext(user, recommender, ctx);
                }
                // Catching Throwable is intentional here as we want to continue the execution
                // even if a particular recommender fails.
                catch (Throwable e) {
                    handleError(user, recommender, startTime, e);
                }
            }
        }

        if (!seenSuccessfulTraining && !seenNonTrainingRecommender) {
            logNothingWasTrained(user);
            return;
        }

        logTrainingOverallEnd(overallStartTime, user);

        schedulePredictionTask(user);
    }

    private void schedulePredictionTask(User user)
    {
        var predictionTask = new PredictionTask(user,
                String.format("TrainingTask %s complete", getId()), currentDocument);
        predictionTask.inheritLog(this);
        schedulingService.enqueue(predictionTask);
    }

    private void logTrainingOverallEnd(long overallStartTime, User user)
    {
        info("Training complete ({} ms).", currentTimeMillis() - overallStartTime);
    }

    private void commitContext(User user, Recommender recommender, RecommenderContext ctx)
    {
        ctx.close();
        recommenderService.putContext(user, recommender, ctx);
    }

    private List<TrainingDocument> readCasses(Project aProject, User aUser)
    {
        var casses = new ArrayList<TrainingDocument>();
        var allDocuments = documentService.listAllDocuments(aProject, aUser);
        for (var entry : allDocuments.entrySet()) {
            var sourceDocument = entry.getKey();
            var annotationDocument = entry.getValue();
            var state = annotationDocument != null ? annotationDocument.getState()
                    : AnnotationDocumentState.NEW;

            casses.add(new TrainingDocument(sourceDocument, aUser.getUsername(), state));
        }
        return casses;
    }

    private boolean containsTargetTypeAndFeature(Recommender aRecommender, CAS aCas)
    {
        Type type;
        try {
            type = CasUtil.getType(aCas, aRecommender.getLayer().getName());
        }
        catch (IllegalArgumentException e) {
            // If the CAS does not contain the target type at all, then it cannot contain any
            // annotations of that type.
            return false;
        }

        if (type.getFeatureByBaseName(aRecommender.getFeature().getName()) == null) {
            // If the CAS does not contain the target feature, then there won't be any training
            // data.
            return false;
        }

        return CasUtil.iterator(aCas, type).hasNext();
    }

    private void logUnsupportedRecommenderType(User user, EvaluatedRecommender evaluatedRecommender)
    {
        log.warn("[{}][{}]: No factory found - skipping recommender", user.getUsername(),
                evaluatedRecommender.getRecommender().getName());
    }

    private void logTrainingNotSupported(User user, Recommender recommender)
    {
        log.debug("[{}][{}][{}]: Engine does not support training", getId(), user.getUsername(),
                recommender.getName());
    }

    private void logRecommenderDisabled(User user, EvaluatedRecommender evaluatedRecommender)
    {
        log.debug("[{}][{}][{}]: Disabled - skipping", user.getUsername(), getId(),
                evaluatedRecommender.getRecommender().getName());
    }

    private void logRecommenderGone(User user, EvaluatedRecommender evaluatedRecommender)
    {
        log.debug("[{}][{}][{}]: Recommender no longer available... skipping", getId(),
                user.getUsername(), evaluatedRecommender.getRecommender().getName());
    }

    private void logNothingWasTrained(User user)
    {
        log.debug("[{}][{}]: No recommenders trained successfully and no non-training "
                + "recommenders, skipping prediction.", getId(), user.getUsername());
    }

    private void logNoActiveRecommenders(User user, AnnotationLayer layer)
    {
        log.trace("[{}][{}][{}]: No active recommenders, skipping training.", getId(),
                user.getUsername(), layer.getUiName());
        info("No active recommenders for layer [%s], skipping training.", layer.getUiName());
    }

    private void logInvalidRecommenderConfiguration(User user, EvaluatedRecommender r,
            Recommender recommender)
    {
        log.debug(
                "[{}][{}][{}]: Recommender configured with invalid layer or "
                        + "feature - skipping recommender",
                getId(), user.getUsername(), r.getRecommender().getName());
        error("Recommender [%s] configured with invalid layer or feature - skipping recommender.",
                r.getRecommender().getName());
        appEventPublisher.publishEvent(
                RecommenderTaskNotificationEvent.builder(this, getProject(), user.getUsername()) //
                        .withMessage(LogMessage.error(this,
                                "Recommender [%s] configured with invalid layer or "
                                        + "feature - skipping training recommender.",
                                recommender.getName()))
                        .build());
    }

    private void logNoDataAvailableForTraining(User user, AnnotationLayer layer,
            Recommender recommender)
    {
        log.debug("[{}][{}][{}]: There are no annotations available to train on", getId(),
                user.getUsername(), recommender.getName());
        warn("There are no [%s] annotations available to train on.", layer.getUiName());
    }

    private void logTrainingFailure(User user, Recommender recommender, long duration,
            List<TrainingDocument> aAllCasses, List<CAS> aTrainCasses)
    {
        int docNum = aAllCasses.size();
        int trainDocNum = aTrainCasses.size();

        log.debug("[{}][{}][{}]: Training on [{}] out of [{}] documents not successful ({} ms)",
                getId(), user.getUsername(), recommender.getName(), trainDocNum, docNum, duration);
        info("Training not successful (%d ms).", duration);
        // The recommender may decide for legitimate reasons not to train and
        // then this event is annoying
        // appEventPublisher.publishEvent(new RecommenderTaskEvent(this,
        // user.getUsername(),
        // format("Training on %d out of %d documents not successful (%d ms)",
        // trainDocNum, docNum, duration),
        // recommender));
    }

    private void logTrainingSuccessful(User user, LazyInitializer<List<TrainingDocument>> casses,
            Recommender recommender, List<CAS> cassesForTraining, long duration)
        throws ConcurrentException
    {
        log.debug("[{}][{}][{}]: Training successful on [{}] out of [{}] documents ({} ms)",
                getId(), user.getUsername(), recommender.getName(), cassesForTraining.size(),
                casses.get().size(), duration);
        info("Training successful on [%d] out of [%d] documents (%d ms)", cassesForTraining.size(),
                casses.get().size(), duration);
    }

    private void logTrainingOverallStart(User user)
    {
        log.debug("[{}][{}]: Starting training for project {} triggered by [{}]...", getId(),
                user.getUsername(), getProject(), getTrigger());
        info("Starting training triggered by [%s]...", getTrigger());
    }

    private void logTrainingRecommenderStart(User user,
            LazyInitializer<List<TrainingDocument>> casses, AnnotationLayer layer,
            Recommender recommender, List<CAS> cassesForTraining)
        throws ConcurrentException
    {
        log.debug("[{}][{}][{}]: Training model on [{}] out of [{}] documents ...", getId(),
                user.getUsername(), recommender.getName(), cassesForTraining.size(),
                casses.get().size());
        info("Training model for [%s] on [%d] out of [%d] documents ...", layer.getUiName(),
                cassesForTraining.size(), casses.get().size());
    }

    private void handleError(User user, Recommender recommender, long startTime, Throwable e)
    {
        long duration = currentTimeMillis() - startTime;
        log.error("[{}][{}][{}]: Training failed ({} ms)", getId(), user.getUsername(),
                recommender.getName(), (currentTimeMillis() - startTime), e);

        error("Training failed (%d ms): %s", duration, getRootCauseMessage(e));

        appEventPublisher.publishEvent(
                RecommenderTaskNotificationEvent.builder(this, getProject(), user.getUsername()) //
                        .withMessage(LogMessage.error(this, "Training failed (%d ms) with %s",
                                duration, e.getMessage()))
                        .build());
    }

    private class TrainingDocument
    {
        private final SourceDocument document;
        private final String user;
        private final AnnotationDocumentState state;

        private boolean attemptedLoading = false;
        private CAS _cas;

        private TrainingDocument(SourceDocument aDocument, String aUser,
                AnnotationDocumentState aState)
        {
            document = aDocument;
            user = aUser;
            state = aState;
        }

        public CAS getCas()
        {
            if (attemptedLoading) {
                return _cas;
            }

            attemptedLoading = true;
            try {
                // During training, we should not have to modify the CASes... right? Fingers
                // crossed.
                _cas = documentService.readAnnotationCas(document, user, AUTO_CAS_UPGRADE,
                        SHARED_READ_ONLY_ACCESS);
            }
            catch (IOException e) {
                log.error("Unable to load CAS to train recommender", e);
            }

            return _cas;
        }
    }
}

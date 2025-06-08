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

import static de.tudarmstadt.ukp.inception.recommendation.api.recommender.TrainingCapability.TRAINING_NOT_SUPPORTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.recommender.TrainingCapability.TRAINING_REQUIRED;
import static java.lang.System.currentTimeMillis;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.EvaluatedRecommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommenderTaskNotificationEvent;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.support.WebAnnoConst;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import jakarta.persistence.NoResultException;

public class TrainingTask
    extends RecommendationTask_ImplBase
{
    public static final String TYPE = "TrainingTask";

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @Autowired DocumentService documentService;
    private @Autowired RecommendationService recommenderService;
    private @Autowired SchedulingService schedulingService;
    private @Autowired ApplicationEventPublisher appEventPublisher;

    private final SourceDocument currentDocument;
    private final String dataOwner;

    private boolean seenSuccessfulTraining = false;
    private boolean seenNonTrainingRecommender = false;

    public TrainingTask(Builder<? extends Builder<?>> aBuilder)
    {
        super(aBuilder.withType(TYPE));

        currentDocument = aBuilder.currentDocument;
        dataOwner = aBuilder.dataOwner;
    }

    @Override
    public String getTitle()
    {
        return "Training recommenders...";
    }

    @Override
    public void execute()
    {
        try (var session = CasStorageSession.open()) {
            executeTraining();
        }

        schedulePredictionTask();
    }

    private User getMandatorySessionOwner()
    {
        return getSessionOwner().orElseThrow();
    }

    private void executeTraining()
    {
        var activeRecommenders = recommenderService
                .getActiveRecommenders(getMandatorySessionOwner(), getProject());

        if (activeRecommenders.isEmpty()) {
            logNoActiveRecommenders();
            return;
        }

        var overallStartTime = currentTimeMillis();

        logTrainingOverallStart();

        // Read the CASes only when they are accessed the first time. This allows us to skip
        // reading the CASes in case that no layer / recommender is available or if no
        // recommender requires evaluation.
        var casLoader = new LazyCasLoader(documentService, getProject(), dataOwner);

        try (var progress = getMonitor().openScope("recommenders", activeRecommenders.size())) {
            for (var activeRecommender : activeRecommenders) {
                progress.update(up -> up.increment() //
                        .addMessage(LogMessage.info(this, "%s",
                                activeRecommender.getRecommender().getName())));

                // Make sure we have the latest recommender config from the DB - the one from
                // the active recommenders list may be outdated
                Recommender recommender;
                try {
                    recommender = recommenderService
                            .getRecommender(activeRecommender.getRecommender().getId());
                }
                catch (NoResultException e) {
                    logRecommenderGone(activeRecommender);
                    continue;
                }

                if (!recommender.isEnabled()) {
                    logRecommenderDisabled(recommender);
                    continue;
                }

                if (!recommender.getLayer().isEnabled()) {
                    logLayerDisabled(recommender);
                    continue;
                }

                if (!recommender.getFeature().isEnabled()) {
                    logFeatureDisabled(recommender);
                    continue;
                }

                try {
                    trainRecommender(recommender, casLoader);
                }
                // Catching Throwable is intentional here as we want to continue the execution
                // even if a particular recommender fails.
                catch (Throwable e) {
                    handleError(recommender, e);
                }
            }
        }

        if (!seenSuccessfulTraining && !seenNonTrainingRecommender) {
            logNothingWasTrained();
            return;
        }

        logTrainingOverallEnd(overallStartTime);
    }

    private void trainRecommender(Recommender aRecommender, LazyCasLoader casLoader)
        throws ConcurrentException, RecommendationException
    {
        var startTime = currentTimeMillis();
        var sessionOwner = getMandatorySessionOwner();

        var maybeFactory = recommenderService.getRecommenderFactory(aRecommender);
        if (maybeFactory.isEmpty()) {
            logUnsupportedRecommenderType(aRecommender);
            return;
        }

        var factory = maybeFactory.get();
        if (!factory.accepts(aRecommender)) {
            logInvalidRecommenderConfiguration(aRecommender);
            return;
        }

        var engine = factory.build(aRecommender);
        var ctx = engine
                .newContext(recommenderService.getContext(sessionOwner.getUsername(), aRecommender)
                        .orElse(RecommenderContext.emptyContext()));
        ctx.setUser(sessionOwner);

        // If engine does not support training, mark engine ready and skip to
        // prediction
        if (engine.getTrainingCapability() == TRAINING_NOT_SUPPORTED) {
            seenNonTrainingRecommender = true;
            logTrainingNotSupported(aRecommender);
            commitContext(sessionOwner, aRecommender, ctx);
            return;
        }

        var trainingCasses = casLoader.getRelevantCasses(aRecommender);

        // If no data for training is available, but the engine requires training,
        // do not mark as ready
        if (trainingCasses.isEmpty() && engine.getTrainingCapability() == TRAINING_REQUIRED) {
            logNoDataAvailableForTraining(aRecommender);
            // This can happen if there were already predictions based on existing
            // annotations, but all annotations have been removed/deleted. To ensure
            // that the prediction run removes the stale predictions, we need to
            // call it a success here.
            seenSuccessfulTraining = true;
            return;
        }

        logTrainingRecommenderStart(casLoader, aRecommender, trainingCasses);

        engine.train(ctx, trainingCasses);
        inheritLog(ctx.getMessages());

        var duration = currentTimeMillis() - startTime;

        if (!engine.isReadyForPrediction(ctx)) {
            logTrainingFailure(aRecommender, duration, casLoader, trainingCasses);
            return;
        }

        logTrainingSuccessful(casLoader, aRecommender, trainingCasses, duration);
        seenSuccessfulTraining = true;

        commitContext(sessionOwner, aRecommender, ctx);
    }

    private void schedulePredictionTask()
    {
        var predictionTask = PredictionTask.builder() //
                .withSessionOwner(getMandatorySessionOwner()) //
                .withTrigger(String.format("TrainingTask %s complete", getId())) //
                .withCurrentDocument(currentDocument) //
                .withDataOwner(dataOwner) //
                .withSynchronousRecommenders(false) // ;
                .build();

        predictionTask.inheritLog(this);

        schedulingService.enqueue(predictionTask);
    }

    private void commitContext(User aSessionOwner, Recommender recommender, RecommenderContext ctx)
    {
        ctx.close();
        recommenderService.putContext(aSessionOwner, recommender, ctx);
    }

    private void logTrainingOverallEnd(long overallStartTime)
    {
        info("Training complete (%d ms).", currentTimeMillis() - overallStartTime);
    }

    private void logUnsupportedRecommenderType(Recommender aRecommender)
    {
        warn("Recommender [%s] uses unsupported tool [%s] - skipping", aRecommender.getName(),
                aRecommender.getTool());
        LOG.warn("[{}][{}]: No factory found - skipping recommender",
                getMandatorySessionOwner().getUsername(), aRecommender.getName());
    }

    private void logTrainingNotSupported(Recommender aRecommender)
    {
        LOG.debug("[{}][{}][{}]: Engine does not support training", getId(),
                getMandatorySessionOwner().getUsername(), aRecommender.getName());
    }

    private void logRecommenderDisabled(Recommender aRecommender)
    {
        LOG.debug("[{}][{}][{}]: Recommender disabled - skipping",
                getMandatorySessionOwner().getUsername(), getId(), aRecommender.getName());
    }

    private void logLayerDisabled(Recommender aRecommender)
    {
        warn("Recommender [%s] uses disabled layer [%s] disabled - skipping recommender.",
                aRecommender.getName(), aRecommender.getLayer().getUiName());
        LOG.debug("[{}][{}][{}]: Layer disabled - skipping",
                getMandatorySessionOwner().getUsername(), getId(),
                aRecommender.getLayer().getUiName());
    }

    private void logFeatureDisabled(Recommender aRecommender)
    {
        warn("Recommender [%s] uses disabled feature [%s] - skipping recommender.",
                aRecommender.getName(), aRecommender.getFeature().getUiName());
        LOG.debug("[{}][{}][{}]: Feature disabled - skipping",
                getMandatorySessionOwner().getUsername(), getId(),
                aRecommender.getFeature().getUiName());
    }

    private void logRecommenderGone(EvaluatedRecommender evaluatedRecommender)
    {
        LOG.debug("[{}][{}][{}]: Recommender no longer available... skipping", getId(),
                getMandatorySessionOwner().getUsername(),
                evaluatedRecommender.getRecommender().getName());
    }

    private void logNothingWasTrained()
    {
        LOG.debug(
                "[{}][{}]: No recommenders trained successfully and no non-training "
                        + "recommenders, skipping prediction.",
                getId(), getMandatorySessionOwner().getUsername());
    }

    private void logNoActiveRecommenders()
    {
        LOG.trace("[{}][{}]: No active recommenders, skipping training.", getId(),
                getMandatorySessionOwner().getUsername());

        info("No active recommenders, skipping training.");
    }

    private void logInvalidRecommenderConfiguration(Recommender recommender)
    {
        LOG.debug(
                "[{}][{}][{}]: Recommender configured with invalid layer or "
                        + "feature - skipping recommender",
                getId(), getMandatorySessionOwner().getUsername(), recommender.getName());

        error("Recommender [%s] configured with invalid layer or feature - skipping recommender.",
                recommender.getName());

        appEventPublisher.publishEvent(RecommenderTaskNotificationEvent
                .builder(this, getProject(), getMandatorySessionOwner().getUsername()) //
                .withMessage(LogMessage.error(this,
                        "Recommender [%s] configured with invalid layer or "
                                + "feature - skipping training recommender.",
                        recommender.getName()))
                .build());
    }

    private void logNoDataAvailableForTraining(Recommender recommender)
    {
        LOG.debug("[{}][{}][{}]: There are no annotations available to train on", getId(),
                getMandatorySessionOwner().getUsername(), recommender.getName());

        warn("There are no [%s] annotations available to train on.",
                recommender.getLayer().getUiName());
    }

    private void logTrainingFailure(Recommender recommender, long duration, LazyCasLoader aLoader,
            List<CAS> aTrainCasses)
    {
        int docNum = aLoader.size();
        int trainDocNum = aTrainCasses.size();

        LOG.debug("[{}][{}][{}]: Training on [{}] out of [{}] documents not successful ({} ms)",
                getId(), getMandatorySessionOwner().getUsername(), recommender.getName(),
                trainDocNum, docNum, duration);

        info("Training not successful (%d ms).", duration);

        // The recommender may decide for legitimate reasons not to train and
        // then this event is annoying
        // appEventPublisher.publishEvent(new RecommenderTaskEvent(this,
        // user.getUsername(),
        // format("Training on %d out of %d documents not successful (%d ms)",
        // trainDocNum, docNum, duration),
        // recommender));
    }

    private void logTrainingSuccessful(LazyCasLoader casses, Recommender recommender,
            List<CAS> cassesForTraining, long duration)
        throws ConcurrentException
    {
        LOG.debug("[{}][{}][{}]: Training successful on [{}] out of [{}] documents ({} ms)",
                getId(), getMandatorySessionOwner().getUsername(), recommender.getName(),
                cassesForTraining.size(), casses.size(), duration);

        log(LogMessage.info(recommender.getName(),
                "Training successful on [%d] out of [%d] documents (%d ms)",
                cassesForTraining.size(), casses.size(), duration));
    }

    private void logTrainingOverallStart()
    {
        LOG.debug(
                "[{}][{}]: Starting training for project {} on data from [{}] triggered by [{}]...",
                getId(), getMandatorySessionOwner().getUsername(), getProject(), dataOwner,
                getTrigger());
        info("Starting training on data from [%s] triggered by [%s]...", dataOwner, getTrigger());
    }

    private void logTrainingRecommenderStart(LazyCasLoader aLoader, Recommender recommender,
            List<CAS> cassesForTraining)
        throws ConcurrentException
    {
        getMonitor()
                .update(up -> up.addMessage(LogMessage.info(this, "%s", recommender.getName())));

        LOG.debug("[{}][{}][{}]: Training model on [{}] out of [{}] documents ...", getId(),
                getMandatorySessionOwner().getUsername(), recommender.getName(),
                cassesForTraining.size(), aLoader.size());

        log(LogMessage.info(recommender.getName(),
                "Training model for [%s] on [%d] out of [%d] documents ...",
                recommender.getLayer().getUiName(), cassesForTraining.size(), aLoader.size()));
    }

    private void handleError(Recommender recommender, Throwable e)
    {
        LOG.error("[{}][{}][{}]: Training failed", getId(),
                getMandatorySessionOwner().getUsername(), recommender.getName(), e);

        log(LogMessage.error(recommender.getName(), "Training failed: %s", getRootCauseMessage(e)));

        appEventPublisher.publishEvent(RecommenderTaskNotificationEvent
                .builder(this, getProject(), getMandatorySessionOwner().getUsername()) //
                .withMessage(LogMessage.error(this, "Training failed with %s", e.getMessage()))
                .build());
    }

    public static Builder<Builder<?>> builder()
    {
        return new Builder<>();
    }

    public static class Builder<T extends Builder<?>>
        extends RecommendationTask_ImplBase.Builder<T>
    {
        private SourceDocument currentDocument;
        private String dataOwner;

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

        public TrainingTask build()
        {
            Validate.notNull(sessionOwner, "TrainingTask requires a user");

            return new TrainingTask(this);
        }
    }
}

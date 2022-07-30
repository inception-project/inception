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
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.NoResultException;

import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.util.CasUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageSession;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.EvaluatedRecommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.TrainingCapability;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommenderTaskEvent;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

/**
 * This consumer trains a new classifier model, if a classification tool was selected before.
 */
public class TrainingTask
    extends RecommendationTask_ImplBase
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private @Autowired AnnotationSchemaService annoService;
    private @Autowired DocumentService documentService;
    private @Autowired RecommendationService recommendationService;
    private @Autowired SchedulingService schedulingService;
    private @Autowired ApplicationEventPublisher appEventPublisher;

    private final SourceDocument currentDocument;

    public TrainingTask(User aUser, Project aProject, String aTrigger,
            SourceDocument aCurrentDocument)
    {
        super(aUser, aProject, aTrigger);
        currentDocument = aCurrentDocument;
    }

    @Override
    public void execute()
    {
        try (CasStorageSession session = CasStorageSession.open()) {
            Project project = getProject();
            User user = getUser().orElseThrow();

            log.debug("[{}][{}]: Starting training for project {} triggered by [{}]...", getId(),
                    user.getUsername(), project, getTrigger());
            info("Starting training triggered by [%s]...", getTrigger());

            // Read the CASes only when they are accessed the first time. This allows us to skip
            // reading the CASes in case that no layer / recommender is available or if no
            // recommender requires evaluation.
            LazyInitializer<List<TrainingDocument>> casses = new LazyInitializer<List<TrainingDocument>>()
            {
                @Override
                protected List<TrainingDocument> initialize()
                {
                    return readCasses(project, user);
                }
            };

            boolean seenSuccessfulTraining = false;
            boolean seenNonTrainingRecommender = false;

            for (AnnotationLayer layer : annoService.listAnnotationLayer(project)) {
                if (!layer.isEnabled()) {
                    continue;
                }

                List<EvaluatedRecommender> recommenders = recommendationService
                        .getActiveRecommenders(user, layer);

                if (recommenders.isEmpty()) {
                    log.trace("[{}][{}][{}]: No active recommenders, skipping training.", getId(),
                            user.getUsername(), layer.getUiName());
                    info("No active recommenders for layer [%s], skipping training.",
                            layer.getUiName());
                    continue;
                }

                for (EvaluatedRecommender r : recommenders) {
                    // Make sure we have the latest recommender config from the DB - the one from
                    // the active recommenders list may be outdated
                    Recommender recommender;
                    try {
                        recommender = recommendationService
                                .getRecommender(r.getRecommender().getId());
                    }
                    catch (NoResultException e) {
                        log.debug("[{}][{}][{}]: Recommender no longer available... skipping",
                                getId(), user.getUsername(), r.getRecommender().getName());
                        continue;
                    }

                    if (!recommender.isEnabled()) {
                        log.debug("[{}][{}][{}]: Disabled - skipping", user.getUsername(), getId(),
                                r.getRecommender().getName());
                        continue;
                    }

                    long startTime = System.currentTimeMillis();

                    try {
                        Optional<RecommendationEngineFactory<?>> maybeFactory = recommendationService
                                .getRecommenderFactory(recommender);

                        if (maybeFactory.isEmpty()) {
                            log.warn("[{}][{}]: No factory found - skipping recommender",
                                    user.getUsername(), r.getRecommender().getName());
                            continue;
                        }

                        RecommendationEngineFactory<?> factory = maybeFactory.get();

                        if (!factory.accepts(recommender.getLayer(), recommender.getFeature())) {
                            log.debug(
                                    "[{}][{}][{}]: Recommender configured with invalid layer or "
                                            + "feature - skipping recommender",
                                    getId(), user.getUsername(), r.getRecommender().getName());
                            error("Recommender [%s] configured with invalid layer or feature - skipping recommender.",
                                    r.getRecommender().getName());
                            appEventPublisher.publishEvent(new RecommenderTaskEvent(this,
                                    user.getUsername(),
                                    "Recommender configured with invalid layer or feature - skipping training recommender.",
                                    recommender));
                            continue;
                        }

                        RecommendationEngine recommendationEngine = factory.build(recommender);

                        RecommenderContext ctx = recommendationEngine
                                .newContext(recommendationService.getContext(user, recommender)
                                        .orElse(RecommenderContext.EMPTY_CONTEXT));
                        ctx.setUser(user);

                        TrainingCapability capability = recommendationEngine
                                .getTrainingCapability();

                        // If engine does not support training, mark engine ready and skip to
                        // prediction
                        if (capability == TRAINING_NOT_SUPPORTED) {
                            seenNonTrainingRecommender = true;
                            log.debug("[{}][{}][{}]: Engine does not support training", getId(),
                                    user.getUsername(), recommender.getName());
                            ctx.close();
                            recommendationService.putContext(user, recommender, ctx);
                            continue;
                        }

                        List<CAS> cassesForTraining = casses.get().stream() //
                                .filter(e -> !recommender.getStatesIgnoredForTraining()
                                        .contains(e.state))
                                .filter(e -> containsTargetTypeAndFeature(recommender, e.cas))
                                .map(e -> e.cas).collect(toList());

                        // If no data for training is available, but the engine requires training,
                        // do not mark as ready
                        if (cassesForTraining.isEmpty() && capability == TRAINING_REQUIRED) {
                            log.debug(
                                    "[{}][{}][{}]: There are no annotations available to train on",
                                    getId(), user.getUsername(), recommender.getName());
                            warn("There are no [%s] annotations available to train on.",
                                    layer.getUiName());
                            // This can happen if there were already predictions based on existing
                            // annotations, but all annotations have been removed/deleted. To ensure
                            // that the prediction run removes the stale predictions, we need to
                            // call it a success here.
                            seenSuccessfulTraining = true;
                            continue;
                        }

                        log.debug("[{}][{}][{}]: Training model on [{}] out of [{}] documents ...",
                                getId(), user.getUsername(), recommender.getName(),
                                cassesForTraining.size(), casses.get().size());
                        info("Training model for [%s] on [%d] out of [%d] documents ...",
                                layer.getUiName(), cassesForTraining.size(), casses.get().size());

                        recommendationEngine.train(ctx, cassesForTraining);
                        inheritLog(ctx.getMessages());

                        long duration = System.currentTimeMillis() - startTime;

                        if (!recommendationEngine.isReadyForPrediction(ctx)) {
                            int docNum = casses.get().size();
                            int trainDocNum = cassesForTraining.size();
                            log.debug(
                                    "[{}][{}][{}]: Training on [{}] out of [{}] documents not successful ({} ms)",
                                    getId(), user.getUsername(), recommender.getName(), trainDocNum,
                                    docNum, duration);
                            info("Training not successful (%d ms).", duration);
                            // The recommender may decide for legitimate reasons not to train and
                            // then this event is annoying
                            // appEventPublisher.publishEvent(new RecommenderTaskEvent(this,
                            // user.getUsername(),
                            // format("Training on %d out of %d documents not successful (%d ms)",
                            // trainDocNum, docNum, duration),
                            // recommender));
                            continue;
                        }

                        log.debug(
                                "[{}][{}][{}]: Training successful on [{}] out of [{}] documents ({} ms)",
                                getId(), user.getUsername(), recommender.getName(),
                                cassesForTraining.size(), casses.get().size(), duration);
                        info("Training successful on [%d] out of [%d] documents (%d ms)",
                                cassesForTraining.size(), casses.get().size(), duration);
                        seenSuccessfulTraining = true;

                        ctx.close();
                        recommendationService.putContext(user, recommender, ctx);
                    }
                    // Catching Throwable is intentional here as we want to continue the execution
                    // even if a particular recommender fails.
                    catch (Throwable e) {
                        long duration = System.currentTimeMillis() - startTime;
                        log.error("[{}][{}][{}]: Training failed ({} ms)", getId(),
                                user.getUsername(), recommender.getName(),
                                (System.currentTimeMillis() - startTime), e);
                        error("Training failed (%d ms): %s", duration, getRootCauseMessage(e));
                        appEventPublisher.publishEvent(new RecommenderTaskEvent(this,
                                user.getUsername(), String.format("Training failed (%d ms) with %s",
                                        duration, e.getMessage()),
                                recommender));
                    }
                }
            }

            if (!seenSuccessfulTraining && !seenNonTrainingRecommender) {
                log.debug(
                        "[{}][{}]: No recommenders trained successfully and no non-training "
                                + "recommenders, skipping prediction.",
                        getId(), user.getUsername());
                return;
            }

            PredictionTask predictionTask = new PredictionTask(user,
                    String.format("TrainingTask %s complete", getId()), currentDocument);
            predictionTask.inheritLog(this);
            schedulingService.enqueue(predictionTask);
        }
    }

    private List<TrainingDocument> readCasses(Project aProject, User aUser)
    {
        List<TrainingDocument> casses = new ArrayList<>();
        Map<SourceDocument, AnnotationDocument> allDocuments = documentService
                .listAllDocuments(aProject, aUser);
        for (Map.Entry<SourceDocument, AnnotationDocument> entry : allDocuments.entrySet()) {
            try {
                SourceDocument sourceDocument = entry.getKey();
                AnnotationDocument annotationDocument = entry.getValue();
                AnnotationDocumentState state = annotationDocument != null
                        ? annotationDocument.getState()
                        : AnnotationDocumentState.NEW;

                // During training, we should not have to modify the CASes... right? Fingers
                // crossed.
                CAS cas = documentService.readAnnotationCas(sourceDocument, aUser.getUsername(),
                        AUTO_CAS_UPGRADE, SHARED_READ_ONLY_ACCESS);
                casses.add(new TrainingDocument(cas, state));
            }
            catch (IOException e) {
                log.error("Cannot read annotation CAS.", e);
            }
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

    private static class TrainingDocument
    {
        private final CAS cas;
        private final AnnotationDocumentState state;

        private TrainingDocument(CAS aCas, AnnotationDocumentState aState)
        {
            cas = aCas;
            state = aState;
        }
    }
}

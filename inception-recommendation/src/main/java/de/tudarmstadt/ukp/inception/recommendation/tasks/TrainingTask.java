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
package de.tudarmstadt.ukp.inception.recommendation.tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.NoResultException;

import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.util.CasUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.scheduling.Task;

/**
 * This consumer trains a new classifier model, if a classification tool was selected before.
 */
public class TrainingTask
    extends Task
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private @Autowired AnnotationSchemaService annoService;
    private @Autowired DocumentService documentService;
    private @Autowired RecommendationService recommendationService;
    private @Autowired SchedulingService schedulingService;

    public TrainingTask(User aUser, Project aProject, String aTrigger)
    {
        super(aUser, aProject, aTrigger);
    }
    
    @Override
    public void run()
    {
        Project project = getProject();
        User user = getUser();

        log.debug("Running training task for user [{}] in project [{}]",  user, project);

        // Read the CASes only when they are accessed the first time. This allows us to skip reading
        // the CASes in case that no layer / recommender is available or if no recommender requires
        // evaluation.
        LazyInitializer<List<TrainingDocument>> casses =
                new LazyInitializer<List<TrainingDocument>>()
        {
            @Override
            protected List<TrainingDocument> initialize()
            {
                return readCasses(project, user);
            }
        };
        
        for (AnnotationLayer layer : annoService.listAnnotationLayer(project)) {
            if (!layer.isEnabled()) {
                continue;
            }
            
            List<Recommender> recommenders = recommendationService.getActiveRecommenders(user,
                    layer);
    
            if (recommenders.isEmpty()) {
                log.debug("[{}][{}]: No active recommenders, skipping training.",
                        user.getUsername(), layer.getUiName());
                continue;
            }
            
            for (Recommender r : recommenders) {
                // Make sure we have the latest recommender config from the DB - the one from the
                // active recommenders list may be outdated
                Recommender recommender;
                try {
                    recommender = recommendationService.getRecommender(r.getId());
                }
                catch (NoResultException e) {
                    log.info("[{}][{}]: Recommender no longer available... skipping",
                            user.getUsername(), r.getName());
                    continue;
                }
                
                if (!recommender.isEnabled()) {
                    log.debug("[{}][{}]: Disabled - skipping", user.getUsername(), r.getName());
                    continue;
                }
                
                long startTime = System.currentTimeMillis();
                RecommenderContext context = recommendationService.getContext(user, recommender);
                RecommendationEngineFactory factory = recommendationService
                    .getRecommenderFactory(recommender);
                
                if (!factory.accepts(recommender.getLayer(), recommender.getFeature())) {
                    log.info("[{}][{}]: Recommender configured with invalid layer or feature "
                            + "- skipping recommender", user.getUsername(), r.getName());
                    continue;
                }

                try {
                    RecommendationEngine recommendationEngine = factory.build(recommender);
                    
                    // If the engine does not require/support training, then we mark the context
                    // as ready for prediction and skip the training step
                    if (!recommendationEngine.requiresTraining()) {
                        log.info("[{}][{}]: Engine does not require training",
                                user.getUsername(), recommender.getName());
                        context.markAsReadyForPrediction();
                        continue;
                    }
                    
                    List<CAS> cassesForTraining = casses.get()
                            .stream()
                            .filter(e -> !recommender.getStatesIgnoredForTraining()
                                    .contains(e.state))
                            .filter(e -> containsTargetAnnotation(recommender, e.cas))
                            .map(e -> e.cas)
                            .collect(Collectors.toList());

                    log.info("[{}][{}]: Training model on [{}] out of [{}] documents ...",
                            user.getUsername(), recommender.getName(), cassesForTraining.size(),
                            casses.get().size());
                    
                    recommendationEngine.train(context, cassesForTraining);
                    
                    log.info("[{}][{}]: Training complete ({} ms)", user.getUsername(),
                            recommender.getName(), (System.currentTimeMillis() - startTime));
                }
                catch (Throwable e) {
                    log.info("[{}][{}]: Training failed ({} ms)", user.getUsername(),
                            recommender.getName(), (System.currentTimeMillis() - startTime), e);
                }
            }
        }

        schedulingService.enqueue(new PredictionTask(user, getProject(),
                        "TrainingTask after training was finished"));
    }

    private List<TrainingDocument> readCasses(Project aProject, User aUser)
    {
        List<TrainingDocument> casses = new ArrayList<>();
        Map<SourceDocument, AnnotationDocument> allDocuments =
                documentService.listAllDocuments(aProject, aUser);
        for (Map.Entry<SourceDocument, AnnotationDocument> entry : allDocuments.entrySet()) {
            try {
                SourceDocument sourceDocument = entry.getKey();
                AnnotationDocument annotationDocument = entry.getValue();
                AnnotationDocumentState state = annotationDocument != null ?
                        annotationDocument.getState() : AnnotationDocumentState.NEW;

                CAS cas = documentService.readAnnotationCas(sourceDocument, aUser.getUsername());
                casses.add(new TrainingDocument(cas, state));
            } catch (IOException e) {
                log.error("Cannot read annotation CAS.", e);
            }
        }
        return casses;
    }

    private boolean containsTargetAnnotation(Recommender aRecommender, CAS aCas)
    {
        Type type = CasUtil.getType(aCas, aRecommender.getLayer().getName());
        return CasUtil.iterator(aCas, type).hasNext();
    }

    private static class TrainingDocument
    {
        private final CAS cas;
        private final AnnotationDocumentState state;

        private TrainingDocument(CAS aCas, AnnotationDocumentState aState) {
            cas = aCas;
            state = aState;
        }
    }
}

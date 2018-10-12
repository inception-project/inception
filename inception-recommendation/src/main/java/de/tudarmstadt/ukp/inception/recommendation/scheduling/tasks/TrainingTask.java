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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.scheduling.RecommendationScheduler;

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
    private @Autowired RecommendationScheduler recommendationScheduler;

    public TrainingTask(User aUser, Project aProject)
    {
        super(aProject, aUser);
    }
    
    @Override
    public void run()
    {
        Project project = getProject();
        User user = getUser();

        List<CAS> casses = readCasses(project, user);
        
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
            
            for (Recommender recommender : recommenders) {
                long startTime = System.currentTimeMillis();
                RecommenderContext context = recommendationService.getContext(user, recommender);
                RecommendationEngineFactory factory = recommendationService
                    .getRecommenderFactory(recommender);
                RecommendationEngine recommendationEngine = factory.build(recommender);

                try {
                    log.info("[{}][{}]: Training model...", user.getUsername(),
                            recommender.getName());
                    recommendationEngine.train(context, casses);
                    log.info("[{}][{}]: Training complete ({} ms)", user.getUsername(),
                            recommender.getName(), (System.currentTimeMillis() - startTime));
                }
                catch (Exception e) {
                    log.info("[{}][{}]: Training failed ({} ms)", user.getUsername(),
                            recommender.getName(), (System.currentTimeMillis() - startTime), e);
                }
            }
        }
        recommendationScheduler.enqueue(new PredictionTask(user, getProject()));
    }

    private List<CAS> readCasses(Project aProject, User aUser)
    {
        List<CAS> casses = new ArrayList<>();
        for (AnnotationDocument doc : documentService.listAnnotationDocuments(aProject, aUser)) {
            try {
                JCas jCas = documentService.readAnnotationCas(doc);
                casses.add(jCas.getCas());
            } catch (IOException e) {
                log.error("Cannot read annotation CAS.", e);
            }
        }
        return casses;
    }
}

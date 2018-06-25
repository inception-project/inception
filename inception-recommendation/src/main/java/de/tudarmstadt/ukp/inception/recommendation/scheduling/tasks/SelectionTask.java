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

import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassificationTool;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.event.RecommenderEvaluationResultEvent;
import de.tudarmstadt.ukp.inception.recommendation.imls.conf.EvaluationConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.ExtendedResult;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.evaluation.EvaluationService;
import de.tudarmstadt.ukp.inception.recommendation.util.EvaluationHelper;

/**
 * This task is run every 60 seconds, if the document has changed. It evaluates all available
 * classification tools for all annotation layers of the current project. If a classifier exceeds
 * its specific activation f-score limit during the evaluation it is selected for active prediction.
 */
public class SelectionTask
    extends Task
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private @Autowired AnnotationSchemaService annoService;
    private @Autowired DocumentService documentService;
    private @Autowired RecommendationService recommendationService;
    private @Autowired ApplicationEventPublisher appEventPublisher;
    
    public SelectionTask(User aUser, Project aProject)
    {
        super(aProject, aUser);
    }

    @Override
    public void run()
    {
        User user = getUser();
        for (AnnotationLayer layer : annoService.listAnnotationLayer(getProject())) {
            if (!layer.isEnabled()) {
                continue;
            }
            
            List<Recommender> recommenders = recommendationService.listRecommenders(layer);
            if (recommenders == null || recommenders.isEmpty()) {
                log.debug("[{}][{}]: No recommenders, skipping selection.", user.getUsername(),
                        layer.getUiName());
                continue;
            }
    
            List<Recommender> activeRecommenders = new ArrayList<>();
            
            for (Recommender recommender : recommenders) {
                try {
                    long start = System.currentTimeMillis();
                    
                    ClassificationTool<?> ct = recommendationService.getTool(recommender,
                            recommendationService.getMaxSuggestions(user));
    
                    if (ct == null || !recommender.isEnabled()) {
                        continue;
                    }
                    
                    if (recommender.isAlwaysSelected()) {
                        log.info("[{}][{}]: Always active", user.getUsername(), ct.getId());
                        activeRecommenders.add(recommender);
                        continue;
                    }
    
                    log.info("[{}][{}]: Evaluating...", user.getUsername(), recommender.getName());
                    
                    EvaluationConfiguration suiteConf = EvaluationHelper
                            .getTrainingSuiteConfiguration("classificationToolSelection",
                                    documentService, getProject());
                    suiteConf.setFeature(ct.getFeature());
                    EvaluationHelper.customizeConfiguration(ct, "_selectionModel.bin",
                            documentService, layer.getProject());
    
                    ExtendedResult result = evaluate(suiteConf, ct,
                            documentService.listSourceDocuments(layer.getProject()));
                    
                    if (result == null || result.getFscore() < 0) {
                        log.info("[{}][{}]: Not activated (unable to determine score)",
                                user.getUsername(), recommender.getName());
                        continue;
                    }
    
                    Double threshold = recommender.getThreshold();
                    if (result.getFscore() >= threshold) {
                        activeRecommenders.add(recommender);
                        log.info("[{}][{}]: Activated ({} is above threshold {})",
                                user.getUsername(), recommender.getName(), result.getFscore(),
                                threshold);
                    }
                    else {
                        log.info("[{}][{}]: Not activated ({} is not above threshold {})",
                                user.getUsername(), recommender.getName(), result.getFscore(),
                                threshold);
                    }
                    
                    appEventPublisher.publishEvent(new RecommenderEvaluationResultEvent(this,
                            recommender, user.getUsername(), result,
                            System.currentTimeMillis() - start));
                }
                catch (Exception e) {
                    log.error("An error occured", e);
                }
            }
    
            recommendationService.setActiveRecommenders(user, layer, activeRecommenders);
        }
    }

    private ExtendedResult evaluate(EvaluationConfiguration suiteConf, ClassificationTool<?> ct,
            List<SourceDocument> docs)
    {
        EvaluationService es = new EvaluationService(ct, suiteConf);
        
        List<List<AnnotationObject>> data = new ArrayList<>();
        for (SourceDocument doc : docs) {
            AnnotationDocument annoDoc = documentService.createOrGetAnnotationDocument(doc,
                    getUser());
            JCas jCas;

            try {
                jCas = documentService.readAnnotationCas(annoDoc);
            }
            catch (IOException e) {
                log.error("Cannot read annotation CAS.", e);
                continue;
            }
            
            data.addAll(ct.getLoader().loadAnnotationObjectsForEvaluation(jCas));
        }

        return es.evaluate(data);
    }
}

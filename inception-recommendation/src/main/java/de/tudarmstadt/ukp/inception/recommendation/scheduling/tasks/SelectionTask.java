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

import javax.persistence.NoResultException;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.PercentageBasedSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;

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
        Project project = getProject();
        User user = getUser();
        String userName = user.getUsername();
        List<CAS> casses = readCasses(project, userName);

        for (AnnotationLayer layer : annoService.listAnnotationLayer(getProject())) {
            if (!layer.isEnabled()) {
                continue;
            }
            
            List<Recommender> recommenders = recommendationService.listRecommenders(layer);
            if (recommenders == null || recommenders.isEmpty()) {
                log.debug("[{}][{}]: No recommenders, skipping selection.", userName,
                        layer.getUiName());
                continue;
            }
    
            List<Recommender> activeRecommenders = new ArrayList<>();
            
            for (Recommender r : recommenders) {
                // Make sure we have the latest recommender config from the DB - the one from
                // the active recommenders list may be outdated
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
                    log.debug("[{}][{}]: Disabled - skipping", userName, recommender.getName());
                    continue;
                }

                String recommenderName = recommender.getName();
                
                try {
                    long start = System.currentTimeMillis();
                    RecommendationEngineFactory factory = recommendationService
                        .getRecommenderFactory(recommender);
                    RecommendationEngine recommendationEngine = factory.build(recommender);

                    if (recommender.isAlwaysSelected()) {
                        log.debug("[{}][{}]: Activating [{}] without evaluating - always selected",
                                userName, recommenderName, recommenderName);
                        activeRecommenders.add(recommender);
                        continue;
                    } else if (!factory.isEvaluable()) {
                        log.debug("[{}][{}]: Activating [{}] without evaluating - not evaluable",
                                userName, recommenderName, recommenderName);
                        activeRecommenders.add(recommender);
                        continue;
                    }
    
                    log.info("[{}][{}]: Evaluating...", userName, recommenderName);

                    DataSplitter splitter = new PercentageBasedSplitter(0.8, 10);
                    double score = recommendationEngine.evaluate(casses, splitter);

                    Double threshold = recommender.getThreshold();
                    if (score >= threshold) {
                        activeRecommenders.add(recommender);
                        log.info("[{}][{}]: Activated ({} is above threshold {})",
                                user.getUsername(), recommenderName, score,
                                threshold);
                    }
                    else {
                        log.info("[{}][{}]: Not activated ({} is not above threshold {})",
                                user.getUsername(), recommenderName, score,
                                threshold);
                    }

                    // TODO: Publish an evaluation event
                }
                catch (Throwable e) {
                    log.error("[{}][{}]: Failed", user.getUsername(), recommenderName, e);
                }
            }
    
            recommendationService.setActiveRecommenders(user, layer, activeRecommenders);
        }
    }

    private List<CAS> readCasses(Project aProject, String aUserName)
    {
        List<CAS> casses = new ArrayList<>();
        for (SourceDocument document : documentService.listSourceDocuments(aProject)) {
            try {
                JCas jCas = documentService.readAnnotationCas(document, aUserName);
                annoService.upgradeCas(jCas.getCas(), document, aUserName);
                casses.add(jCas.getCas());
            } catch (IOException e) {
                log.error("Cannot read annotation CAS.", e);
                continue;
            } catch (UIMAException e) {
                log.error("Cannot upgrade annotation CAS.", e);
                continue;
            }
        }
        return casses;
    }
}

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

import static org.apache.commons.lang3.Validate.notNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.recommendation.imls.conf.EvaluationConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.classificationtool.ClassificationTool;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.ExtendedResult;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.evaluation.EvaluationService;
import de.tudarmstadt.ukp.inception.recommendation.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.service.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.util.EvaluationHelper;

/**
 * This task is run every 60 seconds, if the document has changed. It evaluates all available
 * classification tools for all annotation layers of the current project. If a classifier exceeds
 * its specific activation f-score limit during the evaluation it is selected for active prediction.
 */
public class ClassificationSelectionConsumer
    implements Consumer<AnnotationLayer>
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private DocumentService documentService;
    private RecommendationService recommendationService;
    private Project project;
    private EvaluationConfiguration suiteConf;
    private User user;
    
    public ClassificationSelectionConsumer(DocumentService aDocumentService, User aUser,
            Project aProject, RecommendationService aRecommendationService)
    {
        notNull(aDocumentService);
        notNull(aUser);
        notNull(aProject);
        notNull(aRecommendationService);

        documentService = aDocumentService;
        recommendationService = aRecommendationService;
        project = aProject;
        user = aUser;
        suiteConf = EvaluationHelper.getTrainingSuiteConfiguration("classificationToolSelection",
                aDocumentService, aProject);
    }

    @Override
    public void accept(AnnotationLayer aLayer)
    {
        List<Recommender> recommenders = recommendationService.listRecommenders(aLayer);
        if (recommenders == null || recommenders.isEmpty()) {
            log.debug("[{}][{}]: No recommenders, skipping selection.", user.getUsername(),
                    aLayer.getUiName());
            return;
        }

        Map<String, Float> fScores = new HashedMap<>();
        List<Recommender> activeRecommenders = new ArrayList<>();
        
        for (Recommender recommender : recommenders) {
            try {
                ClassificationTool<?> ct = recommendationService.getTool(recommender,
                        recommendationService.getMaxSuggestions(user));

                if (ct == null) {
                    continue;
                }
                
                if (recommender.isAlwaysSelected()) {
                    log.info("[{}][{}]: Always enabled", user.getUsername(), ct.getId());
                    activeRecommenders.add(recommender);
                    continue;
                }

                log.info("[{}][{}]: Evaluating...", user.getUsername(), recommender.getName());
                
                suiteConf.setFeature(ct.getFeature());

                EvaluationHelper.customizeConfiguration(ct, "_selectionModel.bin", documentService,
                        project);

                fScores.put(recommender.getName(),
                        evaluate(ct, documentService.listSourceDocuments(project)));

                Float threshold = recommender.getThreshold();
                Float fScore = fScores.get(recommender.getName());

                if (threshold == null || fScore == null) {
                    log.info("[{}][{}]: Unable to determine threshold [{}] or score [{}]",
                            user.getUsername(), recommender.getName(), threshold, fScore);
                    continue;
                }

                if (fScore >= threshold) {
                    activeRecommenders.add(recommender);
                    log.info("[{}][{}]: Activated ({} is above threshold {})", user.getUsername(),
                            recommender.getName(), fScore, threshold);
                }
                else {
                    log.info("[{}][{}]: Not activated ({} is not above threshold {})",
                            user.getUsername(), recommender.getName(), fScore, threshold);
                }
            }
            catch (Exception e) {
                log.error("An error occured", e);
            }
        }

        recommendationService.setActiveRecommenders(user, aLayer, activeRecommenders);
    }

    private float evaluate(ClassificationTool<?> ct, List<SourceDocument> docs)
    {
        if (ct == null || docs == null) {
            return 0;
        }

        float totalFScore = 0;
        int casCounter = 0;
        EvaluationService es = new EvaluationService(ct, suiteConf);

        for (SourceDocument doc : docs) {
            AnnotationDocument annoDoc = documentService.createOrGetAnnotationDocument(doc, user);
            JCas jCas;

            try {
                jCas = documentService.readAnnotationCas(annoDoc);
            }
            catch (IOException e) {
                log.error("Cannot read annotation CAS.", e);
                continue;
            }

            casCounter++;
            ExtendedResult result = es
                    .evaluate(ct.getLoader().loadAnnotationObjects(jCas, ct.getFeature()));
            if (result != null) {
                float currentFScore = (float) result.getFscore();
                // prevent adding -1 as FScore (if for example only one sentence is annotated in a
                // document);
                totalFScore += Math.max(0, currentFScore);
            }
        }

        if (casCounter == 0) {
            return 0;
        }

        return totalFScore / (float) casCounter;
    }
}

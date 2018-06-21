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
import java.util.LinkedList;
import java.util.List;

import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.recommendation.api.AnnotationObjectLoader;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassificationTool;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.Trainer;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
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
        User user = getUser();
        
        for (AnnotationLayer layer : annoService.listAnnotationLayer(getProject())) {
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

                try {
                    ClassificationTool<?> classificationTool = recommendationService
                            .getTool(recommender, recommendationService.getMaxSuggestions(user));
    
                    Trainer<?> trainer = classificationTool.getTrainer();
    
                    log.info("[{}][{}]: Extracting training data...", user.getUsername(),
                            recommender.getName());
                    List<List<AnnotationObject>> trainingData = getTrainingData(classificationTool);
    
                    if (trainingData == null || trainingData.isEmpty()) {
                        log.info("[{}][{}]: No training data.", user.getUsername(),
                                recommender.getName());
                        continue;
                    }
    
                    log.info("[{}][{}]: Training model...", user.getUsername(),
                            recommender.getName());
                    Object model = trainer.train(trainingData);
                    if (model != null) {
                        recommendationService.storeTrainedModel(user, recommender, model);
                    }
                    else {
                        log.info("[{}][{}]: Training produced no model", user.getUsername(),
                                recommender.getName());
                    }
    
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

    private List<List<AnnotationObject>> getTrainingData(ClassificationTool<?> tool)
    {
        List<List<AnnotationObject>> result = new LinkedList<>();

        AnnotationObjectLoader loader = tool.getLoader();
        if (loader == null) {
            return result;
        }

        Project p = getProject();
        List<SourceDocument> docs = documentService.listSourceDocuments(p);

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

            List<List<AnnotationObject>> annotatedSentences = loader.loadAnnotationObjects(jCas);

            if (tool.isTrainOnCompleteSentences()) {
                for (List<AnnotationObject> sentence : annotatedSentences) {
                    if (isCompletelyAnnotated(sentence)) {
                        result.add(sentence);
                    }
                }
            }
            else {
                result.addAll(annotatedSentences);
            }
        }

        return result;
    }

    private boolean isCompletelyAnnotated(List<AnnotationObject> sentence)
    {
        if (sentence == null) {
            return false;
        }

        for (AnnotationObject ao : sentence) {
            if (ao.getLabel() == null) {
                return false;
            }
        }

        return true;
    }
}

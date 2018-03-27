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
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.classificationtool.ClassificationTool;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.loader.AnnotationObjectLoader;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.trainer.Trainer;
import de.tudarmstadt.ukp.inception.recommendation.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.scheduling.RecommendationScheduler;
import de.tudarmstadt.ukp.inception.recommendation.service.RecommendationService;

/**
 * This consumer trains a new classifier model, if a classification tool was selected before.
 */
public class TrainingTask
    extends Task
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Predictions predictions;
    
    private @Resource AnnotationSchemaService annoService;
    private @Resource DocumentService documentService;
    private @Resource RecommendationService recommendationService;
    private @Resource RecommendationScheduler recommendationScheduler;

    public TrainingTask(User aUser, Project aProject, Predictions aPredictions)
    {
        super(aProject, aUser);
        
        notNull(aPredictions);
        
        predictions = aPredictions;
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

                ClassificationTool<?> classificationTool = recommendationService
                        .getTool(recommender, recommendationService.getMaxSuggestions(user));

                Trainer<?> trainer = classificationTool.getTrainer();

                log.info("[{}][{}]: Extracting training data...", user.getUsername(),
                        recommender.getName());
                List<List<AnnotationObject>> trainingData = getTrainingData(classificationTool,
                        predictions);

                if (trainingData == null || trainingData.isEmpty()) {
                    log.info("[{}][{}]: No training data.", user.getUsername(),
                            recommender.getName());
                    continue;
                }

                log.info("[{}][{}]: Training model...", user.getUsername(), recommender.getName());
                Object model = trainer.train(trainingData);
                recommendationService.storeTrainedModel(user, recommender, model);

                log.info("[{}][{}]: Training complete ({} ms)", user.getUsername(),
                        recommender.getName(), (System.currentTimeMillis() - startTime));
            }
        }
        
        recommendationScheduler.enqueue(new PredictionTask(user, getProject(), predictions));
    }

    private List<List<AnnotationObject>> getTrainingData(ClassificationTool<?> tool,
            Predictions model)
    {
        List<List<AnnotationObject>> result = new LinkedList<>();

        AnnotationObjectLoader loader = tool.getLoader();
        if (loader == null) {
            return result;
        }

        Project p = model.getProject();
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
            if (ao.getAnnotation() == null) {
                return false;
            }
        }

        return true;
    }
}

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
package de.tudarmstadt.ukp.inception.recommendation.scheduling;

import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.recommendation.model.Predictions;
import de.tudarmstadt.ukp.inception.recommendation.model.TaskType.Type;
import de.tudarmstadt.ukp.inception.recommendation.scheduling.tasks.ClassificationSelectionConsumer;
import de.tudarmstadt.ukp.inception.recommendation.scheduling.tasks.PredictionConsumer;
import de.tudarmstadt.ukp.inception.recommendation.scheduling.tasks.Task;
import de.tudarmstadt.ukp.inception.recommendation.scheduling.tasks.TrainingConsumer;
import de.tudarmstadt.ukp.inception.recommendation.service.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.service.RecommendationServiceImpl;

/**
 * Used to run the selection, training and prediction task concurrently.
 */
public class RecommendationScheduler
    implements DisposableBean
{
    private Logger log = LoggerFactory.getLogger(getClass());
    
    private RecommendationService recService;
    private DocumentService docService;
    private AnnotationSchemaService annoService;
    
    private Thread consumer;
    private BlockingQueue<Task> queue = new ArrayBlockingQueue<Task>(100);
    private int counter = 0;

    public RecommendationScheduler(RecommendationServiceImpl aRecService,
            AnnotationSchemaService anAnnoService, DocumentService aDocService)
    {
        recService = aRecService;
        annoService = anAnnoService;
        docService = aDocService;

        consumer = new Thread(new TaskConsumer(queue), "Recommendation task consumer");
        consumer.start();
        log.info("Started Recommendation Thread");
    }
    
    public void enqueueTask(User user, Project project, Predictions model)
    {   
        // Add Selection Task
        if (counter % 2 == 0) {
            ClassificationSelectionConsumer selectionConsumer = new ClassificationSelectionConsumer(
                    docService, user, project, recService);
            Task selectionTask = new Task(annoService, project, user, Type.SELECTION, 
                    selectionConsumer);

            Iterator<Task> it = queue.iterator();
            while (it.hasNext()) {
                Task t = it.next();
                if (t.getUser().equals(user) && 
                        (t.getType() == Type.SELECTION)) {
                    queue.remove(t);
                }
            }
            queue.offer(selectionTask);
            log.info("Enqueued Selection Task.");
        }
        
        // Add Training and Prediction Task
        TrainingConsumer trainingConsumer = new TrainingConsumer(model, docService, user,
                recService);
        PredictionConsumer predictionConsumer = new PredictionConsumer(model, docService, user, 
                project, recService);
        Task trainAndPredictTask = new Task(annoService, project, user, 
                Type.TRAINING_AND_PREDICTION, trainingConsumer, predictionConsumer);
        Iterator<Task> it = queue.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            if (t.getUser().equals(user) && 
                    t.getType() == Type.TRAINING_AND_PREDICTION) {
                queue.remove(t);
            }
        }
        queue.offer(trainAndPredictTask);
        counter++;
        log.info("Enqueued TrainingAndPrediction Task.");
    }
    
    @Override
    public void destroy()
        throws Exception
    {
        consumer.interrupt();
    }

    public void stopAllTasksForUser(String username)
    {
        Iterator<Task> it = queue.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            if (t.getUser().getUsername().equals(username)) {
                queue.remove(t);
            }
        }
    }

    /*
     * If there are still tasks from the same project and the same user in the queue,
     * the scheduler does not have to be initialized again.
     */
    public boolean isTraining(Project p, User user)
    {
        Iterator<Task> it = queue.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            if (t.getUser().equals(user) && t.getProject().equals(p)) {
                return true;
            }
        }
        return false;
    }
}

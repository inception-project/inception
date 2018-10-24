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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.recommendation.scheduling.tasks.SelectionTask;
import de.tudarmstadt.ukp.inception.recommendation.scheduling.tasks.Task;
import de.tudarmstadt.ukp.inception.recommendation.scheduling.tasks.TrainingTask;

/**
 * Used to run the selection, training and prediction task concurrently.
 */
@Component
public class RecommendationScheduler
{
    private Logger log = LoggerFactory.getLogger(getClass());
    
    private @Autowired ApplicationContext applicationContext;
    
    private Thread consumer;
    private BlockingQueue<Task> queue = new ArrayBlockingQueue<Task>(100);
    private int counter = 0;

    @PostConstruct
    private void startSchedulerThread()
    {
        consumer = new Thread(new TaskConsumer(applicationContext, queue),
                "Recommendation task consumer");
        consumer.setPriority(Thread.MIN_PRIORITY);
        consumer.start();
        log.info("Started Recommendation Thread");
    }
    
    @PreDestroy
    public void destroy()
    {
        consumer.interrupt();
    }
    
    public void enqueueTask(User user, Project project)
    {   
        // Add Selection Task
        if (counter % 2 == 0) {
            enqueue(new SelectionTask(user, project));
        }
        
        // Add Training (which in turn will later enqueue the prediction Task)
        enqueue(new TrainingTask(user, project));
        
        counter++;
    }
    
    public synchronized void enqueue(Task aRunnable)
    {
        // If the no equivalent task is scheduled, then we schedule the new one.
        if (!queue.contains(aRunnable)) {
            queue.offer(aRunnable);
            log.info("Enqueued new task: {} (queue size {})", aRunnable, queue.size());
        }
        else {
            log.info("Task already in queue: {}", aRunnable);
        }
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
    public boolean isTraining(Project p, String user)
    {
        Iterator<Task> it = queue.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            if (t.getUser().getUsername().equals(user) && t.getProject().equals(p)) {
                return true;
            }
        }
        return false;
    }
}

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
package de.tudarmstadt.ukp.inception.search.scheduling;

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
import de.tudarmstadt.ukp.inception.search.scheduling.tasks.ReindexTask;
import de.tudarmstadt.ukp.inception.search.scheduling.tasks.Task;

/**
 * Indexer scheduler. Does the project reindexing in an asynchronous way.
 */
@Component
public class IndexScheduler
{
    private Logger log = LoggerFactory.getLogger(getClass());

    private @Autowired ApplicationContext applicationContext;

    private Thread consumer;
    private BlockingQueue<Task> queue = new ArrayBlockingQueue<Task>(100);

    @PostConstruct
    private void startSchedulerThread()
    {
        consumer = new Thread(new TaskConsumer(applicationContext, queue), "Index task consumer");
        consumer.setPriority(Thread.MIN_PRIORITY);
        consumer.start();
        log.info("Started Search Indexing Thread");
    }

    @PreDestroy
    public void destroy()
    {
        consumer.interrupt();
    }

    public void enqueueReindexTask(Project aProject)
    {
        // Add reindex task
        enqueue(new ReindexTask(aProject));
    }

    public synchronized void enqueue(Task aRunnable)
    {
        // If there is no indexing in the queue on for this project, enqueue it
        if (!isIndexing(aRunnable.getProject())) {
            queue.offer(aRunnable);
            log.info("Enqueued new indexing task: {}", aRunnable);
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

    public boolean isIndexing(Project p)
    {
        Iterator<Task> it = queue.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            if (t.getProject().equals(p)) {
                return true;
            }
        }
        return false;
    }
}

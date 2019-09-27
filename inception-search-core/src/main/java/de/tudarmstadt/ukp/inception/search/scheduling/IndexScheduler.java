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
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.lang3.Validate;
import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.search.scheduling.tasks.IndexAnnotationDocumentTask;
import de.tudarmstadt.ukp.inception.search.scheduling.tasks.IndexSourceDocumentTask;
import de.tudarmstadt.ukp.inception.search.scheduling.tasks.ReindexTask;
import de.tudarmstadt.ukp.inception.search.scheduling.tasks.Task;

/**
 * Indexer scheduler. Does the project re-indexing in an asynchronous way.
 */
@Component
public class IndexScheduler
    implements InitializingBean, DisposableBean
{
    private Logger log = LoggerFactory.getLogger(getClass());

    private @Autowired ApplicationContext applicationContext;

    private TaskConsumer consumer;
    private Thread consumerThread;
    private BlockingQueue<Task> queue = new ArrayBlockingQueue<Task>(100);

    @Override
    public void afterPropertiesSet()
    {
        consumer = new TaskConsumer(applicationContext, queue);
        consumerThread = new Thread(consumer, "Index task consumer");
        consumerThread.setPriority(Thread.MIN_PRIORITY);
        consumerThread.start();
        log.info("Started Search Indexing Thread");
    }

    @Override
    public void destroy()
    {
        consumerThread.interrupt();
    }

    public void enqueueReindexTask(Project aProject)
    {
        // Add reindex task
        enqueue(new ReindexTask(aProject));
    }

    public void enqueueIndexDocument(SourceDocument aSourceDocument, CAS aCas)
    {
        // Index source document
        enqueue(new IndexSourceDocumentTask(aSourceDocument, aCas));
    }

    public void enqueueIndexDocument(AnnotationDocument aAnnotationDocument, CAS aCas)
    {
        // Index annotation document
        enqueue(new IndexAnnotationDocumentTask(aAnnotationDocument, aCas));
    }
    
    /**
     * Put a new indexing task in the queue.
     * Indexing tasks can be of three types:
     *  - Indexing of a whole project
     *  - Indexing of a source document
     *  - Indexing of an annotation document for a given user
     *  
     * @param aRunnable
     *          The indexing task
     */
    public synchronized void enqueue(Task aRunnable)
    {
        Optional<Task> alreadyScheduledTask = findAlreadyScheduled(aRunnable);
        
        // Project indexing task
        if (aRunnable instanceof ReindexTask) {
            if (alreadyScheduledTask.isPresent()) {
                log.debug("Matching project indexing task already scheduled: [{}] - skipping ...",
                        aRunnable);
            }
            else {
                queue.offer(aRunnable);
                log.debug("Enqueued new project indexing task: {}", aRunnable);
            }
        }
        // Source document indexing task
        else if (aRunnable instanceof IndexSourceDocumentTask) {
            if (alreadyScheduledTask.isPresent()) {
                log.debug(
                        "Matching source document indexing task already scheduled: [{}] - skipping ...",
                        aRunnable);
            }
            else {
                queue.offer(aRunnable);
                log.debug("Enqueued new source document indexing task: {}", aRunnable);
            }
        }
        // Annotation document indexing task
        else if (aRunnable instanceof IndexAnnotationDocumentTask) {
            // Try to update the document CAS in the task currently enqueued for the same 
            // annotation document/user (if there is an enqueued task).
            // This must be done so that the task will take into account the
            // latest changes to the annotation document.
            if (alreadyScheduledTask.isPresent()) {
                alreadyScheduledTask.get().setCas(aRunnable.getCas());
                log.debug(
                        "Matching source document indexing task already scheduled: [{}] - updating CAS",
                        aRunnable);
            }
            else {
                queue.offer(aRunnable);
                log.debug("Enqueued new annotation document indexing task: {}", aRunnable);
            }
        }
    }

    public synchronized void stopAllTasksForUser(String username)
    {
        Iterator<Task> taskIterator = queue.iterator();
        while (taskIterator.hasNext()) {
            Task task = taskIterator.next();
            if (task.getUser().equals(username)) {
                queue.remove(task);
            }
        }
    }

    public boolean isIndexInProgress(Project aProject)
    {
        Validate.notNull(aProject, "Project cannot be null");
        
        return queue.stream().anyMatch(task -> aProject.equals(task.getProject())) ||
                consumer.getActiveTask().map(t -> aProject.equals(t.getProject())).orElse(false);
    }
    
    private Optional<Task> findAlreadyScheduled(Task aTask)
    {
        return queue.stream().filter(aTask::matches).findAny();
    }
}

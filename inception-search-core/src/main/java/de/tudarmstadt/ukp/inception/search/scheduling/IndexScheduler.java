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

import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.search.scheduling.tasks.IndexDocumentTask;
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

    public void enqueueIndexDocument(SourceDocument aSourceDocument, JCas aJCas)
    {
        // Index source document
        enqueue(new IndexDocumentTask(aSourceDocument, aJCas));
    }

    public void enqueueIndexDocument(AnnotationDocument aAnnotationDocument, JCas aJCas)
    {
        // Index annotation document
        enqueue(new IndexDocumentTask(aAnnotationDocument, aJCas));
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
        if (aRunnable.getAnnotationDocument() == null && aRunnable.getSourceDocument() == null) {
            // Project indexing task
            
            // If there is no indexing in the queue on for this project, enqueue it
            if (!isIndexing(aRunnable.getProject())) {
                queue.offer(aRunnable);
                log.info("Enqueued new indexing task: {}", aRunnable);
            }
        }
        else if (aRunnable.getSourceDocument() != null) {
            // Source document indexing task
            
            // If there is no indexing in the queue on for this project, enqueue it
            if (!isIndexingDocument(aRunnable.getSourceDocument())) {
                queue.offer(aRunnable);
                log.info("Enqueued new source document indexing task: {}", aRunnable);
            }
            else {
                log.debug("No source document indexing task enqueued due to a previous "
                        + "enqueued task: {}", aRunnable);
            }
        }
        else if (aRunnable.getAnnotationDocument() != null) {
            // Annotation document indexing task

            // Try to update the document CAS in the task currently enqueued for the same 
            // annotation document/user (if there is an enqueued task).
            // This must be done so that the task will take into account the
            // latest changes to the annotation document.

            if (updateIndexingDocumentTask(aRunnable.getAnnotationDocument(),
                    aRunnable.getAnnotationDocument().getUser(),
                    aRunnable.getJCas())) {
                // There was a task in the queue, it was updated with the current document.
                log.debug("Annotation document indexing task already in the queue. Just updated "
                        + " the document: {}", aRunnable);
            }
            else {
                // There was no indexing task in the queue for this document/user. Enqueue new task.
                queue.offer(aRunnable);
                log.info("Enqueued new document indexing task: {}", aRunnable);
            }
        }
    }

    public void stopAllTasksForUser(String username)
    {
        Iterator<Task> taskIterator = queue.iterator();
        while (taskIterator.hasNext()) {
            Task task = taskIterator.next();
            if (task.getUser().equals(username)) {
                queue.remove(task);
            }
        }
    }

    /**
     * Check if there is an indexing task for this project.
     * 
     * @param aProject
     *          The project
     * @return
     *          True if there is an indexing task for this project.
     *          False otherwise
     */
    public boolean isIndexing(Project aProject)
    {
        Iterator<Task> taskIterator = queue.iterator();
        while (taskIterator.hasNext()) {
            Task t = taskIterator.next();
            if (t.getProject().equals(aProject)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if there is an indexing task for this source document.
     * 
     * @param aSourceDocument
     *          The source document
     * @return
     *          True if there is an indexing task for the source document.
     *          False otherwise.
     */
    public boolean isIndexingDocument(SourceDocument aSourceDocument)
    {
        Iterator<Task> taskIterator = queue.iterator();
        while (taskIterator.hasNext()) {
            Task task = taskIterator.next();
            if (task.getProject().equals(aSourceDocument.getProject())
                    && task.getAnnotationDocument().getId() == aSourceDocument.getId()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Update the CAS in the indexing task that is currently in the queue for this annotation 
     * document/user, if there is one.
     * 
     * @param aAnnotationDocument
     *          The annotation document
     * @param aUser
     *          The user
     * @return
     *          True if there was an indexing task in the queue for this annotation document/user.
     *          False otherwise.
     */
    public boolean updateIndexingDocumentTask(AnnotationDocument aAnnotationDocument, String aUser,
            JCas aJCas)
    {
        Iterator<Task> it = queue.iterator();
        while (it.hasNext()) {
            Task task = it.next();
            if (task.getProject().equals(aAnnotationDocument.getProject())
                    && task.getAnnotationDocument().getId() == aAnnotationDocument.getId()
                    && task.getUser().equals(aUser)) {
                task.setJCas(aJCas);
                return true;
            }
        }
        return false;
    }

}

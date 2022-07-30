/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.search.scheduling.tasks;

import static de.tudarmstadt.ukp.inception.scheduling.MatchResult.NO_MATCH;
import static de.tudarmstadt.ukp.inception.scheduling.MatchResult.UNQUEUE_EXISTING_AND_QUEUE_THIS;

import java.io.IOException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.scheduling.MatchResult;
import de.tudarmstadt.ukp.inception.scheduling.Task;
import de.tudarmstadt.ukp.inception.search.SearchService;
import de.tudarmstadt.ukp.inception.search.model.Monitor;
import de.tudarmstadt.ukp.inception.search.model.Progress;

/**
 * Search indexer task. Runs the re-indexing process for a given project
 */
public class ReindexTask
    extends IndexingTask_ImplBase
{
    private Logger log = LoggerFactory.getLogger(getClass());

    private @Autowired SearchService searchService;

    private Monitor monitor = new Monitor();

    public ReindexTask(Project aProject, String aTrigger)
    {
        super(aProject, null, aTrigger);
    }

    @Override
    public void execute()
    {
        try {
            searchService.reindex(super.getProject(), monitor);
        }
        catch (IOException e) {
            log.error("Unable to reindex project [{}]({})", getProject().getName(),
                    getProject().getId(), e);
        }
    }

    @Override
    public Progress getProgress()
    {
        return monitor.toProgress();
    }

    @Override
    public MatchResult matches(Task aTask)
    {
        // If a re-indexing task for a project is coming in, we can throw out any scheduled tasks
        // for re-indexing and for indexing individual source/annotation documents in the project.
        if (aTask instanceof ReindexTask || aTask instanceof IndexSourceDocumentTask
                || aTask instanceof IndexAnnotationDocumentTask) {
            if (Objects.equals(getProject().getId(), aTask.getProject().getId())) {
                return UNQUEUE_EXISTING_AND_QUEUE_THIS;
            }
        }

        return NO_MATCH;
    }
}

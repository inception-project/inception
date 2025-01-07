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

import static de.tudarmstadt.ukp.inception.scheduling.MatchResult.DISCARD_OR_QUEUE_THIS;
import static de.tudarmstadt.ukp.inception.scheduling.MatchResult.NO_MATCH;
import static de.tudarmstadt.ukp.inception.scheduling.MatchResult.UNQUEUE_EXISTING_AND_QUEUE_THIS;
import static de.tudarmstadt.ukp.inception.scheduling.TaskScope.PROJECT;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.inception.scheduling.MatchResult;
import de.tudarmstadt.ukp.inception.scheduling.Progress;
import de.tudarmstadt.ukp.inception.scheduling.ProjectTask;
import de.tudarmstadt.ukp.inception.scheduling.Task;
import de.tudarmstadt.ukp.inception.search.SearchService;

/**
 * Search indexer task. Runs the re-indexing process for a given project
 */
public class ReindexTask
    extends IndexingTask_ImplBase
    implements ProjectTask
{
    public static final String TYPE = "ReindexTask";

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @Autowired SearchService searchService;

    public ReindexTask(Builder<? extends Builder<?>> aBuilder)
    {
        super(aBuilder.withType(TYPE) //
                .withCancellable(false) //
                .withScope(PROJECT));
    }

    @Override
    public String getTitle()
    {
        return "Rebuilding index...";
    }

    @Override
    public void execute() throws IOException
    {
        searchService.reindex(super.getProject(), getMonitor());
    }

    @Deprecated
    @Override
    public Progress getProgress()
    {
        return getMonitor().toProgress();
    }

    @Override
    public MatchResult matches(Task aTask)
    {
        // If a re-indexing task for a project is coming in, we can throw out any scheduled tasks
        // for indexing individual source/annotation documents in the project.
        if (aTask instanceof IndexSourceDocumentTask
                || aTask instanceof IndexAnnotationDocumentTask) {
            if (Objects.equals(getProject().getId(), aTask.getProject().getId())) {
                return UNQUEUE_EXISTING_AND_QUEUE_THIS;
            }
        }

        if (aTask instanceof ReindexTask) {
            if (Objects.equals(getProject().getId(), aTask.getProject().getId())) {
                return DISCARD_OR_QUEUE_THIS;
            }
        }

        return NO_MATCH;
    }

    public static Builder<Builder<?>> builder()
    {
        return new Builder<>();
    }

    public static class Builder<T extends Builder<?>>
        extends IndexingTask_ImplBase.Builder<T>
    {
        public ReindexTask build()
        {
            return new ReindexTask(this);
        }
    }
}

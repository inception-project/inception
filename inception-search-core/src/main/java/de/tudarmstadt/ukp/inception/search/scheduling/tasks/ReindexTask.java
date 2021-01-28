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

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.search.SearchService;

/**
 * Search indexer task. Runs the re-indexing process for a given project
 */
public class ReindexTask
    extends Task
{
    private Logger log = LoggerFactory.getLogger(getClass());

    private @Autowired SearchService searchService;

    public ReindexTask(Project aProject)
    {
        super(aProject, null);
    }

    @Override
    public void run()
    {
        try {
            searchService.reindex(super.getProject());
        }
        catch (IOException e) {
            log.error("Unable to reindex project [{}]({})", getProject().getName(),
                    getProject().getId(), e);
        }
    }

    @Override
    public boolean matches(Task aTask)
    {
        if (!(aTask instanceof ReindexTask)) {
            return false;
        }

        return getProject().getId() == aTask.getProject().getId();
    }
}

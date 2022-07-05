/*
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
package de.tudarmstadt.ukp.inception.search.cli;

import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.search.SearchService;
import de.tudarmstadt.ukp.inception.search.model.Monitor;
import picocli.CommandLine.Command;

@ConditionalOnNotWebApplication
@Component
@Command( //
        name = "reindex", //
        description = { //
                "Rebuilds the indexes of all projects.", //
                "NOTE: This may take a very long time!" })
public class SearchReindexCliCommand
    implements Callable<Integer>
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ProjectService projectService;
    private final SearchService searchService;

    public SearchReindexCliCommand(ProjectService aProjectService, SearchService aSearchService)
    {
        projectService = aProjectService;
        searchService = aSearchService;
    }

    @Override
    public Integer call() throws Exception
    {
        List<Project> projects = projectService.listProjects();

        for (Project project : projects) {
            Monitor monitor = new Monitor()
            {
                private long lastLog = 0;

                @Override
                public synchronized void incDone()
                {
                    super.incDone();

                    if (isDone() || System.currentTimeMillis() - lastLog > 5_000) {
                        log.info("{}: Documents processed: {}", project, this);
                        lastLog = System.currentTimeMillis();
                    }
                }
            };

            searchService.reindex(project, monitor);
        }

        return 0;
    }
}

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

import static java.util.Arrays.asList;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.scheduling.LoggingTaskMonitor;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.search.ReindexTask;
import de.tudarmstadt.ukp.inception.search.SearchService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

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
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Option(names = { "--project-slug" }, description = "Re-index only the given project")
    private String slug;

    private final ProjectService projectService;
    private final SearchService searchService;
    private final SchedulingService schedulingService;
    private final UserDao userService;

    public SearchReindexCliCommand(ProjectService aProjectService, SearchService aSearchService,
            SchedulingService aSchedulingService, UserDao aUserService)
    {
        projectService = aProjectService;
        searchService = aSearchService;
        schedulingService = aSchedulingService;
        userService = aUserService;
    }

    @Override
    public Integer call() throws Exception
    {
        var projects = selectProjects();

        for (var project : projects) {

            schedulingService.executeSync(ReindexTask.builder() //
                    .withProject(project) //
                    .withSessionOwner(userService.getCurationUser()) //
                    .withTrigger("CLI") //
                    .withMonitor(LoggingTaskMonitor::new) //
                    .build());
        }

        return 0;
    }

    private List<Project> selectProjects()
    {
        if (slug != null) {
            return asList(projectService.getProjectBySlug(slug));
        }

        return projectService.listProjects();
    }
}

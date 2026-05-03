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
package de.tudarmstadt.ukp.inception.cli;

import static de.tudarmstadt.ukp.inception.kb.RepositoryType.LOCAL;
import static java.util.Arrays.asList;

import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.scheduling.LoggingTaskMonitor;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.search.ReindexTask;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@ConditionalOnNotWebApplication
@Component
@Command( //
        name = "reindex", //
        mixinStandardHelpOptions = true, //
        description = { //
                "Rebuilds all indexes (document search and knowledge base full-text) "
                        + "for all projects.", //
                "Use 'search reindex' or 'kb reindex' to rebuild only one kind of index.", //
                "NOTE: This may take a very long time!" })
public class ReindexCliCommand
    implements Callable<Integer>
{
    private final static Logger LOG = LoggerFactory.getLogger("inception.cli");

    @Option(names = { "--project-slug" }, description = "Re-index only the given project")
    private String slug;

    private final ProjectService projectService;
    private final SchedulingService schedulingService;
    private final UserDao userService;
    private final KnowledgeBaseService kbService;

    public ReindexCliCommand(ProjectService aProjectService, SchedulingService aSchedulingService,
            UserDao aUserService, KnowledgeBaseService aKbService)
    {
        projectService = aProjectService;
        schedulingService = aSchedulingService;
        userService = aUserService;
        kbService = aKbService;
    }

    @Override
    public Integer call() throws Exception
    {
        var projects = selectProjects();
        var failures = 0;

        for (var project : projects) {
            LOG.info("Re-indexing documents in project [{}]...", project.getSlug());
            schedulingService.executeSync(ReindexTask.builder() //
                    .withProject(project) //
                    .withSessionOwner(userService.getCurationUser()) //
                    .withTrigger("CLI") //
                    .withMonitor(LoggingTaskMonitor::new) //
                    .build());

            for (var kb : kbService.getKnowledgeBases(project)) {
                if (kb.getType() != LOCAL) {
                    LOG.info("Skipping non-local KB [{}] in project [{}]", kb.getName(),
                            project.getSlug());
                    continue;
                }

                LOG.info("Re-indexing KB [{}] in project [{}]...", kb.getName(), project.getSlug());
                try {
                    kbService.rebuildFullTextIndex(kb);
                }
                catch (Exception e) {
                    LOG.error("Failed to re-index KB [{}] in project [{}]: {}", kb.getName(),
                            project.getSlug(), e.getMessage(), e);
                    failures++;
                }
            }
        }

        return failures == 0 ? 0 : 1;
    }

    private List<Project> selectProjects()
    {
        if (slug != null) {
            return asList(projectService.getProjectBySlug(slug));
        }

        return projectService.listProjects();
    }
}

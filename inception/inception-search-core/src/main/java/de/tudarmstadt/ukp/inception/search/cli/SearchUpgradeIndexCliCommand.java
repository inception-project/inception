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

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.search.SearchService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command( //
        name = "upgrade-index", //
        mixinStandardHelpOptions = true, //
        description = { //
                "Upgrades the document (CAS) search indexes to the current Lucene format "
                        + "without re-indexing the documents.", //
                "Only works if the existing index format is still readable by the running "
                        + "Lucene version. If not, use 'search reindex' instead." })
public class SearchUpgradeIndexCliCommand
    implements Callable<Integer>
{
    private final static Logger LOG = LoggerFactory.getLogger("inception.cli");

    @Option(names = {
            "--project-slug" }, description = "Upgrade only the index of the given project")
    private String slug;

    private final ProjectService projectService;
    private final SearchService searchService;

    public SearchUpgradeIndexCliCommand(ProjectService aProjectService,
            SearchService aSearchService)
    {
        projectService = aProjectService;
        searchService = aSearchService;
    }

    @Override
    public Integer call() throws Exception
    {
        var projects = selectProjects();
        var processed = 0;
        var failures = 0;

        for (var project : projects) {
            LOG.info("Upgrading document search index for project [{}]...", project.getSlug());
            try {
                searchService.upgradeIndex(project);
                processed++;
            }
            catch (Exception e) {
                LOG.error("Failed to upgrade document search index for project [{}]: {}",
                        project.getSlug(), e.getMessage(), e);
                failures++;
            }
        }

        LOG.info("Upgraded {} document search index(es); failed {}", processed, failures);
        return failures == 0 ? 0 : 1;
    }

    private List<Project> selectProjects()
    {
        if (slug != null) {
            return List.of(projectService.getProjectBySlug(slug));
        }

        return projectService.listProjects();
    }
}

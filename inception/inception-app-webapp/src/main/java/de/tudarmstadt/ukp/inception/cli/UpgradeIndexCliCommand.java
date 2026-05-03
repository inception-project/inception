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
import java.util.Optional;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.assistant.documents.DocumentQueryService;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.search.SearchService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@ConditionalOnNotWebApplication
@Component
@Command( //
        name = "upgrade-index", //
        mixinStandardHelpOptions = true, //
        description = { //
                "Upgrades all Lucene-based indexes (document search, knowledge base full-text "
                        + "and assistant document embeddings) to the current Lucene format "
                        + "without re-indexing or re-embedding the data.", //
                "Use 'search upgrade-index' or 'kb upgrade-index' to upgrade only one kind "
                        + "of index. If the on-disk index format is too old to be read by the "
                        + "running Lucene version, use 'reindex' instead." })
public class UpgradeIndexCliCommand
    implements Callable<Integer>
{
    private final static Logger LOG = LoggerFactory.getLogger("inception.cli");

    @Option(names = {
            "--project-slug" }, description = "Upgrade only the indexes of the given project")
    private String slug;

    private final ProjectService projectService;
    private final SearchService searchService;
    private final KnowledgeBaseService kbService;
    private final ObjectProvider<DocumentQueryService> documentQueryServiceProvider;

    public UpgradeIndexCliCommand(ProjectService aProjectService, SearchService aSearchService,
            KnowledgeBaseService aKbService,
            ObjectProvider<DocumentQueryService> aDocumentQueryServiceProvider)
    {
        projectService = aProjectService;
        searchService = aSearchService;
        kbService = aKbService;
        documentQueryServiceProvider = aDocumentQueryServiceProvider;
    }

    @Override
    public Integer call() throws Exception
    {
        var projects = selectProjects();
        var failures = 0;
        var assistant = Optional.ofNullable(documentQueryServiceProvider.getIfAvailable());

        for (var project : projects) {
            LOG.info("Upgrading document search index for project [{}]...", project.getSlug());
            try {
                searchService.upgradeIndex(project);
            }
            catch (Exception e) {
                LOG.error("Failed to upgrade document search index for project [{}]: {}",
                        project.getSlug(), e.getMessage(), e);
                failures++;
            }

            for (var kb : kbService.getKnowledgeBases(project)) {
                if (kb.getType() != LOCAL) {
                    LOG.info("Skipping non-local KB [{}] in project [{}]", kb.getName(),
                            project.getSlug());
                    continue;
                }

                LOG.info("Upgrading KB index [{}] in project [{}]...", kb.getName(),
                        project.getSlug());
                try {
                    kbService.upgradeFullTextIndex(kb);
                }
                catch (Exception e) {
                    LOG.error("Failed to upgrade KB index [{}] in project [{}]: {}", kb.getName(),
                            project.getSlug(), e.getMessage(), e);
                    failures++;
                }
            }

            if (assistant.isPresent()) {
                LOG.info("Upgrading assistant document index for project [{}]...",
                        project.getSlug());
                try {
                    assistant.get().upgradeIndex(project);
                }
                catch (Exception e) {
                    LOG.error("Failed to upgrade assistant document index for project [{}]: {}",
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

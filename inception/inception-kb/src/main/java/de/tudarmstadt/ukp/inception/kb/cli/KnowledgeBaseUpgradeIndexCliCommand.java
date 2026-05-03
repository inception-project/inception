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
package de.tudarmstadt.ukp.inception.kb.cli;

import static de.tudarmstadt.ukp.inception.kb.RepositoryType.LOCAL;

import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command( //
        name = "upgrade-index", //
        mixinStandardHelpOptions = true, //
        description = { //
                "Upgrades the full-text indexes of local knowledge bases to the current Lucene "
                        + "format without re-indexing the KB content.", //
                "Skips remote and non-Lucene knowledge bases.", //
                "Only works if the existing index format is still readable by the running "
                        + "Lucene version. If not, use 'kb reindex' instead." })
public class KnowledgeBaseUpgradeIndexCliCommand
    implements Callable<Integer>
{
    private final static Logger LOG = LoggerFactory.getLogger("inception.cli");

    @Option(names = { "--project-slug" }, //
            description = "Upgrade only knowledge bases in the project with the given slug")
    private String projectSlug;

    @Option(names = { "--kb-name" }, //
            description = "Upgrade only knowledge bases whose name matches this value")
    private String kbName;

    private final ProjectService projectService;
    private final KnowledgeBaseService kbService;

    public KnowledgeBaseUpgradeIndexCliCommand(ProjectService aProjectService,
            KnowledgeBaseService aKbService)
    {
        projectService = aProjectService;
        kbService = aKbService;
    }

    @Override
    public Integer call() throws Exception
    {
        var projects = selectProjects();
        var failures = 0;
        var processed = 0;
        var skipped = 0;

        for (var project : projects) {
            for (var kb : kbService.getKnowledgeBases(project)) {
                if (kbName != null && !kbName.equals(kb.getName())) {
                    continue;
                }

                if (kb.getType() != LOCAL) {
                    LOG.info("Skipping non-local KB [{}] in project [{}]", kb.getName(),
                            project.getSlug());
                    skipped++;
                    continue;
                }

                LOG.info("Upgrading KB index [{}] in project [{}]...", kb.getName(),
                        project.getSlug());
                try {
                    kbService.upgradeFullTextIndex(kb);
                    processed++;
                }
                catch (Exception e) {
                    LOG.error("Failed to upgrade KB index [{}] in project [{}]: {}", kb.getName(),
                            project.getSlug(), e.getMessage(), e);
                    failures++;
                }
            }
        }

        LOG.info("Upgraded {} KB index(es); skipped {}; failed {}", processed, skipped, failures);
        return failures == 0 ? 0 : 1;
    }

    private List<Project> selectProjects()
    {
        if (projectSlug != null) {
            return List.of(projectService.getProjectBySlug(projectSlug));
        }

        return projectService.listProjects();
    }
}

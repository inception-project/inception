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
        name = "reindex", //
        mixinStandardHelpOptions = true, //
        description = { //
                "Rebuilds the full-text indexes of local knowledge bases.",
                "Skips remote and non-Lucene knowledge bases.", //
                "NOTE: This may take a very long time!" })
public class KnowledgeBaseReindexCliCommand
    implements Callable<Integer>
{
    private final static Logger LOG = LoggerFactory.getLogger("inception.cli");

    @Option(names = { "--project-slug" }, //
            description = "Re-index only knowledge bases in the project with the given slug")
    private String projectSlug;

    @Option(names = { "--kb-name" }, //
            description = "Re-index only knowledge bases whose name matches this value")
    private String kbName;

    private final ProjectService projectService;
    private final KnowledgeBaseService kbService;

    public KnowledgeBaseReindexCliCommand(ProjectService aProjectService,
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

                LOG.info("Re-indexing KB [{}] in project [{}]...", kb.getName(), project.getSlug());
                try {
                    kbService.rebuildFullTextIndex(kb);
                    processed++;
                }
                catch (Exception e) {
                    LOG.error("Failed to re-index KB [{}] in project [{}]: {}", kb.getName(),
                            project.getSlug(), e.getMessage(), e);
                    failures++;
                }
            }
        }

        LOG.info("Re-indexed {} KB(s); skipped {}; failed {}", processed, skipped, failures);
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

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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.cli;

import static java.util.Arrays.asList;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.log.EventRepository;
import de.tudarmstadt.ukp.inception.log.model.StateChangeDetails;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@ConditionalOnNotWebApplication
@Component
@Command( //
        name = "rebuild-state-updated", //
        description = { //
                "Rebuilds the state-updated field of projects, documents and annotations based on the event log", //
                "NOTE: This may take some time!" })
public class RebuildStateUpdatedFieldsCliCommand
    implements Callable<Integer>
{
    private final static Logger LOG = LoggerFactory.getLogger("inception.cli");

    @Option(names = { "--project-slug" }, description = "Re-build only the given project")
    private String slug;

    private final ProjectService projectService;
    private final DocumentService documentService;
    private final EventRepository eventRepository;

    public RebuildStateUpdatedFieldsCliCommand(ProjectService aProjectService,
            DocumentService aDocumentService, EventRepository aEventRepository)
    {
        projectService = aProjectService;
        documentService = aDocumentService;
        eventRepository = aEventRepository;
    }

    @Override
    public Integer call() throws Exception
    {
        var projectsUpdated = new LongOpenHashSet();
        var sourceDocumentUpdated = new LongOpenHashSet();
        var annotationDocumentUpdated = new LongOpenHashSet();

        for (var project : selectProjects()) {
            LOG.info("Processing project: {}", project);

            var srcDocs = documentService.listSourceDocuments(project);
            var annDocs = documentService.listAnnotationDocuments(project);
            var stateChangeEvents = Set.of("ProjectStateChangedEvent", "DocumentStateChangedEvent",
                    "AnnotationStateChangeEvent");

            // We get the events in creation order
            eventRepository.forEachLoggedEvent(project, event -> {
                if (!stateChangeEvents.contains(event.getEvent())) {
                    return;
                }

                try {
                    var details = JSONUtil.fromJsonString(StateChangeDetails.class,
                            event.getDetails());

                    if ("ProjectStateChangedEvent".equals(event.getEvent())) {
                        if (project.getState() != ProjectState.fromString(details.getState())) {
                            return;
                        }

                        projectService.updateProjectStateUpdatedDirectly(project.getId(),
                                event.getCreated());
                        projectsUpdated.add(project.getId());
                    }

                    if ("DocumentStateChangedEvent".equals(event.getEvent())) {
                        var maybeSrcDoc = srcDocs.stream()
                                .filter(sd -> Objects.equals(event.getDocument(), sd.getId()))
                                .findFirst();

                        if (maybeSrcDoc.isEmpty()) {
                            return;
                        }

                        var srcDoc = maybeSrcDoc.get();

                        if (srcDoc.getState() != SourceDocumentState
                                .fromString(details.getState())) {
                            return;
                        }

                        documentService.updateSourceDocumentStateUpdatedDirectly(srcDoc.getId(),
                                event.getCreated());
                        sourceDocumentUpdated.add(srcDoc.getId());
                    }

                    if ("AnnotationStateChangeEvent".equals(event.getEvent())) {
                        var maybeAnnDoc = annDocs.stream().filter(
                                ad -> Objects.equals(event.getDocument(), ad.getDocument().getId())
                                        && Objects.equals(event.getAnnotator(), ad.getUser()))
                                .findFirst();

                        if (maybeAnnDoc.isEmpty()) {
                            return;
                        }

                        var annDoc = maybeAnnDoc.get();

                        if (annDoc.getState() != AnnotationDocumentState
                                .fromString(details.getState())) {
                            return;
                        }

                        documentService.updateAnnotationDocumentStateUpdatedDirectly(annDoc.getId(),
                                event.getCreated());
                        annotationDocumentUpdated.add(annDoc.getId());
                    }
                }
                catch (Exception e) {
                    LOG.error("Error while processing event [{}]", event, e);
                }
            });
        }

        LOG.info("Projects updated: {}", projectsUpdated.size());
        LOG.info("Source documents updated: {}", sourceDocumentUpdated.size());
        LOG.info("Annotation documents updated: {}", annotationDocumentUpdated.size());

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

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
package de.tudarmstadt.ukp.inception.documents.cli;

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
import de.tudarmstadt.ukp.inception.log.api.EventRepository;
import de.tudarmstadt.ukp.inception.log.api.model.StateChangeDetails;
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
    public Integer call()
    {
        rebuild(LoggerFactory.getLogger("inception.cli"), slug);

        return 0;
    }

    public void rebuildAll(Logger aLog)
    {
        rebuild(aLog, null);
    }

    private void rebuild(Logger aLog, String aSlug)
    {
        var projectsUpdated = new LongOpenHashSet();
        var sourceDocumentUpdated = new LongOpenHashSet();
        var annotationDocumentUpdated = new LongOpenHashSet();

        for (var project : selectProjects(aSlug)) {
            aLog.info("Processing project: {}", project);

            var srcDocs = documentService.listSourceDocuments(project);
            var annDocs = documentService.listAnnotationDocuments(project);
            var stateChangeEvents = Set.of("ProjectStateChangedEvent", "DocumentStateChangedEvent",
                    "AnnotationStateChangeEvent");

            // We get the events in creation order
            eventRepository.forEachLoggedEventUpdatable(project, event -> {
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
                    aLog.error("Error while processing event [{}]", event, e);
                }
            });
        }

        aLog.info("Projects updated: {}", projectsUpdated.size());
        aLog.info("Source documents updated: {}", sourceDocumentUpdated.size());
        aLog.info("Annotation documents updated: {}", annotationDocumentUpdated.size());
    }

    private List<Project> selectProjects(String aSlug)
    {
        if (aSlug != null) {
            return asList(projectService.getProjectBySlug(aSlug));
        }

        return projectService.listProjects();
    }
}

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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.dashlet;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CURATION_USER;
import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.SetUtils.unmodifiableSet;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.ChainLinkCreatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.ChainLinkDeletedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.ChainSpanCreatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.ChainSpanDeletedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.FeatureValueUpdatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.RelationCreatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.RelationDeletedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.SpanCreatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.SpanDeletedEvent;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageMenuItem;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.page.CurationPageMenuItem;
import de.tudarmstadt.ukp.inception.log.EventRepository;
import de.tudarmstadt.ukp.inception.log.model.LoggedEvent;

@RestController
@RequestMapping(ActivitiesDashletController.BASE_URL)
public class ActivitiesDashletControllerImpl
    implements ActivitiesDashletController
{
    private final EventRepository eventRepository;
    private final DocumentService documentService;
    private final CurationDocumentService curationService;
    private final ProjectService projectRepository;
    private final UserDao userRepository;
    private final AnnotationPageMenuItem annotationPageMenuItem;
    private final CurationPageMenuItem curationPageMenuItem;
    private final ServletContext servletContext;

    private final Set<String> annotationEvents = unmodifiableSet( //
            SpanCreatedEvent.class.getSimpleName(), //
            SpanDeletedEvent.class.getSimpleName(), //
            RelationCreatedEvent.class.getSimpleName(), //
            RelationDeletedEvent.class.getSimpleName(), //
            ChainLinkCreatedEvent.class.getSimpleName(), //
            ChainLinkDeletedEvent.class.getSimpleName(), //
            ChainSpanCreatedEvent.class.getSimpleName(), //
            ChainSpanDeletedEvent.class.getSimpleName(), //
            FeatureValueUpdatedEvent.class.getSimpleName(), //
            "DocumentMetadataCreatedEvent", //
            "DocumentMetadataDeletedEvent");

    @Autowired
    public ActivitiesDashletControllerImpl(EventRepository aEventRepository,
            DocumentService aDocumentService, CurationDocumentService aCurationService,
            ProjectService aProjectRepository, UserDao aUserRepository,
            AnnotationPageMenuItem aAnnotationPageMenuItem,
            CurationPageMenuItem aCurationPageMenuItem, ServletContext aServletContext)
    {
        super();
        eventRepository = aEventRepository;
        documentService = aDocumentService;
        curationService = aCurationService;
        projectRepository = aProjectRepository;
        userRepository = aUserRepository;
        annotationPageMenuItem = aAnnotationPageMenuItem;
        curationPageMenuItem = aCurationPageMenuItem;
        servletContext = aServletContext;
    }

    @Override
    public String listActivitiesUrl(long aProjectId)
    {
        return servletContext.getContextPath() + BASE_URL
                + LIST_PATH.replace("{projectId}", String.valueOf(aProjectId));
    }

    @Override
    @GetMapping(LIST_PATH)
    public List<Activity> listActivities(@PathVariable("projectId") long aProjectId)
    {
        User user = userRepository.getCurrentUser();
        Project project = projectRepository.getProject(aProjectId);

        if (!projectRepository.existsProjectPermission(user, project)) {
            return emptyList();
        }

        Map<Long, SourceDocument> annotatableSourceDocuments = documentService
                .listAnnotatableDocuments(project, user).keySet().stream()
                .collect(toMap(SourceDocument::getId, identity()));

        Map<Long, SourceDocument> curatableSourceDocuments = curationService
                .listCuratableSourceDocuments(project).stream()
                .collect(toMap(SourceDocument::getId, identity()));

        boolean isCurator = projectRepository.isCurator(project, user);

        // get last annotation events
        // return filtered by user rights and document state
        List<LoggedEvent> recentEvents = eventRepository.listRecentActivity(project,
                user.getUsername(), annotationEvents, 10);
        return recentEvents.stream() //
                .filter(Objects::nonNull) //
                // Filter out documents which are not annotatable or curatable
                .filter(event -> {
                    if (CURATION_USER.equals(event.getAnnotator())) {
                        if (!isCurator) {
                            return false;
                        }

                        return curatableSourceDocuments.containsKey(event.getDocument());
                    }
                    else {
                        return annotatableSourceDocuments.containsKey(event.getDocument());
                    }
                })
                // Link events and documents before returning them
                .map(event -> {
                    if (CURATION_USER.equals(event.getAnnotator())) {
                        return new Activity(event,
                                annotatableSourceDocuments.get(event.getDocument()),
                                curationPageMenuItem.getUrl(event.getProject(),
                                        event.getDocument()));
                    }
                    else {
                        return new Activity(event,
                                annotatableSourceDocuments.get(event.getDocument()),
                                annotationPageMenuItem.getUrl(event.getProject(),
                                        event.getDocument()));
                    }
                })//
                .collect(toList());
    }
}

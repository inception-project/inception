/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.ui.core.dashboard.dashlet;

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.NoResultException;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.page.CurationPage;
import de.tudarmstadt.ukp.inception.log.EventRepository;
import de.tudarmstadt.ukp.inception.log.model.LoggedEvent;
import de.tudarmstadt.ukp.inception.support.ui.LinkProvider;

public class ActivitiesDashlet extends DashLet_ImplBase
{
    // annotation events
    public static final String SPAN_CREATED_EVENT = "SpanCreatedEvent";
    public static final String FEATURE_UPDATED_EVENT = "FeatureValueUpdatedEvent";
    public static final String RELATION_CREATED_EVENT = "RelationUpdateEvent";

    private static final int MAX_NUM_ACTIVITIES = 2;

    private static final long serialVersionUID = -2010294259619748756L;
    
    private static final Logger log = LoggerFactory.getLogger(ActivitiesDashlet.class);
    
    private @SpringBean EventRepository eventRepository;
    private @SpringBean UserDao userRepository;
    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;

    private final IModel<Project> projectModel;
    private Set<String> annotationEvents;

    public ActivitiesDashlet(String aId, IModel<Project> aCurrentProject)
    {
        super(aId);
        
        projectModel = aCurrentProject;
        
        if (aCurrentProject == null || aCurrentProject.getObject() == null) {
            return;
        }
        
        annotationEvents = new HashSet<>();
        Collections.addAll(annotationEvents, SPAN_CREATED_EVENT, FEATURE_UPDATED_EVENT, 
                RELATION_CREATED_EVENT);
        
        WebMarkupContainer activitiesList = new WebMarkupContainer("activities",
                new StringResourceModel("activitiesHeading", this));
        activitiesList.setOutputMarkupPlaceholderTag(true);
        
        ListView<LoggedEvent> listView = new ListView<LoggedEvent>("activity",
                LoadableDetachableModel.of(this::listActivities))
        {
            private static final long serialVersionUID = -8613360620764882858L;

            @Override
            protected void populateItem(ListItem<LoggedEvent> aItem)
            {
                SourceDocument document = getSourceDocument(aItem.getModelObject());
                ExternalLink eventLink = createLastActivityLink("eventLink", aItem.getModelObject(),
                        document);
                aItem.add(eventLink);
            }
        };

        add(visibleWhen(() -> !listView.getList().isEmpty()));
        setOutputMarkupPlaceholderTag(true);
        activitiesList.add(listView);
        add(activitiesList);
    }
   
    /**
     * Check that user still has the rights to access the document from the given event
     */
    private boolean isStillAccessibleToUser(LoggedEvent aEvent)
    {
        Project project = projectModel.getObject();
        SourceDocument aDocument = getSourceDocument(aEvent);
        if (aDocument == null || project == null) {
            return false;
        }
        
        User user = userRepository.getCurrentUser();
        
        // the document is in curation and the user is a curator
        if (SourceDocumentState.CURATION_IN_PROGRESS.equals(aDocument.getState()) 
                && projectService.isCurator(project, user)) {
            return true;
        }
        
        return isAnnotationStillPossible(project, aDocument, user);
    }

    /**
     * Check document state and user rights are still valid for annotation
     */
    private boolean isAnnotationStillPossible(Project project, SourceDocument aDocument, User user)
    {
        if (!documentService.existsAnnotationDocument(aDocument, user) 
                || !projectService.isAnnotator(project, user)) {
            return false;
        }
        
        AnnotationDocument annoDocument = documentService.getAnnotationDocument(aDocument, user);
        AnnotationDocumentState annoDocState = annoDocument.getState();

        // check that anno doc exists and user has not finished annotating it
        if (!AnnotationDocumentState.IN_PROGRESS.equals(annoDocState)) {
            log.info(String.format(
                    "Annotation document [%s] in project [%d]] is locked for user [%s]",
                    aDocument.getName(), project.getId(), user.getUsername()));
            return false;
        }
        
        return true;
    }
    
    private SourceDocument getSourceDocument(LoggedEvent aEvent)
    {
        if (aEvent == null) {
            return null;
        }

        long docId = aEvent.getDocument();
        if (docId == -1) {
            return null;
        }

        SourceDocument document = null;
        try {
            document = documentService.getSourceDocument(projectModel.getObject().getId(), docId);
        }
        catch (NoResultException e) {
            log.info(String.format("Source document [%d] no longer exists.", docId));
            return document;
        }

        return document;
    }

    private String formatDateStr(LoggedEvent event)
    {
        String eventDate = event.getCreated().toString();
        String[] times = eventDate.split(":");
        if (times.length >= 2) {
            eventDate = String.join(":", times[0], times[1]);
        }
        return eventDate;
    }
    
    private ExternalLink createLastActivityLink(String aId, LoggedEvent aEvent,
            SourceDocument aDocument)
    {
        if (aEvent == null || aDocument == null) {
            return getDummyLink(aId);
        }
        
        String eventName = aEvent.getEvent();
        if (!annotationEvents.contains(eventName)) {
            log.info(String.format("Unknown last activities event: %s", eventName));
            return getDummyLink(aId);
        }        
       
        Project project = projectModel.getObject();
        Long docId = aDocument.getId();
        String documentName = aDocument.getName();
        String eventDate = formatDateStr(aEvent);
        if (SourceDocumentState.CURATION_IN_PROGRESS.equals(aDocument.getState())) {
            String linkLabel = String.format("%s: Curated \"%s\"", eventDate, documentName);
            return LinkProvider.createDocumentPageLink(project, docId, aId, linkLabel,
                    CurationPage.class);
        }
        else {
            String linkLabel = String.format("%s: Annotated \"%s\"", eventDate, documentName);
            return LinkProvider.createDocumentPageLink(project, docId, aId, linkLabel,
                    AnnotationPage.class);
        }
    }

    private ExternalLink getDummyLink(String aId)
    {
        ExternalLink link = new ExternalLink(aId, "");
        link.setVisible(false);
        return link;
    }

    private List<LoggedEvent> listActivities()
    {
        List<LoggedEvent> events = new ArrayList<>();
        User user = userRepository.getCurrentUser();
        Project project = projectModel.getObject();
        String username = user.getUsername();

        // get last annotation events
        events.addAll(eventRepository.listUniqueLoggedEventsForDoc(project, username,
                annotationEvents.toArray(new String[annotationEvents.size()]), MAX_NUM_ACTIVITIES));

        // return filtered by user rights and document state
        return events.stream().filter(event -> isStillAccessibleToUser(event))
                .collect(Collectors.toList());
    }
}

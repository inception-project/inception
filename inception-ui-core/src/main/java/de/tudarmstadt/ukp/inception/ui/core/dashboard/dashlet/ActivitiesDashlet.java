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

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PAGE_PARAM_DOCUMENT_ID;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PAGE_PARAM_PROJECT_ID;

import java.util.List;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaStatelessLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.inception.log.EventRepository;
import de.tudarmstadt.ukp.inception.log.model.LoggedEvent;


public class ActivitiesDashlet extends Dashlet_ImplBase
{
    private static final long serialVersionUID = -2010294259619748756L;
    
    private @SpringBean EventRepository eventRepository;
    private @SpringBean UserDao userRepository;
    private @SpringBean DocumentService documentService;
    
    private final IModel<Project> projectModel;
    

    public ActivitiesDashlet(String aId, IModel<Project> aCurrentProject)
    {
        super(aId);
        projectModel = aCurrentProject;
        WebMarkupContainer activitiesList = new WebMarkupContainer("activities",
                new StringResourceModel("activitiesHeading", this));
        ListView<LoggedEvent> listview = new ListView<LoggedEvent>("activity",
                LoadableDetachableModel.of(this::listActivities))
        {
            private static final long serialVersionUID = -8613360620764882858L;

            @Override
            protected void populateItem(ListItem<LoggedEvent> aItem)
            {
                LambdaStatelessLink eventLink = new LambdaStatelessLink("eventLink",
                    () -> openDocument(aItem.getModelObject()));
                eventLink.add(new Label("eventName", getEventDescription(aItem)));
                aItem.add(eventLink);
            }
        };
        add(activitiesList);
        activitiesList.add(listview);
    }
    
    private String getEventDescription(ListItem<LoggedEvent> aItem) {
        //return aItem.getModelObject().getEvent()
        String documentName = documentService.getSourceDocument(projectModel.getObject().getId(), 
                aItem.getModelObject().getDocument()).getName();
        return String.format("Annotated in document %s", documentName);
    }
    
    private void openDocument(LoggedEvent aEvent)
    {
        PageParameters params = new PageParameters();
        params.add(PAGE_PARAM_PROJECT_ID, projectModel.getObject().getId());
        params.add(PAGE_PARAM_DOCUMENT_ID, aEvent.getDocument());
        setResponsePage(AnnotationPage.class, params);
    }

    private List<LoggedEvent> listActivities()
    {
        User user = userRepository.getCurrentUser();
        Project project = projectModel.getObject();
        String annotationEvent = "AfterAnnotationUpdateEvent";
        List<LoggedEvent> events = eventRepository.listLoggedEventsForEventType(project,
                user.getUsername(), annotationEvent, 1);
        return events;
    }
    


}

package de.tudarmstadt.ukp.inception.ui.core.dashboard.dashlet;

import java.util.List;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaStatelessLink;
import de.tudarmstadt.ukp.inception.log.EventRepository;
import de.tudarmstadt.ukp.inception.log.model.LoggedEvent;


public class ActivitiesDashlet extends Dashlet_ImplBase
{
    private static final long serialVersionUID = -2010294259619748756L;
    
    private @SpringBean EventRepository eventRepository;
    private @SpringBean UserDao userRepository;
    
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
                eventLink.add(new Label("eventName", aItem.getModelObject().getEvent()));
                aItem.add(eventLink);
            }
        };
        add(activitiesList);
        activitiesList.add(listview);
    }
    
    private void openDocument(LoggedEvent aModelObject)
    {
        // TODO Auto-generated method stub
        System.out.println("Open document");
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

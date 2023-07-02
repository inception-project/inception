package de.tudarmstadt.ukp.inception.ui.core.dashboard.projectlist;

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;

import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public class ProjectsOverviewPageMenuBarItem
    extends Panel
{
    private static final long serialVersionUID = 8794033673959375712L;

    private static final String CID_PROJECTS_LINK = "projectsLink";

    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;

    private IModel<User> user;

    public ProjectsOverviewPageMenuBarItem(String aId)
    {
        super(aId);

        user = LoadableDetachableModel.of(userRepository::getCurrentUser);

        add(new BookmarkablePageLink<>(CID_PROJECTS_LINK, ProjectsOverviewPage.class)
                .add(visibleWhen(user.map(this::requiresProjectsOverview).orElse(false))));
    }

    private boolean requiresProjectsOverview(User aUser)
    {
        return userRepository.isAdministrator(aUser) || userRepository.isProjectCreator(aUser)
                || projectService.listAccessibleProjects(aUser).size() > 1;
    }
}

package de.tudarmstadt.ukp.inception.ui.core.dashboard.admin;

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;

import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public class AdminDashboardPageMenuBarItem
    extends Panel
{
    private static final long serialVersionUID = 7486091139970717604L;

    private static final String CID_ADMIN_LINK = "adminLink";

    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;

    private IModel<User> user;

    public AdminDashboardPageMenuBarItem(String aId)
    {
        super(aId);

        user = LoadableDetachableModel.of(userRepository::getCurrentUser);

        add(new BookmarkablePageLink<>(CID_ADMIN_LINK, AdminDashboardPage.class)
                .add(visibleWhen(user.map(userRepository::isAdministrator).orElse(false))));
    }
}

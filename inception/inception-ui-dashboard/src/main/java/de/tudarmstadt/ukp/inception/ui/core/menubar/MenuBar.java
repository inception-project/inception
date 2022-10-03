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
package de.tudarmstadt.ukp.inception.ui.core.menubar;

import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_REMOTE;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.support.help.DocLink.Book.USER_GUIDE;

import java.util.MissingResourceException;

import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.UrlResourceReference;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.ImageLinkDecl;
import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.ImageLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.logout.LogoutPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectContext;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.support.help.DocLink;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.admin.AdminDashboardPage;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.project.ProjectDashboardPage;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.projectlist.ProjectsOverviewPage;

public class MenuBar
    extends Panel
{
    private static final long serialVersionUID = -8018701379688272826L;

    private static final String CID_HOME_LINK = "homeLink";
    private static final String CID_HELP_LINK = "helpLink";
    private static final String CID_ADMIN_LINK = "adminLink";
    private static final String CID_PROJECTS_LINK = "projectsLink";
    private static final String CID_DASHBOARD_LINK = "dashboardLink";
    private static final String CID_SWAGGER_LINK = "swaggerLink";

    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectService projectService;

    private IModel<User> user;
    private IModel<Project> project;
    private ExternalLink helpLink;
    private ListView<ImageLinkDecl> links;

    public MenuBar(String aId)
    {
        super(aId);
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        user = LoadableDetachableModel.of(userRepository::getCurrentUser);
        project = LoadableDetachableModel.of(() -> findParent(ProjectContext.class)) //
                .map(ProjectContext::getProject);

        add(new LogoutPanel("logoutPanel", user));

        helpLink = new DocLink(CID_HELP_LINK, USER_GUIDE, new ResourceModel("page.help.link", ""));
        helpLink.setBody(Model.of("<i class=\"fas fa-question-circle\"></i>"
                + " <span class=\"nav-link active p-0 d-none d-lg-inline\">Help</span>"));
        helpLink.add(visibleWhen(this::isPageHelpAvailable));
        add(helpLink);

        links = new ListView<ImageLinkDecl>("links", SettingsUtil.getLinks())
        {
            private static final long serialVersionUID = 1768830545639450786L;

            @Override
            protected void populateItem(ListItem<ImageLinkDecl> aItem)
            {
                aItem.add(new ImageLink("link",
                        new UrlResourceReference(Url.parse(aItem.getModelObject().getImageUrl())),
                        Model.of(aItem.getModelObject().getLinkUrl())));
            }
        };
        add(links);

        add(new BookmarkablePageLink<>(CID_HOME_LINK, getApplication().getHomePage()));

        BookmarkablePageLink<Void> dashboardLink = new BookmarkablePageLink<>(CID_DASHBOARD_LINK,
                ProjectDashboardPage.class)
        {
            private static final long serialVersionUID = -8735871053513210365L;

            public PageParameters getPageParameters()
            {
                PageParameters pageParameters = new PageParameters();
                if (project.isPresent().getObject()) {
                    ProjectPageBase.setProjectPageParameter(pageParameters, project.getObject());
                }
                else {
                    pageParameters.set(ProjectPageBase.PAGE_PARAM_PROJECT, -1);
                }

                return pageParameters;
            }
        };
        dashboardLink.add(visibleWhen(user.map(this::userCanAccessProject).orElse(false)));
        add(dashboardLink);

        add(new BookmarkablePageLink<>(CID_PROJECTS_LINK, ProjectsOverviewPage.class)
                .add(visibleWhen(user.map(this::requiresProjectsOverview).orElse(false))));

        add(new BookmarkablePageLink<>(CID_ADMIN_LINK, AdminDashboardPage.class)
                .add(visibleWhen(user.map(userRepository::isAdministrator).orElse(false))));

        add(new ExternalLink(CID_SWAGGER_LINK, LoadableDetachableModel.of(this::getSwaggerUiUrl))
                .add(visibleWhen(
                        user.map(u -> userRepository.hasRole(u, ROLE_REMOTE)).orElse(false))));
    }

    private String getSwaggerUiUrl()
    {
        return RequestCycle.get().getUrlRenderer().renderContextRelativeUrl("/swagger-ui.html");
    }

    private boolean isPageHelpAvailable()
    {
        try {
            // Trying to access the resources - if we can, then we show the link, but if
            // we fail, then we hide the link
            getString("page.help.link");
            return true;
        }
        catch (MissingResourceException e) {
            return false;
        }
    }

    private boolean userCanAccessProject(User aUser)
    {
        if (!project.isPresent().getObject() || !user.isPresent().getObject()) {
            return false;
        }

        // Project has not been saved yet (happens when creating a project)
        if (project.getObject().getId() == null) {
            return false;
        }

        return projectService.hasAnyRole(aUser, project.getObject());
    }

    private boolean requiresProjectsOverview(User aUser)
    {
        return userRepository.isAdministrator(aUser) || userRepository.isProjectCreator(aUser)
                || projectService.listAccessibleProjects(aUser).size() > 1;
    }
}

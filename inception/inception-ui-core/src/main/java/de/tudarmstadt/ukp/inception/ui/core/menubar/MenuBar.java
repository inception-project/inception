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

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.stream.Collectors.toList;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.resource.UrlResourceReference;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.logout.LogoutPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectContext;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.support.SettingsUtil;
import de.tudarmstadt.ukp.inception.support.help.ImageLinkDecl;
import de.tudarmstadt.ukp.inception.support.wicket.ImageLink;
import de.tudarmstadt.ukp.inception.ui.core.config.DashboardProperties;

public class MenuBar
    extends Panel
{
    private static final long serialVersionUID = -8018701379688272826L;

    private static final String CID_HOME_LINK = "homeLink";

    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectService projectService;
    private @SpringBean DashboardProperties dashboardProperties;
    private @SpringBean MenuBarItemRegistry menuBarItemRegistry;

    private IModel<User> user;
    private IModel<Project> project;

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

        add(visibleWhen(this::isMenubarVisibleToCurrentUser));

        add(new BookmarkablePageLink<>(CID_HOME_LINK, getApplication().getHomePage()));
        add(createMenuItems("leftItems", MenuBarItemJusification.LEFT));
        add(createLinks("links"));
        add(createMenuItems("rightItems", MenuBarItemJusification.RIGHT));
        add(new LogoutPanel("logoutPanel", user));
    }

    private ListView<ImageLinkDecl> createLinks(String aId)
    {
        return new ListView<ImageLinkDecl>(aId, SettingsUtil.getLinks())
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
    }

    private ListView<Component> createMenuItems(String aId, MenuBarItemJusification aJustification)
    {
        var menuItems = new ListModel<>(menuBarItemRegistry.getExtensions(getPage()).stream() //
                .filter(c -> c.getJustification() == aJustification) //
                .map(c -> c.create("item")) //
                .collect(toList()));

        return new ListView<Component>(aId, menuItems)
        {
            private static final long serialVersionUID = 4982007173719441739L;

            @Override
            protected void populateItem(ListItem<Component> aItem)
            {
                aItem.add(aItem.getModelObject());
            }
        };
    }

    private boolean isMenubarVisibleToCurrentUser()
    {
        // When we have no project, we would like to show the menubar, e.g. on the
        // - project overview page
        // When we have no user, we would also like to show the the empty menu bar saying
        // "INCEpTION", e.g. on the:
        // - invite page
        if (!user.isPresent().getObject() || !project.isPresent().getObject()) {
            return true;
        }

        if (userRepository.isAdministrator(user.getObject())
                || userRepository.isProjectCreator(user.getObject())) {
            return true;
        }

        var eligibleRoles = dashboardProperties.getAccessibleByRoles()
                .toArray(PermissionLevel[]::new);

        // If we do not know the current project, we make the menu accessible if the user has an
        // eligible role in any project - they could then use the menubar to switch to a project
        // where they have more permissions.
        // The project might be null if it is in the process of being created. Normally, this can
        // only be done by admin and project creators that are handled above - so returning false
        // here is really just a sanity fallback that should never kick in.
        // if (!project.isPresent().getObject() || project.getObject().getId() == null) {
        // return projectService.hasRoleInAnyProject(user.getObject(), MANAGER, eligibleRoles);
        // }

        // If we know the project, we show the menubar if the user has an eligible role
        return projectService.hasRole(user.getObject(), project.getObject(), MANAGER,
                eligibleRoles);
    }
}

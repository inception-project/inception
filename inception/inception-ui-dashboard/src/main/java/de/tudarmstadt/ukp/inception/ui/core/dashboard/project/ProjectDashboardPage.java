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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.project;

import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItem;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItemRegistry;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.ui.core.config.DashboardProperties;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.DashboardMenu;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.dashlet.CurrentProjectDashlet;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.dashlet.ProjectDashboardDashletExtension;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.dashlet.ProjectDashboardDashletExtensionPoint;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

/**
 * Project dashboard page
 */
@MountPath(NS_PROJECT + "/${" + PAGE_PARAM_PROJECT + "}")
public class ProjectDashboardPage
    extends ProjectPageBase
{
    private static final long serialVersionUID = -2487663821276301436L;

    private static final String MID_MENU = "menu";
    private static final String MID_RIGHT_SIDEBAR_DASHLETS = "rightSidebarDashlets";
    private static final String MID_CURRENT_PROJECT_DASHLET = "currentProjectDashlet";

    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;
    private @SpringBean MenuItemRegistry menuItemService;
    private @SpringBean WorkloadManagementService workloadService;
    private @SpringBean ProjectDashboardDashletExtensionPoint dashletRegistry;
    private @SpringBean DashboardProperties dashboardProperties;

    public ProjectDashboardPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);

        setStatelessHint(true);
        setVersioned(false);

        var currentUser = userRepository.getCurrentUser();

        if (!userRepository.isAdministrator(currentUser)) {
            requireProjectRole(currentUser, PermissionLevel.MANAGER,
                    dashboardProperties.getAccessibleByRoles().toArray(PermissionLevel[]::new));
        }

        add(new DashboardMenu(MID_MENU, LoadableDetachableModel.of(this::getMenuItems)));
        add(new CurrentProjectDashlet(MID_CURRENT_PROJECT_DASHLET, getProjectModel()));
        add(createRightSidebarDashlets(MID_RIGHT_SIDEBAR_DASHLETS));
    }

    private Component createRightSidebarDashlets(String aId)
    {
        var sidebarModel = LoadableDetachableModel.of(dashletRegistry::getExtensions);

        var sidebar = new ListView<ProjectDashboardDashletExtension>(aId, sidebarModel)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<ProjectDashboardDashletExtension> aItem)
            {
                aItem.add(aItem.getModelObject().createDashlet("dashlet", getProjectModel()));
            }
        };

        sidebar.add(visibleWhen(sidebarModel.map(dashlets -> !dashlets.isEmpty())));

        return sidebar;
    }

    @Override
    public void backToProjectPage()
    {
        // If accessing the project dashboard is not possible, we need to jump back up to the
        // project overview page. This is called e.g. by requireProjectRole()
        throw new RestartResponseException(getApplication().getHomePage());
    }

    public void setModel(IModel<Project> aModel)
    {
        setDefaultModel(aModel);
    }

    private List<MenuItem> getMenuItems()
    {
        return menuItemService.getMenuItems().stream() //
                .filter(item -> item.getPath().matches("/[^/]+")) //
                .toList();
    }
}

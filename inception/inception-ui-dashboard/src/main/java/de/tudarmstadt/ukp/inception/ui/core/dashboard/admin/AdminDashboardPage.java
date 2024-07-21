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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.admin;

import java.util.List;

import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.login.LoginPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItem;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItemRegistry;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.DashboardMenu;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.admin.dashlet.SystemStatusDashlet;

/**
 * Admin menu page.
 */
@MountPath(value = "/manage/overview.html")
public class AdminDashboardPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = -2487663821276301436L;

    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;
    private @SpringBean MenuItemRegistry menuItemService;

    public AdminDashboardPage()
    {
        setStatelessHint(true);
        setVersioned(false);

        // In case we restore a saved session, make sure the user actually still exists in the DB.
        // redirect to login page (if no user is found, admin/admin will be created)
        User user = userRepository.getCurrentUser();
        if (user == null) {
            setResponsePage(LoginPage.class);
        }

        // Admins need access to the admin area to manage projects
        if (!userRepository.isAdministrator(user)) {
            setResponsePage(getApplication().getHomePage());
        }

        add(new DashboardMenu("menu", LoadableDetachableModel.of(this::getMenuItems)));

        add(new SystemStatusDashlet("systemStatusDashlet"));
    }

    private List<MenuItem> getMenuItems()
    {
        return menuItemService.getMenuItems().stream() //
                .filter(item -> item.getPath().matches("/admin/[^/]+")) //
                .toList();
    }
}

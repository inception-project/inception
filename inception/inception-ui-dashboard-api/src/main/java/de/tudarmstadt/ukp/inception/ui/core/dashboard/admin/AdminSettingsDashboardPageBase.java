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

import static java.lang.String.format;

import java.util.List;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItem;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItemRegistry;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.DashboardMenu;

public class AdminSettingsDashboardPageBase
    extends ApplicationPageBase
{
    private static final long serialVersionUID = 1938235814084993286L;

    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userService;
    private @SpringBean MenuItemRegistry menuItemService;
    private @SpringBean PreferencesService userPrefService;

    public AdminSettingsDashboardPageBase()
    {
        if (!userService.isCurrentUserAdmin()) {
            denyAccess();
        }
    }

    public AdminSettingsDashboardPageBase(final PageParameters aParameters)
    {
        super(aParameters);

        if (!userService.isCurrentUserAdmin()) {
            denyAccess();
        }
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        add(new DashboardMenu("menu", LoadableDetachableModel.of(this::getMenuItems)));
    }

    private List<MenuItem> getMenuItems()
    {
        return menuItemService.getMenuItems().stream() //
                .filter(item -> item.getPath().matches("/admin/[^/]+")) //
                .toList();
    }

    private void denyAccess()
    {
        getSession().error(format("Access to [%s] denied.", getClass().getSimpleName()));
        throw new RestartResponseException(getApplication().getHomePage());
    }
}

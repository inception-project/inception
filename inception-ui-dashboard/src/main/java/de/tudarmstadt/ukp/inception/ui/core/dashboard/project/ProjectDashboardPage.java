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

import static de.tudarmstadt.ukp.clarin.webanno.api.SecurityUtil.annotationEnabeled;
import static de.tudarmstadt.ukp.clarin.webanno.api.SecurityUtil.curationEnabeled;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.login.LoginPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItem;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItemRegistry;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.DashboardMenu;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.dashlet.ActivitiesDashlet;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.dashlet.CurrentProjectDashlet;

/**
 * Project dashboard page
 */
@MountPath(NS_PROJECT + "/${" + PAGE_PARAM_PROJECT + "}")
public class ProjectDashboardPage
    extends ProjectPageBase
{
    private static final long serialVersionUID = -2487663821276301436L;

    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;
    private @SpringBean MenuItemRegistry menuItemService;

    private DashboardMenu menu;
    
    public ProjectDashboardPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);

        User currentUser = userRepository.getCurrentUser();
        
        if (!userRepository.isAdministrator(currentUser)) {
            requireProjectRole(currentUser);
        }
                
        setStatelessHint(true);
        setVersioned(false);

        // In case we restore a saved session, make sure the user actually still exists in the DB.
        // redirect to login page (if no usr is found, admin/admin will be created)
        User user = userRepository.getCurrentUser();
        if (user == null) {
            setResponsePage(LoginPage.class);
        }

        // if not either a curator or annotator, display warning message
        if (!annotationEnabeled(projectService, user)
                && !curationEnabeled(projectService, user)) {
            info("You are not member of any projects to annotate or curate");
        }

        menu = new DashboardMenu("menu", LoadableDetachableModel.of(this::getMenuItems));
        add(menu);

        add(new CurrentProjectDashlet("currentProjectDashlet", Model.of(getProject())));
        add(new ActivitiesDashlet("activitiesDashlet", Model.of(getProject())));
    }

    public void setModel(IModel<Project> aModel) {
        setDefaultModel(aModel);
    }
    
    private List<MenuItem> getMenuItems()
    {
        return menuItemService.getMenuItems().stream()
                .filter(item -> item.getPath().matches("/[^/]+")).collect(Collectors.toList());
    }
}

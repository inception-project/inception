/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.project;

import static de.tudarmstadt.ukp.clarin.webanno.api.SecurityUtil.annotationEnabeled;
import static de.tudarmstadt.ukp.clarin.webanno.api.SecurityUtil.curationEnabeled;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PAGE_PARAM_PROJECT_ID;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PROJECT_TYPE_ANNOTATION;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PROJECT_TYPE_AUTOMATION;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PROJECT_TYPE_CORRECTION;
import static de.tudarmstadt.ukp.inception.ui.core.session.SessionMetaData.CURRENT_PROJECT;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.Session;
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
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.DashboardMenu;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.dashlet.ActivitiesDashlet;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.dashlet.CurrentProjectDashlet;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.projectlist.ProjectsOverviewPage;

/**
 * Project dashboard page
 */
@MountPath(value = "/project/${" + PAGE_PARAM_PROJECT_ID + "}" )
public class ProjectDashboardPage
    extends ApplicationPageBase
{
    // Page parameters
    public static final String PAGE_PARAM_PROJECT_ID = "p";
     
    private static final long serialVersionUID = -2487663821276301436L;

    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;
    private @SpringBean MenuItemRegistry menuItemService;

    private DashboardMenu menu;

    public ProjectDashboardPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);
        
        User currentUser = userRepository.getCurrentUser();
        
        // Check if use can access the project
        Project project = projectService.listAccessibleProjects(currentUser).stream()
                .filter(p -> p.getId().equals(
                        aPageParameters.get(PAGE_PARAM_PROJECT_ID).toOptionalLong()))
                .findFirst()
                .orElse(null);

        // If the user has no access, send the user back to the overview page
        if (project == null) {
            setResponsePage(ProjectsOverviewPage.class);
        }
        
        // Otherwise make the project the current project
        Session.get().setMetaData(CURRENT_PROJECT, project);
        
        commonInit();
    }
    
    public ProjectDashboardPage()
    {
        commonInit();
    }
    
    protected void commonInit()
    {
        setStatelessHint(true);
        setVersioned(false);
        
        // In case we restore a saved session, make sure the user actually still exists in the DB.
        // redirect to login page (if no usr is found, admin/admin will be created)
        User user = userRepository.getCurrentUser();
        if (user == null) {
            setResponsePage(LoginPage.class);
        }
        
        // If no project has been selected yet, redirect to the project overview page. This allows
        // us to keep the ProjectDashboardPage as the application home page.
        Project project = Session.get().getMetaData(CURRENT_PROJECT);
        if (project == null) {
            setResponsePage(ProjectsOverviewPage.class);
        }
        
        // if not either a curator or annotator, display warning message
        if (
                !annotationEnabeled(projectService, user, PROJECT_TYPE_ANNOTATION) && 
                !annotationEnabeled(projectService, user, PROJECT_TYPE_AUTOMATION) && 
                !annotationEnabeled(projectService, user, PROJECT_TYPE_CORRECTION) && 
                !curationEnabeled(projectService, user)) 
        {
            info("You are not member of any projects to annotate or curate");
        }
        
        menu = new DashboardMenu("menu", LoadableDetachableModel.of(this::getMenuItems));
        add(menu);
        
        Model<Project> currentProject = Model.of(getProject());
        add(new CurrentProjectDashlet("currentProjectDashlet", currentProject));
        add(new ActivitiesDashlet("activitiesDashlet", currentProject));
    }
    
    /**
     * Look up project in session but get fresh info from the DB in case the session object is
     * stale (e.g. if the title/description has changed).
     */
    private Project getProject()
    {
        Project project = Session.get().getMetaData(CURRENT_PROJECT);
        if (project == null) {
            return null;
        }
        
        return projectService.getProject(project.getId());
    }
    
    private List<MenuItem> getMenuItems()
    {
        return menuItemService.getMenuItems().stream()
                .filter(item -> item.getPath().matches("/[^/]+"))
                .collect(Collectors.toList());
    }
}

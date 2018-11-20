/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.projectlist;

import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_ADMIN;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_PROJECT_CREATOR;
import static de.tudarmstadt.ukp.inception.ui.core.session.SessionMetaData.CURRENT_PROJECT;
import static org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy.authorize;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.lang.Classes;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaStatelessLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.project.ProjectPage;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.project.ProjectDashboardPage;

@MountPath(value = "/projects.html")
public class ProjectsOverviewPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = -2159246322262294746L;

    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;
    
    public ProjectsOverviewPage()
    {
        add(createProjectList());
        
        add(new LambdaAjaxLink("newProject", _target -> {
            PageParameters params = new PageParameters();
            params.set(WebAnnoConst.PAGE_PARAM_PROJECT_ID, ProjectPage.NEW_PROJECT_ID);
            setResponsePage(ProjectPage.class, params);
        }));
        
        ProjectImportPanel importProjectPanel = new ProjectImportPanel("importPanel");
        add(importProjectPanel);
        authorize(importProjectPanel, Component.RENDER,
                String.join(",", ROLE_ADMIN.name(), ROLE_PROJECT_CREATOR.name()));
    }
    
    private ListView<Project> createProjectList()
    {
        List<Project> projects = listProjects();
        
        if (projects.isEmpty()) {
            warn("There are no projects accessible to you.");
        }
        
        return new ListView<Project>("project", projects)
        {
            private static final long serialVersionUID = -755155675319764642L;

            @Override
            protected void populateItem(ListItem<Project> aItem)
            {
                LambdaStatelessLink projectLink = new LambdaStatelessLink("projectLink", () -> 
                        selectProject(aItem.getModelObject()));
                projectLink.add(new Label("name", aItem.getModelObject().getName()));
                aItem.add(projectLink);
                aItem.add(createRoleBadges(aItem.getModelObject()));
            }
        };
    }
    
    private ListView<ProjectPermission> createRoleBadges(Project aProject)
    {
        return new ListView<ProjectPermission>("role", projectService
                .listProjectPermissionLevel(userRepository.getCurrentUser(), aProject))
        {
            private static final long serialVersionUID = -96472758076828409L;

            @Override
            protected void populateItem(ListItem<ProjectPermission> aItem)
            {
                PermissionLevel level = aItem.getModelObject().getLevel();
                aItem.add(new Label("label", getString(
                        Classes.simpleName(level.getDeclaringClass()) + '.' + level.getName())));
            }
        };
    }
    
    private void selectProject(Project aProject)
    {
        Session.get().setMetaData(CURRENT_PROJECT, aProject);
        setResponsePage(ProjectDashboardPage.class);
    }
    
    private List<Project> listProjects()
    {
        return projectService.listAccessibleProjects(userRepository.getCurrentUser());
    }
}

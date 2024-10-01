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
package de.tudarmstadt.ukp.clarin.webanno.ui.core.page;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

import java.util.stream.Stream;

import org.apache.wicket.Page;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.util.lang.WicketObjects;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValueConversionException;

import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.ui.core.AccessDeniedPage;
import de.tudarmstadt.ukp.inception.ui.core.config.DashboardProperties;
import jakarta.persistence.NoResultException;

public abstract class ProjectPageBase
    extends ApplicationPageBase
    implements ProjectContext
{
    private static final long serialVersionUID = -5616613971979378668L;

    public static final String NS_PROJECT = "/p";
    public static final String PAGE_PARAM_PROJECT = "p";

    private @SpringBean ProjectService projectService;
    private @SpringBean DashboardProperties dashboardProperties;
    private @SpringBean UserDao userService;
    private IModel<Project> projectModel;

    public ProjectPageBase(final PageParameters aParameters)
    {
        super(aParameters);

        if (getProjectModel().getObject() == null) {
            getSession().error(
                    format("[%s] requires a project to be selected", getClass().getSimpleName()));
            throw new RestartResponseException(getApplication().getHomePage());
        }
    }

    protected final void requireAnyProjectRole(User aUser)
    {
        var project = getProjectModel().getObject();

        if (aUser == null || !projectService.hasAnyRole(aUser, project)) {
            getSession().error(format("To access the [%s] you need to be a member of the project",
                    getClass().getSimpleName()));

            backToProjectPage();
        }
    }

    protected final void requireProjectRole(User aUser, PermissionLevel aRole,
            PermissionLevel... aMoreRoles)
    {
        var project = getProjectModel().getObject();

        // Check access to project
        if (aUser == null || !projectService.hasRole(aUser, project, aRole, aMoreRoles)) {
            var roles = Stream.concat(Stream.of(aRole), Stream.of(aMoreRoles)).distinct();
            getSession().error(format("To access the [%s] you require any of these roles: [%s]",
                    getClass().getSimpleName(),
                    roles.map(PermissionLevel::getName).collect(joining(", "))));

            backToProjectPage();
        }
    }

    public void backToProjectPage()
    {
        // If the current user cannot access the dashboard, send them to an access denied page
        if (!projectService.hasRole(userService.getCurrentUsername(), getProject(), MANAGER,
                dashboardProperties.getAccessibleByRoles().toArray(PermissionLevel[]::new))) {
            getRequestCycle().find(AjaxRequestTarget.class)
                    .ifPresent(_target -> _target.addChildren(getPage(), IFeedback.class));
            throw new RestartResponseException(AccessDeniedPage.class);
        }

        var pageParameters = new PageParameters();
        setProjectPageParameter(pageParameters, getProject());
        Class<? extends Page> projectDashboard = WicketObjects.resolveClass(
                "de.tudarmstadt.ukp.inception.ui.core.dashboard.project.ProjectDashboardPage");
        throw new RestartResponseException(projectDashboard, pageParameters);
    }

    protected void setProjectModel(IModel<Project> aModel)
    {
        projectModel = aModel;
    }

    @Override
    public Project getProject()
    {
        return getProjectModel().getObject();
    }

    public IModel<Project> getProjectModel()
    {
        if (projectModel == null) {
            projectModel = LoadableDetachableModel.of(this::getProjectFromParameters);
        }

        return projectModel;
    }

    public Project getProjectFromParameters()
    {
        return getProjectFromParameters(this, projectService);
    }

    public static Project getProjectFromParameters(Page aPage, ProjectService aProjectService)
    {
        var projectParameter = aPage.getPageParameters().get(PAGE_PARAM_PROJECT);

        if (projectParameter.isEmpty()) {
            return null;
        }

        try {
            try {
                return aProjectService.getProject(projectParameter.toLong());
            }
            catch (StringValueConversionException e) {
                // Ignore lookup by ID and try lookup by slug instead.
            }

            return aProjectService.getProjectBySlug(projectParameter.toString());
        }
        catch (NoResultException e) {
            aPage.getSession().error("Project [" + projectParameter + "] does not exist");
            throw new RestartResponseException(aPage.getApplication().getHomePage());
        }
    }

    public static void setProjectPageParameter(PageParameters aParameters, Project aProject)
    {
        if (aProject.getSlug() != null) {
            aParameters.set(PAGE_PARAM_PROJECT, aProject.getSlug());
        }
        else {
            aParameters.set(PAGE_PARAM_PROJECT, aProject.getId());
        }
    }
}

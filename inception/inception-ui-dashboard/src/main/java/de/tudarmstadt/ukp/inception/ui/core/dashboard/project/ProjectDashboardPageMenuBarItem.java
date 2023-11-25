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

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;

import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectContext;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;

public class ProjectDashboardPageMenuBarItem
    extends Panel
{
    private static final long serialVersionUID = 8794033673959375712L;

    private static final String CID_DASHBOARD_LINK = "dashboardLink";

    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;

    private IModel<User> user;
    private IModel<Project> project;

    public ProjectDashboardPageMenuBarItem(String aId)
    {
        super(aId);

        user = LoadableDetachableModel.of(userRepository::getCurrentUser);
        project = LoadableDetachableModel.of(() -> findParent(ProjectContext.class)) //
                .map(ProjectContext::getProject);

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
    }

    private boolean userCanAccessProject(User aUser)
    {
        if (!project.isPresent().getObject()) {
            return false;
        }

        // Project has not been saved yet (happens when creating a project)
        if (project.getObject().getId() == null) {
            return false;
        }

        return projectService.hasAnyRole(aUser, project.getObject());
    }
}

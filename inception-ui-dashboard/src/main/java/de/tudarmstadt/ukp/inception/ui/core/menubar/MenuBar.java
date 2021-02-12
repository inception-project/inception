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

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;

import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectContext;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.admin.AdminDashboardPage;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.project.ProjectDashboardPage;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.projectlist.ProjectsOverviewPage;

public class MenuBar
    extends de.tudarmstadt.ukp.clarin.webanno.ui.core.page.MenuBar
{
    private static final long serialVersionUID = -8018701379688272826L;

    private static final String CID_HOME_LINK = "homeLink";
    private static final String CID_ADMIN_LINK = "adminLink";
    private static final String CID_PROJECTS_LINK = "projectsLink";
    private static final String CID_DASHBOARD_LINK = "dashboardLink";

    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectService projectService;

    public MenuBar(String aId)
    {
        super(aId);
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        add(new BookmarkablePageLink<>(CID_HOME_LINK, getApplication().getHomePage()));

        ProjectContext projectContext = findParent(ProjectContext.class);
        if (projectContext != null && projectContext.getProject() != null
                && projectContext.getProject().getId() != null) {
            long projectId = projectContext.getProject().getId();
            add(new BookmarkablePageLink<>(CID_DASHBOARD_LINK, ProjectDashboardPage.class,
                    new PageParameters().set(PAGE_PARAM_PROJECT, projectId)));
        }
        else {
            add(new BookmarkablePageLink<>(CID_DASHBOARD_LINK, ProjectDashboardPage.class)
                    .setVisible(false));
        }

        add(new BookmarkablePageLink<>(CID_PROJECTS_LINK, ProjectsOverviewPage.class)
                .add(visibleWhen(() -> userRepository.getCurrentUser() != null)));

        add(new BookmarkablePageLink<>(CID_ADMIN_LINK, AdminDashboardPage.class)
                .add(visibleWhen(this::adminAreaAccessRequired)));
    }

    private boolean adminAreaAccessRequired()
    {
        return userRepository.getCurrentUser() != null
                && AdminDashboardPage.adminAreaAccessRequired(userRepository, projectService);
    }
}

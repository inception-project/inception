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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.projectlist;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;

import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;

public class ProjectsOverviewPageMenuBarItem
    extends Panel
{
    private static final long serialVersionUID = 8794033673959375712L;

    private static final String CID_PROJECTS_LINK = "projectsLink";

    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;

    private IModel<User> user;

    public ProjectsOverviewPageMenuBarItem(String aId)
    {
        super(aId);

        user = LoadableDetachableModel.of(userRepository::getCurrentUser);

        add(new BookmarkablePageLink<>(CID_PROJECTS_LINK, ProjectsOverviewPage.class)
                .add(visibleWhen(user.map(this::requiresProjectsOverview).orElse(false))));
    }

    private boolean requiresProjectsOverview(User aUser)
    {
        return userRepository.isAdministrator(aUser) || userRepository.isProjectCreator(aUser)
                || projectService.listAccessibleProjects(aUser).size() > 1;
    }
}

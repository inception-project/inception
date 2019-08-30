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
package de.tudarmstadt.ukp.inception.ui.core.menubar;

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;

import org.apache.wicket.Session;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.admin.AdminDashboardPage;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.project.ProjectDashboardPage;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.projectlist.ProjectsOverviewPage;
import de.tudarmstadt.ukp.inception.ui.core.session.SessionMetaData;

public class MenuBar
    extends de.tudarmstadt.ukp.clarin.webanno.ui.core.page.MenuBar
{
    private static final long serialVersionUID = -8018701379688272826L;

    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectService projectService;

    public MenuBar(String aId)
    {
        super(aId);
        
        add(new BookmarkablePageLink<>("homeLink", getApplication().getHomePage()));

        add(new BookmarkablePageLink<>("dashboardLink", ProjectDashboardPage.class)
                .add(visibleWhen(() -> 
                        Session.get().getMetaData(SessionMetaData.CURRENT_PROJECT) != null)));

        add(new BookmarkablePageLink<>("projectsLink", ProjectsOverviewPage.class)
                .add(visibleWhen(() -> userRepository.getCurrentUser() != null)));

        add(new BookmarkablePageLink<>("adminLink", AdminDashboardPage.class)
                .add(visibleWhen(this::adminAreaAccessRequired)));
    }
    
    private boolean adminAreaAccessRequired()
    {
        return userRepository.getCurrentUser() != null && AdminDashboardPage
                .adminAreaAccessRequired(userRepository, projectService);
    }
}

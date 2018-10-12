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

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaStatelessLink;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.AdminDashboardPage;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.ProjectsOverviewPage;

public class MenuBar
    extends de.tudarmstadt.ukp.clarin.webanno.ui.core.page.MenuBar
{
    private static final long serialVersionUID = -8018701379688272826L;

    private static final Logger LOG = LoggerFactory.getLogger(MenuBar.class);

    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectService projectService;

    private DropDownChoice<Project> project;

    public MenuBar(String aId)
    {
        super(aId);
        
//        project = new DropDownChoice<Project>("project");
//        project.add(LambdaBehavior.onConfigure(_this -> 
//                _this.setVisible(AuthenticatedWebSession.get().isSignedIn())));
//        project.add(new AjaxFormComponentUpdatingBehavior("change")
//        {
//            private static final long serialVersionUID = -2064647315598402594L;
//
//            @Override
//            public boolean getStatelessHint(Component component)
//            {
//                return true;
//            }
//
//            @Override
//            protected void onUpdate(AjaxRequestTarget aTarget)
//            {
//                // Reload the current page so the new project selection can take effect
//                // For legacy WebAnno pages, we set PAGE_PARAM_PROJECT_ID while INCEpTION
//                // pages may pick the project up from the session.
//                PageParameters params = new PageParameters();
//                if (project.getModelObject() != null) {
//                    params.set(PAGE_PARAM_PROJECT_ID, project.getModelObject().getId());
//                }
//                setResponsePage(getPage().getClass(), params);
//            }
//        });
//        project.setModel(LambdaModelAdapter.of(
//            () -> Session.get().getMetaData(SessionMetaData.CURRENT_PROJECT), 
//            (v) -> Session.get().setMetaData(SessionMetaData.CURRENT_PROJECT, v)));
//
//        project.setChoiceRenderer(new ChoiceRenderer<>("name"));
//        project.setChoices(LoadableDetachableModel.of(() -> 
//                projectService.listAccessibleProjects(userRepository.getCurrentUser())));
//        add(project);

        LambdaStatelessLink homeLink = new LambdaStatelessLink("homeLink", () -> 
                setResponsePage(getApplication().getHomePage()));
        add(homeLink);

        LambdaStatelessLink projectsLink = new LambdaStatelessLink("projectsLink", () -> 
                setResponsePage(ProjectsOverviewPage.class));
        add(projectsLink);

        LambdaStatelessLink adminLink = new LambdaStatelessLink("adminLink", () -> 
                setResponsePage(AdminDashboardPage.class));
        adminLink.add(visibleWhen(this::adminAreaAccessRequired));
        add(adminLink);
    }
    
    private boolean adminAreaAccessRequired()
    {
        return userRepository.getCurrentUser() != null && AdminDashboardPage
                .adminAreaAccessRequired(userRepository, projectService);
    }
}

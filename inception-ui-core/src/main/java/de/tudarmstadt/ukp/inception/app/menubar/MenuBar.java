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
package de.tudarmstadt.ukp.inception.app.menubar;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PAGE_PARAM_PROJECT_ID;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.inception.app.session.SessionMetaData;

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
        
        project = new DropDownChoice<Project>("project");
        project.add(LambdaBehavior.onConfigure(_this -> 
                _this.setVisible(AuthenticatedWebSession.get().isSignedIn())));
        project.add(new AjaxFormComponentUpdatingBehavior("change")
        {
            private static final long serialVersionUID = -2064647315598402594L;

            @Override
            public boolean getStatelessHint(Component component)
            {
                return true;
            }

            @Override
            protected void onUpdate(AjaxRequestTarget aTarget)
            {
                // Reload the current page so the new project selection can take effect
                // For legacy WebAnno pages, we set PAGE_PARAM_PROJECT_ID while INCEpTION
                // pages may pick the project up from the session.
                PageParameters params = new PageParameters();
                if (project.getModelObject() != null) {
                    params.set(PAGE_PARAM_PROJECT_ID, project.getModelObject().getId());
                }
                setResponsePage(getPage().getClass(), params);
            }
        });
        project.setModel(new Model<Project>() {
            private static final long serialVersionUID = 3648165425058995604L;

            @Override
            public Project getObject()
            {
                return Session.get().getMetaData(SessionMetaData.CURRENT_PROJECT);
            }
            
            @Override
            public void setObject(Project aObject)
            {
                Session.get().setMetaData(SessionMetaData.CURRENT_PROJECT, aObject);
            }
        });
        project.setChoiceRenderer(new ChoiceRenderer<>("name"));
        project.setChoices(LambdaModel.of(() -> 
                projectService.listAccessibleProjects(userRepository.getCurrentUser())));
        add(project);
    }
}

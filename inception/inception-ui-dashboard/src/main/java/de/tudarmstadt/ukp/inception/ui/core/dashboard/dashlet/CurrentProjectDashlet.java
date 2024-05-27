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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.dashlet;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static org.apache.commons.lang3.StringUtils.isBlank;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.support.markdown.MarkdownLabel;

public class CurrentProjectDashlet
    extends Dashlet_ImplBase
{
    private static final long serialVersionUID = 7732921923832675326L;

    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectService projectService;

    private IModel<String> nameModel;

    public CurrentProjectDashlet(String aId, IModel<Project> aCurrentProject)
    {
        super(aId, aCurrentProject);

        nameModel = Model.of(aCurrentProject.map(Project::getName).getObject());

        AjaxEditableLabel<String> name = new AjaxEditableLabel<String>("name", nameModel)
        {
            private static final long serialVersionUID = -2867104998844713915L;

            @Override
            protected void onSubmit(AjaxRequestTarget aTarget)
            {
                aTarget.addChildren(getPage(), IFeedback.class);

                if (!Project.isValidProjectName(nameModel.getObject())) {
                    // nameModel.setObject(aCurrentProject.map(Project::getName).getObject());
                    error("Invalid project name");
                }
                else {
                    aCurrentProject.getObject().setName(nameModel.getObject());
                    projectService.updateProject(aCurrentProject.getObject());
                    success("Project name was updated");
                    // Put the component back into label mode
                    onCancel(aTarget);
                }
            }
        };

        boolean isManager = projectService.hasRole(userRepository.getCurrentUser(),
                getModelObject(), MANAGER);
        name.setEnabled(isManager);
        add(name);

        add(new MarkdownLabel("description",
                LoadableDetachableModel.of(this::getProjectDescription)));
    }

    public Project getModelObject()
    {
        return getModel().orElse(null).getObject();
    }

    @SuppressWarnings("unchecked")
    public IModel<Project> getModel()
    {
        return (IModel<Project>) getDefaultModel();
    }

    private String getProjectDescription()
    {
        Project project = getModelObject();

        if (project == null) {
            return "Please select a project from the drop-down list above.";
        }

        if (isBlank(project.getDescription())) {
            return "Project has no description.";
        }

        return project.getDescription();
    }
}

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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.detail;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.ScriptDirection;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.NameUtil;

public class ProjectDetailPanel
    extends Panel
{
    private static final long serialVersionUID = 1118880151557285316L;

    private static final Logger LOG = LoggerFactory.getLogger(ProjectDetailPanel.class);

    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;
    private @SpringBean AnnotationSchemaService annotationService;

    private IModel<Project> projectModel;

    private Label idLabel;

    public ProjectDetailPanel(String id, IModel<Project> aModel)
    {
        super(id, aModel);

        projectModel = aModel;

        Form<Project> form = new Form<>("form", CompoundPropertyModel.of(aModel));
        add(form);

        TextField<String> projectNameTextField = new TextField<>("name");
        projectNameTextField.setRequired(true);
        projectNameTextField.add(new ProjectExistsValidator());
        projectNameTextField.add(new ProjectNameValidator());
        form.add(projectNameTextField);

        // If we run in development mode, then also show the ID of the project
        form.add(idLabel = new Label("id"));
        idLabel.setVisible(RuntimeConfigurationType.DEVELOPMENT
                .equals(getApplication().getConfigurationType()));

        form.add(new TextArea<String>("description").setOutputMarkupId(true));

        DropDownChoice<ScriptDirection> scriptDirection = new BootstrapSelect<>("scriptDirection");
        scriptDirection.setChoiceRenderer(new EnumChoiceRenderer<>(this));
        scriptDirection.setChoices(Arrays.asList(ScriptDirection.values()));
        form.add(scriptDirection);

        form.add(new CheckBox("disableExport").setOutputMarkupPlaceholderTag(true));

        form.add(new CheckBox("anonymousCuration").setOutputMarkupPlaceholderTag(true));

        form.add(new LambdaAjaxButton<>("save", this::actionSave));
    }

    private void actionSave(AjaxRequestTarget aTarget, Form<Project> aForm)
    {
        aTarget.add(getPage());
        // aTarget.add(((ApplicationPageBase) getPage()).getPageContent());
        // aTarget.addChildren(getPage(), IFeedback.class);

        Project project = aForm.getModelObject();
        if (isNull(project.getId())) {
            try {
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                projectService.createProject(project);

                projectService.createProjectPermission(
                        new ProjectPermission(project, username, PermissionLevel.MANAGER));
                projectService.createProjectPermission(
                        new ProjectPermission(project, username, PermissionLevel.CURATOR));
                projectService.createProjectPermission(
                        new ProjectPermission(project, username, PermissionLevel.ANNOTATOR));

                projectService.initializeProject(project);
            }
            catch (IOException e) {
                error("Project repository path not found " + ":"
                        + ExceptionUtils.getRootCauseMessage(e));
                LOG.error("Project repository path not found " + ":"
                        + ExceptionUtils.getRootCauseMessage(e));
            }
        }
        else {
            projectService.updateProject(project);
        }
    }

    private class ProjectNameValidator
        implements IValidator<String>
    {
        private static final long serialVersionUID = -2198422133042040370L;

        @Override
        public void validate(IValidatable<String> aValidatable)
        {
            String newName = aValidatable.getValue();
            if (isNotBlank(newName) && !NameUtil.isNameValid(newName)) {
                aValidatable.error(new ValidationError(
                        "Project name shouldn't contain characters such as /\\*?&!$+[^]"));
            }
        }
    }

    private class ProjectExistsValidator
        implements IValidator<String>
    {
        private static final long serialVersionUID = -267638869839503827L;

        @Override
        public void validate(IValidatable<String> aValidatable)
        {
            String newName = aValidatable.getValue();
            String oldName = aValidatable.getModel().getObject();
            if (!StringUtils.equals(newName, oldName) && isNotBlank(newName)
                    && projectService.existsProject(newName)) {
                aValidatable.error(new ValidationError(
                        "Another project with the same name exists. Please try a different name"));
            }
        }
    }
}

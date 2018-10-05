/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.detail;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
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
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.ScriptDirection;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ChallengeResponseDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
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
    
    private ChallengeResponseDialog deleteProjectDialog;
    private Label idLabel;
    private DropDownChoice<String> projectTypes;

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
        
        DropDownChoice<ScriptDirection> scriptDirection = new DropDownChoice<>("scriptDirection");
        scriptDirection.setChoiceRenderer(new EnumChoiceRenderer<>(this));
        scriptDirection.setChoices(Arrays.asList(ScriptDirection.values()));
        form.add(scriptDirection);
        
        form.add(new CheckBox("disableExport"));
        
        form.add(projectTypes = makeProjectTypeChoice());
        
        form.add(new LambdaAjaxButton<>("save", this::actionSave));
        form.add(new LambdaAjaxLink("cancel", this::actionCancel));
        form.add(new LambdaAjaxLink("delete", this::actionDelete).onConfigure((_this) -> 
            _this.setEnabled(projectModel.getObject() != null && 
                    projectModel.getObject().getId() != null )));

        IModel<String> projectNameModel = PropertyModel.of(projectModel, "name");
        add(deleteProjectDialog = new ChallengeResponseDialog("deleteProjectDialog",
                new StringResourceModel("DeleteProjectDialog.title", this),
                new StringResourceModel("DeleteProjectDialog.text", this)
                        .setModel(projectModel).setParameters(projectNameModel),
                projectNameModel));
        deleteProjectDialog.setConfirmAction((target) -> {
            try {
                projectService.removeProject(projectModel.getObject());
                projectModel.setObject(null);
                target.add(getPage());
            }
            catch (IOException e) {
                LOG.error("Unable to remove project :" + ExceptionUtils.getRootCauseMessage(e));
                error("Unable to remove project " + ":" + ExceptionUtils.getRootCauseMessage(e));
                target.addChildren(getPage(), IFeedback.class);
            }
        });
    }
    
    @Override
    protected void onModelChanged()
    {
        super.onModelChanged();
        
        // If there is only a single project type, then we can simply select that and do not need
        // to show the choice at all.
        if (projectTypes.getChoices().size() == 1 && projectModel.getObject().getMode() == null) {
            projectModel.getObject().setMode(projectTypes.getChoices().get(0));
        }
    }
        
    private DropDownChoice<String> makeProjectTypeChoice()
    {
        List<String> types = projectService.listProjectTypes().stream().map(t -> t.id())
                .collect(Collectors.toList());

        DropDownChoice<String> projTypes = new DropDownChoice<>("mode", types);

        projTypes.add(LambdaBehavior.onConfigure(it -> it.setEnabled(
                nonNull(projectModel.getObject()) && isNull(projectModel.getObject().getId()))));

        projTypes.setRequired(true);
        
        // If there is only a single project type, then we can simply select that and do not need
        // to show the choice at all.
        if (projectTypes.getChoices().size() == 1) {
            projectTypes.setVisible(false);
        }
        
        return projTypes;
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
                        new ProjectPermission(project, username, PermissionLevel.ADMIN));
                projectService.createProjectPermission(
                        new ProjectPermission(project, username, PermissionLevel.CURATOR));
                projectService.createProjectPermission(
                        new ProjectPermission(project, username, PermissionLevel.USER));

                annotationService.initializeProject(project);
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

    private void actionCancel(AjaxRequestTarget aTarget)
    {
        projectModel.setObject(null);
        
        // Reload whole page because master panel also needs to be reloaded.
        aTarget.add(getPage());
    }

    private void actionDelete(AjaxRequestTarget aTarget)
    {
        deleteProjectDialog.show(aTarget);
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

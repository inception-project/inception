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

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.model.Project.isValidProjectName;
import static de.tudarmstadt.ukp.clarin.webanno.model.Project.isValidProjectSlug;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.apache.wicket.RuntimeConfigurationType.DEVELOPMENT;

import java.io.IOException;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;

public class ProjectDetailPanel
    extends GenericPanel<Project>
{
    private static final long serialVersionUID = 1118880151557285316L;

    private static final Logger LOG = LoggerFactory.getLogger(ProjectDetailPanel.class);

    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;
    private @SpringBean AnnotationSchemaService annotationService;

    private IModel<String> slugSuggestionModel;
    private boolean slugChanged = false;

    private Label idLabel;
    private FootprintPanel footprintPanel;

    public ProjectDetailPanel(String id, IModel<Project> aModel)
    {
        super(id, aModel);

        slugSuggestionModel = Model.of();

        if (getModel().isPresent().getObject()) {
            deriveSlugFromName(getModelObject()).ifPresent(slugSuggestionModel::setObject);

            if (isBlank(getModelObject().getSlug())) {
                // Ok, so by just opening a project in the detail panel, a slug will be generated
                // and since this typically happens in the request where the project was loaded
                // from the DB, changing a field in the project will propagate back to the DB via
                // Hibernate magic. It may actually not be a bad thing because it means that the
                // slugs get populate over time just as part of regular usage.
                Project project = getModelObject();
                deriveSlugFromName(project).ifPresent(project::setSlug);
            }
        }

        var form = new Form<>("form", CompoundPropertyModel.of(aModel));
        add(form);

        var projectSlugTextField = new TextField<String>("slug");
        projectSlugTextField.setOutputMarkupId(true);
        projectSlugTextField.add(new ProjectWithSlugAlreadyExistsValidator());
        projectSlugTextField.add(new ProjectSlugIsValidValidator());
        projectSlugTextField.add(AttributeModifier.replace("placeholder", slugSuggestionModel));
        projectSlugTextField.add(new LambdaAjaxFormComponentUpdatingBehavior("change",
                _target -> slugChanged = true));
        form.add(projectSlugTextField);

        var projectNameTextField = new TextField<String>("name");
        projectNameTextField.setOutputMarkupId(true);
        projectNameTextField.setRequired(true);
        projectNameTextField.add(new ProjectNameIsValidValidator());
        projectNameTextField.add(new LambdaAjaxFormComponentUpdatingBehavior("change", _target -> {
            deriveSlugFromName(getModelObject()).ifPresent(slugSuggestionModel::setObject);
            _target.add(projectSlugTextField);
        }));
        form.add(projectNameTextField);

        // If we run in development mode, then also show the ID of the project
        form.add(idLabel = new Label("id"));
        idLabel.setVisible(getApplication().getConfigurationType() == DEVELOPMENT);

        form.add(new TextArea<String>("description").setOutputMarkupId(true));

        form.add(new CheckBox("disableExport").setOutputMarkupPlaceholderTag(true));

        form.add(new LambdaAjaxButton<>("save", this::actionSave));

        footprintPanel = new FootprintPanel("footprint", getModel());
        form.add(footprintPanel);
    }

    @Override
    protected void onModelChanged()
    {
        super.onModelChanged();
        footprintPanel.modelChanged();
    }

    private Optional<String> deriveSlugFromName(Project aProject)
    {
        var slug = projectService.deriveSlugFromName(aProject.getName());
        slug = projectService.deriveUniqueSlug(slug);
        // Just a safe-guard in case we would generate an invalid slug which we would not want
        // to get written back to the database
        if (isValidProjectSlug(slug)) {
            return Optional.of(slug);
        }

        LOG.warn(
                "Tried to derive a project slug from the project name [{}], but the "
                        + "derived slug [{}] is not valid - leaving the slug empty.",
                aProject.getName(), slug);

        return Optional.empty();
    }

    private void actionSave(AjaxRequestTarget aTarget, Form<Project> aForm)
    {
        aTarget.add(getPage());

        var project = aForm.getModelObject();

        if (isBlank(project.getSlug())) {
            var slugSuggestion = slugSuggestionModel.getObject();
            if (isBlank(slugSuggestion) || !isValidProjectSlug(slugSuggestion)) {
                error("A valid URL slug must be specified.");
                return;
            }
            project.setSlug(slugSuggestion);
        }

        var isNewProject = isNull(project.getId());
        if (isNewProject) {
            try {
                var user = userRepository.getCurrentUser();
                projectService.createProject(project);
                projectService.assignRole(project, user, ANNOTATOR, CURATOR, MANAGER);
                projectService.initializeProject(project);
            }
            catch (IOException e) {
                error("Unable to initialize project: " + getRootCauseMessage(e));
                LOG.error("Unable to initialize project", e);
            }
        }
        else {
            projectService.updateProject(project);
        }

        getSession().success("Project details saved.");
        aTarget.addChildren(getPage(), IFeedback.class);

        if (isNewProject || slugChanged) {
            // After saving a new project we want the URL to reflect the slug
            var pageParameters = new PageParameters();
            ProjectPageBase.setProjectPageParameter(pageParameters, project);
            setResponsePage(getPage().getClass(), pageParameters);
        }
    }

    private class ProjectSlugIsValidValidator
        implements IValidator<String>
    {
        private static final long serialVersionUID = 2143631682062991287L;

        @Override
        public void validate(IValidatable<String> aValidatable)
        {
            var newSlug = aValidatable.getValue();
            if (isNotBlank(newSlug) && !isValidProjectSlug(newSlug)) {
                aValidatable.error(new ValidationError(
                        "Project URL slug can consist only of lower-case letters [a-z], "
                                + "numbers [0-9], [-] or [_]. It must start with a letter."));
            }
        }
    }

    private class ProjectNameIsValidValidator
        implements IValidator<String>
    {
        private static final long serialVersionUID = -2198422133042040370L;

        @Override
        public void validate(IValidatable<String> aValidatable)
        {
            var newName = aValidatable.getValue();
            if (isNotBlank(newName) && !isValidProjectName(newName)) {
                aValidatable.error(new ValidationError(
                        "Project name shouldn't contain characters such as /\\*?&!$+[^]"));
            }
        }
    }

    private class ProjectWithSlugAlreadyExistsValidator
        implements IValidator<String>
    {
        private static final long serialVersionUID = 4424913151386159625L;

        @Override
        public void validate(IValidatable<String> aValidatable)
        {
            var newSlug = aValidatable.getValue();
            var oldSlug = aValidatable.getModel().getObject();
            if (!StringUtils.equals(newSlug, oldSlug) && isNotBlank(newSlug)
                    && projectService.existsProjectWithSlug(newSlug)) {
                aValidatable.error(new ValidationError(
                        "Another project with this URL slug exists. Please try a different one."));
            }
        }
    }
}

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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.users;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhenModelIsNotNull;
import static java.util.Arrays.asList;

import java.util.Collection;
import java.util.Optional;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;

import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectUserPermissions;
import de.tudarmstadt.ukp.clarin.webanno.security.Realm;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapCheckBoxMultipleChoice;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaModelAdapter;

public class ProjectUserDetailPanel
    extends GenericPanel<ProjectUserPermissions>
{
    private static final long serialVersionUID = -5278078988218713188L;

    private static final String MID_DIALOG = "dialog";

    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userService;

    private IModel<Project> project;
    private Form<Void> form;
    private CheckBoxMultipleChoice<PermissionLevel> levels;
    private final BootstrapModalDialog dialog;

    public ProjectUserDetailPanel(String aId, IModel<Project> aProject,
            IModel<ProjectUserPermissions> aUser)
    {
        super(aId, aUser);

        setOutputMarkupPlaceholderTag(true);
        add(visibleWhenModelIsNotNull(this));
        project = aProject;

        dialog = new BootstrapModalDialog(MID_DIALOG);
        dialog.trapFocus();
        queue(dialog);

        form = new Form<>("form");
        queue(form);

        queue(new TextField<String>("uiName") //
                .setModel(LambdaModel.of( //
                        aUser.map(ProjectUserPermissions::getUser).map(Optional::get), //
                        User::getUiName, //
                        User::setUiName))
                .add(this::validateUiName) //
                .setOutputMarkupPlaceholderTag(true) //
                .add(visibleWhen(this::isProjectBoundUser)) //
                .add(AttributeModifier.replace("placeholder",
                        getModel().map(ProjectUserPermissions::getUsername))));

        levels = new BootstrapCheckBoxMultipleChoice<>("permissions");
        // This model adapter handles loading/saving permissions directly to the DB
        levels.setModel(new LambdaModelAdapter<Collection<PermissionLevel>>( //
                () -> {
                    return projectService.listRoles(project.getObject(),
                            getModel().map(ProjectUserPermissions::getUsername).getObject());
                }, //
                (lvls) -> projectService.setProjectPermissionLevels(
                        getModel().map(ProjectUserPermissions::getUsername).getObject(),
                        project.getObject(), lvls)));
        levels.setChoices(asList(MANAGER, CURATOR, ANNOTATOR));
        levels.setChoiceRenderer(new EnumChoiceRenderer<>(levels));
        levels.add(this::ensureManagersNotRemovingThemselves);
        queue(levels);

        queue(new Label("username",
                aUser.map(u -> u.getUser().map(User::toLongString).orElse(u.getUsername()))));
        queue(new LambdaAjaxButton<>("save", this::actionSave));
        queue(new LambdaAjaxLink("cancel", this::actionCancel));
        queue(new LambdaAjaxLink("delete", this::actionDeleteRequested) //
                .add(visibleWhen(this::isProjectBoundUser)));
    }

    @Override
    protected void onModelChanged()
    {
        levels.modelChanged();
        form.modelChanged();
        super.onModelChanged();
    }

    private boolean isProjectBoundUser()
    {
        var user = getModelObject().getUser().orElse(null);
        if (user == null || user.getRealm() == null) {
            return false;
        }

        return user.getRealm().startsWith(Realm.REALM_PROJECT_PREFIX);
    }

    private void validateUiName(IValidatable<String> aValidatable)
    {
        var realm = projectService.getRealm(project.getObject());

        var other = userService.getUserByRealmAndUiName(realm, aValidatable.getValue());
        if (other != null && !other.getUsername().equals(getModel().getObject().getUsername())) {
            aValidatable.error(new ValidationError().addKey("uiName.alreadyExistsError")
                    .setVariable("name", aValidatable.getValue()));
        }

        userService.validateUiName(aValidatable.getValue()).forEach(aValidatable::error);
    }

    private void ensureManagersNotRemovingThemselves(
            IValidatable<Collection<PermissionLevel>> aValidatable)
    {
        if (!userService.getCurrentUsername().equals(
                getModel().map(ProjectUserPermissions::getUsername).orElse(null).getObject())) {
            return;
        }

        if (!projectService.hasRole(getModel().map(ProjectUserPermissions::getUsername).getObject(),
                project.getObject(), MANAGER)) {
            return;
        }

        if (!aValidatable.getValue().contains(MANAGER)) {
            aValidatable.error(new ValidationError(
                    "Managers cannot remove their own manager role from a project"));
        }
    }

    private void actionSave(AjaxRequestTarget aTarget, Form<Void> aForm)
    {
        // The model adapter already commits the changes for us as part of the normal form
        // processing cycle. So nothing special to do here.

        success("User saved");
        aTarget.addChildren(getPage(), IFeedback.class);

        aTarget.add(findParent(ProjectUsersPanel.class));
    }

    private void actionCancel(AjaxRequestTarget aTarget)
    {
        setModelObject(null);

        aTarget.add(findParent(ProjectUsersPanel.class));
    }

    private void actionDeleteRequested(AjaxRequestTarget aTarget)
    {
        var userModel = getModel().map(ProjectUserPermissions::getUser).map(Optional::get);
        var dialogContent = new DeleteProjectBoundUserConfirmationDialogContentPanel(
                ModalDialog.CONTENT_ID,
                getModel().map(ProjectUserPermissions::getUser).map(Optional::get));
        dialogContent.setExpectedResponseModel(userModel.map(User::getUiName));
        dialogContent.setConfirmAction($ -> actionDeleteConfirmed($, userModel.getObject()));

        dialog.open(dialogContent, aTarget);
    }

    private void actionDeleteConfirmed(AjaxRequestTarget aTarget, User aUser)
    {
        projectService.deleteProjectBoundUser(project.getObject(), aUser);

        success("User deleted");
        aTarget.addChildren(getPage(), IFeedback.class);

        setModelObject(null);

        aTarget.add(findParent(ProjectUsersPanel.class));
    }
}

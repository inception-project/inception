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

import java.io.Serializable;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectUserPermissions;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public class AddProjectUserPanel
    extends GenericPanel<Project>
{
    private static final long serialVersionUID = 716627578324682205L;

    private @SpringBean UserDao userService;
    private @SpringBean ProjectService projectService;

    private final IModel<ProjectUserPermissions> userModel;

    public AddProjectUserPanel(String aId, IModel<Project> aModel,
            IModel<ProjectUserPermissions> aUser)
    {
        super(aId, aModel);
        userModel = aUser;

        queue(new Form<>("form", CompoundPropertyModel.of(new FormData())));

        var uiName = new TextField<String>("uiName");
        uiName.add(this::validateUiName);
        queue(uiName);

        queue(new LambdaAjaxButton<>("confirm", this::actionConfirm));

        var closeDialogButton = new LambdaAjaxLink("closeDialog", this::actionCancel);
        closeDialogButton.setOutputMarkupId(true);
        queue(closeDialogButton);
    }

    private void validateUiName(IValidatable<String> aValidatable)
    {
        var realm = projectService.getRealm(getModelObject());
        var other = userService.getUserByRealmAndUiName(realm, aValidatable.getValue());

        if (other != null) {
            aValidatable.error(new ValidationError().addKey("uiName.alreadyExistsError")
                    .setVariable("name", aValidatable.getValue()));
        }

        userService.validateUiName(aValidatable.getValue()).forEach(aValidatable::error);
    }

    protected void actionCancel(AjaxRequestTarget aTarget)
    {
        findParent(ModalDialog.class).close(aTarget);
    }

    private void actionConfirm(AjaxRequestTarget aTarget, Form<FormData> aForm)
    {
        var user = projectService.getOrCreateProjectBoundUser(getModelObject(),
                aForm.getModelObject().uiName);

        projectService.assignRole(getModelObject(), user, ANNOTATOR);

        userModel.setObject(projectService.getProjectUserPermissions(getModelObject(), user));

        aTarget.add(findParent(ProjectUsersPanel.class));
        findParent(ModalDialog.class).close(aTarget);
    }

    class FormData
        implements Serializable
    {
        private static final long serialVersionUID = 1978074327564033679L;

        String uiName;
    }
}

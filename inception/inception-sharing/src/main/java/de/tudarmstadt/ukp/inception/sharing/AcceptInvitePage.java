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
package de.tudarmstadt.ukp.inception.sharing;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;
import static de.tudarmstadt.ukp.inception.sharing.AcceptInvitePage.PAGE_PARAM_INVITE_ID;

import java.io.Serializable;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.project.ProjectDashboardPage;

@MountPath(value = NS_PROJECT + "/${" + PAGE_PARAM_PROJECT + "}/join-project/${"
        + PAGE_PARAM_INVITE_ID + "}")
public class AcceptInvitePage
    extends ProjectPageBase
{
    private static final long serialVersionUID = 5160703195387357692L;

    public static final String PAGE_PARAM_INVITE_ID = "i";

    private @SpringBean InviteService inviteService;
    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;

    private IModel<FormData> formModel;

    public AcceptInvitePage(final PageParameters aPageParameters)
    {
        super(aPageParameters);

        IModel<Boolean> invitationIsValid = LoadableDetachableModel.of(this::checkInvitation);

        User user = userRepository.getCurrentUser();

        // If the current user has already accepted the invitation, directly forward to the project
        if (user != null && invitationIsValid.getObject()) {
            if (projectService.existsProjectPermissionLevel(user, getProject(), ANNOTATOR)) {
                backToProjectPage();
            }
        }

        add(new WebMarkupContainer("expirationNotice")
                .add(visibleWhen(() -> !invitationIsValid.orElse(false).getObject())));

        formModel = new CompoundPropertyModel<>(new FormData());
        Form<FormData> acceptInvitationPanel = new Form<>("acceptInvitationForm", formModel);
        acceptInvitationPanel.add(new Label("project", PropertyModel.of(getProject(), "name")));
        acceptInvitationPanel.add(new RequiredTextField<String>("username") //
                .add(visibleWhen(() -> user == null)));
        acceptInvitationPanel.add(new PasswordTextField("password") //
                .add(visibleWhen(() -> user == null)));
        acceptInvitationPanel.add(new LambdaAjaxButton<>("join", this::actionJoinProject));

        acceptInvitationPanel.add(visibleWhen(() -> invitationIsValid.orElse(false).getObject()));
        add(acceptInvitationPanel);
    }

    private String getInviteId()
    {
        return getPageParameters().get(PAGE_PARAM_INVITE_ID).toOptionalString();
    }

    private boolean checkInvitation()
    {
        return getProject() != null && inviteService.isValidInviteLink(getProject(), getInviteId());
    }

    private void actionJoinProject(AjaxRequestTarget aTarget, Form<?> aForm)
    {
        if (!checkInvitation()) {
            error("Invitation has expired.");
            return;
        }

        if (userRepository.getCurrentUser() == null) {
            FormData data = formModel.getObject();
            if (!AuthenticatedWebSession.get().signIn(data.username, data.password)) {
                error("Login failed");
                aTarget.addChildren(getPage(), IFeedback.class);
                return;
            }
        }

        User currentUser = userRepository.getCurrentUser();

        if (!projectService.existsProjectPermissionLevel(currentUser, getProject(), ANNOTATOR)) {
            projectService.createProjectPermission(
                    new ProjectPermission(getProject(), currentUser.getUsername(), ANNOTATOR));
            getSession().success("You have successfully joined the project.");
        }
        else {
            getSession().info("You were already an annotator on this project.");
        }

        setResponsePage(ProjectDashboardPage.class, new PageParameters()
                .set(ProjectDashboardPage.PAGE_PARAM_PROJECT, getProject().getId()));
    }

    private static class FormData
        implements Serializable
    {
        private static final long serialVersionUID = -2338711546557816393L;

        String username;
        String password;
    }
}

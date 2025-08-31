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
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;
import static de.tudarmstadt.ukp.inception.sharing.AcceptInvitePage.PAGE_PARAM_INVITE_ID;
import static de.tudarmstadt.ukp.inception.sharing.model.Mandatoriness.MANDATORY;
import static de.tudarmstadt.ukp.inception.sharing.model.Mandatoriness.NOT_ALLOWED;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.EmailTextField;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.session.SessionRegistry;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.LoginProperties;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.ApplicationSession;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.sharing.config.InviteServiceProperties;
import de.tudarmstadt.ukp.inception.sharing.model.ProjectInvite;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.markdown.MarkdownLabel;
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
    private @SpringBean LoginProperties loginProperties;
    private @SpringBean SessionRegistry sessionRegistry;
    private @SpringBean InviteServiceProperties inviteServiceProperties;

    private final IModel<FormData> formModel;
    private final IModel<ProjectInvite> invite;
    private final IModel<Boolean> invitationIsValid;
    private final WebMarkupContainer tooManyUsersNotice;

    public AcceptInvitePage(final PageParameters aPageParameters)
    {
        super(aPageParameters);

        invitationIsValid = LoadableDetachableModel.of(this::checkInvitation);
        invite = LoadableDetachableModel.of(() -> inviteService.readProjectInvite(getProject()));

        User user = userRepository.getCurrentUser();

        // If the current user has already accepted the invitation, directly forward to the project
        if (user != null && invitationIsValid.getObject()) {
            if (projectService.hasRole(user, getProject(), ANNOTATOR)) {
                backToProjectPage();
            }
            else if (user.getRealm() != null) {
                // It the current user is a project-bound user and does not already exist in the
                // project, then the user belongs to a different project. In this case, the user
                // must not join another project to which they are not bound
                ApplicationSession.get().signOut();
                throw new RestartResponseException(getClass(), aPageParameters);
            }
        }

        add(new WebMarkupContainer("expirationNotice")
                .add(visibleWhen(() -> !invitationIsValid.orElse(false).getObject())));

        formModel = new CompoundPropertyModel<>(new FormData());
        formModel.getObject().registeredLogin = !invite.getObject().isGuestAccessible()
                || !inviteServiceProperties.isGuestsEnabled();
        formModel.getObject().askForEMail = invite.getObject().isGuestAccessible()
                && invite.getObject().getAskForEMail() != NOT_ALLOWED
                && inviteServiceProperties.isGuestsEnabled();

        Form<FormData> form = new Form<>("acceptInvitationForm", formModel);
        // form.add(new Label("project", PropertyModel.of(getProject(), "name")));
        form.add(new RequiredTextField<String>("username") //
                .add(AttributeModifier.replace("placeholder",
                        LoadableDetachableModel.of(
                                () -> getUserIdPlaceholder(formModel.getObject().registeredLogin))))
                .add(visibleWhen(() -> user == null)));
        form.add(new PasswordTextField("password") //
                .add(visibleWhen(() -> user == null && formModel.getObject().registeredLogin)));
        form.add(new EmailTextField("eMail") //
                .setRequired(invite.getObject().getAskForEMail() == MANDATORY)
                .add(visibleWhen(() -> user == null && formModel.getObject().askForEMail
                        && !formModel.getObject().registeredLogin)));
        form.add(new LambdaAjaxButton<>("join", this::actionJoinProject));
        form.add(new CheckBox("registeredLogin") //
                .setOutputMarkupPlaceholderTag(true) //
                .add(visibleWhen(() -> invite.getObject().isGuestAccessible()
                        && inviteServiceProperties.isGuestsEnabled() && user == null))
                .add(new LambdaAjaxFormComponentUpdatingBehavior("change",
                        _target -> _target.add(form))));
        form.add(new MarkdownLabel("invitationText",
                LoadableDetachableModel.of(this::getInvitationText)));

        tooManyUsersNotice = new WebMarkupContainer("tooManyUsersNotice");
        tooManyUsersNotice.add(visibleWhen(this::isTooManyUsers));
        form.add(tooManyUsersNotice);

        form.add(visibleWhen(() -> invitationIsValid.orElse(false).getObject()));
        form.add(enabledWhen(() -> !isTooManyUsers()));
        add(form);
    }

    private String getUserIdPlaceholder(boolean aRegisteredLogin)
    {
        if (aRegisteredLogin || invite.getObject() == null
                || isBlank(invite.getObject().getUserIdPlaceholder())) {
            return "User ID";
        }

        return invite.getObject().getUserIdPlaceholder();
    }

    private String getInvitationText()
    {
        if (invite.getObject() == null) {
            return "Invitation does not exist.";
        }

        String invitationText;
        if (isBlank(invite.getObject().getInvitationText())) {
            invitationText = String.join("\n", //
                    "## Welcome!", //
                    "", //
                    "You have been invited to join the project", //
                    "**" + getProject().getName() + "**", //
                    "as an annotator.", //
                    "", //
                    "Would you like to join?");
        }
        else {
            invitationText = invite.getObject().getInvitationText();
        }

        return invitationText;
    }

    private String getInviteId()
    {
        return getPageParameters().get(PAGE_PARAM_INVITE_ID).toOptionalString();
    }

    private boolean checkInvitation()
    {
        return getProject() != null && inviteService.isValidInviteLink(getProject(), getInviteId());
    }

    private void actionJoinProject(AjaxRequestTarget aTarget, Form<FormData> aForm)
    {
        if (!checkInvitation()) {
            error("Invitation has expired.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        User user;
        if (aForm.getModelObject().registeredLogin || userRepository.getCurrentUser() != null) {
            user = signInAsRegisteredUserIfNecessary();
        }
        else {
            user = signInAsProjectUser(aForm.getModelObject());
        }

        if (user == null) {
            error("Login failed");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        PageParameters pageParameters = new PageParameters();
        setProjectPageParameter(pageParameters, getProject());
        setResponsePage(ProjectDashboardPage.class, pageParameters);
    }

    private Authentication asAuthentication(User aUser)
    {
        Set<GrantedAuthority> authorities = userRepository.listAuthorities(aUser).stream()
                .map(_role -> new SimpleGrantedAuthority(_role.getAuthority()))
                .collect(Collectors.toSet());
        return new UsernamePasswordAuthenticationToken(aUser.getUsername(), null, authorities);

    }

    private User signInAsProjectUser(FormData aFormData)
    {
        var existingUser = projectService.getProjectBoundUser(getProject(), aFormData.username);

        if (existingUser.isPresent() && !existingUser.get().isEnabled()) {
            error("User deactivated");
            return null;
        }

        if (invite.getObject().getAskForEMail() != NOT_ALLOWED) {
            String storedEMail = existingUser.map(User::getEmail).orElse(null);
            if (storedEMail != null && !storedEMail.equals(aFormData.eMail)) {
                error("Provided eMail address does not match stored eMail address");
                return null;
            }
        }

        var user = projectService.getOrCreateProjectBoundUser(getProject(), aFormData.username);
        if (aFormData.eMail != null && user.getEmail() == null) {
            user.setEmail(aFormData.eMail);
            userRepository.update(user);
        }

        // Want to make sure we clear any session-bound state
        ApplicationSession.get().signIn(asAuthentication(user));
        createProjectPermissionsIfNecessary(user);

        return user;
    }

    private User signInAsRegisteredUserIfNecessary()
    {
        var user = userRepository.getCurrentUser();

        if (user == null) {
            FormData data = formModel.getObject();
            if (!ApplicationSession.get().signIn(data.username, data.password)) {
                return null;
            }
            user = userRepository.getCurrentUser();
        }

        if (user != null) {
            createProjectPermissionsIfNecessary(user);
        }

        return user;
    }

    private void createProjectPermissionsIfNecessary(User aUser)
    {
        if (!projectService.hasRole(aUser, getProject(), ANNOTATOR)) {
            projectService.assignRole(getProject(), aUser, ANNOTATOR);
            getSession().success("You have successfully joined the project.");
        }
        else {
            getSession().info("You were already an annotator on this project.");
        }
    }

    /**
     * Check if settings property is set and there will be more users logged in (with current one)
     * than max users allowed.
     */
    private boolean isTooManyUsers()
    {
        long maxUsers = loginProperties.getMaxConcurrentSessions();
        return maxUsers > 0 && sessionRegistry.getAllPrincipals().size() >= maxUsers;
    }

    private static class FormData
        implements Serializable
    {
        private static final long serialVersionUID = -2338711546557816393L;

        String username;
        String password;
        String eMail;
        boolean registeredLogin;
        boolean askForEMail;
    }
}

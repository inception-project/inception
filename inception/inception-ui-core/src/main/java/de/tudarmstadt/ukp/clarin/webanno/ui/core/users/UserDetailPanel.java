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
package de.tudarmstadt.ukp.clarin.webanno.ui.core.users;

import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_ADMIN;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_REMOTE;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_USER;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static org.apache.wicket.event.Broadcast.BUBBLE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.EmailTextField;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;

public class UserDetailPanel
    extends Panel
{
    private static final long serialVersionUID = 5307939458232893305L;

    private @SpringBean UserDao userRepository;
    private @SpringBean(required = false) RemoteApiProperties remoteApiProperties;
    private @SpringBean AuthenticationProvider authenticationProvider;

    private boolean isCreate = false;
    private PasswordTextField oldPasswordField;
    private PasswordTextField passwordField;
    private PasswordTextField repeatPasswordField;

    public transient String oldPassword;
    public transient String password;
    public transient String repeatPassword;

    public UserDetailPanel(String aId, IModel<User> aModel)
    {
        super(aId, new CompoundPropertyModel<>(aModel));

        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);

        var username = new TextField<String>("username");
        username.setRequired(true);
        username.add(this::validateUsername);
        username.add(enabledWhen(() -> isCreate));
        queue(username);

        queue(new TextField<String>("uiName") //
                .add(this::validateUiName) //
                .add(AttributeModifier.replace("placeholder", username.getModel())));
        queue(new Label("lastLogin"));
        queue(new Label("created"));
        queue(new EmailTextField("email") //
                .add(this::validateEmail));

        oldPasswordField = new PasswordTextField("oldPassword");
        oldPasswordField.add(this::validateOldPassword);
        oldPasswordField.setModel(PropertyModel.of(this, "oldPassword"));
        oldPasswordField.setRequired(true);
        oldPasswordField.add(visibleWhen(this::requiresOldPasswordToChange));
        queue(oldPasswordField);

        passwordField = new PasswordTextField("password");
        passwordField.add(this::validatePassword);
        passwordField.setModel(PropertyModel.of(this, "password"));
        passwordField.setRequired(false);
        passwordField.add(visibleWhen(this::canChangePassword));
        queue(passwordField);

        repeatPasswordField = new PasswordTextField("repeatPassword");
        repeatPasswordField.add(this::validatePassword);
        repeatPasswordField.setModel(PropertyModel.of(this, "repeatPassword"));
        repeatPasswordField.setRequired(false);
        repeatPasswordField.add(visibleWhen(this::canChangePassword));
        queue(repeatPasswordField);

        queue(new ListMultipleChoice<>("roles", getRoles()) //
                .add(this::validateRoles) //
                .add(visibleWhen(userRepository::isCurrentUserAdmin)));

        queue(new CheckBox("enabled") //
                .add(this::validateEnabled) //
                .add(visibleWhen(userRepository::isCurrentUserAdmin)) //
                .setOutputMarkupPlaceholderTag(true));

        queue(new LambdaAjaxButton<>("save", this::actionSave));

        queue(new LambdaAjaxLink("cancel", this::actionCancel));

        var form = new Form<User>("form");
        form.add(new EqualPasswordInputValidator(passwordField, repeatPasswordField));
        queue(form);
    }

    private boolean requiresOldPasswordToChange()
    {
        // When creating a user we do not need the old password
        // When a password change is not possible anyway, we also do not need the old password
        if (isCreate || !canChangePassword()) {
            return false;
        }

        // Always require old password when changing own password - even for admins
        if (userRepository.getCurrentUsername().equals(getModelObject().getUsername())) {
            return true;
        }

        // Admins do not need the old password for changing the password of a user
        return !userRepository.isCurrentUserAdmin();
    }

    /**
     * Users that are bound to a project (i.e. the realm is set) or which are external users (i.e.
     * they have an empty password) cannot change their password.
     * 
     * @return if the user can change their password.
     */
    private boolean canChangePassword()
    {
        return getModel().map(_u -> _u.getRealm() == null && !_u.isExternalUser()).orElse(false)
                .getObject();
    }

    public void setCreatingNewUser(boolean aIsCreate)
    {
        isCreate = aIsCreate;
    }

    @SuppressWarnings("unchecked")
    public IModel<User> getModel()
    {
        return (IModel<User>) getDefaultModel();
    }

    public User getModelObject()
    {
        return getModel().getObject();
    }

    private void validateOldPassword(IValidatable<String> aValidatable)
    {
        Authentication auth = null;
        try {
            auth = authenticationProvider.authenticate(new UsernamePasswordAuthenticationToken(
                    getModelObject().getUsername(), aValidatable.getValue()));
        }
        catch (AuthenticationException e) {
            auth = null;
        }

        if (auth == null) {
            aValidatable.error(new ValidationError().addKey("oldPassword.wrong"));
        }
    }

    private void validatePassword(IValidatable<String> aValidatable)
    {
        userRepository.validatePassword(aValidatable.getValue()).forEach(aValidatable::error);
    }

    private void validateUsername(IValidatable<String> aValidatable)
    {
        if (userRepository.exists(aValidatable.getValue()) && isCreate) {
            aValidatable.error(new ValidationError().addKey("username.alreadyExistsError")
                    .setVariable("name", aValidatable.getValue()));
        }

        userRepository.validateUsername(aValidatable.getValue()).forEach(aValidatable::error);
    }

    private void validateEmail(IValidatable<String> aValidatable)
    {
        userRepository.validateEmail(aValidatable.getValue()).forEach(aValidatable::error);
    }

    private void validateUiName(IValidatable<String> aValidatable)
    {
        User other = userRepository.getUserByRealmAndUiName(getModelObject().getRealm(),
                aValidatable.getValue());

        if (other != null && !other.getUsername().equals(getModelObject().getUsername())) {
            aValidatable.error(new ValidationError().addKey("uiName.alreadyExistsError")
                    .setVariable("name", aValidatable.getValue()));
        }

        userRepository.validateUiName(aValidatable.getValue()).forEach(aValidatable::error);
    }

    private void validateEnabled(IValidatable<Boolean> aValidatable)
    {
        if (!aValidatable.getValue() && userRepository.getCurrentUser().equals(getModelObject())) {
            aValidatable.error(
                    new ValidationError().setMessage("You cannot disable your own account."));
        }
    }

    private void validateRoles(IValidatable<Collection<Role>> aValidatable)
    {
        Collection<Role> newRoles = aValidatable.getValue();
        if (newRoles.isEmpty()) {
            aValidatable.error(
                    new ValidationError().setMessage("A user has to have at least one role."));
        }
        // enforce users to have at least the ROLE_USER role
        if (!newRoles.contains(ROLE_USER)) {
            aValidatable
                    .error(new ValidationError().setMessage("Every user must have 'ROLE_USER'."));
        }
        // don't let an admin user strip himself of admin rights
        if (userRepository.getCurrentUser().equals(getModelObject())
                && !newRoles.contains(ROLE_ADMIN)) {
            aValidatable.error(
                    new ValidationError().setMessage("You cannot remove your own admin status."));
        }
    }

    private List<Role> getRoles()
    {
        List<Role> roles = new ArrayList<>(Arrays.asList(Role.values()));
        if (remoteApiProperties != null && !remoteApiProperties.isEnabled()) {
            roles.remove(ROLE_REMOTE);
        }
        return roles;
    }

    public void actionSave(AjaxRequestTarget aTarget, Form<User> aForm)
    {
        try {
            User user = getModelObject();

            if (password != null) {
                success("Password for user [" + user.getUsername() + "] has been set.");
                user.setPassword(password);
            }

            if (!userRepository.exists(user.getUsername())) {
                userRepository.create(user);
                success("User [" + user.getUsername() + "] has been created.");
            }
            else {
                userRepository.update(user);
                success("Details for user [" + user.getUsername() + "] have been updated.");
            }

            aTarget.addChildren(getPage(), IFeedback.class);

            send(this, BUBBLE, new UserSavedEvent(aTarget, user));
        }
        finally {
            password = null;
            oldPassword = null;
            repeatPassword = null;
        }
    }

    private void actionCancel(AjaxRequestTarget aTarget)
    {
        if (userRepository.isCurrentUserAdmin()) {
            getModel().setObject(null);
            aTarget.add(findParent(ManageUsersPage.class));
        }
        else {
            setResponsePage(getApplication().getHomePage());
        }
    }
}

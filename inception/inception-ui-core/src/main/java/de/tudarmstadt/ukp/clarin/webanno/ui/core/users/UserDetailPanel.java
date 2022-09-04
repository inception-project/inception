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

import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_REMOTE;
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

    private boolean isCreate = false;
    private PasswordTextField passwordField;
    private PasswordTextField repeatPasswordField;

    public transient String password;

    public transient String repeatPassword;

    public UserDetailPanel(String aId, IModel<User> aModel)
    {
        super(aId, new CompoundPropertyModel<>(aModel));

        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);

        TextField<String> username = new TextField<String>("username");
        username.setRequired(true);
        username.add(this::validateUsername);
        username.add(enabledWhen(() -> isCreate));
        queue(username);

        queue(new TextField<String>("uiName") //
                .add(this::validateUiName)
                .add(AttributeModifier.replace("placeholder", username.getModel())));
        queue(new Label("lastLogin"));
        queue(new Label("created"));
        queue(new EmailTextField("email"));

        passwordField = new PasswordTextField("password");
        passwordField.add(this::validatePassword);
        passwordField.setModel(PropertyModel.of(this, "password"));
        passwordField.setRequired(false);
        passwordField.add(visibleWhen(aModel.map(_u -> _u.getRealm() == null)));
        queue(passwordField);

        repeatPasswordField = new PasswordTextField("repeatPassword");
        repeatPasswordField.add(this::validatePassword);
        repeatPasswordField.setModel(PropertyModel.of(this, "repeatPassword"));
        repeatPasswordField.setRequired(false);
        repeatPasswordField.add(visibleWhen(aModel.map(_u -> _u.getRealm() == null)));
        queue(repeatPasswordField);

        queue(new ListMultipleChoice<>("roles", getRoles()) //
                .add(this::validateRoles) //
                .add(visibleWhen(this::isAdmin)));

        queue(new CheckBox("enabled") //
                .add(this::validateEnabled) //
                .add(visibleWhen(this::isAdmin)) //
                .setOutputMarkupPlaceholderTag(true));

        queue(new LambdaAjaxButton<>("save", this::actionSave));

        queue(new LambdaAjaxLink("cancel", this::actionCancel));

        var form = new Form<User>("form");
        form.add(new EqualPasswordInputValidator(passwordField, repeatPasswordField));
        queue(form);
    }

    public void setCreate(boolean aIsCreate)
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

    private void validateUiName(IValidatable<String> aValidatable)
    {
        User other = userRepository.getUserByRealmAndUiName(getModelObject().getRealm(),
                aValidatable.getValue());
        if (other != null && !other.getUsername().equals(getModelObject().getUsername())) {
            aValidatable.error(new ValidationError().addKey("uiName.alreadyExistsError")
                    .setVariable("name", aValidatable.getValue()));
        }
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
        if (!newRoles.contains(Role.ROLE_USER)) {
            aValidatable
                    .error(new ValidationError().setMessage("Every user must have 'ROLE_USER'."));
        }
        // don't let an admin user strip himself of admin rights
        if (userRepository.getCurrentUser().equals(getModelObject())
                && !newRoles.contains(Role.ROLE_ADMIN)) {
            aValidatable.error(
                    new ValidationError().setMessage("You cannot remove your own admin status."));
        }
    }

    private boolean isAdmin()
    {
        return userRepository.isAdministrator(userRepository.getCurrentUser());
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
        User user = getModelObject();

        if (password != null) {
            user.setPassword(password);
        }

        if (!userRepository.exists(user.getUsername())) {
            userRepository.create(user);
        }
        else {
            userRepository.update(user);
        }

        info("Details for user [" + user.getUsername() + "] have been saved.");
        aTarget.addChildren(getPage(), IFeedback.class);

        send(this, BUBBLE, new UserSavedEvent(aTarget, user));
    }

    private void actionCancel(AjaxRequestTarget aTarget)
    {
        if (isAdmin()) {
            getModel().setObject(null);
            aTarget.add(findParent(ManageUsersPage.class));
        }
        else {
            setResponsePage(getApplication().getHomePage());
        }
    }
}

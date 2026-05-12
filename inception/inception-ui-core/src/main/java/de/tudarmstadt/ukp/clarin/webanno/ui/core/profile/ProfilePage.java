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
package de.tudarmstadt.ukp.clarin.webanno.ui.core.profile;

import static de.tudarmstadt.ukp.clarin.webanno.security.UserDao.userHasNoPassword;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.lang.String.format;

import java.time.Duration;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.EmailTextField;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.inception.log.api.EventRepository;
import de.tudarmstadt.ukp.inception.log.api.model.UserSessionStats;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

/**
 * Self-service profile page for regular (non-admin) users. Lets a user edit their own display name,
 * email and password. Only reachable if the administrator has enabled profile self-service.
 */
@MountPath("/profile")
public class ProfilePage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = 1L;

    private @SpringBean UserDao userService;
    private @SpringBean AuthenticationProvider authenticationProvider;
    private @SpringBean EventRepository eventRepository;

    private final IModel<User> userModel;

    private PasswordTextField oldPasswordField;
    private PasswordTextField passwordField;
    private PasswordTextField repeatPasswordField;

    public transient String oldPassword;
    public transient String password;
    public transient String repeatPassword;

    public ProfilePage(final PageParameters aParameters)
    {
        super(aParameters);

        var currentUser = userService.getCurrentUser();
        if (currentUser == null || !userService.isProfileSelfServiceAllowed(currentUser)) {
            denyAccess();
        }

        userModel = LoadableDetachableModel.of(userService::getCurrentUser);

        var form = new Form<User>("form", new CompoundPropertyModel<>(userModel));
        queue(form);

        var username = new TextField<String>("username");
        username.setEnabled(false);
        queue(username);

        queue(new TextField<String>("uiName") //
                .add(this::validateUiName) //
                .add(AttributeModifier.replace("placeholder", userModel.map(User::getUsername))));

        queue(new EmailTextField("email").add(this::validateEmail));

        queue(new Label("created"));
        queue(new Label("lastLogin"));
        queue(new Label("sessionTime",
                userModel.map(u -> eventRepository.getAggregateSessionDuration(u.getUsername()))
                        .map(UserSessionStats::duration).map(this::renderSessionTime)));

        var passwordUnsetNotice = new WebMarkupContainer("passwordUnsetNotice");
        passwordUnsetNotice.add(visibleWhen(() -> userHasNoPassword(userModel.getObject())
                && userService.canChangePassword(userModel.getObject())));
        queue(passwordUnsetNotice);

        oldPasswordField = new PasswordTextField("oldPassword",
                PropertyModel.of(this, "oldPassword"));
        oldPasswordField.setRequired(true);
        oldPasswordField.add(this::validateOldPassword);
        oldPasswordField.add(visibleWhen(this::requiresOldPasswordToChange));
        queue(oldPasswordField);

        passwordField = new PasswordTextField("password", PropertyModel.of(this, "password"));
        passwordField.setRequired(false);
        passwordField.add(this::validatePassword);
        passwordField.add(visibleWhen(() -> userService.canChangePassword(userModel.getObject())));
        queue(passwordField);

        repeatPasswordField = new PasswordTextField("repeatPassword",
                PropertyModel.of(this, "repeatPassword"));
        repeatPasswordField.setRequired(false);
        repeatPasswordField
                .add(visibleWhen(() -> userService.canChangePassword(userModel.getObject())));
        queue(repeatPasswordField);

        form.add(new EqualPasswordInputValidator(passwordField, repeatPasswordField));

        queue(new LambdaAjaxButton<>("save", this::actionSave));
        queue(new LambdaAjaxLink("close", this::actionClose));
    }

    private void denyAccess()
    {
        getSession().error(format("Access to [%s] denied.", getClass().getSimpleName()));
        throw new RestartResponseException(getApplication().getHomePage());
    }

    private boolean requiresOldPasswordToChange()
    {
        return userService.canChangePassword(userModel.getObject());
    }

    private CharSequence renderSessionTime(Duration aDuration)
    {
        if (aDuration.toHours() > 0) {
            return "~" + aDuration.toHours() + " hours";
        }
        if (aDuration.toSeconds() > 0) {
            return "less than one hour";
        }
        return "none";
    }

    private void validateOldPassword(IValidatable<String> aValidatable)
    {
        Authentication auth;
        try {
            auth = authenticationProvider.authenticate(new UsernamePasswordAuthenticationToken(
                    userModel.getObject().getUsername(), aValidatable.getValue()));
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
        userService.validatePassword(aValidatable.getValue()).forEach(aValidatable::error);
    }

    private void validateEmail(IValidatable<String> aValidatable)
    {
        userService.validateEmail(aValidatable.getValue()).forEach(aValidatable::error);
    }

    private void validateUiName(IValidatable<String> aValidatable)
    {
        var user = userModel.getObject();
        var other = userService.getUserByRealmAndUiName(user.getRealm(), aValidatable.getValue());
        if (other != null && !other.getUsername().equals(user.getUsername())) {
            aValidatable.error(new ValidationError().addKey("uiName.alreadyExistsError")
                    .setVariable("name", aValidatable.getValue()));
        }

        userService.validateUiName(aValidatable.getValue()).forEach(aValidatable::error);
    }

    private void actionSave(AjaxRequestTarget aTarget, Form<User> aForm)
    {
        try {
            var user = userModel.getObject();

            if (password != null) {
                user.setPassword(password);
                success("Password has been updated.");
            }

            userService.update(user);
            success("Your profile has been updated.");

            aTarget.addChildren(getPage(), IFeedback.class);
        }
        finally {
            oldPassword = null;
            password = null;
            repeatPassword = null;
        }
    }

    private void actionClose(AjaxRequestTarget aTarget)
    {
        setResponsePage(getApplication().getHomePage());
    }
}

/*
 * Copyright 2012
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
package de.tudarmstadt.ukp.clarin.webanno.ui.core.users;

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.SecurityUtil;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.ModelChangedVisitor;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.NameUtil;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.config.RemoteApiProperties;

/**
 * Manage Application wide Users.
 */
@MountPath("/users.html")
public class ManageUsersPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = -2102136855109258306L;

    public static final String PARAM_USER = "user";

    private @SpringBean UserDao userRepository;
    private @SpringBean(required = false) RemoteApiProperties remoteApiProperties;

    private class DetailForm
        extends Form<User>
    {
        private static final long serialVersionUID = -1L;

        private boolean isCreate = false;
        private PasswordTextField passwordField;
        private PasswordTextField repeatPasswordField;
        
        public transient String password;
        
        @SuppressWarnings("unused")
        public transient String repeatPassword;

        public DetailForm(String id, IModel<User> aModel)
        {
            super(id, new CompoundPropertyModel<>(aModel));

            setOutputMarkupId(true);
            setOutputMarkupPlaceholderTag(true);
            
            add(new TextField<String>("username")
                    .setRequired(true)
                    .add(this::validateUsername)
                    .add(enabledWhen(() -> isCreate)));
            add(new Label("lastLogin"));
            add(new EmailTextField("email"));
            
            passwordField = new PasswordTextField("password");
            passwordField.setModel(PropertyModel.of(DetailForm.this, "password"));
            passwordField.setRequired(false);
            add(passwordField);
            
            repeatPasswordField = new PasswordTextField("repeatPassword");
            repeatPasswordField.setModel(PropertyModel.of(DetailForm.this, "repeatPassword"));
            repeatPasswordField.setRequired(false);
            add(repeatPasswordField);
            
            add(new EqualPasswordInputValidator(passwordField, repeatPasswordField));
            
            add(new ListMultipleChoice<>("roles", getRoles())
                    .add(this::validateRoles)
                    .add(visibleWhen(ManageUsersPage.this::isAdmin)));
            
            add(new CheckBox("enabled")
                    .add(this::validateEnabled)
                    .add(visibleWhen(ManageUsersPage.this::isAdmin)));

            add(new LambdaAjaxButton<>("save", ManageUsersPage.this::actionSave));
            
            add(new LambdaAjaxLink("cancel", ManageUsersPage.this::actionCancel));
        }
        
        private void validateUsername(IValidatable<String> aValidatable)
        {
            if (userRepository.exists(aValidatable.getValue()) && isCreate) {
                aValidatable.error(new ValidationError().addKey("username.alreadyExistsError")
                        .setVariable("name", aValidatable.getValue()));
            }
            else if (aValidatable.getValue().contains(" ")) {
                aValidatable.error(new ValidationError().addKey("username.containsSpaceError"));
            }
            else if (!NameUtil.isNameValid(aValidatable.getValue())) {
                aValidatable.error(new ValidationError().addKey("username.invalidCharactersError"));
            }
        }
        
        private void validateEnabled(IValidatable<Boolean> aValidatable)
        {
            if (!aValidatable.getValue()
                    && userRepository.getCurrentUser().equals(getModelObject())) {
                aValidatable.error(new ValidationError()
                        .setMessage("You cannot disable your own account."));
            }
        }

        private void validateRoles(IValidatable<Collection<Role>> aValidatable)
        {
            Collection<Role> newRoles = aValidatable.getValue();
            if (newRoles.isEmpty()) {
                aValidatable.error(new ValidationError()
                        .setMessage("A user has to have at least one role."));
            }
            // enforce users to have at least the ROLE_USER role
            if (!newRoles.contains(Role.ROLE_USER)) {
                aValidatable.error(new ValidationError()
                        .setMessage("Every user must have 'ROLE_USER'."));
            }
            // don't let an admin user strip himself of admin rights
            if (userRepository.getCurrentUser().equals(getModelObject())
                    && !newRoles.contains(Role.ROLE_ADMIN)) {
                aValidatable.error(new ValidationError()
                        .setMessage("You cannot remove your own admin status."));
            }
        }
        
        @Override
        protected void onConfigure()
        {
            super.onConfigure();
            
            setVisible(getModelObject() != null);
        }
    }

    private DetailForm detailForm;
    private UserSelectionPanel users;
    
    private IModel<User> selectedUser;

    public ManageUsersPage()
    {
        super();

        commonInit();
        
        // If the user is not an admin, then pre-load the current user to allow self-service
        // editing of the profile
        if (!isAdmin() && SecurityUtil.isProfileSelfServiceAllowed()) {
            selectedUser.setObject(userRepository.getCurrentUser());
        }
    }

    public ManageUsersPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);
        
        commonInit();
        
        String username = aPageParameters.get(PARAM_USER).toOptionalString();
        User user = null;
        if (username != null) {
            user = userRepository.get(username);
        }
        if (user != null) {
            if (isAdmin()) {
                selectedUser.setObject(user);
            }
            else if (SecurityUtil.isProfileSelfServiceAllowed()
                    && userRepository.getCurrentUser().getUsername().equals(user.getUsername())) {
                selectedUser.setObject(userRepository.getCurrentUser());
            }
            else {
                // Make sure a user doesn't try to access the profile of another user via the
                // parameter if self-service is turned on.
                setResponsePage(getApplication().getHomePage());
            }
        }
    }
    
    private void commonInit()
    {
        // If the user is not an admin and self-service is not allowed, go back to the main page
        if (!isAdmin() && !SecurityUtil.isProfileSelfServiceAllowed()) {
            setResponsePage(getApplication().getHomePage());
        }

        selectedUser = Model.of();

        users = new UserSelectionPanel("users", selectedUser);
        // Show the selection for different users only to administrators
        users.add(visibleWhen(this::isAdmin));
        users.setCreateAction(_target -> {
            selectedUser.setObject(new User());
            _target.add(users);
            _target.add(detailForm);
            // Need to defer setting this field because otherwise setChangeAction below
            // sets it back to false.
            _target.registerRespondListener(__target -> detailForm.isCreate = true);
        });
        users.setChangeAction(target -> { 
            detailForm.isCreate = false;
            // Make sure that any invalid forms are cleared now that we load the new project.
            // If we do not do this, then e.g. input fields may just continue showing the values
            // they had when they were marked invalid.
            detailForm.visitChildren(new ModelChangedVisitor(selectedUser));
            target.add(detailForm);
        });
        add(users);
        
        detailForm = new DetailForm("detailForm", selectedUser);
        add(detailForm);
    }

    public void actionSave(AjaxRequestTarget aTarget, Form<User> aForm)
    {
        User user = detailForm.getModelObject();

        if (detailForm.password != null) {
            user.setPassword(detailForm.password);
        }

        if (!userRepository.exists(user.getUsername())) {
            userRepository.create(user);
        }
        else {
            userRepository.update(user);
        }

        if (isAdmin()) {
            selectedUser.setObject(null);
        }

        info("User details have been saved.");
        
        aTarget.add(detailForm);
        aTarget.add(users);
        aTarget.addChildren(getPage(), IFeedback.class);
    }

    private void actionCancel(AjaxRequestTarget aTarget)
    {
        if (isAdmin()) {
            selectedUser.setObject(null);
            aTarget.add(detailForm);
            aTarget.add(users);
        }
        else {
            setResponsePage(getApplication().getHomePage());
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
            roles.remove(Role.ROLE_REMOTE);
        }
        return roles;
    }
}

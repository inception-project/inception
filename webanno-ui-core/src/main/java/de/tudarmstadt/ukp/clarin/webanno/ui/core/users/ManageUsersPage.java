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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.EmailTextField;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.SecurityUtil;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.ModelChangedVisitor;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.NameUtil;

/**
 * Manage Application wide Users.
 */
@MountPath("/users.html")
public class ManageUsersPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = -2102136855109258306L;

    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectService projectRepository;

    private class DetailForm
        extends Form<User>
    {
        private static final long serialVersionUID = -1L;

        public transient Model<String> passwordModel = new Model<>();
        public transient Model<String> repeatPasswordModel = new Model<>();

        public DetailForm(String id, IModel<User> aModel)
        {
            super(id, new CompoundPropertyModel<>(aModel));

            setOutputMarkupId(true);
            setOutputMarkupPlaceholderTag(true);
            
            add(new TextField<String>("username"));
            add(new PasswordTextField("password", passwordModel).setRequired(false));
            add(new PasswordTextField("repeatPassword", repeatPasswordModel).setRequired(false));
            add(new Label("lastLogin"));
            add(new EmailTextField("email"));
            
            WebMarkupContainer adminOnly = new WebMarkupContainer("adminOnly");
            adminOnly.add(new ListMultipleChoice<>("roles", new ArrayList<>(Role.getRoles()))
                    .add(new IValidator<Collection<Role>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void validate(IValidatable<Collection<Role>> aValidatable)
                        {
                            Collection<Role> newRoles = aValidatable.getValue();
                            if (newRoles.isEmpty()) {
                                aValidatable.error(new ValidationError()
                                        .setMessage("A user has to have at least one role."));
                            }
                            // enforce users to have at least the ROLE_USER role
                            if (!newRoles.contains(Role.ROLE_USER)) {
                                aValidatable.error(new ValidationError()
                                        .setMessage("Every user has to be a user."));
                            }
                            // don't let an admin user strip himself of admin rights
                            if (userRepository.getCurrentUser().equals(getModelObject())
                                    && !newRoles.contains(Role.ROLE_ADMIN)) {
                                aValidatable.error(new ValidationError()
                                        .setMessage("You can't remove your own admin status."));
                            }

                        }
                    }));
            adminOnly.add(new CheckBox("enabled").add(new IValidator<Boolean>()
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void validate(IValidatable<Boolean> aValidatable)
                {
                    if (!aValidatable.getValue()
                            && userRepository.getCurrentUser().equals(getModelObject())) {
                        aValidatable.error(new ValidationError()
                                .setMessage("You cannot disable your own account."));
                    }
                }
            }));
            adminOnly.setVisible(isAdmin());
            add(adminOnly);

            add(new LambdaAjaxButton<>("save", (_target, form) -> {
                _target.add(getPage());
                if (userRepository.exists(DetailForm.this.getModelObject().getUsername())
                        && isCreate) {
                    info("User already exists.");
                }
                else if (DetailForm.this.getModelObject().getUsername().contains(" ")) {
                    info("User username cannot contain SPACE character.");
                }
                else if (NameUtil.isNameValid(DetailForm.this.getModelObject().getUsername())) {
                    actionSave();
                }
                else {
                    info("Username cannot contain special characters.");
                }
            }));
            
            add(new LambdaAjaxLink("cancel", ManageUsersPage.this::actionCancel));

            add(new EqualPasswordInputValidator((FormComponent<?>) get("password"),
                    (FormComponent<?>) get("repeatPassword")));
        }

        public String getPassword()
        {
            return passwordModel.getObject();
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
    private boolean isCreate = false;
    
    public ManageUsersPage()
    {
        selectedUser = Model.of();
        
        users = new UserSelectionPanel("users", selectedUser);
        users.setCreateAction(target -> {
            selectedUser.setObject(new User());
            isCreate = true;
            target.add(detailForm);
        });
        users.setChangeAction(target -> { 
            isCreate = false;
            // Make sure that any invalid forms are cleared now that we load the new project.
            // If we do not do this, then e.g. input fields may just continue showing the values
            // they had when they were marked invalid.
            detailForm.visitChildren(new ModelChangedVisitor(selectedUser));
            target.add(detailForm);
        });
        add(users);
        
        detailForm = new DetailForm("detailForm", selectedUser);

        // show only selectionForm when accessing this page as admin
        if (isAdmin()) {
            users.setVisible(true);
        }
        // else show only the own options
        else {
            selectedUser.setObject(userRepository.getCurrentUser());
            users.setVisible(false);
        }

        add(detailForm);
    }

    public void actionSave()
    {
        User user = detailForm.getModelObject();

        if (detailForm.getPassword() != null) {
            user.setPassword(detailForm.getPassword());
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
    }

    private void actionCancel(AjaxRequestTarget aTarget) {
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
        return SecurityUtil.isSuperAdmin(projectRepository, userRepository.getCurrentUser());
    }
}

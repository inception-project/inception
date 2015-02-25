/*******************************************************************************
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.security.page;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.EmailTextField;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.brat.project.ProjectUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.webapp.home.page.ApplicationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.welcome.WelcomePage;

/**
 * Manage Application wide Users.
 *
 * @author Seid Muhie Yimam
 * @author Richard Eckart de Castilho
 *
 */
public class ManageUsersPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = -2102136855109258306L;

    @SpringBean(name = "userRepository")
    private UserDao userRepository;

    @SpringBean(name = "documentRepository")
    private RepositoryService projectRepository;

    boolean isCreate = false;
    private class SelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;

        public SelectionForm(String id)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

            add(new Button("create", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    actionSelectionChanged(null);
                    actionCreate();
                }
            });
            // not used in the 1.0.0 release
            Button delete;
            add(delete = new Button("delete", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onConfigure()
                {
                    // disable delete button when active user is selected
                    setEnabled(selectionForm.getModelObject().user != null
                            && !selectionForm.getModelObject().user.equals(getActiveUser()));
                }

                @Override
                public void onSubmit()
                {
                    actionDelete();
                }
            });
            delete.setVisible(false);

            add(new ListChoice<User>("user")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<User>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<User> load()
                        {
                            return userRepository.list();
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<User>() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public Object getDisplayValue(User aUser)
                        {
                            return aUser.getUsername() + (aUser.isEnabled() ? "" : " (disabled)");
                        }
                    });
                    setNullValid(false);
                }
                
                @Override
                protected void onSelectionChanged(User aNewSelection)
                {
                    actionSelectionChanged(aNewSelection);
                }

                @Override
                protected boolean wantOnSelectionChangedNotifications()
                {
                    return true;
                }

                @Override
                protected CharSequence getDefaultChoice(String aSelectedValue)
                {
                    return "";
                }
            });
        }
    }

    static private class SelectionModel
        implements Serializable
    {
        private static final long serialVersionUID = -1L;

        private User user;
    }

    private class DetailForm
        extends Form<User>
    {
        private static final long serialVersionUID = -1L;

        public transient Model<String> passwordModel = new Model<String>();
        public transient Model<String> repeatPasswordModel = new Model<String>();

        public DetailForm(String id)
        {
            super(id, new CompoundPropertyModel<User>(new Model<User>(new User())));

            add(new TextField<String>("username").setOutputMarkupId(true));
            add(new PasswordTextField("password", passwordModel).setRequired(false));
            add(new PasswordTextField("repeatPassword", repeatPasswordModel).setRequired(false));
            add(new EmailTextField("email"));
            WebMarkupContainer adminOnly = new WebMarkupContainer("adminOnly");
            adminOnly.add(new ListMultipleChoice<Role>("roles",
                    new ArrayList<Role>(Role.getRoles())).add(new IValidator<Collection<Role>>()
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
                    if (getActiveUser().equals(getModelObject())
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
                    if (!aValidatable.getValue() && getActiveUser().equals(getModelObject())) {
                        aValidatable.error(new ValidationError()
                                .setMessage("You can't disable your own account."));
                    }
                }
            }));
            adminOnly.setVisible(isAdmin());
            add(adminOnly);

            add(new Button("save", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    if(userRepository.exists(DetailForm.this.getModelObject().getUsername())&& isCreate){
                        info("User already exists.");
                    }
                    else if(DetailForm.this.getModelObject().getUsername().contains(" ")){
                        info("User username should not contain SPACE character.");
                    }
                    else if (ProjectUtil
                            .isNameValid(DetailForm.this.getModelObject().getUsername())) {
                        actionSave();
                    }
                    else {
                        info("Username should not contain special character.");
                    }
                }
            });
            add(new Button("cancel", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    actionCancel();
                }
            }.setDefaultFormProcessing(false));

            add(new EqualPasswordInputValidator((FormComponent<?>) get("password"),
                    (FormComponent<?>) get("repeatPassword")));
        }

        public String getPassword()
        {
            return passwordModel.getObject();
        }
    }

    private SelectionForm selectionForm;
    private DetailForm detailForm;

    public ManageUsersPage()
    {
        selectionForm = new SelectionForm("selectionForm");
        detailForm = new DetailForm("detailForm");

        // show only selectionForm when accessing this page as admin
        if (isAdmin()) {
            detailForm.setVisible(false);
        }
        // else show only the own options
        else {
            actionSelectionChanged(getActiveUser());
            selectionForm.setVisible(false);
        }

        add(selectionForm);
        add(detailForm);
    }

    public void actionSelectionChanged(User aNewSelection)
    {
        if (aNewSelection != null) {
            detailForm.setModelObject(aNewSelection);
            detailForm.setVisible(true);
            detailForm.get("username").setEnabled(false);
            isCreate = false;
        }
        else {
            detailForm.setVisible(false);
        }
    }

    public void actionCreate()
    {
        selectionForm.getModelObject().user = null;
        detailForm.setModelObject(new User());
        detailForm.setVisible(true);
        detailForm.get("username").setEnabled(true);
        isCreate = true;
    }

    // not used in 1.0.0 release
    public void actionDelete()
    {
        User user = selectionForm.getModelObject().user;
        if (user != null) {
            userRepository.delete(user);
        }
        selectionForm.getModelObject().user = null;
        selectionForm.get("user").detachModels();
        detailForm.setVisible(false);
        info("User [" + user.getUsername() + "] has been removed.");
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
            detailForm.setModelObject(new User());
            detailForm.setVisible(false);
        }
        selectionForm.getModelObject().user = null;
        info("User details have been saved.");
    }

    public void actionCancel()
    {
        if (isAdmin()) {
            detailForm.detach();
            detailForm.setModelObject(new User());
            detailForm.setVisible(false);
        }
        else {
            setResponsePage(WelcomePage.class);
        }
    }

    private boolean isAdmin()
    {
        return getActiveUser().getRoles().contains(Role.ROLE_ADMIN);
    }

    private User getActiveUser()
    {
        String authedUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.get(authedUsername);
    }
}

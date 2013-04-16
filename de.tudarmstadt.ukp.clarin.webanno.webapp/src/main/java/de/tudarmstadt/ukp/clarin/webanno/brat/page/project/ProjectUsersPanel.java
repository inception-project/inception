/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.page.project;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevels;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermissions;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

/**
 * A Panel used to add user permissions to a selected {@link Project}
 * @author Seid Muhie Yimam
 *
 */
public class ProjectUsersPanel
    extends Panel
{

    @SpringBean(name = "documentRepository")
    private RepositoryService projectRepository;

   /* private class ItemContainer
        extends WebMarkupContainer
    {

        public final User user;
        boolean selected = false;

        public ItemContainer(String id, User aUser)
        {
            super(id);
            this.user = aUser;
            add(new Label("users", new PropertyModel(user, "number")));
            add(new CheckBox("itemSelected", new PropertyModel(selected, "selected")));
        }
    }*/

    private class UserSelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;

        public UserSelectionForm(String id)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

            add(users = new ListChoice<User>("user")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<User>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<User> load()
                        {
                            return projectRepository.listProjectUsers(selectedProject);
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<User>("username"));
                    setNullValid(false);
                }

                @Override
                protected void onSelectionChanged(User aNewSelection)
                {
                    if (aNewSelection != null) {
                        selectedUser = aNewSelection;
                        if (permissionLevelDetailForm.getModelObject().permissionLevels != null) {
                            for (PermissionLevels lev : permissionLevelDetailForm.getModelObject().permissionLevels) {
                                System.out.println(lev);
                            }
                            permissionLevelDetailForm.getModelObject().permissionLevels
                                    .add(PermissionLevels.curator);
                        }
                    }
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
            users.setOutputMarkupId(true).add(new SimpleAttributeModifier("style", "width:100%"));
            users.setMaxRows(15);

            add(new Button("addUser", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    userDetailForm.setVisible(true);

                }
            });

            add(new Button("addRole", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    if (selectedUser != null) {
                        permissionLevelDetailForm.setVisible(true);
                    }

                }
            });

        }
    }

    private class SelectionModel
        implements Serializable
    {
        public List<PermissionLevels> permissionLevels;
        public User user;
    }

    private class SelectedPermissionLevelsForm
        extends Form<String>
    {
        private static final long serialVersionUID = -1L;

        public SelectedPermissionLevelsForm(String id)
        {
            super(id);
            selectedPermissionLevelsRepeater = new RefreshingView(
                    "selectedPermissionLevelsRepeater")
            {
                @Override
                protected Iterator getItemModels()
                {
                    List<IModel> models = new ArrayList<IModel>();
                    for (User user : projectRepository.listProjectUsers(selectedProject)) {
                        models.add(new Model(getRole(user, selectedProject)));
                    }
                    return models.iterator();
                }

                @Override
                protected void populateItem(Item item)
                {
                    item.add(new Label("roles", item.getModel()));
                }
            };
            add(selectedPermissionLevelsRepeater);
        }
    }

    private class PermissionLevelDetailForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;

        public PermissionLevelDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));
            add(permissionLevels = new CheckBoxMultipleChoice<PermissionLevels>("permissionLevels")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<PermissionLevels>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<PermissionLevels> load()
                        {

                            return Arrays.asList(new PermissionLevels[] { PermissionLevels.admin,
                                    PermissionLevels.curator, PermissionLevels.user });
                        }
                    });
                }
            });
            permissionLevels.setOutputMarkupId(true);
            add(new Button("update", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    if (selectedUser != null) {
                        List<ProjectPermissions> projectPermissions = projectRepository
                                .listProjectPermisionLevels(selectedUser, selectedProject);
                        if (projectPermissions.size() == 0) {
                            ProjectPermissions permissions = new ProjectPermissions();
                            permissions.setProject(selectedProject);
                            permissions.setUser(selectedUser);
                            Set<String> roles = new HashSet<String>();
                            for (PermissionLevels level : PermissionLevelDetailForm.this
                                    .getModelObject().permissionLevels) {
                                roles.add(level.name());
                            }
                            permissions.setLevel(roles);
                            try {
                                projectRepository.createProjectPermission(permissions);
                            }
                            catch (IOException ex) {
                                error("Unabel to cretae project permissions"
                                        + ExceptionUtils.getRootCauseMessage(ex));
                            }
                        }
                        else {
                            ProjectPermissions permissions = projectPermissions.get(0);
                            Set<String> roles = new HashSet<String>();
                            for (PermissionLevels level : PermissionLevelDetailForm.this
                                    .getModelObject().permissionLevels) {
                                roles.add(level.name());
                            }
                            permissions.setLevel(roles);
                        }
                    }
                    PermissionLevelDetailForm.this.setVisible(false);
                }
            });
        }
    }

    private class UserDetailForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;

        public UserDetailForm(String id)
        {
            super(id);
            add(new CheckBoxMultipleChoice<User>("users", new LoadableDetachableModel<List<User>>()
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected List<User> load()
                {
                    return projectRepository.listUsers();
                }
            }, new ChoiceRenderer<User>("username", "username")).setRequired(true));

            add(new Button("add")
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    // Modify projectPermissions table, in case a user is removed TODO
                    UserDetailForm.this.setVisible(false);
                }
            });
        }
    }

    public String getRole(User userName, Project project)
    {
        List<ProjectPermissions> projectPermissions = projectRepository.listProjectPermisionLevels(
                userName, project);
        String roles = "";
        if (projectPermissions.size() > 0) {
            for (String role : projectPermissions.get(0).getLevel()) {
                roles = roles + " " + role;
            }
        }
        return "-->" + roles.trim().replace(" ", ",");
    }

    // The first document in the project // auto selected in the first time.
    private User selectedUser;

    private ListChoice<User> users;
    private RefreshingView selectedPermissionLevelsRepeater;
    private CheckBoxMultipleChoice<PermissionLevels> permissionLevels;

    private UserSelectionForm userSelectionForm;
    private SelectedPermissionLevelsForm selectedPermissionLevelsForm;
    private PermissionLevelDetailForm permissionLevelDetailForm;
    private UserDetailForm userDetailForm;

    private Project selectedProject;

    public ProjectUsersPanel(String id, Project aProject)
    {
        super(id);
        this.selectedProject = aProject;
        userSelectionForm = new UserSelectionForm("userSelectionForm");
        selectedPermissionLevelsForm = new SelectedPermissionLevelsForm(
                "selectedPermissionLevelsForm");
        selectedPermissionLevelsForm.setOutputMarkupId(true);

        permissionLevelDetailForm = new PermissionLevelDetailForm("permissionLevelDetailForm");
        permissionLevelDetailForm.setVisible(false);

        userDetailForm = new UserDetailForm("userDetailForm");
        userDetailForm.setVisible(false);

        add(userSelectionForm);
        add(selectedPermissionLevelsForm);
        add(permissionLevelDetailForm);
        add(userDetailForm);
    }
}
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
package de.tudarmstadt.ukp.clarin.webanno.ui.project;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;

/**
 * A Panel used to add user permissions to a selected {@link Project}
 */
@ProjectSettingsPanel(label = "Users", prio = 100)
public class ProjectUsersPanel
    extends ProjectSettingsPanelBase
{
    private static final long serialVersionUID = 875749625429630464L;

    private @SpringBean ProjectService projectRepository;
    private @SpringBean UserDao userRepository;

    private User selectedUser;

    private ListChoice<User> users;
    private CheckBoxMultipleChoice<PermissionLevel> permissionLevels;

    private UserSelectionForm userSelectionForm;
    private PermissionLevelDetailForm permissionLevelDetailForm;
    private UserDetailForm userDetailForm;

    public ProjectUsersPanel(String id, IModel<Project> aProject)
    {
        super(id, aProject);
        userSelectionForm = new UserSelectionForm("userSelectionForm");

        permissionLevelDetailForm = new PermissionLevelDetailForm("permissionLevelDetailForm");
        permissionLevelDetailForm.setVisible(false);

        userDetailForm = new UserDetailForm("userDetailForm");
        userDetailForm.setVisible(false);

        add(userSelectionForm);
        add(permissionLevelDetailForm);
        add(userDetailForm);
    }
    
    private class UserSelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;

        List<User> userLists = new ArrayList<>();

        public UserSelectionForm(String id)
        {
            super(id, new CompoundPropertyModel<>(new SelectionModel()));

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
                            userLists = projectRepository.listProjectUsersWithPermissions(
                                    ProjectUsersPanel.this.getModelObject());
                            return userLists;
                        }
                    });

                    setChoiceRenderer(new ChoiceRenderer<User>()
                    {
                        private static final long serialVersionUID = 4607720784161484145L;

                        @Override
                        public Object getDisplayValue(User aObject)
                        {
                            List<ProjectPermission> projectPermissions = projectRepository
                                    .listProjectPermissionLevel(aObject,
                                            ProjectUsersPanel.this.getModelObject());
                            List<String> permissionLevels = new ArrayList<>();
                            for (ProjectPermission projectPermission : projectPermissions) {
                                permissionLevels.add(projectPermission.getLevel().getName());
                            }
                            return aObject.getUsername() + " " + permissionLevels
                                    + (aObject.isEnabled() ? "" : " (login disabled)");
                        }
                    });
                    setNullValid(false);
                }

                @Override
                protected void onSelectionChanged(User aNewSelection)
                {
                    if (aNewSelection == null) {
                        return;
                    }
                    selectedUser = aNewSelection;
                    // Clear old selections
                    permissionLevelDetailForm.setModelObject(null);
                    List<ProjectPermission> projectPermissions = projectRepository
                            .listProjectPermissionLevel(selectedUser,
                                    ProjectUsersPanel.this.getModelObject());
                    List<PermissionLevel> levels = new ArrayList<>();
                    for (ProjectPermission permission : projectPermissions) {
                        levels.add(permission.getLevel());
                    }
                    SelectionModel newSelectionModel = new SelectionModel();
                    newSelectionModel.permissionLevels.addAll(levels);
                    permissionLevelDetailForm.setModelObject(newSelectionModel);
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
            users.setOutputMarkupId(true).add(new AttributeModifier("style", "width:100%"));
            users.setMaxRows(15);
            add(new Button("addUser", new StringResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    userDetailForm.setVisible(true);

                }
            });

            Button removeUserButton = new Button("removeUser", new StringResourceModel("label"))
            {

                private static final long serialVersionUID = 5032883366656500162L;

                @Override
                public void onSubmit()
                {
                    if (selectedUser == null) {
                        info("No user is selected to remove");
                        return;
                    }
                    List<ProjectPermission> projectPermissions = projectRepository
                            .listProjectPermissionLevel(selectedUser,
                                    ProjectUsersPanel.this.getModelObject());
                    for (ProjectPermission projectPermission : projectPermissions) {
                        projectRepository.removeProjectPermission(projectPermission);
                    }
                    userLists.remove(selectedUser);
                }
            };
            // Add check to prevent accidental delete operation
            removeUserButton.add(new AttributeModifier("onclick",
                    "if(!confirm('Do you really want to remove this User?')) return false;"));

            add(removeUserButton);
            // add(new Button("removeUser", new ResourceModel("label"))
            // {
            // private static final long serialVersionUID = 1L;
            //
            // @Override
            // public void onSubmit()
            // {
            // if (selectedUser == null) {
            // info("No user is selected to remove");
            // return;
            // }
            // List<ProjectPermission> projectPermissions = projectRepository
            // .listProjectPermisionLevel(selectedUser, selectedProject.getObject());
            // for (ProjectPermission projectPermission : projectPermissions) {
            // try {
            // projectRepository.removeProjectPermission(projectPermission);
            // }
            // catch (IOException e) {
            // error("Unable to remove project permission level "
            // + ExceptionUtils.getRootCauseMessage(e));
            // }
            // }
            // userLists.remove(selectedUser);
            // }
            // });

            add(new Button("addPermissionLevel", new StringResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    if (selectedUser != null) {
                        permissionLevelDetailForm.setVisible(true);
                    }
                    else {
                        info("Please Select a User First");
                    }

                }
            });

        }
    }

    public class SelectionModel
        implements Serializable
    {
        private static final long serialVersionUID = 9137613222721590389L;

        public List<PermissionLevel> permissionLevels = new ArrayList<>();
        public User user;
        public List<User> users = new ArrayList<>();
        public String userFilter;
    }

    private class PermissionLevelDetailForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;

        public PermissionLevelDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<>(new SelectionModel()));
            add(permissionLevels = new CheckBoxMultipleChoice<PermissionLevel>("permissionLevels")
            {
                private static final long serialVersionUID = 1L;

                {
                    setSuffix("<br>");
                    setChoices(new LoadableDetachableModel<List<PermissionLevel>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<PermissionLevel> load()
                        {
                            return Arrays.asList(PermissionLevel.ADMIN,
                                PermissionLevel.CURATOR, PermissionLevel.USER);
                        }

                    });
                    setChoiceRenderer(new ChoiceRenderer<PermissionLevel>()
                    {
                        private static final long serialVersionUID = 9050427999256764850L;

                        @Override
                        public Object getDisplayValue(PermissionLevel aObject)
                        {
                            return aObject.getName();
                        }
                    });
                }
            });
            permissionLevels.setOutputMarkupId(true);
            add(new Button("update", new StringResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    if (selectedUser != null) {
                        Project project = ProjectUsersPanel.this.getModelObject();
                        
                        List<ProjectPermission> projectPermissions = projectRepository
                                .listProjectPermissionLevel(selectedUser, project);

                        // Remove old permissionLevels
                        for (ProjectPermission ExistingProjectPermission : projectPermissions) {
                            projectRepository.removeProjectPermission(ExistingProjectPermission);
                        }

                        for (PermissionLevel level : PermissionLevelDetailForm.this
                                .getModelObject().permissionLevels) {
                            if (!projectRepository.existsProjectPermissionLevel(selectedUser,
                                    project, level)) {
                                ProjectPermission projectPermission = new ProjectPermission();
                                projectPermission.setLevel(level);
                                projectPermission.setUser(selectedUser.getUsername());
                                projectPermission.setProject(project);
                                projectRepository.createProjectPermission(projectPermission);
                            }
                        }
                    }
                    PermissionLevelDetailForm.this.setVisible(false);
                }
            });

            add(new Button("cancel", new StringResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    PermissionLevelDetailForm.this.setVisible(false);
                }
            });
        }
    }

    private class UserDetailForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;
        private CheckBoxMultipleChoice<User> users;

        public UserDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<>(new SelectionModel()));
            TextField<String> filterText = new TextField<>("userFilter");
            
            add(filterText.setOutputMarkupPlaceholderTag(true));
            add(new Button("filterButton")
            {
                private static final long serialVersionUID = -7523594952670514192L;

                @Override
                public void onSubmit()
                {
                    // Only needed so that user list is loaded
                }
            });
            
            add(users = new CheckBoxMultipleChoice<>("users",
                new LoadableDetachableModel<List<User>>()
                {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected List<User> load()
                    {
                        List<User> allUSers = userRepository.list();
                        List<User> filteredUSers = new ArrayList<>();
                        allUSers.removeAll(projectRepository.listProjectUsersWithPermissions(
                            ProjectUsersPanel.this.getModelObject()));

                        for (User user : allUSers) {
                            if (user.getUsername().contains(filterText.getValue())) {
                                filteredUSers.add(user);
                            }
                        }

                        return filteredUSers;
                    }
                }, new ChoiceRenderer<>("username", "username")));
            users.setSuffix("<br>");

            add(new Button("add")
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    if (users.getModelObject() != null) {
                        UserDetailForm.this.getModelObject().userFilter = "";

                        for (User user : users.getModelObject()) {
                            ProjectPermission projectPermission = new ProjectPermission();
                            projectPermission.setProject(ProjectUsersPanel.this.getModelObject());
                            projectPermission.setUser(user.getUsername());
                            projectPermission.setLevel(PermissionLevel.USER);
                            projectRepository.createProjectPermission(projectPermission);
                            selectedUser = user;
                        }
                        
                        SelectionModel userSelectionModel = new SelectionModel();
                        userSelectionModel.user = selectedUser;
                        userSelectionModel.permissionLevels.clear();
                        userSelectionModel.permissionLevels.add(PermissionLevel.USER);

                        userSelectionForm.setModelObject(userSelectionModel);
                        permissionLevelDetailForm.setModelObject(userSelectionModel);
                        UserDetailForm.this.setVisible(false);
                    }
                    else {
                        info("No user is selected");
                    }
                }
            });
            add(new Button("cancel")
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    UserDetailForm.this.setVisible(false);
                    UserDetailForm.this.getModelObject().userFilter = "";
                }
            });
        }
    }
}

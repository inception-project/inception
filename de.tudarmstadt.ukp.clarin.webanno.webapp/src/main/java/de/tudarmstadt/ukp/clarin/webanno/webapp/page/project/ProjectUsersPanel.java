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
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.project;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

/**
 * A Panel used to add user permissions to a selected {@link Project}
 *
 * @author Seid Muhie Yimam
 *
 */
public class ProjectUsersPanel
    extends Panel
{
    private static final long serialVersionUID = 875749625429630464L;

    @SpringBean(name = "documentRepository")
    private RepositoryService projectRepository;

    private class UserSelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;

        List<User> userLists = new ArrayList<User>();

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
                            userLists = projectRepository
                                    .listProjectUsersWithPermissions(selectedProject.getObject());
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
                                    .listProjectPermisionLevel(aObject, selectedProject.getObject());
                            List<String> permissionLevels = new ArrayList<String>();
                            for (ProjectPermission projectPermission : projectPermissions) {
                                permissionLevels.add(projectPermission.getLevel().getName());
                            }
                            return aObject.getUsername() + " " + permissionLevels;
                        }
                    });
                    setNullValid(false);
                }

                @Override
                protected void onSelectionChanged(User aNewSelection)
                {
                    if (aNewSelection != null) {
                        selectedUser = aNewSelection;
                        // Clear old selections
                        permissionLevelDetailForm.setModelObject(null);
                        List<ProjectPermission> projectPermissions = projectRepository
                                .listProjectPermisionLevel(selectedUser,
                                        selectedProject.getObject());
                        List<PermissionLevel> levels = new ArrayList<PermissionLevel>();
                        for (ProjectPermission permission : projectPermissions) {
                            levels.add(permission.getLevel());
                        }
                        SelectionModel newSelectionModel = new SelectionModel();
                        newSelectionModel.permissionLevels.addAll(levels);
                        permissionLevelDetailForm.setModelObject(newSelectionModel);

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
            add(new Button("removeUser", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    if (selectedUser != null) {
                        List<ProjectPermission> projectPermissions = projectRepository
                                .listProjectPermisionLevel(selectedUser,
                                        selectedProject.getObject());
                        for (ProjectPermission projectPermission : projectPermissions) {
                            try {
                                projectRepository.removeProjectPermission(projectPermission);
                            }
                            catch (IOException e) {
                                error("Unable to remove project permission level "
                                        + ExceptionUtils.getRootCauseMessage(e));
                            }
                        }
                /*        // Remove any annotation document associated with this user too
                        List<SourceDocument> sourceDocuments = projectRepository
                                .listSourceDocuments(selectedProject.getObject());
                        for (SourceDocument sourceDocument : sourceDocuments) {
                            if (projectRepository.existsAnnotationDocument(sourceDocument,
                                    selectedUser)) {
                                try {
                                    projectRepository.removeCurationDocumentContent(sourceDocument);
                                }
                                catch (IOException e) {
                                   error(e.getMessage());
                                }
                                projectRepository.removeAnnotationDocument(projectRepository
                                        .getAnnotationDocument(sourceDocument, selectedUser));
                            }
                        }*/
                        userLists.remove(selectedUser);
                    }
                    else {
                        info("No user is selected to remove");
                    }
                }
            });

            add(new Button("addPermissionLevel", new ResourceModel("label"))
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

    private class SelectionModel
        implements Serializable
    {
        private static final long serialVersionUID = 9137613222721590389L;

        public List<PermissionLevel> permissionLevels = new ArrayList<PermissionLevel>();
        public User user;
        public List<User> users = new ArrayList<User>();
    }

    private class PermissionLevelDetailForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;

        public PermissionLevelDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));
            add(permissionLevels = new CheckBoxMultipleChoice<PermissionLevel>("permissionLevels")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<PermissionLevel>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<PermissionLevel> load()
                        {
                            return Arrays.asList(new PermissionLevel[] { PermissionLevel.ADMIN,
                                    PermissionLevel.CURATOR, PermissionLevel.USER });
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
            add(new Button("update", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    if (selectedUser != null) {
                        List<ProjectPermission> projectPermissions = projectRepository
                                .listProjectPermisionLevel(selectedUser,
                                        selectedProject.getObject());

                        // Remove old permissionLevels
                        for (ProjectPermission ExistingProjectPermission : projectPermissions) {
                            try {
                                projectRepository
                                        .removeProjectPermission(ExistingProjectPermission);
                            }
                            catch (IOException e) {
                                error("Unable to remove permission level "
                                        + ExceptionUtils.getRootCauseMessage(e));
                            }
                        }

                        for (PermissionLevel level : PermissionLevelDetailForm.this
                                .getModelObject().permissionLevels) {
                            if (!projectRepository.existProjectPermissionLevel(selectedUser,
                                    selectedProject.getObject(), level)) {
                                ProjectPermission projectPermission = new ProjectPermission();
                                projectPermission.setLevel(level);
                                projectPermission.setUser(selectedUser);
                                projectPermission.setProject(selectedProject.getObject());
                                try {
                                    projectRepository.createProjectPermission(projectPermission);
                                }
                                catch (IOException e) {
                                    error("Unable to create Log File "
                                            + ExceptionUtils.getRootCauseMessage(e));
                                }
                            }
                        }
                    }
                    PermissionLevelDetailForm.this.setVisible(false);
                }
            });

            add(new Button("cancel", new ResourceModel("label"))
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
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));
            add(users = (CheckBoxMultipleChoice<User>) new CheckBoxMultipleChoice<User>("users",
                    new LoadableDetachableModel<List<User>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<User> load()
                        {
                            List<User> allUSers = projectRepository.listUsers();
                            allUSers.removeAll(projectRepository
                                    .listProjectUsersWithPermissions(selectedProject.getObject()));
                            return allUSers;
                        }
                    }, new ChoiceRenderer<User>("username", "username")));

            add(new Button("add")
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    if (users.getModelObject() != null) {
                        for (User user : users.getModelObject()) {
                            ProjectPermission projectPermission = new ProjectPermission();
                            projectPermission.setProject(selectedProject.getObject());
                            projectPermission.setUser(user);
                            projectPermission.setLevel(PermissionLevel.USER);
                            try {
                                projectRepository.createProjectPermission(projectPermission);
                            }
                            catch (IOException e) {
                                error("Unable to write to LOG file "
                                        + ExceptionUtils.getRootCauseMessage(e));
                            }
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
                }
            });
        }
    }

    private User selectedUser;

    private ListChoice<User> users;
    private CheckBoxMultipleChoice<PermissionLevel> permissionLevels;

    private UserSelectionForm userSelectionForm;
    private PermissionLevelDetailForm permissionLevelDetailForm;
    private UserDetailForm userDetailForm;

    private Model<Project> selectedProject;

    public ProjectUsersPanel(String id, Model<Project> aProject)
    {
        super(id);
        this.selectedProject = aProject;
        userSelectionForm = new UserSelectionForm("userSelectionForm");

        permissionLevelDetailForm = new PermissionLevelDetailForm("permissionLevelDetailForm");
        permissionLevelDetailForm.setVisible(false);

        userDetailForm = new UserDetailForm("userDetailForm");
        userDetailForm.setVisible(false);

        add(userSelectionForm);
        add(permissionLevelDetailForm);
        add(userDetailForm);
    }
}
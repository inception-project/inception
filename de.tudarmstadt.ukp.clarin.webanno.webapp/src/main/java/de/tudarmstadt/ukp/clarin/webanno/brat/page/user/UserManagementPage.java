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
package de.tudarmstadt.ukp.clarin.webanno.brat.page.user;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.NoResultException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.ApplicationUtils;
import de.tudarmstadt.ukp.clarin.webanno.brat.page.project.SettingsPageBase;
import de.tudarmstadt.ukp.clarin.webanno.brat.support.EntityModel;
import de.tudarmstadt.ukp.clarin.webanno.model.Permissions;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermissions;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

/**
 *A Page for User Management.
 * @author Seid Muhie Yimam
 *
 */
public class UserManagementPage
    extends SettingsPageBase
{
    private static final long serialVersionUID = 1299869948010875439L;

    @SpringBean(name = "documentRepository")
    private RepositoryService projectRepository;

    private ProjectSelectionForm projectSelectionForm;
    private UserSelectionForm userSelectionForm;
    private PermisionLevelsForm permissionLevelsForm;

    // The first project - selected by default
    private Project selectedProject;
    // The first document in the project // auto selected in the first time.
    private User selectedUser;

    private ListChoice<User> users;
    private CheckBoxMultipleChoice<Permissions> permissionLevels;
    private String username;
    private User user;

    public ArrayList<String> levels = new ArrayList<String>();
    List<Project> allowedProject = new ArrayList<Project>();

    public UserManagementPage()
    {
        username = SecurityContextHolder.getContext().getAuthentication().getName();
        user = projectRepository.getUser(username);
        if (getAllowedProjects().size() > 0) {
            selectedProject = getAllowedProjects().get(0);
        }

        projectSelectionForm = new ProjectSelectionForm("projectSelectionForm");
        userSelectionForm = new UserSelectionForm("userSelectionForm");
        permissionLevelsForm = new PermisionLevelsForm("permissionLevelsForm");
        permissionLevelsForm.setOutputMarkupId(true);


        add(projectSelectionForm);
        add(userSelectionForm);
        add(permissionLevelsForm);
    }

    private class ProjectSelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;
        @SuppressWarnings({ "unchecked" })
        private ListChoice<Project> projects;

        public ProjectSelectionForm(String id)
        {
            // super(id);
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

            add(projects = new ListChoice<Project>("project")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<Project>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<Project> load()
                        {

                            return getAllowedProjects();
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<Project>("name"));
                    setNullValid(false);

                }

                @Override
                protected CharSequence getDefaultChoice(String aSelectedValue)
                {
                    return "";
                }
            });
            projects.setOutputMarkupId(true);
            projects.setMaxRows(15);
            projects.add(new OnChangeAjaxBehavior()
            {

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    selectedProject = getModelObject().project;
                    selectedUser = null;
                    aTarget.add(users.setOutputMarkupId(true));
                }
            }).add(new SimpleAttributeModifier("style",
                    "color:green; font-weight:bold;background-color:white; width:100%"));
        }
    }

    private class SelectionModel
        implements Serializable
    {
        private static final long serialVersionUID = -1L;

        private Project project;
        private User user;
    }

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
                        ProjectPermissions permissions;
                        try{
                         permissions = projectRepository.getProjectPermission(aNewSelection, selectedProject);
                        }
                        // no permission yet for this user
                        catch(NoResultException e){
                            permissions = new ProjectPermissions();

                        }
                    permissionLevelsForm.setModelObject(permissions);

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
            users.setOutputMarkupId(true).add(new SimpleAttributeModifier("style",
                    "width:100%"));
            users.setMaxRows(15);
        }
    }

    private class PermisionLevelsForm
        extends Form<ProjectPermissions>
    {
        private static final long serialVersionUID = -1L;

        public PermisionLevelsForm(String id)
        {
            // super(id);
            super(id, new CompoundPropertyModel<ProjectPermissions>(
                    new EntityModel<ProjectPermissions>(new ProjectPermissions())));

            add(permissionLevels = new CheckBoxMultipleChoice<Permissions>("level")
                    {
                        private static final long serialVersionUID = 1L;

                        {
                            setChoices(new LoadableDetachableModel<List<Permissions>>()
                            {
                                private static final long serialVersionUID = 1L;

                                @Override
                                protected List<Permissions> load()
                                {return projectRepository.listLevels();
                                }
                            });
                            setChoiceRenderer(new ChoiceRenderer<Permissions>("level"));
                        }
                    });
            permissionLevels.setOutputMarkupId(true);
            add(new Button("update", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {

                    if (selectedProject != null && selectedUser != null) {
                        // no permissions set yet
                        ProjectPermissions permissions = PermisionLevelsForm.this.getModelObject();
                        if(permissions.getId() == 0){
                            permissions = new ProjectPermissions();
                            permissions.setProject(selectedProject);
                            permissions.setUser(selectedUser);
                            permissions.setLevel((Set<Permissions>) permissionLevels.getModelObject());
                            try {
                                projectRepository.createProjectPermission(permissions);
                            }
                            catch (IOException ex) {
                                error("Unabel to cretae project permissions"
                                        + ExceptionUtils.getRootCauseMessage(ex));
                            }
                        }
                        else{
                            permissions.setLevel(permissions.getLevel());
                        }
                    }
                }
            });
        }
    }

    public List<Project> getAllowedProjects()
    {

        List<Project> allowedProject = new ArrayList<Project>();
        for (Project projects : projectRepository.listProjects()) {
            if (projectRepository.listProjectUserNames(projects).contains(username)
                    && ApplicationUtils.isProjectAdmin(projects, projectRepository, user)) {
                allowedProject.add(projects);
            }
        }

        return allowedProject;
    }
}
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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.users;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;

/**
 * A Panel used to add user permissions to a selected {@link Project}
 */
public class ProjectUsersPanel
    extends ProjectSettingsPanelBase
{
    private static final long serialVersionUID = 875749625429630464L;

    private IModel<Project> project;
    private IModel<User> selectedUser;

    public ProjectUsersPanel(String id, IModel<Project> aProject)
    {
        super(id, aProject);
        
        selectedUser = Model.of();
        project = aProject;

        UserPermissionsPanel permissions = new UserPermissionsPanel("permissions", project,
                selectedUser);
        add(permissions);

        UserSelectionPanel users = new UserSelectionPanel("users", project, selectedUser);
        users.setChangeAction(t -> t.add(permissions));
        add(users);
    }
    
    @Override
    protected void onModelChanged()
    {
        super.onModelChanged();
        selectedUser.setObject(null);
    }
}

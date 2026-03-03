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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.users;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectUserPermissions;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;

/**
 * A Panel used to add user permissions to a selected {@link Project}
 */
public class ProjectUsersPanel
    extends ProjectSettingsPanelBase
{
    private static final long serialVersionUID = 875749625429630464L;

    private IModel<Project> project;
    private IModel<ProjectUserPermissions> selectedUser;

    public ProjectUsersPanel(String id, IModel<Project> aProject)
    {
        super(id, aProject);

        selectedUser = Model.of();
        project = aProject;

        var permissions = new ProjectUserDetailPanel("permissions", project, selectedUser);
        add(permissions);

        var users = new UserSelectionPanel("users", project, selectedUser);
        users.setChangeAction(t -> {
            permissions.modelChanged();
            t.add(permissions);
        });
        add(users);
    }

    @Override
    protected void onModelChanged()
    {
        super.onModelChanged();
        selectedUser.setObject(null);
    }
}

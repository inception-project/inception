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

import java.util.HashSet;

import org.apache.wicket.markup.html.form.ChoiceRenderer;

import de.tudarmstadt.ukp.clarin.webanno.model.ProjectUserPermissions;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectUserPermissions.RenderOptions;

public class ProjectUserPermissionChoiceRenderer
    extends ChoiceRenderer<ProjectUserPermissions>
{
    private static final long serialVersionUID = -5097198610598977245L;

    private boolean showRoles = false;
    private boolean markProjectBoundUsers = false;

    public ProjectUserPermissionChoiceRenderer setShowRoles(boolean aShowRoles)
    {
        showRoles = aShowRoles;
        return this;
    }

    public boolean isShowRoles()
    {
        return showRoles;
    }

    public ProjectUserPermissionChoiceRenderer setMarkProjectBoundUsers(
            boolean aMarkProjectBoundUsers)
    {
        markProjectBoundUsers = aMarkProjectBoundUsers;
        return this;
    }

    public boolean isMarkProjectBoundUsers()
    {
        return markProjectBoundUsers;
    }

    @Override
    public Object getDisplayValue(ProjectUserPermissions aPermissions)
    {
        var options = new HashSet<RenderOptions>();
        if (showRoles) {
            options.add(RenderOptions.SHOW_ROLES);
        }
        if (markProjectBoundUsers) {
            options.add(RenderOptions.MARK_PROJECT_BOUND_USERS);
        }
        return aPermissions.render(options.toArray(RenderOptions[]::new));
    }
}

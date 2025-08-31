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

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.Strings.CS;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.markup.html.form.ChoiceRenderer;

import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectUserPermissions;
import de.tudarmstadt.ukp.clarin.webanno.security.Realm;

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
        var username = aPermissions.getUsername();

        var builder = new StringBuilder();

        aPermissions.getUser().ifPresentOrElse( //
                user -> {
                    builder.append(user.getUiName());
                    if (!aPermissions.getUsername().equals(user.getUiName())) {
                        builder.append(" (");
                        builder.append(aPermissions.getUsername());
                        builder.append(")");
                    }
                }, //
                () -> builder.append(username));

        if (showRoles && !CollectionUtils.isEmpty(aPermissions.getRoles())) {
            builder.append(" ");
            builder.append(aPermissions.getRoles().stream() //
                    .map(PermissionLevel::getName) //
                    .collect(joining(", ", "[", "]")));
        }

        aPermissions.getUser().ifPresentOrElse( //
                user -> { //
                    if (!user.isEnabled()) {
                        builder.append(" (deactivated)");
                    }
                }, //
                () -> builder.append(" (missing!)"));

        if (markProjectBoundUsers && aPermissions.getUser().isPresent()) {
            var user = aPermissions.getUser().get();
            if (CS.startsWith(user.getRealm(), Realm.REALM_PROJECT_PREFIX)) {
                builder.append(" (project user)");
            }
        }

        return builder.toString();
    }
}

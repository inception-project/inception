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
package de.tudarmstadt.ukp.clarin.webanno.model;

import static de.tudarmstadt.ukp.clarin.webanno.model.ProjectUserPermissions.RenderOptions.MARK_PROJECT_BOUND_USERS;
import static de.tudarmstadt.ukp.clarin.webanno.model.ProjectUserPermissions.RenderOptions.SHOW_ROLES;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.ArrayUtils.contains;
import static org.apache.commons.lang3.Strings.CS;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import de.tudarmstadt.ukp.clarin.webanno.security.Realm;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public class ProjectUserPermissions
    implements Serializable
{
    private static final long serialVersionUID = -5379506885078207627L;

    public enum RenderOptions
    {
        SHOW_ROLES, MARK_PROJECT_BOUND_USERS
    }

    private final Project project;
    private final String username;
    private final User user;
    private final Set<PermissionLevel> roles;

    public ProjectUserPermissions(Project aProject, String aUsername, User aUser,
            Set<PermissionLevel> aRoles)
    {
        project = aProject;
        username = aUsername;
        user = aUser;
        roles = aRoles;
    }

    public Project getProject()
    {
        return project;
    }

    public Set<PermissionLevel> getRoles()
    {
        return roles;
    }

    public Optional<User> getUser()
    {
        return Optional.ofNullable(user);
    }

    public String getUsername()
    {
        return username;
    }

    @Override
    public String toString()
    {
        return render(SHOW_ROLES, MARK_PROJECT_BOUND_USERS);
    }

    public String render(RenderOptions... aOptions)
    {
        var builder = new StringBuilder();

        getUser().ifPresentOrElse( //
                u -> {
                    builder.append(u.getUiName());
                    if (!getUsername().equals(u.getUiName())) {
                        builder.append(" (");
                        builder.append(getUsername());
                        builder.append(")");
                    }
                }, //
                () -> builder.append(username));

        if (contains(aOptions, SHOW_ROLES) && !isEmpty(getRoles())) {
            builder.append(" ");
            builder.append(getRoles().stream() //
                    .map(PermissionLevel::getName) //
                    .collect(joining(", ", "[", "]")));
        }

        getUser().ifPresentOrElse( //
                u -> { //
                    if (!u.isEnabled()) {
                        builder.append(" (deactivated)");
                    }
                }, //
                () -> builder.append(" (missing!)"));

        if (contains(aOptions, MARK_PROJECT_BOUND_USERS) && getUser().isPresent()) {
            var u = getUser().get();
            if (CS.startsWith(u.getRealm(), Realm.REALM_PROJECT_PREFIX)) {
                builder.append(" (project user)");
            }
        }

        return builder.toString();
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof ProjectUserPermissions)) {
            return false;
        }
        ProjectUserPermissions castOther = (ProjectUserPermissions) other;
        return new EqualsBuilder().append(project, castOther.project)
                .append(username, castOther.username).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(project).append(username).toHashCode();
    }
}

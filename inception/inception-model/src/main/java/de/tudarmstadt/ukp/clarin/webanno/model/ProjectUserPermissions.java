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

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public class ProjectUserPermissions
    implements Serializable
{
    private static final long serialVersionUID = -5379506885078207627L;

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
}

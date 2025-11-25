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
package de.tudarmstadt.ukp.inception.ui.core.config;

import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_ADMIN;
import static java.util.Arrays.asList;
import static org.apache.commons.collections4.CollectionUtils.containsAny;

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

@ConfigurationProperties("ui.project")
public class ProjectUiPropertiesImpl
    implements ProjectUiProperties
{
    private ProjectBulkActions bulkActions = new ProjectBulkActions();

    @Override
    public ProjectBulkActions getBulkActions()
    {
        return bulkActions;
    }

    public void setBulkActions(ProjectBulkActions aBulkActions)
    {
        bulkActions = aBulkActions;
    }

    public static class ProjectBulkActions
    {
        private Action delete = new Action(false);

        public boolean anyActionsAccessible(User aUser)
        {
            return getDelete().isAccessible(aUser);
        }

        public Action getDelete()
        {
            return delete;
        }

        public void setDelete(Action aDelete)
        {
            delete = aDelete;
        }

    }

    public static class Action
    {
        private boolean enabled;
        private Set<Role> roles = new HashSet<>(asList(ROLE_ADMIN));

        public Action(boolean aEnabled)
        {
            super();
            enabled = aEnabled;
        }

        public boolean isAccessible(User aUser)
        {
            return enabled && containsAny(roles, aUser.getRoles());
        }

        public boolean isEnabled()
        {
            return enabled;
        }

        public void setEnabled(boolean aEnabled)
        {
            enabled = aEnabled;
        }

        public Set<Role> getRoles()
        {
            return roles;
        }

        public void setRoles(Set<Role> aRoles)
        {
            roles = aRoles;
        }
    }
}

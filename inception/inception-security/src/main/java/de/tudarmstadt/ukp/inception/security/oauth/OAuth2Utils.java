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

package de.tudarmstadt.ukp.inception.security.oauth;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;

import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

@Configuration
@ConfigurationProperties(prefix = "oauth2-groups")
public class OAuth2Utils {
    
    private static boolean OAUTH2_GROUPS_ENABLED;
    private static String OAUTH2_ADMIN_GROUP;
    private static String OAUTH2_USER_GROUP;
    private static String OAUTH2_PROJECT_CREATOR_GROUP;
    private static String OAUTH2_REMOTE_GROUP;
    
    @Value("${oauth2-groups.enabled:false}")
    public void setOAuth2GroupsEnabled(boolean oAuth2GroupsEnabled)
    {
        OAUTH2_GROUPS_ENABLED = oAuth2GroupsEnabled;
    }

    @Value("${oauth2-groups.admin:}")
    public void setAdminGroup(String adminGroup)
    {
        OAUTH2_ADMIN_GROUP = adminGroup;
    }

    @Value("${oauth2-groups.user:}")
    public void setUserGroup(String userGroup)
    {
        OAUTH2_USER_GROUP = userGroup;
    }

    @Value("${oauth2-groups.project-creator:}")
    public void setProjectCreatorGroup(String projectCreatorGroup)
    {
        OAUTH2_PROJECT_CREATOR_GROUP = projectCreatorGroup;
    }

    @Value("${oauth2-groups.remote:}")
    public void setRemoteGroup(String remoteGroup)
    {
        OAUTH2_REMOTE_GROUP = remoteGroup;
    }


    public static Set<Role> getOAuth2UserRoles(User aUser, ArrayList<String> oauth2groups)
        throws AccessDeniedException
    {
        Set<Role> roles = new HashSet<>();

        if (!OAUTH2_GROUPS_ENABLED) {
            roles.add(Role.ROLE_USER);
            return roles;
        }

        if (oauth2groups == null || oauth2groups.isEmpty()) {
            throw new AccessDeniedException("OAuth2 groups mapping is enabled, but user ["
                + aUser.getUsername() + "] doesn't have any groups, or the corresponding claim is empty");
        }

        oauth2groups.forEach(group -> matchOauth2groupToRole(group, roles));

        if (roles.isEmpty()) {
            throw new AccessDeniedException("User ["
                + aUser.getUsername() + "] doesn't belong to any role");
        }

        return roles;
    }

    private static void matchOauth2groupToRole(String oauth2group, Set<Role> userRoles) {
        
        if (StringUtils.equals(oauth2group, OAUTH2_ADMIN_GROUP)) {
            userRoles.add(Role.ROLE_ADMIN);
        }

        if (StringUtils.equals(oauth2group, OAUTH2_USER_GROUP)) {
            userRoles.add(Role.ROLE_USER);
        }

        if (StringUtils.equals(oauth2group, OAUTH2_PROJECT_CREATOR_GROUP)) {
            userRoles.add(Role.ROLE_PROJECT_CREATOR);
        }

        if (StringUtils.equals(oauth2group, OAUTH2_REMOTE_GROUP)) {
            userRoles.add(Role.ROLE_REMOTE);
        }
    }
}

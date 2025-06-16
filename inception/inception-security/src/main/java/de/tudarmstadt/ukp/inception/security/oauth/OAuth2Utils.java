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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.core.user.OAuth2User;

import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

@Configuration
@ConfigurationProperties(prefix = "security.oauth.roles")
public class OAuth2Utils {
    
    private static boolean OAUTH2_ROLES_ENABLED;
    private static String OAUTH2_ROLES_CLAIM;
    private static String OAUTH2_ADMIN_ROLE;
    private static String OAUTH2_USER_ROLE;
    private static String OAUTH2_PROJECT_CREATOR_ROLE;
    private static String OAUTH2_REMOTE_ROLE;
    
    @Value("${security.oauth.roles.enabled:false}")
    public void setOAuth2RolesEnabled(boolean oAuth2RolesEnabled)
    {
        OAUTH2_ROLES_ENABLED = oAuth2RolesEnabled;
    }
    
    @Value("${security.oauth.roles.claim:groups}")
    public void setOAuth2RolesClaim(String oAuth2RolesClaim)
    {
        OAUTH2_ROLES_CLAIM = oAuth2RolesClaim;
    }

    @Value("${security.oauth.roles.admin:}")
    public void setAdminRole(String adminRole)
    {
        OAUTH2_ADMIN_ROLE = adminRole;
    }

    @Value("${security.oauth.roles.user:}")
    public void setUserRole(String userRole)
    {
        OAUTH2_USER_ROLE = userRole;
    }

    @Value("${security.oauth.roles.project-creator:}")
    public void setProjectCreatorRole(String projectCreatorRole)
    {
        OAUTH2_PROJECT_CREATOR_ROLE = projectCreatorRole;
    }

    @Value("${security.oauth.roles.remote:}")
    public void setRemoteRole(String remoteRole)
    {
        OAUTH2_REMOTE_ROLE = remoteRole;
    }


    public static Set<Role> getOAuth2UserRoles(User aUser, OAuth2User user)
        throws AccessDeniedException
    {
        Set<Role> roles = new HashSet<>();

        if (!OAUTH2_ROLES_ENABLED) {
            roles.add(Role.ROLE_USER);
            return roles;
        }
        
        List<String> oauth2groups = user.getAttribute(OAUTH2_ROLES_CLAIM);

        if (oauth2groups == null || oauth2groups.isEmpty()) {
            throw new AccessDeniedException("OAuth2 roles mapping is enabled, but user ["
                + aUser.getUsername() + "] doesn't have any roles, or the corresponding claim is empty");
        }

        oauth2groups.forEach(group -> matchOauth2groupToRole(group, roles));

        if (roles.isEmpty()) {
            throw new AccessDeniedException("User ["
                + aUser.getUsername() + "] doesn't belong to any role");
        }

        return roles;
    }

    private static void matchOauth2groupToRole(String oauth2group, Set<Role> userRoles) {
        
        if (StringUtils.equals(oauth2group, OAUTH2_ADMIN_ROLE)) {
            userRoles.add(Role.ROLE_ADMIN);
        }

        if (StringUtils.equals(oauth2group, OAUTH2_USER_ROLE)) {
            userRoles.add(Role.ROLE_USER);
        }

        if (StringUtils.equals(oauth2group, OAUTH2_PROJECT_CREATOR_ROLE)) {
            userRoles.add(Role.ROLE_PROJECT_CREATOR);
        }

        if (StringUtils.equals(oauth2group, OAUTH2_REMOTE_ROLE)) {
            userRoles.add(Role.ROLE_REMOTE);
        }
    }
}

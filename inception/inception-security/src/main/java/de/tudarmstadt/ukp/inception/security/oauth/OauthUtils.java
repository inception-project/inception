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

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;

import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.support.SettingsUtil;


public class OauthUtils {

    private static final Properties settings = SettingsUtil.getSettings();

    public static Set<Role> getOAuth2UserRoles(User aUser, ArrayList<String> oauth2groups)
        throws AccessDeniedException
    {
        Set<Role> roles = new HashSet<>();

        if (!equalsIgnoreCase(settings.getProperty(SettingsUtil.CFG_AUTH_OAUTH2_GROUP_MAPPING_ENABLED), "true")) {
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

        String adminGroup = settings.getProperty(SettingsUtil.CFG_AUTH_OAUTH2_GROUP_ADMIN);
        String userGroup = settings.getProperty(SettingsUtil.CFG_AUTH_OAUTH2_GROUP_USER);
        String projectCreatorGroup = settings.getProperty(SettingsUtil.CFG_AUTH_OAUTH2_GROUP_PROJECT_CREATOR);
        String remoteGroup = settings.getProperty(SettingsUtil.CFG_AUTH_OAUTH2_GROUP_REMOTE);

        if (oauth2group.equals(adminGroup)) {
            userRoles.add(Role.ROLE_ADMIN);
        }

        if (oauth2group.equals(userGroup)) {
            userRoles.add(Role.ROLE_USER);
        }

        if (oauth2group.equals(projectCreatorGroup)) {
            userRoles.add(Role.ROLE_PROJECT_CREATOR);
        }

        if (oauth2group.equals(remoteGroup)) {
            userRoles.add(Role.ROLE_REMOTE);
        }
    }
}

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
package de.tudarmstadt.ukp.clarin.webanno.security.preauth;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.support.SettingsUtil;

public class PreAuthUtils
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static Set<Role> getPreAuthenticationNewUserRoles(User aUser)
    {
        Set<Role> roles = new HashSet<>();
        roles.add(Role.ROLE_USER);

        Properties settings = SettingsUtil.getSettings();
        String extraRoles = settings.getProperty(SettingsUtil.CFG_AUTH_PREAUTH_NEWUSER_ROLES);
        if (StringUtils.isNotBlank(extraRoles)) {
            for (String role : extraRoles.split(",")) {
                try {
                    roles.add(Role.valueOf(role.trim()));
                }
                catch (IllegalArgumentException e) {
                    LOG.debug("Ignoring unknown default role [" + role + "] for user ["
                            + aUser.getUsername() + "]");
                }
            }
        }
        return roles;
    }
}

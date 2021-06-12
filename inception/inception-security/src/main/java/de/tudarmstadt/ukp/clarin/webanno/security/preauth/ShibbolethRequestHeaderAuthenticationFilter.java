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

import static de.tudarmstadt.ukp.clarin.webanno.security.UserDao.EMPTY_PASSWORD;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;

public class ShibbolethRequestHeaderAuthenticationFilter
    extends RequestHeaderAuthenticationFilter
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private UserDao userRepository;

    private void newUserLogin(String aID, HttpServletRequest aRequest)
    {
        User u = new User();
        u.setUsername((String) super.getPreAuthenticatedPrincipal(aRequest));
        u.setPassword(EMPTY_PASSWORD);
        u.setEnabled(true);

        Set<Role> s = new HashSet<>();
        s.add(Role.ROLE_USER);
        Properties settings = SettingsUtil.getSettings();

        String extraRoles = settings.getProperty(SettingsUtil.CFG_AUTH_PREAUTH_NEWUSER_ROLES);
        if (StringUtils.isNotBlank(extraRoles)) {
            for (String role : extraRoles.split(",")) {
                try {
                    s.add(Role.valueOf(role.trim()));
                }
                catch (IllegalArgumentException e) {
                    log.debug("Ignoring unknown default role [" + role + "] for user ["
                            + u.getUsername() + "]");
                }
            }
        }
        u.setRoles(s);

        userRepository.create(u);
    }

    public void setUserRepository(UserDao aUserRepository)
    {
        userRepository = aUserRepository;
    }

    @Override
    protected Object getPreAuthenticatedPrincipal(HttpServletRequest aRequest)
    {
        String username = (String) super.getPreAuthenticatedPrincipal(aRequest);

        if (StringUtils.isBlank(username)) {
            throw new BadCredentialsException("Username cannot be empty");
        }

        if (!userRepository.exists(username)) {
            newUserLogin(username, aRequest);
        }

        return username;
    }
}

/*******************************************************************************
 * Copyright 2014
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.security.preauth;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;

import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.webapp.home.page.SettingsUtil;

public class ShibbolethRequestHeaderAuthenticationFilter
    extends RequestHeaderAuthenticationFilter
{
    private final Log log = LogFactory.getLog(getClass());

    private UserDetailsManager userDetailsManager;

    @Resource(name = "userRepository")
    private UserDao userRepository;

    private void newUserLogin(String aID, HttpServletRequest aRequest)
    {
        User u = new User();
        u.setUsername((String) super.getPreAuthenticatedPrincipal(aRequest));
        u.setPassword("");
        u.setEnabled(true);
        
        Set<Role> s = new HashSet<>();
        s.add(Role.ROLE_USER);
        Properties settings = SettingsUtil.getSettings();
        
        String extraRoles = settings.getProperty("auth.newuser.roles");
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
        log.debug("Created new user [" + u.getUsername() + "] with roles " + u.getRoles());
    }

    private void existingUserLogin(String aID, HttpServletRequest aRequest)
    {
        // Nothing to do
    }

    public void setUserDetailsManager(UserDetailsManager aUserDetailsManager)
    {
        userDetailsManager = aUserDetailsManager;
    }

    protected Object getPreAuthenticatedPrincipal(HttpServletRequest aRequest)
    {
        String o = (String) super.getPreAuthenticatedPrincipal(aRequest);

        if (o != null && !o.equals("")) {
            if (!userDetailsManager.userExists(o)) {
                newUserLogin(o, aRequest);
            }
            else {
                existingUserLogin(o, aRequest);
            }
        }
        return o;
    }
}
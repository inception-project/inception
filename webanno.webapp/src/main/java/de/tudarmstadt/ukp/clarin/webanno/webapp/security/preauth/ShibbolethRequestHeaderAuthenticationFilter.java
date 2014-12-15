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

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;

import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

public class ShibbolethRequestHeaderAuthenticationFilter
    extends RequestHeaderAuthenticationFilter
{

    private UserDetailsManager userDetailsManager;
    private boolean enable = true;

    @Resource(name = "userRepository")
    private UserDao userRepository;

    public void newUserLogin(String aID, HttpServletRequest aRequest)
    {

        User u = new User();
        java.util.Set<Role> s = new java.util.HashSet<Role>();
        s.add(Role.ROLE_USER);
        s.add(Role.ROLE_PROJECT_CREATOR);
        u.setRoles(s);
        u.setUsername(aRequest.getHeader("eduPersonPrincipalName"));
        u.setPassword("");
        u.setEnabled(true);
        userRepository.create(u);
    }

    public User existingUserLogin(String aID, HttpServletRequest aRequest)
    {

        return userRepository.get(aRequest.getHeader("eduPersonPrincipalName"));

    }

    public void setUserDetailsManager(UserDetailsManager aUserDetailsManager)
    {

        userDetailsManager = aUserDetailsManager;
    }

    protected Object getPreAuthenticatedPrincipal(HttpServletRequest aRequest)
    {

        if (!enable)
            return null;
        String o = (String) (super.getPreAuthenticatedPrincipal(aRequest));

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
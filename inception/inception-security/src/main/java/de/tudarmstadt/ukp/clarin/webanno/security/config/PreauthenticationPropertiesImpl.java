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
package de.tudarmstadt.ukp.clarin.webanno.security.config;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.security.preauth.ShibbolethRequestHeaderAuthenticationFilter;

/**
 * This class is exposed as a Spring Component via {@link SecurityAutoConfiguration}.
 * <p>
 * There are additional pre-auth settings which are handled directly by the
 * {@link ShibbolethRequestHeaderAuthenticationFilter}.
 */
@ConfigurationProperties("auth.preauth")
public class PreauthenticationPropertiesImpl
    implements PreauthenticationProperties
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private String logoutUrl;

    private NewUser newuser = new NewUser();

    public static class NewUser
    {
        private List<String> roles = new ArrayList<>();

        public List<String> getRoles()
        {
            return roles;
        }

        public void setRoles(List<String> aRoles)
        {
            roles = aRoles;
        }
    }

    @Override
    public Optional<String> getLogoutUrl()
    {
        return Optional.ofNullable(logoutUrl);
    }

    @Override
    public void setLogoutUrl(String aLogoutUrl)
    {
        logoutUrl = aLogoutUrl;
    }

    public NewUser getNewuser()
    {
        return newuser;
    }

    public void setNewuser(NewUser aNewuser)
    {
        newuser = aNewuser;
    }

    @Override
    public Set<Role> getNewUserRoles(User aUser)
    {
        var roles = new HashSet<Role>();
        roles.add(Role.ROLE_USER);
        for (var roleName : newuser.getRoles()) {
            try {
                roles.add(Role.valueOf(roleName));
            }
            catch (IllegalArgumentException e) {
                LOG.debug("Ignoring unknown default role [{}] for user [{}]", roleName,
                        aUser.getUsername());
            }
        }
        return roles;
    }
}

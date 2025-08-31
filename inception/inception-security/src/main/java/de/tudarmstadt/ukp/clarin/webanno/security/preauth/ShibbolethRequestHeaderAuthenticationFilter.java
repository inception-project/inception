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
import static java.util.stream.Collectors.joining;

import java.lang.invoke.MethodHandles;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.validation.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;

import de.tudarmstadt.ukp.clarin.webanno.security.Realm;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import jakarta.servlet.http.HttpServletRequest;

public class ShibbolethRequestHeaderAuthenticationFilter
    extends RequestHeaderAuthenticationFilter
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private UserDao userRepository;

    private void newUserLogin(String aUsername)
    {
        var u = new User();
        u.setUsername(aUsername);
        u.setPassword(EMPTY_PASSWORD);
        u.setEnabled(true);
        u.setRealm(Realm.REALM_PREAUTH);
        u.setRoles(PreAuthUtils.getPreAuthenticationNewUserRoles(u));

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

        return loadUser(username);
    }

    protected Object loadUser(String username)
    {
        denyAccessToUsersWithIllegalUsername(username);

        var user = userRepository.get(username);
        if (user != null) {
            denyAccessOfRealmsDoNotMatch(Realm.REALM_PREAUTH, user);
            denyAccessToDeactivatedUsers(user);
        }
        else {
            newUserLogin(username);
        }

        return username;
    }

    private void denyAccessToUsersWithIllegalUsername(String aUsername)
    {
        if (StringUtils.isBlank(aUsername)) {
            throw new BadCredentialsException("Username cannot be empty");
        }

        var userNameValidationResult = userRepository.validateUsername(aUsername);
        if (!userNameValidationResult.isEmpty()) {
            var messages = userNameValidationResult.stream() //
                    .map(ValidationError::getMessage) //
                    .collect(joining("\n- ", "\n- ", ""));
            LOG.info("Prevented login of user [{}] with illegal username: {}", aUsername, messages);
            throw new BadCredentialsException("Illegal username");
        }
    }

    private void denyAccessOfRealmsDoNotMatch(String aExpectedRealm, User aUser)
    {
        if (!aExpectedRealm.equals(aUser.getRealm())) {
            LOG.info("Prevented login of user {} from realm [{}] via realm [{}]", aUser,
                    aUser.getRealm(), aExpectedRealm);
            throw new BadCredentialsException("Realm mismatch");
        }
    }

    private void denyAccessToDeactivatedUsers(User user)
    {
        if (!user.isEnabled()) {
            LOG.info("Prevented login of locally deactivated user {}", user);
            throw new BadCredentialsException("User deactivated");
        }
    }
}

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
package de.tudarmstadt.ukp.inception.app.config;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import de.tudarmstadt.ukp.clarin.webanno.security.OverridableUserDetailsManager;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;

public class InceptionSecurityWebUIOAuth2Handling
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private DefaultOAuth2UserService oauth2UserService = new DefaultOAuth2UserService();
    private OidcUserService oidcUserService = new OidcUserService();

    private final UserDao userRepository;
    private final OverridableUserDetailsManager userDetailsManager;

    public InceptionSecurityWebUIOAuth2Handling(UserDao aUserRepository,
            @Lazy OverridableUserDetailsManager aUserDetailsManager)
    {
        userRepository = aUserRepository;
        userDetailsManager = aUserDetailsManager;
    }

    public OAuth2User loadUserOAuth2User(OAuth2UserRequest userRequest)
    {
        var externalUser = oauth2UserService.loadUser(userRequest);
        var user = materializeUser(externalUser);

        var authorities = loadAuthorities(externalUser, user);

        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();

        return new DefaultOAuth2User(authorities, externalUser.getAttributes(),
                userNameAttributeName);
    }

    public OidcUser loadOidcUser(OidcUserRequest userRequest)
    {
        var externalUser = oidcUserService.loadUser(userRequest);
        var user = materializeUser(externalUser);

        var authorities = loadAuthorities(externalUser, user);

        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();

        return new DefaultOidcUser(authorities, externalUser.getIdToken(),
                externalUser.getUserInfo(), userNameAttributeName);
    }

    private LinkedHashSet<GrantedAuthority> loadAuthorities(OAuth2User externalUser, User user)
    {
        var authorities = new LinkedHashSet<GrantedAuthority>();
        authorities.addAll(userDetailsManager.loadUserAuthorities(user.getUsername()));
        authorities.addAll(externalUser.getAuthorities());
        return authorities;
    }

    private User materializeUser(OAuth2User user)
    {
        String username = user.getName();
        var userNameValidationResult = userRepository.validateUsername(username);
        if (!userNameValidationResult.isEmpty()) {
            throw new IllegalArgumentException(userNameValidationResult.get(0).getMessage());
        }

        User u = userRepository.get(username);
        if (u != null) {
            return u;
        }

        u = new User();
        u.setUsername(username);
        u.setPassword(UserDao.EMPTY_PASSWORD);
        u.setEnabled(true);

        String email = user.getAttribute("email");
        if (email != null) {
            var emailNameValidationResult = userRepository.validateEmail(email);
            if (!emailNameValidationResult.isEmpty()) {
                throw new IllegalArgumentException(emailNameValidationResult.get(0).getMessage());
            }

            u.setEmail(email);
        }

        String uiName = user.getAttribute("name");
        if (uiName != null) {
            var uiNameNameValidationResult = userRepository.validateUiName(uiName);
            if (!uiNameNameValidationResult.isEmpty()) {
                throw new IllegalArgumentException(uiNameNameValidationResult.get(0).getMessage());
            }

            u.setUiName(uiName);
        }

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
                    LOG.debug("Ignoring unknown default role [" + role + "] for user ["
                            + u.getUsername() + "]");
                }
            }
        }
        u.setRoles(s);

        userRepository.create(u);

        return u;
    }
}

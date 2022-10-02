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

import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static java.util.Collections.emptyMap;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;

@EnableWebSecurity
public class InceptionSecurityWebUIBuiltInAutoConfiguration
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private DefaultOAuth2UserService oauth2UserService = new DefaultOAuth2UserService();
    private OidcUserService oidcUserService = new OidcUserService();

    private @Autowired UserDao userRepository;

    @Profile("auto-mode-builtin")
    @Bean
    public SecurityFilterChain webUiFilterChain(HttpSecurity aHttp,
            SessionRegistry aSessionRegistry, Environment aEnvironment)
        throws Exception
    {
        aHttp.csrf().disable();
        aHttp.headers().frameOptions().sameOrigin();

        aHttp.authorizeRequests().antMatchers("/login.html*").permitAll() //
                // Resources need to be publicly accessible so they don't trigger the login
                // page. Otherwise it could happen that the user is redirected to a resource
                // upon login instead of being forwarded to a proper application page.
                .antMatchers("/favicon.ico").permitAll() //
                .antMatchers("/favicon.png").permitAll() //
                .antMatchers("/assets/**").permitAll() //
                .antMatchers("/images/**").permitAll() //
                .antMatchers("/resources/**").permitAll() //
                .antMatchers("/whoops").permitAll() //
                .antMatchers("/about/**").permitAll() //
                .antMatchers("/wicket/resource/**").permitAll() //
                .antMatchers("/" + NS_PROJECT + "/*/join-project/**").permitAll() //
                .antMatchers("/swagger-ui/**").access("hasAnyRole('ROLE_REMOTE')") //
                .antMatchers("/swagger-ui.html").access("hasAnyRole('ROLE_REMOTE')") //
                .antMatchers("/v3/**").access("hasAnyRole('ROLE_REMOTE')") //
                .antMatchers("/admin/**").access("hasAnyRole('ROLE_ADMIN')") //
                .antMatchers("/doc/**").access("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')") //
                .antMatchers("/**").access("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')") //
                .anyRequest().denyAll();

        // Must use "defaultAuthenticationEntryPointFor" instead of "formLogin" because
        // if we use formLogin, Spring will handle the form submit and we want the Wicket
        // login page to handle the form submit instead!
        // .formLogin(form -> form.loginPage("/login.html").permitAll())
        aHttp.exceptionHandling().defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint("/login.html"),
                new AntPathRequestMatcher("/**"));

        if (!getOAuth2ClientRegistrations(aEnvironment).isEmpty()) {
            aHttp.oauth2Login() //
                    .loginPage("/login.html") //
                    .userInfoEndpoint() //
                    .oidcUserService(this::loadOidcUser) //
                    .userService(this::loadUserOAuth2User);
        }

        aHttp.sessionManagement()
                // Configuring an unlimited session per-user maximum as a side-effect registers
                // the ConcurrentSessionFilter which checks for valid sessions in the session
                // registry. This allows us to indirectly invalidate a server session by marking
                // its Spring-security registration as invalid and have Spring Security in turn
                // mark the server session as invalid on the next request. This is used e.g. to
                // force-sign-out users that are being deleted.
                .maximumSessions(-1) //
                .sessionRegistry(aSessionRegistry);

        return aHttp.build();
    }

    private Map<String, OAuth2ClientProperties.Registration> getOAuth2ClientRegistrations(
            Environment environment)
    {
        var registrationMap = Bindable.mapOf(String.class,
                OAuth2ClientProperties.Registration.class);
        return Binder.get(environment)
                .bind("spring.security.oauth2.client.registration", registrationMap)
                .orElse(emptyMap());
    }

    private OAuth2User loadUserOAuth2User(OAuth2UserRequest userRequest)
    {
        var externalUser = oauth2UserService.loadUser(userRequest);
        materializeUser(externalUser);
        return externalUser;
    }

    private OidcUser loadOidcUser(OidcUserRequest userRequest)
    {
        var externalUser = oidcUserService.loadUser(userRequest);
        materializeUser(externalUser);
        return externalUser;
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

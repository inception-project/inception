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

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE;
import static org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.security.OverridableUserDetailsManager;
import de.tudarmstadt.ukp.clarin.webanno.security.Realm;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.InceptionSecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User_;
import de.tudarmstadt.ukp.inception.security.config.SecurityOAuthRolesPropertiesImpl;
import de.tudarmstadt.ukp.inception.support.deployment.DeploymentModeService;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationContextProvider;

@Transactional
@ActiveProfiles(DeploymentModeService.PROFILE_AUTH_MODE_DATABASE)
@DataJpaTest( //
        showSql = false, //
        properties = { //
                "spring.liquibase.enabled=false", //
                "spring.main.banner-mode=off" })
@ImportAutoConfiguration({ //
        SecurityAutoConfiguration.class, //
        InceptionSecurityAutoConfiguration.class })
@EntityScan(basePackages = { //
        "de.tudarmstadt.ukp.clarin.webanno.security.model" })
class OAuth2AdapterImplTest
{
    private static final String USERNAME = "ThatGuy";

    @Autowired
    UserDao userService;

    @Autowired
    OAuth2Adapter sut;

    ClientRegistration clientRegistration;
    OAuth2AccessToken oAuth2AccessToken;
    OidcIdToken oidcIdToken;

    @BeforeEach
    void setup()
    {
        clientRegistration = clientRegistration();
        oAuth2AccessToken = oAuth2AccessToken();
        oidcIdToken = oidcIdToken(oAuth2AccessToken);
        userService.delete(USERNAME);
    }

    @Test
    void thatUserIsCreatedIfMissing()
    {
        assertThat(userService.get(USERNAME)) //
                .as("User should not exist when test starts").isNull();

        sut.loadOidcUser(new OidcUserRequest(clientRegistration, oAuth2AccessToken, oidcIdToken));

        User autoCreatedUser = userService.get(USERNAME);
        assertThat(autoCreatedUser) //
                .as("User should have been created as part of the OAuth2 authentication")
                .usingRecursiveComparison() //
                .ignoringFields(User_.CREATED, User_.UPDATED, User_.PASSWORD, "passwordEncoder") //
                .isEqualTo(User.builder() //
                        .withUsername(USERNAME) //
                        .withRealm(Realm.REALM_EXTERNAL_PREFIX
                                + clientRegistration.getRegistrationId())
                        .withRoles(Set.of(Role.ROLE_USER)) //
                        .withEnabled(true) //
                        .build());

        assertThat(UserDao.userHasNoPassword(autoCreatedUser)) //
                .as("Auto-created external users should be created without password") //
                .isTrue();
    }

    @Test
    void thatLoginWithExistingUserIsPossible()
    {
        userService.create(User.builder() //
                .withUsername(USERNAME) //
                .withRealm(Realm.REALM_EXTERNAL_PREFIX + clientRegistration.getRegistrationId())
                .withRoles(Set.of(Role.ROLE_USER)) //
                .withEnabled(true) //
                .build());

        assertThat(userService.get(USERNAME)) //
                .as("User should exist when test starts").isNotNull();

        assertThatNoException().isThrownBy(() -> sut.loadOidcUser(
                new OidcUserRequest(clientRegistration, oAuth2AccessToken, oidcIdToken)));
    }

    @Test
    void thatAccessToDisabledUserIsDenied()
    {
        userService.create(User.builder() //
                .withUsername(USERNAME) //
                .withEnabled(false) //
                .build());

        assertThatExceptionOfType(DisabledException.class) //
                .isThrownBy(() -> sut.loadOidcUser(
                        new OidcUserRequest(clientRegistration, oAuth2AccessToken, oidcIdToken)));
    }

    @Test
    void thatUserWithFunkyUsernameIsDeniedAccess()
    {
        oidcIdToken = oidcIdToken("/etc/passwd", oAuth2AccessToken);
        assertThatExceptionOfType(BadCredentialsException.class) //
                .isThrownBy(() -> sut.loadOidcUser(
                        new OidcUserRequest(clientRegistration, oAuth2AccessToken, oidcIdToken)))
                .withMessageContaining("Illegal username");

        oidcIdToken = oidcIdToken("../escape.zip", oAuth2AccessToken);
        assertThatExceptionOfType(BadCredentialsException.class) //
                .isThrownBy(() -> sut.loadOidcUser(
                        new OidcUserRequest(clientRegistration, oAuth2AccessToken, oidcIdToken)))
                .withMessageContaining("Illegal username");

        oidcIdToken = oidcIdToken("", oAuth2AccessToken);
        assertThatExceptionOfType(BadCredentialsException.class) //
                .isThrownBy(() -> sut.loadOidcUser(
                        new OidcUserRequest(clientRegistration, oAuth2AccessToken, oidcIdToken)))
                .withMessageContaining("Illegal username");

        oidcIdToken = oidcIdToken("*".repeat(2000), oAuth2AccessToken);
        assertThatExceptionOfType(BadCredentialsException.class) //
                .isThrownBy(() -> sut.loadOidcUser(
                        new OidcUserRequest(clientRegistration, oAuth2AccessToken, oidcIdToken)))
                .withMessageContaining("Illegal username");

        oidcIdToken = oidcIdToken("mel\0ove", oAuth2AccessToken);
        assertThatExceptionOfType(BadCredentialsException.class) //
                .isThrownBy(() -> sut.loadOidcUser(
                        new OidcUserRequest(clientRegistration, oAuth2AccessToken, oidcIdToken)))
                .withMessageContaining("Illegal username");

        assertThat(userService.list()).isEmpty();
    }

    @Nested
    class RoleMapping
    {
        private static final String OAUTH2_GROUP_ADMIN = "/INCEPTION_ADMIN";
        private static final String OAUTH2_GROUP_USER = "/INCEPTION_USER";
        private static final String OAUTH2_GROUP_PROJECT_CREATOR = "/INCEPTION_PROJECT_CREATOR";
        private static final String OAUTH2_GROUP_REMOTE = "/INCEPTION_REMOTE";

        OAuth2AdapterImpl sutWithRoleMapping;
        User testUser;
        OAuth2User mockOAuth2User;

        @BeforeEach
        void setup()
        {
            var props = new SecurityOAuthRolesPropertiesImpl();
            props.setEnabled(true);
            props.setClaim("groups");
            props.setAdmin(OAUTH2_GROUP_ADMIN);
            props.setUser(OAUTH2_GROUP_USER);
            props.setProjectCreator(OAUTH2_GROUP_PROJECT_CREATOR);
            props.setRemote(OAUTH2_GROUP_REMOTE);

            sutWithRoleMapping = new OAuth2AdapterImpl(mock(UserDao.class),
                    mock(OverridableUserDetailsManager.class), Optional.empty(), props);

            testUser = new User();
            testUser.setUsername(USERNAME);
            mockOAuth2User = mock(OAuth2User.class);
        }

        @Test
        void thatAdminRoleIsGivenIfMatchingGroupFound()
        {
            var groups = new ArrayList<String>();
            groups.add(OAUTH2_GROUP_ADMIN);
            when(mockOAuth2User.getAttribute(anyString())).thenReturn(groups);

            Set<Role> roles = sutWithRoleMapping.getOAuth2UserRoles(testUser, mockOAuth2User);

            assertTrue(roles.contains(Role.ROLE_ADMIN));
        }

        @Test
        void thatUserRoleIsGivenIfMatchingGroupFound()
        {
            var groups = new ArrayList<String>();
            groups.add(OAUTH2_GROUP_USER);
            when(mockOAuth2User.getAttribute(anyString())).thenReturn(groups);

            Set<Role> roles = sutWithRoleMapping.getOAuth2UserRoles(testUser, mockOAuth2User);

            assertTrue(roles.contains(Role.ROLE_USER));
        }

        @Test
        void thatProjectCreatorRoleIsGivenIfMatchingGroupFound()
        {
            var groups = new ArrayList<String>();
            groups.add(OAUTH2_GROUP_PROJECT_CREATOR);
            when(mockOAuth2User.getAttribute(anyString())).thenReturn(groups);

            Set<Role> roles = sutWithRoleMapping.getOAuth2UserRoles(testUser, mockOAuth2User);

            assertTrue(roles.contains(Role.ROLE_PROJECT_CREATOR));
        }

        @Test
        void thatRemoteRoleIsGivenIfMatchingGroupFound()
        {
            var groups = new ArrayList<String>();
            groups.add(OAUTH2_GROUP_REMOTE);
            when(mockOAuth2User.getAttribute(anyString())).thenReturn(groups);

            Set<Role> roles = sutWithRoleMapping.getOAuth2UserRoles(testUser, mockOAuth2User);

            assertTrue(roles.contains(Role.ROLE_REMOTE));
        }

        @Test
        void thatAccessDeniedIfNoGroupMatchesAnyRole()
        {
            when(mockOAuth2User.getAttribute(anyString())).thenReturn(new ArrayList<String>());

            assertThrows(AccessDeniedException.class,
                    () -> sutWithRoleMapping.getOAuth2UserRoles(testUser, mockOAuth2User));
        }
    }

    @SpringBootConfiguration
    @AutoConfigurationPackage
    public static class SpringConfig
    {
        @Bean
        ApplicationContextProvider applicationContextProvider()
        {
            return new ApplicationContextProvider();
        }

        @Bean
        AuthenticationEventPublisher authenticationEventPublisher()
        {
            return new DefaultAuthenticationEventPublisher();
        }
    }

    private ClientRegistration clientRegistration()
    {
        return ClientRegistration.withRegistrationId("client") //
                .authorizationGrantType(AUTHORIZATION_CODE) //
                .clientId("clientId") //
                .redirectUri("http://dummy") //
                .authorizationUri("http://dummy") //
                .tokenUri("http://dummy") //
                .userNameAttributeName("sub") //
                .build();
    }

    private OidcIdToken oidcIdToken(OAuth2AccessToken aToken)
    {
        return oidcIdToken(USERNAME, aToken);
    }

    private OidcIdToken oidcIdToken(String aUsername, OAuth2AccessToken aToken)
    {
        return OidcIdToken.withTokenValue("dummy") //
                .subject(aUsername) //
                .authTime(aToken.getIssuedAt()) //
                .expiresAt(aToken.getExpiresAt()) //
                .build();
    }

    private OAuth2AccessToken oAuth2AccessToken()
    {
        return new OAuth2AccessToken(BEARER, "dummy", now(), now().plus(10, MINUTES));
    }
}

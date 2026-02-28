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
import static org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE;
import static org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.InceptionSecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.inception.support.deployment.DeploymentModeService;
import net.minidev.json.JSONArray;

/**
 * Integration tests for OAuth2 role mapping via the {@code security.oauth.roles.*} configuration.
 * <p>
 * Unlike the unit tests in {@link OAuth2AdapterImplTest.RoleMapping}, these tests exercise the full
 * Spring-context wiring: properties are read from {@code @DataJpaTest} inline properties, bound to
 * {@link de.tudarmstadt.ukp.inception.security.config.SecurityOAuthRolesPropertiesImpl} via
 * {@code @EnableConfigurationProperties}, injected into the adapter bean, and the resulting role
 * assignment is verified against the real database.
 * <p>
 * One test also exercises the {@code net.minidev.json.JSONArray} runtime type that Nimbus JOSE+JWT
 * produces when decoding a real JWT's non-standard array claim, ensuring that the adapter handles
 * this type correctly without a {@link ClassCastException}.
 */
@Transactional
@ActiveProfiles(DeploymentModeService.PROFILE_AUTH_MODE_DATABASE)
@DataJpaTest( //
        showSql = false, //
        properties = { //
                "spring.liquibase.enabled=false", //
                "spring.main.banner-mode=off", //
                "security.oauth.roles.enabled=true", //
                "security.oauth.roles.claim=groups", //
                "security.oauth.roles.admin=/INCEPTION_ADMIN", //
                "security.oauth.roles.user=/INCEPTION_USER", //
                "security.oauth.roles.project-creator=/INCEPTION_PROJECT_CREATOR", //
                "security.oauth.roles.remote=/INCEPTION_REMOTE" })
@ImportAutoConfiguration({ //
        SecurityAutoConfiguration.class, //
        InceptionSecurityAutoConfiguration.class })
@EntityScan(basePackages = { //
        "de.tudarmstadt.ukp.clarin.webanno.security.model" })
class OAuth2AdapterImplRoleMappingTest
{
    private static final String OAUTH2_GROUP_ADMIN = "/INCEPTION_ADMIN";
    private static final String OAUTH2_GROUP_USER = "/INCEPTION_USER";

    @Autowired
    UserDao userService;

    @Autowired
    OAuth2Adapter sut;

    @Test
    void thatAdminRoleIsPersistedWhenGroupsClaimMatchesAdminGroup()
    {
        var accessToken = oAuth2AccessToken();
        var idToken = OidcIdToken.withTokenValue("dummy") //
                .subject("user-with-admin") //
                .authTime(accessToken.getIssuedAt()) //
                .expiresAt(accessToken.getExpiresAt()) //
                .claim("groups", List.of(OAUTH2_GROUP_ADMIN)) //
                .build();

        sut.loadOidcUser(new OidcUserRequest(clientRegistration(), accessToken, idToken));

        assertThat(userService.get("user-with-admin").getRoles()) //
                .containsExactlyInAnyOrder(Role.ROLE_ADMIN);
    }

    @Test
    void thatAccessIsDeniedWhenNoGroupMatchesAnyRole()
    {
        var accessToken = oAuth2AccessToken();
        var idToken = OidcIdToken.withTokenValue("dummy") //
                .subject("user-without-role") //
                .authTime(accessToken.getIssuedAt()) //
                .expiresAt(accessToken.getExpiresAt()) //
                .claim("groups", List.of("/SOME_UNKNOWN_GROUP")) //
                .build();

        assertThatExceptionOfType(AccessDeniedException.class) //
                .isThrownBy(() -> sut.loadOidcUser( //
                        new OidcUserRequest(clientRegistration(), accessToken, idToken)));
    }

    /**
     * Nimbus JOSE+JWT represents non-standard JSON array claims as
     * {@code net.minidev.json.JSONArray} (which extends {@code ArrayList<Object>}) rather than
     * {@code List<String>}. This test ensures that the groups claim is handled correctly even when
     * it has that runtime type, as it would when decoded from a real JWT by Spring Security's
     * {@code NimbusJwtDecoder}.
     */
    @Test
    void thatUserRoleIsPersistedWhenGroupsClaimTypeIsNimbusJSONArray()
    {
        var groups = new JSONArray();
        groups.add(OAUTH2_GROUP_USER);

        var accessToken = oAuth2AccessToken();
        var idToken = OidcIdToken.withTokenValue("dummy") //
                .subject("user-with-json-array-claim") //
                .authTime(accessToken.getIssuedAt()) //
                .expiresAt(accessToken.getExpiresAt()) //
                .claim("groups", groups) //
                .build();

        sut.loadOidcUser(new OidcUserRequest(clientRegistration(), accessToken, idToken));

        assertThat(userService.get("user-with-json-array-claim").getRoles()) //
                .containsExactlyInAnyOrder(Role.ROLE_USER);
    }

    private ClientRegistration clientRegistration()
    {
        return ClientRegistration.withRegistrationId("keycloak") //
                .authorizationGrantType(AUTHORIZATION_CODE) //
                .clientId("clientId") //
                .redirectUri("http://dummy") //
                .authorizationUri("http://dummy") //
                .tokenUri("http://dummy") //
                .userNameAttributeName("sub") //
                .build();
    }

    private OAuth2AccessToken oAuth2AccessToken()
    {
        return new OAuth2AccessToken(BEARER, "dummy", now(), now().plus(10, MINUTES));
    }
}

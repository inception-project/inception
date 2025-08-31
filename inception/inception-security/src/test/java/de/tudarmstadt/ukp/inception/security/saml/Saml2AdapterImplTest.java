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
package de.tudarmstadt.ukp.inception.security.saml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.security.Realm;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.InceptionSecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User_;
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
class Saml2AdapterImplTest
{
    private static final String USERNAME = "ThatGuy";
    private static final String CLIENT_REGISTRATION_ID = "saml";

    @Autowired
    UserDao userService;

    @Autowired
    Saml2Adapter sut;

    static {
        try {
            var c = Saml2AdapterImplTest.class.getClassLoader()
                    .loadClass("jakarta.xml.bind.JAXBException");
        }
        catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @BeforeEach
    void setup()
    {
        userService.delete(USERNAME);
    }

    @Test
    void thatUserIsCreatedIfMissing()
    {
        assertThat(userService.get(USERNAME)) //
                .as("User should not exist when test starts").isNull();

        sut.loadSamlUser(USERNAME, CLIENT_REGISTRATION_ID);

        User autoCreatedUser = userService.get(USERNAME);
        assertThat(autoCreatedUser) //
                .as("User should have been created as part of the OAuth2 authentication")
                .usingRecursiveComparison() //
                .ignoringFields(User_.CREATED, User_.UPDATED, User_.PASSWORD, "passwordEncoder") //
                .isEqualTo(User.builder() //
                        .withUsername(USERNAME) //
                        .withRealm(Realm.REALM_EXTERNAL_PREFIX + CLIENT_REGISTRATION_ID)
                        .withRoles(Set.of(Role.ROLE_USER)) //
                        .withEnabled(true) //
                        .build());

        assertThat(userService.userHasNoPassword(autoCreatedUser)) //
                .as("Auto-created external users should be created without password") //
                .isTrue();
    }

    @Test
    void thatLoginWithExistingUserIsPossible()
    {
        userService.create(User.builder() //
                .withUsername(USERNAME) //
                .withRealm(Realm.REALM_EXTERNAL_PREFIX + CLIENT_REGISTRATION_ID)
                .withRoles(Set.of(Role.ROLE_USER)) //
                .withEnabled(true) //
                .build());

        assertThat(userService.get(USERNAME)) //
                .as("User should exist when test starts").isNotNull();

        assertThatNoException()
                .isThrownBy(() -> sut.loadSamlUser(USERNAME, CLIENT_REGISTRATION_ID));
    }

    @Test
    void thatAccessToDisabledUserIsDenied()
    {
        userService.create(User.builder() //
                .withUsername(USERNAME) //
                .withEnabled(false) //
                .build());

        assertThatExceptionOfType(DisabledException.class) //
                .isThrownBy(() -> sut.loadSamlUser(USERNAME, CLIENT_REGISTRATION_ID));
    }

    @Test
    void thatUserWithFunkyUsernameIsDeniedAccess()
    {
        assertThatExceptionOfType(BadCredentialsException.class) //
                .isThrownBy(() -> sut.loadSamlUser("/etc/passwd", CLIENT_REGISTRATION_ID))
                .withMessageContaining("Illegal username");

        assertThatExceptionOfType(BadCredentialsException.class) //
                .isThrownBy(() -> sut.loadSamlUser("../escape.zip", CLIENT_REGISTRATION_ID))
                .withMessageContaining("Illegal username");

        assertThatExceptionOfType(BadCredentialsException.class) //
                .isThrownBy(() -> sut.loadSamlUser("", CLIENT_REGISTRATION_ID))
                .withMessageContaining("Illegal username");

        assertThatExceptionOfType(BadCredentialsException.class) //
                .isThrownBy(() -> sut.loadSamlUser("*", CLIENT_REGISTRATION_ID))
                .withMessageContaining("Illegal username");

        assertThatExceptionOfType(BadCredentialsException.class) //
                .isThrownBy(() -> sut.loadSamlUser("mel\0ove", CLIENT_REGISTRATION_ID))
                .withMessageContaining("Illegal username");

        assertThat(userService.list()).isEmpty();
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
}

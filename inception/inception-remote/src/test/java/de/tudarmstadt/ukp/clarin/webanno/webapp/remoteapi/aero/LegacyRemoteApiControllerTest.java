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
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero;

import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_ADMIN;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_REMOTE;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_USER;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.legacy.LegacyRemoteApiController;
import de.tudarmstadt.ukp.inception.search.config.SearchServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.support.deployment.DeploymentModeServiceImpl;

/**
 * Tests for the deprecated legacy remote API. This test lives in the {@code aero} package so that
 * it can reuse {@link MockAeroClient} for setting up projects and permissions.
 */
@ActiveProfiles(DeploymentModeServiceImpl.PROFILE_AUTH_MODE_DATABASE)
@SpringBootTest( //
        webEnvironment = WebEnvironment.MOCK, //
        properties = { //
                "spring.main.banner-mode=off", //
                "search.enabled=false", //
                "remote-api.enabled=true", //
                "remote-api.legacy.enabled=true" })
@EnableWebSecurity
@EnableAutoConfiguration( //
        exclude = { //
                SearchServiceAutoConfiguration.class })
@EntityScan({ //
        "de.tudarmstadt.ukp.inception", //
        "de.tudarmstadt.ukp.clarin.webanno" })
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class LegacyRemoteApiControllerTest
{
    static @TempDir Path tempFolder;

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry)
    {
        registry.add("repository.path", () -> tempFolder.toAbsolutePath().toString());
    }

    private @Autowired WebApplicationContext context;
    private @Autowired UserDao userRepository;

    private MockAeroClient adminActor;

    @BeforeEach
    void setup()
    {
        adminActor = new MockAeroClient(context, "admin", "ADMIN", "REMOTE");

        userRepository.create(new User("admin", ROLE_ADMIN, ROLE_REMOTE));
    }

    private ResultActions legacyListProjects(String aUser, String... aRoles) throws Exception
    {
        return MockMvcBuilders.webAppContextSetup(context) //
                .apply(SecurityMockMvcConfigurers.springSecurity()) //
                .build() //
                .perform(get(LegacyRemoteApiController.API_BASE + "/projects") //
                        .with(user(aUser).roles(aRoles)));
    }

    @Test
    void testProjectListReportsOnlyManagedProjects() throws Exception
    {
        userRepository.create(new User("user", ROLE_USER, ROLE_REMOTE));

        adminActor.createProject("managed") //
                .andExpect(status().isCreated());
        adminActor.createProject("annotated") //
                .andExpect(status().isCreated());
        adminActor.createProject("unrelated") //
                .andExpect(status().isCreated());

        adminActor.grantProjectRole(1, "user", "MANAGER") //
                .andExpect(status().isOk());
        adminActor.grantProjectRole(2, "user", "ANNOTATOR", "CURATOR") //
                .andExpect(status().isOk());

        // Only the project in which the user is a manager may be reported - not the one where
        // they are merely an annotator/curator and not the one they have no roles in at all. The
        // projects are keyed by their numeric ID in the legacy response format.
        legacyListProjects("user", "USER", "REMOTE") //
                .andExpect(status().isOk()) //
                .andExpect(jsonPath("$.1.managed").exists()) //
                .andExpect(jsonPath("$.2").doesNotExist()) //
                .andExpect(jsonPath("$.3").doesNotExist());
    }

    @Test
    void testProjectListReportsAllProjectsForAdministrator() throws Exception
    {
        adminActor.createProject("project1") //
                .andExpect(status().isCreated());
        adminActor.createProject("project2") //
                .andExpect(status().isCreated());

        // The administrator sees all projects.
        legacyListProjects("admin", "ADMIN", "REMOTE") //
                .andExpect(status().isOk()) //
                .andExpect(jsonPath("$.1.project1").exists()) //
                .andExpect(jsonPath("$.2.project2").exists());
    }

    @SpringBootConfiguration
    public static class TestContext
    {
        @Bean
        AuthenticationEventPublisher authenticationEventPublisher()
        {
            return new DefaultAuthenticationEventPublisher();
        }
    }
}

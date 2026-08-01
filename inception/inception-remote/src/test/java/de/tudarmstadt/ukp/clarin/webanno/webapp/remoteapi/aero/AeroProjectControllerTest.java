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
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.context.WebApplicationContext;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.search.config.SearchServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.support.deployment.DeploymentModeServiceImpl;

@ActiveProfiles(DeploymentModeServiceImpl.PROFILE_AUTH_MODE_DATABASE)
@SpringBootTest( //
        webEnvironment = WebEnvironment.MOCK, //
        properties = { //
                "spring.main.banner-mode=off", //
                "search.enabled=false", //
                "remote-api.enabled=true" })
@EnableWebSecurity
@EnableAutoConfiguration( //
        exclude = { //
                SearchServiceAutoConfiguration.class })
@EntityScan({ //
        "de.tudarmstadt.ukp.inception", //
        "de.tudarmstadt.ukp.clarin.webanno" })
@TestMethodOrder(MethodOrderer.MethodName.class)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class AeroProjectControllerTest
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

    @Test
    void testCreateAndDelete() throws Exception
    {
        adminActor.listProjects() //
                .andExpect(status().isOk()) //
                .andExpect(content().contentType(APPLICATION_JSON_VALUE)) //
                .andExpect(jsonPath("$.messages").isEmpty());

        adminActor.createProject("project1") //
                .andExpect(status().isCreated()) //
                .andExpect(content().contentType(APPLICATION_JSON_VALUE)) //
                .andExpect(jsonPath("$.body.id").value("1")) //
                .andExpect(jsonPath("$.body.name").value("project1"));

        adminActor.listProjects() //
                .andExpect(status().isOk()) //
                .andExpect(content().contentType(APPLICATION_JSON_VALUE)) //
                .andExpect(jsonPath("$.body[0].id").value("1")) //
                .andExpect(jsonPath("$.body[0].name").value("project1"));

        adminActor.deleteProject(1l) //
                .andExpect(status().isOk()) //
                .andExpect(content().contentType(APPLICATION_JSON_VALUE)) //
                .andExpect(jsonPath("$.messages[0].level").value("INFO")) //
                .andExpect(jsonPath("$.messages[0].message").value(containsString("deleted")));

        adminActor.listProjects() //
                .andExpect(status().isOk()) //
                .andExpect(content().contentType(APPLICATION_JSON_VALUE)) //
                .andExpect(jsonPath("$.messages").isEmpty());
    }

    @Test
    void testReadProjectByIdAndBySlug() throws Exception
    {
        adminActor.createProject("project1") //
                .andExpect(status().isCreated()) //
                .andExpect(jsonPath("$.body.id").value("1"));

        adminActor.readProject(1l) //
                .andExpect(status().isOk()) //
                .andExpect(content().contentType(APPLICATION_JSON_VALUE)) //
                .andExpect(jsonPath("$.body.id").value("1")) //
                .andExpect(jsonPath("$.body.slug").value("project1"));

        adminActor.readProject("project1") //
                .andExpect(status().isOk()) //
                .andExpect(content().contentType(APPLICATION_JSON_VALUE)) //
                .andExpect(jsonPath("$.body.id").value("1")) //
                .andExpect(jsonPath("$.body.slug").value("project1"));
    }

    @Test
    void testReadProjectByUnknownSlugIsNotFound() throws Exception
    {
        adminActor.createProject("project1") //
                .andExpect(status().isCreated());

        adminActor.readProject("no-such-project") //
                .andExpect(status().isNotFound()) //
                .andExpect(jsonPath("$.messages[0].level").value("ERROR")) //
                .andExpect(
                        jsonPath("$.messages[0].message").value(containsString("no-such-project")));
    }

    @Test
    void testReadProjectByUnknownIdIsNotFound() throws Exception
    {
        adminActor.createProject("project1") //
                .andExpect(status().isCreated());

        adminActor.readProject(9999l) //
                .andExpect(status().isNotFound()) //
                .andExpect(jsonPath("$.messages[0].level").value("ERROR"));
    }

    @Test
    void testReadProjectByMalformedIdentifierIsNotFound() throws Exception
    {
        adminActor.createProject("project1") //
                .andExpect(status().isCreated());

        // These are all digit sequences which cannot be parsed as a numeric project ID. Since they
        // are not valid slugs either, they must be reported as not found instead of causing an
        // internal server error.
        for (var identifier : new String[] { //
                "99999999999999999999", // exceeds the long range
                "٤٢", // Arabic-Indic digits
                "-1" }) {
            adminActor.readProject(identifier) //
                    .andExpect(status().isNotFound()) //
                    .andExpect(jsonPath("$.messages[0].level").value("ERROR"));
        }
    }

    @Test
    void testProjectResponseExposesSlugAndDeprecatedName() throws Exception
    {
        // Use a slug which differs from the title so that mixing up the two fields is actually
        // detectable - "name" carries the slug for backwards compatibility while the
        // human-readable project name is reported as "title".
        adminActor.createProject("project-slug", "Project Title") //
                .andExpect(status().isCreated()) //
                .andExpect(jsonPath("$.body.slug").value("project-slug")) //
                .andExpect(jsonPath("$.body.name").value("project-slug")) //
                .andExpect(jsonPath("$.body.title").value("Project Title"));

        adminActor.readProject("project-slug") //
                .andExpect(status().isOk()) //
                .andExpect(jsonPath("$.body.slug").value("project-slug")) //
                .andExpect(jsonPath("$.body.name").value("project-slug")) //
                .andExpect(jsonPath("$.body.title").value("Project Title"));

        adminActor.listProjects() //
                .andExpect(status().isOk()) //
                .andExpect(jsonPath("$.body[0].slug").value("project-slug")) //
                .andExpect(jsonPath("$.body[0].name").value("project-slug")) //
                .andExpect(jsonPath("$.body[0].title").value("Project Title"));
    }

    @Test
    void testCreateProjectUsingDeprecatedNameParameter() throws Exception
    {
        // The "name" parameter sets the slug - it is deprecated in favor of "slug" but must
        // keep working for existing clients.
        adminActor.createProjectUsingNameParameter("project-slug") //
                .andExpect(status().isCreated()) //
                .andExpect(jsonPath("$.body.slug").value("project-slug")) //
                .andExpect(jsonPath("$.body.name").value("project-slug")) //
                .andExpect(jsonPath("$.body.title").value("project-slug"));

        adminActor.readProject("project-slug") //
                .andExpect(status().isOk()) //
                .andExpect(jsonPath("$.body.slug").value("project-slug"));
    }

    @Test
    void testCreateProjectWithBothSlugAndNameIsRejected() throws Exception
    {
        adminActor.createProjectUsingSlugAndNameParameters("project-slug", "other-slug") //
                .andExpect(status().isBadRequest()) //
                .andExpect(jsonPath("$.messages[0].level").value("ERROR"));

        // Nothing must have been created
        adminActor.listProjects() //
                .andExpect(status().isOk()) //
                .andExpect(jsonPath("$.body").isEmpty());
    }

    @Test
    void testCreateProjectWithoutSlugOrNameIsRejected() throws Exception
    {
        adminActor.createProjectWithoutSlugOrName() //
                .andExpect(status().isBadRequest()) //
                .andExpect(jsonPath("$.messages[0].level").value("ERROR"));

        adminActor.listProjects() //
                .andExpect(status().isOk()) //
                .andExpect(jsonPath("$.body").isEmpty());
    }

    @Test
    void testDeleteProjectBySlug() throws Exception
    {
        adminActor.createProject("project1") //
                .andExpect(status().isCreated());

        adminActor.deleteProject("project1") //
                .andExpect(status().isOk()) //
                .andExpect(jsonPath("$.messages[0].message").value(containsString("deleted")));

        adminActor.listProjects() //
                .andExpect(status().isOk()) //
                .andExpect(jsonPath("$.body").isEmpty());
    }

    @SpringBootConfiguration
    static class TestContext
    {
        @Bean
        AuthenticationEventPublisher authenticationEventPublisher()
        {
            return new DefaultAuthenticationEventPublisher();
        }
    }
}

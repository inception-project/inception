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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
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

        var project = adminActor.toRProject(adminActor.createProject("project1") //
                .andExpect(content().contentType(APPLICATION_JSON_VALUE)) //
                .andExpect(jsonPath("$.body.name").value("project1")));

        adminActor.listProjects() //
                .andExpect(status().isOk()) //
                .andExpect(content().contentType(APPLICATION_JSON_VALUE)) //
                .andExpect(jsonPath("$.body[0].id").value(project.id())) //
                .andExpect(jsonPath("$.body[0].name").value("project1"));

        adminActor.deleteProject(project.id()) //
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
        var project = adminActor.createProjectAndGet("project1");

        // Addressing the project by its numeric ID and by its slug must resolve to the same
        // project.
        adminActor.readProject(project.id()) //
                .andExpect(status().isOk()) //
                .andExpect(content().contentType(APPLICATION_JSON_VALUE)) //
                .andExpect(jsonPath("$.body.id").value(project.id())) //
                .andExpect(jsonPath("$.body.slug").value("project1"));

        adminActor.readProject("project1") //
                .andExpect(status().isOk()) //
                .andExpect(content().contentType(APPLICATION_JSON_VALUE)) //
                .andExpect(jsonPath("$.body.id").value(project.id())) //
                .andExpect(jsonPath("$.body.slug").value("project1"));
    }

    @Test
    void testProjectResponseExposesRolesOfSessionOwner() throws Exception
    {
        var userActor = new MockAeroClient(context, "user", "USER", "REMOTE");
        userRepository.create(new User("user", ROLE_USER, ROLE_REMOTE));

        var project = adminActor.createProjectAndGet("project1");

        adminActor.grantProjectRole(project.id(), "user", "MANAGER", "CURATOR") //
                .andExpect(status().isOk());

        // The roles must be those of the user making the request - not those of any other user.
        // The admin holds all roles in this project, so reporting the admin's roles here would
        // be detectable.
        userActor.listProjects() //
                .andExpect(status().isOk()) //
                .andExpect(jsonPath("$.body[0].roles", contains("CURATOR", "MANAGER")));

        userActor.readProject(project.id()) //
                .andExpect(status().isOk()) //
                .andExpect(jsonPath("$.body.roles", contains("CURATOR", "MANAGER")));
    }

    @Test
    void testProjectsWithoutManagerRoleAreNotListedOrReadable() throws Exception
    {
        var userActor = new MockAeroClient(context, "user", "USER", "REMOTE");
        userRepository.create(new User("user", ROLE_USER, ROLE_REMOTE));

        var project = adminActor.createProjectAndGet("project1");

        adminActor.grantProjectRole(project.id(), "user", "ANNOTATOR", "CURATOR") //
                .andExpect(status().isOk());

        // The whole remote API requires the manager role, so a user who is merely an annotator or
        // curator in a project must not see it in the project list at all.
        userActor.listProjects() //
                .andExpect(status().isOk()) //
                .andExpect(jsonPath("$.body").isEmpty());

        userActor.readProject(project.id()) //
                .andExpect(status().isForbidden());

        userActor.readProject("project1") //
                .andExpect(status().isForbidden());
    }

    @Test
    void testProjectListReportsOnlyManagedProjects() throws Exception
    {
        var userActor = new MockAeroClient(context, "user", "USER", "REMOTE");
        userRepository.create(new User("user", ROLE_USER, ROLE_REMOTE));

        var managedProject = adminActor.createProjectAndGet("managed");
        var annotatedProject = adminActor.createProjectAndGet("annotated");
        adminActor.createProject("unrelated") //
                .andExpect(status().isCreated());

        adminActor.grantProjectRole(managedProject.id(), "user", "MANAGER") //
                .andExpect(status().isOk());
        adminActor.grantProjectRole(annotatedProject.id(), "user", "ANNOTATOR", "CURATOR") //
                .andExpect(status().isOk());

        // Only the project in which the user is a manager may be reported - not the one where
        // they are merely an annotator/curator and not the one they have no roles in at all.
        userActor.listProjects() //
                .andExpect(status().isOk()) //
                .andExpect(jsonPath("$.body", hasSize(1))) //
                .andExpect(jsonPath("$.body[0].slug").value("managed")) //
                .andExpect(jsonPath("$.body[0].roles", contains("MANAGER")));

        // The administrator sees all projects, including those without any roles.
        adminActor.listProjects() //
                .andExpect(status().isOk()) //
                .andExpect(jsonPath("$.body", hasSize(3)));
    }

    @Test
    void testProjectResponseReportsNoRolesForAdminWithoutRoles() throws Exception
    {
        userRepository.create(new User("user", ROLE_USER, ROLE_REMOTE));

        // Create the project on behalf of the other user so that the admin ends up without any
        // roles in it - an admin creating a project for themselves would receive all roles.
        var project = adminActor.createProjectForCreatorAndGet("project1", "user");

        // The admin can access the project but holds no explicitly assigned roles in it, so the
        // roles must be reported as empty rather than being omitted or guessed from the access.
        adminActor.listProjects() //
                .andExpect(status().isOk()) //
                .andExpect(jsonPath("$.body[0].roles").isArray()) //
                .andExpect(jsonPath("$.body[0].roles").isEmpty());

        adminActor.readProject(project.id()) //
                .andExpect(status().isOk()) //
                .andExpect(jsonPath("$.body.roles").isEmpty());
    }

    @Test
    void testProjectRolesAreReportedInStableOrder() throws Exception
    {
        var userActor = new MockAeroClient(context, "user", "USER", "REMOTE");
        userRepository.create(new User("user", ROLE_USER, ROLE_REMOTE));

        var project = adminActor.createProjectAndGet("project1");

        // Assign the roles in an order which differs from the order in which they must be
        // reported so that the sorting is actually detectable.
        adminActor.grantProjectRole(project.id(), "user", "CURATOR", "MANAGER", "ANNOTATOR") //
                .andExpect(status().isOk());

        userActor.listProjects() //
                .andExpect(status().isOk()) //
                .andExpect(
                        jsonPath("$.body[0].roles", contains("ANNOTATOR", "CURATOR", "MANAGER")));
    }

    @Test
    void testCreateProjectForCreatorReportsRolesOfCreator() throws Exception
    {
        userRepository.create(new User("user", ROLE_USER, ROLE_REMOTE));

        // When an administrator creates a project on behalf of somebody else, it is that other
        // user who receives the roles - so it is their roles which must be reported. Reporting
        // the roles of the administrator would always yield an empty list here.
        adminActor.createProjectForCreator("project1", "user") //
                .andExpect(status().isCreated()) //
                .andExpect(jsonPath("$.body.roles", contains("ANNOTATOR", "CURATOR", "MANAGER")));
    }

    @Test
    void testCreateProjectReportsOwnRolesWhenNoCreatorGiven() throws Exception
    {
        adminActor.createProject("project1") //
                .andExpect(status().isCreated()) //
                .andExpect(jsonPath("$.body.roles", contains("ANNOTATOR", "CURATOR", "MANAGER")));
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

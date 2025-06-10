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

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_ADMIN;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_PROJECT_CREATOR;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_REMOTE;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_USER;
import static org.apache.commons.io.FileUtils.writeByteArrayToFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.context.WebApplicationContext;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.log.config.EventLoggingAutoConfiguration;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.search.config.SearchServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.support.deployment.DeploymentModeServiceImpl;
import de.tudarmstadt.ukp.inception.support.logging.Logging;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationContextProvider;

@ActiveProfiles(DeploymentModeServiceImpl.PROFILE_AUTH_MODE_DATABASE)
@SpringBootTest( //
        webEnvironment = WebEnvironment.MOCK, //
        properties = { //
                "spring.main.banner-mode=off", //
                "remote-api.enabled=true", //
                "repository.path=" + AeroProjectController_ProjectExport_Test.TEST_OUTPUT_FOLDER })
@EnableWebSecurity
@EnableAutoConfiguration( //
        exclude = { //
                LiquibaseAutoConfiguration.class, //
                EventLoggingAutoConfiguration.class, //
                SearchServiceAutoConfiguration.class })
@EntityScan({ //
        "de.tudarmstadt.ukp.inception", //
        "de.tudarmstadt.ukp.clarin.webanno" })
@TestMethodOrder(MethodOrderer.MethodName.class)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class AeroProjectController_ProjectExport_Test
{
    static final String TEST_OUTPUT_FOLDER = "target/test-output/AeroRemoteApiController_Project_Test";

    private @Autowired WebApplicationContext context;
    private @Autowired UserDao userService;
    private @Autowired ProjectService projectService;
    private @Autowired RepositoryProperties repositoryProperties;

    private MockAeroClient adminActor;
    private MockAeroClient projectCreatorActor;

    @BeforeAll
    static void setupClass()
    {
        FileSystemUtils.deleteRecursively(new File(TEST_OUTPUT_FOLDER));
    }

    @BeforeEach
    void setup()
    {
        adminActor = new MockAeroClient(context, "admin", "ADMIN", "REMOTE");
        projectCreatorActor = new MockAeroClient(context, "projectCreator", "PROJECT_CREATOR",
                "REMOTE");

        userService.create(new User("admin", ROLE_ADMIN, ROLE_REMOTE));
        userService.create(new User("projectCreator", ROLE_PROJECT_CREATOR, ROLE_REMOTE));

        MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
        new ApplicationContextProvider().setApplicationContext(context);
    }

    @AfterEach
    void tearDown()
    {
        MDC.clear();
    }

    @Test
    void testExportAndImport(@TempDir Path aTempDir) throws Exception
    {
        adminActor.createProject("project1") //
                .andExpect(status().isCreated()) //
                .andExpect(jsonPath("$.body.id").value("1"));

        var result = adminActor.exportProject(1l) //
                .andExpect(status().isOk()) //
                .andExpect(content().contentType("application/zip")).andReturn();

        var exportFile = aTempDir.resolve("export.zip").toFile();
        writeByteArrayToFile(exportFile, result.getResponse().getContentAsByteArray());

        adminActor.importProject(exportFile, false, false) //
                .andExpect(status().isOk()) //
                .andExpect(jsonPath("$.body.id").value("2"));
    }

    @Test
    void testExportAndImportWithUsersAndPermissionsAdmin(@TempDir Path aTempDir) throws Exception
    {
        var manager = User.builder() //
                .withUsername("manager") //
                .withRoles(ROLE_USER) //
                .withEnabled(true) //
                .build();
        userService.create(manager);

        var annotator = User.builder() //
                .withUsername("annotator") //
                .withRoles(ROLE_USER) //
                .withEnabled(true) //
                .build();
        userService.create(annotator);

        var project = Project.builder() //
                .withName("test") //
                .withSlug("test") //
                .build();
        projectService.createProject(project);

        projectService.assignRole(project, manager, MANAGER);
        projectService.assignRole(project, annotator, ANNOTATOR);

        var result = adminActor.exportProject(1l) //
                .andExpect(status().isOk()) //
                .andExpect(content().contentType("application/zip")).andReturn();

        var exportFile = aTempDir.resolve("export.zip").toFile();
        writeByteArrayToFile(exportFile, result.getResponse().getContentAsByteArray());

        MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
        projectService.removeProject(project);
        userService.delete(manager);
        userService.delete(annotator);

        assertThat(projectService.existsProjectWithSlug(project.getSlug())).isFalse();
        assertThat(userService.exists(manager.getUsername())).isFalse();
        assertThat(userService.exists(annotator.getUsername())).isFalse();

        adminActor.importProject(exportFile, true, true) //
                .andExpect(status().isOk()) //
                .andExpect(jsonPath("$.body.id").value("2"));

        var actualProject = projectService.getProjectBySlug(project.getSlug());
        var actualProjectCreator = userService.get(manager.getUsername());
        var actualAnnotator = userService.get(annotator.getUsername());
        assertThat(actualAnnotator) //
                .isNotNull() //
                .satisfies(u -> assertThat(projectService.hasRole(u, actualProject, ANNOTATOR))
                        .isTrue()) //
                .satisfies(u -> assertThat(u.isEnabled()).isFalse());
        assertThat(actualProjectCreator) //
                .isNotNull() //
                .satisfies(
                        u -> assertThat(projectService.hasRole(u, actualProject, MANAGER)).isTrue()) //
                .satisfies(u -> assertThat(u.isEnabled()).isFalse());
    }

    @Test
    void testExportAndImportWithUsersAndPermissionsProjectCreator(@TempDir Path aTempDir)
        throws Exception
    {
        var manager = User.builder() //
                .withUsername("manager") //
                .withRoles(ROLE_USER) //
                .withEnabled(true) //
                .build();
        userService.create(manager);

        var annotator = User.builder() //
                .withUsername("annotator") //
                .withRoles(ROLE_USER) //
                .withEnabled(true) //
                .build();
        userService.create(annotator);

        var project = Project.builder() //
                .withName("test") //
                .withSlug("test") //
                .build();
        projectService.createProject(project);

        projectService.assignRole(project, manager, MANAGER);
        projectService.assignRole(project, annotator, ANNOTATOR);

        var result = adminActor.exportProject(1l) //
                .andExpect(status().isOk()) //
                .andExpect(content().contentType("application/zip")).andReturn();

        var exportFile = aTempDir.resolve("export.zip").toFile();
        writeByteArrayToFile(exportFile, result.getResponse().getContentAsByteArray());

        MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
        projectService.removeProject(project);
        userService.delete(manager);
        userService.delete(annotator);

        assertThat(projectService.existsProjectWithSlug(project.getSlug())).isFalse();
        assertThat(userService.exists(manager.getUsername())).isFalse();
        assertThat(userService.exists(annotator.getUsername())).isFalse();

        projectCreatorActor.importProject(exportFile, true, true) //
                .andExpect(status().isForbidden());

        projectCreatorActor.importProject(exportFile, false, true) //
                .andExpect(status().isForbidden());

        projectCreatorActor.importProject(exportFile, true, false) //
                .andExpect(status().isForbidden());

        projectCreatorActor.importProject(exportFile, false, false) //
                .andExpect(status().isOk()) //
                .andExpect(jsonPath("$.body.id").value("2"));

        assertThat(userService.exists(manager.getUsername())).isFalse();
        assertThat(userService.exists(annotator.getUsername())).isFalse();
    }

    @SpringBootConfiguration
    public static class TestContext
    {
        // All handled by auto-config
    }
}

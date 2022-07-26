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
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_USER;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.context.WebApplicationContext;

import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.text.config.TextFormatsAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.config.RemoteApiAutoConfiguration;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.curation.config.CurationDocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.config.DocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.export.config.DocumentImportExportServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.log.config.EventLoggingAutoConfiguration;
import de.tudarmstadt.ukp.inception.project.export.config.ProjectExportServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.schema.config.AnnotationSchemaServiceAutoConfiguration;

@EnableAutoConfiguration(exclude = { LiquibaseAutoConfiguration.class,
        EventLoggingAutoConfiguration.class })
@SpringBootTest(webEnvironment = WebEnvironment.MOCK, //
        properties = { //
                "spring.main.banner-mode=off", //
                "remote-api.enabled=true", //
                "repository.path=" + AeroRemoteApiController_Permissions_Test.TEST_OUTPUT_FOLDER })
@EnableWebSecurity
@Import({ //
        ProjectServiceAutoConfiguration.class, //
        ProjectExportServiceAutoConfiguration.class, //
        AnnotationSchemaServiceAutoConfiguration.class, //
        CasStorageServiceAutoConfiguration.class, //
        CurationDocumentServiceAutoConfiguration.class, //
        TextFormatsAutoConfiguration.class, //
        DocumentImportExportServiceAutoConfiguration.class, //
        DocumentServiceAutoConfiguration.class, //
        RepositoryAutoConfiguration.class, //
        SecurityAutoConfiguration.class, //
        RemoteApiAutoConfiguration.class })
@EntityScan({ //
        "de.tudarmstadt.ukp.clarin.webanno.model", //
        "de.tudarmstadt.ukp.clarin.webanno.security.model" })
@TestMethodOrder(MethodOrderer.MethodName.class)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class AeroRemoteApiController_Permissions_Test
{
    static final String TEST_OUTPUT_FOLDER = "target/test-output/AeroRemoteApiController_Permissions_Test";

    private @Autowired WebApplicationContext context;
    private @Autowired UserDao userRepository;

    private MockAeroClient adminActor;
    private MockAeroClient userActor;

    @BeforeAll
    public static void setupClass()
    {
        FileSystemUtils.deleteRecursively(new File(TEST_OUTPUT_FOLDER));
    }

    @BeforeEach
    public void setup() throws Exception
    {
        adminActor = new MockAeroClient(context, "admin", "ADMIN");
        userActor = new MockAeroClient(context, "user", "USER");

        userRepository.create(new User("admin", ROLE_ADMIN));
        userRepository.create(new User("user", ROLE_USER));

        adminActor.createProject("project1").andExpect(status().isCreated())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.body.id").value("1"))
                .andExpect(jsonPath("$.body.name").value("project1"));
    }

    @Test
    public void testGrantAndRevokePermissions() throws Exception
    {
        adminActor.listPermissionsForUser(1, "user") //
                .andExpect(status().isOk()) //
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.body").isEmpty());

        adminActor.grantProjectRole(1, "user", "ANNOTATOR", "CURATOR") //
                .andExpect(status().isOk()) //
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.body[0].project").value("1"))
                .andExpect(jsonPath("$.body[0].user").value("user"))
                .andExpect(jsonPath("$.body[0].role").value("CURATOR"))
                .andExpect(jsonPath("$.body[1].project").value("1"))
                .andExpect(jsonPath("$.body[1].user").value("user"))
                .andExpect(jsonPath("$.body[1].role").value("ANNOTATOR"));

        adminActor.revokeProjectRole(1, "user", "CURATOR") //
                .andExpect(status().isOk()) //
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.body[0].project").value("1"))
                .andExpect(jsonPath("$.body[0].user").value("user"))
                .andExpect(jsonPath("$.body[0].role").value("ANNOTATOR"));

        adminActor.revokeProjectRole(1, "user", "ANNOTATOR") //
                .andExpect(status().isOk()) //
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.body").isEmpty());
    }

    @Test
    public void testPermissionListAll() throws Exception
    {
        adminActor.grantProjectRole(1, "user", "ANNOTATOR", "CURATOR") //
                .andExpect(status().isOk());

        adminActor.listPermissionsForProject(1) //
                .andExpect(status().isOk()) //
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.body[0].project").value("1"))
                .andExpect(jsonPath("$.body[0].user").value("admin"))
                .andExpect(jsonPath("$.body[0].role").value("MANAGER"))
                .andExpect(jsonPath("$.body[1].project").value("1"))
                .andExpect(jsonPath("$.body[1].user").value("admin"))
                .andExpect(jsonPath("$.body[1].role").value("CURATOR"))
                .andExpect(jsonPath("$.body[2].project").value("1"))
                .andExpect(jsonPath("$.body[2].user").value("admin"))
                .andExpect(jsonPath("$.body[2].role").value("ANNOTATOR"))
                .andExpect(jsonPath("$.body[3].project").value("1"))
                .andExpect(jsonPath("$.body[3].user").value("user"))
                .andExpect(jsonPath("$.body[3].role").value("CURATOR"))
                .andExpect(jsonPath("$.body[4].project").value("1"))
                .andExpect(jsonPath("$.body[4].user").value("user"))
                .andExpect(jsonPath("$.body[4].role").value("ANNOTATOR"));
    }

    @Test
    public void thatNonAdminCannotChangePermissions() throws Exception
    {
        userActor.grantProjectRole(1, "user", "MANAGER") //
                .andExpect(status().isForbidden());

        userActor.revokeProjectRole(1, "user", "MANAGER") //
                .andExpect(status().isForbidden());
    }

    @Test
    public void thatManagerCanChangePermissions() throws Exception
    {
        adminActor.grantProjectRole(1, "user", "MANAGER") //
                .andExpect(status().isOk()); //

        userActor.grantProjectRole(1, "user", "CURATOR") //
                .andExpect(status().isOk()); //

        adminActor.revokeProjectRole(1, "user", "MANAGER") //
                .andExpect(status().isOk()); //

        userActor.revokeProjectRole(1, "user", "CURATOR") //
                .andExpect(status().isForbidden());

    }

    @SpringBootConfiguration
    public static class TestContext
    {
        // All handled by auto-config
    }
}

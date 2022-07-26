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
                "repository.path=" + AeroRemoteApiController_Annotation_Test.TEST_OUTPUT_FOLDER })
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
public class AeroRemoteApiController_Annotation_Test
{
    static final String TEST_OUTPUT_FOLDER = "target/test-output/AeroRemoteApiController_Annotation_Test";

    private @Autowired WebApplicationContext context;
    private @Autowired UserDao userRepository;

    private MockAeroClient adminActor;

    @BeforeAll
    static void setupClass()
    {
        FileSystemUtils.deleteRecursively(new File(TEST_OUTPUT_FOLDER));
    }

    @BeforeEach
    void setup() throws Exception
    {
        adminActor = new MockAeroClient(context, "admin", "ADMIN");

        userRepository.create(new User("admin", ROLE_ADMIN));
        userRepository.create(new User("user", ROLE_USER));

        adminActor.createProject("project1").andExpect(status().isCreated())
                .andExpect(jsonPath("$.body.id").value("1"))
                .andExpect(jsonPath("$.body.name").value("project1"));

        adminActor.importTextDocument(1l, "test.txt", "This is a test.")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.body.id").value("1"));
    }

    @Test
    void testAnnotationCreate() throws Exception
    {
        adminActor.listAnnotations(1, 1) //
                .andExpect(status().isOk()) //
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages").isEmpty());

        adminActor.createAnnotations(1, 1, "admin", "This is a test.", "IN-PROGRESS") //
                .andExpect(status().isCreated()) //
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.body.user").value("admin"))
                .andExpect(jsonPath("$.body.state").value("IN-PROGRESS"))
                .andExpect(jsonPath("$.body.timestamp").doesNotExist());

        adminActor.listAnnotations(1, 1) //
                .andExpect(status().isOk()) //
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.body[0].user").value("admin"))
                .andExpect(jsonPath("$.body[0].state").value("IN-PROGRESS"))
                .andExpect(jsonPath("$.body[0].timestamp").doesNotExist());
    }

    @Test
    void testUpdatingTheAnnotationState() throws Exception
    {
        adminActor.createAnnotations(1, 1, "admin", "This is a test.") //
                .andExpect(status().isCreated()) //
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.body.user").value("admin"))
                .andExpect(jsonPath("$.body.state").value("NEW"))
                .andExpect(jsonPath("$.body.timestamp").doesNotExist());

        adminActor.updateAnnotationState(1, 1, "admin", "LOCKED") //
                .andExpect(status().isOk()) //
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.body.user").value("admin"))
                .andExpect(jsonPath("$.body.state").value("LOCKED"))
                .andExpect(jsonPath("$.body.timestamp").doesNotExist());
    }

    @SpringBootConfiguration
    static class TestContext
    {
        // All handled by auto-config
    }
}

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
public class AeroCurationControllerTest
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
    public void setup() throws Exception
    {
        adminActor = new MockAeroClient(context, "admin", "ADMIN", "REMOTE");

        userRepository.create(new User("admin", ROLE_ADMIN, ROLE_REMOTE));
        userRepository.create(new User("user", ROLE_USER, ROLE_REMOTE));

        adminActor.createProject("project1").andExpect(status().isCreated())
                .andExpect(jsonPath("$.body.id").value("1"))
                .andExpect(jsonPath("$.body.name").value("project1"));

        adminActor.importTextDocument(1l, "test.txt", "This is a test.")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.body.id").value("1"));
    }

    @Test
    public void testCurationCreateDelete() throws Exception
    {
        adminActor.listDocuments(1l) //
                .andExpect(status().isOk()) //
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.body[0].id").value("1"))
                .andExpect(jsonPath("$.body[0].name").value("test.txt"))
                .andExpect(jsonPath("$.body[0].state").value("NEW"));

        adminActor.importCurations(1, 1, "This is a test.", "CURATION-COMPLETE") //
                .andExpect(status().isCreated()) //
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.body.user").value("CURATION_USER"))
                .andExpect(jsonPath("$.body.state").value("COMPLETE"))
                .andExpect(jsonPath("$.body.timestamp").exists());

        adminActor.listDocuments(1l) //
                .andExpect(status().isOk()) //
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.body[0].id").value("1"))
                .andExpect(jsonPath("$.body[0].name").value("test.txt"))
                .andExpect(jsonPath("$.body[0].state").value("CURATION-COMPLETE"));

        adminActor.deleteCurations(1, 1) //
                .andExpect(status().isOk()) //
                .andExpect(content().contentType(APPLICATION_JSON_VALUE));

        // Nobody ever annotated this document - only a curation was imported and then deleted - so
        // it goes back to being untouched rather than claiming that annotation is under way. This
        // used to assert ANNOTATION-IN-PROGRESS, which was the hard-coded guess the delete endpoint
        // made before it started deriving the state from the annotation documents.
        adminActor.listDocuments(1l) //
                .andExpect(status().isOk()) //
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.body[0].id").value("1"))
                .andExpect(jsonPath("$.body[0].name").value("test.txt"))
                .andExpect(jsonPath("$.body[0].state").value("NEW"));
    }

    @Test
    public void testCurationDeleteFallsBackToStateDerivedFromAnnotations() throws Exception
    {
        adminActor.grantProjectRole(1l, "user", "ANNOTATOR").andExpect(status().isOk());

        // Every annotator finished the document, so with the curation gone it is
        // annotation-complete
        // again - not annotation-in-progress.
        adminActor.createAnnotations(1l, 1l, "user", "This is a test.", "COMPLETE")
                .andExpect(status().isCreated());
        adminActor.createAnnotations(1l, 1l, "admin", "This is a test.", "COMPLETE")
                .andExpect(status().isCreated());

        adminActor.importCurations(1, 1, "This is a test.", "CURATION-COMPLETE")
                .andExpect(status().isCreated());

        adminActor.deleteCurations(1, 1) //
                .andExpect(status().isOk()) //
                .andExpect(content().contentType(APPLICATION_JSON_VALUE));

        adminActor.listDocuments(1l) //
                .andExpect(status().isOk()) //
                .andExpect(jsonPath("$.body[0].state").value("ANNOTATION-COMPLETE"));
    }

    @Test
    public void testCurationDeleteFallsBackToNewWithoutAnnotations() throws Exception
    {
        adminActor.grantProjectRole(1l, "user", "ANNOTATOR").andExpect(status().isOk());

        adminActor.importCurations(1, 1, "This is a test.", "CURATION-COMPLETE")
                .andExpect(status().isCreated());

        adminActor.deleteCurations(1, 1) //
                .andExpect(status().isOk()) //
                .andExpect(content().contentType(APPLICATION_JSON_VALUE));

        adminActor.listDocuments(1l) //
                .andExpect(status().isOk()) //
                .andExpect(jsonPath("$.body[0].state").value("NEW"));
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

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
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi;

import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_ADMIN;
import static de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.AeroRemoteApiController.API_BASE;
import static java.util.Arrays.asList;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.context.WebApplicationContext;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.DocumentImportExportServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.DocumentServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.annotationservice.config.AnnotationSchemaServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.OpenCasStorageSessionForRequestFilter;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.config.CasStorageServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.docimexport.config.DocumentImportExportServiceProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.docimexport.config.DocumentImportExportServicePropertiesImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.export.ProjectExportServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportService;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LoggingFilter;
import de.tudarmstadt.ukp.clarin.webanno.text.TextFormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.config.RemoteApiAutoConfiguration;

@EnableAutoConfiguration(exclude = LiquibaseAutoConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.MOCK, //
        properties = { //
                "spring.main.banner-mode=off", //
                "remote-api.enabled=true", //
                "repository.path=" + AeroRemoteApiControllerTest.TEST_OUTPUT_FOLDER })
@EnableWebSecurity
@Import({ //
        ProjectServiceAutoConfiguration.class, //
        CasStorageServiceAutoConfiguration.class, //
        RepositoryAutoConfiguration.class, //
        AnnotationSchemaServiceAutoConfiguration.class, //
        SecurityAutoConfiguration.class, //
        RemoteApiAutoConfiguration.class })
@EntityScan({ //
        "de.tudarmstadt.ukp.clarin.webanno.model", //
        "de.tudarmstadt.ukp.clarin.webanno.security.model" })
@TestMethodOrder(MethodOrderer.MethodName.class)
public class AeroRemoteApiControllerTest
{
    static final String TEST_OUTPUT_FOLDER = "target/test-output/AeroRemoteApiControllerTest";

    private @Autowired WebApplicationContext context;
    private @Autowired UserDao userRepository;
    private @Autowired RepositoryProperties repositoryProperties;

    private MockMvc mvc;

    // If this is not static, for some reason the value is re-set to false before a
    // test method is invoked. However, the DB is not reset - and it should not be.
    // So we need to make this static to ensure that we really only create the user
    // in the DB and clean the test repository once!
    private static boolean initialized = false;

    @BeforeAll
    public static void setupClass()
    {
        FileSystemUtils.deleteRecursively(new File(TEST_OUTPUT_FOLDER));
    }

    @BeforeEach
    public void setup()
    {
        // @formatter:off
        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .alwaysDo(print())
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .addFilters(new LoggingFilter(repositoryProperties.getPath().toString()))
                .addFilters(new OpenCasStorageSessionForRequestFilter())
                .build();
        // @formatter:on

        if (!initialized) {
            userRepository.create(new User("admin", ROLE_ADMIN));
            initialized = true;
        }
    }

    @Test
    public void t001_testProjectCreate() throws Exception
    {
        // @formatter:off
        mvc.perform(get(API_BASE + "/projects")
                .with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.messages").isEmpty());
        
        mvc.perform(post(API_BASE + "/projects")
                .with(csrf().asHeader())
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .param("name", "project1"))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.body.id").value("1"))
            .andExpect(jsonPath("$.body.name").value("project1"));
        
        mvc.perform(get(API_BASE + "/projects")
                .with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.body[0].id").value("1"))
            .andExpect(jsonPath("$.body[0].name").value("project1"));
        // @formatter:on
    }

    @Test
    public void t002_testDocumentCreate() throws Exception
    {
        // @formatter:off
        mvc.perform(get(API_BASE + "/projects/1/documents")
                .with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.messages").isEmpty());
        
        mvc.perform(multipart(API_BASE + "/projects/1/documents")
                .file("content", "This is a test.".getBytes("UTF-8"))
                .with(csrf().asHeader())
                .with(user("admin").roles("ADMIN"))
                .param("name", "test.txt")
                .param("format", "text"))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.body.id").value("1"))
            .andExpect(jsonPath("$.body.name").value("test.txt"));
     
        mvc.perform(get(API_BASE + "/projects/1/documents")
                .with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.body[0].id").value("1"))
            .andExpect(jsonPath("$.body[0].name").value("test.txt"))
            .andExpect(jsonPath("$.body[0].state").value("NEW"));
        // @formatter:on
    }

    @Test
    public void t003_testAnnotationCreate() throws Exception
    {
        // @formatter:off
        mvc.perform(get(API_BASE + "/projects/1/documents/1/annotations")
                .with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.messages").isEmpty());
        
        mvc.perform(multipart(API_BASE + "/projects/1/documents/1/annotations/admin")
                .file("content", "This is a test.".getBytes("UTF-8"))
                .with(csrf().asHeader())
                .with(user("admin").roles("ADMIN"))
                .param("name", "test.txt")
                .param("format", "text")
                .param("state", "IN-PROGRESS"))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.body.user").value("admin"))
            .andExpect(jsonPath("$.body.state").value("IN-PROGRESS"))
            .andExpect(jsonPath("$.body.timestamp").doesNotExist());
     
        mvc.perform(get(API_BASE + "/projects/1/documents/1/annotations")
                .with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.body[0].user").value("admin"))
            .andExpect(jsonPath("$.body[0].state").value("IN-PROGRESS"))
            .andExpect(jsonPath("$.body[0].timestamp").doesNotExist());
        // @formatter:on
    }

    @Test
    public void t004_testCurationCreate() throws Exception
    {
        // @formatter:off
        mvc.perform(get(API_BASE + "/projects/1/documents")
                .with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.body[0].id").value("1"))
            .andExpect(jsonPath("$.body[0].name").value("test.txt"))
            .andExpect(jsonPath("$.body[0].state").value("NEW"));
        
        mvc.perform(multipart(API_BASE + "/projects/1/documents/1/curation")
                .file("content", "This is a test.".getBytes("UTF-8"))
                .with(csrf().asHeader())
                .with(user("admin").roles("ADMIN"))
                .param("name", "test.txt")
                .param("format", "text")
                .param("state", "CURATION-COMPLETE"))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.body.user").value("CURATION_USER"))
            .andExpect(jsonPath("$.body.state").value("COMPLETE"))
            .andExpect(jsonPath("$.body.timestamp").exists());
     
        mvc.perform(get(API_BASE + "/projects/1/documents")
                .with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.body[0].id").value("1"))
            .andExpect(jsonPath("$.body[0].name").value("test.txt"))
            .andExpect(jsonPath("$.body[0].state").value("CURATION-COMPLETE"));
        // @formatter:on
    }

    @Test
    public void t005_testCurationDelete() throws Exception
    {
        // @formatter:off
        mvc.perform(delete(API_BASE + "/projects/1/documents/1/curation")
                .with(csrf().asHeader())
                .with(user("admin").roles("ADMIN"))
                .param("projectId", "1")
                .param("documentId", "1"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE));
     
        mvc.perform(get(API_BASE + "/projects/1/documents")
                .with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.body[0].id").value("1"))
            .andExpect(jsonPath("$.body[0].name").value("test.txt"))
            .andExpect(jsonPath("$.body[0].state").value("ANNOTATION-IN-PROGRESS"));
        // @formatter:on
    }

    @Configuration
    public static class TestContext
    {
        private @Autowired ApplicationEventPublisher applicationEventPublisher;
        private @Autowired EntityManager entityManager;

        @Bean
        public DocumentService documentService(CasStorageService aCasStorageService,
                DocumentImportExportService aDocumentImportExportService,
                RepositoryProperties aProperties, ProjectService aProjectService)
        {
            return new DocumentServiceImpl(aProperties, aCasStorageService,
                    aDocumentImportExportService, aProjectService, applicationEventPublisher,
                    entityManager);
        }

        @Bean
        public DocumentImportExportService importExportService(CasStorageService aCasStorageService,
                RepositoryProperties aProperties, AnnotationSchemaService aAnnotationSchemaService)
        {
            DocumentImportExportServiceProperties properties = new DocumentImportExportServicePropertiesImpl();

            return new DocumentImportExportServiceImpl(aProperties, asList(new TextFormatSupport()),
                    aCasStorageService, aAnnotationSchemaService, properties);
        }

        @Bean
        public CurationDocumentService curationDocumentService(CasStorageService aCasStorageService,
                AnnotationSchemaService aAnnotationService)
        {
            return new CurationDocumentServiceImpl(aCasStorageService, aAnnotationService);
        }

        @Bean
        public ProjectExportService exportService(ProjectService aProjectService)
        {
            return new ProjectExportServiceImpl(null, null, aProjectService);
        }
    }
}

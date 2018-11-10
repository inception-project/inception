/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi;

import static java.util.Arrays.asList;
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
import java.util.Collections;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.context.WebApplicationContext;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.ChainLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.RelationLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.SpanLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.AnnotationSchemaServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.BackupProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.CasStorageServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.DocumentServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.ImportExportServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.export.ProjectExportServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportService;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.project.ProjectServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDaoImpl;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.ApplicationContextProvider;
import de.tudarmstadt.ukp.clarin.webanno.text.TextFormatSupport;

@RunWith(SpringRunner.class) 
@EnableAutoConfiguration
@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@EnableWebSecurity
@EntityScan({
    "de.tudarmstadt.ukp.clarin.webanno.model",
    "de.tudarmstadt.ukp.clarin.webanno.security.model" })
@TestPropertySource(locations = "classpath:RemoteApiController2Test.properties")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RemoteApiController2Test
{
    private @Autowired WebApplicationContext context;
    private @Autowired UserDao userRepository;
    
    private MockMvc mvc;

    // If this is not static, for some reason the value is re-set to false before a
    // test method is invoked. However, the DB is not reset - and it should not be.
    // So we need to make this static to ensure that we really only create the user
    // in the DB and clean the test repository once!
    private static boolean initialized = false;

    @Before
    public void setup() {
        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .alwaysDo(print())
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        
        if (!initialized) {
            userRepository.create(new User("admin", Role.ROLE_ADMIN));
            initialized = true;
            
            FileSystemUtils.deleteRecursively(new File("target/RemoteApiController2Test"));
        }
    }

    @Test
    public void t001_testProjectCreate() throws Exception
    {
        mvc.perform(get("/api/v2/projects")
                .with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(jsonPath("$.messages").isEmpty());
        
        mvc.perform(post("/api/v2/projects")
                .with(csrf().asHeader())
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .param("name", "project1"))
            .andExpect(status().isCreated())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(jsonPath("$.body.id").value("1"))
            .andExpect(jsonPath("$.body.name").value("project1"));
        
        mvc.perform(get("/api/v2/projects")
                .with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(jsonPath("$.body[0].id").value("1"))
            .andExpect(jsonPath("$.body[0].name").value("project1"));
    }
    
    @Test
    public void t002_testDocumentCreate() throws Exception
    {
        mvc.perform(get("/api/v2/projects/1/documents")
                .with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(jsonPath("$.messages").isEmpty());
        
        mvc.perform(multipart("/api/v2/projects/1/documents")
                .file("content", "This is a test.".getBytes("UTF-8"))
                .with(csrf().asHeader())
                .with(user("admin").roles("ADMIN"))
                .param("name", "test.txt")
                .param("format", "text"))
            .andExpect(status().isCreated())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(jsonPath("$.body.id").value("1"))
            .andExpect(jsonPath("$.body.name").value("test.txt"));
     
        mvc.perform(get("/api/v2/projects/1/documents")
                .with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(jsonPath("$.body[0].id").value("1"))
            .andExpect(jsonPath("$.body[0].name").value("test.txt"))
            .andExpect(jsonPath("$.body[0].state").value("NEW"));
    }

    @Test
    public void t003_testAnnotationCreate() throws Exception
    {
        mvc.perform(get("/api/v2/projects/1/documents/1/annotations")
                .with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(jsonPath("$.messages").isEmpty());
        
        mvc.perform(multipart("/api/v2/projects/1/documents/1/annotations/admin")
                .file("content", "This is a test.".getBytes("UTF-8"))
                .with(csrf().asHeader())
                .with(user("admin").roles("ADMIN"))
                .param("name", "test.txt")
                .param("format", "text"))
            .andExpect(status().isCreated())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(jsonPath("$.body.user").value("admin"))
            .andExpect(jsonPath("$.body.state").value("IN-PROGRESS"))
            .andExpect(jsonPath("$.body.timestamp").doesNotExist());
     
        mvc.perform(get("/api/v2/projects/1/documents/1/annotations")
                .with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(jsonPath("$.body[0].user").value("admin"))
            .andExpect(jsonPath("$.body[0].state").value("IN-PROGRESS"))
            .andExpect(jsonPath("$.body[0].timestamp").doesNotExist());
    }

    @Test
    public void t004_testCurationCreate() throws Exception
    {
        mvc.perform(get("/api/v2/projects/1/documents")
                .with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(jsonPath("$.body[0].id").value("1"))
            .andExpect(jsonPath("$.body[0].name").value("test.txt"))
            .andExpect(jsonPath("$.body[0].state").value("NEW"));
        
        mvc.perform(multipart("/api/v2/projects/1/documents/1/curation")
                .file("content", "This is a test.".getBytes("UTF-8"))
                .with(csrf().asHeader())
                .with(user("admin").roles("ADMIN"))
                .param("name", "test.txt")
                .param("format", "text")
                .param("state", "CURATION-COMPLETE"))
            .andExpect(status().isCreated())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(jsonPath("$.body.user").value("CURATION_USER"))
            .andExpect(jsonPath("$.body.state").value("COMPLETE"))
            .andExpect(jsonPath("$.body.timestamp").exists());
     
        mvc.perform(get("/api/v2/projects/1/documents")
                .with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(jsonPath("$.body[0].id").value("1"))
            .andExpect(jsonPath("$.body[0].name").value("test.txt"))
            .andExpect(jsonPath("$.body[0].state").value("CURATION-COMPLETE"));
    }

    @Test
    public void t005_testCurationDelete() throws Exception
    {
        mvc.perform(delete("/api/v2/projects/1/documents/1/curation")
                .with(csrf().asHeader())
                .with(user("admin").roles("ADMIN"))
                .param("projectId", "1")
                .param("documentId", "1"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"));
     
        mvc.perform(get("/api/v2/projects/1/documents")
                .with(csrf().asHeader())
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(jsonPath("$.body[0].id").value("1"))
            .andExpect(jsonPath("$.body[0].name").value("test.txt"))
            .andExpect(jsonPath("$.body[0].state").value("ANNOTATION-IN-PROGRESS"));
    }

    @Configuration
    public static class TestContext {
        @Autowired ApplicationEventPublisher applicationEventPublisher;
        
        @Bean
        public RemoteApiController2 remoteApiV2()
        {
            return new RemoteApiController2();
        }
        
        @Bean
        public ProjectService projectService()
        {
            return new ProjectServiceImpl();
        }
        
        @Bean
        public UserDao userRepository()
        {
            return new UserDaoImpl();
        }
        
        @Bean
        public DocumentService documentService()
        {
            return new DocumentServiceImpl(repositoryProperties(), userRepository(),
                    casStorageService(), importExportService(), projectService(),
                    applicationEventPublisher);
        }
        
        @Bean
        public AnnotationSchemaService annotationService()
        {
            return new AnnotationSchemaServiceImpl();
        }
        
        @Bean
        public FeatureSupportRegistry featureSupportRegistry()
        {
            return new FeatureSupportRegistryImpl(Collections.emptyList());
        }
        
        @Bean
        public CasStorageService casStorageService()
        {
            return new CasStorageServiceImpl(null, repositoryProperties(), backupProperties());
        }
        
        @Bean
        public ImportExportService importExportService()
        {
            return new ImportExportServiceImpl(repositoryProperties(),
                    asList(new TextFormatSupport()), casStorageService(), annotationService());
        }
        
        @Bean
        public CurationDocumentService curationDocumentService()
        {
            return new CurationDocumentServiceImpl();
        }

        @Bean
        public ProjectExportService exportService()
        {
            return new ProjectExportServiceImpl(null, projectService());
        }
        
        @Bean
        public RepositoryProperties repositoryProperties()
        {
            return new RepositoryProperties();
        }

        @Bean 
        public BackupProperties backupProperties()
        {
            return new BackupProperties();
        }

        @Bean
        public ApplicationContextProvider contextProvider()
        {
            return new ApplicationContextProvider();
        }
        
        @Bean
        public LayerSupportRegistry layerSupportRegistry()
        {
            return new LayerSupportRegistryImpl(asList(
                    new SpanLayerSupport(featureSupportRegistry(), null, annotationService()),
                    new RelationLayerSupport(featureSupportRegistry(), null, annotationService()),
                    new ChainLayerSupport(featureSupportRegistry(), null, annotationService())));
        }
    }
}

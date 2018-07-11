/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.search.index.mtas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.AnnotationSchemaServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.BackupProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.CasStorageServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.DocumentServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.ImportExportServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.initializers.NamedEntityLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.initializers.PartOfSpeechLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.initializers.TokenLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.project.ProjectServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDaoImpl;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.ApplicationContextProvider;
import de.tudarmstadt.ukp.dkpro.core.io.text.TextReader;
import de.tudarmstadt.ukp.dkpro.core.io.text.TextWriter;
import de.tudarmstadt.ukp.inception.search.ExecutionException;
import de.tudarmstadt.ukp.inception.search.SearchResult;
import de.tudarmstadt.ukp.inception.search.SearchService;
import de.tudarmstadt.ukp.inception.search.SearchServiceImpl;
import de.tudarmstadt.ukp.inception.search.index.PhysicalIndexFactory;
import de.tudarmstadt.ukp.inception.search.index.PhysicalIndexRegistry;
import de.tudarmstadt.ukp.inception.search.index.PhysicalIndexRegistryImpl;
import de.tudarmstadt.ukp.inception.search.scheduling.IndexScheduler;

/**
 * The Class MtasDocumentIndexTest.
 */

@RunWith(SpringRunner.class)
@EnableAutoConfiguration
@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
//@EnableWebSecurity
@EntityScan({ "de.tudarmstadt.ukp.clarin.webanno.model",
        "de.tudarmstadt.ukp.inception.search.model",
        "de.tudarmstadt.ukp.clarin.webanno.security.model" })
@TestPropertySource(locations = "classpath:MtasDocumentIndexTest.properties")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@DataJpaTest
@Transactional
public class MtasDocumentIndexTest
{
//    @Rule
//    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private @Autowired UserDao userRepository;

    private @Autowired ProjectService projectService;
    private @Autowired DocumentService documentService;
    private @Autowired SearchService searchService;
    private @Autowired AnnotationSchemaService annotationSchemaService;

//    @Autowired
//    private TestEntityManager testEntityManager;

    // If this is not static, for some reason the value is re-set to false before a
    // test method is invoked. However, the DB is not reset - and it should not be.
    // So we need to make this static to ensure that we really only create the user
    // in the DB and clean the test repository once!
    private static boolean initialized = false;

//    private SearchServiceImpl ssi;
    
    @Before
    public void setUp()
    {
        if (!initialized) {
            userRepository.create(new User("admin", Role.ROLE_ADMIN));
            initialized = true;

//            EntityManager entityManager = testEntityManager.getEntityManager();

//            ssi = new SearchServiceImpl(temporaryFolder.getRoot(), entityManager);
            
            FileSystemUtils.deleteRecursively(new File("target/MtasDocumentIndexTest"));
        }
    }

    @Test
    public void testSimpleQuery() throws Exception
    {
        Project project = new Project();

        project.setName("TestSimpleQuery");

        projectService.createProject(project);

        annotationSchemaService.initializeProject(project);

        SourceDocument sourceDocument = new SourceDocument();

        sourceDocument.setName("test");
        sourceDocument.setProject(project);
        sourceDocument.setFormat("text");

        String fileContent = "The capital of Galicia is Santiago de Compostela.";

        InputStream fileStream = new ByteArrayInputStream(
                fileContent.getBytes(StandardCharsets.UTF_8));

        User user = userRepository.get("admin");

        documentService.uploadSourceDocument(fileStream, sourceDocument);

        String query = "Galicia";

        List<SearchResult> results = null;
        
        int i = 0;
        Thread.sleep(2000);
        while (i < 1) {
            try {
                results = (ArrayList<SearchResult>) searchService.query(user,
                        project, query);
            }
            catch (ExecutionException e) {
                i++;
//                Thread.sleep(5000);
            }
        }

        SearchResult expectedResult = new SearchResult();
        expectedResult.setDocumentId(sourceDocument.getId());
        expectedResult.setDocumentTitle("test");
        expectedResult.setText("Galicia ");
        expectedResult.setLeftContext("capital of ");
        expectedResult.setRightContext("is ");
        expectedResult.setOffsetStart(15);
        expectedResult.setOffsetEnd(22);
        expectedResult.setTokenStart(3);
        expectedResult.setTokenLength(1);

        ArrayList<SearchResult> expectedResults = new ArrayList<SearchResult>();
        expectedResults.add(expectedResult);

        if (results != null) {
            assertEquals(results.get(0), expectedResult);
        }
        else {
            assertNotNull(results);
        }
    }

//    @Test
    public void testAnnotationQuery() throws Exception
    {
        Project project = new Project();

        project.setName("TestAnnotationQuery");

        projectService.createProject(project);

        annotationSchemaService.initializeProject(project);

        SourceDocument sourceDocument = new SourceDocument();

        sourceDocument.setName("test");
        sourceDocument.setProject(project);
        sourceDocument.setFormat("text");

        String fileContent = "The capital of Galicia is Santiago de Compostela.";

        InputStream fileStream = new ByteArrayInputStream(
                fileContent.getBytes(StandardCharsets.UTF_8));

        User user = userRepository.get("admin");
        String query = "Galicia";

        documentService.uploadSourceDocument(fileStream, sourceDocument);

        ArrayList<SearchResult> results = (ArrayList<SearchResult>) searchService.query(user,
                project, query);

        SearchResult expectedResult = new SearchResult();
        expectedResult.setDocumentId(sourceDocument.getId());
        expectedResult.setDocumentTitle("test");
        expectedResult.setText("Galicia ");
        expectedResult.setLeftContext("capital of ");
        expectedResult.setRightContext("is ");
        expectedResult.setOffsetStart(15);
        expectedResult.setOffsetEnd(22);
        expectedResult.setTokenStart(3);
        expectedResult.setTokenLength(1);

        ArrayList<SearchResult> expectedResults = new ArrayList<SearchResult>();
        expectedResults.add(expectedResult);

        assertEquals(results.get(0), expectedResult);
    }

    @Configuration
    public static class TestContext
    {
        @Autowired ApplicationEventPublisher applicationEventPublisher;

        @Bean
        public ProjectService projectService()
        {
            return new ProjectServiceImpl();
        }

        @Bean
        public PhysicalIndexFactory mtasDocumentIndexFactory()
        {
            return new MtasDocumentIndexFactory();
        }

        @Lazy
        @Bean
        public NamedEntityLayerInitializer NamedEntityLayerInitializer(
                @Lazy @Autowired AnnotationSchemaService aAnnotationService)
        {
            return new NamedEntityLayerInitializer(aAnnotationService);
        }

        @Lazy
        @Bean
        public PartOfSpeechLayerInitializer PartOfSpeechLayerInitializer(
                @Lazy @Autowired AnnotationSchemaService aAnnotationSchemaService)
        {
            return new PartOfSpeechLayerInitializer(aAnnotationSchemaService);
        }

        @Lazy
        @Bean
        public TokenLayerInitializer TokenLayerInitializer(
                @Lazy @Autowired AnnotationSchemaService aAnnotationSchemaService)
        {
            return new TokenLayerInitializer(aAnnotationSchemaService);
        }

        @Lazy
        @Bean
        public PhysicalIndexRegistry indexRegistry(
                @Lazy @Autowired(required = false) List<PhysicalIndexFactory> aExtensions)
        {
            return new PhysicalIndexRegistryImpl(aExtensions);
        }

        @Bean
        public SearchService searchService()
        {
            return new SearchServiceImpl();
//            return new SearchServiceImpl();
        }

        @Bean
        public UserDao userRepository()
        {
            return new UserDaoImpl();
        }

        @Bean
        public IndexScheduler indexScheduler()
        {
            return new IndexScheduler();
        }

        @Bean
        public DocumentService documentService()
        {
            return new DocumentServiceImpl(repositoryProperties(), userRepository(),
                    casStorageService(), importExportService(), projectService(),
                    applicationEventPublisher);
        }

        @Bean
        public AnnotationSchemaService annotationSchemaService()
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
            return new ImportExportServiceImpl();
        }

        @Bean
        public CurationDocumentService curationDocumentService()
        {
            return new CurationDocumentServiceImpl();
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
        public Properties formats()
        {
            Properties props = new Properties();
            props.put("text.label", "Plain text");
            props.put("text.reader", TextReader.class.getName());
            props.put("text.writer", TextWriter.class.getName());
            return props;
        }

        @Bean
        public ApplicationContextProvider contextProvider()
        {
            return new ApplicationContextProvider();
        }
    }
}

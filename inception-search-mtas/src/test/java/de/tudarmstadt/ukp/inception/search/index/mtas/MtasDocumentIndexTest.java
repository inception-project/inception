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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.fit.factory.JCasBuilder;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestWatcher;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.PrimitiveUimaFeatureSupport;
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
import de.tudarmstadt.ukp.clarin.webanno.api.dao.initializers.NamedEntityLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.initializers.PartOfSpeechLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.initializers.TokenLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.conll.Conll2002FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.project.ProjectServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDaoImpl;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.ApplicationContextProvider;
import de.tudarmstadt.ukp.clarin.webanno.text.TextFormatSupport;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseServiceImpl;
import de.tudarmstadt.ukp.inception.search.FeatureIndexingSupport;
import de.tudarmstadt.ukp.inception.search.FeatureIndexingSupportRegistry;
import de.tudarmstadt.ukp.inception.search.FeatureIndexingSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.search.PrimitiveUimaIndexingSupport;
import de.tudarmstadt.ukp.inception.search.SearchResult;
import de.tudarmstadt.ukp.inception.search.SearchService;
import de.tudarmstadt.ukp.inception.search.SearchServiceImpl;
import de.tudarmstadt.ukp.inception.search.index.PhysicalIndexFactory;
import de.tudarmstadt.ukp.inception.search.index.PhysicalIndexRegistry;
import de.tudarmstadt.ukp.inception.search.index.PhysicalIndexRegistryImpl;
import de.tudarmstadt.ukp.inception.search.scheduling.IndexScheduler;

@RunWith(SpringRunner.class)
@EnableAutoConfiguration
@SpringBootTest
@EntityScan({ 
        "de.tudarmstadt.ukp.clarin.webanno.model",
        "de.tudarmstadt.ukp.inception.search.model",
        "de.tudarmstadt.ukp.inception.kb.model",
        "de.tudarmstadt.ukp.clarin.webanno.security.model" })
@TestPropertySource(locations = "classpath:MtasDocumentIndexTest.properties")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@DataJpaTest
@Transactional(propagation = Propagation.NEVER)
public class MtasDocumentIndexTest
{
    private @Autowired UserDao userRepository;
    private @Autowired ProjectService projectService;
    private @Autowired DocumentService documentService;
    private @Autowired SearchService searchService;
    private @Autowired AnnotationSchemaService annotationSchemaService;

    @Rule
    public TestWatcher watcher = new TestWatcher()
    {
        @Override
        protected void starting(org.junit.runner.Description aDescription)
        {
            String methodName = aDescription.getMethodName();
            System.out.printf("\n=== " + methodName + " =====================\n");
        };
    };
    
    @Before
    public void setUp()
    {
        if (!userRepository.exists("admin")) {
            userRepository.create(new User("admin", Role.ROLE_ADMIN));
        }
    }

    private void createProject(Project aProject) throws Exception
    {
        projectService.createProject(aProject);
        annotationSchemaService.initializeProject(aProject);
    }

    @SafeVarargs
    private void uploadDocument(Pair<SourceDocument, String>... aDocuments)
        throws Exception
    {
        Project project = null;
        for (Pair<SourceDocument, String> doc : aDocuments) {
            project = doc.getLeft().getProject();
            
            try (InputStream fileStream = new ByteArrayInputStream(
                    doc.getRight().getBytes(UTF_8))) {
                documentService.uploadSourceDocument(fileStream, doc.getLeft());
            }
        }
        
        // Avoid the compiler complaining about project not being an effectively final variable
        Project p = project;
        await("Waiting for indexing process to complete")
                .atMost(60, SECONDS)
                .pollInterval(5, SECONDS)
                .until(() -> searchService.isIndexValid(p) && !searchService.isIndexInProgress(p));
    }

    private void annotateDocument(Project aProject, User aUser, SourceDocument aSourceDocument)
        throws Exception
    {
        // Manually build annotated CAS
        JCas jCas = JCasFactory.createJCas();
        
        JCasBuilder builder = new JCasBuilder(jCas);

        builder.add("The", Token.class);
        builder.add(" ");
        builder.add("capital", Token.class);
        builder.add(" ");
        builder.add("of", Token.class);
        builder.add(" ");
        
        int begin = builder.getPosition();
        builder.add("Galicia", Token.class);
        
        NamedEntity ne = new NamedEntity(jCas, begin, builder.getPosition());
        ne.setValue("LOC");
        ne.addToIndexes();
        
        builder.add(" ");
        builder.add("is", Token.class);
        builder.add(" ");
        builder.add("Santiago", Token.class);
        builder.add(" ");
        builder.add("de", Token.class);
        builder.add(" ");
        builder.add("Compostela", Token.class);
        builder.add(" ");
        builder.add(".", Token.class);
        
        // Create annotation document
        AnnotationDocument annotationDocument = documentService
                .createOrGetAnnotationDocument(aSourceDocument, aUser);

        // Write annotated CAS to annotated document
        documentService.writeAnnotationCas(jCas, annotationDocument, false);

        await("Waiting for indexing process to complete")
                .atMost(60, SECONDS)
                .pollInterval(5, SECONDS)
                .until(() -> searchService.isIndexValid(aProject)
                        && !searchService.isIndexInProgress(aProject));
    }

    @Test
    public void testRawTextQuery() throws Exception
    {
        Project project = new Project();
        project.setName("TestRawTextQuery");
        project.setMode(WebAnnoConst.PROJECT_TYPE_ANNOTATION);

        createProject(project);

        SourceDocument sourceDocument = new SourceDocument();

        sourceDocument.setName("Raw text document");
        sourceDocument.setProject(project);
        sourceDocument.setFormat("text");

        String fileContent = "The capital of Galicia is Santiago de Compostela.";

        uploadDocument(Pair.of(sourceDocument, fileContent));

        User user = userRepository.get("admin");

        String query = "Galicia";

        // Execute query
        List<SearchResult> results = searchService.query(user, project, query);

        // Test results
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

        assertNotNull(results);
        if (results != null) {
            assertEquals(1, results.size());
            assertEquals(expectedResult, results.get(0));
        }
    }

    @Test
    public void testLimitQueryToDocument() throws Exception
    {
        Project project = new Project();
        project.setName("TestLimitQueryToDocument");
        project.setMode(WebAnnoConst.PROJECT_TYPE_ANNOTATION);

        createProject(project);

        SourceDocument sourceDocument1 = new SourceDocument();
        sourceDocument1.setName("Raw text document 1");
        sourceDocument1.setProject(project);
        sourceDocument1.setFormat("text");
        String fileContent1 = "The capital of Galicia is Santiago de Compostela.";

        SourceDocument sourceDocument2 = new SourceDocument();
        sourceDocument2.setName("Raw text document 2");
        sourceDocument2.setProject(project);
        sourceDocument2.setFormat("text");
        String fileContent2 = "The capital of Portugal is Lissabon.";
        
        uploadDocument(
                Pair.of(sourceDocument1, fileContent1),
                Pair.of(sourceDocument2, fileContent2));

        User user = userRepository.get("admin");

        String query = "capital";

        // Execute query
        SourceDocument sourceDocument = documentService.getSourceDocument(project,
                "Raw text document 1");
        List<SearchResult> resultsNotLimited = searchService.query(user, project, query);
        List<SearchResult> resultsLimited = searchService.query(user, project, query,
                sourceDocument);

        // Test results
        SearchResult expectedResult1 = new SearchResult();
        expectedResult1.setDocumentId(sourceDocument1.getId());
        expectedResult1.setDocumentTitle("Raw text document 1");
        expectedResult1.setText("capital ");
        expectedResult1.setLeftContext("The ");
        expectedResult1.setRightContext("of ");
        expectedResult1.setOffsetStart(4);
        expectedResult1.setOffsetEnd(11);

        SearchResult expectedResult2 = new SearchResult();
        expectedResult2.setDocumentId(sourceDocument2.getId());
        expectedResult2.setDocumentTitle("Raw text document 2");
        expectedResult2.setText("capital ");
        expectedResult2.setLeftContext("The ");
        expectedResult2.setRightContext("of ");
        expectedResult2.setOffsetStart(4);
        expectedResult2.setOffsetEnd(11);

        assertThat(resultsLimited)
                .containsExactly(expectedResult1)
                .usingElementComparatorIgnoringFields("tokenStart", "tokenEnd");
        
        assertThat(resultsNotLimited)
                .containsExactlyInAnyOrder(expectedResult1, expectedResult2)
                .usingElementComparatorIgnoringFields("tokenStart", "tokenEnd");
    }

    @Test
    public void testSimplifiedTokenTextQuery() throws Exception
    {
        Project project = new Project();
        project.setName("SimplifiedTokenTextQuery");
        project.setMode(WebAnnoConst.PROJECT_TYPE_ANNOTATION);

        createProject(project);

        SourceDocument sourceDocument = new SourceDocument();

        sourceDocument.setName("Raw text document");
        sourceDocument.setProject(project);
        sourceDocument.setFormat("text");

        String fileContent = "The capital of Galicia is Santiago de Compostela.";

        uploadDocument(Pair.of(sourceDocument, fileContent));

        User user = userRepository.get("admin");

        String query = "\"Galicia\"";

        // Execute query
        List<SearchResult> results = searchService.query(user, project, query);

        // Test results
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

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(expectedResult, results.get(0));
    }
    
    @Test
    public void testAnnotationQuery() throws Exception
    {
        Project project = new Project();
        project.setName("TestAnnotationQuery");
        project.setMode(WebAnnoConst.PROJECT_TYPE_ANNOTATION);

        createProject(project);

        User user = userRepository.get("admin");

        SourceDocument sourceDocument = new SourceDocument();

        sourceDocument.setName("Annotation document");
        sourceDocument.setProject(project);
        sourceDocument.setFormat("text");

        String fileContent = "The capital of Galicia is Santiago de Compostela.";

        uploadDocument(Pair.of(sourceDocument, fileContent));
        annotateDocument(project, user, sourceDocument);

        String query = "<Named_entity.value=\"LOC\"/>";

        List<SearchResult> results = searchService.query(user, project, query);

        // Test results
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

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(expectedResult, results.get(0));
    }

    @Configuration
    public static class TestContext
    {
        @Autowired
        ApplicationEventPublisher applicationEventPublisher;

        private final String temporaryFolderPath = "target/MtasDocumentIndexTest";
        private final File temporaryFolder;

        @Rule TemporaryFolder folder;
        
        public TestContext()
        {
            try {
                FileUtils.deleteDirectory(new File(temporaryFolderPath));
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            temporaryFolder = new File(temporaryFolderPath);
        }
        
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

        @Bean
        public FeatureSupport featureSupport()
        {
            return new PrimitiveUimaFeatureSupport();
        }

        @Bean
        public FeatureSupportRegistry featureSupportRegistry(
                @Lazy @Autowired List<FeatureSupport> aFeatureSupports)
        {
            return new FeatureSupportRegistryImpl(aFeatureSupports);
        }

        @Bean
        public FeatureIndexingSupport primitiveUimaIndexingSupport(
                @Autowired FeatureSupportRegistry aFeatureSupportRegistry)
        {
            return new PrimitiveUimaIndexingSupport(aFeatureSupportRegistry);
        }

        @Bean
        public FeatureIndexingSupportRegistry featureIndexingSupportRegistry(
                @Lazy @Autowired(required = false) List<FeatureIndexingSupport> aIndexingSupports)
        {
            return new FeatureIndexingSupportRegistryImpl(aIndexingSupports);
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
        }

        @Bean
        public UserDao userRepository()
        {
            return new UserDaoImpl();
        }

        @Bean
        public KnowledgeBaseService knowledgeBaseService()
        {
            return new KnowledgeBaseServiceImpl(repositoryProperties());
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
        public CasStorageService casStorageService()
        {
            return new CasStorageServiceImpl(null, repositoryProperties(), backupProperties());
        }

        @Bean
        public ImportExportService importExportService()
        {
            return new ImportExportServiceImpl(repositoryProperties(),
                    asList(new TextFormatSupport(), new Conll2002FormatSupport()),
                    casStorageService(), annotationSchemaService());
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
        public ApplicationContextProvider contextProvider()
        {
            return new ApplicationContextProvider();
        }
        
        @Bean
        public LayerSupportRegistry layerSupportRegistry(
                @Autowired FeatureSupportRegistry aFeatureSupportRegistry)
        {
            return new LayerSupportRegistryImpl(asList(
                    new SpanLayerSupport(aFeatureSupportRegistry, null, annotationSchemaService(),
                            null),
                    new RelationLayerSupport(aFeatureSupportRegistry, null,
                            annotationSchemaService(), null),
                    new ChainLayerSupport(aFeatureSupportRegistry, null,
                            annotationSchemaService(), null)));
        }
    }
}

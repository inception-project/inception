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
package de.tudarmstadt.ukp.inception.search.index.mtas;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.MDC;
import org.apache.uima.fit.factory.JCasBuilder;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.BooleanFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.NumberFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.StringFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.ChainLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.RelationLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.SpanLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.AnnotationSchemaServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.BackupProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.CasStorageServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.DocumentImportExportServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.DocumentServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.config.CasStoragePropertiesImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.docimexport.config.DocumentImportExportServiceProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.docimexport.config.DocumentImportExportServicePropertiesImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.project.ProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.conll.Conll2002FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.project.ProjectServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.NamedEntityLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.NamedEntityTagSetInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.PartOfSpeechLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.PartOfSpeechTagSetInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.TokenLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDaoImpl;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.ApplicationContextProvider;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;
import de.tudarmstadt.ukp.clarin.webanno.text.TextFormatSupport;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseServiceImpl;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseProperties;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBasePropertiesImpl;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingServiceImpl;
import de.tudarmstadt.ukp.inception.scheduling.config.SchedulingProperties;
import de.tudarmstadt.ukp.inception.search.FeatureIndexingSupport;
import de.tudarmstadt.ukp.inception.search.FeatureIndexingSupportRegistry;
import de.tudarmstadt.ukp.inception.search.FeatureIndexingSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.search.PrimitiveUimaIndexingSupport;
import de.tudarmstadt.ukp.inception.search.SearchResult;
import de.tudarmstadt.ukp.inception.search.SearchService;
import de.tudarmstadt.ukp.inception.search.SearchServiceImpl;
import de.tudarmstadt.ukp.inception.search.config.SearchServiceProperties;
import de.tudarmstadt.ukp.inception.search.config.SearchServicePropertiesImpl;
import de.tudarmstadt.ukp.inception.search.index.PhysicalIndexFactory;
import de.tudarmstadt.ukp.inception.search.index.PhysicalIndexRegistry;
import de.tudarmstadt.ukp.inception.search.index.PhysicalIndexRegistryImpl;

@EnableAutoConfiguration
@EntityScan({ "de.tudarmstadt.ukp.clarin.webanno.model",
        "de.tudarmstadt.ukp.inception.search.model", "de.tudarmstadt.ukp.inception.kb.model",
        "de.tudarmstadt.ukp.clarin.webanno.security.model" })
@TestPropertySource(locations = "classpath:MtasDocumentIndexTest.properties")
@TestMethodOrder(MethodOrderer.MethodName.class)
@DataJpaTest(excludeAutoConfiguration = LiquibaseAutoConfiguration.class)
@Transactional(propagation = Propagation.NEVER)
public class MtasDocumentIndexTest
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private @Autowired UserDao userRepository;
    private @Autowired ProjectService projectService;
    private @Autowired DocumentService documentService;
    private @Autowired SearchService searchService;
    private @Autowired RepositoryProperties repositoryProperties;

    public @TempDir File temporaryFolder;

    @BeforeEach
    public void testWatcher(TestInfo aTestInfo)
    {
        String methodName = aTestInfo.getTestMethod().map(Method::getName).orElse("<unknown>");
        System.out.printf("\n=== %s === %s=====================\n", methodName,
                aTestInfo.getDisplayName());
    }

    @BeforeEach
    public void setUp()
    {
        repositoryProperties.setPath(temporaryFolder);

        MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

        if (!userRepository.exists("admin")) {
            userRepository.create(new User("admin", Role.ROLE_ADMIN));
        }
    }

    private void createProject(Project aProject) throws Exception
    {
        projectService.createProject(aProject);
        projectService.initializeProject(aProject);
    }

    @SafeVarargs
    private final void uploadDocument(Pair<SourceDocument, String>... aDocuments) throws Exception
    {
        Project project = null;
        try (CasStorageSession casStorageSession = CasStorageSession.open()) {
            for (Pair<SourceDocument, String> doc : aDocuments) {
                log.info("Uploading document via documentService.uploadSourceDocument: {}", doc);
                project = doc.getLeft().getProject();

                try (InputStream fileStream = new ByteArrayInputStream(
                        doc.getRight().getBytes(UTF_8))) {
                    documentService.uploadSourceDocument(fileStream, doc.getLeft());
                }
            }
        }

        // Avoid the compiler complaining about project not being an effectively final variable
        log.info("Waiting for uploaded documents to be indexed...");
        Project p = project;
        await("Waiting for indexing process to complete").atMost(60, SECONDS)
                .pollInterval(5, SECONDS)
                .until(() -> searchService.isIndexValid(p) && !searchService.isIndexInProgress(p));
        log.info("Indexing complete!");
    }

    private void annotateDocument(Project aProject, User aUser, SourceDocument aSourceDocument)
        throws Exception
    {
        log.info("Preparing annotated document....");

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
        try (CasStorageSession casStorageSession = CasStorageSession.open()) {
            log.info("Writing annotated document using documentService.writeAnnotationCas");
            documentService.writeAnnotationCas(jCas.getCas(), annotationDocument, false);
        }

        log.info("Writing for annotated document to be indexed");
        await("Waiting for indexing process to complete").atMost(60, SECONDS)
                .pollInterval(5, SECONDS).until(() -> searchService.isIndexValid(aProject)
                        && !searchService.isIndexInProgress(aProject));
        log.info("Indexing complete!");
    }

    @Test
    public void testRawTextQuery() throws Exception
    {
        Project project = new Project();
        project.setName("TestRawTextQuery");

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
        expectedResult.setDocumentTitle("Raw text document");
        expectedResult.setLeftContext("The capital of ");
        expectedResult.setText("Galicia");
        expectedResult.setRightContext(" is Santiago de");
        expectedResult.setOffsetStart(15);
        expectedResult.setOffsetEnd(22);
        expectedResult.setTokenStart(3);
        expectedResult.setTokenLength(1);

        assertThat(results).usingFieldByFieldElementComparator().containsExactly(expectedResult);
    }

    @Test
    public void thatLastTokenInDocumentCanBeFound() throws Exception
    {
        Project project = new Project();
        project.setName("LastTokenInDocumentCanBeFound");

        createProject(project);

        SourceDocument sourceDocument = new SourceDocument();

        sourceDocument.setName("Raw text document");
        sourceDocument.setProject(project);
        sourceDocument.setFormat("text");

        String fileContent = "The capital of Galicia is Santiago de Compostela.";

        uploadDocument(Pair.of(sourceDocument, fileContent));

        User user = userRepository.get("admin");

        String query = "\"\\.\"";

        // Execute query
        List<SearchResult> results = searchService.query(user, project, query);

        // Test results
        SearchResult expectedResult = new SearchResult();
        expectedResult.setDocumentId(sourceDocument.getId());
        expectedResult.setDocumentTitle("Raw text document");
        expectedResult.setLeftContext("Santiago de Compostela");
        expectedResult.setText(".");
        expectedResult.setRightContext("");
        expectedResult.setOffsetStart(48);
        expectedResult.setOffsetEnd(49);
        expectedResult.setTokenStart(8);
        expectedResult.setTokenLength(1);

        assertThat(results).usingFieldByFieldElementComparator().containsExactly(expectedResult);
    }

    @Test
    public void testLimitQueryToDocument() throws Exception
    {
        Project project = new Project();
        project.setName("TestLimitQueryToDocument");

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

        uploadDocument(Pair.of(sourceDocument1, fileContent1),
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
        expectedResult1.setText("capital");
        expectedResult1.setLeftContext("The ");
        expectedResult1.setRightContext(" of Galicia is");
        expectedResult1.setOffsetStart(4);
        expectedResult1.setOffsetEnd(11);
        expectedResult1.setTokenStart(1);
        expectedResult1.setTokenLength(1);

        SearchResult expectedResult2 = new SearchResult();
        expectedResult2.setDocumentId(sourceDocument2.getId());
        expectedResult2.setDocumentTitle("Raw text document 2");
        expectedResult2.setText("capital");
        expectedResult2.setLeftContext("The ");
        expectedResult2.setRightContext(" of Portugal is");
        expectedResult2.setOffsetStart(4);
        expectedResult2.setOffsetEnd(11);
        expectedResult2.setTokenStart(1);
        expectedResult2.setTokenLength(1);

        assertThat(resultsLimited).usingFieldByFieldElementComparator()
                .containsExactly(expectedResult1);

        assertThat(resultsNotLimited).usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedResult1, expectedResult2);
    }

    @Test
    public void testSimplifiedTokenTextQuery() throws Exception
    {
        Project project = new Project();
        project.setName("SimplifiedTokenTextQuery");

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
        expectedResult.setDocumentTitle("Raw text document");
        expectedResult.setText("Galicia");
        expectedResult.setLeftContext("The capital of ");
        expectedResult.setRightContext(" is Santiago de");
        expectedResult.setOffsetStart(15);
        expectedResult.setOffsetEnd(22);
        expectedResult.setTokenStart(3);
        expectedResult.setTokenLength(1);

        assertThat(results).usingFieldByFieldElementComparator().containsExactly(expectedResult);
    }

    @Test
    public void testAnnotationQuery() throws Exception
    {
        Project project = new Project();
        project.setName("TestAnnotationQuery");

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
        expectedResult.setDocumentTitle("Annotation document");
        // When searching for an annotation, we don't get the matching
        // text back... not sure why...
        expectedResult.setText("");
        expectedResult.setLeftContext("");
        expectedResult.setRightContext("");
        expectedResult.setOffsetStart(15);
        expectedResult.setOffsetEnd(22);
        expectedResult.setTokenStart(3);
        expectedResult.setTokenLength(1);

        assertThat(results).usingFieldByFieldElementComparator().containsExactly(expectedResult);
    }

    @Configuration
    public static class TestContext
    {
        private @Autowired ApplicationEventPublisher applicationEventPublisher;
        private @Autowired EntityManager entityManager;

        @Bean
        public ProjectService projectService(UserDao aUserRepository,
                @Lazy @Autowired(required = false) List<ProjectInitializer> aInitializerProxy,
                RepositoryProperties aRepositoryProperties)
        {
            return new ProjectServiceImpl(aUserRepository, applicationEventPublisher,
                    aRepositoryProperties, aInitializerProxy);
        }

        @Bean
        public PhysicalIndexFactory mtasDocumentIndexFactory(DocumentService aDocumentService,
                AnnotationSchemaService aSchemaService, RepositoryProperties aRepositoryProperties,
                FeatureIndexingSupportRegistry aFeatureIndexingSupportRegistry,
                FeatureSupportRegistry aFeatureSupportRegistry)
        {
            return new MtasDocumentIndexFactory(aSchemaService, aDocumentService,
                    aRepositoryProperties, aFeatureIndexingSupportRegistry,
                    aFeatureSupportRegistry);
        }

        @Bean
        public FeatureSupportRegistry featureSupportRegistry()
        {
            return new FeatureSupportRegistryImpl(asList(new NumberFeatureSupport(),
                    new BooleanFeatureSupport(), new StringFeatureSupport()));
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
        public NamedEntityLayerInitializer namedEntityLayerInitializer(
                @Lazy @Autowired AnnotationSchemaService aAnnotationService)
        {
            return new NamedEntityLayerInitializer(aAnnotationService);
        }

        @Lazy
        @Bean
        public NamedEntityTagSetInitializer namedEntityTagSetInitializer(
                @Lazy @Autowired AnnotationSchemaService aAnnotationService)
        {
            return new NamedEntityTagSetInitializer(aAnnotationService);
        }

        @Lazy
        @Bean
        public PartOfSpeechLayerInitializer partOfSpeechLayerInitializer(
                @Lazy @Autowired AnnotationSchemaService aAnnotationSchemaService)
        {
            return new PartOfSpeechLayerInitializer(aAnnotationSchemaService);
        }

        @Lazy
        @Bean
        public PartOfSpeechTagSetInitializer partOfSpeechTagSetInitializer(
                @Lazy @Autowired AnnotationSchemaService aAnnotationSchemaService)
        {
            return new PartOfSpeechTagSetInitializer(aAnnotationSchemaService);
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
        public SchedulingProperties schedulingProperties()
        {
            return new SchedulingProperties();
        }

        @Bean
        public SchedulingService schedulingService(ApplicationContext aContext,
                SchedulingProperties aSchedulingProperties)
        {
            return new SchedulingServiceImpl(aContext, aSchedulingProperties);
        }

        @Bean
        public SearchService searchService(DocumentService aDocumentService,
                ProjectService aProjectService, PhysicalIndexRegistry aPhysicalIndexRegistry,
                SchedulingService aSchedulingService, SearchServiceProperties aProperties)
        {
            return new SearchServiceImpl(aDocumentService, aProjectService, aPhysicalIndexRegistry,
                    aSchedulingService, aProperties);
        }

        @Bean
        public SearchServiceProperties searchServiceProperties()
        {
            SearchServicePropertiesImpl properties = new SearchServicePropertiesImpl();
            properties.setEnabled(true);
            return properties;
        }

        @Bean
        public UserDao userRepository()
        {
            return new UserDaoImpl();
        }

        @Bean
        public KnowledgeBaseService knowledgeBaseService(RepositoryProperties aRepoProperties,
                KnowledgeBaseProperties aKBProperties)
        {
            return new KnowledgeBaseServiceImpl(aRepoProperties, aKBProperties);
        }

        @Bean
        public DocumentService documentService(RepositoryProperties aRepositoryProperties,
                CasStorageService aCasStorageService,
                DocumentImportExportService aImportExportService, ProjectService aProjectService)
        {
            return new DocumentServiceImpl(aRepositoryProperties, aCasStorageService,
                    aImportExportService, aProjectService, applicationEventPublisher,
                    entityManager);
        }

        @Bean
        public AnnotationSchemaService annotationSchemaService()
        {
            return new AnnotationSchemaServiceImpl(layerSupportRegistry(), featureSupportRegistry(),
                    entityManager);
        }

        @Bean
        public CasStorageService casStorageService()
        {
            return new CasStorageServiceImpl(null, null, repositoryProperties(),
                    new CasStoragePropertiesImpl(), backupProperties());
        }

        @Bean
        public DocumentImportExportService importExportService()
        {
            DocumentImportExportServiceProperties properties = new DocumentImportExportServicePropertiesImpl();
            return new DocumentImportExportServiceImpl(repositoryProperties(),
                    asList(new TextFormatSupport(), new Conll2002FormatSupport()),
                    casStorageService(), annotationSchemaService(), properties);
        }

        @Bean
        public CurationDocumentService curationDocumentService(CasStorageService aCasStorageService,
                AnnotationSchemaService aAnnotationService)
        {
            return new CurationDocumentServiceImpl(aCasStorageService, aAnnotationService);
        }

        @Bean
        public RepositoryProperties repositoryProperties()
        {
            return new RepositoryProperties();
        }

        @Bean
        public KnowledgeBaseProperties knowledgeBaseProperties()
        {
            return new KnowledgeBasePropertiesImpl();
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
            FeatureSupportRegistry fsr = featureSupportRegistry();

            return new LayerSupportRegistryImpl(asList(new SpanLayerSupport(fsr, null, null, null),
                    new RelationLayerSupport(fsr, null, null),
                    new ChainLayerSupport(fsr, null, null)));
        }
    }
}

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
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
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
    private final void uploadDocument(Pair<SourceDocument, String>... aDocuments)
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
        documentService.writeAnnotationCas(jCas.getCas(), annotationDocument, false);

        await("Waiting for indexing process to complete")
                .atMost(60, SECONDS)
                .pollInterval(5, SECONDS)
                .until(() -> searchService.isIndexValid(aProject)
                        && !searchService.isIndexInProgress(aProject));
    }

    @Test
    public void testRawTextQuery() throws Exception
    {
        Project project8 = new Project();
        project8.setName("TestRawTextQuery");
        project8.setMode(WebAnnoConst.PROJECT_TYPE_ANNOTATION);

        createProject(project8);

        SourceDocument sourceDocument8 = new SourceDocument();

        sourceDocument8.setName("Raw text document");
        sourceDocument8.setProject(project8);
        sourceDocument8.setFormat("text");

        String fileContent8 = "The capital of Galicia is Santiago de Compostela.";

        uploadDocument(Pair.of(sourceDocument8, fileContent8));

        User user8 = userRepository.get("admin");

        String query8 = "Galicia";

        // Execute query
        List<SearchResult> results8 = searchService.query(user8, project8, query8);

        // Test results
        SearchResult expectedResult8 = new SearchResult();
        expectedResult8.setDocumentId(sourceDocument8.getId());
        expectedResult8.setDocumentTitle("Raw text document");
        expectedResult8.setLeftContext("The capital of ");
        expectedResult8.setText("Galicia");
        expectedResult8.setRightContext(" is Santiago de");
        expectedResult8.setOffsetStart(15);
        expectedResult8.setOffsetEnd(22);
        expectedResult8.setTokenStart(3);
        expectedResult8.setTokenLength(1);

        assertThat(results8)
                .usingFieldByFieldElementComparator()
                .containsExactly(expectedResult8);
    }

    @Test
    public void thatLastTokenInDocumentCanBeFound() throws Exception
    {
        Project project7 = new Project();
        project7.setName("LastTokenInDocumentCanBeFound");
        project7.setMode(WebAnnoConst.PROJECT_TYPE_ANNOTATION);

        createProject(project7);

        SourceDocument sourceDocument7 = new SourceDocument();

        sourceDocument7.setName("Raw text document");
        sourceDocument7.setProject(project7);
        sourceDocument7.setFormat("text");

        String fileContent7 = "The capital of Galicia is Santiago de Compostela.";

        uploadDocument(Pair.of(sourceDocument7, fileContent7));

        User user7 = userRepository.get("admin");

        String query7 = "\"\\.\"";

        // Execute query
        List<SearchResult> results7 = searchService.query(user7, project7, query7);

        // Test results
        SearchResult expectedResult7 = new SearchResult();
        expectedResult7.setDocumentId(sourceDocument7.getId());
        expectedResult7.setDocumentTitle("Raw text document");
        expectedResult7.setLeftContext("Santiago de Compostela");
        expectedResult7.setText(".");
        expectedResult7.setRightContext("");
        expectedResult7.setOffsetStart(48);
        expectedResult7.setOffsetEnd(49);
        expectedResult7.setTokenStart(8);
        expectedResult7.setTokenLength(1);

        assertThat(results7)
                .usingFieldByFieldElementComparator()
                .containsExactly(expectedResult7);
    }
    @Test
    public void testLimitQueryToDocument() throws Exception
    {
        Project project6 = new Project();
        project6.setName("TestLimitQueryToDocument");
        project6.setMode(WebAnnoConst.PROJECT_TYPE_ANNOTATION);

        createProject(project6);

        SourceDocument sourceDocument6 = new SourceDocument();
        sourceDocument6.setName("Raw text document 1");
        sourceDocument6.setProject(project6);
        sourceDocument6.setFormat("text");
        String fileContent6 = "The capital of Galicia is Santiago de Compostela.";

        SourceDocument sourceDocument66 = new SourceDocument();
        sourceDocument66.setName("Raw text document 2");
        sourceDocument66.setProject(project6);
        sourceDocument66.setFormat("text");
        String fileContent66 = "The capital of Portugal is Lissabon.";
        
        uploadDocument(
                Pair.of(sourceDocument6, fileContent6),
                Pair.of(sourceDocument66, fileContent66));

        User user6 = userRepository.get("admin");

        String query6 = "capital";

        // Execute query
        SourceDocument sourceDocument666 = documentService.getSourceDocument(project6,
                "Raw text document 1");
        List<SearchResult> resultsNotLimited6 = searchService.query(user6, project6, query6);
        List<SearchResult> resultsLimited6 = searchService.query(user6, project6, query6,
                sourceDocument6);

        // Test results
        SearchResult expectedResult6 = new SearchResult();
        expectedResult6.setDocumentId(sourceDocument6.getId());
        expectedResult6.setDocumentTitle("Raw text document 1");
        expectedResult6.setText("capital");
        expectedResult6.setLeftContext("The ");
        expectedResult6.setRightContext(" of Galicia is");
        expectedResult6.setOffsetStart(4);
        expectedResult6.setOffsetEnd(11);
        expectedResult6.setTokenStart(1);
        expectedResult6.setTokenLength(1);

        SearchResult expectedResult66 = new SearchResult();
        expectedResult66.setDocumentId(sourceDocument66.getId());
        expectedResult66.setDocumentTitle("Raw text document 2");
        expectedResult66.setText("capital");
        expectedResult66.setLeftContext("The ");
        expectedResult66.setRightContext(" of Portugal is");
        expectedResult66.setOffsetStart(4);
        expectedResult66.setOffsetEnd(11);
        expectedResult66.setTokenStart(1);
        expectedResult66.setTokenLength(1);

        assertThat(resultsLimited6)
                .usingFieldByFieldElementComparator()
                .containsExactly(expectedResult6);
        
        assertThat(resultsNotLimited6)
                .usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedResult6, expectedResult66);
    }

    @Test
    public void testSimplifiedTokenTextQuery() throws Exception
    {
        Project project9 = new Project();
        project9.setName("SimplifiedTokenTextQuery");
        project9.setMode(WebAnnoConst.PROJECT_TYPE_ANNOTATION);

        createProject(project9);

        SourceDocument sourceDocument9 = new SourceDocument();

        sourceDocument9.setName("Raw text document");
        sourceDocument9.setProject(project9);
        sourceDocument9.setFormat("text");

        String fileContent9 = "The capital of Galicia is Santiago de Compostela.";

        uploadDocument(Pair.of(sourceDocument9, fileContent9));

        User user9 = userRepository.get("admin");

        String query9 = "\"Galicia\"";

        // Execute query
        List<SearchResult> results9 = searchService.query(user9, project9, query9);

        // Test results
        SearchResult expectedResult9 = new SearchResult();
        expectedResult9.setDocumentId(sourceDocument9.getId());
        expectedResult9.setDocumentTitle("Raw text document");
        expectedResult9.setText("Galicia");
        expectedResult9.setLeftContext("The capital of ");
        expectedResult9.setRightContext(" is Santiago de");
        expectedResult9.setOffsetStart(15);
        expectedResult9.setOffsetEnd(22);
        expectedResult9.setTokenStart(3);
        expectedResult9.setTokenLength(1);

        assertThat(results9)
                .usingFieldByFieldElementComparator()
                .containsExactly(expectedResult9);
    }
    
    @Test
    public void testAnnotationQuery() throws Exception
    {
        Project project5 = new Project();
        project5.setName("TestAnnotationQuery");
        project5.setMode(WebAnnoConst.PROJECT_TYPE_ANNOTATION);

        createProject(project5);

        User user5 = userRepository.get("admin");

        SourceDocument sourceDocument5 = new SourceDocument();

        sourceDocument5.setName("Annotation document");
        sourceDocument5.setProject(project5);
        sourceDocument5.setFormat("text");

        String fileContent5 = "The capital of Galicia is Santiago de Compostela.";

        uploadDocument(Pair.of(sourceDocument5, fileContent5));
        annotateDocument(project5, user5, sourceDocument5);

        String query5 = "<Named_entity.value=\"LOC\"/>";

        List<SearchResult> results5 = searchService.query(user5, project5, query5);

        // Test results
        SearchResult expectedResult5 = new SearchResult();
        expectedResult5.setDocumentId(sourceDocument5.getId());
        expectedResult5.setDocumentTitle("Annotation document");
        // When searching for an annotation, we don't get the matching
        // text back... not sure why...
        expectedResult5.setText("");
        expectedResult5.setLeftContext("");
        expectedResult5.setRightContext("");
        expectedResult5.setOffsetStart(15);
        expectedResult5.setOffsetEnd(22);
        expectedResult5.setTokenStart(3);
        expectedResult5.setTokenLength(1);

        assertThat(results5)
                .usingFieldByFieldElementComparator()
                .containsExactly(expectedResult5);
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
            return new CasStorageServiceImpl(null, null, repositoryProperties(),
                    backupProperties());
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

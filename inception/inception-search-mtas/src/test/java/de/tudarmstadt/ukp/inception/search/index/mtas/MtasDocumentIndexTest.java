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
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.ByteArrayInputStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.fit.factory.JCasBuilder;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.conll.config.ConllFormatsAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.constraints.config.ConstraintsServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.diag.config.CasDoctorAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.config.ProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.text.config.TextFormatsAutoConfiguration;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.config.RelationLayerAutoConfiguration;
import de.tudarmstadt.ukp.inception.annotation.storage.CasMetadataUtils;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryAutoConfiguration;
import de.tudarmstadt.ukp.inception.documents.config.DocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.export.config.DocumentImportExportServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.io.xmi.XmiFormatSupport;
import de.tudarmstadt.ukp.inception.io.xmi.config.UimaFormatsPropertiesImpl.XmiFormatProperties;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.preferences.config.PreferencesServiceAutoConfig;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.scheduling.config.SchedulingServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.schema.config.AnnotationSchemaServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.search.LayerStatistics;
import de.tudarmstadt.ukp.inception.search.SearchResult;
import de.tudarmstadt.ukp.inception.search.SearchServiceImpl;
import de.tudarmstadt.ukp.inception.search.StatisticsResult;
import de.tudarmstadt.ukp.inception.search.config.SearchServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.search.index.mtas.config.MtasDocumentIndexAutoConfiguration;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationContextProvider;

@EnableAutoConfiguration
@EntityScan({ //
        "de.tudarmstadt.ukp.clarin.webanno.model", //
        "de.tudarmstadt.ukp.inception.search.model", //
        "de.tudarmstadt.ukp.inception.kb.model", //
        "de.tudarmstadt.ukp.clarin.webanno.security.model", //
        "de.tudarmstadt.ukp.inception.preferences.model" })
@TestMethodOrder(MethodOrderer.MethodName.class)
@DataJpaTest( //
        excludeAutoConfiguration = LiquibaseAutoConfiguration.class, //
        showSql = false, //
        properties = { //
                "recommender.enabled=false", //
                "spring.main.banner-mode=off", //
                "debug.cas-doctor.force-release-behavior=true", //
                "document-import.run-cas-doctor-on-import=OFF" })
// REC: Not particularly clear why Propagation.NEVER is required, but if it is not there, the test
// waits forever for the indexing to complete...
@Transactional(propagation = Propagation.NEVER)
@Import({ //
        ConstraintsServiceAutoConfiguration.class, //
        AnnotationSchemaServiceAutoConfiguration.class, //
        TextFormatsAutoConfiguration.class, //
        ConllFormatsAutoConfiguration.class, //
        DocumentImportExportServiceAutoConfiguration.class, //
        CasDoctorAutoConfiguration.class, //
        DocumentServiceAutoConfiguration.class, //
        ProjectServiceAutoConfiguration.class, //
        ProjectInitializersAutoConfiguration.class, //
        CasStorageServiceAutoConfiguration.class, //
        RepositoryAutoConfiguration.class, //
        SecurityAutoConfiguration.class, //
        PreferencesServiceAutoConfig.class, //
        SearchServiceAutoConfiguration.class, //
        SchedulingServiceAutoConfiguration.class, //
        MtasDocumentIndexAutoConfiguration.class, //
        KnowledgeBaseServiceAutoConfiguration.class, //
        RelationLayerAutoConfiguration.class })
class MtasDocumentIndexTest
{

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @Autowired UserDao userRepository;
    private @Autowired ProjectService projectService;
    private @Autowired DocumentService documentService;
    private @Autowired SearchServiceImpl searchService;

    private User user;

    static @TempDir Path tempFolder;
    static SearchServiceImpl _searchService;

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry)
    {
        registry.add("repository.path", () -> tempFolder.toAbsolutePath().toString());
    }

    @BeforeEach
    void testWatcher(TestInfo aTestInfo)
    {
        var methodName = aTestInfo.getTestMethod().map(Method::getName).orElse("<unknown>");
        System.out.printf("\n=== %s === %s=====================\n", methodName,
                aTestInfo.getDisplayName());
    }

    @BeforeEach
    void setUp()
    {
        _searchService = searchService;

        if (!userRepository.exists("admin")) {
            userRepository.create(new User("admin", Role.ROLE_ADMIN));
        }

        user = userRepository.get("admin");
    }

    @AfterAll
    static void afterAll()
    {
        if (_searchService != null) {
            _searchService.destroy();
        }
    }

    private void createProject(Project aProject) throws Exception
    {
        projectService.createProject(aProject);
        projectService.initializeProject(aProject);
    }

    @SafeVarargs
    private final void uploadAndIndexDocument(Pair<SourceDocument, String>... aDocuments)
        throws Exception
    {
        if (aDocuments == null || aDocuments.length == 0) {
            throw new IllegalArgumentException("At least one document must be provided");
        }

        var project = aDocuments[0].getLeft().getProject();

        try (var session = CasStorageSession.open()) {
            for (var doc : aDocuments) {
                LOG.info("Uploading document via documentService.uploadSourceDocument: {}", doc);

                try (var fileStream = new ByteArrayInputStream(doc.getRight().getBytes(UTF_8))) {
                    documentService.uploadSourceDocument(fileStream, doc.getLeft());
                }
            }
        }

        LOG.info("Waiting for uploaded documents to be indexed...");
        await("Waiting for indexing process to complete") //
                .atMost(60, SECONDS) //
                .pollInterval(200, MILLISECONDS) //
                .until(() -> searchService.isIndexValid(project)
                        && searchService.getIndexProgress(project).isEmpty());
        LOG.info("Indexing complete!");
    }

    private void annotateDocument(Project aProject, User aUser, SourceDocument aSourceDocument)
        throws Exception
    {
        LOG.info("Preparing annotated document....");

        // Manually build annotated CAS
        var internalTsd = CasMetadataUtils.getInternalTypeSystem();
        var globalTsd = TypeSystemDescriptionFactory.createTypeSystemDescription();
        var tsd = CasCreationUtils.mergeTypeSystems(asList(globalTsd, internalTsd));
        var jCas = JCasFactory.createJCas(tsd);

        var builder = new JCasBuilder(jCas);

        builder.add("The", Token.class);
        builder.add(" ");
        builder.add("capital", Token.class);
        builder.add(" ");
        builder.add("of", Token.class);
        builder.add(" ");

        var begin = builder.getPosition();
        builder.add("Galicia", Token.class);

        var ne = new NamedEntity(jCas, begin, builder.getPosition());
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
        builder.close();

        // Create annotation document
        var annotationDocument = documentService.createOrGetAnnotationDocument(aSourceDocument,
                aUser);

        // Write annotated CAS to annotated document
        try (var casStorageSession = CasStorageSession.open()) {
            LOG.info("Writing annotated document using documentService.writeAnnotationCas");
            documentService.writeAnnotationCas(jCas.getCas(), annotationDocument);
        }

        LOG.info("Writing for annotated document to be indexed");
        await("Waiting for indexing process to complete") //
                .atMost(60, SECONDS) //
                .pollInterval(200, MILLISECONDS) //
                .until(() -> searchService.isIndexValid(aProject)
                        && searchService.getIndexProgress(aProject).isEmpty());
        LOG.info("Indexing complete!");
    }

    void annotateDocumentAdvanced(Project aProject, User aUser, SourceDocument aSourceDocument)
        throws Exception
    {
        LOG.info("Preparing annotated document....");

        // Manually build annotated CAS
        var internalTsd = CasMetadataUtils.getInternalTypeSystem();
        var globalTsd = TypeSystemDescriptionFactory.createTypeSystemDescription();
        var tsd = CasCreationUtils.mergeTypeSystems(asList(globalTsd, internalTsd));
        var jCas = JCasFactory.createJCas(tsd);

        var builder = new JCasBuilder(jCas);

        builder.add("The", Token.class);
        builder.add(" ");
        builder.add("capital", Token.class);
        builder.add(" ");
        builder.add("of", Token.class);
        builder.add(" ");

        var begin = builder.getPosition();
        builder.add("Galicia", Token.class);

        var ne = new NamedEntity(jCas, begin, builder.getPosition());
        ne.setValue("LOC");
        ne.addToIndexes();

        builder.add(" ");
        builder.add("is", Token.class);
        builder.add(" ");

        begin = builder.getPosition();

        builder.add("Santiago", Token.class);
        builder.add(" ");
        builder.add("de", Token.class);
        builder.add(" ");
        builder.add("Compostela", Token.class);

        ne = new NamedEntity(jCas, begin, builder.getPosition());
        ne.setValue("LOC");
        ne.addToIndexes();

        builder.add(" ");
        builder.add(".", Token.class);

        var sent = new Sentence(jCas, 0, builder.getPosition());
        sent.addToIndexes();

        // Create annotation document
        var annotationDocument = documentService.createOrGetAnnotationDocument(aSourceDocument,
                aUser);

        // Write annotated CAS to annotated document
        try (CasStorageSession casStorageSession = CasStorageSession.open()) {
            LOG.info("Writing annotated document using documentService.writeAnnotationCas");
            documentService.writeAnnotationCas(jCas.getCas(), annotationDocument);
        }

        LOG.info("Writing for annotated document to be indexed");
        await("Waiting for indexing process to complete") //
                .atMost(60, SECONDS) //
                .pollInterval(200, MILLISECONDS) //
                .until(() -> searchService.isIndexValid(aProject)
                        && searchService.getIndexProgress(aProject).isEmpty());
        LOG.info("Indexing complete!");
    }

    @Test
    void testRawTextQuery() throws Exception
    {
        var project = new Project("raw-text-query");

        createProject(project);

        var sourceDocument = new SourceDocument("Raw text document", project, "text");

        var fileContent = "The capital of Galicia is Santiago de Compostela.";

        uploadAndIndexDocument(Pair.of(sourceDocument, fileContent));

        var query = "Galicia";

        // Execute query
        var results = searchService.query(user, project, query);

        // Test results
        var expectedResult = new SearchResult();
        expectedResult.setDocumentId(sourceDocument.getId());
        expectedResult.setDocumentTitle("Raw text document");
        expectedResult.setLeftContext("The capital of ");
        expectedResult.setText("Galicia");
        expectedResult.setRightContext(" is Santiago de");
        expectedResult.setOffsetStart(15);
        expectedResult.setOffsetEnd(22);
        expectedResult.setTokenStart(3);
        expectedResult.setTokenLength(1);

        assertThat(results).containsExactly(expectedResult);
    }

    @Test
    void thatLastTokenInDocumentCanBeFound() throws Exception
    {
        var project = new Project("last-token-in-document-can-be-found");

        createProject(project);

        var sourceDocument = new SourceDocument();

        sourceDocument.setName("Raw text document");
        sourceDocument.setProject(project);
        sourceDocument.setFormat("text");

        var fileContent = "The capital of Galicia is Santiago de Compostela.";

        uploadAndIndexDocument(Pair.of(sourceDocument, fileContent));

        var query = "\"\\.\"";

        // Execute query
        var results = searchService.query(user, project, query);

        // Test results
        var expectedResult = new SearchResult();
        expectedResult.setDocumentId(sourceDocument.getId());
        expectedResult.setDocumentTitle("Raw text document");
        expectedResult.setLeftContext("Santiago de Compostela");
        expectedResult.setText(".");
        expectedResult.setRightContext("");
        expectedResult.setOffsetStart(48);
        expectedResult.setOffsetEnd(49);
        expectedResult.setTokenStart(8);
        expectedResult.setTokenLength(1);

        assertThat(results).containsExactly(expectedResult);
    }

    @Test
    void testLimitQueryToDocument() throws Exception
    {
        var project = new Project("limit-query-to-document");

        createProject(project);

        var sourceDocument1 = new SourceDocument("Raw text document 1", project, "text");
        var fileContent1 = "The capital of Galicia is Santiago de Compostela.";

        var sourceDocument2 = new SourceDocument("Raw text document 2", project, "text");
        var fileContent2 = "The capital of Portugal is Lissabon.";

        uploadAndIndexDocument(Pair.of(sourceDocument1, fileContent1));
        uploadAndIndexDocument(Pair.of(sourceDocument2, fileContent2));

        var query = "capital";

        // Execute query
        var sourceDocument = documentService.getSourceDocument(project, "Raw text document 1");
        var resultsNotLimited = searchService.query(user, project, query);
        var resultsLimited = searchService.query(user, project, query, sourceDocument);

        // Test results
        var expectedResult1 = new SearchResult();
        expectedResult1.setDocumentId(sourceDocument1.getId());
        expectedResult1.setDocumentTitle("Raw text document 1");
        expectedResult1.setText("capital");
        expectedResult1.setLeftContext("The ");
        expectedResult1.setRightContext(" of Galicia is");
        expectedResult1.setOffsetStart(4);
        expectedResult1.setOffsetEnd(11);
        expectedResult1.setTokenStart(1);
        expectedResult1.setTokenLength(1);

        var expectedResult2 = new SearchResult();
        expectedResult2.setDocumentId(sourceDocument2.getId());
        expectedResult2.setDocumentTitle("Raw text document 2");
        expectedResult2.setText("capital");
        expectedResult2.setLeftContext("The ");
        expectedResult2.setRightContext(" of Portugal is");
        expectedResult2.setOffsetStart(4);
        expectedResult2.setOffsetEnd(11);
        expectedResult2.setTokenStart(1);
        expectedResult2.setTokenLength(1);

        assertThat(resultsLimited).containsExactly(expectedResult1);

        assertThat(resultsNotLimited).containsExactlyInAnyOrder(expectedResult1, expectedResult2);
    }

    @Test
    void testSimplifiedTokenTextQuery() throws Exception
    {
        var project = new Project("simplified-token-text-query");

        createProject(project);

        var sourceDocument = SourceDocument.builder() //
                .withName("Raw text document") //
                .withProject(project) //
                .withFormat("text") // v
                .build();
        var fileContent = "The capital of Galicia is Santiago de Compostela.";
        uploadAndIndexDocument(Pair.of(sourceDocument, fileContent));

        var query = "\"galicia\"";

        // Execute query
        var results = searchService.query(user, project, query);

        // Test results
        var expectedResult = new SearchResult();
        expectedResult.setDocumentId(sourceDocument.getId());
        expectedResult.setDocumentTitle("Raw text document");
        expectedResult.setText("Galicia");
        expectedResult.setLeftContext("The capital of ");
        expectedResult.setRightContext(" is Santiago de");
        expectedResult.setOffsetStart(15);
        expectedResult.setOffsetEnd(22);
        expectedResult.setTokenStart(3);
        expectedResult.setTokenLength(1);

        assertThat(results).containsExactly(expectedResult);
    }

    @Test
    void testAnnotationQuery() throws Exception
    {
        var project = new Project("annotation-query");

        createProject(project);

        var sourceDocument = new SourceDocument("Annotation document", project, "text");

        var fileContent = "The capital of Galicia is Santiago de Compostela.";

        uploadAndIndexDocument(Pair.of(sourceDocument, fileContent));
        annotateDocument(project, user, sourceDocument);

        var query = "<Named_entity.value=\"LOC\"/>";

        var results = searchService.query(user, project, query);

        // Test results
        var expectedResult = new SearchResult();
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

        assertThat(results).containsExactly(expectedResult);
    }

    @Test
    void testAnnotationQueryRegex() throws Exception
    {
        var project = new Project("annotation-query-regex");

        createProject(project);

        var sourceDocument = new SourceDocument("Annotation document", project, "text");

        var fileContent = "The capital of Galicia is Santiago de Compostela.";

        uploadAndIndexDocument(Pair.of(sourceDocument, fileContent));
        annotateDocument(project, user, sourceDocument);

        var query = "<Named_entity=\".*\"/>";

        var results = searchService.query(user, project, query);

        // Test results
        var expectedResult = new SearchResult();
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

        assertThat(results).containsExactly(expectedResult);
    }

    @Test
    void testAnnotationQueryMultiTokenWithoutConditions() throws Exception
    {
        var project = new Project("annotation-query-multi-token-no-cond");

        createProject(project);

        var sourceDocument = new SourceDocument("Annotation document", project, "text");

        var fileContent = "The capital of Galicia is Santiago de Compostela.";

        uploadAndIndexDocument(Pair.of(sourceDocument, fileContent));
        annotateDocument(project, user, sourceDocument);

        var query = "<Named_entity/>";

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

        assertThat(results).containsExactly(expectedResult);
    }

    @Test
    void testKeepResultsOrdering() throws Exception
    {
        var project = new Project("keep-results-ordering");

        createProject(project);

        var sourceDocument1 = new SourceDocument("Annotation document 1", project, "text");
        var fileContent1 = "The capital of Galicia is Santiago de Compostela.";
        uploadAndIndexDocument(Pair.of(sourceDocument1, fileContent1));
        annotateDocument(project, user, sourceDocument1);

        var sourceDocument2 = new SourceDocument("Annotation document 2", project, "text");
        var fileContent2 = "The capital of Galicia is Santiago de Compostela.";
        uploadAndIndexDocument(Pair.of(sourceDocument2, fileContent2));
        annotateDocument(project, user, sourceDocument2);

        var query = "<Named_entity.value=\"LOC\"/>";
        annotateDocument(project, user, sourceDocument1);

        var resultsBefore = searchService.query(user, project, query, null, null, null, 0, 10);
        annotateDocument(project, user, sourceDocument1);

        var resultsAfter = searchService.query(user, project, query, null, null, null, 0, 10);

        // Before the fix, the keys of resultsAfter were ["Annotation document 2", "Annotation
        // document 1"].
        // Document 1 moved to the back of the index because we updated its annotation
        assertThat(resultsBefore.keySet()) //
                .containsExactlyElementsOf(resultsAfter.keySet());
    }

    @Disabled("This test is flaky, but I do not know why - maybe some race condition in the indexing")
    @Test
    void testStatistics() throws Exception
    {
        // Create sample project with two documents
        var project = new Project("statistics");

        createProject(project);

        var sourceDocument = new SourceDocument("Annotation document", project, "text");

        var sourceContent = "The capital of Galicia is Santiago de Compostela.";

        uploadAndIndexDocument(Pair.of(sourceDocument, sourceContent));
        annotateDocumentAdvanced(project, user, sourceDocument);

        var otherDocument = new SourceDocument("Other document", project, "text");

        var otherContent = "Goodbye moon. Hello World.";
        uploadAndIndexDocument(Pair.of(otherDocument, otherContent));

        // Define input for the statistics methods
        var minTokenPerDoc = Integer.MIN_VALUE;
        var maxTokenPerDoc = Integer.MAX_VALUE;

        var ne = new AnnotationLayer();
        ne.setUiName("Named entity");
        var value = new AnnotationFeature();
        value.setUiName("value");
        value.setLayer(ne);
        var features = new HashSet<AnnotationFeature>();
        features.add(value);

        var raw = new AnnotationLayer();
        raw.setUiName("Segmentation");
        var sent = new AnnotationFeature();
        sent.setUiName("sentence");
        sent.setLayer(raw);
        var token = new AnnotationFeature();
        token.setUiName("token");
        token.setLayer(raw);

        // Check layer-based statistics
        var statsResults = searchService.getProjectStatistics(user, project, minTokenPerDoc,
                maxTokenPerDoc, features);

        Map<String, LayerStatistics> expectedResults = new HashMap<String, LayerStatistics>();

        var expectedNamedEntity = new LayerStatistics(2.0, 2.0, 0.0, 1.0, 1.0, Math.pow(2, 0.5),
                2.0, 2.0, 0.0, 1.0, 1.0, Math.pow(2, 0.5), 2.0);
        expectedNamedEntity.setFeature(value);
        var expectedToken = new LayerStatistics(15.0, 9.0, 6.0, 7.5, 7.5, Math.pow(4.5, 0.5), 12.0,
                9.0, 3.0, 6.0, 6.0, Math.pow(18, 0.5), 2.0);
        expectedToken.setFeature(token);
        var expectedSentence = new LayerStatistics(3.0, 2.0, 1.0, 1.5, 1.5, Math.pow(0.5, 0.5), 2.0,
                1.0, 1.0, 1.0, 1.0, 0.0, 2.0);
        expectedSentence.setFeature(sent);

        expectedResults.put("Named entity.value", expectedNamedEntity);
        expectedResults.put("Segmentation.token", expectedToken);
        expectedResults.put("Segmentation.sentence", expectedSentence);

        assertThat(statsResults.getMaxTokenPerDoc()).isEqualTo(maxTokenPerDoc);
        assertThat(statsResults.getMinTokenPerDoc()).isEqualTo(minTokenPerDoc);
        assertThat(statsResults.getProject()).isEqualTo(project);
        assertThat(statsResults.getUser()).isEqualTo(user);
        assertThat(statsResults.getResults()).containsAllEntriesOf(expectedResults);

        // Check query-based statistics
        String query = "moon";

        StatisticsResult queryStatsResults = searchService.getQueryStatistics(user, project, query,
                minTokenPerDoc, maxTokenPerDoc, features);

        var expected = new HashMap<String, LayerStatistics>();

        var expectedSearch = new LayerStatistics(1.0, 1.0, 0.0, 0.5, 0.5, Math.pow(0.5, 0.5), 0.5,
                0.5, 0.0, 0.25, 0.25, Math.pow(0.125, 0.5), 2.0);
        expectedSearch.setQuery("moon");
        expected.put("query.moon", expectedSearch);

        assertThat(queryStatsResults.getMinTokenPerDoc()).isEqualTo(minTokenPerDoc);
        assertThat(queryStatsResults.getMaxTokenPerDoc()).isEqualTo(maxTokenPerDoc);
        assertThat(queryStatsResults.getUser()).isEqualTo(user);
        assertThat(queryStatsResults.getProject()).isEqualTo(project);
        assertThat(queryStatsResults.getResults()).isEqualTo(expected);
    }

    @SpringBootConfiguration
    public static class TestContext
    {
        @Bean
        public ApplicationContextProvider contextProvider()
        {
            return new ApplicationContextProvider();
        }

        @Bean
        public XmiFormatSupport xmiFormatSupport()
        {
            return new XmiFormatSupport(new XmiFormatProperties());
        }
    }
}

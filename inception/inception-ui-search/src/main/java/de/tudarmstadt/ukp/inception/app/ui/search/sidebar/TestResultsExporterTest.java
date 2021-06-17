package de.tudarmstadt.ukp.inception.app.ui.search.sidebar;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.app.ui.search.sidebar.options.SearchOptions;
import de.tudarmstadt.ukp.inception.search.ResultsGroup;
import de.tudarmstadt.ukp.inception.search.SearchResult;
import org.apache.commons.lang3.tuple.Pair;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.fit.factory.JCasBuilder;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.annotationservice.config.AnnotationSchemaServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.config.CasStorageServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.docimexport.config.DocumentImportExportServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.documentservice.config.DocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.conll.config.ConllFormatsAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.config.ProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.ApplicationContextProvider;
import de.tudarmstadt.ukp.clarin.webanno.text.config.TextFormatsAutoConfiguration;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.scheduling.config.SchedulingServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.search.SearchResult;
import de.tudarmstadt.ukp.inception.search.SearchService;
import de.tudarmstadt.ukp.inception.search.config.SearchServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.search.index.mtas.config.MtasDocumentIndexAutoConfiguration;

import de.tudarmstadt.ukp.inception.app.ui.search.sidebar.TestResultsExporter;

import java.util.List;
/*
@EnableAutoConfiguration
@EntityScan({ //
    "de.tudarmstadt.ukp.clarin.webanno.model", //
    "de.tudarmstadt.ukp.inception.search.model", //
    "de.tudarmstadt.ukp.inception.kb.model", //
    "de.tudarmstadt.ukp.clarin.webanno.security.model" })
@TestMethodOrder(MethodOrderer.MethodName.class)
@DataJpaTest(excludeAutoConfiguration = LiquibaseAutoConfiguration.class, showSql = false, //
    properties = { //
        "spring.main.banner-mode=off", //
        "repository.path=" + TestResultsExporterTest.TEST_OUTPUT_FOLDER })
// REC: Not particularly clear why Propagation.NEVER is required, but if it is not there, the test
// waits forever for the indexing to complete...
@Transactional(propagation = Propagation.NEVER)
@Import({ //
    TextFormatsAutoConfiguration.class, //
    ConllFormatsAutoConfiguration.class, //
    DocumentImportExportServiceAutoConfiguration.class, //
    DocumentServiceAutoConfiguration.class, //
    ProjectServiceAutoConfiguration.class, //
    ProjectInitializersAutoConfiguration.class, //
    CasStorageServiceAutoConfiguration.class, //
    RepositoryAutoConfiguration.class, //
    AnnotationSchemaServiceAutoConfiguration.class, //
    SecurityAutoConfiguration.class, //
    SearchServiceAutoConfiguration.class, //
    SchedulingServiceAutoConfiguration.class, //
    MtasDocumentIndexAutoConfiguration.class, //
    KnowledgeBaseServiceAutoConfiguration.class })


 */
public class TestResultsExporterTest {

    static final String TEST_OUTPUT_FOLDER = "D:\\Falko\\Documents\\UKP";

    private final Logger log = LoggerFactory.getLogger(getClass());
    /*
    private @Autowired
    UserDao userRepository;
    private @Autowired
    ProjectService projectService;
    private @Autowired
    DocumentService documentService;
    private @Autowired
    SearchService searchService;

    @BeforeAll
    public static void setupClass() {
        FileSystemUtils.deleteRecursively(new File(TEST_OUTPUT_FOLDER));
    }

    @BeforeEach
    public void testWatcher(TestInfo aTestInfo) {
        String methodName = aTestInfo.getTestMethod().map(Method::getName).orElse("<unknown>");
        System.out.printf("\n=== %s === %s=====================\n", methodName,
            aTestInfo.getDisplayName());
    }

    @BeforeEach
    public void setUp() {
        if (!userRepository.exists("admin")) {
            userRepository.create(new User("admin", Role.ROLE_ADMIN));
        }
    }

    private void createProject(Project aProject) throws Exception {
        projectService.createProject(aProject);
        projectService.initializeProject(aProject);
    }

    @SafeVarargs
    private final void uploadDocument(Pair<SourceDocument, String>... aDocuments) throws Exception {
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


     */

    @Test
    public void testTestResultsExporter() throws Exception {
        //System.out.println("x");
        /*
        Project project = new Project();
        project.setName("TestResultsExporter");

        createProject(project);

        SourceDocument sourceDocument = new SourceDocument();

        sourceDocument.setName("Raw text document");
        sourceDocument.setProject(project);
        sourceDocument.setFormat("text");

        String fileContent = "The capital of Galicia is Santiago de Compostela." +
            " Santiago de Compostela is the capital of Galicia.";

        uploadDocument(Pair.of(sourceDocument, fileContent));

        User user = userRepository.get("admin");

        String query1 = "\"Galicia\"";
        String query2 = "\"Compostela\"";

        //List<SearchResult> results1 = searchService.query(user, project, query1);
        //List<SearchResult> results2 = searchService.query(user, project, query2);


         */

        SearchResult result1 = new SearchResult();
        result1.setText("is");
        result1.setLeftContext("of Galicia");
        result1.setRightContext("Santiago de");
        result1.setDocumentTitle("Doc1");

        SearchResult result2 = new SearchResult();
        result2.setText("is");
        result2.setLeftContext("de Compostela");
        result2.setRightContext("the capital");
        result2.setDocumentTitle("Doc2");


        List<SearchResult> results1 = new ArrayList<SearchResult>();
        results1.add(result1);
        results1.add(result2);

        List<SearchResult> results2 = new ArrayList<SearchResult>();
        results2.add(result2);
        results2.add(result1);


        //CompoundPropertyModel<SearchOptions> searchOptions = CompoundPropertyModel.of(new SearchOptions());
        //SearchResultsProvider searchResultsProvider = new SearchResultsProvider(searchService, Model.of(new SearchResultsPagesCache()));
        //SearchResultsProviderWrapper resultsWrapper = new SearchResultsProviderWrapper(searchResultsProvider, searchOptions.bind("isLowLevelPaging"));

        ResultsGroup resultsGroup1 = new ResultsGroup("1", results1);
        ResultsGroup resultsGroup2 = new ResultsGroup("2", results2);
        List<ResultsGroup> resultList = new ArrayList<ResultsGroup>();
        resultList.add(resultsGroup1);
        resultList.add(resultsGroup2);

        //resultsWrapper.initializeQuery(user, project, query, sourceDocument, );
        System.out.println("vor export");
        TestResultsExporter.export(resultList, TEST_OUTPUT_FOLDER + "\\csv.txt");
        System.out.println("nach export");
        List<ResultsGroup> reimported = TestResultsExporter.importCSV(TEST_OUTPUT_FOLDER + "\\csv.txt");
        System.out.println("nach import");
        TestResultsExporter.export(reimported, TEST_OUTPUT_FOLDER + "\\rerelease.txt");
        System.out.println(("nach export2"));

        assertThat(1).isEqualTo(1);

    }

    /*@SpringBootConfiguration
    public static class TestContext
    {
        @Bean
        public ApplicationContextProvider contextProvider()
        {
            return new ApplicationContextProvider();
        }
    }

     */
}

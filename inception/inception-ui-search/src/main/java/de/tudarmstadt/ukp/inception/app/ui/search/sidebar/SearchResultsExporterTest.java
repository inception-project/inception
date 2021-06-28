package de.tudarmstadt.ukp.inception.app.ui.search.sidebar;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.search.ResultsGroup;
import de.tudarmstadt.ukp.inception.search.SearchResult;

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
public class SearchResultsExporterTest
{

    static final String TEST_OUTPUT_FOLDER = "D:\\Falko\\Documents\\UKP";

    private final Logger log = LoggerFactory.getLogger(getClass());
    /*
     * private @Autowired UserDao userRepository; private @Autowired ProjectService projectService;
     * private @Autowired DocumentService documentService; private @Autowired SearchService
     * searchService;
     * 
     * @BeforeAll public static void setupClass() { FileSystemUtils.deleteRecursively(new
     * File(TEST_OUTPUT_FOLDER)); }
     * 
     * @BeforeEach public void testWatcher(TestInfo aTestInfo) { String methodName =
     * aTestInfo.getTestMethod().map(Method::getName).orElse("<unknown>");
     * System.out.printf("\n=== %s === %s=====================\n", methodName,
     * aTestInfo.getDisplayName()); }
     * 
     * @BeforeEach public void setUp() { if (!userRepository.exists("admin")) {
     * userRepository.create(new User("admin", Role.ROLE_ADMIN)); } }
     * 
     * private void createProject(Project aProject) throws Exception {
     * projectService.createProject(aProject); projectService.initializeProject(aProject); }
     * 
     * @SafeVarargs private final void uploadDocument(Pair<SourceDocument, String>... aDocuments)
     * throws Exception { Project project = null; try (CasStorageSession casStorageSession =
     * CasStorageSession.open()) { for (Pair<SourceDocument, String> doc : aDocuments) {
     * log.info("Uploading document via documentService.uploadSourceDocument: {}", doc); project =
     * doc.getLeft().getProject();
     * 
     * try (InputStream fileStream = new ByteArrayInputStream( doc.getRight().getBytes(UTF_8))) {
     * documentService.uploadSourceDocument(fileStream, doc.getLeft()); } } }
     * 
     * // Avoid the compiler complaining about project not being an effectively final variable
     * log.info("Waiting for uploaded documents to be indexed..."); Project p = project;
     * await("Waiting for indexing process to complete").atMost(60, SECONDS) .pollInterval(5,
     * SECONDS) .until(() -> searchService.isIndexValid(p) && !searchService.isIndexInProgress(p));
     * log.info("Indexing complete!"); }
     * 
     * 
     */

    @Test
    public void testSearchResultsExporter() throws Exception
    {
        // System.out.println("x");
        /*
         * Project project = new Project(); project.setName("TestResultsExporter");
         * 
         * createProject(project);
         * 
         * SourceDocument sourceDocument = new SourceDocument();
         * 
         * sourceDocument.setName("Raw text document"); sourceDocument.setProject(project);
         * sourceDocument.setFormat("text");
         * 
         * String fileContent = "The capital of Galicia is Santiago de Compostela." +
         * " Santiago de Compostela is the capital of Galicia.";
         * 
         * uploadDocument(Pair.of(sourceDocument, fileContent));
         * 
         * User user = userRepository.get("admin");
         * 
         * String query1 = "\"Galicia\""; String query2 = "\"Compostela\"";
         * 
         * //List<SearchResult> results1 = searchService.query(user, project, query1);
         * //List<SearchResult> results2 = searchService.query(user, project, query2);
         * 
         * 
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

        // CompoundPropertyModel<SearchOptions> searchOptions = CompoundPropertyModel.of(new
        // SearchOptions());
        // SearchResultsProvider searchResultsProvider = new SearchResultsProvider(searchService,
        // Model.of(new SearchResultsPagesCache()));
        // SearchResultsProviderWrapper resultsWrapper = new
        // SearchResultsProviderWrapper(searchResultsProvider,
        // searchOptions.bind("isLowLevelPaging"));

        ResultsGroup resultsGroup1 = new ResultsGroup("1", results1);
        ResultsGroup resultsGroup2 = new ResultsGroup("2", results2);
        List<ResultsGroup> resultList = new ArrayList<ResultsGroup>();
        resultList.add(resultsGroup1);
        resultList.add(resultsGroup2);

        SearchResultsProviderWrapper mockedWrapper = mock(SearchResultsProviderWrapper.class);
        // when(mockedWrapper.getAllResults()).thenReturn(resultList);
        when(mockedWrapper.iterator(0L, mockedWrapper.size()))
                .thenReturn(resultList.listIterator());

        // resultsWrapper.initializeQuery(user, project, query, sourceDocument, );
        // System.out.println("vor export");
        try(OutputStream os = Files.newOutputStream(Paths.get(TEST_OUTPUT_FOLDER + "\\csv.txt"))) {
            //Do something with is

            //SearchResultsExporter.export(resultList, os);
        }
        // SearchResultsExporter.export(mockedWrapper, TEST_OUTPUT_FOLDER + "\\csv2.txt");
        // System.out.println("nach export");
        List<ResultsGroup> reimported = SearchResultsExporter
                .importCSV(TEST_OUTPUT_FOLDER + "\\csv.txt");
        // System.out.println("nach import");
        try(OutputStream os = Files.newOutputStream(Paths.get(TEST_OUTPUT_FOLDER + "\\rerelease.txt"))) {
            //Do something with is

            //SearchResultsExporter.export(resultList, os);
        }
        //SearchResultsExporter.export(reimported, TEST_OUTPUT_FOLDER + "\\rerelease.txt");
        // System.out.println(("nach export2"));

        assertEquals(reimported.size(), resultList.size());
        for (int i = 0; i < reimported.size(); i++) {
            for (int j = 0; j < reimported.get(i).getResults().size(); j++) {
                assertEquals(reimported.get(i).getResults().get(j).getText(),
                        resultList.get(i).getResults().get(j).getText());
                assertEquals(reimported.get(i).getResults().get(j).getLeftContext(),
                        resultList.get(i).getResults().get(j).getLeftContext());
                assertEquals(reimported.get(i).getResults().get(j).getRightContext(),
                        resultList.get(i).getResults().get(j).getRightContext());
                assertEquals(reimported.get(i).getResults().get(j).getDocumentTitle(),
                        resultList.get(i).getResults().get(j).getDocumentTitle());
            }

        }
        // assertEquals(reimported.get(i), resultList.get(i))

    }

    /*
     * @SpringBootConfiguration public static class TestContext {
     * 
     * @Bean public ApplicationContextProvider contextProvider() { return new
     * ApplicationContextProvider(); } }
     * 
     */
}

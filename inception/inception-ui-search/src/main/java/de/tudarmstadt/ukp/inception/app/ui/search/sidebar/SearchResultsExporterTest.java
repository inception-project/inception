package de.tudarmstadt.ukp.inception.app.ui.search.sidebar;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
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

public class SearchResultsExporterTest
{
    //Customize this filepath so you can have a look at the resulting document
    static final String TEST_OUTPUT_FOLDER = "D:\\Falko\\Documents\\UKP";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Test
    public void testSearchResultsExporter() throws Exception
    {

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

        ResultsGroup resultsGroup1 = new ResultsGroup("1", results1);
        ResultsGroup resultsGroup2 = new ResultsGroup("2", results2);
        List<ResultsGroup> resultList = new ArrayList<ResultsGroup>();
        resultList.add(resultsGroup1);
        resultList.add(resultsGroup2);

        try {
            InputStream stream = SearchResultsExporter.generateCsv(resultList);
            OutputStream os = Files.newOutputStream(Paths.get(TEST_OUTPUT_FOLDER + "\\csv.txt"));
            stream.transferTo(os);
            os.flush();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        List<ResultsGroup> reimported = SearchResultsExporter
                .importCSV(TEST_OUTPUT_FOLDER + "\\csv.txt");

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
    }

}

package de.tudarmstadt.ukp.inception.app.ui.search.sidebar;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import de.tudarmstadt.ukp.inception.search.ResultsGroup;
import de.tudarmstadt.ukp.inception.search.SearchResult;

public class SearchResultsExporter
{

    public static InputStream generateCsv(List<ResultsGroup> aSearchResults) throws IOException
    {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(buf, "UTF-8"),
                CSVFormat.RFC4180)) {
            toCSV(aSearchResults, printer);
        }

        return new ByteArrayInputStream(buf.toByteArray());
    }

    public static void toCSV(List<ResultsGroup> aSearchResults, CSVPrinter aOut) throws IOException
    {
        aOut.printRecord("text", "context left", "context right", "document name");
        for (int i = 0; i < aSearchResults.size(); i++) {
            for (int j = 0; j < aSearchResults.get(i).getResults().size(); j++) {
                String text = aSearchResults.get(i).getResults().get(j).getText();
                String leftContext = aSearchResults.get(i).getResults().get(j).getLeftContext();
                String rightContext = aSearchResults.get(i).getResults().get(j).getRightContext();
                String documentName = aSearchResults.get(i).getResults().get(j).getDocumentTitle();
                aOut.printRecord(text, leftContext, rightContext, documentName);
            }
            // blank line after each ResultsGroup
            aOut.println();
        }
    }

    //This method only exists for a better testing of the export method
    public static List<ResultsGroup> importCSV(String aDataPath)
    {
        List<ResultsGroup> list = new ArrayList<ResultsGroup>();
        try (Reader reader = Files.newBufferedReader(Paths.get(aDataPath))) {
            Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(reader);

            int i = 0;
            List<SearchResult> inCurrentGroup = new ArrayList<SearchResult>();
            for (CSVRecord record : records) {
                // skip header
                if (i != 0) {
                    // blank line indicates new group
                    if (record.size() < 3) {
                        list.add(new ResultsGroup(String.valueOf(i), inCurrentGroup));
                        inCurrentGroup = new ArrayList<SearchResult>();
                    }
                    else {
                        SearchResult currentSearchResult = new SearchResult();
                        currentSearchResult.setText(record.get(0));
                        currentSearchResult.setLeftContext(record.get(1));
                        currentSearchResult.setRightContext(record.get(2));
                        currentSearchResult.setDocumentTitle(record.get(3));
                        inCurrentGroup.add(currentSearchResult);
                    }
                }
                i = i + 1;
            }

        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        return list;
    }
}

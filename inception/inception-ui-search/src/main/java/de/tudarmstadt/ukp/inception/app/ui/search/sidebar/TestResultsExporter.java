package de.tudarmstadt.ukp.inception.app.ui.search.sidebar;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.inception.search.SearchResult;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import de.tudarmstadt.ukp.inception.search.ResultsGroup;

//want: text, context left, context right, document name
public class TestResultsExporter {

    public static void export(List<ResultsGroup> searchResults, String filePath) {
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(filePath), CSVFormat.EXCEL)) {
            printer.printRecord("document name", "left context", "text", "right context");
            for (int i = 0; i < searchResults.size(); i++) {
                //System.out.println(i);
                //System.out.println(searchResults.get(i).getResults().size());
                for (int j = 0; j < searchResults.get(i).getResults().size(); j++) {
                    String text = searchResults.get(i).getResults().get(j).getText();
                    String leftContext = searchResults.get(i).getResults().get(j).getLeftContext();
                    String rightContext = searchResults.get(i).getResults().get(j).getRightContext();
                    String documentName = searchResults.get(i).getResults().get(j).getDocumentTitle();
                    printer.printRecord(documentName, leftContext, text, rightContext);
                    //printer.printRecords(searchResults);

                }
                printer.println();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static List<ResultsGroup> importCSV(String filePath) {
        List<ResultsGroup> list = new ArrayList<ResultsGroup>();
        try {
            Reader reader = Files.newBufferedReader(Paths.get(filePath));
            Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(reader);

            int i = 0;
            List<SearchResult> inCurrentGroup = new ArrayList<SearchResult>();
            for (CSVRecord record : records) {
                //System.out.println(i);
                //skip header
                if (i != 0) {
                    //blank line indicates new group
                    if (record.size() < 3) {
                        list.add(new ResultsGroup(String.valueOf(i), inCurrentGroup));
                        inCurrentGroup = new ArrayList<SearchResult>();
                    }
                    else {
                        System.out.println(i);
                        SearchResult currentSearchResult = new SearchResult();
                        currentSearchResult.setDocumentTitle(record.get(0));
                        currentSearchResult.setLeftContext(record.get(1));
                        currentSearchResult.setText(record.get(2));
                        currentSearchResult.setRightContext(record.get(3));
                        inCurrentGroup.add(currentSearchResult);

                    }
                }
                i = i + 1;
            }

            // close the reader
            reader.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return list;
    }
}




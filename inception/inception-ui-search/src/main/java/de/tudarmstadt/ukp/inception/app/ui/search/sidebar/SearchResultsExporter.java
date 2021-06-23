package de.tudarmstadt.ukp.inception.app.ui.search.sidebar;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import de.tudarmstadt.ukp.inception.search.ResultsGroup;
import de.tudarmstadt.ukp.inception.search.SearchResult;

//want: text, context left, context right, document name
public class SearchResultsExporter
{
    /*
     * public static void export(SearchResultsProviderWrapper aWrapper, String aFilePath) throws
     * FileNotFoundException { List columns = new ArrayList<LambdaColumn<ResultsGroup, Object>>();
     * columns.add(new LambdaColumn(new Model<String>("text"), results::getText)); columns.add(new
     * LambdaColumn(new Model<String>("context left"), "getResults().getLeftContext()"));
     * columns.add(new LambdaColumn(new Model<String>("context right"),
     * "getResults().getRightContext()")); columns.add(new LambdaColumn(new
     * Model<String>("document name"), "getResults().getDocumentTitle()")); // columns.add(new
     * PropertyColumn())
     * 
     * // OutputStream destination = new FileOutputStream(aFilePath);
     * 
     * System.out.println( aWrapper.iterator(0,
     * aWrapper.size()).next().getResults().get(0).getLeftContext()); //
     * System.out.println(aFilePath);
     * 
     * CSVDataExporter exporter = new CSVDataExporter();
     * 
     * try { exporter.exportData(aWrapper, columns, new FileOutputStream(aFilePath)); } catch
     * (IOException e) { e.printStackTrace(); }
     * 
     * }
     */

    /*
    public static void export(SearchResultsProviderWrapper aWrapper, String aFilePath)
    {
        export(aWrapper.getAllResults(), aFilePath);
    }

     */

    public static void export(List<ResultsGroup> aSearchResults, String aFilePath)
    {
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(aFilePath), CSVFormat.EXCEL)) {
            printer.printRecord("text", "context left", "context right", "document name");
            for (int i = 0; i < aSearchResults.size(); i++) {
                // System.out.println(i);
                // System.out.println(searchResults.get(i).getResults().size());
                for (int j = 0; j < aSearchResults.get(i).getResults().size(); j++) {
                    String text = aSearchResults.get(i).getResults().get(j).getText();
                    String leftContext = aSearchResults.get(i).getResults().get(j).getLeftContext();
                    String rightContext = aSearchResults.get(i).getResults().get(j)
                            .getRightContext();
                    String documentName = aSearchResults.get(i).getResults().get(j)
                            .getDocumentTitle();
                    printer.printRecord(text, leftContext, rightContext, documentName);
                    // printer.printRecords(searchResults);

                }
                // blank line after each ResultsGroup
                printer.println();
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static List<ResultsGroup> importCSV(String aFilePath)
    {
        List<ResultsGroup> list = new ArrayList<ResultsGroup>();
        try {
            Reader reader = Files.newBufferedReader(Paths.get(aFilePath));
            Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(reader);

            int i = 0;
            List<SearchResult> inCurrentGroup = new ArrayList<SearchResult>();
            for (CSVRecord record : records) {
                // System.out.println(i);
                // skip header
                if (i != 0) {
                    // blank line indicates new group
                    if (record.size() < 3) {
                        list.add(new ResultsGroup(String.valueOf(i), inCurrentGroup));
                        inCurrentGroup = new ArrayList<SearchResult>();
                    }
                    else {
                        // System.out.println(i);
                        SearchResult currentSearchResult = new SearchResult();
                        currentSearchResult.setDocumentTitle(record.get(3));
                        currentSearchResult.setLeftContext(record.get(1));
                        currentSearchResult.setText(record.get(0));
                        currentSearchResult.setRightContext(record.get(2));
                        inCurrentGroup.add(currentSearchResult);

                    }
                }
                i = i + 1;
            }

            // close the reader
            reader.close();

        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        return list;
    }
}

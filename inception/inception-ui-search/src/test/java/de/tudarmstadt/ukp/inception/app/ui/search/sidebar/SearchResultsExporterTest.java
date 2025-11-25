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

package de.tudarmstadt.ukp.inception.app.ui.search.sidebar;

import static org.apache.commons.csv.CSVFormat.EXCEL;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.tudarmstadt.ukp.inception.search.ResultsGroup;
import de.tudarmstadt.ukp.inception.search.SearchResult;

class SearchResultsExporterTest
{
    @Test
    void testSearchResultsExporter(@TempDir Path tempDir) throws Exception
    {
        var csvPath = tempDir.resolve("csv.txt");

        var result1 = new SearchResult();
        result1.setText("is");
        result1.setLeftContext("of Galicia");
        result1.setRightContext("Santiago de");
        result1.setDocumentTitle("Doc1");
        result1.setOffsetStart(5);
        result1.setOffsetEnd(7);

        var result2 = new SearchResult();
        result2.setText("is");
        result2.setLeftContext("de Compostela");
        result2.setRightContext("the capital");
        result2.setDocumentTitle("Doc2");
        result2.setOffsetStart(2);
        result2.setOffsetEnd(3);

        var results1 = new ArrayList<SearchResult>();
        results1.add(result1);
        results1.add(result2);

        var results2 = new ArrayList<SearchResult>();
        results2.add(result2);
        results2.add(result1);

        var resultsGroup1 = new ResultsGroup("1", results1);
        var resultsGroup2 = new ResultsGroup("2", results2);
        var resultList = new ArrayList<ResultsGroup>();
        resultList.add(resultsGroup1);
        resultList.add(resultsGroup2);

        var exporter = new SearchResultsExporter();

        try (var stream = exporter.generateCsv(resultList);
                var os = Files.newOutputStream(csvPath);) {
            stream.transferTo(os);
        }

        var reimported = importCSV(csvPath);

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
                assertEquals(reimported.get(i).getResults().get(j).getOffsetStart(),
                        resultList.get(i).getResults().get(j).getOffsetStart());
                assertEquals(reimported.get(i).getResults().get(j).getOffsetEnd(),
                        resultList.get(i).getResults().get(j).getOffsetEnd());
            }
        }
    }

    static List<ResultsGroup> importCSV(Path aDataPath) throws IOException
    {
        var list = new ArrayList<ResultsGroup>();
        try (var reader = Files.newBufferedReader(aDataPath)) {
            var records = EXCEL.parse(reader);

            var i = 0;
            var inCurrentGroup = new ArrayList<SearchResult>();
            for (var record : records) {
                // skip header
                if (i != 0) {
                    // blank line indicates new group
                    if (record.size() < 3) {
                        list.add(new ResultsGroup(String.valueOf(i), inCurrentGroup));
                        inCurrentGroup = new ArrayList<SearchResult>();
                    }
                    else {
                        var currentSearchResult = new SearchResult();
                        currentSearchResult.setDocumentTitle(record.get(0));
                        currentSearchResult.setOffsetStart(Integer.parseInt(record.get(1)));
                        currentSearchResult.setOffsetEnd(Integer.parseInt(record.get(2)));
                        currentSearchResult.setLeftContext(record.get(3));
                        currentSearchResult.setText(record.get(4));
                        currentSearchResult.setRightContext(record.get(5));
                        inCurrentGroup.add(currentSearchResult);
                    }
                }
                i = i + 1;
            }
        }
        return list;
    }
}

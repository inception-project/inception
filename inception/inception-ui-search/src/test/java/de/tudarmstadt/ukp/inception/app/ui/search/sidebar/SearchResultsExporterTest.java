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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.tudarmstadt.ukp.inception.search.ResultsGroup;
import de.tudarmstadt.ukp.inception.search.SearchResult;

public class SearchResultsExporterTest
{
    @Test
    public void testSearchResultsExporter(@TempDir Path tempDir) throws Exception
    {
        Path csvPath = tempDir.resolve("csv.txt");

        SearchResult result1 = new SearchResult();
        result1.setText("is");
        result1.setLeftContext("of Galicia");
        result1.setRightContext("Santiago de");
        result1.setDocumentTitle("Doc1");
        result1.setOffsetStart(5);
        result1.setOffsetEnd(7);

        SearchResult result2 = new SearchResult();
        result2.setText("is");
        result2.setLeftContext("de Compostela");
        result2.setRightContext("the capital");
        result2.setDocumentTitle("Doc2");
        result2.setOffsetStart(2);
        result2.setOffsetEnd(3);

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

        SearchResultsExporter exporter = new SearchResultsExporter();

        try (InputStream stream = exporter.generateCsv(resultList);
                OutputStream os = Files.newOutputStream(csvPath);) {
            stream.transferTo(os);
        }

        List<ResultsGroup> reimported = new ArrayList<ResultsGroup>();

        reimported = SearchResultsExporter.importCSV(csvPath);

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

}

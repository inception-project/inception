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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.csv.CSVFormat.EXCEL;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import org.apache.commons.csv.CSVPrinter;

import de.tudarmstadt.ukp.inception.search.ResultsGroup;

public class SearchResultsExporter
{
    public InputStream generateCsv(List<ResultsGroup> aSearchResults) throws IOException
    {
        var buf = new ByteArrayOutputStream();
        try (var printer = new CSVPrinter(new OutputStreamWriter(buf, UTF_8), EXCEL)) {
            toCSV(aSearchResults, printer);
        }

        return new ByteArrayInputStream(buf.toByteArray());
    }

    static void toCSV(List<ResultsGroup> aSearchResults, CSVPrinter aOut) throws IOException
    {
        aOut.printRecord("document name", "begin offset", "end offset", "context left", "text",
                "context right");
        for (var i = 0; i < aSearchResults.size(); i++) {
            for (var j = 0; j < aSearchResults.get(i).getResults().size(); j++) {
                var res = aSearchResults.get(i).getResults().get(j);
                aOut.printRecord(res.getDocumentTitle(), res.getOffsetStart(), res.getOffsetEnd(),
                        res.getLeftContext(), res.getText(), res.getRightContext());
            }
            // blank line after each ResultsGroup
            aOut.println();
        }
    }
}

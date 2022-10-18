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

import static de.tudarmstadt.ukp.inception.search.Metrics.DOC_COUNT;
import static org.apache.commons.csv.CSVFormat.EXCEL;
import static org.apache.commons.csv.CSVFormat.MONGODB_TSV;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import de.tudarmstadt.ukp.inception.app.ui.search.Formats;
import de.tudarmstadt.ukp.inception.search.ExecutionException;
import de.tudarmstadt.ukp.inception.search.LayerStatistics;
import de.tudarmstadt.ukp.inception.search.Metrics;

public class StatisticsExporter
{

    private static final String VIRTUAL_SENTENCE = Metrics.VIRTUAL_LAYER_SEGMENTATION + "."
            + Metrics.VIRTUAL_FEATURE_SENTENCE;

    public InputStream generateFile(List<LayerStatistics> aStatsList, Formats aFormat)
        throws IOException, ExecutionException
    {
        CSVFormat format = MONGODB_TSV;
        if (aFormat == Formats.CSV) {
            format = EXCEL;
        }
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(buf, "UTF-8"), format)) {
            toCSV(aStatsList, printer);
        }

        return new ByteArrayInputStream(buf.toByteArray());
    }

    public static void toCSV(List<LayerStatistics> aStatsList, CSVPrinter aOut)
        throws IOException, ExecutionException
    {
        aOut.printRecord("Number of Documents: " + aStatsList.get(0).getNoOfDocuments());

        List<String> perDocList = Metrics.uiList();
        perDocList.add(0, "layer name");
        perDocList.add(1, "feature name");
        perDocList.remove("Number of Documents");
        List<String> completeList = ListUtils.union(perDocList, Metrics.uiList().stream()
                .map(s -> s + " per Sentence").collect(Collectors.toList()));
        completeList.remove("Number of Documents per Sentence");
        aOut.printRecord(completeList);

        for (LayerStatistics ls : aStatsList) {
            List<Object> resultsList = new ArrayList<Object>();
            resultsList.add(ls.getFeature().getLayer().getUiName());
            resultsList.add(ls.getFeature().getUiName());

            // Results per document
            for (String metric : Metrics.uiList()) {
                if (DOC_COUNT.uiName.equals(metric)) {
                    continue;
                }

                resultsList.add(ls.getMetric(Metrics.uiToInternal(metric), false));
            }

            // Results per sentence
            for (String metric : Metrics.uiList()) {
                if (DOC_COUNT.uiName.equals(metric)) {
                    continue;
                }

                if (VIRTUAL_SENTENCE.equals(ls.getLayerFeatureName())) {
                    resultsList.add("n/a");
                }

                resultsList.add(ls.getMetric(Metrics.uiToInternal(metric), true));
            }

            aOut.printRecord(resultsList);
        }
    }
}

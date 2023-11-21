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
package de.tudarmstadt.ukp.inception.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum Metrics
{
    DOC_COUNT("n", "Number of Documents"), SUM("sum", "Sum"), MIN("min", "Minimum"),
    MAX("max", "Maximum"), MEAN("mean", "Mean"), MEDIAN("median", "Median"),
    STANDARD_DEVIATION("standarddeviation", "Standard Deviation");

    public static final String VIRTUAL_FEATURE_SENTENCE = "sentence";
    public static final String VIRTUAL_FEATURE_TOKEN = "token";
    public static final String VIRTUAL_LAYER_SEGMENTATION = "Segmentation";

    private static final Map<String, Metrics> MTAS_TO_INTERNAL = new HashMap<String, Metrics>();
    private static final Map<Metrics, String> INTERNAL_TO_MTAS = new HashMap<Metrics, String>();
    private static final Map<String, Metrics> UI_TO_INTERNAL = new HashMap<String, Metrics>();
    private static final Map<Metrics, String> INTERNAL_TO_UI = new HashMap<Metrics, String>();

    static {
        for (Metrics m : values()) {
            MTAS_TO_INTERNAL.put(m.mtasName, m);
            INTERNAL_TO_MTAS.put(m, m.mtasName);
            UI_TO_INTERNAL.put(m.uiName, m);
            INTERNAL_TO_UI.put(m, m.uiName);
        }
    }

    public final String mtasName;
    public final String uiName;

    private Metrics(String aMtasName, String aUiName)
    {
        mtasName = aMtasName;
        uiName = aUiName;
    }

    public static String internalToMtas(Metrics aMetric)
    {
        return INTERNAL_TO_MTAS.get(aMetric);
    }

    public static Metrics mtasToInternal(String aMetric)
    {
        return MTAS_TO_INTERNAL.get(aMetric);
    }

    public static List<String> mtasList()
    {
        List<String> sortedList = new ArrayList<String>(INTERNAL_TO_MTAS.values());
        Collections.sort(sortedList);
        return sortedList;
    }

    public static String mtasRegExp()
    {
        String regexp = "";
        for (Metrics metric : Metrics.values()) {
            regexp = regexp + "," + internalToMtas(metric);
        }
        return regexp.substring(1);
    }

    public static String internalToUi(Metrics aMetric)
    {
        return INTERNAL_TO_UI.get(aMetric);
    }

    public static Metrics uiToInternal(String aMetric)
    {
        return UI_TO_INTERNAL.get(aMetric);
    }

    public static List<String> uiList()
    {
        List<String> sortedList = new ArrayList<String>(INTERNAL_TO_UI.values());
        Collections.sort(sortedList);
        return sortedList;
    }

}

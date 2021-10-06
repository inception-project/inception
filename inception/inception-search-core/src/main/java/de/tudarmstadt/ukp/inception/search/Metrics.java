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
import java.util.List;

public enum Metrics
{
    DOC_COUNT, SUM, MIN, MAX, MEAN, MEDIAN, STANDARD_DEVIATION;

    public static String internalToMtas(Metrics aMetric)
    {
        switch (aMetric) {
            case DOC_COUNT:
                return "n";
        case SUM:
            return "sum";
        case MIN:
            return "min";
        case MAX:
            return "max";
        case MEAN:
            return "mean";
        case MEDIAN:
            return "median";
        default:
            return "standarddeviation";
        }
    }

    public static Metrics mtasToInternal(String aMetric) throws ExecutionException
    {
        switch (aMetric) {
            case "n":
                return DOC_COUNT;
        case "sum":
            return SUM;
        case "min":
            return MIN;
        case "max":
            return MAX;
        case "mean":
            return MEAN;
        case "median":
            return MEDIAN;
        case "standarddeviation":
            return STANDARD_DEVIATION;
        default:
            throw new ExecutionException(aMetric + " is not a supported metric");
        }
    }

    public static List<String> mtasList()
    {
        List<String> metricList = new ArrayList<String>();
        for (Metrics metric : Metrics.values()) {
            metricList.add(internalToMtas(metric));
        }
        return metricList;
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
        switch (aMetric) {
            case DOC_COUNT:
                return "Number of Documents";
        case SUM:
            return "Sum";
        case MIN:
            return "Minimum";
        case MAX:
            return "Maximum";
        case MEAN:
            return "Mean";
        case MEDIAN:
            return "Median";
        default:
            return "Standard Deviation";
        }
    }

    public static Metrics uiToInternal(String aMetric) throws ExecutionException
    {
        switch (aMetric) {
            case "Number of Documents":
                return DOC_COUNT;
        case "Sum":
            return SUM;
        case "Minimum":
            return MIN;
        case "Maximum":
            return MAX;
        case "Mean":
            return MEAN;
        case "Median":
            return MEDIAN;
        case "Standard Deviation":
            return STANDARD_DEVIATION;
        default:
            throw new ExecutionException(aMetric + " is not a supported metric");
        }
    }

    public static List<String> uiList()
    {
        List<String> metricList = new ArrayList<String>();
        for (Metrics metric : Metrics.values()) {
            metricList.add(internalToUi(metric));
        }
        return metricList;
    }

}

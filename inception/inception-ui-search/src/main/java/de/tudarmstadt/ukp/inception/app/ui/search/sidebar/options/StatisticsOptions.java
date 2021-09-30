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

package de.tudarmstadt.ukp.inception.app.ui.search.sidebar.options;

import java.util.Arrays;
import java.util.List;

import de.tudarmstadt.ukp.inception.search.ExecutionException;

public class StatisticsOptions
    extends Options
{

    private String statistic;
    private String granularity;
    private String format;

    private static final String PER_DOCUMENT = "per document";
    private static final String PER_SENTENCE = "per sentence";

    public static final List<String> GRANULARITY_LEVELS = Arrays
            .asList(new String[] { PER_DOCUMENT, PER_SENTENCE });

    private static final String TOTAL = "sum";
    private static final String MAX = "max";
    private static final String MIN = "min";
    private static final String MEAN = "mean";
    private static final String MEDIAN = "median";
    private static final String STANDARD_DEVIATION = "standarddeviation";

    public static final List<String> STATISTICS = Arrays
        .asList(new String[] { TOTAL, MAX, MIN, MEAN, MEDIAN, STANDARD_DEVIATION });

    private static final String TOTAL_UI = "Sum";
    private static final String MAX_UI = "Maximum";
    private static final String MIN_UI = "Minimum";
    private static final String MEAN_UI = "Mean";
    private static final String MEDIAN_UI = "Median";
    private static final String STANDARD_DEVIATION_UI = "Standard Deviation";

    public static final List<String> STATISTICS_UI = Arrays
        .asList(new String[] { TOTAL_UI, MAX_UI, MIN_UI, MEAN_UI, MEDIAN_UI, STANDARD_DEVIATION_UI });

    private static final String CSV = ".csv";
    private static final String TXT = ".txt";

    public static final List<String> FORMATS = Arrays.asList(new String[] { CSV, TXT });

    public StatisticsOptions()
    {
        statistic = null;
        granularity = null;
        format = null;
    }


    public void setStatistic(String aStatistic) throws ExecutionException
    {
        if (STATISTICS.contains(aStatistic)) {
            statistic = aStatistic;
        } else if (STATISTICS_UI.contains(aStatistic)) {
            statistic = uiToInternal(aStatistic);
        }
        else {
            throw new ExecutionException("The statistic " + aStatistic + " is not supported!");
        }
    }



    public void setGranularity(String aGranularity) throws ExecutionException
    {
        if (GRANULARITY_LEVELS.contains(aGranularity)) {
            granularity = aGranularity;
        }
        else {
            throw new ExecutionException("The granularity " + aGranularity + " is not supported!");
        }
    }

    public void setFormat(String aFormat) throws ExecutionException
    {
        if (FORMATS.contains(aFormat)) {
            format = aFormat;
        }
        else {
            throw new ExecutionException("The format " + aFormat + " is not supported!");
        }
    }

    public String getStatistic()
    {
        return statistic;
    }

    public String getFormat()
    {
        return format;
    }

    public String getGranularity()
    {
        return granularity;
    }

    public static String uiToInternal(String aUIStatistic) throws ExecutionException{
        switch (aUIStatistic) {
            case TOTAL_UI: return TOTAL;
            case MAX_UI: return MAX;
            case MIN_UI: return MIN;
            case MEAN_UI: return MEAN;
            case MEDIAN_UI: return MEDIAN;
            case STANDARD_DEVIATION_UI: return STANDARD_DEVIATION;
            default: throw new ExecutionException(aUIStatistic + "is not a supported UI name for a statistic");
        }
    }

    public static String internalToUI(String aInternalStatistic) throws ExecutionException{
        switch (aInternalStatistic) {
            case TOTAL: return TOTAL_UI;
            case MAX: return MAX_UI;
            case MIN: return MIN_UI;
            case MEAN: return MEAN_UI;
            case MEDIAN: return MEDIAN_UI;
            case STANDARD_DEVIATION: return STANDARD_DEVIATION_UI;
            //dont know how to handle method abuse without an exception...
            default: throw new ExecutionException(aInternalStatistic + "is not a supported internal name for a statistic");
        }
    }

}

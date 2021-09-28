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

    private static final String TOTAL = "n";
    private static final String MAX = "max";
    private static final String MIN = "min";
    private static final String MEAN = "mean";
    private static final String MEDIAN = "median";
    private static final String STANDARD_DEVIATION = "standarddeviation";

    public static final List<String> STATISTICS = Arrays
            .asList(new String[] { TOTAL, MAX, MIN, MEAN, MEDIAN, STANDARD_DEVIATION });

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

}

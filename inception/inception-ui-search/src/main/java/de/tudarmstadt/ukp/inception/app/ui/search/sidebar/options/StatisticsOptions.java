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

import java.io.Serializable;

import de.tudarmstadt.ukp.inception.app.ui.search.Formats;
import de.tudarmstadt.ukp.inception.search.ExecutionException;
import de.tudarmstadt.ukp.inception.search.Granularities;
import de.tudarmstadt.ukp.inception.search.LayerStatistics;
import de.tudarmstadt.ukp.inception.search.Metrics;

public class StatisticsOptions
    implements Serializable
{
    private static final long serialVersionUID = 1103399322884303842L;

    private Metrics statistic;
    private Granularities granularity;
    private Formats format;

    public StatisticsOptions()
    {
        statistic = null;
        granularity = null;
        format = null;
    }

    public void setStatistic(Metrics aStatistic)
    {
        statistic = aStatistic;
    }

    public void setGranularity(Granularities aGranularity)
    {
        granularity = aGranularity;
    }

    public void setFormat(Formats aFormat)
    {
        format = aFormat;
    }

    public Metrics getStatistic()
    {
        return statistic;
    }

    public Formats getFormat()
    {
        return format;
    }

    public Granularities getGranularity()
    {
        return granularity;
    }

    public static String buildPropertyExpression(Metrics aStatistic, Granularities aGranularity)
        throws ExecutionException
    {
        String granularity = "";
        String metric = "";
        if (aGranularity == Granularities.PER_DOCUMENT) {
            granularity = "";
        }
        if (aGranularity == Granularities.PER_SENTENCE) {
            granularity = "PerSentence";
        }
        metric = LayerStatistics.getPropertyExpression(aStatistic);

        return metric + granularity;
    }

}

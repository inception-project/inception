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

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;

public class LayerStatistics
{
    /** The stats to be calculated */
    private static final String DOC_COUNT = "n";
    private static final String TOTAL = "sum";
    private static final String MINIMUM = "min";
    private static final String MAXIMUM = "max";
    private static final String MEAN = "mean";
    private static final String MEDIAN = "median";
    private static final String STANDARD_DEVIATION = "standarddeviation";

    public static final String STATS = DOC_COUNT + "," + TOTAL + "," + MINIMUM + "," + MAXIMUM + ","
            + MEAN + "," + MEDIAN + "," + STANDARD_DEVIATION;
    public static final List<String> STATS_LIST = Arrays.asList(STATS.split(","));

    private final double total;
    private final double maximum;
    private final double minimum;
    private final double mean;
    private final double median;
    private final double standardDeviation;

    private final double totalPerSentence;
    private final double maximumPerSentence;
    private final double minimumPerSentence;
    private final double meanPerSentence;
    private final double medianPerSentence;
    private final double standardDeviationPerSentence;

    private final double noOfDocuments;

    private String query;
    private AnnotationFeature feature;

    public LayerStatistics(double aTotal, double aMaximum, double aMinimum, double aMean,
            double aMedian, double aStandardDeviation, double aTotalPerSentence,
            double aMaximumPerSentence, double aMinimumPerSentence, double aMeanPerSentence,
            double aMedianPerSentence, double aStandardDeviationPerSentence, double aNoOfDocuments)
    {
        total = aTotal;
        maximum = aMaximum;
        minimum = aMinimum;
        mean = aMean;
        median = aMedian;
        standardDeviation = aStandardDeviation;

        totalPerSentence = aTotalPerSentence;
        maximumPerSentence = aMaximumPerSentence;
        minimumPerSentence = aMinimumPerSentence;
        meanPerSentence = aMeanPerSentence;
        medianPerSentence = aMedianPerSentence;
        standardDeviationPerSentence = aStandardDeviationPerSentence;

        noOfDocuments = aNoOfDocuments;

        query = null;
    }

    public double getMetric(String aMetric, boolean aPerSentence) throws ExecutionException
    {

        if (!STATS_LIST.contains(aMetric)) {
            throw new ExecutionException("Metric " + aMetric + " is not supported");
        }

        switch (aMetric) {
        case DOC_COUNT:
            if (!aPerSentence) {
                return getNoOfDocuments();
            }
            else {
                return getNoOfDocuments();
            }
        case TOTAL:
            if (!aPerSentence) {
                return getTotal();
            }
            else {
                return getTotalPerSentence();
            }
        case MINIMUM:
            if (!aPerSentence) {
                return getMinimum();
            }
            else {
                return getMinimumPerSentence();
            }
        case MAXIMUM:
            if (!aPerSentence) {
                return getMaximum();
            }
            else {
                return getMaximumPerSentence();
            }
        case MEAN:
            if (!aPerSentence) {
                return getMean();
            }
            else {
                return getMeanPerSentence();
            }
        case MEDIAN:
            if (!aPerSentence) {
                return getMedian();
            }
            else {
                return getMedianPerSentence();
            }
        case STANDARD_DEVIATION:
            if (!aPerSentence) {
                return getStandardDeviation();
            }
            else {
                return getStandardDeviationPerSentence();
            }
        }
        //formal return statement. is never reached
        return -1;
    }

    public double getTotal()
    {
        return total;
    }

    public double getMaximum()
    {
        return maximum;
    }

    public double getMinimum()
    {
        return minimum;
    }

    public double getMean()
    {
        return mean;
    }

    public double getMedian()
    {
        return median;
    }

    public double getStandardDeviation()
    {
        return standardDeviation;
    }

    public double getTotalPerSentence()
    {
        return totalPerSentence;
    }

    public double getMaximumPerSentence()
    {
        return maximumPerSentence;
    }

    public double getMinimumPerSentence()
    {
        return minimumPerSentence;
    }

    public double getMeanPerSentence()
    {
        return meanPerSentence;
    }

    public double getMedianPerSentence()
    {
        return medianPerSentence;
    }

    public double getStandardDeviationPerSentence()
    {
        return standardDeviationPerSentence;
    }

    public double getNoOfDocuments()
    {
        return noOfDocuments;
    }

    public String getQuery()
    {
        return query;
    }

    public AnnotationFeature getFeature()
    {
        return feature;
    }

    public void setQuery(String aQuery)
    {
        query = aQuery;
    }

    public void setFeature(AnnotationFeature aFeature)
    {
        feature = aFeature;
    }

    @Override
    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) {
            return true;
        }

        if (!(o instanceof LayerStatistics)) {
            return false;
        }

        LayerStatistics ls = (LayerStatistics) o;
        return EqualsBuilder.reflectionEquals(this, ls);
    }
}

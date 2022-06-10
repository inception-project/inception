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

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;

public class LayerStatistics
    implements Serializable
{
    private static final long serialVersionUID = -7451764100585199249L;

    public static final String STATS = Metrics.mtasRegExp();
    public static final List<String> STATS_LIST = Arrays.asList(STATS.split(","));

    private final double sum;
    private final double maximum;
    private final double minimum;
    private final double mean;
    private final double median;
    private final double standardDeviation;

    private final double sumPerSentence;
    private final double maximumPerSentence;
    private final double minimumPerSentence;
    private final double meanPerSentence;
    private final double medianPerSentence;
    private final double standardDeviationPerSentence;

    private final double noOfDocuments;

    private String query;
    private AnnotationFeature feature;

    public LayerStatistics(double aSum, double aMaximum, double aMinimum, double aMean,
            double aMedian, double aStandardDeviation, double aSumPerSentence,
            double aMaximumPerSentence, double aMinimumPerSentence, double aMeanPerSentence,
            double aMedianPerSentence, double aStandardDeviationPerSentence, double aNoOfDocuments)
    {
        sum = aSum;
        maximum = aMaximum;
        minimum = aMinimum;
        mean = aMean;
        median = aMedian;
        standardDeviation = aStandardDeviation;

        sumPerSentence = aSumPerSentence;
        maximumPerSentence = aMaximumPerSentence;
        minimumPerSentence = aMinimumPerSentence;
        meanPerSentence = aMeanPerSentence;
        medianPerSentence = aMedianPerSentence;
        standardDeviationPerSentence = aStandardDeviationPerSentence;

        noOfDocuments = aNoOfDocuments;

        query = null;
    }

    public String getLayerFeatureName()
    {
        return feature.getLayer().getUiName() + "." + feature.getUiName();
    }

    public double getMetric(Metrics aMetric, boolean aPerSentence) throws ExecutionException
    {
        switch (aMetric) {
        case DOC_COUNT:
            if (!aPerSentence) {
                return getNoOfDocuments();
            }
            else {
                return getNoOfDocuments();
            }
        case SUM:
            if (!aPerSentence) {
                return getSum();
            }
            else {
                return getSumPerSentence();
            }
        case MIN:
            if (!aPerSentence) {
                return getMinimum();
            }
            else {
                return getMinimumPerSentence();
            }
        case MAX:
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
        default:
            throw new ExecutionException("This metric is not supported!");
        }
    }

    public static String getPropertyExpression(Metrics aMetric) throws ExecutionException
    {
        switch (aMetric) {
        case DOC_COUNT:
            return "getNoOfDocuments";
        case SUM:
            return "getSum";
        case MIN:
            return "getMinimum";
        case MAX:
            return "getMaximum";
        case MEAN:
            return "getMean";
        case MEDIAN:
            return "getMedian";
        case STANDARD_DEVIATION:
            return "getStandardDeviation";
        default:
            throw new ExecutionException("This metric is not supported!");
        }
    }

    public double getSum()
    {
        return sum;
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

    public double getSumPerSentence()
    {
        return sumPerSentence;
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

    public double getNoOfDocumentsPerSentence()
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

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("query", query)
                .append("feature", feature).append("noOfDocuments", noOfDocuments)
                .append("sum", sum).append("maximum", maximum).append("minimum", minimum)
                .append("mean", mean).append("median", median)
                .append("standardDeviation", standardDeviation)
                .append("sumPerSentence", sumPerSentence)
                .append("maximumPerSentence", maximumPerSentence)
                .append("minimumPerSentence", minimumPerSentence)
                .append("meanPerSentence", meanPerSentence)
                .append("medianPerSentence", medianPerSentence)
                .append("standardDeviationPerSentence", standardDeviationPerSentence).toString();
    }
}

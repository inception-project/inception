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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class LayerStatistics
{
    /** The stats to be calculated */
    public static final String STATS = "n,sum,min,max,mean,median,standarddeviation";

    private final long total;
    private final long maximum;
    private final long minimum;
    private final double mean;
    private final double median;
    private final double standardDeviation;

    private final double maximumPerSentence;
    private final double minimumPerSentence;
    private final double meanPerSentence;
    private final double medianPerSentence;
    private final double standardDeviationPerSentence;

    private final long noOfDocuments;

    public LayerStatistics(long aTotal, long aMaximum, long aMinimum, double aMean, double aMedian,
            double aStandardDeviation, double aMaximumPerSentence, double aMinimumPerSentence,
            double aMeanPerSentence, double aMedianPerSentence,
            double aStandardDeviationPerSentence, long aNoOfDocuments)
    {
        total = aTotal;
        maximum = aMaximum;
        minimum = aMinimum;
        mean = aMean;
        median = aMedian;
        standardDeviation = aStandardDeviation;

        maximumPerSentence = aMaximumPerSentence;
        minimumPerSentence = aMinimumPerSentence;
        meanPerSentence = aMeanPerSentence;
        medianPerSentence = aMedianPerSentence;
        standardDeviationPerSentence = aStandardDeviationPerSentence;

        noOfDocuments = aNoOfDocuments;
    }

    public long getTotal()
    {
        return total;
    }

    public long getMaximum()
    {
        return maximum;
    }

    public long getMinimum()
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

    public long getNoOfDocuments()
    {
        return noOfDocuments;
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

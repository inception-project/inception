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

public class LayerStatistics
{
    /** The stats to be calculated */
    public static final String STATS = "n,sum,min,max,mean,median,standarddeviation";

    private long total;
    private long maximum;
    private long minimum;
    private double mean;
    private double median;
    private double standardDeviation;

    private double maximumPerSentence;
    private double minimumPerSentence;
    private double meanPerSentence;
    private double medianPerSentence;
    private double standardDeviationPerSentence;

    private long noOfDocuments;

    public LayerStatistics()
    {
    }

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

    public boolean isLayerTrivial()
    {
        if (maximum == 0L) {
            return true;
        }
        return false;
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
    public boolean equals(Object o)
    {
        if (o == this) {
            return true;
        }

        if (!(o instanceof LayerStatistics)) {
            return false;
        }

        LayerStatistics ls = (LayerStatistics) o;

        return ((ls.getTotal() == getTotal()) && (ls.getMaximum() == getMaximum())
                && (ls.getMinimum() == getMinimum()) && (ls.getMedian() == getMedian())
                && (ls.getMean() == getMean())
                && (ls.getStandardDeviation() == getStandardDeviation())
                && (ls.getNoOfDocuments() == getNoOfDocuments())
                && (ls.getMaximumPerSentence() == getMaximumPerSentence())
                && (ls.getMinimumPerSentence() == getMinimumPerSentence())
                && (ls.getMedianPerSentence() == getMedianPerSentence())
                && (ls.getMeanPerSentence() == getMeanPerSentence())
                && (ls.getStandardDeviationPerSentence() == getStandardDeviationPerSentence()));
    }
}

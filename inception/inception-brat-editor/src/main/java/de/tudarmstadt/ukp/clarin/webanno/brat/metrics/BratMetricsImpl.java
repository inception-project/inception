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
package de.tudarmstadt.ukp.clarin.webanno.brat.metrics;

import static org.springframework.jmx.support.MetricType.COUNTER;

import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.RenderType;

@ManagedResource
@Component
public class BratMetricsImpl
    implements BratMetrics
{
    private long fullRenderCount = 0;
    private long fullRenderedSize = 0;

    private long diffRenderAttempts = 0;
    private long diffRenderCount = 0;
    private long diffRenderedSize = 0;

    private long skipRenderCount = 0;

    private long savedRenderedSize = 0;

    private long sentRenderedSize = 0;

    private long renderTime = 0;
    private long maxRenderTime = 0;
    private long lastRenderTime = 0;

    @ManagedMetric(metricType = COUNTER)
    public long getFullRenderCount()
    {
        return fullRenderCount;
    }

    @ManagedMetric(metricType = COUNTER, unit = "chars")
    public long getFullRenderedSize()
    {
        return fullRenderedSize;
    }

    @ManagedMetric(metricType = COUNTER)
    public long getDiffRenderAttempts()
    {
        return diffRenderAttempts;
    }

    @ManagedMetric(metricType = COUNTER)
    public long getDiffRenderCount()
    {
        return diffRenderCount;
    }

    @ManagedMetric(metricType = COUNTER, unit = "chars")
    public long getDiffRenderedSize()
    {
        return diffRenderedSize;
    }

    @ManagedMetric(metricType = COUNTER)
    public long getSkipRenderCount()
    {
        return skipRenderCount;
    }

    @ManagedMetric(metricType = COUNTER, unit = "chars")
    public long getSavedRenderedSize()
    {
        return savedRenderedSize;
    }

    @ManagedMetric(metricType = COUNTER, unit = "ms")
    public long getRenderTime()
    {
        return renderTime;
    }

    @ManagedMetric(metricType = COUNTER, unit = "ms")
    public long getMaxRenderTime()
    {
        return maxRenderTime;
    }

    @ManagedMetric(metricType = COUNTER, unit = "ms")
    public long getLastRenderTime()
    {
        return lastRenderTime;
    }

    @ManagedMetric(metricType = COUNTER, unit = "chars")
    public long getSentRenderedSize()
    {
        return sentRenderedSize;
    }

    @ManagedOperation
    public void reset()
    {
        fullRenderCount = 0;
        fullRenderedSize = 0;
        diffRenderAttempts = 0;
        diffRenderCount = 0;
        diffRenderedSize = 0;
        savedRenderedSize = 0;
        sentRenderedSize = 0;
        renderTime = 0;
        maxRenderTime = 0;
        lastRenderTime = 0;
    }

    @Override
    public synchronized void renderComplete(RenderType aType, long aTime, String aFull,
            String aDiff)
    {
        switch (aType) {
        case SKIP:
            skipRenderCount++;
            savedRenderedSize += aFull.length();
            break;
        case DIFFERENTIAL:
            diffRenderCount++;
            diffRenderedSize += aDiff.length();
            sentRenderedSize += aDiff.length();
            savedRenderedSize += aFull.length() - aDiff.length();
            break;
        case FULL:
            fullRenderCount++;
            fullRenderedSize += aFull.length();
            sentRenderedSize += aFull.length();
            if (aDiff != null) {
                diffRenderAttempts++;
            }
            break;
        }

        renderTime += aTime;
        maxRenderTime = Math.max(maxRenderTime, aTime);
        lastRenderTime = aTime;
    }
}

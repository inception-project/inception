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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client;

import static org.springframework.jmx.support.MetricType.COUNTER;

import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

@ManagedResource
public class OllamaMetricsImpl
    implements OllamaMetrics
{
    private int responseCount;
    private long totalDuration;
    private long loadDuration;
    private long promptEvalDuration;
    private long evalDuration;
    private int promptEvalCount;
    private int evalCount;

    @Override
    public void handleResponse(OllamaTokenMetrics aResponse)
    {
        responseCount++;
        totalDuration += aResponse.getTotalDuration();
        loadDuration += aResponse.getLoadDuration();
        promptEvalDuration += aResponse.getPromptEvalDuration();
        evalDuration += aResponse.getEvalDuration();
        promptEvalCount += aResponse.getPromptEvalCount();
        evalCount += aResponse.getEvalCount();
    }

    @ManagedMetric(metricType = COUNTER)
    public int getResponseCount()
    {
        return responseCount;
    }

    @ManagedMetric(metricType = COUNTER, unit = "ns")
    public long getTotalDuration()
    {
        return totalDuration;
    }

    @ManagedMetric(metricType = COUNTER, unit = "ns")
    public long getLoadDuration()
    {
        return loadDuration;
    }

    @ManagedMetric(metricType = COUNTER, unit = "ns")
    public long getPromptEvalDuration()
    {
        return promptEvalDuration;
    }

    @ManagedMetric(metricType = COUNTER, unit = "ns")
    public long getEvalDuration()
    {
        return evalDuration;
    }

    @ManagedMetric(metricType = COUNTER)
    public int getPromptEvalCount()
    {
        return promptEvalCount;
    }

    @ManagedMetric(metricType = COUNTER)
    public int getEvalCount()
    {
        return evalCount;
    }

    @ManagedOperation
    public void reset()
    {
        responseCount = 0;
        totalDuration = 0;
        loadDuration = 0;
        promptEvalDuration = 0;
        evalDuration = 0;
        promptEvalCount = 0;
        evalCount = 0;
    }
}

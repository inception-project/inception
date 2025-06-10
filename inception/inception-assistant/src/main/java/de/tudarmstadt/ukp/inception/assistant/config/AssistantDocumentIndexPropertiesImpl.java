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
package de.tudarmstadt.ukp.inception.assistant.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.jmx.export.annotation.ManagedResource;

@ConfigurationProperties("assistant.documents")
@ManagedResource
public class AssistantDocumentIndexPropertiesImpl
    implements AssistantDocumentIndexProperties
{
    private Duration idleEvictionDelay = Duration.ofMinutes(5);
    private Duration minIdleTime = Duration.ofMinutes(5);
    private Duration borrowWaitTimeout = Duration.ofMinutes(3);
    private int maxChunks = 10;
    private int chunkSize = 128;
    private double minScore = 0.6;
    private int unitOverlap = 0;

    @Override
    public Duration getIdleEvictionDelay()
    {
        return idleEvictionDelay;
    }

    public void setIdleEvictionDelay(Duration aIdleEvictionDelay)
    {
        idleEvictionDelay = aIdleEvictionDelay;
    }

    @Override
    public Duration getMinIdleTime()
    {
        return minIdleTime;
    }

    public void setMinIdleTime(Duration aMinIdleTime)
    {
        minIdleTime = aMinIdleTime;
    }

    @Override
    public Duration getBorrowWaitTimeout()
    {
        return borrowWaitTimeout;
    }

    public void setBorrowWaitTimeout(Duration aBorrowWaitTimeout)
    {
        borrowWaitTimeout = aBorrowWaitTimeout;
    }

    @Override
    public int getMaxChunks()
    {
        return maxChunks;
    }

    public void setMaxChunks(int aMaxChunks)
    {
        maxChunks = aMaxChunks;
    }

    @Override
    public double getMinScore()
    {
        return minScore;
    }

    public void setMinScore(double aMinScore)
    {
        minScore = aMinScore;
    }

    @Override
    public int getChunkSize()
    {
        return chunkSize;
    }

    public void setChunkSize(int aChunkSize)
    {
        chunkSize = aChunkSize;
    }

    @Override
    public int getUnitOverlap()
    {
        return unitOverlap;
    }

    public void setUnitOverlap(int aUnitOverlap)
    {
        unitOverlap = aUnitOverlap;
    }
}

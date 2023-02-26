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
package de.tudarmstadt.ukp.inception.feature.lookup.config;

import static java.time.Duration.ofMinutes;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

@ConfigurationProperties("annotation.feature-support.lookup")
public class LookupServicePropertiesImpl
    implements LookupServiceProperties
{
    public static final int HARD_QUERY_LENGTH = 200;
    public static final int HARD_QUERY_CONTEXT_LENGTH = 200;
    public static final int HARD_MIN_RESULTS = 10;

    private int defaultMaxResults = 1_000;
    private int hardMaxResults = 10_000;

    private long cacheSize = 100_000;
    private @DurationUnit(MINUTES) Duration cacheExpireDelay = ofMinutes(15);
    private @DurationUnit(MINUTES) Duration cacheRefreshDelay = ofMinutes(5);

    private long renderCacheSize = 10_000;
    private @DurationUnit(MINUTES) Duration renderCacheExpireDelay = ofMinutes(10);
    private @DurationUnit(MINUTES) Duration renderCacheRefreshDelay = ofMinutes(1);

    private Duration connectTimeout = Duration.of(10, SECONDS);
    private Duration readTimeout = Duration.of(10, SECONDS);

    @Override
    public int getDefaultMaxResults()
    {
        return defaultMaxResults;
    }

    public void setDefaultMaxResults(int aDefaultMaxResults)
    {
        defaultMaxResults = aDefaultMaxResults;
    }

    @Override
    public int getHardMaxResults()
    {
        return hardMaxResults;
    }

    public void setHardMaxResults(int aHardMaxResults)
    {
        hardMaxResults = aHardMaxResults;
    }

    @Override
    public long getCacheSize()
    {
        return cacheSize;
    }

    public void setCacheSize(long aCacheSize)
    {
        cacheSize = aCacheSize;
    }

    @Override
    public long getRenderCacheSize()
    {
        return renderCacheSize;
    }

    public void setRenderCacheSize(long aRenderCacheSize)
    {
        renderCacheSize = aRenderCacheSize;
    }

    @Override
    public Duration getRenderCacheExpireDelay()
    {
        return renderCacheExpireDelay;
    }

    public void setRenderCacheExpireDelay(Duration aRenderCacheExpireDelay)
    {
        renderCacheExpireDelay = aRenderCacheExpireDelay;
    }

    @Override
    public Duration getRenderCacheRefreshDelay()
    {
        return renderCacheRefreshDelay;
    }

    public void setRenderCacheRefreshDelay(Duration aRencerCacheRefreshDelay)
    {
        renderCacheRefreshDelay = aRencerCacheRefreshDelay;
    }

    @Override
    public Duration getCacheExpireDelay()
    {
        return cacheExpireDelay;
    }

    public void setCacheExpireDelay(Duration aCacheExpireDelay)
    {
        cacheExpireDelay = aCacheExpireDelay;
    }

    @Override
    public Duration getCacheRefreshDelay()
    {
        return cacheRefreshDelay;
    }

    public void setCacheRefreshDelay(Duration aCacheRefreshDelay)
    {
        cacheRefreshDelay = aCacheRefreshDelay;
    }

    @Override
    public Duration getConnectTimeout()
    {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration aConnectTimeout)
    {
        connectTimeout = aConnectTimeout;
    }

    @Override
    public Duration getReadTimeout()
    {
        return readTimeout;
    }

    public void setReadTimeout(Duration aReadTimeout)
    {
        readTimeout = aReadTimeout;
    }
}

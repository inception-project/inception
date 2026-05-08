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
package de.tudarmstadt.ukp.inception.kb.config;

import static java.time.Duration.ofMinutes;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Collections.emptyList;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

/**
 * <p>
 * This class is exposed as a Spring Component via {@link KnowledgeBaseServiceAutoConfiguration}.
 * </p>
 */
@ConfigurationProperties("knowledge-base")
public class KnowledgeBasePropertiesImpl
    implements KnowledgeBaseProperties
{
    public static final int HARD_MIN_RESULTS = 10;

    /**
     * Multiplier applied to the per-KB maximum result count to determine how many candidates the
     * internal full-text search (Lucene) backing the local KB indexes returns before secondary
     * filtering and ranking are applied. Higher values improve recall at the cost of search
     * performance. Clamped to the range [1.0, 10.0].
     */
    private double ftsInternalMaxResultsFactor = 2.5;

    /**
     * Default result limit for SPARQL queries. Determines the default value for the maximum number
     * of results that can be retrieved from a SPARQL query. The queries are used to retrieve
     * concepts, statements, properties, etc. from the knowledge base. The maximum number of results
     * can also be configured separately for each knowledge base in the project settings.
     */
    private int defaultMaxResults = 1_000;

    /** Hard limit for the maximum number of results from a query. */
    private int hardMaxResults = 10_000;

    /** Whether to delete orphaned KBs on start. */
    private boolean removeOrphansOnStart = false;

    /** Number of items (classes, instances and properties) to cache. */
    private long cacheSize = 100_000;

    /** Time before items are expunged from the cache. */
    private @DurationUnit(MINUTES) Duration cacheExpireDelay = ofMinutes(15);

    /** Time before items are asynchronously refreshed. */
    private @DurationUnit(MINUTES) Duration cacheRefreshDelay = ofMinutes(5);

    /** Number of items (classes, instances and properties) to cache during rendering. */
    private long renderCacheSize = 10_000;

    /** Time before items are expunged from the render cache. */
    private @DurationUnit(MINUTES) Duration renderCacheExpireDelay = ofMinutes(10);

    /** Time before items are asynchronously refreshed when rendering. */
    private @DurationUnit(MINUTES) Duration renderCacheRefreshDelay = ofMinutes(1);

    private List<String> defaultFallbackLanguages = emptyList();

    public void setFtsInternalMaxResultsFactor(double aFtsMaxResultsFactor)
    {
        ftsInternalMaxResultsFactor = aFtsMaxResultsFactor;
    }

    @Override
    public double getFtsInternalMaxResultsFactor()
    {
        return ftsInternalMaxResultsFactor;
    }

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
    public boolean isRemoveOrphansOnStart()
    {
        return removeOrphansOnStart;
    }

    public void setRemoveOrphansOnStart(boolean aRemoveOrphansOnStart)
    {
        removeOrphansOnStart = aRemoveOrphansOnStart;
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

    public void setRenderCacheRefreshDelay(Duration aRenderCacheRefreshDelay)
    {
        renderCacheRefreshDelay = aRenderCacheRefreshDelay;
    }

    @Override
    public List<String> getDefaultFallbackLanguages()
    {
        return defaultFallbackLanguages;
    }

    public void setDefaultFallbackLanguages(List<String> aDefaultFallbackLanguages)
    {
        defaultFallbackLanguages = aDefaultFallbackLanguages;
    }
}

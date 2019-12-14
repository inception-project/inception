/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.kb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("inception.knowledge-base")
public class KnowledgeBasePropertiesImpl
    implements KnowledgeBaseProperties
{
    public static final int HARD_MIN_RESULTS = 10;
    
    private int defaultMaxResults = 1_000;
    private int hardMaxResults = 10_000;
    private long cacheSize = 100_000;
    private int cacheExpireDelay = 15;
    private int cacheRefreshDelay = 5;

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
    public int getCacheExpireDelay()
    {
        return cacheExpireDelay;
    }
    
    public void setCacheExpireDelay(int aCacheExpireDelay)
    {
        cacheExpireDelay = aCacheExpireDelay;
    }

    @Override
    public int getCacheRefreshDelay()
    {
        return cacheRefreshDelay;
    }
    
    public void setCacheRefreshDelay(int aCacheRefreshDelay)
    {
        cacheRefreshDelay = aCacheRefreshDelay;
    }
}

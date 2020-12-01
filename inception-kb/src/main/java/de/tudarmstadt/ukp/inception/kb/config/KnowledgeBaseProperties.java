/*
 * Copyright 2018
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

import java.time.Duration;

public interface KnowledgeBaseProperties
{
    int getDefaultMaxResults();
    
    int getHardMaxResults();

    /**
     * The cache size in terms of KB items that are being cached. A single query may return a
     * large number of such items.
     */
    long getCacheSize();

    /**
     * The time before KB items are dropped from the cache if they have not been accessed (in
     * minutes).
     */
    Duration getCacheExpireDelay();

    /**
     * The time before KB items are asynchronously refreshed (in minutes).
     */
    Duration getCacheRefreshDelay();
}

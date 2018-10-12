/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.api.recommender;

import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecommenderContext
{
    private static final Logger LOG = LoggerFactory.getLogger(RecommenderContext.class);

    private final ConcurrentHashMap<String, Object> store;

    public RecommenderContext()
    {
        store = new ConcurrentHashMap<>();
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T get(Key<T> aKey)
    {
        String name = aKey.name;
        if (!store.containsKey(name)) {
            LOG.warn("Value with key [{}] not found in context!", name);
            return null;
        }
        return (T) store.get(name);
    }

    public <T> void put(Key<T> aKey, T aValue)
    {
        store.put(aKey.name, aValue);
    }

    public static class Key<T>
    {
        private final String name;

        public Key(String aName)
        {
            name = aName;
        }
    }
}

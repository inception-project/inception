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

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

public class RecommenderContext
{
    private final ConcurrentHashMap<String, Object> store;
    private boolean ready = false;

    public RecommenderContext()
    {
        store = new ConcurrentHashMap<>();
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> Optional<T> get(Key<T> aKey)
    {
        return Optional.ofNullable((T) store.get(aKey.name));
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
    
    /**
     * Mark context as ready meaning that it can be used to generate predictions.
     */
    public void markAsReadyForPrediction()
    {
        ready = true;
    }
    
    /**
     * @return whether the context is ready to be used for making predictions.
     */
    public boolean isReadyForPrediction()
    {
        return ready;
    }
}

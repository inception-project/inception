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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

public class RecommenderContext
{
    /**
     * Empty context which starts out being closed.
     */
    public static final RecommenderContext EMPTY_CONTEXT;
    
    static {
        EMPTY_CONTEXT = new  RecommenderContext();
        EMPTY_CONTEXT.close();
    }
    
    private final Map<String, Object> store;
    private boolean closed = false;

    public RecommenderContext()
    {
        store = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    @Nullable
    synchronized public <T> Optional<T> get(Key<T> aKey)
    {
        return Optional.ofNullable((T) store.get(aKey.name));
    }

    synchronized public <T> void put(Key<T> aKey, T aValue)
    {
        if (closed) {
            throw new IllegalStateException("Adding data to a closed context is not permitted.");
        }
        
        store.put(aKey.name, aValue);
    }
    
    /**
     * Close the context. Further modifications to the context are not permitted.
     */
    synchronized public void close()
    {
        closed = true;
    }
    
    /**
     * @return whether the context is closed.
     */
    synchronized public boolean isClosed()
    {
        return closed;
    }
    
    /**
     * Creates a non-closed copy of the current context. The data from the internal store is copied
     * into the store of the new context.
     */
    synchronized public RecommenderContext copy()
    {
        if (!closed) {
            throw new IllegalStateException("Context must be closed before it can be copied.");
        }
        
        RecommenderContext ctx = new RecommenderContext();
        ctx.store.putAll(store);   
        return ctx;
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

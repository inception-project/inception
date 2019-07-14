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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;

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
    private List<LogMessage> messages;
    private boolean closed = false;

    public RecommenderContext()
    {
        store = new HashMap<>();
        messages = new ArrayList<>();
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
    
    synchronized public void info(String aFormat, Object... aValues)
    {
        if (closed) {
            throw new IllegalStateException("Adding data to a closed context is not permitted.");
        }
        
        messages.add(LogMessage.info(this, aFormat, aValues));
    }

    synchronized public void warn(String aFormat, Object... aValues)
    {
        if (closed) {
            throw new IllegalStateException("Adding data to a closed context is not permitted.");
        }
        
        messages.add(LogMessage.warn(this, aFormat, aValues));
    }

    synchronized public void error(String aFormat, Object... aValues)
    {
        if (closed) {
            throw new IllegalStateException("Adding data to a closed context is not permitted.");
        }
        
        messages.add(LogMessage.error(this, aFormat, aValues));
    }
    
    public List<LogMessage> getMessages()
    {
        return messages;
    }

    /**
     * Close the context. Further modifications to the context are not permitted.
     */
    synchronized public void close()
    {
        if (!closed) {
            closed = true;
            messages = Collections.unmodifiableList(messages);
        }
    }
    
    /**
     * @return whether the context is closed.
     */
    synchronized public boolean isClosed()
    {
        return closed;
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

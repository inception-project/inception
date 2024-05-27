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
package de.tudarmstadt.ukp.inception.recommendation.api.recommender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class RecommenderContext
{
    private final Map<String, Object> store;
    private List<LogMessage> messages;
    private Optional<User> user;
    private boolean closed = false;

    public RecommenderContext()
    {
        store = new HashMap<>();
        messages = new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
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

    synchronized public void log(LogMessage aMessage)
    {
        if (closed) {
            throw new IllegalStateException("Adding data to a closed context is not permitted.");
        }

        messages.add(aMessage);
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

    public Optional<User> getUser()
    {
        return user;
    }

    public void setUser(User aUser)
    {
        user = Optional.ofNullable(aUser);
    }

    /**
     * Empty context which starts out being closed.
     */
    public static RecommenderContext emptyContext()
    {
        var ctx = new RecommenderContext();
        ctx.close();
        return ctx;
    }
}

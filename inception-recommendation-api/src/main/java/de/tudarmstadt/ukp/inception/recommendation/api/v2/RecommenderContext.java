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
package de.tudarmstadt.ukp.inception.recommendation.api.v2;

import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

public class RecommenderContext {
    private final ConcurrentHashMap<String, Object> store;
    private final String nameSpace;

    public RecommenderContext() {
        store = new ConcurrentHashMap<>();
        nameSpace = "";
    }

    private RecommenderContext(String aNameSpace, ConcurrentHashMap<String, Object> aStore) {
        nameSpace = aNameSpace;
        store = aStore;
    }

    public RecommenderContext getView(String aNameSpace) {
        return new RecommenderContext(aNameSpace, store);
    }

    public <T> T get(String aKey) {
        String key = buildKey(aKey);
        if (!store.containsKey(key)) {
            String message = String.format("Value with key [%s] not found in context!", key);
            throw new NoSuchElementException(message);
        }

        return (T) store.get(key);
    }
    public <T> void set(String aKey, T aValue) {
        store.put(buildKey(aKey), aValue);
    }

    private String buildKey(String aKey) {
        return nameSpace + ":" + aKey;
    }
}

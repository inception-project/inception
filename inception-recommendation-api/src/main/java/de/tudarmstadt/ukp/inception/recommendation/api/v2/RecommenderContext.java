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
        if (!store.contains(aKey)) {
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

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
package de.tudarmstadt.ukp.inception.conceptlinking.model;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableMap;

import java.util.IllformedLocaleException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.builder.ToStringBuilder;

import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;

/**
 * Stores information about entities retrieved from a knowledge base needed to rank candidates.
 */
public class CandidateEntity
{
    /**
     * The query entered by the user.
     */
    public static final Key<String> KEY_QUERY = new Key<>("query");
    
    /**
     * The mention in the text which is to be linked.
     */
    public static final Key<String> KEY_MENTION = new Key<>("mention");
    
    /**
     * The context of the mention.
     */
    public static final Key<List<String>> KEY_MENTION_CONTEXT = new Key<>("mentionContext");

    /**
     * Edit distance between mention and candidate entity label.
     * <p>
     * The smaller the distance, the better the ranking. Thus we use {@link Integer#MAX_VALUE} as
     * the default value to ensure that candidates are ranked last on this feature if it could not
     * be calculated.
     */
    public static final Key<Integer> KEY_LEVENSHTEIN_MENTION = new Key<>("levMention",
            Integer.MAX_VALUE);

    /**
     * Edit distance between mention + context and candidate entity label
     * <p>
     * The smaller the distance, the better the ranking. Thus we use {@link Integer#MAX_VALUE} as
     * the default value to ensure that candidates are ranked last on this feature if it could not
     * be calculated.
     */
    public static final Key<Integer> KEY_LEVENSHTEIN_MENTION_CONTEXT = new Key<>("levContext",
            Integer.MAX_VALUE);

    /**
     * Edit distance between typed string and candidate entity label
     * <p>
     * The smaller the distance, the better the ranking. Thus we use {@link Integer#MAX_VALUE} as
     * the default value to ensure that candidates are ranked last on this feature if it could not
     * be calculated.
     */
    public static final Key<Integer> KEY_LEVENSHTEIN_QUERY = new Key<>("levQuery",
            Integer.MAX_VALUE);

    /**
     * set of directly related entities as IRI Strings
     */
    public static final Key<Set<String>> KEY_SIGNATURE_OVERLAP = new Key<>("signatureOverlap",
            emptySet());

    /**
     * number of distinct relations to other entities
     */
    public static final Key<Integer> KEY_NUM_RELATIONS = new Key<>("numRelatedRelations", 0);

    /**
     * number of related entities whose entity label occurs in <i>content tokens</i>
     * <i>Content tokens</i> consist of tokens in mention sentence annotated as nouns, verbs or
     * adjectives
     */
    public static final Key<Integer> KEY_SIGNATURE_OVERLAP_SCORE = new Key<>(
            "signatureOverlapScore", 0);

    /**
     * logarithm of the wikidata ID - based on the assumption that lower IDs are more important
     */
    public static final Key<Double> KEY_ID_RANK = new Key<>("idRank", 0.0d);

    /**
     * in-link count of wikipedia article of IRI
     */
    public static final Key<Integer> KEY_FREQUENCY = new Key<>("frequency", 0);

    private final KBHandle handle;
    private final ConcurrentHashMap<String, Object> features = new ConcurrentHashMap<>();
    private Locale locale;
    
    public CandidateEntity(KBHandle aHandle)
    {
        handle = aHandle;
        
        if (aHandle.getKB().getDefaultLanguage() == null) {
            locale = Locale.ENGLISH;
        }
        else {
            try {
                locale = Locale.forLanguageTag(aHandle.getKB().getDefaultLanguage());
            }
            catch (IllformedLocaleException e) {
                locale = Locale.ENGLISH;
            }
        }
    }
    
    public KBHandle getHandle()
    {
        return handle;
    }
    
    public Locale getLocale()
    {
        return locale;
    }

    /**
     * @return The IRI String of this entity
     */
    public String getIRI()
    {
        return handle.getIdentifier();
    }

    /**
     * @return The main label of this entity
     */
    public String getLabel()
    {
        return handle.getUiLabel();
    }

    /**
     * Get a description for this entity
     */
    public String getDescription()
    {
        return handle.getDescription();
    }

    public String getLanguage()
    {
        return handle.getLanguage();
    }

    public <T> Optional<T> get(Key<T> aKey)
    {
        return Optional.ofNullable((T) features.getOrDefault(aKey.name, aKey.getDefaultValue()));
    }

    public <T> void put(Key<T> aKey, T aValue)
    {
        if (aValue != null) {
            features.put(aKey.name, aValue);
        }
        else {
            features.remove(aKey.name);
        }
    }
    
    public Map<String, Object> getFeatures()
    {
        return unmodifiableMap(features);
    }

    public static class Key<T>
    {
        private final String name;
        private final T defaultValue;

        public Key(String aName)
        {
            this(aName, null);
        }
        
        public Key(String aName, T aDefaultValue)
        {
            name = aName;
            defaultValue = aDefaultValue;
        }
        
        public T getDefaultValue()
        {
            return defaultValue;
        }
    }
    
    /**
     * @return edit distance between mention and candidate entity label
     */
    @Deprecated
    public int getLevMention()
    {
        return get(KEY_LEVENSHTEIN_MENTION).get();
    }

    /**
     * @return edit distance between mention + context and candidate entity label
     */
    @Deprecated
    public int getLevContext()
    {
        return get(KEY_LEVENSHTEIN_MENTION_CONTEXT).get();
    }

    @Deprecated
    public int getLevQuery()
    {
        return get(KEY_LEVENSHTEIN_QUERY).get();
    }

    /**
     * @return number of distinct relations to other entities
     */
    @Deprecated
    public int getNumRelatedRelations()
    {
        return get(KEY_NUM_RELATIONS).get();
    }

    /**
     * @return number of related entities whose entity label occurs in <i>content tokens</i>.
     * <i>Content tokens</i> consist of tokens in mention sentence annotated as nouns, verbs or
     * adjectives
     */
    @Deprecated
    public int getSignatureOverlapScore()
    {
        return get(KEY_SIGNATURE_OVERLAP_SCORE).get();
    }

    /**
     * @return logarithm of the wikidata ID - based on the assumption that lower IDs are more
     * important
     */
    @Deprecated
    public double getIdRank()
    {
        return get(KEY_ID_RANK).get();
    }

    /**
     * @return in-link count of wikipedia article of IRI
     */
    @Deprecated
    public int getFrequency()
    {
        return get(KEY_FREQUENCY).get();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this).append("handle", handle).append("features", features)
                .toString();
    }
}

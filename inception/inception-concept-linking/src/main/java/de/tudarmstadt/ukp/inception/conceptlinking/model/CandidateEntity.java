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
package de.tudarmstadt.ukp.inception.conceptlinking.model;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Collections.unmodifiableMap;
import static java.util.Comparator.comparing;
import static java.util.function.Function.identity;

import java.util.IllformedLocaleException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang3.builder.ToStringBuilder;

import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;

/**
 * Stores information about entities retrieved from a knowledge base needed to rank candidates.
 */
public class CandidateEntity
{
    public static final Pattern TOKENKIZER_PATTERN = Pattern.compile("[\\s()\\-]+");

    public static String[] sortedBagOfWords(String aString)
    {
        return Stream.of(TOKENKIZER_PATTERN.split(aString))
                .sorted(comparing(String::length).reversed().thenComparing(identity())) //
                .distinct() //
                .toArray(String[]::new);
    }

    /**
     * The query entered by the user.
     */
    public static final Key<String> KEY_QUERY = new Key<>("query");
    public static final Key<String> KEY_QUERY_NC = new Key<>("queryNC");
    public static final Key<String[]> KEY_QUERY_BOW = new Key<>("queryBow");
    public static final Key<String[]> KEY_QUERY_BOW_NC = new Key<>("queryBowNC");

    /**
     * The term which had the best match with query or mention. This should be displayed to the user
     * in addition to the handles pref-label if it does differ from the pref-label.
     */
    public static final Key<String> KEY_QUERY_BEST_MATCH_TERM_NC = new Key<>(
            "queryBestMatchTermNC");

    /**
     * Whether the query entered by the user is completely in lower case.
     */
    public static final Key<Boolean> KEY_QUERY_IS_LOWER_CASE = new Key<>("queryIsLowerCase");

    /**
     * The mention in the text which is to be linked.
     */
    public static final Key<String> KEY_MENTION = new Key<>("mention");
    public static final Key<String> KEY_MENTION_NC = new Key<>("mentionNC");
    public static final Key<String[]> KEY_MENTION_BOW = new Key<>("mentionBow");
    public static final Key<String[]> KEY_MENTION_BOW_NC = new Key<>("mentionBowNC");

    public static final Key<String> KEY_LABEL_NC = new Key<>("labelNC");

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
    public static final Key<Integer> SCORE_LEVENSHTEIN_MENTION = new Key<>("levMention", MAX_VALUE);

    public static final Key<Integer> SCORE_LEVENSHTEIN_MENTION_NC = new Key<>("levMentionNC",
            MAX_VALUE);

    public static final Key<Integer> SCORE_TOKEN_OVERLAP_MENTION = new Key<>("tokenOverlapMention",
            MAX_VALUE);

    public static final Key<Integer> SCORE_TOKEN_OVERLAP_MENTION_NC = new Key<>(
            "tokenOverlapMentionNC", MAX_VALUE);

    /**
     * Edit distance between mention + context and candidate entity label
     * <p>
     * The smaller the distance, the better the ranking. Thus we use {@link Integer#MAX_VALUE} as
     * the default value to ensure that candidates are ranked last on this feature if it could not
     * be calculated.
     */
    public static final Key<Integer> SCORE_LEVENSHTEIN_MENTION_CONTEXT = new Key<>("levContext",
            MAX_VALUE);

    public static final Key<Integer> SCORE_TOKEN_OVERLAP_MENTION_CONTEXT = new Key<>(
            "tokenOverlapContext", MAX_VALUE);

    /**
     * Edit distance between typed string and candidate entity label
     * <p>
     * The smaller the distance, the better the ranking. Thus we use {@link Integer#MAX_VALUE} as
     * the default value to ensure that candidates are ranked last on this feature if it could not
     * be calculated.
     */
    public static final Key<Integer> SCORE_LEVENSHTEIN_QUERY = new Key<>("levQuery", MAX_VALUE);

    public static final Key<Integer> SCORE_LEVENSHTEIN_QUERY_NC = new Key<>("levQueryNC",
            MAX_VALUE);

    public static final Key<Integer> SCORE_TOKEN_OVERLAP_QUERY = new Key<>("tokenOverlapQuery",
            MAX_VALUE);

    public static final Key<Integer> SCORE_TOKEN_OVERLAP_QUERY_NC = new Key<>("tokenOverlapQueryNC",
            MAX_VALUE);

    /**
     * FTS score - score assigned by the KB FTS (if any)
     */
    public static final Key<Double> SCORE_FTS = new Key<>("ftsScore", 0.0d);

    private final KBHandle handle;
    private final ConcurrentHashMap<String, Object> features = new ConcurrentHashMap<>();
    private Locale locale;

    public CandidateEntity(KBHandle aHandle)
    {
        handle = aHandle;

        if (aHandle.getKB() == null || aHandle.getKB().getDefaultLanguage() == null) {
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

        put(KEY_LABEL_NC, getLabel().toLowerCase(getLocale()));
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
     * @return the description for this entity
     */
    public String getDescription()
    {
        return handle.getDescription();
    }

    public String getLanguage()
    {
        return handle.getLanguage();
    }

    public boolean isDeprecated()
    {
        return handle.isDeprecated();
    }

    public CandidateEntity withQuery(String aQuery)
    {
        if (aQuery != null) {
            put(KEY_QUERY, aQuery);
            put(KEY_QUERY_BOW, sortedBagOfWords(aQuery));
            var lowerCaseQuery = aQuery.toLowerCase(getLocale());
            put(KEY_QUERY_NC, lowerCaseQuery);
            put(KEY_QUERY_BOW_NC, sortedBagOfWords(lowerCaseQuery));
        }
        return this;
    }

    public CandidateEntity withMention(String aMention)
    {
        if (aMention != null) {
            put(KEY_MENTION, aMention);
            put(KEY_MENTION_BOW, sortedBagOfWords(aMention));
            var lowerCaseMention = aMention.toLowerCase(getLocale());
            put(KEY_MENTION_NC, lowerCaseMention);
            put(KEY_MENTION_BOW_NC, sortedBagOfWords(lowerCaseMention));
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(Key<T> aKey)
    {
        return Optional.ofNullable((T) features.getOrDefault(aKey.name, aKey.getDefaultValue()));
    }

    /**
     * Same as {@link #put} except that it is fluent.
     * 
     * @param aKey
     *            a key.
     * @param aValue
     *            a value.
     * @param <T>
     *            the value type.
     * 
     * @return object for chaining.
     */
    public <T> CandidateEntity with(Key<T> aKey, T aValue)
    {
        put(aKey, aValue);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T put(Key<T> aKey, T aValue)
    {
        if (aValue != null) {
            return (T) features.put(aKey.name, aValue);
        }
        else {
            return (T) features.remove(aKey.name);
        }
    }

    public boolean mergeMin(Key<Integer> aKey, int aValue)
    {
        var newValue = (int) features.merge(aKey.name, aValue,
                (o, n) -> o == null ? n : Math.min((int) o, (int) n));
        return newValue == aValue;
    }

    public Map<String, Object> getFeatures()
    {
        return unmodifiableMap(features);
    }

    public String getFeaturesAsString()
    {
        var sb = new StringBuilder();

        var featureKeys = new LinkedHashSet<String>();
        featureKeys.add(KEY_QUERY_NC.name);
        featureKeys.add(KEY_MENTION_NC.name);
        featureKeys.add(KEY_MENTION_CONTEXT.name);
        featureKeys.add(KEY_LABEL_NC.name);
        featureKeys.add(KEY_QUERY_BEST_MATCH_TERM_NC.name);
        featureKeys.add(KEY_QUERY_IS_LOWER_CASE.name);
        featureKeys.add(SCORE_TOKEN_OVERLAP_QUERY_NC.name);
        featureKeys.add(SCORE_LEVENSHTEIN_QUERY_NC.name);
        featureKeys.add(SCORE_LEVENSHTEIN_QUERY.name);
        featureKeys.add(SCORE_LEVENSHTEIN_MENTION_NC.name);
        featureKeys.add(SCORE_LEVENSHTEIN_MENTION.name);
        featureKeys.add(SCORE_LEVENSHTEIN_MENTION_CONTEXT.name);
        featureKeys.addAll(features.keySet());
        featureKeys.remove(KEY_QUERY.name);
        featureKeys.remove(KEY_MENTION.name);

        for (var key : featureKeys) {
            var value = features.get(key);
            if (value == null || value.getClass().isArray()) {
                continue;
            }

            if (sb.length() > 0) {
                sb.append(", ");
            }

            sb.append(key);
            sb.append(":");
            sb.append(value);
        }

        return sb.toString();
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

    @Override
    public String toString()
    {
        return new ToStringBuilder(this) //
                .append("handle", handle) //
                .append("features", features) //
                .toString();
    }
}

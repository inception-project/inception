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
package de.tudarmstadt.ukp.inception.kb.querybuilder;

import static de.tudarmstadt.ukp.inception.kb.IriConstants.PREFIX_GRAPHDB;
import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilder.convertToRequiredTokenPrefixMatchingQuery;
import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilder.Priority.PRIMARY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder.prefix;
import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.and;
import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.union;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.sparqlbuilder.constraint.Expression;
import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;

public class FtsAdapterGraphDb
    implements FtsAdapter
{
    private static final String MULTI_CHAR_WILDCARD = "*";

    private static final Prefix PREFIX_GRAPHDB_SEARCH = prefix("onto", iri(PREFIX_GRAPHDB));

    private final SPARQLQueryBuilder builder;

    public FtsAdapterGraphDb(SPARQLQueryBuilder aBuilder)
    {
        builder = aBuilder;
        builder.addPrefix(PREFIX_GRAPHDB_SEARCH);
    }

    @Override
    public void withLabelMatchingExactlyAnyOf(String... aValues)
    {
        var kb = builder.getKnowledgeBase();

        var valuePatterns = new ArrayList<GraphPattern>();
        for (var value : aValues) {
            var sanitizedValue = builder.sanitizeQueryString_FTS(value);

            if (isBlank(sanitizedValue)) {
                continue;
            }

            builder.addProjection(VAR_SCORE);

            var filter = builder.equalsPattern(VAR_MATCH_TERM, value, kb);
            valuePatterns.addAll(buildLanguageUnionPatterns(sanitizedValue, filter));
        }

        if (valuePatterns.isEmpty()) {
            builder.noResult();
        }

        builder.addPattern(PRIMARY, and( //
                builder.bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY), //
                union(valuePatterns.toArray(GraphPattern[]::new))));
    }

    @Override
    public void withLabelContainingAnyOf(String... aValues)
    {
        var valuePatterns = new ArrayList<GraphPattern>();
        for (var value : aValues) {
            var sanitizedValue = builder.sanitizeQueryString_FTS(value);

            if (isBlank(sanitizedValue)) {
                continue;
            }

            builder.addProjection(VAR_SCORE);

            // Use a prefix wildcard so that Lucene matches tokens that START WITH the search
            // term. Without the wildcard, only exact token matches are returned (e.g. "auto"
            // finds "auto"@nl but NOT "automóvel"@pt or "automobile"@en). The REGEX CONTAINS
            // post-filter then confirms the actual substring relationship.
            var ftsQuery = sanitizedValue + MULTI_CHAR_WILDCARD;
            var filter = builder.containsPattern(VAR_MATCH_TERM, value);
            valuePatterns.addAll(buildLanguageUnionPatterns(ftsQuery, filter));
        }

        if (valuePatterns.isEmpty()) {
            builder.noResult();
        }

        builder.addPattern(PRIMARY, and(builder.bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY),
                union(valuePatterns.toArray(GraphPattern[]::new))));
    }

    @Override
    public void withLabelStartingWith(String aPrefixQuery)
    {
        // Strip single quotes and asterisks because they have special semantics
        var queryString = builder.sanitizeQueryString_FTS(aPrefixQuery);

        if (isBlank(queryString)) {
            builder.noResult();
        }

        // If the query string entered by the user does not end with a space character, then
        // we assume that the user may not yet have finished writing the word and add a
        // wildcard
        if (!aPrefixQuery.endsWith(" ")) {
            queryString += MULTI_CHAR_WILDCARD;
        }

        builder.addProjection(VAR_SCORE);

        // Locate all entries where the label contains the prefix (using the FTS) and then
        // filter them by those which actually start with the prefix.
        // We issue one FTS query per effective language so that language-tagged literals
        // are returned with their language tag preserved (onto:fts requires the language
        // code to be passed explicitly to search the language-specific Lucene index).
        var filter = builder.startsWithPattern(VAR_MATCH_TERM, aPrefixQuery);
        var patterns = buildLanguageUnionPatterns(queryString, filter);
        builder.addPattern(PRIMARY, and( //
                builder.bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY), //
                union(patterns.toArray(GraphPattern[]::new))));
    }

    @Override
    public void withLabelMatchingAnyOf(String... aValues)
    {
        var valuePatterns = new ArrayList<GraphPattern>();
        for (var value : aValues) {
            var sanitizedValue = builder.sanitizeQueryString_FTS(value);

            if (isBlank(sanitizedValue)) {
                continue;
            }

            var fuzzyQuery = convertToRequiredTokenPrefixMatchingQuery(sanitizedValue, "",
                    MULTI_CHAR_WILDCARD);

            if (isBlank(fuzzyQuery)) {
                continue;
            }

            builder.addProjection(VAR_SCORE);

            var filter = builder.matchKbLanguage(VAR_MATCH_TERM);
            valuePatterns.addAll(buildLanguageUnionPatterns(fuzzyQuery, filter));
        }

        if (valuePatterns.isEmpty()) {
            builder.noResult();
        }

        builder.addPattern(PRIMARY, and(builder.bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY),
                union(valuePatterns.toArray(GraphPattern[]::new))));
    }

    /**
     * Builds a list of {@link GraphDbFtsQuery} patterns — one for the default (plain literal) index
     * and one for each effective language index. Calling code should union these patterns.
     * <p>
     * GraphDB’s simple FTS ({@code onto:fts}) only indexes language-tagged literals in
     * language-specific indexes, not in the default index. To search language-tagged literals while
     * preserving their language tag in the result, the language code must be passed as a second
     * string argument: {@code onto:fts ("query" "en" limit)}.
     */
    private List<GraphPattern> buildLanguageUnionPatterns(String aFtsQuery, Expression<?> aFilter)
    {
        var patterns = new ArrayList<GraphPattern>();

        // Per-language indexes: each language-specific index is searched separately
        // and returns literals with the language tag intact
        var defaultLanguage = builder.getKnowledgeBase().getDefaultLanguage();
        if (defaultLanguage != null) {
            patterns.add(match(aFtsQuery, aFilter, defaultLanguage));
        }

        // ... also search fallback language indices
        for (var lang : builder.getFallbackLanguages()) {
            patterns.add(match(aFtsQuery, aFilter, lang));
        }

        // Default index: matches plain (language-untagged) literals
        patterns.add(match(aFtsQuery, aFilter, null));

        return patterns;
    }

    private GraphPattern match(String aFtsQuery, Expression<?> aFilter, String lang)
    {
        return new GraphDbFtsQuery(VAR_SUBJECT, VAR_SCORE, VAR_MATCH_TERM, VAR_MATCH_TERM_PROPERTY,
                aFtsQuery) //
                        .withLanguage(lang) //
                        .withLimit(builder.getLimit()) //
                        .filter(aFilter);
    }
}

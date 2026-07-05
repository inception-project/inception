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

import static de.tudarmstadt.ukp.inception.kb.IriConstants.PREFIX_MARKLOGIC;
import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilder.Priority.PRIMARY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.and;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.custom;
import static org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder.prefix;
import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.union;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.literalOf;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.sparqlbuilder.constraint.Expression;
import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;

/**
 * Full-text search adapter for <a href="https://www.marklogic.com">MarkLogic Server</a>. MarkLogic
 * exposes its text search inside SPARQL through the {@code cts:contains} extension function which
 * can be used in a {@code FILTER} together with a {@code cts:query} - here a {@code cts:word-query}
 * built from the search terms:
 *
 * <pre>
 *   ?subj ?pMatch ?m .
 *   FILTER( cts:contains(?m, cts:word-query("albert")) )
 *   FILTER( cts:contains(?m, cts:word-query("ein*", "wildcarded")) )
 *   FILTER( ... exactness check on ?m ... )
 * </pre>
 *
 * Each search token becomes its own {@code cts:contains(...)} filter, so all tokens have to occur
 * in the matched literal (in any order), mirroring the "contains all tokens" semantics of the other
 * adapters. A trailing {@code *} turns the last token into a prefix match (used for the
 * auto-completion / starts-with lookups) via the {@code wildcarded} option. As with the other
 * adapters, the {@code cts:contains} filters only pre-select candidates through the text index; the
 * final {@code FILTER} enforces the actual starts-with / contains / exact-match semantics on the
 * matched literal.
 * <p>
 * This requires the target MarkLogic database to have the triple index enabled (otherwise SPARQL
 * queries fail with {@code XDMP-TRPLIDXNOTFOUND}) and, for the wildcard lookups to be effective,
 * wildcard searches enabled.
 */
public class FtsAdapterMarkLogic
    implements FtsAdapter
{
    private static final Prefix PREFIX_MARKLOGIC_CTS = prefix("cts", iri(PREFIX_MARKLOGIC));
    private static final Iri CTS_CONTAINS = PREFIX_MARKLOGIC_CTS.iri("contains");
    private static final Iri CTS_WORD_QUERY = PREFIX_MARKLOGIC_CTS.iri("word-query");

    private static final String WILDCARDED = "wildcarded";

    /**
     * How the search tokens are turned into {@code cts:word-query} terms. MarkLogic word queries
     * match whole words, so substring / prefix lookups have to be expressed with wildcards (which
     * require the {@code wildcarded} option). The trailing exactness {@code FILTER} added by the
     * caller still enforces the precise semantics; these terms only pre-select candidates.
     */
    private enum MatchMode
    {
        /** Every token must occur as a whole word (e.g. for exact-match pre-filtering). */
        WHOLE_WORD,
        /** Every token must occur as a substring - {@code *token*}. */
        SUBSTRING,
        /** The last token is a prefix - {@code token*} - all preceding tokens are whole words. */
        PREFIX_LAST
    }

    private final SPARQLQueryBuilder builder;

    public FtsAdapterMarkLogic(SPARQLQueryBuilder aBuilder)
    {
        builder = aBuilder;
    }

    @Override
    public void withLabelMatchingExactlyAnyOf(String... aValues)
    {
        builder.addPrefix(PREFIX_MARKLOGIC_CTS);

        var valuePatterns = new ArrayList<GraphPattern>();
        for (var value : aValues) {
            var sanitizedValue = builder.sanitizeQueryString_FTS(value);

            if (isBlank(sanitizedValue)) {
                continue;
            }

            var textFilters = toContainsFilters(sanitizedValue, MatchMode.WHOLE_WORD);

            if (textFilters.isEmpty()) {
                continue;
            }

            valuePatterns.add(textMatch(textFilters) //
                    .filter(builder.equalsPattern(VAR_MATCH_TERM, value)));
        }

        if (valuePatterns.isEmpty()) {
            builder.noResult();
        }

        builder.addPattern(PRIMARY, GraphPatterns.and( //
                builder.bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY), //
                union(valuePatterns.toArray(GraphPattern[]::new))));
    }

    @Override
    public void withLabelContainingAnyOf(String... aValues)
    {
        builder.addPrefix(PREFIX_MARKLOGIC_CTS);

        var valuePatterns = new ArrayList<GraphPattern>();
        for (var value : aValues) {
            var sanitizedValue = builder.sanitizeQueryString_FTS(value);

            if (isBlank(sanitizedValue)) {
                continue;
            }

            var textFilters = toContainsFilters(sanitizedValue, MatchMode.SUBSTRING);

            if (textFilters.isEmpty()) {
                continue;
            }

            valuePatterns.add(textMatch(textFilters) //
                    .filter(builder.containsPattern(VAR_MATCH_TERM, value)));
        }

        if (valuePatterns.isEmpty()) {
            builder.noResult();
        }

        builder.addPattern(PRIMARY, GraphPatterns.and( //
                builder.bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY), //
                union(valuePatterns.toArray(GraphPattern[]::new))));
    }

    @Override
    public void withLabelStartingWith(String aPrefixQuery)
    {
        builder.addPrefix(PREFIX_MARKLOGIC_CTS);

        var sanitizedValue = builder.sanitizeQueryString_FTS(aPrefixQuery);

        if (isBlank(sanitizedValue)) {
            builder.noResult();
            return;
        }

        // If the query string entered by the user does not end with a space character, then we
        // assume that the user may not yet have finished writing the word and turn the last token
        // into a prefix match.
        var mode = aPrefixQuery.endsWith(" ") ? MatchMode.WHOLE_WORD : MatchMode.PREFIX_LAST;
        var textFilters = toContainsFilters(sanitizedValue, mode);

        if (textFilters.isEmpty()) {
            builder.noResult();
            return;
        }

        // Locate all entries where the label contains the prefix (using the FTS) and then filter
        // them by those which actually start with the prefix.
        builder.addPattern(PRIMARY, GraphPatterns.and( //
                builder.bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY), //
                textMatch(textFilters)
                        .filter(builder.startsWithPattern(VAR_MATCH_TERM, aPrefixQuery))));
    }

    @Override
    public void withLabelMatchingAnyOf(String... aValues)
    {
        builder.addPrefix(PREFIX_MARKLOGIC_CTS);

        var valuePatterns = new ArrayList<GraphPattern>();
        for (var value : aValues) {
            var sanitizedValue = builder.sanitizeQueryString_FTS(value);

            if (isBlank(sanitizedValue)) {
                continue;
            }

            // If the query string entered by the user does not end with a space character, then we
            // assume that the user may not yet have finished writing the word and turn the last
            // token into a prefix match.
            var mode = value.endsWith(" ") ? MatchMode.WHOLE_WORD : MatchMode.PREFIX_LAST;
            var textFilters = toContainsFilters(sanitizedValue, mode);

            if (textFilters.isEmpty()) {
                continue;
            }

            valuePatterns.add(textMatch(textFilters));
        }

        if (valuePatterns.isEmpty()) {
            builder.noResult();
        }

        builder.addPattern(PRIMARY, GraphPatterns.and( //
                builder.bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY), //
                union(valuePatterns.toArray(GraphPattern[]::new))));
    }

    /**
     * Builds the {@code ?subj ?pMatch ?m . FILTER(cts:contains(...))...} core that binds the
     * subject via the MarkLogic text index. One {@code cts:contains} filter is emitted per search
     * token, so all of them have to match the same literal.
     */
    private GraphPattern textMatch(List<Expression<?>> aTextFilters)
    {
        return VAR_SUBJECT.has(VAR_MATCH_TERM_PROPERTY, VAR_MATCH_TERM) //
                .filter(and(aTextFilters.toArray(Expression[]::new)));
    }

    /**
     * Turns a sanitized query string into one {@code cts:contains(?m, cts:word-query(...))} filter
     * per token, with wildcards applied according to {@code aMode}.
     */
    private List<Expression<?>> toContainsFilters(String aSanitizedQuery, MatchMode aMode)
    {
        var tokens = aSanitizedQuery.split(" ");
        var filters = new ArrayList<Expression<?>>();
        for (var i = 0; i < tokens.length; i++) {
            var token = tokens[i];

            if (isBlank(token)) {
                continue;
            }

            switch (aMode) {
            case WHOLE_WORD:
                filters.add(containsWord(token, false));
                break;
            case SUBSTRING:
                filters.add(containsWord("*" + token + "*", true));
                break;
            case PREFIX_LAST:
                if (i == tokens.length - 1) {
                    filters.add(containsWord(token + "*", true));
                }
                else {
                    filters.add(containsWord(token, false));
                }
                break;
            }
        }

        return filters;
    }

    private static Expression<?> containsWord(String aWord, boolean aWildcarded)
    {
        var wordQuery = aWildcarded //
                ? custom(CTS_WORD_QUERY, literalOf(aWord), literalOf(WILDCARDED)) //
                : custom(CTS_WORD_QUERY, literalOf(aWord));
        return custom(CTS_CONTAINS, VAR_MATCH_TERM, wordQuery);
    }
}

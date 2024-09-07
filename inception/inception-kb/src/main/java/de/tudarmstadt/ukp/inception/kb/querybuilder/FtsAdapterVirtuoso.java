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

import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilder.Priority.PRIMARY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.and;
import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.union;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.literalOf;

import java.util.ArrayList;

import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;

public class FtsAdapterVirtuoso
    implements FtsAdapter
{
    private final SPARQLQueryBuilder builder;

    public FtsAdapterVirtuoso(SPARQLQueryBuilder aBuilder)
    {
        builder = aBuilder;
    }

    @Override
    public void withLabelMatchingExactlyAnyOf(String... aValues)
    {
        // addPrefix(PREFIX_VIRTUOSO_SEARCH);

        var valuePatterns = new ArrayList<GraphPattern>();
        for (var value : aValues) {
            var sanitizedValue = builder.sanitizeQueryString_FTS(value);

            if (isBlank(sanitizedValue)) {
                continue;
            }

            valuePatterns.add(VAR_SUBJECT.has(VAR_MATCH_TERM_PROPERTY, VAR_MATCH_TERM)
                    .and(VAR_MATCH_TERM.has(VIRTUOSO_QUERY,
                            literalOf("\"" + sanitizedValue + "\"")))
                    .filter(builder.equalsPattern(VAR_MATCH_TERM, value,
                            builder.getKnowledgeBase())));
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
        // addPrefix(PREFIX_VIRTUOSO_SEARCH);

        var valuePatterns = new ArrayList<GraphPattern>();
        for (var value : aValues) {
            var sanitizedValue = builder.sanitizeQueryString_FTS(value);

            if (isBlank(sanitizedValue)) {
                continue;
            }

            valuePatterns.add(VAR_SUBJECT.has(VAR_MATCH_TERM_PROPERTY, VAR_MATCH_TERM)
                    .and(VAR_MATCH_TERM.has(VIRTUOSO_QUERY,
                            literalOf("\"" + sanitizedValue + "\"")))
                    .filter(builder.containsPattern(VAR_MATCH_TERM, value)));
        }

        if (valuePatterns.isEmpty()) {
            builder.noResult();
        }

        builder.addPattern(PRIMARY, and( //
                builder.bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY), //
                union(valuePatterns.toArray(GraphPattern[]::new))));
    }

    @Override
    public void withLabelStartingWith(String aPrefixQuery)
    {
        // addPrefix(PREFIX_VIRTUOSO_SEARCH);

        // Strip single quotes and asterisks because they have special semantics
        var queryString = builder.sanitizeQueryString_FTS(aPrefixQuery);

        // If the query string entered by the user does not end with a space character, then
        // we assume that the user may not yet have finished writing the word and add a
        // wildcard
        if (!aPrefixQuery.endsWith(" ")) {
            queryString = virtuosoStartsWithQuery(queryString);
        }

        // If the query string was reduced to nothing, then the query should always return an
        // empty
        // result.
        if (queryString.length() == 2) {
            builder.noResult();
        }

        // Locate all entries where the label contains the prefix (using the FTS) and then
        // filter them by those which actually start with the prefix.
        builder.addPattern(PRIMARY, and( //
                builder.bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY),
                VAR_SUBJECT.has(VAR_MATCH_TERM_PROPERTY, VAR_MATCH_TERM)
                        .and(VAR_MATCH_TERM.has(VIRTUOSO_QUERY,
                                literalOf("\"" + queryString + "\"")))
                        .filter(builder.startsWithPattern(VAR_MATCH_TERM, aPrefixQuery))));
    }

    private static String virtuosoStartsWithQuery(String sanitizedQuery)
    {
        var ftsQueryString = new StringBuilder();
        var queryTokens = sanitizedQuery.split(" ");

        for (var i = 0; i < queryTokens.length; i++) {
            if (i > 0) {
                ftsQueryString.append(" ");
            }

            // Virtuoso requires that a token has at least 4 characters before it can be
            // used with a wildcard. If the last token has less than 4 characters, we simply
            // drop it to avoid the user hitting a point where the auto-suggesions suddenly
            // are empty. If the token 4 or more, we add the wildcard.
            if (i == (queryTokens.length - 1)) {
                if (queryTokens[i].length() >= 4) {
                    ftsQueryString.append(queryTokens[i]);
                    ftsQueryString.append("*");
                }
            }
            else {
                ftsQueryString.append(queryTokens[i]);
            }
        }

        return ftsQueryString.toString();
    }

    @Override
    public void withLabelMatchingAnyOf(String... aValues)
    {
        // addPrefix(PREFIX_VIRTUOSO_SEARCH);

        var valuePatterns = new ArrayList<GraphPattern>();
        for (var value : aValues) {
            var sanitizedValue = builder.sanitizeQueryString_FTS(value);

            if (isBlank(sanitizedValue)) {
                continue;
            }

            // If the query string entered by the user does not end with a space character, then
            // we assume that the user may not yet have finished writing the word and add a
            // wildcard
            if (!value.endsWith(" ")) {
                sanitizedValue = FtsAdapterVirtuoso.virtuosoStartsWithQuery(sanitizedValue);
            }

            valuePatterns.add(VAR_SUBJECT.has(VAR_MATCH_TERM_PROPERTY, VAR_MATCH_TERM).and(
                    VAR_MATCH_TERM.has(VIRTUOSO_QUERY, literalOf("\"" + sanitizedValue + "\""))));
        }

        if (valuePatterns.isEmpty()) {
            builder.noResult();
        }

        builder.addPattern(PRIMARY, and( //
                builder.bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY),
                union(valuePatterns.toArray(GraphPattern[]::new))));
    }
}

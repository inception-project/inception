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

import java.util.ArrayList;

import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;

public class FtsAdapterStardog
    implements FtsAdapter
{
    private final SPARQLQueryBuilder builder;

    public FtsAdapterStardog(SPARQLQueryBuilder aBuilder)
    {
        builder = aBuilder;
    }

    @Override
    public void withLabelMatchingExactlyAnyOf(String... aValues)
    {
        builder.addPrefix(PREFIX_STARDOG_SEARCH);

        var valuePatterns = new ArrayList<GraphPattern>();
        for (var value : aValues) {
            var sanitizedValue = SPARQLQueryBuilder.sanitizeQueryString_FTS(value);

            if (isBlank(sanitizedValue)) {
                continue;
            }

            valuePatterns.add(new StardogEntitySearchService(VAR_MATCH_TERM, sanitizedValue) //
                    .withLimit(builder.getLimit()) //
                    .and(VAR_SUBJECT.has(VAR_MATCH_TERM_PROPERTY, VAR_MATCH_TERM).filter(builder
                            .equalsPattern(VAR_MATCH_TERM, value, builder.getKnowledgeBase()))));
        }

        builder.addPattern(PRIMARY, and( //
                builder.bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY), //
                union(valuePatterns.toArray(GraphPattern[]::new))));
    }

    @Override
    public void withLabelContainingAnyOf(String... aValues)
    {
        builder.addPrefix(PREFIX_STARDOG_SEARCH);

        var valuePatterns = new ArrayList<GraphPattern>();
        for (var value : aValues) {
            var sanitizedValue = SPARQLQueryBuilder.sanitizeQueryString_FTS(value);

            if (isBlank(sanitizedValue)) {
                continue;
            }

            valuePatterns.add(new StardogEntitySearchService(VAR_MATCH_TERM, sanitizedValue)
                    .withLimit(builder.getLimit()) //
                    .and(VAR_SUBJECT.has(VAR_MATCH_TERM_PROPERTY, VAR_MATCH_TERM)
                            .filter(builder.containsPattern(VAR_MATCH_TERM, value))));
        }

        builder.addPattern(PRIMARY, and( //
                builder.bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY), //
                union(valuePatterns.toArray(GraphPattern[]::new))));
    }

    @Override
    public void withLabelStartingWith(String aPrefixQuery)
    {
        builder.addPrefix(PREFIX_STARDOG_SEARCH);

        // Strip single quotes and asterisks because they have special semantics
        var sanitizedValue = SPARQLQueryBuilder.sanitizeQueryString_FTS(aPrefixQuery);

        if (isBlank(sanitizedValue)) {
            builder.setReturnEmptyResult(true);
        }

        var queryString = sanitizedValue.trim();

        // If the query string entered by the user does not end with a space character, then
        // we assume that the user may not yet have finished writing the word and add a
        // wildcard
        if (!aPrefixQuery.endsWith(" ")) {
            queryString += "*";
        }

        builder.addPattern(PRIMARY, and( //
                builder.bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY),
                new StardogEntitySearchService(VAR_MATCH_TERM, queryString) //
                        .withLimit(builder.getLimit()) //
                        .and(VAR_SUBJECT.has(VAR_MATCH_TERM_PROPERTY, VAR_MATCH_TERM)
                                .filter(builder.startsWithPattern(VAR_MATCH_TERM, aPrefixQuery)))));
    }

    @Override
    public void withLabelMatchingAnyOf(String... aValues)
    {
        builder.addPrefix(PREFIX_STARDOG_SEARCH);

        var valuePatterns = new ArrayList<GraphPattern>();
        for (var value : aValues) {
            var sanitizedValue = SPARQLQueryBuilder.sanitizeQueryString_FTS(value);
            var fuzzyQuery = SPARQLQueryBuilder.convertToFuzzyMatchingQuery(sanitizedValue, "~");

            if (isBlank(sanitizedValue) || isBlank(fuzzyQuery)) {
                continue;
            }

            valuePatterns.add(new StardogEntitySearchService(VAR_MATCH_TERM, fuzzyQuery) //
                    .withLimit(builder.getLimit()) //
                    .and(VAR_SUBJECT.has(VAR_MATCH_TERM_PROPERTY, VAR_MATCH_TERM)));
        }

        builder.addPattern(PRIMARY, and( //
                builder.bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY), //
                union(valuePatterns.toArray(GraphPattern[]::new))));
    }
}

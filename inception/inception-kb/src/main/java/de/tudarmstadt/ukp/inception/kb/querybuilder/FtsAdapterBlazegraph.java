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

public class FtsAdapterBlazegraph
    implements FtsAdapter
{
    private final SPARQLQueryBuilder builder;

    public FtsAdapterBlazegraph(SPARQLQueryBuilder aBuilder)
    {
        builder = aBuilder;
        builder.addPrefix(SPARQLQueryBuilder.PREFIX_BLAZEGRAPH_SEARCH);
    }

    @Override
    public void withLabelMatchingExactlyAnyOf(String[] aValues)
    {
        var kb = builder.getKnowledgeBase();

        var valuePatterns = new ArrayList<GraphPattern>();
        for (var value : aValues) {
            var sanitizedValue = SPARQLQueryBuilder.sanitizeQueryString_FTS(value);

            // We assume that the FTS is case insensitive and found that some FTSes (i.e.
            // Fuseki) can have trouble matching if they get upper-case query when they
            // internally lower-case#
            if (builder.isCaseInsensitive()) {
                sanitizedValue = SPARQLQueryBuilder.toLowerCase(kb, sanitizedValue);
            }

            if (isBlank(sanitizedValue)) {
                continue;
            }

            builder.addProjection(SPARQLQueryBuilder.VAR_SCORE);

            valuePatterns.add(new BlazegraphFtsQuery(SPARQLQueryBuilder.VAR_SUBJECT,
                    SPARQLQueryBuilder.VAR_SCORE, SPARQLQueryBuilder.VAR_MATCH_TERM,
                    SPARQLQueryBuilder.VAR_MATCH_TERM_PROPERTY, sanitizedValue) //
                            .withLimit(builder.getLimit()) //
                            .filter(builder.equalsPattern(SPARQLQueryBuilder.VAR_MATCH_TERM, value,
                                    kb)));
        }

        builder.addPattern(PRIMARY, and( //
                builder.bindMatchTermProperties(SPARQLQueryBuilder.VAR_MATCH_TERM_PROPERTY), //
                union(valuePatterns.toArray(GraphPattern[]::new))));
    }

    @Override
    public void withLabelContainingAnyOf(String... aValues)
    {
        var kb = builder.getKnowledgeBase();

        var valuePatterns = new ArrayList<GraphPattern>();
        for (var value : aValues) {
            var sanitizedValue = SPARQLQueryBuilder.sanitizeQueryString_FTS(value);

            // We assume that the FTS is case insensitive and found that some FTSes (i.e.
            // Fuseki) can have trouble matching if they get upper-case query when they
            // internally lower-case#
            if (builder.isCaseInsensitive()) {
                sanitizedValue = SPARQLQueryBuilder.toLowerCase(kb, sanitizedValue);
            }

            if (isBlank(sanitizedValue)) {
                continue;
            }

            builder.addProjection(SPARQLQueryBuilder.VAR_SCORE);

            valuePatterns.add(new BlazegraphFtsQuery(SPARQLQueryBuilder.VAR_SUBJECT,
                    SPARQLQueryBuilder.VAR_SCORE, SPARQLQueryBuilder.VAR_MATCH_TERM,
                    SPARQLQueryBuilder.VAR_MATCH_TERM_PROPERTY, sanitizedValue) //
                            .withLimit(builder.getLimit()) //
                            .filter(builder.containsPattern(SPARQLQueryBuilder.VAR_MATCH_TERM,
                                    value)));
        }

        builder.addPattern(PRIMARY,
                and(builder.bindMatchTermProperties(SPARQLQueryBuilder.VAR_MATCH_TERM_PROPERTY),
                        union(valuePatterns.toArray(GraphPattern[]::new))));
    }

    @Override
    public void withLabelStartingWith(String aPrefixQuery)
    {
        var kb = builder.getKnowledgeBase();

        var queryString = aPrefixQuery.trim();

        // We assume that the FTS is case insensitive and found that some FTSes (i.e.
        // Fuseki) can have trouble matching if they get upper-case query when they
        // internally lower-case#
        if (builder.isCaseInsensitive()) {
            queryString = SPARQLQueryBuilder.toLowerCase(kb, queryString);
        }

        if (queryString.isEmpty()) {
            builder.setReturnEmptyResult(true);
        }

        // If the query string entered by the user does not end with a space character, then
        // we assume that the user may not yet have finished writing the word and add a
        // wildcard
        if (!aPrefixQuery.endsWith(" ")) {
            queryString += "*";
        }

        builder.addProjection(SPARQLQueryBuilder.VAR_SCORE);

        // Locate all entries where the label contains the prefix (using the FTS) and then
        // filter them by those which actually start with the prefix.
        builder.addPattern(PRIMARY, and( //
                builder.bindMatchTermProperties(SPARQLQueryBuilder.VAR_MATCH_TERM_PROPERTY), //
                new BlazegraphFtsQuery(SPARQLQueryBuilder.VAR_SUBJECT, SPARQLQueryBuilder.VAR_SCORE,
                        SPARQLQueryBuilder.VAR_MATCH_TERM,
                        SPARQLQueryBuilder.VAR_MATCH_TERM_PROPERTY, queryString) //
                                .withLimit(builder.getLimit()) //
                                .filter(builder.startsWithPattern(SPARQLQueryBuilder.VAR_MATCH_TERM,
                                        aPrefixQuery))));
    }

    @Override
    public void withLabelMatchingAnyOf(String... aValues)
    {
        var kb = builder.getKnowledgeBase();

        var valuePatterns = new ArrayList<GraphPattern>();
        for (var value : aValues) {
            var sanitizedValue = SPARQLQueryBuilder.sanitizeQueryString_FTS(value);

            if (isBlank(sanitizedValue)) {
                continue;
            }

            // We assume that the FTS is case insensitive and found that some FTSes (i.e.
            // Fuseki) can have trouble matching if they get upper-case query when they
            // internally lower-case#
            if (builder.isCaseInsensitive()) {
                sanitizedValue = SPARQLQueryBuilder.toLowerCase(kb, sanitizedValue);
            }

            var fuzzyQuery = SPARQLQueryBuilder.convertToFuzzyMatchingQuery(sanitizedValue, "*");

            if (isBlank(fuzzyQuery)) {
                continue;
            }

            builder.addProjection(SPARQLQueryBuilder.VAR_SCORE);

            valuePatterns.add(new BlazegraphFtsQuery(SPARQLQueryBuilder.VAR_SUBJECT,
                    SPARQLQueryBuilder.VAR_SCORE, SPARQLQueryBuilder.VAR_MATCH_TERM,
                    SPARQLQueryBuilder.VAR_MATCH_TERM_PROPERTY, fuzzyQuery)
                            .withLimit(builder.getLimit()));
        }

        builder.addPattern(PRIMARY,
                and(builder.bindMatchTermProperties(SPARQLQueryBuilder.VAR_MATCH_TERM_PROPERTY),
                        union(valuePatterns.toArray(GraphPattern[]::new))));
    }
}

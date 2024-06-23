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

import static de.tudarmstadt.ukp.inception.kb.IriConstants.PREFIX_BLAZEGRAPH;
import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilder.convertToRequiredTokenPrefixMatchingQuery;
import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilder.Priority.PRIMARY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.and;
import static org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder.prefix;
import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.and;
import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.union;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import java.util.ArrayList;

import org.eclipse.rdf4j.sparqlbuilder.constraint.Expression;
import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;

public class FtsAdapterBlazegraph
    implements FtsAdapter
{
    private static final String MULTI_CHAR_WILDCARD = "*";

    private static final Prefix PREFIX_BLAZEGRAPH_SEARCH = prefix("bds", iri(PREFIX_BLAZEGRAPH));

    private final SPARQLQueryBuilder builder;

    public FtsAdapterBlazegraph(SPARQLQueryBuilder aBuilder)
    {
        builder = aBuilder;
        builder.addPrefix(PREFIX_BLAZEGRAPH_SEARCH);
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

            builder.addProjection(SPARQLQueryBuilder.VAR_SCORE);

            valuePatterns.add(new BlazegraphFtsQuery(SPARQLQueryBuilder.VAR_SUBJECT,
                    SPARQLQueryBuilder.VAR_SCORE, SPARQLQueryBuilder.VAR_MATCH_TERM,
                    SPARQLQueryBuilder.VAR_MATCH_TERM_PROPERTY, sanitizedValue) //
                            .withLimit(builder.getLimit()) //
                            .filter(builder.equalsPattern(SPARQLQueryBuilder.VAR_MATCH_TERM, value,
                                    kb)));
        }

        if (valuePatterns.isEmpty()) {
            builder.noResult();
        }

        builder.addPattern(PRIMARY, and( //
                builder.bindMatchTermProperties(SPARQLQueryBuilder.VAR_MATCH_TERM_PROPERTY), //
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

            builder.addProjection(SPARQLQueryBuilder.VAR_SCORE);

            valuePatterns.add(new BlazegraphFtsQuery(SPARQLQueryBuilder.VAR_SUBJECT,
                    SPARQLQueryBuilder.VAR_SCORE, SPARQLQueryBuilder.VAR_MATCH_TERM,
                    SPARQLQueryBuilder.VAR_MATCH_TERM_PROPERTY, sanitizedValue) //
                            .withLimit(builder.getLimit()) //
                            .filter(builder.containsPattern(SPARQLQueryBuilder.VAR_MATCH_TERM,
                                    value)));
        }

        if (valuePatterns.isEmpty()) {
            builder.noResult();
        }

        builder.addPattern(PRIMARY,
                and(builder.bindMatchTermProperties(SPARQLQueryBuilder.VAR_MATCH_TERM_PROPERTY),
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

            builder.addProjection(SPARQLQueryBuilder.VAR_SCORE);

            var labelFilterExpressions = new ArrayList<Expression<?>>();
            labelFilterExpressions.add(builder.matchKbLanguage(VAR_MATCH_TERM));

            valuePatterns.add(new BlazegraphFtsQuery(SPARQLQueryBuilder.VAR_SUBJECT,
                    SPARQLQueryBuilder.VAR_SCORE, SPARQLQueryBuilder.VAR_MATCH_TERM,
                    SPARQLQueryBuilder.VAR_MATCH_TERM_PROPERTY, fuzzyQuery) //
                            .withLimit(builder.getLimit()) //
                            .filter(and(labelFilterExpressions.toArray(Expression[]::new))));
        }

        if (valuePatterns.isEmpty()) {
            builder.noResult();
        }

        builder.addPattern(PRIMARY,
                and(builder.bindMatchTermProperties(SPARQLQueryBuilder.VAR_MATCH_TERM_PROPERTY),
                        union(valuePatterns.toArray(GraphPattern[]::new))));
    }
}

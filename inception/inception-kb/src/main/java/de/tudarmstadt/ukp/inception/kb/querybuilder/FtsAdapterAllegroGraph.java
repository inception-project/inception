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
import static org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder.prefix;
import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.and;
import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.union;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import java.util.ArrayList;

import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;

import de.tudarmstadt.ukp.inception.kb.IriConstants;

public class FtsAdapterAllegroGraph
    implements FtsAdapter
{
    private static final Prefix PREFIX_ALLEGRO_GRAPH_FTI = prefix("fti",
            iri(IriConstants.PREFIX_ALLEGRO_GRAPH_FTI));
    private static final Iri ALLEGRO_GRAPH_MATCH = PREFIX_ALLEGRO_GRAPH_FTI.iri("match");

    private final SPARQLQueryBuilder builder;

    public FtsAdapterAllegroGraph(SPARQLQueryBuilder aBuilder)
    {
        builder = aBuilder;
    }

    @Override
    public void withLabelMatchingExactlyAnyOf(String... aValues)
    {
        builder.addPrefix(PREFIX_ALLEGRO_GRAPH_FTI);

        var valuePatterns = new ArrayList<GraphPattern>();
        for (var value : aValues) {
            var sanitizedValue = SPARQLQueryBuilder.sanitizeQueryString_FTS(value);

            // We assume that the FTS is case insensitive and found that some FTSes (i.e.
            // Fuseki) can have trouble matching if they get upper-case query when they
            // internally lower-case#
            if (builder.isCaseInsensitive()) {
                sanitizedValue = SPARQLQueryBuilder.toLowerCase(builder.getKnowledgeBase(),
                        sanitizedValue);
            }

            if (isBlank(sanitizedValue)) {
                continue;
            }

            builder.addProjection(VAR_SCORE);

            valuePatterns.add(new AllegroGraphFtsQuery(VAR_SUBJECT, VAR_SCORE, VAR_MATCH_TERM,
                    VAR_MATCH_TERM_PROPERTY, sanitizedValue) //
                            .filter(builder.equalsPattern(VAR_MATCH_TERM, value,
                                    builder.getKnowledgeBase())));
        }

        builder.addPattern(PRIMARY, and( //
                builder.bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY), //
                union(valuePatterns.toArray(GraphPattern[]::new))));
    }

    @Override
    public void withLabelContainingAnyOf(String... aValues)
    {
        builder.addPrefix(PREFIX_ALLEGRO_GRAPH_FTI);

        var valuePatterns = new ArrayList<GraphPattern>();
        for (var value : aValues) {
            var sanitizedValue = SPARQLQueryBuilder.sanitizeQueryString_FTS(value);

            // We assume that the FTS is case insensitive and found that some FTSes (i.e.
            // Fuseki) can have trouble matching if they get upper-case query when they
            // internally lower-case#
            if (builder.isCaseInsensitive()) {
                sanitizedValue = SPARQLQueryBuilder.toLowerCase(builder.getKnowledgeBase(),
                        sanitizedValue);
            }

            if (isBlank(sanitizedValue)) {
                continue;
            }

            builder.addProjection(VAR_SCORE);

            valuePatterns.add(new AllegroGraphFtsQuery(VAR_SUBJECT, VAR_SCORE, VAR_MATCH_TERM,
                    VAR_MATCH_TERM_PROPERTY, sanitizedValue) //
                            .filter(builder.containsPattern(VAR_MATCH_TERM, value)));
        }

        builder.addPattern(PRIMARY, and(builder.bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY),
                union(valuePatterns.toArray(GraphPattern[]::new))));
    }

    @Override
    public void withLabelStartingWith(String aPrefixQuery)
    {
        builder.addPrefix(PREFIX_ALLEGRO_GRAPH_FTI);

        var queryString = aPrefixQuery.trim();

        // We assume that the FTS is case insensitive and found that some FTSes (i.e.
        // Fuseki) can have trouble matching if they get upper-case query when they
        // internally lower-case#
        if (builder.isCaseInsensitive()) {
            queryString = SPARQLQueryBuilder.toLowerCase(builder.getKnowledgeBase(), queryString);
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

        builder.addProjection(VAR_SCORE);

        // Locate all entries where the label contains the prefix (using the FTS) and then
        // filter them by those which actually start with the prefix.
        builder.addPattern(PRIMARY, and( //
                builder.bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY), //
                new AllegroGraphFtsQuery(VAR_SUBJECT, VAR_SCORE, VAR_MATCH_TERM,
                        VAR_MATCH_TERM_PROPERTY, queryString) //
                                .filter(builder.startsWithPattern(VAR_MATCH_TERM, aPrefixQuery))));
    }

    @Override
    public void withLabelMatchingAnyOf(String... aValues)
    {
        builder.addPrefix(PREFIX_ALLEGRO_GRAPH_FTI);

        var valuePatterns = new ArrayList<GraphPattern>();
        for (var value : aValues) {
            var sanitizedValue = SPARQLQueryBuilder.sanitizeQueryString_FTS(value);

            if (isBlank(sanitizedValue)) {
                continue;
            }

            // We assume that the FTS is case insensitive and found that some FTSes (i.e.
            // Fuseki) can have trouble matching if they get upper-case query when they
            // internally lower-case
            if (builder.isCaseInsensitive()) {
                sanitizedValue = SPARQLQueryBuilder.toLowerCase(builder.getKnowledgeBase(),
                        sanitizedValue);
            }

            var fuzzyQuery = SPARQLQueryBuilder.convertToFuzzyMatchingQuery(sanitizedValue, "*");

            if (isBlank(fuzzyQuery)) {
                continue;
            }

            builder.addProjection(VAR_SCORE);

            valuePatterns.add(new AllegroGraphFtsQuery(VAR_SUBJECT, VAR_SCORE, VAR_MATCH_TERM,
                    VAR_MATCH_TERM_PROPERTY, fuzzyQuery));
        }

        builder.addPattern(PRIMARY, and(builder.bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY),
                union(valuePatterns.toArray(GraphPattern[]::new))));
    }
}

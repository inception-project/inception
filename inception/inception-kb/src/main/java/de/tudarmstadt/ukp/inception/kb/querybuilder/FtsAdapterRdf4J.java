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

import static de.tudarmstadt.ukp.inception.kb.IriConstants.FTS_RDF4J_LUCENE;
import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilder.Priority.PRIMARY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.and;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.bind;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.function;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.str;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction.REPLACE;
import static org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder.prefix;
import static org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder.var;
import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.and;
import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.union;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.bNode;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.literalOf;

import java.util.ArrayList;

import org.eclipse.rdf4j.sparqlbuilder.constraint.Expression;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;

import de.tudarmstadt.ukp.inception.kb.IriConstants;

public class FtsAdapterRdf4J
    implements FtsAdapter
{
    private static final Prefix PREFIX_RDF4J_LUCENE_SEARCH = prefix("search",
            iri(IriConstants.PREFIX_RDF4J_LUCENE_SEARCH));
    private static final Iri LUCENE_QUERY = PREFIX_RDF4J_LUCENE_SEARCH.iri("query");
    private static final Iri LUCENE_PROPERTY = PREFIX_RDF4J_LUCENE_SEARCH.iri("property");
    private static final Iri LUCENE_SCORE = PREFIX_RDF4J_LUCENE_SEARCH.iri("score");
    private static final Iri LUCENE_SNIPPET = PREFIX_RDF4J_LUCENE_SEARCH.iri("snippet");

    private final SPARQLQueryBuilder builder;

    public FtsAdapterRdf4J(SPARQLQueryBuilder aBuilder)
    {
        builder = aBuilder;
    }

    @Override
    public void withLabelMatchingExactlyAnyOf(String... aValues)
    {
        builder.addPrefix(PREFIX_RDF4J_LUCENE_SEARCH);

        var valuePatterns = new ArrayList<GraphPattern>();
        for (var value : aValues) {
            // Strip single quotes and asterisks because they have special semantics
            var sanitizedValue = SPARQLQueryBuilder.sanitizeQueryString_FTS(value);

            if (isBlank(sanitizedValue)) {
                continue;
            }

            builder.addProjection(VAR_SCORE);

            valuePatterns.add(VAR_SUBJECT
                    .has(FTS_RDF4J_LUCENE, bNode(LUCENE_QUERY, literalOf(sanitizedValue)) //
                            .andHas(LUCENE_PROPERTY, VAR_MATCH_TERM_PROPERTY) //
                            .andHas(LUCENE_SCORE, VAR_SCORE))
                    .andHas(VAR_MATCH_TERM_PROPERTY, VAR_MATCH_TERM).filter(builder
                            .equalsPattern(VAR_MATCH_TERM, value, builder.getKnowledgeBase())));
        }

        builder.addPattern(PRIMARY, and( //
                builder.bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY), //
                union(valuePatterns.toArray(GraphPattern[]::new))));
    }

    @Override
    public void withLabelContainingAnyOf(String... aValues)
    {
        builder.addPrefix(PREFIX_RDF4J_LUCENE_SEARCH);

        var valuePatterns = new ArrayList<GraphPattern>();
        for (var value : aValues) {
            var sanitizedValue = SPARQLQueryBuilder.sanitizeQueryString_FTS(value);

            if (isBlank(sanitizedValue)) {
                continue;
            }

            builder.addProjection(VAR_SCORE);

            valuePatterns
                    .add(VAR_SUBJECT
                            .has(FTS_RDF4J_LUCENE,
                                    bNode(LUCENE_QUERY, literalOf(sanitizedValue + "*")) //
                                            .andHas(LUCENE_PROPERTY, VAR_MATCH_TERM_PROPERTY) //
                                            .andHas(LUCENE_SCORE, VAR_SCORE))
                            .andHas(VAR_MATCH_TERM_PROPERTY, VAR_MATCH_TERM)
                            .filter(builder.containsPattern(VAR_MATCH_TERM, value)));
        }

        builder.addPattern(PRIMARY, and(builder.bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY),
                union(valuePatterns.toArray(GraphPattern[]::new))));
    }

    @Override
    public void withLabelStartingWith(String aPrefixQuery)
    {
        builder.addPrefix(PREFIX_RDF4J_LUCENE_SEARCH);

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

        builder.addProjection(VAR_SCORE);

        // Locate all entries where the label contains the prefix (using the FTS) and then
        // filter them by those which actually start with the prefix.
        builder.addPattern(PRIMARY, and( //
                builder.bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY), //
                VAR_SUBJECT.has(FTS_RDF4J_LUCENE, bNode(LUCENE_QUERY, literalOf(queryString)) //
                        .andHas(LUCENE_SCORE, VAR_SCORE)
                        .andHas(LUCENE_PROPERTY, VAR_MATCH_TERM_PROPERTY))
                        .andHas(VAR_MATCH_TERM_PROPERTY, VAR_MATCH_TERM)
                        .filter(builder.startsWithPattern(VAR_MATCH_TERM, aPrefixQuery))));
    }

    @Override
    public void withLabelMatchingAnyOf(String... aValues)
    {
        builder.addPrefix(PREFIX_RDF4J_LUCENE_SEARCH);

        var valuePatterns = new ArrayList<GraphPattern>();
        for (var value : aValues) {
            // Strip single quotes and asterisks because they have special semantics
            var sanitizedValue = SPARQLQueryBuilder.sanitizeQueryString_FTS(value);

            var fuzzyQuery = SPARQLQueryBuilder.convertToFuzzyMatchingQuery(sanitizedValue, "~");

            if (isBlank(sanitizedValue) || isBlank(fuzzyQuery)) {
                continue;
            }

            var labelFilterExpressions = new ArrayList<Expression<?>>();
            labelFilterExpressions.add(Expressions.equals(str(var("label")), str(VAR_MATCH_TERM)));
            labelFilterExpressions.add(builder.matchKbLanguage(VAR_MATCH_TERM));

            builder.addProjection(VAR_SCORE);

            // If a KB item has multiple labels, we want to return only the ones which actually
            // match the query term such that the user is not confused that the results contain
            // items that don't match the query (even though they do through a label that is not
            // returned). RDF4J only provides access to the matched term in a "highlighed" form
            // where "<B>" and "</B>" match the search term. So we have to strip these markers
            // out as part of the query.
            valuePatterns.add(VAR_SUBJECT //
                    .has(FTS_RDF4J_LUCENE, bNode(LUCENE_QUERY, literalOf(fuzzyQuery)) //
                            .andHas(LUCENE_PROPERTY, VAR_MATCH_TERM_PROPERTY) //
                            .andHas(LUCENE_SCORE, VAR_SCORE) //
                            .andHas(LUCENE_SNIPPET, var("snippet")))
                    .and(bind(
                            function(REPLACE,
                                    function(REPLACE, var("snippet"), literalOf("</B>"),
                                            literalOf("")),
                                    literalOf("<B>"), literalOf("")),
                            var("label")))
                    .and(VAR_SUBJECT.has(VAR_MATCH_TERM_PROPERTY, VAR_MATCH_TERM))
                    .filter(and(labelFilterExpressions.toArray(Expression[]::new))));
        }

        builder.addPattern(PRIMARY, and( //
                builder.bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY),
                union(valuePatterns.toArray(GraphPattern[]::new))));
    }
}

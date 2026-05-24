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
import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilder.convertToFuzzyMatchingQuery;
import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilder.convertToRequiredTokenPrefixMatchingQuery;
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
import java.util.List;

import org.eclipse.rdf4j.sparqlbuilder.constraint.Expression;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;

import de.tudarmstadt.ukp.inception.kb.IriConstants;

public class FtsAdapterRdf4J
    implements FtsAdapter
{
    private static final String REQUIRED_PREFIX = "+";
    private static final String VAR_LABEL_NAME = "label";
    private static final Variable VAR_LABEL = var(VAR_LABEL_NAME);
    private static final String VAR_SNIPPET_NAME = "snippet";
    private static final Variable VAR_SNIPPET = var(VAR_SNIPPET_NAME);

    private static final Prefix PREFIX_RDF4J_LUCENE_SEARCH = prefix("search",
            iri(IriConstants.PREFIX_RDF4J_LUCENE_SEARCH));
    private static final Iri LUCENE_QUERY = PREFIX_RDF4J_LUCENE_SEARCH.iri("query");
    private static final Iri LUCENE_PROPERTY = PREFIX_RDF4J_LUCENE_SEARCH.iri("property");
    private static final Iri LUCENE_SCORE = PREFIX_RDF4J_LUCENE_SEARCH.iri("score");
    private static final Iri LUCENE_SNIPPET = PREFIX_RDF4J_LUCENE_SEARCH.iri(VAR_SNIPPET_NAME);

    private static final String MULTI_CHAR_WILDCARD = "*";
    private static final String FUZZY_SUFFIX = "~";

    private final SPARQLQueryBuilder builder;

    // Branches are accumulated by withLabel* methods and only assembled into a UNION + PRIMARY
    // pattern in finalizeQuery() — that way the adapter has a chance to push the
    // retrieveLabel/retrieveDescription/retrieveDeprecation OPTIONAL lookups INTO each branch.
    // RDF4J 5.1.4+ (see GH-4872) otherwise plans the trailing OPTIONALs as cartesian-product
    // BadlyDesignedLeftJoinIterators that re-scan large fractions of the store per FTS hit,
    // turning sub-second queries into multi-minute ones on large KBs like SNOMED (#5444).
    private final List<GraphPattern> pendingFtsBranches = new ArrayList<>();
    private boolean ftsActive = false;

    public FtsAdapterRdf4J(SPARQLQueryBuilder aBuilder)
    {
        builder = aBuilder;
    }

    @Override
    public void withLabelMatchingExactlyAnyOf(String... aValues)
    {
        builder.addPrefix(PREFIX_RDF4J_LUCENE_SEARCH);
        ftsActive = true;

        for (var value : aValues) {
            var sanitizedValue = builder.sanitizeQueryString_FTS(value);

            if (isBlank(sanitizedValue)) {
                continue;
            }

            builder.addProjection(VAR_SCORE);

            pendingFtsBranches.add(VAR_SUBJECT
                    .has(FTS_RDF4J_LUCENE, bNode(LUCENE_QUERY, literalOf(sanitizedValue)) //
                            .andHas(LUCENE_PROPERTY, VAR_MATCH_TERM_PROPERTY) //
                            .andHas(LUCENE_SCORE, VAR_SCORE))
                    .andHas(VAR_MATCH_TERM_PROPERTY, VAR_MATCH_TERM).filter(builder
                            .equalsPattern(VAR_MATCH_TERM, value, builder.getKnowledgeBase())));
        }

        if (pendingFtsBranches.isEmpty()) {
            builder.noResult();
        }
    }

    @Override
    public void withLabelContainingAnyOf(String... aValues)
    {
        builder.addPrefix(PREFIX_RDF4J_LUCENE_SEARCH);
        ftsActive = true;

        for (var value : aValues) {
            // Strip single quotes and asterisks because they have special semantics
            var sanitizedValue = builder.sanitizeQueryString_FTS(value);

            var query = convertToRequiredTokenPrefixMatchingQuery(sanitizedValue, REQUIRED_PREFIX,
                    MULTI_CHAR_WILDCARD);

            if (isBlank(query)) {
                continue;
            }

            var labelFilterExpressions = new ArrayList<Expression<?>>();
            labelFilterExpressions.add(Expressions.equals(str(VAR_LABEL), str(VAR_MATCH_TERM)));
            labelFilterExpressions.add(builder.matchKbLanguage(VAR_MATCH_TERM));

            builder.addProjection(VAR_SCORE);

            // If a KB item has multiple labels, we want to return only the ones which actually
            // match the query term such that the user is not confused that the results contain
            // items that don't match the query (even though they do through a label that is not
            // returned). RDF4J only provides access to the matched term in a "highlighted" form
            // where "<B>" and "</B>" match the search term. So we have to strip these markers
            // out as part of the query.
            pendingFtsBranches.add(VAR_SUBJECT //
                    .has(FTS_RDF4J_LUCENE, bNode(LUCENE_QUERY, literalOf(query)) //
                            .andHas(LUCENE_PROPERTY, VAR_MATCH_TERM_PROPERTY) //
                            .andHas(LUCENE_SCORE, VAR_SCORE) //
                            .andHas(LUCENE_SNIPPET, VAR_SNIPPET))
                    .and(bind(
                            function(REPLACE,
                                    function(REPLACE, VAR_SNIPPET, literalOf("</B>"),
                                            literalOf("")),
                                    literalOf("<B>"), literalOf("")),
                            VAR_LABEL))
                    .and(VAR_SUBJECT.has(VAR_MATCH_TERM_PROPERTY, VAR_MATCH_TERM))
                    .filter(and(labelFilterExpressions.toArray(Expression[]::new))));
        }

        if (pendingFtsBranches.isEmpty()) {
            builder.noResult();
        }
    }

    @Override
    public void withLabelStartingWith(String aPrefixQuery)
    {
        builder.addPrefix(PREFIX_RDF4J_LUCENE_SEARCH);
        ftsActive = true;

        // Strip single quotes and asterisks because they have special semantics
        var queryString = builder.sanitizeQueryString_FTS(aPrefixQuery);

        if (isBlank(queryString)) {
            builder.noResult();
            return;
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
        pendingFtsBranches.add(VAR_SUBJECT
                .has(FTS_RDF4J_LUCENE, bNode(LUCENE_QUERY, literalOf(queryString)) //
                        .andHas(LUCENE_SCORE, VAR_SCORE)
                        .andHas(LUCENE_PROPERTY, VAR_MATCH_TERM_PROPERTY))
                .andHas(VAR_MATCH_TERM_PROPERTY, VAR_MATCH_TERM)
                .filter(builder.startsWithPattern(VAR_MATCH_TERM, aPrefixQuery)));
    }

    @Override
    public void withLabelMatchingAnyOf(String... aValues)
    {
        builder.addPrefix(PREFIX_RDF4J_LUCENE_SEARCH);
        ftsActive = true;

        for (var value : aValues) {
            // Strip single quotes and asterisks because they have special semantics
            var sanitizedValue = builder.sanitizeQueryString_FTS(value);

            var queryString = convertToFuzzyMatchingQuery(sanitizedValue, FUZZY_SUFFIX);

            if (isBlank(queryString)) {
                continue;
            }

            var labelFilterExpressions = new ArrayList<Expression<?>>();
            labelFilterExpressions.add(Expressions.equals(str(VAR_LABEL), str(VAR_MATCH_TERM)));
            labelFilterExpressions.add(builder.matchKbLanguage(VAR_MATCH_TERM));

            builder.addProjection(VAR_SCORE);

            // If a KB item has multiple labels, we want to return only the ones which actually
            // match the query term such that the user is not confused that the results contain
            // items that don't match the query (even though they do through a label that is not
            // returned). RDF4J only provides access to the matched term in a "highlighted" form
            // where "<B>" and "</B>" match the search term. So we have to strip these markers
            // out as part of the query.
            pendingFtsBranches.add(VAR_SUBJECT //
                    .has(FTS_RDF4J_LUCENE, bNode(LUCENE_QUERY, literalOf(queryString)) //
                            .andHas(LUCENE_PROPERTY, VAR_MATCH_TERM_PROPERTY) //
                            .andHas(LUCENE_SCORE, VAR_SCORE) //
                            .andHas(LUCENE_SNIPPET, VAR_SNIPPET))
                    .and(bind(
                            function(REPLACE,
                                    function(REPLACE, VAR_SNIPPET, literalOf("</B>"),
                                            literalOf("")),
                                    literalOf("<B>"), literalOf("")),
                            VAR_LABEL))
                    .and(VAR_SUBJECT.has(VAR_MATCH_TERM_PROPERTY, VAR_MATCH_TERM))
                    .filter(and(labelFilterExpressions.toArray(Expression[]::new))));
        }

        if (pendingFtsBranches.isEmpty()) {
            builder.noResult();
        }
    }

    @Override
    public void finalizeQuery(SPARQLQueryBuilder aBuilder)
    {
        if (!ftsActive || pendingFtsBranches.isEmpty()) {
            return;
        }

        // The inner OPTIONALs reference ?pPrefLabel, which is bound at top level by the
        // SECONDARY pattern emitted by retrieveLabel — but as an OPTIONAL { VALUES ... } wrapper.
        // RDF4J's optimizer reorders that OPTIONAL to AFTER the union, leaving ?pPrefLabel
        // unbound when the inner OPTIONALs run; the inner StatementPattern then matches ANY
        // property and we get back altLabel instead of prefLabel. Workaround: emit a hard
        // (non-OPTIONAL) VALUES binding directly inside each UNION branch so ?pPrefLabel is
        // bound before the inner OPTIONALs are evaluated.
        // Drain the SECONDARY bindings (the OPTIONAL { VALUES ?pPrefLabel ... } / bindMatchTerm
        // wrappers added by retrieveLabel). We re-emit equivalent hard bindings inside each
        // branch below, so leaving them in SECONDARY would just be duplicates.
        aBuilder.drainSecondaryPatterns();
        var optionalLookups = aBuilder.drainOptionalLookupPatterns();
        var prefLabelValuesBinding = aBuilder.bindPrefLabelPropertiesPlain(VAR_PREF_LABEL_PROPERTY);

        var augmentedBranches = pendingFtsBranches.stream() //
                .map(branch -> augmentBranch(branch, prefLabelValuesBinding, optionalLookups)) //
                .toArray(GraphPattern[]::new);

        aBuilder.addPattern(PRIMARY, and( //
                aBuilder.bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY), //
                union(augmentedBranches)));
    }

    private static GraphPattern augmentBranch(GraphPattern aBranch, GraphPattern aPrefLabelBinding,
            List<GraphPattern> aOptionalLookups)
    {
        if (aPrefLabelBinding == null && aOptionalLookups.isEmpty()) {
            return aBranch;
        }

        var parts = new ArrayList<GraphPattern>();
        if (aPrefLabelBinding != null) {
            parts.add(aPrefLabelBinding);
        }
        parts.add(aBranch);
        parts.addAll(aOptionalLookups);
        return and(parts.toArray(GraphPattern[]::new));
    }
}

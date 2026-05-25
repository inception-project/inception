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
import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.select;
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
    /**
     * @param body
     *            the FTS body (search:matches + ?subj ?pMatch ?m for the exact-match case;
     *            already-filtered GraphPattern for the other variants)
     * @param filters
     *            filters to apply outside {@code body} so they can be appended to a flat
     *            {@code and(matchTermBinding, body)} group — only populated for the exact-match
     *            path where the inner-StatementPattern cardinality estimate is catastrophic
     * @param wrapInSubSelect
     *            when {@code true}, finalizeQuery wraps the body in a sub-SELECT scope to force the
     *            RDF4J planner away from a 5.5M-row HashJoinIteration. Only used for
     *            {@link #withLabelMatchingExactlyAnyOf} — the other FTS shapes (startsWith,
     *            containing, fuzzy) trigger an RDF4J QueryJoinOptimizer "rightArg must not be null"
     *            assertion when wrapped, so they keep the legacy emission.
     */
    private record PendingFtsBranch(GraphPattern body, List<Expression<?>> filters,
            boolean wrapInSubSelect)
    {}

    private final List<PendingFtsBranch> pendingFtsBranches = new ArrayList<>();
    private boolean ftsActive = false;
    private boolean finalized = false;

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

            // Emit one FILTER per conjunct (REGEX, LANGMATCHES) instead of one combined
            // FILTER ( REGEX(...) && LANGMATCHES(...) ). The combined form is opaque to the
            // RDF4J cost estimator and triggers the #5444 pathology where the planner cannot
            // push the cheap language check separately from the expensive anchored regex.
            // Filters are stored separately so finalizeQuery can apply them inside a flat
            // GroupGraphPattern (calling .filter() on a TriplePattern wraps it in an extra
            // group, which the optimiser then treats as a "new scope" and falls back to a
            // HashJoinIteration with a 5.5M-row estimate on `?subj ?pMatch ?m`).
            var triple = VAR_SUBJECT
                    .has(FTS_RDF4J_LUCENE, bNode(LUCENE_QUERY, literalOf(sanitizedValue)) //
                            .andHas(LUCENE_PROPERTY, VAR_MATCH_TERM_PROPERTY) //
                            .andHas(LUCENE_SCORE, VAR_SCORE))
                    .andHas(VAR_MATCH_TERM_PROPERTY, VAR_MATCH_TERM);
            var filters = builder.equalsFilters(VAR_MATCH_TERM, value, builder.getKnowledgeBase());
            pendingFtsBranches.add(new PendingFtsBranch(triple, filters, true));
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
            pendingFtsBranches.add(new PendingFtsBranch(VAR_SUBJECT //
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
                    .filter(and(labelFilterExpressions.toArray(Expression[]::new))), List.of(),
                    false));
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
        pendingFtsBranches
                .add(new PendingFtsBranch(
                        VAR_SUBJECT
                                .has(FTS_RDF4J_LUCENE, bNode(LUCENE_QUERY, literalOf(queryString)) //
                                        .andHas(LUCENE_SCORE, VAR_SCORE)
                                        .andHas(LUCENE_PROPERTY, VAR_MATCH_TERM_PROPERTY))
                                .andHas(VAR_MATCH_TERM_PROPERTY, VAR_MATCH_TERM)
                                .filter(builder.startsWithPattern(VAR_MATCH_TERM, aPrefixQuery)),
                        List.of(), false));
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
            pendingFtsBranches.add(new PendingFtsBranch(VAR_SUBJECT //
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
                    .filter(and(labelFilterExpressions.toArray(Expression[]::new))), List.of(),
                    false));
        }

        if (pendingFtsBranches.isEmpty()) {
            builder.noResult();
        }
    }

    @Override
    public void finalizeQuery(SPARQLQueryBuilder aBuilder)
    {
        // Defense in depth against repeated invocation: draining secondary/optional lookup patterns
        // and re-emitting the PRIMARY UNION is destructive, so a second pass would either drop
        // OPTIONAL lookups or accumulate duplicate UNION patterns. The builder also guards this,
        // but adapters should be self-contained.
        if (finalized) {
            return;
        }
        finalized = true;

        if (!ftsActive || pendingFtsBranches.isEmpty()) {
            return;
        }

        // Each FTS branch gets two transformations:
        //
        // 1. The FTS body (search:matches + ?subj ?pMatch ?m + post-filters) is wrapped in a
        // sub-SELECT. The inner ?subj ?pMatch ?m StatementPattern is estimated by the
        // planner at ~5.5M rows on real SNOMED (the planner can't statically see that
        // search:matches will bind ?subj), so without a sub-SELECT boundary it picks a
        // HashJoinIteration that materialises the whole ?subj?pMatch?m index — multi-second
        // disk I/O. The sub-SELECT forces a nested-loop join with ?subj already bound by
        // Lucene, which is essentially free.
        //
        // 2. The retrieve* OPTIONALs are pushed INSIDE each branch (rather than left at the
        // outer level). RDF4J 5.1.4+ otherwise reorders the outer OPTIONAL { VALUES
        // ?pPrefLabel … } past the union, leaving ?pPrefLabel unbound when the inner
        // OPTIONALs evaluate — they then match against any predicate and we get altLabel
        // back instead of prefLabel (#5444). A pure outer-OPTIONALs shape ALSO trips an
        // RDF4J QueryJoinOptimizer "rightArg must not be null" assertion on some
        // LuceneSail+small-data combinations, so keeping the OPTIONALs in-branch is also
        // a correctness fix.
        aBuilder.drainSecondaryPatterns();
        var optionalLookups = aBuilder.drainOptionalLookupPatterns();
        var prefLabelValuesBinding = aBuilder.bindPrefLabelPropertiesPlain(VAR_PREF_LABEL_PROPERTY);
        var matchTermBinding = aBuilder.bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY);

        // The sub-SELECT wrap is only safe when there are 2+ branches: with a single branch the
        // sparqlbuilder collapses union(singleBranch) to just the branch, and a single sub-SELECT
        // wrapped in an outer scope trips an RDF4J QueryJoinOptimizer "rightArg must not be null"
        // assertion via LuceneSailConnection.evaluateInternal. Multi-branch sub-SELECTs work
        // because the UNION node sits between the sub-SELECTs and the outer scope.
        var useSubSelect = pendingFtsBranches.size() > 1
                && pendingFtsBranches.stream().allMatch(PendingFtsBranch::wrapInSubSelect);

        var augmentedBranches = pendingFtsBranches.stream() //
                .map(branch -> augmentBranch(
                        useSubSelect ? wrapBranchInSubSelect(branch, matchTermBinding)
                                : applyFilters(branch),
                        prefLabelValuesBinding, optionalLookups)) //
                .toArray(GraphPattern[]::new);

        // With sub-SELECT branches the match-term VALUES is already inside each sub-SELECT, so
        // the outer wrap is unnecessary. Otherwise it needs to live at the outer level so the
        // branches' ?pMatch references resolve.
        if (useSubSelect) {
            aBuilder.addPattern(PRIMARY, union(augmentedBranches));
        }
        else {
            aBuilder.addPattern(PRIMARY, and(matchTermBinding, union(augmentedBranches)));
        }
    }

    private static GraphPattern applyFilters(PendingFtsBranch aBranch)
    {
        if (aBranch.filters().isEmpty()) {
            return aBranch.body();
        }
        GraphPattern result = aBranch.body();
        for (var filter : aBranch.filters()) {
            result = result.filter(filter);
        }
        return result;
    }

    private static GraphPattern wrapBranchInSubSelect(PendingFtsBranch aBranch,
            GraphPattern aMatchTermBinding)
    {
        // Build the sub-SELECT WHERE as a single flat GroupGraphPattern (matchTermBinding +
        // triple) and then append the filters. and(...) wraps in a GraphPatternNotTriples
        // whose .filter() appends to the inner GroupGraphPattern's filter list without adding
        // a new group level — that flat shape is what lets the planner pick a JoinIterator
        // instead of a HashJoinIteration.
        var body = aMatchTermBinding != null //
                ? and(aMatchTermBinding, aBranch.body()) //
                : and(aBranch.body());
        for (var filter : aBranch.filters()) {
            body = body.filter(filter);
        }
        return select(VAR_SUBJECT, VAR_SCORE, VAR_MATCH_TERM).where(body);
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

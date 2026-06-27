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
import static org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder.var;
import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.and;
import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.union;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.literalOf;

import java.util.ArrayList;
import java.util.StringJoiner;

import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;

/**
 * Full-text search adapter for the <a href="https://github.com/ad-freiburg/qlever">QLever</a>
 * SPARQL engine. QLever extends SPARQL with the magic predicates {@code ql:contains-entity} and
 * {@code ql:contains-word} which match against a separate text index rather than against the
 * literals in the knowledge base directly.
 * <p>
 * This adapter targets QLever indexes that were built <b>from the literals of the knowledge
 * base</b> (e.g. the public DBLP endpoint at {@code https://sparql.dblp.org/sparql}). In such an
 * index, {@code ?text ql:contains-entity ?lit} binds the literal {@code ?lit} that a text record
 * was derived from. We can therefore join that literal back to the entity that carries it via the
 * label property and end up with the same {@code ?subj ?pMatch ?m} shape that the other FTS
 * adapters (e.g. {@link FtsAdapterVirtuoso}) produce:
 *
 * <pre>
 *   ?subj ?pMatch ?m .
 *   ?txt ql:contains-entity ?m .
 *   ?txt ql:contains-word "albert ein*" .
 *   FILTER( ... exactness check on ?m ... )
 * </pre>
 *
 * The {@code ql:contains-word} object is a space-separated list of words, all of which must occur
 * in the same text record. A trailing {@code *} turns the last word into a prefix match, which is
 * used for the auto-completion (starts-with / fuzzy) lookups. As with the other adapters, the FTS
 * predicate only pre-filters the candidates; the trailing {@code FILTER} enforces the actual
 * starts-with / contains / exact-match semantics on the matched literal.
 */
public class FtsAdapterQLever
    implements FtsAdapter
{
    private static final Prefix PREFIX_QLEVER_SEARCH = prefix("ql",
            iri("http://qlever.cs.uni-freiburg.de/builtin-functions/"));
    private static final Iri QLEVER_CONTAINS_ENTITY = PREFIX_QLEVER_SEARCH.iri("contains-entity");
    private static final Iri QLEVER_CONTAINS_WORD = PREFIX_QLEVER_SEARCH.iri("contains-word");

    /**
     * The text record variable shared by the {@code ql:contains-entity} and
     * {@code ql:contains-word} clauses. It is never projected.
     */
    private static final Variable VAR_TEXT = var("txt");

    /**
     * Minimum length a token must have before a prefix wildcard ({@code *}) is appended to it.
     * Single-character prefix searches match a very large fraction of the text index and make
     * QLever extremely slow, so such tokens are dropped instead (mirrors the behavior of
     * {@link FtsAdapterVirtuoso}).
     */
    private static final int MIN_PREFIX_LENGTH = 2;

    private final SPARQLQueryBuilder builder;

    public FtsAdapterQLever(SPARQLQueryBuilder aBuilder)
    {
        builder = aBuilder;
    }

    @Override
    public void withLabelMatchingExactlyAnyOf(String... aValues)
    {
        builder.addPrefix(PREFIX_QLEVER_SEARCH);

        var valuePatterns = new ArrayList<GraphPattern>();
        for (var value : aValues) {
            var sanitizedValue = builder.sanitizeQueryString_FTS(value);

            if (isBlank(sanitizedValue)) {
                continue;
            }

            var wordQuery = toContainsWordQuery(sanitizedValue, false);

            if (isBlank(wordQuery)) {
                continue;
            }

            valuePatterns.add(textMatch(wordQuery) //
                    .filter(builder.equalsPattern(VAR_MATCH_TERM, value)));
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
        builder.addPrefix(PREFIX_QLEVER_SEARCH);

        var valuePatterns = new ArrayList<GraphPattern>();
        for (var value : aValues) {
            var sanitizedValue = builder.sanitizeQueryString_FTS(value);

            if (isBlank(sanitizedValue)) {
                continue;
            }

            var wordQuery = toContainsWordQuery(sanitizedValue, false);

            if (isBlank(wordQuery)) {
                continue;
            }

            valuePatterns.add(textMatch(wordQuery) //
                    .filter(builder.containsPatternWithoutLookahead(VAR_MATCH_TERM, value)));
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
        builder.addPrefix(PREFIX_QLEVER_SEARCH);

        var sanitizedValue = builder.sanitizeQueryString_FTS(aPrefixQuery);

        if (isBlank(sanitizedValue)) {
            builder.noResult();
            return;
        }

        // If the query string entered by the user does not end with a space character, then we
        // assume that the user may not yet have finished writing the word and turn the last token
        // into a prefix match.
        var wordQuery = toContainsWordQuery(sanitizedValue, !aPrefixQuery.endsWith(" "));

        if (isBlank(wordQuery)) {
            builder.noResult();
            return;
        }

        // Locate all entries where the label contains the prefix (using the FTS) and then filter
        // them by those which actually start with the prefix.
        builder.addPattern(PRIMARY, and( //
                builder.bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY), //
                textMatch(wordQuery)
                        .filter(builder.startsWithPattern(VAR_MATCH_TERM, aPrefixQuery))));
    }

    @Override
    public void withLabelMatchingAnyOf(String... aValues)
    {
        builder.addPrefix(PREFIX_QLEVER_SEARCH);

        var valuePatterns = new ArrayList<GraphPattern>();
        for (var value : aValues) {
            var sanitizedValue = builder.sanitizeQueryString_FTS(value);

            if (isBlank(sanitizedValue)) {
                continue;
            }

            // If the query string entered by the user does not end with a space character, then we
            // assume that the user may not yet have finished writing the word and turn the last
            // token into a prefix match.
            var wordQuery = toContainsWordQuery(sanitizedValue, !value.endsWith(" "));

            if (isBlank(wordQuery)) {
                continue;
            }

            valuePatterns.add(textMatch(wordQuery));
        }

        if (valuePatterns.isEmpty()) {
            builder.noResult();
        }

        builder.addPattern(PRIMARY, and( //
                builder.bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY), //
                union(valuePatterns.toArray(GraphPattern[]::new))));
    }

    /**
     * Builds the {@code ?subj ?pMatch ?m . ?txt ql:contains-entity ?m . ?txt ql:contains-word
     * "..."} core that binds the subject via the QLever text index.
     */
    private GraphPattern textMatch(String aWordQuery)
    {
        return VAR_SUBJECT.has(VAR_MATCH_TERM_PROPERTY, VAR_MATCH_TERM) //
                .and(VAR_TEXT.has(QLEVER_CONTAINS_ENTITY, VAR_MATCH_TERM)) //
                .and(VAR_TEXT.has(QLEVER_CONTAINS_WORD, literalOf(aWordQuery)));
    }

    /**
     * Turns a sanitized query string into a QLever {@code ql:contains-word} word list. All tokens
     * have to occur in the same text record. If {@code aPrefixLastToken} is set, the last token is
     * turned into a prefix match by appending {@code *} - unless it is shorter than
     * {@link #MIN_PREFIX_LENGTH}, in which case the wildcard token is dropped to avoid
     * pathologically slow prefix scans.
     */
    private static String toContainsWordQuery(String aSanitizedQuery, boolean aPrefixLastToken)
    {
        var tokens = aSanitizedQuery.split(" ");
        var joiner = new StringJoiner(" ");
        for (var i = 0; i < tokens.length; i++) {
            var token = tokens[i];

            if (isBlank(token)) {
                continue;
            }

            if (aPrefixLastToken && i == tokens.length - 1) {
                if (token.length() >= MIN_PREFIX_LENGTH) {
                    joiner.add(token + "*");
                }
                // Drop a too-short trailing prefix token rather than letting it explode the scan.
            }
            else {
                joiner.add(token);
            }
        }

        return joiner.toString();
    }
}

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

public class FtsAdapterWikidata
    implements FtsAdapter
{
    private static final String FALLBACK_LANGUAGE = "en";
    private final SPARQLQueryBuilder builder;

    public FtsAdapterWikidata(SPARQLQueryBuilder aBuilder)
    {
        builder = aBuilder;
    }

    @Override
    public void withLabelMatchingExactlyAnyOf(String... aValues)
    {
        var kb = builder.getKnowledgeBase();

        // In our KB settings, the language can be unset, but the Wikidata entity search
        // requires a preferred language. So we use English as the default.
        var language = kb.getDefaultLanguage() != null ? kb.getDefaultLanguage()
                : FALLBACK_LANGUAGE;

        var valuePatterns = new ArrayList<GraphPattern>();
        for (var value : aValues) {
            var query = builder.sanitizeQueryString_FTS(value);

            if (isBlank(query)) {
                continue;
            }

            valuePatterns.add(new WikidataEntitySearchService(VAR_SUBJECT, query, language)
                    .and(VAR_SUBJECT.has(VAR_MATCH_TERM_PROPERTY, VAR_MATCH_TERM)
                            .filter(builder.equalsPattern(VAR_MATCH_TERM, value, kb))));
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
        var kb = builder.getKnowledgeBase();

        // In our KB settings, the language can be unset, but the Wikidata entity search
        // requires a preferred language. So we use English as the default.
        var language = kb.getDefaultLanguage() != null ? kb.getDefaultLanguage()
                : FALLBACK_LANGUAGE;

        var valuePatterns = new ArrayList<GraphPattern>();
        for (var value : aValues) {
            var query = builder.sanitizeQueryString_FTS(value);

            if (isBlank(query)) {
                continue;
            }

            valuePatterns.add(new WikidataEntitySearchService(VAR_SUBJECT, query, language)
                    .and(VAR_SUBJECT.has(VAR_MATCH_TERM_PROPERTY, VAR_MATCH_TERM)
                            .filter(builder.containsPattern(VAR_MATCH_TERM, value))));
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
        var kb = builder.getKnowledgeBase();

        // In our KB settings, the language can be unset, but the Wikidata entity search
        // requires a preferred language. So we use English as the default.
        var language = kb.getDefaultLanguage() != null ? kb.getDefaultLanguage()
                : FALLBACK_LANGUAGE;

        if (aPrefixQuery.isEmpty()) {
            builder.noResult();
        }

        var sanitizedValue = builder.sanitizeQueryString_FTS(aPrefixQuery);

        builder.addPattern(PRIMARY, and( //
                builder.bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY),
                new WikidataEntitySearchService(VAR_SUBJECT, sanitizedValue, language)
                        .and(VAR_SUBJECT.has(VAR_MATCH_TERM_PROPERTY, VAR_MATCH_TERM)
                                .filter(builder.startsWithPattern(VAR_MATCH_TERM, aPrefixQuery)))));
    }

    @Override
    public void withLabelMatchingAnyOf(String... aValues)
    {
        var kb = builder.getKnowledgeBase();

        // In our KB settings, the language can be unset, but the Wikidata entity search
        // requires a preferred language. So we use English as the default.
        var language = kb.getDefaultLanguage() != null ? kb.getDefaultLanguage()
                : FALLBACK_LANGUAGE;

        var valuePatterns = new ArrayList<GraphPattern>();
        for (var value : aValues) {
            var sanitizedValue = builder.sanitizeQueryString_FTS(value);

            if (isBlank(sanitizedValue)) {
                continue;
            }

            valuePatterns.add(new WikidataEntitySearchService(VAR_SUBJECT, sanitizedValue, language)
                    .and(VAR_SUBJECT.has(VAR_MATCH_TERM_PROPERTY, VAR_MATCH_TERM)));
        }

        if (valuePatterns.isEmpty()) {
            builder.noResult();
        }

        builder.addPattern(PRIMARY, and( //
                builder.bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY),
                union(valuePatterns.toArray(GraphPattern[]::new))));
    }
}

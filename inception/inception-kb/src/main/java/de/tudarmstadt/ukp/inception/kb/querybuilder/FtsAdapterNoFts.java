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
import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilder.Priority.SECONDARY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.and;
import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.union;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.literalOf;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.literalOfLanguage;

import java.util.ArrayList;

import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfValue;

public class FtsAdapterNoFts
    implements FtsAdapter
{
    private final SPARQLQueryBuilder builder;

    public FtsAdapterNoFts(SPARQLQueryBuilder aBuilder)
    {
        builder = aBuilder;
    }

    @Override
    public void withLabelMatchingExactlyAnyOf(String... aValues)
    {
        var values = new ArrayList<RdfValue>();
        var language = builder.getKnowledgeBase().getDefaultLanguage();

        for (var value : aValues) {
            var sanitizedValue = sanitizeQueryString_noFTS(value);

            if (isBlank(sanitizedValue)) {
                continue;
            }

            if (language != null) {
                values.add(literalOfLanguage(sanitizedValue, language));
            }

            values.add(literalOf(sanitizedValue));
        }

        if (values.isEmpty()) {
            builder.noResult();
        }

        builder.addPattern(PRIMARY,
                and(builder.bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY),
                        new ValuesPattern(VAR_MATCH_TERM, values),
                        VAR_SUBJECT.has(VAR_MATCH_TERM_PROPERTY, VAR_MATCH_TERM)));
    }

    @Override
    public void withLabelContainingAnyOf(String... aValues)
    {

        var valuePatterns = new ArrayList<GraphPattern>();
        for (var value : aValues) {
            if (isBlank(value)) {
                continue;
            }

            valuePatterns.add(VAR_SUBJECT.has(VAR_MATCH_TERM_PROPERTY, VAR_MATCH_TERM)
                    .filter(builder.containsPattern(VAR_MATCH_TERM, value)));
        }

        if (valuePatterns.isEmpty()) {
            builder.noResult();
        }

        // The WikiData search service does not support properties. So we disable the use of the
        // WikiData search service when looking for properties. But then, searching first by
        // the label becomes very slow because withLabelMatchingAnyOf falls back to "containing"
        // when no FTS is used. To avoid forcing the SPARQL server to perform a full scan
        // of its database, we demote the label matching to a secondary condition, allowing the
        // the matching by type (e.g. PRIMARY_RESTRICTIONS is-a property) to take precedence.
        builder.addPattern(SECONDARY, and(builder.bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY),
                union(valuePatterns.toArray(GraphPattern[]::new))));
    }

    @Override
    public void withLabelStartingWith(String aPrefixQuery)
    {
        if (aPrefixQuery.isEmpty()) {
            builder.noResult();
        }

        // Label matching without FTS is slow, so we add this with low prio and hope that some
        // other higher-prio condition exists which limites the number of candidates to a
        // manageable level
        builder.addPattern(SECONDARY, and( //
                builder.bindMatchTermProperties(VAR_MATCH_TERM_PROPERTY), //
                VAR_SUBJECT.has(VAR_MATCH_TERM_PROPERTY, VAR_MATCH_TERM)
                        .filter(builder.startsWithPattern(VAR_MATCH_TERM, aPrefixQuery))));
    }

    @Override
    public void withLabelMatchingAnyOf(String... aValues)
    {
        // Falling back to "contains" semantics if there is no FTS
        withLabelContainingAnyOf(aValues);
    }

    private String sanitizeQueryString_noFTS(String aQuery)
    {
        return aQuery
                // character classes to replace with a simple space
                .replaceAll("[\\p{Space}\\p{Cntrl}]+", " ") //
                .trim();
    }
}

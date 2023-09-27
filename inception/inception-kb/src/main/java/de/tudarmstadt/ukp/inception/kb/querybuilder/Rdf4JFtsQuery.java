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

import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.function;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.str;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction.REPLACE;
import static org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder.prefix;
import static org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder.var;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.bNode;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.literalOf;

import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;

import de.tudarmstadt.ukp.inception.kb.querybuilder.backport.Bind;

public class Rdf4JFtsQuery
    implements GraphPattern
{
    public static final Prefix PREFIX_RDF4J_SEARCH = prefix("search",
            iri("http://www.openrdf.org/contrib/lucenesail#"));
    public static final Iri RDF4J_MATCHES = PREFIX_RDF4J_SEARCH.iri("matches");
    public static final Iri RDF4J_QUERY = PREFIX_RDF4J_SEARCH.iri("query");
    public static final Iri RDF4J = PREFIX_RDF4J_SEARCH.iri("property");
    public static final Iri RDF4J_SNIPPET = PREFIX_RDF4J_SEARCH.iri("snippet");
    public static final Iri LUCENE_SCORE = PREFIX_RDF4J_SEARCH.iri("score");

    private final Variable subject;
    private final Variable score;
    private final Variable matchTerm;
    private final Variable matchTermProperty;
    private final String query;
    private int limit = 0;
    private boolean alternativeMode = false;

    public Rdf4JFtsQuery(Variable aSubject, Variable aScore, Variable aMatchTerm,
            Variable aMatchTermProperty, String aQuery)
    {
        subject = aSubject;
        score = aScore;
        matchTerm = aMatchTerm;
        matchTermProperty = aMatchTermProperty;
        query = aQuery;
    }

    public Rdf4JFtsQuery withLimit(int aLimit)
    {
        limit = aLimit;
        return this;
    }

    public Rdf4JFtsQuery alternativeMode()
    {
        alternativeMode = true;
        return this;
    }

    @Override
    public String getQueryString()
    {
        if (alternativeMode) {
            // If a KB item has multiple labels, we want to return only the ones which actually
            // match the query term such that the user is not confused that the results contain
            // items that don't match the query (even though they do through a label that is not
            // returned). RDF4J only provides access to the matched term in a "highlighted" form
            // where "<B>" and "</B>" match the search term. So we have to strip these markers
            // out as part of the query.
            GraphPattern pattern = subject //
                    .has(RDF4J_MATCHES, bNode(RDF4J_QUERY, literalOf(query)) //
                            .andHas(LUCENE_SCORE, score) //
                            .andHas(RDF4J, matchTermProperty) //
                            .andHas(RDF4J_SNIPPET, var("snippet")))
                    .and(new Bind(
                            function(REPLACE,
                                    function(REPLACE, var("snippet"), literalOf("</B>"),
                                            literalOf("")),
                                    literalOf("<B>"), literalOf("")),
                            var("label")))
                    .and(subject.has(matchTermProperty, matchTerm)) //
                    .filter(Expressions.equals(str(var("label")), str(matchTermProperty)));

            return GraphPatterns.select(subject, matchTerm, score).where(pattern) //
                    .limit(limit) //
                    .getQueryString();
        }

        TriplePattern pattern = subject.has(RDF4J_MATCHES, bNode(RDF4J_QUERY, literalOf(query)) //
                .andHas(LUCENE_SCORE, score) //
                .andHas(RDF4J, matchTermProperty)) //
                .andHas(matchTermProperty, matchTerm);

        return GraphPatterns.select(subject, matchTerm, score).where(pattern).limit(limit) //
                .getQueryString();
    }

    @Override
    public boolean isEmpty()
    {
        return false;
    }
}

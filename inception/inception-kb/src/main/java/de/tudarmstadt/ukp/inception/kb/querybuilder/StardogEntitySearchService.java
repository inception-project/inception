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

import static de.tudarmstadt.ukp.inception.kb.IriConstants.PREFIX_STARDOG;
import static org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder.prefix;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.bNode;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;

public class StardogEntitySearchService
    implements GraphPattern
{
    private static final Prefix FTS = prefix("fts", iri(PREFIX_STARDOG));

    private static final Iri FTS_TEXT_MATCH = FTS.iri("textMatch");
    private static final Iri FTS_QUERY = FTS.iri("query");
    private static final Iri FTS_THRESHOLD = FTS.iri("threshold");
    private static final Iri FTS_LIMIT = FTS.iri("limit");
    private static final Iri FTS_OFFSET = FTS.iri("offset");
    private static final Iri FTS_SCORE = FTS.iri("score");
    private static final Iri FTS_RESULT = FTS.iri("result");

    // service fts:textMatch {
    // [] fts:query 'Mexico AND city' ;
    // fts:threshold 0.6 ;
    // fts:limit 10 ;
    // fts:offset 5 ;
    // fts:score ?score ;
    // fts:result ?res ;
    private final Collection<TriplePattern> patterns;

    /**
     * Provides access to the Stardog search service.
     * 
     * @param aResult
     *            the variable to which the matching values are to be bound.
     * @param aQuery
     *            the query term.
     */
    public StardogEntitySearchService(Variable aResult, String aQuery)
    {
        patterns = new ArrayList<>();
        patterns.add(bNode() //
                .has(FTS_QUERY, Rdf.literalOf(aQuery)) //
                .andHas(FTS_RESULT, aResult));
    }

    @Override
    public String getQueryString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("SERVICE ");
        sb.append(FTS_TEXT_MATCH.getQueryString());
        sb.append(" { \n");
        for (TriplePattern pattern : patterns) {
            sb.append(pattern.getQueryString());
            sb.append(" \n");
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean isEmpty()
    {
        return false;
    }
}

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

import static org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder.prefix;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.literalOf;

import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;

import de.tudarmstadt.ukp.inception.kb.IriConstants;

public class BlazegraphFtsQuery
    implements GraphPattern
{
    public static final Prefix PREFIX_BLAZEGRAPH_SEARCH = prefix("bds",
            iri(IriConstants.FTS_BLAZEGRAPH));
    public static final Iri BLAZEGRAPH_SEARCH = PREFIX_BLAZEGRAPH_SEARCH.iri("search");
    public static final Iri BLAZEGRAPH_RELEVANCE = PREFIX_BLAZEGRAPH_SEARCH.iri("relevance");
    public static final Iri BLAZEGRAPH_MAX_RANK = PREFIX_BLAZEGRAPH_SEARCH.iri("maxRank");

    private final Variable subject;
    private final Variable score;
    private final Variable matchTerm;
    private final Variable matchTermProperty;
    private final String query;
    private int limit = 0;

    public BlazegraphFtsQuery(Variable aSubject, Variable aScore, Variable aMatchTerm,
            Variable aMatchTermProperty, String aQuery)
    {
        subject = aSubject;
        score = aScore;
        matchTerm = aMatchTerm;
        matchTermProperty = aMatchTermProperty;
        query = aQuery;
    }

    public BlazegraphFtsQuery withLimit(int aLimit)
    {
        limit = aLimit;
        return this;
    }

    @Override
    public String getQueryString()
    {
        var sb = new StringBuilder();
        sb.append("SERVICE ");
        sb.append(BLAZEGRAPH_SEARCH.getQueryString());
        sb.append(" { \n");

        var pattern = matchTerm //
                .has(BLAZEGRAPH_SEARCH, literalOf(query)) //
                .andHas(BLAZEGRAPH_RELEVANCE, score);

        if (limit > 0) {
            pattern = pattern.andHas(BLAZEGRAPH_MAX_RANK, literalOf(2 * limit));
        }

        sb.append(pattern.getQueryString());
        sb.append(" } ");
        sb.append(subject.has(matchTermProperty, matchTerm).getQueryString());
        return sb.toString();
    }

    @Override
    public boolean isEmpty()
    {
        return false;
    }
}

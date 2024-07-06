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

import static de.tudarmstadt.ukp.inception.kb.querybuilder.RdfCollection.collectionOf;
import static org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder.prefix;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.literalOf;

import java.util.ArrayList;

import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.QueryElement;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;

import de.tudarmstadt.ukp.inception.kb.IriConstants;

public class GraphDbFtsQuery
    implements GraphPattern
{
    public static final Prefix PREFIX_GRAPHDB_FTS = prefix("onto",
            iri(IriConstants.PREFIX_GRAPHDB));
    public static final Iri GRAPHDB_FTS = PREFIX_GRAPHDB_FTS.iri("fts");

    private final Variable subject;
    private final Variable score;
    private final Variable matchTerm;
    private final Variable matchTermProperty;
    private final String query;
    private int limit = 0;

    public GraphDbFtsQuery(Variable aSubject, Variable aScore, Variable aMatchTerm,
            Variable aMatchTermProperty, String aQuery)
    {
        subject = aSubject;
        score = aScore;
        matchTerm = aMatchTerm;
        matchTermProperty = aMatchTermProperty;
        query = aQuery;
    }

    public GraphDbFtsQuery withLimit(int aLimit)
    {
        limit = aLimit;
        return this;
    }

    @Override
    public String getQueryString()
    {
        var queryElements = new ArrayList<QueryElement>();
        queryElements.add(literalOf(query));
        if (limit > 0) {
            queryElements.add(literalOf(2 * limit));
        }

        return GraphPatterns.and( //
                matchTerm.has(GRAPHDB_FTS, collectionOf(queryElements)), //
                subject.has(matchTermProperty, matchTerm)) //
                .getQueryString();
    }

    @Override
    public boolean isEmpty()
    {
        return false;
    }
}

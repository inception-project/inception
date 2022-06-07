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
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;

public class FusekiFtsQuery
    implements GraphPattern
{
    public static final Prefix PREFIX_FUSEKI_SEARCH = prefix("text",
            iri("http://jena.apache.org/text#"));
    public static final Iri FUSEKI_QUERY = PREFIX_FUSEKI_SEARCH.iri("query");

    private final Variable subject;
    private final Variable score;
    private final Variable matchTerm;
    private final Variable matchTermProperty;
    private final String query;
    private int limit = 0;

    public FusekiFtsQuery(Variable aSubject, Variable aScore, Variable aMatchTerm,
            Variable aMatchTermProperty, String aQuery)
    {
        subject = aSubject;
        score = aScore;
        matchTerm = aMatchTerm;
        matchTermProperty = aMatchTermProperty;
        query = aQuery;
    }

    public FusekiFtsQuery withLimit(int aLimit)
    {
        limit = aLimit;
        return this;
    }

    @Override
    public String getQueryString()
    {
        var queryElements = new ArrayList<QueryElement>();
        queryElements.add(matchTermProperty);
        queryElements.add(literalOf(query));
        if (limit > 0) {
            queryElements.add(literalOf(2 * limit));
        }

        return collectionOf(subject, score, matchTerm) //
                .has(FUSEKI_QUERY, collectionOf(queryElements)).getQueryString();
    }

    @Override
    public boolean isEmpty()
    {
        return false;
    }
}

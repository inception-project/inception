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

import static java.util.Arrays.asList;
import static org.eclipse.rdf4j.sparqlbuilder.util.SparqlBuilderUtils.getParenthesizedString;

import java.util.Collection;

import org.eclipse.rdf4j.sparqlbuilder.core.QueryElement;
import org.eclipse.rdf4j.sparqlbuilder.core.QueryElementCollection;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfObject;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfSubject;

public class RdfCollection
    extends QueryElementCollection<QueryElement>
    implements RdfObject, RdfSubject
{
    public RdfCollection()
    {
        super(" ");
    }

    public RdfCollection(Collection<QueryElement> aElements)
    {
        super(" ", aElements);
    }

    public static RdfCollection collectionOf(Collection<QueryElement> aElements)
    {
        return new RdfCollection(aElements);
    }

    public static RdfCollection collectionOf(QueryElement... aElements)
    {
        return new RdfCollection(asList(aElements));
    }

    @Override
    public String getQueryString()
    {
        return getParenthesizedString(super.getQueryString());
    }
}

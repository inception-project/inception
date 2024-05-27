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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.support.StopWatch;

public class Queries
{
    private final static Logger LOG = LoggerFactory.getLogger(Queries.class);

    public static Map<String, KBProperty> fetchProperties(KnowledgeBase aKB,
            RepositoryConnection aConn, Collection<String> aStmts)
    {
        try (StopWatch watch = new StopWatch(LOG, "fetchProperties(%d)", aStmts.size())) {
            String[] propertyIris = aStmts.stream() //
                    .distinct() //
                    .toArray(String[]::new);

            return SPARQLQueryBuilder.forProperties(aKB) //
                    .withIdentifier(propertyIris) //
                    .retrieveLabel() //
                    .retrieveDescription() //
                    .retrieveDeprecation() //
                    .retrieveDomainAndRange() //
                    .asHandles(aConn, true) //
                    .stream() //
                    .map(handle -> KBHandle.convertTo(KBProperty.class, handle)) //
                    .collect(toMap(KBObject::getIdentifier, identity()));
        }
    }

    public static Map<String, KBHandle> fetchLabelsForIriValues(KnowledgeBase aKB,
            RepositoryConnection aConn, Collection<Object> aStmts)
    {
        try (StopWatch watch = new StopWatch(LOG, "fetchLabelsForIriValues(%d)", aStmts.size())) {
            String[] iriValues = aStmts.stream() //
                    .filter(v -> v instanceof IRI) //
                    .map(v -> ((IRI) v).stringValue()) //
                    .distinct() //
                    .toArray(String[]::new);

            return SPARQLQueryBuilder.forItems(aKB) //
                    .withIdentifier(iriValues) //
                    .retrieveLabel() //
                    .asHandles(aConn, true) //
                    .stream() //
                    .collect(toMap(KBObject::getIdentifier, identity()));
        }
    }
}

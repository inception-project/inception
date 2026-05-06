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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;

public interface SPARQLQuery
{
    /**
     * Execute the query and retrieve the results as {@link KBHandle KBHandles}.
     * 
     * @param aConnection
     *            a connection to a triple store.
     * @param aAll
     *            if items from implicit namespaces (e.g. defined by RDF) should be included.
     * @return a list of the retrieved handles.
     */
    List<KBHandle> asHandles(RepositoryConnection aConnection, boolean aAll);

    /**
     * Execute the query and see if it returns any results.
     * 
     * This may be optimized internally, e.g. by requesting only a single result so it should be
     * faster than e.g. using {@link #asHandles} and checking if the result is (non)empty.
     * 
     * @param aConnection
     *            a connection to a triple store.
     * @param aAll
     *            if items from implicit namespaces (e.g. defined by RDF) should be included.
     * @return a list of the retrieved handles.
     */
    default boolean exists(RepositoryConnection aConnection, boolean aAll)
    {
        return !asHandles(aConnection, aAll).isEmpty();
    }

    /**
     * Execute the query and return a single handle.
     * 
     * @param aConnection
     *            a connection to a triple store.
     * @param aAll
     *            if items from implicit namespaces (e.g. defined by RDF) should be included.
     * @return the matching handle (if there is one).
     */
    default Optional<KBHandle> asHandle(RepositoryConnection aConnection, boolean aAll)
    {
        return asHandles(aConnection, aAll).stream().findFirst();
    }

    // This has been moved to NoReification
    // List<KBStatement> asStatements(RepositoryConnection aConnection, boolean aAll);

    void logQueryString(Logger aLog, Level aLevel, String aPrefix);

    Set<String> resolvePrefLabelProperties(RepositoryConnection aConnection);

    Set<String> resolveAdditionalMatchingProperties(RepositoryConnection aConnection);
}

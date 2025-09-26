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

import java.util.Collection;

public interface SPARQLQueryOptionalElements
    extends SPARQLQuery
{
    /**
     * Request that a label be retrieved as part of the query.
     * 
     * @return the builder (fluent API)
     */
    SPARQLQueryOptionalElements retrieveLabel();

    /**
     * Request that a description be retrieved as part of the query.
     * 
     * @return the builder (fluent API)
     */
    SPARQLQueryOptionalElements retrieveDescription();

    SPARQLQueryOptionalElements retrieveDomainAndRange();

    SPARQLQueryOptionalElements retrieveDeprecation();

    SPARQLQueryOptionalElements limit(int aLimit);

    SPARQLQueryOptionalElements caseSensitive();

    SPARQLQueryOptionalElements caseSensitive(boolean aEnabled);

    SPARQLQueryOptionalElements caseInsensitive();

    /**
     * Include inferred statements in query results. This is the default.
     * 
     * This setting is only effective for methods which actually return results (e.g.
     * {@link SPARQLQuery#asHandles} or {@link SPARQLQuery#exists}) but not for methods which just
     * construct the query (e.g. {@link SPARQLQuery#selectQuery}.
     * 
     * @return the builder (fluent API)
     */
    SPARQLQueryOptionalElements includeInferred();

    /**
     * Exclude inferred statements in query results. The default is to include them.
     * 
     * This setting is only effective for methods which actually return results (e.g.
     * {@link SPARQLQuery#asHandles} or {@link SPARQLQuery#exists}) but not for methods which just
     * construct the query (e.g. {@link SPARQLQuery#selectQuery}.
     * 
     * @return the builder (fluent API)
     */
    SPARQLQueryOptionalElements excludeInferred();

    /**
     * Set whether to include inferred statements in query results. The default is {@code true}.
     * 
     * This setting is only effective for methods which actually return results (e.g.
     * {@link SPARQLQuery#asHandles} or {@link SPARQLQuery#exists}) but not for methods which just
     * construct the query (e.g. {@link SPARQLQuery#selectQuery}.
     * 
     * @return the builder (fluent API)
     */
    @SuppressWarnings("javadoc")
    SPARQLQueryOptionalElements includeInferred(boolean aEnabled);

    SPARQLQueryPrimaryConditions withPrefLabelProperties(Collection<String> aString);

    SPARQLQueryPrimaryConditions withAdditionalMatchingProperties(Collection<String> aString);
}

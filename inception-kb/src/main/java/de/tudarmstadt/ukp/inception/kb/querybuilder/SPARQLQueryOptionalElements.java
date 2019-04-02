/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.kb.querybuilder;

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
     */
    SPARQLQueryOptionalElements includeInferred();

    /**
     * Exclude inferred statements in query results. The default is to include them.
     * 
     * This setting is only effective for methods which actually return results (e.g.
     * {@link SPARQLQuery#asHandles} or {@link SPARQLQuery#exists}) but not for methods which just
     * construct the query (e.g. {@link SPARQLQuery#selectQuery}.
     */
    SPARQLQueryOptionalElements excludeInferred();

    /**
     * Set whether to include inferred statements in query results. The default is {@code true}.
     * 
     * This setting is only effective for methods which actually return results (e.g.
     * {@link SPARQLQuery#asHandles} or {@link SPARQLQuery#exists}) but not for methods which just
     * construct the query (e.g. {@link SPARQLQuery#selectQuery}.
     */
    SPARQLQueryOptionalElements includeInferred(boolean aEnabled);
}

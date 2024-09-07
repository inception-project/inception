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

public interface SPARQLQueryPrimaryConditions
    extends SPARQLQuery, SPARQLQueryOptionalElements
{
    /**
     * Find the item with the given identifiers.
     * <p>
     * <b>NOTE:</b> this method implicitly disables FTS for the query and must be called before
     * {@link #withLabelStartingWith(String)} or any other label-matching methods. Failure to do so
     * may result in queries returning fewer than expected results (in the worst case, no results).
     * 
     * @param aIdentifiers
     *            the item identifiers.
     * @return the builder (fluent API)
     */
    SPARQLQueryPrimaryConditions withIdentifier(String... aIdentifiers);

    /**
     * Find entries where the label matches exactly one of the given values. The match is
     * case-sensitive if requested and it takes the default language of the KB into consideration.
     * 
     * @param aValues
     *            label values.
     * @return the builder (fluent API)
     */
    SPARQLQueryPrimaryConditions withLabelMatchingExactlyAnyOf(String... aValues);

    /**
     * Find entries where the label starts with the given prefix. If fulltext search capabilities
     * are available, they will be used. Depending on the circumstances, the match may be case
     * sensitive or not.
     * 
     * @param aPrefixQuery
     *            label prefix.
     * @return the builder (fluent API)
     */
    SPARQLQueryPrimaryConditions withLabelStartingWith(String aPrefixQuery);

    /**
     * Match any items with a label containing any of the given values.
     * 
     * @param aValues
     *            values to match.
     * @return the builder (fluent API)
     */
    SPARQLQueryPrimaryConditions withLabelContainingAnyOf(String... aValues);

    /**
     * Find entries where the label matching one of the given values. The match may be fuzzy if this
     * is supported by the FTS. If there is no support for fuzzy matching in the FTS, then this
     * falls back to simply calling {@link #withLabelContainingAnyOf} Depending on the
     * circumstances, the match may be case sensitive or not.
     * 
     * @param aValues
     *            label values.
     * @return the builder (fluent API)
     */
    SPARQLQueryPrimaryConditions withLabelMatchingAnyOf(String... aValues);

    /**
     * Match all the roots of the class hierarchy.
     * <p>
     * <b>NOTE:</b> this method may implicitly disable FTS for the query and must be called before
     * {@link #withLabelStartingWith(String)} or any other label-matching methods. Failure to do so
     * may result in queries returning fewer than expected results (in the worst case, no results).
     * 
     * @return the builder (fluent API)
     */
    SPARQLQueryPrimaryConditions roots();

    /**
     * Limits results to ancestors of the given item. If the item is an instance, its classes are
     * considered to be its parents. If the item is a class, then the super-classes are the parents.
     * <p>
     * <b>NOTE:</b> this method may implicitly disable FTS for the query and must be called before
     * {@link #withLabelStartingWith(String)} or any other label-matching methods. Failure to do so
     * may result in queries returning fewer than expected results (in the worst case, no results).
     * 
     * @param aItemIri
     *            IRI of some knowledge base item
     * @return the builder (fluent API)
     */
    SPARQLQueryPrimaryConditions ancestorsOf(String aItemIri);

    /**
     * Limits results to descendants of the given class. Descendants of a class include its
     * subclasses and instances (of subclasses). Depending on which kind if items the query is built
     * for, either one of them or both are returned.
     * <p>
     * <b>NOTE:</b> this method may implicitly disable FTS for the query and must be called before
     * {@link #withLabelStartingWith(String)} or any other label-matching methods. Failure to do so
     * may result in queries returning fewer than expected results (in the worst case, no results).
     * 
     * @param aClassIri
     *            IRI of a class
     * @return the builder (fluent API)
     */
    SPARQLQueryPrimaryConditions descendantsOf(String aClassIri);

    /**
     * Limits results to children of the given class. Children of a class are its subclasses and
     * instances. Depending on which kind if items the query is built for, either one of them or
     * both are returned.
     * <p>
     * <b>NOTE:</b> this method may implicitly disable FTS for the query and must be called before
     * {@link #withLabelStartingWith(String)} or any other label-matching methods. Failure to do so
     * may result in queries returning fewer than expected results (in the worst case, no results).
     * 
     * @param aClassIri
     *            IRI of a class
     * @return the builder (fluent API)
     */
    SPARQLQueryPrimaryConditions childrenOf(String aClassIri);

    /**
     * Limits results to parents of the given class.
     * <p>
     * <b>NOTE:</b> this method may implicitly disable FTS for the query and must be called before
     * {@link #withLabelStartingWith(String)} or any other label-matching methods. Failure to do so
     * may result in queries returning fewer than expected results (in the worst case, no results).
     * 
     * @param aClassIri
     *            IRI of a class
     * @return the builder (fluent API)
     */
    SPARQLQueryPrimaryConditions parentsOf(String aClassIri);

    /**
     * Limits results to properties with the given domain or without any domain. Considers the
     * inheritance hierarchy, so if A has a property x and B is a subclass of A, then B also has the
     * property x.
     * <p>
     * <b>NOTE:</b> this method may implicitly disable FTS for the query and must be called before
     * {@link #withLabelStartingWith(String)} or any other label-matching methods. Failure to do so
     * may result in queries returning fewer than expected results (in the worst case, no results).
     * 
     * @param aItemIri
     *            IRI of some knowledge base item
     * @return the builder (fluent API)
     */
    SPARQLQueryPrimaryConditions matchingDomain(String aItemIri);

    SPARQLQueryPrimaryConditions withFallbackLanguages(String... aLanguages);

    SPARQLQueryPrimaryConditions withFallbackLanguages(Collection<String> aString);
}

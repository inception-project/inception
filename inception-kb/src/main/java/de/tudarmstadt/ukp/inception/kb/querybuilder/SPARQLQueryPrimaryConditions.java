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

public interface SPARQLQueryPrimaryConditions
    extends SPARQLQuery, SPARQLQueryOptionalElements
{
    /**
     * Find the item with the given identifiers.
     * 
     * @param aIdentifiers
     *            the item identifiers.
     * @return the builder (fluent API)
     */
    SPARQLQueryPrimaryConditions withIdentifier(String... aIdentifiers);
    
    /**
     * Find entries where the label matches exactly one of the given values. The match is
     * case-sensitive and it takes the default language of the KB into consideration.
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
     * Match all the roots of the class hierarchy.
     */
    SPARQLQueryPrimaryConditions roots();

    /**
     * Limits results to ancestors of the given item. If the item is an instance, its classes are
     * considered to be its parents. If the item is a class, then the superclasses are the parents.
     * 
     * @return the builder (fluent API)
     */
    SPARQLQueryPrimaryConditions ancestorsOf(String aItemIri);

    /**
     * Limits results to descendants of the given class. Descendants of a class include its
     * subclasses and instances (of subclasses). Depending on which kind if items the query is built
     * for, either one of them or both are returned.
     * 
     * @return the builder (fluent API)
     */
    SPARQLQueryPrimaryConditions descendantsOf(String aClassIri);

    /**
     * Limits results to children of the given class. Children of a class are its subclasses and
     * instances. Depending on which kind if items the query is built for, either one of them or
     * both are returned.
     * 
     * @return the builder (fluent API)
     */
    SPARQLQueryPrimaryConditions childrenOf(String aClassIri);

    /**
     * Limits results to parents of the given class.
     * 
     * @return the builder (fluent API)
     */
    SPARQLQueryPrimaryConditions parentsOf(String aClassIri);
    
    /**
     * Limits results to properties with the given domain or without any domain.
     * 
     * @return the builder (fluent API)
     */
    SPARQLQueryPrimaryConditions matchingDomain(String aIdentifier);

}

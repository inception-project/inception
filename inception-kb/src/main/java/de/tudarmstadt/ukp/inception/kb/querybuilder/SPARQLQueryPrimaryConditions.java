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

/**
 * When working with queries using any of {@link #roots()}, @link #withIdentifier(String)}, @link
 * #childrenOf(String)}, {@link #descendantsOf(String)}, {@link #parentsOf(String)} or
 * {@link #ancestorsOf(String)}}, the FTS is internally disabled. These methods must be called
 * <b>before</b> any label-restricting methods like {@link #withLabelStartingWith(String)} This is
 * because the FTS part of the query pre-filters the potential candidates, but the FTS may not
 * return all candidates. Let's consider a large KB (e.g. Wikidata) and a query for <i>all humans
 * named Amanda in the Star Trek universe</i> (there is a category for <i>humans in the Star Trek
 * universe</i> in Wikidata). First the FTS would try to retrieve all entities named <i>Amanda</i>,
 * but it does not really return all, just the top 50 (which is what Wikidata seems to be hard-coded
 * to despite the documentation for <i>wikidata:limit</i> saying otherwise). None of these Amandas,
 * however, is part of the Star Trek universe, so the final result of the query is empty. Here, the
 * FTS restricts too much and too early. For such cases, we should rely on the scope sufficiently
 * limiting the returned results such that the regex-based filtering does not get too slow.
 */
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
     * <p>
     * <b>NOTE:</b> this method implicitly disables FTS for the query and must be called before
     * {@link #withLabelStartingWith(String)} or any other label-matching methods. Failure to do so
     * may result in queries returning fewer than expected results (in the worst case, no results).
     */
    SPARQLQueryPrimaryConditions roots();

    /**
     * Limits results to ancestors of the given item. If the item is an instance, its classes are
     * considered to be its parents. If the item is a class, then the superclasses are the parents.
     * <p>
     * <b>NOTE:</b> this method implicitly disables FTS for the query and must be called before
     * {@link #withLabelStartingWith(String)} or any other label-matching methods. Failure to do so
     * may result in queries returning fewer than expected results (in the worst case, no results).
     * 
     * @return the builder (fluent API)
     */
    SPARQLQueryPrimaryConditions ancestorsOf(String aItemIri);

    /**
     * Limits results to descendants of the given class. Descendants of a class include its
     * subclasses and instances (of subclasses). Depending on which kind if items the query is built
     * for, either one of them or both are returned.
     * <p>
     * <b>NOTE:</b> this method implicitly disables FTS for the query and must be called before
     * {@link #withLabelStartingWith(String)} or any other label-matching methods. Failure to do so
     * may result in queries returning fewer than expected results (in the worst case, no results).
     * 
     * @return the builder (fluent API)
     */
    SPARQLQueryPrimaryConditions descendantsOf(String aClassIri);

    /**
     * Limits results to children of the given class. Children of a class are its subclasses and
     * instances. Depending on which kind if items the query is built for, either one of them or
     * both are returned.
     * <p>
     * <b>NOTE:</b> this method implicitly disables FTS for the query and must be called before
     * {@link #withLabelStartingWith(String)} or any other label-matching methods. Failure to do so
     * may result in queries returning fewer than expected results (in the worst case, no results).
     * 
     * @return the builder (fluent API)
     */
    SPARQLQueryPrimaryConditions childrenOf(String aClassIri);

    /**
     * Limits results to parents of the given class.
     * <p>
     * <b>NOTE:</b> this method implicitly disables FTS for the query and must be called before
     * {@link #withLabelStartingWith(String)} or any other label-matching methods. Failure to do so
     * may result in queries returning fewer than expected results (in the worst case, no results).
     * 
     * @return the builder (fluent API)
     */
    SPARQLQueryPrimaryConditions parentsOf(String aClassIri);
    
    /**
     * Limits results to properties with the given domain or without any domain. Considers the
     * inheritance hierarchy, so if A has a property x and B is a subclass of A, then B also has
     * the property x.
     * <p>
     * <b>NOTE:</b> this method implicitly disables FTS for the query and must be called before
     * {@link #withLabelStartingWith(String)} or any other label-matching methods. Failure to do so
     * may result in queries returning fewer than expected results (in the worst case, no results).
     * 
     * @return the builder (fluent API)
     */
    SPARQLQueryPrimaryConditions matchingDomain(String aIdentifier);

}

/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.inception.kb;

import java.util.Set;

import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;

/**
 * The {@link KBSearchService} provides search functionality for elements in a knowledge
 * graph repository.
 */
// TODO expand / test once query language is decided
public interface KBSearchService
{
    /**
     * Searches for entities in the knowledge base containing the given term.
     * 
     * @param term
     *            a search term
     * @return a list of entities relevant for the given the term
     */
    Set<KBInstance> searchEntitiesLike(String term);

    /**
     * Searches for types in the knowledge base containing the given term.
     * 
     * @param term
     *            a search term
     * @return a list of types relevant for the given the term
     */
    Set<KBConcept> searchTypesLike(String term);

    /**
     * Searches for properties in the knowledge base containing the given term.
     * 
     * @param term
     *            a search term
     * @return a list of properties relevant for the given the term
     */
    Set<KBProperty> searchPropertiesLike(String term);
}

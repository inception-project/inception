/*
 * Copyright 2018
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

import java.util.List;

import org.eclipse.rdf4j.model.IRI;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.JCasProvider;
import de.tudarmstadt.ukp.inception.kb.model.Entity;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public interface KnowledgeBaseExtension
{
    /**
     * The bean name of this extension
     * @return bean name
     */
    String getBeanName();

    /**
     * Given a mention in the text, this method returns a list of ranked candidate entities
     * generated from a Knowledge Base. It only contains entities which are instances of a
     * pre-defined concept.
     *
     * @param aKB the KB used to generate candidates
     * @param aConceptIri the concept of which instances should be generated as candidates
     * @param aMentionBeginOffset the offset where the mention begins in the text
     * @param aJcasProvider contains JCas, used to extract information about mention sentence
     *                       tokens
     * @return ranked list of entities, starting with the most probable entity
     */
    List<Entity> disambiguate(KnowledgeBase aKB, IRI aConceptIri, String
        aMention, int aMentionBeginOffset, JCasProvider aJcasProvider);
}

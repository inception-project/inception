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
package de.tudarmstadt.ukp.inception.kb.reification;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBQualifier;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public interface ReificationStrategy
{
    /**
     * Upserts the statement. Any qualifiers in the statement are <b>NOT</b> upserted. Call
     * {@link #upsertQualifier} if you need that.
     * <p>
     * Certain updates may not be permitted, e.g. changing the property of an <i>instance-of</i>
     * statement. This is meant to avoid dangling triples accumulating in the knowledge base. In
     * these cases, a {@link IllegalArgumentException} is thrown.
     */
    void upsertStatement(RepositoryConnection aConnection, KnowledgeBase kb,
            KBStatement aStatement);

    /**
     * Deletes the specified statement from the knowledge base. This includes any qualifiers that
     * might be associated with it.
     * <p>
     * Certain statements may not be deletable. E.g. the last <i>instance-of</i> statement cannot
     * be deleted unless the deletion of the whole concept/instance is requested. This is meant
     * to avoid dangling triples accumulating in the knowledge base. In these cases, a
     * {@link IllegalArgumentException} is thrown.
     */
    void deleteStatement(RepositoryConnection aConnection, KnowledgeBase kb,
            KBStatement aStatement);

    List<KBStatement> listStatements(RepositoryConnection aConnection, KnowledgeBase kb,
            KBHandle aInstance, boolean aAll);

    /**
     * Writes the given qualifier to the knowledge base. If it does not exist yet in the knowledge
     * base, it is created. If it does exist, its previous version will be replaced with the given
     * one.
     */
    void upsertQualifier(RepositoryConnection aConnection, KnowledgeBase kb,
            KBQualifier aQualifier);

    /**
     * Deletes the given qualifier from the knowledge base.
     * <p>
     * <b>NOTE:</b> the statement owning the qualifier is <b>NOT</b> updated.
     */
    void deleteQualifier(RepositoryConnection aConnection, KnowledgeBase kb,
            KBQualifier aQualifier);

    /**
     * Retrieves the qualifiers for the given statement.
     * <p>
     * <b>NOTE:</b> the statement owning the qualifier is <b>NOT</b> updated. If you wish to update
     * the given statement with the new qualifiers, call {@link KBStatement#setQualifiers(List)}.
     * <p>
     * However, the returned qualifiers <b>DO</b> return the passed statement on
     * {@link KBQualifier#getStatement()}.
     */
    List<KBQualifier> listQualifiers(RepositoryConnection aConnection, KnowledgeBase kb,
            KBStatement aStatement);

    /**
     * Delete the specified instance by removing all statements/qualifiers describing it. Also,
     * and statements in other concepts/instances which refer to the given instance will be
     * deleted.
     */
    void deleteInstance(RepositoryConnection aConnection, KnowledgeBase kb, KBInstance aInstance);

    /**
     * Deletes the specified property. Any statements/qualifiers using this property will also be
     * deleted.
     * <p>
     * Properties which are part of the knowledge base mapping (e.g. the instance-of property)
     * cannot be deleted and trying to do so will throw an {@link IllegalArgumentException}. A
     * reification strategy may additional reserve additional properties that cannot be deleted.
     */
    void deleteProperty(RepositoryConnection aConnection, KnowledgeBase kb, KBProperty aProperty);

    /**
     * Deletes the specified concept. Any statements or qualifiers referencing the concept are
     * deleted as well. This includes all instances of the given concept (except if they are 
     * also instances of another concept) and all statements referring to the concept.
     */
    void deleteConcept(RepositoryConnection aConnection, KnowledgeBase kb, KBConcept aConcept);

    /**
     * Checks if the given statement exists. Qualifiers are not considered by this check.
     */
    boolean exists(RepositoryConnection aConnection, KnowledgeBase akb,
            KBStatement aStatement);
    
    default void upsert(RepositoryConnection aConnection, Collection<Statement> aOriginalTriples,
            Collection<Statement> aNewTriples)
    {
        // Delete all original triples except the ones which we would re-create anyway
        Set<Statement> triplesToDelete = new HashSet<>();
        aOriginalTriples.forEach(triplesToDelete::add);
        triplesToDelete.removeAll(aNewTriples);
        aConnection.remove(triplesToDelete);
        
        // Store the new triples
        aConnection.add(aNewTriples);
    }

    String generatePropertyIdentifier(RepositoryConnection aConn, KnowledgeBase aKb);

    String generateInstanceIdentifier(RepositoryConnection aConn, KnowledgeBase aKb);

    String generateConceptIdentifier(RepositoryConnection aConn, KnowledgeBase aKb);
}

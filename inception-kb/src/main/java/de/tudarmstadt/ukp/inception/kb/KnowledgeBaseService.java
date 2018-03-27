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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;

import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.manager.RepositoryInfo;
import org.eclipse.rdf4j.rio.RDFFormat;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public interface KnowledgeBaseService
{
    String INCEPTION_SCHEMA_NAMESPACE = "http://www.ukp.informatik.tu-darmstadt.de/inception/schema-1.0#";
    String INCEPTION_NAMESPACE = "http://www.ukp.informatik.tu-darmstadt.de/inception/1.0#";
    String[] IMPLICIT_NAMESPACES = { RDF.NAMESPACE, RDFS.NAMESPACE, XMLSchema.NAMESPACE,
            OWL.NAMESPACE, INCEPTION_SCHEMA_NAMESPACE };

    String SERVICE_NAME = "knowledgeBaseService";

    void importData(KnowledgeBase kb, String aFilename, InputStream aIS) throws IOException;

    /**
     * Writes the contents of a knowledge base of type {@link RepositoryType#LOCAL} to a given
     * {@link OutputStream} in a specificable format.<br>
     * No action will be taken if the given knowledge base is not of type
     * {@link RepositoryType#LOCAL} (nothing will be written to the output stream).
     *
     * @param kb
     * @param format
     * @param os
     */
    void exportData(KnowledgeBase kb, RDFFormat format, OutputStream os);

    void clear(KnowledgeBase kb);

    /**
     * {@code False} if a knowledge base does not contain any statements.
     *
     * @param kb a {@link KnowledgeBase}
     */
    boolean isEmpty(KnowledgeBase kb);

    void registerKnowledgeBase(KnowledgeBase kb, RepositoryImplConfig cfg);

    boolean knowledgeBaseExists(Project project, String kbName);

    /**
     * Update the configuration of a knowledge base.
     * The given knowledge base must have been added before.
     */
    void updateKnowledgeBase(KnowledgeBase kb, RepositoryImplConfig cfg);

    void removeKnowledgeBase(KnowledgeBase kb);

    List<KnowledgeBase> getKnowledgeBases(Project aProject);

    // TODO refactor so that rdf4j dependencies are not leaked here anymore
    RepositoryInfo getKnowledgeBaseInfo(KnowledgeBase kb);

    RepositoryImplConfig getNativeConfig();

    RepositoryImplConfig getRemoteConfig(String url);

    RepositoryImplConfig getKnowledgeBaseConfig(KnowledgeBase kb);

    /**
     * Creates a new concept in the given knowledge base. Does nothing 
     * if the knowledge base is read only.
     * @param kb The knowledge base to which the new concept will be added
     * @param aType The concept to add
     */
    KBHandle createConcept(KnowledgeBase kb, KBConcept aType);

    Optional<KBConcept> readConcept(KnowledgeBase kb, String aIdentifier);

    /**
     * Updates an existing concept in the given knowledge base. Does nothing if 
     * the knowledge base is read only.
     * @param kb The knowledge base in which the new concept will updated
     * @param aType The concept to update
     */
    void updateConcept(KnowledgeBase kb, KBConcept aType);

    /**
     * Deletes a concept in the given knowledge base if it exists. Does nothing if the 
     * knowledge base is read only.
     * @param kb The knowledge base from which the new concept will be deleted
     * @param aType The concept to delete
     */
    void deleteConcept(KnowledgeBase kb, KBConcept aType);

    List<KBHandle> listConcepts(KnowledgeBase kb, boolean aAll);

    /**
     * Creates a new property in the given knowledge base. Does nothing
     * if the knowledge base is read only.
     * @param kb The knowledge base to which the new property will be added
     * @param aProperty The property to add
     */
    KBHandle createProperty(KnowledgeBase kb, KBProperty aProperty);

    Optional<KBProperty> readProperty(KnowledgeBase kb, String aIdentifier);

    /**
     * Updates an existing property in the given knowledge base. Does nothing
     * if the knowledge base is read only.
     * @param kb The knowledge base to which the new property will be added
     * @param aProperty The property to add
     */
    void updateProperty(KnowledgeBase kb, KBProperty aProperty);

    /**
     * Deletes a property in the given knowledge base if it exists. Does
     * nothing if the knowledge base is read only.
     * @param kb The knowledge base from which the new concept will be deleted
     * @param aType The property to delete
     */
    void deleteProperty(KnowledgeBase kb, KBProperty aType);

    List<KBHandle> listProperties(KnowledgeBase kb, boolean aAll);

    /**
     * Creates a new instance in the given knowledge base. Does nothing
     * if the knowledge base is read only.
     * @param kb The knowledge base to which the new instance will be added
     * @param aInstance The instance to add
     */
    KBHandle createInstance(KnowledgeBase kb, KBInstance aInstance);

    Optional<KBInstance> readInstance(KnowledgeBase kb, String aIdentifier);

    /**
     * Updates an existing instance in the given knowledge base. Does nothing
     * if the knowledge base is read only.
     * @param kb The knowledge base to which the new instance will be added
     * @param aInstance The instance to add
     */
    void updateInstance(KnowledgeBase kb, KBInstance aInstance);

    /**
     * Delete the given instance. Also deletes all statements about that instance (i.e. where the
     * instance is the subject), but not statements pointing to the instance (i.e. where the
     * instance is the object). Does nothing if the knowledge base is read only.
     */
    void deleteInstance(KnowledgeBase kb, KBInstance aInstance);

    /**
     * Returns all instances for the given concept.
     *
     * @param kb          The knowledge base to query
     * @param aConceptIri The URI of the concept finding instances for
     * @param aAll        True if entities with implicit namespaces (e.g. defined by RDF)
     * @return All instances of the given concept
     */
    List<KBHandle> listInstances(KnowledgeBase kb, String aConceptIri, boolean aAll);

    /**
     * Inserts a new statement. If the statement has an original statement, that one is deleted
     * before inserting the new one. If the statement is an inferred statement, then no deletion
     * attempt will be made, but the statement will be added as a new explicit statement. Does
     * nothing if the knowledge base is read only.
     */
    void upsertStatement(KnowledgeBase kb, KBStatement aStatement);

    /**
     * Deletes a statement in the given knowledge base if it exists. Does
     * nothing if the knowledge base is read only.
     * @param kb The knowledge base from which the new concept will be deleted
     * @param aStatement The statement to delete
     */
    void deleteStatement(KnowledgeBase kb, KBStatement aStatement);

    List<KBStatement> listStatements(KnowledgeBase kb, KBHandle aInstance, boolean aAll);

    List<KBStatement> listStatements(KnowledgeBase kb, KBInstance aInstance, boolean aAll);

    List<KBHandle> listRootConcepts(KnowledgeBase kb, boolean aAll);

    List<KBHandle> listChildConcepts(KnowledgeBase kb, String parentIdentifier, boolean aAll);
}

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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.rio.RDFFormat;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBQualifier;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseProfile;

public interface KnowledgeBaseService
{
    String SERVICE_NAME = "knowledgeBaseService";
    
    /**
     * Reads knowledgebase profiles from a YAML file and stores them in a HashMap with the key that
     * is defined in the file and a corresponding {@link KnowledgeBaseProfile} object as value
     * 
     * @return a HashMap with the knowledgebase profiles
     * @throws IOException
     *             if an error occurs when reading the file
     */
    Map<String, KnowledgeBaseProfile> readKnowledgeBaseProfiles() throws IOException;

    void importData(KnowledgeBase kb, String aFilename, InputStream aIS) throws IOException;

    /**
     * Writes the contents of a knowledge base of type {@link RepositoryType#LOCAL} to a given
     * {@link OutputStream} in a specificable format.<br>
     * No action will be taken if the given knowledge base is not of type
     * {@link RepositoryType#LOCAL} (nothing will be written to the output stream).
     *
     * @param kb The knowledge base to export
     * @param format Format of the data
     * @param os The {@link OutputStream} variable
     */
    void exportData(KnowledgeBase kb, RDFFormat format, OutputStream os);

    void clear(KnowledgeBase kb);

    /**
     * {@code False} if a knowledge base does not contain any statements.
     *
     * @param kb a {@link KnowledgeBase}
     * @return a {@link Boolean} value
     */
    boolean isEmpty(KnowledgeBase kb);

    void registerKnowledgeBase(KnowledgeBase kb, RepositoryImplConfig cfg)
        throws RepositoryException, RepositoryConfigException;

    boolean knowledgeBaseExists(Project project, String kbName);

    Optional<KnowledgeBase> getKnowledgeBaseById(Project project, String aId);

    /**
     * Update the configuration of a knowledge base.
     * The given knowledge base must have been added before.
     * @param kb the {@link KnowledgeBase} to update
     */
    void updateKnowledgeBase(KnowledgeBase kb)
        throws RepositoryException, RepositoryConfigException;

    /**
     * Update the configuration of a knowledge base.
     * The given knowledge base must have been added before.
     * @param kb the {@link KnowledgeBase} to update
     * @param cfg the {@link RepositoryImplConfig} variable
     */
    void updateKnowledgeBase(KnowledgeBase kb, RepositoryImplConfig cfg)
        throws RepositoryException, RepositoryConfigException;

    void removeKnowledgeBase(KnowledgeBase kb)
        throws RepositoryException, RepositoryConfigException;
    
    /**
     * Get knowledge bases from a given project.
     * @param aProject The project that contains the knowledge bases.
     * @return All KBs sorted by name in lexicographical order, ignoring the case.
     */
    List<KnowledgeBase> getKnowledgeBases(Project aProject);
    
    /**
     * Get enabled knowledge bases from a given project.
     * @param aProject The project that contains the knowledge bases.
     * @return All enabled KBs sorted by name in lexicographical order, ignoring the case.
     */
    List<KnowledgeBase> getEnabledKnowledgeBases(Project aProject);

    RepositoryImplConfig getNativeConfig();

    RepositoryImplConfig getRemoteConfig(String url);

    RepositoryImplConfig getKnowledgeBaseConfig(KnowledgeBase kb)
        throws RepositoryConfigException, RepositoryException;

    /**
     * Creates a new concept in the given knowledge base. Does nothing 
     * if the knowledge base is read only.
     * @param kb The knowledge base to which the new concept will be added
     * @param aType The concept to add
     * @return the KBHandle for the created concept 
     */
    KBHandle createConcept(KnowledgeBase kb, KBConcept aType);

    /**
     * Find the specified concept form the first KB in the project which provides it.
     * 
     * @param aProject
     *            a project.
     * @param aIdentifier
     *            a concept identifier.
     * @return the concept.
     */
    Optional<KBConcept> readConcept(Project aProject, String aIdentifier)
        throws QueryEvaluationException;

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

    
    List<KBHandle> listConcepts(KnowledgeBase kb, boolean aAll) throws QueryEvaluationException;

    /**
     * Creates a new property in the given knowledge base. Does nothing
     * if the knowledge base is read only.
     * @param kb The knowledge base to which the new property will be added
     * @param aProperty The property to add
     * @return the KBHandle for the created concept 
     */
    KBHandle createProperty(KnowledgeBase kb, KBProperty aProperty);

    Optional<KBProperty> readProperty(KnowledgeBase kb, String aIdentifier)
        throws QueryEvaluationException;

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

    /**
     * List the properties from the knowledge base
     * @param kb The knowledge base from which the new concept will be deleted
     * @param aAll indicates whether to include base properties or not
     * @return list of all the properties {@link KBHandle} 
     */
    List<KBHandle> listProperties(KnowledgeBase kb, boolean aAll) throws QueryEvaluationException;

    /**
     * List the properties from the knowledge base
     * @param kb The knowledge base from which the new concept will be deleted
     * @param aIncludeInferred
     *        indicates whether inferred statements should be included in the result.
     * @param aAll indicates whether to include base properties or not
     * @return list of all the properties {@link KBHandle} 
     */
    List<KBHandle> listProperties(KnowledgeBase kb, boolean aIncludeInferred, boolean aAll);

    /**
     * Creates a new instance in the given knowledge base. Does nothing if the knowledge base is
     * read only.
     * 
     * @param kb
     *            The knowledge base to which the new instance will be added
     * @param aInstance
     *            The instance to add
     * @return the instance {@link KBHandle}
     */
    KBHandle createInstance(KnowledgeBase kb, KBInstance aInstance);

    /**
     * Read the instance with the given identifier from the given knowledge base.
     * 
     * @param kb
     *            a knowledge base.
     * @param aIdentifier
     *            an instance identifier.
     * @return the concept.
     */
    Optional<KBInstance> readInstance(KnowledgeBase kb, String aIdentifier)
        throws QueryEvaluationException;

    /**
     * Find the specified instance form the first KB in the project which provides it.
     * 
     * @param aProject
     *            a project.
     * @param aIdentifier
     *            an instance identifier.
     * @return the concept.
     */
    Optional<KBInstance> readInstance(Project aProject, String aIdentifier)
        throws QueryEvaluationException;

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
     * 
     * @param kb The knowledge base to which the instance will be deleted
     * @param aInstance The instance to delete
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
    List<KBHandle> listInstances(KnowledgeBase kb, String aConceptIri, boolean aAll)
        throws QueryEvaluationException;

    // Statements

    /**
     * Initializes the internal representation of a KBStatement specifically
     * for the given knowledge base. Call this before upserting it
     * @param kb The knowledge base the statement will be use in
     * @param aStatement The statement itself
     */
    void initStatement(KnowledgeBase kb, KBStatement aStatement);

    /**
     * Inserts a new statement. If the statement has an original statement, that one is deleted
     * before inserting the new one. If the statement is an inferred statement, then no deletion
     * attempt will be made, but the statement will be added as a new explicit statement. Does
     * nothing if the knowledge base is read only.
     * 
     * @param kb The knowledge base the statement will be use in
     * @param aStatement The statement itself
     */
    void upsertStatement(KnowledgeBase kb, KBStatement aStatement) throws RepositoryException;

    /**
     * Deletes a statement in the given knowledge base if it exists. Does
     * nothing if the knowledge base is read only.
     * @param kb The knowledge base from which the new concept will be deleted
     * @param aStatement The statement to delete
     */
    void deleteStatement(KnowledgeBase kb, KBStatement aStatement) throws RepositoryException;
    
    /**
     * Lists all statements in which the given identifier appears as predicate or object 
     * @param kb The knowledge base to query
     * @param aIdentifier The identifier of the entity
     * @return All statements that match the specification
     */
    List<Statement> listStatementsWithPredicateOrObjectReference(KnowledgeBase kb,
            String aIdentifier);
    
    List<KBStatement> listStatements(KnowledgeBase kb, KBHandle aInstance, boolean aAll)
        throws QueryEvaluationException;

    List<KBStatement> listStatements(KnowledgeBase kb, KBInstance aInstance, boolean aAll)
        throws QueryEvaluationException;

    List<KBHandle> listRootConcepts(KnowledgeBase kb, boolean aAll) throws QueryEvaluationException;

    boolean hasChildConcepts(KnowledgeBase aKB, String aParentIdentifier, boolean aAll);
    
    List<KBHandle> listChildConcepts(KnowledgeBase kb, String parentIdentifier, boolean aAll)
        throws QueryEvaluationException;

    List<KBHandle> listChildConcepts(KnowledgeBase kb, String parentIdentifier, boolean aAll,
            int aLimit)
        throws QueryEvaluationException;
    
    /**
     * List the instances for the child concepts
     * 
     * @param aKB The knowledge base
     * @param aParentIdentifier a Parent identifier.
     * @param aAll True if entities with implicit namespaces (e.g. defined by RDF)
     * @param aLimit Limit for SPARQL queries
     * @return All instances of the child concepts
     */
    List<KBHandle> listInstancesForChildConcepts(KnowledgeBase aKB, String aParentIdentifier,
            boolean aAll, int aLimit) throws QueryEvaluationException;
    
    
    RepositoryConnection getConnection(KnowledgeBase kb);

    interface ReadAction<T>
    {
        T accept(RepositoryConnection aConnection);
    }

    <T> T read(KnowledgeBase kb, ReadAction<T> aAction);

    KBHandle update(KnowledgeBase kb, UpdateAction aAction);

    interface UpdateAction
    {
        KBHandle accept(RepositoryConnection aConnection);
    }

    List<KBHandle> list(KnowledgeBase kb, IRI aType, boolean aIncludeInferred, boolean
        aAll, int aLimit);

    /**
     * List the properties for a specific accepted domain identifier and also 
     * include properties which do not have any domain specified. 
     * 
     * @param kb
     *            The knowledge base
     * @param aDomain
     *            a domain identifier.
     * @param aIncludeInferred
     *            indicates whether inferred statements should be included in the result.
     * @param aAll
     *            indicates whether to include base properties or not
     * @return All properties for a specific accepted domain identifier
     */
    List<KBHandle> listDomainProperties(KnowledgeBase kb, String aDomain, boolean aIncludeInferred,
            boolean aAll);

    /**
     * List the properties for a specific accepted range identifier
     * 
     * @param kb
     *            The knowledge base
     * @param aDomain
     *            a range identifier.
     * @param aIncludeInferred
     *            indicates whether inferred statements should be included in the result.
     * @param aAll
     *            indicates whether to include base properties or not
     * @return All properties for a specific accepted range identifier
     */
    List<KBHandle> listPropertiesRangeValue(KnowledgeBase kb, String aDomain,
            boolean aIncludeInferred, boolean aAll);
    
    /**
     * List the properties
     * 
     * @param kb
     *            The knowledge base
     * @param aType
     *            a {@link IRI} for type of property
     * @param aIncludeInferred
     *            indicates whether inferred statements should be included in the result.
     * @param aAll
     *            indicates whether to include base properties or not
     * @return All properties 
     */
    List<KBHandle> listProperties(KnowledgeBase kb, IRI aType, boolean aIncludeInferred, boolean
            aAll);

    /**
     * Adds a new qualifier in the given knowledge base. Does
     * nothing if the knowledge base is read only.
     * @param kb The knowledge base from which the new qualifier will be added
     * @param newQualifier The qualifier to add
     */
    void addQualifier(KnowledgeBase kb, KBQualifier newQualifier);

    /**
     * Deletes a qualifier in the given knowledge base if it exists. Does
     * nothing if the knowledge base is read only.
     * @param kb The knowledge base from which the new qualifier will be deleted
     * @param oldQualifier The qualifier to delete
     */
    void deleteQualifier(KnowledgeBase kb, KBQualifier oldQualifier);

    /**
     * Updates a qualifier or inserts a new one. If the qualifier has an original qualifier,
     * that old one is deleted before inserting the new one. Does nothing if the knowledge base is
     * read only.
     * @param kb The knowledge base from which the qualifier will be upserted
     * @param aQualifier The qualifier to upsert
     */
    void upsertQualifier(KnowledgeBase kb, KBQualifier aQualifier);

    /**
     * Returns all qualifiers for the given statement
     * @param kb The knowledge base to query
     * @param aStatement The statement finding qualifiers for
     * @return all qualifiers for the given statement
     */
    List<KBQualifier> listQualifiers(KnowledgeBase kb, KBStatement aStatement);

    boolean statementsMatchSPO(KnowledgeBase akb, KBStatement mockStatement);

    /**
     * Define base default properties of comment, label and subClassOf with schema set defined for
     * KB while initializing the KB
     * 
     * @param akb
     *            The knowledge base to initialize base properties
     */
    void defineBaseProperties(KnowledgeBase akb);

    /**
     * Read an identifier value to return {@link KBObject}
     * @param aProject Project to read the KB identifier
     * @param aIdentifier String value for IRI
     * @return {@link Optional} of {@link KBObject} of type {@link KBConcept} or {@link KBInstance}
     */
    Optional<KBObject> readKBIdentifier(Project aProject, String aIdentifier);

    /**
     * Read an identifier value from a particular kb to return {@link KBObject}
     * @param akb
     * @param aIdentifier
     * @return {@link Optional} of {@link KBObject} of type {@link KBConcept} or {@link KBInstance}
     */
    Optional<KBObject> readKBIdentifier(KnowledgeBase akb, String aIdentifier);

     /** Retrieves the parent concept for a concept identifier
     * 
     * @param aKB The knowledge base
     * @param aHandle a concept.
     * @param aAll True if entities with implicit namespaces (e.g. defined by RDF)
     * @return List of parent concept for an identifier
     */
    List<KBHandle> getParentConcept(KnowledgeBase aKB, KBHandle aHandle, boolean aAll)
        throws QueryEvaluationException;
    
    /**
     * Retrieves the distinct parent concepts till the root element for an identifier regardless of
     * it being an instance or concept
     *
     * @param aKB The knowledge base
     * @param aIdentifier a concept/instance identifier.
     * @param aAll True if entities with implicit namespaces (e.g. defined by RDF)
     * @return List of parent concept for an identifier
     */
    Set<KBHandle> getParentConceptList(KnowledgeBase aKB, String aIdentifier, boolean aAll)
        throws QueryEvaluationException;
    
    /**
     * Retrieves the concepts for an instance identifier 
     * 
     * @param aKB The knowledge base
     * @param aIdentifier an instance identifier.
     * @param aAll True if entities with implicit namespaces (e.g. defined by RDF)
     * @return List of concepts for an instance identifier
     */
    List<KBHandle> getConceptForInstance(KnowledgeBase aKB, String aIdentifier, boolean aAll)
        throws QueryEvaluationException;

    /**
     * List all the concepts
     * @param kb The knowledge base from which concepts will be listed
     * @param aAll indicates whether to include everything
     * @return list of all the properties {@link KBHandle} 
     */
    List<KBHandle> listAllConcepts(KnowledgeBase kb, boolean aAll) throws QueryEvaluationException;

    /**
     * Returns whether the given identifier is a subproperty of the label IRI defined for this
     * knowledge base
     *
     * @param aKB the knowledge base
     * @param aIdentifier the identifier of the label
     * @return true if the identifier is a subproperty of the label IRI defined for this
     * knowledge base, false otherwise
     */
    boolean isSubpropertyLabel(KnowledgeBase aKB, String aIdentifier);

    /**
     * Checks whether a property is a base property
     * @param propertyIdentifier the property that is to be checked
     * @param aKB the KB
     * @return true if the property is a base property, false otherwise
     */
    boolean isBaseProperty(String propertyIdentifier, KnowledgeBase aKB);

    /**
     * Can be used to re-index a local KB in case the full text index is corrupt.
     */
    void rebuildFullTextIndex(KnowledgeBase aKb) throws Exception;
    
    /**
     * Read the concept with the given identifier from the given knowledge base with a 
     * specific query
     * 
     * @param aKB
     *            a knowledge base.
     * @param aIdentifier
     *            a concept identifier.
     * @param aAll True if entities with implicit namespaces (e.g. defined by RDF)
     * @return the concept.
     */
    Optional<KBConcept> readConcept(KnowledgeBase aKB, String aIdentifier, boolean aAll)
            throws QueryEvaluationException;

    /**
     * Gets a list of sub-property of label 
     * 
     * @param aKB
     *            a knowledge base.
     * @return set of properties
     */
    Set<KBHandle> getSubPropertyLabels(KnowledgeBase aKB);
}

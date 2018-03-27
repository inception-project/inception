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
import java.util.List;

import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.manager.RepositoryInfo;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;

public interface KnowledgeBaseService
{
    public static final String INCEPTION_NAMESPACE = "http://www.ukp.informatik.tu-darmstadt.de/inception/1.0#";
    public static final String[] IMPLICIT_NAMESPACES = { RDF.NAMESPACE, RDFS.NAMESPACE,
            XMLSchema.NAMESPACE, OWL.NAMESPACE };
    
    String SERVICE_NAME = "knowledgeBaseService";    
    
    public void importData(KnowledgeBase kb, String aFilename, InputStream aIS)
            throws IOException;
    
    public void clear(KnowledgeBase kb);
    
    /**
     * {@code False} if a knowledge base does not contain any statements.
     * @param kb a {@link KnowledgeBase}
     */
    public boolean isEmpty(KnowledgeBase kb);
    
    public void registerKnowledgeBase(KnowledgeBase kb, RepositoryImplConfig cfg);
    
    /**
     * Update the configuration of a knowledge base.
     * The given knowledge base must have been added before.
     */
    public void updateKnowledgeBase(KnowledgeBase kb, RepositoryImplConfig cfg);
    
    public void removeKnowledgeBase(KnowledgeBase kb);
    
    public List<KnowledgeBase> getKnowledgeBases(Project aProject);
    
    // TODO refactor so that rdf4j dependencies are not leaked here anymore
    public RepositoryInfo getKnowledgeBaseInfo(KnowledgeBase kb);
    
    public RepositoryImplConfig getNativeConfig();

    public RepositoryImplConfig getRemoteConfig(String url);
    
    public RepositoryImplConfig getKnowledgeBaseConfig(KnowledgeBase kb);

    /**
     * Normally, this method should not be used directly. It should only be used if direct
     * access to the repository is required, e.g. for exporting the repository.
     */
    RepositoryConnection getConnection(KnowledgeBase kb);
        
    KBHandle createConcept(KnowledgeBase kb, KBConcept aType);
    
    KBConcept readConcept(KnowledgeBase kb, String aIdentifier);
    
    void updateConcept(KnowledgeBase kb, KBConcept aType);
    
    void deleteConcept(KnowledgeBase kb, KBConcept aType);

    List<KBHandle> listConcepts(KnowledgeBase kb, boolean aAll);

    KBHandle createProperty(KnowledgeBase kb, KBProperty aType);
    
    KBProperty readProperty(KnowledgeBase kb, String aIdentifier);
    
    void updateProperty(KnowledgeBase kb, KBProperty aType);
    
    void deleteProperty(KnowledgeBase kb, KBProperty aType);

    List<KBHandle> listProperties(KnowledgeBase kb, boolean aAll);  
    
    KBHandle createInstance(KnowledgeBase kb, KBInstance aInstance);
    
    KBInstance readInstance(KnowledgeBase kb, String aIdentifier);

    void updateInstance(KnowledgeBase kb, KBInstance aInstance);

    /**
     * Delete the given instance. Also deletes all statements about that instance (i.e. where the
     * instance is the subject), but not statements pointing to the instance (i.e. where the
     * instance is the object).
     */
    void deleteInstance(KnowledgeBase kb, KBInstance aInstance);

    List<KBHandle> listInstances(KnowledgeBase kb, String aConceptIri, boolean aAll);

    /**
     * Inserts a new statement. If the statement has an original statement, that one is deleted
     * before inserting the new one. If the statement is an inferred statement, then no deletion
     * attempt will be made, but the statement will be added as a new explicit statement.
     */
    void upsertStatement(KnowledgeBase kb, KBStatement aStatement);
    
    void deleteStatement(KnowledgeBase kb, KBStatement aStatement);

    List<KBStatement> listStatements(KnowledgeBase kb, String aInstance, boolean aAll);
}

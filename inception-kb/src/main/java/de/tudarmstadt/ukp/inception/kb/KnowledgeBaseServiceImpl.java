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

import static org.apache.commons.lang3.StringUtils.startsWithAny;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.lang3.StringUtils;
import org.cyberborean.rdfbeans.datatype.DatatypeMapper;
import org.cyberborean.rdfbeans.datatype.DefaultDatatypeMapper;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.manager.RepositoryInfo;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryProvider;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.repository.sparql.config.SPARQLRepositoryConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.inferencer.fc.config.ForwardChainingRDFSInferencerConfig;
import org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.graph.RdfUtils;

@Component(KnowledgeBaseService.SERVICE_NAME)
public class KnowledgeBaseServiceImpl
        implements KnowledgeBaseService, InitializingBean, DisposableBean {
    
    private final Logger log = LoggerFactory.getLogger(getClass());

    private RepositoryManager repoManager;

    @org.springframework.beans.factory.annotation.Value(value = "${data.path}/kb")
    private File dataDir;

    @PersistenceContext
    private EntityManager entityManager;    

    @Override
    public void afterPropertiesSet() throws Exception {
        String url = dataDir.getAbsoluteFile().toURI().toURL().toString();
        repoManager = RepositoryProvider.getRepositoryManager(url);

        log.info("Knowledge base repository path: " + dataDir);
    }

    @Override
    public void destroy() throws Exception {
        repoManager.shutDown();
    }

    /**
     * Sanity check to test if a knowledge base is already registered with RDF4J.
     * 
     * @param kb
     */
    private void assertRegistration(KnowledgeBase kb) {
        if (!kb.isManagedRepository()) {
            throw new IllegalStateException(kb.toString() + " has to be registered first.");
        }
    }

    @Transactional
    @Override
    public void registerKnowledgeBase(KnowledgeBase kb, RepositoryImplConfig cfg) {
        // obtain unique repository id
        String baseName = Long.toString(kb.getProject().getId());
        if (kb.getName() != null) {
            baseName += kb.getName();
        }
        String repositoryId = repoManager.getNewRepositoryID(baseName);
        kb.setRepositoryId(repositoryId);

        repoManager.addRepositoryConfig(new RepositoryConfig(repositoryId, cfg));
        entityManager.persist(kb);
    }

    @Transactional
    @Override
    public void updateKnowledgeBase(KnowledgeBase kb, RepositoryImplConfig cfg) {
        assertRegistration(kb);
        repoManager.addRepositoryConfig(new RepositoryConfig(kb.getRepositoryId(), cfg));
        entityManager.merge(kb);
    }

    @SuppressWarnings("unchecked")
    @Transactional
    @Override
    public List<KnowledgeBase> getKnowledgeBases(Project aProject) {
        Query query = entityManager.createNamedQuery("KnowledgeBase.getByProject");
        query.setParameter("project", aProject);
        return (List<KnowledgeBase>) query.getResultList();
    }

    @Transactional
    @Override
    public void removeKnowledgeBase(KnowledgeBase kb) {
        assertRegistration(kb);
        repoManager.removeRepository(kb.getRepositoryId());

        entityManager.remove(entityManager.contains(kb) ? kb : entityManager.merge(kb));
    }

    @Override
    public RepositoryInfo getKnowledgeBaseInfo(KnowledgeBase kb) {
        assertRegistration(kb);
        return repoManager.getRepositoryInfo(kb.getRepositoryId());
    }

    @Override
    public RepositoryImplConfig getNativeConfig() {
        return new SailRepositoryConfig(
                new ForwardChainingRDFSInferencerConfig(new NativeStoreConfig()));
    }

    @Override
    public RepositoryImplConfig getRemoteConfig(String url) {
        return new SPARQLRepositoryConfig(url);
    }

    @Override
    public RepositoryImplConfig getKnowledgeBaseConfig(KnowledgeBase kb) {
        assertRegistration(kb);
        return repoManager.getRepositoryConfig(kb.getRepositoryId()).getRepositoryImplConfig();
    }

    @Override
    public RepositoryConnection getConnection(KnowledgeBase kb) {
        if (!kb.isManagedRepository()) {
            throw new IllegalStateException(
                    kb.toString() + " was not added yet, cannot get connection.");
        }
        return repoManager.getRepository(kb.getRepositoryId()).getConnection();
    }

    @SuppressWarnings("resource")
    @Override
    public void importData(KnowledgeBase kb, String aFilename, InputStream aIS)
            throws RDFParseException, RepositoryException, IOException {
        InputStream is = new BufferedInputStream(aIS);
        try {
            // Stream is expected to be closed by caller of importData
            is = new CompressorStreamFactory().createCompressorInputStream(is);
        } catch (CompressorException e) {
            // Probably not compressed then or unknown format - just try as is.
        }

        // Detect the file format
        RDFFormat format = Rio.getParserFormatForFileName(aFilename).orElse(RDFFormat.RDFXML);

        // Load files into the repository
        try (RepositoryConnection conn = getConnection(kb);) {
            conn.add(is, "", format);
        }
    }

    @Override
    public void clear(KnowledgeBase kb) {
        getConnection(kb).clear();
    }

    @Override
    public boolean isEmpty(KnowledgeBase kb) {
        return getConnection(kb).isEmpty();
    }

    @Override
    public KBHandle createConcept(KnowledgeBase kb, KBConcept aConcept) {
        if (StringUtils.isNotEmpty(aConcept.getIdentifier())) {
            throw new IllegalArgumentException("Identifier must be empty on create");
        }

        return update(kb, (conn) -> {
            String identifier = generateIdentifier(conn, kb);
            aConcept.setIdentifier(identifier);
            aConcept.write(conn);
            return new KBHandle(identifier, aConcept.getName());
        });
    }

    @Override
    public KBConcept readConcept(KnowledgeBase kb, String aIdentifier) {
        return read(kb, (conn) -> {
            ValueFactory vf = conn.getValueFactory();
            try (RepositoryResult<Statement> stmts = RdfUtils.getStatements(conn,
                    vf.createIRI(aIdentifier), RDF.TYPE, RDFS.CLASS, true)) {
                if (stmts.hasNext()) {
                    Statement conceptStmt = stmts.next();
                    KBConcept kbConcept = KBConcept.read(conn, conceptStmt);
                    return kbConcept;
                } else {
                    return null;
                }
            }
        });
    }

    @Override
    public void updateConcept(KnowledgeBase kb, KBConcept aConcept) {
        if (StringUtils.isEmpty(aConcept.getIdentifier())) {
            throw new IllegalArgumentException("Identifier cannot be empty on update");
        }

        update(kb, (conn) -> {
            conn.remove(aConcept.getOriginalStatements());
            aConcept.write(conn);
            return null;
        });
    }

    @Override
    public void deleteConcept(KnowledgeBase kb, KBConcept aConcept) {
        delete(kb, aConcept.getIdentifier());
    }

    @Override
    public List<KBHandle> listConcepts(KnowledgeBase kb, boolean aAll) {
        return list(kb, RDFS.CLASS, true, aAll);

        // return read(aProject, (conn) -> {
        // List<KBConcept> concepts = new ArrayList<>();
        //
        // // We need to include inferred statements here, otherwise we won't see e.g. OWL
        // // classes
        // try (RepositoryResult<Statement> stmts = conn.getStatements(null, RDF.TYPE,
        // RDFS.CLASS, true)) {
        // while (stmts.hasNext()) {
        // Statement conceptStmt = stmts.next();
        //
        // if (!aAll && startsWithAny(conceptStmt.getSubject().stringValue(),
        // IMPLICIT_NAMESPACES)) {
        // continue;
        // }
        //
        // // Smells like a blank node
        // if (!conceptStmt.getSubject().stringValue().contains(":")) {
        // continue;
        // }
        //
        // KBConcept kbConcept = KBConcept.read(conn, conceptStmt);
        // concepts.add(kbConcept);
        // }
        // }
        //
        // concepts.sort(Comparator.comparing(KBObject::getUiLabel));
        //
        // return concepts;
        // });
    }

    @Override
    public KBHandle createProperty(KnowledgeBase kb, KBProperty aProperty) {
        if (StringUtils.isNotEmpty(aProperty.getIdentifier())) {
            throw new IllegalArgumentException("Identifier must be empty on create");
        }

        return update(kb, (conn) -> {
            String identifier = generateIdentifier(conn, kb);
            aProperty.setIdentifier(identifier);
            aProperty.write(conn);
            return new KBHandle(identifier, aProperty.getName());
        });        
    }

    @Override
    public KBProperty readProperty(KnowledgeBase kb, String aIdentifier) {
        return read(kb, (conn) -> {
            ValueFactory vf = conn.getValueFactory();
            try (RepositoryResult<Statement> stmts = RdfUtils.getStatements(conn,
                    vf.createIRI(aIdentifier), RDF.TYPE, RDF.PROPERTY, true)) {
                if (stmts.hasNext()) {
                    Statement propStmt = stmts.next();
                    KBProperty kbProp = KBProperty.read(conn, propStmt);
                    return kbProp;
                } else {
                    return null;
                }
            }
        });
    }

    @Override
    public void updateProperty(KnowledgeBase kb, KBProperty aProperty) {
        if (StringUtils.isEmpty(aProperty.getIdentifier())) {
            throw new IllegalArgumentException("Identifier cannot be empty on update");
        }

        update(kb, (conn) -> {
            conn.remove(aProperty.getOriginalStatements());
            aProperty.write(conn);
            return null;
        });
    }

    @Override
    public void deleteProperty(KnowledgeBase kb, KBProperty aType) {
        delete(kb, aType.getIdentifier());
    }

    @Override
    public List<KBHandle> listProperties(KnowledgeBase kb, boolean aAll) {
        return list(kb, RDF.PROPERTY, true, aAll);

        // return read(aProject, (conn) -> {
        // List<KBProperty> properties = new ArrayList<>();
        //
        // // Including inferred statements here will also list e.g. sub-properties
        // try (RepositoryResult<Statement> stmts = conn.getStatements(null, RDF.TYPE,
        // RDF.PROPERTY, true)) {
        // while (stmts.hasNext()) {
        // Statement propStmt = stmts.next();
        //
        // if (!aAll && startsWithAny(propStmt.getSubject().stringValue(),
        // IMPLICIT_NAMESPACES)) {
        // continue;
        // }
        //
        // KBProperty kbProp = KBProperty.read(conn, propStmt);
        // properties.add(kbProp);
        // }
        // }
        //
        // // We don't support blank nodes as ranges or domains
        // properties.removeIf((p) -> {
        // boolean val = (p.getDomain() != null && !p.getDomain().toString()
        // .contains(":")) ||
        // (p.getRange() != null && !p.getRange().toString().contains(":"));
        // if (val) {
        // log.warn("Property [{}] has blank node range [{}] or domain [{}]", p,
        // p.getDomain(), p.getRange());
        // }
        // return val;
        // });
        //
        // properties.sort(Comparator.comparing(KBObject::getUiLabel));
        //
        // return properties;
        // });
    }

    @Override
    public KBHandle createInstance(KnowledgeBase kb, KBInstance aInstance) {
        if (StringUtils.isNotEmpty(aInstance.getIdentifier())) {
            throw new IllegalArgumentException("Identifier must be empty on create");
        }

        return update(kb, (conn) -> {
            String identifier = generateIdentifier(conn, kb);
            aInstance.setIdentifier(identifier);
            aInstance.write(conn);
            
            return new KBHandle(identifier, aInstance.getName());
        });
    }

    @Override
    public KBInstance readInstance(KnowledgeBase kb, String aIdentifier) {
        try (RepositoryConnection conn = getConnection(kb)) {
            ValueFactory vf = conn.getValueFactory();
            // Try to figure out the type of the instance - we ignore the inferred types here
            // and only make use of the explicitly asserted types
            String conceptIdentifier = null;
            try (RepositoryResult<Statement> stmts = RdfUtils.getStatementsSparql(conn,
                    vf.createIRI(aIdentifier), RDF.TYPE, null, false)) {
                while (stmts.hasNext() && conceptIdentifier == null) {
                    Statement stmt = stmts.next();
                    String id = stmt.getObject().stringValue();
                    if (!startsWithAny(id, IMPLICIT_NAMESPACES) && id.contains(":")) {
                        conceptIdentifier = stmt.getObject().stringValue();
                    }
                }
            }

            // Didn't find a suitable concept for the instance - consider the instance as
            // non-existing
            if (conceptIdentifier == null) {
                return null;
            }

            // Read the instance
            try (RepositoryResult<Statement> stmts = RdfUtils.getStatements(conn,
                    vf.createIRI(aIdentifier), RDF.TYPE, vf.createIRI(conceptIdentifier), true)) {
                if (stmts.hasNext()) {
                    Statement kbStmt = stmts.next();
                    KBInstance kbInst = KBInstance.read(conn, kbStmt);
                    return kbInst;
                } else {
                    return null;
                }
            }
        }
    }

    @Override
    public void updateInstance(KnowledgeBase kb, KBInstance aInstance) {
        update(kb, (conn) -> {
            conn.remove(aInstance.getOriginalStatements());
            aInstance.write(conn);
            return null;
        });
    }

    @Override
    public void deleteInstance(KnowledgeBase kb, KBInstance aInstance) {
        delete(kb, aInstance.getIdentifier());
    }

    @Override
    public List<KBHandle> listInstances(KnowledgeBase kb, String aConceptIri, boolean aAll) {
        IRI conceptIri = SimpleValueFactory.getInstance().createIRI(aConceptIri);
        return list(kb, conceptIri, false, aAll);
    }

    @Override
    public void upsertStatement(KnowledgeBase kb, KBStatement aStatement) {
        update(kb, (conn) -> {
            if (!aStatement.isInferred()) {
                conn.remove(aStatement.getOriginalStatements());
            }
            aStatement.write(conn);
            return null;
        });
    }

    @Override
    public void deleteStatement(KnowledgeBase kb, KBStatement aStatement) {
        DatatypeMapper mapper = new DefaultDatatypeMapper();
        update(kb, (conn) -> {
            Statement statement = aStatement.toStatement(conn);
            conn.remove(statement);
            return null;
        });
    }

    private List<Statement> _listStatements(KnowledgeBase kb, String aInstance,
            boolean aIncludeInferred) {
        return read(kb, (conn) -> {
            ValueFactory vf = conn.getValueFactory();
            String QUERY = "SELECT * WHERE { ?s ?p ?o . }";
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
            tupleQuery.setBinding("s", vf.createIRI(aInstance));
            tupleQuery.setIncludeInferred(aIncludeInferred);

            List<Statement> statements = new ArrayList<>();
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                IRI subject = vf.createIRI(aInstance);
                while (result.hasNext()) {
                    BindingSet bindings = result.next();
                    Binding pred = bindings.getBinding("p");
                    Binding obj = bindings.getBinding("o");

                    Statement stmt = vf.createStatement(subject,
                            vf.createIRI(pred.getValue().stringValue()), obj.getValue());
                    statements.add(stmt);
                }
            }
            return statements;
        });
    }

    @Override
    public List<KBStatement> listStatements(KnowledgeBase kb, String aInstance, boolean aAll) {
        Map<String, KBHandle> props = new HashMap<>();
        for (KBHandle prop : listProperties(kb, aAll)) {
            props.put(prop.getIdentifier(), prop);
        }

        DatatypeMapper mapper = new DefaultDatatypeMapper();

        List<Statement> explicitStmts = _listStatements(kb, aInstance, false);

        List<Statement> allStmts = _listStatements(kb, aInstance, true);

        List<KBStatement> statements = new ArrayList<>();
        for (Statement stmt : allStmts) {
            // Can this really happen?
            if (stmt.getObject() == null) {
                continue;
            }

            if (stmt.getObject() instanceof BNode) {
                log.warn("Properties with blank node values are not supported");
                continue;
            }

            KBHandle prop = props.get(stmt.getPredicate().stringValue());
            if (prop == null) {
                // This happens in particular for built-in properties such as
                // RDF / RDFS / OWL properties
                if (aAll) {
                    prop = new KBHandle();
                    prop.setIdentifier(stmt.getPredicate().stringValue());
                    prop.setName(stmt.getPredicate().stringValue());
                } else {
                    continue;
                }
            }

            KBStatement<Serializable> kbStmt = KBStatement.read(new KBHandle(aInstance), prop,
                    !explicitStmts.contains(stmt), stmt);
            statements.add(kbStmt);
        }

        return statements;
    }

    private void delete(KnowledgeBase kb, String aIdentifier) {
        update(kb, (conn) -> {
            ValueFactory vf = conn.getValueFactory();
            try (RepositoryResult<Statement> stmts = conn.getStatements(vf.createIRI(aIdentifier),
                    null, null)) {
                conn.remove(stmts);
            }
            return null;
        });
    }

    private String generateIdentifier(RepositoryConnection conn, KnowledgeBase kb) {
        ValueFactory vf = conn.getValueFactory();
        return KnowledgeBaseService.INCEPTION_NAMESPACE + vf.createBNode().getID();
    }

    private KBHandle update(KnowledgeBase kb, UpdateAction aAction) {
        KBHandle result = null;
        try (RepositoryConnection conn = getConnection(kb)) {
            boolean error = true;
            try {
                conn.begin();
                result = aAction.accept(conn);
                conn.commit();
                error = false;
            } finally {
                if (error) {
                    conn.rollback();
                }
            }
        }
        return result;
    }

    private <T> T read(KnowledgeBase kb, ReadAction<T> aAction) {
        try (RepositoryConnection conn = getConnection(kb)) {
            return aAction.accept(conn);
        }
    }

    private List<KBHandle> list(KnowledgeBase kb, IRI aType, boolean aIncludeInferred,
            boolean aAll) {
        List<KBHandle> resultList = read(kb, (conn) -> {
            String QUERY = "SELECT DISTINCT ?s ?l WHERE { \n" + "  ?s ?pTYPE ?oPROPERTY . \n"
                    + "  OPTIONAL { \n" + "    ?s ?pLABEL ?l . \n"
                    + "    FILTER(LANG(?l) = \"\" || LANGMATCHES(LANG(?l), \"en\")) \n" + "  } \n"
                    + "} \n" + "LIMIT 10000";
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
            tupleQuery.setBinding("pTYPE", RDF.TYPE);
            tupleQuery.setBinding("oPROPERTY", aType);
            tupleQuery.setBinding("pLABEL", RDFS.LABEL);
            tupleQuery.setIncludeInferred(aIncludeInferred);

            List<KBHandle> handles = new ArrayList<>();
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                while (result.hasNext()) {
                    BindingSet bindings = result.next();
                    String id = bindings.getBinding("s").getValue().stringValue();
                    Binding label = bindings.getBinding("l");

                    if (!id.contains(":") || (!aAll && startsWithAny(id, IMPLICIT_NAMESPACES))) {
                        continue;
                    }

                    KBHandle handle = new KBHandle(id);
                    if (label != null) {
                        handle.setName(label.getValue().stringValue());
                    }

                    handles.add(handle);
                }
            }
            return handles;
        });

        resultList.sort(Comparator.comparing(KBObject::getUiLabel));

        return resultList;
    }

    //
    // private void create(KnowledgeBase kb, KBObject aObject)
    // {
    // if (StringUtils.isNotEmpty(aObject.getIdentifier())) {
    // throw new IllegalArgumentException("Identifier must be empty on create");
    // }
    //
    // update(aProject, (manager) -> {
    // // Generate an ID for the new concept
    // aObject.setIdentifier("inception.local:" + aProject.getId() + "#"
    // + manager.getRepositoryConnection().getValueFactory().createBNode().getID());
    // manager.add(aObject);
    // });
    // }
    //
    // private void update(KnowledgeBase kb, KBObject aObject)
    // {
    // if (StringUtils.isEmpty(aObject.getIdentifier())) {
    // throw new IllegalArgumentException("Identifier cannot be empty on update");
    // }
    //
    // update(aProject, (manager) -> manager.update(aObject));
    // }
    //
    // private <T extends KBObject> List<T> list(KnowledgeBase kb, Class<T> aClass)
    // {
    // return read(aProject, (manager) -> {
    // CloseableIteration<T, Exception> iterator = manager.getAll(aClass, true);
    // List<T> result = new ArrayList<>();
    // while (iterator.hasNext()) {
    // T item = iterator.next();
    // if (
    // StringUtils.isNotEmpty(item.getIdentifier()) &&
    // //StringUtils.isNotEmpty(item.getName()) &&
    // !startsWithAny(item.getIdentifier(), IMPLICIT_NAMESPACES)
    // ) {
    // result.add(item);
    // }
    // }
    // result.sort(Comparator.comparing(KBObject::getUiLabel));
    // return result;
    // });
    // }
    //
    private interface UpdateAction {
        KBHandle accept(RepositoryConnection aConnection);
    }

    private interface ReadAction<T> {
        T accept(RepositoryConnection aConnection);
    }

    private interface ListAction {
        KBHandle accept(RepositoryConnection aConnection);
    }
}

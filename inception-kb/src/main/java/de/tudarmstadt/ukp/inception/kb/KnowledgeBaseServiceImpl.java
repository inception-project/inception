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
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
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
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.inferencer.fc.config.ForwardChainingRDFSInferencerConfig;
import org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.graph.RdfUtils;
import de.tudarmstadt.ukp.inception.kb.model.Entity;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

@Component(KnowledgeBaseService.SERVICE_NAME)
public class KnowledgeBaseServiceImpl
    implements KnowledgeBaseService, DisposableBean
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private @PersistenceContext EntityManager entityManager;
    private final RepositoryManager repoManager;
    private @Resource KnowledgeBaseExtensionRegistry extensionRegistry;

    @org.springframework.beans.factory.annotation.Value(value = "${data.path}/kb")
    private File dataDir;

    @Autowired
    public KnowledgeBaseServiceImpl(@org.springframework.beans.factory.annotation.Value("${data.path}") File dataDir)
    {
        String url = Paths.get(dataDir.getAbsolutePath(), "kb").toUri().toString();
        repoManager = RepositoryProvider.getRepositoryManager(url);
        log.info("Knowledge base repository path: " + url);
    }

    public KnowledgeBaseServiceImpl(@org.springframework.beans.factory.annotation.Value("${data.path}") File dataDir,
        EntityManager entityManager)
    {
        this(dataDir);
        this.entityManager = entityManager;
    }

    @Override
    public void destroy() throws Exception
    {
        repoManager.shutDown();
    }

    /**
     * Sanity check to test if a knowledge base is already registered with RDF4J.
     *
     * @param kb
     */
    private void assertRegistration(KnowledgeBase kb)
    {
        if (!kb.isManagedRepository()) {
            throw new IllegalStateException(kb.toString() + " has to be registered first.");
        }
    }

    @Transactional
    @Override
    public void registerKnowledgeBase(KnowledgeBase kb, RepositoryImplConfig cfg, 
            boolean canSupportConceptLinking, String aModelingLanguage) {
        // obtain unique repository id
        String baseName = "pid-" + Long.toString(kb.getProject().getId()) + "-kbid-";
        String repositoryId = repoManager.getNewRepositoryID(baseName);
        kb.setRepositoryId(repositoryId);
        kb.setSupportConceptLinking(canSupportConceptLinking);
        kb.setModelingLanguage(aModelingLanguage);

        repoManager.addRepositoryConfig(new RepositoryConfig(repositoryId, cfg));
        entityManager.persist(kb);
    }

    @Transactional
    @Override
    public boolean knowledgeBaseExists(Project project, String kbName)
    {
        Query query = entityManager.createNamedQuery("KnowledgeBase.getByName");
        query.setParameter("project", project);
        query.setParameter("name", kbName);
        return !query.getResultList().isEmpty();
    }

    @Transactional(noRollbackFor = NoResultException.class)
    @Override
    public Optional<KnowledgeBase> getKnowledgeBaseById(Project aProject, String aId)
    {
        return Optional.ofNullable(entityManager.find(KnowledgeBase.class, aId));
    }

    @Transactional
    @Override
    public void updateKnowledgeBase(KnowledgeBase kb, RepositoryImplConfig cfg)
    {
        assertRegistration(kb);
        repoManager.addRepositoryConfig(new RepositoryConfig(kb.getRepositoryId(), cfg));
        entityManager.merge(kb);
    }

    @SuppressWarnings("unchecked")
    @Transactional
    @Override
    public List<KnowledgeBase> getKnowledgeBases(Project aProject)
    {
        Query query = entityManager.createNamedQuery("KnowledgeBase.getByProject");
        query.setParameter("project", aProject);
        return (List<KnowledgeBase>) query.getResultList();
    }

    @SuppressWarnings("unchecked")
    @Transactional
    @Override
    public List<KnowledgeBase> getEnabledKnowledgeBases(Project aProject)
    {
        Query query = entityManager.createNamedQuery("KnowledgeBase.getByProjectWhereEnabledTrue");
        query.setParameter("project", aProject);
        return (List<KnowledgeBase>) query.getResultList();
    }

    @Transactional
    @Override
    public void removeKnowledgeBase(KnowledgeBase kb)
    {
        assertRegistration(kb);
        repoManager.removeRepository(kb.getRepositoryId());

        entityManager.remove(entityManager.contains(kb) ? kb : entityManager.merge(kb));
    }

    @Override
    public RepositoryInfo getKnowledgeBaseInfo(KnowledgeBase kb)
    {
        assertRegistration(kb);
        return repoManager.getRepositoryInfo(kb.getRepositoryId());
    }

    @Override
    public RepositoryImplConfig getNativeConfig()
    {
        return new SailRepositoryConfig(
            new ForwardChainingRDFSInferencerConfig(new NativeStoreConfig()));
    }

    @Override
    public RepositoryImplConfig getRemoteConfig(String url)
    {
        return new SPARQLRepositoryConfig(url);
    }

    @Override
    public RepositoryImplConfig getKnowledgeBaseConfig(KnowledgeBase kb)
    {
        assertRegistration(kb);
        return repoManager.getRepositoryConfig(kb.getRepositoryId()).getRepositoryImplConfig();
    }

    @Override
    public RepositoryConnection getConnection(KnowledgeBase kb) {
        assertRegistration(kb);
        return repoManager.getRepository(kb.getRepositoryId()).getConnection();
    }

    @SuppressWarnings("resource")
    @Override
    public void importData(KnowledgeBase kb, String aFilename, InputStream aIS)
        throws RDFParseException, RepositoryException, IOException
    {
        InputStream is = new BufferedInputStream(aIS);
        try {
            // Stream is expected to be closed by caller of importData
            is = new CompressorStreamFactory().createCompressorInputStream(is);
        }
        catch (CompressorException e) {
            // Probably not compressed then or unknown format - just try as is.
            log.debug("Stream is not compressed, continue as is.");
        }

        // Detect the file format
        RDFFormat format = Rio.getParserFormatForFileName(aFilename).orElse(RDFFormat.RDFXML);

        // Load files into the repository
        try (RepositoryConnection conn = getConnection(kb)) {
            conn.add(is, "", format);
        }
    }

    @Override
    public void exportData(KnowledgeBase kb, RDFFormat format, OutputStream os)
    {
        if (kb.getType() != RepositoryType.LOCAL) {
            log.info("Not exporting non-local knowledge base: [{}]", kb.getName());
            return;
        }
        try (RepositoryConnection conn = getConnection(kb)) {
            RDFWriter rdfWriter = Rio.createWriter(format, os);
            conn.export(rdfWriter);
        }
    }

    @Override
    public void clear(KnowledgeBase kb)
    {
        try (RepositoryConnection conn = getConnection(kb)) {
            conn.clear();
        }
    }

    @Override
    public boolean isEmpty(KnowledgeBase kb)
    {
        try (RepositoryConnection conn = getConnection(kb)) {
            return conn.isEmpty();
        }
    }

    @Override
    public KBHandle createConcept(KnowledgeBase kb, KBConcept aConcept)
    {
        if (StringUtils.isNotEmpty(aConcept.getIdentifier())) {
            throw new IllegalArgumentException("Identifier must be empty on create");
        }

        return update(kb, (conn) -> {
            String identifier = generateIdentifier(conn, kb);
            aConcept.setIdentifier(identifier);
            aConcept.write(conn, kb);
            return new KBHandle(identifier, aConcept.getName());
        });
    }

    @Override
    public Optional<KBConcept> readConcept(KnowledgeBase kb, String aIdentifier)
    {
        return read(kb, (conn) -> {
            ValueFactory vf = conn.getValueFactory();
            Value modelingLanguage;
            if (kb.getModelingLanguage().equals("OWL")) {
                modelingLanguage = OWL.CLASS;
            } else {
                modelingLanguage = RDFS.CLASS;
            }

            try (RepositoryResult<Statement> stmts = RdfUtils.getStatements(conn,
                    vf.createIRI(aIdentifier), kb.getTypeIri(), kb.getClassIri(), true)) {
                if (stmts.hasNext()) {
                    Statement conceptStmt = stmts.next();
                    KBConcept kbConcept = KBConcept.read(conn, conceptStmt);
                    return Optional.of(kbConcept);
                } else {
                    return Optional.empty();
                }
            } catch (QueryEvaluationException e) {
                log.warn("Reading concept failed", e);
                return Optional.empty();
            }
        });
    }
    
    @Override
    public Optional<KBConcept> readConcept(Project aProject, String aIdentifier)
    {
        for (KnowledgeBase kb : getKnowledgeBases(aProject)) {
            Optional<KBConcept> concept = readConcept(kb, aIdentifier);
            if (concept.isPresent()) {
                return concept;
            }
        }
        
        return Optional.empty();
    }
    
    @Override
    public void updateConcept(KnowledgeBase kb, KBConcept aConcept)
    {
        if (StringUtils.isEmpty(aConcept.getIdentifier())) {
            throw new IllegalArgumentException("Identifier cannot be empty on update");
        }

        update(kb, (conn) -> {
            conn.remove(aConcept.getOriginalStatements());
            aConcept.write(conn, kb);
            return null;
        });
    }

    @Override
    public void deleteConcept(KnowledgeBase kb, KBConcept aConcept)
    {
        delete(kb, aConcept.getIdentifier());
    }

    @Override
    public List<KBHandle> listConcepts(KnowledgeBase kb, boolean aAll)
    {
        if (kb.canSupportConceptLinking()) {
            return listLinkingConcepts(kb, true, aAll);
        } else {
            return list(kb, RDFS.CLASS, true, aAll);
        }
    }

    @Override
    public KBHandle createProperty(KnowledgeBase kb, KBProperty aProperty)
    {
        if (StringUtils.isNotEmpty(aProperty.getIdentifier())) {
            throw new IllegalArgumentException("Identifier must be empty on create");
        }

        return update(kb, (conn) -> {
            String identifier = generateIdentifier(conn, kb);
            aProperty.setIdentifier(identifier);
            aProperty.write(conn, kb);
            return new KBHandle(identifier, aProperty.getName());
        });
    }

    @Override
    public Optional<KBProperty> readProperty(KnowledgeBase kb, String aIdentifier)
    {
        return read(kb, (conn) -> {
            ValueFactory vf = conn.getValueFactory();
            try (RepositoryResult<Statement> stmts = RdfUtils.getStatements(conn,
                    vf.createIRI(aIdentifier), kb.getTypeIri(), RDF.PROPERTY, true)) {
                if (stmts.hasNext()) {
                    Statement propStmt = stmts.next();
                    KBProperty kbProp = KBProperty.read(conn, propStmt);
                    return Optional.of(kbProp);
                } else {
                    return Optional.empty();
                }
            } catch (QueryEvaluationException e) {
                log.warn("Reading property failed.", e);
                return Optional.empty();
            }
        });
    }

    @Override
    public void updateProperty(KnowledgeBase kb, KBProperty aProperty)
    {
        if (StringUtils.isEmpty(aProperty.getIdentifier())) {
            throw new IllegalArgumentException("Identifier cannot be empty on update");
        }

        update(kb, (conn) -> {
            conn.remove(aProperty.getOriginalStatements());
            aProperty.write(conn, kb);
            return null;
        });
    }

    @Override
    public void deleteProperty(KnowledgeBase kb, KBProperty aType)
    {
        delete(kb, aType.getIdentifier());
    }

    @Override
    public List<KBHandle> listProperties(KnowledgeBase kb, boolean aAll)
    {
        return list(kb, RDF.PROPERTY, true, aAll);
    }

    @Override
    public KBHandle createInstance(KnowledgeBase kb, KBInstance aInstance)
    {
        if (StringUtils.isNotEmpty(aInstance.getIdentifier())) {
            throw new IllegalArgumentException("Identifier must be empty on create");
        }

        return update(kb, (conn) -> {
            String identifier = generateIdentifier(conn, kb);
            aInstance.setIdentifier(identifier);
            aInstance.write(conn, kb);

            return new KBHandle(identifier, aInstance.getName());
        });
    }

    @Override
    public Optional<KBInstance> readInstance(KnowledgeBase kb, String aIdentifier)
    {
        try (RepositoryConnection conn = getConnection(kb)) {
            ValueFactory vf = conn.getValueFactory();
            // Try to figure out the type of the instance - we ignore the inferred types here
            // and only make use of the explicitly asserted types
            RepositoryResult<Statement> conceptStmts = RdfUtils.getStatementsSparql(conn,
                    vf.createIRI(aIdentifier), kb.getTypeIri(), null, false);

            String conceptIdentifier = null;
            while (conceptStmts.hasNext() && conceptIdentifier == null) {
                Statement stmt = conceptStmts.next();
                String id = stmt.getObject().stringValue();
                if (!startsWithAny(id, IMPLICIT_NAMESPACES) && id.contains(":")) {
                    conceptIdentifier = stmt.getObject().stringValue();
                }
            }

            // Didn't find a suitable concept for the instance - consider the instance as
            // non-existing
            if (conceptIdentifier == null) {
                return Optional.empty();
            }

            // Read the instance
            try (RepositoryResult<Statement> instanceStmts = RdfUtils.getStatements(conn,
                    vf.createIRI(aIdentifier), kb.getTypeIri(), vf.createIRI(conceptIdentifier),
                    true)) {
                if (instanceStmts.hasNext()) {
                    Statement kbStmt = instanceStmts.next();
                    KBInstance kbInst = KBInstance.read(conn, kbStmt);
                    return Optional.of(kbInst);
                } else {
                    return Optional.empty();
                }
            }
        } catch (QueryEvaluationException e) {
            log.warn("Reading concept for instance failed.", e);
            return Optional.empty();
        }
    }
    
    @Override
    public Optional<KBInstance> readInstance(Project aProject, String aIdentifier)
    {
        for (KnowledgeBase kb : getKnowledgeBases(aProject)) {
            Optional<KBInstance> instance = readInstance(kb, aIdentifier);
            if (instance.isPresent()) {
                return instance;
            }
        }
        
        return Optional.empty();
    }

    @Override
    public void updateInstance(KnowledgeBase kb, KBInstance aInstance)
    {
        if (StringUtils.isEmpty(aInstance.getIdentifier())) {
            throw new IllegalArgumentException("Identifier cannot be empty on update");
        }

        update(kb, (conn) -> {
            conn.remove(aInstance.getOriginalStatements());
            aInstance.write(conn ,kb);
            return null;
        });
    }

    @Override
    public void deleteInstance(KnowledgeBase kb, KBInstance aInstance)
    {
        delete(kb, aInstance.getIdentifier());
    }

    @Override
    public List<KBHandle> listInstances(KnowledgeBase kb, String aConceptIri, boolean aAll, 
            AnnotatorState aState, AnnotationActionHandler aActionHandler) {
        IRI conceptIri = SimpleValueFactory.getInstance().createIRI(aConceptIri);
        if (kb.canSupportConceptLinking()) {
            return listLinkingInstances(kb, conceptIri, false, aAll, aState, aActionHandler);
        } else {
            return list(kb, conceptIri, false, aAll);
        }
    }
    
    private List<KBHandle> listLinkingInstances(KnowledgeBase kb, IRI conceptIri,
            boolean aIncludeInferred, boolean aAll, AnnotatorState aState, 
            AnnotationActionHandler aActionHandler)
    {
        List<KBHandle> resultList = read(kb, (conn) -> {
            List<Entity> candidates = extensionRegistry.fireDisambiguate(kb, conceptIri, aState, 
                    aActionHandler);
            List<KBHandle> handles = new ArrayList<>();

            for (Entity c: candidates) {
                String id = c.getIRI();
                String label = c.getLabel();

                if (!id.contains(":") || (!aAll && startsWithAny(id, IMPLICIT_NAMESPACES))) {
                    continue;
                }

                KBHandle handle = new KBHandle(id, label);
                handles.add(handle);
            }

            return handles;
        });

        return resultList;
    }


    @Override
    public void upsertStatement(KnowledgeBase kb, KBStatement aStatement)
    {
        update(kb, (conn) -> {
            if (!aStatement.isInferred()) {
                conn.remove(aStatement.getOriginalStatements());
            }
            aStatement.write(conn);
            return null;
        });
    }

    @Override
    public void deleteStatement(KnowledgeBase kb, KBStatement aStatement)
    {
        update(kb, (conn) -> {
            Statement statement = aStatement.toStatement(conn);
            conn.remove(statement);
            return null;
        });
    }

    @Override
    public List<KBStatement> listStatements(KnowledgeBase kb, KBHandle aInstance, boolean aAll)
    {
        Map<String, KBHandle> props = new HashMap<>();
        for (KBHandle prop : listProperties(kb, aAll)) {
            props.put(prop.getIdentifier(), prop);
        }

        List<Statement> explicitStmts = listStatements(kb, aInstance.getIdentifier(), false);
        List<Statement> allStmts = listStatements(kb, aInstance.getIdentifier(), true);

        List<KBStatement> statements = new ArrayList<>();
        for (Statement stmt : allStmts) {
            // Can this really happen?
            if (stmt.getObject() == null) {
                log.warn("Property with null value detected.");
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
                }
                else {
                    continue;
                }
            }

            KBStatement kbStmt = KBStatement.read(aInstance, prop, stmt);
            kbStmt.setInferred(!explicitStmts.contains(stmt));
            statements.add(kbStmt);
        }

        return statements;
    }

    @Override
    public List<KBStatement> listStatements(KnowledgeBase kb, KBInstance aInstance, boolean aAll)
    {
        KBHandle handle = new KBHandle(aInstance.getIdentifier(), aInstance.getName());
        return listStatements(kb, handle, aAll);
    }

    @Override
private List<Statement> listStatements(KnowledgeBase kb, String aInstanceIdentifier,
                                           boolean aIncludeInferred)
    {
        return read(kb, (conn) -> {
            ValueFactory vf = conn.getValueFactory();
            String QUERY = "SELECT * WHERE { ?s ?p ?o . }";
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
            tupleQuery.setBinding("s", vf.createIRI(aInstanceIdentifier));
            tupleQuery.setIncludeInferred(aIncludeInferred);

            TupleQueryResult result;
            try {
                result = tupleQuery.evaluate();
            } catch (QueryEvaluationException e) {
                log.warn("Listing statements failed.", e);
                return Collections.emptyList();
            }

            List<Statement> statements = new ArrayList<>();
            IRI subject = vf.createIRI(aInstanceIdentifier);
            while (result.hasNext()) {
                BindingSet bindings = result.next();
                Binding pred = bindings.getBinding("p");
                Binding obj = bindings.getBinding("o");

                IRI predicate = vf.createIRI(pred.getValue().stringValue());
                Statement stmt = vf.createStatement(subject, predicate, obj.getValue());
                statements.add(stmt);
            }
            return statements;
        });
    }

    private void delete(KnowledgeBase kb, String aIdentifier)
    {
        update(kb, (conn) -> {
            ValueFactory vf = conn.getValueFactory();
            try (RepositoryResult<Statement> stmts = conn
                .getStatements(vf.createIRI(aIdentifier), null, null)) {
                conn.remove(stmts);
            }
            return null;
        });
    }

    private String generateIdentifier(RepositoryConnection conn, KnowledgeBase kb)
    {
        ValueFactory vf = conn.getValueFactory();
        return KnowledgeBaseService.INCEPTION_NAMESPACE + vf.createBNode().getID();
    }

    private KBHandle update(KnowledgeBase kb, UpdateAction aAction)
    {
        if (kb.isReadOnly()) {
            log.warn("Knowledge base [{}] is read only, will not alter!", kb.getName());
            return null;
        }

        KBHandle result = null;
        try (RepositoryConnection conn = getConnection(kb)) {
            boolean error = true;
            try {
                conn.begin();
                result = aAction.accept(conn);
                conn.commit();
                error = false;
            }
            finally {
                if (error) {
                    conn.rollback();
                }
            }
        }
        return result;
    }

    private <T> T read(KnowledgeBase kb, ReadAction<T> aAction)
    {
        try (RepositoryConnection conn = getConnection(kb)) {
            return aAction.accept(conn);
        }
    }

    private List<KBHandle> list(KnowledgeBase kb, IRI aType, boolean aIncludeInferred, boolean aAll)
    {
        List<KBHandle> resultList = read(kb, (conn) -> {
            String QUERY = String.join("\n"
                         , "SELECT DISTINCT ?s ?l WHERE {"
                         , "  ?s ?pTYPE ?oPROPERTY ."
                         , "  OPTIONAL {"
                         , "    ?s ?pLABEL ?l ."
                         , "    FILTER(LANG(?l) = \"\" || LANGMATCHES(LANG(?l), \"en\"))"
                         , "  }"
                         , "}"
                         , "LIMIT 10000");
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
            tupleQuery.setBinding("pTYPE", kb.getTypeIri());
            tupleQuery.setBinding("oPROPERTY", aType);
            tupleQuery.setBinding("pLABEL", RDFS.LABEL);
            tupleQuery.setIncludeInferred(aIncludeInferred);

            return evaluateListQuery(tupleQuery, aAll);
        });

        resultList.sort(Comparator.comparing(KBObject::getUiLabel));

        return resultList;
    }

    @Override
    public List<KBHandle> listRootConcepts(KnowledgeBase kb, boolean aAll)
    {
        List<KBHandle> resultList = read(kb, (conn) -> {
            String QUERY = String.join("\n"
                , "SELECT DISTINCT ?s ?l WHERE { "
                , "  ?s ?pTYPE ?oCLASS . "
                , "FILTER NOT EXISTS { "
                , "  ?s ?pSUBCLASS ?otherSub . "
                , "} OPTIONAL { "
                , "    ?s ?pLABEL ?l . "
                , "    FILTER(LANG(?l) = \"\" || LANGMATCHES(LANG(?l), \"en\")) "
                , "  } "
                , "} "
                , "LIMIT 10000");
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
            tupleQuery.setBinding("pTYPE", kb.getTypeIri());
            tupleQuery.setBinding("oCLASS", kb.getClassIri());
            tupleQuery.setBinding("pSUBCLASS", kb.getSubclassIri());
            tupleQuery.setBinding("pLABEL", RDFS.LABEL);
            tupleQuery.setIncludeInferred(false);

            return evaluateListQuery(tupleQuery, aAll);
        });

        resultList.sort(Comparator.comparing(KBObject::getUiLabel));
        return resultList;
    }

    @Override
    public List<KBHandle> listChildConcepts(KnowledgeBase kb, String parentIdentifier, boolean aAll)
    {
        // The query below only returns subclasses which simultaneously declare being a class
        // via the class property defined in the KB specification. This means that if the KB
        // is configured to use rdfs:Class but a subclass defines itself using owl:Class, then
        // this subclass is *not* returned. We do presently *not* support mixed schemes in a
        // single KB.
        List<KBHandle> resultList = read(kb, (conn) -> {
            String QUERY = String.join("\n"
                , "SELECT DISTINCT ?s ?l WHERE { "
                , "  ?s ?pSUBCLASS ?oPARENT . "
                , "  ?s ?pTYPE ?oCLASS . "
                , "  OPTIONAL { "
                , "    ?s ?pLABEL ?l . "
                , "    FILTER(LANG(?l) = \"\" || LANGMATCHES(LANG(?l), \"en\")) "
                , "  } "
                , "} "
                , "LIMIT 10000");
            ValueFactory vf = SimpleValueFactory.getInstance();
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
            tupleQuery.setBinding("oPARENT", vf.createIRI(parentIdentifier));
            tupleQuery.setBinding("pTYPE", kb.getTypeIri());
            tupleQuery.setBinding("oCLASS", kb.getClassIri());
            tupleQuery.setBinding("pSUBCLASS", kb.getSubclassIri());
            tupleQuery.setBinding("pLABEL", RDFS.LABEL);
            tupleQuery.setIncludeInferred(false);

            return evaluateListQuery(tupleQuery, aAll);
        });

        resultList.sort(Comparator.comparing(KBObject::getUiLabel));
        return resultList;
    }
    
    // TODO make language configurable 
    private List<KBHandle> listLinkingConcepts(KnowledgeBase kb, boolean aIncludeInferred,
            boolean aAll)
    {
        List<KBHandle> resultList = read(kb, (conn) -> {
            String QUERY = "SELECT DISTINCT ?any ?l WHERE { \n" 
                    + "  ?s ?pTYPE ?any . \n"
                    + "  OPTIONAL { \n" 
                    + "    ?any ?pLABEL ?l . \n"
                    + "    FILTER(LANG(?l) = \"\" || LANGMATCHES(LANG(?l), \"" 
                    + "en" 
                    + "\")) \n" 
                    + "  } \n"
                    + "} \n" + "LIMIT 10000";
            
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
            tupleQuery.setBinding("pTYPE", RDFS.SUBCLASSOF);
            tupleQuery.setBinding("pLABEL", RDFS.LABEL);
            tupleQuery.setIncludeInferred(aIncludeInferred);
            log.info("waiting for query...");
            
            List<KBHandle> handles = new ArrayList<>();
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                while (result.hasNext()) {
                    BindingSet bindings = result.next();
                    String id = bindings.getBinding("any").getValue().stringValue();
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
            } catch (QueryEvaluationException | RepositoryException e) {
                log.error("Error: ", e);
            }
            return handles;
        });

        resultList.sort(Comparator.comparing(KBObject::getUiLabel));

        return resultList;
    }

    private List<KBHandle> evaluateListQuery(TupleQuery tupleQuery, boolean aAll) {
        TupleQueryResult result;
        try {
            result = tupleQuery.evaluate();
        } catch (QueryEvaluationException e) {
            log.warn("Evaluating list query failed.", e);
            return Collections.emptyList();
        }
        
        List<KBHandle> handles = new ArrayList<>();
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
        return handles;
    }

    private interface UpdateAction
    {
        KBHandle accept(RepositoryConnection aConnection);
    }

    private interface ReadAction<T>
    {
        T accept(RepositoryConnection aConnection);
    }
}

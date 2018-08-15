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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
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
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryProvider;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.repository.sparql.config.SPARQLRepositoryConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBQualifier;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.graph.RdfUtils;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.reification.NoReification;
import de.tudarmstadt.ukp.inception.kb.reification.ReificationStrategy;
import de.tudarmstadt.ukp.inception.kb.reification.WikiDataReification;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseProfile;

@Component(KnowledgeBaseService.SERVICE_NAME)
public class KnowledgeBaseServiceImpl
    implements KnowledgeBaseService, DisposableBean
{
    private static final String KNOWLEDGEBASE_PROFILES_YAML = "knowledgebase-profiles.yaml";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private @PersistenceContext EntityManager entityManager;
    private final RepositoryManager repoManager;
    private final Set<String> implicitNamespaces;

    @org.springframework.beans.factory.annotation.Value(value = "${data.path}/kb")
    private File dataDir;

    @Autowired
    public KnowledgeBaseServiceImpl(
            @org.springframework.beans.factory.annotation.Value("${data.path}") File dataDir)
    {
        String url = Paths.get(dataDir.getAbsolutePath(), "kb").toUri().toString();
        repoManager = RepositoryProvider.getRepositoryManager(url);
        log.info("Knowledge base repository path: " + url);
        implicitNamespaces = IriConstants.IMPLICIT_NAMESPACES;
    }

    public KnowledgeBaseServiceImpl(
            @org.springframework.beans.factory.annotation.Value("${data.path}") File dataDir,
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
    public void registerKnowledgeBase(KnowledgeBase kb, RepositoryImplConfig cfg)
        throws RepositoryException, RepositoryConfigException
    {
        // obtain unique repository id
        String baseName = "pid-" + Long.toString(kb.getProject().getId()) + "-kbid-";
        String repositoryId = repoManager.getNewRepositoryID(baseName);
        kb.setRepositoryId(repositoryId);

        repoManager.addRepositoryConfig(new RepositoryConfig(repositoryId, cfg));
        entityManager.persist(kb);
    }
    
    @Override
    public void defineBaseProperties(KnowledgeBase akb) 
    {
        // KB will initialize base properties with base IRI schema properties defined by user
        if (akb.getType() == RepositoryType.LOCAL) {
            createBaseProperty(akb, new KBProperty(akb.getSubclassIri().getLocalName(),
                    akb.getSubclassIri().stringValue()));
            createBaseProperty(akb, new KBProperty(akb.getLabelIri().getLocalName(),
                    akb.getLabelIri().stringValue()));
            createBaseProperty(akb, new KBProperty(akb.getDescriptionIri().getLocalName(),
                    akb.getDescriptionIri().stringValue()));
        }
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
        throws RepositoryException, RepositoryConfigException
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
        throws RepositoryException, RepositoryConfigException
    {
        assertRegistration(kb);
        repoManager.removeRepository(kb.getRepositoryId());

        entityManager.remove(entityManager.contains(kb) ? kb : entityManager.merge(kb));
    }

    @Override
    public RepositoryImplConfig getNativeConfig()
    {
        // See #221 - Disabled because it is too slow during import
        // return new SailRepositoryConfig(
        //   new ForwardChainingRDFSInferencerConfig(new NativeStoreConfig()));
        
        return new SailRepositoryConfig(new NativeStoreConfig());
    }

    @Override
    public RepositoryImplConfig getRemoteConfig(String url)
    {
        return new SPARQLRepositoryConfig(url);
    }

    @Override
    public RepositoryImplConfig getKnowledgeBaseConfig(KnowledgeBase kb)
        throws RepositoryConfigException, RepositoryException
    {
        assertRegistration(kb);
        return repoManager.getRepositoryConfig(kb.getRepositoryId()).getRepositoryImplConfig();
    }

    @Override
    public void registerImplicitNamespace(String aImplicitNameSpace)
    {
        implicitNamespaces.add(aImplicitNameSpace);
    }

    @Override
    public RepositoryConnection getConnection(KnowledgeBase kb)
    {
        assertRegistration(kb);
        return repoManager.getRepository(kb.getRepositoryId()).getConnection();
    }

    @SuppressWarnings("resource")
    @Override
    public void importData(KnowledgeBase kb, String aFilename, InputStream aIS)
        throws RDFParseException, RepositoryException, IOException
    {
        if (kb.isReadOnly()) {
            log.warn("Knowledge base [{}] is read only, will not import!", kb.getName());
            return;
        }

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
        throws QueryEvaluationException
    {
        return read(kb, (conn) -> {
            ValueFactory vf = conn.getValueFactory();
            Resource subject = vf.createIRI(aIdentifier);
            if (RdfUtils.existsStatementsWithSubject(conn, subject, false)) {
                KBConcept kbConcept = KBConcept.read(conn, vf.createIRI(aIdentifier),kb);
                return Optional.of(kbConcept);
            }
            else {
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
        getReificationStrategy(kb).deleteConcept(kb, aConcept);
    }

    @Override
    public List<KBHandle> listConcepts(KnowledgeBase kb, boolean aAll)
    {
        return list(kb, kb.getClassIri(), true, aAll);
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
            try (RepositoryResult<Statement> stmts = RdfUtils.getPropertyStatementsSparql(conn,
                    vf.createIRI(aIdentifier), kb.getTypeIri(), kb.getPropertyTypeIri(), 1000, true,
                    null)) {
                if (stmts.hasNext()) {
                    Statement propStmt = stmts.next();
                    KBProperty kbProp = KBProperty.read(conn, propStmt, kb);
                    return Optional.of(kbProp);
                } else {
                    return Optional.empty();
                }
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
        getReificationStrategy(kb).deleteProperty(kb, aType);
    }

    @Override
    public List<KBHandle> listProperties(KnowledgeBase kb, boolean aAll)
    {
        return listProperties(kb, true, aAll);
    }

    @Override
    public List<KBHandle> listProperties(KnowledgeBase kb, boolean aIncludeInferred, boolean aAll)
        throws QueryEvaluationException
    {
        List<KBHandle> resultList = read(kb, (conn) -> {
            String QUERY = getPropertyListQuery(kb);
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
            tupleQuery.setBinding("pTYPE", kb.getTypeIri());
            tupleQuery.setBinding("oPROPERTY", kb.getPropertyTypeIri());
            tupleQuery.setBinding("pLABEL", kb.getLabelIri());
            tupleQuery.setIncludeInferred(aIncludeInferred);
            return evaluateListQuery(tupleQuery, aAll);
        });
        resultList.sort(Comparator.comparing(KBObject::getUiLabel));
        return resultList;
    }
    
    public String getPropertyListQuery(KnowledgeBase kb) {
        return SPARQLQueryStore.PROPERTYLIST_QUERY;
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
        throws QueryEvaluationException
    {
        try (RepositoryConnection conn = getConnection(kb)) {
            ValueFactory vf = conn.getValueFactory();
            // Try to figure out the type of the instance - we ignore the inferred types here
            // and only make use of the explicitly asserted types
            RepositoryResult<Statement> conceptStmts = RdfUtils.getStatementsSparql(conn,
                    vf.createIRI(aIdentifier), kb.getTypeIri(), null, 1000, false, null);

            String conceptIdentifier = null;
            while (conceptStmts.hasNext() && conceptIdentifier == null) {
                Statement stmt = conceptStmts.next();
                String id = stmt.getObject().stringValue();
                if (!hasImplicitNamespace(id) && id.contains(":")) {
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
                    KBInstance kbInst = KBInstance.read(conn, kbStmt, kb);
                    return Optional.of(kbInst);
                } else {
                    return Optional.empty();
                }
            }
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
        getReificationStrategy(kb).deleteInstance(kb, aInstance);
    }

    @Override
    public List<KBHandle> listInstances(KnowledgeBase kb, String aConceptIri, boolean aAll)
    {
        IRI conceptIri = SimpleValueFactory.getInstance().createIRI(aConceptIri);
        return list(kb, conceptIri, false, aAll);
    }

    // Statements

    @Override
    public void initStatement(KnowledgeBase kb, KBStatement aStatement)
    {
        Set<Statement> statements = getReificationStrategy(kb).reify(kb, aStatement);
        aStatement.setOriginalStatements(statements);
    }

    @Override
    public void upsertStatement(KnowledgeBase kb, KBStatement aStatement) throws RepositoryException
    {
        getReificationStrategy(kb).upsertStatement(kb, aStatement);
    }

    @Override
    public void deleteStatement(KnowledgeBase kb, KBStatement aStatement) throws RepositoryException
    {
        getReificationStrategy(kb).deleteStatement(kb, aStatement);
    }

    @Override
    public List<KBStatement> listStatements(KnowledgeBase kb, KBHandle aInstance, boolean aAll)
    {
        return getReificationStrategy(kb).listStatements(kb, aInstance, aAll);
    }

    @Override
    public List<KBStatement> listStatements(KnowledgeBase kb, KBInstance aInstance, boolean aAll)
    {
        KBHandle handle = new KBHandle(aInstance.getIdentifier(), aInstance.getName());
        return listStatements(kb, handle, aAll);
    }
    
    @Override
    public List<Statement> listStatementsWithPredicateOrObjectReference(KnowledgeBase kb,
            String aIdentifier)
    {
        try (RepositoryConnection conn = getConnection(kb)) {
            ValueFactory vf = conn.getValueFactory();
            IRI iri = vf.createIRI(aIdentifier);
            try (RepositoryResult<Statement> predStmts = conn.getStatements(null, iri, null);
                    RepositoryResult<Statement> objStmts = conn.getStatements(null, null, iri)) {
                List<Statement> allStmts = new ArrayList<>();
                Iterations.addAll(predStmts, allStmts);
                Iterations.addAll(objStmts, allStmts);
                return allStmts;

            }
        }
    }

    private String generateIdentifier(RepositoryConnection conn, KnowledgeBase kb)
    {
        ValueFactory vf = conn.getValueFactory();
        // default value of basePrefix is IriConstants.INCEPTION_NAMESPACE
        String basePrefix = kb.getBasePrefix();
        return basePrefix + vf.createBNode().getID();
    }

    @Override
    public KBHandle update(KnowledgeBase kb, UpdateAction aAction)
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

    @Override
    public <T> T read(KnowledgeBase kb, ReadAction<T> aAction)
    {
        try (RepositoryConnection conn = getConnection(kb)) {
            return aAction.accept(conn);
        }
    }

    @Override
    public List<KBHandle> list(KnowledgeBase kb, IRI aType, boolean aIncludeInferred, boolean aAll)
        throws QueryEvaluationException
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
            tupleQuery.setBinding("pLABEL", kb.getLabelIri());
            tupleQuery.setIncludeInferred(aIncludeInferred);

            return evaluateListQuery(tupleQuery, aAll);
        });

        resultList.sort(Comparator.comparing(KBObject::getUiLabel));

        return resultList;
    }
    
    @Override
    public List<KBHandle> listDomainProperties(KnowledgeBase kb, String aDomain,
            boolean aIncludeInferred, boolean aAll)
        throws QueryEvaluationException
    {
        List<KBHandle> resultList = read(kb, (conn) -> {
            ValueFactory vf = conn.getValueFactory();
            String QUERY = SPARQLQueryStore.PROPERTYLIST_DOMAIN_DEPENDENT;
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
            tupleQuery.setBinding("aDomain", vf.createIRI(aDomain));
            tupleQuery.setBinding("pLABEL", kb.getLabelIri());
            tupleQuery.setIncludeInferred(aIncludeInferred);

            return evaluateListQuery(tupleQuery, aAll);
        });
        
        // Sorting is not done as part of SPARQL queries as it will be more expensive on
        // SPARQL with sorting the whole data and then setting a limit for the number of
        // result data set and hence will also skip number of results as part of sorted
        // data from SPARQL
        resultList.sort(Comparator.comparing(KBObject::getUiLabel));
        return resultList;
    }
    
    @Override
    public List<KBHandle> listPropertiesRangeValue(KnowledgeBase kb, String aProperty,
            boolean aIncludeInferred, boolean aAll)
        throws QueryEvaluationException
    {
        List<KBHandle> resultList = read(kb, (conn) -> {
            ValueFactory vf = conn.getValueFactory();
            String QUERY = SPARQLQueryStore.PROPERTY_SPECIFIC_RANGE;
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
            tupleQuery.setBinding("aProperty", vf.createIRI(aProperty));
            tupleQuery.setBinding("pLABEL", kb.getLabelIri());
            tupleQuery.setIncludeInferred(aIncludeInferred);

            return evaluateListQuery(tupleQuery, aAll);
        });
        
        resultList.sort(Comparator.comparing(KBObject::getUiLabel));
        return resultList;
    }
    
    @Override
    public List<KBHandle> listProperties(KnowledgeBase kb, IRI aType, boolean aIncludeInferred,
            boolean aAll)
        throws QueryEvaluationException
    {
        List<KBHandle> resultList = read(kb, (conn) -> {
            String QUERY = getPropertyListQuery(kb);
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
            tupleQuery.setBinding("pTYPE", kb.getTypeIri());
            tupleQuery.setBinding("oPROPERTY", aType);
            tupleQuery.setBinding("pLABEL", kb.getLabelIri());
            tupleQuery.setIncludeInferred(aIncludeInferred);

            return evaluateListQuery(tupleQuery, aAll);
        });

        resultList.sort(Comparator.comparing(KBObject::getUiLabel));

        return resultList;
    }
    
    @Override
    public List<KBHandle> listRootConcepts(KnowledgeBase kb, boolean aAll)
        throws QueryEvaluationException
    {
        List<KBHandle> resultList = new ArrayList<>();

        if (!kb.getExplicitlyDefinedRootConcepts().isEmpty()) {
            for (IRI conceptIRI : kb.getExplicitlyDefinedRootConcepts()) {
                KBConcept concept = readConcept(kb, conceptIRI.stringValue()).get();
                KBHandle conceptHandle = new KBHandle(concept.getIdentifier(), concept.getName(),
                        concept.getDescription());
                resultList.add(conceptHandle);
            }
        }
        else {
            resultList = read(kb, (conn) -> {
                String QUERY = String.join("\n"
                    , SPARQLQueryStore.SPARQL_PREFIX
                    , "SELECT DISTINCT ?s ?l WHERE { "
                    , "     { ?s ?pTYPE ?oCLASS . } "
                    , "     UNION { ?someSubClass ?pSUBCLASS ?s . } ."
                    , "     FILTER NOT EXISTS { "
                    , "         ?s ?pSUBCLASS ?otherSub . "
                    , "         FILTER (?s != ?otherSub) }"
                    , "     FILTER NOT EXISTS { "
                    , "         ?s owl:intersectionOf ?list . }"
                    , "     OPTIONAL { "
                    , "         ?s ?pLABEL ?l . "
                    , "         FILTER(LANG(?l) = \"\" || LANGMATCHES(LANG(?l), \"en\")) "
                    , "     } "
                    , "} "
                    , "LIMIT 10000" );
                TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
                tupleQuery.setBinding("pTYPE", kb.getTypeIri());
                tupleQuery.setBinding("oCLASS", kb.getClassIri());
                tupleQuery.setBinding("pSUBCLASS", kb.getSubclassIri());
                tupleQuery.setBinding("pLABEL", kb.getLabelIri());
                tupleQuery.setIncludeInferred(false);
    
                return evaluateListQuery(tupleQuery, aAll);
            });
        }
        resultList.sort(Comparator.comparing(KBObject::getUiLabel));
        return resultList;
    }
    
    @Override
    public boolean hasChildConcepts(KnowledgeBase aKB, String aParentIdentifier, boolean aAll)
    {
        return !listChildConcepts(aKB, aParentIdentifier, aAll, 1).isEmpty();
    }

    @Override
    public List<KBHandle> listChildConcepts(KnowledgeBase aKB, String aParentIdentifier,
            boolean aAll)
        throws QueryEvaluationException
    {
        return listChildConcepts(aKB, aParentIdentifier, aAll, 10000);
    }
    
    @Override
    public List<KBHandle> getParentConcept(KnowledgeBase aKB, String aIdentifier,
            boolean aAll)
        throws QueryEvaluationException
    {
        List<KBHandle> resultList = read(aKB, (conn) -> {
            String QUERY = SPARQLQueryStore.PARENT_CONCEPT;
            ValueFactory vf = SimpleValueFactory.getInstance();
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
            tupleQuery.setBinding("oChild", vf.createIRI(aIdentifier));
            tupleQuery.setBinding("pTYPE", aKB.getTypeIri());
            tupleQuery.setBinding("oCLASS", aKB.getClassIri());
            tupleQuery.setBinding("pSUBCLASS", aKB.getSubclassIri());
            tupleQuery.setBinding("pLABEL", aKB.getLabelIri());
            tupleQuery.setIncludeInferred(false);
            return evaluateListQuery(tupleQuery, aAll);
        });
        return resultList;
    }
    
    @Override
    public List<KBHandle> getConceptForInstance(KnowledgeBase aKB, String aIdentifier,
            boolean aAll)
        throws QueryEvaluationException
    {
        List<KBHandle> resultList = read(aKB, (conn) -> {
            String QUERY = SPARQLQueryStore.CONCEPT_FOR_INSTANCE;
            ValueFactory vf = SimpleValueFactory.getInstance();
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
            tupleQuery.setBinding("pTYPE", aKB.getTypeIri());
            tupleQuery.setBinding("pInstance", vf.createIRI(aIdentifier));
            tupleQuery.setBinding("pLABEL", aKB.getLabelIri());
            tupleQuery.setIncludeInferred(true);

            return evaluateListQuery(tupleQuery, aAll);
        });
        resultList.sort(Comparator.comparing(KBObject::getUiLabel));
        return resultList;
    }
    
    @Override
    public Set<KBHandle> getParentConceptList(KnowledgeBase aKB, String aIdentifier, boolean aAll)
        throws QueryEvaluationException
    {
        Set<KBHandle> parentConceptList = new HashSet<KBHandle>();
        if (aIdentifier != null) {
            Optional<KBObject> identifierKBObj = readKBIdentifier(aKB.getProject(), aIdentifier);
            if (!identifierKBObj.isPresent()) {
                return parentConceptList;
            }
            else if (identifierKBObj.get() instanceof KBConcept) {
                getParentConceptListforConcept(parentConceptList, aKB, aIdentifier,
                        aAll);
            }
            else if (identifierKBObj.get() instanceof KBInstance) {
                List<KBHandle> conceptList = getConceptForInstance(aKB, aIdentifier, aAll);
                parentConceptList.addAll(conceptList);
                for (KBHandle parent : conceptList) {
                    getParentConceptListforConcept(parentConceptList, aKB, parent.getIdentifier(),
                            aAll);
                }
            }
        }
        return parentConceptList;
    }
    
    // recursive method to get concept tree
    public Set<KBHandle> getParentConceptListforConcept(Set<KBHandle> parentConceptList,
            KnowledgeBase aKB, String aIdentifier, boolean aAll)
        throws QueryEvaluationException
    {
        List<KBHandle> parentList = getParentConcept(aKB, aIdentifier, aAll);
        parentConceptList.addAll(parentList);
        for (KBHandle parent : parentList) {
            getParentConceptListforConcept(parentConceptList, aKB, parent.getIdentifier(), aAll);
        }
        return parentConceptList;
    }

    // Need to work on the query for variable inputs like owl:intersectionOf, rdf:rest*/rdf:first
    @Override
    public List<KBHandle> listChildConcepts(KnowledgeBase aKB, String aParentIdentifier,
            boolean aAll, int aLimit)
        throws QueryEvaluationException
    {
        // The query below only returns subclasses which simultaneously declare being a class
        // via the class property defined in the KB specification. This means that if the KB
        // is configured to use rdfs:Class but a subclass defines itself using owl:Class, then
        // this subclass is *not* returned. We do presently *not* support mixed schemes in a
        // single KB.
        List<KBHandle> resultList = read(aKB, (conn) -> {
            String QUERY = String.join("\n"
                , SPARQLQueryStore.SPARQL_PREFIX
                , "SELECT DISTINCT ?s ?l WHERE { "
                , "     {?s ?pSUBCLASS ?oPARENT . }" 
                , "     UNION { ?s ?pTYPE ?oCLASS ."
                , "         ?s owl:intersectionOf ?list . "
                , "         FILTER EXISTS { ?list rdf:rest*/rdf:first ?oPARENT} }"
                , "     OPTIONAL { "
                , "         ?s ?pLABEL ?l . "
                , "         FILTER(LANG(?l) = \"\" || LANGMATCHES(LANG(?l), \"en\")) "
                , "     } "
                , "} "
                , "LIMIT " + aLimit);
            ValueFactory vf = SimpleValueFactory.getInstance();
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
            tupleQuery.setBinding("oPARENT", vf.createIRI(aParentIdentifier));
            tupleQuery.setBinding("pTYPE", aKB.getTypeIri());
            tupleQuery.setBinding("oCLASS", aKB.getClassIri());
            tupleQuery.setBinding("pSUBCLASS", aKB.getSubclassIri());
            tupleQuery.setBinding("pLABEL", aKB.getLabelIri());
            tupleQuery.setIncludeInferred(false);

            return evaluateListQuery(tupleQuery, aAll);
        });

        
        if (resultList.size() > 1) {
            resultList.sort(Comparator.comparing(KBObject::getUiLabel));
        }
        
        return resultList;
    }
    
    @Override
    public List<KBHandle> listInstancesForChildConcepts(KnowledgeBase aKB, String aParentIdentifier,
            boolean aAll, int aLimit)
        throws QueryEvaluationException
    {
        List<KBHandle> childConceptInstances = new ArrayList<KBHandle>();
        List<KBHandle> childConcepts =  listChildConcepts(aKB, aParentIdentifier, aAll, aLimit);
        for (KBHandle childConcept : childConcepts) {
            childConceptInstances.addAll(listInstances(aKB, childConcept.getIdentifier(), aAll));
        }
        return childConceptInstances;
    }
    
    
    private List<KBHandle> evaluateListQuery(TupleQuery tupleQuery, boolean aAll)
        throws QueryEvaluationException
    {
        TupleQueryResult result = tupleQuery.evaluate();        
        
        List<KBHandle> handles = new ArrayList<>();
        while (result.hasNext()) {
            BindingSet bindings = result.next();
            String id = bindings.getBinding("s").getValue().stringValue();
            Binding label = bindings.getBinding("l");

            if (!id.contains(":") || (!aAll && hasImplicitNamespace(id))) {
                continue;
            }

            KBHandle handle = new KBHandle(id);
            if (label != null) {
                handle.setName(label.getValue().stringValue());
            }
            else {
                handle.setName(handle.getUiLabel());
            }
            handles.add(handle);
        }
        return handles;
    }

    private ReificationStrategy getReificationStrategy(KnowledgeBase kb)
    {
        switch (kb.getReification()) {
        case WIKIDATA:
            return new WikiDataReification(this);
        case NONE: // Fallthrough
        default:
            return new NoReification(this);
        }
    }
    
    /**
     * Create base property with a specific IRI as identifier for the base property 
     * (which includes subClassOf, label and description)   
     * @param akb
     *            The knowledge base to initialize base properties
     * @param aProperty
     *            Property to be created for KB
     */
    public KBHandle createBaseProperty(KnowledgeBase akb, KBProperty aProperty)
    {
        return update(akb, (conn) -> {
            aProperty.write(conn, akb);
            return new KBHandle(aProperty.getIdentifier(), aProperty.getName());
        });
    }

    private boolean hasImplicitNamespace(String s)
    {
        for (String ns : implicitNamespaces) {
            if (s.startsWith(ns)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addQualifier(KnowledgeBase kb, KBQualifier newQualifier)
    {
        getReificationStrategy(kb).addQualifier(kb, newQualifier);
    }

    @Override
    public void deleteQualifier(KnowledgeBase kb, KBQualifier oldQualifier)
    {
        getReificationStrategy(kb).deleteQualifier(kb, oldQualifier);
    }

    @Override
    public void upsertQualifier(KnowledgeBase kb, KBQualifier aQualifier)
    {
        getReificationStrategy(kb).upsertQualifier(kb, aQualifier);
    }

    @Override
    public List<KBQualifier> listQualifiers(KnowledgeBase kb, KBStatement aStatement)
    {
        return getReificationStrategy(kb).listQualifiers(kb, aStatement);
    }

    @Override
    public boolean statementsMatchSPO(KnowledgeBase akb, KBStatement mockStatement)
    {
        return getReificationStrategy(akb).statementsMatchSPO(akb, mockStatement);
    }

    @Override
    public Map<String, KnowledgeBaseProfile> readKnowledgeBaseProfiles()
        throws IOException
    {
        try (Reader r = new InputStreamReader(
                getClass().getResourceAsStream(KNOWLEDGEBASE_PROFILES_YAML),
                StandardCharsets.UTF_8)) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            return mapper.readValue(r, 
                    new TypeReference<HashMap<String, KnowledgeBaseProfile>>(){});
        }
    }
    
    /**
     * Read identifier IRI and return {@link Optional} of {@link KBObject}
     * 
     * @return {@link Optional} of {@link KBObject} of type {@link KBConcept} or {@link KBInstance}
     */
    @Override
    public Optional<KBObject> readKBIdentifier(Project aProject, String aIdentifier)
    {
        for (KnowledgeBase kb : getKnowledgeBases(aProject)) {
            try (RepositoryConnection conn = getConnection(kb)) {
                ValueFactory vf = conn.getValueFactory();
                RepositoryResult<Statement> stmts = RdfUtils.getStatements(conn,
                        vf.createIRI(aIdentifier), kb.getTypeIri(), kb.getClassIri(), true);
                if (stmts.hasNext()) {
                    KBConcept kbConcept = KBConcept.read(conn, vf.createIRI(aIdentifier), kb);
                    if (kbConcept != null) {
                        return Optional.of(kbConcept);
                    }
                }
                else if (!stmts.hasNext()) {
                    Optional<KBInstance> kbInstance = readInstance(kb, aIdentifier);
                    if (kbInstance.isPresent()) {
                        return kbInstance.flatMap((p) -> Optional.of(p));
                    }
                }
            }
            catch (QueryEvaluationException e) {
                log.error("Reading KB Entries failed.", e);
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}

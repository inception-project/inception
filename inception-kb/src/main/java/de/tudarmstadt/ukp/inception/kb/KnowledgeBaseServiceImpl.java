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

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
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
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.repository.sparql.config.SPARQLRepositoryConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.lucene.config.LuceneSailConfig;
import org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.dao.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;
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
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseMapping;
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
    private final File kbRepositoriesRoot;

    @Autowired
    public KnowledgeBaseServiceImpl(RepositoryProperties aRepoProperties)
    {
        kbRepositoriesRoot = new File(aRepoProperties.getPath(), "kb");
        
        // Originally, the KBs were stored next to the repository folder - but they should be
        // *under* the repository folder
        File legacyLocation = new File(System.getProperty(SettingsUtil.getPropApplicationHome(),
                System.getProperty("user.home") + "/"
                        + SettingsUtil.getApplicationUserHomeSubdir()),
                "kb");

        if (legacyLocation.exists() && legacyLocation.isDirectory()) {
            try {
                log.info("Found legacy KB folder at [" + legacyLocation
                        + "]. Trying to move it to the new location at [" + kbRepositoriesRoot
                        + "]");
                Files.move(legacyLocation.toPath(), kbRepositoriesRoot.toPath(), REPLACE_EXISTING);
                log.info("Move successful.");
            }
            catch (IOException e) {
                throw new RuntimeException("Detected legacy KB folder at [" + legacyLocation
                        + "] but cannot move it to the new location at [" + kbRepositoriesRoot
                        + "]. Please perform the move manually and ensure that the application "
                        + "has all necessary permissions to the new location - then try starting "
                        + "the application again.");
            }
        }
        
        repoManager = RepositoryProvider.getRepositoryManager(kbRepositoriesRoot);
        log.info("Knowledge base repository path: {}", kbRepositoriesRoot);
        implicitNamespaces = IriConstants.IMPLICIT_NAMESPACES;
    }
    
    public KnowledgeBaseServiceImpl(
            RepositoryProperties aRepoProperties,
            EntityManager entityManager)
    {
        this(aRepoProperties);
        this.entityManager = entityManager;
    }

    @EventListener({ContextRefreshedEvent.class})
    void onContextRefreshed()
    {
        Set<String> orphanedIDs = new HashSet<>();
        try {
            orphanedIDs.addAll(repoManager.getRepositoryIDs());
        }
        catch (Exception e) {
            log.error("Unable to enumerate KB repositories. This may not be a critical issue, "
                    + "but it means that I cannot check if there are orphaned repositories. I "
                    + "will continue loading the application. Please try to fix the problem.", e);
        }
        
        // We loop over all the local repositories and ensure that they have the latest
        // configuration. One effect of this is that the directory where the full text indexes
        // is stored is updated with respect to the location of the application data repository
        // in case the application data was moved to another location by the user (i.e. the
        // index dir is normally stored as an absolute path in the KB repo config and here we fix
        // this).
        for (KnowledgeBase kb : listKnowledgeBases()) {
            orphanedIDs.remove(kb.getRepositoryId());
            
            if (RepositoryType.LOCAL.equals(kb.getType())) {
                reconfigureLocalKnowledgeBase(kb);
            }
        }
        
        if (!orphanedIDs.isEmpty()) {
            log.info("Found orphaned KB repositories: {}",
                    orphanedIDs.stream().sorted().collect(Collectors.toList()));
        }
        
        repoManager.refresh();
    }
    
    @Override
    public void destroy() throws Exception
    {
        repoManager.shutDown();
    }

    /**
     * Sanity check to test if a knowledge base is already registered with RDF4J.
     *
     * @param aKB a knowledge base
     */
    private void assertRegistration(KnowledgeBase aKB)
    {
        if (!aKB.isManagedRepository()) {
            throw new IllegalStateException(aKB.toString() + " has to be registered first.");
        }
    }

    @Transactional
    @Override
    public void registerKnowledgeBase(KnowledgeBase aKB, RepositoryImplConfig aCfg)
        throws RepositoryException, RepositoryConfigException
    {
        // Obtain unique repository id
        String baseName = "pid-" + Long.toString(aKB.getProject().getId()) + "-kbid-";
        String repositoryId = repoManager.getNewRepositoryID(baseName);
        aKB.setRepositoryId(repositoryId);
        
        // We want to have a separate Lucene index for every local repo, so we need to hack the
        // index dir in here because this is the place where we finally know the repo ID.
        setIndexDir(aKB, aCfg);

        repoManager.addRepositoryConfig(new RepositoryConfig(repositoryId, aCfg));
        entityManager.persist(aKB);
    }
    
    @Override
    public void defineBaseProperties(KnowledgeBase akb) 
    {
        // KB will initialize base properties with base IRI schema properties defined by user
        if (akb.getType() == RepositoryType.LOCAL) {
            createBaseProperty(akb, new KBProperty(akb.getSubclassIri().getLocalName(),
                    akb.getSubclassIri().stringValue()));
            createBaseProperty(akb, new KBProperty(akb.getLabelIri().getLocalName(),
                    akb.getLabelIri().stringValue(),null,XMLSchema.STRING.stringValue()));
            createBaseProperty(akb, new KBProperty(akb.getDescriptionIri().getLocalName(),
                    akb.getDescriptionIri().stringValue(),null,XMLSchema.STRING.stringValue()));
            createBaseProperty(akb, new KBProperty(akb.getTypeIri().getLocalName(),
                    akb.getTypeIri().stringValue()));
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

    @Transactional
    public List<KnowledgeBase> listKnowledgeBases()
    {
        String query = 
                "FROM KnowledgeBase " +
                "ORDER BY name ASC";
        return entityManager
                .createQuery(query, KnowledgeBase.class)
                .getResultList();
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
    public void removeKnowledgeBase(KnowledgeBase aKB)
        throws RepositoryException, RepositoryConfigException
    {
        assertRegistration(aKB);
        
        repoManager.removeRepository(aKB.getRepositoryId());

        entityManager.remove(entityManager.contains(aKB) ? aKB : entityManager.merge(aKB));
    }

    @Override
    public RepositoryImplConfig getNativeConfig()
    {
        // See #221 - Disabled because it is too slow during import
        // return new SailRepositoryConfig(
        //   new ForwardChainingRDFSInferencerConfig(new NativeStoreConfig()));

        LuceneSailConfig config = new LuceneSailConfig(new NativeStoreConfig());
        // NOTE: We do not set the index dir here but when the KB is registered because we want each
        // repo to have its own index folder and we don't know the repo ID until it is registered
        return new SailRepositoryConfig(config);
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
    public List<KBHandle> listAllConcepts(KnowledgeBase aKB, boolean aAll)
        throws QueryEvaluationException
    {
        List<KBHandle> resultList;
        
        resultList = read(aKB, (conn) -> {
            String QUERY = SPARQLQueryStore.queryForAllConceptList(aKB);
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
            tupleQuery.setBinding("pTYPE", aKB.getTypeIri());
            tupleQuery.setBinding("oCLASS", aKB.getClassIri());
            tupleQuery.setBinding("pSUBCLASS", aKB.getSubclassIri());
            tupleQuery.setBinding("pLABEL", aKB.getLabelIri());
            tupleQuery.setBinding("pDESCRIPTION", aKB.getDescriptionIri());
            tupleQuery.setIncludeInferred(false);
            return evaluateListQuery(tupleQuery, aAll);
        });
        resultList.sort(Comparator.comparing(KBObject::getUiLabel));
        return resultList;
    }
    
    @Override
    public List<KBHandle> listConcepts(KnowledgeBase kb, boolean aAll)
    {
        return listAllConcepts(kb, aAll);
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
    public List<KBHandle> listProperties(KnowledgeBase aKB, boolean aIncludeInferred, boolean aAll)
        throws QueryEvaluationException
    {
        List<KBHandle> resultList = read(aKB, (conn) -> {
            String QUERY = SPARQLQueryStore.queryForPropertyList(aKB);
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
            tupleQuery.setBinding("pTYPE", aKB.getTypeIri());
            tupleQuery.setBinding("oPROPERTY", aKB.getPropertyTypeIri());
            tupleQuery.setBinding("pLABEL", aKB.getLabelIri());
            tupleQuery.setBinding("pDESCRIPTION", aKB.getDescriptionIri());
            tupleQuery.setIncludeInferred(aIncludeInferred);
            return evaluateListQuery(tupleQuery, aAll);
        });
        resultList.sort(Comparator.comparing(KBObject::getUiLabel));
        return resultList;
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
            RepositoryResult<Statement> conceptStmts = RdfUtils
                .getStatementsSparql(conn, vf.createIRI(aIdentifier), kb.getTypeIri(), null,
                    kb.getMaxResults(), false, null);

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
                    true, kb.getMaxResults())) {
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
        return list(kb, conceptIri, false, aAll, kb.getMaxResults());
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
    public List<KBHandle> list(KnowledgeBase aKB, IRI aType, boolean aIncludeInferred, boolean aAll,
            int aLimit)
        throws QueryEvaluationException
    {
        List<KBHandle> resultList = read(aKB, (conn) -> {
            String QUERY = SPARQLQueryStore.listInstances(aKB);
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
            tupleQuery.setBinding("pTYPE", aKB.getTypeIri());
            tupleQuery.setBinding("oPROPERTY", aType);
            tupleQuery.setBinding("pLABEL", aKB.getLabelIri());
            tupleQuery.setBinding("pDESCRIPTION", aKB.getDescriptionIri());
            tupleQuery.setIncludeInferred(aIncludeInferred);

            return evaluateListQuery(tupleQuery, aAll);
        });

        resultList.sort(Comparator.comparing(KBObject::getUiLabel));

        return resultList;
    }
    
    @Override
    public List<KBHandle> listDomainProperties(KnowledgeBase aKB, String aDomain,
            boolean aIncludeInferred, boolean aAll)
        throws QueryEvaluationException
    {
        List<KBHandle> resultList = read(aKB, (conn) -> {
            ValueFactory vf = conn.getValueFactory();
            String QUERY = SPARQLQueryStore.queryForPropertyListWithDomain(aKB);
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
            tupleQuery.setBinding("aDomain", vf.createIRI(aDomain));
            tupleQuery.setBinding("pLABEL", aKB.getLabelIri());
            tupleQuery.setBinding("pDESCRIPTION", aKB.getDescriptionIri());
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
    public List<KBHandle> listPropertiesRangeValue(KnowledgeBase aKB, String aProperty,
            boolean aIncludeInferred, boolean aAll)
        throws QueryEvaluationException
    {
        List<KBHandle> resultList = read(aKB, (conn) -> {
            ValueFactory vf = conn.getValueFactory();
            String QUERY = SPARQLQueryStore.queryForPropertySpecificRange(aKB);
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
            tupleQuery.setBinding("aProperty", vf.createIRI(aProperty));
            tupleQuery.setBinding("pLABEL", aKB.getLabelIri());
            tupleQuery.setIncludeInferred(aIncludeInferred);

            return evaluateListQuery(tupleQuery, aAll);
        });
        
        resultList.sort(Comparator.comparing(KBObject::getUiLabel));
        return resultList;
    }
    
    @Override
    public List<KBHandle> listProperties(KnowledgeBase aKB, IRI aType, boolean aIncludeInferred,
            boolean aAll)
        throws QueryEvaluationException
    {
        List<KBHandle> resultList = read(aKB, (conn) -> {
            String QUERY = SPARQLQueryStore.queryForPropertyList(aKB);
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
            tupleQuery.setBinding("pTYPE", aKB.getTypeIri());
            tupleQuery.setBinding("oPROPERTY", aType);
            tupleQuery.setBinding("pLABEL", aKB.getLabelIri());
            tupleQuery.setBinding("pDESCRIPTION", aKB.getDescriptionIri());
            tupleQuery.setIncludeInferred(aIncludeInferred);

            return evaluateListQuery(tupleQuery, aAll);
        });

        resultList.sort(Comparator.comparing(KBObject::getUiLabel));

        return resultList;
    }
    
    @Override
    public List<KBHandle> listRootConcepts(KnowledgeBase aKB, boolean aAll)
        throws QueryEvaluationException
    {
        List<KBHandle> resultList = new ArrayList<>();

        if (!aKB.getExplicitlyDefinedRootConcepts().isEmpty()) {
            for (IRI conceptIRI : aKB.getExplicitlyDefinedRootConcepts()) {
                KBConcept concept = readConcept(aKB, conceptIRI.stringValue()).get();
                KBHandle conceptHandle = new KBHandle(concept.getIdentifier(), concept.getName(),
                        concept.getDescription());
                resultList.add(conceptHandle);
            }
        }
        else {
            resultList = read(aKB, (conn) -> {
                String QUERY = SPARQLQueryStore.listRootConcepts(aKB);
                TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
                tupleQuery.setBinding("pTYPE", aKB.getTypeIri());
                tupleQuery.setBinding("oCLASS", aKB.getClassIri());
                tupleQuery.setBinding("pSUBCLASS", aKB.getSubclassIri());
                tupleQuery.setBinding("pLABEL", aKB.getLabelIri());
                tupleQuery.setBinding("pDESCRIPTION", aKB.getDescriptionIri());
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
        return listChildConcepts(aKB, aParentIdentifier, aAll, aKB.getMaxResults());
    }
    
    @Override
    public List<KBHandle> getParentConcept(KnowledgeBase aKB, KBHandle aHandle,
            boolean aAll)
        throws QueryEvaluationException
    {
        List<KBHandle> resultList = read(aKB, (conn) -> {
            String QUERY = SPARQLQueryStore.queryForParentConcept(aKB);
            ValueFactory vf = SimpleValueFactory.getInstance();
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
            tupleQuery.setBinding("oChild", vf.createIRI(aHandle.getIdentifier()));
            tupleQuery.setBinding("pTYPE", aKB.getTypeIri());
            tupleQuery.setBinding("oCLASS", aKB.getClassIri());
            tupleQuery.setBinding("pSUBCLASS", aKB.getSubclassIri());
            tupleQuery.setBinding("pLABEL", aKB.getLabelIri());
            tupleQuery.setBinding("pDESCRIPTION", aKB.getDescriptionIri());
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
            String QUERY = SPARQLQueryStore.queryForConceptForInstance(aKB);
            ValueFactory vf = SimpleValueFactory.getInstance();
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
            tupleQuery.setBinding("pTYPE", aKB.getTypeIri());
            tupleQuery.setBinding("pInstance", vf.createIRI(aIdentifier));
            tupleQuery.setBinding("pLABEL", aKB.getLabelIri());
            tupleQuery.setBinding("pDESCRIPTION", aKB.getDescriptionIri());
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
        Set<KBHandle> parentConceptSet = new LinkedHashSet<KBHandle>();
        if (aIdentifier != null) {
            Optional<KBObject> identifierKBObj = readKBIdentifier(aKB.getProject(), aIdentifier);
            if (!identifierKBObj.isPresent()) {
                return parentConceptSet;
            }
            else if (identifierKBObj.get() instanceof KBConcept) {
                parentConceptSet.add(identifierKBObj.get().toKBHandle());
                getParentConceptListforConcept(parentConceptSet, aKB,
                        identifierKBObj.get().toKBHandle(), aAll);
            }
            else if (identifierKBObj.get() instanceof KBInstance) {
                List<KBHandle> conceptList = getConceptForInstance(aKB, aIdentifier, aAll);
                parentConceptSet.addAll(conceptList);
                for (KBHandle parent : conceptList) {
                    getParentConceptListforConcept(parentConceptSet, aKB, parent, aAll);
                }
            }
        }
        return parentConceptSet;
    }
    
    // recursive method to get concept tree
    public Set<KBHandle> getParentConceptListforConcept(Set<KBHandle> parentConceptSet,
            KnowledgeBase aKB, KBHandle aHandle, boolean aAll)
        throws QueryEvaluationException
    {
        List<KBHandle> parentList = getParentConcept(aKB, aHandle, aAll);
        for (KBHandle parent : parentList) {
            if (!parentConceptSet.contains(parent)) {
                parentConceptSet.add(parent);
                getParentConceptListforConcept(parentConceptSet, aKB, parent, aAll);
            }
        }
        return parentConceptSet;
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
            String QUERY = SPARQLQueryStore.listChildConcepts(aKB);
            ValueFactory vf = SimpleValueFactory.getInstance();
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
            tupleQuery.setBinding("oPARENT", vf.createIRI(aParentIdentifier));
            tupleQuery.setBinding("pTYPE", aKB.getTypeIri());
            tupleQuery.setBinding("oCLASS", aKB.getClassIri());
            tupleQuery.setBinding("pSUBCLASS", aKB.getSubclassIri());
            tupleQuery.setBinding("pLABEL", aKB.getLabelIri());
            tupleQuery.setBinding("pDESCRIPTION", aKB.getDescriptionIri());
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
            if (bindings.size() == 0) {
                continue;
            }
            String id = bindings.getBinding("s").getValue().stringValue();
            Binding label = bindings.getBinding("l");
            Binding description = bindings.getBinding("d");

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
            
            if (description != null) {
                handle.setDescription(description.getValue().stringValue());
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

    @Override
    public boolean hasImplicitNamespace(String s)
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
    @Override public Optional<KBObject> readKBIdentifier(Project aProject, String aIdentifier)
    {
        for (KnowledgeBase kb : getKnowledgeBases(aProject)) {
            Optional<KBObject> handle = readKBIdentifier(kb, aIdentifier);
            if (handle.isPresent()) {
                return handle;
            }
        }
        return Optional.empty();
    }
    
    @Override
    public Optional<KBObject> readKBIdentifier(KnowledgeBase aKb, String aIdentifier)
    {
        try (RepositoryConnection conn = getConnection(aKb)) {
            ValueFactory vf = conn.getValueFactory();
            RepositoryResult<Statement> stmts = RdfUtils.getStatements(conn,
                    vf.createIRI(aIdentifier), aKb.getTypeIri(), aKb.getClassIri(), true,
                aKb.getMaxResults());
            if (stmts.hasNext()) {
                KBConcept kbConcept = KBConcept.read(conn, vf.createIRI(aIdentifier), aKb);
                if (kbConcept != null) {
                    return Optional.of(kbConcept);
                }
            }
            else if (!stmts.hasNext()) {
                Optional<KBInstance> kbInstance = readInstance(aKb, aIdentifier);
                if (kbInstance.isPresent()) {
                    return kbInstance.flatMap((p) -> Optional.of(p));
                }
            }
        }
        catch (QueryEvaluationException e) {
            log.error("Reading KB Entries failed.", e);
            return Optional.empty();
        }
        return Optional.empty();
    }

    @Override
    public SchemaProfile checkSchemaProfile(KnowledgeBaseProfile aProfile)
    {
        SchemaProfile[] profiles = SchemaProfile.values();
        KnowledgeBaseMapping mapping = aProfile.getMapping();
        for (int i = 0; i < profiles.length; i++) {
            // Check if kb profile corresponds to a known schema profile
            if (equalsSchemaProfile(profiles[i], mapping.getClassIri(), mapping.getSubclassIri(),
                mapping.getTypeIri(), mapping.getDescriptionIri(), mapping.getLabelIri(),
                mapping.getPropertyTypeIri())) {
                return profiles[i];
            }
        }
        // If the iris don't represent a known schema profile , return CUSTOM
        return SchemaProfile.CUSTOMSCHEMA;
    }

    @Override
    public SchemaProfile checkSchemaProfile(KnowledgeBase aKb)
    {
        SchemaProfile[] profiles = SchemaProfile.values();
        for (int i = 0; i < profiles.length; i++) {
            // Check if kb has a known schema profile
            if (equalsSchemaProfile(profiles[i], aKb.getClassIri(), aKb.getSubclassIri(),
                aKb.getTypeIri(), aKb.getDescriptionIri(), aKb.getLabelIri(),
                aKb.getPropertyTypeIri())) {
                return profiles[i];
            }
        }
        // If the iris don't represent a known schema profile , return CUSTOM
        return SchemaProfile.CUSTOMSCHEMA;
    }

    /**
     * Compares a schema profile to given IRIs. Returns true if the IRIs are the same as in the
     * profile
     */
    private boolean equalsSchemaProfile(SchemaProfile profile, IRI classIri, IRI subclassIri,
        IRI typeIri, IRI descriptionIri, IRI labelIri, IRI propertyTypeIri)
    {
        return profile.getClassIri().equals(classIri) && profile.getSubclassIri()
            .equals(subclassIri) && profile.getTypeIri().equals(typeIri) && profile
            .getDescriptionIri().equals(descriptionIri) && profile.getLabelIri().equals(labelIri)
            && profile.getPropertyTypeIri().equals(propertyTypeIri);
    }

    @Override
    public File readKbFileFromClassPathResource(String aLocation) throws IOException
    {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        File kbFile = resolver.getResource(aLocation).getFile();
        return kbFile;
    }

    private void reconfigureLocalKnowledgeBase(KnowledgeBase aKB)
    {
        /*
        log.info("Forcing update of configuration for {}", aKB);
        Model model = new TreeModel();
        ValueFactory vf = SimpleValueFactory.getInstance();
        IRI root = vf
                .createIRI("http://inception-project.github.io/kbexport#" + aKB.getRepositoryId());
        repoManager.getRepositoryConfig(aKB.getRepositoryId()).export(model, root);
        StringWriter out = new StringWriter();
        Rio.write(model, out, RDFFormat.TURTLE);
        log.info("Current configuration: {}", out.toString());
        */
        
        RepositoryImplConfig config = getNativeConfig();
        setIndexDir(aKB, config);
        repoManager.addRepositoryConfig(new RepositoryConfig(aKB.getRepositoryId(), config));
    }
    
    private void setIndexDir(KnowledgeBase aKB, RepositoryImplConfig aCfg)
    {
        assertRegistration(aKB);
        
        // We want to have a separate Lucene index for every local repo, so we need to hack the
        // index dir in here because this is the place where we finally know the repo ID.
        if (aCfg instanceof SailRepositoryConfig) {
            SailRepositoryConfig cfg = (SailRepositoryConfig) aCfg;
            if (cfg.getSailImplConfig() instanceof LuceneSailConfig) {
                LuceneSailConfig luceneSailCfg = (LuceneSailConfig) cfg.getSailImplConfig();
                luceneSailCfg
                        .setIndexDir(new File(kbRepositoriesRoot, "indexes" + aKB.getRepositoryId())
                                .getAbsolutePath());
            }
        }
    }
    
    @Override
    public void rebuildFullTextIndex(KnowledgeBase aKB) throws Exception
    {
        if (!RepositoryType.LOCAL.equals(aKB.getType())) {
            throw new IllegalArgumentException("Reindexing is only supported on local KBs");
        }
        
        boolean reindexSupported = false;
        
        // Handle re-indexing of local repos that use a Lucene FTS
        if (repoManager.getRepository(aKB.getRepositoryId()) instanceof SailRepository) {
            SailRepository sailRepo = (SailRepository) repoManager
                .getRepository(aKB.getRepositoryId());
            if (sailRepo.getSail() instanceof LuceneSail) {
                reindexSupported = true;
                LuceneSail luceneSail = (LuceneSail) (sailRepo.getSail());
                try (RepositoryConnection conn = getConnection(aKB)) {
                    luceneSail.reindex();
                    conn.commit();
                }
            }
        }
        
        if (!reindexSupported) {
            throw new IllegalArgumentException(
                    aKB + "] does not support rebuilding its full text index.");
        }
    }
}

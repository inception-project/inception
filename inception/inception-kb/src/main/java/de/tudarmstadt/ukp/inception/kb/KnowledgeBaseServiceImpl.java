/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.kb;

import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.withProjectLogger;
import static de.tudarmstadt.ukp.inception.kb.http.PerThreadSslCheckingHttpClientUtils.restoreSslVerification;
import static de.tudarmstadt.ukp.inception.kb.http.PerThreadSslCheckingHttpClientUtils.skipCertificateChecks;
import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilder.DEFAULT_LIMIT;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.lucene.index.IndexFormatTooNewException;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.base.RepositoryConnectionWrapper;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryProvider;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.eclipse.rdf4j.repository.sparql.config.SPARQLRepositoryConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.lucene.impl.config.LuceneSailConfig;
import org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreConfig;
import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.builder.PropertyPathBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpHeaders;
import org.springframework.transaction.annotation.Transactional;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.event.BeforeProjectRemovedEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.StopWatch;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.BaseLoggers;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseProperties;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.kb.event.KnowledgeBaseConfigurationChangedEvent;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBQualifier;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.http.PerThreadSslCheckingHttpClientUtils;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.model.RemoteRepositoryTraits;
import de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQuery;
import de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilder;
import de.tudarmstadt.ukp.inception.kb.reification.NoReification;
import de.tudarmstadt.ukp.inception.kb.reification.ReificationStrategy;
import de.tudarmstadt.ukp.inception.kb.reification.WikiDataReification;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseProfile;
import de.tudarmstadt.ukp.inception.security.client.auth.basic.BasicAuthenticationTraits;
import de.tudarmstadt.ukp.inception.security.client.auth.oauth.MemoryOAuthSessionRepository;
import de.tudarmstadt.ukp.inception.security.client.auth.oauth.OAuthAuthenticationClientImpl;
import de.tudarmstadt.ukp.inception.security.client.auth.oauth.OAuthClientCredentialsAuthenticationTraits;
import de.tudarmstadt.ukp.inception.security.client.auth.oauth.OAuthSessionImpl;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link KnowledgeBaseServiceAutoConfiguration#knowledgeBaseService}.
 * </p>
 */
public class KnowledgeBaseServiceImpl
    implements KnowledgeBaseService, DisposableBean
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private @PersistenceContext EntityManager entityManager;
    private final RepositoryManager repoManager;
    private final File kbRepositoriesRoot;
    private final KnowledgeBaseProperties properties;

    private final LoadingCache<QueryKey, List<KBHandle>> queryCache;
    private final MemoryOAuthSessionRepository<KnowledgeBase> oAuthSessionRepository;

    @Autowired
    public KnowledgeBaseServiceImpl(RepositoryProperties aRepoProperties,
            KnowledgeBaseProperties aKBProperties)
    {
        properties = aKBProperties;

        queryCache = createQueryCache(aKBProperties);
        oAuthSessionRepository = new MemoryOAuthSessionRepository<>();

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
                Files.createDirectories(kbRepositoriesRoot.getParentFile().toPath());
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
        repoManager.setHttpClient(PerThreadSslCheckingHttpClientUtils
                .newPerThreadSslCheckingHttpClientBuilder().build());

        BaseLoggers.BOOT_LOG.info("Knowledge base repository path: {}", kbRepositoriesRoot);
    }

    private LoadingCache<QueryKey, List<KBHandle>> createQueryCache(
            KnowledgeBaseProperties aKBProperties)
    {
        Caffeine<QueryKey, List<KBHandle>> queryCacheBuilder = Caffeine.newBuilder()
                .maximumWeight(aKBProperties.getCacheSize())
                .expireAfterAccess(aKBProperties.getCacheExpireDelay())
                .refreshAfterWrite(aKBProperties.getCacheRefreshDelay())
                .weigher((QueryKey key, List<KBHandle> value) -> value.size());

        if (log.isTraceEnabled()) {
            queryCacheBuilder.recordStats();
        }

        return queryCacheBuilder.build(this::runQuery);
    }

    public KnowledgeBaseServiceImpl(RepositoryProperties aRepoProperties,
            KnowledgeBaseProperties aKBProperties, EntityManager entityManager)
    {
        this(aRepoProperties, aKBProperties);
        this.entityManager = entityManager;
    }

    @EventListener({ ContextRefreshedEvent.class })
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
            log.info("Found [{}] orphaned KB repositories: {}", orphanedIDs.size(),
                    orphanedIDs.stream().sorted().collect(toList()));

            if (properties.isRemoveOrphansOnStart()) {
                for (String id : orphanedIDs) {
                    repoManager.removeRepository(id);
                    log.info("Deleted orphaned KB repository: {}", id);
                }
            }
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
     * @param aKB
     *            a knowledge base
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
            ValueFactory vf = SimpleValueFactory.getInstance();

            createBaseProperty(akb, new KBProperty(akb.getSubclassIri(),
                    vf.createIRI(akb.getSubclassIri()).getLocalName()));
            createBaseProperty(akb,
                    new KBProperty(akb.getLabelIri(),
                            vf.createIRI(akb.getLabelIri()).getLocalName(), null,
                            XSD.STRING.stringValue()));
            createBaseProperty(akb,
                    new KBProperty(akb.getDescriptionIri(),
                            vf.createIRI(akb.getDescriptionIri()).getLocalName(), null,
                            XSD.STRING.stringValue()));
            createBaseProperty(akb, new KBProperty(akb.getTypeIri(),
                    vf.createIRI(akb.getTypeIri()).getLocalName()));
            createBaseProperty(akb, new KBProperty(akb.getSubPropertyIri(),
                    vf.createIRI(akb.getSubPropertyIri()).getLocalName()));
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
    public Optional<KnowledgeBase> getKnowledgeBaseByName(Project aProject, String aName)
    {
        TypedQuery<KnowledgeBase> query = entityManager.createNamedQuery("KnowledgeBase.getByName",
                KnowledgeBase.class);
        query.setParameter("project", aProject);
        query.setParameter("name", aName);

        try {
            return Optional.of(query.getSingleResult());
        }
        catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Transactional
    @Override
    public void updateKnowledgeBase(KnowledgeBase kb)
        throws RepositoryException, RepositoryConfigException
    {
        assertRegistration(kb);
        // We clear any current OAuth session on the repository because we do not know if maybe
        // the user has changed the repository URL / credentials as part of the update...
        oAuthSessionRepository.clear(kb);
        entityManager.merge(kb);
    }

    @Transactional
    @Override
    public void updateKnowledgeBase(KnowledgeBase kb, RepositoryImplConfig cfg)
        throws RepositoryException, RepositoryConfigException
    {
        assertRegistration(kb);
        repoManager.addRepositoryConfig(new RepositoryConfig(kb.getRepositoryId(), cfg));
        updateKnowledgeBase(kb);
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
        String query = //
                "FROM KnowledgeBase " + //
                        "ORDER BY name ASC";
        return entityManager //
                .createQuery(query, KnowledgeBase.class) //
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

        oAuthSessionRepository.clear(aKB);

        repoManager.removeRepository(aKB.getRepositoryId());

        entityManager.remove(entityManager.contains(aKB) ? aKB : entityManager.merge(aKB));
    }

    @Override
    public RepositoryImplConfig getNativeConfig()
    {
        // See #221 - Disabled because it is too slow during import
        // return new SailRepositoryConfig(
        // new ForwardChainingRDFSInferencerConfig(new NativeStoreConfig()));

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
        var repositoryConfig = repoManager.getRepositoryConfig(kb.getRepositoryId());
        if (repositoryConfig == null) {
            return null;
        }
        return repositoryConfig.getRepositoryImplConfig();
    }

    @Override
    public RepositoryConnection getConnection(KnowledgeBase kb)
    {
        assertRegistration(kb);
        Repository repo = repoManager.getRepository(kb.getRepositoryId());

        if (repo instanceof SPARQLRepository) {
            SPARQLRepositoryConfig sparqlRepoConfig = (SPARQLRepositoryConfig) getKnowledgeBaseConfig(
                    kb);
            SPARQLRepository sparqlRepo = (SPARQLRepository) repo;
            applyBasicHttpAuthenticationConfigurationFromUrl(sparqlRepoConfig, sparqlRepo);
            RemoteRepositoryTraits traits = readTraits(kb);

            if (traits != null && traits.getAuthentication() != null) {
                switch (traits.getAuthentication().getType()) {
                case BASIC: {
                    applyBasicHttpAuthenticationConfiguration(sparqlRepo, traits);
                    break;
                }
                case OAUTH_CLIENT_CREDENTIALS: {
                    applyOAuthConfiguration(kb, sparqlRepo, traits);
                    break;
                }
                }
            }
        }

        return new RepositoryConnectionWrapper(repo, repo.getConnection())
        {
            {
                skipCertificateChecks(kb.isSkipSslValidation());
            }

            @Override
            public void close() throws RepositoryException
            {
                try {
                    super.close();
                }
                finally {
                    restoreSslVerification();
                }
            };
        };
    }

    private RemoteRepositoryTraits readTraits(KnowledgeBase kb)
    {
        try {
            return JSONUtil.fromJsonString(RemoteRepositoryTraits.class, kb.getTraits());
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void applyBasicHttpAuthenticationConfiguration(SPARQLRepository sparqlRepo,
            RemoteRepositoryTraits traits)
    {
        var auth = (BasicAuthenticationTraits) traits.getAuthentication();
        sparqlRepo.setUsernameAndPassword(auth.getUsername(), auth.getPassword());
    }

    private void applyOAuthConfiguration(KnowledgeBase kb, SPARQLRepository sparqlRepo,
            RemoteRepositoryTraits traits)
    {
        var auth = (OAuthClientCredentialsAuthenticationTraits) traits.getAuthentication();
        var client = OAuthAuthenticationClientImpl.builder() //
                .withClientId(auth.getClientId()) //
                .withClientSecret(auth.getClientSecret()) //
                .withTokenEndpointUrl(auth.getTokenEndpointUrl()) //
                .build();

        // Check if there is already a session we can use
        var session = oAuthSessionRepository.get(kb, _kb -> {
            log.debug("[{}] Creating new OAuth session as [{}]...", _kb, auth.getClientId());
            var _session = new OAuthSessionImpl(client.getToken());
            log.debug("[{}] OAuth session as [{}] will expire in [{}]", _kb, auth.getClientId(),
                    _session.getAccessTokenExpiresIn());
            return _session;
        });

        try {
            client.refreshSessionIfNecessary(session);
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }

        var headers = Map.of(HttpHeaders.AUTHORIZATION, "Bearer " + session.getAccessToken());
        sparqlRepo.setAdditionalHttpHeaders(headers);
    }

    private void applyBasicHttpAuthenticationConfigurationFromUrl(
            SPARQLRepositoryConfig sparqlRepoConfig, SPARQLRepository sparqlRepo)
    {
        URI uri = URI.create(sparqlRepoConfig.getQueryEndpointUrl());
        String userInfo = uri.getUserInfo();
        if (isNotBlank(userInfo)) {
            userInfo = userInfo.trim();
            String username;
            String password;
            if (userInfo.contains(":")) {
                username = substringBefore(userInfo, ":");
                password = substringAfter(userInfo, ":");
            }
            else {
                username = userInfo;
                password = "";
            }

            sparqlRepo.setUsernameAndPassword(username, password);
        }
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
            // If the RDF file contains relative URLs, then they probably start with a hash.
            // To avoid having two hashes here, we drop the hash from the base prefix configured
            // by the user.
            String prefix = StringUtils.removeEnd(kb.getBasePrefix(), "#");
            conn.add(is, prefix, format);
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
    public void createConcept(KnowledgeBase kb, KBConcept aConcept)
    {
        if (StringUtils.isNotEmpty(aConcept.getIdentifier())) {
            throw new IllegalArgumentException("Identifier must be empty on create");
        }

        update(kb, (conn) -> {
            String identifier = getReificationStrategy(kb).generateConceptIdentifier(conn, kb);
            aConcept.setIdentifier(identifier);
            aConcept.write(conn, kb);
        });
    }

    @Override
    public Optional<KBConcept> readConcept(KnowledgeBase aKB, String aIdentifier, boolean aAll)
        throws QueryEvaluationException
    {
        try (StopWatch watch = new StopWatch(log, "readConcept(%s)", aIdentifier)) {
            SPARQLQuery query = SPARQLQueryBuilder.forClasses(aKB) //
                    .withIdentifier(aIdentifier) //
                    .excludeInferred() //
                    .retrieveLabel() //
                    .retrieveDescription();

            Optional<KBHandle> result;
            if (aKB.isReadOnly()) {
                result = fetchHandleCaching(aKB, query, aAll);
            }
            else {
                result = read(aKB, conn -> query.asHandle(conn, aAll));
            }

            return result.map(handle -> KBHandle.convertTo(KBConcept.class, handle));
        }
    }

    @Override
    public Optional<KBConcept> readConcept(Project aProject, String aIdentifier)
    {
        for (KnowledgeBase kb : getEnabledKnowledgeBases(aProject)) {
            Optional<KBConcept> concept = readConcept(kb, aIdentifier, true);
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
        });
    }

    @Override
    public void deleteConcept(KnowledgeBase aKB, KBConcept aConcept)
    {
        update(aKB, conn -> getReificationStrategy(aKB).deleteConcept(conn, aKB, aConcept));
    }

    @Override
    public List<KBHandle> listAllConcepts(KnowledgeBase aKB, boolean aAll)
        throws QueryEvaluationException
    {
        try (StopWatch watch = new StopWatch(log, "listAllConcepts()")) {
            SPARQLQuery query = SPARQLQueryBuilder.forClasses(aKB) //
                    .retrieveLabel() //
                    .retrieveDescription() //
                    .excludeInferred();

            List<KBHandle> result;
            if (aKB.isReadOnly()) {
                result = listHandlesCaching(aKB, query, aAll);
            }
            else {
                result = read(aKB, conn -> query.asHandles(conn, aAll));
            }

            return result;
        }
    }

    @Override
    public void createProperty(KnowledgeBase kb, KBProperty aProperty)
    {
        if (StringUtils.isNotEmpty(aProperty.getIdentifier())) {
            throw new IllegalArgumentException("Identifier must be empty on create");
        }

        update(kb, (conn) -> {
            String identifier = getReificationStrategy(kb).generatePropertyIdentifier(conn, kb);
            aProperty.setIdentifier(identifier);
            aProperty.write(conn, kb);
        });
    }

    @Override
    public Optional<KBProperty> readProperty(KnowledgeBase aKB, String aIdentifier)
    {
        try (StopWatch watch = new StopWatch(log, "readProperty(%s)", aIdentifier)) {
            SPARQLQuery query = SPARQLQueryBuilder.forProperties(aKB) //
                    .withIdentifier(aIdentifier) //
                    .retrieveDescription() //
                    .retrieveLabel() //
                    .retrieveDomainAndRange() //
                    .excludeInferred();

            Optional<KBHandle> result;
            if (aKB.isReadOnly()) {
                result = fetchHandleCaching(aKB, query, true);
            }
            else {
                result = read(aKB, conn -> query.asHandle(conn, true));
            }

            return result.map(handle -> KBHandle.convertTo(KBProperty.class, handle));
        }
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
        });
    }

    @Override
    public void deleteProperty(KnowledgeBase aKB, KBProperty aType)
    {
        update(aKB, conn -> getReificationStrategy(aKB).deleteProperty(conn, aKB, aType));
    }

    @Override
    public List<KBProperty> listProperties(KnowledgeBase kb, boolean aAll)
    {
        return listProperties(kb, true, aAll).stream()
                .map(h -> KBHandle.convertTo(KBProperty.class, h)).collect(Collectors.toList());
    }

    @Override
    public List<KBHandle> listProperties(KnowledgeBase aKB, boolean aIncludeInferred, boolean aAll)
        throws QueryEvaluationException
    {
        try (StopWatch watch = new StopWatch(log, "listProperties()")) {
            SPARQLQuery query = SPARQLQueryBuilder.forProperties(aKB) //
                    .retrieveLabel() //
                    .retrieveDescription() //
                    .retrieveDomainAndRange() //
                    .includeInferred(aIncludeInferred);

            List<KBHandle> result;
            if (aKB.isReadOnly()) {
                result = listHandlesCaching(aKB, query, aAll);
            }
            else {
                result = read(aKB, conn -> query.asHandles(conn, aAll));
            }

            return result;
        }
    }

    @Override
    public void createInstance(KnowledgeBase kb, KBInstance aInstance)
    {
        if (StringUtils.isNotEmpty(aInstance.getIdentifier())) {
            throw new IllegalArgumentException("Identifier must be empty on create");
        }

        update(kb, (conn) -> {
            String identifier = getReificationStrategy(kb).generateInstanceIdentifier(conn, kb);
            aInstance.setIdentifier(identifier);
            aInstance.write(conn, kb);
        });
    }

    @Override
    public Optional<KBInstance> readInstance(KnowledgeBase aKB, String aIdentifier)
        throws QueryEvaluationException
    {
        try (StopWatch watch = new StopWatch(log, "readInstance(%s)", aIdentifier)) {
            SPARQLQuery query = SPARQLQueryBuilder.forInstances(aKB) //
                    .withIdentifier(aIdentifier) //
                    .retrieveDescription() //
                    .retrieveLabel() //
                    .excludeInferred();

            Optional<KBHandle> result;
            if (aKB.isReadOnly()) {
                result = fetchHandleCaching(aKB, query, true);
            }
            else {
                result = read(aKB, conn -> query.asHandle(conn, true));
            }

            return result.map(handle -> KBHandle.convertTo(KBInstance.class, handle));
        }
    }

    @Override
    public Optional<KBInstance> readInstance(Project aProject, String aIdentifier)
    {
        for (KnowledgeBase kb : getEnabledKnowledgeBases(aProject)) {
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
            aInstance.write(conn, kb);
        });
    }

    @Override
    public void deleteInstance(KnowledgeBase aKB, KBInstance aInstance)
    {
        update(aKB, conn -> getReificationStrategy(aKB).deleteInstance(conn, aKB, aInstance));
    }

    @Override
    public List<KBHandle> listInstances(KnowledgeBase aKB, String aConceptIri, boolean aAll)
    {
        try (StopWatch watch = new StopWatch(log, "readInstance(%s)", aConceptIri)) {
            SPARQLQuery query = SPARQLQueryBuilder.forInstances(aKB) //
                    .childrenOf(aConceptIri) //
                    .retrieveLabel() //
                    .retrieveDescription();

            List<KBHandle> result;
            if (aKB.isReadOnly()) {
                result = listHandlesCaching(aKB, query, aAll);
            }
            else {
                result = read(aKB, conn -> query.asHandles(conn, aAll));
            }

            return result;
        }
    }

    // Statements

    @Override
    public void upsertStatement(KnowledgeBase aKB, KBStatement aStatement)
        throws RepositoryException
    {
        update(aKB, conn -> getReificationStrategy(aKB).upsertStatement(conn, aKB, aStatement));
    }

    @Override
    public void deleteStatement(KnowledgeBase aKB, KBStatement aStatement)
        throws RepositoryException
    {
        update(aKB, conn -> getReificationStrategy(aKB).deleteStatement(conn, aKB, aStatement));
    }

    @Override
    public List<KBStatement> listStatements(KnowledgeBase kb, KBHandle aInstance, boolean aAll)
    {
        try (StopWatch watch = new StopWatch(log, "listStatements(%s)",
                aInstance.getIdentifier())) {
            return read(kb,
                    conn -> getReificationStrategy(kb).listStatements(conn, kb, aInstance, aAll));
        }
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
        try (StopWatch watch = new StopWatch(log,
                "listStatementsWithPredicateOrObjectReference(%s)", aIdentifier)) {
            try (RepositoryConnection conn = getConnection(kb)) {
                ValueFactory vf = conn.getValueFactory();
                IRI iri = vf.createIRI(aIdentifier);
                try (RepositoryResult<Statement> predStmts = conn.getStatements(null, iri, null);
                        RepositoryResult<Statement> objStmts = conn.getStatements(null, null,
                                iri)) {
                    List<Statement> allStmts = new ArrayList<>();
                    Iterations.addAll(predStmts, allStmts);
                    Iterations.addAll(objStmts, allStmts);
                    return allStmts;
                }
            }
        }
    }

    @Override
    public void update(KnowledgeBase kb, UpdateAction aAction)
    {
        if (kb.isReadOnly()) {
            throw new ReadOnlyException(
                    "Knowledge base [" + kb.getName() + "] is read only, will not alter!");
        }

        try (RepositoryConnection conn = getConnection(kb)) {
            boolean error = true;
            try {
                conn.begin();
                aAction.accept(conn);
                conn.commit();
                error = false;
            }
            finally {
                if (error) {
                    conn.rollback();
                }
            }
        }
    }

    @Override
    public <T> T read(KnowledgeBase kb, ReadAction<T> aAction)
    {
        try (RepositoryConnection conn = getConnection(kb)) {
            return aAction.accept(conn);
        }
    }

    @Override
    public List<KBProperty> listDomainProperties(KnowledgeBase aKB, String aDomain,
            boolean aIncludeInferred, boolean aAll)
        throws QueryEvaluationException
    {
        try (StopWatch watch = new StopWatch(log, "listDomainProperties(%s)", aDomain)) {
            SPARQLQuery query = SPARQLQueryBuilder.forProperties(aKB).matchingDomain(aDomain)
                    .retrieveLabel().retrieveDescription().retrieveDomainAndRange()
                    .includeInferred(aIncludeInferred);

            List<KBHandle> result;
            if (aKB.isReadOnly()) {
                result = listHandlesCaching(aKB, query, aAll);
            }
            else {
                result = read(aKB, conn -> query.asHandles(conn, aAll));
            }

            return result.stream().map(handle -> KBHandle.convertTo(KBProperty.class, handle))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public List<KBHandle> listRootConcepts(KnowledgeBase aKB, boolean aAll)
        throws QueryEvaluationException
    {
        try (StopWatch watch = new StopWatch(log, "listRootConcepts()")) {
            SPARQLQuery query = SPARQLQueryBuilder.forClasses(aKB).roots().retrieveLabel()
                    .retrieveDescription();

            List<KBHandle> result;
            if (aKB.isReadOnly()) {
                result = listHandlesCaching(aKB, query, aAll);
            }
            else {
                result = read(aKB, conn -> query.asHandles(conn, aAll));
            }

            return result;
        }
    }

    @Override
    public boolean hasChildConcepts(KnowledgeBase aKB, String aParentIdentifier, boolean aAll)
    {
        try (StopWatch watch = new StopWatch(log, "hasChildConcepts(%s)", aParentIdentifier)) {
            return read(aKB, conn -> SPARQLQueryBuilder.forClasses(aKB)
                    .childrenOf(aParentIdentifier).exists(conn, aAll));
        }
    }

    @Override
    public List<KBHandle> listChildConcepts(KnowledgeBase aKB, String aParentIdentifier,
            boolean aAll)
        throws QueryEvaluationException
    {
        return listChildConcepts(aKB, aParentIdentifier, aAll, DEFAULT_LIMIT);
    }

    @Override
    public List<KBHandle> getConceptForInstance(KnowledgeBase aKB, String aIdentifier, boolean aAll)
        throws QueryEvaluationException
    {
        try (StopWatch watch = new StopWatch(log, "getConceptForInstance(%s)", aIdentifier)) {
            SPARQLQuery query = SPARQLQueryBuilder.forClasses(aKB) //
                    .parentsOf(aIdentifier) //
                    .retrieveLabel() //
                    .retrieveDescription();

            List<KBHandle> result;
            if (aKB.isReadOnly()) {
                result = listHandlesCaching(aKB, query, aAll);
            }
            else {
                result = read(aKB, conn -> query.asHandles(conn, aAll));
            }

            return result;
        }
    }

    @Override
    public List<KBHandle> getParentConceptList(KnowledgeBase aKB, String aIdentifier, boolean aAll)
        throws QueryEvaluationException
    {
        try (StopWatch watch = new StopWatch(log, "getParentConceptList(%s)", aIdentifier)) {
            SPARQLQuery query = SPARQLQueryBuilder.forClasses(aKB) //
                    .ancestorsOf(aIdentifier) //
                    .retrieveLabel() //
                    .retrieveDescription();

            List<KBHandle> result;
            if (aKB.isReadOnly()) {
                result = listHandlesCaching(aKB, query, aAll);
            }
            else {
                result = read(aKB, conn -> query.asHandles(conn, aAll));
            }

            return result;
        }
    }

    @Override
    public List<KBHandle> listChildConcepts(KnowledgeBase aKB, String aParentIdentifier,
            boolean aAll, int aLimit)
        throws QueryEvaluationException
    {
        try (StopWatch watch = new StopWatch(log, "listChildConcepts(%s)", aParentIdentifier)) {
            SPARQLQuery query = SPARQLQueryBuilder.forClasses(aKB) //
                    .childrenOf(aParentIdentifier) //
                    .retrieveLabel() //
                    .retrieveDescription() //
                    .limit(aLimit);

            List<KBHandle> result;
            if (aKB.isReadOnly()) {
                result = listHandlesCaching(aKB, query, aAll);
            }
            else {
                result = read(aKB, conn -> query.asHandles(conn, aAll));
            }

            return result;
        }
    }

    private ReificationStrategy getReificationStrategy(KnowledgeBase kb)
    {
        switch (kb.getReification()) {
        case WIKIDATA:
            return new WikiDataReification();
        case NONE: // Fallthrough
        default:
            return new NoReification();
        }
    }

    /**
     * Create base property with a specific IRI as identifier for the base property (which includes
     * subClassOf, label and description)
     * 
     * @param akb
     *            The knowledge base to initialize base properties
     * @param aProperty
     *            Property to be created for KB
     */
    public void createBaseProperty(KnowledgeBase akb, KBProperty aProperty)
    {
        update(akb, (conn) -> aProperty.write(conn, akb));
    }

    @Override
    public void addQualifier(KnowledgeBase aKB, KBQualifier newQualifier)
    {
        update(aKB, conn -> getReificationStrategy(aKB).upsertQualifier(conn, aKB, newQualifier));
    }

    @Override
    public void deleteQualifier(KnowledgeBase aKB, KBQualifier oldQualifier)
    {
        update(aKB, conn -> getReificationStrategy(aKB).deleteQualifier(conn, aKB, oldQualifier));
    }

    @Override
    public void upsertQualifier(KnowledgeBase aKB, KBQualifier aQualifier)
    {
        update(aKB, conn -> getReificationStrategy(aKB).upsertQualifier(conn, aKB, aQualifier));
    }

    @Override
    public List<KBQualifier> listQualifiers(KnowledgeBase aKB, KBStatement aStatement)
    {
        try (StopWatch watch = new StopWatch(log, "listQualifiers(%s)",
                aStatement.getStatementId())) {
            return read(aKB,
                    conn -> getReificationStrategy(aKB).listQualifiers(conn, aKB, aStatement));
        }
    }

    @Override
    public boolean exists(KnowledgeBase aKB, KBStatement mockStatement)
    {
        return read(aKB, conn -> getReificationStrategy(aKB).exists(conn, aKB, mockStatement));
    }

    @Override
    public Map<String, KnowledgeBaseProfile> readKnowledgeBaseProfiles() throws IOException
    {
        return KnowledgeBaseProfile.readKnowledgeBaseProfiles();
    }

    /**
     * Read identifier IRI and return {@link Optional} of {@link KBObject}
     * 
     * @return {@link Optional} of {@link KBObject} of type {@link KBConcept} or {@link KBInstance}
     */
    @Override
    public Optional<KBObject> readItem(Project aProject, String aIdentifier)
    {
        for (KnowledgeBase kb : getEnabledKnowledgeBases(aProject)) {
            Optional<KBObject> handle = readItem(kb, aIdentifier);
            if (handle.isPresent()) {
                return handle;
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<KBObject> readItem(KnowledgeBase aKb, String aIdentifier)
    {
        try (StopWatch watch = new StopWatch(log, "readItem(%s)", aIdentifier)) {
            Optional<KBConcept> kbConcept = readConcept(aKb, aIdentifier, false);
            if (kbConcept.isPresent()) {
                return kbConcept.flatMap((c) -> Optional.of(c));
            }
            // In case we don't get the identifier as a concept we look for property/instance
            Optional<KBProperty> kbProperty = readProperty(aKb, aIdentifier);
            if (kbProperty.isPresent()) {
                return kbProperty.flatMap((p) -> Optional.of(p));
            }
            Optional<KBInstance> kbInstance = readInstance(aKb, aIdentifier);
            if (kbInstance.isPresent()) {
                return kbInstance.flatMap((i) -> Optional.of(i));
            }
            return Optional.empty();
        }
    }

    @Override
    public Optional<KBHandle> readHandle(KnowledgeBase aKB, String aIdentifier)
    {
        try (StopWatch watch = new StopWatch(log, "readHandle(%s)", aIdentifier)) {
            SPARQLQuery query = SPARQLQueryBuilder.forItems(aKB) //
                    .withIdentifier(aIdentifier) //
                    .retrieveLabel() //
                    .retrieveDescription();

            Optional<KBHandle> result;
            if (aKB.isReadOnly()) {
                result = fetchHandleCaching(aKB, query, true);
            }
            else {
                result = read(aKB, conn -> query.asHandle(conn, true));
            }

            return result;
        }
    }

    @Override
    public Optional<KBHandle> readHandle(Project aProject, String aIdentifier)
    {
        Optional<KBHandle> someResult = Optional.empty();

        for (KnowledgeBase kb : getEnabledKnowledgeBases(aProject)) {
            Optional<KBHandle> concept = readHandle(kb, aIdentifier);
            if (!concept.isPresent()) {
                continue;
            }

            someResult = concept;

            // If we find a handle with a label, we stop immediately. Otherwise, we continue with
            // the other KBs to see if there is one with a label. This is necessary because
            // readHandle *always* returns a result, even if there is no triple actually containing
            // the IRI in the KB.
            if (someResult.map(KBHandle::getName).isPresent()) {
                break;
            }
        }

        return someResult;
    }

    /**
     * List label properties.
     * 
     * @param aKB
     *            the KB for which to list the properties.
     * @param aClassInstance
     *            whether to include label properties for classes and instances.
     * @param aProperties
     *            whether to include label properties for properties.
     * @return list of label properties.
     */
    private List<String> listLabelProperties(KnowledgeBase aKB, boolean aClassInstance,
            boolean aProperties)
    {
        return read(aKB, conn -> {
            Iri pLabel = iri(aKB.getLabelIri());

            Variable property = SparqlBuilder.var("p");
            SelectQuery query = Queries.SELECT(property).distinct();

            List<GraphPattern> patterns = new ArrayList<>();
            if (aClassInstance) {
                Iri pSubProperty = iri(aKB.getSubPropertyIri());
                patterns.add(property.has(PropertyPathBuilder.of(pSubProperty).zeroOrMore().build(),
                        pLabel));
            }
            if (aProperties) {
                Iri pPropertyLabel = iri(aKB.getPropertyLabelIri());
                patterns.add(property
                        .has(PropertyPathBuilder.of(pPropertyLabel).zeroOrMore().build(), pLabel));
            }

            query.where(GraphPatterns.union(patterns.stream().toArray(GraphPattern[]::new)));

            TupleQuery tupleQuery = conn.prepareTupleQuery(query.getQueryString());

            List<String> labelProperties = new ArrayList<>();
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                while (result.hasNext()) {
                    BindingSet bindings = result.next();
                    labelProperties.add(bindings.getValue("p").stringValue());
                }
            }

            return labelProperties;
        });
    }

    @Override
    public List<String> listLabelProperties(KnowledgeBase aKB)
    {
        try (StopWatch watch = new StopWatch(log, "listLabelProperties()")) {
            return listLabelProperties(aKB, true, true);
        }
    }

    @Override
    public List<String> listConceptOrInstanceLabelProperties(KnowledgeBase aKB)
    {
        try (StopWatch watch = new StopWatch(log, "listConceptOrInstanceLabelProperties()")) {
            return listLabelProperties(aKB, true, false);
        }
    }

    @Override
    public List<String> listPropertyLabelProperties(KnowledgeBase aKB)
    {
        try (StopWatch watch = new StopWatch(log, "listPropertyLabelProperties()")) {
            return listLabelProperties(aKB, false, true);
        }
    }

    @Override
    public boolean isBaseProperty(String propertyIdentifier, KnowledgeBase aKB)
    {
        return propertyIdentifier.equals(aKB.getLabelIri())
                || propertyIdentifier.equals(aKB.getSubclassIri())
                || propertyIdentifier.equals(aKB.getDescriptionIri())
                || propertyIdentifier.equals(aKB.getTypeIri());
    }

    void reconfigureLocalKnowledgeBase(KnowledgeBase aKB)
    {
        /*
        // @formatter:off
        log.info("Forcing update of configuration for {}", aKB);
        Model model = new TreeModel();
        ValueFactory vf = SimpleValueFactory.getInstance();
        IRI root = vf
                .createIRI("http://inception-project.github.io/kbexport#" + aKB.getRepositoryId());
        repoManager.getRepositoryConfig(aKB.getRepositoryId()).export(model, root);
        StringWriter out = new StringWriter();
        Rio.write(model, out, RDFFormat.TURTLE);
        log.info("Current configuration: {}", out.toString());
        // @formatter:on
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
                luceneSailCfg.setIndexDir(
                        new File(kbRepositoriesRoot, "indexes/" + aKB.getRepositoryId())
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

        var repo = repoManager.getRepository(aKB.getRepositoryId());

        if (!(repo instanceof SailRepository)) {
            throw new IllegalArgumentException(
                    "Reindexing is not supported on [" + repo.getClass() + "] repositories");
        }

        var sail = ((SailRepository) repo).getSail();
        if (!(sail instanceof LuceneSail)) {
            throw new IllegalArgumentException(
                    "Reindexing is not supported on [" + sail.getClass() + "] repositories");
        }

        var luceneSail = (LuceneSail) sail;
        try (RepositoryConnection conn = getConnection(aKB)) {
            luceneSail.reindex();
            conn.commit();
        }
        catch (SailException e) {
            if (ExceptionUtils.hasCause(e, IndexFormatTooNewException.class)) {
                log.warn("Unable to access index: {}", e.getMessage());
                log.info("Downgrade detected - trying to rebuild index from scratch...");

                String luceneDir = luceneSail.getParameter(LuceneSail.LUCENE_DIR_KEY);
                luceneSail.shutDown();
                FileUtils.deleteQuietly(new File(luceneDir));
                luceneSail.init();

                // Only try to rebuild once - so no recursion here!
                try (RepositoryConnection conn = getConnection(aKB)) {
                    luceneSail.reindex();
                    conn.commit();
                }
            }
        }
    }

    @Override
    public boolean isKnowledgeBaseEnabled(Project aProject, String aRepositoryId)
    {
        Optional<KnowledgeBase> kb = Optional.empty();
        String repositoryId = aRepositoryId;
        if (repositoryId != null) {
            kb = getKnowledgeBaseById(aProject, aRepositoryId);
        }
        return kb.isPresent() && kb.get().isEnabled();
    }

    @Override
    public List<KBHandle> listHandlesCaching(KnowledgeBase aKB, SPARQLQuery aQuery, boolean aAll)
    {
        List<KBHandle> results = queryCache.get(QueryKey.of(aKB, aQuery, aAll));
        if (log.isTraceEnabled()) {
            log.trace("KB cache stats: {}", queryCache.stats());
        }
        return results;
    }

    @Override
    public Optional<KBHandle> fetchHandleCaching(KnowledgeBase aKB, SPARQLQuery aQuery,
            boolean aAll)
    {
        Optional<KBHandle> result = queryCache.get(QueryKey.of(aKB, aQuery, aAll)).stream()
                .findFirst();
        if (log.isTraceEnabled()) {
            log.trace("KB cache stats: {}", queryCache.stats());
        }
        return result;
    }

    private List<KBHandle> runQuery(QueryKey aKey)
    {
        return read(aKey.kb, conn -> aKey.query.asHandles(conn, true));
    }

    /**
     * If the KB configuration of a project is changed, clear the caches of any KBs of that project.
     * 
     * @param aEvent
     *            The event containing the project
     */
    @EventListener
    public void onKnowledgeBaseConfigurationChangedEvent(
            KnowledgeBaseConfigurationChangedEvent aEvent)
    {
        queryCache.asMap().keySet().stream()
                .filter(key -> key.kb.getProject().equals(aEvent.getProject()))
                .forEach(key -> queryCache.invalidate(key));
    }

    @EventListener
    @Transactional
    public void onBeforeProjectRemovedEvent(BeforeProjectRemovedEvent aEvent)
    {
        Project project = aEvent.getProject();

        for (KnowledgeBase kb : getKnowledgeBases(project)) {
            removeKnowledgeBase(kb);
        }

        try (var logCtx = withProjectLogger(project)) {
            log.info("Removed all knowledge bases from project {} being deleted", project);
        }
    }

    private static final class QueryKey
    {
        private final KnowledgeBase kb;
        private final SPARQLQuery query;
        private final boolean all;

        public static QueryKey of(KnowledgeBase aKb, SPARQLQuery aQuery, boolean aAll)
        {
            return new QueryKey(aKb, aQuery, aAll);
        }

        public QueryKey(KnowledgeBase aKb, SPARQLQuery aQuery, boolean aAll)
        {
            kb = aKb;
            query = aQuery;
            all = aAll;
        }

        @Override
        public boolean equals(final Object other)
        {
            if (!(other instanceof QueryKey)) {
                return false;
            }

            QueryKey castOther = (QueryKey) other;
            return new EqualsBuilder().append(kb, castOther.kb).append(all, castOther.all)
                    .append(query, castOther.query).isEquals();
        }

        @Override
        public int hashCode()
        {
            return new HashCodeBuilder().append(kb).append(query).append(all).toHashCode();
        }
    }
}

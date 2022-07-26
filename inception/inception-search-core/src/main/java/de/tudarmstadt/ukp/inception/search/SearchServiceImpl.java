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
package de.tudarmstadt.ukp.inception.search;

import static de.tudarmstadt.ukp.clarin.webanno.api.CasUpgradeMode.NO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.UNMANAGED_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.UNMANAGED_NON_INITIALIZING_ACCESS;
import static de.tudarmstadt.ukp.inception.search.SearchCasUtils.casToByteArray;
import static de.tudarmstadt.ukp.inception.search.model.AnnotationSearchState.KEY_SEARCH_STATE;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableSet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang3.Validate;
import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterCasWrittenEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterDocumentCreatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterProjectRemovedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.BeforeDocumentRemovedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.BeforeProjectRemovedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.LayerConfigurationChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageSession;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.search.config.SearchServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.search.config.SearchServiceProperties;
import de.tudarmstadt.ukp.inception.search.index.IndexRebuildRequiredException;
import de.tudarmstadt.ukp.inception.search.index.PhysicalIndex;
import de.tudarmstadt.ukp.inception.search.index.PhysicalIndexFactory;
import de.tudarmstadt.ukp.inception.search.index.PhysicalIndexRegistry;
import de.tudarmstadt.ukp.inception.search.model.BulkIndexingContext;
import de.tudarmstadt.ukp.inception.search.model.Index;
import de.tudarmstadt.ukp.inception.search.model.Monitor;
import de.tudarmstadt.ukp.inception.search.model.Progress;
import de.tudarmstadt.ukp.inception.search.scheduling.tasks.IndexAnnotationDocumentTask;
import de.tudarmstadt.ukp.inception.search.scheduling.tasks.IndexSourceDocumentTask;
import de.tudarmstadt.ukp.inception.search.scheduling.tasks.IndexingTask_ImplBase;
import de.tudarmstadt.ukp.inception.search.scheduling.tasks.ReindexTask;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link SearchServiceAutoConfiguration#searchService}.
 * </p>
 */
@Transactional
public class SearchServiceImpl
    implements SearchService, DisposableBean
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private @PersistenceContext EntityManager entityManager;

    private final DocumentService documentService;
    private final AnnotationSchemaService schemaService;
    private final ProjectService projectService;
    private final PhysicalIndexRegistry physicalIndexRegistry;
    private final SchedulingService schedulingService;
    private final SearchServiceProperties properties;
    private final ScheduledExecutorService indexClosingScheduler;
    private final PreferencesService preferencesService;

    // In fact - the only factory we have at the moment...
    private final String DEFAULT_PHSYICAL_INDEX_FACTORY = "mtasDocumentIndexFactory";

    private boolean shutdown = false;

    @Autowired
    public SearchServiceImpl(DocumentService aDocumentService,
            AnnotationSchemaService aSchemaService, ProjectService aProjectService,
            PhysicalIndexRegistry aPhysicalIndexRegistry, SchedulingService aSchedulingService,
            SearchServiceProperties aProperties, PreferencesService aPreferencesService)
    {
        documentService = aDocumentService;
        schemaService = aSchemaService;
        projectService = aProjectService;
        physicalIndexRegistry = aPhysicalIndexRegistry;
        schedulingService = aSchedulingService;
        preferencesService = aPreferencesService;

        properties = aProperties;
        log.info("Index keep-open time: {}", properties.getIndexKeepOpenTime());

        indexClosingScheduler = new ScheduledThreadPoolExecutor(0);
        indexClosingScheduler.scheduleWithFixedDelay(this::closeIdleIndexes, 10, 10, SECONDS);
    }

    private void closeIdleIndexes()
    {
        long now = System.currentTimeMillis();
        long idleAllowed = properties.getIndexKeepOpenTime().toMillis();

        List<PooledIndex> pooledIndexesSnapshot;
        synchronized (indexes) {
            pooledIndexesSnapshot = new ArrayList<>(indexes.values());

            for (PooledIndex pooledIndex : pooledIndexesSnapshot) {
                if (pooledIndex.isTombstone()) {
                    continue;
                }

                if (pooledIndex.isIdle() && (now - pooledIndex.getLastAccess() > idleAllowed)
                        || pooledIndex.isForceRecycle()) {
                    unloadIndex(pooledIndex);
                }
            }
        }
    }

    @Override
    public void destroy()
    {
        log.info("Shutting down search service!");

        shutdown = true;

        indexClosingScheduler.shutdown();

        // We'll just wait a bit for any running indexing tasks to finish up before we close
        // all the indexes
        schedulingService.stopAllTasksMatching(task -> task instanceof IndexingTask_ImplBase);
        long t0 = currentTimeMillis();
        while (isBusy() && currentTimeMillis() - t0 < 10_000) {
            try {
                Thread.sleep(500);
            }
            catch (InterruptedException e) {
                // Ignore
            }
        }

        while (!indexes.isEmpty()) {
            synchronized (indexes) {
                List<PooledIndex> pooledIndexesSnapshot = new ArrayList<>(indexes.values());

                for (PooledIndex pooledIndex : pooledIndexesSnapshot) {
                    unloadIndex(pooledIndex);
                }
            }
        }
    }

    /**
     * Unloads the index state and deactivates/closes the underlying physical index.
     */
    private synchronized void unloadIndex(PooledIndex aIndex)
    {
        if (aIndex.isDead()) {
            return;
        }

        aIndex.dead();

        Index index = aIndex.get();

        log.trace("Unloading index for project {}", index.getProject());

        synchronized (indexes) {
            try {
                if (!aIndex.isTombstone()) {
                    // We need to leave tombstones in the map - they get cleaned up once the project
                    // is fully deleted.
                    indexes.remove(index.getProject().getId());
                }

                if (index.getPhysicalIndex() != null) {
                    index.getPhysicalIndex().close();
                }
            }
            catch (Throwable e) {
                log.error("Exception while tying to unload index for project {}",
                        index.getProject(), e);
            }
        }
    }

    /**
     * Loads the index state from the database and activates the underlying physical index. If
     * necessary, a new index state is created in the DB.
     * 
     * @param aProjectId
     *            the project ID
     * @return the index or {@code null} if there is no suitable index factory.
     */
    private synchronized Index loadIndex(long aProjectId)
    {
        Project aProject = projectService.getProject(aProjectId);

        log.trace("Loading index for project {}", aProject);

        // Check if an index state has already been stored in the database
        // Currently, we assume that a project can have only a single index
        String sql = "FROM " + Index.class.getName() + " WHERE project = :project";
        Index index = entityManager.createQuery(sql, Index.class) //
                .setParameter("project", aProject) //
                .getResultStream() //
                .findFirst().orElse(null);

        // If no index state has been saved in the database yet, create one and save it
        if (index == null) {
            // Not found in the DB, create new index instance and store it in DB
            log.trace("Initializing persistent index state in project {}", aProject);

            index = new Index();
            index.setInvalid(false);
            index.setProject(aProject);
            index.setPhysicalProvider(DEFAULT_PHSYICAL_INDEX_FACTORY);
            entityManager.persist(index);
        }

        // Get the index factory
        PhysicalIndexFactory indexFactory = physicalIndexRegistry
                .getIndexFactory(DEFAULT_PHSYICAL_INDEX_FACTORY);

        if (indexFactory == null) {
            return null;
        }

        // Acquire the low-level index object and attach it to the index state (transient)
        index.setPhysicalIndex(indexFactory.getPhysicalIndex(aProject));

        return index;
    }

    /**
     * Triggered before a project is removed.
     * 
     * @param aEvent
     *            the event
     * @throws IOException
     *             if the index cannot be removed.
     */
    @EventListener
    public void beforeProjectRemove(BeforeProjectRemovedEvent aEvent) throws IOException
    {
        Project project = aEvent.getProject();

        log.trace("Removing index for project {} because project is being removed", project);

        try (PooledIndex pooledIndex = acquireIndex(project.getId())) {
            pooledIndex.tombstone();

            // Remove the index entry from the memory map
            unloadIndex(pooledIndex);

            // Physical index exists, drop it
            Index index = pooledIndex.get();
            if (index.getPhysicalIndex().isCreated()) {
                index.getPhysicalIndex().delete();
            }

            // Delete the index entry from the DB
            entityManager
                    .remove(entityManager.contains(index) ? index : entityManager.merge(index));
        }
    }

    @EventListener
    public void afterProjectRemove(AfterProjectRemovedEvent aEvent) throws IOException
    {
        synchronized (indexes) {
            indexes.remove(aEvent.getProject().getId());
        }
    }

    private final Map<Long, PooledIndex> indexes = new HashMap<>();

    private PooledIndex acquireIndex(long aProjectId)
    {
        synchronized (indexes) {
            PooledIndex pooledIndex = indexes.get(aProjectId);

            if (pooledIndex != null && pooledIndex.isTombstone()) {
                throw new IllegalStateException("Project [" + aProjectId
                        + "] is being deleted, index no longer accessible.");
            }

            // If the index needs to be recycled, we need to wait for exclusive access and then
            // recycle it
            if (pooledIndex != null) {
                if (pooledIndex.isForceRecycle() || pooledIndex.isDead()) {
                    while (!pooledIndex.isIdle() && !pooledIndex.isDead()) {
                        try {
                            log.trace("Index recycle is forced but index is not idle - waiting...");
                            Thread.sleep(1000);
                        }
                        catch (InterruptedException e) {
                            // Ignore
                        }
                    }

                    unloadIndex(pooledIndex);
                    pooledIndex = null; // Reload below
                }
            }

            if (pooledIndex == null) {
                Index index = loadIndex(aProjectId);
                pooledIndex = new PooledIndex(index);
                indexes.put(aProjectId, pooledIndex);
            }

            pooledIndex.borrow();

            return pooledIndex;
        }
    }

    @EventListener
    public void beforeDocumentRemove(BeforeDocumentRemovedEvent aEvent) throws IOException
    {
        SourceDocument document = aEvent.getDocument();
        Project project = document.getProject();

        log.trace(
                "Removing document {} from index for project {} because document is being removed",
                document, project);

        try (PooledIndex pooledIndex = acquireIndex(project.getId())) {
            Index index = pooledIndex.get();
            // If the index has not been created yet, there is nothing to do
            if (!index.getPhysicalIndex().isCreated()) {
                return;
            }

            // Remove source document from the index
            index.getPhysicalIndex().deindexDocument(document);

            // Remove related annotation documents from the index
            for (AnnotationDocument annotationDocument : documentService
                    .listAnnotationDocuments(document)) {
                index.getPhysicalIndex().deindexDocument(annotationDocument);
            }
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional
    public void afterDocumentCreate(AfterDocumentCreatedEvent aEvent)
    {
        log.trace("Starting afterDocumentCreate");

        // Schedule new document index process
        enqueueIndexDocument(aEvent.getDocument(), "afterDocumentCreate", aEvent.getCas());
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional
    public void afterAnnotationUpdate(AfterCasWrittenEvent aEvent)
    {
        log.trace("Starting afterAnnotationUpdate");

        // Schedule new document index process
        enqueueIndexDocument(aEvent.getDocument(), "afterAnnotationUpdate", aEvent.getCas());
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional
    public void beforeLayerConfigurationChanged(LayerConfigurationChangedEvent aEvent)
    {
        log.trace("Starting beforeLayerConfigurationChanged");

        Project project = aEvent.getProject();

        try (PooledIndex pooledIndex = acquireIndex(project.getId())) {
            pooledIndex.forceRecycle();
            Index index = pooledIndex.get();
            index.setInvalid(true);
            entityManager.merge(index);
        }

        // Schedule re-indexing of the physical index
        enqueueReindexTask(aEvent.getProject(), "beforeLayerConfigurationChanged");
    }

    @Override
    public void indexDocument(SourceDocument aSourceDocument, byte[] aBinaryCas)
    {
        try (PooledIndex pooledIndex = acquireIndex(aSourceDocument.getProject().getId())) {
            indexDocument(pooledIndex, aSourceDocument, aBinaryCas);
        }
    }

    private void indexDocument(PooledIndex aPooledIndex, SourceDocument aSourceDocument,
            byte[] aBinaryCas)
    {
        Project project = aSourceDocument.getProject();

        log.debug("Request to index source document {} in project {}", aSourceDocument, project);

        Index index = aPooledIndex.get();
        // Index already initialized? If not, schedule full re-indexing job. This will also
        // index the given document, so we can stop here after scheduling the re-indexing.
        if (!index.getPhysicalIndex().isCreated()) {
            log.trace(
                    "Index in project {} has not yet been initialized. Scheduling an asynchronous re-indexing.",
                    project);
            invalidateIndexAndForceIndexRebuild(project, index, "indexDocument");
            return;
        }

        // FIXME: This can probably be pulled out of the synchronized block to allow multiple
        // threads to update the index concurrently. The underlying index code should hopefully
        // be thread-safe...
        try {
            index.getPhysicalIndex().indexDocument(aSourceDocument, aBinaryCas);
        }
        catch (IndexRebuildRequiredException e) {
            invalidateIndexAndForceIndexRebuild(project, index, "indexDocument[error]");
        }
        catch (IOException e) {
            log.error("Error indexing source document {} in project {}", aSourceDocument, project,
                    e);
        }
    }

    @Override
    public void indexDocument(AnnotationDocument aAnnotationDocument, byte[] aBinaryCas)
    {
        Project project = aAnnotationDocument.getProject();

        try (PooledIndex pooledIndex = acquireIndex(project.getId())) {
            indexDocument(pooledIndex, aAnnotationDocument, "indexDocument", aBinaryCas);
        }
    }

    private boolean isPerformNoMoreActions(PooledIndex aPooledIndex)
    {
        // If the index is dead or marked to force-recycle, we shouldn't waste time
        // rebuilding the index on it. Either we shut down or there is another
        // re-indexing scheduled that will cover for us.
        return aPooledIndex.isTombstone() || aPooledIndex.isDead() || aPooledIndex.isForceRecycle()
                || shutdown;
    }

    private void indexDocument(PooledIndex aPooledIndex, AnnotationDocument aAnnotationDocument,
            String aTrigger, byte[] aBinaryCas)
    {
        Project project = aAnnotationDocument.getProject();

        log.debug("Request to index annotation document {} in project {}", aAnnotationDocument,
                project);

        if (isPerformNoMoreActions(aPooledIndex)) {
            return;
        }

        Index index = aPooledIndex.get();
        // Index already initialized? If not, schedule full re-indexing job. This will also
        // index the given document, so we can stop here after scheduling the re-indexing.
        if (!index.getPhysicalIndex().isCreated()) {
            log.trace(
                    "Index in project {} has not yet been initialized. Scheduling an asynchronous re-indexing.",
                    project);
            invalidateIndexAndForceIndexRebuild(project, index, "indexDocument");
            return;
        }

        if (isPerformNoMoreActions(aPooledIndex)) {
            return;
        }

        try {
            // Add annotation document to the index again
            log.trace("Indexing new version of annotation document {} in project {}",
                    aAnnotationDocument, project);
            index.getPhysicalIndex().indexDocument(aAnnotationDocument, aBinaryCas);
        }
        catch (IndexRebuildRequiredException e) {
            invalidateIndexAndForceIndexRebuild(project, index, "indexDocument[error]");
        }
        catch (IOException e) {
            log.error("Error indexing annotation document {} in project {}", aAnnotationDocument,
                    project, e);
        }
    }

    @Override
    @Transactional
    public List<SearchResult> query(User aUser, Project aProject, String aQuery)
        throws IOException, ExecutionException
    {
        return query(aUser, aProject, aQuery, null);
    }

    @Override
    @Transactional
    public List<SearchResult> query(User aUser, Project aProject, String aQuery,
            SourceDocument aDocument)
        throws IOException, ExecutionException
    {
        Map<String, List<SearchResult>> groupedResults = query(aUser, aProject, aQuery, aDocument,
                null, null, 0, Integer.MAX_VALUE);
        List<SearchResult> resultsAsList = new ArrayList<>();
        groupedResults.values().stream()
                .forEach(resultsGroup -> resultsAsList.addAll(resultsGroup));
        return resultsAsList;
    }

    @Override
    @Transactional
    public Map<String, List<SearchResult>> query(User aUser, Project aProject, String aQuery,
            SourceDocument aDocument, AnnotationLayer aAnnotationLayer,
            AnnotationFeature aAnnotationFeature, long offset, long count)
        throws IOException, ExecutionException
    {
        log.trace("Query [{}] for user {} in project {}", aQuery, aUser, aProject);

        try (PooledIndex pooledIndex = acquireIndex(aProject.getId())) {
            Index index = pooledIndex.get();
            ensureIndexIsCreatedAndValid(aProject, index);

            var prefs = preferencesService.loadDefaultTraitsForProject(KEY_SEARCH_STATE, aProject);
            return index.getPhysicalIndex().executeQuery(new SearchQueryRequest(aProject, aUser,
                    aQuery, aDocument, aAnnotationLayer, aAnnotationFeature, offset, count, prefs));
        }
    }

    @Override
    public StatisticsResult getProjectStatistics(User aUser, Project aProject,
            OptionalInt aMinTokenPerDoc, OptionalInt aMaxTokenPerDoc,
            Set<AnnotationFeature> aFeatures)
        throws IOException, ExecutionException
    {
        try (PooledIndex pooledIndex = acquireIndex(aProject.getId())) {
            Index index = pooledIndex.get();
            ensureIndexIsCreatedAndValid(aProject, index);

            var prefs = preferencesService.loadDefaultTraitsForProject(KEY_SEARCH_STATE, aProject);
            return index.getPhysicalIndex().getAnnotationStatistics(new StatisticRequest(aProject,
                    aUser, aMinTokenPerDoc, aMaxTokenPerDoc, aFeatures, null, prefs));
        }
    }

    @Override
    public StatisticsResult getQueryStatistics(User aUser, Project aProject, String aQuery,
            OptionalInt aMinTokenPerDoc, OptionalInt aMaxTokenPerDoc,
            Set<AnnotationFeature> aFeatures)
        throws ExecutionException, IOException
    {
        try (PooledIndex pooledIndex = acquireIndex(aProject.getId())) {
            Index index = pooledIndex.get();
            ensureIndexIsCreatedAndValid(aProject, index);
            PhysicalIndex physicalIndex = index.getPhysicalIndex();

            var prefs = preferencesService.loadDefaultTraitsForProject(KEY_SEARCH_STATE, aProject);
            StatisticRequest statRequest = new StatisticRequest(aProject, aUser, aMinTokenPerDoc,
                    aMaxTokenPerDoc, aFeatures, aQuery, prefs);
            LayerStatistics statistics = physicalIndex.getLayerStatistics(statRequest,
                    statRequest.getQuery(), physicalIndex.getUniqueDocuments(statRequest));

            statistics.setQuery(aQuery);
            Map<String, LayerStatistics> statisticsMap = new HashMap<String, LayerStatistics>();
            statisticsMap.put("query." + aQuery, statistics);

            StatisticsResult results = new StatisticsResult(statRequest, statisticsMap, aFeatures);

            return results;
        }
    }

    /**
     * Re-index the project. If there is no physical index, create a new one.
     */
    @Override
    @Transactional
    public void reindex(Project aProject, Monitor aMonitor) throws IOException
    {
        log.info("Re-indexing project {}. This may take a while...", aProject);

        Monitor monitor = aMonitor != null ? aMonitor : new Monitor();

        try (PooledIndex pooledIndex = acquireIndex(aProject.getId())) {
            if (isPerformNoMoreActions(pooledIndex)) {
                return;
            }

            Index index = pooledIndex.get();
            index.setInvalid(true);

            // Clear the index
            try {
                index.getPhysicalIndex().clear();
            }
            catch (IndexRebuildRequiredException e) {
                // We can ignore this since we are rebuilding the index already anyway
            }

            Set<String> usersWithPermissions = projectService
                    .listProjectUsersWithPermissions(aProject).stream() //
                    .map(User::getUsername) //
                    .collect(toUnmodifiableSet());
            List<AnnotationDocument> annotationDocuments = documentService
                    .listAnnotationDocuments(aProject).stream()
                    .filter(annDoc -> usersWithPermissions.contains(annDoc.getUser())) //
                    .collect(toList());
            List<SourceDocument> sourceDocuments = documentService.listSourceDocuments(aProject);

            monitor.setTodo(annotationDocuments.size() + sourceDocuments.size());

            // We do not need write access and do not want to add to the exclusive access CAS cache,
            // so we would normally use SHARED_READ_ONLY_ACCESS. However, that mode can only be used
            // with AUTO_CAS_UPGRADE which makes things slow. We want NO_CAS_UPGRADE.
            // So we use UNMANAGED_NON_INITIALIZING_ACCESS for the annotation CASes to avoid
            // initializing CASes for users who have not started working on a document but for which
            // an AnnotationDocument item exists (e.g. locked documents).
            // For INITIAL_CASes, we use UNMANAGED_ACCESS since the INITIAL_CAS should always
            // exist.
            final var accessModeAnnotationCas = UNMANAGED_NON_INITIALIZING_ACCESS;
            final var accessModeInitialCas = UNMANAGED_ACCESS;
            final var casUpgradeMode = NO_CAS_UPGRADE;

            var prefs = preferencesService.loadDefaultTraitsForProject(KEY_SEARCH_STATE, aProject);
            try (var indexContext = BulkIndexingContext.init(aProject, schemaService, true,
                    prefs)) {
                // Index all the source documents
                for (SourceDocument doc : sourceDocuments) {
                    if (isPerformNoMoreActions(pooledIndex)) {
                        return;
                    }

                    try (CasStorageSession session = CasStorageSession.openNested()) {
                        byte[] casAsByteArray = casToByteArray(documentService
                                .createOrReadInitialCas(doc, casUpgradeMode, accessModeInitialCas));
                        indexDocument(pooledIndex, doc, casAsByteArray);
                    }

                    monitor.incDone();
                }

                // Index all the annotation documents
                for (AnnotationDocument doc : annotationDocuments) {
                    if (isPerformNoMoreActions(pooledIndex)) {
                        return;
                    }

                    try (CasStorageSession session = CasStorageSession.openNested()) {
                        byte[] casAsByteArray = casToByteArray(
                                documentService.readAnnotationCas(doc.getDocument(), doc.getUser(),
                                        casUpgradeMode, accessModeAnnotationCas));
                        indexDocument(pooledIndex, doc, "reindex", casAsByteArray);
                    }
                    catch (FileNotFoundException e) {
                        // Ignore it if a annotation CAS does not exist yet
                    }

                    monitor.incDone();
                }
            }

            // After re-indexing, reset the invalid flag
            index.setInvalid(false);
            entityManager.merge(index);
        }

        log.info("Re-indexing project {} complete!", aProject);
    }

    /**
     * For testing only...
     */
    @Override
    public boolean isIndexValid(Project aProject)
    {
        synchronized (indexes) {
            PooledIndex pooledIndex = indexes.get(aProject.getId());

            if (pooledIndex == null) {
                return false;
            }

            return !pooledIndex.get().getInvalid();
        }
    }

    @Override
    public Optional<Progress> getIndexProgress(Project aProject)
    {
        Validate.notNull(aProject, "Project cannot be null");

        List<IndexingTask_ImplBase> tasks = schedulingService.getAllTasks().stream() //
                .filter(task -> task instanceof IndexingTask_ImplBase)
                .map(task -> (IndexingTask_ImplBase) task)
                .filter(task -> aProject.equals(task.getProject())) //
                .collect(Collectors.toList());

        if (tasks.isEmpty()) {
            return Optional.empty();
        }

        int total = 0;
        int done = 0;
        for (IndexingTask_ImplBase task : tasks) {
            Progress p = task.getProgress();
            total += p.getTotal();
            done += p.getDone();
        }

        return Optional.of(new Progress(done, total));
    }

    @Override
    public long determineNumOfQueryResults(User aUser, Project aProject, String aQuery,
            SourceDocument aDocument, AnnotationLayer aAnnotationLayer,
            AnnotationFeature aAnnotationFeature)
        throws IOException, ExecutionException
    {
        log.trace("Count results for query [{}] for user {} in project {}", aQuery, aUser,
                aProject);

        try (PooledIndex pooledIndex = acquireIndex(aProject.getId())) {
            Index index = pooledIndex.get();

            ensureIndexIsCreatedAndValid(aProject, index);

            // Index is valid, try to execute the query
            var prefs = preferencesService.loadDefaultTraitsForProject(KEY_SEARCH_STATE, aProject);
            return index.getPhysicalIndex().numberOfQueryResults(new SearchQueryRequest(aProject,
                    aUser, aQuery, aDocument, aAnnotationLayer, aAnnotationFeature, 0L, 0L, prefs));
        }
    }

    /**
     * Checks if the index has been created and is valid. If necessary, a re-indexing operation is
     * scheduled and an {@link ExecutionException} is thrown to short-circuit the caller.
     */
    private void ensureIndexIsCreatedAndValid(Project aProject, Index aIndex)
        throws ExecutionException
    {
        // Does the index exist at all?
        if (!aIndex.getPhysicalIndex().isCreated()) {
            invalidateIndexAndForceIndexRebuild(aProject, aIndex,
                    "ensureIndexIsCreatedAndValid[doesNotExist]");
            // Throw execution exception so that the user knows the query was not run
            throw (new ExecutionException("Index still building. Try again later."));
        }

        // Is the index valid?
        if (aIndex.getInvalid()) {
            if (getIndexProgress(aProject).isEmpty()) {
                // Index is invalid, schedule a new index rebuild
                enqueueReindexTask(aProject, "ensureIndexIsCreatedAndValid[invalid]");
            }

            // Throw execution exception so that the user knows the query was not run
            throw (new ExecutionException("Index still building. Try again later."));
        }

        // Is the index usable - it might be corrupt and needs rebuilding
        try {
            aIndex.getPhysicalIndex().open();
        }
        catch (IndexRebuildRequiredException e) {
            invalidateIndexAndForceIndexRebuild(aProject, aIndex,
                    "ensureIndexIsCreatedAndValid[error]");
            // Throw execution exception so that the user knows the query was not run
            throw (new ExecutionException("Index still building. Try again later."));
        }
        catch (IOException e) {
            throw new ExecutionException("Unable to access index", e);
        }
    }

    private void invalidateIndexAndForceIndexRebuild(Project aProject, Index aIndex, String aReason)
    {
        aIndex.setInvalid(true);
        entityManager.merge(aIndex);

        enqueueReindexTask(aProject, "ensureIndexIsCreatedAndValid[doesNotExist]");
    }

    @Override
    @Transactional
    public void enqueueReindexTask(Project aProject, String aTrigger)
    {
        enqueue(new ReindexTask(aProject, aTrigger));
    }

    private void enqueueIndexDocument(SourceDocument aSourceDocument, String aTrigger, CAS aCas)
    {
        try {
            enqueue(new IndexSourceDocumentTask(aSourceDocument, aTrigger, casToByteArray(aCas)));
        }
        catch (IOException e) {
            log.error("Unable to enqueue document {} for indexing", aSourceDocument, e);
        }
    }

    private void enqueueIndexDocument(AnnotationDocument aAnnotationDocument, String aTrigger,
            CAS aCas)
    {
        try {
            enqueue(new IndexAnnotationDocumentTask(aAnnotationDocument, aTrigger,
                    casToByteArray(aCas)));
        }
        catch (IOException e) {
            log.error("Unable to enqueue document {} for indexing", aAnnotationDocument, e);
        }
    }

    /**
     * Put a new indexing task in the queue. Indexing tasks can be of three types:
     * <ul>
     * <li>Indexing of a whole project</li>
     * <li>Indexing of a source document</li>
     * <li>Indexing of an annotation document for a given user</li>
     * </ul>
     * 
     * @param aRunnable
     *            The indexing task
     */
    public synchronized void enqueue(IndexingTask_ImplBase aRunnable)
    {
        schedulingService.enqueue(aRunnable);
    }

    private boolean isBusy()
    {
        return schedulingService.getScheduledAndRunningTasks().stream()
                .anyMatch(task -> task instanceof IndexingTask_ImplBase);
    }

    private class PooledIndex
        implements AutoCloseable
    {
        private final Index delegate;
        private final AtomicInteger refCount;
        private final AtomicLong lastAccess;

        private AtomicBoolean forceRecycle;
        private AtomicBoolean dead;
        private AtomicBoolean tombstone;

        public PooledIndex(Index aDelegate)
        {
            delegate = aDelegate;
            refCount = new AtomicInteger(0);
            lastAccess = new AtomicLong(currentTimeMillis());
            forceRecycle = new AtomicBoolean(false);
            dead = new AtomicBoolean(false);
            tombstone = new AtomicBoolean(false);
        }

        public Index get()
        {
            return delegate;
        }

        public void borrow()
        {
            refCount.incrementAndGet();
        }

        @Override
        public void close()
        {
            refCount.decrementAndGet();
            lastAccess.set(currentTimeMillis());
        }

        public void forceRecycle()
        {
            forceRecycle.set(true);
        }

        public boolean isForceRecycle()
        {
            return forceRecycle.get();
        }

        public long getLastAccess()
        {
            return lastAccess.get();
        }

        public boolean isIdle()
        {
            return refCount.get() == 0;
        }

        public void dead()
        {
            dead.set(true);
        }

        public boolean isDead()
        {
            return dead.get();
        }

        public void tombstone()
        {
            tombstone.set(true);
        }

        public boolean isTombstone()
        {
            return tombstone.get();
        }
    }
}

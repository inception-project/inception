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

import static de.tudarmstadt.ukp.inception.scheduling.TaskState.RUNNING;
import static de.tudarmstadt.ukp.inception.search.model.AnnotationSearchState.KEY_SEARCH_STATE;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.event.AfterCasWrittenEvent;
import de.tudarmstadt.ukp.inception.documents.event.AfterDocumentCreatedEvent;
import de.tudarmstadt.ukp.inception.documents.event.BeforeDocumentRemovedEvent;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.project.api.event.AfterProjectCreatedEvent;
import de.tudarmstadt.ukp.inception.project.api.event.AfterProjectRemovedEvent;
import de.tudarmstadt.ukp.inception.project.api.event.BeforeProjectRemovedEvent;
import de.tudarmstadt.ukp.inception.scheduling.Progress;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.schema.api.event.LayerConfigurationChangedEvent;
import de.tudarmstadt.ukp.inception.search.config.SearchServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.search.config.SearchServiceProperties;
import de.tudarmstadt.ukp.inception.search.index.IndexRebuildRequiredException;
import de.tudarmstadt.ukp.inception.search.index.PhysicalIndexRegistry;
import de.tudarmstadt.ukp.inception.search.model.Index;
import de.tudarmstadt.ukp.inception.search.scheduling.tasks.IndexAnnotationDocumentTask;
import de.tudarmstadt.ukp.inception.search.scheduling.tasks.IndexSourceDocumentTask;
import de.tudarmstadt.ukp.inception.search.scheduling.tasks.IndexingTask_ImplBase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

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
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @PersistenceContext EntityManager entityManager;

    private final DocumentService documentService;
    private final ProjectService projectService;
    private final PhysicalIndexRegistry physicalIndexRegistry;
    private final SchedulingService schedulingService;
    private final SearchServiceProperties properties;
    private final ScheduledExecutorService indexClosingScheduler;
    private final PreferencesService preferencesService;

    // In fact - the only factory we have at the moment...
    private final String DEFAULT_PHSYICAL_INDEX_FACTORY = "mtasDocumentIndexFactory";

    private boolean shutdown = false;

    public SearchServiceImpl(DocumentService aDocumentService, ProjectService aProjectService,
            PhysicalIndexRegistry aPhysicalIndexRegistry, SchedulingService aSchedulingService,
            SearchServiceProperties aProperties, PreferencesService aPreferencesService)
    {
        documentService = aDocumentService;
        projectService = aProjectService;
        physicalIndexRegistry = aPhysicalIndexRegistry;
        schedulingService = aSchedulingService;
        preferencesService = aPreferencesService;

        properties = aProperties;
        LOG.debug("Index keep-open time: {}", properties.getIndexKeepOpenTime());

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

            for (var pooledIndex : pooledIndexesSnapshot) {
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
        if (shutdown) {
            return;
        }

        LOG.info("Shutting down search service!");

        shutdown = true;

        indexClosingScheduler.shutdown();

        // We'll just wait a bit for any running indexing tasks to finish up before we close
        // all the indexes
        schedulingService.stopAllTasksMatching(task -> task instanceof IndexingTask_ImplBase);
        long t0 = currentTimeMillis();
        while (isBusy() && (currentTimeMillis() - t0) < 10_000) {
            try {
                Thread.sleep(500);
            }
            catch (InterruptedException e) {
                // Ignore
            }
        }

        while (!indexes.isEmpty()) {
            synchronized (indexes) {
                var pooledIndexesSnapshot = indexes.values() //
                        .stream().filter(i -> !i.isDead()) //
                        .toList();

                if (pooledIndexesSnapshot.isEmpty()) {
                    break;
                }

                for (var pooledIndex : pooledIndexesSnapshot) {
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

        var index = aIndex.get();

        LOG.trace("Unloading index for project {}", index.getProject());

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
                LOG.error("Exception while tying to unload index for project {}",
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
     * @param aPersist
     *            whether to persist the index in the DB - should be false when acquiring the index
     *            as part of deleting a project
     * @return the index or {@code null} if there is no suitable index factory.
     */
    private synchronized Index loadIndex(long aProjectId, boolean aPersist)
    {
        Project project = null;
        try {
            project = projectService.getProject(aProjectId);
        }
        catch (NoResultException e) {
            throw new IllegalStateException(
                    "Project [" + aProjectId + "] does not exit, index not accessible.");
        }

        LOG.trace("Loading index for project {}", project);

        // Check if an index state has already been stored in the database
        // Currently, we assume that a project can have only a single index
        var sql = "FROM " + Index.class.getName() + " WHERE project = :project";
        var index = entityManager.createQuery(sql, Index.class) //
                .setParameter("project", project) //
                .getResultStream() //
                .findFirst().orElse(null);

        // If no index state has been saved in the database yet, create one and save it
        if (index == null) {
            // Not found in the DB, create new index instance and store it in DB
            LOG.trace("Initializing persistent index state in project {}", project);

            index = new Index();
            index.setInvalid(false);
            index.setProject(project);
            index.setPhysicalProvider(DEFAULT_PHSYICAL_INDEX_FACTORY);

            if (aPersist) {
                entityManager.persist(index);
            }
        }
        else {
            if (!aPersist) {
                entityManager.detach(index);
            }
        }

        // Get the index factory
        var indexFactory = physicalIndexRegistry.getIndexFactory(DEFAULT_PHSYICAL_INDEX_FACTORY);

        if (indexFactory == null) {
            return null;
        }

        // Acquire the low-level index object and attach it to the index state (transient)
        index.setPhysicalIndex(indexFactory.getPhysicalIndex(project));

        return index;
    }

    @EventListener
    public void onAfterProjectCreated(AfterProjectCreatedEvent aEvent)
    {
        LOG.trace("Starting onAfterProjectCreated");

        // This will help us block off individual indexDocument tasks that are being triggered
        // while the project is imported or initialized (if those processes run in a tasks-suspended
        // context.
        enqueueReindexTask(aEvent.getProject(), "onAfterProjectCreated");
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
    public void onBeforeProjectRemove(BeforeProjectRemovedEvent aEvent) throws IOException
    {
        var project = aEvent.getProject();

        if (isBeingDeleted(project.getId())) {
            LOG.trace(
                    "Ignoring repeated attempt to delete project which is already in progress of being deleted",
                    project);
            return;
        }

        LOG.trace("Removing index for project {} because project is being removed", project);

        try (var pooledIndex = acquireIndex(project.getId(), false)) {
            pooledIndex.tombstone();

            // Remove the index entry from the memory map
            unloadIndex(pooledIndex);

            // Physical index exists, drop it
            var index = pooledIndex.get();
            if (index.getPhysicalIndex().isCreated()) {
                index.getPhysicalIndex().delete();
            }
        }
    }

    @EventListener
    public void onAfterProjectRemove(AfterProjectRemovedEvent aEvent) throws IOException
    {
        synchronized (indexes) {
            indexes.remove(aEvent.getProject().getId());
        }
    }

    private final Map<Long, PooledIndex> indexes = new HashMap<>();

    private boolean isBeingDeleted(long aProjectId)
    {
        synchronized (indexes) {
            var pooledIndex = indexes.get(aProjectId);
            if (pooledIndex != null && pooledIndex.isTombstone()) {
                return true;
            }
            return false;
        }
    }

    PooledIndex acquireIndex(long aProjectId)
    {
        return acquireIndex(aProjectId, true);
    }

    private PooledIndex acquireIndex(long aProjectId, boolean aPersist)
    {
        synchronized (indexes) {
            var pooledIndex = indexes.get(aProjectId);

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
                            LOG.trace("Index recycle is forced but index is not idle - waiting...");
                            // Thread.sleep(1000);
                            indexes.wait(1000);
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
                var index = loadIndex(aProjectId, aPersist);
                pooledIndex = new PooledIndex(index);
                indexes.put(aProjectId, pooledIndex);
            }

            pooledIndex.borrow();

            return pooledIndex;
        }
    }

    @Transactional
    void writeIndex(PooledIndex aIndex)
    {
        if (aIndex.isTombstone()) {
            return;
        }

        var index = aIndex.get();

        if (index.getId() == null) {
            entityManager.persist(index);
        }
        else {
            entityManager.merge(index);
        }
    }

    @EventListener
    public void onBeforeDocumentRemove(BeforeDocumentRemovedEvent aEvent) throws IOException
    {
        var document = aEvent.getDocument();
        var project = document.getProject();

        LOG.trace(
                "Removing document {} from index for project {} because document is being removed",
                document, project);

        try (var pooledIndex = acquireIndex(project.getId())) {
            var index = pooledIndex.get();
            // If the index has not been created yet, there is nothing to do
            if (!index.getPhysicalIndex().isCreated()) {
                return;
            }

            // Remove source document from the index
            index.getPhysicalIndex().deindexDocument(document);

            // Remove related annotation documents from the index
            for (var annotationDocument : documentService.listAnnotationDocuments(document)) {
                index.getPhysicalIndex().deindexDocument(annotationDocument);
            }
        }
    }

    @EventListener
    public void onAfterDocumentCreated(AfterDocumentCreatedEvent aEvent)
    {
        LOG.trace("Starting afterDocumentCreate");

        // Schedule new document index process
        enqueueIndexDocument(aEvent.getDocument(), "onAfterDocumentCreated");
    }

    @EventListener
    public void onAfterCasWritten(AfterCasWrittenEvent aEvent)
    {
        LOG.trace("Starting afterAnnotationUpdate");

        // Schedule new document index process
        enqueueIndexDocument(aEvent.getDocument(), "onAfterCasWritten");
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = REQUIRES_NEW)
    public void onLayerConfigurationChanged(LayerConfigurationChangedEvent aEvent)
    {
        LOG.trace("Starting beforeLayerConfigurationChanged");

        var project = aEvent.getProject();

        try (var pooledIndex = acquireIndex(project.getId())) {
            pooledIndex.forceRecycle();
            var index = pooledIndex.get();
            index.setInvalid(true);
            writeIndex(pooledIndex);
        }

        // Schedule re-indexing of the physical index
        enqueueReindexTask(aEvent.getProject(), "onLayerConfigurationChanged");
    }

    @Override
    @Transactional
    public void indexDocument(SourceDocument aSourceDocument, byte[] aBinaryCas)
    {
        try (var pooledIndex = acquireIndex(aSourceDocument.getProject().getId())) {
            indexDocument(pooledIndex, aSourceDocument, aBinaryCas);
        }
    }

    void indexDocument(PooledIndex aPooledIndex, SourceDocument aSourceDocument, byte[] aBinaryCas)
    {
        var project = aSourceDocument.getProject();

        LOG.debug("Request to index source document {} in project {}", aSourceDocument, project);

        var index = aPooledIndex.get();
        // Index already initialized? If not, schedule full re-indexing job. This will also
        // index the given document, so we can stop here after scheduling the re-indexing.
        if (!index.getPhysicalIndex().isCreated()) {
            LOG.trace(
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
        catch (Exception e) {
            LOG.error("Error indexing source document {} in project {}", aSourceDocument, project,
                    e);
        }
        catch (Throwable e) {
            LOG.error("Fatal error indexing source document {} in project {}", aSourceDocument,
                    project, e);
        }
    }

    @Override
    public void indexDocument(AnnotationDocument aAnnotationDocument, byte[] aBinaryCas)
    {
        Project project = aAnnotationDocument.getProject();

        try (var pooledIndex = acquireIndex(project.getId())) {
            indexDocument(pooledIndex, aAnnotationDocument, "indexDocument", aBinaryCas);
        }
        catch (Exception e) {
            LOG.error("Error indexing annotation document {} in project {}", aAnnotationDocument,
                    project, e);
        }
    }

    boolean isPerformNoMoreActions(PooledIndex aPooledIndex)
    {
        // If the index is dead or marked to force-recycle, we shouldn't waste time rebuilding the
        // index on it. Either we shut down or there is another re-indexing scheduled that will
        // cover for us.
        return aPooledIndex.isTombstone() || aPooledIndex.isDead() || aPooledIndex.isForceRecycle()
                || shutdown;
    }

    void indexDocument(PooledIndex aPooledIndex, AnnotationDocument aAnnotationDocument,
            String aTrigger, byte[] aBinaryCas)
    {
        var project = aAnnotationDocument.getProject();

        LOG.debug("Request to index annotation document {} in project {}", aAnnotationDocument,
                project);

        if (isPerformNoMoreActions(aPooledIndex)) {
            return;
        }

        var index = aPooledIndex.get();
        // Index already initialized? If not, schedule full re-indexing job. This will also
        // index the given document, so we can stop here after scheduling the re-indexing.
        if (!index.getPhysicalIndex().isCreated()) {
            LOG.trace(
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
            LOG.trace("Indexing new version of annotation document {} in project {}",
                    aAnnotationDocument, project);
            index.getPhysicalIndex().indexDocument(aAnnotationDocument, aBinaryCas);
        }
        catch (IndexRebuildRequiredException e) {
            invalidateIndexAndForceIndexRebuild(project, index, "indexDocument[error]");
        }
        catch (IOException e) {
            LOG.error("Error indexing annotation document {} in project {}", aAnnotationDocument,
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
        var groupedResults = query(aUser, aProject, aQuery, aDocument, null, null, 0, MAX_VALUE);

        return groupedResults.values().stream() //
                .flatMap(resultsGroup -> resultsGroup.stream()) //
                .distinct() //
                .toList();
    }

    @Override
    @Transactional
    public Map<String, List<SearchResult>> query(User aUser, Project aProject, String aQuery,
            SourceDocument aDocument, AnnotationLayer aAnnotationLayer,
            AnnotationFeature aAnnotationFeature, long offset, long count)
        throws IOException, ExecutionException
    {
        return query(SearchQueryRequest.builder() //
                .withProject(aProject) //
                .withUser(aUser) //
                .withQuery(aQuery) //
                .withLimitedToDocument(aDocument) //
                .withAnnotationLayer(aAnnotationLayer) //
                .withAnnotationFeature(aAnnotationFeature) //
                .withOffset(offset) //
                .withLimit(count).build());
    }

    @Override
    @Transactional
    public Map<String, List<SearchResult>> query(SearchQueryRequest aRequest)
        throws ExecutionException, IOException
    {
        LOG.trace("Query [{}] for user {} in project {}", aRequest.getQuery(), aRequest.getUser(),
                aRequest.getProject());

        var prefs = preferencesService.loadDefaultTraitsForProject(KEY_SEARCH_STATE,
                aRequest.getProject());

        try (var pooledIndex = acquireIndex(aRequest.getProject().getId())) {
            var index = pooledIndex.get();
            ensureIndexIsCreatedAndValid(aRequest.getProject(), index);

            return index.getPhysicalIndex().executeQuery(aRequest, prefs);
        }
    }

    @Override
    public StatisticsResult getProjectStatistics(User aUser, Project aProject, int aMinTokenPerDoc,
            int aMaxTokenPerDoc, Set<AnnotationFeature> aFeatures)
        throws IOException, ExecutionException
    {
        try (var pooledIndex = acquireIndex(aProject.getId())) {
            var index = pooledIndex.get();
            ensureIndexIsCreatedAndValid(aProject, index);

            var prefs = preferencesService.loadDefaultTraitsForProject(KEY_SEARCH_STATE, aProject);
            return index.getPhysicalIndex().getAnnotationStatistics(new StatisticRequest(aProject,
                    aUser, aMinTokenPerDoc, aMaxTokenPerDoc, aFeatures, null, prefs));
        }
    }

    @Override
    public StatisticsResult getQueryStatistics(User aUser, Project aProject, String aQuery,
            int aMinTokenPerDoc, int aMaxTokenPerDoc, Set<AnnotationFeature> aFeatures)
        throws ExecutionException, IOException
    {
        try (var pooledIndex = acquireIndex(aProject.getId())) {
            var index = pooledIndex.get();
            ensureIndexIsCreatedAndValid(aProject, index);
            var physicalIndex = index.getPhysicalIndex();

            var prefs = preferencesService.loadDefaultTraitsForProject(KEY_SEARCH_STATE, aProject);
            var statRequest = new StatisticRequest(aProject, aUser, aMinTokenPerDoc,
                    aMaxTokenPerDoc, aFeatures, aQuery, prefs);
            var statistics = physicalIndex.getLayerStatistics(statRequest, statRequest.getQuery(),
                    physicalIndex.getUniqueDocuments(statRequest));

            statistics.setQuery(aQuery);
            var statisticsMap = new HashMap<String, LayerStatistics>();
            statisticsMap.put("query." + aQuery, statistics);

            return new StatisticsResult(statRequest, statisticsMap, aFeatures);
        }
    }

    /**
     * For testing only...
     */
    @Override
    public boolean isIndexValid(Project aProject)
    {
        synchronized (indexes) {
            var pooledIndex = indexes.get(aProject.getId());

            if (pooledIndex == null) {
                return false;
            }

            return !pooledIndex.get().isInvalid();
        }
    }

    /**
     * @return the aggregate progress of all currently active indexing tasks (if any).
     */
    @Override
    public Optional<Progress> getIndexProgress(Project aProject)
    {
        Validate.notNull(aProject, "Project cannot be null");

        var tasks = schedulingService.getAllTasks().stream() //
                .filter(task -> task instanceof IndexingTask_ImplBase)
                .map(task -> (IndexingTask_ImplBase) task)
                .filter(task -> aProject.equals(task.getProject())) //
                .filter(task -> task.getMonitor() != null) //
                .filter(task -> task.getMonitor().getState() == RUNNING) //
                .toList();

        if (tasks.isEmpty()) {
            return Optional.empty();
        }

        var total = 0;
        var done = 0;
        for (var task : tasks) {
            var p = task.getProgress();
            total += p.maxProgress();
            done += p.progress();
        }

        return Optional.of(new Progress("", done, total));
    }

    @Override
    public long determineNumOfQueryResults(User aUser, Project aProject, String aQuery,
            SourceDocument aDocument, AnnotationLayer aAnnotationLayer,
            AnnotationFeature aAnnotationFeature)
        throws IOException, ExecutionException
    {
        LOG.trace("Count results for query [{}] for user {} in project {}", aQuery, aUser,
                aProject);

        try (var pooledIndex = acquireIndex(aProject.getId())) {
            var index = pooledIndex.get();

            ensureIndexIsCreatedAndValid(aProject, index);

            // Index is valid, try to execute the query
            var prefs = preferencesService.loadDefaultTraitsForProject(KEY_SEARCH_STATE, aProject);
            return index.getPhysicalIndex().numberOfQueryResults(new SearchQueryRequest(aProject,
                    aUser, aQuery, aDocument, aAnnotationLayer, aAnnotationFeature, 0L, 0L), prefs);
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
        if (aIndex.isInvalid()) {
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
        if (!aIndex.isInvalid()) {
            // Only update flag and DB if necessary
            aIndex.setInvalid(true);
            entityManager.merge(aIndex);
        }

        enqueueReindexTask(aProject, "ensureIndexIsCreatedAndValid[doesNotExist]");
    }

    private void enqueueReindexTask(Project aProject, String aTrigger)
    {
        enqueue(ReindexTask.builder() //
                .withProject(aProject) //
                .withTrigger(aTrigger) //
                .build());
    }

    @Override
    @Transactional
    public void enqueueReindexTask(Project aProject, User aUser, String aTrigger)
    {
        enqueue(ReindexTask.builder() //
                .withProject(aProject) //
                .withSessionOwner(aUser) //
                .withTrigger(aTrigger) //
                .build());
    }

    private void enqueueIndexDocument(SourceDocument aSourceDocument, String aTrigger)
    {
        enqueue(IndexSourceDocumentTask.builder() //
                .withSourceDocument(aSourceDocument) //
                .withTrigger(aTrigger) //
                .build());
    }

    private void enqueueIndexDocument(AnnotationDocument aAnnotationDocument, String aTrigger)
    {
        enqueue(IndexAnnotationDocumentTask.builder() //
                .withAnnotationDocument(aAnnotationDocument) //
                .withTrigger(aTrigger) //
                .build());
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

    static class PooledIndex
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

        /**
         * Borrow the index from the pool.
         */
        public void borrow()
        {
            refCount.incrementAndGet();
        }

        /**
         * Return the borrowed index to the pool.
         */
        @Override
        public void close()
        {
            refCount.decrementAndGet();
            lastAccess.set(currentTimeMillis());
        }

        /**
         * Mark the index for forced rebuilding.
         */
        public void forceRecycle()
        {
            forceRecycle.set(true);
        }

        /**
         * @return if the index needs to re-built.
         */
        public boolean isForceRecycle()
        {
            return forceRecycle.get();
        }

        public long getLastAccess()
        {
            return lastAccess.get();
        }

        /**
         * @return if the index is currently idle (not being used).
         */
        public boolean isIdle()
        {
            return refCount.get() == 0;
        }

        /**
         * Marks index as unloaded/deactivated.
         */
        public void dead()
        {
            dead.set(true);
        }

        /**
         * @return whether index is unloaded/deactivated.
         */
        public boolean isDead()
        {
            return dead.get();
        }

        /**
         * Marks index as deleted. Deleted indices should not be used anymore or re-created.
         */
        public void tombstone()
        {
            tombstone.set(true);
        }

        /**
         * @return if index has been deleted.
         */
        public boolean isTombstone()
        {
            return tombstone.get();
        }
    }

}

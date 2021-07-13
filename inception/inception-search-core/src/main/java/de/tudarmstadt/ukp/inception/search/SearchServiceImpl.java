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

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterCasWrittenEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterDocumentCreatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.BeforeDocumentRemovedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.BeforeProjectRemovedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.LayerConfigurationChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.search.config.SearchServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.search.config.SearchServiceProperties;
import de.tudarmstadt.ukp.inception.search.index.PhysicalIndexFactory;
import de.tudarmstadt.ukp.inception.search.index.PhysicalIndexRegistry;
import de.tudarmstadt.ukp.inception.search.model.Index;
import de.tudarmstadt.ukp.inception.search.scheduling.tasks.IndexAnnotationDocumentTask;
import de.tudarmstadt.ukp.inception.search.scheduling.tasks.IndexSourceDocumentTask;
import de.tudarmstadt.ukp.inception.search.scheduling.tasks.IndexingTask_ImplBase;
import de.tudarmstadt.ukp.inception.search.scheduling.tasks.ReindexTask;
import org.apache.commons.lang3.Validate;
import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static de.tudarmstadt.ukp.inception.search.SearchCasUtils.casToByteArray;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.SECONDS;

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
    private final ProjectService projectService;
    private final PhysicalIndexRegistry physicalIndexRegistry;
    private final SchedulingService schedulingService;
    private final SearchServiceProperties properties;
    private final ScheduledExecutorService indexClosingScheduler;

    // In fact - the only factory we have at the moment...
    private final String DEFAULT_PHSYICAL_INDEX_FACTORY = "mtasDocumentIndexFactory";

    private boolean shutdown = false;

    @Autowired
    public SearchServiceImpl(DocumentService aDocumentService, ProjectService aProjectService,
            PhysicalIndexRegistry aPhysicalIndexRegistry, SchedulingService aSchedulingService,
            SearchServiceProperties aProperties)
    {
        documentService = aDocumentService;
        projectService = aProjectService;
        physicalIndexRegistry = aPhysicalIndexRegistry;
        schedulingService = aSchedulingService;

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

        log.trace("Unloading index for project [{}]({})", index.getProject().getName(),
                index.getProject().getId());

        synchronized (indexes) {
            try {
                indexes.remove(index.getProject().getId());

                if (index.getPhysicalIndex() != null) {
                    index.getPhysicalIndex().close();
                }
            }
            catch (Throwable e) {
                log.error("Exception while tying to unload index for project [{}]({})",
                        index.getProject().getName(), index.getProject().getId(), e);
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

        log.trace("Loading index for project [{}]({})", aProject.getName(), aProject.getId());

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
            log.trace("Initializing persistent index state in project [{}]({}).",
                    aProject.getName(), aProject.getId());

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

        log.trace("Removing index for project [{}]({}) because project is being removed",
                project.getName(), project.getId());

        try (PooledIndex pooledIndex = acquireIndex(project.getId())) {
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

    private final Map<Long, PooledIndex> indexes = new HashMap<>();

    private PooledIndex acquireIndex(long aId)
    {
        synchronized (indexes) {
            PooledIndex pooledIndex = indexes.get(aId);

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
                Index index = loadIndex(aId);
                pooledIndex = new PooledIndex(index);
                indexes.put(aId, pooledIndex);
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
                "Removing document [{}]({}) from index for project [{}]({}) because document is being removed",
                document.getName(), document.getId(), project.getName(), project.getId());

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

        log.debug("Request to index source document [{}]({}) in project [{}]({})",
                aSourceDocument.getName(), aSourceDocument.getId(), project.getName(),
                project.getId());

        Index index = aPooledIndex.get();
        // Index already initialized? If not, schedule full re-indexing job. This will also
        // index the given document, so we can stop here after scheduling the re-indexing.
        if (!index.getPhysicalIndex().isCreated()) {
            log.trace(
                    "Index in project [{}]({}) has not yet been initialized. Scheduling an asynchronous re-indexing.",
                    project.getName(), project.getId());
            index.setInvalid(true);
            entityManager.merge(index);
            enqueueReindexTask(project, "indexDocument");
            return;
        }

        // FIXME: This can probably be pulled out of the synchronized block to allow multiple
        // threads to update the index concurrently. The underlying index code should hopefully
        // be thread-safe...
        try {
            index.getPhysicalIndex().indexDocument(aSourceDocument, aBinaryCas);
        }
        catch (IOException e) {
            log.error("Error indexing source document [{}]({}) in project [{}]({})",
                    aSourceDocument.getName(), aSourceDocument.getId(), project.getName(),
                    project.getId(), e);
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
        return aPooledIndex.isDead() || aPooledIndex.isForceRecycle() || shutdown;
    }

    private void indexDocument(PooledIndex aPooledIndex, AnnotationDocument aAnnotationDocument,
            String aTrigger, byte[] aBinaryCas)
    {
        Project project = aAnnotationDocument.getProject();

        log.debug("Request to index annotation document [{}]({}) in project [{}]({})",
                aAnnotationDocument.getName(), aAnnotationDocument.getId(), project.getName(),
                project.getId());

        if (isPerformNoMoreActions(aPooledIndex)) {
            return;
        }

        Index index = aPooledIndex.get();
        // Index already initialized? If not, schedule full re-indexing job. This will also
        // index the given document, so we can stop here after scheduling the re-indexing.
        if (!index.getPhysicalIndex().isCreated()) {
            log.trace(
                    "Index in project [{}]({}) has not yet been initialized. Scheduling an asynchronous re-indexing.",
                    project.getName(), project.getId());
            index.setInvalid(true);
            entityManager.merge(index);
            enqueueReindexTask(project, aTrigger);
            return;
        }

        if (isPerformNoMoreActions(aPooledIndex)) {
            return;
        }

        try {
            // Add annotation document to the index again
            log.trace("Indexing new version of annotation document [{}]({}) in project [{}]({})",
                    aAnnotationDocument.getName(), aAnnotationDocument.getId(), project.getName(),
                    project.getId());
            index.getPhysicalIndex().indexDocument(aAnnotationDocument, aBinaryCas);
        }
        catch (IOException e) {
            log.error("Error indexing annotation document [{}]({}) in project [{}]({})",
                    aAnnotationDocument.getName(), aAnnotationDocument.getId(), project.getName(),
                    project.getId(), e);
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
        log.trace("Query [{}] for user [{}] in project [{}]({})", aQuery, aUser.getUsername(),
                aProject.getName(), aProject.getId());

        try (PooledIndex pooledIndex = acquireIndex(aProject.getId())) {
            Index index = pooledIndex.get();

            ensureIndexIsCreatedAndValid(aProject, index);

            return index.getPhysicalIndex().executeQuery(new SearchQueryRequest(aProject, aUser,
                    aQuery, aDocument, aAnnotationLayer, aAnnotationFeature, offset, count));
        }
    }

    @Transactional
    public long getTokenPerDocumentStatistics(User aUser, Project aProject, String aStatistic,
                                                 SourceDocument aDocument, AnnotationLayer aAnnotationLayer,
                                                 AnnotationFeature aAnnotationFeature, long offset, long count)
        throws IOException, ExecutionException
    {
        log.trace("Statistics [{}] for user [{}] in project [{}]({})", aStatistic, aUser.getUsername(),
            aProject.getName(), aProject.getId());

        try (PooledIndex pooledIndex = acquireIndex(aProject.getId())) {
            Index index = pooledIndex.get();

            ensureIndexIsCreatedAndValid(aProject, index);

            return index.getPhysicalIndex().fetchStatistics(new StatisticRequest(aProject, aUser, aStatistic,
                 aDocument, aAnnotationLayer, aAnnotationFeature, offset, count));
        }
    }

    /**
     * Re-index the project. If there is no physical index, create a new one.
     */
    @Override
    @Transactional
    public void reindex(Project aProject) throws IOException
    {
        log.info("Re-indexing project [{}]({}) ", aProject.getName(), aProject.getId());

        try (PooledIndex pooledIndex = acquireIndex(aProject.getId())) {
            if (isPerformNoMoreActions(pooledIndex)) {
                return;
            }

            Index index = pooledIndex.get();
            index.setInvalid(true);

            // Clear the index
            index.getPhysicalIndex().clear();

            // Index all the annotation documents
            for (User user : projectService.listProjectUsersWithPermissions(aProject)) {
                List<AnnotationDocument> annotationDocumentsForUser = documentService
                        .listAnnotationDocuments(aProject, user);
                for (AnnotationDocument doc : annotationDocumentsForUser) {
                    if (isPerformNoMoreActions(pooledIndex)) {
                        return;
                    }

                    // Because serialization is a process which modifies internal data structures of
                    // the CAS, we need exclusive access the CAS for the time being.
                    // This can be relaxed after upgrading to UIMA 3.2.0 which includes a fix for
                    // for https://issues.apache.org/jira/browse/UIMA-6162
                    byte[] casAsByteArray;
                    try (CasStorageSession session = CasStorageSession.openNested()) {
                        casAsByteArray = casToByteArray(documentService.readAnnotationCas(doc));
                    }
                    indexDocument(pooledIndex, doc, "reindex", casAsByteArray);
                }
            }

            // Index all the source documents
            for (SourceDocument doc : documentService.listSourceDocuments(aProject)) {
                if (isPerformNoMoreActions(pooledIndex)) {
                    return;
                }

                // Because serialization is a process which modifies internal data structures of
                // the CAS, we need exclusive access the CAS for the time being.
                // This can be relaxed after upgrading to UIMA 3.2.0 which includes a fix for
                // for https://issues.apache.org/jira/browse/UIMA-6162
                byte[] casAsByteArray;
                try (CasStorageSession session = CasStorageSession.openNested()) {
                    casAsByteArray = casToByteArray(documentService.createOrReadInitialCas(doc));
                }

                indexDocument(pooledIndex, doc, casAsByteArray);
            }

            // After re-indexing, reset the invalid flag
            index.setInvalid(false);
            entityManager.merge(index);
        }
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
    public boolean isIndexInProgress(Project aProject)
    {
        Validate.notNull(aProject, "Project cannot be null");

        return schedulingService.getAllTasks().stream() //
                .filter(task -> task instanceof IndexingTask_ImplBase)
                .map(task -> (IndexingTask_ImplBase) task)
                .anyMatch(task -> aProject.equals(task.getProject()));
    }

    @Override
    public long determineNumOfQueryResults(User aUser, Project aProject, String aQuery,
            SourceDocument aDocument, AnnotationLayer aAnnotationLayer,
            AnnotationFeature aAnnotationFeature)
        throws IOException, ExecutionException
    {
        log.trace("Count results for query [{}] for user [{}] in project [{}]({})", aQuery,
                aUser.getUsername(), aProject.getName(), aProject.getId());

        try (PooledIndex pooledIndex = acquireIndex(aProject.getId())) {
            Index index = pooledIndex.get();

            ensureIndexIsCreatedAndValid(aProject, index);

            // Index is valid, try to execute the query
            return index.getPhysicalIndex().numberOfQueryResults(new SearchQueryRequest(aProject,
                    aUser, aQuery, aDocument, aAnnotationLayer, aAnnotationFeature, 0L, 0L));
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
            // Set the invalid flag
            aIndex.setInvalid(true);
            entityManager.merge(aIndex);

            // Schedule new re-indexing process
            enqueueReindexTask(aProject, "ensureIndexIsCreatedAndValid[doesNotExist]");

            // Throw execution exception so that the user knows the query was not run
            throw (new ExecutionException("Index still building. Try again later."));
        }

        // Is the index valid?
        if (aIndex.getInvalid()) {
            if (!isIndexInProgress(aProject)) {
                // Index is invalid, schedule a new index rebuild
                enqueueReindexTask(aProject, "ensureIndexIsCreatedAndValid[invalid]");
            }

            // Throw execution exception so that the user knows the query was not run
            throw (new ExecutionException("Index still building. Try again later."));
        }
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
            log.error("Unable to enqueue document [{}]({}) for indexing", aSourceDocument.getName(),
                    aSourceDocument.getId(), e);
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
            log.error("Unable to enqueue document [{}]({}) for indexing",
                    aAnnotationDocument.getName(), aAnnotationDocument.getId(), e);
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

        public PooledIndex(Index aDelegate)
        {
            delegate = aDelegate;
            refCount = new AtomicInteger(0);
            lastAccess = new AtomicLong(currentTimeMillis());
            forceRecycle = new AtomicBoolean(false);
            dead = new AtomicBoolean(false);
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
    }
}

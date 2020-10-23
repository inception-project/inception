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
package de.tudarmstadt.ukp.inception.search;

import static de.tudarmstadt.ukp.inception.search.SearchCasUtils.casToByteArray;
import static java.util.concurrent.TimeUnit.MINUTES;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;

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
import de.tudarmstadt.ukp.inception.search.config.SearchServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.search.index.PhysicalIndexFactory;
import de.tudarmstadt.ukp.inception.search.index.PhysicalIndexRegistry;
import de.tudarmstadt.ukp.inception.search.model.Index;
import de.tudarmstadt.ukp.inception.search.scheduling.IndexScheduler;

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

    private @Autowired DocumentService documentService;
    private @Autowired ProjectService projectService;
    private @Autowired PhysicalIndexRegistry physicalIndexRegistry;
    private @Autowired IndexScheduler indexScheduler;

    // In fact - the only factory we have at the moment...
    private final String DEFAULT_PHSYICAL_INDEX_FACTORY = "mtasDocumentIndexFactory";

    // The indexes for each project
    private LoadingCache<Long, Index> indexByProject;

    @Autowired
    public SearchServiceImpl()
    {
        indexByProject = Caffeine.newBuilder().expireAfterAccess(10, MINUTES).maximumSize(1_000)
                .removalListener(this::unloadIndex).build(key -> loadIndex(key));
    }

    @Override
    public void destroy()
    {
        // Invalidate all the cached/open indexes so they get closed
        if (indexByProject != null) {
            indexByProject.invalidateAll();
        }
    }

    /**
     * Unloads the index state and deactivates/closes the underlying physical index.
     */
    private void unloadIndex(Long aProjectId, Index aIndex, RemovalCause aCause)
    {
        if (aIndex.getPhysicalIndex() != null && aIndex.getPhysicalIndex().isOpen()) {
            log.trace("Unloading index for project [{}]({})", aIndex.getProject().getName(),
                    aIndex.getProject().getId());
            aIndex.getPhysicalIndex().close();
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
    private Index loadIndex(long aProjectId)
    {
        Project aProject = projectService.getProject(aProjectId);

        log.trace("Loading index for project [{}]({})", aProject.getName(), aProject.getId());

        // Check if an index state has already been stored in the database
        // Currently, we assume that a project can have only a single index
        String sql = "FROM " + Index.class.getName() + " WHERE project = :project";
        Index index = entityManager.createQuery(sql, Index.class).setParameter("project", aProject)
                .getResultStream().findFirst().orElse(null);

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
     * beforeProjectRemove event. Triggered before a project is removed
     * 
     * @param aEvent
     *            The BeforeProjectRemovedEvent event
     * @throws IOException
     */
    @EventListener
    public void beforeProjectRemove(BeforeProjectRemovedEvent aEvent) throws IOException
    {
        Project project = aEvent.getProject();

        log.trace("Removing index for project [{}]({}) because project is being removed",
                project.getName(), project.getId());

        // Retrieve index entry for the project
        Index index = indexByProject.get(project.getId());

        // If the index is null, then indexing is not supported.
        if (index == null) {
            return;
        }

        synchronized (index) {
            // Physical index exists, drop it
            if (index.getPhysicalIndex().isCreated()) {
                index.getPhysicalIndex().delete();
            }

            // Delete the index entry from the DB
            entityManager
                    .remove(entityManager.contains(index) ? index : entityManager.merge(index));

            // Remove the index entry from the memory map
            indexByProject.invalidate(project.getId());
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

        Index index = indexByProject.get(project.getId());

        // If the index is null, then indexing is not supported.
        if (index == null) {
            return;
        }

        synchronized (index) {
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
        indexScheduler.enqueueIndexDocument(aEvent.getDocument(), aEvent.getCas());

    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional
    public void afterAnnotationUpdate(AfterCasWrittenEvent aEvent)
    {
        log.trace("Starting afterAnnotationUpdate");

        // Schedule new document index process
        indexScheduler.enqueueIndexDocument(aEvent.getDocument(), aEvent.getCas());
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional
    public void beforeLayerConfigurationChanged(LayerConfigurationChangedEvent aEvent)
    {
        log.trace("Starting beforeLayerConfigurationChanged");

        Project project = aEvent.getProject();

        Index index = indexByProject.get(project.getId());

        // If the index is null, then indexing is not supported.
        if (index == null) {
            return;
        }

        synchronized (index) {
            index.setInvalid(true);
            entityManager.merge(index);
        }

        // Schedule re-indexing of the physical index
        indexScheduler.enqueueReindexTask(aEvent.getProject());
    }

    @Override
    public void indexDocument(SourceDocument aSourceDocument, byte[] aBinaryCas)
    {
        Project project = aSourceDocument.getProject();

        log.debug("Request to index source document [{}]({}) in project [{}]({})",
                aSourceDocument.getName(), aSourceDocument.getId(), project.getName(),
                project.getId());

        Index index = indexByProject.get(project.getId());
        
        // If the index is null, then indexing is not supported.
        if (index == null) {
            return;
        }

        synchronized (index) {
            // Index already initialized? If not, schedule full re-indexing job. This will also
            // index the given document, so we can stop here after scheduling the re-indexing.
            if (!index.getPhysicalIndex().isCreated()) {
                log.trace(
                        "Index in project [{}]({}) has not yet been initialized. Scheduling an asynchronous re-indexing.",
                        project.getName(), project.getId());
                index.setInvalid(true);
                entityManager.merge(index);
                indexScheduler.enqueueReindexTask(project);
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
    }

    @Override
    public void indexDocument(AnnotationDocument aAnnotationDocument, byte[] aBinaryCas)
    {
        Project project = aAnnotationDocument.getProject();

        log.debug("Request to index annotation document [{}]({}) in project [{}]({})",
                aAnnotationDocument.getName(), aAnnotationDocument.getId(), project.getName(),
                project.getId());

        Index index = indexByProject.get(project.getId());
        
        // If the index is null, then indexing is not supported.
        if (index == null) {
            return;
        }
        
        synchronized (index) {
            // Index already initialized? If not, schedule full re-indexing job. This will also
            // index the given document, so we can stop here after scheduling the re-indexing.
            if (!index.getPhysicalIndex().isCreated()) {
                log.trace(
                        "Index in project [{}]({}) has not yet been initialized. Scheduling an asynchronous re-indexing.",
                        project.getName(), project.getId());
                index.setInvalid(true);
                entityManager.merge(index);
                indexScheduler.enqueueReindexTask(project);
                return;
            }

            // FIXME: This can probably be pulled out of the synchronized block to allow multiple
            // threads to update the index concurrently. The underlying index code should hopefully
            // be thread-safe...
            try {
                // Add annotation document to the index again
                log.trace(
                        "Indexing new version of annotation document [{}]({}) in project [{}]({})",
                        aAnnotationDocument.getName(), aAnnotationDocument.getId(),
                        project.getName(), project.getId());
                index.getPhysicalIndex().indexDocument(aAnnotationDocument, aBinaryCas);
            }
            catch (IOException e) {
                log.error("Error indexing annotation document [{}]({}) in project [{}]({})",
                        aAnnotationDocument.getName(), aAnnotationDocument.getId(),
                        project.getName(), project.getId(), e);
            }
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

        Index index = indexByProject.get(aProject.getId());

        // If the index is null, then indexing is not supported.
        if (index == null) {
            return Collections.emptyMap();
        }

        ensureIndexIsCreatedAndValid(aProject, index);

        return index.getPhysicalIndex().executeQuery(new SearchQueryRequest(aProject, aUser, aQuery,
                aDocument, aAnnotationLayer, aAnnotationFeature, offset, count));
    }

    /**
     * Re-index the project. If there is no physical index, create a new one.
     */
    @Override
    @Transactional
    public void reindex(Project aProject) throws IOException
    {
        log.info("Re-indexing project [{}]({}) ", aProject.getName(), aProject.getId());

        Index index = indexByProject.get(aProject.getId());

        // If the index is null, then indexing is not supported.
        if (index == null) {
            return;
        }

        synchronized (index) {
            index.setInvalid(true);
            
            // Clear the index
            index.getPhysicalIndex().clear();
            
            // Index all the annotation documents
            for (User user : projectService.listProjectUsersWithPermissions(aProject)) {
                List<AnnotationDocument> annotationDocumentsForUser = documentService
                        .listAnnotationDocuments(aProject, user);
                for (AnnotationDocument doc : annotationDocumentsForUser) {
                    // Because serialization is a process which modifies internal data structures of
                    // the CAS, we need exclusive access the CAS for the time being.
                    // This can be relaxed after upgrading to UIMA 3.2.0 which includes a fix for
                    // for https://issues.apache.org/jira/browse/UIMA-6162
                    byte[] casAsByteArray;
                    try (CasStorageSession session = CasStorageSession.openNested()) {
                        casAsByteArray = casToByteArray(documentService.readAnnotationCas(doc));
                    }
                    indexDocument(doc, casAsByteArray);
                }
            }

            // Index all the source documents
            for (SourceDocument doc : documentService.listSourceDocuments(aProject)) {
                // Because serialization is a process which modifies internal data structures of
                // the CAS, we need exclusive access the CAS for the time being.
                // This can be relaxed after upgrading to UIMA 3.2.0 which includes a fix for
                // for https://issues.apache.org/jira/browse/UIMA-6162
                byte[] casAsByteArray;
                try (CasStorageSession session = CasStorageSession.openNested()) {
                    casAsByteArray = casToByteArray(documentService.createOrReadInitialCas(doc));
                }
                indexDocument(doc, casAsByteArray);
            }            
            
            // After re-indexing, reset the invalid flag
            index.setInvalid(false);
            entityManager.merge(index);
        }
    }

    @Override
    public boolean isIndexValid(Project aProject)
    {
        Index index = indexByProject.getIfPresent(aProject.getId());
        return index != null ? !index.getInvalid() : false;
    }

    @Override
    public boolean isIndexInProgress(Project aProject)
    {
        return indexScheduler.isIndexInProgress(aProject);
    }

    @Override
    public long determineNumOfQueryResults(User aUser, Project aProject, String aQuery,
            SourceDocument aDocument, AnnotationLayer aAnnotationLayer,
            AnnotationFeature aAnnotationFeature)
        throws IOException, ExecutionException
    {
        log.trace("Count results for query [{}] for user [{}] in project [{}]({})", aQuery,
                aUser.getUsername(), aProject.getName(), aProject.getId());

        Index index = indexByProject.get(aProject.getId());

        // If the index is null, then indexing is not supported.
        if (index == null) {
            return 0;
        }
        
        ensureIndexIsCreatedAndValid(aProject, index);

        // Index is valid, try to execute the query
        return index.getPhysicalIndex().numberOfQueryResults(new SearchQueryRequest(aProject, aUser,
                aQuery, aDocument, aAnnotationLayer, aAnnotationFeature, 0L, 0L));
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
            indexScheduler.enqueueReindexTask(aProject);

            // Throw execution exception so that the user knows the query was not run
            throw (new ExecutionException("Index still building. Try again later."));
        }

        // Is the index valid?
        if (aIndex.getInvalid()) {
            if (!indexScheduler.isIndexInProgress(aProject)) {
                // Index is invalid, schedule a new index rebuild
                indexScheduler.enqueueReindexTask(aProject);
            }

            // Throw execution exception so that the user knows the query was not run
            throw (new ExecutionException("Index still building. Try again later."));
        }
    }
}

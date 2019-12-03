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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
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
import de.tudarmstadt.ukp.inception.search.index.PhysicalIndex;
import de.tudarmstadt.ukp.inception.search.index.PhysicalIndexFactory;
import de.tudarmstadt.ukp.inception.search.index.PhysicalIndexRegistry;
import de.tudarmstadt.ukp.inception.search.model.Index;
import de.tudarmstadt.ukp.inception.search.scheduling.IndexScheduler;

@Component(SearchService.SERVICE_NAME)
@Transactional
public class SearchServiceImpl
    implements SearchService
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    @PersistenceContext
    private EntityManager entityManager;

    private @Autowired AnnotationSchemaService annotationSchemaService;
    private @Autowired DocumentService documentService;
    private @Autowired ProjectService projectService;
    private @Autowired PhysicalIndexRegistry physicalIndexRegistry;
    private @Autowired IndexScheduler indexScheduler;
    private @Autowired RepositoryProperties repositoryProperties;

    // Index factory
    private PhysicalIndexFactory physicalIndexFactory;
    private String physicalIndexFactoryName = "mtasDocumentIndexFactory";

    // The indexes for each project
    private static Map<Long, Index> indexes;

    @Autowired
    public SearchServiceImpl()
    {
        indexes = new HashMap<>();
    }

    /** 
     * Get an index entry from the memory map
     * @param aProject The project
     * @return The index
     */
    private Index getIndexFromMemory(Project aProject)
    {
        // Search index entry in the memory map
        if (!indexes.containsKey(aProject.getId())) {
            // Not found. Search index entry in the database
            log.trace("Index is not in memory for project [{}]. Retrieve it from DB.",
                    aProject.getName());

            Index index = getIndex(aProject);
            
            if (index == null) {
                // Not found in the DB, create new index instance and store it in DB
                log.trace("Index not found DB for project [{}]. Create it in the DB.",
                        aProject.getName());

                index = new Index();
                index.setInvalid(false);
                index.setProject(aProject);
                index.setPhysicalProvider(physicalIndexFactoryName);
                createIndex(index);
                updateIndex(index);
            }
            
            // Get physical index object
            physicalIndexFactory = physicalIndexRegistry.getIndexFactory(physicalIndexFactoryName);

            PhysicalIndex physicalIndex = physicalIndexFactory.getNewIndex(aProject,
                    annotationSchemaService, documentService, projectService,
                    repositoryProperties.getPath().getAbsolutePath());
            
            // Set physical index object
            index.setPhysicalIndex(physicalIndex);

            // Add index entry to the memory map
            indexes.put(aProject.getId(), index);
            
        }

        return indexes.get(aProject.getId());
    }

    /**
     * beforeProjectRemove event. Triggered before a project is removed
     * 
     * @param aEvent
     *            The BeforeProjectRemovedEvent event
     */
    @EventListener
    public void beforeProjectRemove(BeforeProjectRemovedEvent aEvent) throws IOException
    {
        log.trace("Starting beforeProjectRemove");

        Project project = aEvent.getProject();
        
        // Retrieve index entry for the project
        Index index = getIndexFromMemory(project);

        if (index.getPhysicalIndex().isCreated()) {
            // Physical index exists, drop it
            index.getPhysicalIndex().dropPhysicalIndex();
        }

        // Remove the index entry from the memory map
        indexes.remove(project.getId());
        
        // Delete the index entry from the DB
        deleteIndexByProject(project);
    }

    @EventListener
    public void beforeDocumentRemove(BeforeDocumentRemovedEvent aEvent) throws IOException
    {
        log.trace("Starting beforeDocumentRemove");

        SourceDocument document = aEvent.getDocument();

        Project project = document.getProject();
        
        // Retrieve index entry for the project
        Index index = getIndexFromMemory(project);

        if (index.getPhysicalIndex().isCreated()) {
            // Physical index exists.
            log.trace("Physical index already created. Proceed to remove document.");
            
            if (!index.getPhysicalIndex().isOpen()) {
                // Physical index is not open. Open it.
                log.trace("Physical index not open. Open it");

                index.getPhysicalIndex().openPhysicalIndex();
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

    private boolean canAddDocumentToIndex(Index aIndex)
    {
        if (!aIndex.getPhysicalIndex().isCreated()) {
            // Physical index does not exist. 
            log.trace("Physical index not created. Set it to invalid and enqueue reindex task.");
            
            // Set the invalid flag
            aIndex.setInvalid(true);
            updateIndex(aIndex);

            // Schedule new reindex process
            indexScheduler.enqueueReindexTask(aIndex.getProject());
            
            return false;
        } else {
            // Physical index already exists
            log.trace("Physical index already created: {}", aIndex.getPhysicalIndex());

            if (!aIndex.getPhysicalIndex().isOpen()) {
                // Physical index is not open. Open it.
                log.trace("Physical index not open. Open it");

                aIndex.getPhysicalIndex().openPhysicalIndex();
            }

            return true;
        }
    }
    
    @Override
    public void indexDocument(SourceDocument aSourceDocument, CAS aJCas)
    {
        // Retrieve index entry for the project
        Index index = getIndexFromMemory(aSourceDocument.getProject());

        if (!canAddDocumentToIndex(index)) {
            log.trace("Cannot add documents to the index at this time. Skipping.");
            return;
        }
            
        try {
            // Remove source document from index.
            log.trace("Remove source document from index");
            index.getPhysicalIndex().deindexDocument(aSourceDocument);

            // Add annotation document to the index again
            log.trace("Add source document to index");
            index.getPhysicalIndex().indexDocument(aSourceDocument, aJCas);
        }
        catch (IOException e) {
            log.error("Error indexing source document [{}]({}) in project [{}]({})",
                    aSourceDocument.getName(), aSourceDocument.getId(),
                    aSourceDocument.getProject().getName(), aSourceDocument.getProject().getId(),
                    e);
        }
    }


    @Override
    public void indexDocument(AnnotationDocument aAnnotationDocument, CAS aCas)
    {
        log.debug("Indexing annotation document [{}]({}) in project [{}]({})",
                aAnnotationDocument.getName(), aAnnotationDocument.getId(),
                aAnnotationDocument.getProject().getName(),
                aAnnotationDocument.getProject().getId());

        // Retrieve index entry for the project
        Index index = getIndexFromMemory(aAnnotationDocument.getProject());

        if (canAddDocumentToIndex(index)) {
            try {
                // Retrieve the timestamp for the current indexed annotation document
                Optional<String> timestamp = index.getPhysicalIndex()
                        .getTimestamp(aAnnotationDocument);
                
                // Add annotation document to the index again
                log.debug("Add to the index: annotation document [{}]({}) in project [{}]({})",
                        aAnnotationDocument.getName(), aAnnotationDocument.getId(),
                        aAnnotationDocument.getProject().getName(),
                        aAnnotationDocument.getProject().getId());
                index.getPhysicalIndex().indexDocument(aAnnotationDocument, aCas);
                
                // If there was a previous timestamped indexed annotation document, remove it from 
                // index
                if (timestamp.isPresent()) {
                    log.debug("Remove from the index previous annotation document [{}]({}) "
                            + "in project [{}]({}) based on last timestamp {}",
                            aAnnotationDocument.getName(), aAnnotationDocument.getId(),
                            aAnnotationDocument.getProject().getName(),
                            aAnnotationDocument.getProject().getId(), timestamp);
                    index.getPhysicalIndex().deindexDocument(aAnnotationDocument, timestamp.get());
                }

                log.debug("Finished indexing annotation document [{}]({}) in project [{}]({})",
                        aAnnotationDocument.getName(), aAnnotationDocument.getId(),
                        aAnnotationDocument.getProject().getName(),
                        aAnnotationDocument.getProject().getId());
            }
            catch (IOException e) {
                log.error("Error indexing annotation document [{}]({}) in project [{}]({})",
                        aAnnotationDocument.getName(), aAnnotationDocument.getId(),
                        aAnnotationDocument.getProject().getName(),
                        aAnnotationDocument.getProject().getId(), e);
            }
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional
    public void afterAnnotationUpdate(AfterCasWrittenEvent aEvent)
    {
        log.trace("Starting afterAnnotationUpdate");

        // Schedule new document index process
        indexScheduler.enqueueIndexDocument(aEvent.getDocument(), aEvent.getCas());
    }

    @Override
    @Transactional
    public List<SearchResult> query(User aUser, Project aProject, String aQuery)
        throws IOException, ExecutionException
    {
        return query(aUser, aProject, aQuery, null);
    }

    @Override
    @Transactional public List<SearchResult> query(User aUser, Project aProject, String aQuery,
        SourceDocument aDocument) throws IOException, ExecutionException
    {
        Map<String, List<SearchResult>> groupedResults = query(aUser, aProject, aQuery, aDocument,
            null, null);
        List<SearchResult> resultsAsList = new ArrayList<>();
        groupedResults.values().stream()
            .forEach(resultsGroup -> resultsAsList.addAll(resultsGroup));
        return resultsAsList;
    }

    @Override
    @Transactional
    public Map<String, List<SearchResult>> query(User aUser,
        Project aProject, String aQuery, SourceDocument aDocument, AnnotationLayer aAnnotationLayer,
        AnnotationFeature aAnnotationFeature) throws IOException, ExecutionException
    {
        log.debug("Starting query for user [{}] in project [{}]({})", aUser.getUsername(),
                aProject.getName(), aProject.getId());

        Map<String, List<SearchResult>> results = null;

        Index index = getIndexFromMemory(aProject);

        if (index.getInvalid()) {
            if (!indexScheduler.isIndexInProgress(aProject)) {
                // Index is invalid, schedule a new index rebuild
                indexScheduler.enqueueReindexTask(aProject);
            }

            // Throw execution exception so that the user knows the query was not run
            throw (new ExecutionException("Query not executed because index is in invalid state. Try again later."));
        }
        else {
            // Index is valid, try to execute the query

            if (!index.getPhysicalIndex().isCreated()) {
                // Physical index does not exist.

                // Set the invalid flag
                index.setInvalid(true);
                updateIndex(index);

                // Schedule new reindexing process
                indexScheduler.enqueueReindexTask(aProject);

                // Throw execution exception so that the user knows the query was not run
                throw (new ExecutionException("Query not executed because index is in invalid state. Try again later."));
            }
            else {
                // Physical index exists

                if (!index.getPhysicalIndex().isOpen()) {
                    // Physical index is not open. Open it.
                    index.getPhysicalIndex().openPhysicalIndex();
                }

                log.debug("Running query: [{}]", aQuery);

                results = index.getPhysicalIndex().executeQuery(
                    new SearchQueryRequest(aProject, aUser, aQuery, aDocument,
                        aAnnotationLayer, aAnnotationFeature));
            }

        }
        return results;
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
    public void beforeLayerConfigurationChanged(LayerConfigurationChangedEvent aEvent)
    {
        log.trace("Starting beforeLayerConfigurationChanged");

        Project project = aEvent.getProject();

        Index index = getIndexFromMemory(project);

        // Set the invalid flag
        index.setInvalid(true);
        updateIndex(index);

        // Schedule reindexing of the physical index
        indexScheduler.enqueueReindexTask(aEvent.getProject());
    }

    /** 
     * Reindex the project. If there is not a physical index, create a new one.
     */
    @Override
    @Transactional
    public void reindex(Project aProject) throws IOException
    {
        log.info("Re-indexing project [{}]({}) ", aProject.getName(), aProject.getId());
        
        Index index = getIndexFromMemory(aProject);

        if (index.getPhysicalIndex().isCreated()) {
            // Physical index already exists, drop it
            log.debug("Physical index already exists. Drop it.");

            index.getPhysicalIndex().dropPhysicalIndex();
        }

        // Create physical index and index all project documents
        log.debug("Create new physical index.");
        index.getPhysicalIndex().createPhysicalIndex();
        
        // After reindexing, reset the invalid flag
        log.trace("Set index invalid flag to false.");

        index.setInvalid(false);
        updateIndex(index);
    }
    
    @Override
    public Index getIndex(Project aProject)
    {
        Index indexObject = null;
        String sql = "FROM " + Index.class.getName() + " i where i.project= :project";
        try {
            indexObject = entityManager.createQuery(sql, Index.class)
                    .setParameter("project", aProject).getSingleResult();
        }
        catch (NoResultException e) {
            indexObject = null;
        }
        return indexObject;
    }

    private void createIndex(Index aIndexObject)
    {
        entityManager.persist(aIndexObject);
    }

    private void updateIndex(Index aIndexObject)
    {
        entityManager.merge(aIndexObject);
    }

    public void deleteIndex(Index aIndexObject)
    {
        entityManager.remove(entityManager.contains(aIndexObject) ? aIndexObject
                : entityManager.merge(aIndexObject));
    }

    public void deleteIndexByProject(Project aProject)
    {
        Index indexObject = this.getIndex(aProject);

        if (indexObject != null) {
            this.deleteIndex(indexObject);
        }
    }
    
    @Override
    public boolean isIndexValid(Project aProject)
    {
        if (indexes.containsKey(aProject.getId())) {
            return !indexes.get(aProject.getId()).getInvalid();
        }
        else {
            return false;
        }
    }
    
    @Override
    public boolean isIndexInProgress(Project aProject)
    {
        return indexScheduler.isIndexInProgress(aProject);
    }
}

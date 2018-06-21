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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterAnnotationUpdateEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.AfterDocumentCreatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.BeforeDocumentRemovedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.BeforeProjectRemovedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.event.LayerConfigurationChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.search.index.Index;
import de.tudarmstadt.ukp.inception.search.index.IndexFactory;
import de.tudarmstadt.ukp.inception.search.index.IndexRegistry;

@Component(SearchService.SERVICE_NAME)
public class SearchServiceImpl
    implements SearchService
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private @Autowired AnnotationSchemaService annotationSchemaService;
    private @Autowired DocumentService documentService;
    private @Autowired ProjectService projectService;
    private @Autowired IndexRegistry indexRegistry;

    // Index factory
    private IndexFactory indexFactory;
    private String indexFactoryName = "mtasDocumentIndexFactory";

    // The indexes for each project
    private static Map<Long, Index> indexes;

    @Value(value = "${repository.path}")
    private String dir;

    @Autowired
    public SearchServiceImpl()
    {
        indexes = new HashMap<>();
    }

    public SearchServiceImpl(@Value("${data.path}") File aDir)
    {
        dir = aDir.getAbsolutePath();
        indexes = new HashMap<>();
    }

    private Index getIndexByProject(Project aProject)
    {
        if (!indexes.containsKey(aProject.getId())) {
            indexFactory = indexRegistry.getIndexFactory(indexFactoryName);

            indexes.put(aProject.getId(), indexFactory.getNewIndex(aProject,
                    annotationSchemaService, documentService, projectService, dir));
        }

        return indexes.get(aProject.getId());
    }

    @EventListener
    public void beforeProjectRemove(BeforeProjectRemovedEvent aEvent) throws Exception
    {
        Index index = getIndexByProject(aEvent.getProject());

        if (index.isIndexCreated()) {
            // Index exists, drop it
            index.dropIndex();
        }

        indexes.remove(aEvent.getProject().getId());
    }

    @EventListener
    public void beforeDocumentRemove(BeforeDocumentRemovedEvent aEvent) throws Exception
    {
        SourceDocument document = aEvent.getDocument();

        Index index = getIndexByProject(document.getProject());

        if (index.isIndexCreated()) {
            // Index exists.

            if (!index.isIndexOpen()) {
                // Index is not open. Open it.
                index.openIndex();
            }

            // Remove source document from the index
            index.deindexDocument(document);

            // Remove related annotation documents from the index
            for (AnnotationDocument annotationDocument : documentService
                    .listAnnotationDocuments(document)) {
                index.deindexDocument(annotationDocument);
            }
        }
    }

    @EventListener
    public void afterAnnotationUpdate(AfterAnnotationUpdateEvent aEvent) throws Exception
    {
        AnnotationDocument document = aEvent.getDocument();

        Index index = getIndexByProject(document.getProject());

        if (!index.isIndexCreated()) {
            // Index does not exist. Create it. The creation will already index this document.
            index.createIndex();
        }
        else {
            // Index exists

            if (!index.isIndexOpen()) {
                // Index is not open. Open it.
                index.openIndex();
            }

            // Remove annotation document from index.
            index.deindexDocument(document);

            // Add document to the index again
            index.indexDocument(document, aEvent.getJCas());
        }
    }

    @Override
    public List<SearchResult> query(User aUser, Project aProject, String aQuery)
        throws IOException, ExecutionException
    {
        Index index = getIndexByProject(aProject);

        if (!index.isIndexCreated()) {
            // Index does not exist. Create it.
            index.createIndex();
        }
        else {
            // Index exists

            if (!index.isIndexOpen()) {
                // Index is not open. Open it.
                index.openIndex();
            }
        }

        log.debug("Running query: {}", aQuery);
        
        List<SearchResult> results = index.executeQuery(aUser, aQuery, null, null);

        for (SearchResult result : results) {
            String title = result.getDocumentTitle();
        }

        return results;
    }

    @EventListener
    public void afterDocumentCreate(AfterDocumentCreatedEvent aEvent) throws Exception
    {
        SourceDocument document = aEvent.getDocument();

        Index index = getIndexByProject(document.getProject());

        if (!index.isIndexCreated()) {
            // Index does not exist. Create it. It will also index the new document
            index.createIndex();
        }
        else {
            // Index exists

            if (!index.isIndexOpen()) {
                // Index is not open. Open it.
                index.openIndex();
            }

            // Index the new document
            index.indexDocument(document, aEvent.getJcas());

        }

    }

    @EventListener
    public void beforeLayerConfigurationChanged(LayerConfigurationChangedEvent aEvent)
        throws Exception
    {
        Index index = getIndexByProject(aEvent.getProject());

        if (index.isIndexCreated()) {
            // Index already exists, drop it
            index.dropIndex();
        }
        // Recreate index and indexes all project documents
        index.createIndex();
    }

    @Override
    public void reindex(Project aProject) throws IOException
    {
        log.info("Reindexing project " + aProject.getName());
        Index index = getIndexByProject(aProject);

        if (index.isIndexCreated()) {
            // Index already exists, drop it
            index.dropIndex();
        }

        // Recreate index and indexes all project documents
        index.createIndex();
    }
}

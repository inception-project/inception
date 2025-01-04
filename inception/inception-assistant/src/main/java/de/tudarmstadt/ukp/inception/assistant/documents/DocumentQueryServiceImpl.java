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
package de.tudarmstadt.ukp.inception.assistant.documents;

import static java.util.Collections.emptyList;
import static org.apache.lucene.util.VectorUtil.l2normalize;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.KnnFloatVectorQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.assistant.config.AssistantDocumentIndexProperties;
import de.tudarmstadt.ukp.inception.assistant.embedding.EmbeddingService;
import de.tudarmstadt.ukp.inception.assistant.index.LuceneIndexPool;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.documents.event.AfterDocumentCreatedEvent;
import de.tudarmstadt.ukp.inception.documents.event.BeforeDocumentRemovedEvent;
import de.tudarmstadt.ukp.inception.project.api.event.AfterProjectRemovedEvent;
import de.tudarmstadt.ukp.inception.project.api.event.BeforeProjectRemovedEvent;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;

public class DocumentQueryServiceImpl
    implements DocumentQueryService
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final RepositoryProperties repositoryProperties;
    private final SchedulingService schedulingService;
    private final EmbeddingService embeddingService;
    private final DocumentService documentService;

    private final Set<Project> projectsPendingDeletion = new HashSet<>();

    private final LuceneIndexPool indexPool;

    public DocumentQueryServiceImpl(RepositoryProperties aRepositoryProperties,
            AssistantDocumentIndexProperties aIndexProperties, SchedulingService aSchedulingService,
            EmbeddingService aEmbeddingService, DocumentService aDocumentService)
    {
        repositoryProperties = aRepositoryProperties;
        schedulingService = aSchedulingService;
        embeddingService = aEmbeddingService;
        documentService = aDocumentService;

        indexPool = LuceneIndexPool.builder() //
                .withName("assistant/index") //
                .withBorrowWaitTimeout(aIndexProperties.getBorrowWaitTimeout()) //
                .withEmbeddingService(aEmbeddingService) //
                .withEvictionDelay(aIndexProperties.getIdleEvictionDelay()) //
                .withMinIdleTime(aIndexProperties.getMinIdleTime()) //
                .withRepositoryPath(repositoryProperties.getPath().toPath()) //
                .build();
    }

    LuceneIndexPool.PooledIndex borrowIndex(Project aProject) throws Exception
    {
        return indexPool.borrowIndex(aProject);
    }

    @Override
    public List<Chunk> query(Project aProject, String aQuery, int aTopN, double aScoreThreshold)
    {
        try (var index = borrowIndex(aProject)) {
            try (var reader = DirectoryReader.open(index.getIndexWriter())) {
                if (reader.numDocs() == 0) {
                    schedulingService.enqueue(UpdateDocumentIndexTask.builder() //
                            .withTrigger("Query on empty index") //
                            .withProject(aProject) //
                            .build());
                    return emptyList();
                }

                LOG.trace("KNN Query: [{}]", aQuery);

                var maybeEmbedding = embeddingService.embed(aQuery);
                if (!maybeEmbedding.isPresent()) {
                    return emptyList();
                }

                var queryEmbedding = l2normalize(maybeEmbedding.get(), false);

                var searcher = new IndexSearcher(reader);
                var query = new KnnFloatVectorQuery(FIELD_EMBEDDING, queryEmbedding, aTopN);
                var result = searcher.search(query, aTopN);

                var documentNameCache = new HashMap<Long, String>();
                var chunks = new ArrayList<Chunk>();
                for (var scoreDoc : result.scoreDocs) {
                    if (scoreDoc.score >= aScoreThreshold) {
                        var doc = reader.storedFields().document(scoreDoc.doc);
                        var docId = doc.getField(FIELD_SOURCE_DOC_ID).numericValue().longValue();
                        var docName = getDocumentName(aProject, documentNameCache, docId);
                        chunks.add(Chunk.builder() //
                                .withDocumentId(docId) //
                                .withDocumentName(docName) //
                                .withSection(doc.get(FIELD_SECTION)) //
                                .withText(doc.get(FIELD_TEXT)) //
                                .withBegin(doc.getField(FIELD_BEGIN).numericValue().intValue()) //
                                .withEnd(doc.getField(FIELD_END).numericValue().intValue()) //
                                .withScore(scoreDoc.score) //
                                .build());
                        LOG.trace("Score {} above threshold: [{}]", scoreDoc.score,
                                doc.get(FIELD_TEXT));
                    }
                    else if (LOG.isTraceEnabled()) {
                        var doc = reader.storedFields().document(scoreDoc.doc);
                        LOG.trace("Score {} too low: [{}]", scoreDoc.score, doc.get(FIELD_TEXT));
                    }
                }
                return chunks;
            }
        }
        catch (Exception e) {
            LOG.error("Error querying document index for project {}", aProject, e);
            return emptyList();
        }
    }

    private String getDocumentName(Project aProject, HashMap<Long, String> documentNameCache,
            long docId)
    {
        try {
            return documentNameCache.computeIfAbsent(docId,
                    id -> documentService.getSourceDocument(aProject.getId(), id).getName());
        }
        catch (Exception e) {
            return "";
        }
    }

    @Override
    public void rebuildIndexAsync(Project aProject)
    {
        try {
            indexPool.clearIndex(aProject);

            schedulingService.enqueue(UpdateDocumentIndexTask.builder() //
                    .withTrigger("rebuildIndexAsync") //
                    .withProject(aProject) //
                    .build());
        }
        catch (Exception e) {
            LOG.error("Error clearing document index for project {}", aProject, e);
        }
    }

    @EventListener
    public void onAfterDocumentCreated(AfterDocumentCreatedEvent aEvent)
    {
        if (projectsPendingDeletion.contains(aEvent.getDocument().getProject())) {
            return;
        }

        schedulingService.enqueue(UpdateDocumentIndexTask.builder() //
                .withTrigger("AfterDocumentCreatedEvent") //
                .withProject(aEvent.getDocument().getProject()) //
                .build());
    }

    @EventListener
    public void onBeforeDocumentRemoved(BeforeDocumentRemovedEvent aEvent) throws IOException
    {
        if (projectsPendingDeletion.contains(aEvent.getDocument().getProject())) {
            return;
        }

        schedulingService.enqueue(UpdateDocumentIndexTask.builder() //
                .withTrigger("BeforeDocumentRemovedEvent") //
                .withProject(aEvent.getDocument().getProject()) //
                .build());
    }

    @EventListener
    public void onBeforeProjectRemoved(BeforeProjectRemovedEvent aEvent)
    {
        projectsPendingDeletion.add(aEvent.getProject());
    }

    @EventListener
    public void onAfterProjectRemovedEvent(AfterProjectRemovedEvent aEvent)
    {
        projectsPendingDeletion.remove(aEvent.getProject());
    }
}

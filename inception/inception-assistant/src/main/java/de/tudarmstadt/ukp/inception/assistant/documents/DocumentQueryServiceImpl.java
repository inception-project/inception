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

import static de.tudarmstadt.ukp.inception.project.api.ProjectService.PROJECT_FOLDER;
import static java.util.Collections.emptyList;
import static org.apache.lucene.util.VectorUtil.l2normalize;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.KnnFloatVectorQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.assistant.config.AssistantDocumentIndexProperties;
import de.tudarmstadt.ukp.inception.assistant.embedding.EmbeddingService;
import de.tudarmstadt.ukp.inception.assistant.index.HighDimensionLucene99Codec;
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
    private final AssistantDocumentIndexProperties indexProperties;
    private final SchedulingService schedulingService;
    private final EmbeddingService embeddingService;
    private final DocumentService documentService;

    private static final String INDEX_FOLDER = "index";
    private static final String ASSISTANT_FOLDER = "assistant";

    private final GenericKeyedObjectPool<Long, PooledIndex> indexPool;
    private final Set<Project> projectsPendingDeletion = new HashSet<>();

    public DocumentQueryServiceImpl(
            RepositoryProperties aRepositoryProperties,
            AssistantDocumentIndexProperties aIndexProperties, SchedulingService aSchedulingService,
            EmbeddingService aEmbeddingService, DocumentService aDocumentService)
    {
        repositoryProperties = aRepositoryProperties;
        indexProperties = aIndexProperties;
        schedulingService = aSchedulingService;
        embeddingService = aEmbeddingService;
        documentService = aDocumentService;

        var indexPoolConfig = new GenericKeyedObjectPoolConfig<PooledIndex>();
        // We only ever want one pooled index per project
        indexPoolConfig.setMaxTotalPerKey(1);
        indexPoolConfig.setMaxIdlePerKey(1);
        // We do not want the pooled index to hang around forever. It can be closed when unused.
        indexPoolConfig.setMinIdlePerKey(0);
        // Run an evictor thread periodically
        indexPoolConfig.setTimeBetweenEvictionRuns(indexProperties.getIdleEvictionDelay());
        // Allow the evictor to drop idle CASes from pool after a short time. This should avoid that
        // CASes that are used regularly are dropped from the pool too quickly.
        indexPoolConfig.setMinEvictableIdleDuration(indexProperties.getMinIdleTime());
        // Allow the evictor to drop all idle CASes on every eviction run
        indexPoolConfig.setNumTestsPerEvictionRun(-1);
        // Allow viewing the pool in JMX
        indexPoolConfig.setJmxEnabled(true);
        indexPoolConfig.setJmxNameBase(getClass().getPackage().getName() + ":type="
                + getClass().getSimpleName() + ",name=");
        indexPoolConfig.setJmxNamePrefix("exclusiveCasAccessPool");
        // Max. time we wait for a CAS to become available before giving up with an error
        indexPoolConfig.setMaxWait(indexProperties.getBorrowWaitTimeout());
        indexPool = new GenericKeyedObjectPool<>(new PooledIndexWriterFactory(), indexPoolConfig);
    }

    @Override
    public PooledIndex borrowIndex(Project aProject) throws Exception
    {
        return indexPool.borrowObject(aProject.getId());
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
                                .withDocumentName(docName) //
                                .withSection(doc.get(FIELD_SECTION))
                                .withText(doc.get(FIELD_TEXT)) //
                                .withScore(scoreDoc.score) //
                                .build());
                        LOG.trace("Score {} above threshold: [{}]", scoreDoc.score, doc.get(FIELD_TEXT));
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
            LOG.error("Unable to query index", e);
            return emptyList();
        }
    }

    private String getDocumentName(Project aProject, HashMap<Long, String> documentNameCache, long docId)
    {
        try {
            return documentNameCache.computeIfAbsent(docId, id -> documentService
                    .getSourceDocument(aProject.getId(), id).getName());
        }
        catch (Exception e) {
            return "";
        }
    }

    private Path getIndexDirectory(Long aProjectId)
    {
        return repositoryProperties.getPath().toPath() //
                .toAbsolutePath() //
                .resolve(PROJECT_FOLDER) //
                .resolve(Long.toString(aProjectId)) //
                .resolve(ASSISTANT_FOLDER) //
                .resolve(INDEX_FOLDER);
    }

    @Override
    public void rebuildIndexAsync(Project aProject)
    {
        try (var index = borrowIndex(aProject)) {
            index.indexWriter.deleteAll();
            index.indexWriter.commit();

            schedulingService.enqueue(UpdateDocumentIndexTask.builder() //
                    .withTrigger("rebuildIndexAsync") //
                    .withProject(aProject) //
                    .build());
        }
        catch (Exception e) {
            LOG.error("Error clearing index", e);
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

    public class PooledIndex
        implements AutoCloseable
    {
        final long projectId;
        final Directory directory;
        final IndexWriter indexWriter;

        PooledIndex(long aProjectId, Directory aDirectory, IndexWriter aIndexWriter)
        {
            projectId = aProjectId;
            directory = aDirectory;
            indexWriter = aIndexWriter;
        }

        long getProjectId()
        {
            return projectId;
        }

        Directory getDirectory()
        {
            return directory;
        }

        IndexWriter getIndexWriter()
        {
            return indexWriter;
        }

        @Override
        public void close() throws Exception
        {
            indexPool.returnObject(projectId, this);
        }

    }

    class PooledIndexWriterFactory
        extends BaseKeyedPooledObjectFactory<Long, PooledIndex>
    {
        @Override
        public PooledIndex create(Long aKey) throws Exception
        {
            var dir = new MMapDirectory(getIndexDirectory(aKey));
            var iwc = new IndexWriterConfig();
            iwc.setCodec(new HighDimensionLucene99Codec(embeddingService.getDimension()));
            return new PooledIndex(aKey, dir, new IndexWriter(dir, iwc));
        }

        @SuppressWarnings("resource")
        @Override
        public void destroyObject(Long aProjectId, PooledObject<PooledIndex> aPooledIndex)
            throws Exception
        {
            var index = aPooledIndex.getObject();
            try {
                index.getIndexWriter().close();
            }
            catch (IOException e) {
                LOG.error("Error closing assistant index writer for project [{}]", aProjectId);
            }

            try {
                index.getDirectory().close();
            }
            catch (IOException e) {
                LOG.error("Error closing assistant index directory for project [{}]", aProjectId);
            }
        }

        @Override
        public PooledObject<PooledIndex> wrap(PooledIndex aWriter)
        {
            return new DefaultPooledObject<>(aWriter);
        }
    }
}

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

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.inception.assistant.documents.DocumentQueryService.FIELD_EMBEDDING;
import static de.tudarmstadt.ukp.inception.assistant.documents.DocumentQueryService.FIELD_SECTION;
import static de.tudarmstadt.ukp.inception.assistant.documents.DocumentQueryService.FIELD_SOURCE_DOC_COMPLETE;
import static de.tudarmstadt.ukp.inception.assistant.documents.DocumentQueryService.FIELD_SOURCE_DOC_ID;
import static de.tudarmstadt.ukp.inception.assistant.documents.DocumentQueryService.FIELD_TEXT;
import static de.tudarmstadt.ukp.inception.scheduling.TaskState.CANCELLED;
import static de.tudarmstadt.ukp.inception.scheduling.TaskState.COMPLETED;
import static de.tudarmstadt.ukp.inception.scheduling.TaskState.RUNNING;
import static java.lang.Math.floorDiv;
import static java.time.Duration.ofSeconds;
import static org.apache.commons.collections4.ListUtils.partition;
import static org.apache.commons.lang3.StringUtils.abbreviateMiddle;
import static org.apache.lucene.index.VectorSimilarityFunction.DOT_PRODUCT;
import static org.apache.lucene.util.VectorUtil.l2normalize;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.KnnFloatVectorField;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.FieldExistsQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.knuddels.jtokkit.api.EncodingRegistry;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageSession;
import de.tudarmstadt.ukp.inception.assistant.config.AssistantProperties;
import de.tudarmstadt.ukp.inception.assistant.documents.DocumentQueryServiceImpl.PooledIndex;
import de.tudarmstadt.ukp.inception.assistant.embedding.EmbeddingService;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.scheduling.DebouncingTask;
import de.tudarmstadt.ukp.inception.scheduling.ProjectTask;
import de.tudarmstadt.ukp.inception.scheduling.TaskScope;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class UpdateDocumentIndexTask
    extends DebouncingTask
    implements ProjectTask
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String TYPE = "UpdateDocumentIndexTask";

    private @Autowired DocumentService documentService;
    private @Autowired DocumentQueryServiceImpl documentQueryService;
    private @Autowired EmbeddingService embeddingService;
    private @Autowired AssistantProperties properties;
    private @Autowired EncodingRegistry encodingRegistry;

    public UpdateDocumentIndexTask(Builder<? extends Builder<?>> aBuilder)
    {
        super(aBuilder.withType(TYPE).withScope(TaskScope.PROJECT).withCancellable(false));
    }

    @Override
    public String getTitle()
    {
        return "Updating assistant index...";
    }

    @Override
    public void execute() throws Exception
    {
        var documents = documentService.listSourceDocuments(getProject());
        if (documents.isEmpty()) {
            return;
        }

        var chunker = new CasChunker(encodingRegistry, properties);

        var monitor = getMonitor();
        try (var index = documentQueryService.borrowIndex(getProject())) {
            try (var reader = DirectoryReader.open(index.getIndexWriter())) {
                var searcher = new IndexSearcher(reader);
                var query = new FieldExistsQuery(FIELD_SOURCE_DOC_COMPLETE);
                var result = searcher.search(query, Integer.MAX_VALUE);

                var documentsToIndex = new HashMap<Long, SourceDocument>();
                for (var doc : documents) {
                    documentsToIndex.put(doc.getId(), doc);
                }
                var documentsToUnindex = new ArrayList<Long>();

                for (var doc : result.scoreDocs) {
                    var fields = searcher.getIndexReader().storedFields().document(doc.doc);
                    var sourceDocId = fields.getField(FIELD_SOURCE_DOC_COMPLETE).numericValue()
                            .longValue();
                    var sourceDoc = documentsToIndex.remove(sourceDocId);
                    if (sourceDoc == null) {
                        documentsToUnindex.add(sourceDocId);
                    }
                }

                var toProcess = documentsToUnindex.size() + documentsToIndex.size() * 100;
                var processed = 0;
                monitor.setStateAndProgress(RUNNING, processed * 100, toProcess);

                for (var sourceDocumentId : documentsToUnindex) {
                    if (monitor.isCancelled()) {
                        monitor.setState(CANCELLED);
                        break;
                    }

                    monitor.setProgressWithMessage(processed * 100, toProcess,
                            LogMessage.info(this, "Unindexing..."));
                    unindexDocument(index, sourceDocumentId);
                    processed++;
                }

                try (var session = CasStorageSession.openNested()) {
                    for (var sourceDocument : documentsToIndex.values()) {
                        if (monitor.isCancelled()) {
                            monitor.setState(CANCELLED);
                            break;
                        }

                        monitor.setProgressWithMessage(processed * 100, toProcess,
                                LogMessage.info(this, "Indexing: %s", sourceDocument.getName()));
                        indexDocument(index, chunker, sourceDocument);
                        processed++;
                    }
                }

                if (!monitor.isCancelled()) {
                    monitor.setStateAndProgress(COMPLETED, processed * 100, toProcess);
                    index.getIndexWriter().commit();
                    // We can probably live with a partial index, so we do not roll back if
                    // cancelled
                }
            }
        }
        catch (Exception e) {
            LOG.error("Error updating assistant index", e);
        }
    }

    private void unindexDocument(PooledIndex aIndex, long aSourceDocumentId)
    {
        try {
            aIndex.getIndexWriter().deleteDocuments(
                    LongPoint.newExactQuery(FIELD_SOURCE_DOC_ID, aSourceDocumentId));
        }
        catch (IOException e) {
            LOG.error("Error unindexing document [{}]", aSourceDocumentId, e);
        }
    }

    private void indexDocument(PooledIndex aIndex, Chunker<CAS> aChunker,
            SourceDocument aSourceDocument)
    {
        try {
            var cas = documentService.createOrReadInitialCas(aSourceDocument, AUTO_CAS_UPGRADE,
                    SHARED_READ_ONLY_ACCESS);

            var chunks = aChunker.process(cas);
            var batches = partition(chunks, properties.getEmbedding().getBatchSize());

            var totalBatches = batches.size();
            var processedBatches = 0;
            var chunksSeen = 0;
            var progressOffset = getMonitor().getProgress();
            for (var batch : batches) {
                try {
                    var docEmbeddings = embeddingService
                            .embed(batch.stream().map(Chunk::text).toArray(String[]::new));
                    for (var embedding : docEmbeddings) {
                        indexChunks(aIndex, aSourceDocument, embedding);
                    }
                }
                catch (IOException e) {
                    LOG.error("Error indexing document {} chunks {}-{} ", aSourceDocument,
                            chunksSeen, chunksSeen + batch.size(), e);
                }

                getMonitor().setStateAndProgress(RUNNING,
                        progressOffset + floorDiv(processedBatches * 100, totalBatches));

                chunksSeen += batch.size();
                processedBatches++;
            }

            markDocumentAsIndexed(aIndex, aSourceDocument);
        }
        catch (IOException e) {
            LOG.error("Error indexing document", aSourceDocument, e);
        }
    }

    private void markDocumentAsIndexed(PooledIndex aIndex, SourceDocument aSourceDocument)
        throws IOException
    {
        var doc = new Document();
        doc.add(new StoredField(FIELD_SOURCE_DOC_COMPLETE, aSourceDocument.getId()));
        aIndex.getIndexWriter().addDocument(doc);
    }

    private void indexChunks(PooledIndex aIndex, SourceDocument aSourceDocument,
            Pair<String, float[]> aEmbeddedChunks)
        throws IOException
    {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Indexing chunk: [{}]",
                    abbreviateMiddle(aEmbeddedChunks.getKey().replaceAll("\\s+", " "), " … ", 60));
        }

        var doc = new Document();
        var normalizedEmbedding = l2normalize(aEmbeddedChunks.getValue(), false);
        doc.add(new KnnFloatVectorField(FIELD_EMBEDDING, normalizedEmbedding, DOT_PRODUCT));
        doc.add(new StoredField(FIELD_SOURCE_DOC_ID, aSourceDocument.getId()));
        doc.add(new StoredField(FIELD_SECTION, ""));
        doc.add(new StoredField(FIELD_TEXT, aEmbeddedChunks.getKey()));
        aIndex.getIndexWriter().addDocument(doc);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var task = (UpdateDocumentIndexTask) o;
        return getProject().equals(task.getProject());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getProject());
    }

    public static Builder<Builder<?>> builder()
    {
        return new Builder<>();
    }

    public static class Builder<T extends Builder<?>>
        extends DebouncingTask.Builder<T>
    {
        protected Builder()
        {
            withDebounceDelay(ofSeconds(3));
        }

        public UpdateDocumentIndexTask build()
        {
            return new UpdateDocumentIndexTask(this);
        }
    }
}

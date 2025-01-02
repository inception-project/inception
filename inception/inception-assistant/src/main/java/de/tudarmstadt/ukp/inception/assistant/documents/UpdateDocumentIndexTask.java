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
import static de.tudarmstadt.ukp.inception.assistant.documents.DocumentQueryService.FIELD_SOURCE_DOC;
import static de.tudarmstadt.ukp.inception.assistant.documents.DocumentQueryService.FIELD_SOURCE_DOC_COMPLETE;
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
import java.util.List;
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
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
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
                        indexDocument(index, sourceDocument);
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
            aIndex.getIndexWriter()
                    .deleteDocuments(LongPoint.newExactQuery(FIELD_SOURCE_DOC, aSourceDocumentId));
        }
        catch (IOException e) {
            LOG.error("Error unindexing document [{}]", aSourceDocumentId, e);
        }
    }

    private void indexDocument(PooledIndex aIndex, SourceDocument aSourceDocument)
    {
        try {
            var cas = documentService.createOrReadInitialCas(aSourceDocument, AUTO_CAS_UPGRADE,
                    SHARED_READ_ONLY_ACCESS);

            var chunks = chunk(cas);
            var batches = partition(chunks, properties.getEmbedding().getBatchSize());

            var totalBatches = batches.size();
            var processedBatches = 0;
            var sentencesSeen = 0;
            var progressOffset = getMonitor().getProgress();
            for (var batch : batches) {
                try {
                    var docEmbeddings = embeddingService.embed(batch.toArray(String[]::new));
                    for (var embedding : docEmbeddings) {
                        addToIndex(aIndex, embedding);
                    }
                }
                catch (IOException e) {
                    LOG.error("Error indexing document {} chunks {}-{} ", aSourceDocument,
                            sentencesSeen, sentencesSeen + batch.size(), e);
                }

                getMonitor().setStateAndProgress(RUNNING,
                        progressOffset + floorDiv(processedBatches * 100, totalBatches));

                sentencesSeen += batch.size();
                processedBatches++;
            }
        }
        catch (IOException e) {
            LOG.error("Error indexing document", aSourceDocument, e);
        }
    }

    private List<String> chunk(CAS cas)
    {
        var encoding = encodingRegistry.getEncoding(properties.getChat().getEncoding())
                .orElseThrow(() -> new IllegalStateException(
                        "Unknown encoding: " + properties.getChat().getEncoding()));
        var limit = floorDiv(properties.getEmbedding().getChunkSize() * 90, 100);

        var unitIterator = cas.select(Sentence.class) //
                .map(Sentence::getCoveredText) //
                .toList().iterator();

        var chunks = new ArrayList<String>();

        var currentChunk = new StringBuilder();
        var chunkTokens = 0;
        while (unitIterator.hasNext()) {
            var unit = unitIterator.next();
            var unitTokens = encoding.countTokensOrdinary(unit);

            if (chunkTokens + unitTokens > limit) {
                chunks.add(currentChunk.toString());
                currentChunk.setLength(0);
                chunkTokens = 0;
            }
            currentChunk.append(unit);
            currentChunk.append("\n");
            chunkTokens += unitTokens;
        }

        // Add the final chunk (unless empty)
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }

        return chunks;
    }

    private void addToIndex(PooledIndex aIndex, Pair<String, float[]> embedding) throws IOException
    {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Indexing chunk: [{}]",
                    abbreviateMiddle(embedding.getKey().replaceAll("\\s+", " "), " … ", 60));
        }

        var doc = new Document();
        var normalizedEmbedding = l2normalize(embedding.getValue(), false);
        doc.add(new KnnFloatVectorField(FIELD_EMBEDDING, normalizedEmbedding, DOT_PRODUCT));
        doc.add(new StoredField(FIELD_TEXT, embedding.getKey()));
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

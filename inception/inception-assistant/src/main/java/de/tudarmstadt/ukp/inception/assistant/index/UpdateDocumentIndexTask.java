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
package de.tudarmstadt.ukp.inception.assistant.index;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.inception.assistant.index.DocumentQueryService.FIELD_EMBEDDING;
import static de.tudarmstadt.ukp.inception.assistant.index.DocumentQueryService.FIELD_SOURCE_DOC;
import static de.tudarmstadt.ukp.inception.assistant.index.DocumentQueryService.FIELD_SOURCE_DOC_COMPLETE;
import static de.tudarmstadt.ukp.inception.assistant.index.DocumentQueryService.FIELD_TEXT;
import static de.tudarmstadt.ukp.inception.scheduling.TaskState.CANCELLED;
import static de.tudarmstadt.ukp.inception.scheduling.TaskState.RUNNING;
import static java.time.Duration.ofSeconds;
import static org.apache.lucene.index.VectorSimilarityFunction.COSINE;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.KnnFloatVectorField;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.FieldExistsQuery;
import org.apache.lucene.search.IndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageSession;
import de.tudarmstadt.ukp.inception.assistant.index.DocumentQueryServiceImpl.PooledIndex;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.scheduling.DebouncingTask;
import de.tudarmstadt.ukp.inception.scheduling.ProjectTask;
import de.tudarmstadt.ukp.inception.scheduling.TaskScope;

public class UpdateDocumentIndexTask
    extends DebouncingTask
    implements ProjectTask
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String TYPE = "UpdateDocumentIndexTask";

    private @Autowired DocumentService documentService;
    private @Autowired DocumentQueryServiceImpl indexService;

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
        try (var index = indexService.borrowIndex(getProject())) {
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

                monitor.setStateAndProgress(RUNNING, 0,
                        documentsToUnindex.size() + documentsToIndex.size());

                for (var sourceDocumentId : documentsToUnindex) {
                    if (monitor.isCancelled()) {
                        monitor.setState(CANCELLED);
                        break;
                    }

                    monitor.incrementProgress();
                    unindexDocument(index, sourceDocumentId);
                }

                try (var session = CasStorageSession.openNested()) {
                    for (var sourceDocument : documentsToIndex.values()) {
                        if (monitor.isCancelled()) {
                            monitor.setState(CANCELLED);
                            break;
                        }

                        indexDocument(index, sourceDocument);
                        monitor.incrementProgress();
                    }
                }

                if (!monitor.isCancelled()) {
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

            for (var sentence : cas.select(Sentence.class)) {
                var text = sentence.getCoveredText();
                var docEmbedding = indexService.getEmbedding(text);
                var doc = new Document();
                doc.add(new KnnFloatVectorField(FIELD_EMBEDDING, docEmbedding, COSINE));
                doc.add(new StoredField(FIELD_TEXT, text));
                aIndex.getIndexWriter().addDocument(doc);
            }
        }
        catch (IOException e) {
            LOG.error("Error indexing document {}", aSourceDocument, e);
        }
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

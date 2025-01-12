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
package de.tudarmstadt.ukp.inception.assistant.userguide;

import static java.nio.file.Files.exists;
import static java.nio.file.Files.readString;
import static java.nio.file.Files.writeString;
import static java.util.Collections.emptyList;
import static org.apache.lucene.index.VectorSimilarityFunction.DOT_PRODUCT;
import static org.apache.lucene.util.VectorUtil.l2normalize;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.KnnFloatVectorField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.KnnFloatVectorQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import de.tudarmstadt.ukp.inception.assistant.config.AssistantProperties;
import de.tudarmstadt.ukp.inception.assistant.embedding.EmbeddingService;
import de.tudarmstadt.ukp.inception.assistant.index.HighDimensionLucene912Codec;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.support.SettingsUtil;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

public class UserGuideQueryServiceImpl
    implements UserGuideQueryService, DisposableBean
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String FIELD_TEXT = "text";
    private static final String FIELD_EMBEDDING = "field";

    private final AssistantProperties properties;
    private final SchedulingService schedulingService;
    private final EmbeddingService embeddingService;

    private Directory indexDir;
    private DirectoryReader indexReader;
    private volatile boolean destroyed = false;

    public UserGuideQueryServiceImpl(AssistantProperties aProperties,
            SchedulingService aSchedulingService, EmbeddingService aEmbeddingService)
    {
        properties = aProperties;
        schedulingService = aSchedulingService;
        embeddingService = aEmbeddingService;
    }

    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        init();
    }

    void init()
    {
        if (properties.getUserGuide().isRebuildIndexOnBoot() || !isIndexUpToDate()) {
            var htmlUrl = getClass().getResource("/public/doc/user-guide.html");

            // try {
            // htmlUrl = new URL(
            // "file:/Users/bluefire/git/inception-application/inception/inception-doc/target/doc-out/user-guide.html");
            // }
            // catch (IOException e) {
            // LOG.debug("Unable to find specified user manual - not building index.");
            // }

            if (htmlUrl != null) {
                LOG.debug("Building user manual index in the background...");
                deleteIndex();
                schedulingService.enqueue(UserGuideIndexingTask.builder() //
                        .withTrigger("User manual index is missing or not up-to-date") //
                        .withHtml(htmlUrl) //
                        .build());
            }
            else {
                LOG.debug("Unable to find user manual - not building index.");
            }
        }
        else {
            LOG.debug("User manual index is already up-to-date");
        }
    }

    @Override
    public void destroy()
    {
        destroyed = true;

        if (indexReader != null) {
            try {
                indexReader.close();
            }
            catch (Exception e) {
                LOG.error("Error closing index reader", e);
            }
            finally {
                indexReader = null;
            }
        }

        if (indexDir != null) {
            try {
                indexDir.close();
            }
            catch (Exception e) {
                LOG.error("Error closing index directory", e);
            }
            finally {
                indexDir = null;
            }
        }
    }

    @Override
    public List<String> query(String aQuery, int aTopN, double aScoreThreshold)
    {
        if (destroyed) {
            return emptyList();
        }

        if (!isIndexUpToDate()) {
            LOG.warn("Unable to query user manual - index does not exist yet");
            return emptyList();
        }

        LOG.trace("User guide knn query: [{}]", aQuery);

        try {
            var reader = getSharedIndexReader();

            var maybeEmbedding = embeddingService.embed(aQuery);
            if (!maybeEmbedding.isPresent()) {
                return emptyList();
            }

            var queryEmbedding = l2normalize(maybeEmbedding.get(), false);

            var searcher = new IndexSearcher(reader);
            var query = new KnnFloatVectorQuery(FIELD_EMBEDDING, queryEmbedding, aTopN);
            var result = searcher.search(query, aTopN);

            var passages = new ArrayList<String>();
            for (var scoreDoc : result.scoreDocs) {
                if (scoreDoc.score >= aScoreThreshold) {
                    var doc = reader.storedFields().document(scoreDoc.doc);
                    var text = doc.get(FIELD_TEXT);
                    passages.add(text);
                    LOG.trace("Score {} above threshold: [{}]", scoreDoc.score, text);
                }
                else if (LOG.isTraceEnabled()) {
                    var doc = reader.storedFields().document(scoreDoc.doc);
                    LOG.trace("Score {} too low: [{}]", scoreDoc.score, doc.get("text"));
                }
            }

            return passages;
        }
        catch (Exception e) {
            LOG.error("Unable to query user manual", e);
            return emptyList();
        }
    }

    IndexWriter getIndexWriter() throws IOException
    {
        var iwc = new IndexWriterConfig();
        iwc.setCodec(new HighDimensionLucene912Codec(embeddingService.getDimension()));
        return new IndexWriter(getSharedIndexDirectory(), iwc);
    }

    DirectoryReader getSharedIndexReader() throws IOException
    {
        if (indexReader == null) {
            indexReader = DirectoryReader.open(getSharedIndexDirectory());
        }

        return indexReader;
    }

    synchronized Directory getSharedIndexDirectory() throws IOException
    {
        if (indexDir == null) {
            indexDir = new MMapDirectory(getIndexPath());
        }

        return indexDir;
    }

    void indexBlocks(IndexWriter aWriter, String... aText) throws IOException
    {
        if (destroyed) {
            return;
        }

        var docEmbeddings = embeddingService.embed(aText);
        for (var embedding : docEmbeddings) {
            var doc = new Document();
            doc.add(new KnnFloatVectorField(FIELD_EMBEDDING,
                    l2normalize(embedding.getValue(), false), DOT_PRODUCT));
            doc.add(new StoredField(FIELD_TEXT, embedding.getKey()));
            aWriter.addDocument(doc);
        }
    }

    void deleteIndex()
    {
        try {
            var indexPath = getIndexPath();
            if (exists(indexPath)) {
                FileUtils.forceDelete(indexPath.toFile());
            }
        }
        catch (IOException e) {
            LOG.error("Cannot delete user manual index", e);
        }
    }

    void markIndexUpToDate()
    {
        var indexPath = getIndexPath();
        if (!exists(indexPath)) {
            LOG.warn("Cannot mark user manual index as complete because index does not exist");
            return;
        }

        try {
            var metadata = getIndexMetadata();
            JSONUtil.toPrettyJsonString(metadata);
            writeString(indexPath.resolve("i7n-metadata.json"),
                    JSONUtil.toPrettyJsonString(metadata));
        }
        catch (IOException e) {
            LOG.error("Cannot mark user manual index as complete", e);
        }
    }

    private UserGuideIndexMetadata getIndexMetadata()
    {
        return new UserGuideIndexMetadata(SettingsUtil.getVersionString(),
                properties.getEmbedding().getModel(), embeddingService.getDimension());
    }

    boolean isIndexUpToDate()
    {
        var indexPath = getIndexPath();
        if (!exists(indexPath)) {
            return false;
        }

        var indexMetadataFile = indexPath.resolve("i7n-metadata.json");
        if (!exists(indexMetadataFile)) {
            return false;
        }

        try {
            var metadata = JSONUtil.fromJsonString(UserGuideIndexMetadata.class,
                    readString(indexMetadataFile));
            return getIndexMetadata().equals(metadata);
        }
        catch (IOException e) {
            LOG.error("Unable to check index metadata", e);
            return false;
        }
    }

    Path getIndexPath()
    {
        return new File(SettingsUtil.getApplicationHome(), "indices/user-manual").toPath();
    }
}

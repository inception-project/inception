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

import static java.lang.Math.max;
import static java.util.Collections.emptyList;
import static java.util.Collections.sort;
import static java.util.Comparator.comparing;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toCollection;
import static org.apache.lucene.util.VectorUtil.l2normalize;
import static org.apache.uima.cas.text.AnnotationPredicates.overlapping;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.function.FailableFunction;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntRange;
import org.apache.lucene.document.KnnFloatVectorField;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.simple.SimpleQueryParser;
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

    private static record Candidate(int doc, double sem, double lex) {}

    private static record Scored(int docId, double score) {}

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

    private SearchResult readFromIndex(Project aProject,
            FailableFunction<DirectoryReader, SearchResult, Exception> aQueryFunction)
    {
        try (var index = borrowIndex(aProject)) {
            try (var reader = DirectoryReader.open(index.getIndexWriter())) {
                if (reader.numDocs() == 0) {
                    triggerIndexing(aProject);
                    return SearchResult.builder().withMatches(emptyList()).withTotalMatches(0)
                            .withTruncated(false).build();
                }

                var result = aQueryFunction.apply(reader);

                // merge overlapping chunks before returning, preserve totalMatches + truncation
                var merged = mergeOverlappingChunks(result.matches());
                var builder = SearchResult.builder() //
                        .withMatches(merged); //
                result.truncated().ifPresent(builder::withTruncated);
                result.totalMatches().ifPresent(builder::withTotalMatches);
                return builder.build();
            }
        }
        catch (Exception e) {
            LOG.error("Error querying document index for project {}", aProject, e);
            return SearchResult.builder().withMatches(emptyList()).withTotalMatches(0)
                    .withTruncated(false).build();
        }
    }

    /**
     * Semantic query using vector similarity (KNN) against document embeddings.
     *
     * <p>
     * When to prefer this method:
     * <ul>
     * <li>Prefer {@code semanticQuery} when you want concept-level or paraphrase-style matches that
     * go beyond exact lexical overlap.</li>
     * <li>Do not use this method if you require strict lexical matching or operator-based queries;
     * use {@link #keywordQuery} for that.</li>
     * </ul>
     *
     * @param aProject
     *            project to search
     * @param aQuery
     *            query string to embed
     * @param aTopN
     *            maximum number of results to return
     * @param aScoreThreshold
     *            minimum semantic score required for a candidate to be included in the results
     * @return a {@link SearchResult} with semantic matches
     */
    @Override
    public SearchResult semanticQuery(Project aProject, String aQuery, int aTopN,
            double aScoreThreshold)
    {
        return readFromIndex(aProject, reader -> {
            LOG.trace("KNN query: [{}]", aQuery);

            var maybeEmbedding = embeddingService.embed(aQuery);
            if (!maybeEmbedding.isPresent()) {
                return SearchResult.emptyResult();
            }

            var queryEmbedding = l2normalize(maybeEmbedding.get(), false);

            var searcher = new IndexSearcher(reader);
            var query = new KnnFloatVectorQuery(FIELD_EMBEDDING, queryEmbedding, aTopN + 1);
            var result = searcher.search(query, aTopN + 1);

            var documentNameCache = new HashMap<Long, String>();
            var chunks = new ArrayList<Chunk>();
            var chunksAboveThreshold = 0;
            for (var scoreDoc : result.scoreDocs) {
                if (scoreDoc.score < aScoreThreshold) {
                    if (LOG.isTraceEnabled()) {
                        var doc = reader.storedFields().document(scoreDoc.doc);
                        LOG.trace("Score {} too low: [{}]", scoreDoc.score, doc.get(FIELD_TEXT));
                    }
                    continue;
                }

                chunksAboveThreshold++;

                if (chunks.size() < aTopN) {
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
            }

            var truncated = chunksAboveThreshold > aTopN;
            var totalMatches = truncated ? -1 : chunks.size();
            return SearchResult.builder() //
                    .withMatches(chunks) //
                    .withTotalMatches(totalMatches) //
                    .withTruncated(truncated) //
                    .build();
        });
    }

    /**
     * Keyword (lexical) query parsed with a {@link StandardAnalyzer} and {@link SimpleQueryParser}.
     *
     * <p>
     * When to prefer this method:
     * <ul>
     * <li>Prefer {@code keywordQuery} when you need exact term matching, operator support, or
     * deterministic lexical scores without embeddings.</li>
     * <li>For fuzzy/conceptual matches, use {@link #semanticQuery}; for a blend of both, use
     * {@link #hybridQuery}.</li>
     * </ul>
     *
     * @param aProject
     *            project to search
     * @param aQuery
     *            user query string
     * @param aTopN
     *            maximum number of results to return
     * @return a {@link SearchResult} with lexical matches
     */
    @Override
    public SearchResult keywordQuery(Project aProject, String aQuery, int aTopN)
    {
        return readFromIndex(aProject, reader -> {
            LOG.trace("Keyword query: [{}]", aQuery);

            // parse lexical query (escape because atm we do not want to explain operators to LLM)
            var analyzer = new StandardAnalyzer();
            var boosts = Map.of(FIELD_TEXT, 1.0f);
            var sqp = new SimpleQueryParser(analyzer, boosts);
            var parsed = sqp.parse(QueryParser.escape(aQuery));

            var searcher = new IndexSearcher(reader);
            var top = searcher.search(parsed, aTopN);

            var totalMatches = top.totalHits == null ? top.scoreDocs.length
                    : (int) top.totalHits.value;
            var truncated = top.totalHits != null && top.totalHits.value > aTopN;

            var documentNameCache = new HashMap<Long, String>();
            var chunks = new ArrayList<Chunk>();
            for (var sd : top.scoreDocs) {
                var doc = reader.storedFields().document(sd.doc);
                var docId = doc.getField(FIELD_SOURCE_DOC_ID).numericValue().longValue();
                var docName = getDocumentName(aProject, documentNameCache, docId);
                chunks.add(Chunk.builder() //
                        .withDocumentId(docId) //
                        .withDocumentName(docName) //
                        .withSection(doc.get(FIELD_SECTION)) //
                        .withText(doc.get(FIELD_TEXT)) //
                        .withBegin(doc.getField(FIELD_BEGIN).numericValue().intValue()) //
                        .withEnd(doc.getField(FIELD_END).numericValue().intValue()) //
                        .withScore(sd.score) //
                        .build());
                LOG.trace("Score {} above threshold: [{}]", sd.score, doc.get(FIELD_TEXT));
            }

            return SearchResult.builder() //
                    .withMatches(chunks) //
                    .withTotalMatches(totalMatches) //
                    .withTruncated(truncated) //
                    .build();
        });
    }

    /**
     * Hybrid query combining semantic (vector) and lexical (keyword) signals.
     *
     * <p>
     * When to prefer this method:
     * <ul>
     * <li>Prefer {@code hybridQuery} when you want the recall benefits of semantic (embedding)
     * matching but also require lexical relevance to boost precision.</li>
     * <li>Use {@link #semanticQuery} when you only need semantic similarity (e.g., paraphrase or
     * concept-level matching) and lexical matches are not important.</li>
     * <li>Use {@link #keywordQuery} when you need exact or purely lexical term matching and
     * deterministic keyword scoring without embeddings.</li>
     * </ul>
     *
     * @param aProject
     *            project to search
     * @param aQuery
     *            user query string
     * @param aTopN
     *            maximum number of results to return
     * @return a {@link SearchResult} containing ranked chunks, the truncation flag and a total
     *         match count (may be -1 when truncated)
     */
    @Override
    public SearchResult hybridQuery(Project aProject, String aQuery, int aTopN)
    {
        return readFromIndex(aProject, reader -> {
            LOG.trace("Hybrid query: [{}]", aQuery);

            var maybeEmbedding = embeddingService.embed(aQuery);
            if (!maybeEmbedding.isPresent()) {
                return SearchResult.builder().withMatches(emptyList()).withTotalMatches(0)
                        .withTruncated(false).build();
            }

            var queryEmbedding = l2normalize(maybeEmbedding.get(), false);
            var searcher = new IndexSearcher(reader);

            // semantic candidates
            var candidateK = max(aTopN * 5, 100);
            var knn = new KnnFloatVectorQuery(FIELD_EMBEDDING, queryEmbedding, candidateK);
            var knnTop = searcher.search(knn, candidateK);

            if (knnTop.scoreDocs.length == 0) {
                return SearchResult.emptyResult();
            }

            // parse lexical query (escape because atm we do not want to explain operators to LLM)
            var analyzer = new StandardAnalyzer();
            var boosts = Map.of(FIELD_TEXT, 1.0f);
            var sqp = new SimpleQueryParser(analyzer, boosts);
            var lexQuery = sqp.parse(QueryParser.escape(aQuery));

            // gather scores and combine
            double maxSem = 0.0d;
            double maxLex = 0.0d;
            var candidates = new ArrayList<Candidate>();
            for (var sd : knnTop.scoreDocs) {
                var sem = sd.score;
                if (sem > maxSem) {
                    maxSem = sem;
                }
                var expl = searcher.explain(lexQuery, sd.doc);
                var lex = expl == null ? 0.0d : expl.getValue().doubleValue();
                if (lex > maxLex) {
                    maxLex = lex;
                }
                candidates.add(new Candidate(sd.doc, sem, lex));
            }

            // normalize and combine
            double wSem = 0.7d;
            double wLex = 0.3d;

            var scored = new ArrayList<Scored>();
            for (var c : candidates) {
                var docId = c.doc();
                var sem = c.sem();
                var lex = c.lex();
                var nSem = maxSem > 0 ? sem / maxSem : 0.0d;
                var nLex = maxLex > 0 ? lex / maxLex : 0.0d;
                var combined = wSem * nSem + wLex * nLex;
                scored.add(new Scored(docId, combined));
            }

            // sort by combined score desc and build chunks
            scored.sort((a, b) -> Double.compare(b.score(), a.score()));
            var documentNameCache = new HashMap<Long, String>();
            var chunks = new ArrayList<Chunk>();
            var taken = 0;
            for (var entry : scored) {
                if (taken >= aTopN) {
                    break;
                }
                var docIdInternal = entry.docId();
                var sdDoc = reader.storedFields().document(docIdInternal);
                var docId = sdDoc.getField(FIELD_SOURCE_DOC_ID).numericValue().longValue();
                var docName = getDocumentName(aProject, documentNameCache, docId);
                chunks.add(Chunk.builder() //
                        .withDocumentId(docId) //
                        .withDocumentName(docName) //
                        .withSection(sdDoc.get(FIELD_SECTION)) //
                        .withText(sdDoc.get(FIELD_TEXT)) //
                        .withBegin(sdDoc.getField(FIELD_BEGIN).numericValue().intValue()) //
                        .withEnd(sdDoc.getField(FIELD_END).numericValue().intValue()) //
                        .withScore(entry.score()) // combined score
                        .build());
                taken++;
            }

            var truncated = chunks.size() >= aTopN;
            var reportedTotal = truncated ? -1 : chunks.size();
            return SearchResult.builder() //
                    .withMatches(chunks) //
                    .withTotalMatches(reportedTotal) //
                    .withTruncated(truncated) //
                    .build();
        });
    }

    private void triggerIndexing(Project aProject)
    {
        schedulingService.enqueue(UpdateDocumentIndexTask.builder() //
                .withTrigger("Query on empty index") //
                .withProject(aProject) //
                .build());
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

    /**
     * @return list of overlapping chunks merged (as long as they are in the same section.
     */
    static List<Chunk> mergeOverlappingDocChunks(List<Chunk> aDocChunks)
    {
        if (aDocChunks.size() < 2) {
            return aDocChunks;
        }

        sort(aDocChunks, comparing(Chunk::begin));

        var mergedChunks = new ArrayList<Chunk>();
        var it = aDocChunks.iterator();
        var prev = it.next();
        while (it.hasNext()) {
            var cur = it.next();
            if (overlapping(prev.begin(), prev.end(), cur.begin(), cur.end())
                    && Objects.equals(prev.section(), cur.section())) {
                prev = prev.merge(cur);
            }
            else {
                mergedChunks.add(prev);
                prev = cur;
            }
        }

        // add the last accumulated chunk
        mergedChunks.add(prev);

        return mergedChunks;
    }

    static List<Chunk> mergeOverlappingChunks(List<Chunk> aChunks)
    {
        var chunksByDocument = aChunks.stream().collect(groupingBy(Chunk::documentId,
                LinkedHashMap::new, mapping(identity(), toCollection(ArrayList::new))));

        var mergedChunks = new ArrayList<Chunk>();
        for (var docChunkSet : chunksByDocument.values()) {
            mergedChunks.addAll(mergeOverlappingDocChunks(docChunkSet));
        }

        sort(mergedChunks, comparing(Chunk::score).reversed());

        return mergedChunks;
    }

    static Document makeDocument(long aSourceDocumentId, int aBegin, int aEnd, float[] aEmbedding,
            String aText)
    {
        var doc = new org.apache.lucene.document.Document();
        var normalizedEmbedding = l2normalize(aEmbedding, false);
        doc.add(new KnnFloatVectorField(FIELD_EMBEDDING, normalizedEmbedding,
                VectorSimilarityFunction.DOT_PRODUCT));
        doc.add(new IntRange(FIELD_RANGE, new int[] { aBegin }, new int[] { aEnd }));
        doc.add(new LongPoint(FIELD_SOURCE_DOC_ID, aSourceDocumentId));
        doc.add(new StoredField(FIELD_SOURCE_DOC_ID, aSourceDocumentId));
        doc.add(new StoredField(FIELD_SECTION, ""));
        doc.add(new TextField(FIELD_TEXT, aText, TextField.Store.YES));
        doc.add(new StoredField(FIELD_BEGIN, aBegin));
        doc.add(new StoredField(FIELD_END, aEnd));
        return doc;
    }
}

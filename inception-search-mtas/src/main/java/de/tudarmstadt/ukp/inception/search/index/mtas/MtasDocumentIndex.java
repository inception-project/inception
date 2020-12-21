/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.inception.search.index.mtas;

import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.PROJECT_FOLDER;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IGNORE;
import static de.tudarmstadt.ukp.inception.search.index.mtas.MtasUimaParser.PARAM_PROJECT_ID;
import static de.tudarmstadt.ukp.inception.search.index.mtas.MtasUimaParser.getIndexedName;
import static de.tudarmstadt.ukp.inception.search.index.mtas.MtasUtils.decodeFSAddress;
import static java.util.concurrent.TimeUnit.SECONDS;
import static mtas.analysis.util.MtasTokenizerFactory.ARGUMENT_PARSER;
import static mtas.analysis.util.MtasTokenizerFactory.ARGUMENT_PARSER_ARGS;
import static mtas.codec.MtasCodec.MTAS_CODEC_NAME;
import static org.apache.commons.io.FileUtils.deleteDirectory;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SegmentReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ReferenceManager;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.spans.SpanWeight;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.openjson.JSONObject;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.search.ExecutionException;
import de.tudarmstadt.ukp.inception.search.FeatureIndexingSupport;
import de.tudarmstadt.ukp.inception.search.FeatureIndexingSupportRegistry;
import de.tudarmstadt.ukp.inception.search.PrimitiveUimaIndexingSupport;
import de.tudarmstadt.ukp.inception.search.SearchQueryRequest;
import de.tudarmstadt.ukp.inception.search.SearchResult;
import de.tudarmstadt.ukp.inception.search.index.PhysicalIndex;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import mtas.analysis.token.MtasTokenString;
import mtas.analysis.util.MtasTokenizerFactory;
import mtas.codec.util.CodecInfo;
import mtas.codec.util.CodecUtil;
import mtas.parser.cql.MtasCQLParser;
import mtas.search.spans.util.MtasSpanQuery;

/**
 * The Mtas implementation for a physical index
 */
public class MtasDocumentIndex
    implements PhysicalIndex
{
    /**
     * Static map allowing access to the MTAS index for a given project. This the
     * {@link MtasUimaParser} to quickly access the current layer configuration from the current
     * index.
     */
    private static Map<Long, MtasDocumentIndex> OPEN_INDEXES = new ConcurrentHashMap<>();

    private static final String INDEX = "indexMtas";

    /**
     * Constant for the field which carries the unique identifier for the index document consisting:
     * {@code [sourceDocumentId]/[annotationDocumentId]}
     */
    private static final String FIELD_ID = "id";

    /**
     * Constant for the field which carries the source document id;
     */
    private static final String FIELD_SOURCE_DOCUMENT_ID = "sourceDocumentId";

    /**
     * Constant for the field which carries the source document id;
     */
    private static final String FIELD_ANNOTATION_DOCUMENT_ID = "annotationDocumentId";

    /** The Constant FIELD_TITLE. */
    private static final String FIELD_TITLE = "title";

    /** The Constant FIELD_CONTENT. */
    private static final String FIELD_CONTENT = "content";

    /**
     * Constant for the field which carries the annotator's username.
     */
    private static final String FIELD_USER = "user";

    /** The Constant FIELD_TIMESTAMP. */
    private static final String FIELD_TIMESTAMP = "timestamp";

    // Default prefix for CQL queries
    private static final String DEFAULT_PREFIX = "Token";

    private static final int RESULT_WINDOW_SIZE = 3;

    private static final String EMPTY_FEATURE_VALUE_KEY = "<Empty>";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final FeatureIndexingSupportRegistry featureIndexingSupportRegistry;
    private final FeatureSupportRegistry featureSupportRegistry;
    private final DocumentService documentService;
    private final AnnotationSchemaService schemaService;
    private final Project project;
    private final File repositoryDir;
    private final ScheduledExecutorService schedulerService;

    // The index writers for this index
    private IndexWriter _indexWriter;
    private ReferenceManager<IndexSearcher> _searcherManager;
    private ScheduledFuture<?> _commitFuture;

    private Map<AnnotationLayer, List<AnnotationFeature>> layersAndFeatures;

    public MtasDocumentIndex(Project aProject, DocumentService aDocumentService,
            AnnotationSchemaService aSchemaService, String aDir,
            FeatureIndexingSupportRegistry aFeatureIndexingSupportRegistry,
            FeatureSupportRegistry aFeatureSupportRegistry)
    {
        schemaService = aSchemaService;
        documentService = aDocumentService;
        project = aProject;
        featureIndexingSupportRegistry = aFeatureIndexingSupportRegistry;
        featureSupportRegistry = aFeatureSupportRegistry;
        repositoryDir = new File(aDir);

        schedulerService = new ScheduledThreadPoolExecutor(0);
    }

    private synchronized IndexWriter getIndexWriter() throws IOException
    {
        if (_indexWriter != null) {
            return _indexWriter;
        }

        log.debug("Opening index for project [{}]({})", project.getName(), project.getId());

        try {
            // Initialize and populate the hash maps for the layers and features
            layersAndFeatures = new LinkedHashMap<>();
            schemaService.listAnnotationLayer(project)
                    .forEach(layer -> layersAndFeatures.put(layer, new ArrayList<>()));
            for (AnnotationFeature feat : schemaService.listAnnotationFeature(project)) {
                if (!feat.getLayer().isEnabled() || !feat.isEnabled()) {
                    continue;
                }

                List<AnnotationFeature> feats = layersAndFeatures.computeIfAbsent( //
                        feat.getLayer(), key -> new ArrayList<>());
                feats.add(feat);
            }

            // Add the project id to the configuration
            JSONObject jsonParserConfiguration = new JSONObject();
            jsonParserConfiguration.put(PARAM_PROJECT_ID, project.getId());

            // Tokenizer parameters
            Map<String, String> tokenizerArguments = new HashMap<>();
            tokenizerArguments.put(ARGUMENT_PARSER, MtasUimaParser.class.getName());
            tokenizerArguments.put(ARGUMENT_PARSER_ARGS, jsonParserConfiguration.toString());

            // Build analyzer
            Analyzer mtasAnalyzer = CustomAnalyzer.builder()
                    .withTokenizer(MtasTokenizerFactory.class, tokenizerArguments).build();

            Map<String, Analyzer> analyzerPerField = new HashMap<String, Analyzer>();
            analyzerPerField.put(FIELD_CONTENT, mtasAnalyzer);

            PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(),
                    analyzerPerField);

            // Build IndexWriter
            FileUtils.forceMkdir(getIndexDir());
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setCodec(Codec.forName(MTAS_CODEC_NAME));
            IndexWriter indexWriter = new IndexWriter(FSDirectory.open(getIndexDir().toPath()),
                    config);

            // Initialize the index
            indexWriter.commit();

            // After the index has been initialized, assign the _indexWriter - this is also used
            // by isOpen() to check if the index writer is available.
            _indexWriter = indexWriter;

            return _indexWriter;
        }
        finally {
            if (isOpen()) {
                OPEN_INDEXES.put(project.getId(), this);
            }
        }
    }

    private void ensureAllIsCommitted()
    {
        if (_commitFuture != null && !_commitFuture.isDone()) {
            try {
                _commitFuture.get();
            }
            catch (Exception e) {
                log.error(
                        "Error waiting for pending scheduled commit to complete in project [{}]({})",
                        project.getName(), project.getId());
            }
            finally {
                _commitFuture = null;
            }
        }

        if (_indexWriter != null) {
            try {
                _indexWriter.commit();
            }
            catch (IOException e) {
                log.error("Error committing changes to index for project [{}]({})",
                        project.getName(), project.getId());
            }
        }
    }

    @Override
    public synchronized void close()
    {
        if (schedulerService != null) {
            schedulerService.shutdown();
        }

        closeIndex();
    }

    private void closeIndex()
    {
        OPEN_INDEXES.remove(project.getId());

        if (!isOpen()) {
            return;
        }

        ensureAllIsCommitted();

        try {
            _indexWriter.close();
        }
        catch (IOException e) {
            log.error("Error closing index for project [{}]({})", project.getName(),
                    project.getId());
        }
        finally {
            _indexWriter = null;
            _searcherManager = null;
        }
    }

    private synchronized ReferenceManager<IndexSearcher> getSearcherManager() throws IOException
    {
        if (_searcherManager == null) {
            _searcherManager = new SearcherManager(getIndexWriter(), true, true,
                    new SearcherFactory());
        }

        return _searcherManager;
    }

    private synchronized void scheduleCommit()
    {
        if (schedulerService.isShutdown() || schedulerService.isTerminated()) {
            return;
        }

        // already scheduled commit which is not done yet, we don't need to schedule again
        if (_commitFuture != null && !_commitFuture.isDone()) {
            return;
        }

        log.debug("Enqueuing new future to index for project [{}]({})", project.getName(),
                project.getId());

        _commitFuture = schedulerService.schedule(() -> {
            try {
                log.debug("Executing future to index for project [{}]({})", project.getName(),
                        project.getId());
                if (_indexWriter != null && _indexWriter.isOpen()) {
                    _indexWriter.commit();
                    log.debug("Committed changes to index for project [{}]({})", project.getName(),
                            project.getId());

                    if (_searcherManager != null) {
                        _searcherManager.maybeRefresh();
                    }
                }
            }
            catch (IOException e) {
                log.error("Unable to commit to index of project [{}]({})", project.getName(),
                        project.getId());
            }
        }, 3, SECONDS);
    }

    /**
     * Checks if a project index is open
     * 
     * @return True if the index is open. False otherwise.
     */
    @Override
    public boolean isOpen()
    {
        return _indexWriter != null ? _indexWriter.isOpen() : false;
    }

    @Override
    public Map<String, List<SearchResult>> executeQuery(SearchQueryRequest aRequest)
        throws IOException, ExecutionException
    {
        return _executeQuery(this::doQuery, aRequest);
    }

    @Override
    public long numberOfQueryResults(SearchQueryRequest aRequest)
        throws ExecutionException, IOException
    {
        return _executeQuery(this::doCountResults, aRequest);
    }

    private <T> T _executeQuery(QueryRunner<T> aRunner, SearchQueryRequest aRequest)
        throws IOException, ExecutionException
    {
        log.trace("Executing query [{}] on index [{}]", aRequest, getIndexDir());

        ensureAllIsCommitted();

        final MtasSpanQuery mtasSpanQuery;
        try {
            String modifiedQuery = preprocessQuery(aRequest.getQuery());
            MtasSpanQuery mtasSpanQuery1;
            try (Reader reader = new StringReader(modifiedQuery)) {
                MtasCQLParser parser = new MtasCQLParser(reader);
                mtasSpanQuery1 = parser.parse(FIELD_CONTENT, DEFAULT_PREFIX, null, null, null);
            }
            mtasSpanQuery = mtasSpanQuery1;
        }
        catch (IOException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ExecutionException("Unable to parse query [" + aRequest.getQuery() + "]", e);
        }

        IndexSearcher searcher = null;
        try {
            searcher = getSearcherManager().acquire();
            return aRunner.run(searcher, aRequest, mtasSpanQuery);
        }
        catch (Exception e) {
            throw new ExecutionException("Unable to execute query [" + aRequest.getQuery() + "]",
                    e);
        }
        finally {
            if (searcher != null) {
                // Releasing and setting to null per recommendation in JavaDoc of release(searcher)
                // method
                getSearcherManager().release(searcher);
                searcher = null;
            }
        }
    }

    private String preprocessQuery(String aQuery)
    {
        String result;

        if (!(aQuery.contains("\"") || aQuery.contains("[") || aQuery.contains("]")
                || aQuery.contains("{") || aQuery.contains("}") || aQuery.contains("<")
                || aQuery.contains(">"))) {
            // Convert raw words query to a Mtas CQP query

            result = "";
            BreakIterator words = BreakIterator.getWordInstance();
            words.setText(aQuery);

            int start = words.first();
            int end = words.next();
            while (end != BreakIterator.DONE) {
                String word = aQuery.substring(start, end);
                if (!word.trim().isEmpty()) {
                    // Add the word to the query
                    result += "\"" + word + "\"";
                }
                start = end;
                end = words.next();
                if (end != BreakIterator.DONE) {
                    result += " ";
                }
            }
        }
        else {
            result = aQuery;
        }

        return result;
    }

    private long doCountResults(IndexSearcher searcher, SearchQueryRequest aRequest,
            MtasSpanQuery q)
        throws IOException
    {
        ListIterator<LeafReaderContext> leafReaderContextIterator = searcher.getIndexReader()
                .leaves().listIterator();

        Map<Long, Long> annotatableDocuments = listAnnotatableDocuments(aRequest.getProject(),
                aRequest.getUser());

        final float boost = 0;
        SpanWeight spanweight = q.rewrite(searcher.getIndexReader()).createWeight(searcher, false,
                boost);

        long numResults = 0;

        while (leafReaderContextIterator.hasNext()) {
            LeafReaderContext leafReaderContext = leafReaderContextIterator.next();
            try {
                Spans spans = spanweight.getSpans(leafReaderContext, SpanWeight.Postings.POSITIONS);
                SegmentReader segmentReader = (SegmentReader) leafReaderContext.reader();
                if (spans != null) {
                    while (spans.nextDoc() != Spans.NO_MORE_DOCS) {
                        if (segmentReader.numDocs() == segmentReader.maxDoc()
                                || segmentReader.getLiveDocs().get(spans.docID())) {
                            Document document = segmentReader.document(spans.docID());

                            // Retrieve user
                            String user = document.get(FIELD_USER);

                            // Retrieve source and annotation document ids
                            String rawSourceDocumentId = document.get(FIELD_SOURCE_DOCUMENT_ID);
                            String rawAnnotationDocumentId = document
                                    .get(FIELD_ANNOTATION_DOCUMENT_ID);
                            if (rawSourceDocumentId == null || rawAnnotationDocumentId == null) {
                                log.trace(
                                        "Indexed document lacks source/annotation document IDs"
                                                + " - source: {}, annotation: {}",
                                        rawSourceDocumentId, rawAnnotationDocumentId);
                                continue;

                            }
                            long sourceDocumentId = Long.valueOf(rawSourceDocumentId);
                            long annotationDocumentId = Long.valueOf(rawAnnotationDocumentId);

                            // If the query is limited to a given document, skip any results
                            // which are not in the given document
                            Optional<SourceDocument> limitedToDocument = aRequest
                                    .getLimitedToDocument();
                            if (limitedToDocument.isPresent() && !Objects
                                    .equals(limitedToDocument.get().getId(), sourceDocumentId)) {
                                log.trace(
                                        "Query limited to document {}, skipping results for "
                                                + "document {}",
                                        limitedToDocument.get().getId(), sourceDocumentId);
                                continue;
                            }

                            if (annotatableDocuments.containsKey(sourceDocumentId)
                                    && annotationDocumentId == -1) {
                                // Exclude result if the retrieved document is a sourcedocument
                                // (that is, has annotationDocument = -1) AND it has a
                                // corresponding annotation document for this user
                                log.trace("Skipping results from indexed source document {} in"
                                        + "favor of results from the corresponding annotation "
                                        + "document", sourceDocumentId);
                                continue;
                            }
                            else if (annotationDocumentId != -1
                                    && !aRequest.getUser().getUsername().equals(user)) {
                                // Exclude result if the retrieved document is an annotation
                                // document (that is, annotationDocument != -1 and its username
                                // is different from the quering user
                                log.trace(
                                        "Skipping results from annotation document for user {} "
                                                + "which does not match the requested user {}",
                                        user, aRequest.getUser().getUsername());
                                continue;
                            }

                            while (spans.nextStartPosition() != Spans.NO_MORE_POSITIONS) {
                                numResults++;
                            }
                        }
                    }
                }
            }
            catch (Exception e) {
                log.error("Unable to process query results", e);
                numResults = -1;
            }
        }
        return numResults;
    }

    private Map<Long, Long> listAnnotatableDocuments(Project aProject, User aUser)
    {
        Map<Long, Long> annotateableDocuments = new HashMap<>();
        documentService.listAnnotatableDocuments(aProject, aUser).entrySet().stream()
                .filter(e -> e.getValue() != null) // only include entries where the annodoc is not
                                                   // null
                .forEach(e -> annotateableDocuments.put(e.getKey().getId(), e.getValue().getId()));
        return annotateableDocuments;
    }

    private Map<String, List<SearchResult>> doQuery(IndexSearcher searcher,
            SearchQueryRequest aRequest, MtasSpanQuery q)
        throws IOException
    {
        Map<String, List<SearchResult>> results = new LinkedHashMap<>();

        ListIterator<LeafReaderContext> leafReaderContextIterator = searcher.getIndexReader()
                .leaves().listIterator();

        Map<SourceDocument, AnnotationDocument> sourceAnnotationDocPairs = documentService
                .listAnnotatableDocuments(aRequest.getProject(), aRequest.getUser());
        Map<Long, SourceDocument> sourceDocumentIndex = new HashMap<>();
        sourceAnnotationDocPairs.entrySet().stream()
                .forEach(e -> sourceDocumentIndex.put(e.getKey().getId(), e.getKey()));

        final float boost = 0;
        SpanWeight spanweight = q.rewrite(searcher.getIndexReader()).createWeight(searcher, false,
                boost);

        long offset = aRequest.getOffset();
        long count = aRequest.getCount();
        long current = 0;

        resultIteration: while (leafReaderContextIterator.hasNext()) {
            LeafReaderContext leafReaderContext = leafReaderContextIterator.next();
            try {
                Spans spans = spanweight.getSpans(leafReaderContext, SpanWeight.Postings.POSITIONS);
                SegmentReader segmentReader = (SegmentReader) leafReaderContext.reader();
                Terms terms = segmentReader.terms(FIELD_CONTENT);
                CodecInfo mtasCodecInfo = CodecInfo.getCodecInfoFromTerms(terms);
                if (spans != null) {
                    while (spans.nextDoc() != Spans.NO_MORE_DOCS) {
                        if (segmentReader.numDocs() == segmentReader.maxDoc()
                                || segmentReader.getLiveDocs().get(spans.docID())) {
                            Document document = segmentReader.document(spans.docID());

                            // Retrieve user
                            String user = document.get(FIELD_USER);

                            // Retrieve source and annotation document ids
                            String rawSourceDocumentId = document.get(FIELD_SOURCE_DOCUMENT_ID);
                            String rawAnnotationDocumentId = document
                                    .get(FIELD_ANNOTATION_DOCUMENT_ID);
                            if (rawSourceDocumentId == null || rawAnnotationDocumentId == null) {
                                log.trace(
                                        "Indexed document lacks source/annotation document IDs"
                                                + " - source: {}, annotation: {}",
                                        rawSourceDocumentId, rawAnnotationDocumentId);
                                continue;

                            }

                            long sourceDocumentId = Long.valueOf(rawSourceDocumentId);
                            long annotationDocumentId = Long.valueOf(rawAnnotationDocumentId);
                            boolean matchInSourceDocument = annotationDocumentId == -1;

                            SourceDocument sourceDocument = sourceDocumentIndex
                                    .get(sourceDocumentId);

                            if (sourceDocument == null) {
                                // Document is not annotatable by this user, so we skip this result
                                continue;
                            }

                            AnnotationDocument annotationDocument = sourceAnnotationDocPairs
                                    .get(sourceDocument);

                            if (annotationDocument != null
                                    && IGNORE != annotationDocument.getState()) {
                                // Skip if the document is ignored for this user
                                log.trace("Skipping results from ignored document {}",
                                        sourceDocumentId);
                            }

                            // If the query is limited to a given document, skip any results
                            // which are not in the given document
                            Optional<SourceDocument> limitedToDocument = aRequest
                                    .getLimitedToDocument();
                            if (limitedToDocument.isPresent() && !Objects
                                    .equals(limitedToDocument.get().getId(), sourceDocumentId)) {
                                log.trace(
                                        "Query limited to document {}, skipping results for "
                                                + "document {}",
                                        limitedToDocument.get().getId(), sourceDocumentId);
                                continue;
                            }

                            if (matchInSourceDocument && annotationDocument != null) {
                                // Exclude result if the retrieved document is a sourcedocument
                                // AND it has a corresponding annotation document for this user
                                // AND the document is not ignored for this user
                                log.trace("Skipping results from indexed source document {} in"
                                        + "favor of results from the corresponding annotation "
                                        + "document", sourceDocumentId);
                                continue;
                            }
                            else if (annotationDocumentId != -1
                                    && !aRequest.getUser().getUsername().equals(user)) {
                                // Exclude result if the retrieved document is an annotation
                                // document (that is, annotationDocument != -1 and its username
                                // is different from the quering user
                                log.trace(
                                        "Skipping results from annotation document for user {} "
                                                + "which does not match the requested user {}",
                                        user, aRequest.getUser().getUsername());
                                continue;
                            }

                            // Retrieve document title
                            String documentTitle = document.get(FIELD_TITLE);

                            // String idValue = segmentReader.document(spans.docID())
                            // .getField(FIELD_ID).stringValue();
                            // log.debug("******** New doc {}-{}", + spans.docID(), idValue);

                            while (spans.nextStartPosition() != Spans.NO_MORE_POSITIONS) {
                                if (current < offset) {
                                    current++;
                                    continue;
                                }
                                if (current - offset + 1 > count) {
                                    break resultIteration;
                                }
                                current++;
                                int matchStart = spans.startPosition();
                                int matchEnd = spans.endPosition();

                                int windowStart = Math.max(matchStart - RESULT_WINDOW_SIZE, 0);
                                int windowEnd = matchEnd + RESULT_WINDOW_SIZE - 1;

                                // Retrieve all indexed objects within the matching range
                                List<MtasTokenString> tokens = mtasCodecInfo.getObjectsByPositions(
                                        FIELD_CONTENT, spans.docID(), windowStart, windowEnd);

                                tokens.sort(Comparator.comparing(MtasTokenString::getOffsetStart));

                                if (tokens.isEmpty()) {
                                    continue;
                                }

                                SearchResult result = new SearchResult();
                                StringBuilder resultText = new StringBuilder();
                                StringBuilder leftContext = new StringBuilder();
                                StringBuilder rightContext = new StringBuilder();
                                result.setDocumentId(sourceDocumentId);
                                result.setDocumentTitle(documentTitle);
                                result.setOffsetStart(tokens.stream()
                                        .filter(t -> t.getPositionStart() >= matchStart
                                                && t.getPositionEnd() < matchEnd)
                                        .mapToInt(MtasTokenString::getOffsetStart).min()
                                        .getAsInt());
                                result.setOffsetEnd(tokens.stream()
                                        .filter(t -> t.getPositionStart() >= matchStart
                                                && t.getPositionEnd() < matchEnd)
                                        .mapToInt(MtasTokenString::getOffsetEnd).max().getAsInt());
                                result.setTokenStart(matchStart);
                                result.setTokenLength(matchEnd - matchStart);
                                result.setReadOnly(annotationDocument != null
                                        && FINISHED.equals(annotationDocument.getState()));
                                result.setSelectedForAnnotation(!result.isReadOnly());

                                MtasTokenString prevToken = null;
                                for (MtasTokenString token : tokens) {
                                    if (!token.getPrefix().equals(DEFAULT_PREFIX)) {
                                        continue;
                                    }

                                    // When searching for an annotation, we don't get the matching
                                    // text back... not sure why...
                                    String tokenText = CodecUtil.termValue(token.getValue());
                                    if (tokenText == null) {
                                        continue;
                                    }

                                    if (token.getPositionStart() < matchStart) {
                                        fill(leftContext, prevToken, token);
                                        leftContext.append(tokenText);
                                    }
                                    else if (token.getPositionStart() >= matchEnd) {
                                        fill(rightContext, prevToken, token);
                                        rightContext.append(tokenText);
                                    }
                                    else {
                                        // Only add the whitespace to the match if we already have
                                        // added any text to the match - otherwise consider the
                                        // whitespace to be part of the left contex
                                        if (resultText.length() > 0) {
                                            fill(resultText, prevToken, token);
                                        }
                                        else {
                                            fill(leftContext, prevToken, token);
                                        }
                                        resultText.append(tokenText);
                                    }
                                    prevToken = token;
                                }
                                result.setText(resultText.toString());
                                result.setLeftContext(leftContext.toString());
                                result.setRightContext(rightContext.toString());

                                AnnotationLayer groupingLayer = aRequest.getAnnoationLayer();
                                AnnotationFeature groupingFeature = aRequest.getAnnotationFeature();

                                if (groupingLayer != null && groupingFeature != null) {
                                    List<String> featureValues = featureValuesAtMatch(tokens,
                                            matchStart, matchEnd, groupingLayer, groupingFeature);
                                    for (String featureValue : featureValues) {
                                        addToResults(results, featureValue, result);
                                    }
                                }
                                else {
                                    // if no annotation feature is specified group by document title
                                    addToResults(results, result.getDocumentTitle(), result);
                                }
                            }
                        }
                    }
                }
            }
            catch (Exception e) {
                log.error("Unable to process query results", e);
            }
        }
        return results;
    }

    private void addToResults(Map<String, List<SearchResult>> aResultsMap, String aKey,
            SearchResult aSearchResult)
    {
        if (aResultsMap.containsKey(aKey)) {
            aResultsMap.get(aKey).add(aSearchResult);
        }
        else {
            List<SearchResult> searchResultsForKey = new ArrayList<>();
            searchResultsForKey.add(aSearchResult);
            aResultsMap.put(aKey, searchResultsForKey);
        }
    }

    private List<String> featureValuesAtMatch(List<MtasTokenString> aTokens, int aMatchStart,
            int aMatchEnd, AnnotationLayer aAnnotationLayer, AnnotationFeature aAnnotationFeature)
    {
        Optional<FeatureIndexingSupport> fisOpt = featureIndexingSupportRegistry
                .getIndexingSupport(aAnnotationFeature);
        FeatureIndexingSupport fis;
        if (fisOpt.isPresent()) {
            fis = fisOpt.get();
        }
        else {
            log.error("No FeatureIndexingSupport found for feature " + aAnnotationFeature
                    + ". Using " + PrimitiveUimaIndexingSupport.class.getSimpleName()
                    + " to determine index name for the feature");
            fis = new PrimitiveUimaIndexingSupport(featureSupportRegistry);
        }

        // a feature prefix is currently only used for target and source of
        // relation-annotations however we just look at the feature value of the
        // relation-annotation itself here, so we can just use "" as feature prefix
        String groupingFeatureIndexName = fis.featureIndexName(aAnnotationLayer.getUiName(), "",
                aAnnotationFeature);

        List<String> featureValues = new ArrayList<>();

        IntSet fsAddresses = new IntOpenHashSet();
        aTokens.stream()
                .filter(t -> t.getPositionStart() == aMatchStart
                        && t.getPositionEnd() == aMatchEnd - 1
                        && t.getPrefix().equals(getIndexedName(groupingFeatureIndexName)) &&
                        // Handle stacked annotations
                        !fsAddresses.contains(decodeFSAddress(t.getPayload())))
                .forEach(t -> {
                    featureValues.add(t.getPostfix());
                    fsAddresses.add(decodeFSAddress(t.getPayload()));
                });
        // now we look for the annotations where the feature value for the grouping feature is empty
        aTokens.stream()
                .filter(t -> t.getPositionStart() == aMatchStart
                        && t.getPositionEnd() == aMatchEnd - 1
                        && t.getPrefix().equals(getIndexedName(aAnnotationLayer.getUiName()))
                        && !fsAddresses.contains(decodeFSAddress(t.getPayload())))
                .forEach(t -> featureValues.add(EMPTY_FEATURE_VALUE_KEY));
        return featureValues;
    }

    /**
     * If there is space between the previous token and the current token, then add the
     * corresponding amount of whitespace the the buffer.
     */
    private void fill(StringBuilder aBuffer, MtasTokenString aPrevToken, MtasTokenString aToken)
    {
        if (aPrevToken != null) {
            for (int g = aPrevToken.getOffsetEnd(); g < aToken.getOffsetStart(); g++) {
                aBuffer.append(' ');
            }
        }
    }

    private void indexDocument(String aDocumentTitle, long aSourceDocumentId,
            long aAnnotationDocumentId, String aUser, byte[] aBinaryCas)
        throws IOException
    {
        // Calculate timestamp that will be indexed
        String timestamp = DateTools.dateToString(new Date(), DateTools.Resolution.MILLISECOND);

        log.trace(
                "Indexing document in project [{}]({}). sourceId: {}, annotationId: {}, "
                        + "user: {} timestamp: {}",
                project.getName(), project.getId(), aSourceDocumentId, aAnnotationDocumentId, aUser,
                timestamp);

        IndexWriter indexWriter = getIndexWriter();

        // Prepare bytearray with document content to be indexed
        String encodedCAS = new String(MtasUtils.bytesToChars(aBinaryCas));

        // Create new Lucene document
        Document doc = new Document();

        // Add indexed fields
        doc.add(new StringField(FIELD_ID,
                String.valueOf(aSourceDocumentId) + "/" + String.valueOf(aAnnotationDocumentId),
                Field.Store.YES));
        doc.add(new StringField(FIELD_SOURCE_DOCUMENT_ID, String.valueOf(aSourceDocumentId),
                Field.Store.YES));
        doc.add(new StringField(FIELD_ANNOTATION_DOCUMENT_ID, String.valueOf(aAnnotationDocumentId),
                Field.Store.YES));
        doc.add(new StringField(FIELD_TITLE, aDocumentTitle, Field.Store.YES));
        doc.add(new StringField(FIELD_USER, aUser, Field.Store.YES));
        doc.add(new StringField(FIELD_TIMESTAMP, timestamp, Field.Store.YES));
        doc.add(new TextField(FIELD_CONTENT, encodedCAS, Field.Store.NO));

        // Add document to the Lucene index
        indexWriter.addDocument(doc);
    };

    /**
     * Remove document from the index
     * 
     * @param aSourceDocumentId
     *            The ID of the source document to be removed
     * @param aAnnotationDocumentId
     *            The ID of the annotation document to be removed
     * @param aUser
     *            The owner of the document to be removed
     */
    private void deindexDocument(long aSourceDocumentId, long aAnnotationDocumentId, String aUser)
        throws IOException
    {
        if (!isCreated()) {
            return;
        }

        log.trace(
                "Removing from index in project [{}]({}). sourceId: {}, annotationId: {}, user: {}",
                project.getName(), project.getId(), aSourceDocumentId, aAnnotationDocumentId,
                aUser);

        IndexWriter indexWriter = getIndexWriter();
        indexWriter.deleteDocuments(new Term(FIELD_ID,
                String.format("%d/%d", aSourceDocumentId, aAnnotationDocumentId)));
    }

    /**
     * Remove a specific document from the index based on its timestamp
     * 
     * @param aSourceDocumentId
     *            The ID of the source document to be removed
     * @param aAnnotationDocumentId
     *            The ID of the annotation document to be removed
     * @param aUser
     *            The owner of the document to be removed
     * @param aTimestamp
     *            The timestamp of the document to be removed
     */
    private void deindexDocument(long aSourceDocumentId, long aAnnotationDocumentId, String aUser,
            String aTimestamp)
        throws IOException
    {
        log.debug(
                "Removing document from index in project [{}]({}). sourceId: {}, "
                        + "annotationId: {}, user: {}, timestamp: {}",
                project.getName(), project.getId(), aSourceDocumentId, aAnnotationDocumentId, aUser,
                aTimestamp);

        IndexWriter indexWriter = getIndexWriter();

        // Prepare boolean query with the two obligatory terms (id and timestamp)
        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder().add(
                new TermQuery(new Term(FIELD_ID,
                        String.format("%d/%d", aSourceDocumentId, aAnnotationDocumentId))),
                BooleanClause.Occur.MUST).add(new TermQuery(new Term(FIELD_TIMESTAMP, aTimestamp)),
                        BooleanClause.Occur.MUST);

        // Delete document based on the previous query
        indexWriter.deleteDocuments(booleanQuery.build());
    }

    /**
     * Remove source document from the index
     * 
     * @param aDocument
     *            The document to be removed
     */
    @Override
    public void deindexDocument(SourceDocument aDocument) throws IOException
    {
        deindexDocument(aDocument.getId(), -1, "");
        scheduleCommit();
    }

    @Override
    public synchronized void clear() throws IOException
    {
        // Remove all data from the index
        IndexWriter indexWriter = getIndexWriter();
        indexWriter.deleteAll();

        // Close the index temporarily because we want the IndexWriter to be re-initialized on the
        // next access in order to pick up the current layer configuration of the project.
        closeIndex();
    }

    /**
     * Remove annotation document from the index
     * 
     * @param aDocument
     *            The document to be removed
     */
    @Override
    public void deindexDocument(AnnotationDocument aDocument) throws IOException
    {
        deindexDocument(aDocument.getDocument().getId(), aDocument.getId(), aDocument.getUser());
        scheduleCommit();
    }

    /**
     * Remove annotation document from the index based on its timestamp
     * 
     * @param aDocument
     *            The document to be removed
     */
    @Override
    public void deindexDocument(AnnotationDocument aDocument, String aTimestamp) throws IOException
    {
        deindexDocument(aDocument.getDocument().getId(), aDocument.getId(), aDocument.getUser(),
                aTimestamp);
        scheduleCommit();
    }

    /**
     * Returns a File object corresponding to the project's index folder
     * 
     * @return File object corresponding to project's index folder
     */
    private File getIndexDir()
    {
        return new File(repositoryDir, "/" + PROJECT_FOLDER + "/" + project.getId() + "/" + INDEX);
    }

    @Override
    public synchronized void delete() throws IOException
    {
        if (isOpen()) {
            close();
        }

        // Delete the index directory
        deleteDirectory(getIndexDir());

        log.debug("Index for project [{}]({}) has been deleted", project.getName(),
                project.getId());
    }

    @Override
    public boolean isCreated()
    {
        return getIndexDir().isDirectory();
    }

    @Override
    public Optional<String> getTimestamp(long aSrcDocId, long aAnnoDocId) throws IOException
    {
        Optional<String> result = Optional.empty();

        if (aSrcDocId == -1 || aAnnoDocId == -1) {
            return result;
        }

        // Prepare index searcher for accessing index
        ReferenceManager<IndexSearcher> searchManager = getSearcherManager();
        searchManager.maybeRefresh();
        IndexSearcher indexSearcher = searchManager.acquire();
        try {

            // Prepare query for the annotation document for this annotation document
            Term term = new Term(FIELD_ID, String.format("%d/%d", aSrcDocId, aAnnoDocId));

            TermQuery query = new TermQuery(term);

            // Do query
            TopDocs docs = indexSearcher.search(query, 1);

            if (docs.scoreDocs.length > 0) {
                // If there are results, retrieve first document, since all results should come
                // from the same document
                Document document = indexSearcher.doc(docs.scoreDocs[0].doc);

                // Retrieve the timestamp field if it exists
                if (document.getField(FIELD_TIMESTAMP) != null) {
                    result = Optional.ofNullable(StringUtils
                            .trimToNull(document.getField(FIELD_TIMESTAMP).stringValue()));
                }
            }
        }
        finally {
            if (indexSearcher != null) {
                searchManager.release(indexSearcher);
                indexSearcher = null;
            }
        }
        return result;
    }

    public Project getProject()
    {
        return project;
    }

    public Map<AnnotationLayer, List<AnnotationFeature>> getLayersAndFeaturesToIndex()
    {
        return layersAndFeatures;
    }

    public static MtasDocumentIndex getIndex(long aProjectId)
    {
        return OPEN_INDEXES.get(aProjectId);
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this).append("project", project).append("path", getIndexDir())
                .toString();
    }

    @FunctionalInterface
    private interface QueryRunner<T>
    {
        T run(IndexSearcher searcher, SearchQueryRequest aRequest, MtasSpanQuery q)
            throws Exception;
    }

    @Override
    public void indexDocument(AnnotationDocument aDocument, byte[] aBinaryCas) throws IOException
    {
        long srcDocId = aDocument.getDocument().getId();
        long annoDocId = aDocument.getId();
        String user = aDocument.getUser();

        // NOTE: Deleting and then re-indexing the annotation document could lead to
        // no results for this annotation document being returned while the
        // re-indexing is still in process. Therefore, we check if there is already
        // a version of the annotation document index, we obtain the timestamp of this
        // version, then we add the new version, and finally we remove the old version
        // as identified by the timestamp.
        Optional<String> oldTimestamp = getTimestamp(srcDocId, annoDocId);
        indexDocument(aDocument.getName(), srcDocId, annoDocId, user, aBinaryCas);
        if (oldTimestamp.isPresent()) {
            deindexDocument(srcDocId, annoDocId, user, oldTimestamp.get());
        }
        scheduleCommit();
    }

    @Override
    public void indexDocument(SourceDocument aSourceDocument, byte[] aBinaryCas) throws IOException
    {
        // NOTE: deleting all index versions related to the sourcedoc is ok in comparison to
        // re-indexing annotation documents, because we do this before the search
        // is accessed and therefore do not care about indices not being available for a short time
        deindexDocument(aSourceDocument.getId(), -1, "");
        indexDocument(aSourceDocument.getName(), aSourceDocument.getId(), -1, "", aBinaryCas);
        scheduleCommit();
    }
}

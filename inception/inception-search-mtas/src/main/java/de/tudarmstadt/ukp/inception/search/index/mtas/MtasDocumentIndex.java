/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
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
package de.tudarmstadt.ukp.inception.search.index.mtas;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IGNORE;
import static de.tudarmstadt.ukp.inception.search.Metrics.VIRTUAL_FEATURE_SENTENCE;
import static de.tudarmstadt.ukp.inception.search.Metrics.VIRTUAL_FEATURE_TOKEN;
import static de.tudarmstadt.ukp.inception.search.Metrics.VIRTUAL_LAYER_SEGMENTATION;
import static de.tudarmstadt.ukp.inception.search.index.mtas.MtasUimaParser.PARAM_PROJECT_ID;
import static de.tudarmstadt.ukp.inception.search.index.mtas.MtasUimaParser.getIndexedName;
import static de.tudarmstadt.ukp.inception.search.index.mtas.MtasUtils.decodeFSAddress;
import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingLong;
import static java.util.concurrent.TimeUnit.SECONDS;
import static mtas.analysis.util.MtasTokenizerFactory.ARGUMENT_PARSER;
import static mtas.analysis.util.MtasTokenizerFactory.ARGUMENT_PARSER_ARGS;
import static mtas.codec.MtasCodec.MTAS_CODEC_NAME;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.lang3.StringUtils.toRootLowerCase;
import static org.apache.lucene.search.ScoreMode.COMPLETE_NO_SCORES;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.tuple.Pair;
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
import org.apache.lucene.queries.spans.SpanWeight;
import org.apache.lucene.queries.spans.Spans;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ReferenceManager;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.openjson.JSONObject;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.search.ExecutionException;
import de.tudarmstadt.ukp.inception.search.FeatureIndexingSupport;
import de.tudarmstadt.ukp.inception.search.FeatureIndexingSupportRegistry;
import de.tudarmstadt.ukp.inception.search.LayerStatistics;
import de.tudarmstadt.ukp.inception.search.PrimitiveUimaIndexingSupport;
import de.tudarmstadt.ukp.inception.search.SearchQueryRequest;
import de.tudarmstadt.ukp.inception.search.SearchResult;
import de.tudarmstadt.ukp.inception.search.StatisticRequest;
import de.tudarmstadt.ukp.inception.search.StatisticsResult;
import de.tudarmstadt.ukp.inception.search.index.IndexRebuildRequiredException;
import de.tudarmstadt.ukp.inception.search.index.PhysicalIndex;
import de.tudarmstadt.ukp.inception.search.model.AnnotationSearchState;
import de.tudarmstadt.ukp.inception.search.model.BulkIndexingContext;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongComparators;
import mtas.analysis.token.MtasTokenString;
import mtas.analysis.util.MtasTokenizerFactory;
import mtas.codec.util.CodecComponent;
import mtas.codec.util.CodecInfo;
import mtas.codec.util.CodecUtil;
import mtas.codec.util.Status;
import mtas.codec.util.collector.MtasDataItem;
import mtas.parser.cql.MtasCQLParser;
import mtas.parser.cql.ParseException;
import mtas.search.spans.util.MtasSpanQuery;

/**
 * The Mtas implementation for a physical index
 */
public class MtasDocumentIndex
    implements PhysicalIndex
{
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

    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final FeatureIndexingSupportRegistry featureIndexingSupportRegistry;
    private final FeatureSupportRegistry featureSupportRegistry;
    private final DocumentService documentService;
    private final Project project;
    private final File indexDir;
    private final ScheduledExecutorService schedulerService;

    private IndexWriter _indexWriter;
    private ReferenceManager<IndexSearcher> _searcherManager;
    private ScheduledFuture<?> _commitFuture;

    public MtasDocumentIndex(Project aProject, DocumentService aDocumentService, File aIndexDir,
            FeatureIndexingSupportRegistry aFeatureIndexingSupportRegistry,
            FeatureSupportRegistry aFeatureSupportRegistry)
    {
        documentService = aDocumentService;
        project = aProject;
        featureIndexingSupportRegistry = aFeatureIndexingSupportRegistry;
        featureSupportRegistry = aFeatureSupportRegistry;
        indexDir = aIndexDir;

        schedulerService = new ScheduledThreadPoolExecutor(0);
    }

    private synchronized IndexWriter getIndexWriter() throws IOException
    {
        if (_indexWriter != null && _indexWriter.isOpen()) {
            return _indexWriter;
        }

        try {
            // After the index has been initialized, assign the _indexWriter - this is also used
            // by isOpen() to check if the index writer is available.
            _indexWriter = createIndexWriter();
            return _indexWriter;
        }
        catch (IOException | IllegalArgumentException e) {
            if (LOG.isDebugEnabled()) {
                LOG.warn("Unable to read MTAS index: {}. Deleting index so it can be rebuilt.",
                        e.getMessage(), e);
            }
            else {
                LOG.warn("Unable to read MTAS index: {}. Deleting index so it can be rebuilt.",
                        e.getMessage());
            }

            // If the index is corrupt, delete it so it can be rebuilt from scratch
            delete();

            _indexWriter = null;
            throw new IndexRebuildRequiredException(e);
        }
    }

    private IndexWriter createIndexWriter() throws IOException
    {
        // Add the project id to the configuration
        var jsonParserConfiguration = new JSONObject();
        jsonParserConfiguration.put(PARAM_PROJECT_ID, project.getId());

        // Tokenizer parameters
        var tokenizerArguments = new HashMap<String, String>();
        tokenizerArguments.put(ARGUMENT_PARSER, MtasUimaParser.class.getName());
        tokenizerArguments.put(ARGUMENT_PARSER_ARGS, jsonParserConfiguration.toString());

        // Build analyzer
        var mtasAnalyzer = CustomAnalyzer.builder()
                .withTokenizer(MtasTokenizerFactory.class, tokenizerArguments).build();

        var analyzerPerField = new HashMap<String, Analyzer>();
        analyzerPerField.put(FIELD_CONTENT, mtasAnalyzer);

        var analyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(), analyzerPerField);

        // Build IndexWriter
        FileUtils.forceMkdir(indexDir);
        var config = new IndexWriterConfig(analyzer);
        config.setCodec(Codec.forName(MTAS_CODEC_NAME));

        @SuppressWarnings("resource")
        var indexWriter = new IndexWriter(FSDirectory.open(indexDir.toPath()), config);

        // Initialize the index
        try {
            indexWriter.commit();
        }
        catch (IOException e) {
            try {
                indexWriter.close();
            }
            catch (IOException e1) {
                LOG.error("Error while trying to close index which could not be initialized"
                        + " - actual exception follows", e);
            }
            throw e;
        }

        return indexWriter;
    }

    private void ensureAllIsCommitted()
    {
        if (_commitFuture != null && !_commitFuture.isDone()) {
            try {
                _commitFuture.get();
            }
            catch (Exception e) {
                LOG.error(
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
                LOG.error("Error committing changes to index for project [{}]({})",
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

    private synchronized void closeIndex()
    {
        try {
            try {
                if (!isOpen()) {
                    return;
                }

                ensureAllIsCommitted();

                _indexWriter.close();
            }
            catch (IOException e) {
                LOG.error("Error closing index for project {}", project, e);
            }

            if (_searcherManager != null) {
                try {
                    _searcherManager.close();
                }
                catch (IOException e) {
                    LOG.error("Error closing index for project {}", project, e);
                }
            }
        }
        finally {
            _indexWriter = null;
            _searcherManager = null;
            LOG.debug("Closed index for project {}", project);
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

        LOG.debug("Enqueuing new future to index for project {}", project);

        _commitFuture = schedulerService.schedule(this::commit, 3, SECONDS);
    }

    private void commit()
    {
        try {
            LOG.debug("Executing future to index for project {}", project);
            if (_indexWriter != null && _indexWriter.isOpen()) {
                _indexWriter.commit();
                LOG.debug("Committed changes to index for project {}", project);

                if (_searcherManager != null) {
                    _searcherManager.maybeRefresh();
                }
            }
        }
        catch (IOException e) {
            LOG.error("Unable to commit to index of project {}", project);
        }
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
    public void open() throws IOException
    {
        getIndexWriter();
    }

    @Override
    public Map<String, List<SearchResult>> executeQuery(SearchQueryRequest aRequest,
            AnnotationSearchState aPrefs)
        throws IOException, ExecutionException
    {
        return _executeQuery(this::doQuery, aRequest, aPrefs);
    }

    @Override
    public long numberOfQueryResults(SearchQueryRequest aRequest, AnnotationSearchState aPrefs)
        throws ExecutionException, IOException
    {
        return _executeQuery(this::doCountResults, aRequest, aPrefs);
    }

    @Override
    public StatisticsResult getAnnotationStatistics(StatisticRequest aStatisticRequest)
        throws IOException, ExecutionException
    {
        var fullDocSet = getUniqueDocuments(aStatisticRequest);
        var allStats = new HashMap<String, LayerStatistics>();
        var nonNullStats = new HashMap<String, LayerStatistics>();
        var features = aStatisticRequest.getFeatures();

        for (var feature : features) {
            var layer = feature.getLayer();
            var searchString = "<" + MtasUimaParser.getIndexedName(layer.getUiName()) + "."
                    + MtasUimaParser.getIndexedName(feature.getUiName()) + "=\"\"/>";

            var results = getLayerStatistics(aStatisticRequest, searchString, fullDocSet);
            results.setFeature(feature);
            if (results.getMaximum() > 0) {
                nonNullStats.put(layer.getUiName() + "." + feature.getUiName(), results);
            }
            allStats.put(layer.getUiName() + "." + feature.getUiName(), results);
        }

        var rawText = new AnnotationLayer();
        rawText.setUiName(VIRTUAL_LAYER_SEGMENTATION);

        var token = new AnnotationFeature();
        token.setUiName(VIRTUAL_FEATURE_TOKEN);
        token.setLayer(rawText);

        var sentence = new AnnotationFeature();
        sentence.setUiName(VIRTUAL_FEATURE_SENTENCE);
        sentence.setLayer(rawText);

        var results = getLayerStatistics(aStatisticRequest, "<Token=\"\"/>", fullDocSet);

        results.setFeature(token);
        allStats.put(VIRTUAL_LAYER_SEGMENTATION + "." + VIRTUAL_FEATURE_TOKEN, results);
        nonNullStats.put(VIRTUAL_LAYER_SEGMENTATION + "." + VIRTUAL_FEATURE_TOKEN, results);

        results = getLayerStatistics(aStatisticRequest, "<s=\"\"/>", fullDocSet);
        results.setFeature(sentence);
        allStats.put(VIRTUAL_LAYER_SEGMENTATION + "." + VIRTUAL_FEATURE_SENTENCE, results);
        nonNullStats.put(VIRTUAL_LAYER_SEGMENTATION + "." + VIRTUAL_FEATURE_SENTENCE, results);

        return new StatisticsResult(aStatisticRequest, allStats, nonNullStats,
                aStatisticRequest.getFeatures());
    }

    @Override
    public List<Integer> getUniqueDocuments(StatisticRequest aRequest) throws IOException
    {
        IndexSearcher searcher = null;
        var sourceAnnotationDocPairs = documentService
                .listAnnotatableDocuments(aRequest.getProject(), aRequest.getUser());
        var sourceDocumentIndex = new HashMap<Long, SourceDocument>();
        sourceAnnotationDocPairs.entrySet().stream()
                .forEach(e -> sourceDocumentIndex.put(e.getKey().getId(), e.getKey()));

        var fullDocSet = new ArrayList<Integer>();

        try {
            searcher = getSearcherManager().acquire();
            var reader = searcher.getIndexReader();
            var storedFields = reader.storedFields();

            for (var i = 0; i < reader.maxDoc(); i++) {
                var rawSourceDocumentId = storedFields.document(i).get(FIELD_SOURCE_DOCUMENT_ID);
                var sourceDocumentId = Long.valueOf(rawSourceDocumentId);
                var sourceDocument = sourceDocumentIndex.get(sourceDocumentId);
                if (sourceDocument == null) {
                    // Document is not annotatable by this user, so we skip this result
                    continue;
                }

                var rawAnnotationDocumentId = storedFields.document(i)
                        .get(FIELD_ANNOTATION_DOCUMENT_ID);
                var annotationDocumentId = Long.valueOf(rawAnnotationDocumentId);
                var matchInSourceDocument = annotationDocumentId == -1L;
                var documentOwner = storedFields.document(i).get(FIELD_USER);

                var annotationDocument = sourceAnnotationDocPairs.get(sourceDocument);
                // If there is no annotation document or the annotation document is NEW, we use the
                // source document
                if (matchInSourceDocument && !isAnnotationDocumentIndexed(annotationDocument)) {
                    fullDocSet.add(i);
                }
                else if (documentOwner.equals(aRequest.getUser().getUsername())) {
                    fullDocSet.add(i);
                }
            }
        }
        finally {
            if (searcher != null) {
                // Releasing and setting to null per recommendation in JavaDoc of
                // release(searcher) method
                getSearcherManager().release(searcher);
                searcher = null;
            }
        }

        return fullDocSet;
    }

    private boolean isAnnotationDocumentIndexed(AnnotationDocument annotationDocument)
    {
        return annotationDocument != null
                && annotationDocument.getState() != AnnotationDocumentState.NEW;
    }

    private MtasSpanQuery parseQuery(String aQuery, AnnotationSearchState aPrefs)
        throws ExecutionException, IOException
    {
        final MtasSpanQuery mtasSpanQuery;
        try {
            var modifiedQuery = preprocessQuery(aQuery, aPrefs);
            try (var queryReader = new StringReader(modifiedQuery)) {
                var parser = new MtasCQLParser(queryReader);
                mtasSpanQuery = parser.parse(FIELD_CONTENT, DEFAULT_PREFIX, null, null, null);
            }
        }
        catch (ParseException | Error e) {
            // The exceptions thrown by the MTAS CQL Parser are inheriting from
            // java.lang.Error...
            throw new ExecutionException("Unable to parse query [" + aQuery + "]", e);
        }
        return mtasSpanQuery;
    }

    @Override
    public LayerStatistics getLayerStatistics(StatisticRequest aStatisticRequest,
            String aFeatureQuery, List<Integer> aFullDocSet)
        throws IOException, ExecutionException
    {
        IndexSearcher searcher = null;
        Map<String, Object> resultsMap = null;
        Map<String, Object> resultsMapSentence = null;

        // Map<Long, Long> annotatableDocuments = listAnnotatableDocuments(
        // aStatisticRequest.getProject(), aStatisticRequest.getUser());
        var minToken = aStatisticRequest.getMinTokenPerDoc() != Integer.MIN_VALUE
                ? (double) aStatisticRequest.getMinTokenPerDoc()
                : null;
        var maxToken = aStatisticRequest.getMaxTokenPerDoc() != Integer.MAX_VALUE
                ? (double) aStatisticRequest.getMaxTokenPerDoc()
                : null;
        try {
            searcher = getSearcherManager().acquire();
            var reader = searcher.getIndexReader();

            // what does this parameter do?
            var fullDocList = new ArrayList<Integer>();

            // there is no real documentation for using mtas directly in Java. This can help:
            // https://textexploration.github.io/mtas/installation_lucene.html
            // This describes the functionality better but only for solr:
            // https://textexploration.github.io/mtas/search_component_stats_spans.html

            // The ComponentField is a big container which can contain many stuff. The only relevant
            // things for us are statsSpanList and spanQueryList
            // Source Code:
            // https://github.com/textexploration/mtas/blob/96c7911e9c591c2bd4a419dbf0e2194813376776/src/main/java/mtas/codec/util/CodecComponent.java
            var fieldStats = new CodecComponent.ComponentField(FIELD_CONTENT);
            // The query is either an actual query (if the method is called from
            // getQueryStatistics in SearchService) or a mock query, e.g. "<Token=\"\"/>"
            // (if the method shall return statistics for a layer). The second query always
            // counts the sentences for per sentence statistics.
            var spanQuerys = new MtasSpanQuery[2];

            // we need two functions. Each function consists of 3 parts
            // 1. The key. This is just a unique identifier
            var functionKeys = new String[2];
            // 2. The actual instructions of the function
            var functions = new String[2];
            // 3. Which values the function shall output
            var functionTypes = new String[2];

            // Add the preprocessed query to the spanQueryList
            var parsedQuery = parseQuery(aFeatureQuery, aStatisticRequest.getSearchSettings());
            fieldStats.spanQueryList.add(parsedQuery);

            // maybe using a function for this simple case is too slow...
            spanQuerys[0] = parsedQuery;
            functionKeys[0] = "currentLayer";
            // This is a mock function (mathematically, the identity function).
            // q0 indicates the result of the first parsed query
            functions[0] = "$q0";
            // The output should be all the requested statistics
            functionTypes[0] = LayerStatistics.STATS;

            // A query which counts sentences
            var parsedSentQuery = parseQuery("<s=\"\"/>", aStatisticRequest.getSearchSettings());
            fieldStats.spanQueryList.add(parsedSentQuery);

            spanQuerys[1] = parsedSentQuery;
            functionKeys[1] = "perSentence";
            // q0 is the result of the actual query. q1 is the result of counting the sentences
            // We divide them using /
            functions[1] = "$q0/$q1";
            // The output should be all the requested statistics
            functionTypes[1] = LayerStatistics.STATS;

            // Now we are finished with the spanQueryList

            // Next we look at the statsSpanList
            // We add a ComponentSpan to the statsSpanList which is created using all the things
            // we defined above
            // The key is a unique but somewhat arbitrary identifier
            // minToken and maxToken indicate that only documents with a number of tokens bigger
            // than minToken and smaller than maxToken should be considered
            fieldStats.statsSpanList
                    .add(new CodecComponent.ComponentSpan(spanQuerys, "ax2", minToken, maxToken,
                            LayerStatistics.STATS, functionKeys, functions, functionTypes));

            // Now we are done with the preparations
            // Next we actually collect the data and do all the calculations
            // Only documents in aFullDocSet are considered
            // I do not know for what Status and fullDocList are needed...
            CodecUtil.collectField(FIELD_CONTENT, searcher, reader,
                    (ArrayList<Integer>) fullDocList, (ArrayList<Integer>) aFullDocSet, fieldStats,
                    new Status());
            // All that is left now is extracting the statistics from fieldStats
            // statsSpanList only has one entry and the first function is the perDocument statistics
            MtasDataItem<?, ?> results = fieldStats.statsSpanList.get(0).functions
                    .get(0).dataCollector.getResult().getData();

            resultsMap = results.rewrite(true);

            // The second function is the perSentence statistics
            MtasDataItem<?, ?> resultsPerSentence = fieldStats.statsSpanList.get(0).functions
                    .get(1).dataCollector.getResult().getData();

            resultsMapSentence = resultsPerSentence.rewrite(true);

            var noOfDocsLong = (Long) resultsMapSentence.get("n");
            var layerStats = new LayerStatistics((double) resultsMap.get("sum"),
                    (double) resultsMap.get("max"), (double) resultsMap.get("min"),
                    (double) resultsMap.get("mean"), (double) resultsMap.get("median"),
                    (double) resultsMap.get("standarddeviation"),
                    (double) resultsMapSentence.get("sum"), (double) resultsMapSentence.get("max"),
                    (double) resultsMapSentence.get("min"), (double) resultsMapSentence.get("mean"),
                    (double) resultsMapSentence.get("median"),
                    (double) resultsMapSentence.get("standarddeviation"),
                    noOfDocsLong.doubleValue());

            return layerStats;
        }
        catch (mtas.parser.function.ParseException e) {
            throw new ExecutionException("Search function could not be parsed!", e);
        }
        catch (IllegalAccessException e) {
            throw new ExecutionException("Collector could not access data!", e);
        }
        catch (InvocationTargetException e) {
            throw new ExecutionException("Problem with collecting the data!", e.getCause());
        }
        finally {
            if (searcher != null) {
                // Releasing and setting to null per recommendation in JavaDoc of
                // release(searcher)
                // method
                getSearcherManager().release(searcher);
                searcher = null;
            }
        }
    }

    private <T> T _executeQuery(QueryRunner<T> aRunner, SearchQueryRequest aRequest,
            AnnotationSearchState aPrefs)
        throws IOException, ExecutionException
    {
        LOG.debug("Executing query [{}] on index [{}]", aRequest, indexDir);

        if (aRequest.isCommitRequired()) {
            ensureAllIsCommitted();
        }

        final MtasSpanQuery query;
        try {
            var modifiedQuery = preprocessQuery(aRequest.getQuery(), aPrefs);
            try (var reader = new StringReader(modifiedQuery)) {
                var parser = new MtasCQLParser(reader);
                query = parser.parse(FIELD_CONTENT, DEFAULT_PREFIX, null, null, null);
            }
        }
        catch (ParseException | Error e) {
            // The exceptions thrown by the MTAS CQL Parser are inheriting from java.lang.Error...
            throw new ExecutionException("Unable to parse query [" + aRequest.getQuery() + "]", e);
        }
        catch (Exception e) {
            throw e;
        }

        IndexSearcher searcher = null;
        try {
            searcher = getSearcherManager().acquire();
            return aRunner.run(searcher, aRequest, query);
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

    private String preprocessQuery(String aQuery, AnnotationSearchState aPrefs)
    {
        if (aQuery.contains("\"") || aQuery.contains("[") || aQuery.contains("]")
                || aQuery.contains("<") || aQuery.contains(">")) {
            return aQuery;
        }

        // Convert raw words query to a Mtas CQP query
        var result = "";
        var words = BreakIterator.getWordInstance();
        words.setText(aQuery);

        int start = words.first();
        int end = words.next();
        while (end != BreakIterator.DONE) {
            var word = aQuery.substring(start, end);

            if (!aPrefs.isCaseSensitiveDocumentText()) {
                word = toRootLowerCase(word);
            }

            if (!word.trim().isEmpty()) {
                word = word.replace("&", "\\&");
                word = word.replace("(", "\\(");
                word = word.replace(")", "\\)");
                word = word.replace("#", "\\#");
                word = word.replace("{", "\\{");
                word = word.replace("}", "\\}");
                word = word.replace("<", "\\<");
                word = word.replace(">", "\\>");
                // Add the word to the query
                result += "\"" + word + "\"";
            }
            start = end;
            end = words.next();
            if (end != BreakIterator.DONE) {
                result += " ";
            }
        }

        return result;
    }

    private long doCountResults(IndexSearcher searcher, SearchQueryRequest aRequest,
            MtasSpanQuery q)
        throws IOException
    {
        var indexReader = searcher.getIndexReader();
        var leafReaderContextIterator = indexReader.leaves().listIterator();

        var annotatableDocuments = documentService.listAnnotatableDocuments(aRequest.getProject(),
                aRequest.getUser());
        var sourceDocumentIndex = new HashMap<Long, SourceDocument>();
        annotatableDocuments.entrySet().stream()
                .forEach(e -> sourceDocumentIndex.put(e.getKey().getId(), e.getKey()));

        final var boost = 0;
        var spanweight = q.rewrite(indexReader).createWeight(searcher, COMPLETE_NO_SCORES, boost);

        var numResults = 0;
        var limitedToDocument = aRequest.getLimitedToDocument();

        while (leafReaderContextIterator.hasNext()) {
            var leafReaderContext = leafReaderContextIterator.next();
            try {
                var spans = spanweight.getSpans(leafReaderContext, SpanWeight.Postings.POSITIONS);
                var segmentReader = (SegmentReader) leafReaderContext.reader();
                var storedFields = segmentReader.storedFields();

                if (spans == null) {
                    continue;
                }

                while (spans.nextDoc() != Spans.NO_MORE_DOCS) {
                    if (segmentReader.numDocs() == segmentReader.maxDoc()
                            || segmentReader.getLiveDocs().get(spans.docID())) {
                        var document = storedFields.document(spans.docID());

                        // Retrieve user
                        var user = document.get(FIELD_USER);

                        // Retrieve source and annotation document ids
                        var rawSourceDocumentId = document.get(FIELD_SOURCE_DOCUMENT_ID);
                        var rawAnnotationDocumentId = document.get(FIELD_ANNOTATION_DOCUMENT_ID);
                        if (!validSourceAndDocumentIds(rawSourceDocumentId,
                                rawAnnotationDocumentId)) {
                            continue;
                        }

                        var sourceDocumentId = Long.valueOf(rawSourceDocumentId);
                        var annotationDocumentId = Long.valueOf(rawAnnotationDocumentId);
                        var sourceDocument = sourceDocumentIndex.get(sourceDocumentId);
                        var annotationDocument = annotatableDocuments.get(sourceDocument);

                        if (sourceDocument == null) {
                            // Document is not annotatable by this user, so we skip this result
                            continue;
                        }

                        if (annotationDocument != null && IGNORE == annotationDocument.getState()) {
                            LOG.trace("Skipping results from ignored document {}", sourceDocument);
                        }

                        if (!isWithinDocumentScope(limitedToDocument, sourceDocument)) {
                            continue;
                        }

                        var matchInSourceDocument = annotationDocumentId == -1L;
                        if (matchInSourceDocument
                                && isAnnotationDocumentIndexed(annotationDocument)) {
                            // Exclude result if the retrieved document is a sourcedocument
                            // (that is, has annotationDocument = -1) AND it has a
                            // corresponding annotation document for this user
                            LOG.trace("Skipping results from indexed source document {} in"
                                    + "favor of results from the corresponding annotation "
                                    + "document", sourceDocument);
                            continue;
                        }

                        if (!matchInSourceDocument
                                && !aRequest.getUser().getUsername().equals(user)) {
                            // Exclude result if the retrieved document is an annotation
                            // document (that is, annotationDocument != -1 and its username
                            // is different from the querying user
                            LOG.trace(
                                    "Skipping results from annotation document for user [{}] "
                                            + "which does not match the requested user [{}]",
                                    user, aRequest.getUser().getUsername());
                            continue;
                        }

                        while (spans.nextStartPosition() != Spans.NO_MORE_POSITIONS) {
                            numResults++;
                        }
                    }
                }
            }
            catch (Exception e) {
                LOG.error("Unable to process query results", e);
                numResults = -1;
            }
        }
        return numResults;
    }

    private boolean validSourceAndDocumentIds(String aRawSourceDocumentId,
            String aRawAnnotationDocumentId)
    {
        if (aRawSourceDocumentId == null || aRawAnnotationDocumentId == null) {
            LOG.trace(
                    "Indexed document lacks source/annotation document IDs"
                            + " - source: {}, annotation: {}",
                    aRawSourceDocumentId, aRawAnnotationDocumentId);
            return false;
        }

        return true;
    }

    private boolean isWithinDocumentScope(Optional<SourceDocument> limitedToDocument,
            SourceDocument aDocument)
    {
        if (limitedToDocument.isEmpty()) {
            return true;
        }

        if (!Objects.equals(limitedToDocument.get().getId(), aDocument.getId())) {
            LOG.trace("Query limited to document [{}], skipping results for document {}",
                    limitedToDocument.get().getId(), aDocument);
            return false;
        }

        return true;
    }

    private List<LeafReaderContext> sortLeaves(List<LeafReaderContext> aLeaves,
            IndexSearcher aSearcher, MtasSpanQuery aQuery)
        throws IOException
    { // This method sorts the LeafReaderContexts according to the document ids
      // they contain. If one does not contain a document for this search, then
      // it is discarded. If one contains multiple document ids the smallest
      // is used for comparison.

        List<Pair<LeafReaderContext, List<Long>>> mapToDocIds = new ArrayList<>();

        // Here, the query comes into play
        var spanweight = aQuery.rewrite(aSearcher.getIndexReader()).createWeight(aSearcher,
                COMPLETE_NO_SCORES, 0);

        // cycle through all the leaves
        for (var leafReaderContext : aLeaves) {
            var spans = spanweight.getSpans(leafReaderContext, SpanWeight.Postings.POSITIONS);
            var segmentReader = (SegmentReader) leafReaderContext.reader();
            var storedFields = segmentReader.storedFields();
            var idList = new LongArrayList();
            // no spans -> no docs
            if (spans != null) {
                // go through the docs in iterator span
                while (spans.nextDoc() != Spans.NO_MORE_DOCS) {
                    // don't know why this if is needed, just copy/pasted it from method doQuery
                    // below
                    if (segmentReader.numDocs() == segmentReader.maxDoc()
                            || segmentReader.getLiveDocs().get(spans.docID())) {
                        // get a document
                        var document = storedFields.document(spans.docID());

                        var rawSourceDocumentId = document.get(FIELD_SOURCE_DOCUMENT_ID);
                        // go to the next document if the docId is not set
                        if (rawSourceDocumentId == null) {
                            continue;
                        }
                        // add id to the list of ids for this leafReaderContext
                        idList.add(Long.parseLong(rawSourceDocumentId));
                    }
                }
            }
            idList.sort(LongComparators.NATURAL_COMPARATOR);
            if (!idList.isEmpty()) {
                mapToDocIds.add(Pair.of(leafReaderContext, idList));
            }
        }
        // Sort according to docId; take the smallest value in the list
        mapToDocIds.sort(comparingLong(s -> s.getValue().get(0)));

        // Only return the leaves
        return mapToDocIds.stream().map(Pair::getKey).collect(Collectors.toList());
    }

    private Map<String, List<SearchResult>> doQuery(IndexSearcher aSearcher,
            SearchQueryRequest aRequest, MtasSpanQuery aQuery)
        throws IOException
    {
        var resultsByDocument = new LinkedHashMap<String, List<SearchResult>>();

        var leafReaderContextIterator = sortLeaves(aSearcher.getIndexReader().leaves(), aSearcher,
                aQuery).listIterator();

        var sourceAnnotationDocPairs = documentService
                .listAnnotatableDocuments(aRequest.getProject(), aRequest.getUser());
        var sourceDocumentIndex = new HashMap<Long, SourceDocument>();
        sourceAnnotationDocPairs.entrySet().stream()
                .forEach(e -> sourceDocumentIndex.put(e.getKey().getId(), e.getKey()));

        final var boost = 0;
        var spanweight = aQuery.rewrite(aSearcher.getIndexReader()).createWeight(aSearcher,
                COMPLETE_NO_SCORES, boost);

        var offset = aRequest.getOffset();
        var count = aRequest.getCount();
        var current = 0;
        var limitedToDocument = aRequest.getLimitedToDocument();

        resultIteration: while (leafReaderContextIterator.hasNext()) {
            var leafReaderContext = leafReaderContextIterator.next();

            try {
                var spans = spanweight.getSpans(leafReaderContext, SpanWeight.Postings.POSITIONS);
                if (spans == null) {
                    continue;
                }

                var segmentReader = (SegmentReader) leafReaderContext.reader();
                var storedFields = segmentReader.storedFields();
                var terms = segmentReader.terms(FIELD_CONTENT);
                var mtasCodecInfo = CodecInfo.getCodecInfoFromTerms(terms);
                while (spans.nextDoc() != Spans.NO_MORE_DOCS) {
                    if (segmentReader.numDocs() == segmentReader.maxDoc()
                            || segmentReader.getLiveDocs().get(spans.docID())) {
                        var document = storedFields.document(spans.docID());

                        // Retrieve user
                        var user = document.get(FIELD_USER);

                        // Retrieve source and annotation document ids
                        var rawSourceDocumentId = document.get(FIELD_SOURCE_DOCUMENT_ID);
                        var rawAnnotationDocumentId = document.get(FIELD_ANNOTATION_DOCUMENT_ID);
                        if (!validSourceAndDocumentIds(rawSourceDocumentId,
                                rawAnnotationDocumentId)) {
                            continue;
                        }

                        var sourceDocumentId = Long.valueOf(rawSourceDocumentId);
                        var annotationDocumentId = Long.valueOf(rawAnnotationDocumentId);
                        var matchInSourceDocument = annotationDocumentId == -1;

                        var sourceDocument = sourceDocumentIndex.get(sourceDocumentId);
                        if (sourceDocument == null) {
                            // Document is not annotatable by this user, so we skip this result
                            continue;
                        }

                        var annotationDocument = sourceAnnotationDocPairs.get(sourceDocument);

                        if (annotationDocument != null && IGNORE == annotationDocument.getState()) {
                            LOG.trace("Skipping results from ignored document {}", sourceDocument);
                        }

                        if (!isWithinDocumentScope(limitedToDocument, sourceDocument)) {
                            continue;
                        }

                        if (matchInSourceDocument
                                && isAnnotationDocumentIndexed(annotationDocument)) {
                            LOG.trace("Skipping results from indexed source document {} in"
                                    + "favor of results from the corresponding annotation "
                                    + "document", sourceDocument);
                            continue;
                        }

                        if (!matchInSourceDocument
                                && !aRequest.getUser().getUsername().equals(user)) {
                            LOG.trace(
                                    "Skipping results from annotation document for user {} "
                                            + "which does not match the requested user {}",
                                    user, aRequest.getUser().getUsername());
                            continue;
                        }

                        // Retrieve document title
                        var documentTitle = document.get(FIELD_TITLE);

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
                            var matchStart = spans.startPosition();
                            var matchEnd = spans.endPosition();

                            var windowStart = Math.max(matchStart - RESULT_WINDOW_SIZE, 0);
                            var windowEnd = matchEnd + RESULT_WINDOW_SIZE - 1;

                            // Retrieve all indexed objects within the matching range
                            var tokens = mtasCodecInfo.getObjectsByPositions(FIELD_CONTENT,
                                    spans.docID(), windowStart, windowEnd);

                            tokens.sort(comparing(MtasTokenString::getOffsetStart));

                            if (tokens.isEmpty()) {
                                continue;
                            }

                            var result = new SearchResult();
                            var resultText = new StringBuilder();
                            var leftContext = new StringBuilder();
                            var rightContext = new StringBuilder();
                            result.setDocumentId(sourceDocumentId);
                            result.setDocumentTitle(documentTitle);
                            result.setOffsetStart(tokens.stream()
                                    .filter(t -> t.getPositionStart() >= matchStart
                                            && t.getPositionEnd() < matchEnd)
                                    .mapToInt(MtasTokenString::getOffsetStart).min()
                                    .orElse(matchStart));
                            result.setOffsetEnd(tokens.stream()
                                    .filter(t -> t.getPositionStart() >= matchStart
                                            && t.getPositionEnd() < matchEnd)
                                    .mapToInt(MtasTokenString::getOffsetEnd).max()
                                    .orElse(matchEnd));
                            result.setTokenStart(matchStart);
                            result.setTokenLength(matchEnd - matchStart);
                            result.setReadOnly(annotationDocument != null
                                    && FINISHED.equals(annotationDocument.getState()));
                            result.setSelectedForAnnotation(!result.isReadOnly());

                            MtasTokenString prevToken = null;
                            for (var token : tokens) {
                                if (!token.getPrefix().equals(DEFAULT_PREFIX)) {
                                    continue;
                                }

                                // When searching for an annotation, we don't get the matching
                                // text back... not sure why...
                                var tokenText = CodecUtil.termValue(token.getValue());
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
                                    // whitespace to be part of the left context
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

                            var groupingLayer = aRequest.getAnnoationLayer();
                            var groupingFeature = aRequest.getAnnotationFeature();

                            if (groupingLayer != null && groupingFeature != null) {
                                var featureValues = featureValuesAtMatch(tokens, matchStart,
                                        matchEnd, groupingLayer, groupingFeature);
                                for (var featureValue : featureValues) {
                                    addToResults(resultsByDocument, featureValue, result);
                                }
                            }
                            else {
                                // if no annotation feature is specified group by document title
                                addToResults(resultsByDocument, result.getDocumentTitle(), result);
                            }
                        }
                    }
                }
            }
            catch (Exception e) {
                LOG.error("Unable to process query results", e);
            }
        }

        var sortedResultsByDocument = new LinkedHashMap<String, List<SearchResult>>();
        var sortedDocuments = resultsByDocument.keySet().stream().sorted().toList();
        for (var document : sortedDocuments) {
            sortedResultsByDocument.put(document, resultsByDocument.get(document));
        }

        return sortedResultsByDocument;
    }

    private void addToResults(Map<String, List<SearchResult>> aResultsMap, String aKey,
            SearchResult aSearchResult)
    {
        var results = aResultsMap.computeIfAbsent(aKey, $ -> new ArrayList<SearchResult>());
        results.add(aSearchResult);
    }

    private List<String> featureValuesAtMatch(List<MtasTokenString> aTokens, int aMatchStart,
            int aMatchEnd, AnnotationLayer aAnnotationLayer, AnnotationFeature aAnnotationFeature)
    {
        var fisOpt = featureIndexingSupportRegistry.getIndexingSupport(aAnnotationFeature);
        FeatureIndexingSupport fis;
        if (fisOpt.isPresent()) {
            fis = fisOpt.get();
        }
        else {
            LOG.error("No FeatureIndexingSupport found for feature " + aAnnotationFeature
                    + ". Using " + PrimitiveUimaIndexingSupport.class.getSimpleName()
                    + " to determine index name for the feature");
            fis = new PrimitiveUimaIndexingSupport(featureSupportRegistry);
        }

        // a feature prefix is currently only used for target and source of
        // relation-annotations however we just look at the feature value of the
        // relation-annotation itself here, so we can just use "" as feature prefix
        var groupingFeatureIndexName = fis.featureIndexName(aAnnotationLayer.getUiName(), "",
                aAnnotationFeature);

        List<String> featureValues = new ArrayList<>();

        var fsAddresses = new IntOpenHashSet();
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

    private String indexDocument(String aDocumentTitle, long aSourceDocumentId,
            long aAnnotationDocumentId, String aUser, byte[] aBinaryCas)
        throws IOException
    {
        // Calculate timestamp that will be indexed
        var timestamp = DateTools.dateToString(new Date(), DateTools.Resolution.MILLISECOND);

        LOG.debug(
                "Indexing document in project [{}]({}). sourceId: {}, annotationId: {}, "
                        + "user: {} timestamp: {}",
                project.getName(), project.getId(), aSourceDocumentId, aAnnotationDocumentId, aUser,
                timestamp);

        // Prepare bytearray with document content to be indexed
        var encodedCAS = new String(MtasUtils.bytesToChars(aBinaryCas));

        // Create new Lucene document
        var doc = new Document();

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
        var indexWriter = getIndexWriter();
        indexWriter.addDocument(doc);

        return timestamp;
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

        LOG.debug(
                "Removing from index in project [{}]({}). sourceId: {}, annotationId: {}, user: {}",
                project.getName(), project.getId(), aSourceDocumentId, aAnnotationDocumentId,
                aUser);

        var indexWriter = getIndexWriter();
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
        LOG.debug(
                "Removing document from index in project [{}]({}). sourceId: {}, "
                        + "annotationId: {}, user: {}, timestamp: {}",
                project.getName(), project.getId(), aSourceDocumentId, aAnnotationDocumentId, aUser,
                aTimestamp);

        var indexWriter = getIndexWriter();

        // Prepare boolean query with the two obligatory terms (id and timestamp)
        var booleanQuery = new BooleanQuery.Builder() //
                .add(new TermQuery(new Term(FIELD_ID,
                        String.format("%d/%d", aSourceDocumentId, aAnnotationDocumentId))),
                        BooleanClause.Occur.MUST) //
                .add(new TermQuery(new Term(FIELD_TIMESTAMP, aTimestamp)),
                        BooleanClause.Occur.MUST);

        // Delete document based on the previous query
        indexWriter.deleteDocuments(booleanQuery.build());
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
     * @param aCurrentVersion
     *            The timestamp of the document to be kept
     */
    private void deindexOldVersionsOfDocument(long aSourceDocumentId, long aAnnotationDocumentId,
            String aUser, String aCurrentVersion)
        throws IOException
    {
        LOG.debug(
                "Removing old versions of document from index in project [{}]({}). sourceId: {}, "
                        + "annotationId: {}, user: {}, current timestamp: {}",
                project.getName(), project.getId(), aSourceDocumentId, aAnnotationDocumentId, aUser,
                aCurrentVersion);

        var indexWriter = getIndexWriter();

        // Prepare boolean query with the two obligatory terms (id and timestamp)
        var booleanQuery = new BooleanQuery.Builder() //
                .add(new TermQuery(new Term(FIELD_ID,
                        String.format("%d/%d", aSourceDocumentId, aAnnotationDocumentId))),
                        BooleanClause.Occur.MUST) //
                .add(new TermQuery(new Term(FIELD_TIMESTAMP, aCurrentVersion)),
                        BooleanClause.Occur.MUST_NOT);

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
        var indexWriter = getIndexWriter();
        indexWriter.deleteAll();
        ensureAllIsCommitted();
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
    @Deprecated
    @Override
    public void deindexDocument(AnnotationDocument aDocument, String aTimestamp) throws IOException
    {
        deindexDocument(aDocument.getDocument().getId(), aDocument.getId(), aDocument.getUser(),
                aTimestamp);
        scheduleCommit();
    }

    @Override
    public synchronized void delete() throws IOException
    {
        if (isOpen()) {
            close();
        }

        // Delete the index directory
        deleteDirectory(indexDir);

        LOG.debug("Index for project {} has been deleted", project);
    }

    @Override
    public boolean isCreated()
    {
        return indexDir.isDirectory();
    }

    @Override
    public Optional<String> getTimestamp(long aSrcDocId, long aAnnoDocId) throws IOException
    {
        if (aSrcDocId == -1 || aAnnoDocId == -1) {
            return Optional.empty();
        }

        Optional<String> result = Optional.empty();

        // Prepare index searcher for accessing index
        var searchManager = getSearcherManager();
        searchManager.maybeRefresh();
        var indexSearcher = searchManager.acquire();
        var storedFields = indexSearcher.storedFields();
        try {
            // Prepare query for the annotation document for this annotation document
            var term = new Term(FIELD_ID, String.format("%d/%d", aSrcDocId, aAnnoDocId));

            var query = new TermQuery(term);

            // Do query
            var docs = indexSearcher.search(query, 1);

            if (docs.scoreDocs.length > 0) {
                // If there are results, retrieve first document, since all results should come
                // from the same document
                var document = storedFields.document(docs.scoreDocs[0].doc);

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

    @Override
    public String toString()
    {
        return new ToStringBuilder(this).append("project", project).append("path", indexDir)
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
        var srcDocId = aDocument.getDocument().getId();
        var annoDocId = aDocument.getId();
        var user = aDocument.getUser();

        // NOTE: Deleting and then re-indexing the annotation document could lead to
        // no results for this annotation document being returned while the
        // re-indexing is still in process. Therefore, we check if there is already
        // a version of the annotation document index, we obtain the timestamp of this
        // version, then we add the new version, and finally we remove the old version
        // as identified by the timestamp.
        // Optional<String> oldTimestamp = Optional.empty();
        // if (!BulkIndexingContext.isFullReindexInProgress()) {
        // // Looking up the timestamp is slow (because it requires refreshing the searcher to
        // // get the latest info) and when we do a full index rebuild, it is just slowing things
        // // down unnecessarily.
        // oldTimestamp = getTimestamp(srcDocId, annoDocId);
        // }

        var currentTimestamp = indexDocument(aDocument.getName(), srcDocId, annoDocId, user,
                aBinaryCas);

        deindexOldVersionsOfDocument(srcDocId, annoDocId, user, currentTimestamp);

        // if (oldTimestamp.isPresent()) {
        // deindexDocument(srcDocId, annoDocId, user, oldTimestamp.get());
        // }

        scheduleCommit();
    }

    @Override
    public void indexDocument(SourceDocument aSourceDocument, byte[] aBinaryCas) throws IOException
    {
        // NOTE: deleting all index versions related to the sourcedoc is ok in comparison to
        // re-indexing annotation documents, because we do this before the search
        // is accessed and therefore do not care about indices not being available for a short time
        if (!BulkIndexingContext.isFullReindexInProgress()) {
            deindexDocument(aSourceDocument.getId(), -1, "");
        }

        indexDocument(aSourceDocument.getName(), aSourceDocument.getId(), -1, "", aBinaryCas);
        scheduleCommit();
    }
}

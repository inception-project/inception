/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene.impl;

import static org.eclipse.rdf4j.sail.lucene.LuceneSail.FUZZY_PREFIX_LENGTH_KEY;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.document.LatLonShape;
import org.apache.lucene.document.ShapeField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.geo.Line;
import org.apache.lucene.geo.Polygon;
import org.apache.lucene.geo.Rectangle;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.StoredFieldVisitor;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.spatial.SpatialStrategy;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTreeFactory;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Bits;
import org.eclipse.rdf4j.common.iterator.EmptyIterator;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.GEOF;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lucene.AbstractLuceneIndex;
import org.eclipse.rdf4j.sail.lucene.AbstractReaderMonitor;
import org.eclipse.rdf4j.sail.lucene.BulkUpdater;
import org.eclipse.rdf4j.sail.lucene.DocumentDistance;
import org.eclipse.rdf4j.sail.lucene.DocumentResult;
import org.eclipse.rdf4j.sail.lucene.DocumentScore;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.lucene.QuerySpec;
import org.eclipse.rdf4j.sail.lucene.SearchDocument;
import org.eclipse.rdf4j.sail.lucene.SearchFields;
import org.eclipse.rdf4j.sail.lucene.SimpleBulkUpdater;
import org.eclipse.rdf4j.sail.lucene.util.GeoUnits;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.SpatialContextFactory;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Shape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 * A LuceneIndex is a one-stop-shop abstraction of a Lucene index. It takes care of proper
 * synchronization of IndexReaders, IndexWriters and IndexSearchers in a way that is suitable for a
 * LuceneSail.
 *
 * @see LuceneSail
 */
public class LuceneIndex
    extends AbstractLuceneIndex
{

    static {
        // do NOT set this to Integer.MAX_VALUE, because this breaks fuzzy
        // queries
        BooleanQuery.setMaxClauseCount(1024 * 1024);
    }

    private static final String GEO_FIELD_PREFIX = "_geo_";
    private static final String POINT_FIELD_PREFIX = "_pt_";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * The Directory that holds the Lucene index files.
     */
    private volatile Directory directory;

    /**
     * The Analyzer used to tokenize strings and queries.
     */
    private volatile Analyzer analyzer;

    private volatile Analyzer queryAnalyzer;

    private volatile Similarity similarity;

    private volatile int fuzzyPrefixLength;

    /**
     * The IndexWriter that can be used to alter the index' contents. Created lazily.
     */
    private volatile IndexWriter indexWriter;

    /**
     * This holds IndexReader and IndexSearcher.
     */
    protected volatile ReaderMonitor currentMonitor;

    private volatile Function<? super String, ? extends SpatialStrategy> geoStrategyMapper;

    private final AtomicBoolean closed = new AtomicBoolean(false);

    public LuceneIndex()
    {
    }

    /**
     * Constructor for keeping backwards compatibility.
     *
     * @param directory
     * @param analyzer
     * @throws IOException
     */
    public LuceneIndex(Directory directory, Analyzer analyzer) throws IOException
    {
        this(directory, analyzer, new ClassicSimilarity());
    }

    /**
     * Creates a new LuceneIndex.
     *
     * @param directory
     *            The Directory in which an index can be found and/or in which index files are
     *            written.
     * @param analyzer
     *            The Analyzer that will be used for tokenizing strings to index and queries.
     * @param similarity
     *            The Similarity that will be used for scoring.
     * @throws IOException
     *             When the Directory could not be unlocked.
     */
    public LuceneIndex(Directory directory, Analyzer analyzer, Similarity similarity)
        throws IOException
    {
        this.directory = directory;
        this.analyzer = analyzer;
        this.queryAnalyzer = analyzer;
        this.similarity = similarity;
        this.geoStrategyMapper = createSpatialStrategyMapper(
                Collections.<String, String> emptyMap());

        postInit();
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized void initialize(Properties parameters) throws Exception
    {
        super.initialize(parameters);
        this.directory = createDirectory(parameters);
        this.analyzer = createAnalyzer(parameters);
        this.queryAnalyzer = createQueryAnalyzer(parameters);
        this.similarity = createSimilarity(parameters);
        // slightly hacky cast to cope with the fact that Properties is
        // Map<Object,Object>
        // even though it is effectively Map<String,String>
        this.geoStrategyMapper = createSpatialStrategyMapper(
                (Map<String, String>) (Map<?, ?>) parameters);

        if (parameters.containsKey(FUZZY_PREFIX_LENGTH_KEY)) {
            this.fuzzyPrefixLength = NumberUtils
                    .toInt(parameters.getProperty(FUZZY_PREFIX_LENGTH_KEY), 0);
        }

        postInit();
    }

    protected Directory createDirectory(Properties parameters) throws IOException
    {
        Directory dir;
        if (parameters.containsKey(LuceneSail.LUCENE_DIR_KEY)) {
            dir = FSDirectory.open(Paths.get(parameters.getProperty(LuceneSail.LUCENE_DIR_KEY)));
        }
        else if (parameters.containsKey(LuceneSail.LUCENE_RAMDIR_KEY)
                && "true".equals(parameters.getProperty(LuceneSail.LUCENE_RAMDIR_KEY))) {
            dir = new ByteBuffersDirectory();
        }
        else {
            throw new IOException("No luceneIndex set, and no '" + LuceneSail.LUCENE_DIR_KEY
                    + "' or '" + LuceneSail.LUCENE_RAMDIR_KEY + "' parameter given. ");
        }
        return dir;
    }

    protected Analyzer createAnalyzer(Properties parameters) throws Exception
    {
        return createAnalyzerWithFallback(parameters, LuceneSail.ANALYZER_CLASS_KEY,
                StandardAnalyzer::new);
    }

    protected Analyzer createQueryAnalyzer(Properties parameters) throws Exception
    {
        return createAnalyzerWithFallback(parameters, LuceneSail.QUERY_ANALYZER_CLASS_KEY,
                StandardAnalyzer::new);
    }

    private Analyzer createAnalyzerWithFallback(Properties parameters, String parameterKey,
            Supplier<Analyzer> fallback)
        throws Exception
    {
        Analyzer a;
        if (parameters.containsKey(parameterKey)) {
            a = (Analyzer) Class.forName(parameters.getProperty(LuceneSail.ANALYZER_CLASS_KEY))
                    .getDeclaredConstructor().newInstance();
        }
        else {
            a = fallback.get();
        }
        return a;
    }

    protected Similarity createSimilarity(Properties parameters) throws Exception
    {
        Similarity s;
        if (parameters.containsKey(LuceneSail.SIMILARITY_CLASS_KEY)) {
            s = (Similarity) Class.forName(parameters.getProperty(LuceneSail.SIMILARITY_CLASS_KEY))
                    .newInstance();
        }
        else {
            s = new ClassicSimilarity();
        }
        return s;
    }

    private void postInit() throws IOException
    {
        // do some initialization for new indices
        if (!DirectoryReader.indexExists(directory)) {
            logger.debug("creating new Lucene index in directory {}", directory);
            IndexWriterConfig indexWriterConfig = getIndexWriterConfig();
            indexWriterConfig.setOpenMode(OpenMode.CREATE);
            IndexWriter writer = new IndexWriter(directory, indexWriterConfig);
            writer.close();
        }
    }

    protected Function<String, ? extends SpatialStrategy> createSpatialStrategyMapper(
            Map<String, String> parameters)
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        SpatialContext geoContext = SpatialContextFactory.makeSpatialContext(parameters,
                classLoader);
        final SpatialPrefixTree spt = SpatialPrefixTreeFactory.makeSPT(parameters, classLoader,
                geoContext);
        return (String field) -> new RecursivePrefixTreeStrategy(spt, GEO_FIELD_PREFIX + field);
    }

    @Override
    protected SpatialContext getSpatialContext(String property)
    {
        return geoStrategyMapper.apply(property).getSpatialContext();
    }

    // //////////////////////////////// Setters and getters

    public Directory getDirectory()
    {
        return directory;
    }

    public Analyzer getAnalyzer()
    {
        return analyzer;
    }

    @VisibleForTesting
    Analyzer getQueryAnalyzer()
    {
        return analyzer;
    }

    public Function<? super String, ? extends SpatialStrategy> getSpatialStrategyMapper()
    {
        return geoStrategyMapper;
    }

    // //////////////////////////////// Methods for controlled index access
    // For quick'n'easy access to reader, the indexreader is returned directly
    // result LuceneQueryIterators use the more elaborate
    // ReaderMonitor directly to be able to close the reader when they
    // are done.

    public synchronized IndexReader getIndexReader() throws IOException
    {
        if (closed.get()) {
            throw new SailException("Index has been closed");
        }
        return getIndexSearcher().getIndexReader();
    }

    public synchronized IndexSearcher getIndexSearcher() throws IOException
    {
        if (closed.get()) {
            throw new SailException("Index has been closed");
        }
        IndexSearcher indexSearcher = getCurrentMonitor().getIndexSearcher();
        indexSearcher.setSimilarity(similarity);
        return indexSearcher;
    }

    /**
     * Current monitor holds instance of IndexReader and IndexSearcher It is used to keep track of
     * readers
     */
    @Override
    public synchronized ReaderMonitor getCurrentMonitor()
    {
        if (closed.get()) {
            throw new SailException("Index has been closed");
        }
        if (currentMonitor == null) {
            currentMonitor = new ReaderMonitor(this, directory);
        }
        return currentMonitor;
    }

    public synchronized IndexWriter getIndexWriter() throws IOException
    {
        if (closed.get()) {
            throw new SailException("Index has been closed");
        }
        if (indexWriter == null || !indexWriter.isOpen()) {
            IndexWriterConfig indexWriterConfig = getIndexWriterConfig();
            indexWriter = new IndexWriter(directory, indexWriterConfig);
        }
        return indexWriter;
    }

    @Override
    public void shutDown() throws IOException
    {
        // try-finally setup ensures that closing of an instance is not skipped
        // when an earlier instance resulted in an IOException
        // FIXME: is there a more elegant way to ensure this?
        if (closed.compareAndSet(false, true)) {
            try {
                // This close oldMonitors which hold InderReader and
                // IndexSeracher
                // Monitor close IndexReader and IndexSearcher
                ReaderMonitor toCloseCurrentMonitor = currentMonitor;
                currentMonitor = null;
                if (toCloseCurrentMonitor != null) {
                    toCloseCurrentMonitor.close();
                }
            }
            finally {
                List<Throwable> exceptions = new ArrayList<>();
                try {
                    synchronized (oldmonitors) {
                        if (!oldmonitors.isEmpty()) {
                            logger.warn(
                                    "LuceneSail: On shutdown {} IndexReaders were not closed. This is due to non-closed Query Iterators, which must be closed!",
                                    oldmonitors.size());
                        }
                        for (AbstractReaderMonitor monitor : oldmonitors) {
                            try {
                                monitor.close();
                            }
                            catch (Throwable e) {
                                exceptions.add(e);
                            }
                        }
                        oldmonitors.clear();
                    }
                }
                finally {
                    try {
                        IndexWriter toCloseIndexWriter = indexWriter;
                        indexWriter = null;
                        if (toCloseIndexWriter != null) {
                            toCloseIndexWriter.close();
                        }
                    }
                    finally {
                        if (!exceptions.isEmpty()) {
                            throw new UndeclaredThrowableException(exceptions.get(0));
                        }
                    }
                }
            }
        }
    }

    // //////////////////////////////// Methods for updating the index

    @Override
    protected synchronized SearchDocument getDocument(String id) throws IOException
    {
        Document document = getDocument(idTerm(id));
        return (document != null) ? new LuceneDocument(document, geoStrategyMapper) : null;
    }

    @Override
    protected synchronized Iterable<? extends SearchDocument> getDocuments(String resourceId)
        throws IOException
    {
        List<Document> docs = getDocuments(new Term(SearchFields.URI_FIELD_NAME, resourceId));
        return Iterables.transform(docs,
                (Document doc) -> new LuceneDocument(doc, geoStrategyMapper));
    }

    @Override
    protected synchronized SearchDocument newDocument(String id, String resourceId, String context)
    {
        return new LuceneDocument(id, resourceId, context, geoStrategyMapper);
    }

    @Override
    protected synchronized SearchDocument copyDocument(SearchDocument doc)
    {
        Document document = ((LuceneDocument) doc).getDocument();
        Document newDocument = new Document();

        // add all existing fields (including id, uri, context, and text)
        for (IndexableField oldField : document.getFields()) {
            newDocument.add(oldField);
        }
        return new LuceneDocument(newDocument, geoStrategyMapper);
    }

    @Override
    protected synchronized void addDocument(SearchDocument doc) throws IOException
    {
        getIndexWriter().addDocument(((LuceneDocument) doc).getDocument());
    }

    @Override
    protected synchronized void updateDocument(SearchDocument doc) throws IOException
    {
        getIndexWriter().updateDocument(idTerm(doc.getId()), ((LuceneDocument) doc).getDocument());
    }

    @Override
    protected synchronized void deleteDocument(SearchDocument doc) throws IOException
    {
        getIndexWriter().deleteDocuments(idTerm(doc.getId()));
    }

    @Override
    protected synchronized BulkUpdater newBulkUpdate()
    {
        return new SimpleBulkUpdater(this);
    }

    private Term idTerm(String id)
    {
        return new Term(SearchFields.ID_FIELD_NAME, id);
    }

    /**
     * Returns a Document representing the specified document ID (combination of resource and
     * context), or null when no such Document exists yet.
     */
    private Document getDocument(Term idTerm) throws IOException
    {
        IndexReader reader = getIndexReader();
        List<LeafReaderContext> leaves = reader.leaves();
        int size = leaves.size();
        for (int i = 0; i < size; i++) {
            LeafReader lreader = leaves.get(i).reader();
            Document document = getDocument(lreader, idTerm);
            if (document != null) {
                return document;
            }
        }
        // no such Document
        return null;
    }

    private static Document getDocument(LeafReader reader, Term term) throws IOException
    {
        PostingsEnum docs = reader.postings(term);
        if (docs != null) {
            int docId = docs.nextDoc();
            // PostingsEnum may contain deleted documents, we have to cope for it
            while (docId != PostingsEnum.NO_MORE_DOCS) {

                // if document is deleted, skip and continue
                Bits liveDocs = reader.getLiveDocs();
                if (liveDocs != null && !liveDocs.get(docId)) {
                    docId = docs.nextDoc();
                    continue;
                }
                if (docs.nextDoc() != PostingsEnum.NO_MORE_DOCS) {
                    throw new IllegalStateException("Multiple Documents for term " + term.text());
                }
                return readDocument(reader, docId, null);
            }
        }
        return null;
    }

    /**
     * Returns a list of Documents representing the specified Resource (empty when no such Document
     * exists yet). Each document represent a set of statements with the specified Resource as a
     * subject, which are stored in a specific context
     */
    private List<Document> getDocuments(Term uriTerm) throws IOException
    {
        List<Document> result = new ArrayList<>();

        IndexReader reader = getIndexReader();
        List<LeafReaderContext> leaves = reader.leaves();
        int size = leaves.size();
        for (int i = 0; i < size; i++) {
            LeafReader lreader = leaves.get(i).reader();
            addDocuments(lreader, uriTerm, result);
        }

        return result;
    }

    private static void addDocuments(LeafReader reader, Term term, Collection<Document> documents)
        throws IOException
    {
        PostingsEnum docs = reader.postings(term);
        if (docs != null) {
            int docId;
            while ((docId = docs.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
                Bits liveDocs = reader.getLiveDocs();
                // Maybe some of the docs have been deleted! Check that too..
                if (liveDocs != null && !liveDocs.get(docId)) {
                    continue;
                }
                Document document = readDocument(reader, docId, null);
                documents.add(document);
            }
        }
    }

    /**
     * Returns a Document representing the specified Resource and Context combination, or null when
     * no such Document exists yet.
     *
     * @param subject
     * @param context
     * @return document
     * @throws IOException
     */
    public synchronized Document getDocument(Resource subject, Resource context) throws IOException
    {
        // fetch the Document representing this Resource
        String resourceId = SearchFields.getResourceID(subject);
        String contextId = SearchFields.getContextID(context);
        Term idTerm = new Term(SearchFields.ID_FIELD_NAME,
                SearchFields.formIdString(resourceId, contextId));
        return getDocument(idTerm);
    }

    /**
     * Returns a list of Documents representing the specified Resource (empty when no such Document
     * exists yet).Each document represent a set of statements with the specified Resource as a
     * subject, which are stored in a specific context
     *
     * @param subject
     * @return list of documents
     * @throws IOException
     */
    public synchronized List<Document> getDocuments(Resource subject) throws IOException
    {
        String resourceId = SearchFields.getResourceID(subject);
        Term uriTerm = new Term(SearchFields.URI_FIELD_NAME, resourceId);
        return getDocuments(uriTerm);
    }

    /**
     * Stores and indexes an ID in a Document.
     *
     * @param id
     * @param document
     */
    public static void addIDField(String id, Document document)
    {
        document.add(new StringField(SearchFields.ID_FIELD_NAME, id, Store.YES));
    }

    /**
     * Add the "context" value to the doc
     *
     * @param context
     *            the context or null, if null-context
     * @param document
     *            the document
     */
    public static void addContextField(String context, Document document)
    {
        if (context != null) {
            document.add(new StringField(SearchFields.CONTEXT_FIELD_NAME, context, Store.YES));
        }
    }

    /**
     * Stores and indexes the resource ID in a Document.
     *
     * @param resourceId
     * @param document
     */
    public static void addResourceField(String resourceId, Document document)
    {
        document.add(new StringField(SearchFields.URI_FIELD_NAME, resourceId, Store.YES));
    }

    public static void addPredicateField(String predicate, String text, Document document)
    {
        // store this predicate
        document.add(new TextField(predicate, text, Store.YES));
    }

    public static void addStoredOnlyPredicateField(String predicate, String text, Document document)
    {
        // store this predicate
        document.add(new StoredField(predicate, text));
    }

    public static void addTextField(String text, Document document)
    {
        // and in TEXT_FIELD_NAME
        document.add(new TextField(SearchFields.TEXT_FIELD_NAME, text, Store.YES));
    }

    /**
     * invalidate readers, free them if possible (readers that are still open by a
     * {@link LuceneQueryConnection} will not be closed. Synchronized on oldmonitors because it
     * manipulates them
     *
     * @throws IOException
     */
    private void invalidateReaders() throws IOException
    {
        synchronized (oldmonitors) {
            // Move current monitor to old monitors and set null
            if (currentMonitor != null) {
                // we do NOT close it directly as it may be used by an open
                // result
                // iterator, hence moving it to the
                // list of oldmonitors where it is handled as other older
                // monitors
                oldmonitors.add(currentMonitor);
            }
            currentMonitor = null;

            // close all monitors if possible
            for (Iterator<AbstractReaderMonitor> i = oldmonitors.iterator(); i.hasNext();) {
                AbstractReaderMonitor monitor = i.next();
                if (monitor.closeWhenPossible()) {
                    i.remove();
                }
            }

            // check if all readers were closed
            if (oldmonitors.isEmpty()) {
                logger.debug("Deleting unused files from Lucene index");

                // clean up unused files (marked as 'deletable' in Luke
                // Filewalker)
                getIndexWriter().deleteUnusedFiles();

                // logIndexStats();
            }
        }
    }

    @SuppressWarnings("unused")
    private void logIndexStats()
    {
        try {
            IndexReader reader;
            try {
                reader = getIndexReader();

                Document doc;
                int totalFields = 0;

                Set<String> ids = new HashSet<>();
                String[] idArray;
                int count = 0;
                for (int i = 0; i < reader.maxDoc(); i++) {
                    if (isDeleted(reader, i)) {
                        continue;
                    }
                    doc = readDocument(reader, i, null);
                    totalFields += doc.getFields().size();
                    count++;
                    idArray = doc.getValues("id");
                    for (String id : idArray) {
                        ids.add(id);
                    }

                }

                logger.info("Total documents in the index: " + reader.numDocs()
                        + ", number of deletable documents in the index: " + reader.numDeletedDocs()
                        + ", valid documents: " + count + ", total fields in all documents: "
                        + totalFields + ", average number of fields per document: "
                        + ((double) totalFields) / reader.numDocs());
                logger.info("Distinct ids in the index: " + ids.size());

            }
            finally {
                ReaderMonitor toCloseCurrentMonitor = currentMonitor;
                currentMonitor = null;
                if (toCloseCurrentMonitor != null) {
                    toCloseCurrentMonitor.closeWhenPossible();
                }
            }
        }
        catch (IOException e) {
            logger.warn(e.getMessage(), e);
        }

    }

    @Override
    public synchronized void begin() throws IOException
    {
        // nothing to do
    }

    /**
     * Commits any changes done to the LuceneIndex since the last commit. The semantics is
     * synchronous to SailConnection.commit(), i.e. the LuceneIndex should be committed/rolled back
     * whenever the LuceneSailConnection is committed/rolled back.
     */
    @Override
    public synchronized void commit() throws IOException
    {
        getIndexWriter().commit();
        // the old IndexReaders/Searchers are not outdated
        invalidateReaders();
    }

    @Override
    public synchronized void rollback() throws IOException
    {
        getIndexWriter().rollback();
    }

    // //////////////////////////////// Methods for querying the index

    /**
     * Parse the passed query.
     *
     * @param subject
     *            subject
     * @param spec
     *            spec
     * @return the parsed query
     * @throws MalformedQueryException
     *             when the parsing breaks
     * @throws IOException
     */
    @Override
    protected Iterable<? extends DocumentScore> query(Resource subject, QuerySpec spec)
        throws MalformedQueryException, IOException
    {
        Query q;
        try {
            q = createQuery(spec.getQueryPatterns());
        }
        catch (ParseException e) {
            throw new MalformedQueryException(e);
        }

        if (q == null) {
            return EmptyIterator::new;
        }

        final Highlighter highlighter;
        if (spec.isHighlight()) {
            Formatter formatter = new SimpleHTMLFormatter(SearchFields.HIGHLIGHTER_PRE_TAG,
                    SearchFields.HIGHLIGHTER_POST_TAG);
            highlighter = new Highlighter(formatter, new QueryScorer(q));
        }
        else {
            highlighter = null;
        }

        int numDocs;

        Integer specNumDocs = spec.getNumDocs();
        if (specNumDocs != null) {
            if (specNumDocs < 0) {
                throw new IllegalArgumentException("numDocs must be >= 0");
            }
            numDocs = specNumDocs;
        }
        else {
            numDocs = -1;
        }

        TopDocs docs;
        if (subject != null) {
            docs = search(subject, q, numDocs);
        }
        else {
            docs = search(q, numDocs);
        }
        return Iterables.transform(Arrays.asList(docs.scoreDocs),
                (ScoreDoc doc) -> new LuceneDocumentScore(doc, highlighter, LuceneIndex.this));
    }

    /**
     * create a query from the params
     *
     * @param queryPatterns
     *            the params
     * @return boolean query for multiple params, query for single param, null for empty collection
     * @throws ParseException
     *             query parsing exception
     */
    private Query createQuery(Collection<QuerySpec.QueryParam> queryPatterns) throws ParseException
    {
        Iterator<QuerySpec.QueryParam> it = queryPatterns.iterator();

        if (!it.hasNext()) {
            return null;
        }

        QuerySpec.QueryParam first = it.next();

        Query q = getQueryParser(first.getProperty()).parse(first.getQuery());
        if (!it.hasNext()) {
            return q;
        }

        BooleanQuery.Builder bld = new BooleanQuery.Builder();
        if (first.getBoost() != null) {
            q = new BoostQuery(q, first.getBoost());
        }
        bld.add(q, Occur.SHOULD);
        do {
            QuerySpec.QueryParam param = it.next();
            Query parsedQuery = getQueryParser(param.getProperty()).parse(param.getQuery());
            if (param.getBoost() != null) {
                parsedQuery = new BoostQuery(parsedQuery, param.getBoost());
            }
            bld.add(parsedQuery, Occur.SHOULD);
        }
        while (it.hasNext());

        return bld.build();
    }

    @Override
    protected Iterable<? extends DocumentDistance> geoQuery(final IRI geoProperty, Point p,
            final IRI units, double distance, String distanceVar, Var contextVar)
        throws MalformedQueryException, IOException
    {
        double degs = GeoUnits.toDegrees(distance, units);
        final String geoField = SearchFields.getPropertyField(geoProperty);
        SpatialContext context = SpatialContext.GEO;
        final Shape boundingCircle = context.getShapeFactory().circle(p, degs);

        // use LatLonPoint for distance query after indexing it with the same data structure

        Query q = LatLonPoint.newDistanceQuery(POINT_FIELD_PREFIX + geoField, p.getY(), p.getX(),
                distance);
        if (contextVar != null) {
            q = addContextTerm(q, (Resource) contextVar.getValue());
        }

        TopDocs docs = search(q);
        final boolean requireContext = (contextVar != null && !contextVar.hasValue());
        return Iterables.transform(Arrays.asList(docs.scoreDocs),
                (ScoreDoc doc) -> new LuceneDocumentDistance(doc, geoField, units,
                        boundingCircle.getCenter(), requireContext, LuceneIndex.this));
    }

    private Query addContextTerm(Query q, Resource ctx)
    {
        BooleanQuery.Builder combinedQuery = new BooleanQuery.Builder();
        TermQuery idQuery = new TermQuery(
                new Term(SearchFields.CONTEXT_FIELD_NAME, SearchFields.getContextID(ctx)));
        // the specified named graph or not the unnamed graph
        combinedQuery.add(idQuery, ctx != null ? Occur.MUST : Occur.MUST_NOT);
        combinedQuery.add(q, Occur.MUST);
        return combinedQuery.build();
    }

    @Override
    protected Iterable<? extends DocumentResult> geoRelationQuery(String relation, IRI geoProperty,
            String wkt, Var contextVar)
        throws MalformedQueryException, IOException
    {

        Object shape = null;
        try {
            shape = super.parseLuceneQueryShape(SearchFields.getPropertyField(geoProperty), wkt);
        }
        catch (java.text.ParseException e) {
            logger.error("error while parsing wkt geometry", e);
        }
        SpatialOperation op = toSpatialOp(relation);
        if (op == null) {
            return null;
        }
        final String geoField = SearchFields.getPropertyField(geoProperty);

        // Use the new indexing algorithm from lucene (LatLonShape)
        Query q = makeQuery(op, GEO_FIELD_PREFIX + geoField, shape);
        if (contextVar != null) {
            q = addContextTerm(q, (Resource) contextVar.getValue());
        }

        TopDocs docs = search(q);
        final Set<String> fields = Sets.newHashSet(SearchFields.URI_FIELD_NAME, geoField);
        if (contextVar != null && !contextVar.hasValue()) {
            fields.add(SearchFields.CONTEXT_FIELD_NAME);
        }
        return Iterables.transform(Arrays.asList(docs.scoreDocs),
                (ScoreDoc doc) -> new LuceneDocumentResult(doc, LuceneIndex.this, fields));
    }

    private ShapeField.QueryRelation getRelation(SpatialOperation op)
    {
        switch (op.toString()) {
        case "Contains":
            return ShapeField.QueryRelation.INTERSECTS;
        case "Within":
            return ShapeField.QueryRelation.WITHIN;
        case "Disjoint":
            return ShapeField.QueryRelation.DISJOINT;
        default:
            throw new IllegalArgumentException(
                    "The geo function [" + op.toString() + "] is not supported");
        }
    }

    private Query makeQuery(SpatialOperation op, String geoField, Object shape)
    {
        Query q = null;
        ShapeField.QueryRelation relation = getRelation(op);
        if (shape instanceof double[]) {
            double[] point = (double[]) shape;
            q = LatLonShape.newBoxQuery(geoField, relation, point[1], point[1], point[0], point[0]);
        }
        else if (shape instanceof Polygon) {
            q = LatLonShape.newPolygonQuery(geoField, relation, (Polygon) shape);
        }
        else if (shape instanceof Polygon[]) {
            q = LatLonShape.newPolygonQuery(geoField, relation, (Polygon[]) shape);
        }
        else if (shape instanceof Line) {
            q = LatLonShape.newLineQuery(geoField, relation, (Line) shape);
        }
        else if (shape instanceof Line[]) {
            q = LatLonShape.newLineQuery(geoField, relation, (Line[]) shape);
        }
        else if (shape instanceof Rectangle) {
            Rectangle box = (Rectangle) shape;
            q = LatLonShape.newBoxQuery(geoField, relation, box.minLat, box.minLon, box.maxLat,
                    box.maxLon);
        }
        else if (shape instanceof Rectangle[]) {
            Rectangle box = ((Rectangle[]) shape)[0];
            q = LatLonShape.newBoxQuery(geoField, relation, box.minLat, box.minLon, box.maxLat,
                    box.maxLon);
        }
        return q;
    }

    private SpatialOperation toSpatialOp(String relation)
    {
        if (GEOF.SF_INTERSECTS.stringValue().equals(relation)) {
            return SpatialOperation.Intersects;
        }
        else if (GEOF.SF_DISJOINT.stringValue().equals(relation)) {
            return SpatialOperation.IsDisjointTo;
        }
        else if (GEOF.SF_EQUALS.stringValue().equals(relation)) {
            return SpatialOperation.IsEqualTo;
        }
        else if (GEOF.SF_OVERLAPS.stringValue().equals(relation)) {
            return SpatialOperation.Overlaps;
        }
        else if (GEOF.EH_COVERED_BY.stringValue().equals(relation)) {
            return SpatialOperation.IsWithin;
        }
        else if (GEOF.EH_COVERS.stringValue().equals(relation)) {
            return SpatialOperation.Contains;
        }
        else if (GEOF.SF_WITHIN.stringValue().equals(relation)) {
            return SpatialOperation.IsWithin;
        }
        else if (GEOF.EH_CONTAINS.stringValue().equals(relation)) {
            return SpatialOperation.Contains;
        }
        return null;
    }

    /**
     * Returns the lucene hit with the given id of the respective lucene query
     *
     * @param docId
     *            the id of the document to return
     * @param fieldsToLoad
     * @return the requested hit, or null if it fails
     */
    public synchronized Document getDocument(int docId, Set<String> fieldsToLoad)
    {
        try {
            return readDocument(getIndexReader(), docId, fieldsToLoad);
        }
        catch (CorruptIndexException e) {
            logger.error("The index seems to be corrupted:", e);
            return null;
        }
        catch (IOException e) {
            logger.error("Could not read from index:", e);
            return null;
        }
    }

    public synchronized String getSnippet(String fieldName, String text, Highlighter highlighter)
    {
        String snippet;
        try {
            TokenStream tokenStream = getAnalyzer().tokenStream(fieldName, new StringReader(text));
            snippet = highlighter.getBestFragments(tokenStream, text, 2, "...");
        }
        catch (Exception e) {
            logger.error("Exception while getting snippet for field " + fieldName, e);
            snippet = null;
        }
        return snippet;
    }

    /**
     * Evaluates the given query only for the given resource.
     *
     * @param resource
     * @param query
     * @return top documents
     * @throws IOException
     */
    public synchronized TopDocs search(Resource resource, Query query) throws IOException
    {
        return search(resource, query, -1);
    }

    /**
     * Evaluates the given query only for the given resource.
     *
     * @param resource
     * @param query
     * @param numDocs
     * @return top documents
     * @throws IOException
     */
    public synchronized TopDocs search(Resource resource, Query query, int numDocs)
        throws IOException
    {
        // rewrite the query
        TermQuery idQuery = new TermQuery(
                new Term(SearchFields.URI_FIELD_NAME, SearchFields.getResourceID(resource)));
        BooleanQuery.Builder combinedQuery = new BooleanQuery.Builder();
        combinedQuery.add(idQuery, Occur.MUST);
        combinedQuery.add(query, Occur.MUST);
        return search(combinedQuery.build(), numDocs);
    }

    /**
     * Evaluates the given query and returns the results as a TopDocs instance.
     *
     * @param query
     * @return top documents
     * @throws IOException
     */
    public synchronized TopDocs search(Query query) throws IOException
    {
        return search(query, -1);
    }

    /**
     * Evaluates the given query and returns the results as a TopDocs instance.
     *
     * @param query
     * @param numDocs
     * @return top documents
     * @throws IOException
     */
    public synchronized TopDocs search(Query query, int numDocs) throws IOException
    {
        if (numDocs < -1) {
            throw new IllegalArgumentException(
                    "numDocs should be 0 or greater if defined by the user");
        }

        int size = defaultNumDocs;
        if (numDocs >= 0) {
            // If the user has set numDocs we will use that. If it is 0 then the implementation may
            // end up throwing an
            // exception.
            size = Math.min(maxDocs, numDocs);
        }
        if (size < 0) {
            size = Math.max(getIndexReader().numDocs(), 1);
        }
        return getIndexSearcher().search(query, size);
    }

    private QueryParser getQueryParser(IRI propertyURI)
    {
        String fieldName;
        // check out which query parser to use, based on the given property URI
        if (propertyURI == null) {
            // if we have no property given, we create a default query parser which has the
            // TEXT_FIELD_NAME as the
            // default field
            fieldName = SearchFields.TEXT_FIELD_NAME;
        }
        else {
            // otherwise we create a query parser that has the given property as the default field
            fieldName = SearchFields.getPropertyField(propertyURI);
        }

        QueryParser queryParser = new QueryParser(fieldName, queryAnalyzer);
        queryParser.setFuzzyPrefixLength(fuzzyPrefixLength);
        return queryParser;
    }

    /**
     * @param contexts
     * @throws IOException
     */
    @Override
    public synchronized void clearContexts(Resource... contexts) throws IOException
    {
        logger.debug("deleting contexts: {}", Arrays.toString(contexts));
        // these resources have to be read from the underlying rdf store
        // and their triples have to be added to the luceneindex after deletion
        // of
        // documents

        // remove all contexts passed
        for (Resource context : contexts) {
            // attention: context can be NULL!
            String contextString = SearchFields.getContextID(context);
            Term contextTerm = new Term(SearchFields.CONTEXT_FIELD_NAME, contextString);

            // now delete all documents from the deleted context
            getIndexWriter().deleteDocuments(contextTerm);
        }
    }

    /**
     *
     */
    @Override
    public synchronized void clear() throws IOException
    {
        if (closed.get()) {
            throw new SailException("Index has been closed");
        }
        // clear
        // the old IndexReaders/Searchers are not outdated
        invalidateReaders();
        if (indexWriter != null) {
            indexWriter.close();
        }

        // crate new writer
        IndexWriterConfig indexWriterConfig = getIndexWriterConfig();
        indexWriterConfig.setOpenMode(OpenMode.CREATE);
        indexWriter = new IndexWriter(directory, indexWriterConfig);
        indexWriter.close();
        indexWriter = null;

    }

    //
    // Lucene helper methods
    //

    /**
     * Method produces {@link IndexWriterConfig} using settings.
     *
     * @return
     */
    private IndexWriterConfig getIndexWriterConfig()
    {
        IndexWriterConfig cnf = new IndexWriterConfig(analyzer);
        cnf.setSimilarity(similarity);
        return cnf;
    }

    private static boolean isDeleted(IndexReader reader, int docId)
    {
        if (reader.hasDeletions()) {
            List<LeafReaderContext> leaves = reader.leaves();
            int size = leaves.size();
            for (int i = 0; i < size; i++) {
                Bits liveDocs = leaves.get(i).reader().getLiveDocs();
                if (docId < liveDocs.length()) {
                    boolean isDeleted = !liveDocs.get(docId);
                    if (isDeleted) {
                        return true;
                    }
                }
            }
            return false;
        }
        else {
            return false;
        }
    }

    private static Document readDocument(IndexReader reader, int docId, Set<String> fieldsToLoad)
        throws IOException
    {
        DocumentStoredFieldVisitor visitor = new DocumentStoredFieldVisitor(fieldsToLoad);
        reader.document(docId, visitor);
        return visitor.getDocument();
    }

    static class DocumentStoredFieldVisitor
        extends StoredFieldVisitor
    {

        private final Set<String> fieldsToLoad;

        private final Document document = new Document();

        DocumentStoredFieldVisitor(Set<String> fieldsToLoad)
        {
            this.fieldsToLoad = fieldsToLoad;
        }

        @Override
        public Status needsField(FieldInfo fieldInfo) throws IOException
        {
            return (fieldsToLoad == null || fieldsToLoad.contains(fieldInfo.name)) ? Status.YES
                    : Status.NO;
        }

        @Override
        public void stringField(FieldInfo fieldInfo, String stringValue)
        {
            String name = fieldInfo.name;
            if (SearchFields.ID_FIELD_NAME.equals(name)) {
                addIDField(stringValue, document);
            }
            else if (SearchFields.CONTEXT_FIELD_NAME.equals(name)) {
                addContextField(stringValue, document);
            }
            else if (SearchFields.URI_FIELD_NAME.equals(name)) {
                addResourceField(stringValue, document);
            }
            else if (SearchFields.TEXT_FIELD_NAME.equals(name)) {
                addTextField(stringValue, document);
            }
            else {
                addPredicateField(name, stringValue, document);
            }
        }

        Document getDocument()
        {
            return document;
        }
    }
}

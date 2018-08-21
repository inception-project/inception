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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SegmentReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.spans.SpanWeight;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.search.ExecutionException;
import de.tudarmstadt.ukp.inception.search.SearchResult;
import de.tudarmstadt.ukp.inception.search.index.PhysicalIndex;
import mtas.analysis.token.MtasTokenString;
import mtas.analysis.util.MtasTokenizerFactory;
import mtas.codec.MtasCodec;
import mtas.codec.util.CodecInfo;
import mtas.codec.util.CodecUtil;
import mtas.parser.cql.MtasCQLParser;
import mtas.parser.cql.ParseException;
import mtas.search.spans.util.MtasSpanQuery;

/**
 * 
 * The Mtas implementation for a physical index
 *
 */
public class MtasDocumentIndex
    implements PhysicalIndex
{
    private final String MTAS_PARSER = "de.tudarmstadt.ukp.inception.search.index.mtas.MtasUimaParser";
    private final String MTAS_TOKENIZER = "mtas";
    private String INDEX = "indexMtas";

    /** The Constant FIELD_ID. */
    private static final String FIELD_ID = "id";

    /** The Constant FIELD_TITLE. */
    private static final String FIELD_TITLE = "title";

    /** The Constant FIELD_CONTENT. */
    private static final String FIELD_CONTENT = "content";

    /** The Constant FIELD_CONTENT. */
    private static final String FIELD_USER = "user";

    // Default prefix for CQL queries
    private static final String DEFAULT_PREFIX = "Token";

    private static final int RESULT_WINDOW_SIZE = 2;

    private final Logger log = LoggerFactory.getLogger(getClass());

    static AnnotationSchemaService annotationSchemaService;
    static DocumentService documentService;
    static ProjectService projectService;
    Project project;

    // The index writers for this index
    private IndexWriter indexWriter;

    // The annotations to be indexed
    ArrayList<String> annotationShortNames;

    private File resourceDir;

    public MtasDocumentIndex(Project aProject, AnnotationSchemaService aAnnotationSchemaService,
            DocumentService aDocumentService, ProjectService aProjectService, String aDir)
        throws IOException
    {
        annotationSchemaService = aAnnotationSchemaService;
        documentService = aDocumentService;
        projectService = aProjectService;
        project = aProject;

        // Create list with the annotation types of the layer (only the enabled ones)
        List<AnnotationLayer> layers = annotationSchemaService.listAnnotationLayer(project);

        annotationShortNames = new ArrayList<String>();

        for (AnnotationLayer layer : layers) {
            if (layer.isEnabled()) {
                annotationShortNames.add(getShortName(layer.getName()));
            }
        }
       
        resourceDir = new File(aDir);
        log.info("New Mtas/Lucene index instance created...");
    }

    @Override
    public boolean connect(String aUrl, String aUser, String aPassword)
    {
        return true;
    }

    @Override
    public List<SearchResult> executeQuery(User aUser, String aQuery, String aSortOrder,
            String... aResultField)
        throws ExecutionException
    {
        try {
            Directory directory = FSDirectory.open(getIndexDir().toPath());
            IndexReader indexReader = DirectoryReader.open(directory);

            // Build the query prefixes list from the annotation types
            List<String> prefixes = new ArrayList<String>(annotationShortNames);
            String modifiedQuery = parseQuery(aQuery);
            MtasSpanQuery mtasSpanQuery = createQuery(FIELD_CONTENT, modifiedQuery);
            return doQuery(indexReader, aUser, FIELD_CONTENT, mtasSpanQuery, prefixes);
        }
        catch (mtas.parser.cql.ParseException e) {
            log.error("Mtas query syntax error", e);
            throw(new ExecutionException(e.getMessage()));
        }
        catch (Exception e) {
            log.error("Query execution error", e);
            throw (new ExecutionException("Query execution error", e));
        }
    }

    private String parseQuery(String aQuery)
    {
        String result;

        if (!(aQuery.contains("[") || aQuery.contains("]") || aQuery.contains("{")
                || aQuery.contains("}") || aQuery.contains("<") || aQuery.contains(">"))) {
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

    private static MtasSpanQuery createQuery(String aField, String aQuery) throws ParseException
    {
        Reader reader = new BufferedReader(new StringReader(aQuery));
        MtasCQLParser p = new MtasCQLParser(reader);
        MtasSpanQuery q = p.parse(aField, DEFAULT_PREFIX, null, null, null);
        return q;
    }

    private List<SearchResult> doQuery(IndexReader aIndexReader, User aUser, String field,
            MtasSpanQuery q, List<String> prefixes)
        throws IOException
    {
        List<SearchResult> results = new ArrayList<>();

        ListIterator<LeafReaderContext> leafReaderContextIterator = aIndexReader.leaves()
                .listIterator();

        IndexSearcher searcher = new IndexSearcher(aIndexReader);

        final float boost = 0;
        SpanWeight spanweight = q.rewrite(aIndexReader).createWeight(searcher, false, boost);

        while (leafReaderContextIterator.hasNext()) {
            LeafReaderContext leafReaderContext = leafReaderContextIterator.next();
            try {
                Spans spans = spanweight.getSpans(leafReaderContext, SpanWeight.Postings.POSITIONS);
                SegmentReader segmentReader = (SegmentReader) leafReaderContext.reader();
                Terms terms = segmentReader.terms(field);
                CodecInfo mtasCodecInfo = CodecInfo.getCodecInfoFromTerms(terms);
                if (spans != null) {
                    while (spans.nextDoc() != Spans.NO_MORE_DOCS) {
                        if (segmentReader.numDocs() == segmentReader.maxDoc()
                                || segmentReader.getLiveDocs().get(spans.docID())) {
                            Document document = segmentReader.document(spans.docID());

                            // Retrieve user
                            String user = document.get(FIELD_USER);

                            // Retrieve source and annotation document ids
                            Long sourceDocumentId = Long
                                    .valueOf(document.get(FIELD_ID).split("/")[0]);
                            long annotationDocumentId = Long
                                    .valueOf(document.get(FIELD_ID).split("/")[1]);

                            // Retrieve the source document
                            SourceDocument sourceDocument = documentService
                                    .getSourceDocument(project.getId(), sourceDocumentId);

                            if (documentService.existsAnnotationDocument(sourceDocument, aUser)
                                    && annotationDocumentId == -1) {
                                // Exclude result if the retrieved document is a sourcedocument
                                // (that is, has annotationDocument = -1) AND it has a 
                                // corresponding annotation document for this user
                                continue;
                            }
                            else if (annotationDocumentId != -1
                                    && !aUser.getUsername().equals(user)) {
                                // Exclude result if the retrieved document is an annotation
                                // document (that is, annotationDocument != -1 and its username 
                                // is different fromthe quering user
                                continue;
                            }

                            // Retrieve document title
                            String documentTitle = document.get(FIELD_TITLE);

                            String idValue = segmentReader.document(spans.docID())
                                    .getField(FIELD_ID).stringValue();
                            log.debug("********  New doc {}-{}", + spans.docID(), idValue);

                            while (spans.nextStartPosition() != Spans.NO_MORE_POSITIONS) {

                                int resultWindowStartPosition = spans.startPosition()
                                        - RESULT_WINDOW_SIZE;

                                // Avoid the window starting before the beginning of the text
                                if (resultWindowStartPosition < 0) {
                                    resultWindowStartPosition = 0;
                                }

                                int resultWindowEndPosition = spans.endPosition()
                                        + RESULT_WINDOW_SIZE + 1;

                                // Avoid the window ending after the end of the text
                                if (resultWindowEndPosition > spans.endPosition()) {
                                    resultWindowEndPosition = spans.endPosition();
                                }
                                List<MtasTokenString> tokens = mtasCodecInfo
                                        .getPrefixFilteredObjectsByPositions(field, spans.docID(),
                                                prefixes, resultWindowStartPosition,
                                                resultWindowEndPosition);

                                Collections.sort(tokens, new Comparator<MtasTokenString>()
                                {
                                    @Override
                                    public int compare(MtasTokenString token2,
                                            MtasTokenString token1)
                                    {
                                        return token2.getPositionStart() > token1.getPositionStart()
                                                ? 1
                                                : -1;
                                    }
                                });

                                SearchResult result = new SearchResult();
                                String resultText = "";
                                String leftContext = "";
                                String rightContext = "";
                                result.setDocumentId(sourceDocumentId);
                                result.setDocumentTitle(documentTitle);

                                int startToken = 0;
                                while (startToken < tokens.size()) {
                                    if (tokens.get(startToken).getPositionStart()
                                            .equals(tokens.get(startToken).getPositionEnd())
                                            && tokens.get(startToken).getPositionStart() == spans
                                                    .startPosition()) {
                                        break;
                                    }
                                    startToken++;
                                }

                                if (startToken >= tokens.size()) {
                                    startToken = 0;
                                }

                                int endToken = 0;
                                while (endToken < tokens.size()) {
                                    if (tokens.get(endToken).getPositionStart()
                                            .equals(tokens.get(endToken).getPositionEnd())
                                            && tokens.get(endToken).getPositionStart() == spans
                                                    .endPosition()) {
                                        break;
                                    }
                                    endToken++;
                                }

                                if (endToken >= tokens.size()) {
                                    endToken = 0;
                                }

                                result.setOffsetStart(tokens.get(startToken).getOffsetStart());
                                result.setOffsetEnd(tokens.get(endToken - 1).getOffsetEnd());
                                result.setTokenStart(spans.startPosition());
                                result.setTokenLength(spans.endPosition() - spans.startPosition());
                                for (int i = 0; i < tokens.size(); i++) {
                                    MtasTokenString token = tokens.get(i);
                                    if (token.getPrefix().equals(DEFAULT_PREFIX)) {
                                        if (token.getPositionStart() < spans.startPosition()) {
                                            leftContext += CodecUtil.termValue(token.getValue())
                                                    + " ";
                                        }
                                        else if (token.getPositionStart() >= spans.endPosition()) {
                                            rightContext += CodecUtil.termValue(token.getValue())
                                                    + " ";
                                        }
                                        else {
                                            resultText += CodecUtil.termValue(token.getValue())
                                                    + " ";
                                        }

                                        if (log.isTraceEnabled()) {
                                            if (token.getPositionEnd() != token
                                                    .getPositionStart()) {
                                                log.trace(
                                                        " doc: {}-{}, mtasID: {} offset: {}-{} position: {}-{}",
                                                        sourceDocumentId, documentTitle,
                                                        token.getId(), token.getOffsetStart(),
                                                        token.getOffsetEnd(),
                                                        token.getPositionStart(),
                                                        token.getPositionEnd());
                                            }
                                            else {
                                                log.trace(
                                                        " doc: {}-{}, mtasID: {} offset: {}-{} position: {} {}:{}",
                                                        sourceDocumentId, documentTitle,
                                                        token.getId(), token.getOffsetStart(),
                                                        token.getOffsetEnd(),
                                                        token.getPositionStart(), token.getPrefix(),
                                                        token.getPostfix());

                                            }
                                        }
                                    }
                                }
                                result.setText(resultText);
                                result.setLeftContext(leftContext);
                                result.setRightContext(rightContext);
                                results.add(result);
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

    private void indexDocument(String aDocumentTitle, long aSourceDocumentId,
            long aAnnotationDocumentId, String aUser, JCas aJCas)
        throws IOException
    {
        if (indexWriter != null) {
            try {
                log.debug(
                        "Indexing document in project [{}]. sourceId: {}, annotationId: {}, "
                                + "user: {}",
                        project.getName(), aSourceDocumentId, aAnnotationDocumentId, aUser);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                XmiCasSerializer.serialize(aJCas.getCas(), null, bos, true, null);
                bos.close();
                Document doc = new Document();
                doc.add(new StringField(FIELD_ID, String.valueOf(aSourceDocumentId) + "/"
                        + String.valueOf(aAnnotationDocumentId), Field.Store.YES));
                doc.add(new StringField(FIELD_TITLE, aDocumentTitle, Field.Store.YES));
                doc.add(new StringField(FIELD_USER, aUser, Field.Store.YES));
                doc.add(new TextField(FIELD_CONTENT, new String(bos.toByteArray(), "UTF-8"),
                        Field.Store.YES));
    
                // Add document to the Lucene index
                indexWriter.addDocument(doc);
    
                // commit
                indexWriter.commit();
    
                log.info(
                        "Document indexed in project [{}]. sourceId: {}, annotationId: {}, "
                                + "user: {}",
                        project.getName(), aSourceDocumentId, aAnnotationDocumentId, aUser);
            }
            catch (SAXException e) {
                log.error("Unable to index document", e);
            }
        }
        else {
            log.debug(
                    "Aborted indexing of document in project [{}]. sourceId: {}, annotationId: {}, "
                            + "user: {} - indexWriter was null",
                    project.getName(), aSourceDocumentId, aAnnotationDocumentId, aUser);
        }
    };

    @Override
    public void indexDocument(SourceDocument aDocument, JCas aJCas) throws IOException
    {
        indexDocument(aDocument.getName(), aDocument.getId(), -1, "", aJCas);
    };

    @Override
    public void indexDocument(AnnotationDocument aDocument, JCas aJCas) throws IOException
    {
        indexDocument(aDocument.getName(), aDocument.getDocument().getId(), aDocument.getId(),
                aDocument.getUser(), aJCas);
    };

    /**
     * Remove document from the index
     * 
     * @param aSourceDocumentId
     *            The ID of the source document to be removed
     * @param aSourceDocumentId
     *            The ID of the annotation document to be removed
     * @param aUser
     *            The owner of the document to be removed
     */
    private void deindexDocument(long aSourceDocumentId, long aAnnotationDocumentId, String aUser)
        throws IOException
    {
        if (indexWriter != null) {
            log.debug(
                    "Removing document from index in project [{}]. sourceId: {}, annotationId: {}, "
                            + "user: {}",
                    project.getName(), aSourceDocumentId, aAnnotationDocumentId, aUser);

            indexWriter.deleteDocuments(new Term(FIELD_ID,
                    String.format("%d/%d", aSourceDocumentId, aAnnotationDocumentId)));

            indexWriter.commit();

            log.info(
                    "Removed document from index in project [{}]. sourceId: {}, annotationId: {}, "
                            + "user: {}",
                    project.getName(), aSourceDocumentId, aAnnotationDocumentId, aUser);
        }
        else {
            log.debug(
                    "Aborted removal of document from index in project [{}]. sourceId: {}, "
                            + "annotationId: {}, " + "user: {} - indexWriter was null.",
                    project.getName(), aSourceDocumentId, aAnnotationDocumentId, aUser);
        }
        return;
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
    }

    /**
     * Checks if a project index is open
     * 
     * @return True if the index is open. False otherwise.
     */
    @Override
    public boolean isOpen()
    {
        boolean result = false;
        
        if (indexWriter != null) {
            result = indexWriter.isOpen();
        }
        return result;
    }

    /**
     * Returns a File object corresponding to the project's index folder
     * 
     * @return File object corresponding to project's index folder
     */
    private File getIndexDir()
    {
        return new File(resourceDir, "/" + PROJECT_FOLDER + "/" + project.getId() + "/" + INDEX);
    }

    @Override
    public void closePhysicalIndex()
    {
        if (indexWriter != null) {
            try {
                if (indexWriter.isOpen()) {
                    // Commit and close the index
                    indexWriter.commit();
                    indexWriter.close();
                }

                log.info("Index for project [{}] has been closed", project.getName());
            }
            catch (IOException e) {
                log.error("Error closing index for project [{}]", project.getId());
            }
        }
    }

    /**
     * Drops the project's index
     * 
     */
    @Override
    public void dropPhysicalIndex() throws IOException
    {
        if (indexWriter != null) {
            closePhysicalIndex();
        }

        // Delete the index directory
        FileUtils.deleteDirectory(getIndexDir());

        log.info("Index for project [{}] has been deleted", project.getName());
    }

    @Override
    public boolean isCreated()
    {
        if (getIndexDir().isDirectory()) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Open a Mtas physical index, setting indexWriter
     */
    @Override
    public void openPhysicalIndex()
    {
        boolean isOpen;
        
        isOpen = (indexWriter == null) ? false : indexWriter.isOpen();

        if (!isOpen) {
            // Only open if it is not already open
            try {
                log.info("indexWriter was not open. Opening it for project [{}]",
                        project.getName());

                indexWriter = openLuceneIndex(getIndexDir());
                indexWriter.commit();

                log.info("indexWriter has been opened for project [{}]", project.getName());
            }
            catch (Exception e) {
                log.error("Unable to open indexWriter", e);
            }
        } else {
            log.info("indexWriter is already open for project [{}]", project.getName());
        }
    }

    /**
     * Create, open and index all documents for a given project
     */
    @Override
    public void createPhysicalIndex()
    {
        File indexDir = getIndexDir();

        try {
            // Create the directory for the new index
            log.info("Creating index directory for project [{}]", project.getName());
            FileUtils.forceMkdir(indexDir);

            // Open the index
            log.info("Opening index for project [{}]", project.getName());
            openPhysicalIndex();
            
            if (isOpen()) {
                // Index all documents of the project
                log.info("Indexing all documents in the project [{}]", project.getName());
                indexAllDocuments();
                log.info("All documents have been indexed in the project [{}]", project.getName());
            } else {
                log.info("Index has not been opened. No documents have been indexed.");
            }
        }
        catch (Exception e) {
            log.error("Error creating index for project [{}]", project.getName(), e);
        }
    }

    public IndexWriter openLuceneIndex(File aIndexDir) throws IOException
    {
        Directory directory = FSDirectory.open(aIndexDir.toPath());

        // Create parser configuration as a JSON object
        JSONObject jsonParserConfiguration = new JSONObject();
        
        // Add the project id to the configuration
        jsonParserConfiguration.put("projectId", project.getId());

        // Tokenizer parameters
        Map<String, String> paramsTokenizer = new HashMap<String, String>();
        paramsTokenizer.put(MtasTokenizerFactory.ARGUMENT_PARSER, MTAS_PARSER);
        paramsTokenizer.put(MtasTokenizerFactory.ARGUMENT_PARSER_ARGS,
                jsonParserConfiguration.toString());

        // Build analyzer
        Analyzer mtasAnalyzer = CustomAnalyzer.builder()
                .withTokenizer(MTAS_TOKENIZER, paramsTokenizer).build();

        Map<String, Analyzer> analyzerPerField = new HashMap<String, Analyzer>();
        analyzerPerField.put(FIELD_CONTENT, mtasAnalyzer);

        PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(),
                analyzerPerField);

        // Build IndexWriter
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setUseCompoundFile(false);
        config.setCodec(Codec.forName(MtasCodec.MTAS_CODEC_NAME));

        return new IndexWriter(directory, config);
    }

    private void indexAllDocuments()
    {

        int users = 0;
        int annotationDocs = 0;
        int sourceDocs = 0;

        try {
            log.info("Indexing all annotation documents of project [{}]", project.getName());

            for (User user : projectService.listProjectUsersWithPermissions(project)) {
                users++;
                for (AnnotationDocument document : documentService.listAnnotationDocuments(project,
                        user)) {
                    indexDocument(document, documentService.readAnnotationCas(document));
                    annotationDocs++;
                }
            }

            log.info("Indexing all source documents of project [{}]", project.getName());

            for (SourceDocument document : documentService.listSourceDocuments(project)) {
                indexDocument(document, documentService.createOrReadInitialCas(document));
                sourceDocs++;
            }
        }
        catch (IOException e) {
            log.error("Unable to index document", e);
        }

        log.info(String.format(
                "Indexing results: %d source doc(s), %d annotation doc(s) for %d user(s)",
                sourceDocs, annotationDocs, users));
    }

    private String getShortName(String aName)
    {
        String name;

        name = aName;

        final int pos = name.lastIndexOf(TypeSystem.NAMESPACE_SEPARATOR);
        if (pos >= 0) {
            return name.substring(pos + 1, name.length());
        }
        return name;
    }
}

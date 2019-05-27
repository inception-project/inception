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
import static java.util.Collections.unmodifiableList;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.BreakIterator;
import java.util.*;

import de.tudarmstadt.ukp.clarin.webanno.model.*;
import de.tudarmstadt.ukp.clarin.webanno.support.ApplicationContextProvider;
import de.tudarmstadt.ukp.inception.search.*;
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
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SegmentReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.spans.SpanWeight;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.xml.sax.SAXException;

import com.github.openjson.JSONObject;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.search.index.PhysicalIndex;
import mtas.analysis.token.MtasTokenString;
import mtas.analysis.util.MtasTokenizerFactory;
import mtas.codec.MtasCodec;
import mtas.codec.util.CodecInfo;
import mtas.codec.util.CodecUtil;
import mtas.parser.cql.MtasCQLParser;
import mtas.search.spans.util.MtasSpanQuery;

/**
 * 
 * The Mtas implementation for a physical index
 */
public class MtasDocumentIndex
    implements PhysicalIndex
{
    private static final String MTAS_PARSER = 
            "de.tudarmstadt.ukp.inception.search.index.mtas.MtasUimaParser";
    private static final String MTAS_TOKENIZER = "mtas";
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

    private final Logger log = LoggerFactory.getLogger(getClass());

    private @Autowired FeatureIndexingSupportRegistry featureIndexingSupportRegistry;

    private final AnnotationSchemaService annotationSchemaService;
    private final DocumentService documentService;
    private final ProjectService projectService;
    private final Project project;

    // The index writers for this index
    private IndexWriter indexWriter;

    // The annotations to be indexed
    private final List<String> annotationShortNames;

    private final File resourceDir;

    public MtasDocumentIndex(Project aProject, AnnotationSchemaService aAnnotationSchemaService,
            DocumentService aDocumentService, ProjectService aProjectService, String aDir)
        throws IOException
    {
        AutowireCapableBeanFactory factory = ApplicationContextProvider.getApplicationContext()
            .getAutowireCapableBeanFactory();
        factory.autowireBean(this);
        factory.initializeBean(this, "transientParser");

        annotationSchemaService = aAnnotationSchemaService;
        documentService = aDocumentService;
        projectService = aProjectService;
        project = aProject;

        // Create list with the annotation types of the layer (only the enabled ones)
        Set<String> shortNames = new HashSet<>();
        annotationSchemaService.listAnnotationLayer(project).stream()
                .filter(AnnotationLayer::isEnabled)
                .map(layer -> getShortName(layer.getUiName()))
                .forEach(shortNames::add);
        shortNames.add(MtasUimaParser.MTAS_TOKEN_LABEL);
        shortNames.add(MtasUimaParser.MTAS_SENTENCE_LABEL);
        shortNames.add("Named_entity");
        shortNames.add("Part_of_speech");
        annotationShortNames = unmodifiableList(new ArrayList<>(shortNames));
       
        resourceDir = new File(aDir);
        log.debug("New Mtas/Lucene index instance created...");
    }

    @Override
    public boolean connect(String aUrl, String aUser, String aPassword)
    {
        return true;
    }

    @Override
    public Map<String, List<SearchResult>> executeQuery(SearchQueryRequest aRequest)
        throws IOException, ExecutionException
    {
        try {
            log.trace("Executing query {} on index {}", aRequest, getIndexDir());
            
            Directory directory = FSDirectory.open(getIndexDir().toPath());
            IndexReader indexReader = DirectoryReader.open(directory);

            String modifiedQuery = parseQuery(aRequest.getQuery());
            MtasSpanQuery mtasSpanQuery;
            try (Reader reader = new StringReader(modifiedQuery)) {
                MtasCQLParser parser = new MtasCQLParser(reader);
                mtasSpanQuery = parser.parse(FIELD_CONTENT, DEFAULT_PREFIX, null, null, null);
            }
            
            return doQuery(indexReader, aRequest, FIELD_CONTENT, mtasSpanQuery,
                    annotationShortNames);
        }
        catch (mtas.parser.cql.ParseException e) {
            log.error("Unable to parse query: [{}]" + aRequest.getQuery(), e);
            throw new ExecutionException("Unable to parse query [" + aRequest.getQuery() + "]", e);
        }
        catch (Exception e) {
            log.error("Query execution error", e);
            throw (new ExecutionException("Query execution error", e));
        }
    }

    private String parseQuery(String aQuery)
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

    private Map<String, List<SearchResult>> doQuery(IndexReader aIndexReader, SearchQueryRequest aRequest,
            String field, MtasSpanQuery q, List<String> prefixes)
        throws IOException
    {
        HashMap<String, List<SearchResult>> results = new HashMap<>();

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
                            String rawSourceDocumentId = document.get(FIELD_SOURCE_DOCUMENT_ID);
                            String rawAnnotationDocumentId = document
                                    .get(FIELD_ANNOTATION_DOCUMENT_ID);
                            if (rawSourceDocumentId == null || rawAnnotationDocumentId == null) {
                                log.trace("Indexed document lacks source/annotation document IDs"
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
                                log.trace("Query limited to document {}, skipping results for "
                                        + "document {}",
                                        limitedToDocument.get().getId(), sourceDocumentId);
                                continue;
                            }

                            // FIXME Checking for every document whether an annotation document
                            // exists will heavily slow down search in larger projects. We have
                            // TWO DB accesses here, one for the source document and one checking
                            // whether there is an annotation document. This is quite bad. However,
                            // if we do not do this here, then the user will not get any results
                            // from documents which the user has not opened yet or alternatively
                            // duplicate results. Better find a solution which handles this during
                            // indexing time.
                            // See: https://github.com/inception-project/inception/issues/790
                            SourceDocument sourceDocument = documentService
                                    .getSourceDocument(project.getId(), sourceDocumentId);
                            if (documentService.existsAnnotationDocument(sourceDocument,
                                    aRequest.getUsername()) && annotationDocumentId == -1) {
                                // Exclude result if the retrieved document is a sourcedocument
                                // (that is, has annotationDocument = -1) AND it has a
                                // corresponding annotation document for this user
                                log.trace("Skipping results from indexed source document {} in"
                                        + "favor of results from the corresponding annotation "
                                        + "document", sourceDocument.getId());
                                continue;
                            }
                            else if (annotationDocumentId != -1
                                    && !aRequest.getUsername().equals(user)) {
                                // Exclude result if the retrieved document is an annotation
                                // document (that is, annotationDocument != -1 and its username
                                // is different from the quering user
                                log.trace("Skipping results from annotation document for user {} "
                                        + "which does not match the requested user {}", user,
                                        aRequest.getUsername());
                                continue;
                            }

                            // Retrieve document title
                            String documentTitle = document.get(FIELD_TITLE);

                            // String idValue = segmentReader.document(spans.docID())
                            // .getField(FIELD_ID).stringValue();
                            // log.debug("******** New doc {}-{}", + spans.docID(), idValue);

                            while (spans.nextStartPosition() != Spans.NO_MORE_POSITIONS) {
                                int matchStart = spans.startPosition();
                                int matchEnd = spans.endPosition();
                                
                                int windowStart = Math.max(matchStart - RESULT_WINDOW_SIZE, 0);
                                int windowEnd = matchEnd + RESULT_WINDOW_SIZE - 1;
                                
                                // Retrieve all indexed objects within the matching range
                                List<MtasTokenString> tokens = mtasCodecInfo.getObjectsByPositions(
                                        field, spans.docID(), windowStart, windowEnd);
                                
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
                                        .filter(t -> t.getPositionStart() >= matchStart && 
                                                t.getPositionEnd() < matchEnd)
                                        .mapToInt(MtasTokenString::getOffsetStart)
                                        .min()
                                        .getAsInt());
                                result.setOffsetEnd(tokens.stream()
                                        .filter(t -> t.getPositionStart() >= matchStart && 
                                                t.getPositionEnd() < matchEnd)
                                        .mapToInt(MtasTokenString::getOffsetEnd)
                                        .max()
                                        .getAsInt());
                                result.setTokenStart(matchStart);
                                result.setTokenLength(matchEnd - matchStart);
                                
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

    private void addToResults(HashMap<String, List<SearchResult>> aResultsMap, String aKey, SearchResult aSearchResult) {
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
        List<String> featureValues = new ArrayList<>();

        Optional<FeatureIndexingSupport> fisOpt = featureIndexingSupportRegistry
            .getIndexingSupport(aAnnotationFeature);
        if (fisOpt.isPresent()) {
            FeatureIndexingSupport fis = fisOpt.get();
            // a feature prefix is currently only used for target and source of relation-annotations.
            // however we just look at the feature value of the relation-annotation itself here, so we
            // can just use "" as feature prefix
            String groupingFeatureIndexName = fis
                .featureIndexName(aAnnotationLayer.getUiName(), "", aAnnotationFeature);

            aTokens.stream()
                .filter(t -> t.getPositionStart() == aMatchStart
                    && t.getPositionEnd() == aMatchEnd - 1
                    && extractFeatureIndexName(t).equals(
                        MtasUimaParser.getIndexedName(groupingFeatureIndexName)))
                .forEach(t -> featureValues.add(extractFeatureValue(t)));
        }
        return featureValues;
    }

    private String extractFeatureValue(MtasTokenString aMtasTokenString) {
        String tokenValue = aMtasTokenString.getValue();
        String featureValue = tokenValue.split(MtasTokenString.DELIMITER, 2)[1];
        return featureValue;
    }

    private String extractFeatureIndexName(MtasTokenString aMtasTokenString) {
        String tokenValue = aMtasTokenString.getValue();
        String featureValue = tokenValue.split(MtasTokenString.DELIMITER, 2)[0];
        return featureValue;
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
            long aAnnotationDocumentId, String aUser, CAS aCas)
        throws IOException
    {
        if (indexWriter != null) {
            try {
                log.debug(
                        "Indexing document in project [{}]({}). sourceId: {}, annotationId: {}, "
                                + "user: {}",
                        project.getName(), project.getId(), aSourceDocumentId,
                        aAnnotationDocumentId, aUser);
                
                // Prepare bytearray with document content to be indexed
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                XmiCasSerializer.serialize(aCas, null, bos, true, null);
                bos.close();

                // Calculate timestamp that will be indexed
                String timestamp = DateTools.dateToString(new Date(),
                        DateTools.Resolution.MILLISECOND);

                // Create new Lucene document
                Document doc = new Document();
                
                // Add indexed fields
                doc.add(new StringField(FIELD_ID, String.valueOf(aSourceDocumentId) + "/"
                        + String.valueOf(aAnnotationDocumentId), Field.Store.YES));
                doc.add(new StringField(FIELD_SOURCE_DOCUMENT_ID, String.valueOf(aSourceDocumentId),
                        Field.Store.YES));
                doc.add(new StringField(FIELD_ANNOTATION_DOCUMENT_ID,
                        String.valueOf(aAnnotationDocumentId), Field.Store.YES));
                doc.add(new StringField(FIELD_TITLE, aDocumentTitle, Field.Store.YES));
                doc.add(new StringField(FIELD_USER, aUser, Field.Store.YES));
                doc.add(new StringField(FIELD_TIMESTAMP, timestamp, Field.Store.YES));
                doc.add(new TextField(FIELD_CONTENT, new String(bos.toByteArray(), "UTF-8"),
                        Field.Store.NO));
    
                // Add document to the Lucene index
                indexWriter.addDocument(doc);
    
                // commit
                indexWriter.commit();
    
                log.debug(
                        "Document indexed in project [{}]({}). sourceId: {}, annotationId: {}, "
                                + "user: {}, timestamp: {}",
                        project.getName(), project.getId(), aSourceDocumentId,
                        aAnnotationDocumentId, aUser, timestamp);
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
    public void indexDocument(SourceDocument aDocument, CAS aCas) throws IOException
    {
        indexDocument(aDocument.getName(), aDocument.getId(), -1, "", aCas);
    };

    @Override
    public void indexDocument(AnnotationDocument aDocument, CAS aCas) throws IOException
    {
        log.debug("***** Indexing annotation document");
        indexDocument(aDocument.getName(), aDocument.getDocument().getId(), aDocument.getId(),
                aDocument.getUser(), aCas);
        log.debug("***** End of Indexing annotation document");
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
                    "Removing document from index in project [{}]({}). sourceId: {}, "
                            + "annotationId: {}, user: {}",
                    project.getName(), project.getId(), aSourceDocumentId, aAnnotationDocumentId,
                    aUser);

            indexWriter.deleteDocuments(new Term(FIELD_ID,
                    String.format("%d/%d", aSourceDocumentId, aAnnotationDocumentId)));

            indexWriter.commit();

            log.debug(
                    "Removed document from index in project [{}]({}). sourceId: {}, "
                            + "annotationId: {}, user: {}",
                    project.getName(), project.getId(), aSourceDocumentId, aAnnotationDocumentId,
                    aUser);
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
     * Remove a specific document from the index based on its timestamp
     * 
     * @param aSourceDocumentId
     *            The ID of the source document to be removed
     * @param aSourceDocumentId
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
        if (indexWriter != null) {
            log.debug(
                    "Removing document from index in project [{}]({}). sourceId: {}, "
                            + "annotationId: {}, user: {}, timestamp: {}",
                    project.getName(), project.getId(), aSourceDocumentId, aAnnotationDocumentId,
                    aUser, aTimestamp);

            // Prepare boolean query with the two obligatory terms (id and timestamp)
            BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder()
                    .add(new TermQuery(new Term(FIELD_ID,
                            String.format("%d/%d", aSourceDocumentId, aAnnotationDocumentId))),
                            BooleanClause.Occur.MUST)
                    .add(new TermQuery(new Term(FIELD_TIMESTAMP, aTimestamp)),
                            BooleanClause.Occur.MUST);

            // Delete document based on the previous query
            indexWriter.deleteDocuments(booleanQuery.build());

            indexWriter.commit();

            log.info(
                    "Removed document from index in project [{}]({}). sourceId: {}, "
                            + "annotationId: {}, user: {}",
                    project.getName(), project.getId(), aSourceDocumentId, aAnnotationDocumentId,
                    aUser);
        }
        else {
            log.debug(
                    "Aborted removal of document from index in project [{}]({}). sourceId: {}, "
                            + "annotationId: {}, " + "user: {} - indexWriter was null.",
                    project.getName(), project.getId(), aSourceDocumentId, aAnnotationDocumentId,
                    aUser);
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

                log.debug("Index for project [{}]({}) has been closed", project.getName(),
                        project.getId());
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

        log.info("Index for project [{}]({}) has been deleted", project.getName(),
                project.getId());
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
                log.debug("indexWriter was not open. Opening it for project [{}]({})",
                        project.getName(), project.getId());

                indexWriter = openLuceneIndex(getIndexDir());
                indexWriter.commit();

                log.debug("indexWriter has been opened for project [{}]({})", project.getName(),
                        project.getId());
            }
            catch (Exception e) {
                log.error("Unable to open indexWriter", e);
            }
        } else {
            log.debug("indexWriter is already open for project [{}]({})", project.getName(),
                    project.getId());
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
            log.debug("Creating index directory for project [{}]({})", project.getName(),
                    project.getId());
            FileUtils.forceMkdir(indexDir);

            // Open the index
            log.debug("Opening index directory for project [{}]({})", project.getName(),
                    project.getId());
            openPhysicalIndex();
            
            if (isOpen()) {
                // Index all documents of the project
                log.info("Indexing all documents in the project [{}]({})", project.getName(),
                        project.getId());
                indexAllDocuments();
                log.info("All documents have been indexed in the project [{}]({})",
                        project.getName(), project.getId());
            } else {
                log.info("Index has not been opened. No documents have been indexed.");
            }
        }
        catch (Exception e) {
            log.error("Error creating index for project [{}]({})", project.getName(),
                    project.getId(), e);
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
            log.info("Indexing all annotation documents of project [{}]({})", project.getName(),
                    project.getId());

            for (User user : projectService.listProjectUsersWithPermissions(project)) {
                users++;
                for (AnnotationDocument document : documentService.listAnnotationDocuments(project,
                        user)) {
                    indexDocument(document, documentService.readAnnotationCas(document));
                    annotationDocs++;
                }
            }

            log.info("Indexing all source documents of project [{}]({})", project.getName(),
                    project.getId());

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
    
    @Override
    public Optional<String> getTimestamp(AnnotationDocument aDocument) throws IOException
    {
        Optional<String> result = Optional.empty();

        // Prepare index searcher for accessing index
        Directory directory = FSDirectory.open(getIndexDir().toPath());
        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);

        // Prepare query for the annotation document for this annotation document
        Term term = new Term(FIELD_ID,
                String.format("%d/%d", aDocument.getDocument().getId(), aDocument.getId()));
        
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
        
        return result;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this).append("project", project)
                .append("path", getIndexDir()).toString();
    }
}

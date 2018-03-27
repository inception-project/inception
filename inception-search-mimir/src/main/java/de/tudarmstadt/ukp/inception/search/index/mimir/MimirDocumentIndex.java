/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.search.index.mimir;

import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.PROJECT_FOLDER;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.search.SearchResult;
import de.tudarmstadt.ukp.inception.search.index.Index;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.creole.ANNIEConstants;
import gate.creole.ResourceInstantiationException;
import gate.creole.tokeniser.SimpleTokeniser;
import gate.mimir.AbstractSemanticAnnotationHelper;
import gate.mimir.DocumentMetadataHelper;
import gate.mimir.IndexConfig;
import gate.mimir.IndexConfig.SemanticIndexerConfig;
import gate.mimir.IndexConfig.TokenIndexerConfig;
import gate.mimir.MimirIndex;
import gate.mimir.SemanticAnnotationHelper;
import gate.mimir.SemanticAnnotationHelper.Mode;
import gate.mimir.index.IndexException;
import gate.mimir.search.QueryEngine;
import gate.mimir.search.query.Binding;
import gate.mimir.search.query.QueryExecutor;
import gate.mimir.search.query.QueryNode;
import gate.mimir.search.query.TermQuery;
import gate.mimir.search.query.parser.QueryParser;
import gate.mimir.util.DocumentFeaturesMetadataHelper;
//import gate.mimir.search.RemoteQueryRunner;
//import gate.mimir.tool.WebUtils;
import gate.util.GateException;
import gate.util.SimpleFeatureMapImpl;
import it.unimi.di.big.mg4j.index.DowncaseTermProcessor;

public class MimirDocumentIndex
    implements Index
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    AnnotationSchemaService annotationSchemaService;
    DocumentService documentService;
    ProjectService projectService;
    Project project;

    // private String indexUrl = "http://localhost:8080/mimir-cloud/testbeto";
    // WebUtils webUtils;

    // @Value(value = "${repository.path}")
    private File resourceDir;

    private String TOKENIZER_FILE = "/mimir/DefaultTokeniser.rules";

    private String INDEX = "index";
    private int CONTEXT_SIZE = 2;
    private int TOKENS_ARRAY = 0;
    private int SPACES_ARRAY = 1;

    public static final String[] helperTypes = System
            .getProperty("helpers.to.test", "gate.mimir.db.DBSemanticAnnotationHelper")
            .split("\\s*,\\s*");

    private String ANNOTATION_HELPER = "gate.mimir.db.DBSemanticAnnotationHelper";
    private String MIMIR_NAME = "mimir";

    private static final Map<Long, MimirIndex> openIndexes = new HashMap<>();

    public MimirDocumentIndex(Project aProject, AnnotationSchemaService aAnnotationSchemaService,
            DocumentService aDocumentService, ProjectService aProjectService, String aDir)
        throws GateException
    {
        annotationSchemaService = aAnnotationSchemaService;
        documentService = aDocumentService;
        projectService = aProjectService;
        project = aProject;

        resourceDir = new File(aDir);

        // Initialize GATE in sandbox mode

        if (Gate.isInitialised()) {
            log.info("Trying to initialize Mimir... Already initialized...");
        }
        else {
            log.info("Initializing Mimir framework in sandbox mode");
            Gate.runInSandbox(true);
            Gate.init();

            log.info("Creating shutdown hook for closing the indexes");
            Runtime.getRuntime().addShutdownHook(new Thread()
            {
                @Override
                public void run()
                {
                    System.out.println("Closing all indexes...");
                    for (MimirIndex i : openIndexes.values()) {
                        try {
                            i.close();
                        }
                        catch (Exception e) {
                            log.error("Error closing index", e);
                        }
                    }
                }
            });

        }
    }

    @Override
    public boolean connect(String aUrl, String aUser, String aPassword)
    {
        // if (aUrl != null) {
        // indexUrl = aUrl;
        // }
        // webUtils = new WebUtils(aUser, aPassword);
        //
        return true;
    }

    @Override
    public ArrayList<SearchResult> executeQuery(User aUser, String aQuery, String aSortOrder,
            String... aResultField)
        throws IOException
    {
        ArrayList<SearchResult> results = new ArrayList<SearchResult>();

        MimirIndex index = null;
        try {
            // Open index
            index = getMimirIndex();

            // Make sure everything has been written to the index before querying it
            syncToDisk(index);

            // Create query engine
            QueryEngine qEngine = index.getQueryEngine();

            // Create feature map to the tokenizer
            FeatureMap fm = new SimpleFeatureMapImpl();
            fm.put(SimpleTokeniser.SIMP_TOK_RULES_URL_PARAMETER_NAME,
                    getClass().getResource(TOKENIZER_FILE));

            // Register GATE simple tokenizer
            Gate.getCreoleRegister().registerComponent(SimpleTokeniser.class);

            // Create the tokenizer
            SimpleTokeniser tokeniser = (SimpleTokeniser) Factory
                    .createResource("gate.creole.tokeniser.SimpleTokeniser", fm);

            // Parse the query
            QueryExecutor executor = QueryParser.parse(aQuery, tokeniser).getQueryExecutor(qEngine);

            // Try to get first document
            long documentId = executor.nextDocument(-1);

            // Loop for getting all documents
            while (documentId != -1) {
                // Ignore deleted documents
                if (index.isDeleted(documentId)) {
                    documentId = executor.nextDocument(-1);
                    continue;
                }

                // Get first hit inside the document
                Binding hit = executor.nextHit();

                // Loop over all hits
                while (hit != null) {
                    // If the user is not null, filter the result. When the user has an
                    // annotation document for a certain source document, only the hits of the
                    // annotation documents will be shown
                    if (aUser != null) {
                        // Get source document id from the document metadata
                        long sourceDocumentId = (long) qEngine.getDocumentMetadataField(documentId,
                                "SourceDocumentId");

                        // Get annotation document id from the document metadata
                        long annotationDocumentId = (long) qEngine
                                .getDocumentMetadataField(documentId, "AnnotationDocumentId");

                        // Get user from the document metadata
                        String user = (String) qEngine.getDocumentMetadataField(documentId, "User");

                        // Retrieve the source document
                        SourceDocument sourceDocument = documentService
                                .getSourceDocument(project.getId(), sourceDocumentId);

                        if (documentService.existsAnnotationDocument(sourceDocument, aUser)
                                && annotationDocumentId == -1) {
                            // Exclude result if the retrieved document is a sourcedocument (that
                            // is, has annotationDocument = -1) AND it has a corresponding
                            // annotation document for this user
                            hit = executor.nextHit();
                            continue;
                        }
                        else if (annotationDocumentId != -1 && !aUser.getUsername().equals(user)) {
                            // Exclude result if the retrieved document is an annotation document
                            // (that is, annotationDocument != -1 and its username is different from
                            // the quering user
                            hit = executor.nextHit();
                            continue;
                        }
                    }

                    // Build result instance for this hit
                    SearchResult result = new SearchResult();

                    result.setDocumentId(hit.getDocumentId());
                    result.setDocumentTitle(index.getQueryEngine().getDocumentTitle(documentId));
                    result.setTokenStart(hit.getTermPosition());
                    result.setTokenLength(hit.getLength());

                    result.setText(mergeTokensToSpaces(index.getQueryEngine().getHitText(hit)));
                    result.setLeftContext(mergeTokensToSpaces(
                            index.getQueryEngine().getLeftContext(hit, CONTEXT_SIZE)));
                    result.setRightContext(mergeTokensToSpaces(
                            index.getQueryEngine().getRightContext(hit, CONTEXT_SIZE)));

                    // Add instance to the result list
                    results.add(result);

                    // Get next hit inside the document
                    hit = executor.nextHit();
                }

                // Try to get next document
                documentId = executor.nextDocument(-1);
            }
        }
        catch (IOException e) {
            throw e;
        }
        catch (Exception e) {
            throw new IOException(e);
        }

        return results;
    }

    String mergeTokensToSpaces(String[][] aArrays)
    {
        StringBuilder result = new StringBuilder();
        for (int x = 0; x < aArrays[TOKENS_ARRAY].length; x++) {
            if (aArrays[TOKENS_ARRAY][x] != null) {
                result.append(aArrays[TOKENS_ARRAY][x]);
                if (aArrays[SPACES_ARRAY][x] != null) {
                    result.append(aArrays[SPACES_ARRAY][x]);
                }
            }
        }
        return result.toString();
    }

    private void indexDocument(long aSourceDocumentId, long aAnnotationDocumentId, String aUser,
            JCas aJCas)
        throws IOException
    {
        MimirIndex index = null;

        try {
            index = getMimirIndex();

            // Add document to disk
            indexJCas(index, aJCas, aSourceDocumentId, aAnnotationDocumentId, aUser);

            log.info("Document indexed in project {}. sourceId: {}, annotationId: {}, user: {}",
                    project.getName(), aSourceDocumentId, aAnnotationDocumentId, aUser);
        }
        catch (IOException e) {
            throw e;
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    };

    @Override
    public void indexDocument(SourceDocument aDocument, JCas aJCas) throws IOException
    {
        indexDocument(aDocument.getId(), -1, "", aJCas);
    };

    @Override
    public void indexDocument(AnnotationDocument aDocument, JCas aJCas) throws IOException
    {
        indexDocument(aDocument.getDocument().getId(), aDocument.getId(), aDocument.getUser(),
                aJCas);
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
        if (!hasIndex()) {
            // Index does not exist. Do nothing
            return;
        }

        MimirIndex index = null;
        try {
            index = getMimirIndex();

            // Prepare query for getting document based on its title
            String query = "{Document SourceDocumentId=\"" + String.valueOf(aSourceDocumentId)
                    + "\" AnnotationDocumentId=\"" + String.valueOf(aAnnotationDocumentId)
                    + "\" User=\"" + aUser + "\"}";

            ArrayList<SearchResult> results = executeQuery(null, query, null, null);

            // Test if got any results (the entire documents should be the only result)
            // and mark the documents as deleted
            for (SearchResult result : results) {
                index.deleteDocument(result.getDocumentId());
            }
        }
        catch (IOException e) {
            throw e;
        }
        catch (Exception e) {
            throw new IOException(e);
        }
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
     * Checks if a project already has a index
     * 
     * @return True if the index exists. False otherwise.
     */
    private boolean hasIndex()
    {
        if (getIndexDir().isDirectory()) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Checks if a project index is open
     * 
     * @return True if the index is open. False otherwise.
     */
    @Override
    public boolean isIndexOpen()
    {
        synchronized (openIndexes) {
            return openIndexes.containsKey(project.getId());
        }
    }

    /**
     * Returns a File object corresponding to the project's index folder
     * 
     * @return File object corresponding to project's index folder
     */
    private File getIndexDir()
    {
        log.debug("Directory prefix:" + resourceDir);
        return new File(resourceDir, "/" + PROJECT_FOLDER + "/" + project.getId() + "/" + INDEX);
    }

    /**
     * Drops the project's index
     * 
     */
    @Override
    public void dropIndex() throws IOException
    {
        synchronized (openIndexes) {
            if (isIndexOpen()) {
                try {
                    // Index is open, close index
                    openIndexes.get(project.getId()).close();
                    // Remove it from the list of open indexes
                    openIndexes.remove(project.getId());
                    log.info("Index for project {} has been closed", project.getName());
                }
                catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (hasIndex()) {
                // If index exists, deletes its folder
                FileUtils.deleteDirectory(getIndexDir());
                log.info("Index for project {} has been deleted", project.getName());
            }
        }
    }

    @Override
    public boolean isIndexCreated()
    {
        return hasIndex();
    }

    @Override
    public void createIndex()
    {
        try {
            createIndex(null, null);
            log.info("Index has been created for project " + project.getName());

        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void syncToDisk(MimirIndex aIndex) throws InterruptedException, ExecutionException
    {
        log.info("Syncing to disk index {}", aIndex.getIndexDirectory().getName());

        // Force flush to disk by scheduling a sync and then getting all the futures values.
        for (Future<Long> task : aIndex.requestSyncToDisk()) {
            task.get();
        }
    }

    private MimirIndex createIndex(String aKbIdentifier, String aSparqlEndpoint)
        throws IOException, IllegalArgumentException, InstantiationException,
        IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException,
        ClassNotFoundException, IndexException
    {
        File indexDir = getIndexDir();

        try {
            // Create the directory for the new index
            FileUtils.forceMkdir(indexDir);
        }
        catch (IOException e) {
            log.error("Error creating index directory for project " + project.getName(), e);
            throw new IOException("Error creating index directory for project " + project.getName()
                    + e.getMessage());
        }

        List<SemanticAnnotationHelper> semanticAnnotationHelpers = new ArrayList<>();
        List<String> annotationNames = new ArrayList<>();

        for (AnnotationLayer annotationLayer : annotationSchemaService
                .listAnnotationLayer(project)) {
            if (annotationLayer.getType().equals(WebAnnoConst.SPAN_TYPE)) {
                String annotationName = annotationLayer.getName();

                // In the case of complex type names like de.tudarmstadt...Token, stay only with the
                // last part of the name
                String[] annotationNameParts = annotationName.split("\\.");

                annotationName = annotationNameParts[annotationNameParts.length - 1];

                // Add the annotation name to the annotation name list
                annotationNames.add(annotationName);

                // Get features for this annotation
                List<AnnotationFeature> annotationFeatures = annotationSchemaService
                        .listAnnotationFeature(annotationLayer);
                String[] textFeatures = new String[annotationFeatures.size()];

                // Convert the feature names to an array
                int i = 0;
                for (AnnotationFeature annotationFeature : annotationFeatures) {
                    textFeatures[i++] = annotationFeature.getName();
                }

                // Create semantic annotation helper for the annotation and add it to the list
                semanticAnnotationHelpers.add(createHelper(
                        Class.forName(ANNOTATION_HELPER, true, Gate.getClassLoader())
                                .asSubclass(AbstractSemanticAnnotationHelper.class),
                        annotationName, null, null, null, textFeatures, null, Mode.ANNOTATION));
            }
        }

        // Special semantic annotation helper for the document identification
        annotationNames.add("Document");

        semanticAnnotationHelpers.add(createHelper(
                Class.forName(ANNOTATION_HELPER, true, Gate.getClassLoader())
                        .asSubclass(AbstractSemanticAnnotationHelper.class),
                "Document", null, null, null,
                new String[] { "SourceDocumentId", "AnnotationDocumentId", "User" }, null,
                Mode.DOCUMENT));

        // Convert list of annotation names to array
        String[] arrAnnotationNames = new String[annotationNames.size()];
        annotationNames.toArray(arrAnnotationNames);

        // Convert list of helpers to array
        SemanticAnnotationHelper[] arrSemanticAnnotationHelpers = 
                new SemanticAnnotationHelper[semanticAnnotationHelpers.size()];
        semanticAnnotationHelpers.toArray(arrSemanticAnnotationHelpers);

        // Create index config for the annotations
        SemanticIndexerConfig semanticIndexerConfig = new SemanticIndexerConfig(arrAnnotationNames,
                arrSemanticAnnotationHelpers, false);

        // // Process the KB annotation types
        //
        // if (aKbAnnotations != null) {
        // for (String kbAnnotation : aKbAnnotations) {
        // // Create sparql helper
        // SPARQLSemanticAnnotationHelper sparqlHelper = new SPARQLSemanticAnnotationHelper();
        //
        // // Set delegate for the sparql helper
        // sparqlHelper.setDelegate(createHelper(
        // Class.forName(ANNOTATION_HELPER, true, Gate.getClassLoader())
        // .asSubclass(AbstractSemanticAnnotationHelper.class),
        // kbAnnotation, null, null, null, null, new String[] { aKbIdentifier },
        // Mode.ANNOTATION));
        // sparqlHelper.setSparqlEndpoint(aSparqlEndpoint);
        //
        // // Create index config for the KB powered annotation
        // SemanticIndexerConfig semanticIndexerConfig = new SemanticIndexerConfig(
        // new String[] { kbAnnotation },
        // new SemanticAnnotationHelper[] { sparqlHelper }, true);
        //
        // semanticIndexerConfigs.add(semanticIndexerConfig);
        // }
        // }

        // Put the index configuration in an array

        SemanticIndexerConfig[] arrSemanticIndexerConfigs = new SemanticIndexerConfig[1];
        arrSemanticIndexerConfigs[0] = semanticIndexerConfig;

        // Create metadata helper
        DocumentFeaturesMetadataHelper docHelper = new DocumentFeaturesMetadataHelper(
                new String[] { "SourceDocumentId", "AnnotationDocumentId", "User" });

        // Create Mimir index configuration with the semantic configuration previously assembled
        IndexConfig indexConfig = new IndexConfig(indexDir, MIMIR_NAME,
                ANNIEConstants.TOKEN_ANNOTATION_TYPE, MIMIR_NAME,
                new TokenIndexerConfig[] {
                        new TokenIndexerConfig(ANNIEConstants.TOKEN_STRING_FEATURE_NAME,
                                DowncaseTermProcessor.getInstance(), true) },
                arrSemanticIndexerConfigs, new DocumentMetadataHelper[] { docHelper }, null);

        // Create Mimir index
        log.info("Creating Mimir index in " + indexDir);
        MimirIndex mimirIndex = new MimirIndex(indexConfig);

        // Index all documents of the project
        log.info("Indexing documents... ");
        indexAllDocuments();

        return mimirIndex;
    }

    public void indexJCas(MimirIndex aIndex, JCas aJCas, long aSourceDocumentId,
            long aAnnotationDocumentId, String aUser)
        throws GateException, InterruptedException
    {
        // Create new GATE document
        Document gateDocument = gate.Factory.newDocument("");

        // Convert UIMA to GATE
        DKPro2Gate converter = new DKPro2Gate();
        converter.convert(aJCas, gateDocument, "mimir", aSourceDocumentId, aAnnotationDocumentId,
                aUser, annotationSchemaService, project);

        // Index GATE document
        aIndex.indexDocument(gateDocument);
        log.info("Indexed document {}", gateDocument.getName());
        // log.debug(gateDocument.toString());
    }

    public void indexFiles(MimirIndex aIndex, String aPathToZipFile)
        throws IOException, ResourceInstantiationException, InterruptedException
    {
        File zipFile = new File(aPathToZipFile);
        String fileURI = zipFile.toURI().toString();
        ZipFile zip = new ZipFile(aPathToZipFile);
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                continue;
            }
            URL url = new URL("jar:" + fileURI + "!/" + entry.getName());
            Document doc = gate.Factory.newDocument(url, "UTF-8");
            aIndex.indexDocument(doc);
            log.info("Adding document to the index: " + entry.getName());
        }
    }

    /**
     * Parses a string query
     * 
     * @param aQuery
     *            The query to be parsed
     * @return Array of query nodes for the parsed query
     */
    private QueryNode[] parseQuery(String aQuery)
    {
        String[] terms = aQuery.split(" ");

        QueryNode[] results = new QueryNode[terms.length];

        int x = 0;
        for (String term : terms) {
            TermQuery termQuery = new TermQuery(ANNIEConstants.TOKEN_STRING_FEATURE_NAME, term);
            results[x++] = termQuery;
        }
        return results;
    }

    /**
     * Execute a mimir string query
     * 
     * @param aIndex
     *            The index to be queried
     * @param aQuery
     *            The string query
     * @return ArrayList of bindings with the query results
     */
    public ArrayList<Binding> executeQuery(MimirIndex aIndex, String aQuery, int a) throws Exception
    {
        // Create query engine
        QueryEngine qEngine = aIndex.getQueryEngine();

        // Create feature map to the tokenizer
        FeatureMap fm = new SimpleFeatureMapImpl();
        fm.put(SimpleTokeniser.SIMP_TOK_RULES_URL_PARAMETER_NAME,
                getClass().getResource(TOKENIZER_FILE));

        // Register GATE simple tokenizer
        Gate.getCreoleRegister().registerComponent(SimpleTokeniser.class);

        // Create the tokenizer
        SimpleTokeniser tokeniser = (SimpleTokeniser) Factory
                .createResource("gate.creole.tokeniser.SimpleTokeniser", fm);

        // Parse the query
        QueryExecutor executor = QueryParser.parse(aQuery, tokeniser).getQueryExecutor(qEngine);

        // Get the results
        ArrayList<Binding> results = new ArrayList<Binding>();

        long documentId = executor.nextDocument(-1);
        while (documentId != -1) {
            Binding hit = executor.nextHit();

            // Ignore deleted documents
            if (aIndex.isDeleted(hit.getDocumentId())) {
                documentId = executor.nextDocument(-1);
                continue;
            }

            while (hit != null) {
                results.add(hit);
                hit = executor.nextHit();
            }
            documentId = executor.nextDocument(-1);
        }

        return results;
    }

    /**
     * Creates a mimir semantic annotation helper
     * 
     * @param helperClass
     *            The class of the helper
     * @param annType
     *            The annotation type which the helper is to process
     * @param nominalFeatures
     *            An array of names of the features to be indexed that have nominal values
     * @param integerFeatures
     *            An array of names of the features to be indexed that have integer values
     * @param floatFeatures
     *            An array of names of the features to be indexed that have float values
     * @param textFeatures
     *            An array of names of the features to be indexed that have arbitrary text values
     * @param uriFeatures
     *            An array of names of the features to be indexed that have URIs as values
     * @param mode
     *            whether to index actual GATE annotations and their features, or to index a single
     *            “virtual” annotation spanning the whole document from document-level features. Can
     *            be: Mode.ANNOTATION or Mode.DOCUMENT
     * @return A mimir semantic annotation helper
     */
    private static SemanticAnnotationHelper createHelper(
            Class<? extends AbstractSemanticAnnotationHelper> helperClass, String annType,
            String[] nominalFeatures, String[] integerFeatures, String[] floatFeatures,
            String[] textFeatures, String[] uriFeatures, SemanticAnnotationHelper.Mode mode)
        throws InstantiationException, IllegalAccessException
    {
        AbstractSemanticAnnotationHelper helper = helperClass.newInstance();
        helper.setAnnotationType(annType);
        helper.setNominalFeatures(nominalFeatures);
        helper.setIntegerFeatures(integerFeatures);
        helper.setFloatFeatures(floatFeatures);
        helper.setTextFeatures(textFeatures);
        helper.setUriFeatures(uriFeatures);
        helper.setMode(mode);
        return helper;
    }

    private MimirIndex getMimirIndex()
        throws IllegalArgumentException, InstantiationException, IllegalAccessException,
        InvocationTargetException, SecurityException, NoSuchMethodException, ClassNotFoundException,
        IOException, IndexException
    {
        synchronized (openIndexes) {
            MimirIndex index = openIndexes.get(project.getId());

            if (index == null) {
                if (!hasIndex()) {
                    // Index does not exist. Create it.
                    index = createIndex(null, null);
                }
                else {
                    try {
                        index = new MimirIndex(getIndexDir());
                    }
                    catch (Exception e) {
                        // Unable to open index - try recreating it.
                        dropIndex();
                        index = createIndex(null, null);
                    }
                }

                openIndexes.put(project.getId(), index);
            }

            return index;
        }
    }

    private void indexAllDocuments()
    {
        log.info("Indexing all annotation documents of project {}", project.getName());

        for (User user : projectService.listProjectUsersWithPermissions(project)) {
            for (AnnotationDocument document : documentService.listAnnotationDocuments(project,
                    user)) {
                try {
                    indexDocument(document, documentService.readAnnotationCas(document));
                }
                catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        log.info("Indexing all source documents of project {}", project.getName());
        for (SourceDocument document : documentService.listSourceDocuments(project)) {
            try {
                indexDocument(document, documentService.readInitialCas(document));
            }
            catch (IOException | CASException | ResourceInitializationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void openIndex()
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void closeIndex()
    {
        // TODO Auto-generated method stub

    }
}

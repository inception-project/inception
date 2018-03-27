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

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.Future;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.api.io.ResourceCollectionReaderBase;
import de.tudarmstadt.ukp.dkpro.core.io.conll.Conll2002Reader;
import de.tudarmstadt.ukp.dkpro.core.io.gate.internal.DKPro2Gate;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.creole.ANNIEConstants;
import gate.creole.tokeniser.SimpleTokeniser;
import gate.mimir.AbstractSemanticAnnotationHelper;
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
import gate.mimir.search.query.parser.ParseException;
import gate.mimir.search.query.parser.QueryParser;
import gate.mimir.sparql.SPARQLSemanticAnnotationHelper;
import gate.util.GateException;
import gate.util.SimpleFeatureMapImpl;
import it.unimi.di.big.mg4j.index.DowncaseTermProcessor;

public class TestMimir
{
//    static InceptionMimir mimir;

    @BeforeClass

    public static void configureTests()
        throws Exception
    {
        // Initialize Mimir with GATE in sandbox mode
//        mimir = new InceptionMimir(true, null);
//        log.info("Initializing Mimir framework in sandbox mode");
        Gate.runInSandbox(true);
        Gate.init();
    }

//    @Test
//    public void testSimpleIndexAndSearch()
//        throws Exception
//    {
//        File indexDir = Files.createTempDirectory("mimir-index-test").toFile();
//
//        // Create Mimir index
//        MimirIndex index = mimir.createIndex(indexDir);
//
//        // Create UIMA document
//        JCas uimaDocument = JCasFactory.createText("This is a test.", "en");
//
//        // Apply segmenter to create tokens and sentences
//        AnalysisEngine segmenter = createEngine(BreakIteratorSegmenter.class);
//        SimplePipeline.runPipeline(uimaDocument, segmenter);
//
//        // Add document to disk
//        mimir.indexJCas(index, uimaDocument, "Test document");
//
//        // Sync index to disk
//        mimir.SyncToDisk(index);
//
//        // Execute query and get results
//        String result = "";
//        for (Binding binding : mimir.executeQuery(index, "is")) {
//            result += Arrays.toString(index.getQueryEngine().getHitText(binding)[0]);
//        }
//
//        assertEquals(result, "[is]");
//        index.close();
//    }
//
//    @Test
    public void testRawSimpleIndexAndSearch()
        throws Exception
    {
        File indexDir = Files.createTempDirectory("mimir-index-test").toFile();

        // Create basic Mimir index configuration
        IndexConfig indexConfig = new IndexConfig(indexDir, "mimir",
                ANNIEConstants.TOKEN_ANNOTATION_TYPE, "mimir",
                new TokenIndexerConfig[] {
                        new TokenIndexerConfig(ANNIEConstants.TOKEN_STRING_FEATURE_NAME,
                                DowncaseTermProcessor.getInstance(), true) },
                new SemanticIndexerConfig[] {}, null, null);

        // Create Mimir index
        MimirIndex index = new MimirIndex(indexConfig);

        // Create UIMA document
        JCas uimaDocument = JCasFactory.createText("This is a test.", "en");

        // Apply segmenter to create tokens and sentences
        AnalysisEngine segmenter = createEngine(BreakIteratorSegmenter.class);
        SimplePipeline.runPipeline(uimaDocument, segmenter);

        // Create new GATE document
        Document gateDocument = gate.Factory.newDocument("");
        gateDocument.setName("Test document");

        // Convert UIMA to GATE
        DKPro2Gate converter = new DKPro2Gate();
        converter.convert(uimaDocument, gateDocument, "mimir");

        // Index GATE document
        index.indexDocument(gateDocument);

        // Force flush to disk by scheduling a sync and then getting all the futures values.
        for (Future<Long> task : index.requestSyncToDisk()) {
            System.out.printf("Index %d%n", task.get());
        }

        QueryEngine qEngine = index.getQueryEngine();

        FeatureMap fm = new SimpleFeatureMapImpl();
        fm.put(SimpleTokeniser.SIMP_TOK_RULES_URL_PARAMETER_NAME,
                new File("src/test/resources/gate/DefaultTokeniser.rules").toURI().toURL());

        Gate.getCreoleRegister().registerComponent(SimpleTokeniser.class);

        SimpleTokeniser tokeniser = (SimpleTokeniser) Factory
                .createResource("gate.creole.tokeniser.SimpleTokeniser", fm);

        // // Parser query
        // DefaultTokeniser tokeniser = (DefaultTokeniser) Factory.createResource(
        // "gate.creole.tokeniser.DefaultTokeniser");

        QueryExecutor executor = QueryParser.parse("is", tokeniser).getQueryExecutor(qEngine);
        // QueryExecutor executor = new TermQuery(ANNIEConstants.TOKEN_STRING_FEATURE_NAME, "is")
        // .getQueryExecutor(qEngine);

        // Execute simple term query
        // QueryExecutor executor = new TermQuery(ANNIEConstants.TOKEN_STRING_FEATURE_NAME, "is")
        // .getQueryExecutor(qEngine);

        // Get results
        String result = "";
        while (executor.nextDocument(-1) != -1) {
            Binding hit = executor.nextHit();
            while (hit != null) {
                result += Arrays.toString(qEngine.getHitText(hit)[0]);
                hit = executor.nextHit();
            }
        }

        assertEquals(result, "[is]");
        index.close();
    }

//    @Test
    public void testRawSimpleIndexAndSearchNE()
        throws Exception
    {
        File indexDir = Files.createTempDirectory("mimir-index-test-ne").toFile();

        // Create basic Mimir index configuration
        IndexConfig indexConfig = new IndexConfig(indexDir, "mimir",
                ANNIEConstants.TOKEN_ANNOTATION_TYPE, "mimir",
                new TokenIndexerConfig[] {
                        new TokenIndexerConfig(ANNIEConstants.TOKEN_STRING_FEATURE_NAME,
                                DowncaseTermProcessor.getInstance(), true) },
                new SemanticIndexerConfig[] {
                        new SemanticIndexerConfig(new String[] { "NamedEntity" },
                                new SemanticAnnotationHelper[] { createHelper(
                                        Class.forName("gate.mimir.db.DBSemanticAnnotationHelper",
                                                true, Gate.getClassLoader())
                                        .asSubclass(AbstractSemanticAnnotationHelper.class),
                                        "NamedEntity", null, null, null, null, null,
                                        Mode.ANNOTATION) },
                                true),
                        new SemanticIndexerConfig(new String[] { "Person" },
                                new SemanticAnnotationHelper[] { createHelper(
                                        Class.forName("gate.mimir.db.DBSemanticAnnotationHelper",
                                                true, Gate.getClassLoader())
                                        .asSubclass(AbstractSemanticAnnotationHelper.class),
                                        "Person", null, null, null, null, null, Mode.ANNOTATION) },
                                true),
                        new SemanticIndexerConfig(new String[] { "Location" },
                                new SemanticAnnotationHelper[] { createHelper(
                                        Class.forName("gate.mimir.db.DBSemanticAnnotationHelper",
                                                true, Gate.getClassLoader())
                                                .asSubclass(AbstractSemanticAnnotationHelper.class),
                                        "Location", null, null, null, null, null,
                                        Mode.ANNOTATION) },
                                true),
                        new SemanticIndexerConfig(new String[] { "Organization" },
                                new SemanticAnnotationHelper[] { createHelper(
                                        Class.forName("gate.mimir.db.DBSemanticAnnotationHelper",
                                                true, Gate.getClassLoader())
                                        .asSubclass(AbstractSemanticAnnotationHelper.class),
                                        "Organization", null, null, null, null, null,
                                        Mode.ANNOTATION) },
                                true), },
                null, null);

        // Create Mimir index
        MimirIndex index = new MimirIndex(indexConfig);

        // Create Conll reader for Named Entities
        CollectionReaderDescription reader = createReaderDescription(Conll2002Reader.class,
                ResourceCollectionReaderBase.PARAM_SOURCE_LOCATION,
                "src/test/resources/texts/ner2002_test.conll");

        // Run pipeline
        SimplePipeline.runPipeline(reader);

        for (JCas uimaDocument : SimplePipeline.iteratePipeline(reader)) {
            try {
                // Create new GATE document
                Document gateDocument = gate.Factory.newDocument("");
                gateDocument.setName("Test document");

                // Convert UIMA to GATE
                DKPro2Gate converter = new DKPro2Gate();
                converter.convert(uimaDocument, gateDocument, "mimir");

                // Index GATE document
                index.indexDocument(gateDocument);
            }
            catch (GateException e) {
                throw new AnalysisEngineProcessException(e);
            }
        }

        // Force flush to disk by scheduling a sync and then getting all the futures values.
        for (Future<Long> task : index.requestSyncToDisk()) {
            System.out.printf("Index %d%n", task.get());
        }

        // Create query engine
        QueryEngine qEngine = index.getQueryEngine();

        // Create and register tokenizer
        FeatureMap fm = new SimpleFeatureMapImpl();
        fm.put(SimpleTokeniser.SIMP_TOK_RULES_URL_PARAMETER_NAME,
                new File("src/test/resources/gate/DefaultTokeniser.rules").toURI().toURL());

        Gate.getCreoleRegister().registerComponent(SimpleTokeniser.class);

        SimpleTokeniser tokeniser = (SimpleTokeniser) Factory
                .createResource("gate.creole.tokeniser.SimpleTokeniser", fm);

        // Execute query for named entity
        QueryExecutor executor = QueryParser.parse("{NamedEntity}", tokeniser)
                .getQueryExecutor(qEngine);

        // Get results
        String result = "";
        while (executor.nextDocument(-1) != -1) {
            Binding hit = executor.nextHit();
            while (hit != null) {
                result += Arrays.toString(qEngine.getHitText(hit)[0]);
                hit = executor.nextHit();
            }
        }

        // Test results
        assertEquals("[Wolff][Argentina][Del, Bosque][Real, Madrid]", result);

        index.close();
    }

    @Test
    public void sparqlTest()
        throws Exception
    {
        // Create new sparql helper
        SPARQLSemanticAnnotationHelper sparqlHelper = new SPARQLSemanticAnnotationHelper();

        // Set delegate
        sparqlHelper.setDelegate(createHelper(
                Class.forName("gate.mimir.db.DBSemanticAnnotationHelper", true,
                        Gate.getClassLoader()).asSubclass(AbstractSemanticAnnotationHelper.class),
                "NamedEntity", null, null, null, null, new String[] { "inst" }, Mode.ANNOTATION));

        // Set sparql endpoint to DBPedia
        sparqlHelper.setSparqlEndpoint("http://dbpedia.org/sparql");

        File indexDir = Files.createTempDirectory("mimir-index-test-ne").toFile();

        // Create index configuration
        IndexConfig indexConfig = new IndexConfig(indexDir, "mimir",
                ANNIEConstants.TOKEN_ANNOTATION_TYPE, "mimir",
                new TokenIndexerConfig[] {
                        new TokenIndexerConfig(ANNIEConstants.TOKEN_STRING_FEATURE_NAME,
                                DowncaseTermProcessor.getInstance(), true) },
                new SemanticIndexerConfig[] {
                        new SemanticIndexerConfig(new String[] { "NamedEntity" },
                                new SemanticAnnotationHelper[] { sparqlHelper }, true) },
                null, null);

        // Create Mimir index
        MimirIndex index = new MimirIndex(indexConfig);

        // Create Conll reader for Named Entities
        CollectionReaderDescription reader = createReaderDescription(Conll2002Reader.class,
                ResourceCollectionReaderBase.PARAM_SOURCE_LOCATION,
                "src/test/resources/texts/ner2002_test.conll");

        // Run pipeline
        SimplePipeline.runPipeline(reader);

        for (JCas uimaDocument : SimplePipeline.iteratePipeline(reader)) {
            try {
                // Create new GATE document
                Document gateDocument = gate.Factory.newDocument("");
                gateDocument.setName("Test document");

                // Convert UIMA to GATE
                DKPro2Gate converter = new DKPro2Gate();
                converter.convert(uimaDocument, gateDocument, "mimir");

                // Get annotation set from GATE document
                AnnotationSet annSet = gateDocument.getAnnotations("mimir");

                // Loop over annotations searching for named entities
                for (Annotation ann : annSet) {
                    if (ann.getType().equals("NamedEntity")) {
                        FeatureMap fm = ann.getFeatures();
                        // Manually add DBPedia URI feature to "Del Bosque" named entity
                        if (fm.get("string").equals("Del Bosque")) {
                            fm.put("inst", "http://dbpedia.org/resource/Vicente_del_Bosque");
                            ann.setFeatures(fm);
                            annSet.add(ann);
                        }
                    }
                }

                // Index GATE document
                index.indexDocument(gateDocument);
            }
            catch (GateException e) {
                throw new AnalysisEngineProcessException(e);
            }
        }

        // Force flush to disk by scheduling a sync and then getting all the futures values.
        for (Future<Long> task : index.requestSyncToDisk()) {
            System.out.printf("Index %d%n", task.get());
        }

        // Create query engine
        QueryEngine qEngine = index.getQueryEngine();

        // Create and register tokenizer
        FeatureMap fm = new SimpleFeatureMapImpl();
        fm.put(SimpleTokeniser.SIMP_TOK_RULES_URL_PARAMETER_NAME,
                new File("src/test/resources/gate/DefaultTokeniser.rules").toURI().toURL());

        Gate.getCreoleRegister().registerComponent(SimpleTokeniser.class);

        SimpleTokeniser tokeniser = (SimpleTokeniser) Factory
                .createResource("gate.creole.tokeniser.SimpleTokeniser", fm);

        // Execute sparql for retrieving entities with birthplace = Salamanca
        String queryText = "{NamedEntity sparql = \"SELECT DISTINCT ?inst WHERE "
                + "{?inst <http://dbpedia.org/ontology/birthPlace> "
                + "<http://dbpedia.org/resource/Salamanca> }\"}";
        QueryExecutor executor = QueryParser.parse(queryText, tokeniser).getQueryExecutor(qEngine);

        // Get results
        String result = "";
        while (executor.nextDocument(-1) != -1) {
            Binding hit = executor.nextHit();
            while (hit != null) {
                result += Arrays.toString(qEngine.getHitText(hit)[0]);
                hit = executor.nextHit();
            }
        }

        // Test results
        assertEquals("[Del, Bosque]", result);

        index.close();

    }

    // @Test
    public void testQuery()
        throws IllegalArgumentException, InstantiationException, IllegalAccessException,
        InvocationTargetException, SecurityException, NoSuchMethodException, ClassNotFoundException,
        IOException, IndexException, InterruptedException, GateException, ParseException,
        ResourceInitializationException, AnalysisEngineProcessException
    {
//        InceptionMimir mimir = new InceptionMimir("/home/beto/svn/mimir/trunk/mimir-test/gate-home");
//        // MimirIndex index = mimir.createIndex("mimir-index-test");
//        MimirIndex index = new MimirIndex(new File(
//                "/home/beto/Documents/ukp/technical/inception/mimir/index/index-7819205242822493548.mimir/"));

        // mimir.indexFiles(index, "/opt/mimir-5.4/mimir-test/data/gatexml-output.zip");

        // CollectionReaderDescription reader = createReaderDescription(TextReader.class,
        // ResourceCollectionReaderBase.PARAM_SOURCE_LOCATION, "src/test/resources/texts",
        // ResourceCollectionReaderBase.PARAM_PATTERNS, "[+]*.txt");
        // AnalysisEngineDescription segmenter =
        // createEngineDescription(BreakIteratorSegmenter.class,
        // BreakIteratorSegmenter.PARAM_WRITE_SENTENCE, false);
        //
        // DKPro2Gate converter = new DKPro2Gate();
        // Document document;
        // for (JCas jcas : SimplePipeline.iteratePipeline(reader, segmenter)) {
        // try {
        // document = new DocumentImpl();
        // converter.convert(jcas, document);
        // }
        // catch (GateException e) {
        // throw new AnalysisEngineProcessException(e);
        // }
        // index.indexDocument(document);
        // }

        /*
         * String queryText = "London"; QueryNode query = QueryParser.parse(queryText); String
         * result = mimir.executeQuery(index, query); System.out.println("Result: " + result);
         * index.close();
         */
    }

    // @Test
    // public void testMimirQuery()
    // {
    // SearchDataSourceMimir mimir = new SearchDataSourceMimir();
    // mimir.connect("xxx", "yyy", "http://localhost:8081/mimir-cloud/testbeto");
    //
    // SearchResults documents = mimir.executeQuery("{Person}", "", "");
    // System.out.println(documents.toString());
    // }
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

}

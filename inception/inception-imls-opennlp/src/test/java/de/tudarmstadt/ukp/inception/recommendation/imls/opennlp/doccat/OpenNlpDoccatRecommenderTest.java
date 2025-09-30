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
package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.doccat;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.EXCLUSIVE_WRITE_ACCESS;
import static de.tudarmstadt.ukp.inception.support.test.recommendation.RecommenderTestHelper.getPredictions;
import static java.util.Arrays.asList;
import static org.apache.commons.io.IOUtils.lineIterator;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.dkpro.core.api.datasets.Dataset;
import org.dkpro.core.api.datasets.DatasetFactory;
import org.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import org.dkpro.core.api.resources.CompressionUtils;
import org.dkpro.core.tokit.BreakIteratorSegmenter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.IncrementalSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.PercentageBasedSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.PredictionContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.support.test.recommendation.DkproTestHelper;
import de.tudarmstadt.ukp.inception.support.test.recommendation.RecommenderTestHelper;

public class OpenNlpDoccatRecommenderTest
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final File cache = DkproTestHelper.getCacheFolder();
    private static final DatasetFactory loader = new DatasetFactory(cache);

    private RecommenderContext context;
    private Recommender recommender;
    private OpenNlpDoccatRecommenderTraits traits;

    @BeforeEach
    public void setUp()
    {
        context = new RecommenderContext();
        recommender = buildRecommender();
        traits = new OpenNlpDoccatRecommenderTraits();
        traits.setNumThreads(2);
        traits.setTrainingSetSizeLimit(250);
        traits.setPredictionLimit(250);
    }

    @Test
    public void thatTrainingWorks() throws Exception
    {
        var sut = new OpenNlpDoccatRecommender(recommender, traits);
        var casList = loadArxivData();

        sut.train(context, casList);

        assertThat(context.get(OpenNlpDoccatRecommender.KEY_MODEL)).as("Model has been set")
                .isPresent();
    }

    @Test
    public void thatPredictionWorks() throws Exception
    {
        var sut = new OpenNlpDoccatRecommender(recommender, traits);
        var casList = loadArxivData();

        var cas = casList.get(0);
        try (var session = CasStorageSession.open()) {
            session.add(AnnotationSet.forTest("testCas"), EXCLUSIVE_WRITE_ACCESS, cas);
            RecommenderTestHelper.addPredictionFeatures(cas, NamedEntity.class, "value");
        }

        sut.train(context, asList(cas));

        sut.predict(new PredictionContext(context), cas);

        var predictions = getPredictions(cas, NamedEntity.class);

        assertThat(predictions).as("Predictions have been written to CAS").isNotEmpty();
    }

    @Test
    public void thatEvaluationWorks() throws Exception
    {
        var splitStrategy = new PercentageBasedSplitter(0.8, 10);
        var sut = new OpenNlpDoccatRecommender(recommender, traits);
        var casList = loadArxivData();

        var result = sut.evaluate(casList, splitStrategy);

        var fscore = result.computeF1Score();
        var accuracy = result.computeAccuracyScore();
        var precision = result.computePrecisionScore();
        var recall = result.computeRecallScore();

        LOG.info("F1-Score:  {}", fscore);
        LOG.info("Accuracy:  {}", accuracy);
        LOG.info("Precision: {}", precision);
        LOG.info("Recall:    {}", recall);

        assertThat(fscore).isStrictlyBetween(0.0, 1.0);
        assertThat(precision).isStrictlyBetween(0.0, 1.0);
        assertThat(recall).isStrictlyBetween(0.0, 1.0);
        assertThat(accuracy).isStrictlyBetween(0.0, 1.0);
    }

    @Test
    public void thatIncrementalNerEvaluationWorks() throws Exception
    {
        var splitStrategy = new IncrementalSplitter(0.8, 250, 10);
        var sut = new OpenNlpDoccatRecommender(recommender, traits);
        var casList = loadArxivData();

        int i = 0;
        while (splitStrategy.hasNext() && i < 3) {
            splitStrategy.next();

            var score = sut.evaluate(casList, splitStrategy).computeF1Score();

            LOG.info("Score:  {}", score);

            assertThat(score).isStrictlyBetween(0.0, 1.0);

            i++;
        }
    }

    private List<CAS> loadArxivData() throws IOException, UIMAException
    {
        var ds = loader.load("sentence-classification-en");
        return loadData(ds, Arrays.stream(ds.getDataFiles()) //
                .filter(file -> file.getName().contains("arxiv")) //
                .toArray(File[]::new));
    }

    private List<CAS> loadData(Dataset ds, File... files) throws UIMAException, IOException
    {
        var reader = createReader(Reader.class, //
                Reader.PARAM_PATTERNS, files, //
                Reader.PARAM_LANGUAGE, ds.getLanguage());

        var segmenter = createEngine( //
                BreakIteratorSegmenter.class, //
                BreakIteratorSegmenter.PARAM_WRITE_SENTENCE, false);

        var casList = new ArrayList<CAS>();
        while (reader.hasNext()) {
            var cas = JCasFactory.createJCas();
            reader.getNext(cas.getCas());
            segmenter.process(cas);
            casList.add(cas.getCas());
        }
        return casList;
    }

    private static Recommender buildRecommender()
    {
        var layer = new AnnotationLayer();
        layer.setName(NamedEntity.class.getName());

        var feature = new AnnotationFeature();
        feature.setName("value");

        var recommender = new Recommender();
        recommender.setLayer(layer);
        recommender.setFeature(feature);

        return recommender;
    }

    public static class Reader
        extends JCasResourceCollectionReader_ImplBase
    {
        @Override
        public void getNext(JCas aJCas) throws IOException, CollectionException
        {
            var res = nextFile();
            initCas(aJCas, res);

            var text = new StringBuilder();

            try (var is = new BufferedInputStream(
                    CompressionUtils.getInputStream(res.getLocation(), res.getInputStream()))) {

                try (var i = lineIterator(is, "UTF-8")) {
                    while (i.hasNext()) {
                        var line = i.next();

                        if (line.startsWith("#")) {
                            continue;
                        }

                        var fields = line.split("\\s", 2);

                        if (text.length() > 0) {
                            text.append("\n");
                        }

                        int sentenceBegin = text.length();
                        text.append(fields[1]);

                        var ne = new NamedEntity(aJCas, sentenceBegin, text.length());
                        ne.setValue(fields[0]);
                        ne.addToIndexes();

                        new Sentence(aJCas, sentenceBegin, text.length()).addToIndexes();
                    }
                }

                aJCas.setDocumentText(text.toString());
            }
        }
    }
}

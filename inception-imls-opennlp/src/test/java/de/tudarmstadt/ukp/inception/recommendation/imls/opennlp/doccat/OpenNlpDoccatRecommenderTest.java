/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Before;
import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.datasets.Dataset;
import de.tudarmstadt.ukp.dkpro.core.api.datasets.DatasetFactory;
import de.tudarmstadt.ukp.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.resources.CompressionUtils;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.IncrementalSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.PercentageBasedSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.api.type.PredictedSpan;

public class OpenNlpDoccatRecommenderTest
{
    private static File cache = DkproTestContext.getCacheFolder();
    private static DatasetFactory loader = new DatasetFactory(cache);

    private RecommenderContext context;
    private Recommender recommender;
    private OpenNlpDoccatRecommenderTraits traits;

    @Before
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
        OpenNlpDoccatRecommender sut = new OpenNlpDoccatRecommender(recommender, traits);
        List<CAS> casList = loadArxivData();

        sut.train(context, casList);

        assertThat(context.get(OpenNlpDoccatRecommender.KEY_MODEL))
            .as("Model has been set")
            .isPresent();
    }

    @Test
    public void thatPredictionWorks() throws Exception
    {
        OpenNlpDoccatRecommender sut = new OpenNlpDoccatRecommender(recommender, traits);
        List<CAS> casList = loadArxivData();
        
        CAS cas = casList.get(0);
        
        sut.train(context, asList(cas));

        sut.predict(context, cas);

        Collection<PredictedSpan> predictions = select(cas.getJCas(), PredictedSpan.class);

        assertThat(predictions).as("Predictions have been written to CAS")
            .isNotEmpty();
    }

    @Test
    public void thatEvaluationWorks() throws Exception
    {
        DataSplitter splitStrategy = new PercentageBasedSplitter(0.8, 10);
        OpenNlpDoccatRecommender sut = new OpenNlpDoccatRecommender(recommender, traits);
        List<CAS> casList = loadArxivData();

        EvaluationResult result = sut.evaluate(casList, splitStrategy);

        double fscore = result.computeF1Score();
        double accuracy = result.computeAccuracyScore();
        double precision = result.computePrecisionScore();
        double recall = result.computeRecallScore();

        System.out.printf("F1-Score: %f%n", fscore);
        System.out.printf("Accuracy: %f%n", accuracy);
        System.out.printf("Precision: %f%n", precision);
        System.out.printf("Recall: %f%n", recall);
        
        assertThat(fscore).isBetween(0.0, 1.0);
        assertThat(precision).isBetween(0.0, 1.0);
        assertThat(recall).isBetween(0.0, 1.0);
        assertThat(accuracy).isBetween(0.0, 1.0);
    }

    @Test
    public void thatIncrementalNerEvaluationWorks() throws Exception
    {
        IncrementalSplitter splitStrategy = new IncrementalSplitter(0.8, 250, 10);
        OpenNlpDoccatRecommender sut = new OpenNlpDoccatRecommender(recommender, traits);
        List<CAS> casList = loadArxivData();

        int i = 0;
        while (splitStrategy.hasNext() && i < 3) {
            splitStrategy.next();
            
            double score = sut.evaluate(casList, splitStrategy).computeF1Score();

            System.out.printf("Score: %f%n", score);

            assertThat(score).isBetween(0.0, 1.0);
            
            i++;
        }
    }

    private List<CAS> loadArxivData() throws IOException, UIMAException
    {
        Dataset ds = loader.load("sentence-classification-en");
        return loadData(ds, Arrays.stream(ds.getDataFiles())
                .filter(file -> file.getName().contains("arxiv"))
                .toArray(File[]::new));
    }

    private List<CAS> loadData(Dataset ds, File ... files) throws UIMAException, IOException
    {
        CollectionReader reader = createReader(Reader.class,
                Reader.PARAM_PATTERNS, files, 
                Reader.PARAM_LANGUAGE, ds.getLanguage());

        AnalysisEngine segmenter = createEngine(BreakIteratorSegmenter.class,
                BreakIteratorSegmenter.PARAM_WRITE_SENTENCE, false);
        
        List<CAS> casList = new ArrayList<>();
        while (reader.hasNext()) {
            JCas cas = JCasFactory.createJCas();
            reader.getNext(cas.getCas());
            segmenter.process(cas);
            casList.add(cas.getCas());
        }
        return casList;
    }

    private static Recommender buildRecommender()
    {
        AnnotationLayer layer = new AnnotationLayer();
        layer.setName(NamedEntity.class.getName());
        
        AnnotationFeature feature = new AnnotationFeature();
        feature.setName("value");

        Recommender recommender = new Recommender();
        recommender.setLayer(layer);
        recommender.setFeature(feature);

        return recommender;
    }
    
    public static class Reader extends JCasResourceCollectionReader_ImplBase
    {
        @Override
        public void getNext(JCas aJCas) throws IOException, CollectionException
        {
            Resource res = nextFile();
            initCas(aJCas, res);

            StringBuilder text = new StringBuilder();
            
            try (InputStream is = new BufferedInputStream(
                    CompressionUtils.getInputStream(res.getLocation(), res.getInputStream()))) {
                LineIterator i = IOUtils.lineIterator(is, "UTF-8");
                try {
                    while (i.hasNext()) {
                        String line = i.next();
                        
                        if (line.startsWith("#")) {
                            continue;
                        }
                        
                        String[] fields = line.split("\\s", 2);
                        
                        if (text.length() > 0) {
                            text.append("\n");
                        }
                        
                        int sentenceBegin = text.length();
                        text.append(fields[1]);
                        
                        NamedEntity ne = new NamedEntity(aJCas,  sentenceBegin, text.length());
                        ne.setValue(fields[0]);
                        ne.addToIndexes();
                        
                        new Sentence(aJCas, sentenceBegin, text.length()).addToIndexes();
                    }
                }
                finally {
                    LineIterator.closeQuietly(i);
                }
                
                aJCas.setDocumentText(text.toString());
            }
        }
    }
}

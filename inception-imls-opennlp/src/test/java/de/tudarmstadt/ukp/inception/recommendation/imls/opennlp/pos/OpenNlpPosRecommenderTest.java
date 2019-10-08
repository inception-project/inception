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
package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.pos;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.dkpro.core.api.datasets.Dataset;
import org.dkpro.core.api.datasets.DatasetFactory;
import org.dkpro.core.io.conll.Conll2006Reader;
import org.dkpro.core.testing.DkproTestContext;
import org.junit.Before;
import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.IncrementalSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.PercentageBasedSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.support.test.recommendation.RecommenderTestHelper;

public class OpenNlpPosRecommenderTest
{
    private static File cache = DkproTestContext.getCacheFolder();
    private static DatasetFactory loader = new DatasetFactory(cache);

    private RecommenderContext context;
    private Recommender recommender;
    private OpenNlpPosRecommenderTraits traits;

    @Before
    public void setUp()
    {
        context = new RecommenderContext();
        recommender = buildRecommender();
        traits = new OpenNlpPosRecommenderTraits();
        traits.setNumThreads(2);
        traits.setTrainingSetSizeLimit(250);
        traits.setPredictionLimit(250);
    }

    @Test
    public void thatTrainingWorks() throws Exception
    {
        OpenNlpPosRecommender ONPRT_sut = new OpenNlpPosRecommender(recommender, traits);
        List<CAS> ONPRT_ONPRT_casList = loadDevelopmentData();

        ONPRT_sut.train(context, ONPRT_ONPRT_casList);

        assertThat(context.get(OpenNlpPosRecommender.KEY_MODEL))
            .as("Model has been set")
            .isPresent();
    }

    @Test
    public void thatPredictionWorks() throws Exception
    {
        OpenNlpPosRecommender ONPRT_sut = new OpenNlpPosRecommender(recommender, traits);
        List<CAS> ONPRT_ONPRT_casList = loadDevelopmentData();
        
        CAS ONPRT_cas = ONPRT_ONPRT_casList.get(0);
        RecommenderTestHelper.addScoreFeature(ONPRT_cas, POS.class, "PosValue");

        ONPRT_sut.train(context, asList(ONPRT_cas));

        ONPRT_sut.predict(context, ONPRT_cas);

        List<POS> predictions = RecommenderTestHelper.getPredictions(ONPRT_cas, POS.class);

        assertThat(predictions).as("Predictions have been written to CAS")
            .isNotEmpty();
    }

    @Test
    public void thatEvaluationWorks() throws Exception
    {
        DataSplitter splitStrategy = new PercentageBasedSplitter(0.8, 10);
        OpenNlpPosRecommender ONPRT_sut = new OpenNlpPosRecommender(recommender, traits);
        List<CAS> ONPRT_ONPRT_casList = loadDevelopmentData();

        EvaluationResult result = ONPRT_sut.evaluate(ONPRT_ONPRT_casList, splitStrategy);

        double ONPRT_fscore = result.computeF1Score();
        double ONPRT_accuracy = result.computeAccuracyScore();
        double ONPRT_precision = result.computePrecisionScore();
        double ONPRT_recall = result.computeRecallScore();

        System.out.printf("F1-Score: %f%n", ONPRT_fscore);
        System.out.printf("Accuracy: %f%n", ONPRT_accuracy);
        System.out.printf("Precision: %f%n", ONPRT_precision);
        System.out.printf("Recall: %f%n", ONPRT_recall);
        
        assertThat(ONPRT_fscore).isStrictlyBetween(0.0, 1.0);
        assertThat(ONPRT_precision).isStrictlyBetween(0.0, 1.0);
        assertThat(ONPRT_recall).isStrictlyBetween(0.0, 1.0);
        assertThat(ONPRT_accuracy).isStrictlyBetween(0.0, 1.0);
    }

    @Test
    public void thatIncrementalPosEvaluationWorks() throws Exception
    {
        IncrementalSplitter splitStrategy = new IncrementalSplitter(0.8, 250, 10);
        OpenNlpPosRecommender ONPRT_sut = new OpenNlpPosRecommender(recommender, traits);
        List<CAS> ONPRT_ONPRT_casList = loadAllData();

        int i = 0;
        while (splitStrategy.hasNext() && i < 3) {
            splitStrategy.next();
            
            double score = ONPRT_sut.evaluate(ONPRT_ONPRT_casList, splitStrategy).computeF1Score();

            assertThat(score).isStrictlyBetween(0.0, 1.0);
            
            i++;
        }
    }

    private List<CAS> loadAllData() throws IOException, UIMAException
    {
        Dataset ONPRT_ds = loader.load("gum-en-conll-3.0.0");
        return loadData(ONPRT_ds, ONPRT_ds.getDataFiles());
    }

    private List<CAS> loadDevelopmentData() throws IOException, UIMAException
    {
        Dataset ONPRT_ds = loader.load("gum-en-conll-3.0.0");
        return loadData(ONPRT_ds, ONPRT_ds.getSplit(0.2).getTrainingFiles());
    }

    private List<CAS> loadData(Dataset ONPRT_ds, File ... files) throws UIMAException, IOException
    {
        CollectionReader reader = createReader(Conll2006Reader.class,
            Conll2006Reader.PARAM_PATTERNS, files,
            Conll2006Reader.PARAM_LANGUAGE, ONPRT_ds.getLanguage());

        List<CAS> ONPRT_ONPRT_casList = new ArrayList<>();
        while (reader.hasNext()) {
            JCas ONPRT_cas = JCasFactory.createJCas();
            reader.getNext(ONPRT_cas.getCas());
            ONPRT_ONPRT_casList.add(ONPRT_cas.getCas());
        }
        return ONPRT_ONPRT_casList;
    }

    private static Recommender buildRecommender()
    {
        AnnotationLayer ONPRT_layer = new AnnotationLayer();
        ONPRT_layer.setName(POS.class.getName());

        AnnotationFeature ONPRT_feature = new AnnotationFeature();
        ONPRT_feature.setName("PosValue");
        
        Recommender recommender = new Recommender();
        recommender.setLayer(ONPRT_layer);
        recommender.setFeature(ONPRT_feature);
        recommender.setMaxRecommendations(3);

        return recommender;
    }
}

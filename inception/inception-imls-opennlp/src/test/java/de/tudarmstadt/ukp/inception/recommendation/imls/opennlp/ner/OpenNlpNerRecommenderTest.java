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
package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.ner;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.EXCLUSIVE_WRITE_ACCESS;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.dkpro.core.api.datasets.DatasetValidationPolicy.CONTINUE;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.dkpro.core.api.datasets.Dataset;
import org.dkpro.core.api.datasets.DatasetFactory;
import org.dkpro.core.io.conll.Conll2002Reader;
import org.dkpro.core.io.conll.Conll2002Reader.ColumnSeparators;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageSession;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.IncrementalSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.PercentageBasedSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.support.test.recommendation.DkproTestHelper;
import de.tudarmstadt.ukp.inception.support.test.recommendation.RecommenderTestHelper;

public class OpenNlpNerRecommenderTest
{
    private static final File cache = DkproTestHelper.getCacheFolder();
    private static final DatasetFactory loader = new DatasetFactory(cache);

    private RecommenderContext context;
    private Recommender recommender;
    private OpenNlpNerRecommenderTraits traits;

    @BeforeEach
    public void setUp()
    {
        context = new RecommenderContext();
        recommender = buildRecommender();
        traits = new OpenNlpNerRecommenderTraits();
        traits.setNumThreads(2);
        traits.setTrainingSetSizeLimit(250);
        traits.setPredictionLimit(250);
    }

    @Test
    public void thatTrainingWorks() throws Exception
    {
        OpenNlpNerRecommender sut = new OpenNlpNerRecommender(recommender, traits);
        List<CAS> casList = loadDevelopmentData();

        sut.train(context, casList);

        assertThat(context.get(OpenNlpNerRecommender.KEY_MODEL)).as("Model has been set")
                .isPresent();
    }

    @Test
    public void thatPredictionWorks() throws Exception
    {
        OpenNlpNerRecommender sut = new OpenNlpNerRecommender(recommender, traits);
        List<CAS> casList = loadDevelopmentData();

        CAS cas = casList.get(0);
        try (CasStorageSession session = CasStorageSession.open()) {
            session.add("testCas", EXCLUSIVE_WRITE_ACCESS, cas);
            RecommenderTestHelper.addScoreFeature(cas, NamedEntity.class, "value");
        }

        sut.train(context, asList(cas));

        sut.predict(context, cas);

        Collection<NamedEntity> predictions = JCasUtil.select(cas.getJCas(), NamedEntity.class);

        assertThat(predictions).as("Predictions have been written to CAS").isNotEmpty();
    }

    @Test
    public void thatEvaluationWorks() throws Exception
    {
        DataSplitter splitStrategy = new PercentageBasedSplitter(0.8, 10);
        OpenNlpNerRecommender sut = new OpenNlpNerRecommender(recommender, traits);
        List<CAS> casList = loadDevelopmentData();

        EvaluationResult result = sut.evaluate(casList, splitStrategy);

        double fscore = result.computeF1Score();
        double accuracy = result.computeAccuracyScore();
        double precision = result.computePrecisionScore();
        double recall = result.computeRecallScore();

        System.out.printf("F1-Score: %f%n", fscore);
        System.out.printf("Accuracy: %f%n", accuracy);
        System.out.printf("Precision: %f%n", precision);
        System.out.printf("Recall: %f%n", recall);

        assertThat(fscore).isStrictlyBetween(0.0, 1.0);
        assertThat(precision).isStrictlyBetween(0.0, 1.0);
        assertThat(recall).isStrictlyBetween(0.0, 1.0);
        assertThat(accuracy).isStrictlyBetween(0.0, 1.0);
    }

    @Test
    public void thatIncrementalNerEvaluationWorks() throws Exception
    {
        IncrementalSplitter splitStrategy = new IncrementalSplitter(0.8, 250, 10);
        OpenNlpNerRecommender sut = new OpenNlpNerRecommender(recommender, traits);
        List<CAS> casList = loadAllData();

        int i = 0;
        while (splitStrategy.hasNext() && i < 3) {
            splitStrategy.next();

            double score = sut.evaluate(casList, splitStrategy).computeF1Score();

            System.out.printf("Score: %f%n", score);

            assertThat(score).isStrictlyBetween(0.0, 1.0);

            i++;
        }
    }

    private List<CAS> loadAllData() throws IOException, UIMAException
    {
        try {
            Dataset ds = loader.load("germeval2014-de", CONTINUE);
            return loadData(ds, ds.getDataFiles());
        }
        catch (Exception e) {
            // Workaround for https://github.com/dkpro/dkpro-core/issues/1469
            assumeThat(e).isNotInstanceOf(FileNotFoundException.class);
            throw e;
        }
    }

    private List<CAS> loadDevelopmentData() throws IOException, UIMAException
    {
        try {
            Dataset ds = loader.load("germeval2014-de", CONTINUE);
            return loadData(ds, ds.getDefaultSplit().getDevelopmentFiles());
        }
        catch (Exception e) {
            // Workaround for https://github.com/dkpro/dkpro-core/issues/1469
            assumeThat(e).isNotInstanceOf(FileNotFoundException.class);
            throw e;
        }
    }

    private List<CAS> loadData(Dataset ds, File... files) throws UIMAException, IOException
    {
        CollectionReader reader = createReader( //
                Conll2002Reader.class, //
                Conll2002Reader.PARAM_PATTERNS, files, //
                Conll2002Reader.PARAM_LANGUAGE, ds.getLanguage(), //
                Conll2002Reader.PARAM_COLUMN_SEPARATOR, ColumnSeparators.TAB.getName(), //
                Conll2002Reader.PARAM_HAS_TOKEN_NUMBER, true, //
                Conll2002Reader.PARAM_HAS_HEADER, true, //
                Conll2002Reader.PARAM_HAS_EMBEDDED_NAMED_ENTITY, true);

        List<CAS> casList = new ArrayList<>();
        while (reader.hasNext()) {
            JCas cas = JCasFactory.createJCas();
            reader.getNext(cas.getCas());
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
}

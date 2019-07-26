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
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch;

import static de.tudarmstadt.ukp.dkpro.core.api.datasets.DatasetValidationPolicy.CONTINUE;
import static de.tudarmstadt.ukp.inception.support.test.recommendation.RecommenderTestHelper.getPredictions;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Before;
import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.datasets.Dataset;
import de.tudarmstadt.ukp.dkpro.core.api.datasets.DatasetFactory;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.io.conll.Conll2002Reader;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.IncrementalSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.PercentageBasedSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.model.GazeteerEntry;
import de.tudarmstadt.ukp.inception.support.test.recommendation.RecommenderTestHelper;

public class StringMatchingRecommenderTest
{
    private static File cache = DkproTestContext.getCacheFolder();
    private static DatasetFactory loader = new DatasetFactory(cache);

    private RecommenderContext context;
    private Recommender recommender;
    private StringMatchingRecommenderTraits traits;

    @Before
    public void setUp() {
        context = new RecommenderContext();
        recommender = buildRecommender();
        traits = new StringMatchingRecommenderTraits();
    }

    @Test
    public void thatTrainingWorks() throws Exception
    {
        StringMatchingRecommender sut = new StringMatchingRecommender(recommender, traits);
        List<CAS> casList = loadDevelopmentData();

        sut.train(context, casList);

        assertThat(context.get(StringMatchingRecommender.KEY_MODEL))
            .as("Model has been set")
            .isNotNull();
    }

    @Test
    public void thatPredictionWorks() throws Exception
    {
        StringMatchingRecommender sut = new StringMatchingRecommender(recommender, traits);
        List<CAS> casList = loadDevelopmentData();
        
        CAS cas = casList.get(0);
        RecommenderTestHelper.addScoreFeature(cas, NamedEntity.class, "value");
        
        sut.train(context, asList(cas));

        sut.predict(context, cas);

        List<NamedEntity> predictions = getPredictions(cas, NamedEntity.class);

        assertThat(predictions).as("Predictions have been written to CAS")
            .isNotEmpty();
        
        assertThat(predictions).as("Score is positive")
            .allMatch(prediction -> getScore(prediction) > 0.0 && getScore(prediction) <= 1.0 );

        assertThat(predictions).as("Some score is not perfect")
            .anyMatch(prediction -> getScore(prediction) > 0.0 && getScore(prediction) < 1.0 );
        
        assertThat(predictions)
            .as("There is no score explanation")
            .allMatch(prediction -> getScoreExplanation(prediction) == null);
    }

    @Test
    public void thatPredictionForNoLabelAnnosWorks() throws Exception
    {
        StringMatchingRecommender sut = new StringMatchingRecommender(recommender, traits);
        CAS cas = getTestCasNoLabelLabels();
        RecommenderTestHelper.addScoreFeature(cas, NamedEntity.class, "value");

        sut.train(context, asList(cas));

        sut.predict(context, cas);

        List<NamedEntity> predictions = getPredictions(cas, NamedEntity.class);

        assertThat(predictions).as("Has all null labels").extracting(NamedEntity::getValue)
                .containsOnlyNulls();
    }

    private CAS getTestCasNoLabelLabels() throws Exception
    {
        Dataset ds = loader.load("germeval2014-de", CONTINUE);
        CAS cas = loadData(ds, ds.getDataFiles()[0]).get(0);
        Type neType = CasUtil.getAnnotationType(cas, NamedEntity.class);
        Feature valFeature = neType.getFeatureByBaseName("value");
        JCasUtil.select(cas.getJCas(), NamedEntity.class)
                .forEach(ne -> ne.setFeatureValueFromString(valFeature, null));

        return cas;
    }

    @Test
    public void thatPredictionWithPretrainigWorks() throws Exception
    {
        StringMatchingRecommender sut = new StringMatchingRecommender(recommender, traits);
        List<CAS> casList = loadDevelopmentData();

        CAS cas = casList.get(0);
        RecommenderTestHelper.addScoreFeature(cas, NamedEntity.class, "value");

        List<GazeteerEntry> gazeteer = new ArrayList<>();
        gazeteer.add(new GazeteerEntry("Toyota", "ORG"));
        gazeteer.add(new GazeteerEntry("Deutschland", "LOC"));
        gazeteer.add(new GazeteerEntry("Deutschland", "GPE"));

        sut.pretrain(gazeteer);

        sut.train(context, emptyList());

        sut.predict(context, cas);

        List<NamedEntity> predictions = getPredictions(cas, NamedEntity.class);

        assertThat(predictions).as("Predictions have been written to CAS")
             .isNotEmpty();

        assertThat(predictions)
            .as("Score is positive")
            .allMatch(prediction -> getScore(prediction) > 0.0 && getScore(prediction) <= 1.0 );

        assertThat(predictions)
            .as("Some score is not perfect")
            .anyMatch(prediction -> getScore(prediction) > 0.0 && getScore(prediction) < 1.0 );
    }


    @Test
    public void thatEvaluationWorks() throws Exception
    {
        DataSplitter splitStrategy = new PercentageBasedSplitter(0.8, 10);
        StringMatchingRecommender sut = new StringMatchingRecommender(recommender, traits);
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
    public void thatEvaluationProducesSpecificResults() throws Exception
    {
        String text = "Hans Peter, Peter und Hans. Blabla Peter. Und so weiter Darmstadt, Darmstadt.";
        String[] vals = new String[] { "PER", "PER", "PER", "PER", "LOC", "ORG" };
        int[][] indices = new int[][] { { 0, 9 }, { 12, 16 }, { 22, 25 }, { 35, 39 }, { 56, 64 },
                { 67, 75 } };
        int[][] sentIndices = new int[][] { { 0, 26 }, {27, 40}, {41, 65}, {66, 76} };
        int[][] tokenIndices = new int[][] { { 0, 3 }, { 5, 9 }, { 10, 10 }, { 12, 16 }, { 18, 20 },
                { 22, 25 }, { 26, 26 }, { 28, 33 }, { 35, 39 }, { 40, 40 }, { 42, 44 }, { 46, 47 },
                { 49, 54 }, { 56, 64 }, { 65, 65 }, {67, 75}, {76, 76} };

        List<CAS> testCas = getTestNECas(text, vals, indices, sentIndices, tokenIndices);

        int expectedTestSize = 2;
        int expectedTrainSize = 2;

        EvaluationResult result = new StringMatchingRecommender(buildRecommender(), null)
                .evaluate(testCas, new PercentageBasedSplitter(0.5, 500));

        assertThat(result.getTestSetSize()).as("correct test size").isEqualTo(expectedTestSize);
        assertThat(result.getTrainingSetSize()).as("correct training size")
                .isEqualTo(expectedTrainSize);

        // sentences are not processed in sequence, here second and fourth are in test set.
        assertThat(result.computeAccuracyScore()).as("correct accuracy").isEqualTo(0.5);
        assertThat(result.computePrecisionScore()).as("correct precision").isEqualTo(1.0 / 3);
        assertThat(result.computeRecallScore()).as("correct recall").isEqualTo(1.0 / 3);
        assertThat(result.computeF1Score()).as("correct f1").isEqualTo((2.0 / 9) / (2.0 / 3));
    }
    
    private List<CAS> getTestNECas(String aText, String[] aVals, int[][] aNEIndices,
            int[][] aSentIndices, int[][] aTokenIndices)
        throws Exception
    {
        JCas jcas = JCasFactory.createText(aText, "de");

        for (int j = 0; j < aSentIndices.length; j++) {
            Sentence newSent = new Sentence(jcas, aSentIndices[j][0], aSentIndices[j][1]);
            newSent.addToIndexes();
        }

        for (int k = 0; k < aTokenIndices.length; k++) {
            Token newToken = new Token(jcas, aTokenIndices[k][0], aTokenIndices[k][1]);
            newToken.addToIndexes();
        }

        for (int i = 0; i < aVals.length; i++) {
            NamedEntity newNE = new NamedEntity(jcas, aNEIndices[i][0], aNEIndices[i][1]);
            newNE.setValue(aVals[i]);
            newNE.addToIndexes();
        }

        List<CAS> casses = new ArrayList<>();
        casses.add(jcas.getCas());

        return casses;
    }
    
    @Test
    public void thatEvaluationSkippingWorks() throws Exception
    {
        DataSplitter splitStrategy = new PercentageBasedSplitter(0.8, 10);
        StringMatchingRecommender sut = new StringMatchingRecommender(recommender, traits);

        double score = sut.evaluate(asList(), splitStrategy).computeF1Score();

        System.out.printf("Score: %f%n", score);
        
        assertThat(score).isEqualTo(0.0);
    }

    @Test
    public void thatIncrementalNerEvaluationWorks() throws Exception
    {
        IncrementalSplitter splitStrategy = new IncrementalSplitter(0.8, 5000, 10);
        StringMatchingRecommender sut = new StringMatchingRecommender(recommender, traits);
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
        Dataset ds = loader.load("germeval2014-de", CONTINUE);
        return loadData(ds, ds.getDataFiles());
    }

    private List<CAS> loadDevelopmentData() throws IOException, UIMAException
    {
        Dataset ds = loader.load("germeval2014-de", CONTINUE);
        return loadData(ds, ds.getDefaultSplit().getDevelopmentFiles());
    }

    private List<CAS> loadData(Dataset ds, File ... files) throws UIMAException, IOException
    {
        CollectionReader reader = createReader(Conll2002Reader.class,
            Conll2002Reader.PARAM_PATTERNS, files, 
            Conll2002Reader.PARAM_LANGUAGE, ds.getLanguage(), 
            Conll2002Reader.PARAM_COLUMN_SEPARATOR, Conll2002Reader.ColumnSeparators.TAB.getName(),
            Conll2002Reader.PARAM_HAS_TOKEN_NUMBER, true, 
            Conll2002Reader.PARAM_HAS_HEADER, true, 
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
        recommender.setMaxRecommendations(3);

        return recommender;
    }

    private static Double getScore(AnnotationFS fs)
    {
        return RecommenderTestHelper.getScore(fs, "value");
    }
    
    private static String getScoreExplanation(AnnotationFS fs)
    {
        return RecommenderTestHelper.getScoreExplanation(fs, "value");
    }
}

/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.inception.recommendation.imls.datamajority;

import static de.tudarmstadt.ukp.inception.support.test.recommendation.RecommenderTestHelper.addScoreFeature;
import static de.tudarmstadt.ukp.inception.support.test.recommendation.RecommenderTestHelper.getPredictions;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.assertj.core.api.Assertions.assertThat;
import static org.dkpro.core.api.datasets.DatasetValidationPolicy.CONTINUE;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
import org.dkpro.core.api.datasets.Dataset;
import org.dkpro.core.api.datasets.DatasetFactory;
import org.dkpro.core.io.conll.Conll2002Reader;
import org.dkpro.core.testing.DkproTestContext;
import org.junit.Before;
import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.IncrementalSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.PercentageBasedSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.support.test.recommendation.RecommenderTestHelper;

public class DataMajorityNerRecommenderTest
{
    private static File cache = DkproTestContext.getCacheFolder();
    private static DatasetFactory loader = new DatasetFactory(cache);

    private RecommenderContext context;
    private Recommender recommender;

    @Before
    public void setUp() {
        context = new RecommenderContext();
        recommender = buildRecommender();
    }

    @Test
    public void thatTrainingWorks() throws Exception
    {
        DataMajorityNerRecommender DMNR_sut = new DataMajorityNerRecommender(recommender);

        List<CAS> DMNR_casList = loadDevelopmentData();

        DMNR_sut.train(context, DMNR_casList);

        assertThat(context.get(DataMajorityNerRecommender.KEY_MODEL))
            .as("Model has been set")
            .isNotNull();
    }

    @Test
    public void thatPredictionWorks() throws Exception
    {
        DataMajorityNerRecommender DMNR_sut = new DataMajorityNerRecommender(recommender);

		List<CAS> DMNR_casList = loadDevelopmentData();

        CAS DMNR_cas = DMNR_casList.get(0);


        addScoreFeature(DMNR_cas, NamedEntity.class.getName(), "value");
        
        DMNR_sut.train(context, asList(DMNR_cas));

        DMNR_sut.predict(context, DMNR_cas);

        Collection<NamedEntity> predictions = getPredictions(DMNR_cas, NamedEntity.class);

        assertThat(predictions).as("Predictions have been written to CAS")
            .isNotEmpty();
        
        assertThat(predictions).as("Score is positive")
            .allMatch(prediction -> getScore(prediction) > 0.0 && getScore(prediction) <= 1.0 );

        assertThat(predictions).as("Some score is not perfect")
            .anyMatch(prediction -> getScore(prediction) > 0.0 && getScore(prediction) < 1.0 );
    }

    @Test
    public void thatEvaluationWorks() throws Exception
    {
        DataSplitter splitStrategy = new PercentageBasedSplitter(0.8, 10);
        DataMajorityNerRecommender DMNR_sut = new DataMajorityNerRecommender(recommender);
        List<CAS> DMNR_casList = loadDevelopmentData();

        EvaluationResult result = DMNR_sut.evaluate(DMNR_casList, splitStrategy);
		/**
		 *this is rename method of DataMajorityNerRecommenderTest.java
		 */
        double DMNR_fscore = result.computeF1Score();
		/**
		 *this is rename method of DataMajorityNerRecommenderTest.java
		 */
        double DMNR_accuracy = result.computeAccuracyScore();
		/**
		 *this is rename method of DataMajorityNerRecommenderTest.java
		 */
        double DMNR_precision = result.computePrecisionScore();
		/**
		 *this is rename method of DataMajorityNerRecommenderTest.java
		 */
        double DMNR_recall = result.computeRecallScore();

        System.out.printf("F1-Score: %f%n", DMNR_fscore);
        System.out.printf("Accuracy: %f%n", DMNR_accuracy);
        System.out.printf("Precision: %f%n", DMNR_precision);
        System.out.printf("Recall: %f%n", DMNR_recall);
        
        assertThat(DMNR_fscore).isStrictlyBetween(0.0, 1.0);
        assertThat(DMNR_precision).isStrictlyBetween(0.0, 1.0);
        assertThat(DMNR_recall).isStrictlyBetween(0.0, 1.0);
        assertThat(DMNR_accuracy).isStrictlyBetween(0.0, 1.0);
    }
    
    @Test
    public void thatEvaluationProducesSpecificResults() throws Exception
    {
        String text = "Angela Dorothea Merkel ist eine deutsche Politikerin (CDU) und seit dem 22. "
                + "November 2005 Bundeskanzlerin der Bundesrepublik Deutschland. "
                + "Merkel wuchs in der DDR auf und war dort als Physikerin am Zentralinstitut "
                + "für Physikalische Chemie wissenschaftlich tätig.";
        String[] vals = new String[] { "PER", "LOC", "LOC", "PER", "LOC", "ORG" };
        int[][] indices = new int[][] { { 0, 21 }, { 54, 56 }, { 110, 135 }, { 138, 143 },
                { 158, 160 }, { 197, 236 } };

        List<CAS> testCas = getTestNECas(text, vals, indices);

        int expectedTestSize = 3;
        int expectedTrainSize = 3;

        EvaluationResult result = new DataMajorityNerRecommender(buildRecommender())
                .evaluate(testCas, new PercentageBasedSplitter(0.5, 500));

        assertThat(result.getTestSetSize()).as("correct test size").isEqualTo(expectedTestSize);
        assertThat(result.getTrainingSetSize()).as("correct training size")
                .isEqualTo(expectedTrainSize);

        assertThat(result.computeAccuracyScore()).as("correct DMNR_accuracy").isEqualTo(1.0 / 3);
        assertThat(result.computePrecisionScore()).as("correct DMNR_precision").isEqualTo(1.0 / 9);
        assertThat(result.computeRecallScore()).as("correct DMNR_recall").isEqualTo(1.0 / 3);
        assertThat(result.computeF1Score()).as("correct f1").isEqualTo( (2.0 / 27) / (4.0 / 9));
    }
    
    @Test
    public void thatEvaluationWithNoClassesWorks() throws Exception
    {
        DataSplitter splitStrategy = new PercentageBasedSplitter(0.8, 10);
        DataMajorityNerRecommender DMNR_sut = new DataMajorityNerRecommender(recommender);
        List<CAS> DMNR_casList = new ArrayList<>();
        DMNR_casList.add(getTestCasNoLabelLabels());

        double score = DMNR_sut.evaluate(DMNR_casList, splitStrategy).computeF1Score();

        System.out.printf("Score: %f%n", score);
        
        assertThat(score).isBetween(0.0, 1.0);
    }

    private CAS getTestCasNoLabelLabels() throws Exception
    {
        Dataset DMNR_ds = loader.load("germeval2014-de", CONTINUE);
        CAS DMNR_cas = loadData(DMNR_ds, DMNR_ds.getDataFiles()[0]).get(0);
        Type neType = CasUtil.getAnnotationType(DMNR_cas, NamedEntity.class);
        Feature valFeature = neType.getFeatureByBaseName("value");
        JCasUtil.select(DMNR_cas.getJCas(), NamedEntity.class)
                .forEach(ne -> ne.setFeatureValueFromString(valFeature, null));

        return DMNR_cas;
    }
    @Test
    public void thatIncrementalNerEvaluationWorks() throws Exception
    {
        IncrementalSplitter splitStrategy = new IncrementalSplitter(0.8, 5000, 10);
        DataMajorityNerRecommender DMNR_sut = new DataMajorityNerRecommender(recommender);
        List<CAS> DMNR_casList = loadAllData();

        int i = 0;
        while (splitStrategy.hasNext() && i < 3) {
            splitStrategy.next();
            
            double score = DMNR_sut.evaluate(DMNR_casList, splitStrategy).computeF1Score();

            System.out.printf("Score: %f%n", score);

            assertThat(score).isStrictlyBetween(0.0, 1.0);
            
            i++;
        }
    }

    private List<CAS> getTestNECas(String aText, String[] aVals, int[][] aIndices) throws Exception
    {
		//this is rename method of DataMajorityNerRecommenderTest.java
        JCas jDMNR_cas = JCasFactory.createText(aText, "de");

        for (int i = 0; i < aVals.length; i++) {
            NamedEntity newNE = new NamedEntity(jDMNR_cas, aIndices[i][0], aIndices[i][1]);
            newNE.setValue(aVals[i]);
            newNE.addToIndexes();
        }

        List<CAS> DMNR_casses = new ArrayList<>();
        DMNR_casses.add(jDMNR_cas.getCas());

        return DMNR_casses;
    }

    private List<CAS> loadAllData() throws IOException, UIMAException
    {
        Dataset DMNR_ds = loader.load("germeval2014-de", CONTINUE);
        return loadData(DMNR_ds, DMNR_ds.getDataFiles());
    }

    private List<CAS> loadDevelopmentData() throws IOException, UIMAException
    {
	    //this is rename method of DataMajorityNerRecommenderTest.java+
        Dataset DMNR_ds = loader.load("germeval2014-de", CONTINUE);
        return loadData(DMNR_ds, DMNR_ds.getDefaultSplit().getDevelopmentFiles());
    }

    private List<CAS> loadData(Dataset DMNR_ds, File ... files) throws UIMAException, IOException
    {
	     //this is rename method of DataMajorityNerRecommenderTest.java
        CollectionReader reader = createReader(Conll2002Reader.class,
            Conll2002Reader.PARAM_PATTERNS, files, 
            Conll2002Reader.PARAM_LANGUAGE, DMNR_ds.getLanguage(), 
            Conll2002Reader.PARAM_COLUMN_SEPARATOR, Conll2002Reader.ColumnSeparators.TAB.getName(),
            Conll2002Reader.PARAM_HAS_TOKEN_NUMBER, true, 
            Conll2002Reader.PARAM_HAS_HEADER, true, 
            Conll2002Reader.PARAM_HAS_EMBEDDED_NAMED_ENTITY, true);

        List<CAS> DMNR_casList = new ArrayList<>();
        while (reader.hasNext()) {
            JCas DMNR_cas = JCasFactory.createJCas();
            reader.getNext(DMNR_cas.getCas());
            DMNR_casList.add(DMNR_cas.getCas());
        }
        return DMNR_casList;
    }

    private static Recommender buildRecommender()
    {
	    //this is rename method of DataMajorityNerRecommenderTest.java
        AnnotationLayer DMNR_layer = new AnnotationLayer();
        DMNR_layer.setName(NamedEntity.class.getName());

        AnnotationFeature DMNR_feature = new AnnotationFeature();
        DMNR_feature.setName("value");

        Recommender recommender = new Recommender();
        recommender.setLayer(DMNR_layer);
        recommender.setFeature(DMNR_feature);
        recommender.setMaxRecommendations(3);

        return recommender;
    }

    private static double getScore(AnnotationFS aAnnotationFS)
    {
        return RecommenderTestHelper.getScore(aAnnotationFS, "value");
    }


}

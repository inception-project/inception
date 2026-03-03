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
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.EXCLUSIVE_WRITE_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.CHARACTERS;
import static de.tudarmstadt.ukp.inception.support.test.recommendation.RecommenderTestHelper.getPredictions;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.dkpro.core.api.datasets.DatasetValidationPolicy.CONTINUE;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.apache.uima.fit.util.CasUtil;
import org.dkpro.core.api.datasets.Dataset;
import org.dkpro.core.api.datasets.DatasetFactory;
import org.dkpro.core.io.conll.Conll2002Reader;
import org.dkpro.core.io.conll.Conll2002Reader.ColumnSeparators;
import org.junit.jupiter.api.AfterEach;
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
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.IncrementalSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.PercentageBasedSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.PredictionContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazeteer.model.GazeteerEntry;
import de.tudarmstadt.ukp.inception.support.test.recommendation.DkproTestHelper;
import de.tudarmstadt.ukp.inception.support.test.recommendation.RecommenderTestHelper;

public class StringMatchingRecommenderTest
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final File cache = DkproTestHelper.getCacheFolder();
    private static final DatasetFactory loader = new DatasetFactory(cache);

    private RecommenderContext context;
    private Recommender recommender;
    private StringMatchingRecommenderTraits traits;
    private CasStorageSession casStorageSession;

    @BeforeEach
    public void setUp()
    {
        casStorageSession = CasStorageSession.open();
        context = new RecommenderContext();
        recommender = buildRecommender();
        traits = new StringMatchingRecommenderTraits();
    }

    @AfterEach
    public void tearDown()
    {
        casStorageSession.close();
    }

    @Test
    public void thatTrainingWorks() throws Exception
    {
        var sut = new StringMatchingRecommender(recommender, traits);
        var casList = loadDevelopmentData();

        sut.train(context, casList);

        assertThat(context.get(StringMatchingRecommender.KEY_MODEL)).as("Model has been set")
                .isNotNull();
    }

    @Test
    public void thatPredictionWorks() throws Exception
    {
        var sut = new StringMatchingRecommender(recommender, traits);
        var casList = loadDevelopmentData();

        var cas = casList.get(0);
        RecommenderTestHelper.addPredictionFeatures(cas, NamedEntity.class, "value");

        sut.train(context, Collections.singletonList(cas));

        sut.predict(new PredictionContext(context), cas);

        var predictions = getPredictions(cas, NamedEntity.class);

        assertThat(predictions).as("Predictions have been written to CAS").isNotEmpty();

        assertThat(predictions).as("Score is positive")
                .allMatch(prediction -> getScore(prediction) > 0.0 && getScore(prediction) <= 1.0);

        assertThat(predictions).as("Some score is not perfect")
                .anyMatch(prediction -> getScore(prediction) > 0.0 && getScore(prediction) < 1.0);

        assertThat(predictions).as("There is no score explanation")
                .allMatch(prediction -> getScoreExplanation(prediction) == null);
    }

    @Test
    public void thatPredictionForNoLabelAnnosWorks() throws Exception
    {
        var sut = new StringMatchingRecommender(recommender, traits);
        var cas = getTestCasNoLabelLabels();
        RecommenderTestHelper.addPredictionFeatures(cas, NamedEntity.class, "value");

        sut.train(context, Collections.singletonList(cas));

        sut.predict(new PredictionContext(context), cas);

        var predictions = getPredictions(cas, NamedEntity.class);

        assertThat(predictions).as("Has all null labels").extracting(NamedEntity::getValue)
                .containsOnlyNulls();
    }

    @Test
    public void thatPredictionForCharacterLevelLayerWorks() throws Exception
    {
        recommender.getLayer().setAnchoringMode(CHARACTERS);

        var sut = new StringMatchingRecommender(recommender, traits);

        var jcas = JCasFactory.createJCas();
        var builder = new TokenBuilder<>(Token.class, Sentence.class);
        builder.buildTokens(jcas, "John Smith. Peter Johnheim .");
        var cas = jcas.getCas();
        casStorageSession.add(AnnotationSet.forTest("cas"), EXCLUSIVE_WRITE_ACCESS, cas);

        RecommenderTestHelper.addPredictionFeatures(cas, NamedEntity.class, "value");

        var gazeteer = asList( //
                new GazeteerEntry("John Smith", "ORG"), //
                new GazeteerEntry("Peter John", "LOC"));

        sut.pretrain(gazeteer, context);

        sut.predict(new PredictionContext(context), cas);

        var predictions = getPredictions(cas, NamedEntity.class);

        assertThat(predictions) //
                .extracting(NamedEntity::getCoveredText)//
                .contains("John Smith", "Peter John");
    }

    @Test
    public void thatPredictionForCrossSentenceLayerWorks() throws Exception
    {
        recommender.getLayer().setCrossSentence(true);

        var sut = new StringMatchingRecommender(recommender, traits);

        var jcas = JCasFactory.createJCas();
        var builder = new TokenBuilder<>(Token.class, Sentence.class);
        builder.buildTokens(jcas, "John Smith .\nPeter Johnheim .");
        var cas = jcas.getCas();
        casStorageSession.add(AnnotationSet.forTest("cas"), EXCLUSIVE_WRITE_ACCESS, cas);

        RecommenderTestHelper.addPredictionFeatures(cas, NamedEntity.class, "value");

        var gazeteer = asList(new GazeteerEntry("Smith . Peter", "ORG"));

        sut.pretrain(gazeteer, context);

        sut.predict(new PredictionContext(context), cas);

        var predictions = getPredictions(cas, NamedEntity.class);

        assertThat(predictions).extracting(NamedEntity::getCoveredText).contains("Smith .\nPeter");
    }

    private CAS getTestCasNoLabelLabels() throws Exception
    {
        try {
            var ds = loader.load("germeval2014-de", CONTINUE);
            var cas = loadData(ds, ds.getDataFiles()[0]).get(0);
            var neType = CasUtil.getAnnotationType(cas, NamedEntity.class);
            var valFeature = neType.getFeatureByBaseName("value");
            select(cas.getJCas(), NamedEntity.class)
                    .forEach(ne -> ne.setFeatureValueFromString(valFeature, null));

            return cas;
        }
        catch (Exception e) {
            // Workaround for https://github.com/dkpro/dkpro-core/issues/1469
            assumeThat(e).isNotInstanceOf(FileNotFoundException.class);
            throw e;
        }
    }

    @Test
    public void thatPredictionWithPretrainigWorks() throws Exception
    {
        var sut = new StringMatchingRecommender(recommender, traits);
        var casList = loadDevelopmentData();

        var cas = casList.get(0);
        RecommenderTestHelper.addPredictionFeatures(cas, NamedEntity.class, "value");

        var gazeteer = asList( //
                new GazeteerEntry("Toyota", "ORG"), //
                new GazeteerEntry("Deutschland", "LOC"), //
                new GazeteerEntry("Deutschland", "GPE"));

        sut.pretrain(gazeteer, context);

        sut.train(context, emptyList());

        sut.predict(new PredictionContext(context), cas);

        var predictions = getPredictions(cas, NamedEntity.class);

        assertThat(predictions).as("Predictions have been written to CAS").isNotEmpty();

        assertThat(predictions).as("Score is positive")
                .allMatch(prediction -> getScore(prediction) > 0.0 && getScore(prediction) <= 1.0);

        assertThat(predictions).as("Some score is not perfect")
                .anyMatch(prediction -> getScore(prediction) > 0.0 && getScore(prediction) < 1.0);
    }

    @Test
    public void thatPredictionWithPretrainigWorks_CaseInsensitve() throws Exception
    {
        traits.setIgnoreCase(true);

        var sut = new StringMatchingRecommender(recommender, traits);
        var casList = loadDevelopmentData();

        var cas = casList.get(0);
        RecommenderTestHelper.addPredictionFeatures(cas, NamedEntity.class, "value");

        var gazeteer = asList( //
                new GazeteerEntry("Toyota", "ORG"), //
                new GazeteerEntry("deutschland", "LOC"), //
                new GazeteerEntry("DEUTSCHLAND", "GPE"));

        sut.pretrain(gazeteer, context);

        sut.train(context, emptyList());

        sut.predict(new PredictionContext(context), cas);

        var predictions = getPredictions(cas, NamedEntity.class);

        assertThat(predictions).as("Predictions have been written to CAS").isNotEmpty();

        assertThat(predictions).as("Score is positive")
                .allMatch(prediction -> getScore(prediction) > 0.0 && getScore(prediction) <= 1.0);

        assertThat(predictions).as("Some score is not perfect")
                .anyMatch(prediction -> getScore(prediction) > 0.0 && getScore(prediction) < 1.0);
    }

    @Test
    public void thatEvaluationWorks() throws Exception
    {
        var splitStrategy = new PercentageBasedSplitter(0.8, 10);
        var sut = new StringMatchingRecommender(recommender, traits);
        var casList = loadDevelopmentData();

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
    public void thatEvaluationWorksWithoutLabels() throws Exception
    {
        var splitStrategy = new PercentageBasedSplitter(0.8, 10);
        var sut = new StringMatchingRecommender(recommender, traits);
        var casList = loadDevelopmentData();

        for (var cas : casList) {
            cas.select(NamedEntity.class).forEach(ne -> ne.setValue(null));
        }

        var result = sut.evaluate(casList, splitStrategy);

        var fscore = result.computeF1Score();
        var accuracy = result.computeAccuracyScore();
        var precision = result.computePrecisionScore();
        var recall = result.computeRecallScore();

        LOG.info("F1-Score:  {}", fscore);
        LOG.info("Accuracy:  {}", accuracy);
        LOG.info("Precision: {}", precision);
        LOG.info("Recall:    {}", recall);

        assertThat(fscore).isBetween(0.0, 1.0);
        assertThat(precision).isBetween(0.0, 1.0);
        assertThat(recall).isBetween(0.0, 1.0);
        assertThat(accuracy).isBetween(0.0, 1.0);
    }

    @Test
    public void thatEvaluationProducesSpecificResults() throws Exception
    {
        var text = "Hans Peter, Peter und Hans. Blabla Peter. Und so weiter Darmstadt, Darmstadt.";
        var vals = new String[] { "PER", "PER", "PER", "PER", "LOC", "ORG" };
        var indices = new int[][] { { 0, 9 }, { 12, 16 }, { 22, 25 }, { 35, 39 }, { 56, 64 },
                { 67, 75 } };
        var sentIndices = new int[][] { { 0, 26 }, { 27, 40 }, { 41, 65 }, { 66, 76 } };
        var tokenIndices = new int[][] { { 0, 3 }, { 5, 9 }, { 10, 10 }, { 12, 16 }, { 18, 20 },
                { 22, 25 }, { 26, 26 }, { 28, 33 }, { 35, 39 }, { 40, 40 }, { 42, 44 }, { 46, 47 },
                { 49, 54 }, { 56, 64 }, { 65, 65 }, { 67, 75 }, { 76, 76 } };

        var testCas = getTestNECas(text, vals, indices, sentIndices, tokenIndices);

        int expectedTestSize = 2;
        int expectedTrainSize = 2;

        var result = new StringMatchingRecommender(buildRecommender(), null).evaluate(testCas,
                new PercentageBasedSplitter(0.5, 500));

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
        var jcas = JCasFactory.createText(aText, "de");

        for (var aSentIndex : aSentIndices) {
            var newSent = new Sentence(jcas, aSentIndex[0], aSentIndex[1]);
            newSent.addToIndexes();
        }

        for (var aTokenIndex : aTokenIndices) {
            var newToken = new Token(jcas, aTokenIndex[0], aTokenIndex[1]);
            newToken.addToIndexes();
        }

        for (int i = 0; i < aVals.length; i++) {
            var newNE = new NamedEntity(jcas, aNEIndices[i][0], aNEIndices[i][1]);
            newNE.setValue(aVals[i]);
            newNE.addToIndexes();
        }

        return asList(jcas.getCas());
    }

    @Test
    public void thatEvaluationSkippingWorks() throws Exception
    {
        var splitStrategy = new PercentageBasedSplitter(0.8, 10);
        var sut = new StringMatchingRecommender(recommender, traits);

        var score = sut.evaluate(emptyList(), splitStrategy).computeF1Score();

        System.out.printf("Score: %f%n", score);

        assertThat(score).isEqualTo(0.0);
    }

    @Test
    public void thatIncrementalNerEvaluationWorks() throws Exception
    {
        var splitStrategy = new IncrementalSplitter(0.8, 5000, 10);
        var sut = new StringMatchingRecommender(recommender, traits);
        var casList = loadAllData();

        var i = 0;
        while (splitStrategy.hasNext() && i < 3) {
            splitStrategy.next();

            var score = sut.evaluate(casList, splitStrategy).computeF1Score();

            System.out.printf("Score: %f%n", score);

            assertThat(score).isStrictlyBetween(0.0, 1.0);

            i++;
        }
    }

    private List<CAS> loadAllData() throws IOException, UIMAException
    {
        try {
            var ds = loader.load("germeval2014-de", CONTINUE);
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
            var ds = loader.load("germeval2014-de", CONTINUE);
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
        var reader = createReader( //
                Conll2002Reader.class, //
                Conll2002Reader.PARAM_PATTERNS, files, //
                Conll2002Reader.PARAM_LANGUAGE, ds.getLanguage(), //
                Conll2002Reader.PARAM_COLUMN_SEPARATOR, ColumnSeparators.TAB.getName(),
                Conll2002Reader.PARAM_HAS_TOKEN_NUMBER, true, //
                Conll2002Reader.PARAM_HAS_HEADER, true, //
                Conll2002Reader.PARAM_HAS_EMBEDDED_NAMED_ENTITY, true);

        var casList = new ArrayList<CAS>();
        var n = 1;
        while (reader.hasNext()) {
            var cas = JCasFactory.createJCas();
            reader.getNext(cas.getCas());
            casList.add(cas.getCas());
            casStorageSession.add(AnnotationSet.forTest("testDataCas" + n), EXCLUSIVE_WRITE_ACCESS,
                    cas.getCas());
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

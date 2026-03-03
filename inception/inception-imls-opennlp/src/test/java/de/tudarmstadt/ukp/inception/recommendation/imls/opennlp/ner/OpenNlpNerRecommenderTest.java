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
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_IS_PREDICTION;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_SCORE_EXPLANATION_SUFFIX;
import static de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.ner.OpenNlpNerRecommender.whenSuggestionsOverlapKeepLongest;
import static de.tudarmstadt.ukp.inception.support.uima.AnnotationBuilder.buildAnnotation;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.dkpro.core.api.datasets.DatasetValidationPolicy.CONTINUE;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.dkpro.core.api.datasets.Dataset;
import org.dkpro.core.api.datasets.DatasetFactory;
import org.dkpro.core.io.conll.Conll2002Reader;
import org.dkpro.core.io.conll.Conll2002Reader.ColumnSeparators;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.IncrementalSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.PercentageBasedSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.PredictionContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.support.test.recommendation.DkproTestHelper;
import de.tudarmstadt.ukp.inception.support.test.recommendation.RecommenderTestHelper;
import de.tudarmstadt.ukp.inception.support.uima.SegmentationUtils;

public class OpenNlpNerRecommenderTest
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
        var sut = new OpenNlpNerRecommender(recommender, traits);
        var casList = loadDevelopmentData();

        sut.train(context, casList);

        assertThat(context.get(OpenNlpNerRecommender.KEY_MODEL)) //
                .as("Model has been set") //
                .isPresent();
    }

    @Test
    public void thatTrainingWorksCrossSentenceWithSimpleExample() throws Exception
    {
        var cas = JCasFactory.createJCas();
        cas.setDocumentText("""
                I like noodles.
                I guess St. John is good.
                Does St. John like noodles?
                """);

        SegmentationUtils.segment(cas.getCas());

        assertThat(cas.select(Token.class).asList()) //
                .map(Token::getCoveredText) //
                .containsAll(asList("St", ".", "John")) //
                .doesNotContain("St.", "St. John");

        buildAnnotation(cas, NamedEntity.class).onAll("St. John").buildAllAndAddToIndexes();
        var casList = asList(cas.getCas());

        recommender.getLayer().setCrossSentence(true);
        var sut = new OpenNlpNerRecommender(recommender, traits);

        sut.train(context, casList);

        assertThat(context.get(OpenNlpNerRecommender.KEY_MODEL)) //
                .as("Model has been set") //
                .isPresent();
    }

    @Test
    public void thatTrainingWorksCrossSentence() throws Exception
    {
        recommender.getLayer().setCrossSentence(true);
        var sut = new OpenNlpNerRecommender(recommender, traits);

        var casList = loadDevelopmentData();
        sut.train(context, casList);

        assertThat(context.get(OpenNlpNerRecommender.KEY_MODEL)) //
                .as("Model has been set") //
                .isPresent();
    }

    @Test
    public void thatPredictionWorks() throws Exception
    {
        var sut = new OpenNlpNerRecommender(recommender, traits);
        var casList = loadDevelopmentData();

        var cas = casList.get(0);
        try (var session = CasStorageSession.open()) {
            session.add(AnnotationSet.forTest("testCas"), EXCLUSIVE_WRITE_ACCESS, cas);
            RecommenderTestHelper.addPredictionFeatures(cas, NamedEntity.class, "value");
        }

        sut.train(context, asList(cas));

        sut.predict(new PredictionContext(context), cas);

        var isPredictionFeature = cas.getCasType(NamedEntity.class)
                .getFeatureByBaseName(FEATURE_NAME_IS_PREDICTION);
        var predictions = cas.select(NamedEntity.class)
                .filter(ne -> ne.getBooleanValue(isPredictionFeature)).toList();

        LOG.info("Found {} named entities (cross sentence: {})", predictions.size(),
                recommender.getLayer().isCrossSentence());
        LOG.debug("{}", predictions.stream().map(p -> String.format("[%d-%d] %s %s\n", p.getBegin(),
                p.getEnd(), p.getCoveredText(), p.getValue())).toList());

        assertThat(predictions).as("Predictions have been written to CAS").isNotEmpty();
    }

    @Test
    public void thatPredictionWorksCrossSentence() throws Exception
    {
        recommender.getLayer().setCrossSentence(true);
        var sut = new OpenNlpNerRecommender(recommender, traits);
        var casList = loadDevelopmentData();

        var cas = casList.get(0);
        try (var session = CasStorageSession.open()) {
            session.add(AnnotationSet.forTest("testCas"), EXCLUSIVE_WRITE_ACCESS, cas);
            RecommenderTestHelper.addPredictionFeatures(cas, NamedEntity.class,
                    NamedEntity._FeatName_value);
        }

        sut.train(context, asList(cas));

        sut.predict(new PredictionContext(context), cas);

        var isPredictionFeature = cas.getCasType(NamedEntity.class)
                .getFeatureByBaseName(FEATURE_NAME_IS_PREDICTION);
        var predictions = cas.select(NamedEntity.class)
                .filter(ne -> ne.getBooleanValue(isPredictionFeature)).toList();

        LOG.info("Found {} named entities (cross sentence: {})", predictions.size(),
                recommender.getLayer().isCrossSentence());
        LOG.debug("{}", predictions.stream().map(p -> String.format("[%d-%d] %s %s\n", p.getBegin(),
                p.getEnd(), p.getCoveredText(), p.getValue())).toList());

        assertThat(predictions).as("Predictions have been written to CAS").isNotEmpty();
    }

    @Test
    public void thatEvaluationWorks() throws Exception
    {
        var splitStrategy = new PercentageBasedSplitter(0.8, 10);
        var sut = new OpenNlpNerRecommender(recommender, traits);
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

        assertThat(fscore).isBetween(0.0, 1.0);
        assertThat(precision).isBetween(0.0, 1.0);
        assertThat(recall).isBetween(0.0, 1.0);
        assertThat(accuracy).isBetween(0.0, 1.0);
    }

    @Test
    public void thatEvaluationWorksNoLabels() throws Exception
    {
        var splitStrategy = new PercentageBasedSplitter(0.8, 10);
        var sut = new OpenNlpNerRecommender(recommender, traits);
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
    public void thatIncrementalNerEvaluationWorks() throws Exception
    {
        var splitStrategy = new IncrementalSplitter(0.8, 250, 10);
        var sut = new OpenNlpNerRecommender(recommender, traits);
        var casList = loadAllData();

        var i = 0;
        while (splitStrategy.hasNext() && i < 3) {
            splitStrategy.next();

            var score = sut.evaluate(casList, splitStrategy).computeF1Score();

            System.out.printf("Score: %f%n", score);

            assertThat(score).isBetween(0.0, 1.0);

            i++;
        }
    }

    @Test
    void thatWhenSuggestionsOverlapKeepLongestWorks() throws Exception
    {
        var cas = CasFactory.createCas();
        try (var session = CasStorageSession.open()) {
            session.add(AnnotationSet.forTest("testCas"), EXCLUSIVE_WRITE_ACCESS, cas);
            RecommenderTestHelper.addPredictionFeatures(cas, NamedEntity.class,
                    NamedEntity._FeatName_value);
        }

        cas.setDocumentText("I am St. John.");

        buildAnnotation(cas, NamedEntity.class).on("St.") //
                .withFeature(FEATURE_NAME_IS_PREDICTION, true) //
                .buildAndAddToIndexes();

        buildAnnotation(cas, NamedEntity.class).on("St. John") //
                .withFeature(FEATURE_NAME_IS_PREDICTION, true) //
                .buildAndAddToIndexes();

        var neType = cas.getCasType(NamedEntity.class);
        var isPredictionFeature = neType.getFeatureByBaseName(FEATURE_NAME_IS_PREDICTION);
        var scoreFeature = neType.getFeatureByBaseName(
                NamedEntity._FeatName_value + FEATURE_NAME_SCORE_EXPLANATION_SUFFIX);

        whenSuggestionsOverlapKeepLongest(cas, neType, isPredictionFeature, scoreFeature);

        assertThat(cas.select(NamedEntity.class).asList()) //
                .extracting(NamedEntity::getCoveredText) //
                .containsExactly("St. John");
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
                Conll2002Reader.PARAM_COLUMN_SEPARATOR, ColumnSeparators.TAB.getName(), //
                Conll2002Reader.PARAM_HAS_TOKEN_NUMBER, true, //
                Conll2002Reader.PARAM_HAS_HEADER, true, //
                Conll2002Reader.PARAM_HAS_EMBEDDED_NAMED_ENTITY, true);

        var casList = new ArrayList<CAS>();
        while (reader.hasNext()) {
            var cas = JCasFactory.createJCas();
            reader.getNext(cas.getCas());
            casList.add(cas.getCas());
        }
        return casList;
    }

    private static Recommender buildRecommender()
    {
        var layer = AnnotationLayer.builder().forJCasClass(NamedEntity.class).build();

        var feature = AnnotationFeature.builder().withLayer(layer)
                .withName(NamedEntity._FeatName_value).build();

        var recommender = Recommender.builder().withLayer(layer).withFeature(feature).build();

        return recommender;
    }
}

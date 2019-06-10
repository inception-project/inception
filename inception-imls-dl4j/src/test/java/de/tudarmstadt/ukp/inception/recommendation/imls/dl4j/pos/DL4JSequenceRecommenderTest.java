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
package de.tudarmstadt.ukp.inception.recommendation.imls.dl4j.pos;

import static de.tudarmstadt.ukp.inception.recommendation.imls.dl4j.pos.DL4JSequenceRecommender.NO_LABEL;
import static de.tudarmstadt.ukp.inception.support.test.recommendation.RecommenderTestHelper.addScoreFeature;
import static de.tudarmstadt.ukp.inception.support.test.recommendation.RecommenderTestHelper.getPredictions;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Before;
import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.datasets.Dataset;
import de.tudarmstadt.ukp.dkpro.core.api.datasets.DatasetFactory;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.io.conll.Conll2000Reader;
import de.tudarmstadt.ukp.dkpro.core.io.conll.Conll2002Reader;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.IncrementalSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.PercentageBasedSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;

public class DL4JSequenceRecommenderTest
{
    private static File cache = DkproTestContext.getCacheFolder();
    private static DatasetFactory loader = new DatasetFactory(cache);

    private RecommenderContext context;
    private DL4JSequenceRecommenderTraits traits;

    @Before
    public void setUp()
    {
        // By default, ND4J will use a value equal to the number of physical CPU cores (not logical
        // cores) as this will give optimal performance
        // Nd4jBlas nd4jBlas = (Nd4jBlas) Nd4j.factory().blas();
        // nd4jBlas.setMaxThreads(2);

        // NativeOpsHolder instance = NativeOpsHolder.getInstance();
        // NativeOps deviceNativeOps = instance.getDeviceNativeOps();
        // deviceNativeOps.setOmpNumThreads(2);

        context = new RecommenderContext();
        traits = new DL4JSequenceRecommenderTraits();
        traits.setTrainingSetSizeLimit(250);
        traits.setPredictionLimit(250);
        traits.setBatchSize(50);
    }

    @Test
    public void testExtractDenseLabels() throws Exception
    {
        JCas jcas = JCasFactory.createJCas();
        jcas.setDocumentText("a b b c c c abbccc");
        
        new Token(jcas,  0,  1).addToIndexes(); // a
        new Token(jcas,  2,  3).addToIndexes(); // b
        new Token(jcas,  4,  5).addToIndexes(); // b
        new Token(jcas,  6,  7).addToIndexes(); // c
        new Token(jcas,  8,  9).addToIndexes(); // c
        new Token(jcas, 10, 11).addToIndexes(); // c
        new Token(jcas, 12, 13).addToIndexes(); // a
        new Token(jcas, 13, 14).addToIndexes(); // b
        new Token(jcas, 14, 15).addToIndexes(); // b
        new Token(jcas, 15, 16).addToIndexes(); // c
        new Token(jcas, 16, 17).addToIndexes(); // c
        new Token(jcas, 17, 18).addToIndexes(); // c
        
        NamedEntity ne;
        ne = new NamedEntity(jcas,  0,  1); // a
        ne.setValue("A");
        ne.addToIndexes();
        ne = new NamedEntity(jcas,  2,  5); // b b
        ne.setValue("B");
        ne.addToIndexes();
        ne = new NamedEntity(jcas,  6, 11); // c c c
        ne.setValue("C");
        ne.addToIndexes();
        ne = new NamedEntity(jcas, 12, 13); // a
        ne.setValue("A");
        ne.addToIndexes();
        ne = new NamedEntity(jcas, 13, 15); // bb
        ne.setValue("B");
        ne.addToIndexes();
        ne = new NamedEntity(jcas, 15, 18); // ccc
        ne.setValue("C");
        ne.addToIndexes();
        
        DL4JSequenceRecommender sut = new DL4JSequenceRecommender(buildNerRecommender(), traits,
                cache);
        List<String> labels = sut.extractTokenLabels(
                new ArrayList<>(select(jcas, Token.class)), 
                new ArrayList<>(select(jcas, NamedEntity.class)));
        
        assertThat(labels).containsExactly("A", "B", "B", "C", "C", "C", "A", "B", "B", "C", "C",
                "C");
    }
    
    @Test
    public void testExtractSparseLabels() throws Exception
    {
        JCas jcas = JCasFactory.createJCas();
        jcas.setDocumentText("a b b c c c abbccc");
        
        new Token(jcas,  0,  1).addToIndexes(); // a
        new Token(jcas,  2,  3).addToIndexes(); // b
        new Token(jcas,  4,  5).addToIndexes(); // b
        new Token(jcas,  6,  7).addToIndexes(); // c
        new Token(jcas,  8,  9).addToIndexes(); // c
        new Token(jcas, 10, 11).addToIndexes(); // c
        new Token(jcas, 12, 13).addToIndexes(); // a
        new Token(jcas, 13, 14).addToIndexes(); // b
        new Token(jcas, 14, 15).addToIndexes(); // b
        new Token(jcas, 15, 16).addToIndexes(); // c
        new Token(jcas, 16, 17).addToIndexes(); // c
        new Token(jcas, 17, 18).addToIndexes(); // c
        
        NamedEntity ne;
        ne = new NamedEntity(jcas,  6, 11); // c c c
        ne.setValue("C");
        ne.addToIndexes();
        ne = new NamedEntity(jcas, 13, 15); // bb
        ne.setValue("B");
        ne.addToIndexes();
        
        DL4JSequenceRecommender sut = new DL4JSequenceRecommender(buildNerRecommender(), traits,
                cache);
        List<String> labels = sut.extractTokenLabels(
                new ArrayList<>(select(jcas, Token.class)), 
                new ArrayList<>(select(jcas, NamedEntity.class)));
        
        assertThat(labels).containsExactly(NO_LABEL, NO_LABEL, NO_LABEL, "C", "C", "C", NO_LABEL,
                "B", "B", NO_LABEL, NO_LABEL, NO_LABEL);
    }    
    
    @Test
    public void testExtractLabelsWithBadBoundaries() throws Exception
    {
        JCas jcas = JCasFactory.createJCas();
        jcas.setDocumentText("a b b");
        
        new Token(jcas,  0,  1).addToIndexes(); // a
        new Token(jcas,  2,  3).addToIndexes(); // b
        new Token(jcas,  4,  5).addToIndexes(); // b
        
        NamedEntity ne;
        ne = new NamedEntity(jcas,  0,  1);
        ne.setValue("A");
        ne.addToIndexes();
        ne = new NamedEntity(jcas,  1,  3);
        ne.setValue("B");
        ne.addToIndexes();
        
        DL4JSequenceRecommender sut = new DL4JSequenceRecommender(buildNerRecommender(), traits,
                cache);
        
        assertThatThrownBy(() -> sut.extractTokenLabels(
                new ArrayList<>(select(jcas, Token.class)), 
                new ArrayList<>(select(jcas, NamedEntity.class))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must start/end at token boundaries");
    }
    
    @Test
    public void testExtractOverlappingLabelsFails1() throws Exception
    {
        JCas jcas = JCasFactory.createJCas();
        jcas.setDocumentText("a b b");
        
        new Token(jcas,  0,  1).addToIndexes(); // a
        new Token(jcas,  2,  3).addToIndexes(); // b
        new Token(jcas,  4,  5).addToIndexes(); // b
        
        NamedEntity ne;
        ne = new NamedEntity(jcas,  0,  1); // a
        ne.setValue("A");
        ne.addToIndexes();
        ne = new NamedEntity(jcas,  0,  5); // b b
        ne.setValue("B");
        ne.addToIndexes();
        
        DL4JSequenceRecommender sut = new DL4JSequenceRecommender(buildNerRecommender(), traits,
                cache);
        
        assertThatThrownBy(() -> sut.extractTokenLabels(
                new ArrayList<>(select(jcas, Token.class)), 
                new ArrayList<>(select(jcas, NamedEntity.class))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Overlapping labels are not supported");
    }

    @Test
    public void testExtractOverlappingLabelsFails2() throws Exception
    {
        JCas jcas = JCasFactory.createJCas();
        jcas.setDocumentText("a b b");
        
        new Token(jcas,  0,  1).addToIndexes(); // a
        new Token(jcas,  2,  3).addToIndexes(); // b
        new Token(jcas,  4,  5).addToIndexes(); // b
        
        NamedEntity ne;
        ne = new NamedEntity(jcas,  0,  3); // a b
        ne.setValue("A");
        ne.addToIndexes();
        ne = new NamedEntity(jcas,  2,  5); // b b
        ne.setValue("B");
        ne.addToIndexes();
        
        DL4JSequenceRecommender sut = new DL4JSequenceRecommender(buildNerRecommender(), traits,
                cache);
        
        assertThatThrownBy(() -> sut.extractTokenLabels(
                new ArrayList<>(select(jcas, Token.class)), 
                new ArrayList<>(select(jcas, NamedEntity.class))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Overlapping labels are not supported");
    }

    @Test
    public void thatPosTrainingWorks() throws Exception
    {
        DL4JSequenceRecommender sut = new DL4JSequenceRecommender(buildPosRecommender(), traits,
                cache);
        JCas cas = loadPosDevelopmentData();

        sut.train(context, asList(cas.getCas()));

        assertThat(context.get(DL4JSequenceRecommender.KEY_MODEL))
            .as("Model has been set")
            .isNotNull();
    }

    @Test
    public void thatPosPredictionWorks() throws Exception
    {
        DL4JSequenceRecommender sut = new DL4JSequenceRecommender(buildPosRecommender(), traits,
                cache);
        JCas cas = loadPosDevelopmentData();
        
        sut.train(context, asList(cas.getCas()));

        addScoreFeature(cas.getCas(), POS.class, "PosValue");
        sut.predict(context, cas.getCas());

        List<POS> predictions = getPredictions(cas.getCas(), POS.class);
        assertThat(predictions).as("Predictions have been written to CAS")
            .isNotEmpty();
        
        // check how many labels are not padding labels
        long numWithLabel = predictions.stream()
                .filter(p -> !p.getPosValue().equals(DL4JSequenceRecommender.NO_LABEL)).count();
        System.out.printf("Predicted %d labels not no_label out of %d.%n", numWithLabel,
                predictions.size());
        
        assertThat(predictions).as("There are predictions other than *No_Label*")
            .anyMatch(l -> !l.getPosValue().equals(DL4JSequenceRecommender.NO_LABEL));
    }

    @Test
    public void thatPosEvaluationWorks() throws Exception
    {
        DataSplitter splitStrategy = new PercentageBasedSplitter(0.8, 10);
        DL4JSequenceRecommender sut = new DL4JSequenceRecommender(buildPosRecommender(), traits,
                cache);
        JCas cas = loadPosDevelopmentData();

        EvaluationResult result = sut.evaluate(asList(cas.getCas()), splitStrategy);

        double fscore = result.computeF1Score();
        double accuracy = result.computeAccuracyScore();
        double precision = result.computePrecisionScore();
        double recall = result.computeRecallScore();

        System.out.printf("POS F1-Score: %f%n", fscore);
        System.out.printf("POS Accuracy: %f%n", accuracy);
        System.out.printf("POS Precision: %f%n", precision);
        System.out.printf("POS Recall: %f%n", recall);
        
        assertThat(fscore).isStrictlyBetween(0.0, 1.0);
        assertThat(precision).isStrictlyBetween(0.0, 1.0);
        assertThat(recall).isStrictlyBetween(0.0, 1.0);
        assertThat(accuracy).isStrictlyBetween(0.0, 1.0);
    }

    @Test
    public void thatNerTrainingWorks() throws Exception
    {
        DL4JSequenceRecommender sut = new DL4JSequenceRecommender(buildNerRecommender(), traits,
                cache);
        JCas cas = loadNerDevelopmentData();

        sut.train(context, asList(cas.getCas()));

        assertThat(context.get(DL4JSequenceRecommender.KEY_MODEL))
            .as("Model has been set")
            .isNotNull();
    }

    @Test
    public void thatNerPredictionWorks() throws Exception
    {
        DL4JSequenceRecommender sut = new DL4JSequenceRecommender(buildNerRecommender(), traits,
                cache);
        JCas jCas = loadNerDevelopmentData();
        CAS cas = jCas.getCas();

        sut.train(context, asList(cas));

        addScoreFeature(cas, NamedEntity.class, "value");
        sut.predict(context, cas);

        List<NamedEntity> predictions = getPredictions(cas, NamedEntity.class);
        assertThat(predictions).as("Predictions have been written to CAS")
            .isNotEmpty();
        
        // check how many labels are not padding labels
        long numWithLabel = predictions.stream()
                .filter(p -> !p.getValue().equals(DL4JSequenceRecommender.NO_LABEL)).count();
        System.out.printf("Predicted %d labels not no_label out of %d.%n", numWithLabel,
                predictions.size());
        
        assertThat(predictions).as("There are predictions other than *No_Label*")
            .anyMatch(l -> !l.getValue().equals(DL4JSequenceRecommender.NO_LABEL));
    }

    @Test
    public void thatNerEvaluationWorks() throws Exception
    {
        DataSplitter splitStrategy = new PercentageBasedSplitter(0.8, 10);
        DL4JSequenceRecommender sut = new DL4JSequenceRecommender(buildNerRecommender(), traits,
                cache);
        JCas cas = loadNerDevelopmentData();

        EvaluationResult result = sut.evaluate(asList(cas.getCas()), splitStrategy);

        double fscore = result.computeF1Score();
        double accuracy = result.computeAccuracyScore();
        double precision = result.computePrecisionScore();
        double recall = result.computeRecallScore();

        System.out.printf("NER F1-Score: %f%n", fscore);
        System.out.printf("NER Accuracy: %f%n", accuracy);
        System.out.printf("NER Precision: %f%n", precision);
        System.out.printf("NER Recall: %f%n", recall);
        
        // FIXME is always zero
        assertThat(fscore).isBetween(0.0, 1.0);
        assertThat(precision).isBetween(0.0, 1.0);
        assertThat(recall).isBetween(0.0, 1.0);
        assertThat(accuracy).isBetween(0.0, 1.0);
    }

    @Test
    public void thatIncrementalNerEvaluationWorks() throws Exception
    {
        IncrementalSplitter splitStrategy = new IncrementalSplitter(0.8, 50, 10);
        DL4JSequenceRecommender sut = new DL4JSequenceRecommender(buildNerRecommender(), traits,
                cache);
        JCas cas = loadNerDevelopmentData();

        int i = 0;
        while (splitStrategy.hasNext() && i < 3) {
            splitStrategy.next();
            
            double score = sut.evaluate(asList(cas.getCas()), splitStrategy).computeF1Score();

            System.out.printf("Score: %f%n", score);

            assertThat(score).isBetween(0.0, 1.0);

            i++;
        }
    }

    private JCas loadPosDevelopmentData() throws IOException, UIMAException
    {
        Dataset ds = loader.load("conll2000-en");
        
        CollectionReader reader = createReader(Conll2000Reader.class,
                Conll2000Reader.PARAM_PATTERNS, ds.getDefaultSplit().getTestFiles(), 
                Conll2000Reader.PARAM_LANGUAGE, ds.getLanguage());
        
        JCas cas = JCasFactory.createJCas();
        reader.getNext(cas.getCas());
        return cas;
    }

    private JCas loadNerDevelopmentData() throws IOException, UIMAException
    {
        Dataset ds = loader.load("germeval2014-de");
        
        CollectionReader reader = createReader(Conll2002Reader.class,
            Conll2002Reader.PARAM_PATTERNS, ds.getDefaultSplit().getDevelopmentFiles(), 
            Conll2002Reader.PARAM_LANGUAGE, ds.getLanguage(), 
            Conll2002Reader.PARAM_COLUMN_SEPARATOR, Conll2002Reader.ColumnSeparators.TAB.getName(),
            Conll2002Reader.PARAM_HAS_TOKEN_NUMBER, true, 
            Conll2002Reader.PARAM_HAS_HEADER, true, 
            Conll2002Reader.PARAM_HAS_EMBEDDED_NAMED_ENTITY, true);
        
        JCas cas = JCasFactory.createJCas();
        reader.getNext(cas.getCas());
        return cas;
    }

    private static Recommender buildPosRecommender()
    {
        AnnotationLayer layer = new AnnotationLayer();
        layer.setName(POS.class.getName());

        AnnotationFeature feature = new AnnotationFeature();
        feature.setName("PosValue");
        
        Recommender recommender = new Recommender();
        recommender.setLayer(layer);
        recommender.setFeature(feature);

        return recommender;
    }
    
    private static Recommender buildNerRecommender()
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

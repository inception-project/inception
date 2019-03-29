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
package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.ner;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
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
import org.junit.Before;
import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.datasets.Dataset;
import de.tudarmstadt.ukp.dkpro.core.api.datasets.DatasetFactory;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.io.conll.Conll2002Reader;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.IncrementalSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.PercentageBasedSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.api.type.PredictedSpan;

public class OpenNlpNerRecommenderTest
{
    private static File cache = DkproTestContext.getCacheFolder();
    private static DatasetFactory loader = new DatasetFactory(cache);

    private RecommenderContext context;
    private Recommender recommender;
    private OpenNlpNerRecommenderTraits traits;

    @Before
    public void setUp()
    {
        context = new RecommenderContext();
        recommender = buildRecommender();
        traits = new OpenNlpNerRecommenderTraits();
        traits.setNumThreads(2);
    }

    @Test
    public void thatTrainingWorks() throws Exception
    {
        OpenNlpNerRecommender sut = new OpenNlpNerRecommender(recommender, traits);
        List<CAS> casList = loadDevelopmentData();

        sut.train(context, casList);

        assertThat(context.get(OpenNlpNerRecommender.KEY_MODEL))
            .as("Model has been set")
            .isPresent();
    }

    @Test
    public void thatPredictionWorks() throws Exception
    {
        OpenNlpNerRecommender sut = new OpenNlpNerRecommender(recommender, traits);
        List<CAS> casList = loadDevelopmentData();
        
        CAS cas = casList.get(0);
        
        sut.train(context, asList(cas));

        sut.predict(context, cas);

        Collection<PredictedSpan> predictions = JCasUtil.select(cas.getJCas(), PredictedSpan.class);

        assertThat(predictions).as("Predictions have been written to CAS")
            .isNotEmpty();
    }

    @Test
    public void thatEvaluationWorks() throws Exception
    {
        DataSplitter splitStrategy = new PercentageBasedSplitter(0.8, 10);
        OpenNlpNerRecommender sut = new OpenNlpNerRecommender(recommender, traits);
        List<CAS> casList = loadDevelopmentData();

        double score = sut.evaluate(casList, splitStrategy).getDefaultScore();

        System.out.printf("Score: %f%n", score);
        
        assertThat(score).isStrictlyBetween(0.0, 1.0);
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
            
            double score = sut.evaluate(casList, splitStrategy).getDefaultScore();

            System.out.printf("Score: %f%n", score);

            assertThat(score).isBetween(0.0, 1.0);
            
            i++;
        }
    }

    private List<CAS> loadAllData() throws IOException, UIMAException
    {
        Dataset ds = loader.load("germeval2014-de");
        return loadData(ds, ds.getDataFiles());
    }

    private List<CAS> loadDevelopmentData() throws IOException, UIMAException
    {
        Dataset ds = loader.load("germeval2014-de");
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

        return recommender;
    }
}

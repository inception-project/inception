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
package de.tudarmstadt.ukp.inception.recommendation.imls.external;

import static de.tudarmstadt.ukp.inception.recommendation.imls.external.util.CasAssert.assertThat;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.datasets.Dataset;
import de.tudarmstadt.ukp.dkpro.core.api.datasets.DatasetFactory;
import de.tudarmstadt.ukp.dkpro.core.io.conll.Conll2002Reader;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.v2.RecommenderContext;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class ExternalRecommenderIntegrationTest
{
    private static String TYPE = "de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity";
    private static File cache = DkproTestContext.getCacheFolder();
    private static DatasetFactory loader = new DatasetFactory(cache);

    private Recommender recommender;
    private RecommenderContext context;
    private ExternalRecommender sut;
    private ExternalRecommenderTraits traits;
    private RemoteStringMatchingRecommender remoteRecommender;
    private MockWebServer server;

    @Before
    public void setUp() throws Exception
    {
        recommender = buildRecommender();
        context = new RecommenderContext();

        traits = new ExternalRecommenderTraits();
        sut = new ExternalRecommender(recommender, traits);

        remoteRecommender = new RemoteStringMatchingRecommender(recommender);

        server = new MockWebServer();
        server.setDispatcher(buildDispatcher());
        server.start();

        String url = server.url("/").toString();
        traits.setRemoteUrl(url);
    }

    @After
    public void tearDown() throws Exception
    {
        server.shutdown();
    }

    @Test
    public void thatTrainingWorks()
    {
        assertThatCode(() -> {
            sut.train(context, loadDevelopmentData());
        }).doesNotThrowAnyException();
    }

    @Test
    public void thatPredictingWorks() throws Exception
    {
        List<CAS> casses = loadDevelopmentData();
        sut.train(context, casses);

        // TODO: Remove named entity annotations from CAS for prediction and
        // then let external recommender predict NE instead of PredictedSpan
        CAS cas = casses.get(0);
        sut.predict(context, cas);

        assertThat(cas).as("Predictions are correct")
            .containsPrediction("Ecce homo", "OTH")
            .containsPrediction("The Lindsey School Lindsey School & Community Arts College", "ORG")
            .containsPrediction("Lido delle Nazioni", "LOC");
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
        layer.setName(TYPE);

        Recommender recommender = new Recommender();
        recommender.setLayer(layer);
        recommender.setFeature("value");

        return recommender;
    }

    private Dispatcher buildDispatcher()
    {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if (request.getPath().equals("/train")) {
                    remoteRecommender.train(request.getBody().readUtf8());
                    return new MockResponse().setResponseCode(204);
                } else if (request.getPath().equals("/predict")) {
                    String response = remoteRecommender.predict(request.getBody().readUtf8());
                    return new MockResponse().setResponseCode(200).setBody(response);
                } else {
                    System.err.println("Unknown URL: " + request.getPath());
                    return new MockResponse().setResponseCode(404);
                }
            }
        };
    }
}

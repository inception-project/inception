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
package de.tudarmstadt.ukp.inception.recommendation.imls.lapps;

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
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.datasets.DatasetFactory;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.io.conll.ConllUReader;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.imls.lapps.traits.LappsGridRecommenderTraits;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class LappsRecommenderIntegrationTest
{
    private static File cache = DkproTestContext.getCacheFolder();
    private static DatasetFactory loader = new DatasetFactory(cache);

    private MockWebServer server;

    private LappsGridRecommender sut;

    @Before
    public void setUp() throws IOException
    {
        server = new MockWebServer();
        server.setDispatcher(buildDispatcher());
        server.start();

        LappsGridRecommenderTraits traits = new LappsGridRecommenderTraits();

        String url = server.url("/").toString();
        traits.setUrl(url);

        sut = new LappsGridRecommender(buildRecommender(), null);
    }

    @After
    public void tearDown() throws Exception
    {
        server.shutdown();
    }

    @Test
    @Ignore
    public void thatPredictingPosWorks() throws Exception
    {
        RecommenderContext context = new RecommenderContext();
        CAS cas = loadData();

        sut.predict(context, cas);

        Collection<POS> predictions = JCasUtil.select(cas.getJCas(), POS.class);

        assertThat(predictions).as("There should be some predictions")
                .isNotEmpty();
    }


    private static Recommender buildRecommender()
    {
        AnnotationLayer layer = new AnnotationLayer();
        layer.setName(POS.class.getName());

        AnnotationFeature feature = new AnnotationFeature();
        feature.setName("PosValue");

        Recommender recommender = new Recommender();
        recommender.setLayer(layer);
        recommender.setFeature(feature);
        recommender.setMaxRecommendations(3);

        return recommender;
    }

    private Dispatcher buildDispatcher()
    {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                try {
                    String url = request.getPath();
                    String body = request.getBody().readUtf8();

                    if (request.getPath().equals("/pos/predict")) {
                        String response = "";
                        return new MockResponse().setResponseCode(200).setBody(response);
                    } else {
                        throw new RuntimeException("Invalid URL called: " + url);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private CAS loadData() throws IOException, UIMAException {
        ClassLoader cl = getClass().getClassLoader();
        File file = new File(cl.getResource("conllu-en-orig.conll").getFile());

        return loadData(file);
    }

    private static CAS loadData(File aFile) throws UIMAException, IOException
    {
        CollectionReader reader = createReader(ConllUReader.class,
                ConllUReader.PARAM_PATTERNS, aFile);

        List<CAS> casList = new ArrayList<>();
        while (reader.hasNext()) {
            JCas cas = JCasFactory.createJCas();
            reader.getNext(cas.getCas());
            casList.add(cas.getCas());
        }

        return casList.get(0);
    }
}

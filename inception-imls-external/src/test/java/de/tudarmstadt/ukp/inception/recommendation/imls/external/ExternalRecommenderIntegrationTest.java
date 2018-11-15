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

import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.fromJsonString;
import static de.tudarmstadt.ukp.inception.recommendation.imls.external.util.InceptionAssertions.assertThat;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.assertj.core.api.Assertions.assertThatCode;

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
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.api.dao.CasMetadataUtils;
import de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.datasets.Dataset;
import de.tudarmstadt.ukp.dkpro.core.api.datasets.DatasetFactory;
import de.tudarmstadt.ukp.dkpro.core.io.conll.Conll2002Reader;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class ExternalRecommenderIntegrationTest
{
    private static String TYPE = "de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity";
    private static File cache = DkproTestContext.getCacheFolder();
    private static DatasetFactory loader = new DatasetFactory(cache);

    private static final String USER_NAME = "test_user";
    private static final long PROJECT_ID = 42L;

    private Recommender recommender;
    private RecommenderContext context;
    private ExternalRecommender sut;
    private ExternalRecommenderTraits traits;
    private RemoteStringMatchingNerRecommender remoteRecommender;
    private MockWebServer server;
    private List<String> requestBodies;

    @Before
    public void setUp() throws Exception
    {
        recommender = buildRecommender();
        context = new RecommenderContext();

        traits = new ExternalRecommenderTraits();
        sut = new ExternalRecommender(recommender, traits);

        remoteRecommender = new RemoteStringMatchingNerRecommender(recommender);

        server = new MockWebServer();
        server.setDispatcher(buildDispatcher());
        server.start();

        requestBodies = new ArrayList<>();

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
        assertThatCode(() ->
            sut.train(context, loadDevelopmentData())
        ).doesNotThrowAnyException();
    }

    @Test
    public void thatPredictingWorks() throws Exception
    {
        List<CAS> casses = loadDevelopmentData();
        sut.train(context, casses);

        CAS cas = casses.get(0);
        sut.predict(context, cas);

        assertThat(cas).as("Predictions are correct")
            .containsNamedEntity("Ecce homo", "OTH")
            .containsNamedEntity("The Lindsey School Lindsey School & Community Arts College", "ORG")
            .containsNamedEntity("Lido delle Nazioni", "LOC");
    }

    @Test
    public void thatTrainingSendsCorrectRequest() throws Exception
    {
        List<CAS> casses = loadDevelopmentData();
        sut.train(context, casses);

        TrainingRequest request = fromJsonString(TrainingRequest.class, requestBodies.get(0));

        assertThat(request).hasNoNullFieldsOrProperties()
            .hasFieldOrPropertyWithValue("projectId", PROJECT_ID)
            .hasFieldOrPropertyWithValue("layer", recommender.getLayer().getName())
            .hasFieldOrPropertyWithValue("feature", recommender.getFeature());

        for (int i = 0; i < request.getDocuments().size(); i++) {
            Document doc = request.getDocuments().get(i);
            assertThat(doc)
                .hasFieldOrPropertyWithValue("documentId", (long) i)
                .hasFieldOrPropertyWithValue("userId", USER_NAME);
        }
    }

    @Test
    public void thatPredictingSendsCorrectRequest() throws Exception
    {
        List<CAS> casses = loadDevelopmentData();
        sut.train(context, casses);
        CAS cas = casses.get(0);
        sut.predict(context, cas);

        PredictionRequest request = fromJsonString(PredictionRequest.class, requestBodies.get(1));

        assertThat(request).hasNoNullFieldsOrProperties()
            .hasFieldOrPropertyWithValue("projectId", PROJECT_ID)
            .hasFieldOrPropertyWithValue("layer", recommender.getLayer().getName())
            .hasFieldOrPropertyWithValue("feature", recommender.getFeature());
        assertThat(request.getDocument())
            .hasFieldOrPropertyWithValue("userId", USER_NAME)
            .hasFieldOrPropertyWithValue("documentId", 0L);
    }

    @Test
    public void thatAnnotationsAreClearedBeforeSending() throws Exception
    {
        List<CAS> casses = loadDevelopmentData();
        sut.train(context, casses);

        // Add fake annotation to the CAS that should be cleared by
        // the external recommender when predicting
        CAS cas = casses.get(0);
        createNamedEntity(cas, "FAKE");
        sut.predict(context, cas);

        assertThat(cas).as("Predictions are cleared")
            .extractNamedEntities()
            .noneMatch(fs -> {
                Type neType = getType(cas, "de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity");
                Feature valueFeature = neType.getFeatureByBaseName("value");
                return "FAKE".equals(fs.getStringValue(valueFeature));
            });
    }

    private List<CAS> loadDevelopmentData() throws Exception
    {
        Dataset ds = loader.load("germeval2014-de");
        List<CAS> data = loadData(ds, ds.getDefaultSplit().getDevelopmentFiles());

        for (int i = 0; i < data.size(); i++) {
            CAS cas = data.get(i);
            addCasMetadata(cas.getJCas(), i);
        }
        return data;
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
            // Add the CasMetadata type to the CAS
            List<TypeSystemDescription> typeSystems = new ArrayList<>();
            typeSystems.add(createTypeSystemDescription());
            typeSystems.add(CasMetadataUtils.getInternalTypeSystem());
            JCas cas = JCasFactory.createJCas(mergeTypeSystems(typeSystems));
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
                try {
                    String body = request.getBody().readUtf8();
                    requestBodies.add(body);

                    if (request.getPath().equals("/train")) {
                        remoteRecommender.train(body);
                        return new MockResponse().setResponseCode(204);
                    } else if (request.getPath().equals("/predict")) {
                        String response = remoteRecommender.predict(body);
                        return new MockResponse().setResponseCode(200).setBody(response);
                    }
                } catch (RecommendationException e) {
                    throw new RuntimeException(e);
                }

                System.err.println("Unknown URL: " + request.getPath());
                return new MockResponse().setResponseCode(404);
            }
        };
    }

    private void createNamedEntity(CAS aCas, String aValue)
    {
        Type neType = getType(aCas, "de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity");
        Feature valueFeature = neType.getFeatureByBaseName("value");
        AnnotationFS ne = aCas.createAnnotation(neType, 0, 42);
        ne.setStringValue(valueFeature, aValue);
        aCas.addFsToIndexes(ne);
    }

    private void addCasMetadata(JCas aJCas, long aDocumentId)
    {
        CASMetadata cmd = new CASMetadata(aJCas);
        cmd.setUsername(USER_NAME);
        cmd.setProjectId(PROJECT_ID);
        cmd.setSourceDocumentId(aDocumentId);
        aJCas.addFsToIndexes(cmd);
    }
}

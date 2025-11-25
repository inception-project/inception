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
package de.tudarmstadt.ukp.inception.recommendation.imls.external.v1;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.EXCLUSIVE_WRITE_ACCESS;
import static de.tudarmstadt.ukp.inception.recommendation.imls.external.util.InceptionAssertions.assertThat;
import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.fromJsonString;
import static de.tudarmstadt.ukp.inception.support.test.recommendation.RecommenderTestHelper.getPredictions;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.dkpro.core.api.datasets.DatasetValidationPolicy.CONTINUE;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.dkpro.core.api.datasets.Dataset;
import org.dkpro.core.api.datasets.DatasetFactory;
import org.dkpro.core.io.conll.Conll2002Reader;
import org.dkpro.core.io.conll.Conll2002Reader.ColumnSeparators;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata;
import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.annotation.storage.CasMetadataUtils;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.PredictionContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.imls.external.v1.config.ExternalRecommenderPropertiesImpl;
import de.tudarmstadt.ukp.inception.recommendation.imls.external.v1.messages.PredictionRequest;
import de.tudarmstadt.ukp.inception.recommendation.imls.external.v1.messages.TrainingRequest;
import de.tudarmstadt.ukp.inception.support.test.recommendation.DkproTestHelper;
import de.tudarmstadt.ukp.inception.support.test.recommendation.RecommenderTestHelper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.QueueDispatcher;
import okhttp3.mockwebserver.RecordedRequest;

public class ExternalRecommenderIntegrationTest
{
    private static final String TYPE = "de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity";
    private static final File cache = DkproTestHelper.getCacheFolder();
    private static final DatasetFactory loader = new DatasetFactory(cache);

    private static final String USER_NAME = "test_user";
    private static final long PROJECT_ID = 42L;
    private static final boolean CROSS_SENTENCE = true;
    private static final AnchoringMode ANCHORING_MODE = AnchoringMode.TOKENS;

    private Recommender recommender;
    private RecommenderContext context;
    private ExternalRecommender sut;
    private ExternalRecommenderTraits traits;
    private MockRemoteStringMatchingNerRecommender remoteRecommender;
    private MockWebServer server;
    private List<String> requestBodies;
    private CasStorageSession casStorageSession;

    @BeforeEach
    public void setUp() throws Exception
    {
        casStorageSession = CasStorageSession.open();
        recommender = buildRecommender();
        context = new RecommenderContext();

        traits = new ExternalRecommenderTraits();
        sut = new ExternalRecommender(new ExternalRecommenderPropertiesImpl(), recommender, traits);

        remoteRecommender = new MockRemoteStringMatchingNerRecommender(recommender);

        server = new MockWebServer();
        server.setDispatcher(buildDispatcher());
        server.start();

        requestBodies = new ArrayList<>();

        String url = server.url("/").toString();
        traits.setRemoteUrl(url);
    }

    @AfterEach
    public void tearDown() throws Exception
    {
        casStorageSession.close();
        server.shutdown();
    }

    @Test
    public void thatTrainingWorks() throws Exception
    {
        var data = loadDevelopmentData();

        assertThatCode(() -> sut.train(context, data)).doesNotThrowAnyException();
    }

    @Test
    public void thatPredictingWorks() throws Exception
    {
        var casses = loadDevelopmentData();
        sut.train(context, casses);

        var cas = casses.get(0);
        RecommenderTestHelper.addPredictionFeatures(cas, NamedEntity.class, "value");
        sut.predict(new PredictionContext(context), cas);

        var predictions = getPredictions(cas, NamedEntity.class);

        assertThat(predictions).as("Predictions are not empty").isNotEmpty();

        assertThat(cas).as("Predictions are correct") //
                .containsNamedEntity("Ecce homo", "OTH") //
                .containsNamedEntity("The Lindsey School Lindsey School & Community Arts College",
                        "ORG") //
                .containsNamedEntity("Lido delle Nazioni", "LOC");
    }

    @Test
    public void thatTrainingSendsCorrectRequest() throws Exception
    {
        var casses = loadDevelopmentData();
        sut.train(context, casses);

        var request = fromJsonString(TrainingRequest.class, requestBodies.get(0));

        assertThat(request.getMetadata()) //
                .hasNoNullFieldsOrProperties() //
                .hasFieldOrPropertyWithValue("projectId", PROJECT_ID)
                .hasFieldOrPropertyWithValue("layer", recommender.getLayer().getName())
                .hasFieldOrPropertyWithValue("feature", recommender.getFeature().getName())
                .hasFieldOrPropertyWithValue("range.begin", 0)
                .hasFieldOrPropertyWithValue("range.end", 263034)
                .hasFieldOrPropertyWithValue("crossSentence", CROSS_SENTENCE)
                .hasFieldOrPropertyWithValue("anchoringMode", ANCHORING_MODE.getId());

        for (int i = 0; i < request.getDocuments().size(); i++) {
            var doc = request.getDocuments().get(i);
            assertThat(doc) //
                    .hasFieldOrPropertyWithValue("documentId", (long) i) //
                    .hasFieldOrPropertyWithValue("userId", USER_NAME);
        }
    }

    @Test
    public void thatPredictingSendsCorrectRequest() throws Exception
    {
        var casses = loadDevelopmentData();
        sut.train(context, casses);

        var cas = casses.get(0);
        RecommenderTestHelper.addPredictionFeatures(cas, NamedEntity.class, "value");
        sut.predict(new PredictionContext(context), cas);

        var request = fromJsonString(PredictionRequest.class, requestBodies.get(1));

        assertThat(request.getMetadata()) //
                .hasNoNullFieldsOrProperties() //
                .hasFieldOrPropertyWithValue("projectId", PROJECT_ID)
                .hasFieldOrPropertyWithValue("layer", recommender.getLayer().getName())
                .hasFieldOrPropertyWithValue("feature", recommender.getFeature().getName())
                .hasFieldOrPropertyWithValue("range.begin", 0)
                .hasFieldOrPropertyWithValue("range.end", 263034)
                .hasFieldOrPropertyWithValue("crossSentence", CROSS_SENTENCE)
                .hasFieldOrPropertyWithValue("anchoringMode", ANCHORING_MODE.getId());

        assertThat(request.getDocument()) //
                .hasFieldOrPropertyWithValue("userId", USER_NAME) //
                .hasFieldOrPropertyWithValue("documentId", 0L);
    }

    private List<CAS> loadDevelopmentData() throws Exception
    {
        try {
            var ds = loader.load("germeval2014-de", CONTINUE);
            var data = loadData(ds, ds.getDefaultSplit().getDevelopmentFiles());

            for (int i = 0; i < data.size(); i++) {
                var cas = data.get(i);
                addCasMetadata(cas.getJCas(), i);
                casStorageSession.add(AnnotationSet.forTest("testDataCas" + i),
                        EXCLUSIVE_WRITE_ACCESS, cas);
            }
            return data;
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
        var layer = new AnnotationLayer();
        layer.setName(TYPE);
        layer.setCrossSentence(CROSS_SENTENCE);
        layer.setAnchoringMode(ANCHORING_MODE);

        var feature = new AnnotationFeature();
        feature.setName("value");

        var recommender = new Recommender();
        recommender.setLayer(layer);
        recommender.setFeature(feature);
        recommender.setMaxRecommendations(3);

        return recommender;
    }

    private QueueDispatcher buildDispatcher()
    {
        return new QueueDispatcher()
        {
            @Override
            public MockResponse dispatch(RecordedRequest request)
            {
                try {
                    var body = request.getBody().readUtf8();
                    requestBodies.add(body);

                    if (Objects.equals(request.getPath(), "/train")) {
                        remoteRecommender.train(body);
                        return new MockResponse().setResponseCode(204);
                    }
                    else if (request.getPath().equals("/predict")) {
                        var response = remoteRecommender.predict(body);
                        return new MockResponse().setResponseCode(200).setBody(response);
                    }
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }

                System.err.println("Unknown URL: " + request.getPath());
                return new MockResponse().setResponseCode(404);
            }
        };
    }

    private void addCasMetadata(JCas aJCas, long aDocumentId)
    {
        var cmd = new CASMetadata(aJCas);
        cmd.setUsername(USER_NAME);
        cmd.setProjectId(PROJECT_ID);
        cmd.setSourceDocumentId(aDocumentId);
        aJCas.addFsToIndexes(cmd);
    }
}

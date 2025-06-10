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

import static de.tudarmstadt.ukp.inception.annotation.storage.CasMetadataUtils.getInternalTypeSystem;
import static de.tudarmstadt.ukp.inception.support.test.http.HttpTestUtils.assumeEndpointIsAvailable;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Arrays;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata;
import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.imls.external.v1.config.ExternalRecommenderPropertiesImpl;

public class ExternalRecommenderSslTest
{
    private static final String TYPE = "de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity";

    private static final String USER_NAME = "test_user";
    private static final long PROJECT_ID = 42L;
    private static final boolean CROSS_SENTENCE = true;
    private static final AnchoringMode ANCHORING_MODE = AnchoringMode.TOKENS;

    private Recommender recommender;
    private RecommenderContext context;
    private ExternalRecommender sut;
    private ExternalRecommenderTraits traits;
    private CasStorageSession casStorageSession;
    private List<CAS> data;

    @BeforeEach
    public void setUp() throws Exception
    {
        casStorageSession = CasStorageSession.open();
        recommender = buildRecommender();
        context = new RecommenderContext();

        traits = new ExternalRecommenderTraits();

        JCas jcas = JCasFactory.createJCas(
                mergeTypeSystems(asList(createTypeSystemDescription(), getInternalTypeSystem())));
        jcas.setDocumentText("No text");
        addCasMetadata(jcas, 1l);
        data = Arrays.asList(jcas.getCas());
    }

    @AfterEach
    public void tearDown() throws Exception
    {
        casStorageSession.close();
    }

    @Tag("slow")
    @Test
    void thatDisablingCertificateValidationWorks_expired()
    {
        assumeEndpointIsAvailable("https://expired.badssl.com/");

        traits.setRemoteUrl("https://expired.badssl.com/");

        traits.setVerifyCertificates(true);
        sut = new ExternalRecommender(new ExternalRecommenderPropertiesImpl(), recommender, traits);
        assertThatExceptionOfType(RecommendationException.class) //
                .isThrownBy(() -> sut.train(context, data)) //
                .withMessageContaining("PKIX path validation failed");

        traits.setVerifyCertificates(false);
        sut = new ExternalRecommender(new ExternalRecommenderPropertiesImpl(), recommender, traits);
        assertThatExceptionOfType(RecommendationException.class) //
                .isThrownBy(() -> sut.train(context, data)) //
                .withMessageContaining("404 Not Found");
    }

    @Tag("slow")
    @Test
    void thatDisablingCertificateValidationWorks_wrongHost()
    {
        assumeEndpointIsAvailable("https://wrong.host.badssl.com/");

        traits.setRemoteUrl("https://wrong.host.badssl.com/");

        traits.setVerifyCertificates(true);
        sut = new ExternalRecommender(new ExternalRecommenderPropertiesImpl(), recommender, traits);
        assertThatExceptionOfType(RecommendationException.class) //
                .isThrownBy(() -> sut.train(context, data)) //
                .withMessageContaining("No subject alternative DNS name matching");

        // Disabling certificate validation does not disable host checking for recommenders.
        // Instead the VM would need to be started with {@code
        // -Djdk.internal.httpclient.disableHostnameVerification}
        // System.setProperty("", "true");
        // // traits.setVerifyCertificates(false);
        // sut = new ExternalRecommender(new ExternalRecommenderPropertiesImpl(), recommender,
        // traits);
        // assertThatExceptionOfType(RecommendationException.class) //
        // .isThrownBy(() -> sut.train(context, data)) //
        // .withMessageContaining("404 Not Found");
    }

    @Tag("slow")
    @Test
    void thatDisablingCertificateValidationWorks_selfSigned()
    {
        assumeEndpointIsAvailable("https://self-signed.badssl.com/");

        traits.setRemoteUrl("https://self-signed.badssl.com/");

        traits.setVerifyCertificates(true);
        sut = new ExternalRecommender(new ExternalRecommenderPropertiesImpl(), recommender, traits);
        assertThatExceptionOfType(RecommendationException.class) //
                .isThrownBy(() -> sut.train(context, data)) //
                .withMessageContaining("PKIX path building failed");

        traits.setVerifyCertificates(false);
        sut = new ExternalRecommender(new ExternalRecommenderPropertiesImpl(), recommender, traits);
        assertThatExceptionOfType(RecommendationException.class) //
                .isThrownBy(() -> sut.train(context, data)) //
                .withMessageContaining("404 Not Found");
    }

    @Tag("slow")
    @Test
    void thatDisablingCertificateValidationWorks_untrusted()
    {
        assumeEndpointIsAvailable("https://untrusted-root.badssl.com/");

        traits.setRemoteUrl("https://untrusted-root.badssl.com/");

        traits.setVerifyCertificates(true);
        sut = new ExternalRecommender(new ExternalRecommenderPropertiesImpl(), recommender, traits);
        assertThatExceptionOfType(RecommendationException.class) //
                .isThrownBy(() -> sut.train(context, data)) //
                .withMessageContaining("PKIX path building failed");

        traits.setVerifyCertificates(false);
        sut = new ExternalRecommender(new ExternalRecommenderPropertiesImpl(), recommender, traits);
        assertThatExceptionOfType(RecommendationException.class) //
                .isThrownBy(() -> sut.train(context, data)) //
                .withMessageContaining("404 Not Found");
    }

    @Tag("slow")
    @Test
    @Disabled("Currently tends to fail with a 404 error")
    void thatDisablingCertificateValidationWorks_revoked()
    {
        assumeEndpointIsAvailable("https://revoked.badssl.com/");

        traits.setRemoteUrl("https://revoked.badssl.com/");

        traits.setVerifyCertificates(true);
        sut = new ExternalRecommender(new ExternalRecommenderPropertiesImpl(), recommender, traits);
        assertThatExceptionOfType(RecommendationException.class) //
                .isThrownBy(() -> sut.train(context, data)) //
                .withMessageContaining("PKIX path validation failed");

        traits.setVerifyCertificates(false);
        sut = new ExternalRecommender(new ExternalRecommenderPropertiesImpl(), recommender, traits);
        assertThatExceptionOfType(RecommendationException.class) //
                .isThrownBy(() -> sut.train(context, data)) //
                .withMessageContaining("404 Not Found");
    }

    @Tag("slow")
    @Test
    void thatCertificateValidationWorks()
    {
        assumeEndpointIsAvailable("https://tls-v1-2.badssl.com:1012/");

        traits.setRemoteUrl("https://tls-v1-2.badssl.com:1012/");

        traits.setVerifyCertificates(true);
        sut = new ExternalRecommender(new ExternalRecommenderPropertiesImpl(), recommender, traits);
        assertThatExceptionOfType(RecommendationException.class) //
                .isThrownBy(() -> sut.train(context, data)) //
                .withMessageContaining("404 Not Found");
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

    private void addCasMetadata(JCas aJCas, long aDocumentId)
    {
        var cmd = new CASMetadata(aJCas);
        cmd.setUsername(USER_NAME);
        cmd.setProjectId(PROJECT_ID);
        cmd.setSourceDocumentId(aDocumentId);
        aJCas.addFsToIndexes(cmd);
    }
}

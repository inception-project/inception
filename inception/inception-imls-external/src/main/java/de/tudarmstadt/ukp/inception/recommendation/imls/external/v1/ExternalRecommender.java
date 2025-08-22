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

import static de.tudarmstadt.ukp.inception.recommendation.api.recommender.TrainingCapability.TRAINING_REQUIRED;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.getRealCas;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static org.apache.commons.lang3.StringUtils.appendIfMissing;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.util.TypeSystemUtil;
import org.apache.uima.util.XMLSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.PredictionContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext.Key;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.TrainingCapability;
import de.tudarmstadt.ukp.inception.recommendation.imls.external.v1.config.ExternalRecommenderProperties;
import de.tudarmstadt.ukp.inception.recommendation.imls.external.v1.messages.PredictionRequest;
import de.tudarmstadt.ukp.inception.recommendation.imls.external.v1.messages.PredictionResponse;
import de.tudarmstadt.ukp.inception.recommendation.imls.external.v1.messages.TrainingRequest;
import de.tudarmstadt.ukp.inception.recommendation.imls.external.v1.model.Document;
import de.tudarmstadt.ukp.inception.recommendation.imls.external.v1.model.Metadata;
import de.tudarmstadt.ukp.inception.rendering.model.Range;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;
import de.tudarmstadt.ukp.inception.support.xml.sanitizer.IllegalXmlCharacterSanitizingContentHandler;

public class ExternalRecommender
    extends RecommendationEngine
{
    public static final Key<Boolean> KEY_TRAINING_COMPLETE = new Key<>("training_complete");

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_BAD_REQUEST = 400;

    private final ExternalRecommenderProperties properties;
    private final ExternalRecommenderTraits traits;

    private HttpClient _client;

    public ExternalRecommender(ExternalRecommenderProperties aProperties, Recommender aRecommender,
            ExternalRecommenderTraits aTraits)
    {
        super(aRecommender);

        properties = aProperties;
        traits = aTraits;
    }

    private HttpClient getClient() throws RecommendationException
    {
        try {
            if (_client == null) {
                var clientBuilder = HttpClient.newBuilder() //
                        .connectTimeout(properties.getConnectTimeout());
                if (!traits.isVerifyCertificates()) {
                    var sslContext = makeNonVerifyingSslContext();

                    clientBuilder.sslContext(sslContext);

                }
                _client = clientBuilder.build();
            }
            return _client;
        }
        catch (KeyManagementException | NoSuchAlgorithmException e) {
            throw new RecommendationException("Unable to initialize HTTP client", e);
        }
    }

    private SSLContext makeNonVerifyingSslContext()
        throws NoSuchAlgorithmException, KeyManagementException
    {
        var trustManager = new X509TrustManager()
        {
            @Override
            public X509Certificate[] getAcceptedIssuers()
            {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType)
            {
                // no check
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType)
            {
                // no check
            }
        };

        var sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[] { trustManager }, new SecureRandom());
        return sslContext;
    }

    @Override
    public boolean isReadyForPrediction(RecommenderContext aContext)
    {
        if (traits.getTrainingCapability() == TRAINING_REQUIRED) {
            return aContext.get(KEY_TRAINING_COMPLETE).orElse(false);
        }

        return true;
    }

    @Override
    public void train(RecommenderContext aContext, List<CAS> aCasses) throws RecommendationException
    {
        var client = getClient();

        var trainingRequest = new TrainingRequest();

        // We assume that the type system for all CAS are the same
        var representativeCas = aCasses.get(0);
        var typeSystem = serializeTypeSystem(representativeCas);
        trainingRequest.setTypeSystem(typeSystem);

        // Fill in metadata. We use the type system of the first CAS in the list
        // for all the other CAS. It could happen that training happens while
        // the type system changes, e.g. by adding a layer or feature during training.
        // Then the type system of the first CAS might not match the type system
        // of the other CAS. This should happen really rarely, therefore this potential
        // error is neglected.

        trainingRequest.setMetadata(
                buildMetadata(representativeCas, Range.rangeCoveringDocument(representativeCas)));

        var documents = new ArrayList<Document>();
        for (var cas : aCasses) {
            documents.add(buildDocument(cas));
        }
        trainingRequest.setDocuments(documents);

        var request = HttpRequest.newBuilder() //
                .uri(URI.create(appendIfMissing(traits.getRemoteUrl(), "/")).resolve("train")) //
                .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE) //
                .timeout(properties.getReadTimeout())
                .POST(BodyPublishers.ofString(toJson(trainingRequest), UTF_8)).build();

        var response = sendRequest(client, request);
        if (response.statusCode() == HTTP_TOO_MANY_REQUESTS) {
            LOG.info("External recommender is already training");
        }

        // If the response indicates that the request was not successful,
        // then it does not make sense to go on and try to decode the XMI
        else if (response.statusCode() >= HTTP_BAD_REQUEST) {
            var responseBody = getResponseBody(response);
            var msg = format("Request was not successful: [%d] - [%s]", response.statusCode(),
                    responseBody);
            throw new RecommendationException(msg);
        }

        aContext.put(KEY_TRAINING_COMPLETE, true);
    }

    @Override
    public Range predict(PredictionContext aContext, CAS aCas, int aBegin, int aEnd)
        throws RecommendationException
    {
        var client = getClient();

        var typeSystem = serializeTypeSystem(aCas);

        var predictionRequest = new PredictionRequest();
        predictionRequest.setTypeSystem(typeSystem);
        predictionRequest.setDocument(buildDocument(aCas));

        // Fill in metadata
        predictionRequest.setMetadata(buildMetadata(aCas, new Range(aBegin, aEnd)));

        var request = HttpRequest.newBuilder() //
                .uri(URI.create(appendIfMissing(traits.getRemoteUrl(), "/")).resolve("predict")) //
                .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE) //
                .timeout(properties.getReadTimeout()) //
                .POST(BodyPublishers.ofString(toJson(predictionRequest), UTF_8)) //
                .build();

        var response = sendRequest(client, request);
        // If the response indicates that the request was not successful,
        // then it does not make sense to go on and try to decode the XMI
        if (response.statusCode() >= HTTP_BAD_REQUEST) {
            var responseBody = getResponseBody(response);
            var msg = format("Request was not successful: [%d] - [%s]", response.statusCode(),
                    responseBody);
            throw new RecommendationException(msg);
        }

        var predictionResponse = deserializePredictionResponse(response);

        try (var is = IOUtils.toInputStream(predictionResponse.getDocument(), UTF_8)) {
            XmiCasDeserializer.deserialize(is, WebAnnoCasUtil.getRealCas(aCas), true);
        }
        catch (SAXException | IOException e) {
            throw new RecommendationException("Error while deserializing CAS!", e);
        }

        return Range.rangeCoveringDocument(aCas);
    }

    private String serializeTypeSystem(CAS aCas) throws RecommendationException
    {
        var layer = recommender.getLayer();
        var feature = recommender.getFeature();

        var tsd = TypeSystemUtil.typeSystem2TypeSystemDescription(aCas.getTypeSystem());

        var type = tsd.getType(layer.getName());
        type.setDescription(layer.getDescription());

        stream(type.getFeatures()) //
                .filter(f -> f.getName().equals(feature.getName())) //
                .forEach(f -> f.setDescription(feature.getDescription()));

        try (var out = new StringWriter()) {
            tsd.toXML(out);
            return out.toString();
        }
        catch (CASRuntimeException | SAXException | IOException e) {
            throw new RecommendationException("Could not serialize type system", e);
        }
    }

    private String serializeCas(CAS aCas) throws RecommendationException
    {
        try (var out = new StringWriter()) {
            // Passing "null" as the type system to the XmiCasSerializer means that we want
            // to serialize all types (i.e. no filtering for a specific target type system).
            var xmiCasSerializer = new XmiCasSerializer(null);
            var contentHandler = new XMLSerializer(out, true).getContentHandler();
            contentHandler = new IllegalXmlCharacterSanitizingContentHandler(contentHandler);
            xmiCasSerializer.serialize(getRealCas(aCas), contentHandler, null, null, null);
            return out.toString();
        }
        catch (CASRuntimeException | SAXException | IOException e) {
            throw new RecommendationException("Error while serializing CAS!", e);
        }
    }

    private Document buildDocument(CAS aCas) throws RecommendationException
    {
        var casMetadata = getCasMetadata(aCas);
        var xmi = serializeCas(aCas);
        var documentId = casMetadata.getSourceDocumentId();
        var userId = casMetadata.getUsername();

        return new Document(xmi, documentId, userId);
    }

    private CASMetadata getCasMetadata(CAS aCas) throws RecommendationException
    {
        try {
            return JCasUtil.selectSingle(aCas.getJCas(), CASMetadata.class);
        }
        catch (CASException | IllegalArgumentException e) {
            throw new RecommendationException("Error while reading CAS metadata!", e);
        }
    }

    private Metadata buildMetadata(CAS aCas, Range aRange) throws RecommendationException
    {
        var casMetadata = getCasMetadata(aCas);
        var layer = recommender.getLayer();
        return new Metadata(layer.getName(), recommender.getFeature().getName(),
                casMetadata.getProjectId(), layer.getAnchoringMode().getId(),
                layer.isCrossSentence(), aRange);
    }

    private PredictionResponse deserializePredictionResponse(HttpResponse<String> response)
        throws RecommendationException
    {
        try {
            return JSONUtil.fromJsonString(PredictionResponse.class, response.body());
        }
        catch (IOException e) {
            throw new RecommendationException("Error while deserializing prediction response!", e);
        }
    }

    private String toJson(Object aObject) throws RecommendationException
    {
        try {
            return JSONUtil.toJsonString(aObject);
        }
        catch (IOException e) {
            throw new RecommendationException("Error while serializing JSON!", e);
        }
    }

    private HttpResponse<String> sendRequest(HttpClient aClient, HttpRequest aRequest)
        throws RecommendationException
    {
        try {
            return aClient.send(aRequest, BodyHandlers.ofString(UTF_8));
        }
        catch (IOException | InterruptedException e) {
            throw new RecommendationException("Error while sending request: " + e.getMessage(), e);
        }
    }

    private String getResponseBody(HttpResponse<String> response)
    {
        if (response.body() == null) {
            return "";
        }

        return response.body();
    }

    @Override
    public int estimateSampleCount(List<CAS> aCasses)
    {
        return -1;
    }

    @Override
    public EvaluationResult evaluate(List<CAS> aCasses, DataSplitter aDataSplitter)
    {
        var result = new EvaluationResult();
        result.setEvaluationSkipped(true);
        result.setErrorMsg("ExternalRecommender does not support evaluation.");
        return result;
    }

    @Override
    public TrainingCapability getTrainingCapability()
    {
        return traits.getTrainingCapability();
    }
}

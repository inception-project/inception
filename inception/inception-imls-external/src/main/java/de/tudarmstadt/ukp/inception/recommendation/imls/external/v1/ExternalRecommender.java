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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getRealCas;
import static de.tudarmstadt.ukp.inception.recommendation.api.recommender.TrainingCapability.TRAINING_NOT_SUPPORTED;
import static de.tudarmstadt.ukp.inception.recommendation.api.recommender.TrainingCapability.TRAINING_REQUIRED;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.appendIfMissing;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
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

public class ExternalRecommender
    extends RecommendationEngine
{
    public static final Key<Boolean> KEY_TRAINING_COMPLETE = new Key<>("training_complete");

    private static final Logger LOG = LoggerFactory.getLogger(ExternalRecommender.class);

    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_BAD_REQUEST = 400;

    private final ExternalRecommenderProperties properties;
    private final ExternalRecommenderTraits traits;
    private final HttpClient client;

    public ExternalRecommender(ExternalRecommenderProperties aProperties, Recommender aRecommender,
            ExternalRecommenderTraits aTraits)
    {
        super(aRecommender);

        properties = aProperties;
        traits = aTraits;
        client = HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout()).build();
    }

    @Override
    public boolean isReadyForPrediction(RecommenderContext aContext)
    {
        if (traits.isTrainable()) {
            return aContext.get(KEY_TRAINING_COMPLETE).orElse(false);
        }
        else {
            return true;
        }
    }

    @Override
    public void train(RecommenderContext aContext, List<CAS> aCasses) throws RecommendationException
    {
        TrainingRequest trainingRequest = new TrainingRequest();
        List<Document> documents = new ArrayList<>();

        // We assume that the type system for all CAS are the same
        String typeSystem = serializeTypeSystem(aCasses.get(0));
        trainingRequest.setTypeSystem(typeSystem);

        // Fill in metadata. We use the type system of the first CAS in the list
        // for all the other CAS. It could happen that training happens while
        // the type system changes, e.g. by adding a layer or feature during training.
        // Then the type system of the first CAS might not match the type system
        // of the other CAS. This should happen really rarely, therefore this potential
        // error is neglected.

        trainingRequest.setMetadata(buildMetadata(aCasses.get(0)));

        for (CAS cas : aCasses) {
            documents.add(buildDocument(cas));
        }

        trainingRequest.setDocuments(documents);

        HttpRequest request = HttpRequest.newBuilder() //
                .uri(URI.create(appendIfMissing(traits.getRemoteUrl(), "/")).resolve("train")) //
                .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE) //
                .timeout(properties.getReadTimeout())
                .POST(BodyPublishers.ofString(toJson(trainingRequest), UTF_8)).build();

        HttpResponse<String> response = sendRequest(request);
        if (response.statusCode() == HTTP_TOO_MANY_REQUESTS) {
            LOG.info("External recommender is already training");
        }

        // If the response indicates that the request was not successful,
        // then it does not make sense to go on and try to decode the XMI
        else if (response.statusCode() >= HTTP_BAD_REQUEST) {
            String responseBody = getResponseBody(response);
            String msg = format("Request was not successful: [%d] - [%s]", response.statusCode(),
                    responseBody);
            throw new RecommendationException(msg);
        }

        aContext.put(KEY_TRAINING_COMPLETE, true);
    }

    @Override
    public Range predict(RecommenderContext aContext, CAS aCas, int aBegin, int aEnd)
        throws RecommendationException
    {
        String typeSystem = serializeTypeSystem(aCas);

        PredictionRequest predictionRequest = new PredictionRequest();
        predictionRequest.setTypeSystem(typeSystem);
        predictionRequest.setDocument(buildDocument(aCas));

        // Fill in metadata
        predictionRequest.setMetadata(buildMetadata(aCas));

        HttpRequest request = HttpRequest.newBuilder() //
                .uri(URI.create(appendIfMissing(traits.getRemoteUrl(), "/")).resolve("predict")) //
                .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE) //
                .timeout(properties.getReadTimeout()) //
                .POST(BodyPublishers.ofString(toJson(predictionRequest), UTF_8)).build();

        HttpResponse<String> response = sendRequest(request);
        // If the response indicates that the request was not successful,
        // then it does not make sense to go on and try to decode the XMI
        if (response.statusCode() >= HTTP_BAD_REQUEST) {
            String responseBody = getResponseBody(response);
            String msg = format("Request was not successful: [%d] - [%s]", response.statusCode(),
                    responseBody);
            throw new RecommendationException(msg);
        }

        PredictionResponse predictionResponse = deserializePredictionResponse(response);

        try (InputStream is = IOUtils.toInputStream(predictionResponse.getDocument(), UTF_8)) {
            XmiCasDeserializer.deserialize(is, WebAnnoCasUtil.getRealCas(aCas), true);
        }
        catch (SAXException | IOException e) {
            throw new RecommendationException("Error while deserializing CAS!", e);
        }

        return new Range(aCas);
    }

    private String serializeTypeSystem(CAS aCas) throws RecommendationException
    {
        try (StringWriter out = new StringWriter()) {
            TypeSystemUtil.typeSystem2TypeSystemDescription(aCas.getTypeSystem()).toXML(out);
            return out.toString();
        }
        catch (CASRuntimeException | SAXException | IOException e) {
            throw new RecommendationException("Could not serialize type system", e);
        }
    }

    private String serializeCas(CAS aCas) throws RecommendationException
    {
        try (StringWriter out = new StringWriter()) {
            // Passing "null" as the type system to the XmiCasSerializer means that we want
            // to serialize all types (i.e. no filtering for a specific target type system).
            XmiCasSerializer xmiCasSerializer = new XmiCasSerializer(null);
            XMLSerializer sax2xml = new XMLSerializer(out, true);
            xmiCasSerializer.serialize(getRealCas(aCas), sax2xml.getContentHandler(), null, null,
                    null);
            return out.toString();
        }
        catch (CASRuntimeException | SAXException | IOException e) {
            throw new RecommendationException("Error while serializing CAS!", e);
        }
    }

    private Document buildDocument(CAS aCas) throws RecommendationException
    {
        CASMetadata casMetadata = getCasMetadata(aCas);
        String xmi = serializeCas(aCas);
        long documentId = casMetadata.getSourceDocumentId();
        String userId = casMetadata.getUsername();

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

    private Metadata buildMetadata(CAS aCas) throws RecommendationException
    {
        CASMetadata casMetadata = getCasMetadata(aCas);
        AnnotationLayer layer = recommender.getLayer();
        return new Metadata(layer.getName(), recommender.getFeature().getName(),
                casMetadata.getProjectId(), layer.getAnchoringMode().getId(),
                layer.isCrossSentence());
    }

    private PredictionResponse deserializePredictionResponse(HttpResponse<String> response)
        throws RecommendationException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(response.body(), PredictionResponse.class);
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

    private HttpResponse<String> sendRequest(HttpRequest aRequest) throws RecommendationException
    {
        try {
            return client.send(aRequest, BodyHandlers.ofString(UTF_8));
        }
        catch (IOException | InterruptedException e) {
            throw new RecommendationException("Error while sending request: " + e.getMessage(), e);
        }
    }

    private String getResponseBody(HttpResponse<String> response) throws RecommendationException
    {
        if (response.body() != null) {
            return response.body();
        }
        else {
            return "";
        }
    }

    @Override
    public int estimateSampleCount(List<CAS> aCasses)
    {
        return -1;
    }

    @Override
    public EvaluationResult evaluate(List<CAS> aCasses, DataSplitter aDataSplitter)
    {
        EvaluationResult result = new EvaluationResult();
        result.setEvaluationSkipped(true);
        result.setErrorMsg("ExternalRecommender does not support evaluation.");
        return result;
    }

    @Override
    public TrainingCapability getTrainingCapability()
    {
        if (traits.isTrainable()) {
            //
            // return TRAINING_SUPPORTED;
            // We need to get at least one training CAS because we need to extract the type system
            return TRAINING_REQUIRED;
        }
        else {
            return TRAINING_NOT_SUPPORTED;
        }
    }
}

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

import static java.lang.String.format;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.util.TypeSystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ExternalRecommender
    implements RecommendationEngine
{
    private static final Logger LOG = LoggerFactory.getLogger(ExternalRecommender.class);
    private static final MediaType JSON = MediaType.parse("application/json");

    private final Recommender recommender;
    private final ExternalRecommenderTraits traits;
    private final OkHttpClient client;

    public ExternalRecommender(Recommender aRecommender, ExternalRecommenderTraits aTraits)
    {
        recommender = aRecommender;
        traits = aTraits;
        client = new OkHttpClient();
    }

    @Override
    public void train(RecommenderContext aContext, List<CAS> aCasses)
        throws RecommendationException
    {
        TrainingRequest trainingRequest = new TrainingRequest();
        List<String> documents = new ArrayList<>();
        trainingRequest.setDocuments(documents);

        for (CAS cas : aCasses) {
            String typeSystem = serializeTypeSystem(cas);
            String xmi = serializeCas(cas);

            trainingRequest.setTypeSystem(typeSystem);
            trainingRequest.setLayer(recommender.getLayer().getName());
            trainingRequest.setFeature(recommender.getFeature());
            documents.add(xmi);
        }

        HttpUrl url = HttpUrl.parse(traits.getRemoteUrl()).resolve("/train");
        RequestBody body = RequestBody.create(JSON, toJson(trainingRequest));
        Request request = new Request.Builder().url(url).post(body).build();

        Response response = sendRequest(request);

        // If the response indicates that the request was not successful,
        // then it does not make sense to go on and try to decode the XMI
        if (!response.isSuccessful()) {
            int code = response.code();
            String responseBody = getResponseBody(response);
            String msg = format("Request was not successful: [%d] - [%s]", code, responseBody);
            throw new RecommendationException(msg);
        }
    }

    @Override
    public void predict(RecommenderContext aContext, CAS aCas) throws RecommendationException
    {
        // External recommender can predict arbitrary annotations, not only PredictedSpans.
        // In order to support the case where the prediction annotation type is the predicted
        // annotation type (e.g. recommend named entities, recommender creates named entities),
        // the predicted annotation has to be removed from the CAS first in order to be able
        // to differentiate between the two
        removePredictedAnnotations(aCas);

        String typeSystem = serializeTypeSystem(aCas);
        String xmi = serializeCas(aCas);

        PredictionRequest predictionRequest = new PredictionRequest();
        predictionRequest.setDocument(xmi);
        predictionRequest.setTypeSystem(typeSystem);
        predictionRequest.setLayer(recommender.getLayer().getName());
        predictionRequest.setFeature(recommender.getFeature());

        HttpUrl url = HttpUrl.parse(traits.getRemoteUrl()).resolve("/predict");
        RequestBody body = RequestBody.create(JSON, toJson(predictionRequest));
        Request request = new Request.Builder().url(url).post(body).build();

        Response response = sendRequest(request);

        // If the response indicates that the request was not successful,
        // then it does not make sense to go on and try to decode the XMI
        if (!response.isSuccessful()) {
            int code = response.code();
            String responseBody = getResponseBody(response);
            String msg = format("Request was not successful: [%d] - [%s]", code, responseBody);
            throw new RecommendationException(msg);
        }

        PredictionResponse predictionResponse = deserializePredictionResponse(response);

        try (InputStream is = IOUtils.toInputStream(predictionResponse.getDocument(), "utf-8");
             InputStream bis = Base64.getDecoder().wrap(is)) {
            XmiCasDeserializer.deserialize(bis, aCas, true);
        }
        catch (SAXException | IOException e) {
            LOG.error("Error while deserializing CAS!", e);
            throw new RecommendationException("Error while deserializing CAS!", e);
        }
    }

    private void removePredictedAnnotations(CAS aCas)
    {
        Type type = CasUtil.getType(aCas, getPredictedType());
        for (AnnotationFS annotationFS : CasUtil.select(aCas, type)) {
            aCas.removeFsFromIndexes(annotationFS);
        }
    }

    private String serializeTypeSystem(CAS aCas) throws RecommendationException
    {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            TypeSystemUtil.typeSystem2TypeSystemDescription(aCas.getTypeSystem()).toXML(out);
            return new String(Base64.getEncoder().encode(out.toByteArray()), "utf-8");
        }
        catch (CASRuntimeException | SAXException | IOException e) {
            LOG.error("Error while serializing type system!", e);
            throw new RecommendationException("Coud not serialize type system", e);
        }
    }

    private String serializeCas(CAS aCas) throws RecommendationException
    {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XmiCasSerializer.serialize(aCas, null, out, true, null);
            return new String(Base64.getEncoder().encode(out.toByteArray()), "utf-8");
        }
        catch (CASRuntimeException | SAXException | IOException e) {
            LOG.error("Error while serializing CAS!", e);
            throw new RecommendationException("Error while serializing CAS!", e);
        }
    }

    private PredictionResponse deserializePredictionResponse(Response response)
        throws RecommendationException {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(response.body().byteStream(), PredictionResponse.class);
        } catch (IOException e) {
            LOG.error("Error while deserializing prediction response!", e);
            throw new RecommendationException("Error while deserializing prediction response!", e);
        }
    }

    private String toJson(Object aObject) throws RecommendationException
    {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(aObject);
        }
        catch (JsonProcessingException e) {
            LOG.error("Error while serializing JSON!", e);
            throw new RecommendationException("Error while serializing JSON!", e);
        }
    }

    private Response sendRequest(Request aRequest) throws RecommendationException
    {
        try {
            return client.newCall(aRequest).execute();
        }
        catch (IOException e) {
            LOG.error("Error while sending request!", e);
            throw new RecommendationException("Error while sending request!", e);
        }
    }

    private String getResponseBody(Response response) throws RecommendationException
    {
        try {
            if (response.body() != null) {
                return response.body().string();
            } else {
                return "";
            }
        } catch (IOException e) {
            LOG.error("Error while reading response body!", e);
            throw new RecommendationException("Error while reading response body!", e);
        }
    }

    @Override
    public double evaluate(List<CAS> aCasses, DataSplitter aDataSplitter)
    {
        return 0;
    }

    @Override
    public String getPredictedType()
    {
        return recommender.getLayer().getName();
    }

    @Override
    public String getPredictedFeature()
    {
        return recommender.getFeature();
    }
    
    @Override
    public Optional<String> getScoreFeature()
    {
        return Optional.empty();
    }
}

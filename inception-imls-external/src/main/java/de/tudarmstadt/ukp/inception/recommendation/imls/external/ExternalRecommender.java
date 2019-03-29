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
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.util.TypeSystemUtil;
import org.apache.uima.util.XMLSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
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
        aContext.markAsReadyForPrediction();

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


        HttpUrl url = HttpUrl.parse(traits.getRemoteUrl()).newBuilder()
            .addPathSegment("train")
            .build();
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

        PredictionRequest predictionRequest = new PredictionRequest();
        predictionRequest.setTypeSystem(typeSystem);
        predictionRequest.setDocument(buildDocument(aCas));

        // Fill in metadata
        predictionRequest.setMetadata(buildMetadata(aCas));

        HttpUrl url = HttpUrl.parse(traits.getRemoteUrl()).newBuilder()
            .addPathSegment("predict")
            .build();
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

        try (InputStream is = IOUtils.toInputStream(predictionResponse.getDocument(), UTF_8)) {
            XmiCasDeserializer.deserialize(is, aCas, true);
        }
        catch (SAXException | IOException e) {
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
        try (StringWriter out = new StringWriter()) {
            TypeSystemUtil.typeSystem2TypeSystemDescription(aCas.getTypeSystem()).toXML(out);
            return out.toString();
        }
        catch (CASRuntimeException | SAXException | IOException e) {
            throw new RecommendationException("Coud not serialize type system", e);
        }
    }

    private String serializeCas(CAS aCas) throws RecommendationException
    {
        try (StringWriter out = new StringWriter()) {
            // Passing "null" as the type system to the XmiCasSerializer means that we want 
            // to serialize all types (i.e. no filtering for a specific target type system).
            XmiCasSerializer xmiCasSerializer = new XmiCasSerializer(null);
            XMLSerializer sax2xml = new XMLSerializer(out, true);
            xmiCasSerializer.serialize(aCas, sax2xml.getContentHandler(), null, null, null);
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
        } catch (CASException | IllegalArgumentException e) {
            throw new RecommendationException("Error while reading CAS metadata!", e);
        }
    }

    private Metadata buildMetadata(CAS aCas) throws RecommendationException
    {
        CASMetadata casMetadata = getCasMetadata(aCas);
        AnnotationLayer layer =  recommender.getLayer();
        return new Metadata(
            layer.getName(),
            recommender.getFeature().getName(),
            casMetadata.getProjectId(),
            layer.getAnchoringMode().getId(),
            layer.isCrossSentence()
        );
    }

    private PredictionResponse deserializePredictionResponse(Response response)
        throws RecommendationException {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(response.body().byteStream(), PredictionResponse.class);
        } catch (IOException e) {
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

    private Response sendRequest(Request aRequest) throws RecommendationException
    {
        try {
            return client.newCall(aRequest).execute();
        }
        catch (IOException e) {
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
            throw new RecommendationException("Error while reading response body!", e);
        }
    }

    @Override
    public EvaluationResult evaluate(List<CAS> aCasses, DataSplitter aDataSplitter)
    {
        EvaluationResult result = new EvaluationResult(null, null);
        result.setEvaluationSkipped(true);
        return result;
    }

    @Override
    public String getPredictedType()
    {
        return recommender.getLayer().getName();
    }

    @Override
    public String getPredictedFeature()
    {
        return recommender.getFeature().getName();
    }
    
    @Override
    public Optional<String> getScoreFeature()
    {
        return Optional.empty();
    }
    
    @Override
    public boolean requiresTraining()
    {
        return traits.isTrainable();
    }
}

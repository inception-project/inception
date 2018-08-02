package de.tudarmstadt.ukp.inception.recommendation.imls.external.v2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.util.TypeSystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.v2.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.v2.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.v2.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.imls.external.ExternalRecommenderTraits;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ExternalRecommender
    implements RecommendationEngine
{
    private static final Logger LOG = LoggerFactory.getLogger(ExternalRecommender.class);
    private static final MediaType JSON = MediaType.parse("content-type; application/json");

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
    {

    }

    @Override
    public void predict(RecommenderContext aContext, CAS aCas)
    {
        String typeSystem = serializeTypeSystem(aCas);
        String xmi = serializeCas(aCas);

        PredictionRequest predictionRequest = new PredictionRequest();
        predictionRequest.setXmi(xmi);
        predictionRequest.setTypeSystem(typeSystem);
        predictionRequest.setLayer(recommender.getLayer().getName());
        predictionRequest.setFeature(recommender.getFeature());

        RequestBody body = RequestBody.create(JSON, toJson(predictionRequest));
        Request request = new Request.Builder().url(traits.getRemoteUrl()).post(body).build();

        Response response = sendRequest(request);

        // If the response indicates that the request was not successfull,
        // then it does not make sense to go on and try to decode the XMI
        if (!response.isSuccessful()) {
            int code = response.code();
            String status = response.message();
            String msg = String.format("Request was not succesfull: [%i] - [%s]", code, status);
            throw new RuntimeException(msg);
        }

        try {
            XmiCasDeserializer.deserialize(response.body().byteStream(), aCas);
        }
        catch (SAXException | IOException e) {
            LOG.error("Error while deserializing CAS!", e);
            throw new RuntimeException("Error while deserializing CAS!", e);
        }



    }

    private String serializeTypeSystem(CAS aCas)
    {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            TypeSystemUtil.typeSystem2TypeSystemDescription(aCas.getTypeSystem()).toXML(out);
            return new String(Base64.getEncoder().encode(out.toByteArray()), "utf-8");
        }
        catch (CASRuntimeException | SAXException | IOException e) {
            LOG.error("Error while serializing type system!", e);
            throw new RuntimeException("Coud not serialize type system", e);
        }
    }

    private String serializeCas(CAS aCas)
    {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XmiCasSerializer.serialize(aCas, null, out, true, null);
            return new String(Base64.getEncoder().encode(out.toByteArray()), "utf-8");
        }
        catch (CASRuntimeException | SAXException | IOException e) {
            LOG.error("Error while serializing CAS!", e);
            throw new RuntimeException("Error while serializing CAS!", e);
        }
    }

    private String toJson(Object aObject)
    {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(aObject);
        }
        catch (JsonProcessingException e) {
            LOG.error("Error while serializing JSON!", e);
            throw new RuntimeException("Error while serializing JSON!", e);
        }
    }

    private Response sendRequest(Request aRequest)
    {
        try {
            return client.newCall(aRequest).execute();
        }
        catch (IOException e) {
            LOG.error("Error while sending request!", e);
            throw new RuntimeException("Error while sending request!", e);
        }
    }

    @Override
    public double evaluate(List<CAS> aCasses, DataSplitter aDataSplitter)
    {
        return 0;
    }

    @Override
    public boolean isEvaluable()
    {
        return false;
    }
}

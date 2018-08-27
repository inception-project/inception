package de.tudarmstadt.ukp.inception.recommendation.imls.external;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.v2.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.StringMatchingRecommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.StringMatchingRecommenderTraits;

public class RemoteStringMatchingRecommender
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Recommender recommender;
    private final RecommenderContext context;
    private final StringMatchingRecommender recommendationEngine;

    public RemoteStringMatchingRecommender(Recommender aRecommender)
    {
        recommender = aRecommender;
        context = new RecommenderContext();
        StringMatchingRecommenderTraits traits = new StringMatchingRecommenderTraits();
        recommendationEngine = new StringMatchingRecommender(recommender, traits);
    }

    public void train(String aTrainingRequestJson)
    {
        TrainingRequest request = deserializeTrainingRequest(aTrainingRequestJson);

        List<CAS> casses = new ArrayList<>();
        for (String doc : request.getDocuments()) {
            CAS cas = deserializeCas(doc, request.getTypeSystem());
            casses.add(cas);
        }

        recommendationEngine.train(context, casses);
    }

    private TrainingRequest deserializeTrainingRequest(String aRequestJson)
    {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(aRequestJson, TrainingRequest.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String predict(String aPredictionRequestJson)
    {
        PredictionRequest request = deserializePredictionRequest(aPredictionRequestJson);
        CAS cas = deserializeCas(request.getXmi(), request.getTypeSystem());

        recommendationEngine.predict(context, cas);

        return serializeCasToXmi(cas);
    }

    private PredictionRequest deserializePredictionRequest(String aPredictionRequestJson)
    {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(aPredictionRequestJson, PredictionRequest.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // CAS handling

    private CAS deserializeCas(String xmi, String typeSystem)
    {
        byte[] casBytes = Base64.getDecoder().decode(xmi);
        CAS cas = buildCas(typeSystem);
        try (InputStream bais = new ByteArrayInputStream(casBytes)) {
            XmiCasDeserializer.deserialize(bais, cas);
        } catch (IOException | SAXException e) {
            throw new RuntimeException(e);
        }
        return cas;
    }

    private CAS buildCas(String typeSystem)
    {
        // We need to save the typeSystem XML to disk as the
        // JCasFactory needs a file and not a string
        try {
            File typeSystemFile = File.createTempFile("typeSystem", ".xmi");
            FileUtils.writeByteArrayToFile(typeSystemFile, Base64.getDecoder().decode(typeSystem));
            JCas jCas = JCasFactory.createJCasFromPath(typeSystemFile.getAbsolutePath());
            return jCas.getCas();
        } catch (IOException | UIMAException e) {
            throw new RuntimeException(e);
        }
    }

    private String serializeCasToXmi(CAS aCas)
    {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XmiCasSerializer.serialize(aCas, null, out, true, null);
            return new String(out.toByteArray(), "utf-8");
        }
        catch (CASRuntimeException | SAXException | IOException e) {
            log.error("Error while serializing CAS!", e);
            throw new RuntimeException("Error while serializing CAS!", e);
        }
    }
}

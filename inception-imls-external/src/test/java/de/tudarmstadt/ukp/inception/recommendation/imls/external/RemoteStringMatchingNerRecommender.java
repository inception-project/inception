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

import static org.apache.uima.fit.util.CasUtil.getType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.api.type.PredictedSpan;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.StringMatchingRecommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.StringMatchingRecommenderTraits;

public class RemoteStringMatchingNerRecommender
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Recommender recommender;
    private final RecommenderContext context;
    private final StringMatchingRecommender recommendationEngine;

    public RemoteStringMatchingNerRecommender(Recommender aRecommender)
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
        for (Document doc : request.getDocuments()) {
            CAS cas = deserializeCas(doc.getXmi(), request.getTypeSystem());
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

    public String predict(String aPredictionRequestJson) throws RecommendationException
    {
        PredictionRequest request = deserializePredictionRequest(aPredictionRequestJson);
        CAS cas = deserializeCas(request.getDocument().getXmi(), request.getTypeSystem());

        recommendationEngine.predict(context, cas);

        // Convert PredictionSpan to NamedEntity annotations
        Type predictionType = getType(cas, PredictedSpan.class);
        Feature labelFeature = predictionType.getFeatureByBaseName("label");
        Type neType = getType(cas, "de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity");
        Feature valueFeature = neType.getFeatureByBaseName("value");

        for (AnnotationFS fs : CasUtil.select(cas, predictionType)) {
            AnnotationFS ne = cas.createAnnotation(neType, fs.getBegin(), fs.getEnd());
            ne.setStringValue(valueFeature, fs.getStringValue(labelFeature));
            cas.addFsToIndexes(ne);
            cas.removeFsFromIndexes(fs);
        }

        return buildPredictionResponse(cas);
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
        CAS cas = buildCas(typeSystem);
        try (InputStream bais = new ByteArrayInputStream(xmi.getBytes())) {
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
            FileUtils.writeByteArrayToFile(typeSystemFile, typeSystem.getBytes());
            JCas jCas = JCasFactory.createJCasFromPath(typeSystemFile.getAbsolutePath());
            return jCas.getCas();
        } catch (IOException | UIMAException e) {
            throw new RuntimeException(e);
        }
    }

    private String buildPredictionResponse(CAS aCas)
    {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XmiCasSerializer.serialize(aCas, null, out, true, null);
            PredictionResponse response = new PredictionResponse();
            response.setDocument(new String(out.toByteArray()));
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(response);
        }
        catch (CASRuntimeException | SAXException | IOException e) {
            log.error("Error while serializing CAS!", e);
            throw new RuntimeException("Error while serializing CAS!", e);
        }
    }
}

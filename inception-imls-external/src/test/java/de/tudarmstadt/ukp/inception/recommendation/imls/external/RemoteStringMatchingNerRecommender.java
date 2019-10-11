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

import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_IS_PREDICTION;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import org.apache.commons.io.IOUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.XMLInputSource;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.StringMatchingRecommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.StringMatchingRecommenderTraits;

public class RemoteStringMatchingNerRecommender
{
    private final Recommender recommender;
    private final RecommenderContext context;
    private final StringMatchingRecommender recommendationEngine;

    private final String layerName;
    private final String featureName;

    public RemoteStringMatchingNerRecommender(Recommender aRecommender)
    {
        recommender = aRecommender;
        context = new RecommenderContext();
        StringMatchingRecommenderTraits traits = new StringMatchingRecommenderTraits();
        recommendationEngine = new StringMatchingRecommender(recommender, traits);

        layerName = aRecommender.getLayer().getName();
        featureName = aRecommender.getFeature().getName();
    }

    public void train(String aTrainingRequestJson) throws UIMAException, SAXException, IOException, RecommendationException

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

    public String predict(String aPredictionRequestJson) throws IOException, UIMAException, SAXException, RecommendationException
    {
        PredictionRequest request = deserializePredictionRequest(aPredictionRequestJson);
        CAS cas = deserializeCas(request.getDocument().getXmi(), request.getTypeSystem());

        // Only work on real annotations, not on predictions
        Type predictedType = CasUtil.getType(cas, recommender.getLayer().getName());
        Feature feature = predictedType.getFeatureByBaseName(FEATURE_NAME_IS_PREDICTION);

        for (AnnotationFS fs : CasUtil.select(cas, predictedType)) {
            if (fs.getBooleanValue(feature)) {
                cas.removeFsFromIndexes(fs);
            }
        }

        recommendationEngine.predict(context, cas);

        return buildPredictionResponse(cas);
    }

    private PredictionRequest deserializePredictionRequest(String aPredictionRequestJson)
        throws JsonParseException, JsonMappingException, IOException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(aPredictionRequestJson, PredictionRequest.class);
    }

    // CAS handling

    private CAS deserializeCas(String xmi, String typeSystem)
        throws SAXException, IOException, UIMAException
    {
        CAS cas = buildCas(typeSystem);
        try (InputStream bais = new ByteArrayInputStream(xmi.getBytes(UTF_8))) {
            XmiCasDeserializer.deserialize(bais, cas);
        }
        return cas;
    }

    private CAS buildCas(String typeSystem) throws IOException, UIMAException
    {
        // We need to save the typeSystem XML to disk as the
        // JCasFactory needs a file and not a string
        TypeSystemDescription tsd = UIMAFramework.getXMLParser().parseTypeSystemDescription(
                new XMLInputSource(IOUtils.toInputStream(typeSystem, UTF_8), null));
        JCas jCas = JCasFactory.createJCas(tsd);
        return jCas.getCas();
    }

    private String buildPredictionResponse(CAS aCas) throws SAXException, IOException
    {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XmiCasSerializer.serialize(aCas, null, out, true, null);
            PredictionResponse response = new PredictionResponse();
            response.setDocument(new String(out.toByteArray(), UTF_8));
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(response);
        }
    }
}

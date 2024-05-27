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

import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.FEATURE_NAME_IS_PREDICTION;
import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.fromJsonString;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.util.XMLInputSource;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.PredictionContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.imls.external.v1.messages.PredictionRequest;
import de.tudarmstadt.ukp.inception.recommendation.imls.external.v1.messages.PredictionResponse;
import de.tudarmstadt.ukp.inception.recommendation.imls.external.v1.messages.TrainingRequest;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.StringMatchingRecommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.StringMatchingRecommenderTraits;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

public class MockRemoteStringMatchingNerRecommender
{
    private final Recommender recommender;
    private final RecommenderContext context;
    private final StringMatchingRecommender recommendationEngine;

    public MockRemoteStringMatchingNerRecommender(Recommender aRecommender)
    {
        recommender = aRecommender;
        context = new RecommenderContext();
        var traits = new StringMatchingRecommenderTraits();
        recommendationEngine = new StringMatchingRecommender(recommender, traits);
    }

    public void train(String aTrainingRequestJson)
        throws UIMAException, SAXException, IOException, RecommendationException
    {
        var request = fromJsonString(TrainingRequest.class, aTrainingRequestJson);

        List<CAS> casses = new ArrayList<>();
        for (var doc : request.getDocuments()) {
            var cas = deserializeCas(doc.getXmi(), request.getTypeSystem());
            casses.add(cas);
        }

        recommendationEngine.train(context, casses);
    }

    public String predict(String aPredictionRequestJson)
        throws IOException, UIMAException, SAXException, RecommendationException
    {
        var request = fromJsonString(PredictionRequest.class, aPredictionRequestJson);
        var cas = deserializeCas(request.getDocument().getXmi(), request.getTypeSystem());

        // Only work on real annotations, not on predictions
        var predictedType = CasUtil.getType(cas, recommender.getLayer().getName());
        var feature = predictedType.getFeatureByBaseName(FEATURE_NAME_IS_PREDICTION);

        for (AnnotationFS fs : CasUtil.select(cas, predictedType)) {
            if (fs.getBooleanValue(feature)) {
                cas.removeFsFromIndexes(fs);
            }
        }

        recommendationEngine.predict(new PredictionContext(context), cas);

        return buildPredictionResponse(cas);
    }

    // CAS handling

    private CAS deserializeCas(String xmi, String typeSystem)
        throws SAXException, IOException, UIMAException
    {
        CAS cas = buildCas(typeSystem);
        try (var bais = new ByteArrayInputStream(xmi.getBytes(UTF_8))) {
            XmiCasDeserializer.deserialize(bais, cas);
        }
        return cas;
    }

    private CAS buildCas(String typeSystem) throws IOException, UIMAException
    {
        // We need to save the typeSystem XML to disk as the
        // JCasFactory needs a file and not a string
        var tsd = UIMAFramework.getXMLParser().parseTypeSystemDescription(
                new XMLInputSource(IOUtils.toInputStream(typeSystem, UTF_8), null));
        return JCasFactory.createJCas(tsd).getCas();
    }

    private String buildPredictionResponse(CAS aCas) throws SAXException, IOException
    {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XmiCasSerializer.serialize(aCas, null, out, true, null);
            var response = new PredictionResponse();
            response.setDocument(new String(out.toByteArray(), UTF_8));
            return JSONUtil.toJsonString(response);
        }
    }
}

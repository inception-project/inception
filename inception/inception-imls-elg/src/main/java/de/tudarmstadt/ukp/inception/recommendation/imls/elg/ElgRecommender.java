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
package de.tudarmstadt.ukp.inception.recommendation.imls.elg;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.appendIfMissing;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.springframework.http.HttpHeaders;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.NonTrainableRecommenderEngineImplBase;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommenderContext;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.model.ElgAnnotation;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.model.ElgAnnotationsResponse;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.model.ElgResponse;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.model.ElgResponseContainer;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.model.ElgTextsResponse;

public class ElgRecommender
    extends NonTrainableRecommenderEngineImplBase
{
    private static final String BASE_URL = "https://live.european-language-grid.eu/execution/process/";

    private static final int HTTP_BAD_REQUEST = 400;

    private final ElgRecommenderTraits traits;

    private final HttpClient client;

    public ElgRecommender(Recommender aRecommender, ElgRecommenderTraits aTraits)
    {
        super(aRecommender);

        traits = aTraits;

        client = HttpClient.newBuilder().build();
    }

    @Override
    public void predict(RecommenderContext aContext, CAS aCas) throws RecommendationException
    {
        HttpRequest request = HttpRequest.newBuilder() //
                .uri(URI.create(appendIfMissing(BASE_URL, "/")).resolve(traits.getServiceId())) //
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + traits.getToken()) //
                .header(HttpHeaders.CONTENT_TYPE, "text/plain")
                .POST(BodyPublishers.ofString(aCas.getDocumentText(), UTF_8)) //
                .build();

        HttpResponse<String> response = sendRequest(request);

        // If the response indicates that the request was not successful,
        // then it does not make sense to go on and try to decode the XMI
        if (response.statusCode() >= HTTP_BAD_REQUEST) {
            String responseBody = getResponseBody(response);
            String msg = format("Request was not successful: [%d] - [%s]", response.statusCode(),
                    responseBody);
            throw new RecommendationException(msg);
        }

        ElgResponseContainer resp = deserializePredictionResponse(response);

        Type predictedType = getPredictedType(aCas);
        Feature predictedFeature = getPredictedFeature(aCas);
        Feature isPredictionFeature = getIsPredictionFeature(aCas);

        for (Entry<String, List<ElgAnnotation>> group : getAnnotationGroups(resp.getResponse())
                .entrySet()) {
            String tag = group.getKey();
            for (ElgAnnotation elgAnn : group.getValue()) {
                AnnotationFS ann = aCas.createAnnotation(predictedType, elgAnn.getStart(),
                        elgAnn.getEnd());
                ann.setStringValue(predictedFeature, tag);
                ann.setBooleanValue(isPredictionFeature, true);
                aCas.addFsToIndexes(ann);
            }
        }
    }

    private Map<String, List<ElgAnnotation>> getAnnotationGroups(ElgResponse aResponse)
        throws RecommendationException
    {
        if (aResponse instanceof ElgTextsResponse) {
            ElgTextsResponse response = (ElgTextsResponse) aResponse;

            if (response.getTexts().size() != 1) {
                throw new RecommendationException("Expected results for exactly one text, but got ["
                        + response.getTexts().size() + "] results");
            }

            return response.getTexts().get(0).getAnnotations();
        }

        if (aResponse instanceof ElgAnnotationsResponse) {
            return ((ElgAnnotationsResponse) aResponse).getAnnotations();
        }

        throw new IllegalArgumentException("Unknown response type: [" + aResponse.getClass() + "]");
    }

    private ElgResponseContainer deserializePredictionResponse(HttpResponse<String> response)
        throws RecommendationException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(response.body(), ElgResponseContainer.class);
        }
        catch (IOException e) {
            throw new RecommendationException("Error while deserializing prediction response!", e);
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
}

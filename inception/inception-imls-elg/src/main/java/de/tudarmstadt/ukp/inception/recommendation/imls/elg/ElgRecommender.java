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

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.NonTrainableRecommenderEngineImplBase;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.PredictionContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.model.ElgAnnotation;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.model.ElgAnnotationsResponse;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.model.ElgServiceResponse;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.model.ElgSession;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.model.ElgTextsResponse;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.service.ElgService;
import de.tudarmstadt.ukp.inception.rendering.model.Range;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

public class ElgRecommender
    extends NonTrainableRecommenderEngineImplBase
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ElgRecommenderTraits traits;
    private final ElgSession session;

    private final ElgService elgService;

    public ElgRecommender(Recommender aRecommender, ElgRecommenderTraits aTraits,
            ElgService aElgService, ElgSession aSession)
    {
        super(aRecommender);

        traits = aTraits;
        session = aSession;

        elgService = aElgService;
    }

    @Override
    public Range predict(PredictionContext aContext, CAS aCas, int aBegin, int aEnd)
        throws RecommendationException
    {
        ElgServiceResponse response;
        try {
            var text = aCas.getDocumentText().substring(aBegin, aEnd);
            response = elgService.invokeService(session, traits.getServiceUrlSync(), text);
        }
        catch (IOException e) {
            throw new RecommendationException(
                    "Error invoking ELG service: " + getRootCauseMessage(e), e);
        }

        var predictedType = getPredictedType(aCas);
        var predictedFeature = getPredictedFeature(aCas);
        var isPredictionFeature = getIsPredictionFeature(aCas);
        var explanationFeature = getScoreExplanationFeature(aCas);

        for (Entry<String, List<ElgAnnotation>> group : getAnnotationGroups(response).entrySet()) {
            var tag = group.getKey();
            for (var elgAnn : group.getValue()) {
                var ann = aCas.createAnnotation(predictedType, elgAnn.getStart() + aBegin,
                        elgAnn.getEnd() + aBegin);
                ann.setStringValue(predictedFeature, tag);
                ann.setBooleanValue(isPredictionFeature, true);
                if (!elgAnn.getFeatures().isEmpty()) {
                    try {
                        ann.setStringValue(explanationFeature,
                                JSONUtil.toPrettyJsonString(elgAnn.getFeatures()));
                    }
                    catch (CASRuntimeException | IOException e) {
                        ann.setStringValue(explanationFeature,
                                "Unable to display ELG annotation features: "
                                        + getRootCauseMessage(e));
                        log.error("Unable to display ELG annotation features", e);
                    }
                }
                aCas.addFsToIndexes(ann);
            }
        }

        return new Range(aBegin, aEnd);
    }

    private Map<String, List<ElgAnnotation>> getAnnotationGroups(ElgServiceResponse aResponse)
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
}

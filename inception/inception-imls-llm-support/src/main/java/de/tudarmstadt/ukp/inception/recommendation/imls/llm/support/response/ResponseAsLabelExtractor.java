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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response;

import static org.apache.commons.lang3.StringUtils.abbreviateMiddle;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.prompt.PromptContext;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.traits.ChatMessage;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

public class ResponseAsLabelExtractor
    implements ResponseExtractor
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public Map<String, MentionResult> generateExamples(RecommendationEngine aEngine, CAS aCas,
            int aNum)
    {
        return Collections.emptyMap();
    }

    @Override
    public Optional<ResponseFormat> getResponseFormat()
    {
        return Optional.empty();
    }

    @Override
    public Optional<JsonNode> getJsonSchema(Recommender aRecommender,
            AnnotationSchemaService aSchemaService)
    {
        return Optional.empty();
    }

    @Override
    public List<? extends ChatMessage> getFormatDefiningMessages(Recommender aRecommender,
            AnnotationSchemaService aSchemaService)
    {
        return Collections.emptyList();
    }

    @Override
    public void extractMentions(RecommendationEngine aEngine, CAS aCas, PromptContext aContext,
            String aResponse)
    {
        var predictedType = aEngine.getPredictedType(aCas);

        if (aCas.getAnnotationType().subsumes(predictedType)) {
            extractSpanLevelPredictions(aEngine, aCas, aContext, aResponse, predictedType);
        }
        else {
            extractDocumentLevelPredictions(aEngine, aCas, aResponse, predictedType);
        }
    }

    private void extractDocumentLevelPredictions(RecommendationEngine aEngine, CAS aCas,
            String aResponse, Type predictedType)
    {
        var predictedFeature = aEngine.getPredictedFeature(aCas);
        var isPredictionFeature = aEngine.getIsPredictionFeature(aCas);
        var prediction = aCas.createFS(predictedType);
        prediction.setFeatureValueFromString(predictedFeature, aResponse);
        prediction.setBooleanValue(isPredictionFeature, true);
        aCas.addFsToIndexes(prediction);

        LOG.debug("Prediction generated: doc -> [{}]", abbreviateMiddle(aResponse, "…", 30));
    }

    private void extractSpanLevelPredictions(RecommendationEngine aEngine, CAS aCas,
            PromptContext aContext, String aResponse, Type predictedType)
    {
        var range = aContext.getRange();
        var predictedFeature = aEngine.getPredictedFeature(aCas);
        var isPredictionFeature = aEngine.getIsPredictionFeature(aCas);
        var prediction = aCas.createAnnotation(predictedType, range.getBegin(), range.getEnd());
        prediction.setFeatureValueFromString(predictedFeature, aResponse);
        prediction.setBooleanValue(isPredictionFeature, true);
        aCas.addFsToIndexes(prediction);

        LOG.debug("Prediction generated: [{}] -> [{}]",
                abbreviateMiddle(aContext.getText(), "…", 30),
                abbreviateMiddle(aResponse, "…", 30));
    }
}

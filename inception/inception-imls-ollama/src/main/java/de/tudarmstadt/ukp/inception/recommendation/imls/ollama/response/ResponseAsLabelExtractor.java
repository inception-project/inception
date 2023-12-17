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
package de.tudarmstadt.ukp.inception.recommendation.imls.ollama.response;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.imls.ollama.prompt.PromptContext;

public class ResponseAsLabelExtractor
    implements ResponseExtractor
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public List<MentionsSample> generate(RecommendationEngine aEngine, CAS aCas, int aNum)
    {
        return null;
    }

    @Override
    public void extract(RecommendationEngine aEngine, CAS aCas, PromptContext aContext,
            String aResponse)
    {
        var candidate = aContext.getCandidate();

        var predictedType = aEngine.getPredictedType(aCas);
        var predictedFeature = aEngine.getPredictedFeature(aCas);
        var isPredictionFeature = aEngine.getIsPredictionFeature(aCas);

        var prediction = aCas.createAnnotation(predictedType, candidate.getBegin(),
                candidate.getEnd());
        prediction.setFeatureValueFromString(predictedFeature, aResponse);
        prediction.setBooleanValue(isPredictionFeature, true);
        aCas.addFsToIndexes(prediction);

        LOG.debug("Prediction generated [{}] -> [{}]", prediction.getCoveredText(), aResponse);
    }
}

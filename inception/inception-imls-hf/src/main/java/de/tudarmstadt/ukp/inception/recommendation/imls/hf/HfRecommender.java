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
package de.tudarmstadt.ukp.inception.recommendation.imls.hf;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

import java.io.IOException;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.NonTrainableRecommenderEngineImplBase;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.PredictionContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.imls.hf.client.HfInferenceClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.hf.model.HfEntityGroup;
import de.tudarmstadt.ukp.inception.rendering.model.Range;

public class HfRecommender
    extends NonTrainableRecommenderEngineImplBase
{
    private final HfRecommenderTraits traits;

    private final HfInferenceClient hfInferenceClient;

    public HfRecommender(Recommender aRecommender, HfRecommenderTraits aTraits,
            HfInferenceClient aHfInferenceClient)
    {
        super(aRecommender);

        traits = aTraits;

        hfInferenceClient = aHfInferenceClient;
    }

    @Override
    public Range predict(PredictionContext aContext, CAS aCas, int aBegin, int aEnd)
        throws RecommendationException
    {
        List<HfEntityGroup> response;
        try {
            String text = aCas.getDocumentText().substring(aBegin, aEnd);
            response = hfInferenceClient.invokeService(traits.getModelId(), traits.getApiToken(),
                    text);
        }
        catch (IOException e) {
            throw new RecommendationException(
                    "Error invoking HF service: " + getRootCauseMessage(e), e);
        }

        Type predictedType = getPredictedType(aCas);
        Feature predictedFeature = getPredictedFeature(aCas);
        Feature isPredictionFeature = getIsPredictionFeature(aCas);
        Feature scoreFeature = getScoreFeature(aCas);

        for (HfEntityGroup group : response) {
            String tag = group.getEntityGroup();
            AnnotationFS ann = aCas.createAnnotation(predictedType, aBegin + group.getStart(),
                    aBegin + group.getEnd());
            ann.setStringValue(predictedFeature, tag);
            ann.setBooleanValue(isPredictionFeature, true);
            ann.setDoubleValue(scoreFeature, group.getScore());
            aCas.addFsToIndexes(ann);
        }

        return new Range(aBegin, aEnd);
    }
}

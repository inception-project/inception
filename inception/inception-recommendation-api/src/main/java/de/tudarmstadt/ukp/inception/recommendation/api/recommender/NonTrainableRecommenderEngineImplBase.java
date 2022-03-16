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
package de.tudarmstadt.ukp.inception.recommendation.api.recommender;

import java.util.List;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;

public abstract class NonTrainableRecommenderEngineImplBase
    extends RecommendationEngine
{
    public NonTrainableRecommenderEngineImplBase(Recommender aRecommender)
    {
        super(aRecommender);
    }

    @Override
    public void train(RecommenderContext aContext, List<CAS> aCasses)
    {
        // Training not supported
    }

    @Override
    public EvaluationResult evaluate(List<CAS> aCasses, DataSplitter aDataSplitter)
    {
        EvaluationResult result = new EvaluationResult();
        result.setErrorMsg("Evaluation not supported (yet)");
        return result;
    }

    @Override
    public int estimateSampleCount(List<CAS> aCasses)
    {
        return -1;
    }

    @Override
    public boolean isReadyForPrediction(RecommenderContext aContext)
    {
        return true;
    }

    @Override
    public TrainingCapability getTrainingCapability()
    {
        return TrainingCapability.TRAINING_NOT_SUPPORTED;
    }
}

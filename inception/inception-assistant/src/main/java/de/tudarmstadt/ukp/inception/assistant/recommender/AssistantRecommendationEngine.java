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
package de.tudarmstadt.ukp.inception.assistant.recommender;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.NonTrainableRecommenderEngineImplBase;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.PredictionContext;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.support.uima.Range;

/**
 * A no-op recommendation engine for the AI Assistant. This engine never actually runs - it exists
 * only to satisfy the recommender framework requirements. Actual suggestions are created by the
 * assistant tools when explicitly invoked by the AI agent.
 */
public class AssistantRecommendationEngine
    extends NonTrainableRecommenderEngineImplBase
{
    public AssistantRecommendationEngine(Recommender aRecommender)
    {
        super(aRecommender);
    }

    @Override
    public Range predict(PredictionContext aContext, CAS aCas, int aBegin, int aEnd)
        throws RecommendationException
    {
        // Never called - assistant is marked as interactive
        // Suggestions are created by tools, not by engine execution
        return Range.UNDEFINED;
    }
}

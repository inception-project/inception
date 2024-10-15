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
package de.tudarmstadt.ukp.inception.recommendation.api.model;

import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;

public class EvaluatedRecommender
{
    private final Recommender recommender;
    private final EvaluationResult evaluationResult;
    private final boolean active;
    private final String reasonForState;

    private EvaluatedRecommender(Recommender aRecommender, EvaluationResult aEvaluationResult,
            boolean aActive, String aReason)
    {
        super();
        recommender = aRecommender;
        evaluationResult = aEvaluationResult;
        active = aActive;
        reasonForState = aReason;
    }

    public Recommender getRecommender()
    {
        return recommender;
    }

    public EvaluationResult getEvaluationResult()
    {
        return evaluationResult;
    }

    public boolean isActive()
    {
        return active;
    }

    public String getReasonForState()
    {
        return reasonForState;
    }

    @Override
    public String toString()
    {
        return "EvaluatedRecommender [" + recommender + " -> " + (active ? "ACTIVE" : "OFF") + "]";
    }

    public static EvaluatedRecommender makeActiveWithoutEvaluation(Recommender aRecommender,
            String aReason)
    {
        return new EvaluatedRecommender(aRecommender, EvaluationResult.skipped(), true, aReason);
    }

    public static EvaluatedRecommender makeActive(Recommender aRecommender,
            EvaluationResult aEvaluationResult, String aReason)
    {
        return new EvaluatedRecommender(aRecommender, aEvaluationResult, true, aReason);
    }

    public static EvaluatedRecommender makeInactive(Recommender aRecommender,
            EvaluationResult aEvaluationResult, String aReason)
    {
        return new EvaluatedRecommender(aRecommender, aEvaluationResult, false, aReason);
    }

    public static EvaluatedRecommender makeInactiveWithoutEvaluation(Recommender aRecommender,
            String aReason)
    {
        return new EvaluatedRecommender(aRecommender, EvaluationResult.skipped(), false, aReason);
    }
}

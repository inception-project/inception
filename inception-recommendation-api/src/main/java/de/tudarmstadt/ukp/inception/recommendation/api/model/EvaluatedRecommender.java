/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.inception.recommendation.api.model;

import java.io.Serializable;
import java.util.Optional;

import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;

public class EvaluatedRecommender implements Serializable
{
    private static final long serialVersionUID = -2875836319933901200L;
    
    private final Recommender recommender;
    private final EvaluationResult evaluationResult;
    private final boolean active;
    
    public EvaluatedRecommender(Recommender aRecommender, EvaluationResult aEvaluationResult,
            boolean aActive)
    {
        super();
        recommender = aRecommender;
        evaluationResult = aEvaluationResult;
        active = aActive;
    }
    
    public EvaluatedRecommender(Recommender aRecommender)
    {
        this(aRecommender, null, false);
    }

    public Recommender getRecommender()
    {
        return recommender;
    }
    
    public Optional<EvaluationResult> getEvaluationResult()
    {
        return Optional.ofNullable(evaluationResult);
    }

    public boolean isActive()
    {
        return active;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("EvaluatedRecommender [");
        builder.append("recommender=");
        builder.append(recommender.getName());
        builder.append(", ");
        builder.append("active=");
        builder.append(active);
        if (evaluationResult != null) {
            builder.append(", ");
            builder.append("result=");
            builder.append(evaluationResult.toString());
        }
        builder.append("]");
        return builder.toString();
    }
    
    
}

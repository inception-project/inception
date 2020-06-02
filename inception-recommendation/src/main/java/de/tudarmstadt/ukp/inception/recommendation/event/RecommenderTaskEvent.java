/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.event;

import java.util.Optional;

import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.model.EvaluatedRecommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.scheduling.TaskState;
import de.tudarmstadt.ukp.inception.scheduling.TaskUpdateEvent;

public class RecommenderTaskEvent
    extends TaskUpdateEvent
{
    private static final long serialVersionUID = -8145958134839351139L;
    
    private final RecommenderState recommenderState;
    private final EvaluatedRecommender evaluatedRecommender;
    
    public RecommenderTaskEvent(Object aSource, String aUserName, TaskState aState,
            double aProgress, Recommender aRecommender, boolean aActive,
            RecommenderState aRecommenderState, EvaluationResult aResult, String aError)
    {
        super(aSource, aUserName, aState, aProgress, aError);
        recommenderState = aRecommenderState;
        evaluatedRecommender = new EvaluatedRecommender(aRecommender, aResult, aActive);
    }
    
    public RecommenderTaskEvent(Object aSource, String aUserName, TaskState aState,
            double aProgress, Recommender aRecommender, boolean aActive,
            RecommenderState aRecommenderState, EvaluationResult aResult)
    {
        this(aSource, aUserName, aState, aProgress, aRecommender, aActive, aRecommenderState,
                aResult, null);
    }
    
    public RecommenderTaskEvent(Object aSource, String aUserName, TaskState aState,
            double aProgress, Recommender aRecommender, boolean aActive,
            RecommenderState aRecommenderState)
    {
        this(aSource, aUserName, aState, aProgress, aRecommender, aActive, aRecommenderState, null);
    }
    
    public Recommender getRecommender()
    {
        return evaluatedRecommender.getRecommender();
    }
    
    public boolean isActive()
    {
        return evaluatedRecommender.isActive();
    }

    public RecommenderState getRecommenderState()
    {
        return recommenderState;
    }

    public Optional<EvaluationResult> getResult()
    {
        return evaluatedRecommender.getEvaluationResult();
    }

    public EvaluatedRecommender getEvaluatedRecommender()
    {
        return evaluatedRecommender;
    }
    
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("RecommenderTaskEvent [");
        builder.append(infoToString());
        builder.append(", ");
        builder.append("state=");
        builder.append(recommenderState);
        builder.append(", ");
        builder.append("recommender=");
        builder.append(getRecommender().getName());
        builder.append(", ");
        builder.append("active=");
        builder.append(isActive());
        Optional<EvaluationResult> result = getResult();
        if (result.isPresent()) {
            builder.append(", ");
            builder.append("result=");
            builder.append(result.get().toString());
        }
        builder.append("]");
        return builder.toString();
    }
}

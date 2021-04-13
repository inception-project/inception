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
package de.tudarmstadt.ukp.inception.recommendation.event;

import org.springframework.context.ApplicationEvent;

import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;

public class RecommenderEvaluationResultEvent
    extends ApplicationEvent
{
    private static final long serialVersionUID = 4618078923202025558L;

    private final Recommender recommender;
    private final String user;
    private final EvaluationResult evalResult;
    private final long duration;
    private final boolean active;

    public RecommenderEvaluationResultEvent(Object aSource, Recommender aRecommender, String aUser,
            EvaluationResult aResult, long aDuration, boolean aActive)
    {
        super(aSource);

        recommender = aRecommender;
        user = aUser;
        evalResult = aResult;
        duration = aDuration;
        active = aActive;
    }

    public String getUser()
    {
        return user;
    }

    public Recommender getRecommender()
    {
        return recommender;
    }

    public EvaluationResult getResult()
    {
        return evalResult;
    }

    public long getDuration()
    {
        return duration;
    }

    public boolean isActive()
    {
        return active;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("RecommenderEvaluationResultEvent [recommender=");
        builder.append(recommender);
        builder.append(", user=");
        builder.append(user);
        builder.append("]");
        return builder.toString();
    }
}

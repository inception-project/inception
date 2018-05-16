/*
 * Copyright 2018
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

import org.springframework.context.ApplicationEvent;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.ExtendedResult;

public class RecommenderEvaluationResultEvent extends ApplicationEvent
{
    private static final long serialVersionUID = 4618078923202025558L;
    
    private final Recommender recommender;
    private final String user;
    private final ExtendedResult result;
    private final long duration;
    
    public RecommenderEvaluationResultEvent(Object aSource, Recommender aRecommender, String aUser,
            ExtendedResult aResult, long aDuration)
    {
        super(aSource);

        recommender = aRecommender;
        user = aUser;
        result = aResult;
        duration = aDuration;
    }

    public String getUser()
    {
        return user;
    }
    
    public Recommender getRecommender()
    {
        return recommender;
    }
    
    public ExtendedResult getResult()
    {
        return result;
    }
    
    public long getDuration()
    {
        return duration;
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

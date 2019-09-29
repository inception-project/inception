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

public class RecommenderUpdatedEvent
    extends ApplicationEvent
{
    private static final long serialVersionUID = 4618078923202025558L;

    private final Recommender recommender;

    public RecommenderUpdatedEvent(Object aSource, Recommender aRecommender)
    {
        super(aSource);

        recommender = aRecommender;
    }

    public Recommender getRecommender()
    {
        return recommender;
    }

    @Override public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("RecommenderUpdatedEvent [recommender=");
        builder.append(recommender);
        builder.append("]");
        return builder.toString();
    }
}

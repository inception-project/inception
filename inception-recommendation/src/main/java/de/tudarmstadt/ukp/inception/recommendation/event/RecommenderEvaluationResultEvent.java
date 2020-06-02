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

import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.scheduling.TaskState;

public class RecommenderEvaluationResultEvent extends RecommenderTaskEvent
{
    private static final long serialVersionUID = 4618078923202025558L;
    
    private final long duration;
    
    public RecommenderEvaluationResultEvent(Object aSource, String aUser, TaskState aState, 
            double aProgress, Recommender aRecommender, boolean aActive, 
            RecommenderState aRecommenderState, EvaluationResult aResult, long aDuration)
    {
        super(aSource, aUser, aState, aProgress, aRecommender, aActive, aRecommenderState, aResult);
        duration = aDuration;
    }

    public long getDuration()
    {
        return duration;
    }
}

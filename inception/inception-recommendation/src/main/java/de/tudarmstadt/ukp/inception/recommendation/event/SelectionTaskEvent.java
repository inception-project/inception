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


import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;

public class SelectionTaskEvent
    extends RecommenderTaskEvent
{
    private static final long serialVersionUID = -6979755126995087515L;
    
    private final EvaluationResult result;
    
    public SelectionTaskEvent(Object aSource, Recommender aRecommender, String aUser, String aError, 
            String aMsg, EvaluationResult aResult) 
    {
        super(aSource, aUser, aError, aMsg, aRecommender);
        result = aResult;
    }
    
    public SelectionTaskEvent(Object aSource, Recommender aRecommender,
            String aUser, String aError)
    {
        this(aSource, aRecommender, aUser, aError, null, null);
    }

    public SelectionTaskEvent(Object aSource, Recommender aRecommender,
            String aUser, String aError, EvaluationResult aResult)
    {
        this(aSource, aRecommender, aUser, aError, null, aResult);
    }

    public SelectionTaskEvent(Object aSource, Recommender aRecommender,
            String aUser, EvaluationResult aResult)
    {
        this(aSource, aRecommender, aUser, null, null, aResult);
    }

    public EvaluationResult getResult()
    {
        return result;
    }

}

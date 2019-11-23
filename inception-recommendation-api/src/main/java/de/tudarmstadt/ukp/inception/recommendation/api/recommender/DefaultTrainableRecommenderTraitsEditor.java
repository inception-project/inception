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
package de.tudarmstadt.ukp.inception.recommendation.api.recommender;

import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;

public class DefaultTrainableRecommenderTraitsEditor
    extends AbstractTraitsEditor
{
    private static final long serialVersionUID = -1255928405067038588L;

    private static final String MID_DOCUMENT_STATES = "statesForTraining";
    
    private TrainingStatesChoice trainingStatesChoice;

    public DefaultTrainableRecommenderTraitsEditor(String aId, IModel<Recommender> aRecommender)
    {
        super(aId, aRecommender);
        
        trainingStatesChoice = new TrainingStatesChoice(MID_DOCUMENT_STATES, aRecommender);
        trainingStatesChoice.setOutputMarkupPlaceholderTag(true);

        add(trainingStatesChoice);
    }
    
    public TrainingStatesChoice getTrainingStatesChoice()
    {
        return trainingStatesChoice;
    }
}

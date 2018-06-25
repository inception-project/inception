/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.ner;

import de.tudarmstadt.ukp.inception.recommendation.api.ClassifierConfiguration;
import opennlp.tools.ml.AbstractTrainer;
import opennlp.tools.ml.BeamSearch;
import opennlp.tools.ml.EventTrainer;
import opennlp.tools.ml.maxent.GISTrainer;
import opennlp.tools.util.TrainingParameters;

/**
 * The base configuration for the implemented Named Entity Recognition using the
 * <a href="https://opennlp.apache.org/">OpenNlp library</a>.
 * 
 * Using the following parameters for the NamedEntityFinder of the OpenNlp library.
 * 
 * <ul>
 * <li>Algorithm = GIS.MAXENT_VALUE</li>
 * <li>Iterations = 100</li>
 * <li>Trainer Type = EventTrainer.Event_Value</li>
 * <li>Cuttoff = 5</li>
 * <li>Beam Size = 3</li>
 * </ul>
 * 
 *
 *
 */
public class BaseConfiguration
    extends ClassifierConfiguration<TrainingParameters>
{

    public BaseConfiguration()
    {
        TrainingParameters params = new TrainingParameters();
        params.put(AbstractTrainer.VERBOSE_PARAM, "false");
        params.put(TrainingParameters.ALGORITHM_PARAM, GISTrainer.MAXENT_VALUE);
        params.put(TrainingParameters.ITERATIONS_PARAM, Integer.toString(100));
        params.put(TrainingParameters.TRAINER_TYPE_PARAM, EventTrainer.EVENT_VALUE);
        params.put(TrainingParameters.CUTOFF_PARAM, Integer.toString(5));
        params.put(BeamSearch.BEAM_SIZE_PARAMETER, Integer.toString(3));

        this.setParams(params);

        this.setTrainingSetStartSize(3);
    }
    
    public BaseConfiguration(String aFeature, long aRecommenderId)
    {
        this();
        setFeature(aFeature);
        setRecommenderId(aRecommenderId);
    }
}

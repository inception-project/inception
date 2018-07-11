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
package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.pos;

import de.tudarmstadt.ukp.inception.recommendation.api.ClassifierConfiguration;
import opennlp.tools.ml.AbstractTrainer;
import opennlp.tools.ml.BeamSearch;
import opennlp.tools.ml.EventTrainer;
import opennlp.tools.util.TrainingParameters;

public class CustomConfiguration
    extends ClassifierConfiguration<TrainingParameters>
{

    public CustomConfiguration(int beamSize)
    {
        TrainingParameters params = new TrainingParameters();
        params.put(AbstractTrainer.VERBOSE_PARAM, "false");
        params.put(TrainingParameters.ALGORITHM_PARAM, "MAXENT");
        params.put(TrainingParameters.TRAINER_TYPE_PARAM, EventTrainer.EVENT_VALUE);
        params.put(TrainingParameters.ITERATIONS_PARAM, "10");
        params.put(TrainingParameters.CUTOFF_PARAM, "5");
        params.put(BeamSearch.BEAM_SIZE_PARAMETER, Integer.toString(beamSize));
        this.setParams(params);
    }
    
    public CustomConfiguration(int beamSize, String feature, long aRecommenderId)
    {
        this(beamSize);
        setFeature(feature);
        setRecommenderId(aRecommenderId);
    }
}

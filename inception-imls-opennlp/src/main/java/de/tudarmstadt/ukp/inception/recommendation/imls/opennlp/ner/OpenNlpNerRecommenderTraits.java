/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.ner;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import opennlp.tools.ml.AbstractTrainer;
import opennlp.tools.util.TrainingParameters;

public class OpenNlpNerRecommenderTraits
    implements Serializable
{
    private static final long serialVersionUID = 7717316701623340670L;

    private int trainingSetSizeLimit = Integer.MAX_VALUE;
    private int predictionLimit = Integer.MAX_VALUE;
    
    private int numThreads = 1;

    public int getNumThreads()
    {
        return numThreads;
    }

    public void setNumThreads(int aNumThreads)
    {
        numThreads = aNumThreads;
    }

    public int getTrainingSetSizeLimit()
    {
        return trainingSetSizeLimit;
    }

    public void setTrainingSetSizeLimit(int aTrainingSetSizeLimit)
    {
        trainingSetSizeLimit = aTrainingSetSizeLimit;
    }

    public int getPredictionLimit()
    {
        return predictionLimit;
    }

    public void setPredictionLimit(int aPredictionLimit)
    {
        predictionLimit = aPredictionLimit;
    }

    @JsonIgnore
    public TrainingParameters getParameters()
    {
        TrainingParameters parameters = TrainingParameters.defaultParams();
        parameters.put(AbstractTrainer.VERBOSE_PARAM, "false");
        parameters.put(TrainingParameters.THREADS_PARAM, numThreads);
        return parameters;
    }
}

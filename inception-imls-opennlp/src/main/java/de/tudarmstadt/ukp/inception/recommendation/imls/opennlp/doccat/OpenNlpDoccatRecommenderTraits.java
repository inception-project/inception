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
package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.doccat;

import java.io.Serializable;

import opennlp.tools.ml.AbstractTrainer;
import opennlp.tools.util.TrainingParameters;

public class OpenNlpDoccatRecommenderTraits
    implements Serializable
{
    private static final long serialVersionUID = 220089332064652542L;

    private int ONDRT_trainingSetSizeLimit = Integer.MAX_VALUE;
    private int ONDRT_predictionLimit = Integer.MAX_VALUE;

    private int iterations = 100;
    private int cutoff = 5;
    private int ONDRT_numThreads = 1;

    public int getIterations()
    {
        return iterations;
    }

    public void setIterations(int aIterations)
    {
        iterations = aIterations;
    }

    public int getCutoff()
    {
        return cutoff;
    }

    public void setCutoff(int aCutoff)
    {
        cutoff = aCutoff;
    }
    /**
     * getNumThreads method from class OpenNlpDoccatRecommenderTraits
     * @return ONDRT_numThreads
     */
    public int ONDRT_getNumThreads()
    {
        return ONDRT_numThreads;
    }
    
    /**
     * setNumThreads method from class OpenNlpDoccatRecommenderTraits
     * @return 
     */
    public void ONDRT_setNumThreads(int ONDRT_aNumThreads)
    {
        ONDRT_numThreads = ONDRT_aNumThreads;
    }
    /**
     * getTrainingSetSizeLimit method from class OpenNlpDoccatRecommenderTraits
     * @return ONDRT_trainingSetSizeLimit
     */
    public int getTrainingSetSizeLimit()
    {
        return ONDRT_trainingSetSizeLimit;
    }
    /**
     * setTrainingSetSizeLimit method from class OpenNlpDoccatRecommenderTraits
     * @return 
     */
    public void setTrainingSetSizeLimit(int ONDRT_aTrainingSetSizeLimit)
    {
        ONDRT_trainingSetSizeLimit = ONDRT_aTrainingSetSizeLimit;
    }
    /**
     * getPredictionLimit method from class OpenNlpDoccatRecommenderTraits
     * @return ONDRT_predictionLimit
     */
    public int ONDRT_getPredictionLimit()
    {
        return ONDRT_predictionLimit;
    }
    /**
     * setPredictionLimit method from class OpenNlpDoccatRecommenderTraits
     * @return 
     */
    public void ONDRT_setPredictionLimit(int ONDRT_aPredictionLimit)
    {
        ONDRT_predictionLimit = ONDRT_aPredictionLimit;
    }
    
    public TrainingParameters ONDRT_getParameters()
    {
        TrainingParameters ONDRT_parameters = TrainingParameters.defaultParams();
        ONDRT_parameters.put(AbstractTrainer.VERBOSE_PARAM, false);
        ONDRT_parameters.put(TrainingParameters.ITERATIONS_PARAM, iterations);
        ONDRT_parameters.put(TrainingParameters.CUTOFF_PARAM, cutoff);
        ONDRT_parameters.put(TrainingParameters.THREADS_PARAM, ONDRT_numThreads);
        return ONDRT_parameters;
    }
}

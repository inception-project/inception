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
package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.doccat;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import opennlp.tools.util.TrainingParameters;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenNlpDoccatRecommenderTraits
    implements Serializable
{
    private static final long serialVersionUID = 220089332064652542L;

    private int trainingSetSizeLimit = Integer.MAX_VALUE;
    private int predictionLimit = Integer.MAX_VALUE;

    private int iterations = 100;
    private int cutoff = 5;
    private int numThreads = 1;

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

    public TrainingParameters getParameters()
    {
        TrainingParameters parameters = TrainingParameters.defaultParams();
        parameters.put(TrainingParameters.ITERATIONS_PARAM, iterations);
        parameters.put(TrainingParameters.CUTOFF_PARAM, cutoff);
        parameters.put(TrainingParameters.THREADS_PARAM, numThreads);
        return parameters;
    }
}

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

    private int doc_trainingSetSizeLimit = Integer.MAX_VALUE;
    private int doc_predictionLimit = Integer.MAX_VALUE;

    private int iterations = 100;
    private int cutoff = 5;
    private int doc_numThreads = 1;

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
    
    public int doc_getNumThreads()
    {
        return doc_numThreads;
    }

    public void doc_setNumThreads(int doc_aNumThreads)
    {
        doc_numThreads = doc_aNumThreads;
    }

    public int getTrainingSetSizeLimit()
    {
        return doc_trainingSetSizeLimit;
    }

    public void setTrainingSetSizeLimit(int doc_aTrainingSetSizeLimit)
    {
        doc_trainingSetSizeLimit = doc_aTrainingSetSizeLimit;
    }

    public int doc_getPredictionLimit()
    {
        return doc_predictionLimit;
    }

    public void doc_setPredictionLimit(int doc_aPredictionLimit)
    {
        doc_predictionLimit = doc_aPredictionLimit;
    }

    public TrainingParameters doc_getParameters()
    {
        TrainingParameters doc_parameters = TrainingParameters.defaultParams();
        doc_parameters.put(AbstractTrainer.VERBOSE_PARAM, false);
        doc_parameters.put(TrainingParameters.ITERATIONS_PARAM, iterations);
        doc_parameters.put(TrainingParameters.CUTOFF_PARAM, cutoff);
        doc_parameters.put(TrainingParameters.THREADS_PARAM, doc_numThreads);
        return doc_parameters;
    }
}

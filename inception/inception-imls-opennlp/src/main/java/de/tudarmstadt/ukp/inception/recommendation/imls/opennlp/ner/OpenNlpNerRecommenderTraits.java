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
package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.ner;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import opennlp.tools.util.TrainingParameters;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenNlpNerRecommenderTraits
    implements Serializable
{
    private static final long serialVersionUID = 7717316701623340670L;

    private @JsonInclude(Include.NON_DEFAULT) int trainingSetSizeLimit = 0;
    private @JsonInclude(Include.NON_DEFAULT) int predictionLimit = 0;
    private @JsonInclude(Include.NON_DEFAULT) int windowSize = 0;
    private @JsonInclude(Include.NON_DEFAULT) double correctionThreshold = 0.95;
    private @JsonInclude(Include.NON_DEFAULT) boolean correctionsEnabled = false;

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

    public void setWindowSize(int aWindowSize)
    {
        windowSize = aWindowSize;
    }

    public int getWindowSize()
    {
        return windowSize;
    }

    public double getCorrectionThreshold()
    {
        return correctionThreshold;
    }

    public void setCorrectionThreshold(double aCorrectionThreshold)
    {
        correctionThreshold = aCorrectionThreshold;
    }

    public boolean isCorrectionsEnabled()
    {
        return correctionsEnabled;
    }

    public void setCorrectionsEnabled(boolean aCorrectionsEnabled)
    {
        correctionsEnabled = aCorrectionsEnabled;
    }

    @JsonIgnore
    public TrainingParameters getParameters()
    {
        TrainingParameters parameters = TrainingParameters.defaultParams();
        parameters.put(TrainingParameters.THREADS_PARAM, numThreads);
        return parameters;
    }
}

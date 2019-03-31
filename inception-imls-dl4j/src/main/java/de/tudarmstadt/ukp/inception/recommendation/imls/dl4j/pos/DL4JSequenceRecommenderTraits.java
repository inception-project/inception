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
package de.tudarmstadt.ukp.inception.recommendation.imls.dl4j.pos;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

public class DL4JSequenceRecommenderTraits
{
    // General parameters
    private int trainingSetSizeLimit = Integer.MAX_VALUE;
    private int predictionLimit = Integer.MAX_VALUE;
    private int batchSize = 250;
    private int maxTagsetSize = 70;
    private int maxSentenceLength = 150;
    private int nEpochs = 1;
    
    // Network parameters
    private OptimizationAlgorithm optimizationAlgorithm = 
            OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT;
    private WeightInit weightInit = WeightInit.RELU;
    private GradientNormalization gradientNormalization = 
            GradientNormalization.ClipElementWiseAbsoluteValue;
    private double gradientNormalizationThreshold = 1.0;
    private Updater updater = Updater.RMSPROP;
    private double l2 = 1e-5;
    private Activation activationL0 = Activation.SOFTSIGN;
    private Activation activationL1 = Activation.SOFTMAX;
    private LossFunction lossFunction = LossFunction.MCXENT;

    public int getTrainingSetSizeLimit()
    {
        return trainingSetSizeLimit;
    }

    public void setTrainingSetSizeLimit(int aLimit)
    {
        trainingSetSizeLimit = aLimit;
    }
    
    public int getPredictionLimit()
    {
        return predictionLimit;
    }

    public void setPredictionLimit(int aPredictionLimit)
    {
        predictionLimit = aPredictionLimit;
    }

    public int getBatchSize()
    {
        return batchSize;
    }

    public void setBatchSize(int aBatchSize)
    {
        batchSize = aBatchSize;
    }

    public int getMaxTagsetSize()
    {
        return maxTagsetSize;
    }

    public void setMaxTagsetSize(int maxTagsetSize)
    {
        this.maxTagsetSize = maxTagsetSize;
    }

    public int getMaxSentenceLength()
    {
        return maxSentenceLength;
    }

    public void setMaxSentenceLength(int truncateLength)
    {
        this.maxSentenceLength = truncateLength;
    }

    public int getnEpochs()
    {
        return nEpochs;
    }

    public void setnEpochs(int nEpochs)
    {
        this.nEpochs = nEpochs;
    }

    public OptimizationAlgorithm getOptimizationAlgorithm()
    {
        return optimizationAlgorithm;
    }

    public void setOptimizationAlgorithm(OptimizationAlgorithm optimizationAlgorithm)
    {
        this.optimizationAlgorithm = optimizationAlgorithm;
    }

    public WeightInit getWeightInit()
    {
        return weightInit;
    }

    public void setWeightInit(WeightInit weightInit)
    {
        this.weightInit = weightInit;
    }

    public GradientNormalization getGradientNormalization()
    {
        return gradientNormalization;
    }

    public void setGradientNormalization(GradientNormalization gradientNormalization)
    {
        this.gradientNormalization = gradientNormalization;
    }

    public double getGradientNormalizationThreshold()
    {
        return gradientNormalizationThreshold;
    }

    public void setGradientNormalizationThreshold(double gradientNormalizationThreshold)
    {
        this.gradientNormalizationThreshold = gradientNormalizationThreshold;
    }

    public Updater getUpdater()
    {
        return updater;
    }

    public void setUpdater(Updater updater)
    {
        this.updater = updater;
    }

    public double getL2()
    {
        return l2;
    }

    public void setL2(double l2)
    {
        this.l2 = l2;
    }

    public Activation getActivationL0()
    {
        return activationL0;
    }

    public void setActivationL0(Activation activationL0)
    {
        this.activationL0 = activationL0;
    }

    public Activation getActivationL1()
    {
        return activationL1;
    }

    public void setActivationL1(Activation activationL1)
    {
        this.activationL1 = activationL1;
    }

    public LossFunction getLossFunction()
    {
        return lossFunction;
    }

    public void setLossFunction(LossFunction lossFunction)
    {
        this.lossFunction = lossFunction;
    }
}

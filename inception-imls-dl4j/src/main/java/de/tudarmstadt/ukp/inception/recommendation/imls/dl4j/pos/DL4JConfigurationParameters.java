/*
 * Copyright 2017
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

import java.io.File;
import java.io.IOException;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.weights.WeightInit;
import org.dkpro.core.api.embeddings.binary.BinaryVectorizer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

public class DL4JConfigurationParameters
{
    BinaryVectorizer wordVectors = null;
    private int iterations = 2;
    private double learningRate = 0.1;
    private int maxTagsetSize = 70;
    private int truncateLength = 150;
    private int nEpochs = 2;
    private OptimizationAlgorithm optimizationAlgorithm = 
            OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT;
    private WeightInit weightInit = WeightInit.XAVIER;
    private GradientNormalization gradientNormalization = 
            GradientNormalization.ClipElementWiseAbsoluteValue;
    private double gradientNormalizationThreshold = 1.0;
    private Updater updater = Updater.RMSPROP;
    private boolean regularization = true;
    private double l2 = 1e-5;
    private Activation activationL0 = Activation.SOFTSIGN;
    private Activation activationL1 = Activation.SOFTSIGN;
    private LossFunction lossFunction = LossFunction.MCXENT;    
    private File tagsetFile;
    private static final String tagsetEntryName = "tagset.txt";
        
    public void createWordVectors(File embeddingsLocation) {
        try {
            wordVectors = BinaryVectorizer.load(embeddingsLocation);
        }
        catch (IOException e1) {
            // TODO LOG!
            return;
        }
    }
    
    public BinaryVectorizer getWordVectors() {
        return wordVectors;
    }
    public int getIterations()
    {
        return iterations;
    }
    public void setIterations(int iterations)
    {
        this.iterations = iterations;
    }
    public double getLearningRate()
    {
        return learningRate;
    }
    public void setLearningRate(double learningRate)
    {
        this.learningRate = learningRate;
    }
    public int getMaxTagsetSize()
    {
        return maxTagsetSize;
    }
    public void setMaxTagsetSize(int maxTagsetSize)
    {
        this.maxTagsetSize = maxTagsetSize;
    }
    public int getTruncateLength()
    {
        return truncateLength;
    }
    public void setTruncateLength(int truncateLength)
    {
        this.truncateLength = truncateLength;
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
    public File getTagsetFile() {
        return tagsetFile;
    }    
    public String getTagsetEntryName() {
        return tagsetEntryName;
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
    public boolean isRegularization()
    {
        return regularization;
    }
    public void setRegularization(boolean regularization)
    {
        this.regularization = regularization;
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
    public void setTagsetFile(File tagesetFile)
    {
        this.tagsetFile = tagesetFile;
    }
    
    
}

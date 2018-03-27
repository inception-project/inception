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
package de.tudarmstadt.ukp.inception.recommendation.imls.conf;

import java.io.File;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Configuration settings for the evaluation runs.
 * 
 * It contains the following properties:
 * <ul>
 * <li>trainingSetSizeLimit: Limits the training set to a given number of sentences, i.e. it cancels
 * the evaluation after exceeding the training set limit. A trainingSetSizeLimit equal to 0 means no
 * limit.</li>
 * <li>resultFolder: The path to the folder in which the result reports are stored.</li>
 * <li>debugFolder: The path to the folder in which the debug reports are stored.</li>
 * <li>isDebug: If true, the debug reports are generated.</li>
 * <li>isShuffleTrainingSet: If true, input data is shuffled before training the model to simulate a
 * non-linear annotation process.
 * <li>splitTestDataPercentage: Defines the percentage of the data, which is used as a validation
 * set and not as training data. Ranges from 0.0 to 1.0.</li>
 * </ul>
 * 
 *
 *
 */
public class EvaluationConfiguration
{
    private int trainingSetSizeLimit = 0;
    private File resultFolder = new File("target/results/");
    private File debugFolder = new File("target/debug/");
    private boolean isDebug = false;
    private boolean isShuffleTrainingSet = false;
    private double splitTestDataPercentage = 0.4;
    private boolean useHoldout;
    private String feature;
    private String trainingIncrementStrategy;
    private int trainingIncrementSize;
    private int testIncrementSize;

    public EvaluationConfiguration()
    {
        super();
    }

    @Override
    public String toString()
    {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }

    public String getFeature()
    {
        return feature;
    }
    
    public void setFeature(String feature)
    {
        this.feature = feature;
    }
    
    public int getTrainingSetSizeLimit()
    {
        return trainingSetSizeLimit;
    }

    public void setTrainingSetSizeLimit(int trainingSetSizeLimit)
    {
        this.trainingSetSizeLimit = trainingSetSizeLimit;
    }

    public File getResultFolder()
    {
        if (resultFolder.exists() && resultFolder.isDirectory()) {
            return resultFolder;
        }

        if (resultFolder.mkdirs()) {
            return resultFolder;
        }

        return null;
    }

    public void setResultFolder(String resultFolder)
    {
        this.resultFolder = new File(resultFolder);
    }

    public File getDebugFolder()
    {
        if (debugFolder.exists() && debugFolder.isDirectory()) {
            return debugFolder;
        }

        if (debugFolder.mkdirs()) {
            return debugFolder;
        }

        return debugFolder;
    }

    public void setDebugFolder(String debugFolder)
    {
        this.debugFolder = new File(debugFolder);
    }

    public boolean isDebug()
    {
        return isDebug;
    }

    public void setDebug(boolean isDebug)
    {
        this.isDebug = isDebug;
    }

    public boolean isShuffleTrainingSet()
    {
        return isShuffleTrainingSet;
    }

    public void setShuffleTrainingSet(boolean isShuffleTrainingSet)
    {
        this.isShuffleTrainingSet = isShuffleTrainingSet;
    }

    public double getSplitTestDataPercentage()
    {
        return splitTestDataPercentage;
    }

    public void setSplitTestDataPercentage(double splitTestDataPercentage)
    {
        this.splitTestDataPercentage = splitTestDataPercentage;
    }
    
    public boolean isUseHoldout() {
        return useHoldout;
    }
    
    public void setUseHoldout(boolean useHoldout)
    {
        this.useHoldout = useHoldout;
    }
    
    public void setIncrementStrategy(String strategy) {
        this.trainingIncrementStrategy = strategy;
    }
    
    public boolean isFibonacciIncrementStrategy() {
        return trainingIncrementStrategy.equals("fibonacciIncrementStrategy");
    }
    
    public boolean isFixedSizeIncrementStrategy() {
        return trainingIncrementStrategy.equals("fixedSizeIncrementStrategy");
    }
    
    public int getTrainingIncrementSize()
    {
        return trainingIncrementSize;
    }

    public void setTrainingIncrementSize(int incrementSize)
    {
        this.trainingIncrementSize = incrementSize;
    }
    
    public int getTestIncrementSize()
    {
        return testIncrementSize;
    }

    public void setTestIncrementSize(int testIncrementSize)
    {
        this.testIncrementSize = testIncrementSize;
    }

}

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
package de.tudarmstadt.ukp.inception.recommendation.api;

import java.io.File;

/**
 * The generic classifier configuration. It is used to define more general settings like:
 * <ul>
 * <li>trainingSetStartSize: The minimum amount of training sentences needed to train the classifier
 * model.</li>
 * <li>language: The language of the data used for the training and the prediction.</li>
 * <li>modelFile: The file path to the model file. This is necessary for classification algorithm
 * implementations, which have to persist and load the model.</li>
 * </ul>
 * 
 * Furthermore, it provides algorithm specific parameters.
 * 
 *
 *
 * @param <T>
 *            The algorithm specific parameter object.
 */
public class ClassifierConfiguration<T>
{
    private int trainingSetStartSize = 0;
    private T params;
    private String language = "en";
    private File modelFile = new File("target/models", "model.bin");
    private String feature;
    private long recommenderId;

    /**
     * The number of predictions that should be displayed
     */
    private int numPredictions;

    public ClassifierConfiguration() 
    {
        
    }
    
    public ClassifierConfiguration(String aFeature, long aRecommenderId)
    {
        feature = aFeature;
        recommenderId = aRecommenderId;
    }
    
    public int getTrainingSetStartSize()
    {
        return trainingSetStartSize;
    }

    public void setTrainingSetStartSize(int trainingSetStartSize)
    {
        this.trainingSetStartSize = trainingSetStartSize;
    }

    public T getParams()
    {
        return params;
    }

    public void setParams(T params)
    {
        this.params = params;
    }

    public String getLanguage()
    {
        return language;
    }

    public void setLanguage(String language)
    {
        this.language = language;
    }

    public File getModelFile()
    {
        if (!modelFile.exists()) {
            File dir = modelFile.getParentFile();
            dir.mkdirs();
        }

        return modelFile;
    }

    public void setModelFile(File modelFile)
    {
        this.modelFile = modelFile;
    }

    public int getNumPredictions()
    {
        return numPredictions;
    }

    public void setNumPredictions(int numPredictions)
    {
        this.numPredictions = numPredictions;
    }

    public String getFeature()
    {
        return feature;
    }

    public void setFeature(String feature)
    {
        this.feature = feature;
    }

    public long getRecommenderId()
    {
        return recommenderId;
    }

    public void setRecommenderId(long aRecommenderId)
    {
        recommenderId = aRecommenderId;
    }
}

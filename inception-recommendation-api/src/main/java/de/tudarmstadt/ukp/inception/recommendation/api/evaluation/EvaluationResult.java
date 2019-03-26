/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.inception.recommendation.api.evaluation;

public class EvaluationResult
{
    private double defaultScore;
    private int trainingSetSize;
    private int testSetSize;
    private boolean skippedEvaluation;

    /**
     * Get the specific score which the recommender evaluation uses e.g. accuracy or f-score as a
     * default.
     */
    public double getDefaultScore()
    {
        return defaultScore;
    }

    /**
     * Set the specific score which the recommender evaluation uses e.g. accuracy or f-score.
     */
    public void setDefaultScore(double aDefaultScore)
    {
        defaultScore = aDefaultScore;
    }

    /**
     * Get the size of the training data used in the recommender evaluation.
     * 
     * @return the training set size
     */
    public int getTrainingSetSize()
    {
        return trainingSetSize;
    }

    /**
     * Set the size of the training data used in the recommender evaluation.
     */
    public void setTrainingSetSize(int aTrainingSetSize)
    {
        trainingSetSize = aTrainingSetSize;
    }

    /**
     * Get the size of the test data used in the recommender evaluation.
     * 
     * @return the test size
     */
    public int getTestSetSize()
    {
        return testSetSize;
    }

    /**
     * Set the size of the test data used in the recommender evaluation.
     */
    public void setTestSetSize(int aTestSetSize)
    {
        testSetSize = aTestSetSize;
    }

    public void setEvaluationSkipped(boolean aSkipVal)
    {
        skippedEvaluation = aSkipVal;
    }

    /**
     * Determine if evaluation was skipped.
     * 
     * @return true if evaluation was skipped
     */
    public boolean isEvaluationSkipped()
    {
        return skippedEvaluation;
    }

}

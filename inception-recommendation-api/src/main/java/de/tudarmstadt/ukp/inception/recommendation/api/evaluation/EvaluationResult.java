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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class EvaluationResult
{
    private int trainingSetSize;
    private int testSetSize;
    private boolean skippedEvaluation;

    private String ignoreLabel;

    /**
     * Stores number of predicted labels for each gold label
     */
    private Map<String, Map<String, Integer>> confusionMatrix;
    private List<String> labels;

    private int total;
    private int numOfLabels;

    // TODO replace or delete
    private double defaultScore;

    /**
     * Calculate macro-averaged scores on per-token basis over all labels contained in the given
     * annotated pairs except for those matching the given ignore-label.
     * 
     * @param aIgnoreLabel
     *            this label will be ignored during evaluation
     * @param aAnnotatedPairs
     *            pairs of gold and predicted labels for the same token
     */
    public EvaluationResult(String aIgnoreLabel, Stream<AnnotatedTokenPair> aAnnotatedPairs)
    {
        super();
        labels = new ArrayList<>();
        ignoreLabel = aIgnoreLabel;
        // construct confusion matrix
        confusionMatrix = new HashMap<>();
        aAnnotatedPairs.filter(this::isEqualIgnoreLabel).forEach(this::incConfusionMatrix);
    }

    public EvaluationResult()
    {
    }

    private boolean isEqualIgnoreLabel(AnnotatedTokenPair aPair)
    {
        return !aPair.getGoldLabel().equals(ignoreLabel)
                && !aPair.getPredictedLabel().equals(ignoreLabel);
    }

    private void incConfusionMatrix(AnnotatedTokenPair aPair)
    {
        String goldLabel = aPair.getGoldLabel();
        String predictedLabel = aPair.getPredictedLabel();

        // annotated pair is true positive
        if (goldLabel.equals(predictedLabel)) {
            incCounter(goldLabel, goldLabel);
        }
        else {
            // annotated pair is false negative for gold class = annotated pair is false
            // positive for predicted class
            incCounter(goldLabel, predictedLabel);
        }

        total += 1;
    }

    private void incCounter(String aGoldLabel, String aPredictedLabel)
    {
        if (!confusionMatrix.containsKey(aGoldLabel)) {
            Map<String, Integer> initMap = new HashMap<>();
            initMap.put(aGoldLabel, 0);
            confusionMatrix.put(aGoldLabel, initMap);

            labels.add(aGoldLabel);
            numOfLabels++;
        }
        if (!confusionMatrix.get(aGoldLabel).containsKey(aPredictedLabel)) {
            confusionMatrix.get(aGoldLabel).put(aPredictedLabel, 0);
        }
        int count = confusionMatrix.get(aGoldLabel).get(aPredictedLabel) + 1;
        confusionMatrix.get(aGoldLabel).put(aPredictedLabel, count);
    }

    /**
     * Calculate accuracy
     * 
     * @return accuracy score
     */
    public double getAccuracyScore()
    {
        double tp = 0.0;
        for (String label : labels) {
            tp += confusionMatrix.get(label).get(label);
        }
        return (total > 0) ? tp / (double) total : 0.0;
    }

    /**
     * Calculate macro-averaged precision score
     * 
     * @return precision score
     */
    public double getPrecisionScore()
    {
        double precision = 0.0;
        if (numOfLabels > 0) {
            for (String label : labels) {
                double tp = confusionMatrix.get(label).get(label);
                double numPredictedAsLabel = 0.0;
                for (String predictedLabel : labels) {
                    numPredictedAsLabel += confusionMatrix.get(predictedLabel).get(label);
                }
                precision += tp / numPredictedAsLabel;

            }
            precision = precision / numOfLabels;
        }
        return precision;
    }

    /**
     * Calculate macro-averaged recall score
     * 
     * @return recall score
     */
    public double getRecallScore()
    {
        double recall = 0.0;
        if (numOfLabels > 0) {
            for (String label : labels) {
                double tp = confusionMatrix.get(label).get(label);
                double numIsLabel = 0.0;
                for (String predictedLabel : labels) {
                    numIsLabel += confusionMatrix.get(label).get(predictedLabel);

                }
                recall += (numIsLabel > 0) ? tp / numIsLabel : 0;

            }
            recall = recall / numOfLabels;
        }
        return recall;
    }

    /**
     * Calculate macro-averaged f1-score
     * 
     * @return f1 score
     */
    public double getF1Score()
    {
        double precision = getPrecisionScore();
        double recall = getRecallScore();
        return (precision > 0 || recall > 0) ? 2 * precision * recall / (precision + recall) : 0;
    }

    /**
     * Set the specific score which the recommender evaluation uses e.g. accuracy or f-score.
     */
    public void setDefaultScore(double aDefaultScore)
    {
        defaultScore = aDefaultScore;
    }

    public double getDefaultScore()
    {
        return defaultScore;
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

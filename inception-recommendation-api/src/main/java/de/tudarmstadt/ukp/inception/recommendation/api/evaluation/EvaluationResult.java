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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleBiFunction;
import java.util.stream.Stream;

public class EvaluationResult
{
    private int trainingSetSize;
    private int testSetSize;
    private boolean skippedEvaluation;

    private final Set<String> ignoreLabels;

    /**
     * Stores number of predicted labels for each gold label
     */
    private Map<String, Map<String, Integer>> confusionMatrix;
    private List<String> labels;

    private int total;
    private int numOfLabels;


    /**
     * Calculate macro-averaged scores on per-token basis over all labels contained in the given
     * annotated pairs except for those matching the given ignore-labels as a gold label.
     * 
     * @param aIgnoreLabels
     *            these labels will be ignored as gold labels during evaluation
     * @param aAnnotatedPairs
     *            pairs of gold and predicted labels for the same token
     */
    public EvaluationResult(Collection<String> aIgnoreLabels,
            Stream<AnnotatedTokenPair> aAnnotatedPairs)
    {
        labels = new ArrayList<>();
        ignoreLabels = new HashSet<>();
        if (ignoreLabels != null) {
            ignoreLabels.addAll(aIgnoreLabels);
        }
        numOfLabels = 0;
        // construct confusion matrix
        confusionMatrix = new HashMap<>();
        if (aAnnotatedPairs != null) {
            aAnnotatedPairs.forEach(this::incConfusionMatrix);
            numOfLabels = countLabels();
        }
    }
    
    public EvaluationResult(List<String> aIgnoreLabels, Stream<AnnotatedTokenPair> aAnnotatedPairs,
            int aTrainSetSize, int aTestSetSize)
    {
        this(aIgnoreLabels, aAnnotatedPairs);
        trainingSetSize = aTrainSetSize;
        testSetSize = aTestSetSize;
    }
    
    public EvaluationResult(int aTrainSetSize, int aTestSetSize)
    {
        this(new HashSet<>(), Stream.empty());
        trainingSetSize = aTrainSetSize;
        testSetSize = aTestSetSize;
    }
    
    public EvaluationResult(Stream<AnnotatedTokenPair> aAnnotatedPairs)
    {
        this(new HashSet<>(), aAnnotatedPairs);
    }
    
    private int countLabels()
    {
        if (ignoreLabels.isEmpty()) {
            return labels.size();
        }
        else {
            return Math.toIntExact(labels.stream().filter(l -> !ignoreLabels.contains(l)).count());
        }
    }

    public int getNumOfLabels()
    {
        return numOfLabels;
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

        if (!ignoreLabels.contains(goldLabel)) {
            total += 1;
        }
    }

    private void incCounter(String aGoldLabel, String aPredictedLabel)
    {
        initConfEntries(aGoldLabel, aPredictedLabel);
        
        int count = confusionMatrix.get(aGoldLabel).get(aPredictedLabel) + 1;
        confusionMatrix.get(aGoldLabel).put(aPredictedLabel, count);
    }

    private void initConfEntries(String aGoldLabel, String aPredictedLabel)
    {
        Map<String, Integer> initMap;
        if (!confusionMatrix.containsKey(aGoldLabel)) {
            initMap = new HashMap<>();
            initMap.put(aGoldLabel, 0);
            confusionMatrix.put(aGoldLabel, initMap);

            
            labels.add(aGoldLabel);
            
        }
        if (!confusionMatrix.get(aGoldLabel).containsKey(aPredictedLabel)) {
            confusionMatrix.get(aGoldLabel).put(aPredictedLabel, 0);
            
            if (!labels.contains(aPredictedLabel)) {
                labels.add(aPredictedLabel);
                initMap = new HashMap<>();
                initMap.put(aPredictedLabel, 0);
                confusionMatrix.put(aPredictedLabel, initMap);
            }
        }
    }

    /**
     * Calculate accuracy, ignoring the ignoreLabel class as a gold label.
     * 
     * @return accuracy score
     */
    public double computeAccuracyScore()
    {
        double tp = 0.0;
        for (String label : labels) {
            if (!ignoreLabels.contains(label)) {
                tp += confusionMatrix.get(label).get(label);
            }
        }
        return (total > 0) ? tp / (double) total : 0.0;
    }

    /**
     * Calculate macro-averaged precision score, ignoring the ignoreLabel class as a gold label.
     * 
     * @return precision score
     */
    public double computePrecisionScore()
    {
        // precision divides tp by (tp + fp) i.e num of instances predicted as the goldlabel
        return calcMetricAverage(
            (goldLabel, predictedLabel) -> countLabelCombi(predictedLabel, goldLabel));
    }

    private boolean instanceHasBeenSeen(String label, String predictedLabel)
    {
        return confusionMatrix.containsKey(predictedLabel)
                && confusionMatrix.get(predictedLabel).containsKey(label);
    }

    /**
     * Calculate macro-averaged recall score, ignoring the ignoreLabel class as a gold label.
     * 
     * @return recall score
     */
    public double computeRecallScore()
    {
        // recall divides tp by (tp + fn) i.e num of instances that are the goldlabel
        return calcMetricAverage(
            (goldLabel, predictedLabel) -> countLabelCombi(goldLabel, predictedLabel));
    }

    /**
     * Calculate the metric average for all labels for metrics which divide tp by a specific count
     * @param countFunction the specific count of a certain label combination
     * @return macro-averaged metric score
     */
    private double calcMetricAverage(ToDoubleBiFunction<String, String> countFunction)
    {
        double metric = 0.0;
        if (numOfLabels > 0) {
            for (String label : labels) {
                double tp = confusionMatrix.get(label).get(label);
                double numIsLabel = 0.0;
                for (String predictedLabel : labels) {
                    numIsLabel += countFunction.applyAsDouble(label, predictedLabel);
                }
                metric += calcClassMetric(label, tp, numIsLabel);

            }
            metric = metric / numOfLabels;
        }
        return metric;
    }

    /**
     * Count instances with given gold-label that were predicted as given predicted-label.
     * @return count
     */
    private double countLabelCombi(String aGoldLabel, String aPredictedLabel)
    {
        double numIsLabel = 0.0;
        if (instanceHasBeenSeen(aPredictedLabel, aGoldLabel))
            numIsLabel = confusionMatrix.get(aGoldLabel).get(aPredictedLabel);
        return numIsLabel;
    }

    private double calcClassMetric(String aLabel, double aTp, double aNumIsLabel)
    {
        double classMetric = 0.0;
        if (aNumIsLabel > 0 && !ignoreLabels.contains(aLabel)) {
            classMetric = aTp / aNumIsLabel;
        }
        return classMetric;
    }

    /**
     * Calculate macro-averaged f1-score
     * 
     * @return f1 score
     */
    public double computeF1Score()
    {
        double precision = computePrecisionScore();
        double recall = computeRecallScore();
        return (precision > 0 || recall > 0) ? 2 * precision * recall / (precision + recall) : 0;
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

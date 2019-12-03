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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToDoubleBiFunction;
import java.util.stream.Collector;

/**
 * Provides macro-averaged scores on per-token basis over all labels contained in processed
 * annotated pairs except for those matching the optionally provided ignore-labels as a gold label.
 */
public class EvaluationResult implements Serializable
{
    /**
     * 
     */
    private static final long serialVersionUID = 5842125748342833451L;
    private final int trainingSetSize;
    private final int testSetSize;
    
    /**
     * Rate of this training data compared to all training data
     */
    private final double trainingDataRatio;
    private boolean skippedEvaluation;
    private String errorMsg;

    private final Set<String> ignoreLabels;

    /**
     * Stores number of predicted labels for each gold label
     */
    private ConfusionMatrix confusionMatrix;

    
    public EvaluationResult()
    {
        ignoreLabels = new LinkedHashSet<>();
        confusionMatrix = new ConfusionMatrix();
        trainingSetSize = 0;
        testSetSize = 0;
        trainingDataRatio = 0.0;
    }

    public EvaluationResult(ConfusionMatrix aConfMatrix, int aTrainSetSize, int aTestSetSize,
            double aTrainDataPercentage, Set<String> aIgnoreLabels)
    {
        ignoreLabels = new LinkedHashSet<>();
        ignoreLabels.addAll(aIgnoreLabels);

        confusionMatrix = aConfMatrix;
        trainingSetSize = aTrainSetSize;
        testSetSize = aTestSetSize;
        trainingDataRatio = aTrainDataPercentage;
    }
    
    public EvaluationResult(int aTrainSetSize, int aTestSetSize, double aTrainDataPercentage)
    {
        ignoreLabels = new HashSet<>();
        confusionMatrix = new ConfusionMatrix();
        trainingSetSize = aTrainSetSize;
        testSetSize = aTestSetSize;
        trainingDataRatio = aTrainDataPercentage;
    }

    public int getNumOfLabels()
    {
        Set<String> labels = confusionMatrix.getLabels();
        
        if (ignoreLabels.isEmpty()) {
            return labels.size();
        }
        else {
            return Math.toIntExact(labels.stream().filter(l -> !ignoreLabels.contains(l)).count());
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
        double ignoreLabelAsGold = 0;
        for (String label : confusionMatrix.getLabels()) {
            if (!ignoreLabels.contains(label)) {
                tp += confusionMatrix.getEntryCount(label, label);
            }
            ignoreLabelAsGold += countIgnoreLabelsAsGold(label);
        }
        double total = confusionMatrix.getTotal() - ignoreLabelAsGold;
        return (total > 0) ? tp / total : 0.0;
    }

    /**
     * Count how many times the given label was predicted incorrectly for a label that should be
     * ignored.
     */
    private double countIgnoreLabelsAsGold(String label)
    {
        double ignoreLabelAsGold = 0.0; 
        for (String ignoreLabel : ignoreLabels) {
            ignoreLabelAsGold += confusionMatrix.getEntryCount(label, ignoreLabel);
        }
        return ignoreLabelAsGold;
    }

    /**
     * Calculate macro-averaged precision score, ignoring the ignoreLabel class as a gold label.
     * 
     * @return precision score
     */
    public double computePrecisionScore()
    {
        // precision divides tp by (tp + fp) i.e num of instances predicted as the goldlabel
        return calcMetricAverage((goldLabel, predictedLabel) -> 
                    ignoreLabels.contains(predictedLabel) ? 0.0 :
                        confusionMatrix.getEntryCount(goldLabel, predictedLabel));
    }

    /**
     * Calculate macro-averaged recall score, ignoring the ignoreLabel class as a gold label.
     * 
     * @return recall score
     */
    public double computeRecallScore()
    {
        // recall divides tp by (tp + fn) i.e num of instances that are the goldlabel
        return calcMetricAverage((goldLabel, predictedLabel) -> 
                    ignoreLabels.contains(goldLabel) ? 0.0 : 
                        confusionMatrix.getEntryCount(predictedLabel, goldLabel));
    }

    /**
     * Calculate the metric average for all labels for metrics which divide tp by a specific count
     * @param countFunction the specific count of a certain label combination
     * @return macro-averaged metric score
     */
    private double calcMetricAverage(ToDoubleBiFunction<String, String> countFunction)
    {
        double metric = 0.0;
        int numOfLabels = getNumOfLabels();
        if (numOfLabels > 0) {
            Set<String> labels = confusionMatrix.getLabels();
            for (String label : labels) {
                double tp = 0.0;
                if (!ignoreLabels.contains(label)) {
                    tp = confusionMatrix.getEntryCount(label, label);
                }
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
     * Get the size of the test data used in the recommender evaluation.
     * 
     * @return the test size
     */
    public int getTestSetSize()
    {
        return testSetSize;
    }

    public double getTrainDataRatio()
    {
        return trainingDataRatio;
    }

    public void setEvaluationSkipped(boolean aSkipVal)
    {
        skippedEvaluation = aSkipVal;
    }

    /**
     * Indicates that an evaluation was not performed, either because it was not necessary (e.g.
     * because the recommender is always active or cannot be evaluated) or because it was not
     * possible to perform an evaluation, e.g. because there was not enough data to perform it.
     * 
     * @return true if evaluation was skipped
     */
    public boolean isEvaluationSkipped()
    {
        return skippedEvaluation;
    }
    
    public Optional<String> getErrorMsg()
    {
        return Optional.ofNullable(errorMsg);
    }

    public void setErrorMsg(String aErrorMsg)
    {
        errorMsg = aErrorMsg;
    }

    public void setConfusionMatrix(ConfusionMatrix aConfusionMatrix)
    {
        confusionMatrix = aConfusionMatrix;
    }

    public static EvaluationResultCollector collector()
    {
        return new EvaluationResultCollector();
    }
    
    public static EvaluationResult skipped()
    {
        EvaluationResult result = new EvaluationResult();
        result.setEvaluationSkipped(true);
        return result;
    }

    public static EvaluationResultCollector collector(int aTrainSetSize, int aTestSetSize,
            double aTrainDataPercentage, String... aIgnoreLabels)
    {
        return new EvaluationResultCollector(aTrainSetSize, aTestSetSize, aTrainDataPercentage,
                aIgnoreLabels);
    }
    
    public static class EvaluationResultCollector
        implements Collector<LabelPair, ConfusionMatrix, EvaluationResult>
    {
        private final Set<String> ignoreLabels;
        private final int testSize;
        private final int trainSize;
        private final double trainDataRatio;

        public EvaluationResultCollector(int aTrainSetSize, int aTestSetSize,
                double aTrainDataPercentage, String... aIgnoreLabels)
        {
            ignoreLabels = new HashSet<>();
            testSize = aTestSetSize;
            trainSize = aTrainSetSize;
            trainDataRatio = aTrainDataPercentage;
            Collections.addAll(ignoreLabels, aIgnoreLabels);
        }

        public EvaluationResultCollector()
        {
            ignoreLabels = new HashSet<>();
            testSize = 0;
            trainSize = 0;
            trainDataRatio = 0;
        }

        @Override
        public Supplier<ConfusionMatrix> supplier()
        {
            return ConfusionMatrix::new;
        }

        @Override
        public BiConsumer<ConfusionMatrix, LabelPair> accumulator()
        {
            return (confMatrix, pair) -> confMatrix.incrementCounts(pair.getPredictedLabel(),
                    pair.getGoldLabel());
        }

        @Override
        public BinaryOperator<ConfusionMatrix> combiner()
        {
            return (matrix1, matrix2) -> {
                matrix1.addMatrix(matrix2);
                return matrix1;
            };
        }

        @Override
        public Function<ConfusionMatrix, EvaluationResult> finisher()
        {
            return confMatrix -> new EvaluationResult(confMatrix, trainSize,
                    testSize, trainDataRatio, ignoreLabels);
        }

        @Override
        public Set<Collector.Characteristics> characteristics()
        {
            return Collections.emptySet();
        }
    }
}

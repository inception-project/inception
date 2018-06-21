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
package de.tudarmstadt.ukp.inception.recommendation.imls.core.evaluation;

import static de.tudarmstadt.ukp.inception.recommendation.imls.util.ListUtil.flattenList;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.recommendation.api.Classifier;
import de.tudarmstadt.ukp.inception.recommendation.api.Trainer;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.api.model.TokenObject;
import de.tudarmstadt.ukp.inception.recommendation.imls.conf.EvaluationConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.ExtendedResult;
import de.tudarmstadt.ukp.inception.recommendation.imls.util.DebugSupport;
import de.tudarmstadt.ukp.inception.recommendation.imls.util.MathUtil;

/**
 * The IncrementalTrainingEvaluationSuite stores the state of the incremental evaluation. This means
 * it symbolizes an iteration in the incremental evaluation by containing the total data and the
 * known data. Furthermore, it provides default helper methods to calculate the training data batch
 * for an iteration increment.
 * 
 *
 *
 */
public class IncrementalTrainingEvaluationSuite
{
    private Logger log = LoggerFactory.getLogger(getClass());
    
    private Trainer<?> trainer;
    private Classifier<?> classifier;

    private List<List<AnnotationObject>> totalData = new LinkedList<>();
    
    /**
     * The data the incremental learner has seen until now.
     */
    private List<List<AnnotationObject>> knownData = new LinkedList<>();
    
    /**
     * The training data used for the current increment.
     */
    private List<List<AnnotationObject>> trainingData = new LinkedList<>();

    private int currentIteration = 0;

    private long lastTrainingDuration = 0;

    private int currentBatchSize = 1;
    private int previousBatchSize = 0;
    private int batchIncrementSize = 1;
    
    private EvaluationConfiguration conf;
    
    public IncrementalTrainingEvaluationSuite(EvaluationConfiguration suiteConf,
            List<List<AnnotationObject>> totalData, Trainer<?> trainer, Classifier<?> classifier,
            boolean trainOnCompleteSentence)
    {
        if (suiteConf == null) {
            throw new IllegalArgumentException(
                    "Cannot instantiate a suite without a configuration.");
        }
        if (trainer == null) {
            throw new IllegalArgumentException("Cannot instantiate a suite without a trainer.");
        }
        if (classifier == null) {
            throw new IllegalArgumentException("Cannot instantiate a suite without a classifier.");
        }

        this.trainer = trainer;
        this.classifier = classifier;
        this.conf = suiteConf;

        int trainingSetStartSize = trainer.getClassifierConfiguration().getTrainingSetStartSize();
        if (trainingSetStartSize > 1) {
            setMinimumStartTrainingSetSize(trainingSetStartSize);
        }

        if (trainOnCompleteSentence) {
            this.totalData = trimData(totalData);
        }
        else {
            this.totalData = totalData;
        }
    }

    /**
     * True, if there is still data to use for training, and if the size of the trainingsData does
     * not exceed the trainingSizeLimit.
     * 
     * @return true, if another iteration can be added.
     */
    public boolean hasNextIteration()
    {
        return trainingData.size() < totalData.size() 
                && knownData.size() < totalData.size()
                && (knownData.size() < conf.getTrainingSetSizeLimit() 
                || conf.getTrainingSetSizeLimit() <= 0);
    }
    
    @FunctionalInterface
    static public interface DataSelector
    {
        List<List<AnnotationObject>> apply(EvaluationConfiguration a,
                List<List<AnnotationObject>> b);
    }
    
    /**
     * Adds a batch of data to the training data and returns the current percentage of coverage
     * 
     * @param conf
     *            The evaluation configuration
     * @param batchCreationFunction
     *            The function used to create the training batch.
     * @param trainingSetCreationFunction
     *            The function which specifies how the training batch should be used for the
     *            training data. In general, for incremental algorithms it overrides the training
     *            data, for non incremental algorithms it add the batch to the training data.
     * @param knownDataCreationFunction
     *            The function which specifies how the training batch should be applied to the
     *            complete known data set. In general it is added to the known data set.
     * @return the percentage of dataset coverage
     */
    public double addIncrement(
            EvaluationConfiguration conf,
            DataSelector batchCreationFunction,
            DataSelector trainingSetCreationFunction,
            DataSelector knownDataCreationFunction)
    {
        currentIteration++;

        List<List<AnnotationObject>> batch = batchCreationFunction.apply(conf, totalData);
        trainingData = trainingSetCreationFunction.apply(conf, batch);
        knownData = knownDataCreationFunction.apply(conf, batch);
        
        log.trace("New increment data size: {}.", trainingData.size());
        log.trace("Total known data size: {}.", knownData.size());

        return ((double) knownData.size() / (double) totalData.size()) * 100;
    }

    /**
     * Calls the train function of the trainer service with the trainingData as parameter.
     */
    public void train()
    {
        long startTraining = System.currentTimeMillis();
        Object model = trainer.train(trainingData);
        this.lastTrainingDuration = System.currentTimeMillis() - startTraining;

        classifier.setModel(model);
    }

    /**
     * Evaluates the currently known data as test data.
     * For each token, the annotation with the highest confidence is chosen.
     * 
     * @return Results (F-Score, Precision, Recall) of the test.
     */
    public ExtendedResult evaluateKnownData()
    {

        long classifyingStartTime = System.currentTimeMillis();
        List<List<AnnotationObject>> generatedData = flattenInner(
                classifier.predictSentences(toTokenObjects(knownData)));
        long classifyingEndTime = System.currentTimeMillis();

        if (conf.isDebug()) {
            List<AnnotationObject> flatExpectedData = flattenList(knownData);
            List<AnnotationObject> flatGeneratedData = flattenList(generatedData);

            File fOut = new File("debugKnownOut_" + currentIteration + ".csv");
            try (FileWriter fw = new FileWriter(fOut)) {
                DebugSupport.printComparisonAsExcel(fw, flatExpectedData, flatGeneratedData);
            }
            catch (IOException e) {
                log.warn("Could not write debug file {}.",fOut.getAbsolutePath(), e);
            }
        }

        ExtendedResult result = new ExtendedResult(knownData, generatedData);

        result.setTrainingDuration(lastTrainingDuration);
        result.setClassifyingDuration(classifyingEndTime - classifyingStartTime);
        result.setTestSetSize(knownData.size());
        result.setTrainingSetSize(knownData.size());
        result.setIterationNumber(currentIteration);

        return result;
    }

    private List<List<TokenObject>> toTokenObjects(List<List<AnnotationObject>> sentences) {
        return sentences.stream()
                .map(l -> l.stream().map(ao -> ao.getTokenObject()).collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    /**
     * Evaluates the next unknown data as test data.
     * 
     * @return Results (F-Score, Precision, Recall) of the test.
     */
    public ExtendedResult evaluateUnknownData()
    {
        if (conf.isUseHoldout()) {
            return evaluateUnknownData(holdoutEvaluationDataSelector());
        }
        else {
            return evaluateUnknownData(fixedSizeEvaluationDataSelector());
        }
    }

    /**
     * Evaluates predicted against expected data and produces an ExtendedResult object.
     * 
     * @param expectedData
     *            The expected unknown data.
     * @return Results (F-Score, Precision, Recall) of the test.
     */
    public ExtendedResult evaluateUnknownData(List<List<AnnotationObject>> expectedData)
    {
        if (expectedData == null) {
            return null;
        }

        long classifyingStartTime = System.currentTimeMillis();
        
        List<List<AnnotationObject>> predictedData = 
                flattenInner(classifier.predictSentences(toTokenObjects(expectedData)));
            
        long classifyingEndTime = System.currentTimeMillis();

        long trainingSetAnnotationCount = trainingData.stream().flatMap(List::stream).count();
        
        ExtendedResult result = new ExtendedResult(expectedData, predictedData);
        result.setTrainingDuration(lastTrainingDuration);
        result.setClassifyingDuration(classifyingEndTime - classifyingStartTime);
        result.setTestSetSize(expectedData.size());
        result.setTrainingSetSize(trainingData.size());
        result.setTrainingSetAnnotationCount(trainingSetAnnotationCount);
        result.setIterationNumber(currentIteration);
        result.setSplit(conf.getSplitTestDataPercentage());
        result.setShuffleTrainingSet(conf.isShuffleTrainingSet());

        return result;
    }
    
    /**
     * Flattens data by getting rid of the innermost layer
     * In case of predicted data, the recommendation with the highest confidence will be chosen 
     */
    public List<List<AnnotationObject>> flattenInner(List<List<List<AnnotationObject>>> data)
    {
        List<List<AnnotationObject>> flattenedData = new ArrayList<>();

        for (List<List<AnnotationObject>> s : data) {
            List<AnnotationObject> sentence = new ArrayList<>();
            for (int i = 0; i < s.size(); i++) {
                sentence.add(s.get(i).get(s.get(i).size() - 1));
            }
            flattenedData.add(sentence);
        }

        return flattenedData;
    }
    
    public List<List<AnnotationObject>> trainingDataSelector(List<List<AnnotationObject>> totalData)
    {
        if (conf.isFibonacciIncrementStrategy()) {
            return fibonacciTrainingDataSelector(totalData);
        }
        else {
            return equidistantTrainingDataSelector(totalData);
        }
    }

    /**
     * The default used method to create the next batch. It uses the Fibonacci sequence to increment
     * the training set size;
     * 
     * @param totalData
     *            The total data used in the incremental evaluation.
     * @return The next batch of training data.
     */
    public List<List<AnnotationObject>> fibonacciTrainingDataSelector(
            List<List<AnnotationObject>> totalData)
    {
        int batchSize = getFibonnaciIncrementedBatchSize();
        int start = knownData.size();
        List<List<AnnotationObject>> batch = new LinkedList<>();

        for (int i = 0; i < batchSize; i++) {
            int index = start + i;

            if (!hasNextIteration() || index >= totalData.size()
                    || (index >= conf.getTrainingSetSizeLimit()
                            && conf.getTrainingSetSizeLimit() > 0)) {
                break;
            }

            batch.add(totalData.get(index));
        }

        return batch;
    }
    
    /**
     * Creates the next batch by incrementing previous batch size by a constant
     *  
     * @param totalData
     *            The total data used in the incremental evaluation.
     * @return The next batch of training data.
     */
    public List<List<AnnotationObject>> equidistantTrainingDataSelector(
            List<List<AnnotationObject>> totalData)
    {
        int start = knownData.size();
        List<List<AnnotationObject>> batch = new LinkedList<>();

        for (int i = 0; i < currentBatchSize; i++) {
            int index = start + i;
            
            if (!hasNextIteration() 
                || index >= totalData.size()
                || (index >= conf.getTrainingSetSizeLimit() 
                    && conf.getTrainingSetSizeLimit() > 0)) {
                break;
            }
            
            batch.add(totalData.get(index));
        }
        currentBatchSize += batchIncrementSize;

        return batch;
    }

    /**
     * The default function to create the training data for the current iteration;
     * 
     * @param batch
     *            of data for this iteration
     * @return trainingData for this iteration
     */
    public List<List<AnnotationObject>> allDataSelector(
            List<List<AnnotationObject>> batch)
    {
        return batch;
    }

    /**
     * The default function to create the known data for the current iteration;
     * 
     * @param batch
     *            of data for this iteration
     * @return knownData for this iteration
     */
    public List<List<AnnotationObject>> aggregatingDataSelector(List<List<AnnotationObject>> batch)
    {
        knownData.addAll(batch);
        return knownData;
    }

    /**
     * The default function to create the unknown data for evaluation.
     * 
     * @return The next x unknown sentences.
     */
    public List<List<AnnotationObject>> fixedSizeEvaluationDataSelector()
    {
        List<List<AnnotationObject>> result = new ArrayList<>();

        int start = this.knownData.size();
        
        for (int i = start; i < start + conf.getTestIncrementSize(); i++) {
            if (i >= totalData.size()) {
                break;
            }
            result.add(totalData.get(i));
        }
        return result;
    }
    
    public List<List<AnnotationObject>> holdoutEvaluationDataSelector()
    {
        List<List<AnnotationObject>> result = new ArrayList<>();

        int offsetTestData = (int) Math
                .ceil(((double) totalData.size()) * conf.getSplitTestDataPercentage());

        for (int i = 0; i < offsetTestData; i++) {
            if (i >= totalData.size()) {
                break;
            }

            result.add(totalData.get(i));
        }
        return result;
    }

    /**
     * Increments the batchSize by the fibonacci sequence and returns the incremented value.
     * 
     * @return The next number in the fibonacci sequence.
     */
    public int getFibonnaciIncrementedBatchSize()
    {
        int nextBatchSize = currentBatchSize + previousBatchSize;
        previousBatchSize = currentBatchSize;
        currentBatchSize = nextBatchSize;
        return currentBatchSize;
    }
    
    /**
     * Sets the minimal training set size to reach before training can begin.
     * 
     * @param minTrainingSetSize
     *            The minimum set size of training data necessary to start this evaluation.
     */
    public void setMinimumStartTrainingSetSize(int minTrainingSetSize)
    {
        if (minTrainingSetSize > 1) {
            int steps = MathUtil.getFibonacciSteps(minTrainingSetSize);
            // set the current training set size to the previous,
            // and the previous to one before, so the next increment will be > minTrainingSetSize
            currentBatchSize = MathUtil.getFibonacci(steps - 1);
            previousBatchSize = MathUtil.getFibonacci(steps - 2);
        }
    }

    /**
     * If the sentences have to be completely annotated for training, it removes all sentences which
     * are only partially annotated.
     * 
     * @param data
     *            All sentences containing at least one annotation != null.
     * @return All sentences completely annotated, if necessary, or data otherwise.
     */
    private List<List<AnnotationObject>> trimData(List<List<AnnotationObject>> data)
    {
        List<List<AnnotationObject>> result = new LinkedList<>();

        for (List<AnnotationObject> sentence : data) {
            boolean isComplete = true;

            for (AnnotationObject ao : sentence) {
                if (ao.getLabel() == null) {
                    isComplete = false;
                    break;
                }
            }

            if (isComplete) {
                result.add(sentence);
            }
        }

        return result;
    }

    public List<List<AnnotationObject>> getTotalData () {
        return totalData;
    }
}

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
package de.tudarmstadt.ukp.inception.recommendation.imls.dl4j;

import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.GravesLSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.junit.Ignore;
import org.junit.Test;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.lossfunctions.LossFunctions;

public class TestDL4J
{
    @Ignore("Fails with NPE")
    @SuppressWarnings({ "unused", "null" })
    @Test
    public void test()
    {
        int batchSize = 64; // Number of examples in each minibatch
        int vectorSize = 300; // Size of the word vectors. 300 in the Google News model
        int nEpochs = 1; // Number of epochs (full passes of training data) to train on
        int truncateReviewsToLength = 256; // Truncate reviews with length (# words) greater than
                                           // this

        // Set up network configuration
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().updater(Updater.ADAM)
                .adamMeanDecay(0.9).adamVarDecay(0.999).regularization(true).l2(1e-5)
                .weightInit(WeightInit.XAVIER)
                .gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue)
                .gradientNormalizationThreshold(1.0).learningRate(2e-2).list()
                .layer(0,
                        new GravesLSTM.Builder().nIn(vectorSize).nOut(256)
                                .activation(Activation.TANH).build())
                .layer(1, new RnnOutputLayer.Builder().activation(Activation.SOFTMAX)
                        .lossFunction(LossFunctions.LossFunction.MCXENT).nIn(256).nOut(2).build())
                .pretrain(false).backprop(true).build();

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();
        net.setListeners(new ScoreIterationListener(1));

        DataSet ds = new DataSet();

        DataSetIterator train = ds.iterateWithMiniBatches(); // TODO
        DataSetIterator test = null; // TODO

        System.out.println("Starting training");
        for (int i = 0; i < nEpochs; i++) {
            net.fit(train);
            System.out.println("Epoch " + i + " complete. Starting evaluation:");

            // Run evaluation. This is on 25k reviews, so can take some time
            Evaluation evaluation = new Evaluation();

            while (test.hasNext()) {
                DataSet t = test.next();
                INDArray features = t.getFeatureMatrix();
                INDArray lables = t.getLabels();
                INDArray inMask = t.getFeaturesMaskArray();
                INDArray outMask = t.getLabelsMaskArray();
                INDArray predicted = net.output(features, false, inMask, outMask);

                evaluation.evalTimeSeries(lables, predicted, outMask);
            }
            test.reset();

            System.out.println(evaluation.stats());
        }

        // After training: load a single example and generate predictions
        // File firstPositiveReviewFile = new File(FilenameUtils.concat(DATA_PATH,
        // "aclImdb/test/pos/0_10.txt"));
        // String firstPositiveReview = FileUtils.readFileToString(firstPositiveReviewFile);

        // INDArray features = test.loadFeaturesFromString(firstPositiveReview,
        // truncateReviewsToLength);
        // INDArray networkOutput = net.output(features);
        // int timeSeriesLength = networkOutput.size(2);
        // INDArray probabilitiesAtLastWord = networkOutput.get(NDArrayIndex.point(0),
        // NDArrayIndex.all(), NDArrayIndex.point(timeSeriesLength - 1));

        System.out.println("\n\n-------------------------------");
        // System.out.println("First positive review: \n" + firstPositiveReview);
        System.out.println("\n\nProbabilities at last time step:");
        // System.out.println("p(positive): " + probabilitiesAtLastWord.getDouble(0));
        // System.out.println("p(negative): " + probabilitiesAtLastWord.getDouble(1));

        System.out.println("----- Example complete -----");
    }
}

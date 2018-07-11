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
package de.tudarmstadt.ukp.inception.recommendation.imls.dl4j.pos;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.deeplearning4j.datasets.iterator.impl.ListDataSetIterator;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.GravesLSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.recommendation.api.ClassifierConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.api.Trainer;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;

/**
 * The implementation of a trainer for a POS-Tagger using the DL4J library and a multi layer neural
 * network.
 * 
 *
 *
 */
public class DL4JPosTrainer
    extends Trainer<DL4JConfigurationParameters>
{
    private Logger log = LoggerFactory.getLogger(getClass());

    private MultiLayerNetwork net = null;
    private Vectorizer vectorizer = new Vectorizer();

    public DL4JPosTrainer(ClassifierConfiguration<DL4JConfigurationParameters> conf)
    {
        super(conf);
    }

    private void createConfiguredNetwork(DL4JConfigurationParameters params)
    {
        // Set up network configuration
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .optimizationAlgo(params.getOptimizationAlgorithm())
                .iterations(params.getIterations()).updater(params.getUpdater())
                .regularization(params.isRegularization()).l2(params.getL2())
                .weightInit(params.getWeightInit())
                .gradientNormalization(params.getGradientNormalization())
                .gradientNormalizationThreshold(params.getGradientNormalizationThreshold())
                .learningRate(params.getLearningRate()).list()
                .layer(0,
                        new GravesLSTM.Builder().nIn(params.getWordVectors().dimensions()).nOut(200)
                                .activation(params.getActivationL0()).build())
                .layer(1,
                        new RnnOutputLayer.Builder().activation(params.getActivationL1())
                                .lossFunction(params.getLossFunction()).nIn(200)
                                .nOut(params.getMaxTagsetSize()).build())
                .pretrain(false).backprop(true).build();

        net = new MultiLayerNetwork(conf);
        net.init();
        net.setListeners(new ScoreIterationListener(1));
    }

    @Override
    public Object train(List<List<AnnotationObject>> trainingDataIncrement)
    {
        if (net == null) {
            reconfigure();
        }

        DL4JConfigurationParameters params = getClassifierConfiguration().getParams();
        List<DataSet> trainingData = new LinkedList<>();

        for (List<AnnotationObject> sentence : trainingDataIncrement) {
            try {
                trainingData
                        .add(vectorizer.vectorize(Arrays.asList(sentence), params.getWordVectors(),
                                params.getTruncateLength(), params.getMaxTagsetSize(), true));
            }
            catch (Exception e) {
                log.error("Cannot vectorize sentence.", e);
                return null;
            }
        }

        DataSetIterator trainingIterator = new ListDataSetIterator(trainingData);
        for (int i = 0; i < params.getnEpochs(); i++) {
            net.fit(trainingIterator);
            trainingIterator.reset();
            log.info("Completed epoch #{}.", i);
        }

        saveModel();
        return getClassifierConfiguration().getModelFile().getAbsolutePath();
    }

    @Override
    public boolean saveModel()
    {
        File fOut = getClassifierConfiguration().getModelFile();
        try {
            ModelSerializer.writeModel(net, fOut, true);
        }
        catch (IOException e) {
            log.error("Cannot save model as file.", e);
            return false;
        }

        DL4JConfigurationParameters params = getClassifierConfiguration().getParams();

        fOut = params.getTagsetFile();
        try (ArchiveOutputStream archive = new ZipArchiveOutputStream(new FileOutputStream(fOut))) {
            ZipArchiveEntry entry = new ZipArchiveEntry(params.getTagsetEntryName());
            archive.putArchiveEntry(entry);
            for (String tag : vectorizer.getTagset()) {
                archive.write(tag.getBytes(StandardCharsets.UTF_8));
                archive.write("\n".getBytes(StandardCharsets.UTF_8));
            }
            archive.closeArchiveEntry();
        }
        catch (IOException e) {
            log.error("Cannot save tagset as file.", e);
            return false;
        }

        return true;
    }

    @Override
    public Object loadModel()
    {
        File fIn = getClassifierConfiguration().getModelFile();
        try {
            net = ModelSerializer.restoreMultiLayerNetwork(fIn);
        }
        catch (IOException e) {
            log.error("Cannot load model from file.", e);
            return null;
        }
        return fIn.getAbsolutePath();
    }

    @Override
    public void reconfigure()
    {
        createConfiguredNetwork(getClassifierConfiguration().getParams());
    }

}

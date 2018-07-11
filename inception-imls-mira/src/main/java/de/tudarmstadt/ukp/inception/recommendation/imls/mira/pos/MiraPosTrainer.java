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
package de.tudarmstadt.ukp.inception.recommendation.imls.mira.pos;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.recommendation.api.ClassifierConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.api.Trainer;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;

/**
 * The implementation of a trainer for a POS-Tagger using the Margin Infused Relaxed Algorithm
 * (MIRA).
 * 
 *
 *
 */
public class MiraPosTrainer
    extends Trainer<MiraConfigurationParameters>
{
    private Logger log = LoggerFactory.getLogger(getClass());
    
    private static final Logger logger = LoggerFactory.getLogger(MiraPosTrainer.class);
    private IncrementalMira mira;

    private int iterations = 0;

    public MiraPosTrainer(ClassifierConfiguration<MiraConfigurationParameters> conf)
    {
        super(conf);
    }

    private void configureTrainer()
    {
        ClassifierConfiguration<MiraConfigurationParameters> conf = getClassifierConfiguration();
        
        if (conf == null) {
            logger.error("Cannot configure Mira Trainer. No configuration defined.");
            return;
        }

        prepareTemplates(conf);

        MiraConfigurationParameters params = conf.getParams();

        // configure training
        iterations = params.getIterations();

        mira = new IncrementalMira();
        // configure the classifier itself
        try {
            mira.loadTemplates(params.getTemplate());
        }
        catch (IOException e) {
            log.error("Could not load mira template.", e);
        }
        mira.setClip(params.getClip());
        mira.setMaxPosteriors(params.isMaxPosteriors());
        mira.setBeamSize(params.getBeamSize());
    }

    private void prepareTemplates(ClassifierConfiguration<MiraConfigurationParameters> conf)
    {
        if (conf == null) {
            return;
        }
        MiraConfigurationParameters params = conf.getParams();
        if (params == null) {
            return;
        }

        File templateFile = new File(conf.getModelFile().getParentFile(), "pos-simple.template");

        if (params.getTemplate() == null || params.getTemplate().isEmpty()) {

            String templateStr = BaseConfiguration.getDefaultTemplate();

            templateFile.getParentFile().mkdirs();
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(templateFile))) {
                bw.write(templateStr);
                bw.newLine();
                bw.close();

                params.setTemplate(templateFile.getAbsolutePath());
            }
            catch (IOException e) {
                logger.error("Cannot create simple pos template", e);
            }
        }
    }

    @Override
    public Object train(List<List<AnnotationObject>> trainingDataIncrement)
    {
        if (mira == null) {
            reconfigure();
        }

        try {
            int numExamples = mira.count(trainingDataIncrement, 2);
            mira.initModel(false);

            for (int i = 0; i < iterations; i++) {
                mira.train(trainingDataIncrement, iterations, numExamples, i);
                mira.averageWeights(iterations * numExamples);
            }

            if (saveModel()) {
                return getClassifierConfiguration().getModelFile().getAbsolutePath();
            }
        }
        catch (IOException e) {
            log.error("Cannot train model.", e);
        }
        return null;
    }

    @Override
    public boolean saveModel()
    {
        File fOut = getClassifierConfiguration().getModelFile();
        if (fOut == null) {
            return false;
        }

        try {
            mira.saveModel(fOut.getAbsolutePath());
        }
        catch (IOException e) {
            log.error("Cannot save model to file.", e);
            return false;
        }

        return true;
    }

    @Override
    public Object loadModel()
    {
        File fIn = getClassifierConfiguration().getModelFile();
        if (fIn == null) {
            return null;
        }

        try {
            mira.loadModel(fIn.getAbsolutePath());
        }
        catch (ClassNotFoundException | IOException e) {
            log.error("Cannot load model from file.", e);
            return null;
        }

        return fIn.getAbsolutePath();
    }

    @Override
    public void reconfigure()
    {
        configureTrainer();
    }

}

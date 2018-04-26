/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.inception.recommendation.util;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassificationTool;
import de.tudarmstadt.ukp.inception.recommendation.api.Classifier;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassifierConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.api.Trainer;
import de.tudarmstadt.ukp.inception.recommendation.imls.conf.EvaluationConfiguration;

/**
 * Little helper class to customize the configurations for the selection evaluation task.
 */
public class EvaluationHelper
{
    private static final Logger logger = LoggerFactory.getLogger(EvaluationHelper.class);

    public static void customizeConfiguration(ClassificationTool<?> ct, String modelNameSuffix,
            DocumentService docService, Project project)
    {
        final String modelFileName = ct.getClass().getSimpleName() + modelNameSuffix;
        final File modelFile = new File(RepositoryUtil.getModelDir(docService, project),
                modelFileName);
        ClassifierConfiguration<?> conf;
        Trainer<?> trainer = ct.getTrainer();
        Classifier<?> classifier = ct.getClassifier();

        if (trainer == null || classifier == null) {
            logger.error(
                    "Cannot customize configuration for selection evaluation. Trainer or Classifier are null.");
            return;
        }

        conf = trainer.getClassifierConfiguration();
        if (conf == null) {
            logger.error("No configuration for Trainer was set. Setting a default configuration.");
            conf = new ClassifierConfiguration<>();
        }
        conf.setModelFile(modelFile);

        conf = classifier.getClassifierConfiguration();
        if (conf == null) {
            logger.error(
                    "No configuration for Classifier was set. Setting a default configuration.");
            conf = new ClassifierConfiguration<>();
        }
        conf.setModelFile(modelFile);
    }

    public static EvaluationConfiguration getTrainingSuiteConfiguration(String resultFolderName,
            DocumentService docService, Project project) {
        return getTrainingSuiteConfiguration(resultFolderName, docService, project, false, 0, 0.4, 
                false, "fibonacciIncrementStrategy", 1, 100);
    }

    public static EvaluationConfiguration getTrainingSuiteConfiguration(String resultFolderName,
            DocumentService docService, Project project, boolean isShuffleTrainingSet, 
            int trainingSetSizeLimit, double splitTestDataPercentage, boolean useHoldout, 
            String incrementStrategy, int trainingIncrementSize, int testIncrementSize)
    {
        EvaluationConfiguration suiteConf;
        suiteConf = new EvaluationConfiguration();
        suiteConf.setDebug(false);
        suiteConf.setResultFolder(
                new File(RepositoryUtil.getResultDir(docService, project), "/" + resultFolderName)
                        .getAbsolutePath());
        suiteConf.setShuffleTrainingSet(isShuffleTrainingSet);
        suiteConf.setTrainingSetSizeLimit(trainingSetSizeLimit);
        suiteConf.setSplitTestDataPercentage(splitTestDataPercentage);
        suiteConf.setUseHoldout(useHoldout);
        suiteConf.setIncrementStrategy(incrementStrategy);
        suiteConf.setTrainingIncrementSize(trainingIncrementSize);
        suiteConf.setTestIncrementSize(testIncrementSize);
        return suiteConf;
    }
}

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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.recommendation.api.AnnotationObjectLoader;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassificationTool;
import de.tudarmstadt.ukp.inception.recommendation.api.Classifier;
import de.tudarmstadt.ukp.inception.recommendation.api.Trainer;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.imls.conf.EvaluationConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.ExtendedResult;

/**
 * The EvaluationService class performs a single evaluation run using a percentage of the given data
 * as validation set.
 * 
 *
 *
 */
public class EvaluationService
{
    private Logger log = LoggerFactory.getLogger(getClass());
    
    protected String id;
    protected Trainer<?> trainer;
    protected Classifier<?> classifier;
    protected AnnotationObjectLoader loader;
    protected boolean trainOnCompleteSentence;
    protected EvaluationConfiguration conf;

    public EvaluationService(ClassificationTool<?> classificationTool, EvaluationConfiguration conf)
    {
        if (classificationTool == null) {
            throw new IllegalArgumentException("ClassificationTool cannot be null.");
        }
        if (classificationTool.getName() == null) {
            throw new IllegalArgumentException(
                    "ClassificationTool Name for evaluation service cannot be null.");
        }
        if (classificationTool.getTrainer() == null) {
            throw new IllegalArgumentException(
                    "ClassificationTool Trainer for evaluation service cannot be null.");
        }
        if (classificationTool.getClassifier() == null) {
            throw new IllegalArgumentException(
                    "ClassificationTool Classifier for evaluation service cannot be null.");
        }
        if (conf == null) {
            throw new IllegalArgumentException(
                    "Training suite configuration for evaluation service cannot be null.");
        }
        if (conf.getSplitTestDataPercentage() < 0 || conf.getSplitTestDataPercentage() > 1) {
            throw new IllegalArgumentException(
                    "Split test data percentage has to be between 0.0 and 1.0");
        }

        this.id = classificationTool.getName();
        this.trainer = classificationTool.getTrainer();
        this.classifier = classificationTool.getClassifier();
        this.loader = classificationTool.getLoader();
        this.trainOnCompleteSentence = classificationTool.isTrainOnCompleteSentences();
        this.conf = conf;
    }

    
    private List<List<AnnotationObject>> createTrainingData(List<List<AnnotationObject>> data)
    {
        List<List<AnnotationObject>> trainingData = new LinkedList<>();

        int offsetTestData = (int) Math
                .ceil(((double) data.size()) * conf.getSplitTestDataPercentage());

        for (int i = offsetTestData; i < data.size(); i++) {
            trainingData.add(data.get(i));
        }

        return trainingData;
    }

    private List<List<AnnotationObject>> createTestData(List<List<AnnotationObject>> data)
    {
        List<List<AnnotationObject>> testData = new LinkedList<>();

        int offsetTestData = (int) Math
                .ceil(((double) data.size()) * conf.getSplitTestDataPercentage());

        for (int i = 0; i < offsetTestData; i++) {
            testData.add(data.get(i));
        }

        return testData;
    }

    /**
     * Loads the annotation data from the Cas and calls the evaluate method using this data.
     * @param jCas The JCas object containing the annotation data used for the evaluation run.
     * @return The results of this evaluation run.
     */
    public ExtendedResult evaluate(JCas jCas)
    {
        if (jCas == null) {
            return null;
        }
        if (loader == null) {
            return null;
        }

        if (conf.getFeature() != null) {
            return evaluate(loader.loadAnnotationObjectsForEvaluation(jCas));
        }
        else {
            return evaluate(loader.loadAnnotationObjectsForEvaluation(jCas));
        }
    }

    /**
     * Creates an InrementalTrainingEvaluationSuite, but performs only one iteration, thus a single
     * evaluation run. To calculate the validation and training data sets it uses the percentage
     * specified in the EvaluationConfiguration. It then trains the classifier on the training set
     * and evaluates calling the evaluateUnknownData using the validation set.
     * 
     * @param data
     *            The complete annotation data, which gets splitted into test and validation data
     *            set.
     * @return The result of the evaluation run.
     */
    public ExtendedResult evaluate(List<List<AnnotationObject>> data)
    {
        if (data == null) {
            return null;
        }

        if (conf.isShuffleTrainingSet()) {
            Collections.shuffle(data);
        }

        trainer.reconfigure();

        IncrementalTrainingEvaluationSuite suite = new IncrementalTrainingEvaluationSuite(conf,
                data, trainer, classifier, trainOnCompleteSentence);

        log.info("Starting evaluation.");

        log.info("Adding training data.");
        suite.addIncrement(conf, 
            (conf, d) -> { return createTrainingData(d); }, 
            (conf, d) -> { return suite.allDataSelector(d); },
            (conf, d) -> { return suite.aggregatingDataSelector(d); });

        log.info("Training classifier " + classifier.getClass().getSimpleName() + ".");

        ExtendedResult result = null;

        if (suite.getTotalData().size() > 1) {
            suite.train();

            log.info("Predict against unknown data.");
            result = suite.evaluateUnknownData(createTestData(data));

            log.info("Evaluation finished.");

        } else {
            log.info("Skipping increment - not enough data");
        }

        return result;
    }

    public String getId()
    {
        return id;
    }

    public Trainer<?> getTrainer()
    {
        return trainer;
    }

    public Classifier<?> getClassifier()
    {
        return classifier;
    }
}

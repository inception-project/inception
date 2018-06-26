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

import de.tudarmstadt.ukp.inception.recommendation.api.ClassificationTool;
import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;
import de.tudarmstadt.ukp.inception.recommendation.imls.conf.EvaluationConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.ExtendedResult;

/**
 * The IncrementalEvaluationService performs an incremental evaluation run.
 * 
 *
 *
 */
public class IncrementalEvaluationService
    extends EvaluationService
{
    private Logger log = LoggerFactory.getLogger(getClass());

    public IncrementalEvaluationService(ClassificationTool<?> classificationTool,
            EvaluationConfiguration conf)
    {
        super(classificationTool, conf);
    }

    /**
     * Loads the annotation data from the CAS and calls the evaluateIncremental method using that
     * data.
     * 
     * @param jCas
     *            The JCas from which to load the annotation data.
     * @return The result of the complete incremental evaluation run. It consists out of the
     *         evaluation results for the known and unknown data for every iteration.
     */
    public EvaluationResult evaluateIncremental(JCas jCas)
    {
        if (jCas == null) {
            return null;
        }
        if (loader != null) {
            return null;
        }
        
        if (conf.getFeature() != null) {
            return evaluateIncremental(loader.loadAnnotationObjectsForEvaluation(jCas));
        }
        else {
            return evaluateIncremental(loader.loadAnnotationObjectsForEvaluation(jCas));
        }
    }

    /**
     * Runs an incremental evaluation. That means the methods runs a single evaluation run as long
     * as a subset of the given data can be taken. The subset is used to train the classifier model
     * and added to the already known data. Then predictions are performed regarding the known and
     * the unknown (data not yet trained on) data. The predictions are compared against the
     * known/unknown data and the results are stored in the EvaluationResult object which is
     * returned.
     * 
     * @param data
     *            The gold-standard data used for training and validation of the classifier.
     * @return The result of the complete incremental evaluation run. It consists out of the
     *         evaluation results for the known and unknown data for every iteration.
     */
    public EvaluationResult evaluateIncremental(List<List<AnnotationObject>> data)
    {
        EvaluationResult result = new EvaluationResult(classifier.getClass().getSimpleName());

        if (data == null) {
            return result;
        }

        if (conf.isShuffleTrainingSet()) {
            Collections.shuffle(data);
        }

        trainer.reconfigure();

        IncrementalTrainingEvaluationSuite suite = new IncrementalTrainingEvaluationSuite(conf,
                data, trainer, classifier, trainOnCompleteSentence);

        List<ExtendedResult> knownEvaluationResults = new LinkedList<>();
        List<ExtendedResult> unknownEvaluationResults = new LinkedList<>();

        List<List<AnnotationObject>> trainingData = new LinkedList<>();

        if (conf.isUseHoldout()) {
            int offsetTestData = (int) Math
                    .ceil(((double) data.size()) * conf.getSplitTestDataPercentage());

            for (int i = offsetTestData; i < data.size(); i++) {
                trainingData.add(data.get(i));
            }
            data = trainingData;
        }
        
        log.info("Starting evaluation.");
        while (suite.hasNextIteration()) {
            
            log.info("Adding increment."); 
            suite.addIncrement(conf,
                (conf, d) -> { return suite.trainingDataSelector(d); }, 
                (conf, d) -> { return suite.allDataSelector(d); },
                (conf, d) -> { return suite.aggregatingDataSelector(d); }
            );
            
            log.info("Training classifier {}.", classifier.getClass().getSimpleName());
            suite.train();
            
            log.info("Predict against known data.");
            knownEvaluationResults.add(suite.evaluateKnownData());
            if (suite.hasNextIteration()) {
                
                log.info("Predict against unknown data.");
                unknownEvaluationResults.add(suite.evaluateUnknownData());
            }
        }
        log.info("Evaluation finished.");

        result.setKnownDataResult(knownEvaluationResults);
        result.setUnknownDataResult(unknownEvaluationResults);
        return result;
    }
}

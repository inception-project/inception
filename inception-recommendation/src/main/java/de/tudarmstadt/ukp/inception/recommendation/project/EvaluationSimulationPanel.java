/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.inception.recommendation.project;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommenderFactoryRegistry;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.IncrementalSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.chart.ChartPanel;
import de.tudarmstadt.ukp.inception.recommendation.model.LearningCurve;

public class EvaluationSimulationPanel
    extends Panel
{
    private static final long serialVersionUID = 4306746527837380863L;

    private static final String MID_CHART_CONTAINER = "chart-container";
    private static final String MID_SIMULATION_START_BUTTON = "simulation-start-button";
    private static final String MID_FORM = "form";
    
    private static final double TRAIN_PERCENTAGE = 0.8;
    private static final int INCREMENT = 250;
    private static final int LOW_SAMPLE_THRESHOLD = 10;

    private static final Logger LOG = LoggerFactory.getLogger(EvaluationSimulationPanel.class);
    
    private @SpringBean DocumentService documentService;
    private @SpringBean UserDao userDao;
    private @SpringBean RecommenderFactoryRegistry recommenderRegistry;

    private final Panel emptyPanel;
    private final Project project;
    private final IModel<Recommender> selectedRecommenderPanel;

    public EvaluationSimulationPanel(String aId, Project aProject,
            IModel<Recommender> aSelectedRecommenderPanel)
    {
        super(aId);
        project = aProject;
        selectedRecommenderPanel = aSelectedRecommenderPanel;
                
        Form<Recommender> form = new Form<>(MID_FORM);
        add(form);
        
        emptyPanel  = new EmptyPanel(MID_CHART_CONTAINER);
        emptyPanel.setOutputMarkupPlaceholderTag(true);
        emptyPanel.setOutputMarkupId(true);
        form.add(emptyPanel);
        
        // clicking the start button the annotated documents are evaluated and the learning curve
        // for the selected recommender is plotted in the hCart Panel
        @SuppressWarnings({ "unchecked", "rawtypes" })
        LambdaAjaxButton startButton = new LambdaAjaxButton(MID_SIMULATION_START_BUTTON,
            (_target, _form) -> {
                // replace the empty panel with chart panel on click event so the chard renders
                // with the loadable detachable model.
                ChartPanel chartPanel = new ChartPanel(MID_CHART_CONTAINER, 
                        LoadableDetachableModel.of(this::renderChart));
                chartPanel.setOutputMarkupPlaceholderTag(true);
                chartPanel.setOutputMarkupId(true);
                
                form.addOrReplace(chartPanel);
                _target.add(chartPanel);                
            });
        
        form.add(startButton);
    }
    
    private LearningCurve renderChart()
    {
        String[] scoresAndTrainingSizes = evaluate();

        if (scoresAndTrainingSizes == null) {
            // no warning message here because it has already been shown in the method
            // evaluate(_target). There are different scenarios when the score is returned
            // null and each one is handled differently in the method evaluate(_target).
            return null;
        }

        String scores = scoresAndTrainingSizes[0];
        String trainingSizes = scoresAndTrainingSizes[1];
        String defaultErrorMessage = scoresAndTrainingSizes[2];

        if (scores == null || scores.isEmpty()) {
            String errorMessage = "There were no evaluation to show";
            LOG.warn(errorMessage);
            return new LearningCurve(errorMessage);
        }

        Map<String, String> curveData = new HashMap<String, String>();
        curveData.put(selectedRecommenderPanel.getObject().getName(), scores);

        LearningCurve learningCurve = new LearningCurve();
        learningCurve.setCurveData(curveData);
        learningCurve.setXaxis(trainingSizes);
        learningCurve.setErrorMessage(defaultErrorMessage);
        
        return learningCurve;
    }
    
    /**
     * evaluates the selected recommender with the help of the annotated documents in the project. 
     * 
     * @param aTarget uses to log errors on the page in case of unwanted behaviour
     * 
     * @return comma separated string of scores in the first index of the array
     */
    private String[] evaluate()
    {
        String errorMessage = "No Data Available";
        
        //there must be some recommender selected by the user on the UI
        if (selectedRecommenderPanel.getObject() == null
                || selectedRecommenderPanel.getObject().getTool() == null) {
            errorMessage = "Please select a recommender from the list";
            LOG.error(errorMessage);
            return new String[] {null, null, errorMessage};
        }

        //get all the source documents related to the project
        Map<SourceDocument, AnnotationDocument> listAllDocuments = documentService
                .listAllDocuments(project, userDao.getCurrentUser());

        //create a list of CAS from the pre-annotated documents of the project
        List<CAS> casList = new ArrayList<>();
        listAllDocuments.forEach((source, annotation) -> {
            try {
                CAS cas = documentService.createOrReadInitialCas(source);
                casList.add(cas);
            }
            catch (IOException e1) {
                LOG.error("Unable to create cas list", e1);
                return;
            }
        });
        
        IncrementalSplitter splitStrategy = new IncrementalSplitter(TRAIN_PERCENTAGE,
                INCREMENT, LOW_SAMPLE_THRESHOLD);

        @SuppressWarnings("rawtypes")
        RecommendationEngineFactory factory = recommenderRegistry
                .getFactory(selectedRecommenderPanel.getObject().getTool());
        RecommendationEngine recommender = factory.build(selectedRecommenderPanel.getObject());
        
        if (recommender == null) {
            errorMessage = "Unknown Recommender selected";
            LOG.warn(errorMessage);
            return new String[] {null, null, errorMessage };
        }
        
        StringBuilder sbScore = new StringBuilder();
        StringBuilder sbTrainingSize = new StringBuilder();

        // create a list of comma separated string of scores from every iteration of
        // evaluation.
        while (splitStrategy.hasNext()) {
            splitStrategy.next();

            double trainingSize;
            double score;
            try {
                EvaluationResult evaluationResult = recommender.evaluate(casList, splitStrategy);
                
                if (evaluationResult.isEvaluationSkipped()) {
                    LOG.warn("Evaluation skipped. Chart cannot to be shown");
                    continue;
                }

                score = evaluationResult.computeF1Score();
                trainingSize = evaluationResult.getTrainDataRatio();
            }
            catch (RecommendationException e) {
                LOG.error(e.toString(),e);
                continue;
            }

            sbScore.append(score).append(",");
            sbTrainingSize.append(trainingSize).append(",");
        }

        return new String[] {sbScore.toString(), sbTrainingSize.toString(), errorMessage };
    }
}

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
package de.tudarmstadt.ukp.inception.recommendation.evaluation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
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
import de.tudarmstadt.ukp.inception.recommendation.model.RecommenderEvaluationScoreMetricEnum;

public class SimulationLearningCurvePanel
    extends Panel
{
    private static final long serialVersionUID = 4306746527837380863L;

    private static final String MID_CHART_CONTAINER = "chart-container";
    private static final String MID_SIMULATION_START_BUTTON = "simulation-start-button";
    private static final String MID_FORM = "form";
    private static final String OUTPUT_MID_CHART_CONTAINER = "html-chart-container";
    
    private static final double TRAIN_PERCENTAGE = 0.8;
    private static final int INCREMENT = 250;
    private static final int LOW_SAMPLE_THRESHOLD = 10;

    private static final Logger LOG = LoggerFactory.getLogger(SimulationLearningCurvePanel.class);
    
    private static final List<RecommenderEvaluationScoreMetricEnum> DROPDOWN_VALUES = Arrays
            .asList(RecommenderEvaluationScoreMetricEnum.values());
    
    private @SpringBean DocumentService documentService;
    private @SpringBean UserDao userDao;
    private @SpringBean RecommenderFactoryRegistry recommenderRegistry;

    private final Panel emptyPanel;
    private final Project project;
    private final IModel<Recommender> selectedRecommenderPanel;

    private ChartPanel chartPanel ;
    private RecommenderEvaluationScoreMetricEnum selectedValue;
    List<EvaluationResult> evaluationResults;
    private boolean evaluate;

    public SimulationLearningCurvePanel(String aId, Project aProject,
            IModel<Recommender> aSelectedRecommenderPanel)
    {
        super(aId);
        project = aProject;
        selectedRecommenderPanel = aSelectedRecommenderPanel;
        evaluate = true;

        Form<Recommender> form = new Form<>(MID_FORM);
        add(form);
        
        final DropDownChoice<RecommenderEvaluationScoreMetricEnum> dropdown = 
                new DropDownChoice<RecommenderEvaluationScoreMetricEnum>(
                "select", new Model<RecommenderEvaluationScoreMetricEnum>(DROPDOWN_VALUES.get(0)),
                new ListModel<RecommenderEvaluationScoreMetricEnum>(DROPDOWN_VALUES));
        dropdown.setRequired(true);
        dropdown.setOutputMarkupId(true);
        selectedValue = RecommenderEvaluationScoreMetricEnum.Accuracy;

        dropdown.add(new AjaxFormComponentUpdatingBehavior("change")
        {
            private static final long serialVersionUID = -6744838136235652577L;

            protected void onUpdate(AjaxRequestTarget _target)
            {
                selectedValue = dropdown.getModelObject();

                if (chartPanel == null) {
                    return;
                }
                
                startEvaluation(_target, form);
            }
        });

        form.add(dropdown);
        
        emptyPanel  = new EmptyPanel(MID_CHART_CONTAINER);
        emptyPanel.setOutputMarkupPlaceholderTag(true);
        emptyPanel.setMarkupId(OUTPUT_MID_CHART_CONTAINER);
        emptyPanel.setOutputMarkupId(true);
        form.add(emptyPanel);
        
        // clicking the start button the annotated documents are evaluated and the learning curve
        // for the selected recommender is plotted in the hCart Panel
        @SuppressWarnings({ "unchecked", "rawtypes" })
        LambdaAjaxButton startButton = new LambdaAjaxButton(MID_SIMULATION_START_BUTTON,
            (_target, _form) -> {
                startEvaluation(_target, _form );
            });
        
        form.add(startButton);
    }

    private void startEvaluation(IPartialPageRequestHandler aTarget, MarkupContainer aForm )
    {
        // replace the empty panel with chart panel on click event so the chard renders
        // with the loadable detachable model.
        chartPanel = new ChartPanel(MID_CHART_CONTAINER, 
                LoadableDetachableModel.of(this::renderChart));
        chartPanel.setOutputMarkupPlaceholderTag(true);
        chartPanel.setOutputMarkupId(true);
        
        aForm.addOrReplace(chartPanel);
        aTarget.add(chartPanel);
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

        if (scores.isEmpty()) {
            LOG.warn("There were no evaluation to show");
            return null;
        }

        Map<String, String> curveData = new HashMap<String, String>();
        curveData.put(selectedRecommenderPanel.getObject().getName(), scores);

        LearningCurve learningCurve = new LearningCurve();
        learningCurve.setCurveData(curveData);
        learningCurve.setXaxis(trainingSizes);
        
        evaluate = false;

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
        //there must be some recommender selected by the user on the UI
        if (selectedRecommenderPanel.getObject() == null
                || selectedRecommenderPanel.getObject().getTool() == null) {
            LOG.error("Please select a recommender from the list");
            return null;
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
                LOG.error("Unable to render chart", e1);
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
            LOG.warn("Unknown Recommender selected");
            return null;
        }
        
        if (!evaluate) {
            return getEvaluationScore(evaluationResults);
        }

        evaluationResults = new ArrayList<EvaluationResult>();
        
        // create a list of comma separated string of scores from every iteration of
        // evaluation.
        while (splitStrategy.hasNext()) {
            splitStrategy.next();

            try {
                EvaluationResult evaluationResult = recommender.evaluate(casList, splitStrategy);
                
                if (evaluationResult.isEvaluationSkipped()) {
                    LOG.warn("Evaluation skipped. Chart cannot to be shown");
                    continue;
                }

                evaluationResults.add(evaluationResult);
            }
            catch (RecommendationException e) {
                LOG.error(e.toString(),e);
                continue;
            }
        }

        return getEvaluationScore(evaluationResults);
    }

    private String[] getEvaluationScore(List<EvaluationResult> aEvaluationResults)
    {
        StringBuilder sbScore = new StringBuilder();
        StringBuilder sbTrainingSize = new StringBuilder();

        // create a list of comma separated string of scores from every iteration of
        // evaluation.
        for (EvaluationResult evaluationResult : aEvaluationResults) {

            double trainingSize;
            double score;

            if (evaluationResult.isEvaluationSkipped()) {
                LOG.warn("Evaluation skipped. Chart cannot to be shown");
                continue;
            }

            switch (selectedValue) {
            case Accuracy:
                score = evaluationResult.computeAccuracyScore();
                break;
            case Precision:
                score = evaluationResult.computePrecisionScore();
                break;
            case Recall:
                score = evaluationResult.computeRecallScore();
                break;
            case F1:
                score = evaluationResult.computeF1Score();
                break;
            default:
                score = evaluationResult.computeAccuracyScore();
            }

            trainingSize = evaluationResult.getTrainDataRatio();

            sbScore.append(score).append(",");
            sbTrainingSize.append(trainingSize).append(",");
        }

        return new String[] { sbScore.toString(), sbTrainingSize.toString() };
    }

    public void recommenderChanged()
    {
        evaluate = true;
        
    }
}

/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.evaluation;

import static de.tudarmstadt.ukp.clarin.webanno.api.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.INITIAL_CAS_PSEUDO_USER;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.recommendation.model.RecommenderEvaluationScoreMetricEnum.Accuracy;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
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

    private static final double TRAIN_PERCENTAGE = 0.8;
    private static final int STEPS = 10;
    private static final int LOW_SAMPLE_THRESHOLD = 10;

    private static final Logger LOG = LoggerFactory.getLogger(SimulationLearningCurvePanel.class);

    private static final List<RecommenderEvaluationScoreMetricEnum> DROPDOWN_VALUES = Arrays
            .asList(RecommenderEvaluationScoreMetricEnum.values());

    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userDao;
    private @SpringBean RecommenderFactoryRegistry recommenderRegistry;

    private final Project project;
    private final IModel<Recommender> recommender;
    private final IModel<String> user;

    private final DropDownChoice<RecommenderEvaluationScoreMetricEnum> metricChoice;
    private final DropDownChoice<String> annotatorChoice;
    private final ChartPanel chartPanel;

    private List<EvaluationResult> evaluationResults;
    private boolean evaluate;

    public SimulationLearningCurvePanel(String aId, Project aProject,
            IModel<Recommender> aRecommender)
    {
        super(aId, aRecommender);

        project = aProject;
        recommender = aRecommender;
        evaluate = true;

        Form<Recommender> form = new Form<>(MID_FORM);
        add(form);

        chartPanel = new ChartPanel(MID_CHART_CONTAINER,
                LoadableDetachableModel.of(this::renderChart));
        chartPanel.setOutputMarkupPlaceholderTag(true);
        chartPanel.add(visibleWhen(() -> evaluationResults != null));
        form.add(chartPanel);

        metricChoice = new BootstrapSelect<RecommenderEvaluationScoreMetricEnum>("metric",
                new Model<RecommenderEvaluationScoreMetricEnum>(Accuracy),
                new ListModel<RecommenderEvaluationScoreMetricEnum>(DROPDOWN_VALUES));
        metricChoice.setOutputMarkupId(true);
        // metricChoice.add(new LambdaAjaxFormComponentUpdatingBehavior(//
        // "change", this::actionEvaluate));
        form.add(metricChoice);

        IModel<List<String>> annotatorChoiceModel = LoadableDetachableModel
                .of(this::getSelectableAnnotators);
        user = Model.of(annotatorChoiceModel.getObject().get(0));
        annotatorChoice = new BootstrapSelect<String>("annotator", user, annotatorChoiceModel);
        annotatorChoice.setOutputMarkupId(true);
        // annotatorChoice.add(new LambdaAjaxFormComponentUpdatingBehavior(//
        // "change", this::actionEvaluate));
        form.add(annotatorChoice);

        // clicking the start button the annotated documents are evaluated and the learning curve
        // for the selected recommender is plotted in the hCart Panel
        form.add(new LambdaAjaxButton<>(MID_SIMULATION_START_BUTTON, (_target, _form) -> {
            evaluate = true;
            actionEvaluate(_target);
        }));
    }

    private List<String> getSelectableAnnotators()
    {
        List<String> list = new ArrayList<>();
        list.add(INITIAL_CAS_PSEUDO_USER);
        list.add(CURATION_USER);
        projectService.listProjectUsersWithPermissions(project, ANNOTATOR)
                .forEach(u -> list.add(u.getUsername()));
        return list;
    }

    private void actionEvaluate(AjaxRequestTarget aTarget)
    {
        renderChart();

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
        curveData.put(recommender.getObject().getName(), scores);

        LearningCurve learningCurve = new LearningCurve();
        learningCurve.setCurveData(curveData);
        learningCurve.setXaxis(trainingSizes);

        evaluate = false;

        return learningCurve;
    }

    /**
     * evaluates the selected recommender with the help of the annotated documents in the project.
     * 
     * @return comma separated string of scores in the first index of the array
     */
    private String[] evaluate()
    {
        Optional<AjaxRequestTarget> target = RequestCycle.get().find(AjaxRequestTarget.class);

        // there must be some recommender selected by the user on the UI
        if (recommender.getObject() == null || recommender.getObject().getTool() == null) {
            error("Please select a recommender from the list");
            target.ifPresent(_target -> _target.addChildren(getPage(), IFeedback.class));

            return null;
        }

        List<CAS> casList = new ArrayList<>();
        try {
            for (SourceDocument doc : documentService.listSourceDocuments(project)) {
                if (INITIAL_CAS_PSEUDO_USER.equals(user.getObject())) {
                    casList.add(documentService.createOrReadInitialCas(doc, AUTO_CAS_UPGRADE,
                            SHARED_READ_ONLY_ACCESS));
                }
                else {
                    if (documentService.existsAnnotationDocument(doc, user.getObject())) {
                        casList.add(documentService.readAnnotationCas(doc, user.getObject(),
                                AUTO_CAS_UPGRADE, SHARED_READ_ONLY_ACCESS));
                    }
                }
            }
        }
        catch (IOException e) {
            error("Unable to load chart data: " + getRootCauseMessage(e));
            target.ifPresent(_target -> _target.addChildren(getPage(), IFeedback.class));
            LOG.error("Unable to render chart", e);
            return null;
        }

        @SuppressWarnings("rawtypes")
        RecommendationEngineFactory factory = recommenderRegistry
                .getFactory(recommender.getObject().getTool());
        RecommendationEngine recommenderEngine = factory.build(recommender.getObject());

        if (recommenderEngine == null) {
            error("Unknown recommender type selected: [" + recommender.getObject().getTool() + "]");
            target.ifPresent(_target -> _target.addChildren(getPage(), IFeedback.class));
            return null;
        }

        if (!evaluate) {
            return getEvaluationScore(evaluationResults);
        }

        int estimatedDatasetSize = recommenderEngine.estimateSampleCount(casList);
        if (estimatedDatasetSize < 0) {
            error("Evaluation is not supported for the selected recommender.");
            target.ifPresent(_target -> _target.addChildren(getPage(), IFeedback.class));
            return null;
        }

        IncrementalSplitter splitStrategy = new IncrementalSplitter(TRAIN_PERCENTAGE,
                estimatedDatasetSize / STEPS, LOW_SAMPLE_THRESHOLD);

        evaluationResults = new ArrayList<EvaluationResult>();

        // create a list of comma separated string of scores from every iteration of
        // evaluation.
        while (splitStrategy.hasNext()) {
            splitStrategy.next();

            try {
                EvaluationResult evaluationResult = recommenderEngine.evaluate(casList,
                        splitStrategy);

                if (!evaluationResult.isEvaluationSkipped()) {
                    evaluationResults.add(evaluationResult);
                }
            }
            catch (RecommendationException e) {
                error("Unable to run simulation: " + getRootCauseMessage(e));
                target.ifPresent(_target -> _target.addChildren(getPage(), IFeedback.class));
                LOG.error("Unable to run simulation", e);
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
                continue;
            }

            switch (metricChoice.getModelObject()) {
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

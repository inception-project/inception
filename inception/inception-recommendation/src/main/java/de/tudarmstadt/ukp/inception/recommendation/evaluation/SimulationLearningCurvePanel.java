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
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.INITIAL_CAS_PSEUDO_USER;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.recommendation.model.RecommenderEvaluationScoreMetricEnum.Accuracy;
import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaChoiceRenderer;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommenderFactoryRegistry;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.EvaluationResult;
import de.tudarmstadt.ukp.inception.recommendation.api.evaluation.IncrementalSplitter;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationException;
import de.tudarmstadt.ukp.inception.recommendation.chart.ChartPanel;
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
    private final IModel<User> user;
    private final IModel<RecommenderEvaluationScoreMetricEnum> metric;

    private final DropDownChoice<RecommenderEvaluationScoreMetricEnum> metricChoice;
    private final DropDownChoice<User> annotatorChoice;
    private final ChartPanel chartPanel;

    private final IModel<List<EvaluationResult>> evaluationResults;

    public SimulationLearningCurvePanel(String aId, Project aProject,
            IModel<Recommender> aRecommender)
    {
        super(aId, aRecommender);

        project = aProject;
        recommender = aRecommender;
        evaluationResults = new ListModel<EvaluationResult>(emptyList());
        metric = new Model<RecommenderEvaluationScoreMetricEnum>(Accuracy);

        Form<Recommender> form = new Form<>(MID_FORM);
        add(form);

        chartPanel = new ChartPanel(MID_CHART_CONTAINER, evaluationResults, metric);
        chartPanel.setOutputMarkupPlaceholderTag(true);
        chartPanel.add(visibleWhen(() -> isNotEmpty(chartPanel.getModelObject())));
        form.add(chartPanel);

        metricChoice = new DropDownChoice<RecommenderEvaluationScoreMetricEnum>("metric", metric,
                new ListModel<RecommenderEvaluationScoreMetricEnum>(DROPDOWN_VALUES));
        metricChoice.setOutputMarkupId(true);
        form.add(metricChoice);

        IModel<List<User>> annotatorChoiceModel = LoadableDetachableModel
                .of(this::getSelectableAnnotators);
        user = Model.of(annotatorChoiceModel.getObject().get(0));
        annotatorChoice = new DropDownChoice<User>("annotator", user, annotatorChoiceModel);
        annotatorChoice.setChoiceRenderer(new LambdaChoiceRenderer<>(User::getUiName));
        annotatorChoice.setOutputMarkupId(true);
        form.add(annotatorChoice);

        // clicking the start button the annotated documents are evaluated and the learning curve
        // for the selected recommender is plotted in the hCart Panel
        form.add(new LambdaAjaxButton<>(MID_SIMULATION_START_BUTTON, (_target, _form) -> {
            recommenderChanged();
            evaluate();
            _target.add(chartPanel);
        }));
    }

    private List<User> getSelectableAnnotators()
    {
        List<User> list = new ArrayList<>();
        list.add(new User(INITIAL_CAS_PSEUDO_USER, "<Source document>"));
        list.add(new User(CURATION_USER, "<Curation document>"));
        list.addAll(projectService.listProjectUsersWithPermissions(project, ANNOTATOR));
        return list;
    }

    /**
     * evaluates the selected recommender with the help of the annotated documents in the project.
     */
    private void evaluate()
    {
        if (evaluationResults.getObject() != null) {
            return;
        }

        Optional<AjaxRequestTarget> target = RequestCycle.get().find(AjaxRequestTarget.class);

        // there must be some recommender selected by the user on the UI
        if (recommender.getObject() == null || recommender.getObject().getTool() == null) {
            error("Please select a recommender from the list");
            target.ifPresent(_target -> _target.addChildren(getPage(), IFeedback.class));
            return;
        }

        List<CAS> casList = new ArrayList<>();
        try {
            for (SourceDocument doc : documentService.listSourceDocuments(project)) {
                if (INITIAL_CAS_PSEUDO_USER.equals(user.getObject().getUsername())) {
                    casList.add(documentService.createOrReadInitialCas(doc, AUTO_CAS_UPGRADE,
                            SHARED_READ_ONLY_ACCESS));
                }
                else {
                    if (documentService.existsAnnotationDocument(doc, user.getObject())) {
                        casList.add(documentService.readAnnotationCas(doc,
                                user.getObject().getUsername(), AUTO_CAS_UPGRADE,
                                SHARED_READ_ONLY_ACCESS));
                    }
                }
            }
        }
        catch (IOException e) {
            error("Unable to load chart data: " + getRootCauseMessage(e));
            target.ifPresent(_target -> _target.addChildren(getPage(), IFeedback.class));
            LOG.error("Unable to render chart", e);
            return;
        }

        @SuppressWarnings("rawtypes")
        RecommendationEngineFactory factory = recommenderRegistry
                .getFactory(recommender.getObject().getTool());
        RecommendationEngine recommenderEngine = factory.build(recommender.getObject());

        if (recommenderEngine == null) {
            error("Unknown recommender type selected: [" + recommender.getObject().getTool() + "]");
            target.ifPresent(_target -> _target.addChildren(getPage(), IFeedback.class));
            return;
        }

        int estimatedDatasetSize = recommenderEngine.estimateSampleCount(casList);
        if (estimatedDatasetSize < 0) {
            error("Evaluation is not supported for the selected recommender.");
            target.ifPresent(_target -> _target.addChildren(getPage(), IFeedback.class));
            return;
        }

        int incrementSize = (int) Math.ceil((estimatedDatasetSize * TRAIN_PERCENTAGE)) / STEPS;
        if (incrementSize <= 0) {
            error("Not enough training data: " + estimatedDatasetSize + " samples");
            target.ifPresent(_target -> _target.addChildren(getPage(), IFeedback.class));
            return;
        }

        IncrementalSplitter splitStrategy = new IncrementalSplitter(TRAIN_PERCENTAGE, incrementSize,
                LOW_SAMPLE_THRESHOLD);

        evaluationResults.setObject(new ArrayList<>());

        // Create a list of comma separated string of scores from every iteration of evaluation.
        while (splitStrategy.hasNext()) {
            splitStrategy.next();

            try {
                EvaluationResult evaluationResult = recommenderEngine.evaluate(casList,
                        splitStrategy);

                if (!evaluationResult.isEvaluationSkipped()) {
                    evaluationResults.getObject().add(evaluationResult);
                }
            }
            catch (RecommendationException e) {
                error("Unable to run simulation: " + getRootCauseMessage(e));
                target.ifPresent(_target -> _target.addChildren(getPage(), IFeedback.class));
                LOG.error("Unable to run simulation", e);
                continue;
            }
        }
    }

    public void recommenderChanged()
    {
        evaluationResults.setObject(null);
    }
}

/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.inception.recommendation.evaluation;

import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.MAX_RECOMMENDATIONS_DEFAULT;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.project.RecommenderListPanel;

@MountPath("/evaluation.html")
public class EvaluationSimulationPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = 3042218455285633439L;

    private Project projectModel;
    private IModel<Recommender> selectedRecommenderModel;
    private @SpringBean ProjectService projectService;

    private static final String MID_EVALUATION_SIMULATION_CONTAINER = "evaluation-simulation-container";
    private static final String MID_RECOMMENDER_LIST = "recommenderList";
    private static final String MID_RECOMMENDER_VIEW = "recommenderView";

    public EvaluationSimulationPage(final PageParameters aPageParameters)
    {
        super();

        projectModel = projectService.getProject(aPageParameters.get("p").toLong());

        selectedRecommenderModel = Model.of();
        
        SimulationLearningCurvePanel evaluationSimulationPanel = new SimulationLearningCurvePanel(
                MID_EVALUATION_SIMULATION_CONTAINER, projectModel,
                selectedRecommenderModel);
        evaluationSimulationPanel.setOutputMarkupId(true);
        add(evaluationSimulationPanel);

        RecommenderViewPanel recommenderViewPanel = new RecommenderViewPanel(MID_RECOMMENDER_VIEW,
                selectedRecommenderModel);
        add(recommenderViewPanel);

        RecommenderListPanel recommenderListPanel = new RecommenderListPanel(MID_RECOMMENDER_LIST,
                Model.of(projectModel), selectedRecommenderModel, false);
        recommenderListPanel.setCreateAction(_target -> {
            Recommender recommender = new Recommender();
            recommender.setMaxRecommendations(MAX_RECOMMENDATIONS_DEFAULT);
            selectedRecommenderModel.setObject(recommender);
        });
        recommenderListPanel.setChangeAction(_target -> {
            _target.add(recommenderViewPanel);
            evaluationSimulationPanel.recommenderChanged();
        });
        add(recommenderListPanel);
    }
}

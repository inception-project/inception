/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
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

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhenModelIsNotNull;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.project.RecommenderListPanel;

@MountPath(NS_PROJECT + "/${" + PAGE_PARAM_PROJECT + "}/simulation")
public class EvaluationSimulationPage
    extends ProjectPageBase
{
    private static final long serialVersionUID = 3042218455285633439L;

    private static final String MID_EVALUATION_SIMULATION_CONTAINER = "evaluation-simulation-container";
    private static final String MID_RECOMMENDER_LIST = "recommenderList";
    private static final String MID_RECOMMENDER_VIEW = "recommenderView";

    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;

    private IModel<Recommender> selectedRecommenderModel;

    public EvaluationSimulationPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);

        Project project = getProject();

        requireProjectRole(userRepository.getCurrentUser(), MANAGER);

        selectedRecommenderModel = Model.of();

        SimulationLearningCurvePanel evaluationSimulationPanel = new SimulationLearningCurvePanel(
                MID_EVALUATION_SIMULATION_CONTAINER, project, selectedRecommenderModel);
        add(evaluationSimulationPanel);

        RecommenderViewPanel recommenderViewPanel = new RecommenderViewPanel(MID_RECOMMENDER_VIEW,
                selectedRecommenderModel);
        recommenderViewPanel.setOutputMarkupPlaceholderTag(true);
        recommenderViewPanel.add(visibleWhenModelIsNotNull(recommenderViewPanel));
        add(recommenderViewPanel);

        RecommenderListPanel recommenderListPanel = new RecommenderListPanel(MID_RECOMMENDER_LIST,
                Model.of(project), selectedRecommenderModel, false);
        recommenderListPanel.setChangeAction(_target -> {
            evaluationSimulationPanel.recommenderChanged();
            _target.add(recommenderViewPanel);
        });
        add(recommenderListPanel);
    }
}

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
package de.tudarmstadt.ukp.inception.recommendation.project;

import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.MAX_RECOMMENDATIONS_DEFAULT;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;

public class ProjectRecommendersPanel
    extends ProjectSettingsPanelBase
{
    private static final long serialVersionUID = 3042218455285633439L;
    
    private static final String EVALUATION_SIMULATION_CONTAINER = "evaluation-simulation-container";

    private IModel<Project> projectModel;
    private IModel<Recommender> selectedRecommenderModel;
    
    public ProjectRecommendersPanel(String aId, IModel<Project> aProject)
    {
        super(aId, aProject);

        selectedRecommenderModel = Model.of();
        projectModel = aProject;

        RecommenderEditorPanel recommenderEditorPanel = new RecommenderEditorPanel(
                "recommenderEditor", projectModel, selectedRecommenderModel);
        add(recommenderEditorPanel);

        RecommenderListPanel recommenderListPanel = new RecommenderListPanel("recommenders",
                projectModel, selectedRecommenderModel);
        recommenderListPanel.setCreateAction(_target -> {
            Recommender recommender = new Recommender();
            recommender.setMaxRecommendations(MAX_RECOMMENDATIONS_DEFAULT);
            selectedRecommenderModel.setObject(recommender);
            recommenderEditorPanel.modelChanged();
        });
        recommenderListPanel.setChangeAction(_target -> {
            recommenderEditorPanel.modelChanged();
            _target.add(recommenderEditorPanel);
        });
        add(recommenderListPanel);
        
        EvaluationSimulationPanel evaluationSimulationPanel = new EvaluationSimulationPanel(
                EVALUATION_SIMULATION_CONTAINER, projectModel.getObject(),
                selectedRecommenderModel);
        add(evaluationSimulationPanel);
    }

    @Override
    protected void onModelChanged()
    {
        super.onModelChanged();
        selectedRecommenderModel.setObject(null);
    }
}

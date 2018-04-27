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

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;

@ProjectSettingsPanel(label = "Recommenders", prio = 400)
public class ProjectRecommendersPanel
    extends ProjectSettingsPanelBase
{
    private static final long serialVersionUID = 3042218455285633439L;

    private IModel<Project> projectModel;
    private IModel<Recommender> selectedRecommenderModel;
    
    public ProjectRecommendersPanel(String aId, IModel<Project> aProject)
    {
        super(aId);

        selectedRecommenderModel = Model.of();
        projectModel = aProject;

        RecommenderEditorPanel recommenderEditorPanel = new RecommenderEditorPanel(
                "recommenderEditor", projectModel, selectedRecommenderModel);
        add(recommenderEditorPanel);

        RecommenderListPanel recommenderListPanel = new RecommenderListPanel("recommenders",
                projectModel, selectedRecommenderModel);
        recommenderListPanel.setCreateAction(_target -> {
            recommenderEditorPanel.modelChanged();
            selectedRecommenderModel.setObject(new Recommender());
        });
        recommenderListPanel.setChangeAction(_target -> {
            recommenderEditorPanel.modelChanged();
            _target.add(recommenderEditorPanel);
        });
        add(recommenderListPanel);
    }
}

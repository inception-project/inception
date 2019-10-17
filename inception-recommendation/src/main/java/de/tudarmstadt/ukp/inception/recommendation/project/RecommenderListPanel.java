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
package de.tudarmstadt.ukp.inception.recommendation.project;

import java.util.List;

import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.ListPanel_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.OverviewListChoice;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;

public class RecommenderListPanel
    extends ListPanel_ImplBase
{
    private static final long serialVersionUID = -9151455840010092452L;

    private @SpringBean RecommendationService recommendationService;

    private IModel<Project> projectModel;
    private IModel<Recommender> selectedRecommender;
    
    private OverviewListChoice<Recommender> overviewList;
    
    public RecommenderListPanel(String id, IModel<Project> aProject,
            IModel<Recommender> aRecommender, boolean showCreateButton)
    {
        super(id);
        
        setOutputMarkupId(true);
        
        projectModel = aProject;
        selectedRecommender = aRecommender;

        overviewList = new OverviewListChoice<>("recommenders");
        overviewList.setChoiceRenderer(new ChoiceRenderer<>("name"));
        overviewList.setModel(selectedRecommender);
        overviewList.setChoices(LambdaModel.of(this::listRecommenders));
        overviewList.add(new LambdaAjaxFormComponentUpdatingBehavior("change", this::onChange));
        add(overviewList);
        
        LambdaAjaxLink lambdaAjaxLink = new LambdaAjaxLink("create", this::actionCreate);
        add(lambdaAjaxLink);  
        
        if (!showCreateButton)        {
            lambdaAjaxLink.setVisible(false);
        }
    }
    
    private List<Recommender> listRecommenders()
    {
        return recommendationService.listRecommenders(projectModel.getObject());
    }
}

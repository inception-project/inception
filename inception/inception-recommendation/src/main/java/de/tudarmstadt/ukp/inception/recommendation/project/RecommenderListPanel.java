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
package de.tudarmstadt.ukp.inception.recommendation.project;

import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.KEY_RECOMMENDER_GENERAL_SETTINGS;
import static java.util.stream.Collectors.toList;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.RecommenderGeneralSettings;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.wicket.ListPanel_ImplBase;
import de.tudarmstadt.ukp.inception.support.wicket.OverviewListChoice;

public class RecommenderListPanel
    extends ListPanel_ImplBase
{
    private static final long serialVersionUID = -9151455840010092452L;

    private static final String MID_CREATE_BUTTON = "create";

    private @SpringBean RecommendationService recommendationService;
    private @SpringBean PreferencesService preferencesService;

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
        overviewList.setChoices(LoadableDetachableModel.of(this::listRecommenders));
        overviewList.add(new LambdaAjaxFormComponentUpdatingBehavior("change", this::onChange));
        add(overviewList);

        var lambdaAjaxLink = new LambdaAjaxLink(MID_CREATE_BUTTON, this::actionCreate);
        lambdaAjaxLink.setVisible(showCreateButton);
        add(lambdaAjaxLink);

        var settings = preferencesService.loadDefaultTraitsForProject(
                KEY_RECOMMENDER_GENERAL_SETTINGS, projectModel.getObject());

        var form = new Form<>("form", CompoundPropertyModel.of(settings));
        form.setOutputMarkupId(true);
        form.add(new CheckBox("waitForRecommendersOnOpenDocument") //
                .setOutputMarkupId(true));
        form.add(new CheckBox("showRecommendationsWhenViewingOtherUser") //
                .setOutputMarkupId(true));
        form.add(new CheckBox("showRecommendationsWhenViewingCurationUser") //
                .setOutputMarkupId(true) //
                .setVisible(recommendationService.isCurationSidebarEnabled()));
        form.add(new CheckBox("annotatorAllowedToExportModel") //
                .setOutputMarkupId(true));
        form.add(new LambdaAjaxButton<>("save", this::actionSaveSettings));
        add(form);
    }

    private void actionSaveSettings(AjaxRequestTarget aTarget,
            Form<RecommenderGeneralSettings> aForm)
    {
        preferencesService.saveDefaultTraitsForProject(KEY_RECOMMENDER_GENERAL_SETTINGS,
                projectModel.getObject(), aForm.getModelObject());
        success("General recommender settings updated");
        aTarget.addChildren(getPage(), IFeedback.class);
    }

    private List<Recommender> listRecommenders()
    {
        return recommendationService.listRecommenders(projectModel.getObject()).stream()
                .filter(e -> recommendationService.getRecommenderFactory(e).isPresent())
                .collect(toList());
    }
}

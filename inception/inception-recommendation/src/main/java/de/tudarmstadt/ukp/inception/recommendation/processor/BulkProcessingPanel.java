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
package de.tudarmstadt.ukp.inception.recommendation.processor;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.TrainingCapability;
import de.tudarmstadt.ukp.inception.recommendation.tasks.BulkPredictionTask;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.ui.scheduling.TaskMonitorPanel;

public class BulkProcessingPanel
    extends GenericPanel<Project>
{
    private static final long serialVersionUID = 3568501821432165745L;

    private @SpringBean ProjectService projectService;
    private @SpringBean RecommendationService recommendationService;
    private @SpringBean SchedulingService schedulingService;
    private @SpringBean UserDao userService;

    public BulkProcessingPanel(String aId, IModel<Project> aModel)
    {
        super(aId, aModel);

        queue(new Form<FormData>("form", new CompoundPropertyModel<>(new FormData())));

        queue(new DropDownChoice<>("user") //
                .setChoices(LoadableDetachableModel.of(this::listUsers)) //
                .setChoiceRenderer(new ChoiceRenderer<>("uiName")) //
                .setRequired(true));

        queue(new DropDownChoice<>("recommender") //
                .setChoices(LoadableDetachableModel.of(this::listRecommenders)) //
                .setChoiceRenderer(new ChoiceRenderer<>("name")) //
                .setRequired(true));

        queue(new LambdaAjaxButton<>("startProcessing", this::actionStartProcessing));

        queue(new TaskMonitorPanel("runningProcesses").setPopupMode(false)
                .setShowFinishedTasks(true));
    }

    private void actionStartProcessing(AjaxRequestTarget aTarget, Form<FormData> aForm)
    {
        var formData = aForm.getModelObject();
        schedulingService.enqueue(BulkPredictionTask.builder() //
                .withSessionOwner(userService.getCurrentUser()) //
                .withRecommender(formData.recommender) //
                .withTrigger("User request") //
                .withDataOwner(formData.user.getUsername()) //
                .build());
    }

    private List<User> listUsers()
    {
        return projectService.listProjectUsersWithPermissions(getModelObject(), ANNOTATOR);
    }

    private List<Recommender> listRecommenders()
    {
        // We list all recommenders here - not only the enabled ones. Maybe we want to manually
        // run a recommender that is not auto-running (enabled).
        var recommenders = new ArrayList<Recommender>();
        for (var recommender : recommendationService.listRecommenders(getModelObject())) {
            var maybeFactory = recommendationService.getRecommenderFactory(recommender);
            if (maybeFactory.isEmpty()) {
                continue;
            }

            var factory = maybeFactory.get();
            var engine = factory.build(recommender);

            // If a recommender requires training, it would yield no results if the user has not yet
            // annotated any documents. So in this case, we do currently not offer it for
            // processing.
            // It could be considered to also offer such recommenders for cases where there user has
            // already marked some documents as finished so that the remaining documents could be
            // annotated based on the training data from the finished documents...
            if (engine.getTrainingCapability() == TrainingCapability.TRAINING_REQUIRED) {
                continue;
            }

            recommenders.add(recommender);
        }
        return recommenders;
    }

    private static class FormData
        implements Serializable
    {
        private static final long serialVersionUID = 375269887794449024L;

        private User user;
        private Recommender recommender;
    }
}

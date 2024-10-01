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
package de.tudarmstadt.ukp.inception.recommendation.imls.external.v1;

import static de.tudarmstadt.ukp.inception.recommendation.api.recommender.TrainingCapability.TRAINING_NOT_SUPPORTED;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Arrays.asList;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.UrlValidator;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.DefaultTrainableRecommenderTraitsEditor;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.TrainingCapability;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;

public class ExternalRecommenderTraitsEditor
    extends DefaultTrainableRecommenderTraitsEditor
{
    private static final long serialVersionUID = 1677442652521110324L;

    private static final String MID_FORM = "form";

    private @SpringBean RecommendationEngineFactory<ExternalRecommenderTraits> toolFactory;

    private final ExternalRecommenderTraits traits;

    public ExternalRecommenderTraitsEditor(String aId, IModel<Recommender> aRecommender)
    {
        super(aId, aRecommender);

        traits = toolFactory.readTraits(aRecommender.getObject());

        Form<ExternalRecommenderTraits> form = new Form<ExternalRecommenderTraits>(MID_FORM,
                CompoundPropertyModel.of(Model.of(traits)))
        {
            private static final long serialVersionUID = -3109239605742291123L;

            @Override
            protected void onSubmit()
            {
                super.onSubmit();
                toolFactory.writeTraits(aRecommender.getObject(), traits);
            }
        };

        var remoteUrl = new TextField<String>("remoteUrl");
        remoteUrl.setRequired(true);
        remoteUrl.add(new UrlValidator());
        form.add(remoteUrl);

        var verifyCertificates = new CheckBox("verifyCertificates");
        verifyCertificates.setOutputMarkupId(true);
        form.add(verifyCertificates);

        var trainingCapability = new DropDownChoice<TrainingCapability>("trainingCapability");
        trainingCapability.setOutputMarkupId(true);
        trainingCapability.setChoiceRenderer(new EnumChoiceRenderer<>(trainingCapability));
        trainingCapability.setChoices(asList(TrainingCapability.values()));
        trainingCapability.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                _target -> _target.add(getTrainingStatesChoice())));
        form.add(trainingCapability);

        getTrainingStatesChoice().add(
                visibleWhen(() -> trainingCapability.getModelObject() != TRAINING_NOT_SUPPORTED));

        var ranker = new CheckBox("ranker");
        ranker.setOutputMarkupId(true);
        form.add(ranker);

        add(form);
    }
}

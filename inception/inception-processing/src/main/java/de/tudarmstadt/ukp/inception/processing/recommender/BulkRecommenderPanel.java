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
package de.tudarmstadt.ukp.inception.processing.recommender;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhenNot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer_;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User_;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender_;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.TrainingCapability;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.ui.core.docanno.layer.DocumentMetadataLayerSupport;

public class BulkRecommenderPanel
    extends GenericPanel<Project>
{
    private static final long serialVersionUID = 3568501821432165745L;

    private @SpringBean ProjectService projectService;
    private @SpringBean RecommendationService recommendationService;
    private @SpringBean SchedulingService schedulingService;
    private @SpringBean UserDao userService;
    private @SpringBean AnnotationSchemaService annotationSchemaService;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;

    private CompoundPropertyModel<FormData> formModel;
    private FeatureEditorPanel processingMetadata;

    public BulkRecommenderPanel(String aId, IModel<Project> aModel)
    {
        super(aId, aModel);

        formModel = new CompoundPropertyModel<>(new FormData());
        queue(new Form<FormData>("form", formModel));

        queue(new DropDownChoice<>("user") //
                .setChoices(LoadableDetachableModel.of(this::listUsers)) //
                .setChoiceRenderer(new ChoiceRenderer<>(User_.UI_NAME)) //
                .setRequired(true));

        queue(new DropDownChoice<>("recommender") //
                .setChoices(LoadableDetachableModel.of(this::listRecommenders)) //
                .setChoiceRenderer(new ChoiceRenderer<>(Recommender_.NAME)) //
                .setRequired(true));

        processingMetadata = new FeatureEditorPanel("processingMetadata");
        processingMetadata.setOutputMarkupPlaceholderTag(true);
        queue(processingMetadata);

        var docMetaLayers = LoadableDetachableModel.of(this::listDocumentMetadataLayers);
        queue(new DropDownChoice<>("processingMetadataLayer") //
                .setNullValid(true) //
                .setChoices(docMetaLayers) //
                .setChoiceRenderer(new ChoiceRenderer<>(AnnotationLayer_.UI_NAME))
                .add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT, _target -> {
                    processingMetadata.setModelObject(listFeatureStates());
                    _target.add(processingMetadata);
                })) //
                .add(visibleWhenNot(docMetaLayers.map(List::isEmpty))));

        queue(new LambdaAjaxButton<>("startProcessing", this::actionStartProcessing));
    }

    private void actionStartProcessing(AjaxRequestTarget aTarget, Form<FormData> aForm)
    {
        var metadata = new HashMap<AnnotationFeature, Serializable>();
        for (var state : processingMetadata.getModelObject()) {
            metadata.put(state.getFeature(), state.getValue());
        }

        var formData = aForm.getModelObject();
        schedulingService.enqueue(BulkPredictionTask.builder() //
                .withSessionOwner(userService.getCurrentUser()) //
                .withRecommender(formData.recommender) //
                .withTrigger("User request") //
                .withDataOwner(formData.user.getUsername()) //
                .withProcessingMetadata(metadata) //
                .build());
    }

    private List<User> listUsers()
    {
        return projectService.listProjectUsersWithPermissions(getModelObject(), ANNOTATOR);
    }

    private List<AnnotationLayer> listDocumentMetadataLayers()
    {
        return annotationSchemaService.listAnnotationLayer(getModelObject()) //
                .stream().filter(l -> DocumentMetadataLayerSupport.TYPE.equals(l.getType())) //
                .toList();
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

    private List<FeatureState> listFeatureStates()
    {
        if (!formModel.map(d -> d.processingMetadataLayer).isPresent().getObject()) {
            return Collections.emptyList();
        }

        var layer = formModel.map(d -> d.processingMetadataLayer).getObject();

        var featureStates = new ArrayList<FeatureState>();
        for (var feature : annotationSchemaService.listEnabledFeatures(layer)) {
            if (feature.getLinkMode() != LinkMode.NONE) {
                continue;
            }

            var featureState = new FeatureState(null, feature, null);
            featureStates.add(featureState);
            featureState.tagset = annotationSchemaService
                    .listTagsReorderable(featureState.feature.getTagset());
        }

        return featureStates;
    }

    private static class FormData
        implements Serializable
    {
        private static final long serialVersionUID = 375269887794449024L;

        private User user;
        private Recommender recommender;
        private AnnotationLayer processingMetadataLayer;
    }
}

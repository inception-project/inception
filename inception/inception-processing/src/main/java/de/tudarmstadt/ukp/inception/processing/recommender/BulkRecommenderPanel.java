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

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.NEW;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.inception.recommendation.api.recommender.TrainingCapability.TRAINING_REQUIRED;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhenNot;
import static de.tudarmstadt.ukp.inception.support.wicket.WicketUtil.wrapInTryCatch;
import static java.lang.String.format;
import static java.util.Arrays.asList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.validation.IValidatable;
import org.wicketstuff.kendo.ui.KendoUIBehavior;
import org.wicketstuff.kendo.ui.form.combobox.ComboBox;
import org.wicketstuff.kendo.ui.form.combobox.ComboBoxBehavior;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer_;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerSupport;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender_;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;

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

        var userRenderer = new org.wicketstuff.kendo.ui.renderer.ChoiceRenderer<User>()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public String getText(User aObject)
            {
                if (!Objects.equals(aObject.getUsername(), aObject.getUiName())) {
                    if (aObject.getUsername() == null) {
                        return aObject.getUiName() + " (new)";
                    }

                    return aObject.getUiName() + " (" + aObject.getUsername() + ")";
                }

                return aObject.getUsername();
            }

            @Override
            public String getValue(User aObject)
            {
                return aObject.getUsername();
            }
        };
        var userChoices = LoadableDetachableModel.of(this::listUsers);
        var user = new ComboBox<User>("user", userChoices, userRenderer)
        {
            private static final long serialVersionUID = -4402805232261971312L;

            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            public <C> IConverter<C> getConverter(Class<C> aType)
            {
                if (User.class.isAssignableFrom(aType)) {
                    return (IConverter) new IConverter<User>()
                    {
                        private static final long serialVersionUID = -8311927973299485L;

                        @Override
                        public User convertToObject(String aValue, Locale aLocale)
                            throws ConversionException
                        {
                            var u = userService.get(aValue);
                            if (u != null) {
                                return u;
                            }
                            return User.builder() //
                                    .withUiName(aValue) //
                                    .withRealm(projectService
                                            .getRealm(BulkRecommenderPanel.this.getModelObject()))
                                    .build();
                        }

                        @Override
                        public String convertToString(User aValue, Locale aLocale)
                        {
                            return aValue.getUsername();
                        }
                    };
                }

                return super.getConverter(aType);
            }
        };
        user.add(LambdaBehavior.onConfigure(() -> {
            // Trigger a re-loading of the model from the server
            userChoices.detach();
            var target = RequestCycle.get().find(AjaxRequestTarget.class);
            if (target.isPresent()) {
                target.get().appendJavaScript(wrapInTryCatch(format( //
                        "var $w = %s; if ($w) { $w.dataSource.read(); }",
                        KendoUIBehavior.widget(this, ComboBoxBehavior.METHOD))));
            }
        }));
        user.add(this::validateUiName);
        ;
        user.setOutputMarkupId(true);
        user.setRequired(true);
        queue(user);

        var recommender = new DropDownChoice<Recommender>("recommender");
        recommender.setChoices(LoadableDetachableModel.of(this::listRecommenders));
        recommender.setChoiceRenderer(new ChoiceRenderer<>(Recommender_.NAME));
        recommender.setRequired(true);
        queue(recommender);

        if (recommender.getChoices().size() == 1) {
            formModel.getObject().recommender = recommender.getChoices().get(0);
        }

        queue(new AnnotationDocumentStatesChoice("states") //
                .setChoices(asList(NEW, IN_PROGRESS)));

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

        queue(new CheckBox("finishDocumentsWithoutRecommendations") //
                .setOutputMarkupId(true));

        queue(new LambdaAjaxButton<>("startProcessing", this::actionStartProcessing));

        var closeDialogButton = new LambdaAjaxLink("closeDialog", this::actionCancel);
        closeDialogButton.setOutputMarkupId(true);
        queue(closeDialogButton);
    }

    private void validateUiName(IValidatable<?> aValidatable)
    {
        if (aValidatable.getValue() instanceof User user) {
            userService.validateUiName(user.getUiName()).forEach(aValidatable::error);
            return;
        }

        if (aValidatable.getValue() instanceof String username) {
            userService.validateUiName(username).forEach(aValidatable::error);
            return;
        }
    }

    protected void actionCancel(AjaxRequestTarget aTarget)
    {
        findParent(ModalDialog.class).close(aTarget);
    }

    private void actionStartProcessing(AjaxRequestTarget aTarget, Form<FormData> aForm)
    {
        var metadata = new HashMap<AnnotationFeature, Serializable>();
        for (var state : processingMetadata.getModelObject()) {
            metadata.put(state.getFeature(), state.getValue());
        }

        var formData = aForm.getModelObject();

        // Create a project-bound user if the user does not exist yet
        if (formData.user.getUsername() == null) {
            var realm = projectService.getRealm(getModelObject());
            var other = userService.getUserByRealmAndUiName(realm, formData.user.getUiName());
            if (other != null) {
                error("There is already a user named [" + other.getUiName()
                        + "] in this project! Select the user from the drop-down menu to use it.");
                aTarget.addChildren(getPage(), IFeedback.class);
                return;
            }

            formData.user = projectService.getOrCreateProjectBoundUser(getModelObject(),
                    formData.user.getUiName());
            projectService.assignRole(getModelObject(), formData.user, ANNOTATOR);
        }

        schedulingService.enqueue(BulkPredictionTask.builder() //
                .withSessionOwner(userService.getCurrentUser()) //
                .withRecommender(formData.recommender) //
                .withTrigger("User request") //
                .withDataOwner(formData.user.getUsername()) //
                .withProcessingMetadata(metadata) //
                .withStatesToProcess(formData.states) //
                .withFinishDocumentsWithoutRecommendations(
                        formData.finishDocumentsWithoutRecommendations) //
                .build());

        findParent(ModalDialog.class).close(aTarget);
    }

    private List<User> listUsers()
    {
        return projectService.listUsersWithRoleInProject(getModelObject(), ANNOTATOR);
    }

    private List<AnnotationLayer> listDocumentMetadataLayers()
    {
        return annotationSchemaService.listAnnotationLayer(getModelObject()).stream() //
                .filter(l -> DocumentMetadataLayerSupport.TYPE.equals(l.getType())) //
                .toList();
    }

    private List<Recommender> listRecommenders()
    {
        // We list all recommenders here - not only the enabled ones. Maybe we want to manually
        // run a recommender that is not auto-running (enabled).
        var recommenders = new ArrayList<Recommender>();
        for (var recommender : recommendationService.listEnabledRecommenders(getModelObject())) {
            var maybeFactory = recommendationService.getRecommenderFactory(recommender);
            if (maybeFactory.isEmpty()) {
                continue;
            }

            var factory = maybeFactory.get();
            if (factory.isInteractive(recommender)) {
                continue;
            }

            // If a recommender requires training, it would yield no results if the user has not yet
            // annotated any documents. So in this case, we do currently not offer it for
            // processing.
            // It could be considered to also offer such recommenders for cases where there user has
            // already marked some documents as finished so that the remaining documents could be
            // annotated based on the training data from the finished documents...
            //
            // see
            // de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.aero.AeroTaskBulkPredictionController.create()
            var engine = factory.build(recommender);
            if (engine.getTrainingCapability() == TRAINING_REQUIRED) {
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
        private boolean finishDocumentsWithoutRecommendations;
        private List<AnnotationDocumentState> states;

        {
            states = new ArrayList<>();
            states.add(NEW);
        }
    }
}

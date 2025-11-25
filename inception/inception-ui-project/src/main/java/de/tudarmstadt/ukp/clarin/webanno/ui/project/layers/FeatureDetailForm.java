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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.layers;

import static de.tudarmstadt.ukp.clarin.webanno.ui.project.layers.ProjectLayersPanel.MID_FEATURE_DETAIL_FORM;
import static de.tudarmstadt.ukp.clarin.webanno.ui.project.layers.ProjectLayersPanel.MID_FEATURE_SELECTION_FORM;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.uima.cas.CAS.TYPE_NAME_BOOLEAN;
import static org.apache.uima.cas.CAS.TYPE_NAME_DOUBLE;
import static org.apache.uima.cas.CAS.TYPE_NAME_FLOAT;
import static org.apache.uima.cas.CAS.TYPE_NAME_INTEGER;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING_ARRAY;

import java.io.IOException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.bootstrap.dialog.ChallengeResponseDialog;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.event.LayerConfigurationChangedEvent;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureType;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationEventPublisherHolder;

public class FeatureDetailForm
    extends Form<AnnotationFeature>
{
    private static final String MID_TRAITS_CONTAINER = "traitsContainer";
    private static final String MID_TRAITS = "traits";
    private static final String FIRST = "first";
    private static final String NEXT = "next";

    private static final long serialVersionUID = -1L;

    private @SpringBean LayerSupportRegistry layerSupportRegistry;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean DocumentService documentService;
    private @SpringBean CasStorageService casStorageService;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisherHolder;

    private final DropDownChoice<FeatureType> featureType;
    private final CheckBox required;
    private final WebMarkupContainer traitsContainer;
    private WebMarkupContainer defaultOptionsContainer;
    private final ChallengeResponseDialog confirmationDialog;
    private final TextField<String> uiName;

    public FeatureDetailForm(String id, IModel<AnnotationFeature> aFeature)
    {
        super(id, CompoundPropertyModel.of(aFeature));

        setOutputMarkupPlaceholderTag(true);

        add(traitsContainer = new WebMarkupContainer(MID_TRAITS_CONTAINER));
        traitsContainer.setOutputMarkupId(true);

        add(new Label("name").add(visibleWhen(() -> isNotBlank(getModelObject().getName()))));
        uiName = new TextField<>("uiName");
        uiName.setRequired(true);
        uiName.setOutputMarkupId(true);
        add(uiName);
        add(new TextArea<String>("description"));

        defaultOptionsContainer = new WebMarkupContainer("defaultOptionsContainer");
        defaultOptionsContainer.add(visibleWhen(this::isUsingDefaultOptions));
        add(defaultOptionsContainer);
        defaultOptionsContainer.add(new CheckBox("enabled").setOutputMarkupPlaceholderTag(true));
        defaultOptionsContainer.add(new CheckBox("curatable").setOutputMarkupPlaceholderTag(true));

        var remember = new CheckBox("remember");
        remember.setOutputMarkupPlaceholderTag(true);
        remember.add(LambdaBehavior.onConfigure(_this -> {
            var type = FeatureDetailForm.this.getModelObject().getType();
            var rememberAllowed = asList(TYPE_NAME_INTEGER, TYPE_NAME_FLOAT, TYPE_NAME_DOUBLE,
                    TYPE_NAME_BOOLEAN, TYPE_NAME_STRING).contains(type);
            if (rememberAllowed) {
                remember.setModel(PropertyModel.of(FeatureDetailForm.this.getModel(), "remember"));
                remember.setVisible(true);
            }
            else {
                remember.setModel(Model.of(false));
                remember.setVisible(false);
            }
        }));
        defaultOptionsContainer.add(remember);

        required = new CheckBox("required");
        required.setOutputMarkupPlaceholderTag(true);
        required.add(LambdaBehavior.onConfigure(_this -> {
            var type = FeatureDetailForm.this.getModelObject().getType();
            var requiredMandatory = asList(TYPE_NAME_INTEGER, TYPE_NAME_FLOAT, TYPE_NAME_DOUBLE,
                    TYPE_NAME_BOOLEAN).contains(type);
            var requiredOptional = asList(TYPE_NAME_STRING_ARRAY, TYPE_NAME_STRING).contains(type);
            if (requiredMandatory) {
                required.setModel(Model.of(true));
                required.setEnabled(false);
            }
            else if (requiredOptional) {
                required.setModel(PropertyModel.of(FeatureDetailForm.this.getModel(), "required"));
                required.setEnabled(true);
                required.setVisible(true);
            }
            else {
                required.setModel(Model.of(false));
                required.setVisible(false);
            }
        }));
        defaultOptionsContainer.add(required);

        defaultOptionsContainer.add(new CheckBox("visible").setOutputMarkupPlaceholderTag(true));
        defaultOptionsContainer.add(new CheckBox("hideUnconstraintFeature") //
                .setOutputMarkupPlaceholderTag(true) //
                .add(visibleWhen(() -> {
                    var type = FeatureDetailForm.this.getModelObject().getType();
                    var constraintsSupportType = asList(TYPE_NAME_STRING_ARRAY, TYPE_NAME_STRING)
                            .contains(type);
                    var hasTagset = FeatureDetailForm.this.getModelObject().getTagset() != null;
                    return constraintsSupportType && hasTagset;
                })) //
                .setOutputMarkupPlaceholderTag(true));
        defaultOptionsContainer
                .add(new CheckBox("includeInHover").setOutputMarkupPlaceholderTag(true) //
                        .add(visibleWhen(() -> {
                            var layertype = FeatureDetailForm.this.getModelObject().getLayer()
                                    .getType();
                            // Currently not configurable for chains or relations
                            // TODO: technically it is possible
                            return !ChainLayerSupport.TYPE.equals(layertype)
                                    && !RelationLayerSupport.TYPE.equals(layertype);
                        })));

        add(featureType = new DropDownChoice<FeatureType>("type")
        {
            private static final long serialVersionUID = 9029205407108101183L;

            @Override
            protected void onModelChanged()
            {
                // If the feature type has changed, we need to set up a new traits editor
                Component newTraits;
                if (FeatureDetailForm.this.getModelObject() != null && getModelObject() != null) {
                    FeatureSupport<?> fs = featureSupportRegistry
                            .getExtension(getModelObject().getFeatureSupportId()).orElseThrow();
                    newTraits = fs.createTraitsEditor(MID_TRAITS,
                            FeatureDetailForm.this.getModel());
                }
                else {
                    newTraits = new EmptyPanel(MID_TRAITS);
                }

                traitsContainer.addOrReplace(newTraits);
            }
        });
        featureType.setRequired(true);
        featureType.setNullValid(false);
        featureType.setChoiceRenderer(new ChoiceRenderer<>("uiName"));
        featureType.setModel(
                LambdaModelAdapter.of(() -> featureSupportRegistry.getFeatureType(getModelObject()),
                        (v) -> getModelObject().setType(v.getName())));
        featureType.add(LambdaBehavior.onConfigure(_this -> {
            if (isNull(getModelObject().getId())) {
                featureType.setEnabled(true);
                featureType.setChoices(() -> featureSupportRegistry
                        .getUserSelectableTypes(getModelObject().getLayer()));
            }
            else {
                featureType.setEnabled(false);
                featureType.setChoices(
                        asList(featureSupportRegistry.getFeatureType(getModelObject())));
            }
        }));
        featureType.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT, _target -> {
            _target.add(required);
            _target.add(traitsContainer);
        }));

        // Processing the data in onAfterSubmit so the traits panel can use the
        // override onSubmit in its nested form and store the traits before
        // we clear the currently selected feature.
        add(new LambdaAjaxButton<>("save", this::actionSave).triggerAfterSubmit());
        add(new LambdaAjaxButton<>("delete", this::actionDelete)
                .add(enabledWhen(this::isDeletable)));
        // Set default form processing to false to avoid saving data
        add(new LambdaButton("cancel", this::actionCancel).setDefaultFormProcessing(false));

        confirmationDialog = new ChallengeResponseDialog("confirmationDialog");
        confirmationDialog.setTitleModel(new ResourceModel("DeleteFeatureDialog.title"));
        add(confirmationDialog);
    }

    private boolean isUsingDefaultOptions()
    {
        var feature = getModelObject();
        if (isNull(feature.getId())) {
            return false;
        }

        return featureSupportRegistry.findExtension(feature) //
                .map(ext -> ext.isUsingDefaultOptions(feature)) //
                .orElse(false);
    }

    private boolean isDeletable()
    {
        var feature = getModelObject();
        if (isNull(feature.getId())) {
            return false;
        }

        if (feature.getLayer().isBuiltIn()) {
            return false;
        }

        return layerSupportRegistry.getLayerSupport(feature.getLayer()).isDeletable(feature);
    }

    public Component getInitialFocusComponent()
    {
        return uiName;
    }

    @Override
    protected void onModelChanged()
    {
        super.onModelChanged();

        // Since feature type uses a lambda model, it needs to be notified explicitly.
        featureType.modelChanged();
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        setVisible(getModelObject() != null);
    }

    private void actionCancel()
    {
        // cancel selection of feature list
        setModelObject(null);
    }

    private void actionDelete(AjaxRequestTarget aTarget, Form<AnnotationLayer> aForm)
    {
        confirmationDialog.setMessageModel(new ResourceModel("DeleteFeatureDialog.text"));
        confirmationDialog.setExpectedResponseModel(getModel().map(AnnotationFeature::getName));
        confirmationDialog.setConfirmAction(this::actionDeleteConfirmed);
        confirmationDialog.show(aTarget);
    }

    private void actionDeleteConfirmed(AjaxRequestTarget aTarget) throws IOException
    {
        annotationService.removeFeature(getModelObject());

        var project = getModelObject().getProject();

        setModelObject(null);

        documentService.upgradeAllAnnotationDocuments(project);

        // Trigger LayerConfigurationChangedEvent
        applicationEventPublisherHolder.get()
                .publishEvent(new LayerConfigurationChangedEvent(this, project));

        aTarget.add(getPage());
    }

    private void actionSave(AjaxRequestTarget aTarget, Form<AnnotationLayer> aForm)
    {
        aTarget.addChildren(getPage(), IFeedback.class);

        var feature = getModelObject();

        if (isNull(feature.getId())) {
            feature.setName(feature.getUiName().replaceAll("\\W", ""));

            var nameValidationResult = annotationService.validateFeatureName(feature);
            if (!nameValidationResult.isEmpty()) {
                nameValidationResult.forEach(msg -> error(msg.getMessage()));
                return;
            }

            feature.setLayer(getModelObject().getLayer());
            feature.setProject(getModelObject().getLayer().getProject());

            FeatureSupport<?> fs = featureSupportRegistry
                    .getExtension(featureType.getModelObject().getFeatureSupportId()).orElseThrow();

            // Let the feature support finalize the configuration of the feature
            fs.configureFeature(feature);
        }

        // Save feature
        annotationService.createFeature(feature);

        // Clear currently selected feature / feature details
        setModelObject(null);

        success("Settings for feature [" + feature.getUiName() + "] saved.");

        aTarget.add(findParent(ProjectLayersPanel.class).get(MID_FEATURE_DETAIL_FORM));
        aTarget.add(findParent(ProjectLayersPanel.class).get(MID_FEATURE_SELECTION_FORM));

        // Trigger LayerConfigurationChangedEvent
        applicationEventPublisherHolder.get()
                .publishEvent(new LayerConfigurationChangedEvent(this, feature.getProject()));
    }
}

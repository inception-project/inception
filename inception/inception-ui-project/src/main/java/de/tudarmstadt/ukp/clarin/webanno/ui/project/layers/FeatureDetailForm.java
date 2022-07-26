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

import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.CHAIN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.RESTRICTED_FEATURE_NAMES;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.clarin.webanno.ui.project.layers.ProjectLayersPanel.MID_FEATURE_DETAIL_FORM;
import static de.tudarmstadt.ukp.clarin.webanno.ui.project.layers.ProjectLayersPanel.MID_FEATURE_SELECTION_FORM;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isAlphanumeric;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static org.apache.wicket.util.string.Strings.escapeMarkup;

import org.apache.uima.cas.CAS;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
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
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.event.LayerConfigurationChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ChallengeResponseDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.clarin.webanno.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureType;

public class FeatureDetailForm
    extends Form<AnnotationFeature>
{
    private static final String MID_TRAITS_CONTAINER = "traitsContainer";
    private static final String MID_TRAITS = "traits";
    private static final String FIRST = "first";
    private static final String NEXT = "next";

    private static final long serialVersionUID = -1L;

    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean DocumentService documentService;
    private @SpringBean CasStorageService casStorageService;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisherHolder;

    private final DropDownChoice<FeatureType> featureType;
    private final CheckBox required;
    private final WebMarkupContainer traitsContainer;
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
        add(new CheckBox("enabled").setOutputMarkupPlaceholderTag(true));
        add(new CheckBox("visible").setOutputMarkupPlaceholderTag(true));
        add(new CheckBox("curatable").setOutputMarkupPlaceholderTag(true));
        add(new CheckBox("hideUnconstraintFeature").setOutputMarkupPlaceholderTag(true));
        add(new CheckBox("remember").setOutputMarkupPlaceholderTag(true));
        add(new CheckBox("includeInHover").setOutputMarkupPlaceholderTag(true)
                .add(LambdaBehavior.visibleWhen(() -> {
                    String layertype = FeatureDetailForm.this.getModelObject().getLayer().getType();
                    // Currently not configurable for chains or relations
                    // TODO: technically it is possible
                    return !CHAIN_TYPE.equals(layertype) && !RELATION_TYPE.equals(layertype);
                })));
        required = new CheckBox("required");
        required.setOutputMarkupPlaceholderTag(true);
        required.add(LambdaBehavior.onConfigure(_this -> {
            boolean relevant = CAS.TYPE_NAME_STRING
                    .equals(FeatureDetailForm.this.getModelObject().getType());
            _this.setEnabled(relevant);
            if (!relevant) {
                FeatureDetailForm.this.getModelObject().setRequired(false);
            }
        }));
        add(required);

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
                        () -> featureSupportRegistry.getAllTypes(getModelObject().getLayer()));
            }
        }));
        featureType.add(new AjaxFormComponentUpdatingBehavior("change")
        {
            private static final long serialVersionUID = -2904306846882446294L;

            @Override
            protected void onUpdate(AjaxRequestTarget aTarget)
            {
                aTarget.add(required);
                aTarget.add(traitsContainer);
            }
        });

        // Processing the data in onAfterSubmit so the traits panel can use the
        // override onSubmit in its nested form and store the traits before
        // we clear the currently selected feature.
        add(new LambdaAjaxButton<>("save", this::actionSave).triggerAfterSubmit());
        add(new LambdaAjaxButton<>("delete", this::actionDelete)
                .add(enabledWhen(() -> !isNull(getModelObject().getId())
                        && !getModelObject().getLayer().isBuiltIn())));
        // Set default form processing to false to avoid saving data
        add(new LambdaButton("cancel", this::actionCancel).setDefaultFormProcessing(false));

        confirmationDialog = new ChallengeResponseDialog("confirmationDialog");
        confirmationDialog
                .setTitleModel(new StringResourceModel("DeleteFeatureDialog.title", this));
        add(confirmationDialog);
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

    private void actionDelete(AjaxRequestTarget aTarget, Form aForm)
    {
        confirmationDialog
                .setChallengeModel(new StringResourceModel("DeleteFeatureDialog.text", this)
                        .setParameters(escapeMarkup(getModelObject().getName())));
        confirmationDialog.setResponseModel(Model.of(getModelObject().getName()));
        confirmationDialog.show(aTarget);

        confirmationDialog.setConfirmAction((_target) -> {
            annotationService.removeFeature(getModelObject());

            Project project = getModelObject().getProject();

            setModelObject(null);

            documentService.upgradeAllAnnotationDocuments(project);

            // Trigger LayerConfigurationChangedEvent
            applicationEventPublisherHolder.get()
                    .publishEvent(new LayerConfigurationChangedEvent(this, project));

            _target.add(getPage());
        });
    }

    private void actionSave(AjaxRequestTarget aTarget, Form<?> aForm)
    {
        AnnotationFeature feature = getModelObject();

        if (isNull(feature.getId())) {
            feature.setName(feature.getUiName().replaceAll("\\W", ""));

            try {
                validateFeatureName(feature);
            }
            catch (IllegalArgumentException e) {
                error(e.getMessage());
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
        aTarget.addChildren(getPage(), IFeedback.class);

        aTarget.add(findParent(ProjectLayersPanel.class).get(MID_FEATURE_DETAIL_FORM));
        aTarget.add(findParent(ProjectLayersPanel.class).get(MID_FEATURE_SELECTION_FORM));

        // Trigger LayerConfigurationChangedEvent
        applicationEventPublisherHolder.get()
                .publishEvent(new LayerConfigurationChangedEvent(this, feature.getProject()));
    }

    private void validateFeatureName(AnnotationFeature aFeature)
    {
        String name = aFeature.getName();

        if (isBlank(name)) {
            throw new IllegalArgumentException("Feature names must start with a letter and consist "
                    + "only of letters, digits, or underscores.");
        }

        // Check if feature name is not from the restricted names list
        if (RESTRICTED_FEATURE_NAMES.contains(name)) {
            throw new IllegalArgumentException("[" + name + "] is a reserved feature name. Please "
                    + "use a different name for the feature.");
        }

        if (RELATION_TYPE.equals(aFeature.getLayer().getType())
                && (name.equals(FEAT_REL_SOURCE) || name.equals(FEAT_REL_TARGET))) {
            throw new IllegalArgumentException("[" + name + "] is a reserved feature name on "
                    + "relation layers. Please use a different name for the feature.");
        }

        if (CHAIN_TYPE.equals(aFeature.getLayer().getType())
                && (name.equals(FIRST) || name.equals(NEXT))) {
            throw new IllegalArgumentException("[" + name + "] is a reserved feature name on "
                    + "chain layers. Please use a different name for the feature.");
        }

        // Checking if feature name doesn't start with a number or underscore
        // And only uses alphanumeric characters
        if (isNumeric(name.substring(0, 1)) || name.substring(0, 1).equals("_")
                || !isAlphanumeric(name.replace("_", ""))) {
            throw new IllegalArgumentException("Feature names must start with a letter and consist "
                    + "only of letters, digits, or underscores.");
        }

        if (annotationService.existsFeature(name, aFeature.getLayer())) {
            throw new IllegalArgumentException(
                    "A feature with the name [" + name + "] already exists on this layer!");
        }
    }
}

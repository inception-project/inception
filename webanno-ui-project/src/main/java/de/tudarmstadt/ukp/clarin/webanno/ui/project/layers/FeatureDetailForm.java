/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.layers;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CHAIN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.wicket.util.string.Strings.escapeMarkup;

import java.io.FileNotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
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
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureType;
import de.tudarmstadt.ukp.clarin.webanno.api.event.LayerConfigurationChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.bootstrap.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ConfirmationDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.clarin.webanno.support.spring.ApplicationEventPublisherHolder;

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
    private final ConfirmationDialog confirmationDialog;
    private final TextField<String> name;

    public FeatureDetailForm(String id, IModel<AnnotationFeature> aFeature)
    {
        super(id, CompoundPropertyModel.of(aFeature));

        setOutputMarkupPlaceholderTag(true);
        
        add(traitsContainer = new WebMarkupContainer(MID_TRAITS_CONTAINER));
        traitsContainer.setOutputMarkupId(true);
        
        add(new Label("name").add(visibleWhen(() -> isNotBlank(getModelObject().getName()))));
        name = new TextField<>("uiName");
        name.setRequired(true);
        name.setOutputMarkupId(true);
        add(name);
        add(new TextArea<String>("description"));
        add(new CheckBox("enabled"));
        add(new CheckBox("visible"));
        add(new CheckBox("hideUnconstraintFeature"));
        add(new CheckBox("remember"));
        add(new CheckBox("includeInHover")
        {
            private static final long serialVersionUID = -8273152168889478682L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                
                String layertype = FeatureDetailForm.this.getModelObject().getLayer().getType();
                // Currently not configurable for chains or relations
                // TODO: technically it is possible
                setVisible(!CHAIN_TYPE.equals(layertype) && !RELATION_TYPE.equals(layertype));
            }

        });
        required = new CheckBox("required");
        required.setOutputMarkupId(true);
        required.add(LambdaBehavior.onConfigure(_this -> {
            boolean relevant = CAS.TYPE_NAME_STRING
                    .equals(FeatureDetailForm.this.getModelObject().getType());
            _this.setEnabled(relevant);
            if (!relevant) {
                FeatureDetailForm.this.getModelObject().setRequired(false);
            }
        }));
        add(required);

        add(featureType = new BootstrapSelect<FeatureType>("type") {
            private static final long serialVersionUID = 9029205407108101183L;

            @Override
            protected void onModelChanged()
            {
                // If the feature type has changed, we need to set up a new traits editor
                Component newTraits;
                if (FeatureDetailForm.this.getModelObject() != null
                        && getModelObject() != null) {
                    FeatureSupport<?> fs = featureSupportRegistry
                            .getFeatureSupport(getModelObject().getFeatureSupportId());
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
        featureType.setModel(LambdaModelAdapter.of(
            () -> featureSupportRegistry.getFeatureType(getModelObject()), 
            (v) -> getModelObject().setType(v.getName())));
        featureType.add(LambdaBehavior.onConfigure(_this -> {
            if (isNull(getModelObject().getId())) {
                featureType.setEnabled(true);
                featureType.setChoices(() -> featureSupportRegistry
                        .getUserSelectableTypes(getModelObject().getLayer()));
            }
            else {
                featureType.setEnabled(false);
                featureType.setChoices(() -> featureSupportRegistry.getAllTypes(
                        getModelObject().getLayer()));
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

        add(new LambdaButton("save", this::actionSave).triggerAfterSubmit());
        add(new LambdaAjaxButton<>("delete", this::actionDelete).add(
                visibleWhen(() -> !isNull(getModelObject().getId()))));
        // Set default form processing to false to avoid saving data
        add(new LambdaButton("cancel", this::actionCancel).setDefaultFormProcessing(false));
        
        confirmationDialog = new ConfirmationDialog("confirmationDialog");
        confirmationDialog
                .setTitleModel(new StringResourceModel("DeleteFeatureDialog.title", this));
        add(confirmationDialog);
    }
    
    public Component getInitialFocusComponent()
    {
        return name;
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
                .setContentModel(new StringResourceModel("DeleteFeatureDialog.text", this)
                .setParameters(escapeMarkup(getModelObject().getName())));
        confirmationDialog.show(aTarget);
        
        confirmationDialog.setConfirmAction((_target) -> {
            annotationService.removeAnnotationFeature(getModelObject());
            
            Project project = getModelObject().getProject();

            setModelObject(null);

            // Perform a forced upgrade on all CASes in the project. This action affects all users
            // currently logged in and working on the project. E.g. an annotator working on a
            // document will be unable to make changes to the document anymore until the user
            // re-opens the document because the force upgrade invalidates the VIDs used in the
            // annotation editor. How exactly (if at all) the user gets information of this is
            // currently undefined.
            casStorageService.performExclusiveBulkOperation(() -> {
                for (SourceDocument doc : documentService.listSourceDocuments(project)) {
                    for (AnnotationDocument ann : documentService.listAllAnnotationDocuments(doc)) {
                        try {
                            JCas cas = casStorageService.readCas(doc, ann.getUser());
                            annotationService.upgradeCas(cas.getCas(), doc, ann.getUser());
                            casStorageService.writeCas(doc, cas, ann.getUser());
                        }
                        catch (FileNotFoundException e) {
                            // If there is no CAS file, we do not have to upgrade it. Ignoring.
                        }
                    }
                    
                    // Also upgrade the curation CAS if it exists
                    try {
                        JCas cas = casStorageService.readCas(doc, CURATION_USER);
                        annotationService.upgradeCas(cas.getCas(), doc, CURATION_USER);
                        casStorageService.writeCas(doc, cas, CURATION_USER);
                    }
                    catch (FileNotFoundException e) {
                        // If there is no CAS file, we do not have to upgrade it. Ignoring.
                    }
                }
            });

            // Trigger LayerConfigurationChangedEvent
            applicationEventPublisherHolder.get()
                    .publishEvent(new LayerConfigurationChangedEvent(this, project));
            
            _target.add(getPage());
        });
    }
    
    private void actionSave()
    {
        // Processing the data in onAfterSubmit so the traits panel can use the
        // override onSubmit in its nested form and store the traits before
        // we clear the currently selected feature.

        AnnotationFeature feature = getModelObject();
        String name = feature.getUiName();
        name = name.replaceAll("\\W", "");
        // Check if feature name is not from the restricted names list
        if (WebAnnoConst.RESTRICTED_FEATURE_NAMES.contains(name)) {
            error("'" + feature.getUiName().toLowerCase() + " (" + name + ")'"
                    + " is a reserved feature name. Please use a different name "
                    + "for the feature.");
            return;
        }
        if (RELATION_TYPE.equals(getModelObject().getLayer().getType())
                && (name.equals(WebAnnoConst.FEAT_REL_SOURCE)
                        || name.equals(WebAnnoConst.FEAT_REL_TARGET) || name.equals(FIRST)
                        || name.equals(NEXT))) {
            error("'" + feature.getUiName().toLowerCase() + " (" + name + ")'"
                    + " is a reserved feature name on relation layers. . Please "
                    + "use a different name for the feature.");
            return;
        }
        // Checking if feature name doesn't start with a number or underscore
        // And only uses alphanumeric characters
        if (StringUtils.isNumeric(name.substring(0, 1))
                || name.substring(0, 1).equals("_")
                || !StringUtils.isAlphanumeric(name.replace("_", ""))) {
            error("Feature names must start with a letter and consist only of "
                    + "letters, digits, or underscores.");
            return;
        }
        if (isNull(feature.getId())) {
            feature.setLayer(getModelObject().getLayer());
            feature.setProject(getModelObject().getLayer().getProject());

            if (annotationService.existsFeature(feature.getName(),
                    feature.getLayer())) {
                error("This feature is already added for this layer!");
                return;
            }

            if (annotationService.existsFeature(name, feature.getLayer())) {
                error("This feature already exists!");
                return;
            }
            feature.setName(name);

            FeatureSupport<?> fs = featureSupportRegistry
                    .getFeatureSupport(featureType.getModelObject().getFeatureSupportId());

            // Let the feature support finalize the configuration of the feature
            fs.configureFeature(feature);

        }

        // Save feature
        annotationService.createFeature(feature);

        // Clear currently selected feature / feature details
        setModelObject(null);

        // Trigger LayerConfigurationChangedEvent
        applicationEventPublisherHolder.get().publishEvent(
                new LayerConfigurationChangedEvent(this, feature.getProject()));
    }
}

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

import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.SPAN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.wicket.util.string.Strings.escapeMarkup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.IResourceStream;

import de.agilecoders.wicket.core.markup.html.bootstrap.form.BootstrapRadioChoice;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.event.LayerConfigurationChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationLayerReference;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ChallengeResponseDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormChoiceComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.clarin.webanno.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.AjaxDownloadLink;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.InputStreamResourceStream;
import de.tudarmstadt.ukp.clarin.webanno.ui.project.layers.ProjectLayersPanel.FeatureSelectionForm;
import de.tudarmstadt.ukp.clarin.webanno.ui.project.layers.ProjectLayersPanel.LayerExportMode;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.export.ImportUtil;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.schema.layer.LayerSupport;
import de.tudarmstadt.ukp.inception.schema.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.layer.LayerType;
import de.tudarmstadt.ukp.inception.support.help.DocLink;

public class LayerDetailForm
    extends Form<AnnotationLayer>
{
    private static final long serialVersionUID = -1L;

    private static final String MID_TRAITS_CONTAINER = "traitsContainer";
    private static final String MID_TRAITS = "traits";

    private static final String TYPE_PREFIX = "webanno.custom.";

    private @SpringBean LayerSupportRegistry layerSupportRegistry;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean DocumentService documentService;
    private @SpringBean CasStorageService casStorageService;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisherHolder;

    private DropDownChoice<LayerType> layerTypeSelect;
    private DropDownChoice<AnnotationLayer> attachTypeSelect;
    private Label effectiveAttachType;
    private TextField<String> uiName;

    private FeatureSelectionForm featureSelectionForm;
    private FeatureDetailForm featureDetailForm;

    private WebMarkupContainer traitsContainer;
    private final ChallengeResponseDialog confirmationDialog;

    private LayerExportMode exportMode = LayerExportMode.JSON;

    public LayerDetailForm(String id, IModel<AnnotationLayer> aSelectedLayer,
            FeatureSelectionForm aFeatureSelectionForm, FeatureDetailForm aFeatureDetailForm)
    {
        super(id, CompoundPropertyModel.of(aSelectedLayer));

        add(new DocLink("propertiesHelpLink", "sect_projects_layers_properties"));
        add(new DocLink("technicalPropertiesHelpLink", "sect_projects_layers_properties"));
        add(new DocLink("behavioursHelpLink", "sect_projects_layers_behaviours"));

        featureSelectionForm = aFeatureSelectionForm;
        featureDetailForm = aFeatureDetailForm;

        setOutputMarkupPlaceholderTag(true);

        add(traitsContainer = new WebMarkupContainer(MID_TRAITS_CONTAINER));
        traitsContainer.setOutputMarkupId(true);

        uiName = new TextField<String>("uiName");
        uiName.setOutputMarkupId(true);
        uiName.setRequired(true);
        add(uiName);
        add(new TextArea<String>("description").setOutputMarkupPlaceholderTag(true));

        add(new Label("name").add(visibleWhen(() -> isNotBlank(getModelObject().getName()))));

        add(new CheckBox("enabled").setOutputMarkupPlaceholderTag(true));

        add(layerTypeSelect = new DropDownChoice<LayerType>("type")
        {
            private static final long serialVersionUID = 9029205407108101183L;

            @Override
            protected void onModelChanged()
            {
                // If the feature type has changed, we need to set up a new traits editor
                Component newTraits;
                if (LayerDetailForm.this.getModelObject() != null && getModelObject() != null) {
                    LayerSupport<?, ?> fs = layerSupportRegistry
                            .getLayerSupport(getModelObject().getlayerSupportId());
                    newTraits = fs.createTraitsEditor(MID_TRAITS, LayerDetailForm.this.getModel());
                }
                else {
                    newTraits = new EmptyPanel(MID_TRAITS);
                }

                traitsContainer.addOrReplace(newTraits);
            }
        });
        layerTypeSelect.setChoices(layerSupportRegistry::getAllTypes);
        layerTypeSelect.add(enabledWhen(() -> isNull(getModelObject().getId())));
        layerTypeSelect.setRequired(true);
        layerTypeSelect.setNullValid(false);
        layerTypeSelect.setChoiceRenderer(new ChoiceRenderer<>("uiName"));
        layerTypeSelect.setModel(
                LambdaModelAdapter.of(() -> layerSupportRegistry.getLayerType(getModelObject()),
                        (v) -> getModelObject().setType(v.getName())));
        layerTypeSelect.add(new AjaxFormComponentUpdatingBehavior("change")
        {
            private static final long serialVersionUID = 6790949494089940303L;

            @Override
            protected void onUpdate(AjaxRequestTarget aTarget)
            {
                aTarget.add(attachTypeSelect);
                aTarget.add(traitsContainer);
            }
        });

        attachTypeSelect = new DropDownChoice<AnnotationLayer>("attachType",
                LoadableDetachableModel.of(this::getAttachLayerChoices),
                new ChoiceRenderer<>("uiName"));
        attachTypeSelect.setNullValid(true);
        attachTypeSelect.add(visibleWhen(() -> isNull(getModelObject().getId())
                && RELATION_TYPE.equals(getModelObject().getType())));
        attachTypeSelect.setOutputMarkupPlaceholderTag(true);
        add(attachTypeSelect);

        effectiveAttachType = new Label("effectiveAttachType",
                LoadableDetachableModel.of(this::getEffectiveAttachTypeName));
        effectiveAttachType.setOutputMarkupPlaceholderTag(true);
        effectiveAttachType.add(visibleWhen(() -> !isNull(getModelObject().getId())
                && RELATION_TYPE.equals(getModelObject().getType())));
        add(effectiveAttachType);

        // Behaviors of layers
        add(new CheckBox("readonly").setOutputMarkupPlaceholderTag(true));

        BootstrapRadioChoice<LayerExportMode> exportModeChoice = new BootstrapRadioChoice<>(
                "exportMode", asList(LayerExportMode.values()));
        exportModeChoice.setModel(new PropertyModel<LayerExportMode>(this, "exportMode"));
        exportModeChoice.setChoiceRenderer(new EnumChoiceRenderer<>(this));
        exportModeChoice.add(new LambdaAjaxFormChoiceComponentUpdatingBehavior());
        add(exportModeChoice);

        add(new AjaxDownloadLink("export",
                new LambdaModel<>(this::getExportLayerFileName).autoDetaching(),
                this::exportLayer));

        // Processing the data in onAfterSubmit so the traits panel can use the
        // override onSubmit in its nested form and store the traits before
        // we clear the currently selected feature.
        add(new LambdaAjaxButton<AnnotationLayer>("save", this::actionSave).triggerAfterSubmit());
        add(new LambdaAjaxButton<AnnotationLayer>("delete", this::actionDelete).add(enabledWhen(
                () -> !isNull(getModelObject().getId()) && isLayerDeletable(getModelObject()))));
        add(new LambdaAjaxLink("cancel", this::actionCancel));

        confirmationDialog = new ChallengeResponseDialog("confirmationDialog");
        confirmationDialog.setTitleModel(new StringResourceModel("DeleteLayerDialog.title", this));
        add(confirmationDialog);
    }

    private String getEffectiveAttachTypeName()
    {
        AnnotationLayer layer = LayerDetailForm.this.getModelObject();

        if (layer.getAttachType() == null) {
            return null;
        }

        if (layer.getAttachFeature() != null) {
            Project project = getModelObject().getProject();
            AnnotationLayer actualAttachLayer = annotationService.findLayer(project,
                    layer.getAttachFeature().getType());
            return String.format("%s :: %s -> %s", layer.getAttachType().getUiName(),
                    layer.getAttachFeature().getUiName(), actualAttachLayer.getUiName());
        }
        else {
            return layer.getAttachType().getUiName();
        }
    }

    /**
     * Gets the list of annotation layers to which a relation layer may attach.
     */
    private List<AnnotationLayer> getAttachLayerChoices()
    {
        Project project = getModelObject().getProject();
        AnnotationLayer layer = LayerDetailForm.this.getModelObject();

        // If the layer has already been created, the attach layer cannot be changed anymore.
        // So in this case, we return either an empty list of a list with exactly the configured
        // attach layer in it.
        if (layer.getId() != null) {
            if (layer.getAttachType() == null) {
                return emptyList();
            }

            if (layer.getAttachFeature() != null) {
                AnnotationLayer actualAttachLayer = annotationService.findLayer(project,
                        layer.getAttachFeature().getType());
                return asList(actualAttachLayer);
            }
            else {
                return asList(layer.getAttachType());
            }
        }

        // Attach layers are only valid for relation layers.
        if (!RELATION_TYPE.equals(layer.getType())) {
            return emptyList();
        }

        // Get all the layers
        List<AnnotationLayer> allLayers = annotationService.listAnnotationLayer(project);

        // Candidates for attach-layers are only span layers, so lets filter these
        List<AnnotationLayer> candidateLayers = allLayers.stream()
                .filter(l -> SPAN_TYPE.equals(l.getType()))
                .filter(l -> !Token.class.getName().equals(l.getName())
                        && !Sentence.class.getName().equals(l.getName()))
                .collect(Collectors.toCollection(ArrayList::new));

        // Further narrow down the candidates by removing all layers which are already the target
        // of an attachment
        for (AnnotationLayer l : allLayers) {
            if (l.getAttachType() != null) {
                // If an attach-feature is configured, then remove the layer to which this feature
                // points from the candidate list
                if (l.getAttachFeature() != null) {
                    AnnotationLayer actualAttachLayer = annotationService.findLayer(project,
                            l.getAttachFeature().getType());
                    candidateLayers.remove(actualAttachLayer);
                }
                // If no attach-feature is configured, the the attach-layer is the target layer of
                // the attachment, so remove it from the candidate list
                else {
                    candidateLayers.remove(l.getAttachType());
                }
            }
        }

        return candidateLayers;
    }

    private boolean isLayerDeletable(AnnotationLayer aLayer)
    {
        return annotationService.listAttachedRelationLayers(aLayer).isEmpty()
                && annotationService.listAttachedLinkFeatures(aLayer).isEmpty();
    }

    private void actionDelete(AjaxRequestTarget aTarget, Form<AnnotationLayer> aForm)
    {
        confirmationDialog.setChallengeModel(new StringResourceModel("DeleteLayerDialog.text", this)
                .setParameters(escapeMarkup(getModelObject().getName())));
        confirmationDialog.setResponseModel(Model.of(getModelObject().getName()));
        confirmationDialog.show(aTarget);

        confirmationDialog.setConfirmAction((_target) -> {
            annotationService.removeLayer(getModelObject());

            Project project = getModelObject().getProject();

            setModelObject(null);

            documentService.upgradeAllAnnotationDocuments(project);

            // Trigger LayerConfigurationChangedEvent
            applicationEventPublisherHolder.get()
                    .publishEvent(new LayerConfigurationChangedEvent(this, project));

            _target.add(getPage());
        });
    }

    private void actionSave(AjaxRequestTarget aTarget, Form<AnnotationLayer> aForm)
    {
        aTarget.add(getParent());
        aTarget.addChildren(getPage(), IFeedback.class);

        AnnotationLayer layer = aForm.getModelObject();

        final Project project = layer.getProject();

        // Set type name only when the layer is initially created. After that, only the UI
        // name may be updated. Also any validation related to the type name only needs to
        // happen on the initial creation.
        boolean isNewLayer = isNull(layer.getId());
        if (isNewLayer) {
            String layerName = StringUtils.capitalize(layer.getUiName());
            layerName = layerName.replaceAll("\\W", "");

            if (layerName.isEmpty()) {
                error("Unable to derive internal name from [" + layer.getUiName()
                        + "]. Please choose a different initial name and rename after the "
                        + "layer has been created.");
                return;
            }

            if (!Character.isJavaIdentifierStart(layerName.charAt(0))) {
                error("Initial layer name cannot start with [" + layerName.charAt(0)
                        + "]. Please choose a different initial name and rename after the "
                        + "layer has been created.");
                return;
            }

            if (annotationService.existsLayer(TYPE_PREFIX + layerName, project)) {
                error("A layer with the name [" + TYPE_PREFIX + layerName
                        + "] already exists in this project.");
                return;
            }

            layer.setName(TYPE_PREFIX + layerName);
        }

        if (layer.getType().equals(RELATION_TYPE) && layer.getAttachType() == null) {
            error("A relation layer needs to attach to a span layer.");
            return;
        }

        annotationService.createOrUpdateLayer(layer);

        // Initialize default features if necessary but only after the layer has actually been
        // persisted in the database.
        if (isNewLayer) {
            TypeAdapter adapter = annotationService.getAdapter(layer);
            adapter.initializeLayerConfiguration(annotationService);
        }

        success("Settings for layer [" + layer.getUiName() + "] saved.");
        aTarget.addChildren(getPage(), IFeedback.class);
        aTarget.add(findParent(ProjectLayersPanel.class));
        aTarget.add(featureDetailForm);
        aTarget.add(featureSelectionForm);

        // Trigger LayerConfigurationChangedEvent
        applicationEventPublisherHolder.get()
                .publishEvent(new LayerConfigurationChangedEvent(this, project));
    }

    private void actionCancel(AjaxRequestTarget aTarget)
    {
        setModelObject(null);
        featureDetailForm.setModelObject(null);

        aTarget.add(getParent());
        aTarget.addChildren(getPage(), IFeedback.class);
    }

    private String getExportLayerFileName()
    {
        switch (exportMode) {
        case JSON:
            return "layer.json";
        case UIMA:
            return "typesystem.xml";
        default:
            throw new IllegalStateException("Unknown mode: [" + exportMode + "]");
        }
    }

    private IResourceStream exportLayer()
    {
        switch (exportMode) {
        case JSON:
            return exportLayerJson();
        case UIMA:
            return exportUimaTypeSystem();
        default:
            throw new IllegalStateException("Unknown mode: [" + exportMode + "]");
        }
    }

    private IResourceStream exportUimaTypeSystem()
    {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            TypeSystemDescription tsd = annotationService
                    .getAllProjectTypes(getModelObject().getProject());
            tsd.toXML(bos);
            return new InputStreamResourceStream(new ByteArrayInputStream(bos.toByteArray()));
        }
        catch (Exception e) {
            error("Unable to generate the UIMA type system file: "
                    + ExceptionUtils.getRootCauseMessage(e));
            ProjectLayersPanel.LOG.error("Unable to generate the UIMA type system file", e);
            RequestCycle.get().find(IPartialPageRequestHandler.class)
                    .ifPresent(handler -> handler.addChildren(getPage(), IFeedback.class));
            return null;
        }
    }

    private IResourceStream exportLayerJson()
    {
        try {
            AnnotationLayer layer = getModelObject();

            List<ExportedAnnotationLayer> exLayers = new ArrayList<>();

            ExportedAnnotationLayer exMainLayer = ImportUtil.exportLayerDetails(null, null, layer,
                    annotationService);
            exLayers.add(exMainLayer);

            // If the layer is attached to another layer, then we also have to export
            // that, otherwise we would be missing it during re-import.
            if (layer.getAttachType() != null) {
                AnnotationLayer attachLayer = layer.getAttachType();
                ExportedAnnotationLayer exAttachLayer = ImportUtil.exportLayerDetails(null, null,
                        attachLayer, annotationService);
                exMainLayer.setAttachType(
                        new ExportedAnnotationLayerReference(exAttachLayer.getName()));
                exLayers.add(exAttachLayer);
            }

            return new InputStreamResourceStream(new ByteArrayInputStream(
                    JSONUtil.toPrettyJsonString(exLayers).getBytes("UTF-8")));
        }
        catch (Exception e) {
            error("Unable to generate the JSON file: " + ExceptionUtils.getRootCauseMessage(e));
            ProjectLayersPanel.LOG.error("Unable to generate the JSON file", e);
            RequestCycle.get().find(IPartialPageRequestHandler.class)
                    .ifPresent(handler -> handler.addChildren(getPage(), IFeedback.class));
            return null;
        }
    }

    @Override
    protected void onModelChanged()
    {
        super.onModelChanged();

        // Since feature type uses a lambda model, it needs to be notified explicitly.
        layerTypeSelect.modelChanged();
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        setVisible(getModelObject() != null);
    }

    public Component getInitialFocusComponent()
    {
        return uiName;
    }
}

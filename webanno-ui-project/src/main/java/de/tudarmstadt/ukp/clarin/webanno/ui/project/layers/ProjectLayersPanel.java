/*
 * Copyright 2012
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
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.NoResultException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.extensions.markup.html.form.select.Select;
import org.apache.wicket.extensions.markup.html.form.select.SelectOption;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureType;
import de.tudarmstadt.ukp.clarin.webanno.api.event.LayerConfigurationChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.export.ImportUtil;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationLayerReference;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.clarin.webanno.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.AjaxDownloadLink;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.InputStreamResourceStream;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;
import de.tudarmstadt.ukp.clarin.webanno.xmi.TypeSystemAnalysis;
import de.tudarmstadt.ukp.clarin.webanno.xmi.TypeSystemAnalysis.RelationDetails;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.SurfaceForm;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * A Panel Used to add Layers to a selected {@link Project} in the project settings page
 */
@ProjectSettingsPanel(label = "Layers", prio = 300)
public class ProjectLayersPanel
    extends ProjectSettingsPanelBase
{
    private static final Logger LOG = LoggerFactory.getLogger(ProjectLayersPanel.class);
    private static final long serialVersionUID = -7870526462864489252L;

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean ProjectService repository;
    private @SpringBean UserDao userRepository;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisherHolder;

    private final String FIRST = "first";
    private final String NEXT = "next";

    private LayerSelectionForm layerSelectionForm;
    private FeatureSelectionForm featureSelectionForm;
    private LayerDetailForm layerDetailForm;
    private final FeatureDetailForm featureDetailForm;
    private final ImportLayerForm importLayerForm;
    private Select<AnnotationLayer> layerSelection;

    private String layerType = WebAnnoConst.SPAN_TYPE;

    public ProjectLayersPanel(String id, final IModel<Project> aProjectModel)
    {
        super(id, aProjectModel);
        setOutputMarkupId(true);

        layerSelectionForm = new LayerSelectionForm("layerSelectionForm");

        featureSelectionForm = new FeatureSelectionForm("featureSelectionForm");
        featureSelectionForm.setVisible(false);
        featureSelectionForm.setOutputMarkupPlaceholderTag(true);

        layerDetailForm = new LayerDetailForm("layerDetailForm");

        featureDetailForm = new FeatureDetailForm("featureDetailForm");
        featureDetailForm.setVisible(false);
        featureDetailForm.setOutputMarkupPlaceholderTag(true);

        add(layerSelectionForm);
        add(featureSelectionForm);
        add(layerDetailForm);
        add(featureDetailForm);

        importLayerForm = new ImportLayerForm("importLayerForm");
        add(importLayerForm);
    }

    @Override
    protected void onModelChanged()
    {
        super.onModelChanged();
        layerSelectionForm.getModelObject().layerSelection = null;

        layerDetailForm.setModelObject(null);

        featureSelectionForm.getModelObject().feature = null;
        featureSelectionForm.setVisible(false);

        featureDetailForm.setModelObject(null);
    }

    private class LayerSelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;

        public LayerSelectionForm(String id)
        {
            super(id, new CompoundPropertyModel<>(new SelectionModel()));

            add(new Button("create", new StringResourceModel("label"))
            {
                private static final long serialVersionUID = -4482428496358679571L;

                @Override
                public void onSubmit()
                {
                    LayerSelectionForm.this.getModelObject().layerSelection = null;

                    layerDetailForm.setModelObject(new AnnotationLayer());
                    featureDetailForm.setModelObject(null);

                    featureSelectionForm.setVisible(false);
                }
            });

            final Map<AnnotationLayer, String> colors = new HashMap<>();

            layerSelection = new Select<>("layerSelection");
            ListView<AnnotationLayer> layers = new ListView<AnnotationLayer>("layers",
                    new LoadableDetachableModel<List<AnnotationLayer>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<AnnotationLayer> load()
                        {
                            Project project = ProjectLayersPanel.this.getModelObject();

                            if (project.getId() != null) {
                                List<AnnotationLayer> _layers = annotationService
                                        .listAnnotationLayer(project);
                                AnnotationLayer tokenLayer = annotationService
                                        .getLayer(Token.class.getName(), project);
                                _layers.remove(tokenLayer);
                                for (AnnotationLayer layer : _layers) {
                                    if (layer.isBuiltIn() && layer.isEnabled()) {
                                        colors.put(layer, "green");
                                    }
                                    else if (layer.isEnabled()) {
                                        colors.put(layer, "blue");
                                    }
                                    else {
                                        colors.put(layer, "red");
                                    }
                                }
                                return _layers;
                            }
                            return new ArrayList<>();
                        }
                    })
            {
                private static final long serialVersionUID = 8901519963052692214L;

                @Override
                protected void populateItem(final ListItem<AnnotationLayer> item)
                {
                    item.add(new SelectOption<AnnotationLayer>("layer",
                            new Model<>(item.getModelObject()))
                    {
                        private static final long serialVersionUID = 3095089418860168215L;

                        @Override
                        public void onComponentTagBody(MarkupStream markupStream,
                                ComponentTag openTag)
                        {
                            replaceComponentTagBody(markupStream, openTag,
                                    item.getModelObject().getUiName());
                        }
                    }.add(new AttributeModifier("style",
                            "color:" + colors.get(item.getModelObject()) + ";")));
                }
            };
            add(layerSelection.add(layers));
            layerSelection.setOutputMarkupId(true);
            layerSelection.add(new OnChangeAjaxBehavior()
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    layerDetailForm.setModelObject(getModelObject().layerSelection);

                    LayerSelectionForm.this.setVisible(true);

                    featureSelectionForm.clearInput();
                    featureSelectionForm.setVisible(true);

                    featureDetailForm.setModelObject(null);

                    layerType = getModelObject().layerSelection.getType();

                    aTarget.add(layerDetailForm);
                    aTarget.add(featureSelectionForm);
                    aTarget.add(featureDetailForm);
                }
            });
        }
    }

    private class ImportLayerForm
        extends Form<String>
    {
        private static final long serialVersionUID = -7777616763931128598L;

        private FileUploadField fileUpload;

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public ImportLayerForm(String id)
        {
            super(id);
            add(fileUpload = new FileUploadField("content", new Model()));
            add(new LambdaAjaxButton("import", this::actionImport));
        }

        private void actionImport(AjaxRequestTarget aTarget, Form<String> aForm)
        {
            List<FileUpload> uploadedFiles = fileUpload.getFileUploads();
            Project project = ProjectLayersPanel.this.getModelObject();

            if (isEmpty(uploadedFiles)) {
                error("Please choose file with layer details before uploading");
                return;
            }
            else if (isNull(project.getId())) {
                error("Project not yet created, please save project details!");
                return;
            }
            for (FileUpload uploadedFile : uploadedFiles) {
                try (BufferedInputStream bis = IOUtils.buffer(uploadedFile.getInputStream())) {
                    byte[] buf = new byte[5];
                    bis.mark(buf.length + 1);
                    bis.read(buf, 0, buf.length);
                    bis.reset();

                    // If the file starts with an XML preamble, then we assume it is an UIMA
                    // type system file.
                    if (Arrays.equals(buf, new byte[] { '<', '?', 'x', 'm', 'l' })) {
                        importUimaTypeSystemFile(bis);
                    }
                    else {
                        importLayerFile(bis);
                    }
                }
                catch (Exception e) {
                    error("Error importing layers: " + ExceptionUtils.getRootCauseMessage(e));
                    aTarget.addChildren(getPage(), IFeedback.class);
                    LOG.error("Error importing layers", e);
                }
            }
            featureDetailForm.setVisible(false);
            aTarget.add(ProjectLayersPanel.this);
        }

        private void importUimaTypeSystemFile(InputStream aIS)
            throws IOException, InvalidXMLException, ResourceInitializationException
        {
            Project project = ProjectLayersPanel.this.getModelObject();
            TypeSystemDescription tsd = UIMAFramework.getXMLParser()
                    .parseTypeSystemDescription(new XMLInputSource(aIS, null));
            TypeSystemAnalysis analysis = TypeSystemAnalysis.of(tsd);
            for (AnnotationLayer l : analysis.getLayers()) {
                if (!annotationService.existsLayer(l.getName(), project)) {
                    l.setProject(project);

                    // Need to set the attach type
                    if (WebAnnoConst.RELATION_TYPE.equals(l.getType())) {
                        RelationDetails relDetails = analysis.getRelationDetails(l.getName());

                        AnnotationLayer attachLayer;
                        try {
                            // First check if this type is already in the project
                            attachLayer = annotationService.getLayer(relDetails.getAttachLayer(),
                                    project);
                        }
                        catch (NoResultException e) {
                            // If it does not exist in the project yet, then we create it
                            attachLayer = analysis.getLayer(relDetails.getAttachLayer());
                            attachLayer.setProject(project);
                            annotationService.createLayer(attachLayer);
                        }

                        l.setAttachType(attachLayer);
                    }

                    annotationService.createLayer(l);
                }

                // Import the features for the layer except if the layer is a built-in layer.
                // We must not touch the built-in layers because WebAnno may rely on their
                // structure. This is a conservative measure for now any may be relaxed in the
                // future.
                AnnotationLayer persistedLayer = annotationService.getLayer(l.getName(), project);
                if (!persistedLayer.isBuiltIn()) {
                    for (AnnotationFeature f : analysis.getFeatures(l.getName())) {
                        if (!annotationService.existsFeature(f.getName(), persistedLayer)) {
                            f.setProject(project);
                            f.setLayer(persistedLayer);
                            annotationService.createFeature(f);
                        }
                    }
                }
            }
        }

        private void importLayerFile(InputStream aIS) throws IOException
        {
            User user = userRepository.getCurrentUser();
            Project project = ProjectLayersPanel.this.getModelObject();
            
            String text = IOUtils.toString(aIS, "UTF-8");

            ExportedAnnotationLayer[] exLayers = JSONUtil.getObjectMapper().readValue(text,
                    ExportedAnnotationLayer[].class);
            
            // First import the layers but without setting the attach-layers/features
            Map<String, ExportedAnnotationLayer> exLayersMap = new HashMap<>();
            Map<String, AnnotationLayer> layersMap = new HashMap<>();
            for (ExportedAnnotationLayer exLayer : exLayers) {
                AnnotationLayer layer = createLayer(exLayer, user);
                layersMap.put(layer.getName(), layer);
                exLayersMap.put(layer.getName(), exLayer);
            }
            
            // Second fill in the attach-layer and attach-feature information
            for (AnnotationLayer layer : layersMap.values()) {
                ExportedAnnotationLayer exLayer = exLayersMap.get(layer.getName());
                if (exLayer.getAttachType() != null) {
                    layer.setAttachType(layersMap.get(exLayer.getAttachType().getName()));
                }
                if (exLayer.getAttachFeature() != null) {
                    AnnotationLayer attachLayer = annotationService.getLayer(
                            exLayer.getAttachType().getName(), project);
                    AnnotationFeature attachFeature = annotationService
                            .getFeature(exLayer.getAttachFeature().getName(), attachLayer);
                    layer.setAttachFeature(attachFeature);
                }
                annotationService.createLayer(layer);
            }
            
            layerDetailForm.setModelObject(layersMap.get(exLayers[0].getName()));
            featureSelectionForm.setVisible(true);
        }

        private AnnotationLayer createLayer(ExportedAnnotationLayer aExLayer, User aUser)
            throws IOException
        {
            Project project = ProjectLayersPanel.this.getModelObject();
            AnnotationLayer layer;
            
            if (annotationService.existsLayer(aExLayer.getName(), aExLayer.getType(), project)) {
                layer = annotationService.getLayer(aExLayer.getName(), project);
                ImportUtil.setLayer(annotationService, layer, aExLayer, project, aUser);
            }
            else {
                layer = new AnnotationLayer();
                ImportUtil.setLayer(annotationService, layer, aExLayer, project, aUser);
            }
            
            for (ExportedAnnotationFeature exfeature : aExLayer.getFeatures()) {
                ExportedTagSet exTagset = exfeature.getTagSet();
                TagSet tagSet = null;
                if (exTagset != null
                        && annotationService.existsTagSet(exTagset.getName(), project)) {
                    tagSet = annotationService.getTagSet(exTagset.getName(), project);
                    ImportUtil.createTagSet(tagSet, exTagset, project, aUser, annotationService);
                }
                else if (exTagset != null) {
                    tagSet = new TagSet();
                    ImportUtil.createTagSet(tagSet, exTagset, project, aUser, annotationService);
                }
                if (annotationService.existsFeature(exfeature.getName(), layer)) {
                    AnnotationFeature feature = annotationService.getFeature(exfeature.getName(),
                            layer);
                    feature.setTagset(tagSet);
                    ImportUtil.setFeature(annotationService, feature, exfeature, project, aUser);
                    continue;
                }
                AnnotationFeature feature = new AnnotationFeature();
                feature.setLayer(layer);
                feature.setTagset(tagSet);
                ImportUtil.setFeature(annotationService, feature, exfeature, project, aUser);
            }
            
            return layer;
        }
    }

    public class SelectionModel
        implements Serializable
    {
        private static final long serialVersionUID = -1L;

        private AnnotationLayer layerSelection;
        public AnnotationFeature feature;
    }

    private static enum LayerExportMode
    {
        JSON, UIMA
    }

    private class LayerDetailForm
        extends Form<AnnotationLayer>
    {
        private static final long serialVersionUID = -1L;

        private static final String TYPE_PREFIX = "webanno.custom.";
        private DropDownChoice<String> layerTypes;
        private DropDownChoice<AnnotationLayer> attachTypes;

        private CheckBox lockToTokenOffset;
        private CheckBox allowStacking;
        private CheckBox crossSentence;
        private CheckBox showTextInHover;
        private CheckBox multipleTokens;
        private CheckBox linkedListBehavior;

        private LayerExportMode exportMode = LayerExportMode.JSON;

        public LayerDetailForm(String id)
        {
            super(id, CompoundPropertyModel.of(Model.of()));

            setOutputMarkupPlaceholderTag(true);

            add(new TextField<String>("uiName").setRequired(true));
            add(new TextArea<String>("description").setOutputMarkupPlaceholderTag(true));

            add(new Label("name")
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onConfigure()
                {
                    setVisible(StringUtils
                            .isNotBlank(LayerDetailForm.this.getModelObject().getName()));
                }
            });

            add(new CheckBox("enabled"));
            add(layerTypes = (DropDownChoice<String>) new DropDownChoice<String>("type",
                    Arrays.asList(new String[] { SPAN_TYPE, RELATION_TYPE, CHAIN_TYPE }))
            {
                private static final long serialVersionUID = 1244555334843130802L;

                @Override
                public boolean isEnabled()
                {
                    return isNull(LayerDetailForm.this.getModelObject().getId());
                }
            }.setRequired(true));
            layerTypes.add(new AjaxFormComponentUpdatingBehavior("change")
            {
                private static final long serialVersionUID = 6790949494089940303L;

                @Override
                protected void onUpdate(AjaxRequestTarget target)
                {
                    layerType = getModelObject().getType();
                    target.add(lockToTokenOffset);
                    target.add(allowStacking);
                    target.add(crossSentence);
                    target.add(showTextInHover);
                    target.add(multipleTokens);
                    target.add(linkedListBehavior);
                    target.add(attachTypes);
                }
            });

            attachTypes = new DropDownChoice<AnnotationLayer>("attachType")
            {
                private static final long serialVersionUID = -6705445053442011120L;

                {
                    setChoices(new LoadableDetachableModel<List<AnnotationLayer>>()
                    {
                        private static final long serialVersionUID = 1784646746122513331L;

                        @Override
                        protected List<AnnotationLayer> load()
                        {
                            List<AnnotationLayer> allLayers = annotationService
                                    .listAnnotationLayer(ProjectLayersPanel.this.getModelObject());

                            if (LayerDetailForm.this.getModelObject().getId() != null) {
                                if (LayerDetailForm.this.getModelObject().getAttachType() == null) {
                                    return new ArrayList<>();
                                }

                                return Arrays.asList(
                                        LayerDetailForm.this.getModelObject().getAttachType());
                            }
                            if (!layerType.equals(RELATION_TYPE)) {
                                return new ArrayList<>();
                            }

                            List<AnnotationLayer> attachTeypes = new ArrayList<>();
                            // remove a span layer which is already used as attach type for the
                            // other
                            List<AnnotationLayer> usedLayers = new ArrayList<>();
                            for (AnnotationLayer layer : allLayers) {
                                if (layer.getAttachType() != null) {
                                    usedLayers.add(layer.getAttachType());
                                }
                            }
                            allLayers.removeAll(usedLayers);

                            for (AnnotationLayer layer : allLayers) {
                                if (layer.getType().equals(SPAN_TYPE) && !layer.isBuiltIn()) {
                                    attachTeypes.add(layer);
                                }
                            }

                            return attachTeypes;
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<>("uiName"));
                }

                @Override
                protected void onConfigure()
                {
                    setEnabled(isNull(LayerDetailForm.this.getModelObject().getId()));
                    setNullValid(isVisible());
                }
            };
            attachTypes.setOutputMarkupPlaceholderTag(true);
            add(attachTypes);

            // Behaviors of layers
            add(new CheckBox("readonly"));

            add(lockToTokenOffset = new CheckBox("lockToTokenOffset")
            {
                private static final long serialVersionUID = -4934708834659137207L;

                {
                    setOutputMarkupPlaceholderTag(true);
                }

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    AnnotationLayer layer = LayerDetailForm.this.getModelObject();
                    // Makes no sense for relation layers or layers that attach to tokens
                    setVisible(!isBlank(layer.getType()) && !RELATION_TYPE.equals(layer.getType())
                            && layer.getAttachFeature() == null);
                    setEnabled(
                            // Surface form must be locked to token boundaries for CONLL-U writer
                            // to work.
                            !SurfaceForm.class.getName().equals(layer.getName()) &&
                            // Not configurable for chains
                            !CHAIN_TYPE.equals(layer.getType()));
                }
            });

            add(allowStacking = new CheckBox("allowStacking")
            {
                private static final long serialVersionUID = 7800627916287273008L;

                {
                    setOutputMarkupPlaceholderTag(true);
                }

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    AnnotationLayer layer = LayerDetailForm.this.getModelObject();
                    setVisible(!isBlank(layer.getType()));
                    setEnabled(
                            // Surface form must be locked to token boundaries for CONLL-U writer
                            // to work.
                            !SurfaceForm.class.getName().equals(layer.getName()) &&
                            // Not configurable for chains
                            !CHAIN_TYPE.equals(layer.getType()) &&
                            // Not configurable for layers that attach to tokens (currently that is
                            // the only layer on which we use the attach feature)
                            layer.getAttachFeature() == null);
                }
            });

            add(crossSentence = new CheckBox("crossSentence")
            {
                private static final long serialVersionUID = -5986386642712152491L;

                {
                    setOutputMarkupPlaceholderTag(true);
                }

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    AnnotationLayer layer = LayerDetailForm.this.getModelObject();
                    setVisible(!isBlank(layer.getType()));
                    setEnabled(
                            // Surface form must be locked to token boundaries for CONLL-U writer
                            // to work.
                            !SurfaceForm.class.getName().equals(layer.getName()) &&
                            // Not configurable for chains
                            !CHAIN_TYPE.equals(layer.getType())
                            // Not configurable for layers that attach to tokens (currently that
                            // is the only layer on which we use the attach feature)
                                            && layer.getAttachFeature() == null);
                }
            });

            add(showTextInHover = new CheckBox("showTextInHover")
            {

                private static final long serialVersionUID = -7739913125218251672L;

                {
                    setOutputMarkupPlaceholderTag(true);
                }

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    AnnotationLayer layer = LayerDetailForm.this.getModelObject();
                    setVisible(!isBlank(layer.getType()) &&
                        // Not configurable for chains or relations
                        !CHAIN_TYPE.equals(layer.getType()) && 
                        !RELATION_TYPE.equals(layer.getType()));
                    setEnabled(
                            // Surface form must be locked to token boundaries for CONLL-U writer
                            // to work.
                            !SurfaceForm.class.getName().equals(layer.getName()));
                }
            });

            add(multipleTokens = (CheckBox) new CheckBox("multipleTokens")
                    .add(LambdaBehavior.onConfigure(_this -> {
                        AnnotationLayer layer = LayerDetailForm.this.getModelObject();
                        // Makes no sense for relations
                        _this.setVisible(!isBlank(layer.getType())
                                && !RELATION_TYPE.equals(layer.getType()));
                        _this.setEnabled(
                                // Surface form must be locked to token boundaries for CONLL-U
                                // writer to work.
                                !SurfaceForm.class.getName().equals(layer.getName()) &&
                                // Not configurable for chains
                                !CHAIN_TYPE.equals(layer.getType())
                                // Not configurable for layers that attach to tokens (currently
                                // that is the only layer on which we use the attach feature)
                                                && layer.getAttachFeature() == null);
                    })).setOutputMarkupPlaceholderTag(true));

            add(linkedListBehavior = new CheckBox("linkedListBehavior")
            {
                private static final long serialVersionUID = 1319818165277559402L;

                {
                    setOutputMarkupPlaceholderTag(true);
                }

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    AnnotationLayer layer = LayerDetailForm.this.getModelObject();
                    setVisible(!isBlank(layer.getType()) && CHAIN_TYPE.equals(layer.getType()));
                }
            });
            linkedListBehavior.add(new AjaxFormComponentUpdatingBehavior("change")
            {
                private static final long serialVersionUID = -2904306846882446294L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    featureSelectionForm.updateChoices();
                    aTarget.add(featureSelectionForm);
                    aTarget.add(featureDetailForm);
                }
            });

            add(new TextArea<String>("onClickJavascriptAction")
                    .add(new AttributeModifier("placeholder",
                            "alert($PARAM.PID + ' ' + $PARAM.PNAME + ' ' + $PARAM.DOCID + ' ' + "
                                    + "$PARAM.DOCNAME + ' ' + $PARAM.fieldname);")));

            add(new DropDownChoice<LayerExportMode>("exportMode",
                    new PropertyModel<LayerExportMode>(this, "exportMode"),
                    asList(LayerExportMode.values()), new EnumChoiceRenderer<>(this))
                            .add(new LambdaAjaxFormComponentUpdatingBehavior("change")));

            add(new AjaxDownloadLink("export",
                    new LambdaModel<>(this::getExportLayerFileName).autoDetaching(),
                    LambdaModel.of(this::exportLayer)));

            add(new LambdaAjaxButton<>("save", this::actionSave));
            add(new LambdaAjaxLink("cancel", this::actionCancel));
        }

        private void actionSave(AjaxRequestTarget aTarget, Form<?> aForm)
        {
            aTarget.add(ProjectLayersPanel.this);
            aTarget.addChildren(getPage(), IFeedback.class);

            AnnotationLayer layer = LayerDetailForm.this.getModelObject();

            if (layer.isLockToTokenOffset() && layer.isMultipleTokens()) {
                layer.setLockToTokenOffset(false);
            }

            final Project project = ProjectLayersPanel.this.getModelObject();
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

            layer.setProject(project);

            annotationService.createLayer(layer);
            
            // Initialize default features if necessary but only after the layer has actually been
            // persisted in the database.
            if (isNewLayer) {
                TypeAdapter adapter = annotationService.getAdapter(layer);
                adapter.initialize(annotationService);
            }

            featureSelectionForm.setVisible(true);

            // Trigger LayerConfigurationChangedEvent
            applicationEventPublisherHolder.get()
                    .publishEvent(new LayerConfigurationChangedEvent(this, project));
        }

        private void actionCancel(AjaxRequestTarget aTarget)
        {
            aTarget.add(ProjectLayersPanel.this);
            aTarget.addChildren(getPage(), IFeedback.class);

            layerSelectionForm.getModelObject().layerSelection = null;

            layerDetailForm.setModelObject(null);

            featureSelectionForm.setVisible(false);
            featureDetailForm.setModelObject(null);
        }

        private String getExportLayerFileName()
        {
            switch (exportMode) {
            case JSON:
                return "layer.json";
            case UIMA:
                return "typesytem.xml";
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
                        .getProjectTypes(ProjectLayersPanel.this.getModelObject());
                tsd.toXML(bos);
                return new InputStreamResourceStream(new ByteArrayInputStream(bos.toByteArray()));
            }
            catch (Exception e) {
                error("Unable to generate the UIMA type system file: "
                        + ExceptionUtils.getRootCauseMessage(e));
                LOG.error("Unable to generate the UIMA type system file", e);
                RequestCycle.get().find(IPartialPageRequestHandler.class)
                        .ifPresent(handler -> handler.addChildren(getPage(), IFeedback.class));
                return null;
            }
        }

        private IResourceStream exportLayerJson()
        {
            try {
                AnnotationLayer layer = layerDetailForm.getModelObject();

                List<ExportedAnnotationLayer> exLayers = new ArrayList<>();

                ExportedAnnotationLayer exMainLayer = ImportUtil.exportLayerDetails(null, null,
                        layer, annotationService);
                exLayers.add(exMainLayer);

                // If the layer is attached to another layer, then we also have to export
                // that, otherwise we would be missing it during re-import.
                if (layer.getAttachType() != null) {
                    AnnotationLayer attachLayer = layer.getAttachType();
                    ExportedAnnotationLayer exAttachLayer = ImportUtil.exportLayerDetails(null,
                            null, attachLayer, annotationService);
                    exMainLayer.setAttachType(
                            new ExportedAnnotationLayerReference(exAttachLayer.getName()));
                    exLayers.add(exAttachLayer);
                }

                return new InputStreamResourceStream(new ByteArrayInputStream(
                        JSONUtil.toPrettyJsonString(exLayers).getBytes("UTF-8")));
            }
            catch (Exception e) {
                error("Unable to generate the JSON file: " + ExceptionUtils.getRootCauseMessage(e));
                LOG.error("Unable to generate the JSON file", e);
                RequestCycle.get().find(IPartialPageRequestHandler.class)
                        .ifPresent(handler -> handler.addChildren(getPage(), IFeedback.class));
                return null;
            }
        }

        @Override
        protected void onConfigure()
        {
            super.onConfigure();

            setVisible(getModelObject() != null);
        }
    }

    private class FeatureDetailForm
        extends Form<AnnotationFeature>
    {
        private static final String MID_TRAITS_CONTAINER = "traitsContainer";
        private static final String MID_TRAITS = "traits";
        private static final long serialVersionUID = -1L;
        private DropDownChoice<FeatureType> featureType;
        private CheckBox required;
        private WebMarkupContainer traitsContainer;

        public FeatureDetailForm(String id)
        {
            super(id, CompoundPropertyModel.of(Model.of()));

            add(traitsContainer = new WebMarkupContainer(MID_TRAITS_CONTAINER));
            traitsContainer.setOutputMarkupId(true);

            add(new Label("name")
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onConfigure()
                {
                    setVisible(StringUtils
                            .isNotBlank(FeatureDetailForm.this.getModelObject().getName()));
                }
            });
            add(new TextField<String>("uiName").setRequired(true));
            add(new TextArea<String>("description").setOutputMarkupPlaceholderTag(true));
            add(new CheckBox("enabled"));
            add(new CheckBox("visible"));
            add(new CheckBox("includeInHover")
            {

                private static final long serialVersionUID = -8273152168889478682L;

                @Override
                protected void onConfigure()
                {
                    String layertype = layerDetailForm.getModelObject().getType();
                    // Currently not configurable for chains or relations
                    // TODO: technically it is possible
                    setVisible(!CHAIN_TYPE.equals(layertype) && !RELATION_TYPE.equals(layertype));
                }

            });
            add(new CheckBox("remember"));
            add(required = new CheckBox("required")
            {
                private static final long serialVersionUID = -2716373442353375910L;

                {
                    setOutputMarkupId(true);
                }

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    boolean relevant = CAS.TYPE_NAME_STRING
                            .equals(FeatureDetailForm.this.getModelObject().getType());
                    setEnabled(relevant);
                    if (!relevant) {
                        FeatureDetailForm.this.getModelObject().setRequired(false);
                    }
                }
            });
            add(new CheckBox("hideUnconstraintFeature"));

            add(featureType = new DropDownChoice<FeatureType>("type")
            {
                private static final long serialVersionUID = 9029205407108101183L;

                {
                    IModel<FeatureType> model = LambdaModelAdapter.of(() -> {
                        return featureSupportRegistry
                                .getFeatureType(featureDetailForm.getModelObject());
                    }, (v) -> FeatureDetailForm.this.getModelObject().setType(v.getName()));
                    setRequired(true);
                    setNullValid(false);
                    setChoiceRenderer(new ChoiceRenderer<>("uiName"));
                    setModel(model);
                }

                @Override
                protected void onConfigure()
                {
                    if (isNull(FeatureDetailForm.this.getModelObject().getId())) {
                        setEnabled(true);
                        setChoices(LambdaModel.of(() -> featureSupportRegistry
                                .getUserSelectableTypes(layerDetailForm.getModelObject())));
                    }
                    else {
                        setEnabled(false);
                        setChoices(LambdaModel.of(() -> featureSupportRegistry
                                .getAllTypes(layerDetailForm.getModelObject())));
                    }
                }

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

            add(new Button("save", new StringResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onAfterSubmit()
                {
                    // Processing the data in onAfterSubmit so the traits panel can use the
                    // override onSubmit in its nested form and store the traits before
                    // we clear the currently selected feature.

                    AnnotationFeature feature = FeatureDetailForm.this.getModelObject();
                    String name = feature.getUiName();
                    name = name.replaceAll("\\W", "");
                    // Check if feature name is not from the restricted names list
                    if (WebAnnoConst.RESTRICTED_FEATURE_NAMES.contains(name)) {
                        error("'" + feature.getUiName().toLowerCase() + " (" + name + ")'"
                                + " is a reserved feature name. Please use a different name "
                                + "for the feature.");
                        return;
                    }
                    if (RELATION_TYPE.equals(layerDetailForm.getModelObject().getType())
                            && (name.equals(WebAnnoConst.FEAT_REL_SOURCE)
                                    || name.equals(WebAnnoConst.FEAT_REL_TARGET)
                                    || name.equals(FIRST) || name.equals(NEXT))) {
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
                        feature.setLayer(layerDetailForm.getModelObject());
                        feature.setProject(ProjectLayersPanel.this.getModelObject());

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
                                .getFeatureSupport(featureDetailForm.featureType.getModelObject()
                                        .getFeatureSupportId());

                        // Let the feature support finalize the configuration of the feature
                        fs.configureFeature(feature);

                    }

                    // Save feature
                    annotationService.createFeature(feature);

                    // Clear currently selected feature / feature details
                    featureSelectionForm.getModelObject().feature = null;
                    featureDetailForm.setModelObject(null);

                    // Trigger LayerConfigurationChangedEvent
                    applicationEventPublisherHolder.get().publishEvent(
                            new LayerConfigurationChangedEvent(this, feature.getProject()));
                }
            });

            add(new Button("cancel", new StringResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                {
                    // Avoid saving data
                    setDefaultFormProcessing(false);
                }

                @Override
                public void onSubmit()
                {
                    // cancel selection of feature list
                    featureSelectionForm.feature.setModelObject(null);
                    featureDetailForm.setModelObject(null);
                }
            });
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
    }

    public class FeatureSelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;

        private ListChoice<AnnotationFeature> feature;

        public FeatureSelectionForm(String id)
        {
            super(id, new CompoundPropertyModel<>(new SelectionModel()));

            add(feature = new ListChoice<AnnotationFeature>("feature")
            {
                private static final long serialVersionUID = 1L;
                {
                    setChoices(regenerateModel());
                    setChoiceRenderer(new ChoiceRenderer<AnnotationFeature>()
                    {
                        private static final long serialVersionUID = 4610648616450168333L;

                        @Override
                        public Object getDisplayValue(AnnotationFeature aObject)
                        {
                            return aObject.getUiName() + " : ["
                                    + StringUtils.substringAfterLast(aObject.getType(), ".") + "]";
                        }
                    });
                    setNullValid(false);
                    add(new FormComponentUpdatingBehavior()
                    {
                        private static final long serialVersionUID = -2961708999353358452L;
                        
                        @Override
                        protected void onUpdate()
                        {
                            if (FeatureSelectionForm.this.getModelObject().feature != null) {
                                featureDetailForm.setVisible(true);
                            }
                        };
                    });
                }

                @Override
                protected CharSequence getDefaultChoice(String aSelectedValue)
                {
                    return "";
                }
            });

            add(new Button("new", new StringResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    // cancel selection of feature list
                    feature.setModelObject(null);

                    AnnotationFeature newFeature = new AnnotationFeature();
                    newFeature.setLayer(layerDetailForm.getModelObject());
                    newFeature.setProject(ProjectLayersPanel.this.getModelObject());
                    featureDetailForm.setDefaultModelObject(newFeature);
                }

                @Override
                public boolean isEnabled()
                {
                    return layerDetailForm.getModelObject() != null
                            && !layerDetailForm.getModelObject().isBuiltIn()
                            && !layerDetailForm.getModelObject().getType().equals(CHAIN_TYPE);
                }
            });
        }

        private LoadableDetachableModel<List<AnnotationFeature>> regenerateModel()
        {
            return new LoadableDetachableModel<List<AnnotationFeature>>()
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected List<AnnotationFeature> load()
                {
                    List<AnnotationFeature> features = annotationService
                            .listAnnotationFeature(layerDetailForm.getModelObject());
                    if (CHAIN_TYPE.equals(layerDetailForm.getModelObject().getType())
                            && !layerDetailForm.getModelObject().isLinkedListBehavior()) {
                        List<AnnotationFeature> filtered = new ArrayList<>();
                        for (AnnotationFeature f : features) {
                            if (!WebAnnoConst.COREFERENCE_RELATION_FEATURE.equals(f.getName())) {
                                filtered.add(f);
                            }
                        }
                        return filtered;
                    }
                    else {
                        return features;
                    }
                }
            };
        }

        public void updateChoices()
        {
            feature.setChoices(regenerateModel());
        }
    }
}

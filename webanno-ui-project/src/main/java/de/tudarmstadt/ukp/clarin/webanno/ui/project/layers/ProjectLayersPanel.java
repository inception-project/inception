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
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.COREFERENCE_RELATION_FEATURE;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhen;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.markup.html.form.select.Select;
import org.apache.wicket.extensions.markup.html.form.select.SelectOption;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.export.ImportUtil;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedAnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * A Panel Used to add Layers to a selected {@link Project} in the project settings page
 */
public class ProjectLayersPanel
    extends ProjectSettingsPanelBase
{
    static final Logger LOG = LoggerFactory.getLogger(ProjectLayersPanel.class);
    private static final long serialVersionUID = -7870526462864489252L;

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean ProjectService repository;
    private @SpringBean UserDao userRepository;
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean LayerSupportRegistry layerSupportRegistry;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisherHolder;

    private LayerSelectionForm layerSelectionForm;
    private FeatureSelectionForm featureSelectionForm;
    private LayerDetailForm layerDetailForm;
    private final FeatureDetailForm featureDetailForm;
    private final ImportLayerForm importLayerForm;
    private Select<AnnotationLayer> layerSelection;
    
    private IModel<AnnotationLayer> selectedLayer;
    private IModel<AnnotationFeature> selectedFeature;

    public ProjectLayersPanel(String id, final IModel<Project> aProjectModel)
    {
        super(id, aProjectModel);
        setOutputMarkupId(true);

        selectedLayer = Model.of();
        selectedFeature = Model.of();

        featureSelectionForm = new FeatureSelectionForm("featureSelectionForm", selectedFeature);
        featureDetailForm = new FeatureDetailForm("featureDetailForm", selectedFeature);

        layerSelectionForm = new LayerSelectionForm("layerSelectionForm", selectedLayer);
        layerDetailForm = new LayerDetailForm("layerDetailForm", selectedLayer,
                featureSelectionForm, featureDetailForm);

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
        
        layerDetailForm.setModelObject(null);
        featureDetailForm.setModelObject(null);
    }

    private class LayerSelectionForm
        extends Form<AnnotationLayer>
    {
        private static final long serialVersionUID = -1L;

        public LayerSelectionForm(String id, IModel<AnnotationLayer> aModel)
        {
            super(id, aModel);

            add(new Button("create", new StringResourceModel("label"))
            {
                private static final long serialVersionUID = -4482428496358679571L;

                @Override
                public void onSubmit()
                {
                    AnnotationLayer layer = new AnnotationLayer();
                    layer.setProject(ProjectLayersPanel.this.getModelObject());
                    
                    layerDetailForm.setModelObject(layer);
                    featureDetailForm.setModelObject(null);
                }
            });

            final Map<AnnotationLayer, String> colors = new HashMap<>();

            layerSelection = new Select<>("layerSelection", aModel);
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
            layerSelection.add(OnChangeAjaxBehavior.onChange(_target -> {
                featureDetailForm.setModelObject(null);

                _target.add(layerDetailForm);
                _target.add(featureSelectionForm);
                _target.add(featureDetailForm);
            }));
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
            
            annotationService.importUimaTypeSystem(project, tsd);
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

    static enum LayerExportMode
    {
        JSON, UIMA
    }

    public class FeatureSelectionForm
        extends Form<AnnotationFeature>
    {
        private static final String CID_CREATE_FEATURE = "createFeature";
        
        private static final long serialVersionUID = -1L;

        public FeatureSelectionForm(String id, IModel<AnnotationFeature> aModel)
        {
            super(id, aModel);

            setOutputMarkupPlaceholderTag(true);
            
            add(new ListChoice<AnnotationFeature>("feature")
            {
                private static final long serialVersionUID = 1L;
                {
                    setChoices(FeatureSelectionForm.this::listFeatures);
                    setModel(aModel);
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
                            // list and detail panel share the same model, but they are not
                            // automatically notified of updates to the model unless the 
                            // updates go through their respective setModelObject() calls
                            featureDetailForm.modelChanged();
                        };
                    });
                }

                @Override
                protected CharSequence getDefaultChoice(String aSelectedValue)
                {
                    return "";
                }
            });
            
            LambdaAjaxLink createButton = new LambdaAjaxLink(CID_CREATE_FEATURE,
                    this::actionCreateFeature);
            createButton.add(enabledWhen(() -> 
                    layerDetailForm.getModelObject() != null
                    && !layerDetailForm.getModelObject().isBuiltIn()
                    && !layerDetailForm.getModelObject().getType().equals(CHAIN_TYPE)
                
            ));
            add(createButton);
        }
        
        private void actionCreateFeature(AjaxRequestTarget aTarget)
        {
            // cancel selection of feature list
            selectedFeature.setObject(null);

            AnnotationFeature newFeature = new AnnotationFeature();
            newFeature.setLayer(layerDetailForm.getModelObject());
            newFeature.setProject(ProjectLayersPanel.this.getModelObject());
            featureDetailForm.setDefaultModelObject(newFeature);
            
            aTarget.add(featureDetailForm);
            aTarget.focusComponent(featureDetailForm.getInitialFocusComponent());
        }

        private List<AnnotationFeature> listFeatures()
        {
            List<AnnotationFeature> features = annotationService
                    .listAnnotationFeature(layerDetailForm.getModelObject());
            if (CHAIN_TYPE.equals(layerDetailForm.getModelObject().getType())
                    && !layerDetailForm.getModelObject().isLinkedListBehavior()) {
                List<AnnotationFeature> filtered = new ArrayList<>();
                for (AnnotationFeature f : features) {
                    if (!COREFERENCE_RELATION_FEATURE.equals(f.getName())) {
                        filtered.add(f);
                    }
                }
                return filtered;
            }
            else {
                return features;
            }
        }
        
        @Override
        protected void onConfigure()
        {
            super.onConfigure();
            
            setVisible(selectedLayer.getObject() != null
                    && nonNull(selectedLayer.getObject().getId()));
        }
    }
}

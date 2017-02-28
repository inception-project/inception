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
package de.tudarmstadt.ukp.clarin.webanno.ui.project;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CHAIN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.COREFERENCE_RELATION_FEATURE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.COREFERENCE_TYPE_FEATURE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static java.util.Arrays.asList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.uima.cas.CAS;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.markup.html.form.select.Select;
import org.apache.wicket.extensions.markup.html.form.select.SelectOption;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.EntityModel;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.SurfaceForm;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * A Panel Used to add Layers to a selected {@link Project} in the project settings page
 *
 *
 */

public class ProjectLayersPanel
    extends Panel
{
    private static final long serialVersionUID = -7870526462864489252L;
    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "userRepository")
    private UserDao userRepository;

    private final String FIRST = "first";
    private final String NEXT = "next";

    private LayerSelectionForm layerSelectionForm;
    private FeatureSelectionForm featureSelectionForm;
    private LayerDetailForm layerDetailForm;
    private final FeatureDetailForm featureDetailForm;
    private final ImportLayerForm importLayerForm;
    private Select<AnnotationLayer> layerSelection;

    private final IModel<Project> selectedProjectModel;

    private final static List<String> PRIMITIVE_TYPES = asList(CAS.TYPE_NAME_STRING,
            CAS.TYPE_NAME_INTEGER, CAS.TYPE_NAME_FLOAT, CAS.TYPE_NAME_BOOLEAN);

    private List<String> spanTypes = new ArrayList<String>();
    private String layerType = WebAnnoConst.SPAN_TYPE;
    private List<FileUpload> uploadedFiles;
    private FileUploadField fileUpload;

    public ProjectLayersPanel(String id, final IModel<Project> aProjectModel)
    {
        super(id);
        this.selectedProjectModel = aProjectModel;
        layerSelectionForm = new LayerSelectionForm("layerSelectionForm");

        featureSelectionForm = new FeatureSelectionForm("featureSelectionForm");
        featureSelectionForm.setVisible(false);
        featureSelectionForm.setOutputMarkupPlaceholderTag(true);

        layerDetailForm = new LayerDetailForm("layerDetailForm");
        layerDetailForm.setVisible(false);
        layerDetailForm.setOutputMarkupPlaceholderTag(true);

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

    private class LayerSelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;

        public LayerSelectionForm(String id)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

            add(new Button("create", new ResourceModel("label"))
            {
                private static final long serialVersionUID = -4482428496358679571L;

                @Override
                public void onSubmit()
                {
                    if (selectedProjectModel.getObject().getId() == 0) {
                        error("Project not yet created. Please save project details first!");
                    }
                    else {
                        LayerSelectionForm.this.getModelObject().layerSelection = null;
                        layerDetailForm.setModelObject(new AnnotationLayer());
                        layerDetailForm.setVisible(true);
                        featureSelectionForm.setVisible(false);
                        featureDetailForm.setVisible(false);
                    }
                }
            });

            final Map<AnnotationLayer, String> colors = new HashMap<AnnotationLayer, String>();

            layerSelection = new Select<AnnotationLayer>("layerSelection");
            ListView<AnnotationLayer> layers = new ListView<AnnotationLayer>("layers",
                    new LoadableDetachableModel<List<AnnotationLayer>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<AnnotationLayer> load()
                        {
                            Project project = selectedProjectModel.getObject();

                            if (project.getId() != 0) {
                                List<AnnotationLayer> layers = annotationService
                                        .listAnnotationLayer(project);
                                AnnotationLayer tokenLayer = annotationService.getLayer(
                                        Token.class.getName(), project);
                                layers.remove(tokenLayer);
                                for (AnnotationLayer layer : layers) {
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
                                return layers;
                            }
                            return new ArrayList<AnnotationLayer>();
                        }
                    })
            {
                private static final long serialVersionUID = 8901519963052692214L;

                @Override
                protected void populateItem(final ListItem<AnnotationLayer> item)
                {
                    item.add(new SelectOption<AnnotationLayer>("layer", new Model<AnnotationLayer>(
                            item.getModelObject()))
                    {
                        private static final long serialVersionUID = 3095089418860168215L;

                        @Override
                        public void onComponentTagBody(MarkupStream markupStream,
                                ComponentTag openTag)
                        {
                            replaceComponentTagBody(markupStream, openTag, item.getModelObject()
                                    .getUiName());
                        }
                    }.add(new AttributeModifier("style", "color:"
                            + colors.get(item.getModelObject()) + ";")));
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
                    layerDetailForm.setVisible(true);

                    LayerSelectionForm.this.setVisible(true);
                    featureSelectionForm.clearInput();
                    featureSelectionForm.setVisible(true);
                    layerDetailForm.setVisible(true);
                    featureDetailForm.setVisible(false);

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

        @SuppressWarnings({ "unchecked", "rawtypes" })
        public ImportLayerForm(String id)
        {
            super(id);
            add(fileUpload = new FileUploadField("content", new Model()));
            add(new Button("import", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    uploadedFiles = fileUpload.getFileUploads();
                    Project project = selectedProjectModel.getObject();
                    String username = SecurityContextHolder.getContext().getAuthentication()
                            .getName();
                    User user = userRepository.get(username);

                    if (isEmpty(uploadedFiles)) {
                        error("Please choose file with layer details before uploading");
                        return;
                    }
                    else if (project.getId() == 0) {
                        error("Project not yet created, please save project Details!");
                        return;
                    }
                    for (FileUpload tagFile : uploadedFiles) {
                        InputStream tagInputStream;
                        try {
                            tagInputStream = tagFile.getInputStream();
                            String text = IOUtils.toString(tagInputStream, "UTF-8");

                            de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationLayer exLayer = JSONUtil
                                    .getJsonConverter()
                                    .getObjectMapper()
                                    .readValue(
                                            text,
                                            de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationLayer.class);

                            AnnotationLayer attachLayer = null;
                            if (exLayer.getAttachType() != null) {
                                de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationLayer exAttachLayer = exLayer
                                        .getAttachType();
                                createLayer(exAttachLayer, user, null);
                                attachLayer = annotationService.getLayer(exAttachLayer.getName(),
                                        project);
                            }
                            createLayer(exLayer, user, attachLayer);
                            layerDetailForm.setModelObject(annotationService.getLayer(
                                    exLayer.getName(), project));
                            layerDetailForm.setVisible(true);
                            featureSelectionForm.setVisible(true);

                        }
                        catch (IOException e) {
                            error("Error Importing TagSet " + ExceptionUtils.getRootCauseMessage(e));
                        }
                    }
                    featureDetailForm.setVisible(false);
                }

                private void createLayer(
                        de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationLayer aExLayer,
                        User aUser, AnnotationLayer aAttachLayer)
                    throws IOException
                {
                    Project project = selectedProjectModel.getObject();
                    AnnotationLayer layer;
                    if (annotationService.existsLayer(aExLayer.getName(), aExLayer.getType(),
                            project)) {
                        layer = annotationService.getLayer(aExLayer.getName(),
                                selectedProjectModel.getObject());
                        ImportUtil.setLayer(annotationService, layer, aExLayer, project, aUser);
                    }
                    else {
                        layer = new AnnotationLayer();
                        ImportUtil.setLayer(annotationService, layer, aExLayer, project, aUser);
                    }
                    layer.setAttachType(aAttachLayer);
                    for (de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationFeature exfeature : aExLayer
                            .getFeatures()) {

                        de.tudarmstadt.ukp.clarin.webanno.model.export.TagSet exTagset = exfeature
                                .getTagSet();
                        TagSet tagSet = null;
                        if (exTagset != null
                                && annotationService.existsTagSet(exTagset.getName(), project)) {
                            tagSet = annotationService.getTagSet(exTagset.getName(), project);
                            ImportUtil.createTagSet(tagSet, exTagset, project, aUser,
                                    annotationService);
                        }
                        else if (exTagset != null) {
                            tagSet = new TagSet();
                            ImportUtil.createTagSet(tagSet, exTagset, project, aUser,
                                    annotationService);
                        }
                        if (annotationService.existsFeature(exfeature.getName(), layer)) {
                            AnnotationFeature feature = annotationService.getFeature(
                                    exfeature.getName(), layer);
                            feature.setTagset(tagSet);
                            ImportUtil.setFeature(annotationService, feature, exfeature, project,
                                    aUser);
                            continue;
                        }
                        AnnotationFeature feature = new AnnotationFeature();
                        feature.setLayer(layer);
                        feature.setTagset(tagSet);
                        ImportUtil
                                .setFeature(annotationService, feature, exfeature, project, aUser);
                    }
                }
            });
        }
    }

    public class SelectionModel
        implements Serializable
    {
        private static final long serialVersionUID = -1L;

        private AnnotationLayer layerSelection;
        public AnnotationFeature feature;
    }

    private class LayerDetailForm
        extends Form<AnnotationLayer>
    {
        private static final long serialVersionUID = -1L;

        private Label name;
        private TextField<String> uiName;
        private static final String TYPE_PREFIX = "webanno.custom.";
        private String layerName;
        private DropDownChoice<String> layerTypes;
        private DropDownChoice<AnnotationLayer> attachTypes;

        private Label lockToTokenOffsetLabel;
        private CheckBox lockToTokenOffset;

        private Label allowStackingLabel;
        private CheckBox allowStacking;

        private Label crossSentenceLabel;
        private CheckBox crossSentence;

        private Label multipleTokensLabel;
        private CheckBox multipleTokens;

        private Label linkedListBehaviorLabel;
        private CheckBox linkedListBehavior;

        public LayerDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<AnnotationLayer>(new EntityModel<AnnotationLayer>(
                    new AnnotationLayer())));

            final Project project = selectedProjectModel.getObject();
            add(uiName = (TextField<String>) new TextField<String>("uiName").setRequired(true));
            uiName.add(new AjaxFormComponentUpdatingBehavior("keyup")
            {
                private static final long serialVersionUID = -1756244972577094229L;

                @Override
                protected void onUpdate(AjaxRequestTarget target)
                {
                    String modelValue = StringUtils.capitalize(getModelObject().getUiName());
                    layerName = modelValue;
                }
            });

            add(name = new Label("name")
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onConfigure()
                {
                    setVisible(StringUtils.isNotBlank(LayerDetailForm.this.getModelObject()
                            .getName()));
                };
            });
            
            add(new TextArea<String>("description").setOutputMarkupPlaceholderTag(true));
            add(new CheckBox("enabled"));
            add(layerTypes = (DropDownChoice<String>) new DropDownChoice<String>("type",
                    Arrays.asList(new String[] { SPAN_TYPE, RELATION_TYPE, CHAIN_TYPE }))
            {
                private static final long serialVersionUID = 1244555334843130802L;

                @Override
                public boolean isEnabled()
                {
                    return LayerDetailForm.this.getModelObject().getId() == 0;
                }
            }.setRequired(true));
            layerTypes.add(new AjaxFormComponentUpdatingBehavior("change")
            {
                private static final long serialVersionUID = 6790949494089940303L;

                @Override
                protected void onUpdate(AjaxRequestTarget target)
                {
                    layerType = getModelObject().getType();
                    target.add(lockToTokenOffsetLabel);
                    target.add(lockToTokenOffset);

                    target.add(allowStackingLabel);
                    target.add(allowStacking);

                    target.add(crossSentenceLabel);
                    target.add(crossSentence);

                    target.add(multipleTokensLabel);
                    target.add(multipleTokens);

                    target.add(linkedListBehaviorLabel);
                    target.add(linkedListBehavior);

                    target.add(attachTypes);
                }
            });

            attachTypes = (DropDownChoice<AnnotationLayer>) new DropDownChoice<AnnotationLayer>(
                    "attachType")
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
                                    .listAnnotationLayer(project);

                            if (LayerDetailForm.this.getModelObject().getId() > 0) {
                                if (LayerDetailForm.this.getModelObject().getAttachType() == null) {
                                    return new ArrayList<AnnotationLayer>();
                                }

                                return Arrays.asList(LayerDetailForm.this.getModelObject()
                                        .getAttachType());
                            }
                            if (!layerType.equals(RELATION_TYPE)) {
                                return new ArrayList<AnnotationLayer>();
                            }

                            List<AnnotationLayer> attachTeypes = new ArrayList<AnnotationLayer>();
                            // remove a span layer which is already used as attach type for the
                            // other
                            List<AnnotationLayer> usedLayers = new ArrayList<AnnotationLayer>();
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
                    setChoiceRenderer(new ChoiceRenderer<AnnotationLayer>("uiName"));
                }

                @Override
                protected void onConfigure()
                {
                    setEnabled(LayerDetailForm.this.getModelObject().getId() == 0);
                    setNullValid(isVisible());
                };
            };
            attachTypes.setOutputMarkupPlaceholderTag(true);
            add(attachTypes);

            // Behaviors of layers
            add(new CheckBox("readonly"));

            add(lockToTokenOffsetLabel = new Label("lockToTokenOffsetLabel",
                    "Lock to token offsets:")
            {
                private static final long serialVersionUID = -1290883833837327207L;

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

            add(allowStackingLabel = new Label("allowStackingLabel", "Allow stacking:")
            {
                private static final long serialVersionUID = -5354062154610496880L;

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
                            // Not configurable for layers that attach to tokens (currently that is the
                            // only layer on which we use the attach feature)
                            layer.getAttachFeature() == null);
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
                            // Not configurable for layers that attach to tokens (currently that is the
                            // only layer on which we use the attach feature)
                            layer.getAttachFeature() == null);
                }
            });

            add(crossSentenceLabel = new Label("crossSentenceLabel",
                    "Allow crossing sentence boundary:")
            {
                private static final long serialVersionUID = -5354062154610496880L;

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

            add(multipleTokensLabel = new Label("multipleTokensLabel", "Allow multiple tokens:")
            {
                private static final long serialVersionUID = -5354062154610496880L;

                {
                    setOutputMarkupPlaceholderTag(true);
                }

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    AnnotationLayer layer = LayerDetailForm.this.getModelObject();
                    // Makes no sense for relations
                    setVisible(!isBlank(layer.getType()) && !RELATION_TYPE.equals(layer.getType()));
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
            add(multipleTokens = new CheckBox("multipleTokens")
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
                    // Makes no sense for relations
                    setVisible(!isBlank(layer.getType()) && !RELATION_TYPE.equals(layer.getType()));
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

            add(linkedListBehaviorLabel = new Label("linkedListBehaviorLabel",
                    "Behave like a linked list:")
            {
                private static final long serialVersionUID = -5354062154610496880L;

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

            add(new Button("save", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    AnnotationLayer layer = LayerDetailForm.this.getModelObject();

                    if (layer.isLockToTokenOffset() && layer.isMultipleTokens()) {
                        layer.setLockToTokenOffset(false);
                    }

                    if (layer.getId() == 0) {
                        layerName = layerName.replaceAll("\\W", "");
                        if(layerName.isEmpty() || !isAscii(layerName)){
                            error("Non ASCII characters can not be used as layer name!");
                            return;
                        }
                        if (annotationService.existsLayer(TYPE_PREFIX + layerName, layer.getType(),
                                project)) {
                            error("Only one Layer per project is allowed!");
                            return;
                        }
                        if (layer.getType().equals(RELATION_TYPE) && layer.getAttachType() == null) {
                            error("a relation layer need an attach type!");
                            return;
                        }

                        if ((TYPE_PREFIX + layerName).endsWith(".")) {
                            error("please give a proper layer name!");
                            return;
                        }

                        String username = SecurityContextHolder.getContext().getAuthentication()
                                .getName();
                        User user = userRepository.get(username);

                        layer.setProject(project);
                        try {
                            layer.setName(TYPE_PREFIX + layerName);
                            annotationService.createLayer(layer);
                            if (layer.getType().equals(WebAnnoConst.CHAIN_TYPE)) {
                                AnnotationFeature relationFeature = new AnnotationFeature();
                                relationFeature.setType(CAS.TYPE_NAME_STRING);
                                relationFeature.setName(COREFERENCE_RELATION_FEATURE);
                                relationFeature.setLayer(layer);
                                relationFeature.setEnabled(true);
                                relationFeature.setUiName("Reference Relation");
                                relationFeature.setProject(project);

                                annotationService.createFeature(relationFeature);

                                AnnotationFeature typeFeature = new AnnotationFeature();
                                typeFeature.setType(CAS.TYPE_NAME_STRING);
                                typeFeature.setName(COREFERENCE_TYPE_FEATURE);
                                typeFeature.setLayer(layer);
                                typeFeature.setEnabled(true);
                                typeFeature.setUiName("Reference Type");
                                typeFeature.setProject(project);

                                annotationService.createFeature(typeFeature);
                            }
                        }
                        catch (IOException e) {
                            error("unable to create Logger file while creating this layer" + ":"
                                    + ExceptionUtils.getRootCauseMessage(e));
                        }
                        featureSelectionForm.setVisible(true);

                    }
                }
            });

            add(new DownloadLink("export", new LoadableDetachableModel<File>()
            {
                private static final long serialVersionUID = 840863954694163375L;

                @Override
                protected File load()
                {
                    File exportFile = null;
                    try {
                        exportFile = File.createTempFile("exportedLayer", ".json");
                    }
                    catch (IOException e1) {
                        error("Unable to create temporary File!!");

                    }
                    if (selectedProjectModel.getObject().getId() == 0) {
                        error("Project not yet created. Please save project details first!");
                        return null;
                    }
                    AnnotationLayer layer = layerDetailForm.getModelObject();

                    de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationLayer exLayer = ImportUtil
                            .exportLayerDetails(null, null, layer, annotationService);
                    if (layer.getAttachType() != null) {
                        AnnotationLayer attachLayer = layer.getAttachType();
                        de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationLayer exAttachLayer = ImportUtil
                                .exportLayerDetails(null, null, attachLayer, annotationService);
                        exLayer.setAttachType(exAttachLayer);
                    }

                    try {
                        JSONUtil.generatePrettyJson(exLayer, exportFile);
                    }
                    catch (IOException e) {
                        error("File Path not found or No permision to save the file!");
                    }
                    info("TagSets successfully exported to :" + exportFile.getAbsolutePath());

                    return exportFile;
                }
            }).setDeleteAfterDownload(true).setOutputMarkupId(true));
            add(new Button("cancel", new ResourceModel("label")) {
                private static final long serialVersionUID = 1L;
                
                {
                    // Avoid saving data
                    setDefaultFormProcessing(false);
                    setVisible(true);
                }
                
                @Override
                public void onSubmit()
                {
//                    layerDetailForm.setModelObject(new AnnotationLayer());
//                    layerSelectionForm.setModelObject(new SelectionModel());
//                    featureSelectionForm.setModelObject(new SelectionModel());
//                    featureDetailForm.setModelObject(new AnnotationFeature());
//                    layerDetailForm.setModelObject(null);
                    layerDetailForm.setVisible(false);
                    featureSelectionForm.setVisible(false);
                    featureDetailForm.setVisible(false);
                }
            });

        }
    }

    public static boolean isAscii(String v)
    {
        CharsetEncoder asciiEncoder = StandardCharsets.US_ASCII.newEncoder();
        return asciiEncoder.canEncode(v);
    }

    private class FeatureDetailForm
        extends Form<AnnotationFeature>
    {
        private static final long serialVersionUID = -1L;
        DropDownChoice<TagSet> tagSet;
        DropDownChoice<String> featureType;
        List<String> types = new ArrayList<String>();

        public FeatureDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<AnnotationFeature>(
                    new EntityModel<AnnotationFeature>(new AnnotationFeature())));

            add(new Label("name")
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onConfigure()
                {
                    setVisible(StringUtils.isNotBlank(FeatureDetailForm.this.getModelObject()
                            .getName()));
                };
            });
            add(new TextField<String>("uiName").setRequired(true));
            add(new TextArea<String>("description").setOutputMarkupPlaceholderTag(true));
            add(new CheckBox("enabled"));
            add(new CheckBox("visible"));
            add(new CheckBox("remember"));
            add(new CheckBox("hideUnconstraintFeature"));

//            spanTypes.add(CAS.TYPE_NAME_ANNOTATION);
//            for (AnnotationLayer spanLayer : annotationService
//                    .listAnnotationLayer(selectedProjectModel.getObject())) {
//                if (spanLayer.getName().equals(Token.class.getName())) {
//                    continue;
//                }
//                if (spanLayer.getType().equals(WebAnnoConst.SPAN_TYPE)) {
//                    spanTypes.add(spanLayer.getName());
//                }
//            }

            add(featureType = (DropDownChoice<String>) new DropDownChoice<String>("type")
            {
                private static final long serialVersionUID = 9029205407108101183L;

                {
                    setRequired(true);
                    setNullValid(false);
                    setChoices(new LoadableDetachableModel<List<String>>()
                    {
                        private static final long serialVersionUID = -5732558926576750673L;

                        @Override
                        protected List<String> load()
                        {
                            if (getModelObject() != null) {
                                return Arrays.asList(getModelObject());
                            }
//                            types.addAll(spanTypes);
                            return types;
                        }
                    });
                }

                @Override
                protected void onConfigure()
                {
                    setEnabled(FeatureDetailForm.this.getModelObject().getId() == 0);
                };
            });
            featureType.add(new AjaxFormComponentUpdatingBehavior("change")
            {
                private static final long serialVersionUID = -2904306846882446294L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    aTarget.add(tagSet);
                }
            });
            add(tagSet = new DropDownChoice<TagSet>("tagset")
            {
                private static final long serialVersionUID = -6705445053442011120L;
                {
                    setOutputMarkupPlaceholderTag(true);
                    setOutputMarkupId(true);
                    setChoiceRenderer(new ChoiceRenderer<TagSet>("name"));
                    setNullValid(true);
                    setChoices(new LoadableDetachableModel<List<TagSet>>()
                    {
                        private static final long serialVersionUID = 1784646746122513331L;

                        @Override
                        protected List<TagSet> load()
                        {
                            return annotationService.listTagSets(selectedProjectModel.getObject());
                        }
                    });
                }

                @Override
                protected void onConfigure()
                {
                    AnnotationFeature feature = FeatureDetailForm.this.getModelObject();
                    // Only display tagset choice for link features with role
                    // and string features
                    // Since we currently set the LinkRole only when saving, we
                    // have to rely on the
                    // feature type here.
                    setEnabled(CAS.TYPE_NAME_STRING.equals(feature.getType())
                            || !PRIMITIVE_TYPES.contains(feature.getType()));
                    
                    //Empty the 'type' list drop-down in FeatureDetailForm
                    types.clear();
                    //Add primitive types
                    types.addAll(PRIMITIVE_TYPES);
                    // Add non-primitive types only when layer is of type SPAN (#62)
                    if (layerDetailForm.getModelObject().getType().equals(WebAnnoConst.SPAN_TYPE)) {
                        spanTypes.add(CAS.TYPE_NAME_ANNOTATION);
                        //Add layers of type SPAN available in the project
                        for (AnnotationLayer spanLayer : annotationService
                                .listAnnotationLayer(selectedProjectModel.getObject())) {
                            if (spanLayer.getType().equals(WebAnnoConst.SPAN_TYPE)) {
                                types.add(spanLayer.getName());
                            }
                        }
                    }
                }
            });

            add(new Button("save", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    AnnotationFeature feature = FeatureDetailForm.this.getModelObject();
                    String name = feature.getUiName();
                    name = name.replaceAll("\\W", "");
                    // Check if feature name is not from the restricted names list
                    if (WebAnnoConst.RESTRICTED_FEATURE_NAMES.contains(name)) {
                        error("'" + feature.getUiName().toLowerCase() + " (" + name + ")"
                                + "' is a restricted keyword for a feature name. Please use a different name for the feature.");
                        return;
                    }
                    if (layerDetailForm.getModelObject().getType().equals(RELATION_TYPE)
                            && (name.equals(WebAnnoConst.FEAT_REL_SOURCE)
                                    || name.equals(WebAnnoConst.FEAT_REL_TARGET)
                                    || name.equals(FIRST) || name.equals(NEXT))) {
                        error("layer " + name + " is not allowed as a feature name");
                        return;
                    }
                    // Checking if feature name doesn't start with a number or underscore
                    // And only uses alphanumeric characters
                    if (StringUtils.isNumeric(name.substring(0, 1))
                            || name.substring(0, 1).equals("_")
                            || !StringUtils.isAlphanumeric(name.replace("_", ""))) {
                        error("Feature names must start with a letter and consist only of letters, digits, or underscores.");
                        return;
                    }
                    if (feature.getId() == 0) {
                        feature.setLayer(layerDetailForm.getModelObject());
                        feature.setProject(selectedProjectModel.getObject());

                        if (annotationService.existsFeature(feature.getName(), feature.getLayer())) {
                            error("This feature is already added for this layer!");
                            return;
                        }

                        if (annotationService.existsFeature(name, feature.getLayer())) {
                            error("this feature already exists!");
                            return;
                        }
                        feature.setName(name);
                        saveFeature(feature);
                    }
                    if (tagSet.getModelObject() != null) {
                        FeatureDetailForm.this.getModelObject().setTagset(tagSet.getModelObject());
                    }
                }
            });
            add(new Button("cancel", new ResourceModel("label")) {
                private static final long serialVersionUID = 1L;
                
                {
                    // Avoid saving data
                    setDefaultFormProcessing(false);
                    setVisible(true);
                }
                
                @Override
                public void onSubmit()
                {
                    featureDetailForm.setModelObject(new AnnotationFeature());
//                    FeatureDetailForm.this.setVisible(false);
                }
            });

        }
    }

    private void saveFeature(AnnotationFeature aFeature)
    {
        // Set properties of link features since these are currently not configurable in the UI
        if (!PRIMITIVE_TYPES.contains(aFeature.getType())) {
            aFeature.setMode(MultiValueMode.ARRAY);
            aFeature.setLinkMode(LinkMode.WITH_ROLE);
            aFeature.setLinkTypeRoleFeatureName("role");
            aFeature.setLinkTypeTargetFeatureName("target");
            aFeature.setLinkTypeName(aFeature.getLayer().getName()
                    + WordUtils.capitalize(aFeature.getName()) + "Link");
        }

        // If the feature is not a string feature or a link-with-role feature, force the tagset
        // to null.
        if (!(CAS.TYPE_NAME_STRING.equals(aFeature.getType()) || !PRIMITIVE_TYPES.contains(aFeature
                .getType()))) {
            aFeature.setTagset(null);
        }

        annotationService.createFeature(aFeature);
        featureDetailForm.setVisible(false);
    }

    public class FeatureSelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;

        private ListChoice<AnnotationFeature> feature;

        public FeatureSelectionForm(String id)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

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
                }

                @Override
                protected void onSelectionChanged(AnnotationFeature aNewSelection)
                {
                    if (aNewSelection != null) {
                        featureDetailForm.setModelObject(aNewSelection);
                        featureDetailForm.setVisible(true);

                    }
                }

                @Override
                protected boolean wantOnSelectionChangedNotifications()
                {
                    return true;
                }

                @Override
                protected CharSequence getDefaultChoice(String aSelectedValue)
                {
                    return "";
                }
            });

            add(new Button("new", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    featureDetailForm.setDefaultModelObject(new AnnotationFeature());
                    featureDetailForm.setVisible(true);
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
                        List<AnnotationFeature> filtered = new ArrayList<AnnotationFeature>();
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

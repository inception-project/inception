/*******************************************************************************
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.project.page;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.uima.cas.CAS;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
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
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.brat.project.ProjectUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.EntityModel;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * A Panel Used to add Layers to a selected {@link Project} in the project settings page
 *
 * @author Seid Muhie Yimam
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

    ModalWindow openHelpDialog;

    private LayerSelectionForm layerSelectionForm;
    private FeatureSelectionForm featureSelectionForm;
    private LayerDetailForm layerDetailForm;
    private final FeatureDetailForm featureDetailForm;

    private Select<AnnotationLayer> layerSelection;

    private final Model<Project> selectedProjectModel;
    List<String> types = new ArrayList<String>();

    String layerType = WebAnnoConst.SPAN_TYPE;
    private IModel<String> helpDataModel = new Model<String>();

    public ProjectLayersPanel(String id, final Model<Project> aProjectModel)
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

        openHelpDialog = new ModalWindow("openHelpDialog");
        openHelpDialog.setOutputMarkupId(true);
        openHelpDialog.setInitialWidth(550);
        openHelpDialog.setInitialHeight(280);
        openHelpDialog.setResizable(true);
        openHelpDialog.setWidthUnit("px");
        openHelpDialog.setHeightUnit("px");

        add(layerSelectionForm.add(openHelpDialog));
        add(featureSelectionForm);
        add(layerDetailForm);
        add(featureDetailForm);
    }

    /**
     * opens the {@link HelpModalWindowPanel} in a {@link ModalWindow}
     */
    private void openHelpDialog(final ModalWindow openAnnotationDialog, AjaxRequestTarget aTarget,
            String helpName)
    {
        helpDataModel.setObject(getHelpContent(helpName));
        openAnnotationDialog.setTitle("....");
        openAnnotationDialog.setContent(new HelpModalWindowPanel(openAnnotationDialog
                .getContentId(), openAnnotationDialog, getHelpContent(), helpName, helpDataModel));
        openAnnotationDialog.show(aTarget);
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
                                        Token.class.getName(), WebAnnoConst.SPAN_TYPE, project);
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

        TextField<String> uiName;
        String prefix = "webanno.custom.";
        String layerName;
        DropDownChoice<String> layerTypes;
        DropDownChoice<AnnotationLayer> attachTypes;

        @SuppressWarnings("unchecked")
        public LayerDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<AnnotationLayer>(new EntityModel<AnnotationLayer>(
                    new AnnotationLayer())));

            final Project project = selectedProjectModel.getObject();
            add(uiName = (TextField<String>) new TextField<String>("uiName").setRequired(true));
            uiName.add(new AjaxFormComponentUpdatingBehavior("onkeyup")
            {
                private static final long serialVersionUID = -1756244972577094229L;

                @Override
                protected void onUpdate(AjaxRequestTarget target)
                {
                    String modelValue = StringUtils.capitalise(getModelObject().getUiName());
                    layerName = modelValue;
                }
            });
            add(new AjaxLink<Void>("showLayerNameHelpModal")
            {
                private static final long serialVersionUID = 7496156015186497496L;

                @Override
                public void onClick(AjaxRequestTarget target)
                {
                    openHelpDialog(openHelpDialog, target, "layerName");
                }
            });

            add(new TextArea<String>("description").setOutputMarkupPlaceholderTag(true));
            add(new CheckBox("enabled"));
            add(new AjaxLink<Void>("showLayerEnabledHelpModal")
            {
                private static final long serialVersionUID = 7496156015186497496L;

                @Override
                public void onClick(AjaxRequestTarget target)
                {
                    openHelpDialog(openHelpDialog, target, "layerEnabled");
                }
            });

            add(layerTypes = (DropDownChoice<String>) new DropDownChoice<String>("type",
                    Arrays.asList(new String[] { "span", "relation", "chain" }))
            {
                private static final long serialVersionUID = 1244555334843130802L;

                @Override
                public boolean isEnabled()
                {
                    return LayerDetailForm.this.getModelObject().getId() == 0;
                }

                /*
                 * @Override protected boolean wantOnSelectionChangedNotifications() { return true;
                 * }
                 */
            }.setRequired(true));
            layerTypes.add(new AjaxFormComponentUpdatingBehavior("onchange")
            {
                private static final long serialVersionUID = 6790949494089940303L;

                @Override
                protected void onUpdate(AjaxRequestTarget target)
                {
                    layerType = getModelObject().getType();
                    target.add(attachTypes);
                }
            });
            add(new AjaxLink<Void>("showLayerTypesHelpModal")
            {
                private static final long serialVersionUID = 7496156015186497496L;

                @Override
                public void onClick(AjaxRequestTarget target)
                {
                    openHelpDialog(openHelpDialog, target, "layerTypes");
                }
            });

            add(attachTypes = (DropDownChoice<AnnotationLayer>) new DropDownChoice<AnnotationLayer>(
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
                            if (!layerType.equals(WebAnnoConst.RELATION_TYPE)) {
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
                                if (layer.getType().equals("span") && !layer.isBuiltIn()) {
                                    attachTeypes.add(layer);
                                }
                            }

                            return attachTeypes;
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<AnnotationLayer>("uiName"));
                }

                @Override
                public boolean isNullValid()
                {
                    return isVisible();
                }

                @Override
                public boolean isEnabled()
                {
                    if (LayerDetailForm.this.getModelObject().getId() != 0) {
                        return false;
                    }
                    return true;
                }

                @Override
                protected boolean wantOnSelectionChangedNotifications()
                {
                    return true;
                }

            }.setOutputMarkupPlaceholderTag(true));
            add(new AjaxLink<Void>("showAttachTypeHelpModal")
            {
                private static final long serialVersionUID = 7496156015186497496L;

                @Override
                public void onClick(AjaxRequestTarget target)
                {
                    openHelpDialog(openHelpDialog, target, "attachType");
                }
            });

            // behaviours of layers
            add(new Label("lockToTokenOffsetLabel", "Lock to token offsets:")
            {
                private static final long serialVersionUID = -1290883833837327207L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    if (LayerDetailForm.this.getModelObject().getId() != 0
                            && LayerDetailForm.this.getModelObject().getType()
                                    .equals(WebAnnoConst.RELATION_TYPE)) {
                        this.setVisible(false);
                    }
                    else if (LayerDetailForm.this.getModelObject().getId() != 0
                            && LayerDetailForm.this.getModelObject().getAttachFeature() != null) {
                        this.setVisible(false);
                    }
                    else {
                        this.setVisible(true);
                    }
                }
            });
            add(new CheckBox("lockToTokenOffset")
            {
                private static final long serialVersionUID = -4934708834659137207L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    if (LayerDetailForm.this.getModelObject().getId() != 0
                            && LayerDetailForm.this.getModelObject().getAttachFeature() != null) {
                        this.setVisible(false);
                    }
                    else if (LayerDetailForm.this.getModelObject().getId() != 0
                            && LayerDetailForm.this.getModelObject().getType()
                                    .equals(WebAnnoConst.RELATION_TYPE)) {
                        this.setVisible(false);
                    }
                    else {
                        this.setVisible(true);
                    }
                }
            });
            add(new AjaxLink<Void>("showLockToTokenOffsetHelpModal")
            {
                private static final long serialVersionUID = 7496156015186497496L;

                @Override
                public void onClick(AjaxRequestTarget target)
                {
                    openHelpDialog(openHelpDialog, target, "lockToToken");
                }

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    if (LayerDetailForm.this.getModelObject().getId() != 0
                            && LayerDetailForm.this.getModelObject().getAttachFeature() != null) {
                        this.setVisible(false);
                    }
                    else if (LayerDetailForm.this.getModelObject().getId() != 0
                            && LayerDetailForm.this.getModelObject().getType()
                                    .equals(WebAnnoConst.RELATION_TYPE)) {
                        this.setVisible(false);
                    }
                    else {
                        this.setVisible(true);
                    }
                }
            });

            add(new Label("allowSTackingLabel", "Allow stacking:")
            {
                private static final long serialVersionUID = -5354062154610496880L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    if (LayerDetailForm.this.getModelObject().getId() != 0
                            && LayerDetailForm.this.getModelObject().getAttachFeature() != null) {
                        this.setVisible(false);
                    }
                    else {
                        this.setVisible(true);
                    }
                }
            });
            add(new CheckBox("allowSTacking")
            {
                private static final long serialVersionUID = 7800627916287273008L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    if (LayerDetailForm.this.getModelObject().getId() != 0
                            && LayerDetailForm.this.getModelObject().getAttachFeature() != null) {
                        this.setVisible(false);
                    }
                    else {
                        this.setVisible(true);
                    }
                }
            });
            add(new AjaxLink<Void>("showAllowSTackingHelpModal")
            {
                private static final long serialVersionUID = 7496156015186497496L;

                @Override
                public void onClick(AjaxRequestTarget target)
                {
                    openHelpDialog(openHelpDialog, target, "allowStacking");
                }

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    if (LayerDetailForm.this.getModelObject().getId() != 0
                            && LayerDetailForm.this.getModelObject().getAttachFeature() != null) {
                        this.setVisible(false);
                    }
                    else {
                        this.setVisible(true);
                    }
                }
            });

            add(new Label("crossSentenceLabel", "Allow crossing sentence boundary:")
            {
                private static final long serialVersionUID = -5354062154610496880L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    if (LayerDetailForm.this.getModelObject().getId() != 0
                            && LayerDetailForm.this.getModelObject().getAttachFeature() != null) {
                        this.setVisible(false);
                    }
                    else {
                        this.setVisible(true);
                    }
                }
            });
            add(new CheckBox("crossSentence")
            {
                private static final long serialVersionUID = -5986386642712152491L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    if (LayerDetailForm.this.getModelObject().getId() != 0
                            && LayerDetailForm.this.getModelObject().getAttachFeature() != null) {
                        this.setVisible(false);
                    }
                    else {
                        this.setVisible(true);
                    }
                }
            });
            add(new AjaxLink<Void>("showCrossSentenceHelpModal")
            {
                private static final long serialVersionUID = 7496156015186497496L;

                @Override
                public void onClick(AjaxRequestTarget target)
                {
                    openHelpDialog(openHelpDialog, target, "crossSentence");
                }

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    if (LayerDetailForm.this.getModelObject().getId() != 0
                            && LayerDetailForm.this.getModelObject().getAttachFeature() != null) {
                        this.setVisible(false);
                    }
                    else {
                        this.setVisible(true);
                    }
                }
            });

            add(new Label("multipleTokensLabel", "Allow multiple tokens:")
            {
                private static final long serialVersionUID = -5354062154610496880L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    if (LayerDetailForm.this.getModelObject().getId() != 0
                            && LayerDetailForm.this.getModelObject().getAttachFeature() != null) {
                        this.setVisible(false);
                    }
                    else if (LayerDetailForm.this.getModelObject().getId() != 0
                            && LayerDetailForm.this.getModelObject().getType()
                                    .equals(WebAnnoConst.RELATION_TYPE)) {
                        this.setVisible(false);
                    }
                    else {
                        this.setVisible(true);
                    }
                }
            });
            add(new CheckBox("multipleTokens")
            {
                private static final long serialVersionUID = 1319818165277559402L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    if (LayerDetailForm.this.getModelObject().getId() != 0
                            && LayerDetailForm.this.getModelObject().getAttachFeature() != null) {
                        this.setVisible(false);
                    }
                    else if (LayerDetailForm.this.getModelObject().getId() != 0
                            && LayerDetailForm.this.getModelObject().getType()
                                    .equals(WebAnnoConst.RELATION_TYPE)) {
                        this.setVisible(false);
                    }
                    else {
                        this.setVisible(true);
                    }
                }
            });
            add(new AjaxLink<Void>("showMultipleTokensHelpModal")
            {
                private static final long serialVersionUID = 7496156015186497496L;

                @Override
                public void onClick(AjaxRequestTarget target)
                {
                    openHelpDialog(openHelpDialog, target, "multipleToken");
                }

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    if (LayerDetailForm.this.getModelObject().getId() != 0
                            && LayerDetailForm.this.getModelObject().getAttachFeature() != null) {
                        this.setVisible(false);
                    }
                    else if (LayerDetailForm.this.getModelObject().getId() != 0
                            && LayerDetailForm.this.getModelObject().getType()
                                    .equals(WebAnnoConst.RELATION_TYPE)) {
                        this.setVisible(false);
                    }
                    else {
                        this.setVisible(true);
                    }
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
                        if (annotationService.existsLayer(prefix + layerName, layer.getType(),
                                project)) {
                            error("Only one Layer per project is allowed!");
                            return;
                        }
                        if (layer.getType().equals("relation") && layer.getAttachType() == null) {
                            error("a relation layer need an attach type!");
                            return;
                        }

                        if ((prefix + layerName).endsWith(".")) {
                            error("please give a proper layer name!");
                            return;
                        }
                        String username = SecurityContextHolder.getContext().getAuthentication()
                                .getName();
                        User user = repository.getUser(username);

                        layer.setProject(project);
                        try {
                            layerName = layerName.replaceAll("\\W", "");
                            layer.setName(prefix + layerName);
                            annotationService.createLayer(layer, user);
                            if (layer.getType().equals("chain")) {
                                AnnotationFeature relationFeature = new AnnotationFeature();
                                relationFeature.setType(layer.getName());
                                relationFeature.setName(WebAnnoConst.COREFERENCE_RELATION_FEATURE);
                                relationFeature.setLayer(layer);
                                relationFeature.setEnabled(true);
                                relationFeature.setUiName("Reference Relation");
                                relationFeature.setProject(project);

                                annotationService.createFeature(relationFeature);

                                AnnotationFeature typeFeature = new AnnotationFeature();
                                typeFeature.setType(layer.getName());
                                typeFeature.setName(WebAnnoConst.COREFERENCE_TYPE_FEATURE);
                                typeFeature.setLayer(layer);
                                typeFeature.setEnabled(true);
                                typeFeature.setUiName("Reference Type");
                                typeFeature.setProject(project);

                                annotationService.createFeature(typeFeature);
                            }
                        }
                        catch (IOException e) {
                            error("unable to create Log file while creating this layer" + ":"
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

                    de.tudarmstadt.ukp.clarin.webanno.model.export.AnnotationLayer exLayer = ProjectUtil
                            .exportLayerDetails(null, null, layer, annotationService);

                    MappingJacksonHttpMessageConverter jsonConverter = new MappingJacksonHttpMessageConverter();
                    ProjectUtil.setJsonConverter(jsonConverter);

                    try {
                        ProjectUtil.generateJson(exLayer, exportFile);
                    }
                    catch (IOException e) {
                        error("File Path not found or No permision to save the file!");
                    }
                    info("TagSets successfully exported to :" + exportFile.getAbsolutePath());

                    return exportFile;
                }
            }).setOutputMarkupId(true));

        }
    }

    private class FeatureDetailForm
        extends Form<AnnotationFeature>
    {
        private static final long serialVersionUID = -1L;
        DropDownChoice<TagSet> tagSet;
        DropDownChoice<String> featureType;

        public FeatureDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<AnnotationFeature>(
                    new EntityModel<AnnotationFeature>(new AnnotationFeature())));

            add(new TextField<String>("uiName").setRequired(true));
            add(new AjaxLink<Void>("showFeatureNameHelpModal")
            {
                private static final long serialVersionUID = 7496156015186497496L;

                @Override
                public void onClick(AjaxRequestTarget target)
                {
                    openHelpDialog(openHelpDialog, target, "featureName");
                }
            });
            add(new TextArea<String>("description").setOutputMarkupPlaceholderTag(true));
            add(new CheckBox("enabled"));
            add(new AjaxLink<Void>("showFeatureEnabledHelpModal")
            {
                private static final long serialVersionUID = 7496156015186497496L;

                @Override
                public void onClick(AjaxRequestTarget target)
                {
                    openHelpDialog(openHelpDialog, target, "featureEnabled");
                }
            });
            add(new CheckBox("visible"));
            add(new AjaxLink<Void>("showFeatureVisibleHelpModal")
            {
                private static final long serialVersionUID = 7496156015186497496L;

                @Override
                public void onClick(AjaxRequestTarget target)
                {
                    openHelpDialog(openHelpDialog, target, "featureVisible");
                }
            });

            types.add(CAS.TYPE_NAME_STRING);
            types.add(CAS.TYPE_NAME_INTEGER);
            types.add(CAS.TYPE_NAME_FLOAT);
            types.add(CAS.TYPE_NAME_BOOLEAN);

            add(featureType = (DropDownChoice<String>) new DropDownChoice<String>("type")
            {
                private static final long serialVersionUID = 9029205407108101183L;

                {
                    setChoices(new LoadableDetachableModel<List<String>>()
                    {
                        private static final long serialVersionUID = -5732558926576750673L;

                        @Override
                        protected List<String> load()
                        {
                            if (getModelObject() != null) {
                                return Arrays.asList(getModelObject());
                            }
                            return types;

                        }
                    });

                }

                @Override
                protected CharSequence getDefaultChoice(String aSelectedValue)
                {
                    return "";
                }

                @Override
                public boolean isEnabled()
                {
                    return FeatureDetailForm.this.getModelObject().getId() == 0;
                }
            }.setRequired(true));
            featureType.add(new AjaxFormComponentUpdatingBehavior("onChange")
            {
                private static final long serialVersionUID = -2904306846882446294L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    aTarget.add(tagSet);

                }
            });
            add(new AjaxLink<Void>("showtFeatureTypeHelpModal")
            {
                private static final long serialVersionUID = 7496156015186497496L;

                @Override
                public void onClick(AjaxRequestTarget target)
                {
                    openHelpDialog(openHelpDialog, target, "featureType");
                }
            });

            add(tagSet = new DropDownChoice<TagSet>("tagset")
            {
                private static final long serialVersionUID = -6705445053442011120L;

                {
                    setChoices(new LoadableDetachableModel<List<TagSet>>()
                    {
                        private static final long serialVersionUID = 1784646746122513331L;

                        @Override
                        protected List<TagSet> load()
                        {

                            return annotationService
                                    .listTagSets(selectedProjectModel.getObject());

                        }
                    });

                    setChoiceRenderer(new ChoiceRenderer<TagSet>()
                    {
                        private static final long serialVersionUID = 8639013729422537472L;

                        @Override
                        public Object getDisplayValue(TagSet aObject)
                        {
                            return aObject.getName();
                        }
                    });
                }

                @Override
                public boolean isNullValid()
                {
                    return isVisible();
                }

                @Override
                public boolean isEnabled()
                {
                    return FeatureDetailForm.this.getModelObject().getTagset() == null
                            && featureType.getModelObject() == null
                            || (featureType.getModelObject() != null && featureType
                                    .getModelObject().equals(CAS.TYPE_NAME_STRING));
                }
            });
            tagSet.setOutputMarkupPlaceholderTag(true);

            add(new AjaxLink<Void>("showTagSetHelpModal")
            {
                private static final long serialVersionUID = 7496156015186497496L;

                @Override
                public void onClick(AjaxRequestTarget target)
                {
                    openHelpDialog(openHelpDialog, target, "tagSet");
                }
            });

            add(new Button("save", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    AnnotationFeature feature = FeatureDetailForm.this.getModelObject();
                    if (feature.getId() == 0) {
                        feature.setLayer(layerDetailForm.getModelObject());
                        feature.setProject(selectedProjectModel.getObject());

                        if (annotationService.existsFeature(feature.getName(), feature.getLayer())) {
                            error("This feature is already added for this layer!");
                            return;
                        }

                        String name = feature.getUiName();
                        name = name.replaceAll("\\W", "");
                        if (annotationService.existsFeature(name, feature.getLayer())) {
                            error("this feature already exists!");
                            return;
                        }
                        feature.setName(name);
                        annotationService.createFeature(feature);
                        featureDetailForm.setVisible(false);
                    }
                    if (tagSet.getModelObject() != null) {
                        FeatureDetailForm.this.getModelObject().setTagset(tagSet.getModelObject());
                    }
                }
            });

        }
    }

    public class FeatureSelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;

        public FeatureSelectionForm(String id)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

            add(new ListChoice<AnnotationFeature>("feature")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<AnnotationFeature>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<AnnotationFeature> load()
                        {
                            return annotationService.listAnnotationFeature(layerDetailForm
                                    .getModelObject());
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<AnnotationFeature>()
                    {
                        private static final long serialVersionUID = 4610648616450168333L;

                        @Override
                        public Object getDisplayValue(AnnotationFeature aObject)
                        {
                            return "[ " + aObject.getUiName() + "] [ " + aObject.getType() + " ]";
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
                            && !layerDetailForm.getModelObject().getType()
                                    .equals(WebAnnoConst.CHAIN_TYPE);
                }
            });
        }
    }

    private HelpDataModel getHelpContent()
    {
        HelpDataModel helpContent = new HelpDataModel();
        BeanWrapper wrapper = new BeanWrapperImpl(helpContent);
        // get annotation preference from file system
        try {
            for (Entry<Object, Object> entry : repository.loadHelpContents().entrySet()) {
                String property = entry.getKey().toString();
                if (wrapper.isWritableProperty(property)) {

                    if (HelpDataModel.class.getDeclaredField(property).getGenericType() instanceof ParameterizedType) {
                        List<String> value = Arrays.asList(StringUtils.replaceChars(
                                entry.getValue().toString(), "[]", "").split(","));
                        if (!value.get(0).equals("")) {
                            wrapper.setPropertyValue(property, value);
                        }
                    }
                    else {
                        wrapper.setPropertyValue(property, entry.getValue());
                    }
                }
            }
        }
        // no preference found
        catch (Exception e) {
        }
        return helpContent;
    }

    private String getHelpContent(String aField)
    {
        String helpFieldContent = "";
        HelpDataModel helpContent = new HelpDataModel();
        BeanWrapper wrapper = new BeanWrapperImpl(helpContent);
        // get annotation preference from file system
        try {
            for (Entry<Object, Object> entry : repository.loadHelpContents().entrySet()) {
                String property = entry.getKey().toString();
                if (wrapper.isWritableProperty(property)) {
                    if (HelpDataModel.class.getDeclaredField(property).getGenericType() instanceof ParameterizedType) {
                        List<String> value = Arrays.asList(StringUtils.replaceChars(
                                entry.getValue().toString(), "[]", "").split(","));
                        if (!value.get(0).equals("")) {
                            wrapper.setPropertyValue(property, value);
                        }
                    }
                    else {
                        if (property.equals(aField)) {
                            helpFieldContent = entry.getValue().toString();
                        }
                        wrapper.setPropertyValue(property, entry.getValue());
                    }
                }
            }
        }
        // no preference found
        catch (Exception e) {
        }
        return helpFieldContent;
    }
}

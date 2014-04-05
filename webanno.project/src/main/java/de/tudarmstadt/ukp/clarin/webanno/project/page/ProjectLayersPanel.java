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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
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
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.WebAnnoConst;
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
    private RepositoryService projectRepository;

    DropDownChoice<String> importTagsetFormat;
    DropDownChoice<String> exportTagsetFormat;

    private LayerSelectionForm layerSelectionForm;
    private FeatureSelectionForm featureSelectionForm;
    private LayerDetailForm layerDetailForm;
    private final FeatureDetailForm featureDetailForm;

    private Select<AnnotationLayer> layerSelection;

    private final Model<Project> selectedProjectModel;
    List<String> types = new ArrayList<String>();

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

        add(layerSelectionForm);
        add(featureSelectionForm);
        add(layerDetailForm);
        add(featureDetailForm);
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
                                        .listAnnotationType(project);
                                AnnotationLayer tokenLayer = annotationService.getType(
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

        DropDownChoice<AnnotationLayer> attachType;
        DropDownChoice<String> LayerType;
        TextField<String> uiName;
        String prefix = "webanno.custom.";
        String layerName;
        AnnotationLayer none;

        public LayerDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<AnnotationLayer>(new EntityModel<AnnotationLayer>(
                    new AnnotationLayer())));
            if (attachType != null) {
                attachType.setVisible(false);
                attachType.setOutputMarkupPlaceholderTag(true);
            }
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
            add(new TextArea<String>("description").setOutputMarkupPlaceholderTag(true));
            add(new CheckBox("enabled"));

            add(LayerType = (DropDownChoice<String>) new DropDownChoice<String>("type",
                    Arrays.asList(new String[] { "span", "relation", "chain" }))
            {
                private static final long serialVersionUID = 1244555334843130802L;

                @Override
                public boolean isEnabled()
                {
                    return LayerDetailForm.this.getModelObject().getId() == 0;
                }

                @Override
                protected CharSequence getDefaultChoice(String aSelectedValue)
                {
                    return "";
                }
            }.setRequired(true));

            add(attachType = new DropDownChoice<AnnotationLayer>("attachType")
            {
                private static final long serialVersionUID = -6705445053442011120L;

                {
                    setChoices(new LoadableDetachableModel<List<AnnotationLayer>>()
                    {
                        private static final long serialVersionUID = 1784646746122513331L;

                        @Override
                        protected List<AnnotationLayer> load()
                        {
                            none = new AnnotationLayer();
                            none.setUiName("-NONE-");
                            if (LayerDetailForm.this.getModelObject().getId() > 0
                                    && LayerDetailForm.this.getModelObject().getAttachType() == null) {
                                return Arrays.asList(none);
                            }
                            List<AnnotationLayer> allLayers = annotationService
                                    .listAnnotationType(project);
                            List<AnnotationLayer> attachTeypes = new ArrayList<AnnotationLayer>();
                            attachTeypes.add(none);
                            for (AnnotationLayer layer : allLayers) {
                                if (!layer.getType().equals("span")) {
                                    continue;
                                }
                                attachTeypes.add(layer);

                            }

                            return attachTeypes;

                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<AnnotationLayer>()
                    {
                        private static final long serialVersionUID = 8639013729422537472L;

                        @Override
                        public Object getDisplayValue(AnnotationLayer aObject)
                        {
                            return aObject.getUiName();
                        }
                    });
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
                protected CharSequence getDefaultChoice(String aSelectedValue)
                {
                    return "";
                }
            });

            // behaviors of layers
            add(new CheckBox("lockToTokenOffset"));
            add(new CheckBox("allowSTacking"));
            add(new CheckBox("crossSentence"));
            add(new CheckBox("multipleTokens"));

            add(new Button("save", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    AnnotationLayer layer = LayerDetailForm.this.getModelObject();

                    if (layer.getId() == 0) {
                        if (annotationService.existsLayer(prefix + layerName, layer.getType(),
                                project)) {
                            error("Only one Layer per project is allowed!");
                            return;
                        }
                        if (layer.getType().equals("relation")
                                && layer.getAttachType().equals(none)) {
                            error("a relation layer need an attach type!");
                            return;
                        }

                        if ((prefix + layerName).endsWith(".")) {
                            error("please give a proper layer name!");
                            return;
                        }
                        String username = SecurityContextHolder.getContext().getAuthentication()
                                .getName();
                        User user = projectRepository.getUser(username);

                        layer.setProject(project);
                        try {
                            layerName = layerName.replaceAll("\\W", "");
                            layer.setName(prefix + layerName);
                            if (layer.getAttachType().equals(none)) {
                                layer.setAttachType(null);
                            }
                            annotationService.createType(layer, user);
                            if (layer.getType().equals("chain")) {
                                AnnotationFeature relationFeature = new AnnotationFeature();
                                relationFeature.setType(layer.getName());
                                relationFeature.setName("referenceRelation");
                                relationFeature.setLayer(layer);
                                relationFeature.setEnabled(true);
                                relationFeature.setUiName("Reference Relation");
                                relationFeature.setProject(project);

                                annotationService.createFeature(relationFeature);

                                AnnotationFeature typeFeature = new AnnotationFeature();
                                typeFeature.setType(layer.getName());
                                typeFeature.setName("referenceType");
                                typeFeature.setLayer(layer);
                                typeFeature.setEnabled(true);
                                typeFeature.setUiName("Reference Type");
                                typeFeature.setProject(project);

                                annotationService.createFeature(typeFeature);
                            }
                        }
                        catch (IOException e) {
                            error("unable to create Log file while creating the TagSet" + ":"
                                    + ExceptionUtils.getRootCauseMessage(e));
                        }
                        featureSelectionForm.setVisible(true);

                    }
                }
            });

        }
    }

    private class FeatureDetailForm
        extends Form<AnnotationFeature>
    {
        private static final long serialVersionUID = -1L;
        DropDownChoice<TagSet> tagSet;
        DropDownChoice<String> featureType;
        TagSet none;

        public FeatureDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<AnnotationFeature>(
                    new EntityModel<AnnotationFeature>(new AnnotationFeature())));

            add(new TextField<String>("uiName").setRequired(true));
            add(new TextArea<String>("description").setOutputMarkupPlaceholderTag(true));
            add(new CheckBox("enabled"));
            add(new CheckBox("visible"));

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
                            none = new TagSet();
                            if (FeatureDetailForm.this.getModelObject().getTagset() != null) {
                                return Arrays.asList(FeatureDetailForm.this.getModelObject()
                                        .getTagset());
                            }
                            List<TagSet> allTagSets = annotationService
                                    .listTagSets(selectedProjectModel.getObject());
                            List<TagSet> avalableTagSets = new ArrayList<TagSet>();

                            none.setName("-NONE-");
                            avalableTagSets.add(none);
                            for (TagSet tagSet : allTagSets) {
                                if (tagSet.getFeature() == null) {
                                    avalableTagSets.add(tagSet);
                                }
                            }

                            return avalableTagSets;

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
                protected CharSequence getDefaultChoice(String aSelectedValue)
                {
                    return "";
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

                        if (annotationService.existsFeature(feature.getName(), feature.getLayer(),
                                feature.getProject())) {
                            error("This feature is already added for this layer!");
                            return;
                        }
                        if (feature.getTagset().equals(none)) {
                            feature.setTagset(null);
                        }

                        String name = feature.getUiName();
                        name = name.replaceAll("\\W", "");
                        if (annotationService.existsFeature(name, feature.getLayer(),
                                feature.getProject())) {
                            error("this feature already exists!");
                            return;
                        }
                        feature.setName(name);
                        annotationService.createFeature(feature);
                        featureDetailForm.setVisible(false);
                    }
                    if (tagSet.getModelObject() != null && !tagSet.getModelObject().equals(none)) {
                        FeatureDetailForm.this.getModelObject().setTagset(tagSet.getModelObject());
                        tagSet.getModelObject().setFeature(FeatureDetailForm.this.getModelObject());
                        tagSet.getModelObject().setLayer(layerDetailForm.getModelObject());
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
            });
        }
    }
}

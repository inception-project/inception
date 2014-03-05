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
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationType;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.EntityModel;

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

    private final LayerSelectionForm layerSelectionForm;
    private final FeatureSelectionForm featureSelectionForm;
    private LayerDetailForm layerDetailForm;
    private final FeatureDetailForm featureDetailForm;

    private final Model<Project> selectedProjectModel;

    public ProjectLayersPanel(String id, final Model<Project> aProjectModel)
    {
        super(id);
        this.selectedProjectModel = aProjectModel;
        layerSelectionForm = new LayerSelectionForm("layerSelectionForm");

        featureSelectionForm = new FeatureSelectionForm("featureSelectionForm")
        {
            private static final long serialVersionUID = -2240366326474930943L;

            @Override
            public boolean isVisible()
            {
                return layerSelectionForm.getModelObject().layer != null
                        && layerSelectionForm.getModelObject().layer.getProject().equals(
                                aProjectModel.getObject());
            }
        };

        layerDetailForm = new LayerDetailForm("layerDetailForm");

        featureDetailForm = new FeatureDetailForm("featureDetailForm");

        add(layerSelectionForm);
        add(featureSelectionForm);
        add(layerDetailForm);

        featureDetailForm.setVisible(false);
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
                        LayerSelectionForm.this.getModelObject().layer = null;
                        layerDetailForm.setModelObject(new AnnotationType());
                        layerDetailForm.setVisible(true);
                        featureSelectionForm.setVisible(false);
                        featureDetailForm.setVisible(false);
                    }
                }
            });

            add(new ListChoice<AnnotationType>("layer")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<AnnotationType>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<AnnotationType> load()
                        {
                            Project project = selectedProjectModel.getObject();
                            if (project.getId() != 0) {
                                return annotationService.listAnnotationType(project);
                            }
                            else {
                                return new ArrayList<AnnotationType>();
                            }
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<AnnotationType>()
                    {
                        private static final long serialVersionUID = 8639013729422537472L;

                        @Override
                        public Object getDisplayValue(AnnotationType aObject)
                        {
                            return aObject.getUiName();
                        }
                    });
                    setNullValid(false);
                }

                @Override
                protected void onSelectionChanged(AnnotationType aNewSelection)
                {
                    if (aNewSelection != null) {

                        layerDetailForm.setModelObject(aNewSelection);
                        layerDetailForm.setVisible(true);

                        LayerSelectionForm.this.setVisible(true);

                        featureSelectionForm.clearInput();
                        featureSelectionForm.setVisible(true);
                        featureDetailForm.setVisible(false);

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
            }).setOutputMarkupId(true);

        }
    }

    public class SelectionModel
        implements Serializable
    {
        private static final long serialVersionUID = -1L;

        private AnnotationType layer;
        public AnnotationFeature feature;
    }

    private class LayerDetailForm
        extends Form<AnnotationType>
    {
        private static final long serialVersionUID = -1L;

        DropDownChoice<AnnotationType> attachType;
        DropDownChoice<AnnotationFeature>  attachFeature;

        public LayerDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<AnnotationType>(new EntityModel<AnnotationType>(
                    new AnnotationType())));

            add(new TextField<String>("uiName").setRequired(true));
            add(new TextArea<String>("description").setOutputMarkupPlaceholderTag(true));
            add(new CheckBox("enabled"));
            add(new TextField<String>("labelFeatureName"){
                private static final long serialVersionUID = 4635897231616551669L;

                @Override
                public boolean isEnabled(){
                    return !LayerDetailForm.this.getModelObject().isBuiltIn();
                }
            });

            // Technical Properties of layers
            add(new TextField<String>("name")
            {
                private static final long serialVersionUID = 4635897231616551669L;

                @Override
                public boolean isEnabled()
                {
                    return LayerDetailForm.this.getModelObject().getId() == 0;
                }
            }.setRequired(true));
            add(new DropDownChoice<String>("type", Arrays.asList(new String[] { "span", "relation",
                    "chain" }))
            {
                private static final long serialVersionUID = 1244555334843130802L;

                @Override
                public boolean isEnabled()
                {
                    return LayerDetailForm.this.getModelObject().getId() == 0;
                }
            }.setRequired(true));

            add(attachType = new DropDownChoice<AnnotationType>("attachType")
            {
                private static final long serialVersionUID = -6705445053442011120L;

                {
                    setChoices(new LoadableDetachableModel<List<AnnotationType>>()
                    {
                        private static final long serialVersionUID = 1784646746122513331L;

                        @Override
                        protected List<AnnotationType> load()
                        {
                            return annotationService.listAnnotationType(selectedProjectModel
                                    .getObject());

                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<AnnotationType>()
                    {
                        private static final long serialVersionUID = 8639013729422537472L;

                        @Override
                        public Object getDisplayValue(AnnotationType aObject)
                        {
                            return aObject.getUiName();
                        }
                    });
                }

                @Override
                public boolean isEnabled()
                {
                    return LayerDetailForm.this.getModelObject().getAttachType() == null;
                }
            });

            attachType.add(new OnChangeAjaxBehavior()
            {
                private static final long serialVersionUID = 3617746295701595177L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    aTarget.add(attachFeature.setOutputMarkupId(true));
                }
            }).setOutputMarkupId(true);

            add(attachFeature = new DropDownChoice<AnnotationFeature>("attachFeature")
            {
                private static final long serialVersionUID = -6705445053442011120L;

                {
                    setChoices(new LoadableDetachableModel<List<AnnotationFeature>>()
                    {
                        private static final long serialVersionUID = 1784646746122513331L;

                        @Override
                        protected List<AnnotationFeature> load()
                        {
                            if (attachType.getModelObject() == null) {
                                return new ArrayList<AnnotationFeature>();

                            }
                            return annotationService.listAnnotationFeature(
                                    selectedProjectModel.getObject(), attachType.getModelObject());

                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<AnnotationFeature>()
                    {
                        private static final long serialVersionUID = 8639013729422537472L;

                        @Override
                        public Object getDisplayValue(AnnotationFeature aObject)
                        {
                            return aObject.getUiName();
                        }
                    });
                }

                @Override
                public boolean isEnabled()
                {
                    return LayerDetailForm.this.getModelObject().getAttachFeature() == null;
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
                    AnnotationType layer = LayerDetailForm.this.getModelObject();

                    if (layer.getId() == 0) {
                        if (annotationService.existsLayer(layer.getName(), layer.getType(),
                                selectedProjectModel.getObject())) {
                            error("Only one Layer per project is allowed!");
                        }
                        else {
                            String username = SecurityContextHolder.getContext()
                                    .getAuthentication().getName();
                            User user = projectRepository.getUser(username);

                            layer.setProject(selectedProjectModel.getObject());
                            try {
                                annotationService.createType(layer, user);
                            }
                            catch (IOException e) {
                                error("unable to create Log file while creating the TagSet" + ":"
                                        + ExceptionUtils.getRootCauseMessage(e));
                            }
                            featureSelectionForm.setVisible(true);
                            featureDetailForm.setVisible(true);

                        }
                    }
                }
            });

        }
    }

    private class FeatureDetailForm
        extends Form<AnnotationFeature>
    {
        private static final long serialVersionUID = -1L;

        public FeatureDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<AnnotationFeature>(
                    new EntityModel<AnnotationFeature>(new AnnotationFeature())));

            add(new TextField<String>("uiName").setRequired(true));
            add(new TextArea<String>("description").setOutputMarkupPlaceholderTag(true));
            add(new CheckBox("enabled"));

            add(new DropDownChoice<TagSet>("tagSet",
                    annotationService.listTagSets(selectedProjectModel.getObject()),
                    new ChoiceRenderer<TagSet>("name")));

            add(new TextField<String>("name")
            {
                private static final long serialVersionUID = -4700830083331199546L;

                @Override
                public boolean isEnabled()
                {
                    return FeatureDetailForm.this.getModelObject().getId() == 0;
                }
            }.setRequired(true));

            add(new DropDownChoice<String>("type", Arrays.asList(new String[] { "string",
                    "integer", "float", "boolean", }))
            {
                private static final long serialVersionUID = 6461017582101369158L;

                @Override
                public boolean isEnabled()
                {
                    return FeatureDetailForm.this.getModelObject().getId() == 0;
                }
            }.setRequired(true));

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
                                feature.getTagSet(), feature.getProject())) {
                            error("This feature is already added for this layer!");
                            return;
                        }
                        annotationService.createFeature(feature);
                        featureDetailForm.setVisible(false);
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
                            return annotationService.listAnnotationFeature(
                                    selectedProjectModel.getObject(),
                                    layerDetailForm.getModelObject());
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<AnnotationFeature>()
                    {
                        private static final long serialVersionUID = 4610648616450168333L;

                        @Override
                        public Object getDisplayValue(AnnotationFeature aObject)
                        {
                            String tagSetName = aObject.getTagSet() == null ? "-None-" : aObject
                                    .getTagSet().getName();
                            return "[ "+aObject.getUiName() + "] [ " + aObject.getType() + " ] [ "
                                    + tagSetName +" ]";
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

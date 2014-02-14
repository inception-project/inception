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
    private static final long serialVersionUID = 7004037105647505760L;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;
    @SpringBean(name = "documentRepository")
    private RepositoryService projectRepository;

    DropDownChoice<String> importTagsetFormat;
    DropDownChoice<String> exportTagsetFormat;

    private final LayerSelectionForm layerSelectionForm;
    // private final FeatureSelectionForm featureSelectionForm;
    private LayerDetailForm layerDetailForm;
    // private final FeatureDetailForm featureDetailForm;

    private final Model<Project> selectedProjectModel;

    public ProjectLayersPanel(String id, final Model<Project> aProjectModel)
    {
        super(id);
        this.selectedProjectModel = aProjectModel;
        layerSelectionForm = new LayerSelectionForm("layerSelectionForm");

        /*
         * featureSelectionForm = new FeatureSelectionForm("featureSelectionForm") {
         *
         * private static final long serialVersionUID = -4772415754052553741L;
         *
         * @Override public boolean isVisible() { return layerSelectionForm.getModelObject().tagSet
         * != null && layerSelectionForm.getModelObject().tagSet.getProject().equals(
         * aProjectModel.getObject()); } };
         */

        layerDetailForm = new LayerDetailForm("layerDetailForm");

        /*
         * featureDetailForm = new FeatureDetailForm("featureDetailForm") { private static final
         * long serialVersionUID = 2338721044859355652L;
         *
         * @Override public boolean isVisible() { return layerSelectionForm.getModelObject().tagSet
         * != null && layerSelectionForm.getModelObject().tagSet.getProject().equals(
         * aProjectModel.getObject()); } };
         */

        add(layerSelectionForm);
        // add(featureSelectionForm);
        add(layerDetailForm);
        // add(featureDetailForm);
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
                private static final long serialVersionUID = 1L;

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
                        // featureSelectionForm.setVisible(false);
                        // featureDetailForm.setVisible(false);
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
                        private static final long serialVersionUID = -2000622431037285685L;

                        @Override
                        public Object getDisplayValue(AnnotationType aObject)
                        {
                            return aObject.getName();
                        }
                    });
                    setNullValid(false);
                }

                @Override
                protected void onSelectionChanged(AnnotationType aNewSelection)
                {
                    if (aNewSelection != null) {
                        layerDetailForm.clearInput();
                        layerDetailForm.setModelObject(aNewSelection);
                        layerDetailForm.setVisible(true);
                        LayerSelectionForm.this.setVisible(true);
                        // featureSelectionForm.setVisible(true);
                        // featureDetailForm.setVisible(true);

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
        private AnnotationFeature feature;
    }

    private class LayerDetailForm
        extends Form<AnnotationType>
    {
        private static final long serialVersionUID = -1L;

        public LayerDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<AnnotationType>(new EntityModel<AnnotationType>(
                    new AnnotationType())));

            add(new TextField<String>("uiName").setRequired(true));
            add(new TextArea<String>("description").setOutputMarkupPlaceholderTag(true));
            add(new CheckBox("enabled"));
            add(new TextField<String>("labelFeatureName"));

            add(new TextField<String>("name").setRequired(true));
            add(new DropDownChoice<String>("type", Arrays.asList(new String[] { "span", "relation",
                    "chain" })).setRequired(true));
            add(new DropDownChoice<AnnotationType>("attachType",
                    annotationService.listAnnotationType(selectedProjectModel.getObject()),
                    new ChoiceRenderer<AnnotationType>("uiName")));

            add(new DropDownChoice<AnnotationFeature>("attachFeature",
                    annotationService.listAnnotationFeature(selectedProjectModel.getObject()),
                    new ChoiceRenderer<AnnotationFeature>("uiName")));

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
                            // featureSelectionForm.setVisible(true);
                            // featureDetailForm.setVisible(true);

                        }
                    }
                }
            });

        }
    }

    /*
     * private class FeatureDetailForm extends Form<Tag> { private static final long
     * serialVersionUID = -1L;
     *
     * public FeatureDetailForm(String id) { super(id, new CompoundPropertyModel<Tag>(new
     * EntityModel<Tag>(new Tag()))); // super(id); add(new
     * TextField<String>("name").setRequired(true));
     *
     * add(new TextArea<String>("description").setOutputMarkupPlaceholderTag(true)); add(new
     * Button("save", new ResourceModel("label")) { private static final long serialVersionUID = 1L;
     *
     * @Override public void onSubmit() { Tag tag = FeatureDetailForm.this.getModelObject(); if
     * (tag.getId() == 0) { tag.setTagSet(layerDetailForm.getModelObject()); try {
     * annotationService.getTag(tag.getName(), layerDetailForm.getModelObject());
     * error("This tag is already added for this tagset!"); } catch (NoResultException ex) {
     *
     * String username = SecurityContextHolder.getContext() .getAuthentication().getName(); User
     * user = projectRepository.getUser(username);
     *
     * try { annotationService.createTag(tag, user); } catch (IOException e) {
     * error("unable to create a log file while creating the Tag " + ":" +
     * ExceptionUtils.getRootCauseMessage(e)); } featureDetailForm.setModelObject(new Tag()); } } }
     * });
     *
     * add(new Button("remove", new ResourceModel("label")) { private static final long
     * serialVersionUID = 1L;
     *
     * @Override public void onSubmit() { Tag tag = FeatureDetailForm.this.getModelObject(); if
     * (tag.getId() != 0) { tag.setTagSet(layerDetailForm.getModelObject());
     * annotationService.removeTag(tag); featureDetailForm.setModelObject(new Tag()); } else {
     * FeatureDetailForm.this.setModelObject(new Tag()); } } }); } }
     *
     * public class FeatureSelectionForm extends Form<SelectionModel> { private static final long
     * serialVersionUID = -1L;
     *
     * @SuppressWarnings("unused") private Tag selectedTag; private ListChoice<Tag> layers;
     *
     * public FeatureSelectionForm(String id) { // super(id); super(id, new
     * CompoundPropertyModel<SelectionModel>(new SelectionModel()));
     *
     * add(layers = new ListChoice<Tag>("layers") { private static final long serialVersionUID = 1L;
     *
     * { setChoices(new LoadableDetachableModel<List<Tag>>() { private static final long
     * serialVersionUID = 1L;
     *
     * @Override protected List<Tag> load() { return
     * annotationService.listTags(layerDetailForm.getModelObject()); } }); setChoiceRenderer(new
     * ChoiceRenderer<Tag>() { private static final long serialVersionUID = 4696303692557735150L;
     *
     * @Override public Object getDisplayValue(Tag aObject) { return aObject.getName(); } });
     * setNullValid(false);
     *
     * }
     *
     * @Override protected CharSequence getDefaultChoice(String aSelectedValue) { return ""; } });
     * layers.add(new OnChangeAjaxBehavior() { private static final long serialVersionUID =
     * 7492425689121761943L;
     *
     * @Override protected void onUpdate(AjaxRequestTarget aTarget) { if (getModelObject().tag !=
     * null) { featureDetailForm.setModelObject(getModelObject().tag);
     * aTarget.add(featureDetailForm.setOutputMarkupId(true)); } } }).setOutputMarkupId(true);
     *
     * add(new Button("new", new ResourceModel("label")) { private static final long
     * serialVersionUID = 1L;
     *
     * @Override public void onSubmit() { featureDetailForm.setDefaultModelObject(new Tag()); } });
     * } }
     */
}

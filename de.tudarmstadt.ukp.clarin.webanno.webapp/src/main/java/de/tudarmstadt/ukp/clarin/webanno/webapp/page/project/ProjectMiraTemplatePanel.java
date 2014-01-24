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
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.project;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.brat.util.AutomationUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.MiraTemplate;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.webapp.support.EntityModel;

/**
 * A Panel used to define automation properties for the {@link MIRA} machine learning algorithm
 *
 * @author Seid Muhie Yimam
 *
 */
public class ProjectMiraTemplatePanel
    extends Panel
{
    private static final long serialVersionUID = 2116717853865353733L;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "userRepository")
    private UserDao userRepository;

    private final MiraTemplateSelectionForm miraTemplateSelectionForm;
    private final MiraTemplateDetailForm miraTemplateDetailForm;
    private ProjectDocumentsPanel documentsPanel;
    private final ApplyForm applyForm;

    private final Model<Project> selectedProjectModel;

    public ProjectMiraTemplatePanel(String id, final Model<Project> aProjectModel)
    {
        super(id);
        this.selectedProjectModel = aProjectModel;
        miraTemplateSelectionForm = new MiraTemplateSelectionForm("miraTemplateSelectionForm");
        add(miraTemplateSelectionForm);

        miraTemplateDetailForm = new MiraTemplateDetailForm("miraTemplateDetailForm");
        miraTemplateDetailForm.setVisible(false);
        add(miraTemplateDetailForm);

        documentsPanel = new ProjectDocumentsPanel("documentsPanel", aProjectModel, miraTemplate);
        documentsPanel.setVisible(false);
        add(documentsPanel);

        applyForm = new ApplyForm("applyForm");
        applyForm.setVisible(false);
        add(applyForm);

    }

    Model<MiraTemplate> miraTemplate = new Model<MiraTemplate>()
    {
        private static final long serialVersionUID = -6394439155356911110L;

        @Override
        public MiraTemplate getObject()
        {
            return miraTemplateDetailForm.getModelObject();
        }
    };

    void update(ProjectDocumentsPanel aDcumentsPanel)
    {
        documentsPanel.remove();
        documentsPanel = new ProjectDocumentsPanel("documentsPanel", selectedProjectModel,
                miraTemplate);
        add(documentsPanel);
    }

    private class MiraTemplateSelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1528847861284911270L;

        public MiraTemplateSelectionForm(String id)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));
            add(new ListChoice<MiraTemplate>("template")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<MiraTemplate>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<MiraTemplate> load()
                        {
                            Project project = selectedProjectModel.getObject();
                            if (project.getId() != 0) {
                                return repository.listMiraTemplates(project);
                            }
                            else {
                                return new ArrayList<MiraTemplate>();
                            }
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<MiraTemplate>()
                    {
                        private static final long serialVersionUID = -2000622431037285685L;

                        @Override
                        public Object getDisplayValue(MiraTemplate aObject)
                        {
                            return "[" + aObject.getTrainTagSet().getType().getName() + "] "
                                    + aObject.getTrainTagSet().getName();
                        }
                    });
                    setNullValid(false);
                }

                @Override
                protected void onSelectionChanged(MiraTemplate aNewSelection)
                {
                    miraTemplateDetailForm.clearInput();
                    miraTemplateDetailForm.setModelObject(aNewSelection);
                    miraTemplateDetailForm.setVisible(true);
                    documentsPanel.setVisible(true);
                    applyForm.setVisible(true);
                    miraTemplate.setObject(aNewSelection);
                    update(documentsPanel);
                }

                @Override
                protected boolean wantOnSelectionChangedNotifications()
                {
                    return true;
                }

                @Override
                protected CharSequence getDefaultChoice(String aSelectedValue)
                {
                    return aSelectedValue;
                }
            }).setOutputMarkupId(true);

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
                        MiraTemplateSelectionForm.this.getModelObject().template = null;
                        miraTemplateDetailForm.setModelObject(new MiraTemplate());
                        miraTemplateDetailForm.setVisible(true);
                    }
                }
            });
        }

    }

    private class MiraTemplateDetailForm
        extends Form<MiraTemplate>
    {
        private static final long serialVersionUID = -683824912741426241L;

        public MiraTemplateDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<MiraTemplate>(new EntityModel<MiraTemplate>(
                    new MiraTemplate())));

            add(new CheckBox("capitalized"));
            add(new CheckBox("containsNumber"));
            add(new CheckBox("prefix1"));
            add(new CheckBox("prefix2"));
            add(new CheckBox("prefix3"));
            add(new CheckBox("prefix4"));
            add(new CheckBox("prefix5"));
            add(new CheckBox("suffix1"));
            add(new CheckBox("suffix2"));
            add(new CheckBox("suffix3"));
            add(new CheckBox("suffix4"));
            add(new CheckBox("suffix5"));

            add(new DropDownChoice<Integer>("ngram", Arrays.asList(new Integer[] { 1, 2, 3 })));

            add(new DropDownChoice<Integer>("bigram", Arrays.asList(new Integer[] { 1, 2, 3 })));

            // checkbox to limit prediction of annotation on same page or not, for Automation page

            add(new CheckBox("predictInThisPage"));

            List<TagSet> allTagSets = annotationService.listTagSets(selectedProjectModel
                    .getObject());
            List<TagSet> spanTagSets = new ArrayList<TagSet>();
            for (TagSet tagSet : allTagSets) {
                if (tagSet.getType().getType().equals("span")) {
                    spanTagSets.add(tagSet);
                }
            }
            add(new DropDownChoice<TagSet>("trainTagSet", spanTagSets, new ChoiceRenderer<TagSet>(
                    "name")).setRequired(true));

            add(new CheckBoxMultipleChoice<TagSet>("featureTagSets", spanTagSets,
                    new ChoiceRenderer<TagSet>("name")));

            add(new CheckBox("annotateAndPredict"));
            add(new Button("save", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    MiraTemplate template = MiraTemplateDetailForm.this.getModelObject();
                    if (template.getFeatureTagSets().contains(template.getTrainTagSet())) {
                        error("A feature train layers should not contain the train layer as a feature");
                        template.getFeatureTagSets().remove(template.getTrainTagSet());
                    }
                    else if (template.getId() == 0) {
                        if (repository.existsMiraTemplate(template.getTrainTagSet())) {
                            error("A template already exists.");
                        }
                        else {
                            repository.createTemplate(template);
                            documentsPanel.setVisible(true);
                            applyForm.setVisible(true);
                            miraTemplate.setObject(MiraTemplateDetailForm.this.getModelObject());
                            update(documentsPanel);
                        }
                    }
                }
            });
        }
    }

    @SuppressWarnings("rawtypes")
    private class ApplyForm
        extends Form
    {
        private static final long serialVersionUID = -683824912741426241L;

        public ApplyForm(String id)
        {
            super(id);
            add(new Button("apply", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    try {

                        boolean trained = AutomationUtil.casToMiraTrainData(
                                miraTemplateDetailForm.getModelObject(), repository);
                        if (!trained) {
                            miraTemplateDetailForm.getModelObject().setResult(
                                    AutomationUtil.train(miraTemplateDetailForm.getModelObject(),
                                            repository));
                        }
                        AutomationUtil.predict(miraTemplateDetailForm.getModelObject(), repository,
                                annotationService);
                    }
                    catch (UIMAException e) {
                        error(ExceptionUtils.getRootCause(e));
                    }
                    catch (ClassNotFoundException e) {
                        error(e.getMessage());
                    }
                    catch (IOException e) {
                        error(e.getMessage());
                    }
                    catch (BratAnnotationException e) {
                        error(e.getMessage());
                    }
                }
            });

        }
    }

    public class SelectionModel
        implements Serializable
    {
        private static final long serialVersionUID = -4905538356691404575L;
        public MiraTemplate template;

    }
}

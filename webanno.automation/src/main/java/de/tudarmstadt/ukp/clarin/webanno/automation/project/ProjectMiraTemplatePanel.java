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
package de.tudarmstadt.ukp.clarin.webanno.automation.project;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
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
import de.tudarmstadt.ukp.clarin.webanno.automation.util.AutomationUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.MiraTemplate;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.EntityModel;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

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
    @SuppressWarnings("unused")
    private final OtherFeatureDeatilForm otherFeatureDeatilForm;
    private ProjectTrainingDocumentsPanel trainFeatureDocumentsPanel;
    private ProjectTrainingDocumentsPanel otherTrainFeatureDocumentsPanel;

    private boolean isLayerDetail = true;
    private final Model<Project> selectedProjectModel;

    private Model<AnnotationFeature> featureModel = new Model<AnnotationFeature>();
    private Model<AnnotationFeature> otherFeatureModel = new Model<AnnotationFeature>();

    private AnnotationFeature selectedFeature = new AnnotationFeature();
    private MiraTemplate templaet = new MiraTemplate();

    @SuppressWarnings("unused")
    private final ApplyForm applyForm;
    private DropDownChoice<AnnotationFeature> features;

    public ProjectMiraTemplatePanel(String id, final Model<Project> aProjectModel)
    {
        super(id);
        this.selectedProjectModel = aProjectModel;
        for (MiraTemplate template : repository.listMiraTemplates(selectedProjectModel.getObject())) {
            if (template.isCurrentLayer()) {
                this.templaet = template;
                selectedFeature = template.getTrainFeature();
                break;
            }
        }
        featureModel.setObject(selectedFeature);
        miraTemplateSelectionForm = new MiraTemplateSelectionForm("miraTemplateSelectionForm");
        add(miraTemplateSelectionForm);

        add(miraTemplateDetailForm = new MiraTemplateDetailForm("miraTemplateDetailForm")
        {
            private static final long serialVersionUID = -4722848235169124717L;

            @Override
            public boolean isVisible()
            {
                return selectedFeature.getId() != 0 && isLayerDetail;
            }
        });
        miraTemplateDetailForm.setModelObject(templaet);

        add(otherFeatureDeatilForm = new OtherFeatureDeatilForm("otherFeatureDeatilForm")
        {
            private static final long serialVersionUID = 3192960675893574547L;

            @Override
            public boolean isVisible()
            {
                return selectedFeature.getId() != 0 && !isLayerDetail;
            }
        });

        add(trainFeatureDocumentsPanel = new ProjectTrainingDocumentsPanel(
                "trainFeatureDocumentsPanel", aProjectModel, featureModel)
        {

            private static final long serialVersionUID = 7698999083009818310L;

            @Override
            public boolean isVisible()
            {
                return selectedFeature.getId() != 0 && isLayerDetail;
            }
        });
        trainFeatureDocumentsPanel.setOutputMarkupPlaceholderTag(true);

        add(otherTrainFeatureDocumentsPanel = new ProjectTrainingDocumentsPanel(
                "otherTrainFeatureDocumentsPanel", aProjectModel, otherFeatureModel)
        {
            private static final long serialVersionUID = -4663938706290521594L;

            @Override
            public boolean isVisible()
            {
                return selectedFeature.getId() != 0 && !isLayerDetail;
            }
        });
        otherTrainFeatureDocumentsPanel.setOutputMarkupPlaceholderTag(true);

        add(applyForm = new ApplyForm("applyForm")
        {
            private static final long serialVersionUID = 3866085992209480718L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                if (templaet.getId() == 0) {
                    this.setVisible(false);
                }
                else {
                    this.setVisible(true);
                }
            }
        });

    }

    private class MiraTemplateSelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1528847861284911270L;

        public MiraTemplateSelectionForm(String id)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));
            final Project project = selectedProjectModel.getObject();

            add(features = new DropDownChoice<AnnotationFeature>("features")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<AnnotationFeature>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<AnnotationFeature> load()
                        {
                            List<AnnotationFeature> allFeatures = annotationService
                                    .listAnnotationFeature(project);
                            List<AnnotationFeature> spanFeatures = new ArrayList<AnnotationFeature>();
                            for (AnnotationFeature feature : allFeatures) {
                                if (feature.getLayer().getName().equals(Token.class.getName())) {
                                    continue;
                                }
                                if (feature.getLayer().getType().equals(WebAnnoConst.SPAN_TYPE)) {
                                    spanFeatures.add(feature);
                                }
                            }
                            return spanFeatures;
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<AnnotationFeature>()
                    {
                        private static final long serialVersionUID = -2000622431037285685L;

                        @Override
                        public Object getDisplayValue(AnnotationFeature aObject)
                        {
                            return "[ " + aObject.getLayer().getUiName() + "] "
                                    + aObject.getUiName();
                        }
                    });
                    setNullValid(false);
                }

                @Override
                public void onSelectionChanged(AnnotationFeature aNewSelection)
                {
                    selectedFeature = (AnnotationFeature) aNewSelection;
                    if (repository.existsMiraTemplate(selectedFeature)) {
                        templaet = repository.getMiraTemplate(selectedFeature);
                    }
                    else {
                        templaet = new MiraTemplate();
                        templaet.setTrainFeature((AnnotationFeature) aNewSelection);
                    }
                    featureModel.setObject(selectedFeature);
                    isLayerDetail = true;
                    updateTrainFeatureDocumentsPanel(trainFeatureDocumentsPanel);
                    updateTrainFeatureDocumentsPanel(otherTrainFeatureDocumentsPanel);
                    miraTemplateDetailForm.setModelObject(templaet);
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
            features.setModelObject(selectedFeature);
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

            add(new CheckBox("predictInThisPage"));

            add(new CheckBox("annotateAndPredict"));

            add(new Button("save", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    templaet = MiraTemplateDetailForm.this.getModelObject();
                    if (templaet.getId() == 0) {
                        templaet.setTrainFeature(selectedFeature);
                        repository.createTemplate(templaet);
                        featureModel.setObject(MiraTemplateDetailForm.this.getModelObject()
                                .getTrainFeature());
                    }
                    templaet.setCurrentLayer(true);
                    for (MiraTemplate template : repository.listMiraTemplates(selectedProjectModel
                            .getObject())) {
                        if (template.equals(templaet)) {
                            continue;
                        }
                        if (template.isCurrentLayer()) {
                            template.setCurrentLayer(false);
                        }
                    }
                }
            });
        }
    }

    /**
     * {@link AnnotationFeature} used as a feature for the current training layer
     *
     */
    private class OtherFeatureDeatilForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -683824912741426241L;

        public OtherFeatureDeatilForm(String id)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

            add(new DropDownChoice<AnnotationFeature>("features")
            {
                private static final long serialVersionUID = 1L;

                {
                    setNullValid(true);
                    setChoices(new LoadableDetachableModel<List<AnnotationFeature>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<AnnotationFeature> load()
                        {
                            List<AnnotationFeature> features = annotationService
                                    .listAnnotationFeature(selectedProjectModel.getObject());
                            features.remove(miraTemplateDetailForm.getModelObject()
                                    .getTrainFeature());
                            features.removeAll(miraTemplateDetailForm.getModelObject()
                                    .getOtherFeatures());
                            for (AnnotationFeature feature : annotationService
                                    .listAnnotationFeature(selectedProjectModel.getObject())) {
                                if (!feature.getLayer().getType().equals(WebAnnoConst.SPAN_TYPE)
                                        || feature.getLayer().getName()
                                                .equals(Token.class.getName())) {
                                    features.remove(feature);
                                }
                            }
                            return features;
                        }
                    });

                    setChoiceRenderer(new ChoiceRenderer<AnnotationFeature>()
                    {
                        private static final long serialVersionUID = 4607720784161484145L;

                        @Override
                        public Object getDisplayValue(AnnotationFeature aObject)
                        {
                            return "[ " + aObject.getLayer().getUiName() + "] "
                                    + aObject.getUiName();
                        }
                    });
                }

                @Override
                protected void onSelectionChanged(AnnotationFeature aNewSelection)
                {
                    miraTemplateDetailForm.getModelObject().getOtherFeatures().add(aNewSelection);
                }

                @Override
                protected boolean wantOnSelectionChangedNotifications()
                {
                    return true;
                }

            });

            add(new ListChoice<AnnotationFeature>("selectedFeatures")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<AnnotationFeature>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<AnnotationFeature> load()
                        {
                            return new ArrayList<AnnotationFeature>(miraTemplateDetailForm
                                    .getModelObject().getOtherFeatures());
                        }
                    });

                    setChoiceRenderer(new ChoiceRenderer<AnnotationFeature>()
                    {
                        private static final long serialVersionUID = 4607720784161484145L;

                        @Override
                        public Object getDisplayValue(AnnotationFeature aObject)
                        {
                            return "[ " + aObject.getLayer().getUiName() + "] "
                                    + aObject.getUiName();
                        }
                    });
                    setNullValid(false);
                }

                @Override
                protected void onSelectionChanged(AnnotationFeature aNewSelection)
                {
                    otherFeatureModel.setObject(aNewSelection);
                    updateOtherFeatureDocumentsPanel(otherTrainFeatureDocumentsPanel);
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
                        MiraTemplate template = miraTemplateDetailForm.getModelObject();

                        AutomationUtil.addOtherFeatureTrainDocument(template, repository);
                        AutomationUtil.otherFeatureClassifiers(template, repository);

                        AutomationUtil.generateTrainDocument(template, repository, true);
                        AutomationUtil.generatePredictDocument(template, repository);

                        AutomationUtil.addOtherFeatureToTrainDocument(template, repository);
                        AutomationUtil.addOtherFeatureToPredictDocument(template, repository);


                        long start = System.nanoTime();
                        boolean trained = AutomationUtil.casToMiraTrainData(template, repository);
                        long time = System.nanoTime() - start;
                        System.out.println("conversion took:" + time / 1000 + " seconds");
                        start = System.nanoTime();
                        if (!trained) {
                            miraTemplateDetailForm.getModelObject().setResult(
                                    AutomationUtil.train(template, repository));
                        }
                        time = System.nanoTime() - start;
                        System.out.println("tarining took:" + time / 1000 + " seconds");
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

            add(new Button("layerDetails", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    isLayerDetail = true;
                    updateTrainFeatureDocumentsPanel(trainFeatureDocumentsPanel);
                    updateTrainFeatureDocumentsPanel(otherTrainFeatureDocumentsPanel);
                }
            });

            add(new Button("addOtherLayer", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    if (miraTemplateDetailForm.getModelObject().getId() == 0) {
                        error("Please save the training layer detail first");
                        return;
                    }
                    isLayerDetail = false;
                    updateTrainFeatureDocumentsPanel(trainFeatureDocumentsPanel);
                    updateTrainFeatureDocumentsPanel(otherTrainFeatureDocumentsPanel);
                }
            });
        }
    }

    void updateTrainFeatureDocumentsPanel(ProjectTrainingDocumentsPanel aDcumentsPanel)
    {
        trainFeatureDocumentsPanel.remove();
        add(trainFeatureDocumentsPanel = new ProjectTrainingDocumentsPanel(
                "trainFeatureDocumentsPanel", selectedProjectModel, featureModel)
        {

            private static final long serialVersionUID = 7698999083009818310L;

            @Override
            public boolean isVisible()
            {
                return selectedFeature.getId() != 0 && isLayerDetail;
            }
        });
    }

    void updateOtherFeatureDocumentsPanel(ProjectTrainingDocumentsPanel aDcumentsPanel)
    {
        otherTrainFeatureDocumentsPanel.remove();
        add(otherTrainFeatureDocumentsPanel = new ProjectTrainingDocumentsPanel(
                "otherTrainFeatureDocumentsPanel", selectedProjectModel, otherFeatureModel)
        {

            private static final long serialVersionUID = 7698999083009818310L;

            @Override
            public boolean isVisible()
            {
                return selectedFeature.getId() != 0 && !isLayerDetail;
            }
        });
    }

    public class SelectionModel
        implements Serializable
    {
        private static final long serialVersionUID = -4905538356691404575L;
        public AnnotationFeature features = new AnnotationFeature();
        public AnnotationFeature selectedFeatures = new AnnotationFeature();

    }
}

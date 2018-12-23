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
package de.tudarmstadt.ukp.clarin.webanno.ui.automation.project;

import static java.util.Objects.isNull;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CorrectionDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.automation.model.AutomationStatus;
import de.tudarmstadt.ukp.clarin.webanno.automation.model.MiraTemplate;
import de.tudarmstadt.ukp.clarin.webanno.automation.service.AutomationService;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Status;
import de.tudarmstadt.ukp.clarin.webanno.model.TrainDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.TrainingDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.EntityModel;
import de.tudarmstadt.ukp.clarin.webanno.support.bootstrap.BootstrapAjaxTabbedPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.automation.util.AutomationUtil;
import de.tudarmstadt.ukp.clarin.webanno.ui.automation.util.TabSepDocModel;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * A Panel used to define automation properties for the {@code MIRA} machine learning algorithm
 */
public class ProjectMiraTemplatePanel
    extends ProjectSettingsPanelBase
{
    private static final long serialVersionUID = 2116717853865353733L;

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean AutomationService automationService;
    private @SpringBean DocumentService documentService;
    private @SpringBean CurationDocumentService curationDocumentService;
    private @SpringBean CorrectionDocumentService correctionDocumentService;
    private @SpringBean UserDao userRepository;

    private MiraTrainLayerSelectionForm miraTrainLayerSelectionForm;
    private MiraTemplateDetailForm miraTemplateDetailForm;
    private OtherLayerDeatilForm otherLayerDetailForm;
    private ProjectTrainingDocumentsPanel targetLayerTarinDocumentsPanel;
    private ProjectTrainingDocumentsPanel otherLayerTarinDocumentsPanel;
    private ProjectTrainingDocumentsPanel freeTrainDocumentsPanel;
    private TargetLaerDetailForm targetLayerDetailForm;

    private Model<AnnotationFeature> featureModel = new Model<>();

    private AnnotationFeature selectedFeature;
    private AnnotationFeature otherSelectedFeature = new AnnotationFeature();
    private MiraTemplate template = new MiraTemplate();

    @SuppressWarnings("unused")
    private final ApplyForm applyForm;
    private ListChoice<AnnotationFeature> features;
    private DropDownChoice<AnnotationFeature> otherFeatures;

    public ProjectMiraTemplatePanel(String id, final IModel<Project> aProjectModel)
    {
        super(id, aProjectModel);

        for (MiraTemplate template : automationService
                .listMiraTemplates(ProjectMiraTemplatePanel.this.getModelObject())) {
            if (template.isCurrentLayer()) {
                this.template = template;
                selectedFeature = template.getTrainFeature();
                break;
            }
        }
        featureModel.setObject(selectedFeature);
        miraTrainLayerSelectionForm = new MiraTrainLayerSelectionForm("miraTrainLayerSelectionForm");
        add(miraTrainLayerSelectionForm);

        updateForm();

        add(applyForm = new ApplyForm("applyForm")
        {
            private static final long serialVersionUID = 3866085992209480718L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                
                if (isNull(template.getId())) {
                    this.setVisible(false);
                }
                else {
                    this.setVisible(true);
                }
            }
        });

    }

    private void updateForm()
    {
        if (targetLayerDetailForm != null) {
            targetLayerDetailForm.remove();
        }
        targetLayerDetailForm = new TargetLaerDetailForm("targetLayerDetailForm");
        targetLayerDetailForm.setOutputMarkupPlaceholderTag(true);
        add(targetLayerDetailForm);
    }

    private class MiraTrainLayerSelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1528847861284911270L;

        public MiraTrainLayerSelectionForm(String id)
        {
            super(id, new CompoundPropertyModel<>(new SelectionModel()));
            final Project project = ProjectMiraTemplatePanel.this.getModelObject();

            add(features = new ListChoice<AnnotationFeature>("features")
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
                            List<AnnotationFeature> spanFeatures = new ArrayList<>();

                            for (AnnotationFeature feature : allFeatures) {
                                if (!feature.getLayer().isEnabled()
                                        || feature.getLayer().getName()
                                                .equals(Token.class.getName())
                                        || feature.getLayer().getName()
                                                .equals(Lemma.class.getName())) {
                                    continue;
                                }
                                // if (feature.getLayer().getType().equals(WebAnnoConst.SPAN_TYPE))
                                // {
                                spanFeatures.add(feature);
                                // }
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
                            return "[ "
                                    + aObject.getLayer().getUiName()
                                    + "] "
                                    + (aObject.getTagset() != null ? aObject.getTagset().getName()
                                            : aObject.getUiName());
                        }
                    });
                    setNullValid(false);
                    
                    add(new FormComponentUpdatingBehavior() {
                        private static final long serialVersionUID = 3955427526154717786L;

                        @Override
                        protected void onUpdate() {
                            selectedFeature = getModelObject();
                            if (automationService.existsMiraTemplate(selectedFeature)) {
                                template = automationService.getMiraTemplate(selectedFeature);
                            }
                            else {
                                template = new MiraTemplate();
                                template.setTrainFeature(getModelObject());
                            }
                            featureModel.setObject(selectedFeature);
                            miraTemplateDetailForm.setModelObject(template);
                        };
                    });
                }
            }).setOutputMarkupId(true);
            features.setModelObject(selectedFeature);
        }

    }

    public class TargetLaerDetailForm
        extends Form<MiraTemplate>
    {
        private static final long serialVersionUID = -4655869081345550397L;
        @SuppressWarnings("rawtypes")
        private BootstrapAjaxTabbedPanel<ITab> autoTabs;

        public TargetLaerDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<>(new EntityModel<>(
                    new MiraTemplate())));

            List<ITab> tabs = new ArrayList<>();
            tabs.add(new AbstractTab(new Model<>("Target layer"))
            {
                private static final long serialVersionUID = 6703144434578403272L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new TargetLayerPanel(panelId);
                }
            });

            tabs.add(new AbstractTab(new Model<>("TAB-SEP target"))
            {
                private static final long serialVersionUID = 6703144434578403272L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new FreeTabSepAsTargetDocumentsPanel(panelId);
                }
            });
            tabs.add(new AbstractTab(new Model<>("Other layers"))
            {
                private static final long serialVersionUID = 6703144434578403272L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new OtherLayerPanel(panelId);
                }
            });

            tabs.add(new AbstractTab(new Model<>("TAB-SEP feature"))
            {
                private static final long serialVersionUID = 6703144434578403272L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new FreeTabSepAsFeatureDocumentsPanel(panelId);
                }
            });

            add(autoTabs = new BootstrapAjaxTabbedPanel<>("autoTabs", tabs));
            autoTabs.setOutputMarkupPlaceholderTag(true);
        }
    }

    private class TargetLayerPanel
        extends Panel
    {
        private static final long serialVersionUID = 7336140137139501974L;

        public TargetLayerPanel(String id)
        {
            super(id);
            add(miraTemplateDetailForm = new MiraTemplateDetailForm("miraTemplateDetailForm")
            {
                private static final long serialVersionUID = 1885112841649058536L;

                @Override
                public boolean isVisible()
                {
                    return selectedFeature != null;
                }
            });
            miraTemplateDetailForm.setModelObject(template);

            add(targetLayerTarinDocumentsPanel = new ProjectTrainingDocumentsPanel(
                    "targetLayerTarinDocumentsPanel", ProjectMiraTemplatePanel.this.getModel(),
                    new Model<>(new TabSepDocModel(false, false)), featureModel)
            {

                private static final long serialVersionUID = 7698999083009818310L;

                @Override
                public boolean isVisible()
                {
                    return miraTemplateDetailForm.getModelObject().getId() != null;
                }
            });
            targetLayerTarinDocumentsPanel.setOutputMarkupPlaceholderTag(true);
        }
    }

    private class OtherLayerPanel
        extends Panel
    {
        private static final long serialVersionUID = -3649285837217362203L;

        public OtherLayerPanel(String id)
        {
            super(id);
            add(otherLayerDetailForm = new OtherLayerDeatilForm("otherLayerDetailForm")
            {
                private static final long serialVersionUID = 3192960675893574547L;

                @Override
                public boolean isVisible()
                {
                    return miraTemplateDetailForm.getModelObject().getId() != null
                            && selectedFeature != null;
                }
            });
            SelectionModel selectedOtherModel = new SelectionModel();
            selectedOtherModel.selectedFeatures = otherSelectedFeature;
            selectedOtherModel.features = null;
            otherLayerDetailForm.setModelObject(selectedOtherModel);

            add(otherLayerTarinDocumentsPanel = new ProjectTrainingDocumentsPanel(
                    "otherLayerTarinDocumentsPanel", ProjectMiraTemplatePanel.this.getModel(),
                    new Model<>(new TabSepDocModel(false, false)),
                    Model.of(otherLayerDetailForm.getModelObject().selectedFeatures))
            {
                private static final long serialVersionUID = -4663938706290521594L;

                @Override
                public boolean isVisible()
                {
                    return otherLayerDetailForm.getModelObject().selectedFeatures.getId() != null;
                }
            });
            otherLayerTarinDocumentsPanel.setOutputMarkupPlaceholderTag(true);
        }
    }

    private class FreeTabSepAsFeatureDocumentsPanel
        extends Panel
    {
        private static final long serialVersionUID = -9173687919199803381L;

        public FreeTabSepAsFeatureDocumentsPanel(String id)
        {
            super(id);
            add(freeTrainDocumentsPanel = new ProjectTrainingDocumentsPanel(
                    "freeTabSepAsFeatureDocumentsPanel", ProjectMiraTemplatePanel.this.getModel(),
                    new Model<>(new TabSepDocModel(false, true)), featureModel)
            {
                private static final long serialVersionUID = -4663938706290521594L;

                @Override
                public boolean isVisible()
                {
                    return miraTemplateDetailForm.getModelObject().getId() != null
                            && selectedFeature != null;
                }
            });
            freeTrainDocumentsPanel.setOutputMarkupPlaceholderTag(true);
        }
    }

    private class FreeTabSepAsTargetDocumentsPanel
        extends Panel
    {
        private static final long serialVersionUID = -9173687919199803381L;

        public FreeTabSepAsTargetDocumentsPanel(String id)
        {
            super(id);
            add(freeTrainDocumentsPanel = new ProjectTrainingDocumentsPanel(
                    "freeTabSepAsTargetDocumentsPanel", ProjectMiraTemplatePanel.this.getModel(),
                    new Model<>(new TabSepDocModel(true, true)), featureModel)
            {
                private static final long serialVersionUID = -4663938706290521594L;

                @Override
                public boolean isVisible()
                {
                    return miraTemplateDetailForm.getModelObject().getId() != null
                            && selectedFeature != null;
                }
            });
            freeTrainDocumentsPanel.setOutputMarkupPlaceholderTag(true);
        }
    }

    private class MiraTemplateDetailForm
        extends Form<MiraTemplate>
    {
        private static final long serialVersionUID = -683824912741426241L;

        public MiraTemplateDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<>(new EntityModel<>(
                    new MiraTemplate())));

            add(new CheckBox("annotateAndRepeat"));

            add(new Button("save", new StringResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    template = MiraTemplateDetailForm.this.getModelObject();
                    if (isNull(template.getId())) {
                        template.setTrainFeature(selectedFeature);
                        automationService.createTemplate(template);
                        featureModel.setObject(
                                MiraTemplateDetailForm.this.getModelObject().getTrainFeature());
                    }
                    template.setCurrentLayer(true);
                    for (MiraTemplate tmp : automationService
                            .listMiraTemplates(ProjectMiraTemplatePanel.this.getModelObject())) {
                        if (tmp.equals(template)) {
                            continue;
                        }
                        if (tmp.isCurrentLayer()) {
                            tmp.setCurrentLayer(false);
                        }
                    }
                    updateForm();
                }
            });
        }
    }

    /**
     * {@link AnnotationFeature} used as a feature for the current training layer
     *
     */
    private class OtherLayerDeatilForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -683824912741426241L;

        public OtherLayerDeatilForm(String id)
        {
            super(id, new CompoundPropertyModel<>(new SelectionModel()));

            add(otherFeatures = new BootstrapSelect<AnnotationFeature>("features")
            {
                private static final long serialVersionUID = -1923453084703805794L;

                {
                    setNullValid(false);
                    setChoices(new LoadableDetachableModel<List<AnnotationFeature>>()
                    {
                        private static final long serialVersionUID = -6376636005341159307L;

                        @Override
                        protected List<AnnotationFeature> load()
                        {
                            Project project = ProjectMiraTemplatePanel.this.getModelObject();
                            List<AnnotationFeature> features = annotationService
                                    .listAnnotationFeature(project);
                            features.remove(miraTemplateDetailForm.getModelObject()
                                    .getTrainFeature());
                            features.removeAll(miraTemplateDetailForm.getModelObject()
                                    .getOtherFeatures());
                            for (AnnotationFeature feature : annotationService
                                    .listAnnotationFeature(project)) {
                                if (!feature.getLayer().isEnabled()
                                        || !feature.getLayer().getType()
                                                .equals(WebAnnoConst.SPAN_TYPE)
                                        || feature.getLayer().getName()
                                                .equals(Lemma.class.getName())
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
                            return "[ "
                                    + aObject.getLayer().getUiName()
                                    + "] "
                                    + (aObject.getTagset() != null ? aObject.getTagset().getName()
                                            : aObject.getUiName());
                        }
                    });
                    
                    add(new FormComponentUpdatingBehavior() {
                        private static final long serialVersionUID = -2174515180334311824L;

                        @Override
                        protected void onUpdate() {
                            miraTemplateDetailForm.getModelObject().getOtherFeatures()
                                    .add(getModelObject());
                            automationService
                                    .createTemplate(miraTemplateDetailForm.getModelObject());
                        };
                    });
                }
            });
            otherFeatures.setModelObject(null);// always force to choose, even
                                               // after selection of
                                               // feature

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
                            return new ArrayList<>(miraTemplateDetailForm
                                    .getModelObject().getOtherFeatures());
                        }
                    });

                    setChoiceRenderer(new ChoiceRenderer<AnnotationFeature>()
                    {
                        private static final long serialVersionUID = 4607720784161484145L;

                        @Override
                        public Object getDisplayValue(AnnotationFeature aObject)
                        {
                            return "[ "
                                    + aObject.getLayer().getUiName()
                                    + "] "
                                    + (aObject.getTagset() != null ? aObject.getTagset().getName()
                                            : aObject.getUiName());
                        }
                    });
                    setNullValid(false);
                    
                    add(new FormComponentUpdatingBehavior()
                    {
                        private static final long serialVersionUID = 7001921645015996995L;

                        @Override
                        protected void onUpdate()
                        {
                            otherSelectedFeature = getModelObject();
                            // always force to choose, even after selection of feature
                            otherFeatures.setModelObject(null);
                            updateForm();
                            targetLayerDetailForm.autoTabs.setSelectedTab(2);
                        };
                    });
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

            add(new IndicatingAjaxButton("apply", new StringResourceModel("label"))
            {

                private static final long serialVersionUID = 1L;

                @Override
                protected void onSubmit(AjaxRequestTarget aTarget)
                {
                    MiraTemplate template = miraTemplateDetailForm.getModelObject();
                    if (!template.getTrainFeature().getLayer().getType()
                            .equals(WebAnnoConst.SPAN_TYPE)) {
                        aTarget.addChildren(getPage(), IFeedback.class);
                        error("Relation automation is not supported yet, but you can use the copy annotator.");
                        // No support yet for relation automation
                        return;
                    }
                    AutomationStatus automationStatus = new AutomationStatus();
                    try {
                        Project project = ProjectMiraTemplatePanel.this.getModelObject();
                        
                        // no training document is added / no curation is done yet!
                        boolean existsTrainDocument = false;
                        for (TrainingDocument document : automationService
                                .listTrainingDocuments(project)) {
                            if (document.getState().equals(TrainDocumentState.CURATION_IN_PROGRESS)
                                    || template.getTrainFeature().equals(document.getFeature())) {
                                existsTrainDocument = true;
                                break;
                            }
                        }
                        if (automationService.listTabSepDocuments(project).size() > 0) {
                            existsTrainDocument = true;
                        }

                        if (!existsTrainDocument) {
                            error("No training document exists to proceed.");
                            aTarget.appendJavaScript("alert('No training document exists to proceed.')");
                            return;
                        }
                        if (!template.isCurrentLayer()) {
                            error("Please save automation layer details to proceed.");
                            aTarget.appendJavaScript("alert('Please save automation layer details to proceed.')");
                            return;
                        }

                        // no need to re-train if no new document is added
                        boolean existUnprocessedDocument = false;
                        for (SourceDocument document : documentService
                                .listSourceDocuments(project)) {
                            if (document.getState().equals(SourceDocumentState.CURATION_FINISHED)) {
                                existUnprocessedDocument = true;
                                break;
                            }
                        }
                        for (TrainingDocument document : automationService
                                .listTrainingDocuments(project)) {
                            if (!document.isProcessed()) {
                                existUnprocessedDocument = true;
                                break;
                            }
                        }
                        if (!existUnprocessedDocument) {
                            error("No new training/annotation document added.");
                            aTarget.appendJavaScript("alert('No new training/annotation document added.')");
                            return;
                        }

                        int annodoc = 0, trainDoc = 0;

                        for (SourceDocument document : documentService
                                .listSourceDocuments(project)) {
                            if (document.getState().equals(SourceDocumentState.CURATION_FINISHED)) {
                                trainDoc++;
                            }
                            else {
                                annodoc++;
                            }
                        }

                        trainDoc = trainDoc
                                + automationService.listTrainingDocuments(project).size();

                        automationStatus = automationService.existsAutomationStatus(template) ?
                                automationService.getAutomationStatus(template) : automationStatus;
                        automationStatus.setStartime(new Timestamp(new Date().getTime()));
                        automationStatus.setEndTime(new Timestamp(new Date().getTime()));
                        automationStatus.setTrainDocs(trainDoc);
                        automationStatus.setAnnoDocs(annodoc);
                        automationStatus.setTotalDocs(annodoc + trainDoc);
                        automationStatus.setTemplate(template);

                        automationService.createAutomationStatus(automationStatus);

                        template.setAutomationStarted(true);

                        automationStatus.setStatus(Status.GENERATE_TRAIN_DOC);
                        template.setResult("---");

                        AutomationUtil.addOtherFeatureTrainDocument(template, 
                                annotationService, automationService, userRepository);
                        AutomationUtil.otherFeatureClassifiers(template, documentService,
                                automationService);

                        AutomationUtil.addTabSepTrainDocument(template,
                                automationService);
                        AutomationUtil.tabSepClassifiers(template, automationService);

                        AutomationUtil.generateTrainDocument(template, documentService,
                                curationDocumentService, annotationService, automationService,
                                userRepository, true);
                        AutomationUtil.generatePredictDocument(template, documentService,
                                correctionDocumentService, annotationService, automationService,
                                userRepository);

                        automationStatus.setStatus(Status.GENERATE_CLASSIFIER);
                        miraTemplateDetailForm.getModelObject().setResult(
                                AutomationUtil.generateFinalClassifier(template, documentService,
                                        curationDocumentService, annotationService,
                                        automationService, userRepository));
                        AutomationUtil.addOtherFeatureToPredictDocument(template, documentService,
                                annotationService, automationService, userRepository);

                        automationStatus.setStatus(Status.PREDICTION);
                        AutomationUtil.predict(template, documentService, correctionDocumentService,
                                automationService, userRepository);

                        template.setAutomationStarted(false);
                        automationStatus.setStatus(Status.COMPLETED);
                        automationStatus.setEndTime(new Timestamp(new Date().getTime()));
                        automationService.createTemplate(template);
                        automationService.createAutomationStatus(automationStatus);

                    }
                    catch (UIMAException e) {
                        template.setAutomationStarted(false);
                        automationStatus.setStatus(Status.INTERRUPTED);
                        automationStatus.setEndTime(new Timestamp(new Date().getTime()));
                        automationService.createTemplate(template);
                        automationService.createAutomationStatus(automationStatus);
                        aTarget.appendJavaScript("alert('" + ExceptionUtils.getRootCause(e) + "')");
                    }
                    catch (ClassNotFoundException | IOException e) {
                        template.setAutomationStarted(false);
                        automationStatus.setStatus(Status.INTERRUPTED);
                        automationStatus.setEndTime(new Timestamp(new Date().getTime()));
                        automationService.createTemplate(template);
                        automationService.createAutomationStatus(automationStatus);
                        aTarget.appendJavaScript("alert('" + e.getMessage() + "')");
                    }
                    catch (AnnotationException e) {
                        aTarget.appendJavaScript("alert('" + e.getMessage() + "')");
                    }
                    // any other exception such as Memmory heap
                    catch (Exception e) {
                        template.setAutomationStarted(false);
                        automationStatus.setStatus(Status.INTERRUPTED);
                        automationStatus.setEndTime(new Timestamp(new Date().getTime()));
                        automationService.createTemplate(template);
                        automationService.createAutomationStatus(automationStatus);
                        aTarget.appendJavaScript("alert('" + e.getMessage() + "')");
                    }
                    finally {
                        automationStatus.setStatus(Status.COMPLETED);
                        automationStatus.setEndTime(new Timestamp(new Date().getTime()));
                        template.setAutomationStarted(false);
                        automationService.createTemplate(template);
                    }
                }

                @Override
                public boolean isEnabled()
                {
                    return miraTemplateDetailForm != null
                            && !miraTemplateDetailForm.getModelObject().isAutomationStarted();
                }
            });
        }
    }

    public class SelectionModel
        implements Serializable
    {
        private static final long serialVersionUID = -4905538356691404575L;
        public AnnotationFeature features = new AnnotationFeature();
        public AnnotationFeature selectedFeatures = new AnnotationFeature();

    }
}

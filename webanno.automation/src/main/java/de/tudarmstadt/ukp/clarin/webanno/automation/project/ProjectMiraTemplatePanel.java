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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
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

import com.googlecode.wicket.jquery.ui.form.button.IndicatingAjaxButton;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.automation.util.AutomationException;
import de.tudarmstadt.ukp.clarin.webanno.automation.util.AutomationUtil;
import de.tudarmstadt.ukp.clarin.webanno.automation.util.TabSepDocModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AutomationStatus;
import de.tudarmstadt.ukp.clarin.webanno.model.MiraTemplate;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Status;
import de.tudarmstadt.ukp.clarin.webanno.support.EntityModel;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
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

    private MiraTrainLayerSelectionForm miraTrainLayerSelectionForm;
    private MiraTemplateDetailForm miraTemplateDetailForm;
    private OtherLayerDeatilForm otherLayerDetailForm;
    private ProjectTrainingDocumentsPanel targetLayerTarinDocumentsPanel;
    private ProjectTrainingDocumentsPanel otherLayerTarinDocumentsPanel;
    private ProjectTrainingDocumentsPanel freeTrainDocumentsPanel;
    private TargetLaerDetailForm targetLayerDetailForm;

    private final Model<Project> selectedProjectModel;

    private Model<AnnotationFeature> featureModel = new Model<AnnotationFeature>();

    private AnnotationFeature selectedFeature;
    private AnnotationFeature otherSelectedFeature = new AnnotationFeature();
    private MiraTemplate template = new MiraTemplate();

    @SuppressWarnings("unused")
    private final ApplyForm applyForm;
    private DropDownChoice<AnnotationFeature> features;
    private DropDownChoice<AnnotationFeature> otherFeatures;

    public ProjectMiraTemplatePanel(String id, final Model<Project> aProjectModel)
    {
        super(id);
        this.selectedProjectModel = aProjectModel;
        for (MiraTemplate template : repository.listMiraTemplates(selectedProjectModel.getObject())) {
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
                if (template.getId() == 0) {
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
                                if (!feature.getLayer().isEnabled()
                                        || feature.getLayer().getName()
                                                .equals(Token.class.getName())
                                        || feature.getLayer().getName()
                                                .equals(Lemma.class.getName())) {
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
                            return "[ "
                                    + aObject.getLayer().getUiName()
                                    + "] "
                                    + (aObject.getTagset() != null ? aObject.getTagset().getName()
                                            : aObject.getUiName());
                        }
                    });
                    setNullValid(false);
                }

                @Override
                public void onSelectionChanged(AnnotationFeature aNewSelection)
                {
                    selectedFeature = (AnnotationFeature) aNewSelection;
                    if (repository.existsMiraTemplate(selectedFeature)) {
                        template = repository.getMiraTemplate(selectedFeature);
                    }
                    else {
                        template = new MiraTemplate();
                        template.setTrainFeature((AnnotationFeature) aNewSelection);
                    }
                    featureModel.setObject(selectedFeature);
                    miraTemplateDetailForm.setModelObject(template);
                }

                @Override
                protected boolean wantOnSelectionChangedNotifications()
                {
                    return true;
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
        private AjaxTabbedPanel autoTabs;

        public TargetLaerDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<MiraTemplate>(new EntityModel<MiraTemplate>(
                    new MiraTemplate())));

            List<ITab> tabs = new ArrayList<ITab>();
            tabs.add(new AbstractTab(new Model<String>("Target layer"))
            {
                private static final long serialVersionUID = 6703144434578403272L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new TargetLayerPanel(panelId);
                }
            });

            tabs.add(new AbstractTab(new Model<String>("TAB-SEP target"))
            {
                private static final long serialVersionUID = 6703144434578403272L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new FreeTabSepAsTargetDocumentsPanel(panelId);
                }
            });
            tabs.add(new AbstractTab(new Model<String>("Other layers"))
            {
                private static final long serialVersionUID = 6703144434578403272L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new OtherLayerPanel(panelId);
                }
            });

            tabs.add(new AbstractTab(new Model<String>("TAB-SEP feature"))
            {
                private static final long serialVersionUID = 6703144434578403272L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new FreeTabSepAsFeatureDocumentsPanel(panelId);
                }
            });

            add(autoTabs = (AjaxTabbedPanel) new AjaxTabbedPanel<ITab>("autoTabs", tabs)
                    .setOutputMarkupPlaceholderTag(true));
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
                    "targetLayerTarinDocumentsPanel", selectedProjectModel,
                    new Model<TabSepDocModel>(new TabSepDocModel(false, false)), featureModel)
            {

                private static final long serialVersionUID = 7698999083009818310L;

                @Override
                public boolean isVisible()
                {
                    return miraTemplateDetailForm.getModelObject().getId() != 0;
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
                    return miraTemplateDetailForm.getModelObject().getId() != 0
                            && selectedFeature != null;
                }
            });
            SelectionModel selectedOtherModel = new SelectionModel();
            selectedOtherModel.selectedFeatures = otherSelectedFeature;
            selectedOtherModel.features = null;
            otherLayerDetailForm.setModelObject(selectedOtherModel);

            add(otherLayerTarinDocumentsPanel = new ProjectTrainingDocumentsPanel(
                    "otherLayerTarinDocumentsPanel", selectedProjectModel,
                    new Model<TabSepDocModel>(new TabSepDocModel(false, false)),
                    Model.of(otherLayerDetailForm.getModelObject().selectedFeatures))
            {
                private static final long serialVersionUID = -4663938706290521594L;

                @Override
                public boolean isVisible()
                {
                    return otherLayerDetailForm.getModelObject().selectedFeatures.getId() != 0;
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
                    "freeTabSepAsFeatureDocumentsPanel", selectedProjectModel,
                    new Model<TabSepDocModel>(new TabSepDocModel(false, true)), featureModel)
            {
                private static final long serialVersionUID = -4663938706290521594L;

                @Override
                public boolean isVisible()
                {
                    return miraTemplateDetailForm.getModelObject().getId() != 0
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
                    "freeTabSepAsTargetDocumentsPanel", selectedProjectModel,
                    new Model<TabSepDocModel>(new TabSepDocModel(true, true)), featureModel)
            {
                private static final long serialVersionUID = -4663938706290521594L;

                @Override
                public boolean isVisible()
                {
                    return miraTemplateDetailForm.getModelObject().getId() != 0
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
            super(id, new CompoundPropertyModel<MiraTemplate>(new EntityModel<MiraTemplate>(
                    new MiraTemplate())));

            add(new CheckBox("annotateAndPredict"));

            add(new Button("save", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    template = MiraTemplateDetailForm.this.getModelObject();
                    if (template.getId() == 0) {

                        // Since the layer is changed, new classifier is needed
                        for (SourceDocument sd : repository
                                .listSourceDocuments(selectedProjectModel.getObject())) {
                            sd.setProcessed(false);
                        }
                        template.setTrainFeature(selectedFeature);
                        repository.createTemplate(template);
                        featureModel.setObject(MiraTemplateDetailForm.this.getModelObject()
                                .getTrainFeature());
                    }
                    template.setCurrentLayer(true);
                    for (MiraTemplate tmp : repository.listMiraTemplates(selectedProjectModel
                            .getObject())) {
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
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

            add(otherFeatures = new DropDownChoice<AnnotationFeature>("features")
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
                            List<AnnotationFeature> features = annotationService
                                    .listAnnotationFeature(selectedProjectModel.getObject());
                            features.remove(miraTemplateDetailForm.getModelObject()
                                    .getTrainFeature());
                            features.removeAll(miraTemplateDetailForm.getModelObject()
                                    .getOtherFeatures());
                            for (AnnotationFeature feature : annotationService
                                    .listAnnotationFeature(selectedProjectModel.getObject())) {
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
                }

                @Override
                protected void onSelectionChanged(AnnotationFeature aNewSelection)
                {
                    miraTemplateDetailForm.getModelObject().getOtherFeatures().add(aNewSelection);
                    repository.createTemplate(miraTemplateDetailForm.getModelObject());
                }

                @Override
                protected boolean wantOnSelectionChangedNotifications()
                {
                    return true;
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
                            return "[ "
                                    + aObject.getLayer().getUiName()
                                    + "] "
                                    + (aObject.getTagset() != null ? aObject.getTagset().getName()
                                            : aObject.getUiName());
                        }
                    });
                    setNullValid(false);
                }

                @Override
                protected void onSelectionChanged(AnnotationFeature aNewSelection)
                {
                    otherSelectedFeature = aNewSelection;
                    // always force to choose, even after selection of feature
                    otherFeatures.setModelObject(null);
                    updateForm();
                    targetLayerDetailForm.autoTabs.setSelectedTab(2);
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

            add(new IndicatingAjaxButton("apply", new ResourceModel("label"))
            {

                private static final long serialVersionUID = 1L;

                @Override
                protected void onSubmit(AjaxRequestTarget aTarget, Form<?> form)
                {
                    MiraTemplate template = miraTemplateDetailForm.getModelObject();
                    AutomationStatus automationStatus = new AutomationStatus();
                    try {

                        // no training document is added / no curation is done
                        // yet!
                        boolean existsTrainDocument = false;
                        for (SourceDocument document : repository
                                .listSourceDocuments(selectedProjectModel.getObject())) {
                            if (document.getState().equals(SourceDocumentState.CURATION_FINISHED)
                                    || (document.isTrainingDocument() && template.getTrainFeature()
                                            .equals(document.getFeature()))) {
                                existsTrainDocument = true;
                                break;
                            }
                        }

                        for (SourceDocument document : repository
                                .listTabSepDocuments(selectedProjectModel.getObject())) {
                            if (document.isTrainingDocument()) {
                                existsTrainDocument = true;
                                break;
                            }
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
                        for (SourceDocument document : repository
                                .listSourceDocuments(selectedProjectModel.getObject())) {
                            if (!document.isProcessed()) {
                                existUnprocessedDocument = true;
                                break;
                            }
                        }
                        for (SourceDocument document : repository
                                .listTabSepDocuments(selectedProjectModel.getObject())) {
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

                        for (SourceDocument document : repository
                                .listSourceDocuments(selectedProjectModel.getObject())) {
                            if ((document.isTrainingDocument() || document.getState().equals(
                                    SourceDocumentState.CURATION_FINISHED))
                                    && !document.isProcessed()) {
                                trainDoc++;
                            }
                            else if (!document.isTrainingDocument() && !document.isProcessed()) {
                                annodoc++;
                            }
                        }

                        automationStatus = repository.existsAutomationStatus(template) ? repository
                                .getAutomationStatus(template) : automationStatus;
                        automationStatus.setStartime(new Timestamp(new Date().getTime()));
                        automationStatus.setEndTime(new Timestamp(new Date().getTime()));
                        automationStatus.setTrainDocs(trainDoc);
                        automationStatus.setAnnoDocs(annodoc);
                        automationStatus.setTotalDocs(annodoc + trainDoc);
                        automationStatus.setTemplate(template);

                        repository.createAutomationStatus(automationStatus);

                        template.setAutomationStarted(true);

                        automationStatus.setStatus(Status.GENERATE_TRAIN_DOC);

                        AutomationUtil.addOtherFeatureTrainDocument(template, repository);
                        AutomationUtil.otherFeatureClassifiers(template, repository);

                        AutomationUtil.addTabSepTrainDocument(template, repository);
                        AutomationUtil.tabSepClassifiers(template, repository);

                        AutomationUtil.generateTrainDocument(template, repository, true);
                        AutomationUtil.generatePredictDocument(template, repository);

                        automationStatus.setStatus(Status.GENERATE_CLASSIFIER);
                        miraTemplateDetailForm.getModelObject().setResult(
                                AutomationUtil.generateFinalClassifier(template, repository));
                        AutomationUtil.addOtherFeatureToPredictDocument(template, repository);

                        automationStatus.setStatus(Status.PREDICTION);
                        AutomationUtil.predict(template, repository);
                        template.setAutomationStarted(false);
                        repository.createTemplate(template);
                        automationStatus.setStatus(Status.COMPLETED);
                        automationStatus.setEndTime(new Timestamp(new Date().getTime()));

                    }
                    catch (UIMAException e) {
                        error(ExceptionUtils.getRootCause(e));
                        aTarget.appendJavaScript("alert('"+ExceptionUtils.getRootCause(e)+"')");
                    }
                    catch (ClassNotFoundException e) {
                        error(e.getMessage());
                        aTarget.appendJavaScript("alert('"+e.getMessage()+"')");
                    }
                    catch (IOException e) {
                        error(e.getMessage());
                        aTarget.appendJavaScript("alert('"+e.getMessage()+"')");
                    }
                    catch (BratAnnotationException e) {
                        aTarget.appendJavaScript("alert('"+e.getMessage()+"')");
                    }
                    catch (AutomationException e) {
                        aTarget.appendJavaScript("alert('"+e.getMessage()+"')");
                    }
                    finally {
                        automationStatus.setStatus(Status.COMPLETED);
                        automationStatus.setEndTime(new Timestamp(new Date().getTime()));
                        template.setAutomationStarted(false);
                        repository.createTemplate(template);
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

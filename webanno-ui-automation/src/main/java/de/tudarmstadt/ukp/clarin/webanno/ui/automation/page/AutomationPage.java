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
package de.tudarmstadt.ukp.clarin.webanno.ui.automation.page;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateUtils.updateDocumentTimestampAfterWrite;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateUtils.verifyAndUpdateDocumentTimestamp;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectAnnotationByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectSentences;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition.ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition.NEW_TO_ANNOTATION_IN_PROGRESS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.CorrectionDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectType;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences.BratPropertiesImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.automation.service.AutomationService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotationEditor;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ConfirmationDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.ActionBarLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxSubmitLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.DecoratedObject;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.DocumentNamePanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.FinishImage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.AnnotationDetailEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog.AnnotationPreferencesDialog;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog.ExportDocumentDialog;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog.GuidelinesDialog;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog.OpenDocumentDialog;
import de.tudarmstadt.ukp.clarin.webanno.ui.automation.util.AutomationUtil;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.SuggestionViewPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.AnnotationSelection;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.CurationContainer;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.SourceListView;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.SuggestionBuilder;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.UserAnnotationSegment;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;

/**
 * This is the main class for the Automation page. Displays in the lower panel the Automatically
 * annotated document and in the upper panel the annotation pane to trigger automation on the lower
 * pane.
 */
@MountPath("/automation.html")
@ProjectType(id = WebAnnoConst.PROJECT_TYPE_AUTOMATION, prio = 110)
public class AutomationPage
    extends AnnotationPageBase
{
    private static final Logger LOG = LoggerFactory.getLogger(AutomationPage.class);

    private static final long serialVersionUID = 1378872465851908515L;

    private @SpringBean CasStorageService casStorageService;
    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean ConstraintsService constraintsService;
    private @SpringBean BratPropertiesImpl defaultPreferences;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean UserDao userRepository;
    private @SpringBean CurationDocumentService curationDocumentService;
    private @SpringBean CorrectionDocumentService correctionDocumentService;
    private @SpringBean AutomationService automationService;

    private NumberTextField<Integer> gotoPageTextField;
    private DocumentNamePanel documentNamePanel;
    
    private long currentprojectId;

    // Open the dialog window on first load
    private boolean firstLoad = true;

    private ModalWindow openDocumentsModal;
    private AnnotationPreferencesDialog preferencesModal;
    private ExportDocumentDialog exportDialog;
    private GuidelinesDialog guidelinesDialog;

    private FinishImage finishDocumentIcon;
    private ConfirmationDialog finishDocumentDialog;
    private LambdaAjaxLink finishDocumentLink;

    private AnnotationEditorBase annotationEditor;
    private AnnotationDetailEditorPanel detailEditor;    
    private SuggestionViewPanel suggestionView;
    
    private final Map<String, Map<Integer, AnnotationSelection>> 
            annotationSelectionByUsernameAndAddress = new HashMap<>();

    private final SourceListView curationSegment = new SourceListView();

    private CurationContainer curationContainer;

    public AutomationPage()
    {
        commonInit();
    }
    
    private void commonInit()
    {
        setVersioned(false);
        
        setModel(Model.of(new AnnotatorStateImpl(Mode.AUTOMATION)));

        WebMarkupContainer rightSidebar = new WebMarkupContainer("rightSidebar");
        // Override sidebar width from preferences
        rightSidebar.add(new AttributeModifier("style", LambdaModel.of(() -> String
                .format("flex-basis: %d%%;", getModelObject().getPreferences().getSidebarSize()))));
        rightSidebar.setOutputMarkupId(true);
        add(rightSidebar);

        List<UserAnnotationSegment> segments = new LinkedList<>();
        UserAnnotationSegment userAnnotationSegment = new UserAnnotationSegment();
        if (getModelObject().getDocument() != null) {
            userAnnotationSegment
                    .setSelectionByUsernameAndAddress(annotationSelectionByUsernameAndAddress);
            userAnnotationSegment.setAnnotatorState(getModelObject());
            segments.add(userAnnotationSegment);
        }
        
        suggestionView = new SuggestionViewPanel("automateView", new ListModel<>(segments))
        {
            private static final long serialVersionUID = 2583509126979792202L;

            @Override
            public void onChange(AjaxRequestTarget aTarget)
            {
                try {
                    // update begin/end of the curation segment based on bratAnnotatorModel changes
                    // (like sentence change in auto-scroll mode,....
                    aTarget.addChildren(getPage(), IFeedback.class);
                    AnnotatorState state = AutomationPage.this.getModelObject();
                    curationContainer.setBratAnnotatorModel(state);
                    CAS editorCas = getEditorCas();
                    setCurationSegmentBeginEnd(editorCas);

                    suggestionView.updatePanel(aTarget, curationContainer,
                            annotationSelectionByUsernameAndAddress, curationSegment);
                    
                    annotationEditor.requestRender(aTarget);
                    aTarget.add(getOrCreatePositionInfoLabel());
                    update(aTarget);
                }
                catch (Exception e) {
                    handleException(aTarget, e);
                }
            }
        };
        add(suggestionView);

        rightSidebar.add(detailEditor = createDetailEditor());

        annotationEditor = new BratAnnotationEditor("mergeView", getModel(), detailEditor,
            this::getEditorCas);
        add(annotationEditor);

        curationContainer = new CurationContainer();
        curationContainer.setBratAnnotatorModel(getModelObject());

        add(documentNamePanel = new DocumentNamePanel("documentNamePanel", getModel()));

        add(getOrCreatePositionInfoLabel());

        add(openDocumentsModal = new OpenDocumentDialog("openDocumentsModal", getModel(),
                getAllowedProjects())
        {
            private static final long serialVersionUID = 5474030848589262638L;

            @Override
            public void onDocumentSelected(AjaxRequestTarget aTarget)
            {
                // Reload the page using AJAX. This does not add the project/document ID to the URL,
                // but being AJAX it flickers less.
                actionLoadDocument(aTarget);
                
//                if (state.getDocument() == null) {
//                    setResponsePage(getApplication().getHomePage());
//                    return;
//                }
//
//                try {
//                    aCallbackTarget.addChildren(getPage(), IFeedback.class);
//
//                    String username = SecurityContextHolder.getContext().getAuthentication()
//                            .getName();
//
//                    actionLoadDocument(aCallbackTarget);
//                    User user = userRepository.get(username);
//                    detailEditor.setEnabled(!FinishImage.isFinished(
//                            new Model<AnnotatorState>(state), user, documentService));
//                    detailEditor.loadFeatureEditorModels(aCallbackTarget);
//                }
//                catch (Exception e) {
//                    handleException(aCallbackTarget, e);
//                }
//                finishDocumentIcon.setModelObject(state);
//                aCallbackTarget.add(finishDocumentIcon.setOutputMarkupId(true));
//                aCallbackTarget.appendJavaScript(
//                        "Wicket.Window.unloadConfirmation=false;window.location.reload()");
//                aCallbackTarget.add(documentNamePanel.setOutputMarkupId(true));
//                aCallbackTarget.add(getOrCreatePositionInfoLabel());
            }
        });

        add(preferencesModal = new AnnotationPreferencesDialog("preferencesDialog", getModel()));
        preferencesModal.setOnChangeAction(this::actionCompletePreferencesChange);

        add(exportDialog = new ExportDocumentDialog("exportDialog", getModel()));

        add(guidelinesDialog = new GuidelinesDialog("guidelinesDialog", getModel()));
        
        Form<Void> gotoPageTextFieldForm = new Form<>("gotoPageTextFieldForm");
        gotoPageTextField = new NumberTextField<>("gotoPageText", Model.of(1), Integer.class);
        // FIXME minimum and maximum should be obtained from the annotator state
        gotoPageTextField.setMinimum(1); 
        gotoPageTextField.setOutputMarkupId(true); 
        gotoPageTextFieldForm.add(gotoPageTextField);
        LambdaAjaxSubmitLink gotoPageLink = new LambdaAjaxSubmitLink("gotoPageLink",
                gotoPageTextFieldForm, this::actionGotoPage);
        gotoPageTextFieldForm.setDefaultButton(gotoPageLink);
        gotoPageTextFieldForm.add(gotoPageLink);
        add(gotoPageTextFieldForm);

        add(new LambdaAjaxLink("showOpenDocumentModal", this::actionShowOpenDocumentDialog));
       
        add(new LambdaAjaxLink("showPreferencesDialog", this::actionShowPreferencesDialog));

        add(new ActionBarLink("showGuidelinesDialog", guidelinesDialog::show));

        add(new LambdaAjaxLink("showExportDialog", exportDialog::show) {
            private static final long serialVersionUID = -3082002656840117267L;

            {
                setOutputMarkupId(true);
                setOutputMarkupPlaceholderTag(true);
            }

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                
                AnnotatorState state = AutomationPage.this.getModelObject();
                setVisible(state.getProject() != null
                        && (projectService.isAdmin(state.getProject(), state.getUser())
                                || !state.getProject().isDisableExport()));
            }
        });
        
        add(new LambdaAjaxLink("showPreviousDocument", t -> actionShowPreviousDocument(t))
                .add(new InputBehavior(new KeyType[] { KeyType.Shift, KeyType.Page_up },
                        EventType.click)));

        add(new LambdaAjaxLink("showNextDocument", t -> actionShowNextDocument(t))
                .add(new InputBehavior(new KeyType[] { KeyType.Shift, KeyType.Page_down },
                        EventType.click)));

        add(new LambdaAjaxLink("showNext", t -> actionShowNextPage(t))
                .add(new InputBehavior(new KeyType[] { KeyType.Page_down }, EventType.click)));

        add(new LambdaAjaxLink("showPrevious", t -> actionShowPreviousPage(t))
                .add(new InputBehavior(new KeyType[] { KeyType.Page_up }, EventType.click)));

        add(new LambdaAjaxLink("showFirst", t -> actionShowFirstPage(t))
                .add(new InputBehavior(new KeyType[] { KeyType.Home }, EventType.click)));

        add(new LambdaAjaxLink("showLast", t -> actionShowLastPage(t))
                .add(new InputBehavior(new KeyType[] { KeyType.End }, EventType.click)));

        add(new LambdaAjaxLink("toggleScriptDirection", this::actionToggleScriptDirection));
        
        add(createOrGetResetDocumentDialog());
        add(createOrGetResetDocumentLink());
        
        add(finishDocumentDialog = new ConfirmationDialog("finishDocumentDialog",
                new StringResourceModel("FinishDocumentDialog.title", this, null),
                new StringResourceModel("FinishDocumentDialog.text", this, null)));
        add(finishDocumentLink = new LambdaAjaxLink("showFinishDocumentDialog",
                this::actionFinishDocument)
        {
            private static final long serialVersionUID = 874573384012299998L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                
                AnnotatorState state = AutomationPage.this.getModelObject();
                setEnabled(state.getDocument() != null && !documentService
                        .isAnnotationFinished(state.getDocument(), state.getUser()));
            }
        });
        finishDocumentIcon = new FinishImage("finishImage", getModel());
        finishDocumentIcon.setOutputMarkupId(true);
        finishDocumentLink.add(finishDocumentIcon);
    }
    
    private IModel<List<DecoratedObject<Project>>> getAllowedProjects()
    {
        return LambdaModel.of(() -> {
            User user = userRepository.getCurrentUser();
            List<DecoratedObject<Project>> allowedProject = new ArrayList<>();
            for (Project project : projectService.listProjects()) {
                if (projectService.isAnnotator(project, user)
                        && WebAnnoConst.PROJECT_TYPE_AUTOMATION.equals(project.getMode())) {
                    allowedProject.add(DecoratedObject.of(project));
                }
            }
            return allowedProject;
        });
    }

    @Override
    public NumberTextField<Integer> getGotoPageTextField()
    {
        return gotoPageTextField;
    }

    private AnnotationDetailEditorPanel createDetailEditor()
    {
        return new AnnotationDetailEditorPanel("annotationDetailEditorPanel", this, getModel())
        {
            private static final long serialVersionUID = 2857345299480098279L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget)
            {
                AnnotatorState state = getModelObject();
                
                aTarget.addChildren(getPage(), IFeedback.class);
                
                try {
                    annotationEditor.requestRender(aTarget);
                }
                catch (Exception e) {
                    handleException(this, aTarget, e);
                    return;
                }

                try {
                    SuggestionBuilder builder = new SuggestionBuilder(casStorageService,
                            documentService, correctionDocumentService, curationDocumentService,
                            annotationService, userRepository);
                    curationContainer = builder.buildCurationContainer(state);
                    setCurationSegmentBeginEnd(getEditorCas());
                    curationContainer.setBratAnnotatorModel(state);

                    suggestionView.updatePanel(aTarget, curationContainer,
                            annotationSelectionByUsernameAndAddress, curationSegment);
                    
                    update(aTarget);
                }
                catch (Exception e) {
                    handleException(this, aTarget, e);
                }
            }
            
            @Override
            public void onAnnotate(AjaxRequestTarget aTarget)
            {
                AnnotatorState state = getModelObject();
                
                if (state.isForwardAnnotation()) {
                    return;
                }
                AnnotationLayer layer = state.getSelectedAnnotationLayer();
                int address = state.getSelection().getAnnotation().getId();
                try {
                    CAS cas = getEditorCas();
                    AnnotationFS fs = selectAnnotationByAddr(cas, address);

                    for (AnnotationFeature f : annotationService.listAnnotationFeature(layer)) {
                        Type type = CasUtil.getType(fs.getCAS(), layer.getName());
                        Feature feat = type.getFeatureByBaseName(f.getName());
                        if (!automationService.existsMiraTemplate(f)) {
                            continue;
                        }
                        if (!automationService.getMiraTemplate(f).isAnnotateAndRepeat()) {
                            continue;
                        }
                        TagSet tagSet = f.getTagset();
                        boolean isRepeatable = false;
                        // repeat only if the value is in the tagset
                        for (Tag tag : annotationService.listTags(tagSet)) {
                            if (fs.getFeatureValueAsString(feat) == null) {
                                break; // this is new annotation without values
                            }
                            if (fs.getFeatureValueAsString(feat).equals(tag.getName())) {
                                isRepeatable = true;
                                break;
                            }
                        }
                        if (automationService.getMiraTemplate(f) != null && isRepeatable) {

                            if (layer.getType().endsWith(WebAnnoConst.RELATION_TYPE)) {
                                AutomationUtil.repeateRelationAnnotation(state, documentService,
                                        correctionDocumentService, annotationService, fs, f,
                                        fs.getFeatureValueAsString(feat));
                                update(aTarget);
                                break;
                            }
                            else if (layer.getType().endsWith(WebAnnoConst.SPAN_TYPE)) {
                                AutomationUtil.repeateSpanAnnotation(state, documentService,
                                        correctionDocumentService, annotationService, fs.getBegin(),
                                        fs.getEnd(), f, fs.getFeatureValueAsString(feat));
                                update(aTarget);
                                break;
                            }

                        }
                    }
                }
                catch (Exception e) {
                    handleException(this, aTarget, e);
                }
            }

            @Override
            protected void onAutoForward(AjaxRequestTarget aTarget)
            {
                annotationEditor.requestRender(aTarget);
            }
            
            @Override
            public void onDelete(AjaxRequestTarget aTarget, AnnotationFS aFS)
            {
                AnnotatorState state = getModelObject();
                AnnotationLayer layer = state.getSelectedAnnotationLayer();
                for (AnnotationFeature f : annotationService.listAnnotationFeature(layer)) {
                    if (!automationService.existsMiraTemplate(f)) {
                        continue;
                    }
                    if (!automationService.getMiraTemplate(f).isAnnotateAndRepeat()) {
                        continue;
                    }
                    try {
                        Type type = CasUtil.getType(aFS.getCAS(), layer.getName());
                        Feature feat = type.getFeatureByBaseName(f.getName());
                        if (layer.getType().endsWith(WebAnnoConst.RELATION_TYPE)) {
                            AutomationUtil.deleteRelationAnnotation(state, documentService,
                                    correctionDocumentService, annotationService, aFS, f,
                                    aFS.getFeatureValueAsString(feat));
                        }
                        else {
                            AutomationUtil.deleteSpanAnnotation(state, documentService,
                                    correctionDocumentService, annotationService, aFS.getBegin(),
                                    aFS.getEnd(), f, aFS.getFeatureValueAsString(feat));
                        }
                        update(aTarget);
                    }
                    catch (Exception e) {
                        handleException(this, aTarget, e);
                    }
                }
            }
            
            @Override
            public CAS getEditorCas() throws IOException
            {
                return AutomationPage.this.getEditorCas();
            }
        };
    }

    @Override
    protected List<SourceDocument> getListOfDocs()
    {
        AnnotatorState state = getModelObject();
        return new ArrayList<>(documentService
                .listAnnotatableDocuments(state.getProject(), state.getUser()).keySet());
    }

    /**
     * for the first time the page is accessed, open the <b>open document dialog</b>
     */
    @Override
    public void renderHead(IHeaderResponse response)
    {
        super.renderHead(response);

        if (firstLoad) {
            response.render(OnLoadHeaderItem
                    .forScript("jQuery('#showOpenDocumentModal').trigger('click');"));
            firstLoad = false;
        }
    }
    
    @Override
    protected CAS getEditorCas()
        throws IOException
    {
        AnnotatorState state = getModelObject();

        if (state.getDocument() == null) {
            throw new IllegalStateException("Please open a document first!");
        }

        // If we have a timestamp, then use it to detect if there was a concurrent access
        verifyAndUpdateDocumentTimestamp(state, documentService
                .getAnnotationCasTimestamp(state.getDocument(), state.getUser().getUsername()));

        return documentService.readAnnotationCas(getModelObject().getDocument(),
                state.getUser().getUsername());
    }
    
    private void setCurationSegmentBeginEnd(CAS aEditorCas)
        throws UIMAException, ClassNotFoundException, IOException
    {
        AnnotatorState state = getModelObject();
        curationSegment.setBegin(state.getWindowBeginOffset());
        curationSegment.setEnd(state.getWindowEndOffset());
    }

    private void update(AjaxRequestTarget target)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        suggestionView.updatePanel(target, curationContainer,
                annotationSelectionByUsernameAndAddress, curationSegment);

        gotoPageTextField.setModelObject(getModelObject().getFirstVisibleUnitIndex());

        target.add(gotoPageTextField);
        target.add(getOrCreatePositionInfoLabel());
    }

    
    private void actionShowOpenDocumentDialog(AjaxRequestTarget aTarget)
    {
        getModelObject().getSelection().clear();
        openDocumentsModal.show(aTarget);
    }

    private void actionShowPreferencesDialog(AjaxRequestTarget aTarget)
    {
        getModelObject().getSelection().clear();
        preferencesModal.show(aTarget);
    }
    
    private void actionGotoPage(AjaxRequestTarget aTarget, Form<?> aForm)
        throws Exception
    {
        AnnotatorState state = getModelObject();
        
        CAS editorCas = getEditorCas();
        List<AnnotationFS> sentences = new ArrayList<>(selectSentences(editorCas));
        int selectedSentence = gotoPageTextField.getModelObject();
        selectedSentence = Math.min(selectedSentence, sentences.size());
        gotoPageTextField.setModelObject(selectedSentence);
        
        state.setFirstVisibleUnit(sentences.get(selectedSentence - 1));
        state.setFocusUnitIndex(selectedSentence);        
        
        SuggestionBuilder builder = new SuggestionBuilder(casStorageService, documentService,
                correctionDocumentService, curationDocumentService, annotationService,
                userRepository);
        curationContainer = builder.buildCurationContainer(state);
        setCurationSegmentBeginEnd(editorCas);
        curationContainer.setBratAnnotatorModel(state);
        update(aTarget);
        
        aTarget.add(gotoPageTextField);
        annotationEditor.requestRender(aTarget);
    }

    private void actionToggleScriptDirection(AjaxRequestTarget aTarget)
            throws Exception
    {
        getModelObject().toggleScriptDirection();
        annotationEditor.requestRender(aTarget);

        curationContainer.setBratAnnotatorModel(getModelObject());
        suggestionView.updatePanel(aTarget, curationContainer,
                annotationSelectionByUsernameAndAddress, curationSegment);
    }
    
    private void actionCompletePreferencesChange(AjaxRequestTarget aTarget)
    {
        try {
            AnnotatorState state = getModelObject();

            CAS editorCas = getEditorCas();
            
            // The number of visible sentences may have changed - let the state recalculate 
            // the visible sentences 
            Sentence sentence = selectByAddr(editorCas, Sentence.class,
                    state.getFirstVisibleUnitAddress());
            state.setFirstVisibleUnit(sentence);
            
            SuggestionBuilder builder = new SuggestionBuilder(casStorageService, documentService,
                    correctionDocumentService, curationDocumentService, annotationService,
                    userRepository);
            curationContainer = builder.buildCurationContainer(state);
            setCurationSegmentBeginEnd(editorCas);
            curationContainer.setBratAnnotatorModel(state);
            
            update(aTarget);
            aTarget.appendJavaScript(
                    "Wicket.Window.unloadConfirmation = false;window.location.reload()");
            
            // Re-render the whole page because the width of the sidebar may have changed
            aTarget.add(this);
        }
        catch (Exception e) {
            handleException(aTarget, e);
        }
    }
    
    private void actionFinishDocument(AjaxRequestTarget aTarget)
    {
        finishDocumentDialog.setConfirmAction((aCallbackTarget) -> {
            actionValidateDocument(aCallbackTarget, getEditorCas());
            
            AnnotatorState state = getModelObject();
            AnnotationDocument annotationDocument = documentService.getAnnotationDocument(
                    state.getDocument(), state.getUser());

            documentService.transitionAnnotationDocumentState(annotationDocument,
                    ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED);
            
            aCallbackTarget.add(finishDocumentIcon);
            aCallbackTarget.add(finishDocumentLink);
            aCallbackTarget.add(detailEditor);
            aCallbackTarget.add(createOrGetResetDocumentLink());
        });
        finishDocumentDialog.show(aTarget);
    }
    
    @Override
    protected void actionLoadDocument(AjaxRequestTarget aTarget)
    {
        LOG.info("BEGIN LOAD_DOCUMENT_ACTION");

        AnnotatorState state = getModelObject();
        
        state.setUser(userRepository.getCurrentUser());
        state.setDocument(state.getDocument(), getListOfDocs());

        try {
            // Check if there is an annotation document entry in the database. If there is none,
            // create one.
            AnnotationDocument annotationDocument = documentService
                    .createOrGetAnnotationDocument(state.getDocument(), state.getUser());

            // Read the correction CAS - if it does not exist yet, from the initial CAS
            CAS correctionCas;
            if (correctionDocumentService.existsCorrectionCas(state.getDocument())) {
                correctionCas = correctionDocumentService.readCorrectionCas(state.getDocument());
            }
            else {
                correctionCas = documentService.createOrReadInitialCas(state.getDocument());
            }

            // Read the annotation CAS or create an annotation CAS from the initial CAS by stripping
            // annotations
            CAS editorCas;
            if (documentService.existsCas(state.getDocument(), state.getUser().getUsername())) {
                editorCas = documentService.readAnnotationCas(annotationDocument);
            }
            else {
                editorCas = documentService.createOrReadInitialCas(state.getDocument());
                // In automation mode, we do not remove the existing annotations from the documents
            }

            // Update the CASes
            annotationService.upgradeCas(editorCas, annotationDocument);
            correctionDocumentService.upgradeCorrectionCas(correctionCas, state.getDocument());

            // After creating an new CAS or upgrading the CAS, we need to save it
            documentService.writeAnnotationCas(editorCas, annotationDocument.getDocument(),
                    state.getUser(), false);
            correctionDocumentService.writeCorrectionCas(correctionCas, state.getDocument());

            // (Re)initialize brat model after potential creating / upgrading CAS
            state.reset();

            // Initialize timestamp in state
            updateDocumentTimestampAfterWrite(state, documentService
                    .getAnnotationCasTimestamp(state.getDocument(), state.getUser().getUsername()));

            // Load constraints
            state.setConstraints(constraintsService.loadConstraints(state.getProject()));

            // Load user preferences
            loadPreferences();

            // Initialize the visible content
            state.setFirstVisibleUnit(WebAnnoCasUtil.getFirstSentence(editorCas));
            
            // if project is changed, reset some project specific settings
            if (currentprojectId != state.getProject().getId()) {
                state.clearRememberedFeatures();
            }

            currentprojectId = state.getProject().getId();

            LOG.debug("Configured BratAnnotatorModel for user [" + state.getUser() + "] f:["
                    + state.getFirstVisibleUnitIndex() + "] l:["
                    + state.getLastVisibleUnitIndex() + "] s:["
                    + state.getFocusUnitIndex() + "]");

            gotoPageTextField.setModelObject(1);

            setCurationSegmentBeginEnd(editorCas);
            suggestionView.init(aTarget, curationContainer, annotationSelectionByUsernameAndAddress,
                    curationSegment);
            update(aTarget);

            // Re-render the whole page because the font size
            if (aTarget != null) {
                aTarget.add(this);
            }

            // Update document state
            if (state.getDocument().getState().equals(SourceDocumentState.NEW)) {
                documentService.transitionSourceDocumentState(state.getDocument(),
                        NEW_TO_ANNOTATION_IN_PROGRESS);
            }
            
            // Reset the editor
            detailEditor.reset(aTarget);
            // Populate the layer dropdown box
            detailEditor.loadFeatureEditorModels(editorCas, aTarget);
        }
        catch (Exception e) {
            handleException(aTarget, e);
        }

        LOG.info("END LOAD_DOCUMENT_ACTION");
    }
    
    @Override
    protected void actionRefreshDocument(AjaxRequestTarget aTarget)
    {
        try {
            AnnotatorState state = getModelObject();
            SuggestionBuilder builder = new SuggestionBuilder(casStorageService, documentService,
                    correctionDocumentService, curationDocumentService, annotationService,
                    userRepository);
            curationContainer = builder.buildCurationContainer(state);
            setCurationSegmentBeginEnd(getEditorCas());
            curationContainer.setBratAnnotatorModel(state);
            update(aTarget);
            annotationEditor.requestRender(aTarget);
        }
        catch (Exception e) {
            handleException(aTarget, e);
        }
    }
}

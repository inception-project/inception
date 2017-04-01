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
package de.tudarmstadt.ukp.clarin.webanno.ui.correction;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static org.apache.uima.fit.util.JCasUtil.select;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.api.CorrectionDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.SettingsService;
import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.SecurityUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotationEditor;
import de.tudarmstadt.ukp.clarin.webanno.brat.util.BratAnnotatorUtility;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ConfirmationDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxSubmitLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.PreferencesUtil;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.AnnotationPreferencesModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.DocumentNamePanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.ExportModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.FinishImage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.GuidelineModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.AnnotationDetailEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog.OpenModalWindowPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItem;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItemCondition;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.SuggestionViewPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.CurationContainer;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.CurationUserSegmentForAnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.SourceListView;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.SuggestionBuilder;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.service.AnnotationSelection;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;

/**
 * This is the main class for the correction page. Displays in the lower panel the Automatically
 * annotated document and in the upper panel the corrected annotation
 */
@MenuItem(icon="images/check_box.png", label="Correction", prio = 120)
@MountPath("/correction.html")
public class CorrectionPage
    extends AnnotationPageBase
{
    private static final Logger LOG = LoggerFactory.getLogger(CorrectionPage.class);

    private static final long serialVersionUID = 1378872465851908515L;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "documentRepository")
    private DocumentService documentService;

    @SpringBean(name = "documentRepository")
    private CorrectionDocumentService correctionDocumentService;

    @SpringBean(name = "documentRepository")
    private ProjectService projectService;

    @SpringBean(name = "documentRepository")
    private ConstraintsService constraintsService;

    @SpringBean(name = "documentRepository")
    private SettingsService settingsService;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "userRepository")
    private UserDao userRepository;

    private NumberTextField<Integer> gotoPageTextField;
    private DocumentNamePanel documentNamePanel;
    
    private long currentprojectId;

    // Open the dialog window on first load
    private boolean firstLoad = true;

    private ModalWindow openDocumentsModal;

    private FinishImage finishDocumentIcon;
    private ConfirmationDialog finishDocumentDialog;
    private LambdaAjaxLink finishDocumentLink;

    private AnnotationEditorBase annotationEditor;
    private AnnotationDetailEditorPanel detailEditor;    
    private SuggestionViewPanel suggestionView;

    private CurationContainer curationContainer;

    private Map<String, Map<Integer, AnnotationSelection>> annotationSelectionByUsernameAndAddress = new HashMap<String, Map<Integer, AnnotationSelection>>();

    private SourceListView curationSegment = new SourceListView();

    public CorrectionPage()
    {
        commonInit();
    }
    
    private void commonInit()
    {
        setVersioned(false);
        
        setModel(Model.of(new AnnotatorStateImpl(Mode.CORRECTION)));

        WebMarkupContainer sidebarCell = new WebMarkupContainer("sidebarCell") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTag(ComponentTag aTag)
            {
                super.onComponentTag(aTag);
                AnnotatorState state = CorrectionPage.this.getModelObject();
                aTag.put("width", state.getPreferences().getSidebarSize()+"%");
            }
        };
        add(sidebarCell);

        WebMarkupContainer annotationViewCell = new WebMarkupContainer("annotationViewCell") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTag(ComponentTag aTag)
            {
                super.onComponentTag(aTag);
                AnnotatorState state = CorrectionPage.this.getModelObject();
                aTag.put("width", (100-state.getPreferences().getSidebarSize())+"%");
            }
        };
        add(annotationViewCell);
        
        LinkedList<CurationUserSegmentForAnnotationDocument> sentences = new LinkedList<CurationUserSegmentForAnnotationDocument>();
        CurationUserSegmentForAnnotationDocument curationUserSegmentForAnnotationDocument = new CurationUserSegmentForAnnotationDocument();
        if (getModelObject().getDocument() != null) {
            curationUserSegmentForAnnotationDocument
                    .setAnnotationSelectionByUsernameAndAddress(annotationSelectionByUsernameAndAddress);
            curationUserSegmentForAnnotationDocument.setBratAnnotatorModel(getModelObject());
            sentences.add(curationUserSegmentForAnnotationDocument);
        }
        suggestionView = new SuggestionViewPanel("correctionView",
                new Model<LinkedList<CurationUserSegmentForAnnotationDocument>>(sentences))
        {
            private static final long serialVersionUID = 2583509126979792202L;

            @Override
            public void onChange(AjaxRequestTarget aTarget)
            {
                AnnotatorState state = CorrectionPage.this.getModelObject();
                
                aTarget.addChildren(getPage(), FeedbackPanel.class);
                try {
                    // update begin/end of the curation segment based on bratAnnotatorModel changes
                    // (like sentence change in auto-scroll mode,....
                    curationContainer.setBratAnnotatorModel(state);
                    JCas editorCas = getEditorCas();
                    setCurationSegmentBeginEnd(editorCas);

                    suggestionView.updatePanel(aTarget, curationContainer, annotationEditor, annotationSelectionByUsernameAndAddress,
                            curationSegment);
                    
                    annotationEditor.render(aTarget, editorCas);
                    aTarget.add(getOrCreatePositionInfoLabel());
                    update(aTarget);
                }
                catch (UIMAException e) {
                    LOG.error("Error: " + e.getMessage(), e);
                    error(ExceptionUtils.getRootCause(e));
                }
                catch (Exception e) {
                    LOG.error("Error: " + e.getMessage(), e);
                    error(e.getMessage());
                }
            }
        };

        suggestionView.setOutputMarkupId(true);
        annotationViewCell.add(suggestionView);

        sidebarCell.add(detailEditor = createDetailEditor());
        
        annotationEditor = new BratAnnotationEditor("mergeView", getModel(), detailEditor,
                () -> { return getEditorCas(); });
        annotationViewCell.add(annotationEditor);

        curationContainer = new CurationContainer();
        curationContainer.setBratAnnotatorModel(getModelObject());

        add(documentNamePanel = createDocumentInfoLabel());

        add(getOrCreatePositionInfoLabel());

        add(openDocumentsModal = new ModalWindow("openDocumentsModal"));
        openDocumentsModal.setOutputMarkupId(true);
        openDocumentsModal.setInitialWidth(620);
        openDocumentsModal.setInitialHeight(440);
        openDocumentsModal.setResizable(true);
        openDocumentsModal.setWidthUnit("px");
        openDocumentsModal.setHeightUnit("px");
        openDocumentsModal.setTitle("Open document");

        add(new AnnotationPreferencesModalPanel("annotationLayersModalPanel", getModel(), detailEditor)
        {
            private static final long serialVersionUID = -4657965743173979437L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget)
            {
                actionCompletePreferencesChange(aTarget);
            }
        });

        add(new ExportModalPanel("exportModalPanel", getModel()){
            private static final long serialVersionUID = -468896211970839443L;

            {
                setOutputMarkupId(true);
                setOutputMarkupPlaceholderTag(true);
            }

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                AnnotatorState state = CorrectionPage.this.getModelObject();
                setVisible(state.getProject() != null
                        && (SecurityUtil.isAdmin(state.getProject(), repository, state.getUser())
                                || !state.getProject().isDisableExport()));
            }
        });

        Form<Void> gotoPageTextFieldForm = new Form<Void>("gotoPageTextFieldForm");
        gotoPageTextField = new NumberTextField<Integer>("gotoPageText", Model.of(1), Integer.class);
        // FIXME minimum and maximum should be obtained from the annotator state
        gotoPageTextField.setMinimum(1); 
        gotoPageTextField.setOutputMarkupId(true); 
        gotoPageTextFieldForm.add(gotoPageTextField);
        gotoPageTextFieldForm.add(new LambdaAjaxSubmitLink("gotoPageLink", gotoPageTextFieldForm,
                this::actionGotoPage));
        add(gotoPageTextFieldForm);

        add(new LambdaAjaxLink("showOpenDocumentModal", this::actionShowOpenDocumentDialog));
        
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
        
        add(new GuidelineModalPanel("guidelineModalPanel", getModel()));
        
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
                AnnotatorState state = CorrectionPage.this.getModelObject();
                setEnabled(state.getDocument() != null && !documentService
                        .isAnnotationFinished(state.getDocument(), state.getUser()));
            }
        });
        finishDocumentIcon = new FinishImage("finishImage", getModel());
        finishDocumentIcon.setOutputMarkupId(true);
        finishDocumentLink.add(finishDocumentIcon);
    }
    
    private DocumentNamePanel createDocumentInfoLabel()
    {
        return new DocumentNamePanel("documentNamePanel", getModel());
    }

    private AnnotationDetailEditorPanel createDetailEditor()
    {
        return new AnnotationDetailEditorPanel("annotationDetailEditorPanel", getModel())
        {
            private static final long serialVersionUID = 2857345299480098279L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget)
            {
                aTarget.addChildren(getPage(), FeedbackPanel.class);
                aTarget.add(getOrCreatePositionInfoLabel());
                aTarget.add(suggestionView);

                try {
                    AnnotatorState state = getModelObject();
                    JCas editorCas = getEditorCas();
                    //JCas correctionCas = repository.readCorrectionCas(state.getDocument());
                    annotationEditor.render(aTarget, editorCas);
                    annotationEditor.setHighlight(aTarget, state.getSelection().getAnnotation());

                    // info(bratAnnotatorModel.getMessage());
                    SuggestionBuilder builder = new SuggestionBuilder(repository,
                            annotationService, userRepository);
                    curationContainer = builder.buildCurationContainer(state);
                    setCurationSegmentBeginEnd(editorCas);
                    curationContainer.setBratAnnotatorModel(state);

                    suggestionView.updatePanel(aTarget, curationContainer, annotationEditor, annotationSelectionByUsernameAndAddress,
                            curationSegment);
                    
                    update(aTarget);
                }
                catch (Exception e) {
                    handleException(this, aTarget, e);
                }
            }

            @Override
            protected void onAutoForward(AjaxRequestTarget aTarget)
            {
                try {
                    annotationEditor.render(aTarget, getEditorCas());
                }
                catch (Exception e) {
                    LOG.info("Error reading CAS: {} " + e.getMessage(), e);
                    error("Error reading CAS " + e.getMessage());
                    return;
                }
            }
        };
    }

    private Label createPositionInfoLabel()
    {
        return new Label("numberOfPages", new StringResourceModel("PositionInfo.text", 
                this, getModel(), 
                PropertyModel.of(getModel(), "firstVisibleSentenceNumber"),
                PropertyModel.of(getModel(), "lastVisibleSentenceNumber"),
                PropertyModel.of(getModel(), "numberOfSentences"),
                PropertyModel.of(getModel(), "documentIndex"),
                PropertyModel.of(getModel(), "numberOfDocuments"))) {
            private static final long serialVersionUID = 7176610419683776917L;

            {
                setOutputMarkupId(true);
                setOutputMarkupPlaceholderTag(true);
            }
            
            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                setVisible(getModelObject().getDocument() != null);
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
    protected JCas getEditorCas()
        throws IOException
    {
        AnnotatorState state = getModelObject();

        if (state.getDocument() == null) {
            throw new IllegalStateException("Please open a document first!");
        }

        SourceDocument aDocument = getModelObject().getDocument();

        AnnotationDocument annotationDocument = documentService.getAnnotationDocument(aDocument,
                state.getUser());

        // If there is no CAS yet for the annotation document, create one.
        return repository.readAnnotationCas(annotationDocument);
    }

    private void setCurationSegmentBeginEnd(JCas aEditorCas)
        throws UIMAException, ClassNotFoundException, IOException
    {
        AnnotatorState state = getModelObject();
        curationSegment.setBegin(state.getWindowBeginOffset());
        curationSegment.setEnd(state.getWindowEndOffset());
    }

    private void update(AjaxRequestTarget target)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        suggestionView.updatePanel(target, curationContainer, annotationEditor, annotationSelectionByUsernameAndAddress,
                curationSegment);

        gotoPageTextField.setModelObject(getModelObject().getFirstVisibleSentenceNumber());

        target.add(gotoPageTextField);
        target.add(suggestionView);
        target.add(getOrCreatePositionInfoLabel());
    }
    
    private void actionShowOpenDocumentDialog(AjaxRequestTarget aTarget)
    {
        AnnotatorState state = getModelObject();
        state.getSelection().clear();
        openDocumentsModal.setContent(new OpenModalWindowPanel(openDocumentsModal.getContentId(),
                state, openDocumentsModal, state.getMode()));
        openDocumentsModal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
        {
            private static final long serialVersionUID = -1746088901018629567L;

            @Override
            public void onClose(AjaxRequestTarget aCallbackTarget)
            {
                if (state.getDocument() == null) {
                    setResponsePage(getApplication().getHomePage());
                    return;
                }

                aCallbackTarget.addChildren(getPage(), FeedbackPanel.class);
                try {
                    actionLoadDocument(aCallbackTarget);

                    String username = SecurityContextHolder.getContext().getAuthentication()
                            .getName();
                    User user = userRepository.get(username);
                    detailEditor
                            .setEnabled(!FinishImage.isFinished(getModel(), user, documentService));
                    detailEditor.loadFeatureEditorModels(aCallbackTarget);
                }
                catch (Exception e) {
                    LOG.error("Unable to load data", e);
                    error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
                }
                aCallbackTarget.add(finishDocumentIcon);
                aCallbackTarget.appendJavaScript(
                        "Wicket.Window.unloadConfirmation=false;window.location.reload()");
                aCallbackTarget.add(documentNamePanel);
                aCallbackTarget.add(getOrCreatePositionInfoLabel());
            }
        });
        openDocumentsModal.show(aTarget);
    }

    private void actionGotoPage(AjaxRequestTarget aTarget, Form<?> aForm)
        throws Exception
    {
        AnnotatorState state = getModelObject();
        
        JCas editorCas = getEditorCas();
        List<Sentence> sentences = new ArrayList<>(select(editorCas, Sentence.class));
        int selectedSentence = gotoPageTextField.getModelObject();
        selectedSentence = Math.min(selectedSentence, sentences.size());
        gotoPageTextField.setModelObject(selectedSentence);
        
        state.setFirstVisibleSentence(sentences.get(selectedSentence - 1));
        state.setFocusSentenceNumber(selectedSentence);        
        
        SuggestionBuilder builder = new SuggestionBuilder(repository, annotationService,
                userRepository);
        curationContainer = builder.buildCurationContainer(state);
        setCurationSegmentBeginEnd(editorCas);
        curationContainer.setBratAnnotatorModel(state);
        update(aTarget);
        
        aTarget.add(gotoPageTextField);
        annotationEditor.render(aTarget, editorCas);
    }

    private void actionToggleScriptDirection(AjaxRequestTarget aTarget)
            throws Exception
    {
        getModelObject().toggleScriptDirection();
        annotationEditor.renderLater(aTarget);

        curationContainer.setBratAnnotatorModel(getModelObject());
        suggestionView.updatePanel(aTarget, curationContainer, annotationEditor, annotationSelectionByUsernameAndAddress,
                curationSegment);
    }
    
    private void actionCompletePreferencesChange(AjaxRequestTarget aTarget)
    {
        try {
            AnnotatorState state = getModelObject();
            curationContainer.setBratAnnotatorModel(state);

            JCas editorCas = getEditorCas();
            setCurationSegmentBeginEnd(editorCas);
            
            // The number of visible sentences may have changed - let the state recalculate 
            // the visible sentences 
            Sentence sentence = selectByAddr(editorCas, Sentence.class,
                    state.getFirstVisibleSentenceAddress());
            state.setFirstVisibleSentence(sentence);
            
            update(aTarget);
            aTarget.appendJavaScript(
                    "Wicket.Window.unloadConfirmation = false;window.location.reload()");
            
            // Re-render the whole page because the width of the sidebar may have changed
            aTarget.add(this);
        }
        catch (Exception e) {
            LOG.info("Error reading CAS " + e.getMessage());
            error("Error reading CAS " + e.getMessage());
            return;
        }
    }

    /**
     * Reset the document by removing all annotations form the initial CAS and using the result as
     * the editor CAS.
     */
    @Override
    protected void actionResetDocument(AjaxRequestTarget aTarget)
        throws Exception
    {
        AnnotatorState state = getModelObject();
        JCas editorCas = repository.createOrReadInitialCas(state.getDocument());
        editorCas = BratAnnotatorUtility.clearJcasAnnotations(editorCas, state.getDocument(),
                state.getUser(), repository);
        documentService.writeAnnotationCas(editorCas, state.getDocument(), state.getUser());
        actionLoadDocument(aTarget);
    }
    
    private void actionFinishDocument(AjaxRequestTarget aTarget)
    {
        finishDocumentDialog.setConfirmAction((aCallbackTarget) -> {
            ensureRequiredFeatureValuesSet(aCallbackTarget, getEditorCas());
            
            AnnotatorState state = getModelObject();
            AnnotationDocument annotationDocument = documentService.getAnnotationDocument(
                    state.getDocument(), state.getUser());

            annotationDocument.setState(AnnotationDocumentStateTransition.transition(
                    AnnotationDocumentStateTransition.ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED));
            
            // manually update state change!! No idea why it is not updated in the DB
            // without calling createAnnotationDocument(...)
            documentService.createAnnotationDocument(annotationDocument);
            
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
        
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.get(username);

        state.setUser(user);
        state.setProject(state.getProject());
        state.setDocument(state.getDocument(), getListOfDocs());

        try {
            // Check if there is an annotation document entry in the database. If there is none,
            // create one.
            AnnotationDocument annotationDocument = documentService
                    .createOrGetAnnotationDocument(state.getDocument(), user);

            // Read the correction CAS - if it does not exist yet, from the initial CAS
            JCas correctionCas;
            if (repository.existsCorrectionCas(state.getDocument())) {
                correctionCas = repository.readCorrectionCas(state.getDocument());
            }
            else {
                correctionCas = documentService.createOrReadInitialCas(state.getDocument());
            }

            // Read the annotation CAS or create an annotation CAS from the initial CAS by stripping
            // annotations
            JCas editorCas;
            if (repository.existsCas(state.getDocument(), user.getUsername())) {
                editorCas = documentService.readAnnotationCas(annotationDocument);
            }
            else {
                editorCas = documentService.createOrReadInitialCas(state.getDocument());
                editorCas = BratAnnotatorUtility.clearJcasAnnotations(editorCas,
                        state.getDocument(), user, repository);
            }

            // Update the CASes
            documentService.upgradeCas(editorCas.getCas(), annotationDocument);
            repository.upgradeCorrectionCas(correctionCas.getCas(), state.getDocument());

            // After creating an new CAS or upgrading the CAS, we need to save it
            documentService.writeAnnotationCas(editorCas.getCas().getJCas(),
                    annotationDocument.getDocument(), user);
            repository.writeCorrectionCas(correctionCas, state.getDocument(), user);

            // (Re)initialize brat model after potential creating / upgrading CAS
            state.clearAllSelections();

            // Load constraints
            state.setConstraints(repository.loadConstraints(state.getProject()));

            // Load user preferences
            PreferencesUtil.loadPreferences(username, settingsService, repository,
                    annotationService, state, state.getMode());

            // Initialize the visible content
            state.setFirstVisibleSentence(WebAnnoCasUtil.getFirstSentence(editorCas));
            
            // if project is changed, reset some project specific settings
            if (currentprojectId != state.getProject().getId()) {
                state.clearRememberedFeatures();
            }

            currentprojectId = state.getProject().getId();

            LOG.debug("Configured BratAnnotatorModel for user [" + state.getUser() + "] f:["
                    + state.getFirstVisibleSentenceNumber() + "] l:["
                    + state.getLastVisibleSentenceNumber() + "] s:["
                    + state.getFocusSentenceNumber() + "]");

            gotoPageTextField.setModelObject(1);

            setCurationSegmentBeginEnd(editorCas);
            update(aTarget);

            // Re-render the whole page because the font size
            if (aTarget != null) {
                aTarget.add(this);
            }

            // Update document state
            if (state.getDocument().getState().equals(SourceDocumentState.NEW)) {
                state.getDocument().setState(SourceDocumentStateTransition
                        .transition(SourceDocumentStateTransition.NEW_TO_ANNOTATION_IN_PROGRESS));
                repository.createSourceDocument(state.getDocument());
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
    protected void actionRefreshDocument(AjaxRequestTarget aTarget, JCas aEditorCas)
    {
        try {
            AnnotatorState state = getModelObject();
            SuggestionBuilder builder = new SuggestionBuilder(repository, annotationService,
                    userRepository);
            curationContainer = builder.buildCurationContainer(state);
            setCurationSegmentBeginEnd(aEditorCas);
            curationContainer.setBratAnnotatorModel(state);
            update(aTarget);
            annotationEditor.render(aTarget, aEditorCas);
        }
        catch (Exception e) {
            handleException(aTarget, e);
        }
    }
    
    /**
     * Only project admins and annotators can see this page
     */
    @MenuItemCondition
    public static boolean menuItemCondition(RepositoryService aRepo, UserDao aUserRepo)
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = aUserRepo.get(username);
        return SecurityUtil.annotationEnabeled(aRepo, user, Mode.CORRECTION);
    }
}

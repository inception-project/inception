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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.page;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static org.apache.uima.fit.util.JCasUtil.select;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.api.CorrectionDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.SettingsService;
import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.SecurityUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
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
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.GuidelineModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog.OpenModalWindowPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItem;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItemCondition;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.CurationPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.CurationContainer;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.SuggestionBuilder;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.dialog.ReCreateMergeCASModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.dialog.ReMergeCasModel;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;

/**
 * This is the main class for the curation page. It contains an interface which displays differences
 * between user annotations for a specific document. The interface provides a tool for merging these
 * annotations and storing them as a new annotation.
 *
 */
@MenuItem(icon="images/data_table.png", label="Curation", prio=200)
@MountPath("/curation.html")
public class CurationPage
    extends AnnotationPageBase
{
    private final static Logger LOG = LoggerFactory.getLogger(CurationPage.class);

    private static final long serialVersionUID = 1378872465851908515L;

    @SpringBean(name = "documentRepository")
    private DocumentService documentService;
    
    @SpringBean(name = "documentRepository")
    private CorrectionDocumentService correctionDocumentService;

    @SpringBean(name = "documentRepository")
    private CurationDocumentService curationDocumentService;

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

    private ReMergeCasModel reMerge;
    private CurationContainer curationContainer;

    private CurationPanel curationPanel;
    private AjaxLink<Void> showreCreateMergeCasModal;
    private ModalWindow reCreateMergeCas;

    private WebMarkupContainer finishDocumentIcon;
    private ConfirmationDialog finishDocumentDialog;
    private LambdaAjaxLink finishDocumentLink;
    
    public CurationPage()
    {
        setModel(Model.of(new AnnotatorStateImpl(Mode.CURATION)));
        reMerge = new ReMergeCasModel();

        curationContainer = new CurationContainer();
        curationContainer.setBratAnnotatorModel(getModelObject());

        curationPanel = new CurationPanel("curationPanel", new Model<CurationContainer> (
                curationContainer))
        {
            private static final long serialVersionUID = 2175915644696513166L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget)
            {
                try {
                    actionRefreshDocument(aTarget, getEditorCas());
                }
                catch (Exception e) {
                    handleException(aTarget, e);
                }
//                
//                AnnotatorState state = CurationPage.this.getModelObject();
//                JCas mergeJCas = null;
//                try {
//                    mergeJCas = repository.readCurationCas(state.getDocument());
//                }
//                catch (Exception e) {
//                    aTarget.add(getFeedbackPanel());
//                    LOG.error("Unable to load data", e);
//                    error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
//                }
//                aTarget.add(numberOfPages);
//                gotoPageTextField.setModelObject(state.getFirstVisibleSentenceNumber());
//                gotoPageAddress = getSentenceAddress(mergeJCas, gotoPageTextField.getModelObject());
//                aTarget.add(gotoPageTextField);
//                aTarget.add(curationPanel);
            }
        };
        add(curationPanel);

        add(documentNamePanel = new DocumentNamePanel("documentNamePanel", getModel()));
        documentNamePanel.setOutputMarkupId(true);

        add(getOrCreatePositionInfoLabel());

        add(openDocumentsModal = new ModalWindow("openDocumentsModal"));
        openDocumentsModal.setOutputMarkupId(true);
        openDocumentsModal.setInitialWidth(620);
        openDocumentsModal.setInitialHeight(440);
        openDocumentsModal.setResizable(true);
        openDocumentsModal.setWidthUnit("px");
        openDocumentsModal.setHeightUnit("px");
        openDocumentsModal.setTitle("Open document");

        add(new AnnotationPreferencesModalPanel("annotationLayersModalPanel", getModel(),
                curationPanel.editor)
        {
            private static final long serialVersionUID = -4657965743173979437L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget)
            {
                actionCompletePreferencesChange(aTarget);
            }
        });

        add(new ExportModalPanel("exportModalPanel", getModel())
        {
            private static final long serialVersionUID = -468896211970839443L;

            {
                setOutputMarkupId(true);
                setOutputMarkupPlaceholderTag(true);
            }

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                AnnotatorState state = CurationPage.this.getModelObject();
                setVisible(state.getProject() != null
                        && (SecurityUtil.isAdmin(state.getProject(), projectService, state.getUser())
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

        add(reCreateMergeCas = new ModalWindow("reCreateMergeCasModal"));
        reCreateMergeCas.setOutputMarkupId(true);
        //Change size if you change text here
        reCreateMergeCas.setInitialWidth(580);
        reCreateMergeCas.setInitialHeight(40);
        reCreateMergeCas.setResizable(true);
        reCreateMergeCas.setWidthUnit("px");
        reCreateMergeCas.setHeightUnit("px");
        reCreateMergeCas
                .setTitle("Are you sure? All curation annotations for this document will be lost.");

        add(showreCreateMergeCasModal = new AjaxLink<Void>("showreCreateMergeCasModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            protected void onConfigure()
            {
                AnnotatorState state = CurationPage.this.getModelObject();
                setEnabled(state.getDocument() != null
                        && !state.getDocument().getState()
                                .equals(SourceDocumentState.CURATION_FINISHED));
            }

            @Override
            public void onClick(AjaxRequestTarget aTarget)
            {
                actionRemergeDocument(aTarget);
            }
        });
        showreCreateMergeCasModal.setOutputMarkupId(true);
        
        add(new GuidelineModalPanel("guidelineModalPanel", getModel()));        
        
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
                AnnotatorState state = CurationPage.this.getModelObject();
                setEnabled(state.getProject() != null && state.getDocument() != null
                        && !documentService
                                .getSourceDocument(state.getDocument().getProject(),
                                        state.getDocument().getName())
                                .getState().equals(SourceDocumentState.CURATION_FINISHED));
            }
        });
        finishDocumentIcon = new WebMarkupContainer("finishImage");
        finishDocumentIcon.setOutputMarkupId(true);
        finishDocumentIcon.add(new AttributeModifier("src", new LoadableDetachableModel<String>()
        {
            private static final long serialVersionUID = 1562727305401900776L;

            @Override
            protected String load()
            {
                AnnotatorState state = CurationPage.this.getModelObject();
                if (state.getProject() != null && state.getDocument() != null) {
                    if (documentService
                            .getSourceDocument(state.getDocument().getProject(),
                                    state.getDocument().getName()).getState()
                            .equals(SourceDocumentState.CURATION_FINISHED)) {
                        return "images/accept.png";
                    }
                    else {
                        return "images/inprogress.png";
                    }
                }
                else {
                    return "images/inprogress.png";
                }
            }
        }));
        finishDocumentLink.add(finishDocumentIcon);
    }
    
    @Override
    public void setModel(IModel<AnnotatorState> aModel)
    {
        setDefaultModel(aModel);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public IModel<AnnotatorState> getModel()
    {
        return (IModel<AnnotatorState>) getDefaultModel();
    }

    @Override
    public void setModelObject(AnnotatorState aModel)
    {
        setDefaultModelObject(aModel);
    }
    
    @Override
    public AnnotatorState getModelObject()
    {
        return (AnnotatorState) getDefaultModelObject();
    }
    
    @Override
    protected List<SourceDocument> getListOfDocs()
    {
        return curationDocumentService.listCuratableSourceDocuments(getModelObject().getProject());
    }

    // Update the curation panel.

    private void updatePanel(CurationContainer aCurationContainer, AjaxRequestTarget aTarget)
    {
        AnnotatorState state = getModelObject();
//        JCas mergeJCas = null;
//        try {
//            mergeJCas = repository.readCurationCas(state.getDocument());
//        }
//        catch (Exception e) {
//            aTarget.add(getFeedbackPanel());
//            LOG.error("Unable to load data", e);
//            error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
//        }
        gotoPageTextField.setModelObject(state.getFirstVisibleSentenceNumber());
        curationPanel.setDefaultModelObject(curationContainer);
        aTarget.add(gotoPageTextField);
        aTarget.add(curationPanel);
    }

    /**
     * for the first time, open the <b>open document dialog</b>
     */
    @Override
    public void renderHead(IHeaderResponse response)
    {
        super.renderHead(response);

        String jQueryString = "";
        if (firstLoad) {
            jQueryString += "jQuery('#showOpenDocumentModal').trigger('click');";
            firstLoad = false;
        }
        response.render(OnLoadHeaderItem.forScript(jQueryString));
    }

    @Override
    protected JCas getEditorCas()
        throws IOException
    {
        AnnotatorState state = getModelObject();

        if (state.getDocument() == null) {
            throw new IllegalStateException("Please open a document first!");
        }

        return curationDocumentService.readCurationCas(state.getDocument());
    }

    private void updateSentenceNumber(JCas aJCas, int aAddress)
    {
        AnnotatorState state = getModelObject();
        Sentence sentence = selectByAddr(aJCas, Sentence.class, aAddress);
        state.setFirstVisibleSentence(sentence);
        state.setFocusSentenceNumber(WebAnnoCasUtil.getSentenceNumber(aJCas, sentence.getBegin()));
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
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                /*
                 * Changed for #152, getDocument was returning null even after opening a document
                 * Also, surrounded following code into if block to avoid error.
                 */
                if (state.getProject() == null) {
                    setResponsePage(getApplication().getHomePage());
                    return;
                }
                if (state.getDocument() != null) {
                    try {
                        documentService.createSourceDocument(state.getDocument());
                        documentService.upgradeCasAndSave(state.getDocument(), state.getMode(), username);

                        actionLoadDocument(aCallbackTarget);
                        curationPanel.editor.loadFeatureEditorModels(aCallbackTarget);
                    }
                    catch (Exception e) {
                        LOG.error("Unable to load data", e);
                        error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
                    }
                }
            }
        });
        openDocumentsModal.show(aTarget);
    }

    private void actionGotoPage(AjaxRequestTarget aTarget, Form<?> aForm)
        throws Exception
    {
        AnnotatorState state = getModelObject();
        
        JCas jcas = getEditorCas();
        List<Sentence> sentences = new ArrayList<>(select(jcas, Sentence.class));
        int selectedSentence = gotoPageTextField.getModelObject();
        selectedSentence = Math.min(selectedSentence, sentences.size());
        gotoPageTextField.setModelObject(selectedSentence);
        
        state.setFirstVisibleSentence(sentences.get(selectedSentence - 1));
        state.setFocusSentenceNumber(selectedSentence);        
        
        actionRefreshDocument(aTarget, jcas);
        
        curationPanel.updatePanel(aTarget, curationContainer);
    }

    private void actionToggleScriptDirection(AjaxRequestTarget aTarget)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        getModelObject().toggleScriptDirection();

        curationPanel.updatePanel(aTarget, curationContainer);
        updatePanel(curationContainer, aTarget);
    }

    private void actionCompletePreferencesChange(AjaxRequestTarget aTarget)
    {
        AnnotatorState state = CurationPage.this.getModelObject();
        
        // Re-render the whole page because the width of the sidebar may have changed
        aTarget.add(CurationPage.this);
        
        aTarget.add(getOrCreatePositionInfoLabel());
        JCas mergeCas = null;
        try {
            aTarget.add(getFeedbackPanel());
            mergeCas = curationDocumentService.readCurationCas(state.getDocument());
            
            // The number of visible sentences may have changed - let the state recalculate 
            // the visible sentences 
            Sentence sentence = selectByAddr(mergeCas, Sentence.class,
                    state.getFirstVisibleSentenceAddress());
            state.setFirstVisibleSentence(sentence);
            
            curationPanel.updatePanel(aTarget, curationContainer);
            updatePanel(curationContainer, aTarget);
            updateSentenceNumber(mergeCas, state.getFirstVisibleSentenceAddress());
        }
        catch (Exception e) {
            aTarget.add(getFeedbackPanel());
            LOG.error("Unable to load data", e);
            error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private void actionFinishDocument(AjaxRequestTarget aTarget)
    {
        finishDocumentDialog.setConfirmAction((aCallbackTarget) -> {
            ensureRequiredFeatureValuesSet(aCallbackTarget, getEditorCas());
            
            AnnotatorState state = getModelObject();
            SourceDocument sourceDocument = state.getDocument();

            if (sourceDocument.getState().equals(SourceDocumentState.CURATION_FINISHED)) {
                sourceDocument.setState(SourceDocumentStateTransition.transition(
                        SourceDocumentStateTransition.CURATION_FINISHED_TO_CURATION_IN_PROGRESS));
            }
            else {
                sourceDocument.setState(SourceDocumentStateTransition.transition(
                        SourceDocumentStateTransition.CURATION_IN_PROGRESS_TO_CURATION_FINISHED));
                sourceDocument.setProcessed(false);
            }
            
            documentService.createSourceDocument(sourceDocument);
            
            aCallbackTarget.add(finishDocumentIcon);
            aCallbackTarget.add(finishDocumentLink);
            aCallbackTarget.add(curationPanel.editor);
            aCallbackTarget.add(showreCreateMergeCasModal);
        });
        finishDocumentDialog.show(aTarget);
    }

    private void actionRemergeDocument(AjaxRequestTarget aTarget)
    {
        reCreateMergeCas.setContent(new ReCreateMergeCASModalPanel(reCreateMergeCas.getContentId(),
                reCreateMergeCas, reMerge));
        reCreateMergeCas.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
        {
            private static final long serialVersionUID = 4816615910398625993L;

            @Override
            public void onClose(AjaxRequestTarget aCallbackTarget)
            {
                AnnotatorState state = CurationPage.this.getModelObject();
                if (reMerge.isReMerege()) {
                    try {
                        aCallbackTarget.add(getFeedbackPanel());
                        curationDocumentService.removeCurationDocumentContent(state.getDocument(),
                                state.getUser().getUsername());
                        actionLoadDocument(aCallbackTarget);

                        aCallbackTarget.appendJavaScript("alert('Re-merge finished!')");
                    }
                    catch (Exception e) {
                        aCallbackTarget.add(getFeedbackPanel());
                        LOG.error("Unable to load data", e);
                        error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
                    }
                }
            }
        });
        reCreateMergeCas.show(aTarget);
    }

    /**
     * Open a document or to a different document. This method should be used only the first time
     * that a document is accessed. It reset the annotator state and upgrades the CAS.
     */
    @Override
    protected void actionLoadDocument(AjaxRequestTarget aTarget)
    {
        LOG.info("BEGIN LOAD_DOCUMENT_ACTION");
        
        AnnotatorState state = getModelObject();
        
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.get(username);

        state.setUser(user);

        try {
            // Update source document state to CURRATION_INPROGRESS, if it was not ANNOTATION_FINISHED
            if (!state.getDocument().getState().equals(SourceDocumentState.CURATION_FINISHED)) {
                state.getDocument().setState(SourceDocumentStateTransition.transition(
                        SourceDocumentStateTransition.ANNOTATION_IN_PROGRESS_TO_CURATION_IN_PROGRESS));
                documentService.createSourceDocument(state.getDocument());
            }
    
            // Load user preferences
            PreferencesUtil.loadPreferences(username, settingsService, projectService,
                    annotationService, state, state.getMode());            
            
            // Re-render whole page as sidebar size preference may have changed
            aTarget.add(CurationPage.this);
    
            List<AnnotationDocument> finishedAnnotationDocuments = new ArrayList<AnnotationDocument>();
    
            for (AnnotationDocument annotationDocument : documentService
                    .listAnnotationDocuments(state.getDocument())) {
                if (annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)) {
                    finishedAnnotationDocuments.add(annotationDocument);
                }
            }
    
            SuggestionBuilder cb = new SuggestionBuilder(documentService, correctionDocumentService,
                    curationDocumentService, annotationService, userRepository);
            AnnotationDocument randomAnnotationDocument = null;
            if (finishedAnnotationDocuments.size() > 0) {
                randomAnnotationDocument = finishedAnnotationDocuments.get(0);
            }
    
            // upgrade CASes for each user, what if new type is added once the user finished
            // annotation
            for (AnnotationDocument ad : finishedAnnotationDocuments) {
                documentService.upgradeCasAndSave(ad.getDocument(), state.getMode(), ad.getUser());
            }
            Map<String, JCas> jCases = cb.listJcasesforCuration(finishedAnnotationDocuments,
                    randomAnnotationDocument, state.getMode());
            JCas mergeJCas = cb.getMergeCas(state, state.getDocument(), jCases,
                    randomAnnotationDocument);
    
            // (Re)initialize brat model after potential creating / upgrading CAS
            state.clearAllSelections();
            state.getPreferences().setCurationWindowSize(WebAnnoCasUtil.getSentenceCount(mergeJCas));
            
            // Initialize the visible content
            state.setFirstVisibleSentence(WebAnnoCasUtil.getFirstSentence(mergeJCas));
    
            // if project is changed, reset some project specific settings
            if (currentprojectId != state.getProject().getId()) {
                state.clearRememberedFeatures();
            }
    
            currentprojectId = state.getProject().getId();
    
            SuggestionBuilder builder = new SuggestionBuilder(documentService,
                    correctionDocumentService, curationDocumentService, annotationService,
                    userRepository);
            curationContainer = builder.buildCurationContainer(state);
            curationContainer.setBratAnnotatorModel(state);
            curationPanel.editor.reset(aTarget);
            curationPanel.updatePanel(aTarget, curationContainer);
            updatePanel(curationContainer, aTarget);
            updateSentenceNumber(mergeJCas, state.getFirstVisibleSentenceAddress());
    
            
            // Load constraints
            state.setConstraints(constraintsService.loadConstraints(state.getProject()));
    
            aTarget.add(getOrCreatePositionInfoLabel());
            aTarget.add(documentNamePanel);
            aTarget.add(showreCreateMergeCasModal);
            aTarget.add(finishDocumentLink);
        }
        catch (Exception e) {
            handleException(aTarget, e);
        }
    
        LOG.info("END LOAD_DOCUMENT_ACTION");
    }

    @Override
    protected void actionRefreshDocument(AjaxRequestTarget aTarget, JCas aJCas)
    {
        try {
            aTarget.add(getOrCreatePositionInfoLabel());
            curationPanel.updatePanel(aTarget, curationContainer);
            updatePanel(curationContainer, aTarget);
        }
        catch (Exception e) {
            handleException(aTarget, e);
        }
    }
    
    /**
     * Only project admins and curators can see this page
     */
    @MenuItemCondition
    public static boolean menuItemCondition(ProjectService aRepo, UserDao aUserRepo)
    {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = aUserRepo.get(username);
        return SecurityUtil.curationEnabeled(aRepo, user);
    }
}

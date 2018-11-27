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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateUtils.verifyAndUpdateDocumentTimestamp;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition.ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED;
import static org.apache.uima.fit.util.JCasUtil.select;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
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
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
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
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotationEditor;
import de.tudarmstadt.ukp.clarin.webanno.brat.config.BratProperties;
import de.tudarmstadt.ukp.clarin.webanno.brat.util.BratAnnotatorUtility;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ConfirmationDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.ActionBarLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxSubmitLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.DecoratedObject;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.PreferencesUtil;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.DocumentNamePanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.FinishImage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.AnnotationDetailEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog.AnnotationPreferencesDialog;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog.ExportDocumentDialog;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog.GuidelinesDialog;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog.OpenDocumentDialog;
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
 * This is the main class for the correction page. Displays in the lower panel the Automatically
 * annotated document and in the upper panel the corrected annotation
 */
@MountPath("/correction.html")
@ProjectType(id = WebAnnoConst.PROJECT_TYPE_CORRECTION, prio = 120)
public class CorrectionPage
    extends AnnotationPageBase
{
    private static final Logger LOG = LoggerFactory.getLogger(CorrectionPage.class);

    private static final long serialVersionUID = 1378872465851908515L;

    private @SpringBean CasStorageService casStorageService;
    private @SpringBean DocumentService documentService;
    private @SpringBean CurationDocumentService curationDocumentService;
    private @SpringBean CorrectionDocumentService correctionDocumentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean ConstraintsService constraintsService;
    private @SpringBean BratProperties defaultPreferences;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean UserDao userRepository;

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

    private CurationContainer curationContainer;

    private Map<String, Map<Integer, AnnotationSelection>> annotationSelectionByUsernameAndAddress =
            new HashMap<>();

    private SourceListView curationSegment = new SourceListView();

    public CorrectionPage()
    {
        commonInit();
    }
    
    private void commonInit()
    {
        setVersioned(false);
        
        setModel(Model.of(new AnnotatorStateImpl(Mode.CORRECTION)));

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
        
        suggestionView = new SuggestionViewPanel("correctionView", new ListModel<>(segments))
        {
            private static final long serialVersionUID = 2583509126979792202L;

            @Override
            public void onChange(AjaxRequestTarget aTarget)
            {
                AnnotatorState state = CorrectionPage.this.getModelObject();
                
                aTarget.addChildren(getPage(), IFeedback.class);
                try {
                    // update begin/end of the curation segment based on bratAnnotatorModel changes
                    // (like sentence change in auto-scroll mode,....
                    curationContainer.setBratAnnotatorModel(state);
                    JCas editorCas = getEditorCas();
                    setCurationSegmentBeginEnd(editorCas);

                    suggestionView.updatePanel(aTarget, curationContainer,
                            annotationSelectionByUsernameAndAddress, curationSegment);
                    
                    annotationEditor.requestRender(aTarget);
                    aTarget.add(getOrCreatePositionInfoLabel());
                    update(aTarget);
                }
                catch (UIMAException e) {
                    LOG.error("Error: " + e.getMessage(), e);
                    error("Error: " + ExceptionUtils.getRootCauseMessage(e));
                }
                catch (Exception e) {
                    LOG.error("Error: " + e.getMessage(), e);
                    error("Error: " + e.getMessage());
                }
            }
        };

        suggestionView.setOutputMarkupId(true);
        add(suggestionView);

        rightSidebar.add(detailEditor = createDetailEditor());
        
        annotationEditor = new BratAnnotationEditor("mergeView", getModel(), detailEditor,
            this::getEditorCas);
        add(annotationEditor);

        curationContainer = new CurationContainer();
        curationContainer.setBratAnnotatorModel(getModelObject());

        add(documentNamePanel = createDocumentInfoLabel());

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
            private static final long serialVersionUID = -708400631769656072L;

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
                
                AnnotatorState state = CorrectionPage.this.getModelObject();
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
        return new LoadableDetachableModel<List<DecoratedObject<Project>>>()
        {
            private static final long serialVersionUID = -2518743298741342852L;

            @Override
            protected List<DecoratedObject<Project>> load()
            {
                User user = userRepository.get(
                        SecurityContextHolder.getContext().getAuthentication().getName());
                List<DecoratedObject<Project>> allowedProject = new ArrayList<>();
                for (Project project : projectService.listProjects()) {
                    if (projectService.isAnnotator(project, user)
                            && WebAnnoConst.PROJECT_TYPE_CORRECTION.equals(project.getMode())) {
                        allowedProject.add(DecoratedObject.of(project));
                    }
                }
                return allowedProject;
            }
        };
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
                aTarget.addChildren(getPage(), IFeedback.class);
                aTarget.add(getOrCreatePositionInfoLabel());

                try {
                    AnnotatorState state = getModelObject();
                    JCas editorCas = getEditorCas();
                    //JCas correctionCas = repository.readCorrectionCas(state.getDocument());
                    annotationEditor.requestRender(aTarget);

                    // info(bratAnnotatorModel.getMessage());
                    SuggestionBuilder builder = new SuggestionBuilder(casStorageService,
                            documentService, correctionDocumentService, curationDocumentService,
                            annotationService, userRepository);
                    curationContainer = builder.buildCurationContainer(state);
                    setCurationSegmentBeginEnd(editorCas);
                    curationContainer.setBratAnnotatorModel(state);

                    update(aTarget);
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
            public JCas getEditorCas() throws IOException
            {
                return CorrectionPage.this.getEditorCas();
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
        
        // If we have a timestamp, then use it to detect if there was a concurrent access
        verifyAndUpdateDocumentTimestamp(state, documentService
                .getAnnotationCasTimestamp(state.getDocument(), state.getUser().getUsername()));
        
        return documentService.readAnnotationCas(getModelObject().getDocument(),
                state.getUser().getUsername());
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
        
        JCas editorCas = getEditorCas();
        List<Sentence> sentences = new ArrayList<>(select(editorCas, Sentence.class));
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

            JCas editorCas = getEditorCas();

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

    /**
     * Reset the document by removing all annotations form the initial CAS and using the result as
     * the editor CAS.
     */
    @Override
    protected void actionResetDocument(AjaxRequestTarget aTarget)
        throws Exception
    {
        AnnotatorState state = getModelObject();
        JCas editorCas = documentService.createOrReadInitialCas(state.getDocument());
        editorCas = BratAnnotatorUtility.clearJcasAnnotations(editorCas);
        documentService.writeAnnotationCas(editorCas, state.getDocument(), state.getUser(), false);
        actionLoadDocument(aTarget);
    }
    
    private void actionFinishDocument(AjaxRequestTarget aTarget)
    {
        finishDocumentDialog.setConfirmAction((aCallbackTarget) -> {
            ensureRequiredFeatureValuesSet(aCallbackTarget, getEditorCas());
            
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
            if (correctionDocumentService.existsCorrectionCas(state.getDocument())) {
                correctionCas = correctionDocumentService.readCorrectionCas(state.getDocument());
            }
            else {
                correctionCas = documentService.createOrReadInitialCas(state.getDocument());
            }

            // Read the annotation CAS or create an annotation CAS from the initial CAS by stripping
            // annotations
            JCas editorCas;
            if (documentService.existsCas(state.getDocument(), user.getUsername())) {
                editorCas = documentService.readAnnotationCas(annotationDocument);
            }
            else {
                editorCas = documentService.createOrReadInitialCas(state.getDocument());
                editorCas = BratAnnotatorUtility.clearJcasAnnotations(editorCas);
                documentService.writeAnnotationCas(editorCas, state.getDocument(), user, false);
            }

            // Update the CASes
            annotationService.upgradeCas(editorCas.getCas(), annotationDocument);
            correctionDocumentService.upgradeCorrectionCas(correctionCas.getCas(),
                    state.getDocument());

            // After creating an new CAS or upgrading the CAS, we need to save it
            documentService.writeAnnotationCas(editorCas.getCas().getJCas(),
                    annotationDocument.getDocument(), user, false);
            correctionDocumentService.writeCorrectionCas(correctionCas, state.getDocument());

            // (Re)initialize brat model after potential creating / upgrading CAS
            state.reset();

            // Initialize timestamp in state
            Optional<Long> diskTimestamp = documentService
                    .getAnnotationCasTimestamp(state.getDocument(), state.getUser().getUsername());
            if (diskTimestamp.isPresent()) {
                state.setAnnotationDocumentTimestamp(diskTimestamp.get());
            }

            // Load constraints
            state.setConstraints(constraintsService.loadConstraints(state.getProject()));

            // Load user preferences
            PreferencesUtil.loadPreferences(username, defaultPreferences, projectService,
                    annotationService, state, state.getMode());

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
            suggestionView.init(aTarget, curationContainer,
                    annotationSelectionByUsernameAndAddress, curationSegment);
            update(aTarget);

            // Re-render the whole page because the font size
            if (aTarget != null) {
                aTarget.add(this);
            }

            // Update document state
            documentService.transitionSourceDocumentState(state.getDocument(),
                    SourceDocumentStateTransition.NEW_TO_ANNOTATION_IN_PROGRESS);            
            
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

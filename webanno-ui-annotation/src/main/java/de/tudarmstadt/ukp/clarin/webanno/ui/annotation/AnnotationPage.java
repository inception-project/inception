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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.NoResultException;

import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.SecurityUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotator;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ChallengeResponseDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ConfirmationDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.AnnotationPreferencesModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.DocumentNamePanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.ExportModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.FinishImage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.GuidelineModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.AnnotationDetailEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog.OpenDocumentDialog;
import de.tudarmstadt.ukp.clarin.webanno.webapp.core.app.ApplicationPageBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;

/**
 * A wicket page for the Brat Annotation/Visualization page. Included components for pagination,
 * annotation layer configuration, and Exporting document
 */
@MountPath(value = "/annotation.html", alt = "/annotate/${" + AnnotationPage.PAGE_PARAM_PROJECT_ID + "}/${"
        + AnnotationPage.PAGE_PARAM_DOCUMENT_ID + "}")
public class AnnotationPage
    extends ApplicationPageBase
{
    private static final Logger LOG = LoggerFactory.getLogger(AnnotationPage.class);

    private static final long serialVersionUID = 1378872465851908515L;
    
    public static final String PAGE_PARAM_PROJECT_ID = "projectId";
    public static final String PAGE_PARAM_DOCUMENT_ID = "documentId";

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "userRepository")
    private UserDao userRepository;

    private NumberTextField<Integer> gotoPageTextField;
    private Label numberOfPages;
    private DocumentNamePanel documentNamePanel;
    
    private long currentprojectId;

    // Open the dialog window on first load
    private boolean firstLoad = true;

    private int gotoPageAddress;

    private ModalWindow openDocumentsModal;

    private BratAnnotator annotator;

    private AnnotationDetailEditorPanel editor;    

    private ChallengeResponseDialog resetDocumentDialog;
    private LambdaAjaxLink resetDocumentLink;
    
    private FinishImage finishDocumentIcon;
    private ConfirmationDialog finishDocumentDialog;
    private LambdaAjaxLink finishDocumentLink;

    private WebMarkupContainer sidebarCell;
    private WebMarkupContainer annotationViewCell;
    
    public AnnotationPage()
    {
        super();
        LOG.debug("Setting up annotation page without parameters");
        commonInit();
    }
    
    public AnnotationPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);
        LOG.debug("Setting up annotation page with parameters: {}", aPageParameters);

        commonInit();

        long projectId = aPageParameters.get("projectId").toLong();
        Project project;
        try {
            project = repository.getProject(projectId);
        }
        catch (NoResultException e) {
            error("Project [" + projectId + "] does not exist");
            return;
        }
       
        long documentId = aPageParameters.get("documentId").toLong();
        SourceDocument document;
        try {
            document = repository.getSourceDocument(projectId, documentId);
        }
        catch (NoResultException e) {
            error("Document [" + documentId + "] does not exist in project [" + projectId + "]");
            return;
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.get(username);
        
        if (!SecurityUtil.isAnnotator(project, repository, user)) {
            error("You have no permission to access document [" + documentId + "] in project ["
                    + projectId + "]");
            return;
        }

        firstLoad = false;
        
        getModelObject().setProject(project);
        getModelObject().setDocument(document);

        actionLoadDocument(null);
    }
    
    private void commonInit() {
        setModel(Model.of(new AnnotatorStateImpl(Mode.ANNOTATION)));
        
        setVersioned(false);
        
        sidebarCell = new WebMarkupContainer("sidebarCell") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTag(ComponentTag aTag)
            {
                super.onComponentTag(aTag);
                AnnotatorState state = AnnotationPage.this.getModelObject();
                aTag.put("width", state.getPreferences().getSidebarSize()+"%");
            }
        };
        sidebarCell.setOutputMarkupId(true);
        add(sidebarCell);

        annotationViewCell = new WebMarkupContainer("annotationViewCell") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTag(ComponentTag aTag)
            {
                super.onComponentTag(aTag);
                AnnotatorState state = AnnotationPage.this.getModelObject();
                aTag.put("width", (100-state.getPreferences().getSidebarSize())+"%");
            }
        };
        annotationViewCell.setOutputMarkupId(true);
        add(annotationViewCell);

        editor = new AnnotationDetailEditorPanel("annotationDetailEditorPanel", getModel())
        {
            private static final long serialVersionUID = 2857345299480098279L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget)
            {
                aTarget.addChildren(getPage(), FeedbackPanel.class);
                aTarget.add(numberOfPages);

                try {
                    annotator.bratRender(aTarget, getEditorCas());
                    annotator.bratSetHighlight(aTarget,
                            getModelObject().getSelection().getAnnotation());
                }
                catch (Exception e) {
                    LOG.info("Error reading CAS: {} " + e.getMessage(), e);
                    error("Error reading CAS: " + e.getMessage());
                    return;
                }
            }

            @Override
            protected void onAutoForward(AjaxRequestTarget aTarget)
            {
                try {
                    annotator.bratRender(aTarget, getEditorCas());
                }
                catch (Exception e) {
                    LOG.info("Error reading CAS: {} " + e.getMessage(), e);
                    error("Error reading CAS " + e.getMessage());
                    return;
                }
            }
        };
        sidebarCell.add(editor);
        
        annotator = new BratAnnotator("embedder1", getModel(), editor);
        annotationViewCell.add(annotator);

        add(documentNamePanel = (DocumentNamePanel) new DocumentNamePanel("documentNamePanel",
                getModel()).setOutputMarkupId(true));

        numberOfPages = new Label("numberOfPages", new Model<String>());
        numberOfPages.setOutputMarkupId(true);
        add(numberOfPages);

        add(openDocumentsModal = new OpenDocumentDialog("openDocumentsModal", getModel()) {
            private static final long serialVersionUID = 5474030848589262638L;

            @Override
            public void onDocumentSelected(AjaxRequestTarget aTarget)
            {
                //actionLoadDocument(aTarget);
                PageParameters pageParameters = new PageParameters();
                pageParameters.set(PAGE_PARAM_PROJECT_ID, getModelObject().getProject().getId());
                pageParameters.set(PAGE_PARAM_DOCUMENT_ID, getModelObject().getDocument().getId());
                setResponsePage(AnnotationPage.class, pageParameters);
            }
        });

        add(new AnnotationPreferencesModalPanel("annotationLayersModalPanel", getModel(), editor)
        {
            private static final long serialVersionUID = -4657965743173979437L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget)
            {
                try {
                    AnnotatorState state = AnnotationPage.this.getModelObject();
                    
                    JCas jCas = getEditorCas();
                    
                    // The number of visible sentences may have changed - let the state recalculate 
                    // the visible sentences 
                    Sentence sentence = selectByAddr(jCas, Sentence.class,
                            state.getFirstVisibleSentenceAddress());
                    state.setFirstVisibleSentence(sentence);
                    
                    updateSentenceAddress(jCas, aTarget);
                    
                    // Re-render the whole page because the width of the sidebar may have changed
                    aTarget.add(AnnotationPage.this);
                }
                catch (Exception e) {
                    LOG.info("Error reading CAS " + e.getMessage());
                    error("Error reading CAS " + e.getMessage());
                    return;
                }
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
                AnnotatorState state = AnnotationPage.this.getModelObject();
                setVisible(state.getProject() != null
                        && (SecurityUtil.isAdmin(state.getProject(), repository, state.getUser())
                                || !state.getProject().isDisableExport()));
            }
        });

        gotoPageTextField = (NumberTextField<Integer>) new NumberTextField<Integer>("gotoPageText",
                new Model<Integer>(0));
        Form<Void> gotoPageTextFieldForm = new Form<Void>("gotoPageTextFieldForm");
        gotoPageTextFieldForm.add(new AjaxFormSubmitBehavior(gotoPageTextFieldForm, "submit")
        {
            private static final long serialVersionUID = -1L;

            @Override
            protected void onSubmit(AjaxRequestTarget aTarget)
            {
                try {
                    actionEnterPageNumer(aTarget);
                }
                catch (Exception e) {
                    handleException(aTarget, e);
                }
            }
        });
        gotoPageTextField.setType(Integer.class);
        gotoPageTextField.setMinimum(1);
        gotoPageTextField.setDefaultModelObject(1);
        add(gotoPageTextFieldForm.add(gotoPageTextField));
        gotoPageTextField.add(new AjaxFormComponentUpdatingBehavior("change")
        {
            private static final long serialVersionUID = 56637289242712170L;

            @Override
            protected void onUpdate(AjaxRequestTarget aTarget)
            {
                try {
                    if (gotoPageTextField.getModelObject() < 1) {
                        aTarget.appendJavaScript("alert('Page number shouldn't be less than 1')");
                    }
                    else {
                        updateSentenceAddress(getEditorCas(), aTarget);
                    }
                }
                catch (Exception e) {
                    error(e.getMessage());
                    aTarget.addChildren(getPage(), FeedbackPanel.class);
                }
            }
        });

        add(new LambdaAjaxLink("showOpenDocumentModal", this::actionOpenDocument));
        
        add(new LambdaAjaxLink("showPreviousDocument", this::actionShowPreviousDocument)
                .add(new InputBehavior(new KeyType[] { KeyType.Shift, KeyType.Page_up },
                        EventType.click)));

        add(new LambdaAjaxLink("showNextDocument", this::actionShowNextDocument)
                .add(new InputBehavior(new KeyType[] { KeyType.Shift, KeyType.Page_down },
                        EventType.click)));

        add(new LambdaAjaxLink("showNext", this::actionShowNextPage)
                .add(new InputBehavior(new KeyType[] { KeyType.Page_down }, EventType.click)));

        add(new LambdaAjaxLink("showPrevious", this::actionShowPreviousPage)
                .add(new InputBehavior(new KeyType[] { KeyType.Page_up }, EventType.click)));

        add(new LambdaAjaxLink("showFirst", this::actionShowFirstPage)
                .add(new InputBehavior(new KeyType[] { KeyType.Home }, EventType.click)));

        add(new LambdaAjaxLink("showLast", this::actionShowLastPage)
                .add(new InputBehavior(new KeyType[] { KeyType.End }, EventType.click)));

        add(new LambdaAjaxLink("gotoPageLink", this::actionGotoPage));

        add(new LambdaAjaxLink("toggleScriptDirection", this::actionToggleScriptDirection));
        
        add(new GuidelineModalPanel("guidelineModalPanel", getModel()));
        
        IModel<String> documentNameModel = PropertyModel.of(getModel(), "document.name");
        add(resetDocumentDialog = new ChallengeResponseDialog("resetDocumentDialog",
                new StringResourceModel("ResetDocumentDialog.title", this, null),
                new StringResourceModel("ResetDocumentDialog.text", this, getModel(),
                        documentNameModel),
                documentNameModel));
        add(resetDocumentLink = new LambdaAjaxLink("showResetDocumentDialog",
                this::actionResetDocument)
        {
            private static final long serialVersionUID = 874573384012299998L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                AnnotatorState state = AnnotationPage.this.getModelObject();
                setEnabled(state.getDocument() != null
                        && !repository.isAnnotationFinished(state.getDocument(), state.getUser()));
            }
        });
        resetDocumentLink.setOutputMarkupId(true);
        
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
                AnnotatorState state = AnnotationPage.this.getModelObject();
                setEnabled(state.getDocument() != null
                        && !repository.isAnnotationFinished(state.getDocument(), state.getUser()));
            }
        });
        finishDocumentIcon = new FinishImage("finishImage", getModel());
        finishDocumentIcon.setOutputMarkupId(true);
        finishDocumentLink.add(finishDocumentIcon);
    }
    
    public void setModel(IModel<AnnotatorState> aModel)
    {
        setDefaultModel(aModel);
    }
    
    @SuppressWarnings("unchecked")
    public IModel<AnnotatorState> getModel()
    {
        return (IModel<AnnotatorState>) getDefaultModel();
    }

    public void setModelObject(AnnotatorState aModel)
    {
        setDefaultModelObject(aModel);
    }
    
    public AnnotatorState getModelObject()
    {
        return (AnnotatorState) getDefaultModelObject();
    }
    
    private List<SourceDocument> getListOfDocs()
    {
        AnnotatorState state = getModelObject();
        return new ArrayList<>(
                repository.listAnnotatableDocuments(state.getProject(), state.getUser()).keySet());
    }

    private void updateSentenceAddress(JCas aJCas, AjaxRequestTarget aTarget)
    {
        AnnotatorState state = getModelObject();

        gotoPageAddress = WebAnnoCasUtil.getSentenceAddress(aJCas,
                gotoPageTextField.getModelObject());

        String labelText = "";
        if (state.getDocument() != null) {

            List<SourceDocument> listofDoc = getListOfDocs();

            int docIndex = listofDoc.indexOf(state.getDocument()) + 1;

            int totalNumberOfSentence = WebAnnoCasUtil.getNumberOfPages(aJCas);

            // If only one page, start displaying from sentence 1
            if (totalNumberOfSentence == 1) {
                state.setFirstVisibleSentence(WebAnnoCasUtil.getFirstSentence(aJCas));
            }

            labelText = "showing " + state.getFirstVisibleSentenceNumber() + "-"
                    + state.getLastVisibleSentenceNumber() + " of " + totalNumberOfSentence
                    + " sentences [document " + docIndex + " of " + listofDoc.size() + "]";
        }
        else {
            labelText = "";// no document yet selected
        }

        numberOfPages.setDefaultModelObject(labelText);
        if (aTarget != null) {
            aTarget.add(numberOfPages);
            aTarget.add(gotoPageTextField);
        }
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

    private JCas getEditorCas()
        throws IOException
    {
        AnnotatorState state = getModelObject();

        if (state.getDocument() == null) {
            throw new IllegalStateException("Please open a document first!");
        }
        
        SourceDocument aDocument = getModelObject().getDocument();

        AnnotationDocument annotationDocument = repository.getAnnotationDocument(aDocument,
                state.getUser());

        // If there is no CAS yet for the annotation document, create one.
        return repository.readAnnotationCas(annotationDocument);
    }

    private void updateSentenceNumber(JCas aJCas, int aAddress)
    {
        AnnotatorState state = AnnotationPage.this.getModelObject();
        
        Sentence sentence = selectByAddr(aJCas, Sentence.class, aAddress);
        state.setFirstVisibleSentence(sentence);
        state.setFocusSentenceNumber(
                WebAnnoCasUtil.getSentenceNumber(aJCas, sentence.getBegin()));
    }

    private void actionOpenDocument(AjaxRequestTarget aTarget)
    {
        getModelObject().getSelection().clear();
        openDocumentsModal.show(aTarget);
    }

    /**
     * Show the previous document, if exist
     */
    private void actionShowPreviousDocument(AjaxRequestTarget aTarget)
    {
        getModelObject().moveToPreviousDocument(getListOfDocs());
        actionLoadDocument(aTarget);
    }

    /**
     * Show the next document if exist
     */
    private void actionShowNextDocument(AjaxRequestTarget aTarget)
    {
        getModelObject().moveToNextDocument(getListOfDocs());
        actionLoadDocument(aTarget);
    }

    private void actionGotoPage(AjaxRequestTarget aTarget)
        throws IOException
    {
        AnnotatorState state = getModelObject();

        if (gotoPageAddress == 0) {
            throw new IllegalStateException("The sentence number entered is not valid");
        }

        if (state.getFirstVisibleSentenceAddress() != gotoPageAddress) {
            JCas jCas = getEditorCas();
            updateSentenceNumber(jCas, gotoPageAddress);
            updateSentenceAddress(jCas, aTarget);
            annotator.bratRenderLater(aTarget);
        }
    }

    private void actionEnterPageNumer(AjaxRequestTarget aTarget)
        throws IOException
    {
        AnnotatorState state = getModelObject();

        if (gotoPageAddress == 0) {
            throw new IllegalStateException("The sentence number entered is not valid");
        }

        if (state.getFirstVisibleSentenceAddress() != gotoPageAddress) {
            JCas jCas = getEditorCas();
            updateSentenceNumber(jCas, gotoPageAddress);
            gotoPageTextField.setModelObject(state.getFirstVisibleSentenceNumber());
            updateSentenceAddress(jCas, aTarget);
            annotator.bratRenderLater(aTarget);
        }
    }

    private void actionShowPreviousPage(AjaxRequestTarget aTarget)
        throws IOException
    {
        JCas jcas = getEditorCas();
        getModelObject().moveToPreviousPage(jcas);
        actionRefreshDocument(aTarget, jcas);
    }

    private void actionShowNextPage(AjaxRequestTarget aTarget)
        throws IOException
    {
        JCas jcas = getEditorCas();
        getModelObject().moveToNextPage(jcas);
        actionRefreshDocument(aTarget, jcas);
    }

    private void actionShowFirstPage(AjaxRequestTarget aTarget)
        throws IOException
    {
        JCas jcas = getEditorCas();
        getModelObject().moveToFirstPage(jcas);
        actionRefreshDocument(aTarget, jcas);
    }

    private void actionShowLastPage(AjaxRequestTarget aTarget)
        throws IOException
    {
        JCas jcas = getEditorCas();
        getModelObject().moveToLastPage(jcas);
        actionRefreshDocument(aTarget, jcas);
    }

    private void actionToggleScriptDirection(AjaxRequestTarget aTarget)
    {
        getModelObject().toggleScriptDirection();
        annotator.bratRenderLater(aTarget);
    }
    
    private void actionResetDocument(AjaxRequestTarget aTarget)
    {
        resetDocumentDialog.setConfirmAction((target) -> {
            AnnotatorState state = getModelObject();
            JCas jcas = repository.createOrReadInitialCas(state.getDocument());
            repository.writeAnnotationCas(jcas, state.getDocument(), state.getUser());
            actionLoadDocument(target);
        });
        resetDocumentDialog.show(aTarget);
    }

    private void actionFinishDocument(AjaxRequestTarget aTarget)
    {
        finishDocumentDialog.setConfirmAction((target) -> {
            AnnotatorState state = getModelObject();
            AnnotationDocument annotationDocument = repository.getAnnotationDocument(
                    state.getDocument(), state.getUser());

            annotationDocument.setState(AnnotationDocumentStateTransition.transition(
                    AnnotationDocumentStateTransition.ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED));
            
            // manually update state change!! No idea why it is not updated in the DB
            // without calling createAnnotationDocument(...)
            repository.createAnnotationDocument(annotationDocument);
            
            target.add(finishDocumentIcon);
            target.add(finishDocumentLink);
            target.add(editor);
            target.add(resetDocumentLink);
        });
        finishDocumentDialog.show(aTarget);
    }

    /**
     * Open a document or to a different document. This method should be used only the first time
     * that a document is accessed. It reset the annotator state and upgrades the CAS.
     */
    private void actionLoadDocument(AjaxRequestTarget aTarget)
    {
        LOG.info("BEGIN LOAD_DOCUMENT_ACTION");

        AnnotatorState state = getModelObject();
        
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.get(username);

        state.setUser(user);

        try {
            // Check if there is an annotation document entry in the database. If there is none,
            // create one.
            AnnotationDocument annotationDocument = repository
                    .createOrGetAnnotationDocument(state.getDocument(), user);

            // Read the CAS
            JCas annotationCas = repository.readAnnotationCas(annotationDocument);

            // Update the annotation document CAS
            repository.upgradeCas(annotationCas.getCas(), annotationDocument);

            // After creating an new CAS or upgrading the CAS, we need to save it
            repository.writeAnnotationCas(annotationCas.getCas().getJCas(),
                    annotationDocument.getDocument(), user);

            // (Re)initialize brat model after potential creating / upgrading CAS
            state.clearAllSelections();

            // Load constraints
            state.setConstraints(repository.loadConstraints(state.getProject()));

            // Load user preferences
            PreferencesUtil.loadPreferences(username, repository, annotationService, state,
                    state.getMode());

            // Initialize the visible content
            state.setFirstVisibleSentence(WebAnnoCasUtil.getFirstSentence(annotationCas));
            
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

            updateSentenceAddress(annotationCas, aTarget);

            // Re-render the whole page because the font size
            if (aTarget != null) {
                aTarget.add(AnnotationPage.this);
            }

            // Update document state
            if (state.getDocument().getState().equals(SourceDocumentState.NEW)) {
                state.getDocument().setState(SourceDocumentStateTransition
                        .transition(SourceDocumentStateTransition.NEW_TO_ANNOTATION_IN_PROGRESS));
                repository.createSourceDocument(state.getDocument());
            }
            
            // Reset the editor
            editor.reset(aTarget);
            // Populate the layer dropdown box
            editor.loadFeatureEditorModels(annotationCas, aTarget);
        }
        catch (Exception e) {
            handleException(aTarget, e);
        }

        LOG.info("END LOAD_DOCUMENT_ACTION");
    }
    
    /**
     * Re-render the document and update all related UI elements.
     * 
     * This method should be used while the editing process is ongoing. It does not upgrade the CAS
     * and it does not reset the annotator state.
     */
    private void actionRefreshDocument(AjaxRequestTarget aTarget, JCas aJCas)
    {
        annotator.bratRenderLater(aTarget);
        gotoPageTextField.setModelObject(getModelObject().getFirstVisibleSentenceNumber());
        updateSentenceAddress(aJCas, aTarget);
    }

    private void handleException(AjaxRequestTarget aTarget, Exception aException)
    {
        LOG.error("Error: " + aException.getMessage(), aException);
        error("Error: " + aException.getMessage());
        if (aTarget != null) {
            aTarget.addChildren(getPage(), FeedbackPanel.class);
        }
    }
}

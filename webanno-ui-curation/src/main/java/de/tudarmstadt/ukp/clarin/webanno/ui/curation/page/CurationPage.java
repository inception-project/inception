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

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PAGE_PARAM_DOCUMENT_ID;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PAGE_PARAM_FOCUS;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PAGE_PARAM_PROJECT_ID;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateUtils.updateDocumentTimestampAfterWrite;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.FocusPosition.TOP;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition.ANNOTATION_IN_PROGRESS_TO_CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition.CURATION_FINISHED_TO_CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition.CURATION_IN_PROGRESS_TO_CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.NoResultException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.CorrectionDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.SessionMetaData;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.SentenceOrientedPagingStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences.BratProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.event.RenderAnnotationsEvent;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ChallengeResponseDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ConfirmationDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.ActionBarLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.DecoratedObject;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.DocumentNamePanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog.AnnotationPreferencesDialog;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog.ExportDocumentDialog;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog.GuidelinesDialog;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog.OpenDocumentDialog;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.CurationPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.CurationContainer;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.SuggestionBuilder;
import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;

/**
 * This is the main class for the curation page. It contains an interface which displays differences
 * between user annotations for a specific document. The interface provides a tool for merging these
 * annotations and storing them as a new annotation.
 */
@MountPath("/curation.html")
public class CurationPage
    extends AnnotationPageBase
{
    private final static Logger LOG = LoggerFactory.getLogger(CurationPage.class);

    private static final long serialVersionUID = 1378872465851908515L;

    private @SpringBean CasStorageService casStorageService;
    private @SpringBean DocumentService documentService;
    private @SpringBean CorrectionDocumentService correctionDocumentService;
    private @SpringBean CurationDocumentService curationDocumentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean ConstraintsService constraintsService;
    private @SpringBean BratProperties defaultPreferences;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean UserDao userRepository;

    private DocumentNamePanel documentNamePanel;
    
    private long currentprojectId;

    // Open the dialog window on first load
    private boolean firstLoad = true;

    private ModalWindow openDocumentsModal;
    private AnnotationPreferencesDialog preferencesModal;
    private ExportDocumentDialog exportDialog;
    private GuidelinesDialog guidelinesDialog;

    private CurationContainer curationContainer;

    private CurationPanel curationPanel;
    private ChallengeResponseDialog remergeDocumentDialog;
    private ActionBarLink remergeDocumentLink;

    private WebMarkupContainer finishDocumentIcon;
    private ConfirmationDialog finishDocumentDialog;
    private LambdaAjaxLink finishDocumentLink;
    
    public CurationPage()
    {
        super();
        LOG.debug("Setting up curation page without parameters");
        commonInit();
        
        Map<String, StringValue> fragmentParameters = Session.get()
                .getMetaData(SessionMetaData.LOGIN_URL_FRAGMENT_PARAMS);
        if (fragmentParameters != null) {
            // Clear the URL fragment parameters - we only use them once!
            Session.get().setMetaData(SessionMetaData.LOGIN_URL_FRAGMENT_PARAMS, null);
            
            StringValue project = fragmentParameters.get(PAGE_PARAM_PROJECT_ID);
            StringValue document = fragmentParameters.get(PAGE_PARAM_DOCUMENT_ID);
            StringValue focus = fragmentParameters.get(PAGE_PARAM_FOCUS);
            
            handleParameters(null, project, document, focus, false);
        }
    }
    
    public CurationPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);
        LOG.debug("Setting up curation page with parameters: {}", aPageParameters);
        
        commonInit();

        StringValue project = aPageParameters.get(PAGE_PARAM_PROJECT_ID);
        StringValue document = aPageParameters.get(PAGE_PARAM_DOCUMENT_ID);
        StringValue focus = aPageParameters.get(PAGE_PARAM_FOCUS);
        
        handleParameters(null, project, document, focus, true);
    }
    
    private void commonInit()
    {
        setModel(Model.of(new AnnotatorStateImpl(Mode.CURATION)));
        
        getModelObject().setPagingStrategy(new SentenceOrientedPagingStrategy());
        add(getModelObject().getPagingStrategy().createPageNavigator("pageNavigator", this));
        add(getModelObject().getPagingStrategy().createPositionLabel("numberOfPages", getModel())
                .add(visibleWhen(() -> getModelObject().getDocument() != null))
                .add(LambdaBehavior.onEvent(RenderAnnotationsEvent.class, (c, e) -> 
                        e.getRequestHandler().add(c))));
        
        // Ensure that a user is set
        getModelObject().setUser(userRepository.getCurrentUser());

        curationContainer = new CurationContainer();
        curationContainer.setState(getModelObject());

        curationPanel = new CurationPanel("curationPanel", this, new Model<>(
                curationContainer))
        {
            private static final long serialVersionUID = 2175915644696513166L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget)
            {
                try {
                    actionRefreshDocument(aTarget);
                }
                catch (Exception e) {
                    handleException(aTarget, e);
                }
            }
        };
        add(curationPanel);

        add(documentNamePanel = new DocumentNamePanel("documentNamePanel", getModel()));
        documentNamePanel.setOutputMarkupId(true);

        add(openDocumentsModal = new OpenDocumentDialog("openDocumentsModal", getModel(),
                getAllowedProjects())
        {
            private static final long serialVersionUID = 5474030848589262638L;

            @Override
            public void onDocumentSelected(AjaxRequestTarget aTarget)
            {
                AnnotatorState state = getModelObject();
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
                        upgradeCasAndSave(state.getDocument(), username);

                        actionLoadDocument(aTarget);
                        curationPanel.getEditor().loadFeatureEditorModels(aTarget);
                    }
                    catch (Exception e) {
                        LOG.error("Unable to load data", e);
                        error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
                    }
                }
            }
        });        

        add(preferencesModal = new AnnotationPreferencesDialog("preferencesDialog", getModel()));
        preferencesModal.setOnChangeAction(this::actionCompletePreferencesChange);

        add(exportDialog = new ExportDocumentDialog("exportDialog", getModel()));
        
        add(guidelinesDialog = new GuidelinesDialog("guidelinesDialog", getModel()));

        IModel<String> documentNameModel = PropertyModel.of(getModel(), "document.name");
        remergeDocumentDialog = new ChallengeResponseDialog("remergeDocumentDialog",
                new StringResourceModel("RemergeDocumentDialog.title", this),
                new StringResourceModel("RemergeDocumentDialog.text", this).setModel(getModel())
                        .setParameters(documentNameModel),
                documentNameModel);
        remergeDocumentDialog.setConfirmAction(this::actionRemergeDocument);
        add(remergeDocumentDialog);
        remergeDocumentLink = new ActionBarLink("showRemergeDocumentDialog", t -> 
            remergeDocumentDialog.show(t));
        remergeDocumentLink.onConfigure(_this -> {
            AnnotatorState state = CurationPage.this.getModelObject();
            _this.setEnabled(state.getDocument() != null && !state.getDocument().getState()
                    .equals(SourceDocumentState.CURATION_FINISHED));
        });
        add(remergeDocumentLink);

        add(new LambdaAjaxLink("showOpenDocumentModal", this::actionShowOpenDocumentDialog));
        
        add(new LambdaAjaxLink("showPreferencesDialog", this::actionShowPreferencesDialog));

        add(new ActionBarLink("showGuidelinesDialog", guidelinesDialog::show));

        add(new LambdaAjaxLink("showExportDialog", exportDialog::show) {
            private static final long serialVersionUID = -8443987117825945678L;

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
    
    private IModel<List<DecoratedObject<Project>>> getAllowedProjects()
    {
        return new LoadableDetachableModel<List<DecoratedObject<Project>>>()
        {
            private static final long serialVersionUID = -2518743298741342852L;

            @Override
            protected List<DecoratedObject<Project>> load()
            {
                User user = userRepository
                        .get(SecurityContextHolder.getContext().getAuthentication().getName());
                List<DecoratedObject<Project>> allowedProject = new ArrayList<>();
                List<Project> projectsWithFinishedAnnos = projectService
                        .listProjectsWithFinishedAnnos();
                for (Project project : projectService.listProjects()) {
                    if (projectService.isCurator(project, user)) {
                        DecoratedObject<Project> dp = DecoratedObject.of(project);
                        if (projectsWithFinishedAnnos.contains(project)) {
                            dp.setColor("green");
                        }
                        else {
                            dp.setColor("red");
                        }
                        allowedProject.add(dp);
                    }
                }
                return allowedProject;
            }
        };
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

    private void updatePanel(CurationContainer aCurationContainer, AjaxRequestTarget aTarget)
    {
        AnnotatorState state = getModelObject();
        curationPanel.setDefaultModelObject(curationContainer);
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
    public CAS getEditorCas()
        throws IOException
    {
        AnnotatorState state = getModelObject();

        if (state.getDocument() == null) {
            throw new IllegalStateException("Please open a document first!");
        }

        return curationDocumentService.readCurationCas(state.getDocument());
    }
    
    @Override
    public void writeEditorCas(CAS aCas) throws IOException
    {
        AnnotatorState state = getModelObject();
        curationDocumentService.writeCurationCas(aCas, state.getDocument(), true);

        // Update timestamp in state
        Optional<Long> diskTimestamp = curationDocumentService
                .getCurationCasTimestamp(state.getDocument());
        if (diskTimestamp.isPresent()) {
            state.setAnnotationDocumentTimestamp(diskTimestamp.get());
        }
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
        
        CAS mergeCas = null;
        try {
            aTarget.add(getFeedbackPanel());
            mergeCas = curationDocumentService.readCurationCas(state.getDocument());
            
            // The number of visible sentences may have changed - let the state recalculate 
            // the visible sentences 
            state.getPagingStrategy().recalculatePage(state, mergeCas);
            
            curationPanel.updatePanel(aTarget, curationContainer);
            updatePanel(curationContainer, aTarget);
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
            SourceDocument sourceDocument = state.getDocument();

            if (SourceDocumentState.CURATION_FINISHED.equals(sourceDocument.getState())) {
                documentService.transitionSourceDocumentState(sourceDocument,
                        CURATION_FINISHED_TO_CURATION_IN_PROGRESS);
            }
            else {
                documentService.transitionSourceDocumentState(sourceDocument,
                        CURATION_IN_PROGRESS_TO_CURATION_FINISHED);
            }
            
            aCallbackTarget.add(finishDocumentIcon);
            aCallbackTarget.add(finishDocumentLink);
            aCallbackTarget.add(curationPanel.getEditor());
            aCallbackTarget.add(remergeDocumentLink);
        });
        finishDocumentDialog.show(aTarget);
    }

    private void actionRemergeDocument(AjaxRequestTarget aTarget) throws IOException
    {
        AnnotatorState state = CurationPage.this.getModelObject();
        curationDocumentService.removeCurationDocumentContent(state.getDocument(),
                state.getUser().getUsername());
        actionLoadDocument(aTarget);
        info("Re-merge finished!");
        aTarget.add(getFeedbackPanel());
    }

    public void upgradeCasAndSave(SourceDocument aDocument, String aUsername)
        throws IOException
    {
        User user = userRepository.get(aUsername);
        if (documentService.existsAnnotationDocument(aDocument, user)) {
            AnnotationDocument annotationDocument = documentService.getAnnotationDocument(aDocument,
                    user);
            try {
                CAS cas = documentService.readAnnotationCas(annotationDocument);
                annotationService.upgradeCas(cas, annotationDocument);
                documentService.writeAnnotationCas(cas, annotationDocument, false);
            }
            catch (Exception e) {
                // no need to catch, it is acceptable that no curation document
                // exists to be upgraded while there are annotation documents
            }
        }
    }

    @Override
    protected void actionLoadDocument(AjaxRequestTarget aTarget)
    {
        actionLoadDocument(aTarget, 0);
    }
    
    /**
     * Open a document or to a different document. This method should be used only the first time
     * that a document is accessed. It reset the annotator state and upgrades the CAS.
     */
    protected void actionLoadDocument(AjaxRequestTarget aTarget, int aFocus)
    {
        LOG.info("BEGIN LOAD_DOCUMENT_ACTION at focus " + aFocus);
        
        AnnotatorState state = getModelObject();
        
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.get(username);

        state.setUser(user);

        try {
            // Update source document state to CURRATION_INPROGRESS, if it was not
            // ANNOTATION_FINISHED
            if (!SourceDocumentState.CURATION_FINISHED.equals(state.getDocument().getState())) {
                documentService.transitionSourceDocumentState(state.getDocument(),
                        ANNOTATION_IN_PROGRESS_TO_CURATION_IN_PROGRESS);
            }
    
            // Load user preferences
            loadPreferences();
            
            // Re-render whole page as sidebar size preference may have changed
            if (aTarget != null) {
                aTarget.add(CurationPage.this);
            }
    
            List<AnnotationDocument> finishedAnnotationDocuments = new ArrayList<>();
    
            for (AnnotationDocument annotationDocument : documentService
                    .listAnnotationDocuments(state.getDocument())) {
                if (annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)) {
                    finishedAnnotationDocuments.add(annotationDocument);
                }
            }
    
            SuggestionBuilder cb = new SuggestionBuilder(casStorageService, documentService,
                    correctionDocumentService, curationDocumentService, annotationService,
                    userRepository);
            AnnotationDocument randomAnnotationDocument = null;
            if (finishedAnnotationDocuments.size() > 0) {
                randomAnnotationDocument = finishedAnnotationDocuments.get(0);
            }
    
            // upgrade CASes for each user, what if new type is added once the user finished
            // annotation
            for (AnnotationDocument ad : finishedAnnotationDocuments) {
                upgradeCasAndSave(ad.getDocument(), ad.getUser());
            }
            Map<String, CAS> casses = cb.listCassesforCuration(finishedAnnotationDocuments,
                    randomAnnotationDocument, state.getMode());
            CAS mergeCas = cb.getMergeCas(state, state.getDocument(), casses,
                    randomAnnotationDocument, true);
    
            // (Re)initialize brat model after potential creating / upgrading CAS
            state.reset();
            
            // Initialize timestamp in state
            updateDocumentTimestampAfterWrite(state, curationDocumentService
                    .getCurationCasTimestamp(state.getDocument()));
                        
            // Initialize the visible content
            state.moveToUnit(mergeCas, aFocus, TOP);
    
            // if project is changed, reset some project specific settings
            if (currentprojectId != state.getProject().getId()) {
                state.clearRememberedFeatures();
            }
    
            currentprojectId = state.getProject().getId();
    
            SuggestionBuilder builder = new SuggestionBuilder(casStorageService, documentService,
                    correctionDocumentService, curationDocumentService, annotationService,
                    userRepository);
            curationContainer = builder.buildCurationContainer(state);
            curationContainer.setState(state);
            curationPanel.getEditor().reset(aTarget);
            updatePanel(curationContainer, aTarget);
            curationPanel.init(aTarget, curationContainer);
            //curationPanel.updatePanel(aTarget, curationContainer);
            
            // Load constraints
            state.setConstraints(constraintsService.loadConstraints(state.getProject()));
    
            if (aTarget != null) {
                aTarget.add(documentNamePanel);
                aTarget.add(remergeDocumentLink);
                aTarget.add(finishDocumentLink);
            }
        }
        catch (Exception e) {
            handleException(aTarget, e);
        }
    
        LOG.info("END LOAD_DOCUMENT_ACTION");
    }

    @Override
    public void actionRefreshDocument(AjaxRequestTarget aTarget)
    {
        try {
            curationPanel.updatePanel(aTarget, curationContainer);
            updatePanel(curationContainer, aTarget);
        }
        catch (Exception e) {
            handleException(aTarget, e);
        }
    }
    
    private Project getProjectFromParameters(StringValue projectParam)
    {
        Project project = null;
        if (projectParam != null && !projectParam.isEmpty()) {
            long projectId = projectParam.toLong();
            project = projectService.getProject(projectId);
        }
        return project;
    }

    private SourceDocument getDocumentFromParameters(Project aProject, StringValue documentParam)
    {
        SourceDocument document = null;
        if (documentParam != null && !documentParam.isEmpty()) {
            long documentId = documentParam.toLong();
            document = documentService.getSourceDocument(aProject.getId(), documentId);
        }
        return document;
    }
    
    private void handleParameters(AjaxRequestTarget aTarget, StringValue aProjectParameter,
            StringValue aDocumentParameter, StringValue aFocusParameter, boolean aLockIfPreset)
    {
        // Get current project from parameters
        Project project = null;
        try {
            project = getProjectFromParameters(aProjectParameter);
        }
        catch (NoResultException e) {
            error("Project [" + aProjectParameter + "] does not exist");
            return;
        }
        
        // Get current document from parameters
        SourceDocument document = null;
        if (project != null) {
            try {
                document = getDocumentFromParameters(project, aDocumentParameter);
            }
            catch (NoResultException e) {
                error("Document [" + aDocumentParameter + "] does not exist in project ["
                        + project.getId() + "]");
            }
        }
        
        // Get current focus unit from parameters
        int focus = 0;
        if (aFocusParameter != null) {
            focus = aFocusParameter.toInt(0);
        }        
        
        // If there is no change in the current document, then there is nothing to do. Mind
        // that document IDs are globally unique and a change in project does not happen unless
        // there is also a document change.
        if (
                document != null &&
                document.equals(getModelObject().getDocument()) && 
                focus == getModelObject().getFocusUnitIndex()
        ) {
            return;
        }
        
        // Check access to project
        if (project != null
                && !projectService.isCurator(project, getModelObject().getUser())) {
            error("You have no permission to access project [" + project.getId() + "]");
            return;
        }
        
        // Update project in state
        // Mind that this is relevant if the project was specified as a query parameter
        // i.e. not only in the case that it was a URL fragment parameter. 
        if (project != null) {
            getModelObject().setProject(project);
            if (aLockIfPreset) {
                getModelObject().setProjectLocked(true);
            }
        }
        
        if (document != null) {
            // If we arrive here and the document is not null, then we have a change of document
            // or a change of focus (or both)
            if (!document.equals(getModelObject().getDocument())) {
                // do not need to choose document
                openDocumentsModal.setVisible(false);
                getModelObject().setDocument(document, getListOfDocs());
                actionLoadDocument(aTarget, focus);
            }
            else {
                try {
                    getModelObject().moveToUnit(getEditorCas(), focus, TOP);
                    actionRefreshDocument(aTarget);
                }
                catch (Exception e) {
                    aTarget.addChildren(getPage(), IFeedback.class);
                    LOG.info("Error reading CAS " + e.getMessage());
                    error("Error reading CAS " + e.getMessage());
                }
            }
        }
    }
}

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

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PAGE_PARAM_DOCUMENT_ID;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PAGE_PARAM_FOCUS;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PAGE_PARAM_PROJECT_ID;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateUtils.updateDocumentTimestampAfterWrite;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateUtils.verifyAndUpdateDocumentTimestamp;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.FocusPosition.TOP;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition.ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition.NEW_TO_ANNOTATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.persistence.NoResultException;

import org.apache.uima.cas.CAS;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.CssContentHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.annotation.mount.MountPath;
import org.wicketstuff.urlfragment.UrlFragment;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectType;
import de.tudarmstadt.ukp.clarin.webanno.api.SessionMetaData;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorFactory;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.DocumentOpenedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences.BratProperties;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ConfirmationDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.ActionBarLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.DecoratedObject;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.wicketstuff.UrlParametersReceivingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.DocumentNamePanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.FinishImage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.AnnotationDetailEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog.AnnotationPreferencesDialog;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog.ExportDocumentDialog;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog.GuidelinesDialog;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.dialog.OpenDocumentDialog;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.SidebarPanel;
import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;

/**
 * A wicket page for the Brat Annotation/Visualization page. Included components for pagination,
 * annotation layer configuration, and Exporting document
 */
@MountPath(value = "/annotation.html", alt = { "/annotate/${" + PAGE_PARAM_PROJECT_ID + "}",
        "/annotate/${" + PAGE_PARAM_PROJECT_ID + "}/${" + PAGE_PARAM_DOCUMENT_ID + "}" })
@ProjectType(id = WebAnnoConst.PROJECT_TYPE_ANNOTATION, prio = 100)
public class AnnotationPage
    extends AnnotationPageBase
{
    private static final Logger LOG = LoggerFactory.getLogger(AnnotationPage.class);

    private static final long serialVersionUID = 1378872465851908515L;

    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean ConstraintsService constraintsService;
    private @SpringBean BratProperties defaultPreferences;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean UserDao userRepository;
    private @SpringBean AnnotationEditorRegistry editorRegistry;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisherHolder;
    
    private long currentprojectId;

    private boolean initialLoadCompleted = false;

    private OpenDocumentDialog openDocumentsModal;
    private AnnotationPreferencesDialog preferencesModal;
    private ExportDocumentDialog exportDialog;
    private GuidelinesDialog guidelinesDialog;

    private FinishImage finishDocumentIcon;
    private ConfirmationDialog finishDocumentDialog;
    private LambdaAjaxLink finishDocumentLink;
    
    private AnnotationEditorBase annotationEditor;
    private AnnotationDetailEditorPanel detailEditor;    

    public AnnotationPage()
    {
        super();
        LOG.debug("Setting up annotation page without parameters");
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

    public AnnotationPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);
        LOG.debug("Setting up annotation page with parameters: {}", aPageParameters);
        
        commonInit();

        StringValue project = aPageParameters.get(PAGE_PARAM_PROJECT_ID);
        StringValue document = aPageParameters.get(PAGE_PARAM_DOCUMENT_ID);
        StringValue focus = aPageParameters.get(PAGE_PARAM_FOCUS);
        
        handleParameters(null, project, document, focus, true);
    }
    
    private void commonInit()
    {
        setModel(Model.of(new AnnotatorStateImpl(Mode.ANNOTATION)));
        // Ensure that a user is set
        getModelObject().setUser(userRepository.getCurrentUser());

        add(createUrlFragmentBehavior());      
        
        createAnnotationEditor(null);
        
        add(createRightSidebar());

        add(createLeftSidebar());
        
        add(createDocumentInfoLabel());

        add(openDocumentsModal = new OpenDocumentDialog("openDocumentsModal", getModel(),
                getAllowedProjects())
        {
            private static final long serialVersionUID = 5474030848589262638L;

            @Override
            public void onDocumentSelected(AjaxRequestTarget aTarget)
            {
                actionLoadDocument(aTarget);
            }
        });
        
        add(preferencesModal = new AnnotationPreferencesDialog("preferencesDialog", getModel()));
        preferencesModal.setOnChangeAction(this::actionCompletePreferencesChange);
        
        add(exportDialog = new ExportDocumentDialog("exportDialog", getModel()));

        add(guidelinesDialog = new GuidelinesDialog("guidelinesDialog", getModel()));

        add(new LambdaAjaxLink("initialLoadComplete", this::actionInitialLoadComplete));

        add(new LambdaAjaxLink("showOpenDocumentDialog", this::actionShowOpenDocumentDialog));

        add(new ActionBarLink("showPreferencesDialog", this::actionShowPreferencesDialog));
        
        add(new ActionBarLink("showGuidelinesDialog", guidelinesDialog::show));

        add(new ActionBarLink("showExportDialog", exportDialog::show).onConfigure(_this -> {
            AnnotatorState state = AnnotationPage.this.getModelObject();
            _this.setVisible(state.getProject() != null
                    && (projectService.isAdmin(state.getProject(), state.getUser())
                            || !state.getProject().isDisableExport()));
        }));

        add(new ActionBarLink("showPreviousDocument", t -> actionShowPreviousDocument(t))
                .add(new InputBehavior(new KeyType[] { KeyType.Shift, KeyType.Page_up },
                        EventType.click)));

        add(new ActionBarLink("showNextDocument", t -> actionShowNextDocument(t))
                .add(new InputBehavior(new KeyType[] { KeyType.Shift, KeyType.Page_down },
                        EventType.click)));

        add(new ActionBarLink("toggleScriptDirection", this::actionToggleScriptDirection));
        
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
                
                AnnotatorState state = AnnotationPage.this.getModelObject();
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
                        && WebAnnoConst.PROJECT_TYPE_ANNOTATION.equals(project.getMode())) {
                    allowedProject.add(DecoratedObject.of(project));
                }
            }
            return allowedProject;
        });
    }

    private DocumentNamePanel createDocumentInfoLabel()
    {
        return new DocumentNamePanel("documentNamePanel", getModel());
    }

    private AnnotationDetailEditorPanel createDetailEditor()
    {
        return new AnnotationDetailEditorPanel("annotationDetailEditorPanel", this, getModel())
        {
            private static final long serialVersionUID = 2857345299480098279L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget)
            {
                actionRefreshDocument(aTarget);
            }

            @Override
            protected void onAutoForward(AjaxRequestTarget aTarget)
            {
                actionRefreshDocument(aTarget);
            }
            
            @Override
            public CAS getEditorCas() throws IOException
            {
                return AnnotationPage.this.getEditorCas();
            }
        };
    }
    
    private void createAnnotationEditor(IPartialPageRequestHandler aTarget)
    {
        AnnotatorState state = getModelObject();
        
        String editorId = getModelObject().getPreferences().getEditor();
        
        AnnotationEditorFactory factory = editorRegistry.getEditorFactory(editorId);
        if (factory == null) {
            factory = editorRegistry.getDefaultEditorFactory();
        }

        annotationEditor = factory.create("editor", getModel(), detailEditor, this::getEditorCas);
        annotationEditor.add(visibleWhen(() -> state.getDocument() != null));
        annotationEditor.setOutputMarkupPlaceholderTag(true);
        
        addOrReplace(annotationEditor);
        
        // Give the new editor an opportunity to configure the current paging strategy
        factory.initState(state);
        if (state.getDocument() != null) {
            try {
                state.getPagingStrategy().recalculatePage(state, getEditorCas());
            }
            catch (Exception e) {
                LOG.info("Error reading CAS: {}", e.getMessage());
                error("Error reading CAS " + e.getMessage());
                if (aTarget != null) {
                    aTarget.addChildren(getPage(), IFeedback.class);
                }
            }
        }
        
        // Use the proper page navigator and position labels for the current paging strategy
        addOrReplace(state.getPagingStrategy().createPageNavigator("pageNavigator", this));
        addOrReplace(state.getPagingStrategy().createPositionLabel("numberOfPages", getModel())
                .add(visibleWhen(() -> getModelObject().getDocument() != null)));
    }

    private SidebarPanel createLeftSidebar()
    {
        SidebarPanel leftSidebar = new SidebarPanel("leftSidebar", getModel(), detailEditor, () -> 
                getEditorCas(), AnnotationPage.this);
        // Override sidebar width from preferences
        leftSidebar.add(new AttributeModifier("style", LambdaModel.of(() -> String
                .format("flex-basis: %d%%;", getModelObject().getPreferences().getSidebarSize()))));
        return leftSidebar;
    }
    
    private WebMarkupContainer createRightSidebar()
    {
        WebMarkupContainer rightSidebar = new WebMarkupContainer("rightSidebar");
        rightSidebar.setOutputMarkupId(true);
        // Override sidebar width from preferences
        rightSidebar.add(new AttributeModifier("style", LambdaModel.of(() -> String
                .format("flex-basis: %d%%;", getModelObject().getPreferences().getSidebarSize()))));
        detailEditor = createDetailEditor();
        rightSidebar.add(detailEditor);
        return rightSidebar;
    }

    @Override
    protected List<SourceDocument> getListOfDocs()
    {
        AnnotatorState state = getModelObject();
        return new ArrayList<>(documentService
                .listAnnotatableDocuments(state.getProject(), state.getUser()).keySet());
    }

    /**
     * for the first time, open the <b>open document dialog</b>
     */
    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        if (!initialLoadCompleted) {
            aResponse.render(OnLoadHeaderItem
                    .forScript("jQuery('#initialLoadComplete').trigger('click');"));
            initialLoadCompleted = true;
        }
        
        aResponse.render(CssContentHeaderItem.forCSS(
                        String.format(Locale.US, ".sidebarCell { flex-basis: %d%%; }",
                                getModelObject().getPreferences().getSidebarSize()),
                        "sidebar-width"));
    }

    @Override
    public CAS getEditorCas()
        throws IOException
    {
        AnnotatorState state = getModelObject();

        if (state.getDocument() == null) {
            throw new IllegalStateException("Please open a document first!");
        }

        // If we have a timestamp, then use it to detect if there was a concurrent access
        verifyAndUpdateDocumentTimestamp(state, documentService
                .getAnnotationCasTimestamp(state.getDocument(), state.getUser().getUsername()));
        
        return documentService.readAnnotationCas(state.getDocument(),
                state.getUser().getUsername());
    }
    
    @Override
    public void writeEditorCas(CAS aCas) throws IOException
    {
        AnnotatorState state = getModelObject();
        documentService.writeAnnotationCas(aCas, state.getDocument(), state.getUser(), true);

        // Update timestamp in state
        Optional<Long> diskTimestamp = documentService
                .getAnnotationCasTimestamp(state.getDocument(), state.getUser().getUsername());
        if (diskTimestamp.isPresent()) {
            state.setAnnotationDocumentTimestamp(diskTimestamp.get());
        }
    }
    
    private void actionInitialLoadComplete(AjaxRequestTarget aTarget)
    {
        // If the page has loaded and there is no document open yet, show the open-document
        // dialog.
        if (getModelObject().getDocument() == null) {
            actionShowOpenDocumentDialog(aTarget);
        }
        else {
            // Make sure the URL fragement parameters are up-to-date
            updateUrlFragment(aTarget);
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
            throws Exception
    {
        getModelObject().toggleScriptDirection();
        actionRefreshDocument(aTarget);
    }
    
    private void actionCompletePreferencesChange(AjaxRequestTarget aTarget)
    {
        try {
            AnnotatorState state = getModelObject();
            
            CAS cas = getEditorCas();
            
            // The number of visible sentences may have changed - let the state recalculate 
            // the visible sentences 
            state.getPagingStrategy().recalculatePage(state, cas);
            
            // The selection of layers may have changed. Update the dropdown
            detailEditor.getAnnotationFeatureForm().updateLayersDropdown();
            
            createAnnotationEditor(aTarget);
            
            // Reload all AJAX-enabled children of the page but not the page itself!
            WicketUtil.refreshPage(aTarget, getPage());
        }
        catch (Exception e) {
            LOG.info("Error reading CAS: {}", e.getMessage());
            error("Error reading CAS " + e.getMessage());
            aTarget.addChildren(getPage(), IFeedback.class);
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
        actionLoadDocument(aTarget, 0);
    }
    
    protected void actionLoadDocument(AjaxRequestTarget aTarget, int aFocus)
    {
        LOG.trace("BEGIN LOAD_DOCUMENT_ACTION at focus " + aFocus);
        
        AnnotatorState state = getModelObject();
        
        state.setUser(userRepository.getCurrentUser());

        try {
            // Check if there is an annotation document entry in the database. If there is none,
            // create one.
            AnnotationDocument annotationDocument = documentService
                    .createOrGetAnnotationDocument(state.getDocument(), state.getUser());

            // Read the CAS
            CAS editorCas = documentService.readAnnotationCas(annotationDocument);

            // Update the annotation document CAS
            annotationService.upgradeCas(editorCas, annotationDocument);

            // After creating an new CAS or upgrading the CAS, we need to save it
            documentService.writeAnnotationCas(editorCas, annotationDocument, false);

            // (Re)initialize brat model after potential creating / upgrading CAS
            state.reset();
            
            // Initialize timestamp in state
            updateDocumentTimestampAfterWrite(state, documentService
                    .getAnnotationCasTimestamp(state.getDocument(), state.getUser().getUsername()));

            // Load constraints
            state.setConstraints(constraintsService.loadConstraints(state.getProject()));

            // Load user preferences
            loadPreferences();

            // if project is changed, reset some project specific settings
            if (currentprojectId != state.getProject().getId()) {
                state.clearRememberedFeatures();
                currentprojectId = state.getProject().getId();
            }

            // Set the actual editor component. This has to happen *before* any AJAX refreshs are
            // scheduled and *after* the preferences have been loaded (because the current editor
            // type is set in the preferences.
            createAnnotationEditor(aTarget);

            // Initialize the visible content - this has to happen after the annotation editor
            // component has been created because only then the paging strategy is known
            state.moveToUnit(editorCas, aFocus + 1, TOP);

            // Update document state
            if (SourceDocumentState.NEW.equals(state.getDocument().getState())) {
                documentService.transitionSourceDocumentState(state.getDocument(),
                        NEW_TO_ANNOTATION_IN_PROGRESS);
            }
            
            // Reset the editor (we reload the page content below, so in order not to schedule
            // a double-update, we pass null here)
            detailEditor.reset(null);
            // Populate the layer dropdown box
            detailEditor.loadFeatureEditorModels(editorCas, null);
            
            if (aTarget != null) {
                // Update URL for current document
                updateUrlFragment(aTarget);
                WicketUtil.refreshPage(aTarget, getPage());
            }
            
            applicationEventPublisherHolder.get().publishEvent(
                    new DocumentOpenedEvent(this, editorCas, getModelObject().getDocument(),
                            getModelObject().getUser().getUsername()));
        }
        catch (Exception e) {
            handleException(aTarget, e);
        }

        LOG.trace("END LOAD_DOCUMENT_ACTION");
    }
    
    @Override
    public void actionRefreshDocument(AjaxRequestTarget aTarget)
    {
        try {
            annotationEditor.requestRender(aTarget);
        }
        catch (Exception e) {
            LOG.warn("Editor refresh requested at illegal time, forcing page refresh",
                    new RuntimeException());
            throw new RestartResponseException(getPage());
        }
        
        aTarget.addChildren(getPage(), IFeedback.class);
        
        // Update URL for current document
        updateUrlFragment(aTarget);
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
    
    private UrlParametersReceivingBehavior createUrlFragmentBehavior()
    {
        return new UrlParametersReceivingBehavior()
        {
            private static final long serialVersionUID = -3860933016636718816L;

            @Override
            protected void onParameterArrival(IRequestParameters aRequestParameters,
                    AjaxRequestTarget aTarget)
            {
                aTarget.addChildren(getPage(), IFeedback.class);

                StringValue project = aRequestParameters.getParameterValue(PAGE_PARAM_PROJECT_ID);
                StringValue document = aRequestParameters.getParameterValue(PAGE_PARAM_DOCUMENT_ID);
                StringValue focus = aRequestParameters.getParameterValue(PAGE_PARAM_FOCUS);

                handleParameters(aTarget, project, document, focus, false);
            }
        };
    }
    
    private void updateUrlFragment(AjaxRequestTarget aTarget)
    {
        if (aTarget != null) {
            AnnotatorState state = getModelObject();
            UrlFragment fragment = new UrlFragment(aTarget);

            // Current project
            fragment.putParameter(PAGE_PARAM_PROJECT_ID, state.getDocument().getProject().getId());

            // Current document
            fragment.putParameter(PAGE_PARAM_DOCUMENT_ID, state.getDocument().getId());

            // Current focus unit
            if (state.getFocusUnitIndex() > 0) {
                fragment.putParameter(PAGE_PARAM_FOCUS, state.getFocusUnitIndex());
            }
            else {
                fragment.removeParameter(PAGE_PARAM_FOCUS);
            }

            // If we do not manually set editedFragment to false, then changing the URL
            // manually or using the back/forward buttons in the browser only works every
            // second time. Might be a but in wicketstuff urlfragment... not sure.
            aTarget.appendJavaScript(
                    "try{if(window.UrlUtil){window.UrlUtil.editedFragment = false;}}catch(e){}");
        }
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
                && !projectService.isAnnotator(project, getModelObject().getUser())) {
            error("You have no permission to access project [" + project.getId() + "]");
            return;
        }
        
        // Check if document is locked for the user
        if (project != null && document != null && documentService
                .existsAnnotationDocument(document, getModelObject().getUser())) {
            AnnotationDocument adoc = documentService.getAnnotationDocument(document,
                    getModelObject().getUser());
            if (AnnotationDocumentState.IGNORE.equals(adoc.getState())) {
                error("Document [" + document.getId() + "] in project [" + project.getId()
                        + "] is locked for user [" + getModelObject().getUser().getUsername()
                        + "]");
                return;
            }
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

/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationEditorManagerPrefs.KEY_ANNOTATION_EDITOR_MANAGER_PREFS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.FORCE_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.NO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IGNORE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateChangeFlag.EXPLICIT_ANNOTATOR_USER_ACTION;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition.ANNOTATION_IN_PROGRESS_TO_CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition.NEW_TO_ANNOTATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.inception.rendering.selection.FocusPosition.CENTERED;
import static de.tudarmstadt.ukp.inception.rendering.selection.FocusPosition.TOP;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBar;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.NoPagingStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences.UserPreferencesService;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateChangeFlag;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.DocumentNamePanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.AnnotationDetailEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.SidebarPanel;
import de.tudarmstadt.ukp.inception.annotation.events.AnnotationEvent;
import de.tudarmstadt.ukp.inception.annotation.events.BeforeDocumentOpenedEvent;
import de.tudarmstadt.ukp.inception.annotation.events.DocumentOpenedEvent;
import de.tudarmstadt.ukp.inception.annotation.events.FeatureValueUpdatedEvent;
import de.tudarmstadt.ukp.inception.annotation.events.PreparingToOpenDocumentEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.documents.api.DocumentAccess;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorBase;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorRegistry;
import de.tudarmstadt.ukp.inception.editor.ContextMenuLookup;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.editor.state.AnnotatorStateImpl;
import de.tudarmstadt.ukp.inception.log.EventRepository;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationPreference;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.selection.AnnotatorViewportChangedEvent;
import de.tudarmstadt.ukp.inception.rendering.selection.SelectionChangedEvent;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.inception.support.wicket.DecoratedObject;
import de.tudarmstadt.ukp.inception.support.wicket.WicketUtil;

public abstract class AnnotationPageBase2
    extends AnnotationPageBase
{
    private static final long serialVersionUID = 1378872465851908515L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String MID_EDITOR = "editor";
    private static final String MID_NUMBER_OF_PAGES = "numberOfPages";

    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean ConstraintsService constraintsService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean UserPreferencesService userPreferenceService;
    private @SpringBean UserDao userRepository;
    private @SpringBean AnnotationEditorRegistry editorRegistry;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisherHolder;
    private @SpringBean PreferencesService preferencesService;
    private @SpringBean DocumentAccess documentAccess;
    private @SpringBean EventRepository eventRepository;

    private long currentProjectId;

    private WebMarkupContainer centerArea;
    private ActionBar actionBar;
    private AnnotationEditorBase annotationEditor;
    private AnnotationDetailEditorPanel detailEditor;
    private SidebarPanel leftSidebar;
    private boolean pageReloaded = false;

    public AnnotationPageBase2(final PageParameters aPageParameters)
    {
        super(aPageParameters);

        var state = new AnnotatorStateImpl(Mode.ANNOTATION);
        state.setUser(userRepository.getCurrentUser());
        setModel(Model.of(state));

        // AnnotationPageBase will push the document and user parameters into the URL fragment so
        // we can afterwards navigate between documents freely. When AnnotationPageBase
        // does that, it restarts the request. So basically when we get here, PAGE_PARAM_DOCUMENT
        // will always be `null`... PAGE_PARAM_DATA_OWNER may be non-null if it is set without a
        // document being specified - but in that case it is pretty useless
        //
        // The actual loading of the documents will be handled by onParameterArrival in the
        // UrlFragmentBehavior which will call handleParameters again, this time with the right
        // information.
        var document = aPageParameters.get(PAGE_PARAM_DOCUMENT);
        var focus = aPageParameters.get(PAGE_PARAM_FOCUS);
        var user = aPageParameters.get(PAGE_PARAM_DATA_OWNER);

        handleParameters(document, focus, user);

        createChildComponents();

        updateDocumentView(null, null, null, focus);
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);
        pageReloaded = true;
    }

    private void createChildComponents()
    {
        add(createUrlFragmentBehavior());

        centerArea = new WebMarkupContainer("centerArea");
        centerArea.add(visibleWhen(() -> getModelObject().getDocument() != null));
        centerArea.setOutputMarkupPlaceholderTag(true);
        centerArea.add(createDocumentInfoLabel());
        add(centerArea);

        actionBar = new ActionBar("actionBar");
        centerArea.add(actionBar);

        add(createRightSidebar());

        createAnnotationEditor();

        leftSidebar = createLeftSidebar();
        add(leftSidebar);
    }

    @Override
    public IModel<List<DecoratedObject<Project>>> getAllowedProjects()
    {
        return LoadableDetachableModel.of(() -> {
            var user = userRepository.getCurrentUser();
            var allowedProjects = new ArrayList<DecoratedObject<Project>>();
            for (var project : projectService.listProjectsWithUserHavingRole(user, ANNOTATOR)) {
                allowedProjects.add(DecoratedObject.of(project));
            }
            return allowedProjects;
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
            public CAS getEditorCas() throws IOException
            {
                return AnnotationPageBase2.this.getEditorCas();
            }

            @Override
            public void writeEditorCas() throws IOException, AnnotationException
            {
                AnnotationPageBase2.this.writeEditorCas(getEditorCas());
            }
        };
    }

    /**
     * Re-render the document when the selection has changed. This is necessary in order to update
     * the selection highlight in the annotation editor.
     */
    @SuppressWarnings("javadoc")
    @OnEvent
    public void onSelectionChangedEvent(SelectionChangedEvent aEvent)
    {
        actionRefreshDocument(aEvent.getRequestHandler());
    }

    /**
     * Re-render the document when an annotation has been created or deleted (assuming that this
     * might have triggered a change in some feature that might be shown on screen.
     * <p>
     * NOTE: Considering that this is a backend event, we check here if it even applies to the
     * current view. It might be more efficient to have another event that more closely mimics
     * {@code AnnotationDetailEditorPanel.onChange()}.
     */
    @SuppressWarnings("javadoc")
    @OnEvent
    public void onAnnotationEvent(AnnotationEvent aEvent)
    {
        var state = getModelObject();

        if (!Objects.equals(state.getProject(), aEvent.getProject())
                || !Objects.equals(state.getDocument(), aEvent.getDocument())
                || !Objects.equals(state.getUser().getUsername(), aEvent.getDocumentOwner())) {
            return;
        }

        aEvent.getRequestTarget().ifPresent(this::actionRefreshDocument);
    }

    /**
     * Re-render the document when a feature value has changed (assuming that this might have
     * triggered a change in some feature that might be shown on screen.
     * <p>
     * NOTE: Considering that this is a backend event, we check here if it even applies to the
     * current view. It might be more efficient to have another event that more closely mimics
     * {@code AnnotationDetailEditorPanel.onChange()}.
     */
    @SuppressWarnings("javadoc")
    @OnEvent
    public void onFeatureValueUpdatedEvent(FeatureValueUpdatedEvent aEvent)
    {
        var state = getModelObject();

        if (!Objects.equals(state.getProject(), aEvent.getProject())
                || !Objects.equals(state.getDocument(), aEvent.getDocument())
                || !Objects.equals(state.getUser().getUsername(), aEvent.getDocumentOwner())) {
            return;
        }

        actionRefreshDocument(aEvent.getRequestTarget().orElse(null));
    }

    /**
     * Re-render the document when the view has changed, e.g. due to paging
     */
    @SuppressWarnings("javadoc")
    @OnEvent
    public void onViewStateChanged(AnnotatorViewportChangedEvent aEvent)
    {
        // Partial page updates only need to be triggered if we are in a partial page update request
        if (aEvent.getRequestHandler() == null) {
            return;
        }

        try {
            aEvent.getRequestHandler().add(centerArea.get(MID_NUMBER_OF_PAGES));
        }
        catch (IllegalStateException e) {
            // Ignore IllegalStateException if rendering of page has already progress so far that
            // no new components can be added. We hope the caller will know what they are doing
            // when they invoke this method so late in the render cycle and trigger a page-reload
            // themselves.
        }

        actionRefreshDocument(aEvent.getRequestHandler());
    }

    private void createAnnotationEditor()
    {
        var state = getModelObject();

        if (state.getDocument() == null) {
            centerArea.addOrReplace(new EmptyPanel(MID_EDITOR).setOutputMarkupId(true));
            state.setPagingStrategy(new NoPagingStrategy());
            centerArea.addOrReplace(
                    state.getPagingStrategy().createPositionLabel(MID_NUMBER_OF_PAGES, getModel()));
            return;
        }

        var editorState = preferencesService
                .loadDefaultTraitsForProject(KEY_ANNOTATION_EDITOR_MANAGER_PREFS, getProject());

        var editorId = editorState.getDefaultEditor();

        if (editorId == null) {
            editorId = getModelObject().getPreferences().getEditor();
        }

        var factory = editorRegistry.getEditorFactory(editorId);
        if (factory == null) {
            if (state.getDocument() != null) {
                factory = editorRegistry.getPreferredEditorFactory(state.getProject(),
                        state.getDocument().getFormat());
            }
            else {
                factory = editorRegistry.getDefaultEditorFactory();
            }
        }

        state.setEditorFactoryId(factory.getBeanName());
        annotationEditor = factory.create(MID_EDITOR, getModel(), detailEditor, this::getEditorCas);
        annotationEditor.setOutputMarkupPlaceholderTag(true);

        centerArea.addOrReplace(annotationEditor);

        // Give the new editor an opportunity to configure the current paging strategy, this does
        // not configure the paging for a document yet this would require loading the CAS which
        // might not have been upgraded yet
        factory.initState(state);
        // Use the proper position labels for the current paging strategy
        centerArea.addOrReplace(
                state.getPagingStrategy().createPositionLabel(MID_NUMBER_OF_PAGES, getModel())
                        .add(visibleWhen(() -> getModelObject().getDocument() != null)));
    }

    private SidebarPanel createLeftSidebar()
    {
        return new SidebarPanel("leftSidebar",
                getModel().map(AnnotatorState::getPreferences)
                        .map(AnnotationPreference::getSidebarSizeLeft),
                detailEditor, () -> getEditorCas(), AnnotationPageBase2.this);
    }

    private WebMarkupContainer createRightSidebar()
    {
        var rightSidebar = new WebMarkupContainer("rightSidebar");
        rightSidebar.setOutputMarkupPlaceholderTag(true);
        // Override sidebar width from preferences
        rightSidebar.add(new AttributeModifier("style",
                LoadableDetachableModel.of(() -> String.format("flex-basis: %d%%;",
                        getModelObject().getPreferences().getSidebarSizeRight()))));
        detailEditor = createDetailEditor();
        rightSidebar.add(detailEditor);
        rightSidebar.add(visibleWhen(getModel() //
                .map(AnnotatorState::getAnnotationLayers) //
                .map(layers -> layers.stream()
                        .anyMatch(l -> SpanLayerSupport.TYPE.equals(l.getType())))));
        return rightSidebar;
    }

    @Override
    public List<SourceDocument> getListOfDocs()
    {
        var state = getModelObject();
        return new ArrayList<>(documentService
                .listAnnotatableDocuments(state.getProject(), state.getUser()).keySet());
    }

    @Override
    public CAS getEditorCas() throws IOException
    {
        var state = getModelObject();

        if (state.getDocument() == null) {
            throw new IllegalStateException("Please open a document first!");
        }

        // If we have a timestamp, then use it to detect if there was a concurrent access
        if (isEditable() && state.getAnnotationDocumentTimestamp().isPresent()) {
            documentService
                    .verifyAnnotationCasTimestamp(state.getDocument(),
                            AnnotationSet.forUser(state.getUser()),
                            state.getAnnotationDocumentTimestamp().get(), "reading the editor CAS")
                    .ifPresent(state::setAnnotationDocumentTimestamp);
        }

        return documentService.readAnnotationCas(state.getDocument(),
                AnnotationSet.forUser(state.getUser()));
    }

    @Override
    public void writeEditorCas(CAS aCas) throws IOException, AnnotationException
    {
        ensureIsEditable();
        var state = getModelObject();
        documentService.writeAnnotationCas(aCas, state.getDocument(), state.getUser(),
                EXPLICIT_ANNOTATOR_USER_ACTION);

        bumpAnnotationCasTimestamp(state);
    }

    public void bumpAnnotationCasTimestamp(AnnotatorState aState) throws IOException
    {
        documentService
                .getAnnotationCasTimestamp(aState.getDocument(),
                        AnnotationSet.forUser(aState.getUser()))
                .ifPresent(aState::setAnnotationDocumentTimestamp);
    }

    @Override
    public AnnotationActionHandler getAnnotationActionHandler()
    {
        return detailEditor;
    }

    @Override
    public void actionLoadDocument(AjaxRequestTarget aTarget)
    {
        actionLoadDocument(aTarget, 0);
    }

    protected void actionLoadDocument(AjaxRequestTarget aTarget, int aFocus)
    {
        try {
            var sessionOwner = userRepository.getCurrentUser();
            var sessionOwnerName = sessionOwner.getUsername();

            var state = getModelObject();
            if (state.getUser() == null) {
                state.setUser(sessionOwner);
            }

            state.refreshProject(projectService);
            state.refreshDocument(documentService);

            LOG.trace("Preparing to open document {}@{}", state.getUser(), state.getDocument());

            // Load constraints
            state.setConstraints(constraintsService.getMergedConstraints(state.getProject()));

            // Load user preferences
            loadPreferences();

            // Set the actual editor component. This has to happen *before* any AJAX refreshes are
            // scheduled and *after* the preferences have been loaded (because the current editor
            // type is set in the preferences.
            createAnnotationEditor();

            state.reset();

            applicationEventPublisherHolder.get().publishEvent(
                    new PreparingToOpenDocumentEvent(this, getModelObject().getDocument(),
                            getModelObject().getUser().getUsername(), sessionOwnerName));

            // INFO BOUNDARY ---------------------------------------------------------------
            // PreparingToOpenDocumentEvent has the option to change the annotator state.
            // No information from the annotator state read before this point may be
            // used afterwards. Information has to be re-read from the annotator state to get
            // the latest values.

            // Check if there is an annotation document entry in the database. If there is none,
            // create one.
            LOG.trace("Opening document {}@{}", state.getUser(), state.getDocument());
            var annotationDocument = documentService
                    .createOrGetAnnotationDocument(state.getDocument(), state.getUser());
            var stateBeforeOpening = annotationDocument.getState();

            var editable = isEditable();

            // Read the CAS
            // Update the annotation document CAS
            var editorCas = documentService.readAnnotationCas(annotationDocument,
                    editable ? FORCE_CAS_UPGRADE : NO_CAS_UPGRADE);

            var dataOwnerName = getModelObject().getUser().getUsername();
            applicationEventPublisherHolder.get()
                    .publishEvent(new BeforeDocumentOpenedEvent(this, editorCas,
                            getModelObject().getDocument(), dataOwnerName, sessionOwnerName,
                            editable));

            if (editable) {
                // After creating an new CAS or upgrading the CAS, we need to save it. If the
                // document is accessed for the first time and thus will transition from NEW to
                // IN_PROGRESS, then we use this opportunity also to set the timestamp of the
                // annotation document - this ensures that e.g. the dynamic workflow considers the
                // document to be "active" for the given user so that it won't be considered as
                // abandoned immediately after having been opened for the first time.
                // We suppress the AfterCasWrittenEvent here - handlers should react to
                // DocumentOpenedEvent instead.
                var flags = AnnotationDocumentState.NEW == annotationDocument.getState()
                        ? new AnnotationDocumentStateChangeFlag[] { EXPLICIT_ANNOTATOR_USER_ACTION }
                        : new AnnotationDocumentStateChangeFlag[] {};
                documentService.writeAnnotationCasSilently(editorCas, annotationDocument, flags);

                bumpAnnotationCasTimestamp(state);
            }

            // if project is changed, reset some project specific settings
            if (currentProjectId != state.getProject().getId()) {
                state.clearRememberedFeatures();
                currentProjectId = state.getProject().getId();
            }

            actionBar.refresh();

            // update paging, only do it during document load so we load the CAS after it has been
            // upgraded
            state.getPagingStrategy().recalculatePage(state, editorCas);

            // Initialize the visible content - this has to happen after the annotation editor
            // component has been created because only then the paging strategy is known
            if (aFocus > 0) {
                state.moveToUnit(editorCas, aFocus, CENTERED);
            }
            else if (dataOwnerName.equals(sessionOwnerName)
                    || dataOwnerName.equals(CURATION_USER)) {
                eventRepository.getLastEditRange(state.getDocument(), dataOwnerName).ifPresent(
                        range -> state.moveToOffset(editorCas, range.getBegin(), CENTERED));
            }
            else {
                state.moveToUnit(editorCas, 0, TOP);
            }

            // Update document state
            if (isEditable()) {
                if (SourceDocumentState.NEW == state.getDocument().getState()) {
                    documentService.transitionSourceDocumentState(state.getDocument(),
                            NEW_TO_ANNOTATION_IN_PROGRESS);
                }

                // We maintain an AnnotationDocument for the `CURATION_USER` now
                if (AnnotationDocumentState.NEW == annotationDocument.getState()) {
                    documentService.setAnnotationDocumentState(annotationDocument,
                            AnnotationDocumentState.IN_PROGRESS, EXPLICIT_ANNOTATOR_USER_ACTION);
                }

                // We also use the SourceDocumentState to indicate the curation status
                if (state.getUser().getUsername().equals(CURATION_USER)) {
                    var sourceDoc = state.getDocument();
                    var sourceDocState = sourceDoc.getState();
                    if (sourceDocState != CURATION_IN_PROGRESS
                            && sourceDocState != CURATION_FINISHED) {
                        documentService.transitionSourceDocumentState(sourceDoc,
                                ANNOTATION_IN_PROGRESS_TO_CURATION_IN_PROGRESS);
                    }
                }
            }

            // Reset the editor (we reload the page content below, so in order not to schedule
            // a double-update, we pass null here)
            detailEditor.reset(null);

            if (aTarget != null) {
                // Update URL for current document
                updateUrlFragment(aTarget);
                WicketUtil.refreshPage(aTarget, getPage());
            }

            LOG.trace("Document opened {}@{}", state.getUser(), state.getDocument());

            applicationEventPublisherHolder.get()
                    .publishEvent(new DocumentOpenedEvent(this, editorCas,
                            getModelObject().getDocument(), stateBeforeOpening,
                            getModelObject().getUser().getUsername(), sessionOwnerName));
        }
        catch (Exception e) {
            handleException(aTarget, e);
        }
    }

    @Override
    public void actionRefreshDocument(AjaxRequestTarget aTarget)
    {
        // Partial page updates only need to be triggered if we are in a partial page update request
        if (aTarget == null) {
            return;
        }

        if (annotationEditor != null) {
            try {
                annotationEditor.requestRender(aTarget);
            }
            catch (Exception e) {
                LOG.warn("Unable to refresh annotation editor, forcing page refresh", e);
                throw new RestartResponseException(getPage());
            }
        }

        updateUrlFragment(aTarget);
    }

    @Override
    protected void handleParameters(StringValue aDocumentParameter, StringValue aFocusParameter,
            StringValue aUserParameter)
    {
        var sessionOwner = userRepository.getCurrentUser();
        requireAnyProjectRole(sessionOwner);

        var state = getModelObject();
        var project = getProject();
        var doc = getDocumentFromParameters(project, aDocumentParameter);

        // If there is no change in the current document, then there is nothing to do. Mind
        // that document IDs are globally unique and a change in project does not happen unless
        // there is also a document change.
        var dataOwner = state.getUser().getUsername();
        if (doc != null && //
                doc.equals(state.getDocument()) && //
                aFocusParameter.toInt(0) == state.getFocusUnitIndex() && //
                dataOwner.equals(aUserParameter.toString()) //
        ) {
            LOG.trace("Page parameters match page state ({}@{} {}) - nothing to do", dataOwner,
                    state.getDocument(), state.getFocusUnitIndex());
            return;
        }

        state.setProject(project);

        if (!aUserParameter.isEmpty()
                && !state.getUser().getUsername().equals(aUserParameter.toString())) {
            // REC: We currently do not want that one can switch to the CURATION_USER directly via
            // the URL without having to activate sidebar curation mode as well, so we do not handle
            // the CURATION_USER here.
            // if (CURATION_USER.equals(aUserParameter.toString())) {
            // state.setUser(new User(CURATION_USER));
            // }
            // else {
            var requestedUser = userRepository.get(aUserParameter.toString());
            if (requestedUser == null) {
                failWithDocumentNotFound("User not found [" + aUserParameter + "]");
                return;
            }
            else {
                LOG.trace("Changing data owner: {}", requestedUser);
                state.setUser(requestedUser);
            }
            // }
        }

        if (doc != null && !documentAccess.canViewAnnotationDocument(sessionOwner.getUsername(),
                String.valueOf(project.getId()), doc.getId(), state.getUser().getUsername())) {
            failWithDocumentNotFound("Access to document [" + aDocumentParameter + "] in project ["
                    + project.getName() + "] is denied");
            return;
        }

        // If we arrive here and the document is not null, then we have a change of document
        // or a change of focus (or both)
        if (doc != null && !doc.equals(state.getDocument())) {
            LOG.trace("Changing document: {} (prev: {})", doc, state.getDocument());
            state.setDocument(doc, getListOfDocs());
        }
    }

    @Override
    protected void updateDocumentView(AjaxRequestTarget aTarget, SourceDocument aPreviousDocument,
            User aPreviousDataOwner, StringValue aFocusParameter)
    {
        var originalPageReloaded = pageReloaded;
        pageReloaded = false;

        // URL is from external link, not just paging through documents, tabs may have changed
        // depending on user rights
        if (aTarget != null && aPreviousDocument == null) {
            LOG.trace(
                    "Refreshing left sidebar as this is the first document loaded on this page instance");
            leftSidebar.refreshTabs(aTarget);
        }

        var state = getModelObject();
        var currentDocument = state.getDocument();
        var dataOwner = state.getUser();
        if (currentDocument == null || dataOwner == null) {
            LOG.trace("No document open");
            return;
        }

        // If we arrive here and the document is not null, then we have a change of document
        // or a change of focus (or both)

        // Get current focus unit from parameters
        var focus = 0;
        if (aFocusParameter != null) {
            focus = aFocusParameter.toInt(0);
        }

        // If there is no change in the current document, then there is nothing to do. Mind
        // that document IDs are globally unique and a change in project does not happen unless
        // there is also a document change.
        if (aPreviousDocument != null && aPreviousDocument.equals(currentDocument) && //
                aPreviousDataOwner != null && aPreviousDataOwner.equals(dataOwner) && //
                focus == state.getFocusUnitIndex() //
        ) {
            LOG.trace("Document and data owner have not changed: {}@{}", dataOwner,
                    currentDocument);

            if (originalPageReloaded) {
                if (state.getDocument() != null) {
                    var sessionOwnerName = userRepository.getCurrentUsername();
                    var dataOwnerName = state.getUser().getUsername();
                    if (dataOwnerName.equals(sessionOwnerName)
                            || dataOwnerName.equals(CURATION_USER)) {
                        try {
                            var editorCas = getEditorCas();
                            eventRepository.getLastEditRange(state.getDocument(), dataOwnerName)
                                    .ifPresent(range -> state.moveToOffset(editorCas,
                                            range.getBegin(), CENTERED));
                        }
                        catch (Exception e) {
                            LOG.error("Error reading CAS of document {} for user {}",
                                    state.getDocument(), state.getUser(), e);
                            error("Error reading CAS " + e.getMessage());
                        }
                    }
                }
            }

            return;
        }

        // Never had set a document or is a new one
        if (aPreviousDocument == null || !aPreviousDocument.equals(currentDocument)
                || aPreviousDataOwner == null || !aPreviousDataOwner.equals(dataOwner)) {
            LOG.trace(
                    "Document or data owner have changed (old: {}@{}, new: {}@{}) - loading document",
                    aPreviousDataOwner, aPreviousDocument, dataOwner, currentDocument);
            actionLoadDocument(aTarget, focus);
            return;
        }

        // No change of document, just change of focus
        try {
            var cas = getEditorCas();
            state.moveToUnit(cas, focus, TOP);

            actionRefreshDocument(aTarget);
        }
        catch (Exception e) {
            if (aTarget != null) {
                aTarget.addChildren(getPage(), IFeedback.class);
            }
            LOG.error("Error reading CAS of document {} for user {}", state.getDocument(),
                    state.getUser(), e);
            error("Error reading CAS " + e.getMessage());
        }
    }

    @Override
    protected void loadPreferences() throws BeansException, IOException
    {
        var state = getModelObject();

        if (state.isUserViewingOthersWork(userRepository.getCurrentUsername())
                || CURATION_USER.equals(state.getUser().getUsername())) {
            userPreferenceService.loadPreferences(state,
                    userRepository.getCurrentUser().getUsername());
        }
        else {
            super.loadPreferences();
        }
    }

    public List<AnnotationDocument> listAccessibleDocuments(Project aProject, User aUser)
    {
        var allDocuments = new ArrayList<AnnotationDocument>();
        var docs = documentService.listAllDocuments(aProject, AnnotationSet.forUser(aUser));

        var sessionOwner = userRepository.getCurrentUser();
        for (var e : docs.entrySet()) {
            var sd = e.getKey();
            var ad = e.getValue();
            if (ad != null) {
                // if current user is opening her own docs, don't let her see locked ones
                var userIsSelected = aUser.equals(sessionOwner);
                if (userIsSelected && ad.getState() == IGNORE) {
                    continue;
                }
            }
            else {
                ad = new AnnotationDocument(sessionOwner.getUsername(), sd);
            }

            allDocuments.add(ad);
        }

        return allDocuments;
    }

    @Override
    public Optional<ContextMenuLookup> getContextMenuLookup()
    {
        return annotationEditor.getContextMenuLookup();
    }
}

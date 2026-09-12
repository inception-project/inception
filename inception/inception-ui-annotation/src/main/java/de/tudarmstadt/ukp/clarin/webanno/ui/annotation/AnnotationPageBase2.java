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

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.FORCE_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.NO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateChangeFlag.EXPLICIT_ANNOTATOR_USER_ACTION;
import static de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.SidebarStateChangedEvent.Side.LEFT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.SidebarStateChangedEvent.Side.RIGHT;
import static de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationPageLayoutState.KEY_LAYOUT_STATE;
import static de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationPreference.SIDEBAR_SIZE_DEFAULT;
import static de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationPreference.SIDEBAR_SIZE_MAX;
import static de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationPreference.SIDEBAR_SIZE_MIN;
import static de.tudarmstadt.ukp.inception.rendering.selection.FocusPosition.CENTERED;
import static de.tudarmstadt.ukp.inception.rendering.selection.FocusPosition.TOP;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static org.apache.wicket.event.Broadcast.BREADTH;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.wicketstuff.event.annotation.OnEvent;
import org.wicketstuff.jquery.core.Options;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.NotEditableException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.PagingKeyBindingsPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.UndoKeyBindingsPanel;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences.UserPreferencesService;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateChangeFlag;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.AnnotationDetailEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.multiedit.DocumentEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.SidebarPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.SidebarStateChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.SidebarTabSelectedEvent;
import de.tudarmstadt.ukp.inception.annotation.events.BeforeDocumentOpenedEvent;
import de.tudarmstadt.ukp.inception.annotation.events.DocumentOpenedEvent;
import de.tudarmstadt.ukp.inception.annotation.events.PreparingToOpenDocumentEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.TypeAdapter_ImplBase;
import de.tudarmstadt.ukp.inception.documents.api.DocumentAccess;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorBase;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorRegistry;
import de.tudarmstadt.ukp.inception.editor.ContextMenuLookup;
import de.tudarmstadt.ukp.inception.editor.state.AnnotatorStateImpl;
import de.tudarmstadt.ukp.inception.log.api.EventRepository;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationException;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DiamContext;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DocumentEditorManager;
import de.tudarmstadt.ukp.inception.rendering.selection.ActiveEditorChangedEvent;
import de.tudarmstadt.ukp.inception.rendering.selection.AnnotatorViewportChangedEvent;
import de.tudarmstadt.ukp.inception.rendering.selection.EditorContentReplacedEvent;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VRange;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.kendo.AjaxSplitterBehavior;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationEventPublisherHolder;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.joining;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.flow.RedirectToUrlException;
import de.tudarmstadt.ukp.inception.support.wicket.UrlFragmentBehavior;

public abstract class AnnotationPageBase2
    extends AnnotationPageBase
    implements DocumentEditorManager
{
    private static final long serialVersionUID = 1378872465851908515L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected static final String MID_DOCUMENT_STATUS_BADGES = "documentStatusBadges";

    private static final String MID_DOCUMENT_EDITOR_PANEL = "documentEditorPanel";
    private static final String MID_PAGING_KEY_BINDINGS = "pagingKeyBindings";
    private static final String MID_UNDO_KEY_BINDINGS = "undoKeyBindings";

    private static final String LEFT_SIDEBAR_COLLAPSED_SIZE = "52px";
    private static final String RIGHT_SIDEBAR_HIDDEN_SIZE = "0px";

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

    private WebMarkupContainer splitterContainer;
    private AjaxSplitterBehavior splitterBehavior;
    private WebMarkupContainer centerArea;
    private DocumentEditorPanel documentEditorPanel;
    private AnnotationDetailEditorPanel detailEditor;
    private DiamContext activeContext;

    private SidebarPanel leftSidebar;

    private long currentProjectId;
    private boolean pageReloaded = false;
    private boolean actionBarCollapsed = false;

    private UrlFragmentBehavior urlFragmentBehavior;

    public AnnotationPageBase2(final PageParameters aPageParameters)
    {
        super(aPageParameters);

        // If the page was accessed using an URL form ending in a document ID, move the document ID
        // into the fragment and redirect to the form without it. This ensures that any links on the
        // page do not carry the document ID, so that we can happily switch between documents using
        // AJAX without having to worry about links with a document ID potentially sending us back
        // to a specific document.
        //
        // This has to happen before anything else in this constructor, because it restarts the
        // request by throwing. It used to live in AnnotationPageBase, where it ran during the super
        // constructor call - i.e. also before this point.
        var params = getPageParameters();
        var documentParameter = params.get(PAGE_PARAM_DOCUMENT);
        if (!documentParameter.isEmpty()) {
            pushParametersIntoUrl(params, documentParameter, params.get(PAGE_PARAM_DATA_OWNER));
        }

        var state = new AnnotatorStateImpl();
        state.setUser(userRepository.getCurrentUser());
        setModel(Model.of(state));

        // The push above restarts the request, so by the time we get here PAGE_PARAM_DOCUMENT will
        // always be `null`... PAGE_PARAM_DATA_OWNER may be non-null if it is set without a
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

        splitterContainer = new WebMarkupContainer("splitter");
        splitterContainer.setOutputMarkupId(true);
        splitterContainer.add(AttributeModifier.replace("style", "width:100%;height:100%"));
        add(splitterContainer);

        splitterBehavior = new AjaxSplitterBehavior("#" + splitterContainer.getMarkupId(),
                AjaxSplitterBehavior.Orientation.HORIZONTAL, new Options())
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void renderHead(org.apache.wicket.Component aComponent,
                    IHeaderResponse aResponse)
            {
                setPanes(buildSplitterPanes());
                super.renderHead(aComponent, aResponse);
            }

            @Override
            protected void onResize(AjaxRequestTarget aTarget, double[] aSizes)
            {
                if (aSizes.length >= 1 && !leftSidebar.isCollapsed()) {
                    persistSidebarSize(LEFT, aSizes[0]);
                }
                if (aSizes.length >= 3 && isRightSidebarVisible()) {
                    persistSidebarSize(RIGHT, aSizes[2]);
                }
            }
        };
        splitterContainer.add(splitterBehavior);

        // Create the right sidebar first because it initializes the detailEditor field.
        splitterContainer.add(createRightSidebar("rightSidebar"));

        centerArea = new WebMarkupContainer("centerArea");
        centerArea.add(visibleWhen(this::hasEditor));
        centerArea.setOutputMarkupPlaceholderTag(true);
        splitterContainer.add(centerArea);

        centerArea.add(new EmptyPanel(MID_DOCUMENT_EDITOR_PANEL));

        leftSidebar = createLeftSidebar("leftSidebar");
        splitterContainer.add(leftSidebar);

        add(new PagingKeyBindingsPanel(MID_PAGING_KEY_BINDINGS));
        add(new UndoKeyBindingsPanel(MID_UNDO_KEY_BINDINGS));
    }

    protected Component createDocumentStatusBadges(String aId)
    {
        return new EmptyPanel(aId);
    }

    /**
     * Create the panel hosting this page's main editor.
     *
     * @param aId
     *            the component id the panel has to use.
     * @return the panel hosting the main editor.
     */
    protected DocumentEditorPanel createDocumentEditorPanel(String aId)
    {
        return new MainDocumentEditorPanel(aId, getModel());
    }

    /**
     * Called before the annotation CAS is created/upgraded and written, so that subclasses can
     * refuse opening the document - typically by throwing a
     * {@link org.apache.wicket.RestartResponseException}. Mind that this runs before it is known
     * whether the document is editable.
     *
     * @param aDocument
     *            the document that is about to be opened.
     */
    protected void ensureDocumentMayBeOpened(SourceDocument aDocument)
    {
        // Nothing to do by default
    }

    private void persistSidebarSize(SidebarStateChangedEvent.Side aSide, double aSize)
    {
        var project = getProject();
        if (project == null) {
            return;
        }

        // Do not reject sizes outside [MIN, MAX] here: dragging the splitter toward the middle
        // produces a reported size at (or just above) SIDEBAR_SIZE_MAX due to splitbar-width
        // rounding, and rejecting it would silently drop the user's drag so the pane snaps back to
        // its previously stored size on the next page load. The trait setters below already clamp
        // the value into range, so we simply let them store the clamped size.

        var sessionOwner = userRepository.getCurrentUser();
        var layoutState = preferencesService.loadTraitsForUserAndProject(KEY_LAYOUT_STATE,
                sessionOwner, project);
        var sessionPrefs = getModelObject().getPreferences();
        switch (aSide) {
        case LEFT:
            layoutState.setSidebarSizeLeft(aSize);
            sessionPrefs.setSidebarSizeLeft(aSize);
            break;
        case RIGHT:
            layoutState.setSidebarSizeRight(aSize);
            sessionPrefs.setSidebarSizeRight(aSize);
            break;
        }
        preferencesService.saveTraitsForUserAndProject(KEY_LAYOUT_STATE, sessionOwner, project,
                layoutState);
    }

    private Options[] buildSplitterPanes()
    {
        double sizeLeft = SIDEBAR_SIZE_DEFAULT;
        double sizeRight = SIDEBAR_SIZE_DEFAULT;
        var project = getProject();
        if (project != null) {
            var layoutState = preferencesService.loadTraitsForUserAndProject(KEY_LAYOUT_STATE,
                    userRepository.getCurrentUser(), project);
            sizeLeft = layoutState.getSidebarSizeLeft();
            sizeRight = layoutState.getSidebarSizeRight();
        }

        var minSize = Options.asString(SIDEBAR_SIZE_MIN + "%");
        var maxSize = Options.asString(SIDEBAR_SIZE_MAX + "%");
        Options leftPane;
        if (leftSidebar.isCollapsed()) {
            leftPane = new Options("size", Options.asString(LEFT_SIDEBAR_COLLAPSED_SIZE)) //
                    .set("resizable", false);
        }
        else {
            leftPane = new Options("size", Options.asString(sizeLeft + "%")) //
                    .set("min", minSize) //
                    .set("max", maxSize);
        }
        Options rightPane;
        if (isRightSidebarVisible()) {
            rightPane = new Options("size", Options.asString(sizeRight + "%")) //
                    .set("min", minSize) //
                    .set("max", maxSize);
        }
        else {
            rightPane = new Options("size", Options.asString(RIGHT_SIDEBAR_HIDDEN_SIZE)) //
                    .set("resizable", false);
        }
        return new Options[] { leftPane, new Options(), rightPane };
    }

    @OnEvent
    public void onSidebarStateChanged(SidebarStateChangedEvent aEvent)
    {
        if (aEvent.getSide() != LEFT) {
            return;
        }

        var target = aEvent.getTarget();

        dropActiveContextIfNotDisplayed(target);

        splitterBehavior.destroy(target);
        target.add(leftSidebar);
        splitterBehavior.reconfigure(target, buildSplitterPanes());
    }

    @OnEvent
    public void onSidebarTabSelected(SidebarTabSelectedEvent aEvent)
    {
        if (aEvent.getSide() != LEFT) {
            return;
        }

        dropActiveContextIfNotDisplayed(aEvent.getTarget());
    }

    private AnnotationDetailEditorPanel createDetailEditor()
    {
        return new AnnotationDetailEditorPanel("annotationDetailEditorPanel", this, getModel());
    }

    /**
     * Keep the URL fragment in sync when the view has changed, e.g. due to paging.
     */
    @SuppressWarnings("javadoc")
    @OnEvent
    public void onViewStateChanged(AnnotatorViewportChangedEvent aEvent)
    {
        if (!aEvent.isFor(getModelObject())) {
            return;
        }

        updateUrlFragment(aEvent.getRequestHandler());
    }

    /**
     * @return the main annotation editor component, or {@code null} if none has been created yet.
     *         Allows page components hosting a second editor (e.g. the reference-document sidebar)
     *         to coordinate with the main editor, e.g. for viewport synchronization.
     */
    private AnnotationEditorBase getAnnotationEditor()
    {
        return documentEditorPanel != null ? documentEditorPanel.getEditor() : null;
    }

    private void openDocumentEditor()
    {
        if (documentEditorPanel == null) {
            documentEditorPanel = createDocumentEditorPanel(MID_DOCUMENT_EDITOR_PANEL);
            centerArea.replace(documentEditorPanel);
        }

        setActiveContext(null, documentEditorPanel);
    }

    @Override
    public Optional<DiamContext> getActiveContext()
    {
        return Optional.ofNullable(activeContext);
    }

    @Override
    public void setActiveContext(AjaxRequestTarget aTarget, DiamContext aContext)
    {
        if (aContext == activeContext) {
            return;
        }

        activeContext = aContext;

        send(this, BREADTH, new ActiveEditorChangedEvent(activeContext, aTarget));
    }

    @Override
    public boolean hasEditor()
    {
        return getActiveContext() //
                .map(DiamContext::getAnnotatorState) //
                .map(AnnotatorState::getDocument) //
                .isPresent();
    }

    protected void dropActiveContextIfNotDisplayed(AjaxRequestTarget aTarget)
    {
        if (isActiveContextDisplayed()) {
            return;
        }

        setActiveContext(aTarget, null);
    }

    private boolean isActiveContextDisplayed()
    {
        return getActiveContext().map(this::isDisplayed).orElse(false);
    }

    private boolean isDisplayed(DiamContext aContext)
    {
        if (!(aContext instanceof Component component)) {
            return true;
        }

        return component.findParent(Page.class) != null && component.isVisibleInHierarchy();
    }

    @Override
    protected void onBeforeRender()
    {
        super.onBeforeRender();
        dropActiveContextIfNotDisplayed(null);
    }

    @Override
    public void actionShowDocument(AjaxRequestTarget aTarget, SourceDocument aDocument, int aBegin,
            int aEnd, List<VRange> aAdditionalPingRanges)
        throws IOException, AnnotationException
    {
        ensureIsAccessible(aDocument);

        resolveEditorFor(aDocument).actionShowSelectedDocument(aTarget, aDocument, aBegin, aEnd,
                aAdditionalPingRanges);
    }

    @Override
    public void ensureIsAccessible(SourceDocument aDocument) throws AnnotationException
    {
        if (!getListOfDocs().contains(aDocument)) {
            throw new AnnotationException(
                    "Document [" + aDocument.getName() + "] is not accessible.");
        }
    }

    @Override
    public Optional<DiamContext> findEditorFor(SourceDocument aDocument, AnnotationSet aDataOwner)
    {
        if (documentEditorPanel == null) {
            return Optional.empty();
        }

        var state = documentEditorPanel.getAnnotatorState();
        if (!Objects.equals(state.getDocument(), aDocument)
                || !Objects.equals(state.getDataOwner(), aDataOwner)) {
            return Optional.empty();
        }

        return Optional.of(documentEditorPanel);
    }

    @Override
    public DiamContext resolveEditorFor(SourceDocument aDocument) throws AnnotationException
    {
        openDocumentEditor();

        return documentEditorPanel;
    }

    private SidebarPanel createLeftSidebar(String aId)
    {
        return new SidebarPanel(aId, AnnotationPageBase2.this);
    }

    private WebMarkupContainer createRightSidebar(String aId)
    {
        var rightSidebar = new WebMarkupContainer(aId);
        rightSidebar.setOutputMarkupPlaceholderTag(true);

        detailEditor = createDetailEditor();
        rightSidebar.add(detailEditor);
        rightSidebar.add(visibleWhen(this::isRightSidebarVisible));
        return rightSidebar;
    }

    private boolean isRightSidebarVisible()
    {
        // The detail panel needs an editor to act on...
        if (!hasEditor()) {
            return false;
        }

        // ...and something to edit in it. The layer check stays: a document can be open in an
        // editor while offering no selectable layers, and the detail panel is empty then.
        var layers = getActiveContext().orElseThrow().getAnnotatorState().getSelectableLayers();
        return layers != null && !layers.isEmpty();
    }

    @Override
    public List<SourceDocument> getListOfDocs()
    {
        var state = getModelObject();
        return new ArrayList<>(documentService
                .listAnnotatableDocuments(state.getProject(), state.getUser()).keySet());
    }

    /**
     * Discard the cached editability verdict so that the next {@link #isEditable()} re-evaluates
     * it. Call this after changing anything the verdict depends on - in particular after a source
     * document state transition, since whether a curator may edit depends on the document having
     * reached a curation state.
     */
    protected void clearIsEditableCache()
    {
        if (documentEditorPanel != null) {
            documentEditorPanel.clearIsEditableCache();
        }
    }

    public AnnotationDetailEditorPanel getDetailEditor()
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

            if (state.getProject() != null) {
                state.setProject(projectService.getProject(state.getProject().getId()));
            }

            state.refreshDocument(documentService);

            LOG.trace("Preparing to open document {}@{}", state.getUser(), state.getDocument());

            // Load constraints
            state.setConstraints(constraintsService.getMergedConstraints(state.getProject()));

            // Load user preferences
            loadPreferences();

            // Create the main editor if this is the first document opened on this page - the page
            // constructs without one. Must happen after the preferences have been loaded, because
            // the panel reads the configured editor type out of them.
            openDocumentEditor();

            // Set the actual editor component. This has to happen *before* any AJAX refreshes are
            // scheduled and *after* the preferences have been loaded (because the current editor
            // type is set in the preferences. Only the component is created here - the document is
            // paged further below, once the CAS has been upgraded.
            documentEditorPanel.createEditorComponent();

            state.reset();

            applicationEventPublisherHolder.get().publishEvent(
                    new PreparingToOpenDocumentEvent(this, getModelObject().getDocument(),
                            getModelObject().getUser().getUsername(), sessionOwnerName));

            // INFO BOUNDARY ---------------------------------------------------------------
            // PreparingToOpenDocumentEvent has the option to change the annotator state.
            // No information from the annotator state read before this point may be
            // used afterwards. Information has to be re-read from the annotator state to get
            // the latest values.

            ensureDocumentMayBeOpened(state.getDocument());

            // Check if there is an annotation document entry in the database. If there is none,
            // create one.
            LOG.trace("Opening document {}@{}", state.getUser(), state.getDocument());
            var annotationDocument = documentService
                    .createOrGetAnnotationDocument(state.getDocument(), state.getUser());
            var stateBeforeOpening = annotationDocument.getState();

            // Update document state
            transitionDocumentStateOnLoadDocument(state, annotationDocument);

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

                documentEditorPanel.bumpAnnotationCasTimestamp(state);
            }

            // if project is changed, reset some project specific settings
            if (currentProjectId != state.getProject().getId()) {
                state.clearRememberedFeatures();
                currentProjectId = state.getProject().getId();
            }

            documentEditorPanel.refreshActionBarItems();

            // update paging, only do it during document load so we load the CAS after it has been
            // upgraded
            state.getPagingStrategy().recalculatePage(state, editorCas);

            // Initialize the visible content - this has to happen after the annotation editor
            // component has been created because only then the paging strategy is known
            moveToFocus(aFocus, sessionOwnerName, state, editorCas, dataOwnerName);

            // Tell anything bound to this editor that its content has been replaced, so it can
            // drop selections and feature values resolved against the previous CAS. We reload the
            // page content below, so in order not to schedule a double-update, we pass null here.
            send(this, BREADTH, new EditorContentReplacedEvent(state, null));

            if (aTarget != null) {
                // Update URL for current document
                updateUrlFragment(aTarget);
                // WicketUtil.refreshPage(aTarget, getPage());
                splitterBehavior.destroy(aTarget);
                aTarget.add(splitterContainer);
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

    private void moveToFocus(int aFocus, String sessionOwnerName, AnnotatorState state,
            CAS editorCas, String dataOwnerName)
    {
        if (aFocus > 0) {
            state.moveToUnit(editorCas, aFocus, CENTERED);
        }
        else if (dataOwnerName.equals(sessionOwnerName) || dataOwnerName.equals(CURATION_USER)) {
            var offset = TypeAdapter_ImplBase.getResumptionLocation(editorCas);
            state.moveToOffset(editorCas, offset, CENTERED);
        }
        else {
            state.moveToUnit(editorCas, 0, TOP);
        }
    }

    /**
     * This is called for every document load, whether or not the document is editable. Implementors
     * decide per transition whether editability matters.
     * <p>
     * Editability is cached per request ({@code annotationNotEditableReason}), and the caller has
     * already resolved it before this method runs. An implementor that transitions into editability
     * and then wants to act on it has to call {@link #clearIsEditableCache()} in between, otherwise
     * {@link #isEditable()} still reports the verdict from before the transition.
     *
     * @param state
     *            the annotator state of the document being opened.
     * @param annotationDocument
     *            the annotation document of the data owner the document is opened for.
     */
    protected abstract void transitionDocumentStateOnLoadDocument(AnnotatorState state,
            AnnotationDocument annotationDocument);

    @Override
    public void actionRefreshDocument(AjaxRequestTarget aTarget)
    {
        // Partial page updates only need to be triggered if we are in a partial page update request
        if (aTarget == null) {
            return;
        }

        var editor = getAnnotationEditor();
        if (editor != null) {
            try {
                editor.requestRender(aTarget);
            }
            catch (Exception e) {
                LOG.warn("Unable to refresh annotation editor, forcing page refresh", e);
                throw new RestartResponseException(getPage());
            }
        }

        updateUrlFragment(aTarget);
    }

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

        var layoutState = preferencesService.loadTraitsForUserAndProject(KEY_LAYOUT_STATE,
                sessionOwner, project);
        actionBarCollapsed = layoutState.isActionBarCollapsed();

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

            if (state.getDocumentIndex() == -1) {
                failWithDocumentNotFound("Document [" + doc.getName() + "] in project ["
                        + project.getName() + "] is not available");
                return;
            }
        }
    }

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
                            var editorCas = documentEditorPanel.getEditorCas();
                            var offset = TypeAdapter_ImplBase.getResumptionLocation(editorCas);
                            state.moveToOffset(editorCas, offset, CENTERED);
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
            var cas = documentEditorPanel.getEditorCas();
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

    public List<AnnotationDocument> listAccessibleDocuments(Project aProject, User aDataOwner)
    {
        var sessionOwner = userRepository.getCurrentUser();
        return documentService.listAccessibleDocuments(aProject, aDataOwner, sessionOwner);
    }

    @Override
    public Optional<ContextMenuLookup> getContextMenuLookup()
    {
        var editor = getAnnotationEditor();
        return editor != null ? editor.getContextMenuLookup() : Optional.empty();
    }

    protected boolean isEditable()
    {
        try {
            documentEditorPanel.ensureIsEditable();
            return true;
        }
        catch (NotEditableException e) {
            return false;
        }
    }

    private void pushParametersIntoUrl(PageParameters aParams, StringValue aDocumentParameter,
            StringValue aUserParameter)
    {
        var requestCycle = getRequestCycle();

        var fragmentParams = new ArrayList<String>();
        fragmentParams.add(format("%s=%s", PAGE_PARAM_DOCUMENT, aDocumentParameter.toString()));
        aParams.remove(PAGE_PARAM_DOCUMENT);

        if (!aUserParameter.isEmpty()) {
            fragmentParams.add(format("%s=%s", PAGE_PARAM_DATA_OWNER, aUserParameter.toString()));
            aParams.remove(PAGE_PARAM_DATA_OWNER);
        }

        var url = Url.parse(requestCycle.urlFor(this.getClass(), aParams));
        var finalUrl = requestCycle.getUrlRenderer().renderFullUrl(url) + "#!"
                + fragmentParams.stream().collect(joining("&"));
        LOG.trace(
                "Pushing parameter for document [{}] and user [{}] into fragment: {} (URL redirect)",
                aDocumentParameter, aUserParameter, finalUrl);
        throw new RedirectToUrlException(finalUrl.toString());
    }

    /**
     * Create the behavior which keeps the URL fragment and the page state in sync. It is remembered
     * so that {@link #updateUrlFragment} can address it.
     *
     * @return the behavior. It has not been added to the page yet.
     */
    protected UrlFragmentBehavior createUrlFragmentBehavior()
    {
        urlFragmentBehavior = new UrlFragmentBehavior(this::getUrlFragmentParameters,
                this::onUrlFragmentParameterArrival);
        return urlFragmentBehavior;
    }

    private void onUrlFragmentParameterArrival(IRequestParameters aRequestParameters,
            AjaxRequestTarget aTarget)
    {
        var document = aRequestParameters.getParameterValue(PAGE_PARAM_DOCUMENT);
        var focus = aRequestParameters.getParameterValue(PAGE_PARAM_FOCUS);
        var user = aRequestParameters.getParameterValue(PAGE_PARAM_DATA_OWNER);

        if (document.isEmpty() && focus.isEmpty()) {
            return;
        }

        LOG.trace("URL fragment update: {}@{} focus {}", user, document, focus);

        var previousDoc = getModelObject().getDocument();
        var aPreviousUser = getModelObject().getUser();

        handleParameters(document, focus, user);

        updateDocumentView(aTarget, previousDoc, aPreviousUser, focus);
    }

    /**
     * @return the parameters that the URL fragment should carry for the current state. Parameters
     *         mapped to {@code null} are removed from the URL fragment.
     */
    protected Map<String, Object> getUrlFragmentParameters()
    {
        var state = getModelObject();

        if (state.getDocument() == null) {
            return emptyMap();
        }

        var parameters = new LinkedHashMap<String, Object>();

        parameters.put(PAGE_PARAM_DOCUMENT, state.getDocument().getId());

        parameters.put(PAGE_PARAM_FOCUS,
                state.getFocusUnitIndex() > 0 ? state.getFocusUnitIndex() : null);

        // REC: We currently do not want that one can switch to the CURATION_USER directly via
        // the URL without having to activate sidebar curation mode as well, so we do not handle
        // the CURATION_USER here.
        var dataOwner = state.getUser().getUsername();
        parameters.put(PAGE_PARAM_DATA_OWNER,
                Set.of(userRepository.getCurrentUsername(), CURATION_USER).contains(dataOwner)
                        ? null
                        : dataOwner);

        return parameters;
    }

    protected void updateUrlFragment(AjaxRequestTarget aTarget)
    {
        // Not every page keeps the URL fragment in sync - cf. createUrlFragmentBehavior()
        if (urlFragmentBehavior == null) {
            return;
        }

        // Update URL for current document
        urlFragmentBehavior.update(aTarget);
    }

    /**
     * The main editor of this page. Hosts the editor, the card header and the action bar, and owns
     * CAS access and editability for the document it shows.
     */
    protected class MainDocumentEditorPanel
        extends DocumentEditorPanel
    {
        private static final long serialVersionUID = -6218510663562034927L;

        public MainDocumentEditorPanel(String aId, IModel<AnnotatorState> aModel)
        {
            super(aId, AnnotationPageBase2.this, aModel);
        }

        @Override
        public boolean isEditor()
        {
            return true;
        }

        @Override
        protected Optional<AnnotationDetailEditorPanel> getDetailPanel()
        {
            return Optional.ofNullable(getDetailEditor());
        }

        @Override
        protected Component createDocumentStatusBadges(String aId)
        {
            return AnnotationPageBase2.this.createDocumentStatusBadges(aId);
        }

        @Override
        public List<SourceDocument> listAccessibleDocuments()
        {
            return getListOfDocs();
        }

        /**
         * Route loading through the page rather than through the panel's own (simpler) path.
         * <p>
         * ⚠️ Required, not cosmetic. The panel's own load only builds the editor and pages the
         * document. Opening a document in the *main* editor additionally has to upgrade and persist
         * the CAS, run the document state transitions, publish
         * {@code PreparingToOpenDocumentEvent}/{@code DocumentOpenedEvent} and update the URL
         * fragment. Everything reaching this from inside the panel - previous/next document,
         * {@code actionOpenDocument}, the preferences-changed rebuild - must therefore land in the
         * page's flow.
         */
        @Override
        public void actionLoadDocument(AjaxRequestTarget aTarget)
        {
            AnnotationPageBase2.this.actionLoadDocument(aTarget);
        }

        @Override
        protected boolean isInitialDocumentLoadedByPanel()
        {
            // The page drives the initial load through actionLoadDocument, which additionally
            // creates/upgrades/persists the CAS. Letting the panel load the document here would
            // read the CAS before it exists for a first-time document.
            return false;
        }

        @Override
        protected boolean isActionBarCollapsed()
        {
            return actionBarCollapsed;
        }

        @Override
        protected void onActionBarCollapsedChanged(boolean aCollapsed)
        {
            actionBarCollapsed = aCollapsed;

            var project = getProject();
            var sessionOwner = userRepository.getCurrentUser();
            var layoutState = preferencesService.loadTraitsForUserAndProject(KEY_LAYOUT_STATE,
                    sessionOwner, project);
            layoutState.setActionBarCollapsed(aCollapsed);
            preferencesService.saveTraitsForUserAndProject(KEY_LAYOUT_STATE, sessionOwner, project,
                    layoutState);
        }
    }
}

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
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.apache.wicket.event.Broadcast.BREADTH;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.wicketstuff.event.annotation.OnEvent;
import org.wicketstuff.jquery.core.Options;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.PagingKeyBindingsPanel;
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
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.UndoKeyBindingsPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.AnnotationDetailEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.editor.DocumentEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.SidebarPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.SidebarStateChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.SidebarTabSelectedEvent;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.url.UrlFragmentTarget;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.ws.EditorRequest;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.ws.DocumentEditorWorkspace_ImplBase;
import de.tudarmstadt.ukp.inception.annotation.events.BeforeDocumentOpenedEvent;
import de.tudarmstadt.ukp.inception.annotation.events.DocumentOpenedEvent;
import de.tudarmstadt.ukp.inception.annotation.events.PreparingToOpenDocumentEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.TypeAdapter_ImplBase;
import de.tudarmstadt.ukp.inception.documents.api.DocumentAccess;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorRegistry;
import de.tudarmstadt.ukp.inception.editor.state.AnnotatorStateImpl;
import de.tudarmstadt.ukp.inception.log.api.EventRepository;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationException;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DocumentEditor;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DocumentEditorManager;
import de.tudarmstadt.ukp.inception.rendering.selection.AnnotatorViewportChangedEvent;
import de.tudarmstadt.ukp.inception.rendering.selection.EditorContentReplacedEvent;
import de.tudarmstadt.ukp.inception.rendering.selection.EditorSetChangedEvent;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.kendo.AjaxSplitterBehavior;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.inception.support.wicket.UrlFragmentBehavior;

public abstract class AnnotationPageBase2
    extends AnnotationPageBase
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
    private DocumentEditorWorkspace_ImplBase workspace;
    private AnnotationDetailEditorPanel detailEditor;

    private SidebarPanel leftSidebar;

    private boolean sidebarsVisible;

    private long currentProjectId;

    /**
     * Whether the left sidebar's tabs have been refreshed on this page instance. They depend on the
     * user's rights for the open document, so they are refreshed once the first document arrives -
     * which used to be detected by "the previous document was null".
     */
    private boolean sidebarTabsRefreshed = false;
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

        // Children first: the parameters address an editor, and createAnnotatorState() resolves
        // the data owner from the page itself, so there is nothing to hand over to the workspace
        // before it exists.
        createChildComponents();

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

        updateDocumentView(null, focus);
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

        // Create workspace first so sidebar components can access it
        workspace = createWorkspace("workspace");
        splitterContainer.add(workspace);

        splitterContainer.add(createRightSidebar("rightSidebar"));

        leftSidebar = createLeftSidebar("leftSidebar");
        splitterContainer.add(leftSidebar);

        add(new PagingKeyBindingsPanel(MID_PAGING_KEY_BINDINGS, workspace));
        add(new UndoKeyBindingsPanel(MID_UNDO_KEY_BINDINGS, workspace));
    }

    protected Component createDocumentStatusBadges(String aId, IModel<AnnotatorState> aState)
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
        return new MainDocumentEditorPanel(aId);
    }

    /**
     * @return the data owner a new editor on this page should default to: the pinned data owner if
     *         this page pins one - curation pins {@code CURATION_SET} - and otherwise the session
     *         owner. Callers that open a document without an owner of their own in hand must use
     *         this rather than assuming the session owner, or they silently unpin a pinned page.
     */
    public AnnotationSet getDefaultDataOwner()
    {
        var pinned = getPinnedDataOwner();
        return pinned != null ? pinned : AnnotationSet.forUser(userRepository.getCurrentUser());
    }

    protected AnnotatorState createAnnotatorState()
    {
        var state = new AnnotatorStateImpl();
        state.setProject(getProject());

        var pinned = getPinnedDataOwner();
        var dataOwner = pinned != null ? userRepository.getUserOrCurationUser(pinned.id())
                : userRepository.getCurrentUser();
        state.setUser(dataOwner != null ? dataOwner : userRepository.getCurrentUser());

        return state;
    }

    /**
     * Called before the annotation CAS is created/upgraded and written, so that subclasses can
     * refuse opening the document - typically by throwing a
     * {@link org.apache.wicket.RestartResponseException}. Mind that this runs before it is known
     * whether the document is editable.
     *
     * @param aDocument
     *            the document that is about to be opened.
     * @param aState
     *            the state of the editor the document is being opened in.
     */
    protected void ensureDocumentMayBeOpened(SourceDocument aDocument, AnnotatorState aState)
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
        switch (aSide) {
        case LEFT:
            layoutState.setSidebarSizeLeft(aSize);
            break;
        case RIGHT:
            layoutState.setSidebarSizeRight(aSize);
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

        workspace.dropActiveEditorIfNotDisplayed(target);

        splitterBehavior.destroy(target);
        target.add(leftSidebar);
        splitterBehavior.reconfigure(target, buildSplitterPanes());
    }

    private boolean refreshSidebarVisibility(AjaxRequestTarget aTarget)
    {
        var visible = isRightSidebarVisible();
        if (visible == sidebarsVisible) {
            return false;
        }

        sidebarsVisible = visible;

        if (aTarget == null) {
            return false;
        }

        splitterBehavior.destroy(aTarget);
        aTarget.add(splitterContainer);
        return true;
    }

    @OnEvent
    public void onEditorContentReplaced(EditorContentReplacedEvent aEvent)
    {
        refreshSidebarVisibility(aEvent.getRequestHandler());
    }

    @OnEvent
    public void onSidebarTabSelected(SidebarTabSelectedEvent aEvent)
    {
        if (aEvent.getSide() != LEFT) {
            return;
        }

        workspace.dropActiveEditorIfNotDisplayed(aEvent.getTarget());
    }

    private AnnotationDetailEditorPanel createDetailEditor()
    {
        return new AnnotationDetailEditorPanel("annotationDetailEditorPanel", workspace);
    }

    /**
     * Keep the URL fragment in sync when the view has changed, e.g. due to paging.
     */
    @SuppressWarnings("javadoc")
    @OnEvent
    public void onViewStateChanged(AnnotatorViewportChangedEvent aEvent)
    {
        if (workspace == null || !workspace.anyEditorOwnsState(aEvent.getSource())) {
            return;
        }

        updateUrlFragment(aEvent.getRequestHandler());
    }

    @OnEvent
    public void onEditorSetChanged(EditorSetChangedEvent aEvent)
    {
        var target = aEvent.getRequestHandler();

        updateUrlFragment(target);

        refreshSidebarVisibility(target);
    }

    @Override
    public DocumentEditorManager getDocumentEditorManager()
    {
        return workspace;
    }

    public DocumentEditorWorkspace_ImplBase getWorkspace()
    {
        return workspace;
    }

    protected abstract DocumentEditorWorkspace_ImplBase createWorkspace(String aId);

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
        sidebarsVisible = isRightSidebarVisible();
        return rightSidebar;
    }

    private boolean isRightSidebarVisible()
    {
        // The detail panel needs an editor to act on...
        if (workspace == null || !workspace.hasOpenDocument()) {
            return false;
        }

        // ...and something to edit in it.
        return workspace.getActiveEditor() //
                .map(DocumentEditor::getAnnotatorState) //
                .map(AnnotatorState::getSelectableLayers) //
                .map(layers -> layers != null && !layers.isEmpty()) //
                .orElse(false);
    }

    public AnnotationDetailEditorPanel getDetailEditor()
    {
        return detailEditor;
    }

    /**
     * This is called for every document load, whether or not the document is editable. Implementors
     * decide per transition whether editability matters.
     *
     * @param aPanel
     *            the editor panel the document is being loaded into. Ask <b>this</b> panel about
     *            editability - with more than one pane open, editability is not a page-level
     *            question. Its annotator state is the state of the document being opened.
     * @param aAnnotationDocument
     *            the annotation document of the data owner the document is opened for.
     */
    protected abstract void transitionDocumentStateOnLoadDocument(DocumentEditorPanel aPanel,
            AnnotationDocument aAnnotationDocument);

    /**
     * @return the data owner this page pins all of its editors to, or {@code null} if the data
     *         owner is whatever the URL asks for. A page that pins one does not accept a data owner
     *         from the URL at all - the two arrive through different doors on purpose, so that a
     *         pinned pseudo user such as the curation user never becomes addressable by URL.
     */
    protected AnnotationSet getPinnedDataOwner()
    {
        return null;
    }

    /**
     * Assert that the session owner may use this page at all, before any URL parameter is acted on.
     *
     * @param aSessionOwner
     *            the user whose session this is.
     */
    protected abstract void requireAccess(User aSessionOwner);

    @Override
    protected void failWithUnopenableDocument(String aDetails)
    {
        // Show error and let the open document dialog come up by itself
        failWithoutOpeningDocument(aDetails);
    }

    protected void failWithoutOpeningDocument(String aDetails)
    {
        if (userRepository.isCurrentUserAdmin()) {
            getSession().error(aDetails);
        }
        else {
            getSession().error(
                    "Requested document does not exist or you have no permissions to access it.");
        }

        getRequestCycle().find(AjaxRequestTarget.class)
                .ifPresent(_target -> _target.addChildren(getPage(), IFeedback.class));
    }

    protected void handleParameters(StringValue aDocumentParameter, StringValue aFocusParameter,
            StringValue aUserParameter)
    {
        var sessionOwner = userRepository.getCurrentUser();
        requireAccess(sessionOwner);

        var project = getProject();

        if (project != null) {
            var layoutState = preferencesService.loadTraitsForUserAndProject(KEY_LAYOUT_STATE,
                    sessionOwner, project);
            actionBarCollapsed = layoutState.isActionBarCollapsed();
        }

        // One target per editor the fragment describes, in display order.
        var requested = workspace.parseUrlFragmentTargets(aDocumentParameter, aFocusParameter,
                aUserParameter);

        if (requested.isEmpty() || project == null) {
            return;
        }

        var target = getRequestCycle().find(AjaxRequestTarget.class).orElse(null);

        // Resolve EVERY slot before touching any editor. Resolution is where the access checks
        // live, and a check failing on a later slot used to abort it after the earlier slots had
        // already been mutated - leaving the page half-way through a layout nobody asked for.
        var requests = new ArrayList<EditorRequest>();
        for (var slot : requested) {
            var request = resolveRequestedDocument(sessionOwner, project, slot);
            if (request != null) {
                requests.add(request);
            }
        }

        if (requests.isEmpty()) {
            return;
        }

        try {
            workspace.actionShowDocumentsInEditors(target, requests);
        }
        catch (IOException | AnnotationException | RuntimeException e) {
            handleException(target, e);
        }
    }

    /**
     * @return what that slot of the fragment asks for, resolved against the project, or
     *         {@code null} if it names nothing or names something the session owner may not see.
     */
    private EditorRequest resolveRequestedDocument(User aSessionOwner, Project aProject,
            UrlFragmentTarget aRequested)
    {
        // An empty slot names no document. Nothing to open, and nothing to complain about.
        if (aRequested.document().isEmpty()) {
            return null;
        }

        var doc = getDocumentFromParameters(aProject, aRequested.document().orElse(null));

        if (doc == null) {
            return null;
        }

        var pinnedOwner = getPinnedDataOwner();
        var dataOwnerId = pinnedOwner != null ? pinnedOwner.id()
                : aRequested.dataOwner().orElseGet(aSessionOwner::getUsername);

        if (pinnedOwner != null ? userRepository.getUserOrCurationUser(dataOwnerId) == null
                : userRepository.get(dataOwnerId) == null) {
            failWithoutOpeningDocument("User not found [" + dataOwnerId + "]");
            return null;
        }

        if (!documentAccess.canViewAnnotationDocument(aSessionOwner.getUsername(),
                String.valueOf(aProject.getId()), doc.getId(), dataOwnerId)) {
            failWithoutOpeningDocument("Document [" + aRequested.document().orElse(null)
                    + "] does not exist in project [" + aProject.getName()
                    + "] or you have no access");
            return null;
        }

        return new EditorRequest(doc, AnnotationSet.forUser(dataOwnerId), aRequested.focus());
    }

    private void updateDocumentView(AjaxRequestTarget aTarget, StringValue aFocusParameter)
    {
        // URL is from external link, not just paging through documents, tabs may have changed
        // depending on user rights
        if (aTarget != null && !sidebarTabsRefreshed) {
            LOG.trace(
                    "Refreshing left sidebar as this is the first document loaded on this page instance");
            sidebarTabsRefreshed = true;
            leftSidebar.refreshTabs(aTarget);
        }
    }

    private void loadPreferences(AnnotatorState aState) throws BeansException, IOException
    {
        if (aState.isUserViewingOthersWork(userRepository.getCurrentUsername())
                || CURATION_USER.equals(aState.getUser().getUsername())) {
            userPreferenceService.loadPreferences(aState,
                    userRepository.getCurrentUser().getUsername());
        }
        else {
            userPreferenceService.loadPreferences(aState, userRepository.getCurrentUsername());
        }
    }

    public List<AnnotationDocument> listAccessibleDocuments(Project aProject, User aDataOwner)
    {
        var sessionOwner = userRepository.getCurrentUser();
        return documentService.listAccessibleDocuments(aProject, aDataOwner, sessionOwner);
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

        // No before-state is captured here any more. The workspace reconciles each editor against
        // the fragment itself, and it IS the before-state - it can simply look at what its own
        // editors are showing. The old snapshot was of the first editor only, which is why change
        // detection never worked past editor 0.
        handleParameters(document, focus, user);

        updateDocumentView(aTarget, focus);
    }

    /**
     * @return the parameters that the URL fragment should carry for the current state. Parameters
     *         mapped to {@code null} are removed from the URL fragment.
     */
    protected Map<String, Object> getUrlFragmentParameters()
    {
        // The fragment describes the WORKSPACE, not one of its editors. Which editors there are
        // and how they name themselves is workspace policy, so the page does not pick one here -
        // it asks what is open.
        return workspace.getUrlFragmentParameters();
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

        public MainDocumentEditorPanel(String aId)
        {
            super(aId, AnnotationPageBase2.this.workspace,
                    Model.of(AnnotationPageBase2.this.createAnnotatorState()));
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
        protected Component createDocumentStatusBadges(String aId, IModel<AnnotatorState> aState)
        {
            return AnnotationPageBase2.this.createDocumentStatusBadges(aId, aState);
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
        public void actionLoadDocument(AjaxRequestTarget aTarget, int aFocus)
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

                // Load user preferences into THIS panel's state
                loadPreferences(state);

                // Set the actual editor component. This has to happen *before* any AJAX
                // refreshes are scheduled and *after* the preferences have been loaded (because
                // the current editor type is set in the preferences). Only the component is
                // created here - the document is paged further below, once the CAS has been
                // upgraded.
                createEditorComponent();

                state.reset();

                applicationEventPublisherHolder.get()
                        .publishEvent(new PreparingToOpenDocumentEvent(AnnotationPageBase2.this,
                                state.getDocument(), state.getUser().getUsername(),
                                sessionOwnerName));

                // INFO BOUNDARY ---------------------------------------------------------------
                // PreparingToOpenDocumentEvent has the option to change the annotator state.
                // No information from the annotator state read before this point may be
                // used afterwards. Information has to be re-read from the annotator state to get
                // the latest values.

                ensureDocumentMayBeOpened(state.getDocument(), state);

                // Check if there is an annotation document entry in the database. If there is none,
                // create one.
                LOG.trace("Opening document {}@{}", state.getUser(), state.getDocument());
                var annotationDocument = documentService
                        .createOrGetAnnotationDocument(state.getDocument(), state.getUser());
                var stateBeforeOpening = annotationDocument.getState();

                // Update document state
                transitionDocumentStateOnLoadDocument(this, annotationDocument);

                // The transition may have changed whether this editor is editable - e.g. a
                // curation document only becomes editable once it has reached a curation state.
                // Drop the cached verdict here rather than asking implementors to remember: the
                // read below, and every read after it, has to see the state the transition left
                // behind.
                clearIsEditableCache();

                var editable = isEditable();

                // Read the CAS
                // Update the annotation document CAS
                var editorCas = documentService.readAnnotationCas(annotationDocument,
                        editable ? FORCE_CAS_UPGRADE : NO_CAS_UPGRADE);

                var dataOwnerName = state.getUser().getUsername();
                applicationEventPublisherHolder.get()
                        .publishEvent(new BeforeDocumentOpenedEvent(AnnotationPageBase2.this,
                                editorCas, state.getDocument(), dataOwnerName, sessionOwnerName,
                                editable));

                if (editable) {
                    // After creating an new CAS or upgrading the CAS, we need to save it. If the
                    // document is accessed for the first time and thus will transition from NEW
                    // to IN_PROGRESS, then we use this opportunity also to set the timestamp of
                    // the annotation document - this ensures that e.g. the dynamic workflow
                    // considers the document to be "active" for the given user so that it won't
                    // be considered as abandoned immediately after having been opened for the
                    // first time.
                    // We suppress the AfterCasWrittenEvent here - handlers should react to
                    // DocumentOpenedEvent instead.
                    var flags = AnnotationDocumentState.NEW == annotationDocument.getState()
                            ? new AnnotationDocumentStateChangeFlag[] {
                                    EXPLICIT_ANNOTATOR_USER_ACTION }
                            : new AnnotationDocumentStateChangeFlag[] {};
                    documentService.writeAnnotationCasSilently(editorCas, annotationDocument,
                            flags);

                    bumpAnnotationCasTimestamp(state);
                }

                // if project is changed, reset some project specific settings
                if (currentProjectId != state.getProject().getId()) {
                    state.clearRememberedFeatures();
                    currentProjectId = state.getProject().getId();
                }

                refreshActionBarItems();

                // update paging, only do it during document load so we load the CAS after it
                // has been upgraded
                state.getPagingStrategy().recalculatePage(state, editorCas);

                // Initialize the visible content - this has to happen after the annotation editor
                // component has been created because only then the paging strategy is known
                moveToFocus(aFocus, sessionOwnerName, state, editorCas, dataOwnerName);

                send(getPage(), BREADTH, new EditorContentReplacedEvent(state, aTarget));

                if (aTarget != null) {
                    updateUrlFragment(aTarget);
                    refreshAfterDocumentChange(aTarget);
                }

                LOG.trace("Document opened {}@{}", state.getUser(), state.getDocument());

                applicationEventPublisherHolder.get()
                        .publishEvent(new DocumentOpenedEvent(workspace, editorCas,
                                state.getDocument(), stateBeforeOpening,
                                state.getUser().getUsername(), sessionOwnerName));
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
            else if (dataOwnerName.equals(sessionOwnerName)
                    || dataOwnerName.equals(CURATION_USER)) {
                var offset = TypeAdapter_ImplBase.getResumptionLocation(editorCas);
                state.moveToOffset(editorCas, offset, CENTERED);
            }
            else {
                state.moveToUnit(editorCas, 0, TOP);
            }
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

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
package de.tudarmstadt.ukp.inception.ui.refdoc;

import static de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome7IconType.chevron_down_s;
import static de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome7IconType.chevron_up_s;
import static de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome7IconType.link_s;
import static de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome7IconType.link_slash_s;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationNavigationUserPrefs.KEY_ANNOTATION_NAVIGATION_USER_PREFS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.inception.rendering.selection.FocusPosition.TOP;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.ui.refdoc.ReferenceDocumentSidebarState.KEY_REFERENCE_DOCUMENT_SIDEBAR_STATE;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.Icon;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.ReadOnlyActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.DefaultPagingNavigator;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.NoPagingStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences.UserPreferencesService;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.open.OpenDocumentDialog;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.DocumentNamePanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.diam.model.DiamContext;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorBase;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorRegistry;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.editor.state.AnnotatorStateImpl;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.selection.AnnotatorViewportChangedEvent;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import jakarta.persistence.NoResultException;

/**
 * A sidebar that opens an arbitrary document from the current project in a read-only annotation
 * editor. The editor is auto-selected based on the document's format ("AUTO" mode).
 * <p>
 * The viewer is fully passive - it uses its own {@link AnnotatorState} so that paging/selection in
 * the sidebar never affects the main editor, and a {@link ReadOnlyActionHandler} so that clicks and
 * mutating actions are absorbed.
 * <p>
 * The sidebar acts as the {@link DiamContext} for its own toolbar: the reused
 * {@link OpenDocumentDialog} and {@link DefaultPagingNavigator} resolve the annotator state, editor
 * CAS and refresh action through this sidebar instead of the main editor's page, so opening or
 * paging a reference document stays local to the sidebar.
 */
public class ReferenceDocumentSidebar
    extends AnnotationSidebar_ImplBase
    implements DiamContext
{
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String MID_EDITOR = "editor";
    private static final String MID_NUMBER_OF_PAGES = "numberOfPages";

    private @SpringBean DocumentService documentService;
    private @SpringBean UserDao userRepository;
    private @SpringBean AnnotationEditorRegistry editorRegistry;
    private @SpringBean UserPreferencesService userPreferencesService;
    private @SpringBean PreferencesService preferencesService;

    private final AnnotatorStateImpl state;
    private final ReadOnlyActionHandler actionHandler;
    private final WebMarkupContainer editorContainer;
    private final DocumentNamePanel documentNamePanel;
    private final WebMarkupContainer actionBar;
    private final LambdaAjaxLink actionBarToggle;
    private final WebMarkupContainer documentNavigation;
    private final DefaultPagingNavigator pagingNavigator;
    private final OpenDocumentDialog openDialog;

    // Whether the sidebar's own action bar (open/navigate/paging controls) is hidden. Mirrors the
    // show/hide toggle the main editor offers, and is persisted per user/project via
    // ReferenceDocumentSidebarState so it survives page reloads and sessions.
    private boolean actionBarCollapsed = false;

    private boolean scrollSyncEnabled = false;
    private transient boolean syncingViewport = false;

    private final WebMarkupContainer scrollSyncGroup;

    // The editor and CAS provider for the reference document currently shown (null while none).
    private AnnotationEditorBase editor;
    private CasProvider casProvider;

    public ReferenceDocumentSidebar(String aId, AnnotationActionHandler aActionHandler,
            CasProvider aCasProvider, AnnotationPageBase2 aAnnotationPage)
    {
        super(aId, aActionHandler, aCasProvider, aAnnotationPage);

        var sessionOwner = userRepository.getCurrentUser();
        var project = getModelObject().getProject();

        // Independent state so paging/selection in the sidebar never affects the main editor.
        state = new AnnotatorStateImpl();
        state.setUser(sessionOwner);
        // No document is shown yet - use the no-op paging strategy so the position label renders
        // empty until a reference document is loaded (mirrors AnnotationPageBase2).
        state.setPagingStrategy(new NoPagingStrategy());
        if (project != null) {
            state.setProject(project);
            // Populates visible/selectable layers and the annotation preferences (window size etc.)
            try {
                userPreferencesService.loadPreferences(state, sessionOwner.getUsername());
            }
            catch (IOException e) {
                throw new RuntimeException("Unable to load annotation preferences", e);
            }

            // Restore the persisted sidebar layout (e.g. whether the action bar is collapsed) so it
            // survives page reloads and sessions.
            var sidebarState = preferencesService.loadTraitsForUserAndProject(
                    KEY_REFERENCE_DOCUMENT_SIDEBAR_STATE, sessionOwner, project);
            actionBarCollapsed = sidebarState.isActionBarCollapsed();
            scrollSyncEnabled = sidebarState.isScrollSyncEnabled();
        }

        actionHandler = new ReadOnlyActionHandler(this::getEditorCas);

        // Document name/project info line, same component the main editor uses in its header. The
        // sidebar is read-only, so editability comes from our own action handler rather than the
        // (editable) enclosing page.
        documentNamePanel = new DocumentNamePanel("documentNamePanel",
                Model.of((AnnotatorState) state), actionHandler::isEditable);
        add(documentNamePanel);

        // Position info line ("N-M / K sentences [doc x / y]"); the concrete label is produced by
        // the current paging strategy, so it is recreated whenever a document is (re)loaded.
        add(state.getPagingStrategy().createPositionLabel(MID_NUMBER_OF_PAGES,
                Model.of((AnnotatorState) state)));

        // The action bar (open/navigate/paging controls) can be collapsed via the toggle in the
        // card header, mirroring the main editor. Its children live inside this container so a
        // single class swap hides them all at once.
        actionBar = new WebMarkupContainer("actionBar");
        actionBar.setOutputMarkupId(true);
        actionBar.add(AttributeModifier.append("class",
                LambdaModel.of(() -> actionBarCollapsed ? " visually-hidden" : "")));
        add(actionBar);

        actionBarToggle = new LambdaAjaxLink("toggleActionBar", this::actionToggleActionBar);
        actionBarToggle.add(new Icon("toggleActionBarIcon",
                LambdaModel.of(() -> actionBarCollapsed ? chevron_down_s : chevron_up_s)));
        actionBarToggle.setOutputMarkupId(true);
        add(actionBarToggle);

        actionBar.add(
                new LambdaAjaxLink("showOpenDocumentDialog", this::actionShowOpenDocumentDialog));

        // Previous/next document buttons stepping through the same accessible-documents list the
        // open dialog offers. Only meaningful once a reference document is loaded.
        documentNavigation = new WebMarkupContainer("documentNavigation");
        documentNavigation.setOutputMarkupPlaceholderTag(true);
        documentNavigation.add(visibleWhen(() -> state.getDocument() != null));
        documentNavigation
                .add(new LambdaAjaxLink("showPreviousDocument", this::actionShowPreviousDocument));
        documentNavigation
                .add(new LambdaAjaxLink("showNextDocument", this::actionShowNextDocument));
        actionBar.add(documentNavigation);

        // Two-way scroll synchronization with the main editor. Only meaningful while a document is
        // shown; whether it actually engages is gated in scrollSyncScript().
        // The tooltip sits on the group, not on the button: browsers deliver no pointer events to a
        // disabled button, so a title on the button itself would stay invisible precisely when it
        // explains why the toggle cannot be used.
        scrollSyncGroup = new WebMarkupContainer("scrollSyncGroup");
        scrollSyncGroup.setOutputMarkupPlaceholderTag(true);
        scrollSyncGroup.add(visibleWhen(() -> state.getDocument() != null));
        scrollSyncGroup.add(AttributeModifier.replace("title",
                LambdaModel.of(() -> isScrollSyncPossible()
                        ? "Synchronize scrolling with the main editor"
                        : "Scroll synchronization is unavailable while a paged editor shows a "
                                + "different document than the main editor")));
        actionBar.add(scrollSyncGroup);

        var scrollSyncToggle = new LambdaAjaxLink("toggleScrollSync", this::actionToggleScrollSync);
        scrollSyncToggle.add(new Icon("toggleScrollSyncIcon",
                LambdaModel.of(() -> scrollSyncEnabled ? link_s : link_slash_s)));
        scrollSyncToggle.add(enabledWhen(this::isScrollSyncPossible));
        scrollSyncGroup.add(scrollSyncToggle);

        openDialog = new OpenDocumentDialog("openDialog", Model.of((AnnotatorState) state),
                getAnnotationPage()::listAccessibleDocuments, this::actionLoadDocument);
        add(openDialog);

        pagingNavigator = new DefaultPagingNavigator("pagingNavigator", this);
        pagingNavigator.add(visibleWhen(() -> state.getDocument() != null));
        actionBar.add(pagingNavigator);

        editorContainer = new WebMarkupContainer("editorContainer");
        editorContainer.setOutputMarkupId(true);
        editorContainer.add(new EmptyPanel(MID_EDITOR).setOutputMarkupId(true));
        add(editorContainer);
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        // Default to the document currently open in the main editor so the sidebar shows useful
        // content immediately instead of an empty viewer. The user can still switch to any other
        // accessible document via the open dialog or the previous/next buttons.
        var mainDocument = getModelObject().getDocument();
        if (mainDocument != null && state.getProject() != null) {
            // Keep the full accessible-documents list on the state so the position label reads
            // "[doc i / n]" and previous/next navigation works from the default document onwards.
            state.setDocument(mainDocument, listReferenceDocuments());
            try {
                loadDocumentIntoEditor();
            }
            catch (IOException e) {
                // Fall back to the empty viewer - the user can pick a document manually.
                state.setDocument(null, Collections.emptyList());
                LOG.error("Unable to load default reference document [{}]", mainDocument, e);
            }
        }
    }

    private void actionToggleActionBar(AjaxRequestTarget aTarget)
    {
        actionBarCollapsed = !actionBarCollapsed;

        var project = state.getProject();
        if (project != null) {
            var sessionOwner = userRepository.getCurrentUser();
            var sidebarState = preferencesService.loadTraitsForUserAndProject(
                    KEY_REFERENCE_DOCUMENT_SIDEBAR_STATE, sessionOwner, project);
            sidebarState.setActionBarCollapsed(actionBarCollapsed);
            preferencesService.saveTraitsForUserAndProject(KEY_REFERENCE_DOCUMENT_SIDEBAR_STATE,
                    sessionOwner, project, sidebarState);
        }

        aTarget.add(actionBar, actionBarToggle);
    }

    private void actionToggleScrollSync(AjaxRequestTarget aTarget)
    {
        scrollSyncEnabled = !scrollSyncEnabled;

        var project = state.getProject();
        if (project != null) {
            var sessionOwner = userRepository.getCurrentUser();
            var sidebarState = preferencesService.loadTraitsForUserAndProject(
                    KEY_REFERENCE_DOCUMENT_SIDEBAR_STATE, sessionOwner, project);
            sidebarState.setScrollSyncEnabled(scrollSyncEnabled);
            preferencesService.saveTraitsForUserAndProject(KEY_REFERENCE_DOCUMENT_SIDEBAR_STATE,
                    sessionOwner, project, sidebarState);
        }

        aTarget.add(scrollSyncGroup);
        scrollSyncScript().ifPresent(aTarget::appendJavaScript);
    }

    private Optional<String> scrollSyncScript()
    {
        var sidebarEditorId = editor != null ? editor.getViewportSyncClientId().orElse(null) : null;
        if (sidebarEditorId == null) {
            return Optional.empty();
        }

        var mainEditor = getAnnotationPage().getAnnotationEditor();
        var mainEditorId = mainEditor != null ? mainEditor.getViewportSyncClientId().orElse(null)
                : null;

        String script;
        if (isScrollSyncActive() && mainEditorId != null) {
            script = format("ExternalEditor.viewportSync.link('%s', '%s');", sidebarEditorId,
                    mainEditorId);
        }
        else {
            script = format("ExternalEditor.viewportSync.unlink('%s');", sidebarEditorId);
        }

        // The hub ships with the external-editor bundle; a page whose editors are all
        // non-external may not have it - then there is nothing to (un-)link anyway
        return Optional
                .of("if (window.ExternalEditor && ExternalEditor.viewportSync) { " + script + " }");
    }

    private boolean isScrollSyncActive()
    {
        return scrollSyncEnabled && isScrollSyncPossible();
    }

    private boolean isScrollSyncPossible()
    {
        if (state.getDocument() == null) {
            return false;
        }

        var refDocPaged = !(state.getPagingStrategy() instanceof NoPagingStrategy);
        var mainPaged = !(getModelObject().getPagingStrategy() instanceof NoPagingStrategy);
        if ((mainPaged && !refDocPaged) || (!mainPaged && refDocPaged)) {
            // Mixed mode cannot sync
            return false;
        }

        if (mainPaged && refDocPaged && !Objects.equals(state.getPagingStrategy().getClass(),
                getModelObject().getPagingStrategy().getClass())) {
            // If both editors are paging, they must have the same paging regime
            return false;
        }

        return true;
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        scrollSyncScript()
                .ifPresent(script -> aResponse.render(OnDomReadyHeaderItem.forScript(script)));
    }

    private void actionShowOpenDocumentDialog(AjaxRequestTarget aTarget)
    {
        openDialog.show(aTarget);
    }

    private void actionShowPreviousDocument(AjaxRequestTarget aTarget)
    {
        actionShowAdjacentDocument(aTarget, -1, "previous");
    }

    private void actionShowNextDocument(AjaxRequestTarget aTarget)
    {
        actionShowAdjacentDocument(aTarget, 1, "next");
    }

    /**
     * Step to the {@code aDirection}-adjacent accessible document, honoring the same "skip finished
     * documents" navigation preference the main editor's {@code DocumentNavigator} respects, so the
     * buttons behave identically in both places.
     */
    private void actionShowAdjacentDocument(AjaxRequestTarget aTarget, int aDirection,
            String aWhich)
    {
        var documents = listReferenceDocuments();
        var prefs = preferencesService.loadTraitsForUserAndProject(
                KEY_ANNOTATION_NAVIGATION_USER_PREFS, userRepository.getCurrentUser(),
                state.getProject());
        var skipFinished = prefs.isFinishedDocumentsSkippedByNavigation();

        var index = documents.indexOf(state.getDocument());
        while (true) {
            index += aDirection;

            if (index < 0 || index >= documents.size()) {
                if (skipFinished) {
                    info("There is no " + aWhich + " unfinished document. Use the Open Document"
                            + " dialog to select finished documents.");
                }
                else {
                    info("There is no " + aWhich + " document.");
                }
                aTarget.addChildren(getPage(), IFeedback.class);
                return;
            }

            var candidate = documents.get(index);
            if (!skipFinished || !isTerminal(candidate)) {
                // Keep the full list on the state so the position label stays
                // "[doc i / n]"-consistent.
                state.setDocument(candidate, documents);
                actionLoadDocument(aTarget);
                return;
            }
        }
    }

    private boolean isTerminal(SourceDocument aDocument)
    {
        var dataOwner = state.getUser();
        try {
            return documentService.getAnnotationDocument(aDocument, dataOwner).getState()
                    .isTerminal();
        }
        catch (NoResultException e) {
            return AnnotationDocumentState.NEW.isTerminal();
        }
    }

    /**
     * The accessible documents to step through - the same list, in the same order, that the open
     * dialog offers (both go through {@link AnnotationPageBase2#listAccessibleDocuments}).
     */
    private List<SourceDocument> listReferenceDocuments()
    {
        var project = state.getProject();
        var user = state.getUser();
        if (project == null || user == null) {
            return Collections.emptyList();
        }

        return getAnnotationPage().listAccessibleDocuments(project, user).stream()
                .map(AnnotationDocument::getDocument) //
                .collect(toList());
    }

    /**
     * Load the document currently set on our {@link #state} into a fresh read-only editor. Invoked
     * by the open dialog once the user has picked a document (the dialog has already set it on our
     * state model). The editor type is resolved automatically from the document format.
     */
    private void actionLoadDocument(AjaxRequestTarget aTarget)
    {
        try {
            loadDocumentIntoEditor();

            aTarget.add(editorContainer);
            aTarget.add(documentNamePanel);
            aTarget.add(documentNavigation);
            aTarget.add(pagingNavigator);
            aTarget.add(scrollSyncGroup);
            aTarget.add(get(MID_NUMBER_OF_PAGES));

            scrollSyncScript().ifPresent(aTarget::appendJavaScript);
        }
        catch (IOException e) {
            error("Unable to load reference document: " + e.getMessage());
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }

    /**
     * Build a fresh read-only editor for the document currently set on our {@link #state} and swap
     * it into the sidebar (along with a matching position label). Does not touch any
     * {@link AjaxRequestTarget}, so it can be used both during initial rendering (see
     * {@link #onInitialize()}) and from AJAX actions such as {@link #actionLoadDocument}. The
     * editor type is resolved automatically from the document format.
     */
    private void loadDocumentIntoEditor() throws IOException
    {
        var document = state.getDocument();

        Component newEditor;
        if (document == null) {
            editor = null;
            casProvider = null;
            state.setPagingStrategy(new NoPagingStrategy());
            newEditor = new EmptyPanel(MID_EDITOR);
        }
        else {
            // AUTO editor mode: pick the editor based on the document format
            var factory = editorRegistry.getPreferredEditorFactory(document.getProject(),
                    document.getFormat());
            state.setEditorFactoryId(factory.getBeanName());

            casProvider = () -> readReferenceCas(document);

            editor = factory.create(MID_EDITOR, Model.of((AnnotatorState) state), actionHandler,
                    casProvider);

            // Let the editor configure the paging strategy, then page the document
            factory.initState(state);
            var cas = casProvider.get();
            state.reset();
            state.getPagingStrategy().recalculatePage(state, cas);
            state.moveToUnit(cas, 0, TOP);

            newEditor = editor;
        }

        newEditor.setOutputMarkupId(true);
        editorContainer.addOrReplace(newEditor);

        // The position label is bound to the (possibly new) paging strategy, so recreate it.
        var positionLabel = state.getPagingStrategy().createPositionLabel(MID_NUMBER_OF_PAGES,
                Model.of((AnnotatorState) state));
        addOrReplace(positionLabel);
    }

    private CAS readReferenceCas(SourceDocument aDocument) throws IOException
    {
        // Read-only: SHARED_READ_ONLY_ACCESS never writes the CAS file (so it does not bump the
        // optimistic-locking timestamp), does not take the exclusive pool lock used by the main
        // editor, and for documents the user has never annotated it returns a transient CAS that is
        // never persisted. This mode requires AUTO_CAS_UPGRADE (upgrade happens in-memory only).
        return documentService.readAnnotationCas(aDocument,
                AnnotationSet.forUser(userRepository.getCurrentUser()), AUTO_CAS_UPGRADE,
                SHARED_READ_ONLY_ACCESS);
    }

    // --- DiamContext: the sidebar hosts its own editor/toolbar --------------------------------

    @Override
    public AnnotatorState getAnnotatorState()
    {
        return state;
    }

    @Override
    public CAS getEditorCas() throws IOException
    {
        if (casProvider == null) {
            throw new IllegalStateException("No reference document is currently loaded");
        }
        return casProvider.get();
    }

    @Override
    public AnnotationActionHandler getActionHandler()
    {
        return actionHandler;
    }

    @Override
    public void actionShowSelectedDocument(AjaxRequestTarget aTarget, SourceDocument aDocument,
            int aBegin, int aEnd)
        throws IOException, AnnotationException
    {
        // A self-contained viewer scrolls within its own document and does not switch documents;
        // the read-only action handler ignores navigation, so this stays local.
        getActionHandler().actionJump(aTarget, aBegin, aEnd);
    }

    @Override
    public void actionRefreshDocument(AjaxRequestTarget aTarget)
    {
        if (editor != null) {
            editor.requestRender(aTarget);
        }
    }

    @OnEvent
    public void onAnnotatorViewportChanged(AnnotatorViewportChangedEvent aEvent)
    {
        if (syncingViewport || !isScrollSyncActive()) {
            return;
        }

        var target = aEvent.getRequestHandler();
        if (target == null) {
            return;
        }

        var mainState = getModelObject();

        // If not showing the same document, we cannot sync by character offset - bail out
        if (!Objects.equals(mainState.getDocument(), state.getDocument())) {
            return;
        }

        // ... otherwise we can sync by character offset.
        try {
            syncingViewport = true;

            if (aEvent.isFor(mainState)) {
                // Main editor paged - follow it in the sidebar.
                var mainEditor = getAnnotationPage().getAnnotationEditor();
                if (editor == null || mainEditor == null) {
                    return;
                }
                state.moveToOffset(getEditorCas(), mainState.getWindowBeginOffset(), TOP);
                editor.requestRender(target);
                target.add(pagingNavigator);
                target.add(get(MID_NUMBER_OF_PAGES));
            }
            else if (aEvent.isFor(state)) {
                // Sidebar paged - follow it in the main editor.
                var mainEditor = getAnnotationPage().getAnnotationEditor();
                if (mainEditor == null) {
                    return;
                }
                mainState.moveToOffset(getCasProvider().get(), state.getWindowBeginOffset(), TOP);
                mainEditor.requestRender(target);
            }
        }
        catch (IOException e) {
            LOG.error("Unable to synchronize reference document viewport", e);
        }
        finally {
            syncingViewport = false;
        }
    }
}

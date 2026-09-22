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

import static de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome7IconType.link_s;
import static de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome7IconType.link_slash_s;
import static de.tudarmstadt.ukp.inception.rendering.selection.FocusPosition.TOP;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.ui.refdoc.ReferenceDocumentSidebarState.KEY_REFERENCE_DOCUMENT_SIDEBAR_STATE;
import static java.lang.String.format;
import static java.util.Collections.emptyList;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.Icon;
import de.tudarmstadt.ukp.inception.rendering.paging.NoPagingStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences.UserPreferencesService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.open.OpenDocumentDialog;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.editor.DocumentEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.editor.state.AnnotatorStateImpl;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DiamContext;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DocumentEditor;
import de.tudarmstadt.ukp.inception.rendering.selection.AnnotatorViewportChangedEvent;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.SidebarContext;

public class ReferenceDocumentSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @SpringBean UserDao userRepository;
    private @SpringBean UserPreferencesService userPreferencesService;
    private @SpringBean PreferencesService preferencesService;

    private final IModel<AnnotatorState> stateModel;
    private final RefDocEditorPanel documentEditorPanel;
    private final OpenDocumentDialog openDialog;

    private boolean scrollSyncEnabled = false;
    private transient boolean syncingViewport = false;

    private WebMarkupContainer scrollSyncGroup;

    public ReferenceDocumentSidebar(String aId, SidebarContext aContext)
    {
        super(aId, aContext);

        var sessionOwner = userRepository.getCurrentUser();
        var project = aContext.getProject();

        var state = new AnnotatorStateImpl();
        stateModel = Model.of(state);
        state.setUser(sessionOwner);
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

            scrollSyncEnabled = loadSidebarState().isScrollSyncEnabled();
        }

        state.setDocument(aContext.editorContext() //
                .map(DiamContext::getAnnotatorState) //
                .map(AnnotatorState::getDocument) //
                .orElse(null), emptyList());

        documentEditorPanel = new RefDocEditorPanel("documentEditorPanel", stateModel);
        add(documentEditorPanel);

        openDialog = new OpenDocumentDialog("openDialog",
                stateModel.map(AnnotatorState::getProject),
                stateModel.map(AnnotatorState::getDataOwner),
                getAnnotationPage()::listAccessibleDocuments, this::actionOpenDocument);
        add(openDialog);
    }

    private void actionOpenDocument(AjaxRequestTarget aTarget, SourceDocument aDocument,
            AnnotationSet aDataOwner, List<SourceDocument> aDocuments)
    {
        var state = stateModel.getObject();

        var user = userRepository.getUserOrCurationUser(aDataOwner.id());
        if (user != null) {
            state.setUser(user);
        }

        state.setDocument(aDocument, aDocuments);

        documentEditorPanel.actionLoadDocument(aTarget);
    }

    private ReferenceDocumentSidebarState loadSidebarState()
    {
        return preferencesService.loadTraitsForUserAndProject(KEY_REFERENCE_DOCUMENT_SIDEBAR_STATE,
                userRepository.getCurrentUser(), stateModel.getObject().getProject());
    }

    private void saveSidebarState(ReferenceDocumentSidebarState aState)
    {
        var project = stateModel.getObject().getProject();
        if (project == null) {
            return;
        }

        preferencesService.saveTraitsForUserAndProject(KEY_REFERENCE_DOCUMENT_SIDEBAR_STATE,
                userRepository.getCurrentUser(), project, aState);
    }

    private void actionShowOpenDocumentDialog(AjaxRequestTarget aTarget)
    {
        openDialog.show(aTarget, documentEditorPanel);
    }

    private void actionToggleScrollSync(AjaxRequestTarget aTarget)
    {
        scrollSyncEnabled = !scrollSyncEnabled;

        var sidebarState = loadSidebarState();
        sidebarState.setScrollSyncEnabled(scrollSyncEnabled);
        saveSidebarState(sidebarState);

        aTarget.add(scrollSyncGroup);
        scrollSyncScript().ifPresent(aTarget::appendJavaScript);
    }

    private Optional<String> scrollSyncScript()
    {
        var editor = documentEditorPanel.getEditor();
        var sidebarEditorId = editor != null ? editor.getViewportSyncClientId().orElse(null) : null;
        if (sidebarEditorId == null) {
            return Optional.empty();
        }

        var partnerEditorId = findSyncPartner(stateModel.getObject())
                .flatMap(DiamContext::getViewportSyncClientId).orElse(null);

        String script;
        if (isScrollSyncActive() && partnerEditorId != null) {
            script = format("ExternalEditor.viewportSync.link('%s', '%s');", sidebarEditorId,
                    partnerEditorId);
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
        var state = stateModel.getObject();

        var maybePartner = findSyncPartner(state);
        if (maybePartner.isEmpty()) {
            // Nothing shows the same annotations - there is nothing to sync with
            return false;
        }

        var partnerState = maybePartner.get().getAnnotatorState();

        var refDocPaged = !(state.getPagingStrategy() instanceof NoPagingStrategy);
        var partnerPaged = !(partnerState.getPagingStrategy() instanceof NoPagingStrategy);
        if ((partnerPaged && !refDocPaged) || (!partnerPaged && refDocPaged)) {
            // Mixed mode cannot sync
            return false;
        }

        if (partnerPaged && refDocPaged && !Objects.equals(state.getPagingStrategy().getClass(),
                partnerState.getPagingStrategy().getClass())) {
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

    /**
     * @param aState
     *            the state of the reference document shown in this sidebar
     * @return the editor showing the same annotations as this sidebar, if any. Scroll sync moves
     *         one viewport to the other's character offset, which is only meaningful between two
     *         views of the same document and data owner.
     */
    private Optional<DocumentEditor> findSyncPartner(AnnotatorState aState)
    {
        if (aState.getDocument() == null) {
            return Optional.empty();
        }

        return getDocumentEditorManager().findEditorFor(aState.getDocument(), aState.getDataOwner())
                // ... but not this sidebar itself, which is where the event came from.
                .filter(context -> context != documentEditorPanel);
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

        var state = stateModel.getObject();

        // Sync by character offset only makes sense against an editor showing the same
        // annotations. Ask which editor that is rather than assuming a particular one - if none
        // is, there is nothing to sync with.
        var partner = findSyncPartner(state);
        if (partner.isEmpty()) {
            return;
        }

        var partnerContext = partner.get();
        var partnerState = partnerContext.getAnnotatorState();

        try {
            syncingViewport = true;

            var editor = documentEditorPanel.getEditor();

            if (aEvent.isFor(partnerState)) {
                // Partner editor paged - follow it in the sidebar.
                if (editor == null) {
                    return;
                }
                state.moveToOffset(documentEditorPanel.getEditorCas(),
                        partnerState.getWindowBeginOffset(), TOP);
                editor.requestRender(target);
                target.add(documentEditorPanel.getActionBarItems(),
                        documentEditorPanel.getPositionLabel());
            }
            else if (aEvent.isFor(state)) {
                // Sidebar paged - follow it in the partner editor. Explicitly the partner's CAS,
                // not the active editor's: this branch moves the partner's state and re-renders
                // it, and the active editor here is typically this sidebar itself.
                partnerState.moveToOffset(partnerContext.getEditorCas(),
                        state.getWindowBeginOffset(), TOP);
                partnerContext.actionRefreshDocument(target);
            }
        }
        catch (IOException e) {
            LOG.error("Unable to synchronize reference document viewport", e);
        }
        finally {
            syncingViewport = false;
        }
    }

    /**
     * The reference document viewer. Read-only, lists the same documents the open dialog offers,
     * and contributes the sidebar-specific action bar items (open document, scroll sync toggle).
     */
    private class RefDocEditorPanel
        extends DocumentEditorPanel
        implements ReferenceDocumentEditor
    {
        private static final long serialVersionUID = -6631967232128940695L;

        public RefDocEditorPanel(String aId, IModel<AnnotatorState> aModel)
        {
            super(aId, ReferenceDocumentSidebar.this.getDocumentEditorManager(), aModel);
        }

        @Override
        protected boolean isActionBarCollapsed()
        {
            var project = stateModel.getObject().getProject();
            return project != null && loadSidebarState().isActionBarCollapsed();
        }

        @Override
        protected void onActionBarCollapsedChanged(boolean aCollapsed)
        {
            var sidebarState = loadSidebarState();
            sidebarState.setActionBarCollapsed(aCollapsed);
            saveSidebarState(sidebarState);
        }

        @Override
        protected Component createActionBarItemsBefore(String aId)
        {
            var fragment = new Fragment(aId, "openDocumentItem", ReferenceDocumentSidebar.this);

            fragment.add(new LambdaAjaxLink("showOpenDocumentDialog",
                    ReferenceDocumentSidebar.this::actionShowOpenDocumentDialog));

            return fragment;
        }

        @Override
        protected Component createActionBarItemsAfter(String aId)
        {
            var fragment = new Fragment(aId, "scrollSyncItem", ReferenceDocumentSidebar.this);

            // Two-way scroll synchronization with the main editor. Only meaningful while a document
            // is shown; whether it actually engages is gated in scrollSyncScript().
            // The tooltip sits on the group, not on the button: browsers deliver no pointer events
            // to a disabled button, so a title on the button itself would stay invisible precisely
            // when it explains why the toggle cannot be used.
            scrollSyncGroup = new WebMarkupContainer("scrollSyncGroup");
            scrollSyncGroup.setOutputMarkupPlaceholderTag(true);
            scrollSyncGroup
                    .add(visibleWhen(getModel().map(AnnotatorState::getDocument).isPresent()));
            scrollSyncGroup.add(AttributeModifier.replace("title",
                    LambdaModel.of(() -> isScrollSyncPossible()
                            ? "Synchronize scrolling with the main editor"
                            : "Scroll synchronization is unavailable while a paged editor shows a "
                                    + "different document than the main editor")));
            fragment.add(scrollSyncGroup);

            var scrollSyncToggle = new LambdaAjaxLink("toggleScrollSync",
                    ReferenceDocumentSidebar.this::actionToggleScrollSync);
            scrollSyncToggle.add(new Icon("toggleScrollSyncIcon",
                    LambdaModel.of(() -> scrollSyncEnabled ? link_s : link_slash_s)));
            scrollSyncToggle.add(enabledWhen(ReferenceDocumentSidebar.this::isScrollSyncPossible));
            scrollSyncGroup.add(scrollSyncToggle);

            return fragment;
        }

        @Override
        protected void onDocumentLoaded(AjaxRequestTarget aTarget)
        {
            aTarget.add(scrollSyncGroup);

            scrollSyncScript().ifPresent(aTarget::appendJavaScript);
        }
    }
}

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

import static de.tudarmstadt.ukp.clarin.webanno.api.CasUpgradeMode.FORCE_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase.PAGE_PARAM_DOCUMENT;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IGNORE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateChangeFlag.EXPLICIT_ANNOTATOR_USER_ACTION;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition.ANNOTATION_IN_PROGRESS_TO_CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition.NEW_TO_ANNOTATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;
import static de.tudarmstadt.ukp.inception.rendering.selection.FocusPosition.TOP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.uima.cas.CAS;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
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
import org.wicketstuff.annotation.mount.MountPath;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBar;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationEditorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.NoPagingStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences.UserPreferencesService;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.DecoratedObject;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketUtil;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.DocumentNamePanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.AnnotationDetailEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.SidebarPanel;
import de.tudarmstadt.ukp.inception.annotation.events.AnnotationEvent;
import de.tudarmstadt.ukp.inception.annotation.events.BeforeDocumentOpenedEvent;
import de.tudarmstadt.ukp.inception.annotation.events.DocumentOpenedEvent;
import de.tudarmstadt.ukp.inception.annotation.events.FeatureValueUpdatedEvent;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorBase;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorFactory;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorRegistry;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.editor.state.AnnotatorStateImpl;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationPreference;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.selection.AnnotatorViewportChangedEvent;
import de.tudarmstadt.ukp.inception.rendering.selection.SelectionChangedEvent;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.adapter.AnnotationException;

@MountPath(NS_PROJECT + "/${" + PAGE_PARAM_PROJECT + "}/annotate/#{" + PAGE_PARAM_DOCUMENT + "}")
public class AnnotationPage
    extends AnnotationPageBase
{
    private static final String MID_EDITOR = "editor";

    private static final String MID_NUMBER_OF_PAGES = "numberOfPages";

    private static final Logger LOG = LoggerFactory.getLogger(AnnotationPage.class);

    private static final long serialVersionUID = 1378872465851908515L;

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

    private long currentprojectId;

    private WebMarkupContainer centerArea;
    private WebMarkupContainer actionBar;
    private AnnotationEditorBase annotationEditor;
    private AnnotationDetailEditorPanel detailEditor;
    private SidebarPanel leftSidebar;

    public AnnotationPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);

        LOG.debug("Setting up annotation page with parameters: {}", aPageParameters);

        AnnotatorState state = new AnnotatorStateImpl(Mode.ANNOTATION);
        state.setUser(userRepository.getCurrentUser());
        setModel(Model.of(state));

        StringValue document = aPageParameters.get(PAGE_PARAM_DOCUMENT);
        StringValue focus = aPageParameters.get(PAGE_PARAM_FOCUS);
        StringValue user = aPageParameters.get(PAGE_PARAM_USER);
        if (focus == null) {
            focus = StringValue.valueOf(0);
        }

        handleParameters(document, focus, user);

        createChildComponents();

        updateDocumentView(null, null, null, focus);
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
            User user = userRepository.getCurrentUser();
            List<DecoratedObject<Project>> allowedProjects = new ArrayList<>();
            for (Project project : projectService.listProjectsWithUserHavingRole(user, ANNOTATOR)) {
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
                return AnnotationPage.this.getEditorCas();
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
     * current view. It might be more efficient to have another event that more closely mimicks
     * {@code AnnotationDetailEditorPanel.onChange()}.
     */
    @SuppressWarnings("javadoc")
    @OnEvent
    public void onAnnotationEvent(AnnotationEvent aEvent)
    {
        AnnotatorState state = getModelObject();

        if (!Objects.equals(state.getProject(), aEvent.getProject())
                || !Objects.equals(state.getDocument(), aEvent.getDocument())
                || !Objects.equals(state.getUser().getUsername(), aEvent.getUser())) {
            return;
        }

        actionRefreshDocument(aEvent.getRequestTarget());
    }

    /**
     * Re-render the document when a feature value has changed (assuming that this might have
     * triggered a change in some feature that might be shown on screen.
     * <p>
     * NOTE: Considering that this is a backend event, we check here if it even applies to the
     * current view. It might be more efficient to have another event that more closely mimicks
     * {@code AnnotationDetailEditorPanel.onChange()}.
     */
    @SuppressWarnings("javadoc")
    @OnEvent
    public void onFeatureValueUpdatedEvent(FeatureValueUpdatedEvent aEvent)
    {
        AnnotatorState state = getModelObject();

        if (!Objects.equals(state.getProject(), aEvent.getProject())
                || !Objects.equals(state.getDocument(), aEvent.getDocument())
                || !Objects.equals(state.getUser().getUsername(), aEvent.getUser())) {
            return;
        }

        actionRefreshDocument(aEvent.getRequestTarget());
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

        aEvent.getRequestHandler().add(centerArea.get(MID_NUMBER_OF_PAGES));

        actionRefreshDocument(aEvent.getRequestHandler());
    }

    private void createAnnotationEditor()
    {
        AnnotatorState state = getModelObject();

        if (state.getDocument() == null) {
            centerArea.addOrReplace(new EmptyPanel(MID_EDITOR).setOutputMarkupId(true));
            state.setPagingStrategy(new NoPagingStrategy());
            centerArea.addOrReplace(
                    state.getPagingStrategy().createPositionLabel(MID_NUMBER_OF_PAGES, getModel()));
            return;
        }

        AnnotationEditorState editorState = preferencesService
                .loadDefaultTraitsForProject(KEY_EDITOR_STATE, getProject());

        String editorId = editorState.getDefaultEditor();

        if (editorId == null) {
            editorId = getModelObject().getPreferences().getEditor();
        }

        AnnotationEditorFactory factory = editorRegistry.getEditorFactory(editorId);
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
        return new SidebarPanel("leftSidebar", getModel(),
                getModel().map(AnnotatorState::getPreferences)
                        .map(AnnotationPreference::getSidebarSizeLeft),
                detailEditor, () -> getEditorCas(), AnnotationPage.this);
    }

    private WebMarkupContainer createRightSidebar()
    {
        WebMarkupContainer rightSidebar = new WebMarkupContainer("rightSidebar");
        rightSidebar.setOutputMarkupPlaceholderTag(true);
        // Override sidebar width from preferences
        rightSidebar.add(new AttributeModifier("style",
                LoadableDetachableModel.of(() -> String.format("flex-basis: %d%%;",
                        getModelObject().getPreferences().getSidebarSizeRight()))));
        detailEditor = createDetailEditor();
        rightSidebar.add(detailEditor);
        rightSidebar.add(visibleWhen(getModel().map(AnnotatorState::getSelectableLayers)
                .map(List::isEmpty).map(b -> !b)));
        return rightSidebar;
    }

    @Override
    public List<SourceDocument> getListOfDocs()
    {
        AnnotatorState state = getModelObject();
        return new ArrayList<>(documentService
                .listAnnotatableDocuments(state.getProject(), state.getUser()).keySet());
    }

    @Override
    public CAS getEditorCas() throws IOException
    {
        AnnotatorState state = getModelObject();

        if (state.getDocument() == null) {
            throw new IllegalStateException("Please open a document first!");
        }

        // If we have a timestamp, then use it to detect if there was a concurrent access
        if (isEditable() && state.getAnnotationDocumentTimestamp().isPresent()) {
            documentService
                    .verifyAnnotationCasTimestamp(state.getDocument(),
                            state.getUser().getUsername(),
                            state.getAnnotationDocumentTimestamp().get(), "reading")
                    .ifPresent(state::setAnnotationDocumentTimestamp);
        }

        return documentService.readAnnotationCas(state.getDocument(),
                state.getUser().getUsername());
    }

    @Override
    public void writeEditorCas(CAS aCas) throws IOException, AnnotationException
    {
        ensureIsEditable();
        AnnotatorState state = getModelObject();
        documentService.writeAnnotationCas(aCas, state.getDocument(), state.getUser(), true);

        // Update timestamp in state
        documentService
                .getAnnotationCasTimestamp(state.getDocument(), state.getUser().getUsername())
                .ifPresent(state::setAnnotationDocumentTimestamp);
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
        LOG.trace("BEGIN LOAD_DOCUMENT_ACTION at focus " + aFocus);

        AnnotatorState state = getModelObject();
        if (state.getUser() == null) {
            state.setUser(userRepository.getCurrentUser());
        }

        try {
            // Check if there is an annotation document entry in the database. If there is none,
            // create one.
            AnnotationDocument annotationDocument = documentService
                    .createOrGetAnnotationDocument(state.getDocument(), state.getUser());

            // Read the CAS
            // Update the annotation document CAS
            CAS editorCas = documentService.readAnnotationCas(annotationDocument,
                    FORCE_CAS_UPGRADE);

            // (Re)initialize brat model after potential creating / upgrading CAS
            state.reset();

            boolean editable = isEditable();
            applicationEventPublisherHolder.get()
                    .publishEvent(new BeforeDocumentOpenedEvent(this, editorCas,
                            getModelObject().getDocument(),
                            getModelObject().getUser().getUsername(),
                            userRepository.getCurrentUser().getUsername(), editable));

            if (editable) {
                // After creating an new CAS or upgrading the CAS, we need to save it. If the
                // document is accessed for the first time and thus will transition from NEW to
                // IN_PROGRESS, then we use this opportunity also to set the timestamp of the
                // annotation document - this ensures that e.g. the dynamic workflow considers the
                // document to be "active" for the given user so that it won't be considered as
                // abandoned immediately after having been opened for the first time
                documentService.writeAnnotationCas(editorCas, annotationDocument,
                        AnnotationDocumentState.NEW.equals(annotationDocument.getState()));

                // Initialize timestamp in state
                documentService
                        .getAnnotationCasTimestamp(state.getDocument(),
                                state.getUser().getUsername())
                        .ifPresent(state::setAnnotationDocumentTimestamp);
            }

            // Load constraints
            state.setConstraints(constraintsService.loadConstraints(state.getProject()));

            // Load user preferences
            loadPreferences();

            // if project is changed, reset some project specific settings
            if (currentprojectId != state.getProject().getId()) {
                state.clearRememberedFeatures();
                currentprojectId = state.getProject().getId();
            }

            // Set the actual editor component. This has to happen *before* any AJAX refreshes are
            // scheduled and *after* the preferences have been loaded (because the current editor
            // type is set in the preferences.
            createAnnotationEditor();

            // update paging, only do it during document load so we load the CAS after it has been
            // upgraded
            state.getPagingStrategy().recalculatePage(state, editorCas);

            // Initialize the visible content - this has to happen after the annotation editor
            // component has been created because only then the paging strategy is known
            state.moveToUnit(editorCas, aFocus, TOP);

            // Update document state
            if (isEditable()) {
                if (SourceDocumentState.NEW.equals(state.getDocument().getState())) {
                    documentService.transitionSourceDocumentState(state.getDocument(),
                            NEW_TO_ANNOTATION_IN_PROGRESS);
                }

                if (AnnotationDocumentState.NEW.equals(annotationDocument.getState())) {
                    documentService.setAnnotationDocumentState(annotationDocument,
                            AnnotationDocumentState.IN_PROGRESS, EXPLICIT_ANNOTATOR_USER_ACTION);
                }

                if (state.getUser().getUsername().equals(CURATION_USER)) {
                    SourceDocument sourceDoc = state.getDocument();
                    SourceDocumentState sourceDocState = sourceDoc.getState();
                    if (!sourceDocState.equals(SourceDocumentState.CURATION_IN_PROGRESS)
                            && !sourceDocState.equals(SourceDocumentState.CURATION_FINISHED)) {
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

            applicationEventPublisherHolder.get().publishEvent(
                    new DocumentOpenedEvent(this, editorCas, getModelObject().getDocument(),
                            getModelObject().getUser().getUsername(),
                            userRepository.getCurrentUser().getUsername()));
        }
        catch (Exception e) {
            handleException(aTarget, e);
        }

        LOG.trace("END LOAD_DOCUMENT_ACTION");
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

        // Update URL for current document
        try {
            updateUrlFragment(aTarget);
        }
        catch (Exception e) {
            LOG.warn("Unable to request URL fragment update anymore", e);
        }
    }

    @Override
    protected void handleParameters(StringValue aDocumentParameter, StringValue aFocusParameter,
            StringValue aUserParameter)
    {
        User user = userRepository.getCurrentUser();
        requireAnyProjectRole(user);

        AnnotatorState state = getModelObject();
        Project project = getProject();
        SourceDocument document = getDocumentFromParameters(project, aDocumentParameter);

        // If there is no change in the current document, then there is nothing to do. Mind
        // that document IDs are globally unique and a change in project does not happen unless
        // there is also a document change.
        if (document != null && //
                document.equals(state.getDocument()) && //
                aFocusParameter != null && //
                aFocusParameter.toInt(0) == state.getFocusUnitIndex() && //
                aUserParameter != null && //
                state.getUser().getUsername().equals(aUserParameter.toString()) //
        ) {
            return;
        }

        if (!aUserParameter.isEmpty()) {
            User requestedUser = userRepository.get(aUserParameter.toString());
            if (requestedUser == null) {
                error("User not found [" + aUserParameter + "]");
                return;
            }
            state.setUser(requestedUser);
        }

        if (!user.equals(state.getUser())) {
            if (CURATION_USER.equals(state.getUser().getUsername())) {
                // Curators can access the special CURATION_USER
                requireProjectRole(user, MANAGER, CURATOR);
            }
            else {
                // Only managers are allowed to open documents as another user
                requireProjectRole(user, MANAGER);
            }
        }

        // Check if document is locked for the user
        if (document != null
                && documentService.existsAnnotationDocument(document, state.getUser())) {
            AnnotationDocument adoc = documentService.getAnnotationDocument(document,
                    state.getUser());

            if (AnnotationDocumentState.IGNORE.equals(adoc.getState()) && isEditable()) {
                error("Document [" + document.getId() + "] in project [" + project.getId()
                        + "] is locked for user [" + state.getUser().getUsername() + "]");
                return;
            }
        }

        // Update project in state
        // Mind that this is relevant if the project was specified as a query parameter
        // i.e. not only in the case that it was a URL fragment parameter.
        state.setProject(project);

        // If we arrive here and the document is not null, then we have a change of document
        // or a change of focus (or both)
        if (document != null && !document.equals(state.getDocument())) {
            state.setDocument(document, getListOfDocs());
        }
    }

    @Override
    protected void updateDocumentView(AjaxRequestTarget aTarget, SourceDocument aPreviousDocument,
            User aPreviousUser, StringValue aFocusParameter)
    {
        // url is from external link, not just paging through documents,
        // tabs may have changed depending on user rights
        if (aTarget != null && aPreviousDocument == null) {
            leftSidebar.refreshTabs(aTarget);
        }

        SourceDocument currentDocument = getModelObject().getDocument();
        if (currentDocument == null) {
            return;
        }

        User currentUser = getModelObject().getUser();
        if (currentUser == null) {
            return;
        }

        // If we arrive here and the document is not null, then we have a change of document
        // or a change of focus (or both)

        // Get current focus unit from parameters
        int focus = 0;
        if (aFocusParameter != null) {
            focus = aFocusParameter.toInt(0);
        }

        // If there is no change in the current document, then there is nothing to do. Mind
        // that document IDs are globally unique and a change in project does not happen unless
        // there is also a document change.
        if (aPreviousDocument != null && aPreviousDocument.equals(currentDocument) && //
                aPreviousUser != null && aPreviousUser.equals(currentUser) && //
                focus == getModelObject().getFocusUnitIndex() //
        ) {
            return;
        }

        // never had set a document or is a new one
        if (aPreviousDocument == null || !aPreviousDocument.equals(currentDocument)
                || aPreviousUser == null || !aPreviousUser.equals(currentUser)) {
            actionLoadDocument(aTarget, focus);
            return;
        }

        try {
            getModelObject().moveToUnit(getEditorCas(), focus, TOP);
            actionRefreshDocument(aTarget);
        }
        catch (Exception e) {
            if (aTarget != null) {
                aTarget.addChildren(getPage(), IFeedback.class);
            }
            LOG.info("Error reading CAS " + e.getMessage());
            error("Error reading CAS " + e.getMessage());
        }
    }

    @Override
    protected void loadPreferences() throws BeansException, IOException
    {
        AnnotatorState state = getModelObject();

        if (state.isUserViewingOthersWork(userRepository.getCurrentUsername())
                || state.getUser().getUsername().equals(CURATION_USER)) {
            userPreferenceService.loadPreferences(state,
                    userRepository.getCurrentUser().getUsername());
        }
        else {
            super.loadPreferences();
        }
    }

    public List<AnnotationDocument> listAccessibleDocuments(Project aProject, User aUser)
    {
        final List<AnnotationDocument> allDocuments = new ArrayList<>();
        Map<SourceDocument, AnnotationDocument> docs = documentService.listAllDocuments(aProject,
                aUser);

        User user = userRepository.getCurrentUser();
        for (Entry<SourceDocument, AnnotationDocument> e : docs.entrySet()) {
            SourceDocument sd = e.getKey();
            AnnotationDocument ad = e.getValue();
            if (ad != null) {
                // if current user is opening her own docs, don't let her see locked ones
                boolean userIsSelected = aUser.equals(user);
                if (userIsSelected && ad.getState() == IGNORE) {
                    continue;
                }
            }
            else {
                ad = new AnnotationDocument(user.getUsername(), sd);
            }

            allDocuments.add(ad);
        }

        return allDocuments;
    }
}

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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.page;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateUtils.updateDocumentTimestampAfterWrite;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateUtils.verifyAndUpdateDocumentTimestamp;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase.PAGE_PARAM_DOCUMENT;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.FocusPosition.CENTERED;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.FocusPosition.TOP;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition.ANNOTATION_IN_PROGRESS_TO_CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.wicketstuff.annotation.mount.MountPath;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBar;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.SentenceOrientedPagingStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.event.RenderAnnotationsEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.event.SelectionChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotationEditor;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.DecoratedObject;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketUtil;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.DocumentNamePanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.AnnotationDetailEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.SuggestionViewPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.AnnotationSelection;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.CurationContainer;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.SourceListView;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.SuggestionBuilder;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.UserAnnotationSegment;

/**
 * This is the main class for the curation page. It contains an interface which displays differences
 * between user annotations for a specific document. The interface provides a tool for merging these
 * annotations and storing them as a new annotation.
 */
@MountPath(NS_PROJECT + "/${" + PAGE_PARAM_PROJECT + "}/curate/#{" + PAGE_PARAM_DOCUMENT + "}")
public class CurationPage
    extends AnnotationPageBase
{
    private static final String MID_NUMBER_OF_PAGES = "numberOfPages";

    private final static Logger LOG = LoggerFactory.getLogger(CurationPage.class);

    private static final long serialVersionUID = 1378872465851908515L;

    private @SpringBean DocumentService documentService;
    private @SpringBean CurationDocumentService curationDocumentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean ConstraintsService constraintsService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean UserDao userRepository;

    private long currentprojectId;

    // Open the dialog window on first load
    private boolean firstLoad = true;

    private CurationContainer curationContainer;

    private WebMarkupContainer centerArea;
    private WebMarkupContainer actionBar;

    private SuggestionViewPanel suggestionViewPanel;

    private WebMarkupContainer sentenceListContainer;
    private WebMarkupContainer sentenceLinksListView;

    private AnnotationEditorBase annotationEditor;
    private AnnotationDetailEditorPanel editor;

    private SourceListView curationView;
    private List<SourceListView> sourceListModel;

    private int fSn = 0;
    private int lSn = 0;

    /**
     * Map for tracking curated spans. Key contains the address of the span, the value contains the
     * username from which the span has been selected
     */
    private Map<String, Map<Integer, AnnotationSelection>> annotationSelectionByUsernameAndAddress = new HashMap<>();

    // public CurationPage()
    // {
    // super();
    // LOG.debug("Setting up curation page without parameters");
    // commonInit();
    //
    // Map<String, StringValue> fragmentParameters = Session.get()
    // .getMetaData(SessionMetaData.LOGIN_URL_FRAGMENT_PARAMS);
    // if (fragmentParameters != null) {
    // // Clear the URL fragment parameters - we only use them once!
    // Session.get().setMetaData(SessionMetaData.LOGIN_URL_FRAGMENT_PARAMS, null);
    //
    // StringValue project = fragmentParameters.get(PAGE_PARAM_PROJECT_ID);
    // StringValue document = fragmentParameters.get(PAGE_PARAM_DOCUMENT_ID);
    // StringValue focus = fragmentParameters.get(PAGE_PARAM_FOCUS);
    //
    // handleParameters(null, project, document, focus, false);
    // }
    // }

    public CurationPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);

        LOG.debug("Setting up curation page with parameters: {}", aPageParameters);

        AnnotatorState state = new AnnotatorStateImpl(Mode.CURATION);
        // state.setUser(userRepository.getCurrentUser());
        setModel(Model.of(state));

        User user = userRepository.getCurrentUser();

        requireProjectRole(user, CURATOR);

        StringValue document = aPageParameters.get(PAGE_PARAM_DOCUMENT);
        StringValue focus = aPageParameters.get(PAGE_PARAM_FOCUS);

        handleParameters(document, focus, true);

        commonInit();

        updateDocumentView(null, null, focus);
    }

    private void commonInit()
    {
        add(createUrlFragmentBehavior());

        centerArea = new WebMarkupContainer("centerArea");
        centerArea.add(visibleWhen(() -> getModelObject().getDocument() != null));
        centerArea.setOutputMarkupPlaceholderTag(true);
        centerArea.add(new DocumentNamePanel("documentNamePanel", getModel()));
        add(centerArea);

        actionBar = new ActionBar("actionBar");
        centerArea.add(actionBar);

        getModelObject().setPagingStrategy(new SentenceOrientedPagingStrategy());
        centerArea.add(getModelObject().getPagingStrategy()
                .createPositionLabel(MID_NUMBER_OF_PAGES, getModel())
                .add(visibleWhen(() -> getModelObject().getDocument() != null))
                .add(LambdaBehavior.onEvent(RenderAnnotationsEvent.class,
                        (c, e) -> e.getRequestHandler().add(c))));

        // Ensure that a user is set
        getModelObject().setUser(new User(CURATION_USER, Role.ROLE_USER));

        curationContainer = new CurationContainer();
        curationContainer.setState(getModelObject());

        WebMarkupContainer sidebarCell = new WebMarkupContainer("rightSidebar");
        sidebarCell.setOutputMarkupPlaceholderTag(true);
        // Override sidebar width from preferences
        sidebarCell.add(new AttributeModifier("style",
                () -> String.format("flex-basis: %d%%;",
                        getModelObject() != null
                                ? getModelObject().getPreferences().getSidebarSize()
                                : 10)));
        add(sidebarCell);

        curationView = new SourceListView();

        List<UserAnnotationSegment> segments = new LinkedList<>();
        UserAnnotationSegment userAnnotationSegments = new UserAnnotationSegment();

        if (getModelObject() != null) {
            userAnnotationSegments
                    .setSelectionByUsernameAndAddress(annotationSelectionByUsernameAndAddress);
            userAnnotationSegments.setAnnotatorState(getModelObject());
            segments.add(userAnnotationSegments);
        }

        // update source list model only first time.
        sourceListModel = sourceListModel == null ? curationContainer.getCurationViews()
                : sourceListModel;

        suggestionViewPanel = new SuggestionViewPanel("suggestionViewPanel",
                new ListModel<>(segments))
        {
            private static final long serialVersionUID = 2583509126979792202L;

            @Override
            public void onChange(AjaxRequestTarget aTarget)
            {
                try {
                    // update begin/end of the curationsegment based on annotator state
                    // changes
                    // (like sentence change in auto-scroll mode,....
                    aTarget.addChildren(getPage(), IFeedback.class);
                    CurationPage.this.updatePanel(aTarget, curationContainer);
                }
                catch (UIMAException e) {
                    error(ExceptionUtils.getRootCause(e));
                }
                catch (ClassNotFoundException | AnnotationException | IOException e) {
                    error("Error: " + e.getMessage());
                }
            }
        };
        suggestionViewPanel.setOutputMarkupPlaceholderTag(true);
        suggestionViewPanel.add(visibleWhen(
                () -> getModelObject() != null && getModelObject().getDocument() != null));
        centerArea.add(suggestionViewPanel);

        editor = new AnnotationDetailEditorPanel("annotationDetailEditorPanel", this, getModel())
        {
            private static final long serialVersionUID = 2857345299480098279L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget)
            {
                aTarget.addChildren(getPage(), IFeedback.class);

                try {
                    updatePanel(aTarget, curationContainer);
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

            @Override
            protected void onAutoForward(AjaxRequestTarget aTarget)
            {
                annotationEditor.requestRender(aTarget);
            }

            @Override
            protected void onConfigure()
            {
                super.onConfigure();

                setEnabled(getModelObject() != null && getModelObject().getDocument() != null
                        && !documentService
                                .getSourceDocument(getModelObject().getDocument().getProject(),
                                        getModelObject().getDocument().getName())
                                .getState().equals(SourceDocumentState.CURATION_FINISHED));
            }

            @Override
            public CAS getEditorCas() throws IOException
            {
                return CurationPage.this.getEditorCas();
            }
        };
        sidebarCell.add(editor);

        annotationEditor = new BratAnnotationEditor("mergeView", getModel(), editor,
                this::getEditorCas);
        annotationEditor.setHighlightEnabled(false);
        annotationEditor.add(visibleWhen(
                () -> getModelObject() != null && getModelObject().getDocument() != null));
        annotationEditor.setOutputMarkupPlaceholderTag(true);
        // reset sentenceAddress and lastSentenceAddress to the orginal once
        centerArea.add(annotationEditor);

        // add container for sentences panel
        sentenceListContainer = new WebMarkupContainer("sentenceListContainer");
        sentenceListContainer.setOutputMarkupPlaceholderTag(true);
        sentenceListContainer.add(visibleWhen(
                () -> getModelObject() != null && getModelObject().getDocument() != null));
        add(sentenceListContainer);

        // add container for list of sentences panel
        sentenceLinksListView = new WebMarkupContainer("sentenceLinkListView");
        sentenceLinksListView.setOutputMarkupPlaceholderTag(true);
        sentenceLinksListView.add(new ListView<SourceListView>("sentenceLinkList",
                LoadableDetachableModel.of(() -> curationContainer.getCurationViews()))
        {
            private static final long serialVersionUID = 8539162089561432091L;

            @Override
            protected void populateItem(ListItem<SourceListView> item)
            {
                item.add(new SentenceLink("sentenceNumber", item.getModel()));
            }
        });

        sentenceListContainer.add(sentenceLinksListView);
    }

    /**
     * Re-render the document when the selection has changed.
     */
    @OnEvent
    public void onSelectionChangedEvent(SelectionChangedEvent aEvent)
    {
        actionRefreshDocument(aEvent.getRequestHandler());
    }

    public List<DecoratedObject<SourceDocument>> listDocuments(Project aProject, User aUser)
    {
        final List<DecoratedObject<SourceDocument>> allSourceDocuments = new ArrayList<>();
        List<SourceDocument> sdocs = curationDocumentService.listCuratableSourceDocuments(aProject);

        for (SourceDocument sourceDocument : sdocs) {
            DecoratedObject<SourceDocument> dsd = DecoratedObject.of(sourceDocument);
            dsd.setLabel("%s (%s)", sourceDocument.getName(), sourceDocument.getState());
            dsd.setColor(sourceDocument.getState().getColor());
            allSourceDocuments.add(dsd);
        }
        return allSourceDocuments;
    }

    public void onDocumentSelected(AjaxRequestTarget aTarget)
    {
        AnnotatorState state = getModelObject();
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        /*
         * Changed for #152, getDocument was returning null even after opening a document Also,
         * surrounded following code into if block to avoid error.
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
            }
            catch (Exception e) {
                LOG.error("Unable to load data", e);
                error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
            }
        }
    }

    @Override
    public IModel<List<DecoratedObject<Project>>> getAllowedProjects()
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
    public List<SourceDocument> getListOfDocs()
    {
        return curationDocumentService.listCuratableSourceDocuments(getModelObject().getProject());
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
    public void writeEditorCas(CAS aCas) throws IOException, AnnotationException
    {
        ensureIsEditable();

        AnnotatorState state = getModelObject();
        curationDocumentService.writeCurationCas(aCas, state.getDocument(), true);

        // Update timestamp in state
        Optional<Long> diskTimestamp = curationDocumentService
                .getCurationCasTimestamp(state.getDocument());
        if (diskTimestamp.isPresent()) {
            state.setAnnotationDocumentTimestamp(diskTimestamp.get());
        }
    }

    public void upgradeCasAndSave(SourceDocument aDocument, String aUsername) throws IOException
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
    public void actionLoadDocument(AjaxRequestTarget aTarget)
    {
        actionLoadDocument(aTarget, 0);
    }

    /**
     * Open a document or to a different document. This method should be used only the first time
     * that a document is accessed. It reset the annotator state and upgrades the CAS.
     */
    protected void actionLoadDocument(AjaxRequestTarget aTarget, int aFocus)
    {
        LOG.trace("BEGIN LOAD_DOCUMENT_ACTION at focus " + aFocus);

        AnnotatorState state = getModelObject();

        state.setUser(new User(CURATION_USER, Role.ROLE_USER));

        try {
            // Update source document state to CURRATION_INPROGRESS, if it was not
            // ANNOTATION_FINISHED
            if (!CURATION_FINISHED.equals(state.getDocument().getState())) {
                documentService.transitionSourceDocumentState(state.getDocument(),
                        ANNOTATION_IN_PROGRESS_TO_CURATION_IN_PROGRESS);
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

            CAS mergeCas = readOrCreateMergeCas(false, false);

            // (Re)initialize brat model after potential creating / upgrading CAS
            state.reset();

            // Initialize timestamp in state
            updateDocumentTimestampAfterWrite(state,
                    curationDocumentService.getCurationCasTimestamp(state.getDocument()));

            // Initialize the visible content
            state.moveToUnit(mergeCas, aFocus + 1, TOP);

            currentprojectId = state.getProject().getId();

            SuggestionBuilder builder = new SuggestionBuilder(documentService,
                    curationDocumentService, annotationService, userRepository);
            curationContainer = builder.buildCurationContainer(state);
            curationContainer.setState(state);
            editor.reset(aTarget);
            init(aTarget, curationContainer);

            // Re-render whole page as sidebar size preference may have changed
            if (aTarget != null) {
                WicketUtil.refreshPage(aTarget, getPage());
            }
        }
        catch (Exception e) {
            handleException(aTarget, e);
        }

        LOG.trace("END LOAD_DOCUMENT_ACTION");
    }

    public CAS readOrCreateMergeCas(boolean aMergeIncompleteAnnotations, boolean aForceRecreateCas)
        throws IOException, UIMAException, ClassNotFoundException, AnnotationException
    {
        AnnotatorState state = getModelObject();

        List<AnnotationDocument> finishedAnnotationDocuments = new ArrayList<>();

        // FIXME: This is slow and should be done via a proper SQL query
        for (AnnotationDocument annotationDocument : documentService
                .listAnnotationDocuments(state.getDocument())) {
            if (annotationDocument.getState().equals(FINISHED)) {
                finishedAnnotationDocuments.add(annotationDocument);
            }
        }

        if (finishedAnnotationDocuments.isEmpty()) {
            throw new IllegalStateException("This document has the state "
                    + state.getDocument().getState() + " but "
                    + "there are no finished annotation documents! This "
                    + "can for example happen when curation on a document has already started "
                    + "and afterwards all annotators have been remove from the project, have been "
                    + "disabled or if all were put back into " + AnnotationDocumentState.IN_PROGRESS
                    + " mode. It can "
                    + "also happen after importing a project when the users and/or permissions "
                    + "were not imported (only admins can do this via the projects page in the) "
                    + "administration dashboard and if none of the imported users have been "
                    + "enabled via the users management page after the import (also something "
                    + "that only administrators can do).");
        }

        AnnotationDocument randomAnnotationDocument = finishedAnnotationDocuments.get(0);

        SuggestionBuilder cb = new SuggestionBuilder(documentService, curationDocumentService,
                annotationService, userRepository);
        Map<String, CAS> casses = cb.listCassesforCuration(finishedAnnotationDocuments,
                state.getMode());
        CAS mergeCas = cb.getMergeCas(state, state.getDocument(), casses, randomAnnotationDocument,
                true, aMergeIncompleteAnnotations, aForceRecreateCas);
        return mergeCas;
    }

    @Override
    public void actionRefreshDocument(AjaxRequestTarget aTarget)
    {
        try {
            updatePanel(aTarget, curationContainer);
            aTarget.add(centerArea.get(MID_NUMBER_OF_PAGES));
        }
        catch (Exception e) {
            handleException(aTarget, e);
        }
    }

    @Override
    protected void handleParameters(StringValue aDocumentParameter, StringValue aFocusParameter,
            boolean aLockIfPreset)
    {
        Project project = getProject();

        SourceDocument document = getDocumentFromParameters(project, aDocumentParameter);

        AnnotatorState state = getModelObject();

        // If there is no change in the current document, then there is nothing to do. Mind
        // that document IDs are globally unique and a change in project does not happen unless
        // there is also a document change.
        if (document != null && document.equals(state.getDocument()) && aFocusParameter != null
                && aFocusParameter.toInt(0) == state.getFocusUnitIndex()) {
            return;
        }

        // Check access to project
        if (project != null
                && !projectService.isCurator(project, userRepository.getCurrentUser())) {
            error("You have no permission to access project [" + project.getId() + "]");
            return;
        }

        // Update project in state
        // Mind that this is relevant if the project was specified as a query parameter
        // i.e. not only in the case that it was a URL fragment parameter.
        state.setProject(project);
        if (aLockIfPreset) {
            state.setProjectLocked(true);
        }

        // If we arrive here and the document is not null, then we have a change of document
        // or a change of focus (or both)
        if (document != null && !document.equals(state.getDocument())) {
            state.setDocument(document, getListOfDocs());
        }
    }

    @Override
    protected void updateDocumentView(AjaxRequestTarget aTarget, SourceDocument aPreviousDocument,
            StringValue aFocusParameter)
    {
        SourceDocument currentDocument = getModelObject().getDocument();
        if (currentDocument == null) {
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
        if (aPreviousDocument != null && aPreviousDocument.equals(currentDocument)
                && focus == getModelObject().getFocusUnitIndex()) {
            return;
        }
        // If we arrive here and the document is not null, then we have a change of document
        // or a change of focus (or both)
        if (aPreviousDocument == null || !aPreviousDocument.equals(currentDocument)) {
            actionLoadDocument(aTarget, focus);
        }
        else {
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
    }

    public class SentenceLink
        extends AjaxLink<SourceListView>
    {
        private static final long serialVersionUID = 4558300090461815010L;

        public SentenceLink(String aId, IModel<SourceListView> aModel)
        {
            super(aId, aModel);
            setBody(Model.of(aModel.getObject().getSentenceNumber().toString()));
        }

        @Override
        protected void onComponentTag(ComponentTag aTag)
        {
            super.onComponentTag(aTag);

            final SourceListView curationViewItem = getModelObject();

            // Is in focus?
            if (curationViewItem.getSentenceNumber() == CurationPage.this.getModelObject()
                    .getFocusUnitIndex()) {
                aTag.append("class", "current", " ");
            }

            // Agree or disagree?
            String cC = curationViewItem.getSentenceState().getValue();
            if (cC != null) {
                aTag.append("class", "disagree", " ");
            }
            else {
                aTag.append("class", "agree", " ");
            }

            // In range or not?
            if (curationViewItem.getSentenceNumber() >= fSn
                    && curationViewItem.getSentenceNumber() <= lSn) {
                aTag.append("class", "in-range", " ");
            }
            else {
                aTag.append("class", "out-range", " ");
            }
        }

        @Override
        protected void onAfterRender()
        {
            super.onAfterRender();

            // The sentence list is refreshed using AJAX. Unfortunately, the renderHead() method
            // of the AjaxEventBehavior created by AjaxLink does not seem to be called by Wicket
            // during an AJAX rendering, causing the sentence links to loose their functionality.
            // Here, we ensure that the callback scripts are attached to the sentence links even
            // during AJAX updates.
            if (isEnabledInHierarchy()) {
                RequestCycle.get().find(AjaxRequestTarget.class).ifPresent(_target -> {
                    for (AjaxEventBehavior b : getBehaviors(AjaxEventBehavior.class)) {
                        _target.appendJavaScript(
                                WicketUtil.wrapInTryCatch(b.getCallbackScript().toString()));
                    }
                });
            }
        }

        @Override
        public void onClick(AjaxRequestTarget aTarget)
        {
            final SourceListView curationViewItem = getModelObject();
            curationView = curationViewItem;
            fSn = 0;
            try {
                AnnotatorState state = CurationPage.this.getModelObject();
                CAS cas = curationDocumentService.readCurationCas(state.getDocument());
                updateCurationView(curationContainer, curationViewItem, aTarget, cas);
                state.setFocusUnitIndex(curationViewItem.getSentenceNumber());
            }
            catch (IOException e) {
                error("Error: " + e.getMessage());
            }
        }
    }

    public void setModelObject(CurationContainer aModel)
    {
        setDefaultModelObject(aModel);
    }

    private void updateCurationView(final CurationContainer aCurationContainer,
            final SourceListView curationViewItem, AjaxRequestTarget aTarget, CAS aCas)
    {
        AnnotatorState state = CurationPage.this.getModelObject();
        state.getPagingStrategy().moveToOffset(state, aCas, curationViewItem.getBegin(), CENTERED);
        aCurationContainer.setState(state);
        onChange(aTarget);
    }

    protected void onChange(AjaxRequestTarget aTarget)
    {
        try {
            actionRefreshDocument(aTarget);
        }
        catch (Exception e) {
            handleException(aTarget, e);
        }
    }

    @Override
    public CAS getEditorCas() throws IOException
    {
        AnnotatorState state = CurationPage.this.getModelObject();

        if (state.getDocument() == null) {
            throw new IllegalStateException("Please open a document first!");
        }

        // If we have a timestamp, then use it to detect if there was a concurrent access
        verifyAndUpdateDocumentTimestamp(state,
                curationDocumentService.getCurationCasTimestamp(state.getDocument()));

        return curationDocumentService.readCurationCas(state.getDocument());
    }

    public void init(AjaxRequestTarget aTarget, CurationContainer aCC)
        throws UIMAException, ClassNotFoundException, IOException
    {
        commonUpdate();

        suggestionViewPanel.init(aTarget, aCC, annotationSelectionByUsernameAndAddress,
                curationView);
    }

    public void updatePanel(AjaxRequestTarget aTarget, CurationContainer aCC)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        commonUpdate();

        // Render the main annotation editor (upper part)
        annotationEditor.requestRender(aTarget);

        // Render the user annotation segments (lower part)
        suggestionViewPanel.requestUpdate(aTarget, aCC, annotationSelectionByUsernameAndAddress,
                curationView);

        // Render the sentence list sidebar
        aTarget.add(sentenceLinksListView);
    }

    private void commonUpdate() throws IOException
    {
        AnnotatorState state = CurationPage.this.getModelObject();

        curationView.setCurationBegin(state.getWindowBeginOffset());
        curationView.setCurationEnd(state.getWindowEndOffset());
        fSn = state.getFirstVisibleUnitIndex();
        lSn = state.getLastVisibleUnitIndex();
    }

    /**
     * Class for combining an on click ajax call and a label
     */
    class AjaxLabel
        extends Label
    {
        private static final long serialVersionUID = -4528869530409522295L;
        private AbstractAjaxBehavior click;

        public AjaxLabel(String id, String label, AbstractAjaxBehavior aClick)
        {
            super(id, label);
            click = aClick;
        }

        @Override
        public void onComponentTag(ComponentTag tag)
        {
            // add onclick handler to the browser
            // if clicked in the browser, the function
            // click.response(AjaxRequestTarget target) is called on the server side
            tag.put("ondblclick", "Wicket.Ajax.get({'u':'" + click.getCallbackUrl() + "'})");
            tag.put("onclick", "Wicket.Ajax.get({'u':'" + click.getCallbackUrl() + "'})");
        }
    }
}

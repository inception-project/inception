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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase.PAGE_PARAM_DOCUMENT;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiff;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;
import static de.tudarmstadt.ukp.inception.curation.service.CurationMergeMode.LOAD_ONLY;
import static de.tudarmstadt.ukp.inception.rendering.selection.FocusPosition.CENTERED;
import static de.tudarmstadt.ukp.inception.rendering.selection.FocusPosition.TOP;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.support.wicket.WicketUtil.refreshPage;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static org.apache.wicket.event.Broadcast.BREADTH;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.annotation.mount.MountPath;
import org.wicketstuff.event.annotation.OnEvent;
import org.wicketstuff.jquery.core.Options;
import org.wicketstuff.kendo.ui.widget.splitter.SplitterAdapter;
import org.wicketstuff.kendo.ui.widget.splitter.SplitterBehavior;
import org.springframework.security.access.AccessDeniedException;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBar;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.NotEditableException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.SentenceOrientedPagingStrategy;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratLineOrientedAnnotationEditorFactory;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratSentenceOrientedAnnotationEditorFactory;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiffSummaryState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.UndoKeyBindingsPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.DocumentNamePanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.AnnotationDetailEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.DetailPanelHostingPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.AnnotatorsPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.AnnotatorSegmentState;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.event.CurationUnitClickedEvent;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.overview.CurationUnit;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.overview.CurationUnitOverview;
import de.tudarmstadt.ukp.inception.annotation.events.AnnotationEvent;
import de.tudarmstadt.ukp.inception.curation.api.DiffAdapterRegistry;
import de.tudarmstadt.ukp.inception.curation.service.CurationDocumentService;
import de.tudarmstadt.ukp.inception.curation.service.CurationEditingService;
import de.tudarmstadt.ukp.inception.curation.service.CurationService;
import de.tudarmstadt.ukp.inception.documents.api.DocumentAccess;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorBase;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorRegistry;
import de.tudarmstadt.ukp.inception.editor.ContextMenuLookup;
import de.tudarmstadt.ukp.inception.editor.state.AnnotatorStateImpl;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationException;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DiamContext;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DocumentEditorManager;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VRange;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequestedEvent;
import de.tudarmstadt.ukp.inception.rendering.selection.EditorContentReplacedEvent;
import de.tudarmstadt.ukp.inception.rendering.selection.Selection;
import de.tudarmstadt.ukp.inception.rendering.selection.SelectionChangedEvent;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.inception.ui.curation.page.CuratableDocumentPage;
import de.tudarmstadt.ukp.inception.ui.curation.readiness.CurationReadinessBadgePanel;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;
import static java.util.Collections.emptyMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.flow.RedirectToUrlException;
import de.tudarmstadt.ukp.inception.support.wicket.UrlFragmentBehavior;

/**
 * This is the main class for the curation page. It contains an interface which displays differences
 * between user annotations for a specific document. The interface provides a tool for merging these
 * annotations and storing them as a new annotation.
 */
@MountPath(NS_PROJECT + "/${" + PAGE_PARAM_PROJECT + "}" + LegacyCurationPage.PAGE_PATH + "/#{"
        + PAGE_PARAM_DOCUMENT + "}")
public class LegacyCurationPage
    extends AnnotationPageBase
    implements DetailPanelHostingPage, DiamContext, DocumentEditorManager, CuratableDocumentPage
{
    public static final String PAGE_PATH = "/curate-split-legacy";

    private static final String MID_NUMBER_OF_PAGES = "numberOfPages";
    private static final String MID_UNDO_KEY_BINDINGS = "undoKeyBindings";

    private final static Logger LOG = LoggerFactory.getLogger(LegacyCurationPage.class);

    private static final long serialVersionUID = 1378872465851908515L;

    private @SpringBean DocumentService documentService;
    private @SpringBean CurationDocumentService curationDocumentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean ConstraintsService constraintsService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean UserDao userRepository;
    private @SpringBean DocumentAccess documentAccess;
    private @SpringBean WorkloadManagementService workloadManagementService;
    private @SpringBean CurationService curationService;
    private @SpringBean CurationEditingService curationEditingService;
    private @SpringBean AnnotationEditorRegistry editorRegistry;
    private @SpringBean DiffAdapterRegistry diffAdapterRegistry;

    private LoadableDetachableModel<String> annotationNotEditableReason = LoadableDetachableModel
            .of(this::loadAnnotationNotEditableReason);

    private long currentprojectId;

    // Open the dialog window on first load
    private boolean firstLoad = true;

    private WebMarkupContainer leftSidebar;
    private IModel<List<CurationUnit>> curationUnits;
    private CurationUnitOverview curationUnitOverview;

    private WebMarkupContainer rightSidebar;
    private AnnotationDetailEditorPanel detailEditor;

    private WebMarkupContainer centerArea;
    private WebMarkupContainer splitter;
    private AnnotationEditorBase annotationEditor;
    private AnnotatorsPanel annotatorsPanel;

    public LegacyCurationPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);

        // If the page was accessed using an URL form ending in a document ID, move the document ID
        // into the fragment and redirect to the form without it, so that links on the page do not
        // carry the document ID and we can switch documents via AJAX freely.
        //
        // This has to happen before anything else, because it restarts the request by throwing. It
        // used to live in AnnotationPageBase, where it ran during the super constructor call - i.e.
        // also before this point.
        //
        // This page always curates as the CURATION_USER, so unlike AnnotationPageBase2 it has no
        // data owner parameter to push.
        var params = getPageParameters();
        var documentParameter = params.get(PAGE_PARAM_DOCUMENT);
        if (!documentParameter.isEmpty()) {
            pushParametersIntoUrl(params, documentParameter);
        }

        LOG.debug("Setting up curation page with parameters: {}", aPageParameters);

        var state = new AnnotatorStateImpl(Mode.CURATION);
        setModel(Model.of(state));

        var user = userRepository.getCurrentUser();

        requireProjectRole(user, CURATOR);

        var document = aPageParameters.get(PAGE_PARAM_DOCUMENT);
        var focus = aPageParameters.get(PAGE_PARAM_FOCUS);

        handleParameters(document, focus, null);

        commonInit();

        updateDocumentView(null, null, null, focus);
    }

    private void commonInit()
    {
        // Ensure that a user is set
        getModelObject().setUser(userRepository.getCurationUser());
        getModelObject().setPagingStrategy(new SentenceOrientedPagingStrategy());
        curationUnits = new ListModel<>(new ArrayList<>());

        add(createUrlFragmentBehavior());

        centerArea = new WebMarkupContainer("centerArea");
        centerArea.add(visibleWhen(() -> getModelObject().getDocument() != null));
        centerArea.setOutputMarkupPlaceholderTag(true);
        add(centerArea);

        splitter = new WebMarkupContainer("splitter");
        splitter.setOutputMarkupId(true);
        centerArea.add(splitter);

        splitter.add(new DocumentNamePanel("documentNamePanel", getModel(), () -> isEditable()));
        splitter.add(new CurationReadinessBadgePanel("documentStatusBadges", getModel()));
        splitter.add(new ActionBar("actionBar", this));

        splitter.add(new SplitterBehavior("#" + splitter.getMarkupId(),
                new Options("orientation", Options.asString("vertical")), new SplitterAdapter()));

        var segments = new LinkedList<AnnotatorSegmentState>();

        var annotatorSegment = new AnnotatorSegmentState();
        annotatorSegment.setAnnotatorState(getModelObject());
        segments.add(annotatorSegment);

        annotatorsPanel = new AnnotatorsPanel("annotatorsPanel", this, new ListModel<>(segments));
        annotatorsPanel.setOutputMarkupPlaceholderTag(true);
        annotatorsPanel.add(visibleWhen(getModel().map(AnnotatorState::getDocument).isPresent()));
        splitter.add(annotatorsPanel);

        detailEditor = createDetailEditor("annotationDetailEditorPanel");
        rightSidebar = createRightSidebar("rightSidebar");
        rightSidebar.add(detailEditor);
        add(rightSidebar);

        annotationEditor = createAnnotationEditor("editor");
        splitter.add(annotationEditor);

        curationUnitOverview = new CurationUnitOverview("unitOverview", getModel(), curationUnits);

        leftSidebar = createLeftSidebar("leftSidebar");
        leftSidebar.add(curationUnitOverview);
        leftSidebar.add(new LambdaAjaxLink("refresh", this::actionRefresh));
        add(leftSidebar);

        add(new UndoKeyBindingsPanel(MID_UNDO_KEY_BINDINGS));
    }

    @Override
    public IModel<AnnotatorState> getStateModel()
    {
        return getModel();
    }

    private AnnotationEditorBase createAnnotationEditor(String aId)
    {
        var editorId = annotationService.isSentenceLayerEditable(getProject())
                ? BratLineOrientedAnnotationEditorFactory.ID
                : BratSentenceOrientedAnnotationEditorFactory.ID;
        var state = getModelObject();
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
        var editor = factory.create(aId, getModel(), this, this, this::getEditorCas);
        editor.add(visibleWhen(getModel().map(AnnotatorState::getDocument).isPresent()));
        editor.setOutputMarkupPlaceholderTag(true);

        // Give the new editor an opportunity to configure the current paging strategy, this does
        // not configure the paging for a document yet this would require loading the CAS which
        // might not have been upgraded yet
        factory.initState(state);

        // Use the proper position labels for the current paging strategy
        splitter.addOrReplace(getModelObject().getPagingStrategy()
                .createPositionLabel(MID_NUMBER_OF_PAGES, getModel())
                .add(visibleWhen(() -> getModelObject().getDocument() != null))
                .add(LambdaBehavior.onEvent(RenderRequestedEvent.class,
                        (c, e) -> e.getRequestHandler().add(c))));

        return editor;
    }

    private void actionRefresh(AjaxRequestTarget aTarget)
    {
        try {
            curationUnits.setObject(buildUnitOverview(getModelObject()));
            aTarget.add(leftSidebar);
        }
        catch (Exception e) {
            handleException(aTarget, e);
        }
    }

    private WebMarkupContainer createLeftSidebar(String aId)
    {
        var sidebar = new WebMarkupContainer("leftSidebar");
        sidebar.setOutputMarkupPlaceholderTag(true);
        sidebar.add(visibleWhen(
                () -> getModelObject() != null && getModelObject().getDocument() != null));
        // Override sidebar width from preferences
        sidebar.add(new AttributeModifier("style",
                () -> format("flex-basis: %s%%;",
                        getModelObject() != null
                                ? getModelObject().getPreferences().getSidebarSizeLeft()
                                : 10)));
        return sidebar;
    }

    private WebMarkupContainer createRightSidebar(String aId)
    {
        var sidebar = new WebMarkupContainer(aId);
        sidebar.setOutputMarkupPlaceholderTag(true);
        // Override sidebar width from preferences
        sidebar.add(new AttributeModifier("style",
                () -> format("flex-basis: %s%%;",
                        getModelObject() != null
                                ? getModelObject().getPreferences().getSidebarSizeRight()
                                : 10)));
        return sidebar;
    }

    private AnnotationDetailEditorPanel createDetailEditor(String aId)
    {
        var panel = new AnnotationDetailEditorPanel(aId, this, getModel());
        panel.add(enabledWhen(() -> getModelObject() != null //
                && getModelObject().getDocument() != null
                && !documentService
                        .getSourceDocument(getModelObject().getDocument().getProject(),
                                getModelObject().getDocument().getName())
                        .getState().equals(SourceDocumentState.CURATION_FINISHED)));
        return panel;
    }

    @OnEvent
    public void onAnnotationEvent(AnnotationEvent aEvent)
    {
        actionRefreshDocument(aEvent.getRequestTarget().orElse(null));
    }

    /**
     * Re-render the document when the selection has changed.
     * 
     * @param aEvent
     *            the event.
     */
    @OnEvent
    public void onSelectionChangedEvent(SelectionChangedEvent aEvent)
    {
        // Only react to selection changes in this page's own editor state, not in other editors /
        // panes hosted on the page (#6146).
        if (!aEvent.isFor(getModelObject())) {
            return;
        }

        actionRefreshDocument(aEvent.getRequestHandler());
    }

    @OnEvent
    public void onUnitClickedEvent(CurationUnitClickedEvent aEvent)
    {
        try {
            var state = getModelObject();
            var cas = curationDocumentService.readCurationCas(state.getDocument());
            state.getPagingStrategy().moveToOffset(state, cas, aEvent.getUnit().getBegin(),
                    CENTERED);
            state.setFocusUnitIndex(aEvent.getUnit().getUnitIndex());

            actionRefreshDocument(aEvent.getTarget());
        }
        catch (Exception e) {
            handleException(aEvent.getTarget(), e);
        }
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
        var state = getModelObject();
        // Since the curatable documents depend on the document state, let's make sure the document
        // state is up-to-date
        workloadManagementService.getWorkloadManagerExtension(state.getProject())
                .freshenStatus(state.getProject());
        return curationDocumentService.listCuratableSourceDocuments(state.getProject());
    }

    /**
     * for the first time, open the <b>open document dialog</b>
     */
    @Override
    public void renderHead(IHeaderResponse response)
    {
        super.renderHead(response);

        if (firstLoad) {
            response.render(OnLoadHeaderItem
                    .forScript("jQuery('#showOpenDocumentModal').trigger('click');"));
            firstLoad = false;
        }
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
            curationDocumentService
                    .verifyCurationCasTimestamp(state.getDocument(),
                            state.getAnnotationDocumentTimestamp().get(), "reading")
                    .ifPresent(state::setAnnotationDocumentTimestamp);
        }

        return curationDocumentService.readCurationCas(state.getDocument());
    }

    @Override
    public void writeEditorCas(CAS aCas) throws IOException, AnnotationException
    {
        ensureIsEditable();

        var state = getModelObject();
        curationDocumentService.writeCurationCas(aCas, state.getDocument(), true);

        // Update timestamp in state
        curationDocumentService.getCurationCasTimestamp(state.getDocument())
                .ifPresent(state::setAnnotationDocumentTimestamp);
    }

    @Override
    public void writeEditorCas() throws IOException, AnnotationException
    {
        writeEditorCas(getEditorCas());
    }

    @Override
    public Optional<DiamContext> getActiveContext()
    {
        return Optional.of(this);
    }

    @Override
    public boolean hasEditor()
    {
        return getModelObject().getDocument() != null;
    }

    @Override
    public void setActiveContext(AjaxRequestTarget aTarget, DiamContext aContext)
    {
        // The context is fixed to this page, so there is nothing to switch to.
    }

    @Override
    public void actionShowDocument(AjaxRequestTarget aTarget, SourceDocument aDocument, int aBegin,
            int aEnd, List<VRange> aAdditionalPingRanges)
        throws IOException, AnnotationException
    {
        ensureIsAccessible(aDocument);

        actionShowSelectedDocument(aTarget, aDocument, aBegin, aEnd, aAdditionalPingRanges);
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
        // Page is its own single editor - it is either showing the document or nothing is.
        var state = getModelObject();
        if (!Objects.equals(state.getDocument(), aDocument)
                || !Objects.equals(state.getDataOwner(), aDataOwner)) {
            return Optional.empty();
        }

        return Optional.of(this);
    }

    @Override
    public DiamContext resolveEditorFor(SourceDocument aDocument)
    {
        return this;
    }

    @Override
    public DocumentEditorManager getDocumentEditorManager()
    {
        return this;
    }

    @Override
    public AnnotationActionHandler getActionHandler()
    {
        return this;
    }

    @Override
    public AnnotationDetailEditorPanel getDetailEditor()
    {
        return detailEditor;
    }

    @Override
    public Selection selectionFor(VID aVid, AnnotationFS aAnnotation)
    {
        return annotationService.findAdapter(getProject(), aAnnotation).select(aVid, aAnnotation);
    }

    @Override
    public void actionOpenDocument(AjaxRequestTarget aTarget, SourceDocument aDocument)
        throws AnnotationException
    {
        var docs = getListOfDocs();
        if (!docs.contains(aDocument)) {
            throw new AnnotationException(
                    "Document [" + aDocument.getName() + "] is not accessible.");
        }

        // Keep the full list on the state so the position label stays "[doc i / n]"-consistent.
        getModelObject().setDocument(aDocument, docs);
        actionLoadDocument(aTarget);
    }

    @Override
    public void actionLoadDocument(AjaxRequestTarget aTarget)
    {
        actionLoadDocument(aTarget, 0);
    }

    /**
     * Open a document. This method should be used only the first time that a document is accessed.
     * It resets the editor state and upgrades the CAS.
     */
    private void actionLoadDocument(AjaxRequestTarget aTarget, int aFocus)
    {
        LOG.trace("BEGIN LOAD_DOCUMENT_ACTION at focus " + aFocus);

        try {
            var state = getModelObject();

            if (state.getProject() != null) {
                state.setProject(projectService.getProject(state.getProject().getId()));
            }

            state.refreshDocument(documentService);

            var project = state.getProject();
            state.setUser(userRepository.getCurationUser());
            state.reset();

            // Load constraints
            state.setConstraints(constraintsService.getMergedConstraints(project));

            // Load user preferences
            loadPreferences();

            // if project is changed, reset some project specific settings
            if (currentprojectId != project.getId()) {
                state.clearRememberedFeatures();
                currentprojectId = project.getId();
            }

            var mergeCas = readOrCreateCurationCas(state.getDocument(),
                    curationService.getDefaultMergeStrategy(project), LOAD_ONLY);

            // Initialize timestamp in state
            curationDocumentService.getCurationCasTimestamp(state.getDocument())
                    .ifPresent(state::setAnnotationDocumentTimestamp);

            // Initialize the visible content
            state.moveToUnit(mergeCas, aFocus + 1, TOP);

            curationUnits.setObject(buildUnitOverview(state));

            send(this, BREADTH, new EditorContentReplacedEvent(state, aTarget));

            annotatorsPanel.init(aTarget, getModelObject());

            // Re-render whole page as sidebar size preference may have changed
            if (aTarget != null) {
                refreshPage(aTarget, getPage());
            }
        }
        catch (Exception e) {
            handleException(aTarget, e);
        }

        LOG.trace("END LOAD_DOCUMENT_ACTION");
    }

    @Override
    public CurationEditingService getCurationEditingService()
    {
        return curationEditingService;
    }

    @Override
    public void actionRefreshDocument(AjaxRequestTarget aTarget)
    {
        try {
            annotationEditor.requestRender(aTarget);
            annotatorsPanel.requestRender(aTarget, getModelObject());
            aTarget.add(curationUnitOverview);
            aTarget.add(splitter.get(MID_NUMBER_OF_PAGES));
        }
        catch (Exception e) {
            handleException(aTarget, e);
        }
    }

    protected void handleParameters(StringValue aDocumentParameter, StringValue aFocusParameter,
            StringValue aUser)
    {
        var project = getProject();
        var document = getDocumentFromParameters(project, aDocumentParameter);
        var state = getModelObject();

        // If there is no change in the current document, then there is nothing to do. Mind
        // that document IDs are globally unique and a change in project does not happen unless
        // there is also a document change.
        if (document != null && document.equals(state.getDocument()) && aFocusParameter != null
                && aFocusParameter.toInt(0) == state.getFocusUnitIndex()) {
            return;
        }

        // Check access to project
        if (project != null
                && !projectService.hasRole(userRepository.getCurrentUser(), project, CURATOR)) {
            getSession()
                    .error("You have no permission to access project [" + project.getId() + "]");
            backToProjectPage();
            return;
        }

        // Update project in state
        // Mind that this is relevant if the project was specified as a query parameter
        // i.e. not only in the case that it was a URL fragment parameter.
        state.setProject(project);

        // If we arrive here and the document is not null, then we have a change of document
        // or a change of focus (or both)
        if (document != null && !document.equals(state.getDocument())) {
            state.setDocument(document, getListOfDocs());

            if (state.getDocumentIndex() == -1) {
                getSession().error("The document [" + document.getName() + "] is not curatable");
                backToProjectPage();
                return;
            }
        }
    }

    protected void updateDocumentView(AjaxRequestTarget aTarget, SourceDocument aPreviousDocument,
            User aPreviousUser, StringValue aFocusParameter)
    {
        var currentDocument = getModelObject().getDocument();
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
            LOG.error("Error reading CAS " + e.getMessage(), e);
            error("Error reading CAS " + e.getMessage());
        }
    }

    private List<CurationUnit> buildUnitOverview(AnnotatorState aState)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        // get annotation documents
        var document = aState.getDocument();
        var curatableUsers = curationDocumentService.listCuratableUsers(document);
        var casses = documentService.readAllCasesSharedNoUpgrade(aState.getDocument(),
                curatableUsers);

        var editorCas = curationEditingService.readCurationCas(document,
                aState.getUser().getUsername(), casses, null, false, aState.getAnnotationLayers(),
                curationService.getDefaultMergeStrategy(getProject()), LOAD_ONLY);

        curationDocumentService.getCurationCasTimestamp(document)
                .ifPresent(aState::setAnnotationDocumentTimestamp);

        casses.put(CURATION_USER, editorCas);

        var adapters = diffAdapterRegistry.getDiffAdapters(aState.getAnnotationLayers());

        var diffStart = System.currentTimeMillis();
        LOG.debug("Calculating differences...");
        var unitIndex = 0;
        var curationUnitList = new ArrayList<CurationUnit>();
        var units = aState.getPagingStrategy().units(editorCas);
        for (var unit : units) {
            unitIndex++;
            if (unitIndex % 100 == 0) {
                LOG.debug("Processing differences: {} of {} units...", unitIndex, units.size());
            }

            var diff = doDiff(adapters, casses, unit.getBegin(), unit.getEnd()).toResult();

            var curationUnit = new CurationUnit(unit.getBegin(), unit.getEnd(), unitIndex);
            curationUnit.setState(CasDiffSummaryState.calculateState(diff));

            curationUnitList.add(curationUnit);
        }
        LOG.debug("Difference calculation completed in {}ms", (currentTimeMillis() - diffStart));

        return curationUnitList;
    }

    @Override
    public Optional<ContextMenuLookup> getContextMenuLookup()
    {
        return annotationEditor.getContextMenuLookup();
    }

    private void pushParametersIntoUrl(PageParameters aParams, StringValue aDocumentParameter)
    {
        var requestCycle = getRequestCycle();

        aParams.remove(PAGE_PARAM_DOCUMENT);

        var url = Url.parse(requestCycle.urlFor(this.getClass(), aParams));
        var finalUrl = requestCycle.getUrlRenderer().renderFullUrl(url) + "#!"
                + format("%s=%s", PAGE_PARAM_DOCUMENT, aDocumentParameter.toString());
        LOG.trace("Pushing parameter for document [{}] into fragment: {} (URL redirect)",
                aDocumentParameter, finalUrl);
        throw new RedirectToUrlException(finalUrl.toString());
    }

    /**
     * Create the behavior which reacts to URL fragment changes.
     * <p>
     * Note this page does <em>not</em> push state back into the URL fragment - it never called the
     * inherited {@code updateUrlFragment}, so switching documents in the page leaves the fragment
     * showing the document it was opened with. Preserved as-is; see the note on independent
     * evolution of the URL handling.
     *
     * @return the behavior. It has not been added to the page yet.
     */
    protected UrlFragmentBehavior createUrlFragmentBehavior()
    {
        return new UrlFragmentBehavior(this::getUrlFragmentParameters,
                this::onUrlFragmentParameterArrival);
    }

    private void onUrlFragmentParameterArrival(IRequestParameters aRequestParameters,
            AjaxRequestTarget aTarget)
    {
        var document = aRequestParameters.getParameterValue(PAGE_PARAM_DOCUMENT);
        var focus = aRequestParameters.getParameterValue(PAGE_PARAM_FOCUS);

        if (document.isEmpty() && focus.isEmpty()) {
            return;
        }

        LOG.trace("URL fragment update: {} focus {}", document, focus);

        var previousDoc = getModelObject().getDocument();
        var previousUser = getModelObject().getUser();

        handleParameters(document, focus, null);

        updateDocumentView(aTarget, previousDoc, previousUser, focus);
    }

    /**
     * @return the parameters that the URL fragment should carry for the current state. Parameters
     *         mapped to {@code null} are removed from the URL fragment. The data owner is always
     *         the CURATION_USER on this page, so it is never put into the fragment.
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

        return parameters;
    }

    private String loadAnnotationNotEditableReason()
    {
        try {
            var state = getModelObject();
            var sessionOwner = userRepository.getCurrentUser();
            documentAccess.assertCanEditAnnotationDocument(sessionOwner, state.getDocument(),
                    state.getUser().getUsername());
            return null;
        }
        catch (AccessDeniedException e) {
            return e.getMessage();
        }
    }

    /**
     * @deprecated The page should no longer know about editors. Use {@link #getActiveContext()}
     *             instead.
     */
    @Deprecated
    public void ensureIsEditable() throws NotEditableException
    {
        var state = getModelObject();

        if (state.getDocument() == null) {
            throw new NotEditableException("No document selected");
        }

        var notEditableReason = annotationNotEditableReason.getObject();
        if (notEditableReason != null) {
            throw new NotEditableException(notEditableReason);
        }
    }

    /**
     * Discard the cached editability verdict so that the next {@link #isEditable()} or
     * {@link #ensureIsEditable()} re-evaluates it.
     * 
     * @deprecated The page should no longer know about editors. Use {@link #getActiveContext()}
     *             instead.
     */
    @Deprecated
    protected void clearIsEditableCache()
    {
        annotationNotEditableReason.detach();
    }

    public boolean isEditable()
    {
        try {
            ensureIsEditable();
            return true;
        }
        catch (NotEditableException e) {
            return false;
        }
    }

    @Override
    public void detachModels()
    {
        super.detachModels();
        annotationNotEditableReason.detach();
    }
}

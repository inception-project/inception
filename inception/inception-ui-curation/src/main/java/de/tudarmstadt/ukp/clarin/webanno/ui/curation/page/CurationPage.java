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

import static de.tudarmstadt.ukp.clarin.webanno.api.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.api.CasUpgradeMode.FORCE_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateUtils.updateDocumentTimestampAfterWrite;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateUtils.verifyAndUpdateDocumentTimestamp;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase.PAGE_PARAM_DOCUMENT;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.FocusPosition.CENTERED;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.FocusPosition.TOP;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.UNMANAGED_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiffSingle;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.getDiffAdapters;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.LinkCompareBehavior.LINK_ROLE_AS_LABEL;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition.ANNOTATION_IN_PROGRESS_TO_CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketUtil.refreshPage;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.curation.overview.CurationUnitState.AGREE;
import static de.tudarmstadt.ukp.clarin.webanno.ui.curation.overview.CurationUnitState.CURATED;
import static de.tudarmstadt.ukp.clarin.webanno.ui.curation.overview.CurationUnitState.DISAGREE;
import static de.tudarmstadt.ukp.clarin.webanno.ui.curation.overview.CurationUnitState.INCOMPLETE;
import static de.tudarmstadt.ukp.clarin.webanno.ui.curation.overview.CurationUnitState.STACKED;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.wicketstuff.annotation.mount.MountPath;
import org.wicketstuff.event.annotation.OnEvent;

import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.kendo.ui.widget.splitter.SplitterAdapter;
import com.googlecode.wicket.kendo.ui.widget.splitter.SplitterBehavior;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBar;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.AnnotationEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorStateImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.SentenceOrientedPagingStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.Unit;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.event.RenderAnnotationsEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.event.SelectionChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotationEditor;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.ConfigurationSet;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.DiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.curation.casmerge.CasMerge;
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
import de.tudarmstadt.ukp.clarin.webanno.support.StopWatch;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.DecoratedObject;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component.DocumentNamePanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.AnnotationDetailEditorPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.AnnotatorsPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model.AnnotatorSegment;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.event.CurationUnitClickedEvent;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.overview.CurationUnit;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.overview.CurationUnitOverviewLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.overview.CurationUnitState;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

/**
 * This is the main class for the curation page. It contains an interface which displays differences
 * between user annotations for a specific document. The interface provides a tool for merging these
 * annotations and storing them as a new annotation.
 */
@MountPath(NS_PROJECT + "/${" + PAGE_PARAM_PROJECT + "}/curate/#{" + PAGE_PARAM_DOCUMENT + "}")
public class CurationPage
    extends AnnotationPageBase
{
    private static final long serialVersionUID = 1378872465851908515L;

    private static final String MID_NUMBER_OF_PAGES = "numberOfPages";

    private @SpringBean DocumentService documentService;
    private @SpringBean CurationDocumentService curationDocumentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean ConstraintsService constraintsService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean UserDao userRepository;
    private @SpringBean WorkloadManagementService workloadManagementService;

    private long currentprojectId;

    // Open the dialog window on first load
    private boolean firstLoad = true;

    private WebMarkupContainer leftSidebar;
    private IModel<List<CurationUnit>> curationUnits;
    private WebMarkupContainer curationUnitOverview;

    private WebMarkupContainer rightSidebar;
    private AnnotationDetailEditorPanel detailPanel;

    private WebMarkupContainer centerArea;
    private WebMarkupContainer splitter;
    private AnnotationEditorBase annotationEditor;
    private AnnotatorsPanel annotatorsPanel;

    private CurationUnit focussedUnit;

    public CurationPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);

        LOG.debug("Setting up curation page with parameters: {}", aPageParameters);

        AnnotatorState state = new AnnotatorStateImpl(Mode.CURATION);
        setModel(Model.of(state));

        User user = userRepository.getCurrentUser();

        requireProjectRole(user, CURATOR);

        StringValue document = aPageParameters.get(PAGE_PARAM_DOCUMENT);
        StringValue focus = aPageParameters.get(PAGE_PARAM_FOCUS);

        handleParameters(document, focus, null, true);

        commonInit();

        updateDocumentView(null, null, null, focus);
    }

    private void commonInit()
    {
        // Ensure that a user is set
        getModelObject().setUser(new User(CURATION_USER, Role.ROLE_USER));
        getModelObject().setPagingStrategy(new SentenceOrientedPagingStrategy());
        curationUnits = new ListModel<>(new ArrayList<>());
        focussedUnit = new CurationUnit();

        add(createUrlFragmentBehavior());

        centerArea = new WebMarkupContainer("centerArea");
        centerArea.add(visibleWhen(() -> getModelObject().getDocument() != null));
        centerArea.setOutputMarkupPlaceholderTag(true);
        add(centerArea);

        splitter = new WebMarkupContainer("splitter");
        splitter.setOutputMarkupId(true);
        centerArea.add(splitter);

        splitter.add(new DocumentNamePanel("documentNamePanel", getModel()));
        splitter.add(new ActionBar("actionBar"));

        splitter.add(new SplitterBehavior("#" + splitter.getMarkupId(),
                new Options("orientation", Options.asString("vertical")), new SplitterAdapter()));

        splitter.add(getModelObject().getPagingStrategy()
                .createPositionLabel(MID_NUMBER_OF_PAGES, getModel())
                .add(visibleWhen(() -> getModelObject().getDocument() != null))
                .add(LambdaBehavior.onEvent(RenderAnnotationsEvent.class,
                        (c, e) -> e.getRequestHandler().add(c))));

        List<AnnotatorSegment> segments = new LinkedList<>();

        if (getModelObject() != null) {
            AnnotatorSegment annotatorSegment = new AnnotatorSegment();
            annotatorSegment.setAnnotatorState(getModelObject());
            segments.add(annotatorSegment);
        }

        annotatorsPanel = new AnnotatorsPanel("annotatorsPanel", new ListModel<>(segments));
        annotatorsPanel.setOutputMarkupPlaceholderTag(true);
        annotatorsPanel.add(visibleWhen(
                () -> getModelObject() != null && getModelObject().getDocument() != null));
        splitter.add(annotatorsPanel);

        rightSidebar = makeRightSidebar("rightSidebar");
        rightSidebar
                .add(detailPanel = makeAnnotationDetailEditorPanel("annotationDetailEditorPanel"));
        add(rightSidebar);

        annotationEditor = new BratAnnotationEditor("annotationEditor", getModel(), detailPanel,
                this::getEditorCas);
        annotationEditor.setHighlightEnabled(false);
        annotationEditor.add(visibleWhen(
                () -> getModelObject() != null && getModelObject().getDocument() != null));
        annotationEditor.setOutputMarkupPlaceholderTag(true);
        splitter.add(annotationEditor);

        curationUnitOverview = new WebMarkupContainer("unitOverview");
        curationUnitOverview.setOutputMarkupPlaceholderTag(true);
        curationUnitOverview.add(new ListView<CurationUnit>("unit", curationUnits)
        {
            private static final long serialVersionUID = 8539162089561432091L;

            @Override
            protected void populateItem(ListItem<CurationUnit> item)
            {
                item.add(new CurationUnitOverviewLink("label", item.getModel(),
                        CurationPage.this.getModel()));
            }
        });

        leftSidebar = makeLeftSidebar("leftSidebar");
        leftSidebar.add(curationUnitOverview);
        leftSidebar.add(new LambdaAjaxLink("refresh", this::actionRefresh));
        add(leftSidebar);
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

    private WebMarkupContainer makeLeftSidebar(String aId)
    {
        WebMarkupContainer sidebar = new WebMarkupContainer("leftSidebar");
        sidebar.setOutputMarkupPlaceholderTag(true);
        sidebar.add(visibleWhen(
                () -> getModelObject() != null && getModelObject().getDocument() != null));
        // Override sidebar width from preferences
        sidebar.add(new AttributeModifier("style",
                () -> format("flex-basis: %d%%;",
                        getModelObject() != null
                                ? getModelObject().getPreferences().getSidebarSizeLeft()
                                : 10)));
        return sidebar;
    }

    private WebMarkupContainer makeRightSidebar(String aId)
    {
        WebMarkupContainer sidebar = new WebMarkupContainer(aId);
        sidebar.setOutputMarkupPlaceholderTag(true);
        // Override sidebar width from preferences
        sidebar.add(new AttributeModifier("style",
                () -> format("flex-basis: %d%%;",
                        getModelObject() != null
                                ? getModelObject().getPreferences().getSidebarSizeRight()
                                : 10)));
        return sidebar;
    }

    private AnnotationDetailEditorPanel makeAnnotationDetailEditorPanel(String aId)
    {
        AnnotationDetailEditorPanel panel = new AnnotationDetailEditorPanel(aId, this, getModel())
        {
            private static final long serialVersionUID = 2857345299480098279L;

            @Override
            protected void onAutoForward(AjaxRequestTarget aTarget)
            {
                annotationEditor.requestRender(aTarget);
            }

            @Override
            public CAS getEditorCas() throws IOException
            {
                return CurationPage.this.getEditorCas();
            }
        };
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
        if (aEvent.getRequestTarget() != null) {
            actionRefreshDocument(aEvent.getRequestTarget());
        }
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
        actionRefreshDocument(aEvent.getRequestHandler());
    }

    @OnEvent
    public void onUnitClickedEvent(CurationUnitClickedEvent aEvent)
    {
        focussedUnit = aEvent.getUnit();
        try {
            AnnotatorState state = CurationPage.this.getModelObject();
            CAS cas = curationDocumentService.readCurationCas(state.getDocument());
            state.getPagingStrategy().moveToOffset(state, cas, focussedUnit.getBegin(), CENTERED);
            state.setFocusUnitIndex(focussedUnit.getUnitIndex());

            actionRefreshDocument(aEvent.getTarget());
        }
        catch (Exception e) {
            handleException(aEvent.getTarget(), e);
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
                User user = userRepository.getCurrentUser();
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
        AnnotatorState state = getModelObject();
        // Since the curatable documents depend on the document state, let's make sure the document
        // state is up-to-date
        workloadManagementService.getWorkloadManagerExtension(state.getProject())
                .freshenStatus(state.getProject());
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
    public CAS getEditorCas() throws IOException
    {
        AnnotatorState state = getModelObject();

        if (state.getDocument() == null) {
            throw new IllegalStateException("Please open a document first!");
        }

        // If we have a timestamp, then use it to detect if there was a concurrent access
        verifyAndUpdateDocumentTimestamp(state,
                curationDocumentService.getCurationCasTimestamp(state.getDocument()));

        return curationDocumentService.readCurationCas(state.getDocument());
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

    @Override
    public void actionLoadDocument(AjaxRequestTarget aTarget)
    {
        actionLoadDocument(aTarget, 0);
    }

    /**
     * Open a document or to a different document. This method should be used only the first time
     * that a document is accessed. It reset the annotator state and upgrades the CAS.
     */
    private void actionLoadDocument(AjaxRequestTarget aTarget, int aFocus)
    {
        LOG.trace("BEGIN LOAD_DOCUMENT_ACTION at focus " + aFocus);

        AnnotatorState state = getModelObject();

        state.setUser(new User(CURATION_USER, Role.ROLE_USER));

        try {
            // Update source document state to CURRATION_INPROGRESS, if it was not
            // CURATION_FINISHED
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

            curationUnits.setObject(buildUnitOverview(state));
            detailPanel.reset(aTarget);
            commonUpdate();

            annotatorsPanel.init(aTarget, getModelObject(), focussedUnit);

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

    public CAS readOrCreateMergeCas(boolean aMergeIncompleteAnnotations, boolean aForceRecreateCas)
        throws IOException, UIMAException, ClassNotFoundException, AnnotationException
    {
        AnnotatorState state = getModelObject();

        List<AnnotationDocument> finishedAnnotationDocuments = documentService
                .listFinishedAnnotationDocuments(state.getDocument());

        if (finishedAnnotationDocuments.isEmpty()) {
            getSession().error("This document has the state " + state.getDocument().getState()
                    + " but " + "there are no finished annotation documents! This "
                    + "can for example happen when curation on a document has already started "
                    + "and afterwards all annotators have been remove from the project, have been "
                    + "disabled or if all were put back into " + AnnotationDocumentState.IN_PROGRESS
                    + " mode. It can "
                    + "also happen after importing a project when the users and/or permissions "
                    + "were not imported (only admins can do this via the projects page in the) "
                    + "administration dashboard and if none of the imported users have been "
                    + "enabled via the users management page after the import (also something "
                    + "that only administrators can do).");
            backToProjectPage();
        }

        AnnotationDocument randomAnnotationDocument = finishedAnnotationDocuments.get(0);

        Map<String, CAS> casses = readAllCasesSharedNoUpgrade(finishedAnnotationDocuments);
        CAS mergeCas = readCurationCas(state, state.getDocument(), casses, randomAnnotationDocument,
                true, aMergeIncompleteAnnotations, aForceRecreateCas);

        return mergeCas;
    }

    @Override
    public void actionRefreshDocument(AjaxRequestTarget aTarget)
    {
        try {
            commonUpdate();
            annotationEditor.requestRender(aTarget);
            annotatorsPanel.requestRender(aTarget, getModelObject(), focussedUnit);
            aTarget.add(curationUnitOverview);
            aTarget.add(splitter.get(MID_NUMBER_OF_PAGES));
        }
        catch (Exception e) {
            handleException(aTarget, e);
        }
    }

    @Override
    protected void handleParameters(StringValue aDocumentParameter, StringValue aFocusParameter,
            StringValue aUser, boolean aLockIfPreset)
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
            getSession()
                    .error("You have no permission to access project [" + project.getId() + "]");
            backToProjectPage();
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

            if (state.getDocumentIndex() == -1) {
                getSession().error("The document [" + document.getName() + "] is not curatable");
                backToProjectPage();
                return;
            }
        }
    }

    @Override
    protected void updateDocumentView(AjaxRequestTarget aTarget, SourceDocument aPreviousDocument,
            User aPreviousUser, StringValue aFocusParameter)
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

    private void commonUpdate()
    {
        AnnotatorState state = CurationPage.this.getModelObject();

        focussedUnit.setCurationBegin(state.getWindowBeginOffset());
        focussedUnit.setCurationEnd(state.getWindowEndOffset());
    }

    @Override
    public List<DecoratedObject<SourceDocument>> listAccessibleDocuments(Project aProject,
            User aUser)
    {
        final List<DecoratedObject<SourceDocument>> allSourceDocuments = new ArrayList<>();
        List<SourceDocument> sdocs = getListOfDocs();

        for (SourceDocument sourceDocument : sdocs) {
            DecoratedObject<SourceDocument> dsd = DecoratedObject.of(sourceDocument);
            dsd.setLabel("%s (%s)", sourceDocument.getName(), sourceDocument.getState());
            dsd.setColor(sourceDocument.getState().getColor());
            allSourceDocuments.add(dsd);
        }
        return allSourceDocuments;
    }

    private List<CurationUnit> buildUnitOverview(AnnotatorState aState)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        // get annotation documents
        Map<String, CAS> casses = readAllCasesSharedNoUpgrade(
                documentService.listFinishedAnnotationDocuments(aState.getDocument()));

        CAS editorCas = readCurationCas(aState, aState.getDocument(), casses, null, false, false,
                false);

        casses.put(CURATION_USER, editorCas);

        List<DiffAdapter> adapters = getDiffAdapters(annotationService,
                aState.getAnnotationLayers());

        long diffStart = System.currentTimeMillis();
        LOG.debug("Calculating differences...");
        int unitIndex = 0;
        List<CurationUnit> curationUnitList = new ArrayList<>();
        List<Unit> units = aState.getPagingStrategy().units(editorCas);
        for (Unit unit : units) {
            unitIndex++;
            if (unitIndex % 100 == 0) {
                LOG.debug("Processing differences: {} of {} units...", unitIndex, units.size());
            }

            DiffResult diff = doDiffSingle(adapters, LINK_ROLE_AS_LABEL, casses, unit.getBegin(),
                    unit.getEnd()).toResult();

            CurationUnit curationUnit = new CurationUnit(unit.getBegin(), unit.getEnd(), unitIndex);

            curationUnit.setState(calculateState(diff));

            curationUnitList.add(curationUnit);
        }
        LOG.debug("Difference calculation completed in {}ms", (currentTimeMillis() - diffStart));

        return curationUnitList;
    }

    private CurationUnitState calculateState(DiffResult diff)
    {
        if (!diff.hasDifferences() && diff.getIncompleteConfigurationSets().isEmpty()) {
            return AGREE;
        }

        boolean allCurated = true;
        curatedDiffSet: for (ConfigurationSet d : diff.getConfigurationSets()) {
            if (!d.getCasGroupIds().contains(CURATION_USER)) {
                allCurated = false;
                break curatedDiffSet;
            }
        }

        if (allCurated) {
            return CURATED;
        }

        // Is this confSet a diff due to stacked annotations (with same configuration)?
        boolean stackedDiff = false;
        stackedDiffSet: for (ConfigurationSet d : diff.getDifferingConfigurationSets().values()) {
            for (String user : d.getCasGroupIds()) {
                if (d.getConfigurations(user).size() > 1) {
                    stackedDiff = true;
                    break stackedDiffSet;
                }
            }
        }

        if (stackedDiff) {
            return STACKED;
        }

        Set<String> usersExceptCurator = new HashSet<>(diff.getCasGroupIds());
        usersExceptCurator.remove(CURATION_USER);
        for (ConfigurationSet d : diff.getIncompleteConfigurationSets().values()) {
            if (!d.getCasGroupIds().containsAll(usersExceptCurator)) {
                return INCOMPLETE;
            }
        }

        return DISAGREE;
    }

    private Map<String, CAS> readAllCasesSharedNoUpgrade(List<AnnotationDocument> aDocuments)
        throws IOException
    {
        Map<String, CAS> casses = new HashMap<>();
        for (AnnotationDocument annDoc : aDocuments) {
            String username = annDoc.getUser();
            CAS cas = documentService.readAnnotationCas(annDoc.getDocument(), username,
                    AUTO_CAS_UPGRADE, SHARED_READ_ONLY_ACCESS);
            casses.put(username, cas);
        }
        return casses;
    }

    /**
     * Fetches the CAS that the user will be able to edit. In AUTOMATION/CORRECTION mode, this is
     * the CAS for the CORRECTION_USER and in CURATION mode it is the CAS for the CURATION user.
     *
     * @param aState
     *            the model.
     * @param aDocument
     *            the source document.
     * @param aCasses
     *            the CASes.
     * @param aTemplate
     *            an annotation document which is used as a template for the new merge CAS.
     * @return the CAS.
     * @throws UIMAException
     *             hum?
     * @throws ClassNotFoundException
     *             hum?
     * @throws IOException
     *             if an I/O error occurs.
     * @throws AnnotationException
     *             hum?
     */
    private CAS readCurationCas(AnnotatorState aState, SourceDocument aDocument,
            Map<String, CAS> aCasses, AnnotationDocument aTemplate, boolean aUpgrade,
            boolean aMergeIncompleteAnnotations, boolean aForceRecreateCas)
        throws UIMAException, ClassNotFoundException, IOException, AnnotationException
    {
        if (aForceRecreateCas || !curationDocumentService.existsCurationCas(aDocument)) {
            return initializeEditorCas(aState, aCasses, aTemplate, aMergeIncompleteAnnotations);
        }

        CAS mergeCas = curationDocumentService.readCurationCas(aDocument);
        if (aUpgrade) {
            curationDocumentService.upgradeCurationCas(mergeCas, aDocument);
            curationDocumentService.writeCurationCas(mergeCas, aDocument, true);
            updateDocumentTimestampAfterWrite(aState,
                    curationDocumentService.getCurationCasTimestamp(aState.getDocument()));
        }

        return mergeCas;
    }

    private CAS initializeEditorCas(AnnotatorState aState, Map<String, CAS> aCasses,
            AnnotationDocument aTemplate, boolean aMergeIncompleteAnnotations)
        throws ClassNotFoundException, UIMAException, IOException, AnnotationException
    {
        Validate.notNull(aState, "State must be specified");
        Validate.notNull(aTemplate, "Annotation document must be specified");

        // We need a modifiable copy of some annotation document which we can use to initialize
        // the curation CAS. This is an exceptional case where BYPASS is the correct choice
        CAS editorCas = documentService.readAnnotationCas(aTemplate.getDocument(),
                aTemplate.getUser(), FORCE_CAS_UPGRADE, UNMANAGED_ACCESS);

        List<DiffAdapter> adapters = getDiffAdapters(annotationService,
                aState.getAnnotationLayers());

        DiffResult diff;
        try (StopWatch watch = new StopWatch(LOG, "CasDiff")) {
            diff = doDiffSingle(adapters, LINK_ROLE_AS_LABEL, aCasses, 0,
                    editorCas.getDocumentText().length()).toResult();
        }

        try (StopWatch watch = new StopWatch(LOG, "CasMerge")) {
            CasMerge casMerge = new CasMerge(annotationService);
            casMerge.setMergeIncompleteAnnotations(aMergeIncompleteAnnotations);
            casMerge.reMergeCas(diff, aState.getDocument(), aState.getUser().getUsername(),
                    editorCas, aCasses);
        }

        curationDocumentService.writeCurationCas(editorCas, aTemplate.getDocument(), false);

        updateDocumentTimestampAfterWrite(aState,
                curationDocumentService.getCurationCasTimestamp(aState.getDocument()));

        return editorCas;
    }
}

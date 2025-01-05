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
package de.tudarmstadt.ukp.inception.app.ui.externalsearch.sidebar;

import static de.tudarmstadt.ukp.inception.app.ui.externalsearch.sidebar.ExternalSearchUserStateMetaData.CURRENT_ES_USER_STATE;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.app.ui.externalsearch.ExternalResultDataProvider;
import de.tudarmstadt.ukp.inception.app.ui.externalsearch.utils.DocumentImporter;
import de.tudarmstadt.ukp.inception.app.ui.externalsearch.utils.HighlightLabel;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchHighlight;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchService;
import de.tudarmstadt.ukp.inception.externalsearch.HighlightUtils;
import de.tudarmstadt.ukp.inception.externalsearch.event.ExternalSearchQueryEvent;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.rendering.pipeline.RenderAnnotationsEvent;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VMarker;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VRange;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VTextMarker;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.annotation.OffsetSpan;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxSubmitLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationEventPublisherHolder;

public class ExternalSearchAnnotationSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = -3358207848681467994L;

    private static final Logger LOG = LoggerFactory
            .getLogger(ExternalSearchAnnotationSidebar.class);

    private @SpringBean DocumentService documentService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean ProjectService projectService;
    private @SpringBean ExternalSearchService externalSearchService;
    private @SpringBean UserDao userRepository;
    private @SpringBean DocumentImportExportService importExportService;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisher;
    private @SpringBean DocumentImporter documentImporter;

    private CompoundPropertyModel<ExternalSearchUserState> searchStateModel;

    private final WebMarkupContainer mainContainer;

    private IModel<List<DocumentRepository>> repositoriesModel;

    private Project project;

    private DocumentRepository currentRepository;

    private WebMarkupContainer dataTableContainer;

    public ExternalSearchAnnotationSidebar(String aId, AnnotationActionHandler aActionHandler,
            CasProvider aCasProvider, AnnotationPageBase2 aAnnotationPage)
    {
        super(aId, aActionHandler, aCasProvider, aAnnotationPage);

        // Attach search state to annotation page
        // This state is to maintain persistence of this sidebar so that when user moves to another
        // sidebar and comes back here, the state of this sidebar (search results) are preserved.
        searchStateModel = new CompoundPropertyModel<>(LambdaModelAdapter.of(
                () -> aAnnotationPage.getMetaData(CURRENT_ES_USER_STATE),
                searchState -> aAnnotationPage.setMetaData(CURRENT_ES_USER_STATE, searchState)));

        // Set up the search state in the page if it is not already there
        if (aAnnotationPage.getMetaData(CURRENT_ES_USER_STATE) == null) {
            searchStateModel.setObject(new ExternalSearchUserState());
        }

        project = getModel().getObject().getProject();
        List<DocumentRepository> repositories = externalSearchService
                .listDocumentRepositories(project);

        ExternalSearchUserState searchState = searchStateModel.getObject();
        currentRepository = searchState.getCurrentRepository();
        if (currentRepository == null && repositories.size() > 0) {
            currentRepository = repositories.get(0);
        }

        repositoriesModel = LoadableDetachableModel
                .of(() -> externalSearchService.listDocumentRepositories(project));

        mainContainer = new WebMarkupContainer("mainContainer");
        mainContainer.setOutputMarkupId(true);
        add(mainContainer);

        DocumentRepositorySelectionForm projectSelectionForm = new DocumentRepositorySelectionForm(
                "repositorySelectionForm");
        mainContainer.add(projectSelectionForm);

        SearchForm searchForm = new SearchForm("searchForm");
        add(searchForm);
        mainContainer.add(searchForm);

        List<IColumn<ExternalSearchResult, String>> columns = new ArrayList<>();

        columns.add(new AbstractColumn<ExternalSearchResult, String>(new Model<>("Results"))
        {
            private static final long serialVersionUID = -5658664083675871242L;

            @Override
            public void populateItem(Item<ICellPopulator<ExternalSearchResult>> cellItem,
                    String componentId, IModel<ExternalSearchResult> model)
            {
                @SuppressWarnings("rawtypes")
                Item rowItem = cellItem.findParent(Item.class);
                int rowIndex = rowItem.getIndex();
                ResultRowView rowView = new ResultRowView(componentId, rowIndex + 1, model);
                cellItem.add(rowView);
            }
        });

        if (searchState.getDataProvider() == null) {
            searchState.setDataProvider(new ExternalResultDataProvider(externalSearchService,
                    userRepository.getCurrentUser()));
        }

        dataTableContainer = new WebMarkupContainer("dataTableContainer");
        dataTableContainer.setOutputMarkupId(true);
        mainContainer.add(dataTableContainer);

        DataTable<ExternalSearchResult, String> resultTable = new DefaultDataTable<>("resultsTable",
                columns, searchState.getDataProvider(), 8);
        resultTable.setCurrentPage(searchState.getCurrentPage());
        dataTableContainer.add(resultTable);
    }

    @Override
    protected void onDetach()
    {
        ExternalSearchUserState searchState = searchStateModel.getObject();

        // Save the current page number of the search results when the sidebar being switched
        DataTable<ExternalSearchResult, String> resultTable = //
                (DataTable<ExternalSearchResult, String>) dataTableContainer.get("resultsTable");
        searchState.setCurrentPage(resultTable.getCurrentPage());

        // save current repository
        searchState.setCurrentRepository(currentRepository);

        super.onDetach();
    }

    @OnEvent
    public void onRenderAnnotations(RenderAnnotationsEvent aEvent)
    {
        ExternalSearchUserState searchState = searchStateModel.getObject();

        // highlight keywords if a document is selected from result list
        // and it is the current document opened
        if (searchState.getSelectedResult() != null
                && (searchState.getSelectedResult().getDocumentId()
                        .equals(getAnnotationPage().getModelObject().getDocument().getName()))) {
            highlightKeywords(aEvent.getRequest(), aEvent.getVDocument());
        }
        else {
            // a document was opened not by selecting from the result list
            searchState.setSelectedResult(null);
        }
    }

    // TODO: Maybe we should highlight all occurrences of the query term in the texst and
    // not only the ones returned in the highlights?
    private void highlightKeywords(RenderRequest aRequest, VDocument aVDocument)
    {
        ExternalSearchUserState searchState = searchStateModel.getObject();
        try {
            String documentText = getCasProvider().get().getDocumentText();

            for (ExternalSearchHighlight highlight : searchState.getSelectedResult()
                    .getHighlights()) {

                Optional<ExternalSearchHighlight> maybeExHighlight = HighlightUtils
                        .parseHighlight(highlight.getHighlight(), documentText);
                if (!maybeExHighlight.isPresent()) {
                    continue;
                }

                var exHighlight = maybeExHighlight.get();

                // Highlight the keywords in the annotator indicated by the offsets
                // if they are within the current window.
                for (OffsetSpan offset : exHighlight.getOffsets()) {
                    Optional<VRange> range = VRange.clippedRange(aVDocument, offset);

                    range.ifPresent(r -> aVDocument.add(new VTextMarker(VMarker.MATCH_FOCUS, r)));

                    if (offset.getBegin() > aRequest.getWindowEndOffset()) {
                        break;
                    }
                }
            }
        }
        catch (IOException e) {
            LOG.error("Unable to load document {}: {}",
                    searchState.getSelectedResult().getDocumentId(), e.getMessage(), e);
            error("Unable to load document " + searchState.getSelectedResult().getDocumentId()
                    + ": " + ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private void actionImport(AjaxRequestTarget aTarget, ExternalSearchResult aResult)
    {
        aTarget.addChildren(getPage(), IFeedback.class);
        searchStateModel.getObject().setSelectedResult(aResult);
        try {
            boolean imported = documentImporter.importDocumentFromDocumentRepository(
                    userRepository.getCurrentUser(), project, aResult.getCollectionId(),
                    aResult.getDocumentId(), currentRepository);

            if (imported) {
                success("Imported document: " + aResult.getDocumentId());
            }
            else {
                info("Document already present: " + aResult.getDocumentId());
            }

            getAnnotationPage().actionShowDocument(aTarget,
                    documentService.getSourceDocument(project, aResult.getDocumentId()));
        }
        catch (Exception e) {
            LOG.error("Unable to load document {}: {}", aResult.getDocumentId(), e.getMessage(), e);
            error("Unable to load document " + aResult.getDocumentId() + ": "
                    + ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private void actionOpen(AjaxRequestTarget aTarget, ExternalSearchResult aResult)
    {
        try {
            searchStateModel.getObject().setSelectedResult(aResult);
            getAnnotationPage().actionShowDocument(aTarget,
                    documentService.getSourceDocument(project, aResult.getDocumentId()));
        }
        catch (Exception e) {
            LOG.error("Unable to load document {}: {}", aResult.getDocumentId(), e.getMessage(), e);
            error("Unable to load document " + aResult.getDocumentId() + ": "
                    + ExceptionUtils.getRootCauseMessage(e));
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }

    private class DocumentRepositorySelectionForm
        extends Form<DocumentRepository>
    {
        private static final long serialVersionUID = 660903434919120494L;

        public DocumentRepositorySelectionForm(String aId)
        {
            super(aId);

            DropDownChoice<DocumentRepository> repositoryCombo = new DropDownChoice<DocumentRepository>(
                    "repositoryCombo",
                    new PropertyModel<DocumentRepository>(ExternalSearchAnnotationSidebar.this,
                            "currentRepository"),
                    repositoriesModel);

            repositoryCombo.setChoiceRenderer(new ChoiceRenderer<DocumentRepository>("name"));
            repositoryCombo.setNullValid(false);

            // Just update the selection
            repositoryCombo.add(new LambdaAjaxFormComponentUpdatingBehavior("change"));
            add(repositoryCombo);

        }
    }

    private class SearchForm
        extends Form<Void>
    {
        private static final long serialVersionUID = -2787363313878650063L;

        public SearchForm(String id)
        {
            super(id);
            add(new TextField<>("queryInput", searchStateModel.bind("query"), String.class));
            var searchLink = new LambdaAjaxSubmitLink<Void>("submitSearch",
                    ExternalSearchAnnotationSidebar.this::actionSearch);
            add(searchLink);
            setDefaultButton(searchLink);
        }
    }

    private void actionSearch(AjaxRequestTarget aTarget, Form<Void> aForm)
    {
        ExternalSearchUserState searchState = searchStateModel.getObject();

        searchState.setSelectedResult(null);

        // No repository, no results
        if (currentRepository == null) {
            error("No repository selected");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        try {
            searchState.getDataProvider().searchDocuments(currentRepository,
                    searchState.getQuery());
        }
        catch (Exception e) {
            LOG.error("Unable to perform query", e);
            error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
            aTarget.addChildren(getPage(), IFeedback.class);
        }

        aTarget.add(dataTableContainer);

        applicationEventPublisher.get()
                .publishEvent(new ExternalSearchQueryEvent(this, currentRepository.getProject(),
                        userRepository.getCurrentUsername(), searchState.getQuery()));
    }

    public class ResultRowView
        extends Panel
    {
        private static final long serialVersionUID = 6212628948731147733L;

        public ResultRowView(String id, long rowNumber, IModel<ExternalSearchResult> model)
        {
            super(id, model);

            ExternalSearchResult result = (ExternalSearchResult) getDefaultModelObject();

            boolean existsSourceDocument = documentService.existsSourceDocument(project,
                    result.getDocumentId());

            // Import and open annotation
            LambdaAjaxLink link;
            if (!existsSourceDocument) {
                link = new LambdaAjaxLink("docLink", t -> actionImport(t, result));
            }
            else {
                // open action
                link = new LambdaAjaxLink("docLink", t -> actionOpen(t, result));
            }

            String title = defaultIfBlank(result.getDocumentTitle(), defaultIfBlank(
                    result.getDocumentId(), defaultIfBlank(result.getOriginalUri(), "<no title>")));

            add(link);

            link.add(new Label("title", title));
            link.add(new Label("score", result.getScore()));
            link.add(new Label("importStatus",
                    () -> existsSourceDocument ? "imported" : "not imported"));

            // FIXME: Should display all highlights
            String highlight = "NO MATCH PREVIEW AVAILABLE";
            if (!result.getHighlights().isEmpty()) {
                highlight = result.getHighlights().get(0).getHighlight();
            }
            link.add(new HighlightLabel("highlight", highlight));
        }
    }

    public static class ExternalSearchUserState
        implements Serializable
    {
        private static final long serialVersionUID = 366937089563292016L;

        private AnnotationLayer layer;

        private DocumentRepository currentRepository = null;

        private String query = null;

        private ExternalResultDataProvider dataProvider = null;

        private long currentPage = 1;

        private ExternalSearchResult selectedResult;

        public DocumentRepository getCurrentRepository()
        {
            return currentRepository;
        }

        public void setCurrentRepository(DocumentRepository aRepository)
        {
            currentRepository = aRepository;
        }

        public AnnotationLayer getLayer()
        {
            return layer;
        }

        public void setLayer(AnnotationLayer aAnnotationLayer)
        {
            layer = aAnnotationLayer;
        }

        public String getQuery()
        {
            return query;
        }

        public void setQuery(String aQuery)
        {
            query = aQuery;
        }

        public ExternalResultDataProvider getDataProvider()
        {
            return dataProvider;
        }

        public void setDataProvider(ExternalResultDataProvider aDataProvider)
        {
            dataProvider = aDataProvider;
        }

        public long getCurrentPage()
        {
            return currentPage;
        }

        public void setCurrentPage(long aCurrentPage)
        {
            currentPage = aCurrentPage;
        }

        public ExternalSearchResult getSelectedResult()
        {
            return selectedResult;
        }

        public void setSelectedResult(ExternalSearchResult aSelectedResult)
        {
            selectedResult = aSelectedResult;
        }
    }
}

/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.wicketstuff.event.annotation.OnEvent;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.JCasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.event.RenderAnnotationsEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxSubmitLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.clarin.webanno.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.app.ui.externalsearch.DocumentImporterImpl;
import de.tudarmstadt.ukp.inception.app.ui.externalsearch.ExternalResultDataProvider;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchService;
import de.tudarmstadt.ukp.inception.externalsearch.event.ExternalSearchQueryEvent;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;

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
    private @SpringBean ImportExportService importExportService;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisher;
    private @SpringBean DocumentImporterImpl documentImporter;

    private CompoundPropertyModel<ExternalSearchUserState> searchStateModel;

    private final WebMarkupContainer mainContainer;

    private List<ExternalSearchResult> results = new ArrayList<ExternalSearchResult>();

    private IModel<List<DocumentRepository>> repositoriesModel;

    private ExternalSearchResult selectedResult;

    private Project project;

    private WebMarkupContainer dataTableContainer;

    public ExternalSearchAnnotationSidebar(String aId, IModel<AnnotatorState> aModel,
        AnnotationActionHandler aActionHandler, JCasProvider aJCasProvider,
        AnnotationPage aAnnotationPage)
    {
        super(aId, aModel, aActionHandler, aJCasProvider, aAnnotationPage);

        // Attach search state to annotation page
        // This state is to maintain persistence of this sidebar so that when user moves to another
        // sidebar and comes back here, the state of this sidebar (search results) are preserved.
        searchStateModel = new CompoundPropertyModel<>(LambdaModelAdapter
            .of(() -> aAnnotationPage.getMetaData(CURRENT_ES_USER_STATE),
                searchState -> aAnnotationPage.setMetaData(CURRENT_ES_USER_STATE, searchState)));

        // Set up the search state in the page if it is not already there
        if (aAnnotationPage.getMetaData(CURRENT_ES_USER_STATE) == null) {
            searchStateModel.setObject(new ExternalSearchUserState());
        }

        project = getModel().getObject().getProject();
        List<DocumentRepository> repositories = externalSearchService
            .listDocumentRepositories(project);

        ExternalSearchUserState searchState = searchStateModel.getObject();
        if (searchState.getCurrentRepository() == null && repositories.size() > 0) {
            searchState.setCurrentRepository(repositories.get(0));
        }
        if (searchState.getQuery() == null) {
            searchState.setQuery(Model.of(""));
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

            @Override public void populateItem(Item<ICellPopulator<ExternalSearchResult>> cellItem,
                String componentId, IModel<ExternalSearchResult> model)
            {
                @SuppressWarnings("rawtypes") Item rowItem = cellItem.findParent(Item.class);
                int rowIndex = rowItem.getIndex();
                ResultRowView rowView = new ResultRowView(componentId, rowIndex + 1, model);
                cellItem.add(rowView);
            }
        });

        if (searchState.getDataProvider() == null) {
            searchState.setDataProvider(new ExternalResultDataProvider(externalSearchService,
                userRepository.getCurrentUser(),
                searchStateModel.getObject().getCurrentRepository(), ""));
        }

        dataTableContainer = new WebMarkupContainer("dataTableContainer");
        dataTableContainer.setOutputMarkupId(true);
        mainContainer.add(dataTableContainer);

        DataTable<ExternalSearchResult, String> resultTable = new DefaultDataTable<>("resultsTable",
            columns, searchState.getDataProvider(), 8);
        resultTable.setCurrentPage(searchState.getCurrentPage());
        dataTableContainer.add(resultTable);

    }

    @Override protected void onDetach()
    {
        // Save the current page number of the search results when the sidebar being switched
        DataTable<ExternalSearchResult, String> resultTable =
            (DataTable<ExternalSearchResult, String>) dataTableContainer.get("resultsTable");
        searchStateModel.getObject().setCurrentPage(resultTable.getCurrentPage());
        super.onDetach();
    }

    @OnEvent public void onRenderAnnotations(RenderAnnotationsEvent aEvent)
    {
        if (selectedResult != null) {
            // TODO highlight keyword
        }
    }

    private void actionImport(AjaxRequestTarget aTarget, ExternalSearchResult aResult,
        String aDocumentTitle) {
        selectedResult = aResult;
        try {
            documentImporter
                .importDocumentFromDocumentRepository(userRepository.getCurrentUser(), project,
                    aDocumentTitle, searchStateModel.getObject().getCurrentRepository());

            getAnnotationPage().actionShowSelectedDocument(aTarget,
                documentService.getSourceDocument(project, aDocumentTitle));
        }
        catch (IOException e) {
            LOG.error("{}", e.getMessage(), e);
            error(e.getMessage() + " - " + ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private void actionOpen(AjaxRequestTarget aTarget, ExternalSearchResult aResult,
        String aDocumentTitle) {
        selectedResult = aResult;
        getAnnotationPage().actionShowSelectedDocument(aTarget,
            documentService.getSourceDocument(project, aDocumentTitle));
    }

    private class DocumentRepositorySelectionForm
        extends Form<DocumentRepository>
    {
        private static final long serialVersionUID = 660903434919120494L;

        public DocumentRepositorySelectionForm(String aId)
        {
            super(aId);

            DropDownChoice<DocumentRepository> repositoryCombo =
                new BootstrapSelect<DocumentRepository>("repositoryCombo",
                new PropertyModel<DocumentRepository>(ExternalSearchAnnotationSidebar.this,
                    "searchStateModel.getObject().getCurrentRepository()"), repositoriesModel);

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
            add(new TextField<>("queryInput", searchStateModel.getObject().getQuery(),
                String.class));
            LambdaAjaxSubmitLink searchLink = new LambdaAjaxSubmitLink("submitSearch",
                this::actionSearch);
            add(searchLink);
            setDefaultButton(searchLink);
        }

        private void actionSearch(AjaxRequestTarget aTarget, Form aForm)
        {
            IModel<String> query = searchStateModel.getObject().getQuery();
            selectedResult = null;
            if (query.getObject() == null) {
                query.setObject("*.*");
            }

            searchDocuments(query.getObject());

            searchStateModel.getObject().getDataProvider().searchDocuments(query.getObject());

            aTarget.add(dataTableContainer);
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }

    private void searchDocuments(String aQuery)
    {
        if (searchStateModel.getObject().getCurrentRepository() == null) {
            error("Error: No repository selected");
            return;
        }

        results.clear();
        applicationEventPublisher.get().publishEvent(new ExternalSearchQueryEvent(this,
            searchStateModel.getObject().getCurrentRepository().getProject(),
            userRepository.getCurrentUser().getUsername(), aQuery));

        try {
            for (ExternalSearchResult result : externalSearchService
                .query(userRepository.getCurrentUser(),
                    searchStateModel.getObject().getCurrentRepository(), aQuery)) {
                results.add(result);
            }
        }
        catch (Exception e) {
            LOG.error("Unable to perform query [" + aQuery + "]", e);
            error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
        }
    }

    public class ResultRowView
        extends Panel
    {
        private static final long serialVersionUID = 6212628948731147733L;

        public ResultRowView(String id, long rowNumber, IModel<ExternalSearchResult> model)
        {
            super(id, model);

            ExternalSearchResult result = (ExternalSearchResult) getDefaultModelObject();

            String documentTitle = result.getDocumentTitle();

            Whitelist wl = new Whitelist();
            wl.addTags("em");
            Document dirty = Jsoup.parseBodyFragment(result.getHighlights().get(0), "");
            Cleaner cleaner = new Cleaner(wl);
            Document clean = cleaner.clean(dirty);
            clean.select("em").tagName("mark");
            String highlight = clean.body().html();

            boolean existsSourceDocument = documentService
                .existsSourceDocument(project, documentTitle);

            // Import and open annotation
            LambdaAjaxLink link;
            if (!existsSourceDocument) {
                link = new LambdaAjaxLink("docLink", t -> actionImport(t, result, documentTitle));
            }
            else {
                // open action
                link = new LambdaAjaxLink("docLink", t -> actionOpen(t, result, documentTitle));
            }

            String title = defaultIfBlank(result.getDocumentTitle(),
                defaultIfBlank(result.getDocumentId(),
                    defaultIfBlank(result.getUri(), "<no title>")));

            add(link);

            link.add(new Label("title", title));
            link.add(new Label("score", result.getScore()));
            link.add(new Label("highlight", highlight).setEscapeModelStrings(false));
            link.add(new Label("importStatus",
                () -> existsSourceDocument ? "imported" : "not imported"));
        }
    }

    public static class ExternalSearchUserState
        implements Serializable
    {
        private static final long serialVersionUID = 366937089563292016L;

        private AnnotationLayer layer;

        private DocumentRepository currentRepository = null;

        private IModel<String> query = null;

        private ExternalResultDataProvider dataProvider = null;

        private long currentPage = 1;

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

        public IModel<String> getQuery()
        {
            return query;
        }

        public void setQuery(IModel<String> aQuery)
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
    }
}

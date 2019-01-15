/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.inception.app.ui.externalsearch;

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.bootstrap.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxSubmitLink;
import de.tudarmstadt.ukp.clarin.webanno.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchService;
import de.tudarmstadt.ukp.inception.externalsearch.event.ExternalSearchQueryEvent;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;
import de.tudarmstadt.ukp.inception.ui.core.session.SessionMetaData;

@MountPath("/search.html")
public class SearchPage extends ApplicationPageBase
{
    private static final long serialVersionUID = 4090656233059899062L;

    private static final Logger LOG = LoggerFactory.getLogger(SearchPage.class);

    private static final String PLAIN_TEXT = "text";

    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean ExternalSearchService externalSearchService;
    private @SpringBean UserDao userRepository;
    private @SpringBean ImportExportService importExportService;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisher;

    private WebMarkupContainer dataTableContainer;

    private List<ExternalSearchResult> results = new ArrayList<ExternalSearchResult>();

    private IModel<String> targetQuery = Model.of("");

    private IModel<List<DocumentRepository>> repositoriesModel;

    private DocumentRepository currentRepository;
    private Project project;

    ExternalResultDataProvider dataProvider;

    public SearchPage(PageParameters aParameters)
    {
        project = Session.get().getMetaData(SessionMetaData.CURRENT_PROJECT);
        if (project == null) {
            abort();
        }

        List<DocumentRepository> repositories = externalSearchService
                .listDocumentRepositories(project);

        if (repositories.size() > 0) {
            currentRepository = repositories.get(0);
        }
        else {
            currentRepository = null;
        }

        repositoriesModel = LoadableDetachableModel.of(() -> externalSearchService
                        .listDocumentRepositories(project));


        DocumentRepositorySelectionForm projectSelectionForm = new DocumentRepositorySelectionForm(
                "repositorySelectionForm");
        add(projectSelectionForm);

        SearchForm searchForm = new SearchForm("searchForm");
        add(searchForm);

        List<IColumn<ExternalSearchResult, String>> columns = new ArrayList<>();

        columns.add(new AbstractColumn<ExternalSearchResult, String>(new Model<>("Results"))
        {
            @Override
            public void populateItem(Item<ICellPopulator<ExternalSearchResult>> cellItem,
                    String componentId, IModel<ExternalSearchResult> model)
            {
                @SuppressWarnings("rawtypes")
                Item rowItem = cellItem.findParent( Item.class );
                int rowIndex = rowItem.getIndex();
                ResultRowView rowView = new ResultRowView(componentId, rowIndex + 1, model);
                cellItem.add(rowView);
            }
        });

        dataProvider = new ExternalResultDataProvider(
                externalSearchService, userRepository.getCurrentUser(), currentRepository, "merck");

        dataTableContainer = new WebMarkupContainer("dataTableContainer");
        dataTableContainer.setOutputMarkupId(true);
        add(dataTableContainer);

        DataTable<ExternalSearchResult, String> resultTable = new DefaultDataTable<>("resultsTable",
                columns, dataProvider, 8);

        dataTableContainer.add(resultTable);
    }

    private void actionImportDocument(AjaxRequestTarget aTarget, ExternalSearchResult aResult)
    {
        String documentTitle = aResult.getDocumentTitle();
        
        String text = externalSearchService
                .getDocumentById(userRepository.getCurrentUser(), currentRepository,
                        documentTitle)
                .getText();

        if (documentService.existsSourceDocument(project, documentTitle)) {
            error("Document [" + documentTitle + "] already uploaded! Delete "
                + "the document if you want to upload again");
        }
        else {
            importDocument(documentTitle, text);
            aTarget.add(dataTableContainer);
        }
    }
    
    private void importDocument(String aFileName, String aText)
    {
        InputStream stream = new ByteArrayInputStream(aText.getBytes(StandardCharsets.UTF_8));

        SourceDocument document = new SourceDocument();
        document.setName(aFileName);
        document.setProject(project);
        document.setFormat(PLAIN_TEXT);

        try (InputStream is = stream) {
            documentService.uploadSourceDocument(is, document);
        }
        catch (IOException | UIMAException e) {
            LOG.error("Unable to retrieve document " + aFileName, e);
            error("Unable to retrieve document " + aFileName + " - "
                    + ExceptionUtils.getRootCauseMessage(e));
            e.printStackTrace();
        }

    }

    private class SearchForm
        extends Form<Void>
    {
        private static final long serialVersionUID = 2186231514180399862L;

        public SearchForm(String id)
        {
            super(id);
            add(new TextField<>("queryInput", targetQuery, String.class));
            LambdaAjaxSubmitLink searchLink = new LambdaAjaxSubmitLink("submitSearch",
                    this::actionSearch);
            add(searchLink);
            setDefaultButton(searchLink);
        }
        
        private void actionSearch(AjaxRequestTarget aTarget, Form aForm)
        {
            if (targetQuery.getObject() == null) {
                targetQuery.setObject("*.*");
            }

            searchDocuments(targetQuery.getObject());

            dataProvider.searchDocuments(targetQuery.getObject());
            
            aTarget.add(dataTableContainer);
        }
    }

    private void searchDocuments(String aQuery)
    {
        results.clear();
        applicationEventPublisher.get()
                .publishEvent(new ExternalSearchQueryEvent(this, currentRepository.getProject(),
                        userRepository.getCurrentUser().getUsername(), aQuery));

        try {
            for (ExternalSearchResult result : externalSearchService
                    .query(userRepository.getCurrentUser(), currentRepository, aQuery)) {
                results.add(result);
            }
        }
        catch (Exception e) {
            LOG.error("Unable to perform query", e);
            error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private class DocumentRepositorySelectionForm
        extends
        Form<DocumentRepository>
    {
        public DocumentRepositorySelectionForm(String aId)
        {
            super(aId);

            DropDownChoice<DocumentRepository> repositoryCombo =
                    new BootstrapSelect<DocumentRepository>(
                    "repositoryCombo",
                    new PropertyModel<DocumentRepository>(SearchPage.this, "currentRepository"),
                    repositoriesModel)
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoiceRenderer(new ChoiceRenderer<DocumentRepository>("name"));
                    setNullValid(false);
                }

                @Override
                protected CharSequence getDefaultChoice(String aSelectedValue)
                {
                    return "";
                }
            };
            // Just update the selection
            repositoryCombo.add(new LambdaAjaxFormComponentUpdatingBehavior("change"));
            add(repositoryCombo);

        }

        private static final long serialVersionUID = -1L;
    }
    
    private void abort() {
        throw new RestartResponseException(getApplication().getHomePage());
    }

    public class ResultRowView
        extends Panel
    {
        private static final long serialVersionUID = -6708211343231617251L;

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
            
            LambdaAjaxLink link = new LambdaAjaxLink("titleLink", _target -> {
                PageParameters pageParameters = new PageParameters()
                    .add(DocumentDetailsPage.DOCUMENT_TITLE, documentTitle);
                setResponsePage(DocumentDetailsPage.class, pageParameters);

            });
            
            String title = defaultIfBlank(result.getDocumentTitle(),
                            defaultIfBlank(result.getDocumentId(), 
                            defaultIfBlank(result.getUri(), "<no title>")));
            boolean existsSourceDocument = documentService.existsSourceDocument(project,
                    documentTitle);
            
            link.add(new Label("title", title));
            add(link);

            add(new Label("score", result.getScore()));
            add(new Label("highlight", highlight).setEscapeModelStrings(false));
            add(new Label("importStatus", () ->
                    existsSourceDocument ? "imported" : "not imported"));
            add(new LambdaAjaxLink("importLink", _target -> actionImportDocument(_target, result))
                    .add(visibleWhen(() -> !existsSourceDocument)));
            add(new LambdaAjaxLink("openLink", _target -> {
                PageParameters pageParameters = new PageParameters()
                    .add(WebAnnoConst.PAGE_PARAM_PROJECT_ID, project.getId())
                    .add(WebAnnoConst.PAGE_PARAM_DOCUMENT_ID,
                        documentService.getSourceDocument(project, documentTitle).getId());
                setResponsePage(AnnotationPage.class, pageParameters);
            }).add(
                visibleWhen(() -> existsSourceDocument)));
        }
    }
}

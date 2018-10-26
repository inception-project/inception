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
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchService;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;
import de.tudarmstadt.ukp.inception.ui.core.session.SessionMetaData;

@MountPath("/search.html")
public class SearchPage extends ApplicationPageBase
{
    private static final long serialVersionUID = 4090656233059899062L;

    private static final Logger LOG = LoggerFactory.getLogger(SearchPage.class);

    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean ExternalSearchService externalSearchService;
    private @SpringBean UserDao userRepository;
    private @SpringBean ImportExportService importExportService;

    final WebMarkupContainer mainContainer = new WebMarkupContainer("mainContainer");

    final String PLAIN_TEXT = "text";
    
    private ListView<ExternalSearchResult> resultList;
    private ArrayList<ExternalSearchResult> results = new ArrayList<ExternalSearchResult>();

    Model<String> targetQuery = Model.of("");

    private IModel<ArrayList<DocumentRepository>> repositoriesModel;

    private DocumentRepository currentRepository;
    private User currentUser;
    private Project project;

    ExternalResultDataProvider dataProvider;

    ModalWindow modalDocumentWindow;

    public SearchPage(PageParameters aParameters)
    {
        project = Session.get().getMetaData(SessionMetaData.CURRENT_PROJECT);
        if (project == null) {
            abort();
        }

        currentUser = userRepository.getCurrentUser();

        ArrayList<DocumentRepository> repositories;

        repositories = (ArrayList<DocumentRepository>) externalSearchService
                .listDocumentRepositories(project);

        if (repositories.size() > 0) {
            currentRepository = repositories.get(0);
        }
        else {
            currentRepository = null;
        }

        repositoriesModel = new LoadableDetachableModel<ArrayList<DocumentRepository>>()
        {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            protected ArrayList<DocumentRepository> load()
            {
                ArrayList<DocumentRepository> documentRepositories;
                // Load user's projects
                documentRepositories = (ArrayList<DocumentRepository>) externalSearchService
                        .listDocumentRepositories(project);
                return documentRepositories;
            }
        };

        add(mainContainer);

        DocumentRepositorySelectionForm projectSelectionForm = new DocumentRepositorySelectionForm(
                "repositorySelectionForm");
        mainContainer.add(projectSelectionForm);

        SearchForm searchForm = new SearchForm("searchForm");

        modalDocumentWindow = new ModalWindow("modalDocumentWindow");

        add(modalDocumentWindow);
        
        modalDocumentWindow.setCloseButtonCallback(new ModalWindow.CloseButtonCallback()
        {
            @Override
            public boolean onCloseButtonClicked(AjaxRequestTarget target)
            {
                return true;
            }
        });

        modalDocumentWindow.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
        {
            @Override
            public void onClose(AjaxRequestTarget target)
            {
            }
        });


        mainContainer.add(searchForm);

        List<IColumn<ExternalSearchResult, String>> columns = new ArrayList<>();

        columns.add(new AbstractColumn<ExternalSearchResult, String>(new Model<>("Text Id"))
        {
            @Override
            public void populateItem(Item<ICellPopulator<ExternalSearchResult>> cellItem,
                    String componentId, IModel<ExternalSearchResult> model)
            {
                cellItem.add(new ShowPanel(componentId, model));
            }
        });
        
        columns.add(new PropertyColumn<ExternalSearchResult, String>(new Model<>("Doc. Id"),
                "documentId", "documentId"));

        columns.add(new PropertyColumn<ExternalSearchResult, String>(new Model<>("Source"),
                "source", "source"));

        columns.add(new PropertyColumn<ExternalSearchResult, String>(new Model<>("Timestamp"),
                "timestamp", "timestamp"));

        columns.add(new PropertyColumn<ExternalSearchResult, String>(new Model<>("URI"),
                "uri", "uri"));

        columns.add(new PropertyColumn<ExternalSearchResult, String>(new Model<>("Score"),
                "score", "score"));

        columns.add(new AbstractColumn<ExternalSearchResult, String>(new Model<>("Highlights"))
        {
            @Override
            public void populateItem(Item<ICellPopulator<ExternalSearchResult>> cellItem,
                    String componentId, IModel<ExternalSearchResult> model)
            {
                cellItem.add(new HighlightsPanel(componentId, model));
            }
        });
        
        columns.add(new AbstractColumn<ExternalSearchResult, String>(new Model<>("Action"))
        {
            @Override
            public void populateItem(Item<ICellPopulator<ExternalSearchResult>> cellItem,
                    String componentId, IModel<ExternalSearchResult> model)
            {
                cellItem.add(new ImportPanel(componentId, model));
            }
        });
        

        dataProvider = new ExternalResultDataProvider(
                externalSearchService, currentUser, currentRepository, "merck");
        
        DataTable<ExternalSearchResult, String> resultTable = new DefaultDataTable<>("resultsTable",
                columns, dataProvider, 8);

        mainContainer.add(resultTable);
        
        mainContainer.setOutputMarkupId(true);

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
        extends Form
    {
        private static final long serialVersionUID = 2186231514180399862L;
        private TextField<String> queryField;

        public SearchForm(String id)
        {
            super(id);

            queryField = new TextField<String>("queryInput", targetQuery);

            SubmitLink submitSearch = new SubmitLink("submitSearch")
            {
                private static final long serialVersionUID = -8353553433583302935L;

                @Override
                public void onSubmit()
                {
                    if (targetQuery.getObject() == null) {
                        targetQuery.setObject(new String("*.*"));
                    }

                    searchDocuments(targetQuery.getObject());
                    
                    dataProvider.searchDocuments(targetQuery.getObject());
                }
            };

            add(queryField);
            add(submitSearch);
        }
    }

    private void searchDocuments(String aQuery)
    {
        results.clear();

        try {
            for (ExternalSearchResult result : externalSearchService.query(currentUser,
                    currentRepository, aQuery)) {
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
                    new DropDownChoice<DocumentRepository>(
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

    class ShowPanel extends Panel
    {
        public ShowPanel(String id, IModel<ExternalSearchResult> model)
        {
            super(id, model);
            
            ExternalSearchResult result = (ExternalSearchResult) getDefaultModelObject();

            String documentId = result.getDocumentTitle();

            LambdaAjaxLink link = new LambdaAjaxLink("openDocument", _target -> {
                String text = externalSearchService.getDocumentById(currentUser, 
                        currentRepository, documentId).getText();

                modalDocumentWindow.setContent(new ModalDocumentWindow("content", text));
                modalDocumentWindow.setTitle(documentId);
                
                modalDocumentWindow.show(_target);
                
            });
            link.add(new Label("documentId", documentId));
            
            add(link);
        }
    }

    class ImportPanel extends Panel
    {
        public ImportPanel(String id, IModel<ExternalSearchResult> model)
        {
            super(id, model);
            
            ExternalSearchResult result = (ExternalSearchResult) getDefaultModelObject();

            String documentId = result.getDocumentTitle();

            LambdaAjaxLink link = new LambdaAjaxLink("importDocument", _target -> {
                String text = externalSearchService.getDocumentById(currentUser, 
                        currentRepository, documentId).getText();

                if (documentService.existsSourceDocument(project, documentId)) {
                    error("Document " + documentId + " already uploaded ! Delete "
                            + "the document if you want to upload again");
                }
                else {
                    importDocument(documentId, text);
                }
            });
            link.add(new Label("documentId", documentId));
            
            add(link);
        }
    }

    class HighlightsPanel extends Panel
    {
        public HighlightsPanel(String id, IModel<ExternalSearchResult> model)
        {
            super(id, model);
            
            ExternalSearchResult result = (ExternalSearchResult) getDefaultModelObject();

            ArrayList<String> highlights = result.getHighlights();

            String highlight = highlights.get(0);
            
            add(new Label("highlight", highlight).setEscapeModelStrings(false));
        }
    }
}

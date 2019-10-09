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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
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
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.annotation.mount.MountPath;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxSubmitLink;
import de.tudarmstadt.ukp.clarin.webanno.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.inception.app.ui.externalsearch.utils.DocumentImporter;
import de.tudarmstadt.ukp.inception.app.ui.externalsearch.utils.Utilities;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchHighlight;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchService;
import de.tudarmstadt.ukp.inception.externalsearch.event.ExternalSearchQueryEvent;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;
import de.tudarmstadt.ukp.inception.support.ui.LinkProvider;
import de.tudarmstadt.ukp.inception.ui.core.session.SessionMetaData;

@MountPath("/search.html")
public class SearchPage extends ApplicationPageBase
{
    private static final long serialVersionUID = 4090656233059899062L;

    private static final Logger LOG = LoggerFactory.getLogger(SearchPage.class);

    private @SpringBean DocumentService documentService;
    private @SpringBean ExternalSearchService externalSearchService;
    private @SpringBean UserDao userRepository;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisher;
    private @SpringBean DocumentImporter documentImporter;

    private WebMarkupContainer dataTableContainer;

    private Project project;

    ExternalResultDataProvider dataProvider;
    
    public SearchPage()
    {
        project = Session.get().getMetaData(SessionMetaData.CURRENT_PROJECT);
        if (project == null) {
            abort();
        }

        add(new SearchForm("searchForm"));

        List<IColumn<ExternalSearchResult, String>> columns = new ArrayList<>();

        columns.add(new AbstractColumn<ExternalSearchResult, String>(new Model<>("Results"))
        {
            private static final long serialVersionUID = 3795885786416467291L;

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

        dataProvider = new ExternalResultDataProvider(externalSearchService,
                userRepository.getCurrentUser());

        dataTableContainer = new WebMarkupContainer("dataTableContainer");
        dataTableContainer.setOutputMarkupId(true);
        add(dataTableContainer);

        dataTableContainer.add(new DefaultDataTable<>("resultsTable", columns, dataProvider, 10));
    }

    private void actionImportDocument(AjaxRequestTarget aTarget, ExternalSearchResult aResult)
    {
        try {
            documentImporter.importDocumentFromDocumentRepository(userRepository.getCurrentUser(),
                    project, aResult.getCollectionId(), aResult.getDocumentId(),
                    aResult.getRepository());
            aTarget.add(dataTableContainer);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            error(e.getMessage() + " - " + ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private class SearchFormModel implements Serializable
    {
        private static final long serialVersionUID = 4857333535866668775L;
        
        public DocumentRepository repository;
        public String query;
    }
    
    private class SearchForm
        extends Form<SearchFormModel>
    {
        private static final long serialVersionUID = 2186231514180399862L;

        public SearchForm(String id)
        {
            super(id);
            
            setModel(CompoundPropertyModel.of(new SearchFormModel()));
            
            DropDownChoice<DocumentRepository> repositoryCombo = 
                    new BootstrapSelect<DocumentRepository>("repository");
            repositoryCombo.setChoices(LoadableDetachableModel
                    .of(() -> externalSearchService.listDocumentRepositories(project)));
            repositoryCombo.setChoiceRenderer(new ChoiceRenderer<DocumentRepository>("name"));
            repositoryCombo.setNullValid(false);
            add(repositoryCombo);
            
            if (!repositoryCombo.getChoices().isEmpty()) {
                repositoryCombo.setModelObject(repositoryCombo.getChoices().get(0));
            }
            
            add(new TextField<>("query", String.class));
            
            LambdaAjaxSubmitLink searchLink = new LambdaAjaxSubmitLink("submitSearch",
                    this::actionSearch);
            add(searchLink);
            setDefaultButton(searchLink);
        }
        
        private void actionSearch(AjaxRequestTarget aTarget, Form<?> aForm)
        {
            SearchFormModel model = getModelObject();

            try {
                dataProvider.searchDocuments(model.repository, model.query);
            }
            catch (Exception e) {
                LOG.error("Unable to perform query", e);
                error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
                aTarget.addChildren(getPage(), IFeedback.class);
            }

            applicationEventPublisher.get()
                    .publishEvent(new ExternalSearchQueryEvent(this, model.repository.getProject(),
                            userRepository.getCurrentUser().getUsername(), model.query));

            aTarget.add(dataTableContainer);
        }
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
            
            // FIXME: Should display all highlights
            String highlight = "NO MATCH PREVIEW AVAILABLE";
            if (!result.getHighlights().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("<ul>");
                for (ExternalSearchHighlight h : result.getHighlights()) {
                    sb.append("<li>").append(Utilities.cleanHighlight(h.getHighlight()))
                            .append("</li>");
                }
                sb.append("</ul>");
                highlight = sb.toString();
            }
            add(new Label("highlight", highlight).setEscapeModelStrings(false));
            
            LambdaAjaxLink link = new LambdaAjaxLink("titleLink", _target -> {
                PageParameters pageParameters = new PageParameters()
                    .add(DocumentDetailsPage.REPOSITORY_ID, result.getRepository().getId())
                    .add(DocumentDetailsPage.COLLECTION_ID, result.getCollectionId())
                    .add(DocumentDetailsPage.DOCUMENT_ID, result.getDocumentId());
                setResponsePage(DocumentDetailsPage.class, pageParameters);

            });
            
            String title = defaultIfBlank(result.getDocumentTitle(),
                            defaultIfBlank(result.getDocumentId(), 
                            defaultIfBlank(result.getOriginalUri(), "<no title>")));
            boolean existsSourceDocument = documentService.existsSourceDocument(project,
                    result.getDocumentId());
            
            link.add(new Label("title", title));
            add(link);

            add(new Label("score", result.getScore()));
            add(new Label("importStatus", () ->
                    existsSourceDocument ? "imported" : "not imported"));
            add(new LambdaAjaxLink("importLink", _target -> actionImportDocument(_target, result))
                    .add(visibleWhen(() -> !existsSourceDocument)));
            
            add(LinkProvider
                    .createDocumentPageLink(documentService, project, result.getDocumentId(),
                            "openLink", AnnotationPage.class)
                    .add(visibleWhen(() -> existsSourceDocument)));
        }
    }
}

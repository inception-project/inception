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
package de.tudarmstadt.ukp.inception.app.ui.externalsearch;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;
import static de.tudarmstadt.ukp.inception.app.ui.externalsearch.DocumentDetailsPage.PAGE_PARAM_COLLECTION_ID;
import static de.tudarmstadt.ukp.inception.app.ui.externalsearch.DocumentDetailsPage.PAGE_PARAM_DOCUMENT_ID;
import static de.tudarmstadt.ukp.inception.app.ui.externalsearch.DocumentDetailsPage.PAGE_PARAM_REPOSITORY_ID;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
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
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxSubmitLink;
import de.tudarmstadt.ukp.clarin.webanno.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.app.ui.externalsearch.utils.DocumentImporter;
import de.tudarmstadt.ukp.inception.app.ui.externalsearch.utils.Utilities;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchHighlight;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchService;
import de.tudarmstadt.ukp.inception.externalsearch.event.ExternalSearchQueryEvent;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;

@MountPath(NS_PROJECT + "/${" + PAGE_PARAM_PROJECT + "}/search")
public class SearchPage
    extends ProjectPageBase
{
    private static final long serialVersionUID = 4090656233059899062L;

    private @SpringBean DocumentService documentService;
    private @SpringBean ExternalSearchService externalSearchService;
    private @SpringBean UserDao userRepository;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisher;
    private @SpringBean DocumentImporter documentImporter;

    private WebMarkupContainer dataTableContainer;

    ExternalResultDataProvider dataProvider;

    public SearchPage(final PageParameters aParameters)
    {
        super(aParameters);

        User user = userRepository.getCurrentUser();

        requireProjectRole(user, ANNOTATOR, CURATOR);

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
                Item rowItem = cellItem.findParent(Item.class);
                int rowIndex = rowItem.getIndex();
                ResultRowView rowView = new ResultRowView(componentId, rowIndex + 1, model);
                cellItem.add(rowView);
            }
        });

        dataProvider = new ExternalResultDataProvider(externalSearchService, user);

        dataTableContainer = new WebMarkupContainer("dataTableContainer");
        dataTableContainer.setOutputMarkupId(true);
        add(dataTableContainer);

        dataTableContainer.add(new DefaultDataTable<>("resultsTable", columns, dataProvider, 10));
    }

    private void actionImportDocument(AjaxRequestTarget aTarget, ExternalSearchResult aResult)
    {
        try {
            documentImporter.importDocumentFromDocumentRepository(userRepository.getCurrentUser(),
                    getProject(), aResult.getCollectionId(), aResult.getDocumentId(),
                    aResult.getRepository());
            aTarget.add(dataTableContainer);
        }
        catch (IOException e) {
            LOG.error(e.getMessage(), e);
            error(e.getMessage() + " - " + ExceptionUtils.getRootCauseMessage(e));
            aTarget.addChildren(getPage(), IFeedback.class);
        }
    }

    private class SearchFormModel
        implements Serializable
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

            DropDownChoice<DocumentRepository> repositoryCombo = new DropDownChoice<>("repository");
            repositoryCombo.setChoices(LoadableDetachableModel
                    .of(() -> externalSearchService.listDocumentRepositories(getProject())));
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
                            userRepository.getCurrentUsername(), model.query));

            aTarget.add(dataTableContainer);
        }
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

            PageParameters titleLinkPageParameters = new PageParameters();
            ProjectPageBase.setProjectPageParameter(titleLinkPageParameters, getProject());
            titleLinkPageParameters.set(PAGE_PARAM_REPOSITORY_ID, result.getRepository().getId());
            titleLinkPageParameters.set(PAGE_PARAM_COLLECTION_ID, result.getCollectionId());
            titleLinkPageParameters.set(PAGE_PARAM_DOCUMENT_ID, result.getDocumentId());
            Link<Void> link = new BookmarkablePageLink<>("titleLink", DocumentDetailsPage.class,
                    titleLinkPageParameters);
            link.add(new Label("title",
                    defaultIfBlank(result.getDocumentTitle(), defaultIfBlank(result.getDocumentId(),
                            defaultIfBlank(result.getOriginalUri(), "<no title>")))));
            add(link);

            boolean existsSourceDocument = documentService.existsSourceDocument(getProject(),
                    result.getDocumentId());

            add(new Label("score", result.getScore()));
            add(new Label("importStatus",
                    () -> existsSourceDocument ? "imported" : "not imported"));
            add(new LambdaAjaxLink("importLink", _target -> actionImportDocument(_target, result))
                    .add(visibleWhen(() -> !existsSourceDocument)));

            PageParameters openLinkPageParameters = new PageParameters();
            ProjectPageBase.setProjectPageParameter(openLinkPageParameters, getProject());
            openLinkPageParameters.set(AnnotationPage.PAGE_PARAM_DOCUMENT, result.getDocumentId());
            Link<Void> openLink = new BookmarkablePageLink<>("openLink", AnnotationPage.class,
                    openLinkPageParameters);
            openLink.add(visibleWhen(() -> existsSourceDocument));
            add(openLink);

            // add(LinkProvider
            // .createDocumentPageLink(documentService, getProject(), result.getDocumentId(),
            // "openLink", AnnotationPage.class)
            // .add(visibleWhen(() -> existsSourceDocument)));
        }
    }
}

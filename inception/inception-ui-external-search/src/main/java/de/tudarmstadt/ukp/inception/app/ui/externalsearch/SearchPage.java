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
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;

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
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
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
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchResult;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchService;
import de.tudarmstadt.ukp.inception.externalsearch.event.ExternalSearchQueryEvent;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxSubmitLink;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationEventPublisherHolder;

@MountPath(NS_PROJECT + "/${" + PAGE_PARAM_PROJECT + "}/search")
public class SearchPage
    extends ProjectPageBase
{
    private static final long serialVersionUID = 4090656233059899062L;

    private static final Logger LOG = LoggerFactory.getLogger(SearchPage.class);

    private @SpringBean ExternalSearchService externalSearchService;
    private @SpringBean UserDao userRepository;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisher;

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
                ResultRowView rowView = new ResultRowView(componentId, rowIndex + 1,
                        getProjectModel(), model);
                cellItem.add(rowView);
            }
        });

        dataProvider = new ExternalResultDataProvider(externalSearchService, user);

        dataTableContainer = new WebMarkupContainer("dataTableContainer");
        dataTableContainer.setOutputMarkupId(true);
        add(dataTableContainer);

        dataTableContainer.add(new DefaultDataTable<>("resultsTable", columns, dataProvider, 10));
    }

    @OnEvent
    public void onExternalDocumentImportedEvent(ExternalDocumentImportedEvent aEvent)
    {
        aEvent.getTarget().add(dataTableContainer);
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

            var searchLink = new LambdaAjaxSubmitLink<Void>("submitSearch", this::actionSearch);
            add(searchLink);
            setDefaultButton(searchLink);
        }

        private void actionSearch(AjaxRequestTarget aTarget, Form<Void> aForm)
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
}

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
package de.tudarmstadt.ukp.inception.app.ui.search.sidebar;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateChangeFlag.EXPLICIT_ANNOTATOR_USER_ACTION;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhenNot;
import static java.lang.Math.min;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.uima.fit.util.CasUtil.selectAt;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

import de.agilecoders.wicket.core.markup.html.bootstrap.navigation.BootstrapPagingNavigator.Size;
import de.agilecoders.wicket.core.markup.html.bootstrap.navigation.ajax.BootstrapAjaxPagingNavigator;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.events.BulkAnnotationEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.CreateSpanAnnotationRequest;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanAdapter;
import de.tudarmstadt.ukp.inception.app.ui.search.sidebar.options.CreateAnnotationsOptions;
import de.tudarmstadt.ukp.inception.app.ui.search.sidebar.options.DeleteAnnotationsOptions;
import de.tudarmstadt.ukp.inception.app.ui.search.sidebar.options.SearchOptions;
import de.tudarmstadt.ukp.inception.bootstrap.IconToggleBox;
import de.tudarmstadt.ukp.inception.documents.api.DocumentAccess;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.search.ResultsGroup;
import de.tudarmstadt.ukp.inception.search.SearchResult;
import de.tudarmstadt.ukp.inception.search.SearchService;
import de.tudarmstadt.ukp.inception.search.config.SearchProperties;
import de.tudarmstadt.ukp.inception.search.event.SearchQueryEvent;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;
import de.tudarmstadt.ukp.inception.support.wicket.AjaxDownloadLink;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

public class SearchAnnotationSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final String MID_LIMITED_TO_CURRENT_DOCUMENT = "limitedToCurrentDocument";
    private static final String MID_EXPORT = "export";
    private static final String MID_CLEAR_BUTTON = "clearButton";
    private static final String MID_TOGGLE_DELETE_OPTIONS_VISIBILITY = "toggleDeleteOptionsVisibility";
    private static final String MID_DELETE_ONLY_MATCHING_FEATURE_VALUES = "deleteOnlyMatchingFeatureValues";
    private static final String MID_DELETE_OPTIONS = "deleteOptions";
    private static final String MID_DELETE_BUTTON = "deleteButton";
    private static final String MID_TOGGLE_CREATE_OPTIONS_VISIBILITY = "toggleCreateOptionsVisibility";
    private static final String MID_OVERRIDE_EXISTING_ANNOTATIONS = "overrideExistingAnnotations";
    private static final String MID_CREATE_OPTIONS = "createOptions";
    private static final String MID_ANNOTATE_ALL_BUTTON = "annotateAllButton";
    private static final String MID_SELECT_ALL_IN_GROUP = "selectAllInGroup";
    private static final String MID_GROUP_TITLE = "groupTitle";
    private static final String MID_SEARCH_RESULT_GROUPS = "searchResultGroups";
    private static final String MID_RESULTS_TABLE = "resultsTable";
    private static final String MID_RESULTS_GROUP_CONTAINER = "resultsGroupContainer";
    private static final String MID_SEARCH_FORM = "searchForm";
    private static final String MID_SEARCH_OPTIONS_FORM = "searchOptionsForm";
    private static final String MID_MAIN_CONTAINER = "mainContainer";
    private static final String MID_ANNOTATE_FORM = "annotateForm";
    private static final String MID_NUMBER_OF_RESULTS = "numberOfResults";
    private static final String MID_PAGING_NAVIGATOR = "pagingNavigator";

    private static final long serialVersionUID = -3358207848681467993L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @SpringBean DocumentService documentService;
    private @SpringBean DocumentAccess documentAccess;
    private @SpringBean AnnotationSchemaService schemaService;
    private @SpringBean SearchService searchService;
    private @SpringBean UserDao userRepository;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisher;
    private @SpringBean SearchProperties searchProperties;
    private @SpringBean WorkloadManagementService workloadService;
    private @SpringBean PreferencesService preferencesService;

    private WebMarkupContainer mainContainer;
    private WebMarkupContainer resultsGroupContainer;
    private WebMarkupContainer resultsTable;

    private CompoundPropertyModel<SearchOptions> searchOptions;
    private IModel<CreateAnnotationsOptions> createOptions;
    private IModel<DeleteAnnotationsOptions> deleteOptions;
    private IModel<List<SearchHistoryItem>> history;

    private DropDownChoice<AnnotationFeature> groupingFeatureChoice;
    private CheckBox lowLevelPagingCheckBox;
    private Label resultCountLabel;

    private DataView<ResultsGroup> resultsView;
    private SearchHistoryPanel historyPanel;
    private SearchResultsProviderWrapper resultsProvider;

    // UI elements for bulk annotation
    private Form<Void> annotationForm;
    private Form<CreateAnnotationsOptions> annotationOptionsForm;
    private LambdaAjaxLink createOptionsLink;
    private LambdaAjaxButton<Void> deleteButton;
    private Form<DeleteAnnotationsOptions> deleteOptionsForm;
    private LambdaAjaxButton<Void> annotateButton;
    private LambdaAjaxLink deleteOptionsLink;

    public SearchAnnotationSidebar(String aId, AnnotationActionHandler aActionHandler,
            CasProvider aCasProvider, AnnotationPageBase2 aAnnotationPage)
    {
        super(aId, aActionHandler, aCasProvider, aAnnotationPage);
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        history = SearchHistoryMetaDataKey.get(getPage());
        resultsProvider = SearchResultsProviderMetaDataKey.get(getPage(), searchService);
        searchOptions = SearchOptionsMetaDataKey.get(getPage());
        createOptions = CreateAnnotationsOptionsMetaDataKey.get(getPage());
        deleteOptions = DeleteAnnotationsOptionsMetaDataKey.get(getPage());

        var historyState = preferencesService.loadTraitsForUserAndProject(
                SearchHistoryState.KEY_SEARCH_HISTORY, userRepository.getCurrentUser(),
                getAnnotationPage().getProject());
        history.setObject(historyState.getHistoryItems());

        mainContainer = new WebMarkupContainer(MID_MAIN_CONTAINER);
        mainContainer.setOutputMarkupId(true);
        queue((historyPanel = new SearchHistoryPanel("history", history)) //
                .onSelectAction(this::actionSelectHistoryItem) //
                .onTogglePinAction(this::toggleHistoryItemPin) //
                .onDeleteAction(this::actionDeleteHistoryItem) //
                .setOutputMarkupId(true));
        queue(createSearchOptionsForm(MID_SEARCH_OPTIONS_FORM));
        queue(createSearchForm(MID_SEARCH_FORM));
        queue(mainContainer);

        queue(new IconToggleBox(MID_LIMITED_TO_CURRENT_DOCUMENT) //
                .setCheckedIcon(FontAwesome5IconType.file_s)
                .setCheckedTitle(Model.of("Search in current document"))
                .setUncheckedIcon(FontAwesome5IconType.copy_s)
                .setUncheckedTitle(Model.of("Search in all documents"))
                .setModel(searchOptions.bind(MID_LIMITED_TO_CURRENT_DOCUMENT))
                .add(visibleWhen(() -> workloadService
                        .getWorkloadManagerExtension(
                                getAnnotationPage().getModelObject().getProject())
                        .isDocumentRandomAccessAllowed(
                                getAnnotationPage().getModelObject().getProject())))
                .add(new LambdaAjaxFormComponentUpdatingBehavior()));

        resultsTable = new WebMarkupContainer(MID_RESULTS_TABLE);
        resultsTable.setOutputMarkupPlaceholderTag(true);
        resultsTable.add(visibleWhen(() -> resultsView.getItemCount() > 0));
        queue(resultsTable);

        resultsGroupContainer = new WebMarkupContainer(MID_RESULTS_GROUP_CONTAINER);
        resultsGroupContainer.setOutputMarkupPlaceholderTag(true);
        queue(resultsGroupContainer);

        var noDataNotice = new WebMarkupContainer("noDataNotice");
        noDataNotice.setOutputMarkupPlaceholderTag(true);
        noDataNotice.add(
                visibleWhen(() -> resultsView.getItemCount() == 0 && resultsProvider.isEmpty()));
        queue(noDataNotice);

        resultsView = new DataView<ResultsGroup>(MID_SEARCH_RESULT_GROUPS, resultsProvider)
        {
            private static final long serialVersionUID = -631500052426449048L;

            @Override
            protected void populateItem(Item<ResultsGroup> item)
            {
                var result = item.getModelObject();
                item.add(new Label(MID_GROUP_TITLE,
                        LoadableDetachableModel.of(() -> groupSizeLabelValue(result))));
                item.add(createGroupLevelSelectionCheckBox(MID_SELECT_ALL_IN_GROUP,
                        result.getGroupKey()));
                item.add(new SearchResultGroup("group", "resultGroup", SearchAnnotationSidebar.this,
                        result.getGroupKey(), Model.of(result)));
            }
        };
        searchOptions.getObject().setItemsPerPage(searchProperties.getPageSizes()[0]);
        resultsView.setItemsPerPage(searchOptions.getObject().getItemsPerPage());
        queue(resultsView);

        queue(resultCountLabel = createNumberOfResults(MID_NUMBER_OF_RESULTS));
        queue(createPagingNavigator(MID_PAGING_NAVIGATOR));

        // create annotate-button and options form
        annotateButton = new LambdaAjaxButton<>(MID_ANNOTATE_ALL_BUTTON,
                (target, form) -> actionApplyToSelectedResults(target,
                        this::createAnnotationAtSearchResult));
        queue(annotateButton);

        annotationOptionsForm = new Form<>(MID_CREATE_OPTIONS, createOptions);
        queue(new CheckBox(MID_OVERRIDE_EXISTING_ANNOTATIONS));
        annotationOptionsForm
                .add(visibleWhen(() -> annotationOptionsForm.getModelObject().isVisible()));
        annotationOptionsForm.setOutputMarkupPlaceholderTag(true);
        queue(annotationOptionsForm);

        createOptionsLink = new LambdaAjaxLink(MID_TOGGLE_CREATE_OPTIONS_VISIBILITY, _target -> {
            annotationOptionsForm.getModelObject().toggleVisibility();
            _target.add(annotationOptionsForm);
        });
        queue(createOptionsLink);

        // create delete-button and options form
        deleteButton = new LambdaAjaxButton<>(MID_DELETE_BUTTON,
                (target, form) -> actionApplyToSelectedResults(target,
                        this::deleteAnnotationAtSearchResult));
        queue(deleteButton);

        deleteOptionsForm = new Form<>(MID_DELETE_OPTIONS, deleteOptions);
        deleteOptionsForm.add(new CheckBox(MID_DELETE_ONLY_MATCHING_FEATURE_VALUES));
        deleteOptionsForm.add(visibleWhen(() -> deleteOptionsForm.getModelObject().isVisible()));
        deleteOptionsForm.setOutputMarkupPlaceholderTag(true);
        queue(deleteOptionsForm);

        deleteOptionsLink = new LambdaAjaxLink(MID_TOGGLE_DELETE_OPTIONS_VISIBILITY, _target -> {
            deleteOptionsForm.getModelObject().toggleVisibility();
            _target.add(deleteOptionsForm);
        });
        queue(deleteOptionsLink);

        annotationForm = new Form<>(MID_ANNOTATE_FORM);
        annotationForm.setOutputMarkupPlaceholderTag(true);
        annotationForm.add(visibleWhen(() -> resultsView.getItemCount() > 0));
        annotationForm.setDefaultButton(annotateButton);

        var clearButton = new LambdaAjaxLink(MID_CLEAR_BUTTON, this::actionClearResults);
        clearButton.add(visibleWhenNot(resultsProvider::isEmpty));
        queue(clearButton);

        var exportButton = new AjaxDownloadLink(MID_EXPORT, () -> "searchResults.csv",
                this::exportSearchResults);
        exportButton.add(visibleWhen(() -> resultsView.getItemCount() > 0));
        queue(exportButton);

        queue(annotationForm);
    }

    private Label createNumberOfResults(String aId)
    {
        var label = new Label(aId);
        label.setOutputMarkupId(true);
        label.setDefaultModel(LoadableDetachableModel.of(() -> {
            var first = resultsView.getFirstItemOffset();
            var total = resultsView.getItemCount();
            return format("%d-%d / %d", first + 1,
                    min(first + resultsView.getItemsPerPage(), total), total);
        }));
        label.add(visibleWhen(() -> resultsView.getItemCount() > 0));
        return label;
    }

    private BootstrapAjaxPagingNavigator createPagingNavigator(String aId)
    {
        var pagingNavigator = new BootstrapAjaxPagingNavigator(aId, resultsView)
        {
            private static final long serialVersionUID = 853561772299520056L;

            @Override
            protected void onAjaxEvent(AjaxRequestTarget aTarget)
            {
                super.onAjaxEvent(aTarget);
                aTarget.add(resultCountLabel);
            }
        };
        pagingNavigator.setSize(Size.Small);
        pagingNavigator.add(LambdaBehavior
                .onConfigure(() -> pagingNavigator.getPagingNavigation().setViewSize(1)));
        pagingNavigator.add(visibleWhen(() -> resultsView.getItemCount() > 0));
        return pagingNavigator;
    }

    private Form<Void> createSearchForm(String aId)
    {
        var searchForm = new Form<Void>(aId);

        var searchButton = new LambdaAjaxButton<>("search", this::actionSearch);
        searchButton.setOutputMarkupId(true);
        searchForm.add(searchButton);
        searchForm.setDefaultButton(searchButton);

        var queryInput = new TextArea<>("queryInput", searchOptions.bind("query"));
        queryInput.setOutputMarkupId(true);

        searchForm.add(queryInput);

        // Add JavaScript to handle Ctrl + Enter
        searchForm.add(new Behavior()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void renderHead(Component component, IHeaderResponse response)
            {
                super.renderHead(component, response);
                // This will find the Wicket-generated Ajax callback
                String js = String.format("""
                        document.getElementById('%s').addEventListener('keydown', function(event) {
                            if (!event.shiftKey && event.key === 'Enter') {
                                var btn = Wicket.$('%s');
                                console.log(btn);
                                if (btn) {
                                    btn.click();
                                }
                                event.preventDefault(); // Prevents default behavior
                            }
                        });
                        """, queryInput.getMarkupId(), searchButton.getMarkupId());

                response.render(OnDomReadyHeaderItem.forScript(js));
            }
        });

        return searchForm;
    }

    private Form<SearchOptions> createSearchOptionsForm(String aId)
    {
        var searchOptionsForm = new Form<>(aId, searchOptions);
        searchOptionsForm.add(createLayerDropDownChoice("groupingLayer",
                schemaService.listAnnotationLayer(getModelObject().getProject())));

        groupingFeatureChoice = new DropDownChoice<>("groupingFeature", emptyList(),
                new ChoiceRenderer<>("uiName"));
        groupingFeatureChoice.setNullValid(true);
        groupingFeatureChoice.add(new LambdaAjaxFormComponentUpdatingBehavior());
        searchOptionsForm.add(groupingFeatureChoice);

        searchOptionsForm.add(createResultsPerPageSelection("itemsPerPage"));
        searchOptionsForm.add(lowLevelPagingCheckBox = createLowLevelPagingCheckBox());
        searchOptionsForm.setOutputMarkupPlaceholderTag(true);

        return searchOptionsForm;
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        setChangeAnnotationsElementsEnabled(getAnnotationPage().isEditable());
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        aResponse.render(CssHeaderItem.forReference(SearchAnnotationSidebarCssReference.get()));
    }

    private void setChangeAnnotationsElementsEnabled(boolean aEnabled)
    {
        annotationOptionsForm.setEnabled(aEnabled);
        createOptionsLink.setEnabled(aEnabled);
        deleteButton.setEnabled(aEnabled);
        deleteOptionsForm.setEnabled(aEnabled);
        annotateButton.setEnabled(aEnabled);
        deleteOptionsLink.setEnabled(aEnabled);
    }

    private String groupSizeLabelValue(ResultsGroup aResultsGroup)
    {
        var sb = new StringBuilder();
        sb.append(aResultsGroup.getGroupKey() + " (" + aResultsGroup.getResults().size());
        if (!resultsProvider.applyLowLevelPaging()) {
            sb.append("/" + resultsProvider.groupSize(aResultsGroup.getGroupKey()));
        }
        sb.append(")");
        return sb.toString();
    }

    private DropDownChoice<Long> createResultsPerPageSelection(String aId)
    {
        var choices = Arrays.stream(searchProperties.getPageSizes()).boxed().toList();

        var dropdown = new DropDownChoice<>(aId, choices);
        dropdown.add(new LambdaAjaxFormComponentUpdatingBehavior());
        return dropdown;
    }

    private DropDownChoice<AnnotationLayer> createLayerDropDownChoice(String aId,
            List<AnnotationLayer> aChoices)
    {
        var layerChoice = new DropDownChoice<AnnotationLayer>(aId, aChoices,
                new ChoiceRenderer<>("uiName"));

        layerChoice.add(new AjaxFormComponentUpdatingBehavior("change")
        {
            private static final long serialVersionUID = -6095969211884063787L;

            @Override
            protected void onUpdate(AjaxRequestTarget aTarget)
            {
                // update the choices for the feature selection dropdown
                groupingFeatureChoice.setChoices(schemaService
                        .listAnnotationFeature(searchOptions.getObject().getGroupingLayer()));
                lowLevelPagingCheckBox.setModelObject(false);
                aTarget.add(groupingFeatureChoice, lowLevelPagingCheckBox);
            }
        });
        layerChoice.setNullValid(true);
        return layerChoice;
    }

    private CheckBox createLowLevelPagingCheckBox()
    {
        var checkbox = new CheckBox("lowLevelPaging");
        checkbox.setOutputMarkupId(true);
        checkbox.add(enabledWhen(() -> searchOptions.getObject().getGroupingLayer() == null
                && searchOptions.getObject().getGroupingFeature() == null));
        checkbox.add(AttributeModifier.append("title",
                new StringResourceModel("lowLevelPagingMouseover", this)));
        checkbox.add(new LambdaAjaxFormComponentUpdatingBehavior());
        return checkbox;
    }

    private AjaxCheckBox createGroupLevelSelectionCheckBox(String aId, String aGroupKey)
    {
        return new AjaxCheckBox(aId, Model.of(true))
        {
            private static final long serialVersionUID = 2431702654443882657L;

            @Override
            protected void onUpdate(AjaxRequestTarget target)
            {
                for (var resultsGroup : resultsProvider.getGroupedResults()) {
                    if (resultsGroup.getGroupKey().equals(aGroupKey)) {
                        resultsGroup.getResults().stream() //
                                .filter(r -> !r.isReadOnly()) //
                                .forEach(r -> r.setSelectedForAnnotation(getModelObject()));
                    }
                }
                target.add(resultsGroupContainer);
            }

            @Override
            protected void onConfigure()
            {
                super.onConfigure();

                var group = resultsProvider.getGroupedResults().stream() //
                        .filter(rg -> rg.getGroupKey().equals(aGroupKey)) //
                        .findFirst();

                if (!group.isPresent()) {
                    return;
                }

                setVisible(group.get().getResults().stream().anyMatch(r -> !r.isReadOnly()));

                setModelObject(group.get().getResults().stream() //
                        .filter(r -> !r.isReadOnly()) //
                        .allMatch(SearchResult::isSelectedForAnnotation));
            }
        };
    }

    private void actionSearch(AjaxRequestTarget aTarget, Form<Void> aForm)
    {
        var opt = searchOptions.getObject();

        if (isBlank(opt.getQuery())) {
            resultsProvider.emptyQuery();
            return;
        }

        var state = getModelObject();
        var project = state.getProject();

        var maybeProgress = searchService.getIndexProgress(project);
        if (maybeProgress.isPresent()) {
            var p = maybeProgress.get();
            info("Indexing in progress... cannot perform query at this time. " + p.percent() + "% ("
                    + p.progress() + "/" + p.maxProgress() + ")");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        // If a layer is selected but no feature show error
        if (opt.getGroupingLayer() != null && opt.getGroupingFeature() == null) {
            error("A feature has to be selected in order to group by feature values. If you want to group by document title, select none for both layer and feature.");
            resultsProvider.emptyQuery();
            return;
        }

        var limitToDocument = state.getDocument();
        if (workloadService.getWorkloadManagerExtension(project)
                .isDocumentRandomAccessAllowed(project) && !opt.isLimitedToCurrentDocument()) {
            limitToDocument = null;
        }

        var request = SearchRequest.builder() //
                .withDataOwner(getModelObject().getUser()) //
                .withProject(project) //
                .withQuery(opt.getQuery()) //
                .withLimitToDocument(limitToDocument) //
                .withGroupingLayer(opt.getGroupingLayer()) //
                .withGroupingFeature(opt.getGroupingFeature()) //
                .withLowLevelPaging(opt.isLowLevelPaging()) //
                .build();

        executeSearch(aTarget, request);
    }

    private IResourceStream exportSearchResults()
    {
        return new AbstractResourceStream()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public InputStream getInputStream() throws ResourceStreamNotFoundException
            {
                try {
                    var exporter = new SearchResultsExporter();
                    return exporter.generateCsv(resultsProvider.getGroupedResults());
                }
                catch (Exception e) {
                    LOG.error("Unable to generate search results csv", e);
                    error("Error: " + e.getMessage());
                    throw new ResourceStreamNotFoundException(e);
                }
            }

            @Override
            public void close() throws IOException
            {
                // Nothing to do
            }
        };
    }

    private void actionClearResults(AjaxRequestTarget aTarget)
    {
        searchOptions.getObject().setQuery("");
        resultsProvider.emptyQuery();
        searchOptions.getObject().setSelectedResult(null);
        aTarget.add(mainContainer);

        // Need to re-render because we want to highlight the match
        getAnnotationPage().actionRefreshDocument(aTarget);
    }

    private void executeSearch(AjaxRequestTarget aTarget, SearchRequest aRequest)
    {
        try {
            var opt = searchOptions.getObject();
            opt.setSelectedResult(null);
            resultsView.setItemsPerPage(opt.getItemsPerPage());

            resultsProvider.initializeQuery(aRequest);

            aTarget.add(mainContainer);
            aTarget.addChildren(getPage(), IFeedback.class);

            // Need to re-render because we want to highlight the match
            getAnnotationPage().actionRefreshDocument(aTarget);

            updateSearchHistory(aTarget, aRequest);

            var sessionOwner = userRepository.getCurrentUser();
            var historyState = new SearchHistoryState();
            historyState.setHistoryItems(history.getObject());
            preferencesService.saveTraitsForUserAndProject(SearchHistoryState.KEY_SEARCH_HISTORY,
                    sessionOwner, aRequest.project(), historyState);

            applicationEventPublisher.get()
                    .publishEvent(new SearchQueryEvent(this, aRequest.project(),
                            aRequest.dataOwner().getUsername(), aRequest.query(),
                            aRequest.limitToDocument()));
        }
        catch (Exception e) {
            error("Query error: " + e.getMessage());
            aTarget.addChildren(getPage(), IFeedback.class);
            resultsProvider.emptyQuery();
        }
    }

    private void updateSearchHistory(AjaxRequestTarget aTarget, SearchRequest aRequest)
    {
        var newItem = SearchHistoryItem.builder() //
                .withQuery(aRequest.query()) //
                .withLimitToDocument(aRequest.limitToDocument() != null) //
                .withGroupingLayer(aRequest.groupingLayer()) //
                .withGroupingFeature(aRequest.groupingFeature()) //
                .withLowLevelPaging(aRequest.lowLevelPaging()) //
                .build();

        var source = new ArrayList<>(history.getObject());
        var target = history.getObject();
        target.clear();

        // If the item already exists, we keep the existing item as it may be pinned
        // and we do not want to lose the pinned state
        var i = source.indexOf(newItem);
        if (i >= 0) {
            newItem = source.get(i);
        }
        target.add(newItem);

        // Now preserve the rest
        int nonPinned = 0;
        for (var item : source) {
            if (newItem.equals(item)) {
                continue;
            }

            if (item.pinned()) {
                target.add(item);
            }
            else if (nonPinned < 10) {
                target.add(item);
                nonPinned++;
            }
        }

        aTarget.add(historyPanel);
    }

    public void actionApplyToSelectedResults(AjaxRequestTarget aTarget, Operation aConsumer)
    {
        aTarget.addChildren(getPage(), IFeedback.class);
        if (VID.NONE_ID.equals(getModelObject().getSelection().getAnnotation())) {
            error("No annotation selected. Please select an annotation first");
            getAnnotationPage().actionRefreshDocument(aTarget);
            return;
        }

        var sessionOwner = userRepository.getCurrentUser();
        var dataOwner = getAnnotationPage().getModelObject().getUser();
        var layer = getModelObject().getSelectedAnnotationLayer();
        try {
            var adapter = (SpanAdapter) schemaService.getAdapter(layer);
            adapter.silenceEvents();

            // Group the results by document such that we can process one CAS at a time
            var resultsByDocument = resultsProvider.getGroupedResults().stream()
                    // the grouping can be based on some other strategy than the document, so
                    // we re-group here
                    .flatMap(group -> group.getResults().stream())
                    .collect(groupingBy(SearchResult::getDocumentId));

            var bulkResult = new BulkOperationResult();

            var state = getModelObject();
            for (var resultsGroup : resultsByDocument.entrySet()) {
                var documentId = resultsGroup.getKey();
                var sourceDoc = documentService.getSourceDocument(state.getProject().getId(),
                        documentId);

                if (!canAccessDocument(sessionOwner, sourceDoc, dataOwner)) {
                    continue;
                }

                var annoDoc = documentService.createOrGetAnnotationDocument(sourceDoc, dataOwner);

                // Holder for lazily-loaded CAS
                Optional<CAS> cas = Optional.empty();

                // Apply bulk operations to all hits from this document
                for (var result : resultsGroup.getValue()) {
                    if (result.isReadOnly() || !result.isSelectedForAnnotation()) {
                        continue;
                    }

                    if (!cas.isPresent()) {
                        // Lazily load annotated document
                        cas = Optional
                                .of(documentService.readAnnotationCas(annoDoc, AUTO_CAS_UPGRADE));
                    }

                    aConsumer.apply(sourceDoc, cas.get(), adapter, result, bulkResult);
                }

                // Persist annotated document
                if (cas.isPresent()) {
                    writeJCasAndUpdateTimeStamp(sourceDoc, cas.get());
                }
            }

            if (bulkResult.created > 0) {
                success("Created annotations: " + bulkResult.created);
            }
            if (bulkResult.updated > 0) {
                success("Updated annotations: " + bulkResult.updated);
            }
            if (bulkResult.deleted > 0) {
                success("Deleted annotations: " + bulkResult.deleted);
            }
            if (bulkResult.conflict > 0) {
                warn("Annotations skipped due to conflicts: " + bulkResult.conflict);
            }

            if (bulkResult.created == 0 && bulkResult.updated == 0 && bulkResult.deleted == 0) {
                info("No changes");
            }

            applicationEventPublisher.get().publishEvent(new BulkAnnotationEvent(this,
                    getModelObject().getProject(), dataOwner.getUsername(), layer));
        }
        catch (ClassCastException e) {
            error("Can only create SPAN annotations for search results.");
            LOG.error("Can only create SPAN annotations for search results", e);
        }
        catch (Exception e) {
            error("Unable to apply action to search results: " + e.getMessage());
            LOG.error("Unable to apply action to search results: ", e);
        }

        getAnnotationPage().actionRefreshDocument(aTarget);
    }

    private boolean canAccessDocument(User sessionOwner, SourceDocument sourceDoc, User dataOwner)
    {
        try {
            documentAccess.assertCanEditAnnotationDocument(sessionOwner, sourceDoc,
                    dataOwner.getUsername());
            return true;
        }
        catch (AccessDeniedException e) {
            return false;
        }
    }

    private void createAnnotationAtSearchResult(SourceDocument aDocument, CAS aCas,
            SpanAdapter aAdapter, SearchResult aSearchResult, BulkOperationResult aBulkResult)
        throws AnnotationException
    {
        var state = getModelObject();
        var layer = aAdapter.getLayer();

        var type = CasUtil.getAnnotationType(aCas, aAdapter.getAnnotationTypeName());
        var annoFS = selectAt(aCas, type, aSearchResult.getOffsetStart(),
                aSearchResult.getOffsetEnd()).stream().findFirst().orElse(null);

        var overrideExisting = createOptions.getObject().isOverrideExistingAnnotations();

        // if there is already an annotation of the same type at the target location
        // and we don't want to override it and stacking is not enabled, do nothing.
        if (annoFS != null && !overrideExisting && !layer.isAllowStacking()) {
            return;
        }

        var match = false;

        // create a new annotation if not already there or if stacking is enabled and the
        // new annotation has different features than the existing one
        for (var eannoFS : selectAt(aCas, type, aSearchResult.getOffsetStart(),
                aSearchResult.getOffsetEnd())) {
            if (overrideExisting) {
                setFeatureValues(aDocument, aCas, aAdapter, state, eannoFS);
                aBulkResult.updated++;
            }
            else if (featureValuesMatchCurrentState(eannoFS)) {
                match = true;
            }
        }

        if (annoFS == null || (!match && !overrideExisting)) {
            try {
                annoFS = aAdapter.handle(CreateSpanAnnotationRequest.builder() //
                        .withDocument(aDocument, state.getUser().getUsername(), aCas) //
                        .withRange(aSearchResult.getOffsetStart(), aSearchResult.getOffsetEnd()) //
                        .withAnchoringMode(state.getAnchoringMode()) //
                        .build());
                aBulkResult.created++;
            }
            catch (AnnotationException e) {
                aBulkResult.conflict++;
                return;
            }

            // set values for all features according to current state
            setFeatureValues(aDocument, aCas, aAdapter, state, annoFS);
        }
    }

    private void setFeatureValues(SourceDocument aDocument, CAS aCas, SpanAdapter aAdapter,
            AnnotatorState state, AnnotationFS annoFS)
        throws AnnotationException
    {
        var addr = ICasUtil.getAddr(annoFS);
        for (var featureState : state.getFeatureStates()) {
            var featureValue = featureState.value;
            var feature = featureState.feature;

            // Ignore slot features - cf. https://github.com/inception-project/inception/issues/2505
            if (feature.getLinkMode() != LinkMode.NONE) {
                continue;
            }

            if (featureValue != null) {
                aAdapter.setFeatureValue(aDocument, state.getUser().getUsername(), aCas, addr,
                        feature, featureValue);
            }
        }
    }

    private void deleteAnnotationAtSearchResult(SourceDocument aDocument, CAS aCas,
            SpanAdapter aAdapter, SearchResult aSearchResult, BulkOperationResult aBulkResult)
        throws AnnotationException
    {
        var dataOwner = getAnnotationPage().getModelObject().getUser();
        var type = CasUtil.getAnnotationType(aCas, aAdapter.getAnnotationTypeName());

        for (var annoFS : selectAt(aCas, type, aSearchResult.getOffsetStart(),
                aSearchResult.getOffsetEnd())) {
            if ((annoFS != null && featureValuesMatchCurrentState(annoFS))
                    || !deleteOptions.getObject().isDeleteOnlyMatchingFeatureValues()) {
                aAdapter.delete(aDocument, dataOwner.getUsername(), aCas, VID.of(annoFS));
                aBulkResult.deleted++;
            }
        }
    }

    private void writeJCasAndUpdateTimeStamp(SourceDocument aSourceDoc, CAS aCas)
        throws IOException, AnnotationException
    {
        var state = getModelObject();

        if (Objects.equals(state.getDocument().getId(), aSourceDoc.getId())) {
            // Updating the currently open document is done through the page in order to notify the
            // mechanism to detect concurrent modifications.
            getAnnotationPage().writeEditorCas(aCas);
            return;
        }

        documentService.writeAnnotationCas(aCas, aSourceDoc, AnnotationSet.forUser(state.getUser()),
                EXPLICIT_ANNOTATOR_USER_ACTION);
    }

    private boolean featureValuesMatchCurrentState(AnnotationFS aAnnotationFS)
    {
        var aAdapter = (SpanAdapter) schemaService
                .getAdapter(getModelObject().getSelectedAnnotationLayer());
        for (var state : getModelObject().getFeatureStates()) {
            var featureValue = state.value;
            var feature = state.feature;

            // Ignore slot features - cf. https://github.com/inception-project/inception/issues/2505
            if (feature.getLinkMode() != LinkMode.NONE) {
                continue;
            }

            var valueAtFS = aAdapter.getFeatureValue(feature, aAnnotationFS);
            if (!Objects.equals(valueAtFS, featureValue)) {
                return false;
            }
        }
        return true;
    }

    private void toggleHistoryItemPin(AjaxRequestTarget aTarget, ListItem<SearchHistoryItem> aItem)
    {
        var item = aItem.getModelObject();
        var index = history.getObject().indexOf(item);
        if (index >= 0) {
            var toggled = item.togglePin();
            aItem.setModelObject(toggled);
            history.getObject().set(index, toggled);
            saveSearchHistory();
        }
        aTarget.add(aItem);
    }

    private void actionSelectHistoryItem(AjaxRequestTarget aTarget, SearchHistoryItem aItem)
    {
        var opt = searchOptions.getObject();
        var project = getModelObject().getProject();

        var layer = aItem.groupingLayer() != null
                ? schemaService.findLayer(project, aItem.groupingLayer())
                : null;
        var feature = aItem.groupingFeature() != null
                ? schemaService.getFeature(aItem.groupingFeature(), layer)
                : null;

        opt.setSelectedResult(null);
        opt.setQuery(aItem.query());
        opt.setGroupingLayer(layer);
        opt.setGroupingFeature(feature);
        opt.setLimitedToCurrentDocument(aItem.limitToDocument());
        opt.setLowLevelPaging(aItem.lowLevelPaging());

        resultsView.setItemsPerPage(opt.getItemsPerPage());

        actionSearch(aTarget, null);
    }

    private void actionDeleteHistoryItem(AjaxRequestTarget aTarget, SearchHistoryItem aItem)
    {
        var index = history.getObject().indexOf(aItem);
        if (index >= 0) {
            history.getObject().remove(index);
            saveSearchHistory();
        }
        aTarget.add(historyPanel);
    }

    private void saveSearchHistory()
    {
        var historyState = new SearchHistoryState();
        historyState.setHistoryItems(history.getObject());
        var sessionOwner = userRepository.getCurrentUser();
        var project = getModelObject().getProject();
        preferencesService.saveTraitsForUserAndProject(SearchHistoryState.KEY_SEARCH_HISTORY,
                sessionOwner, project, historyState);
    }

    private class SearchResultGroup
        extends Fragment
    {
        private static final long serialVersionUID = 3540041356505975132L;

        public SearchResultGroup(String aId, String aMarkupId, MarkupContainer aMarkupProvider,
                String groupKey, IModel<ResultsGroup> aModel)
        {
            super(aId, aMarkupId, aMarkupProvider, aModel);

            var statementList = new ListView<SearchResult>("results")
            {
                private static final long serialVersionUID = 5811425707843441458L;

                @Override
                protected void populateItem(ListItem<SearchResult> aItem)
                {
                    var currentProject = SearchAnnotationSidebar.this.getModel().getObject()
                            .getProject();
                    var result = aItem.getModelObject();

                    var lambdaAjaxLink = new LambdaAjaxLink("showSelectedDocument",
                            t -> actionJumpToResult(aItem, currentProject, t));
                    aItem.add(lambdaAjaxLink);

                    var selected = new AjaxCheckBox("selected",
                            Model.of(result.isSelectedForAnnotation()))
                    {
                        private static final long serialVersionUID = -6955396602403459129L;

                        @Override
                        protected void onUpdate(AjaxRequestTarget target)
                        {
                            var modelObject = aItem.getModelObject();
                            modelObject.setSelectedForAnnotation(getModelObject());
                            if (!getModelObject()) {
                                // not all results in the document are selected, so set document
                                // level selection to false
                                target.add(resultsGroupContainer);
                            }
                        }
                    };
                    selected.setVisible(!result.isReadOnly());
                    aItem.add(selected);

                    try {
                        var doc = documentService.getSourceDocument(currentProject,
                                result.getDocumentTitle());
                        var cas = documentService.createOrReadInitialCas(doc, AUTO_CAS_UPGRADE,
                                SHARED_READ_ONLY_ACCESS);
                        var text = cas.getDocumentText();
                        var begin = result.getOffsetStart();
                        int end = result.getOffsetEnd();
                        var precedingContext = cas.select(Token.class).preceding(begin).limit(3)
                                .asList();
                        var preBegin = precedingContext.isEmpty() ? begin
                                : precedingContext.get(0).getBegin();
                        var trailingContext = cas.select(Token.class).following(end).limit(3)
                                .asList();
                        var trailEnd = trailingContext.isEmpty() ? end
                                : trailingContext.get(trailingContext.size() - 1).getEnd();
                        var leftContext = text.substring(preBegin, begin);
                        var match = text.substring(begin, end);
                        var rightContext = text.substring(end, trailEnd);
                        lambdaAjaxLink.add(new Label("leftContext", leftContext));
                        lambdaAjaxLink.add(new Label("match", match));
                        lambdaAjaxLink.add(new Label("rightContext", rightContext));
                    }
                    catch (Exception e) {
                        lambdaAjaxLink.add(new Label("leftContext", result.getLeftContext()));
                        lambdaAjaxLink.add(new Label("match", result.getText()));
                        lambdaAjaxLink.add(new Label("rightContext", result.getRightContext()));
                    }
                }

                private void actionJumpToResult(ListItem<SearchResult> aItem,
                        Project currentProject, AjaxRequestTarget t)
                    throws IOException
                {
                    var selectedResult = aItem.getModelObject();
                    searchOptions.getObject().setSelectedResult(selectedResult);
                    getAnnotationPage().actionShowSelectedDocument(t,
                            documentService.getSourceDocument(currentProject,
                                    selectedResult.getDocumentTitle()),
                            selectedResult.getOffsetStart(), selectedResult.getOffsetEnd());
                    // Need to re-render because we want to highlight the match
                    getAnnotationPage().actionRefreshDocument(t);
                }
            };
            statementList
                    .setModel(LoadableDetachableModel.of(() -> aModel.getObject().getResults()));
            add(statementList);
        }
    }

    @FunctionalInterface
    private interface Operation
    {
        void apply(SourceDocument aSourceDoc, CAS aCas, SpanAdapter aAdapter, SearchResult aResult,
                BulkOperationResult aBulkResult)
            throws AnnotationException;
    }

    private class BulkOperationResult
    {
        int created = 0;
        int deleted = 0;
        int updated = 0;
        int conflict = 0;
    }
}

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

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateChangeFlag.EXPLICIT_ANNOTATOR_USER_ACTION;
import static de.tudarmstadt.ukp.inception.rendering.vmodel.VMarker.MATCH_FOCUS;
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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
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
import org.wicketstuff.event.annotation.OnEvent;

import de.agilecoders.wicket.core.markup.html.bootstrap.navigation.BootstrapPagingNavigator.Size;
import de.agilecoders.wicket.core.markup.html.bootstrap.navigation.ajax.BootstrapAjaxPagingNavigator;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.annotation.events.BulkAnnotationEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanAdapter;
import de.tudarmstadt.ukp.inception.app.ui.search.sidebar.options.CreateAnnotationsOptions;
import de.tudarmstadt.ukp.inception.app.ui.search.sidebar.options.DeleteAnnotationsOptions;
import de.tudarmstadt.ukp.inception.app.ui.search.sidebar.options.SearchOptions;
import de.tudarmstadt.ukp.inception.bootstrap.IconToggleBox;
import de.tudarmstadt.ukp.inception.documents.api.DocumentAccess;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.pipeline.RenderAnnotationsEvent;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VRange;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VTextMarker;
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
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean SearchService searchService;
    private @SpringBean UserDao userRepository;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisher;
    private @SpringBean SearchProperties searchProperties;
    private @SpringBean WorkloadManagementService workloadService;

    private final WebMarkupContainer mainContainer;
    private final WebMarkupContainer resultsGroupContainer;
    private final WebMarkupContainer resultsTable;
    private final SearchResultsProviderWrapper resultsProvider;

    private IModel<String> targetQuery = Model.of("");
    private CompoundPropertyModel<SearchOptions> searchOptions = CompoundPropertyModel
            .of(new SearchOptions());
    private IModel<SearchResultsPagesCache> groupedResults = Model
            .of(new SearchResultsPagesCache());
    private IModel<CreateAnnotationsOptions> createOptions = CompoundPropertyModel
            .of(new CreateAnnotationsOptions());
    private IModel<DeleteAnnotationsOptions> deleteOptions = CompoundPropertyModel
            .of(new DeleteAnnotationsOptions());
    private DataView<ResultsGroup> searchResultGroups;

    private DropDownChoice<AnnotationFeature> groupingFeature;
    private CheckBox lowLevelPagingCheckBox;
    private Label numberOfResults;

    private SearchResult selectedResult;

    // UI elements for annotation changes
    private final Form<CreateAnnotationsOptions> annotationOptionsForm;
    private final LambdaAjaxLink createOptionsLink;
    private final LambdaAjaxButton<Void> deleteButton;

    private final Form<DeleteAnnotationsOptions> deleteOptionsForm;
    private final LambdaAjaxButton<Void> annotateButton;
    private final LambdaAjaxLink deleteOptionsLink;
    private final Form<Void> annotationForm;

    public SearchAnnotationSidebar(String aId, AnnotationActionHandler aActionHandler,
            CasProvider aCasProvider, AnnotationPageBase2 aAnnotationPage)
    {
        super(aId, aActionHandler, aCasProvider, aAnnotationPage);

        resultsProvider = new SearchResultsProviderWrapper(
                new SearchResultsProvider(searchService, groupedResults),
                searchOptions.bind("isLowLevelPaging"));

        mainContainer = new WebMarkupContainer(MID_MAIN_CONTAINER);
        mainContainer.setOutputMarkupId(true);
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
                        .getWorkloadManagerExtension(aAnnotationPage.getModelObject().getProject())
                        .isDocumentRandomAccessAllowed(
                                aAnnotationPage.getModelObject().getProject())))
                .add(new LambdaAjaxFormComponentUpdatingBehavior()));

        resultsProvider.emptyQuery();

        resultsTable = new WebMarkupContainer(MID_RESULTS_TABLE);
        resultsTable.setOutputMarkupPlaceholderTag(true);
        resultsTable.add(visibleWhen(() -> searchResultGroups.getItemCount() > 0));
        queue(resultsTable);

        resultsGroupContainer = new WebMarkupContainer(MID_RESULTS_GROUP_CONTAINER);
        resultsGroupContainer.setOutputMarkupPlaceholderTag(true);
        queue(resultsGroupContainer);

        var noDataNotice = new WebMarkupContainer("noDataNotice");
        noDataNotice.setOutputMarkupPlaceholderTag(true);
        noDataNotice.add(visibleWhen(() -> searchResultGroups.getItemCount() == 0
                && groupedResults.map(_gr -> !_gr.isEmpty()).orElse(true).getObject()));
        queue(noDataNotice);

        searchResultGroups = new DataView<ResultsGroup>(MID_SEARCH_RESULT_GROUPS, resultsProvider)
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
        searchResultGroups.setItemsPerPage(searchOptions.getObject().getItemsPerPage());
        queue(searchResultGroups);

        queue(numberOfResults = createNumberOfResults(MID_NUMBER_OF_RESULTS));
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
        annotationForm.add(visibleWhen(() -> searchResultGroups.getItemCount() > 0));
        annotationForm.setDefaultButton(annotateButton);

        var clearButton = new LambdaAjaxLink(MID_CLEAR_BUTTON, this::actionClearResults);
        clearButton.add(visibleWhenNot(groupedResults.map(SearchResultsPagesCache::isEmpty)));
        queue(clearButton);

        var exportButton = new AjaxDownloadLink(MID_EXPORT, () -> "searchResults.csv",
                this::exportSearchResults);
        exportButton.add(visibleWhen(() -> searchResultGroups.getItemCount() > 0));
        queue(exportButton);

        queue(annotationForm);
    }

    private Label createNumberOfResults(String aId)
    {
        var label = new Label(aId);
        label.setOutputMarkupId(true);
        label.setDefaultModel(LoadableDetachableModel.of(() -> {
            var first = searchResultGroups.getFirstItemOffset();
            var total = searchResultGroups.getItemCount();
            return format("%d-%d / %d", first + 1,
                    min(first + searchResultGroups.getItemsPerPage(), total), total);
        }));
        label.add(visibleWhen(() -> searchResultGroups.getItemCount() > 0));
        return label;
    }

    private BootstrapAjaxPagingNavigator createPagingNavigator(String aId)
    {
        var pagingNavigator = new BootstrapAjaxPagingNavigator(aId, searchResultGroups)
        {
            private static final long serialVersionUID = 853561772299520056L;

            @Override
            protected void onAjaxEvent(AjaxRequestTarget aTarget)
            {
                super.onAjaxEvent(aTarget);
                aTarget.add(numberOfResults);
            }
        };
        pagingNavigator.setSize(Size.Small);
        pagingNavigator.add(LambdaBehavior
                .onConfigure(() -> pagingNavigator.getPagingNavigation().setViewSize(1)));
        pagingNavigator.add(visibleWhen(() -> searchResultGroups.getItemCount() > 0));
        return pagingNavigator;
    }

    private Form<Void> createSearchForm(String aId)
    {
        var searchForm = new Form<Void>(aId);
        searchForm.add(new TextArea<>("queryInput", targetQuery));

        var searchButton = new LambdaAjaxButton<>("search", this::actionSearch);
        searchForm.add(searchButton);
        searchForm.setDefaultButton(searchButton);

        return searchForm;
    }

    private Form<SearchOptions> createSearchOptionsForm(String aId)
    {
        var searchOptionsForm = new Form<>(aId, searchOptions);
        searchOptionsForm.add(createLayerDropDownChoice("groupingLayer",
                annotationService.listAnnotationLayer(getModelObject().getProject())));

        groupingFeature = new DropDownChoice<>("groupingFeature", emptyList(),
                new ChoiceRenderer<>("uiName"));
        groupingFeature.setNullValid(true);
        groupingFeature.add(new LambdaAjaxFormComponentUpdatingBehavior());
        searchOptionsForm.add(groupingFeature);

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
                groupingFeature.setChoices(annotationService
                        .listAnnotationFeature(searchOptions.getObject().getGroupingLayer()));
                lowLevelPagingCheckBox.setModelObject(false);
                aTarget.add(groupingFeature, lowLevelPagingCheckBox);
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
                for (var resultsGroup : groupedResults.getObject().allResultsGroups()) {
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

                var group = groupedResults.getObject().allResultsGroups().stream() //
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

    private void actionSearch(AjaxRequestTarget aTarget, Form<SearchOptions> aForm)
    {
        selectedResult = null;
        searchResultGroups.setItemsPerPage(searchOptions.getObject().getItemsPerPage());
        executeSearchResultsGroupedQuery(aTarget);
        aTarget.add(mainContainer);
        aTarget.addChildren(getPage(), IFeedback.class);
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
                    return exporter.generateCsv(resultsProvider.getAllResults());
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
        targetQuery.setObject("");
        resultsProvider.emptyQuery();
        selectedResult = null;
        aTarget.add(mainContainer);
    }

    private void executeSearchResultsGroupedQuery(AjaxRequestTarget aTarget)
    {
        if (isBlank(targetQuery.getObject())) {
            resultsProvider.emptyQuery();
            return;
        }

        var state = getModelObject();
        var project = state.getProject();

        var maybeProgress = searchService.getIndexProgress(project);
        if (maybeProgress.isPresent()) {
            var p = maybeProgress.get();
            info("Indexing in progress... cannot perform query at this time. " + p.percent() + "% ("
                    + p.getDone() + "/" + p.getTotal() + ")");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        // If a layer is selected but no feature show error
        if (searchOptions.getObject().getGroupingLayer() != null
                && searchOptions.getObject().getGroupingFeature() == null) {
            error("A feature has to be selected in order to group by feature values. If you want to group by document title, select none for both layer and feature.");
            resultsProvider.emptyQuery();
            return;
        }

        try {
            var limitToDocument = state.getDocument();
            if (workloadService.getWorkloadManagerExtension(project).isDocumentRandomAccessAllowed(
                    project) && !searchOptions.getObject().isLimitedToCurrentDocument()) {
                limitToDocument = null;
            }

            applicationEventPublisher.get().publishEvent(new SearchQueryEvent(this, project,
                    state.getUser().getUsername(), targetQuery.getObject(), limitToDocument));
            var opt = searchOptions.getObject();
            resultsProvider.initializeQuery(getModelObject().getUser(), project,
                    targetQuery.getObject(), limitToDocument, opt.getGroupingLayer(),
                    opt.getGroupingFeature());
            return;
        }
        catch (Exception e) {
            error("Query error: " + e.getMessage());
            aTarget.addChildren(getPage(), IFeedback.class);
            resultsProvider.emptyQuery();
            return;
        }
    }

    @OnEvent
    public void onRenderAnnotations(RenderAnnotationsEvent aEvent)
    {
        if (selectedResult != null) {
            var range = VRange.clippedRange(aEvent.getVDocument(), selectedResult.getOffsetStart(),
                    selectedResult.getOffsetEnd());

            range.ifPresent(r -> aEvent.getVDocument().add(new VTextMarker(MATCH_FOCUS, r)));
        }
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
            var adapter = (SpanAdapter) annotationService.getAdapter(layer);
            adapter.silenceEvents();

            // Group the results by document such that we can process one CAS at a time
            var resultsByDocument = groupedResults.getObject().allResultsGroups().stream()
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
                annoFS = aAdapter.add(aDocument, state.getUser().getUsername(), aCas,
                        aSearchResult.getOffsetStart(), aSearchResult.getOffsetEnd());
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

        documentService.writeAnnotationCas(aCas, aSourceDoc, state.getUser().getUsername(),
                EXPLICIT_ANNOTATOR_USER_ACTION);
    }

    private boolean featureValuesMatchCurrentState(AnnotationFS aAnnotationFS)
    {
        var aAdapter = (SpanAdapter) annotationService
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

                    var lambdaAjaxLink = new LambdaAjaxLink("showSelectedDocument", t -> {
                        selectedResult = aItem.getModelObject();
                        actionShowSelectedDocument(t,
                                documentService.getSourceDocument(currentProject,
                                        selectedResult.getDocumentTitle()),
                                selectedResult.getOffsetStart(), selectedResult.getOffsetEnd());
                        // Need to re-render because we want to highlight the match
                        getAnnotationPage().actionRefreshDocument(t);
                    });
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

                    lambdaAjaxLink.add(new Label("leftContext", result.getLeftContext()));
                    lambdaAjaxLink.add(new Label("match", result.getText()));
                    lambdaAjaxLink.add(new Label("rightContext", result.getRightContext()));
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
        public int created = 0;
        public int deleted = 0;
        public int updated = 0;
        public int conflict = 0;
    }
}

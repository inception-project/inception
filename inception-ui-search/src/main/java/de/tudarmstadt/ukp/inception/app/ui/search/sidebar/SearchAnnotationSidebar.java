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
package de.tudarmstadt.ukp.inception.app.ui.search.sidebar;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.uima.fit.util.CasUtil.selectAt;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.feedback.IFeedback;
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
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.BulkAnnotationEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.event.RenderAnnotationsEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VMarker;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VTextMarker;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.app.ui.search.sidebar.options.CreateAnnotationsOptions;
import de.tudarmstadt.ukp.inception.app.ui.search.sidebar.options.DeleteAnnotationsOptions;
import de.tudarmstadt.ukp.inception.app.ui.search.sidebar.options.SearchOptions;
import de.tudarmstadt.ukp.inception.search.SearchResult;
import de.tudarmstadt.ukp.inception.search.SearchService;
import de.tudarmstadt.ukp.inception.search.event.SearchQueryEvent;

public class SearchAnnotationSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = -3358207848681467993L;

    private static final Logger LOG = LoggerFactory.getLogger(SearchAnnotationSidebar.class);

    private @SpringBean DocumentService documentService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean SearchService searchService;
    private @SpringBean UserDao userRepository;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisher;

    private User currentUser;

    private final WebMarkupContainer mainContainer;
    private final WebMarkupContainer resultsGroupContainer;

    private IModel<String> targetQuery = Model.of("");
    private IModel<SearchOptions> searchOptions = CompoundPropertyModel.of(new SearchOptions());
    private IModel<Map<String, ResultsGroup>> groupedSearchResults;
    private Map<String, Boolean> groupLevelSelections;
    private IModel<CreateAnnotationsOptions> createOptions = CompoundPropertyModel
        .of(new CreateAnnotationsOptions());
    private IModel<DeleteAnnotationsOptions> deleteOptions = CompoundPropertyModel
        .of(new DeleteAnnotationsOptions());

    DropDownChoice<AnnotationFeature> groupingFeature = new BootstrapSelect<>("groupingFeature",
        Collections.emptyList(), new ChoiceRenderer<>("uiName"));

    private SearchResult selectedResult;

    public SearchAnnotationSidebar(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider,
            AnnotationPage aAnnotationPage)
    {
        super(aId, aModel, aActionHandler, aCasProvider, aAnnotationPage);

        currentUser = userRepository.getCurrentUser();
        
        mainContainer = new WebMarkupContainer("mainContainer");
        mainContainer.setOutputMarkupId(true);
        add(mainContainer);

        Form<Void> searchForm = new Form<Void>("searchForm");
        searchForm.add(new TextArea<>("queryInput", targetQuery));
        LambdaAjaxButton<Void> searchButton = new LambdaAjaxButton<>("search", this::actionSearch);
        searchForm.add(searchButton);
        searchForm.setDefaultButton(searchButton);
        mainContainer.add(searchForm);

        Form<SearchOptions> searchOptionsForm = new Form<>("searchOptionsForm", searchOptions);
        searchOptionsForm.add(new CheckBox("limitedToCurrentDocument"));
        searchOptionsForm.add(createLayerDropDownChoice("groupingLayer",
            annotationService.listAnnotationLayer(getModelObject().getProject())));
        searchOptionsForm.add(groupingFeature);
        groupingFeature.setNullValid(true);
        searchOptionsForm.add(visibleWhen(() -> searchOptionsForm.getModelObject().isVisible()));
        searchOptionsForm.setOutputMarkupPlaceholderTag(true);
        searchForm.add(searchOptionsForm);

        searchForm.add(new LambdaAjaxLink("toggleOptionsVisibility", _target -> {
            searchOptionsForm.getModelObject().toggleVisibility();
            _target.add(searchOptionsForm);
        }));

        groupedSearchResults = LambdaModel.of(this::getSearchResultsGrouped);

        // Add link for re-indexing the project
        searchOptionsForm.add(new LambdaAjaxLink("reindexProject", t -> {
            Project project = ((IModel<AnnotatorState>) t.getPage().getDefaultModel()).getObject()
                    .getProject();
            searchService.reindex(project);
        }));

        resultsGroupContainer = new WebMarkupContainer("resultsGroupContainer");
        resultsGroupContainer.setOutputMarkupId(true);
        mainContainer.add(resultsGroupContainer);

        ListView<ResultsGroup> searchResultGroups = new ListView<ResultsGroup>("searchResultGroups")
        {
            private static final long serialVersionUID = -631500052426449048L;

            @Override
            protected void populateItem(ListItem<ResultsGroup> item)
            {
                ResultsGroup result = item.getModelObject();
                item.add(new Label("groupTitle", LoadableDetachableModel
                        .of(() -> result.getGroupKey() + " (" + result.getResults().size() + ")")));
                item.add(createGroupLevelSelectionCheckBox("selectAllInGroup",
                    Model.of(groupLevelSelections.get(result.getGroupKey())),
                    result.getGroupKey()));
                item.add(new SearchResultGroup("group", "resultGroup", SearchAnnotationSidebar.this,
                        result.getGroupKey(), LambdaModel.of(() -> 
                        groupedSearchResults.getObject().get(result.getGroupKey()))));
            }
        };
        searchResultGroups.setModel(LoadableDetachableModel.of(() -> 
                groupedSearchResults.getObject().values().stream()
                        .sorted(Comparator.comparing(ResultsGroup::getGroupKey))
                        .collect(Collectors.toList())));
        resultsGroupContainer.add(searchResultGroups);

        Form<Void> annotationForm = new Form<>("annotateForm");
        // create annotate-button and options form
        LambdaAjaxButton<Void> annotateButton = new LambdaAjaxButton<>("annotateAllButton",
            (target, form) -> actionApplyToSelectedResults(target,
                this::createAnnotationAtSearchResult));
        annotationForm.add(annotateButton);

        Form<CreateAnnotationsOptions> annotationOptionsForm = new Form<>("createOptions",
            createOptions);
        annotationOptionsForm.add(new CheckBox("overrideExistingAnnotations"));
        annotationOptionsForm
            .add(visibleWhen(() -> annotationOptionsForm.getModelObject().isVisible()));
        annotationOptionsForm.setOutputMarkupPlaceholderTag(true);
        annotationForm.add(annotationOptionsForm);

        annotationForm.add(new LambdaAjaxLink("toggleCreateOptionsVisibility", _target -> {
            annotationOptionsForm.getModelObject().toggleVisibility();
            _target.add(annotationOptionsForm);
        }));

        // create delete-button and options form
        LambdaAjaxButton<Void> deleteButton = new LambdaAjaxButton<>("deleteButton",
            (target, from) -> actionApplyToSelectedResults(target,
                this::deleteAnnotationAtSearchResult));
        annotationForm.add(deleteButton);

        Form<DeleteAnnotationsOptions> deleteOptionsForm = new Form<>("deleteOptions",
            deleteOptions);
        deleteOptionsForm.add(new CheckBox("deleteOnlyMatchingFeatureValues"));
        deleteOptionsForm.add(visibleWhen(() -> deleteOptionsForm.getModelObject().isVisible()));
        deleteOptionsForm.setOutputMarkupPlaceholderTag(true);
        annotationForm.add(deleteOptionsForm);

        annotationForm.add(new LambdaAjaxLink("toggleDeleteOptionsVisibility", _target -> {
            deleteOptionsForm.getModelObject().toggleVisibility();
            _target.add(deleteOptionsForm);
        }));

        annotationForm.setDefaultButton(annotateButton);
        annotationForm.add(visibleWhen(() -> !groupedSearchResults.getObject().isEmpty()));

        
        LambdaAjaxButton<Void> clearButton = new LambdaAjaxButton<>("clearButton",
                this::actionClearResults);
        annotationForm.add(clearButton);
        
        mainContainer.add(annotationForm);
    }

    private Map<String, Boolean> initGroupLevelSelections(
        Set<String> groupKeys)
    {
        Map<String, Boolean> selections = new HashMap<>();
        for (String key : groupKeys) {
            selections.put(key, true);
        }
        return selections;
    }

    private DropDownChoice<AnnotationLayer> createLayerDropDownChoice(String aId,
        List<AnnotationLayer> aChoices)
    {
        DropDownChoice<AnnotationLayer> layerChoice = new BootstrapSelect<>(aId, aChoices,
            new ChoiceRenderer<>("uiName"));

        layerChoice.add(new AjaxFormComponentUpdatingBehavior("change")
        {
            private static final long serialVersionUID = -6095969211884063787L;

            @Override protected void onUpdate(AjaxRequestTarget aTarget)
            {
                //update the choices for the feature selection dropdown
                groupingFeature.setChoices(annotationService
                    .listAnnotationFeature(searchOptions.getObject().getGroupingLayer()));
                aTarget.add(groupingFeature);
            }
        });
        layerChoice.setNullValid(true);
        return layerChoice;
    }

    private AjaxCheckBox createGroupLevelSelectionCheckBox(String aId, IModel<Boolean> aModel,
        String aGroupKey)
    {
        AjaxCheckBox selectAllCheckBox = new AjaxCheckBox(aId, aModel)
        {
            private static final long serialVersionUID = 2431702654443882657L;

            @Override
            protected void onUpdate(AjaxRequestTarget target)
            {
                for (Entry<String, ResultsGroup> entry : groupedSearchResults.getObject()
                    .entrySet()) {
                    if (entry.getKey().equals(aGroupKey)) {
                        entry.getValue().getResults().stream()
                            .forEach(r -> r.setSelectedForAnnotation(getModelObject()));
                    }
                }
                groupLevelSelections.put(aGroupKey, getModelObject());
                target.add(resultsGroupContainer);
            }
        };
        return selectAllCheckBox;
    }

    private void actionSearch(AjaxRequestTarget aTarget, Form<Void> aForm)
    {
        selectedResult = null;
        groupedSearchResults.detach();
        aTarget.add(mainContainer);
        aTarget.addChildren(getPage(), IFeedback.class);
    }
    
    private void actionClearResults(AjaxRequestTarget aTarget, Form<Void> aForm)
    {
        targetQuery.setObject("");
        selectedResult = null;
        groupedSearchResults.detach();
        aTarget.add(mainContainer);
    }
    
    private Map<String, ResultsGroup> getSearchResultsGrouped()
    {
        if (isBlank(targetQuery.getObject())) {
            return Collections.emptyMap();
        }

        // If a layer is selected but no feature show error
        if (searchOptions.getObject().getGroupingLayer() != null
            && searchOptions.getObject().getGroupingFeature() == null) {
            error(
                "A feature has to be selected in order to group by feature values. If you want to group by document title, select none for both layer and feature.");
            return Collections.emptyMap();
        }
        
        try {
            AnnotatorState state = getModelObject();
            Project project = state.getProject();
            SourceDocument limitToDocument = searchOptions.getObject().isLimitedToCurrentDocument()
                    ? state.getDocument()
                    : null;
            applicationEventPublisher.get().publishEvent(new SearchQueryEvent(this, project,
                    currentUser.getUsername(), targetQuery.getObject(), limitToDocument));
            SearchOptions opt = searchOptions.getObject();
            Map<String, ResultsGroup> queryResults = searchService
                    .query(currentUser, project, targetQuery.getObject(), limitToDocument,
                            opt.getGroupingLayer(), opt.getGroupingFeature())
                    .entrySet().stream().collect(Collectors.toMap(Entry::getKey, e -> 
                            new ResultsGroup(e.getKey(), e.getValue())));

            // init group level selection as soon as we know what the group-keys are
            groupLevelSelections = initGroupLevelSelections(queryResults.keySet());
            return queryResults;
        }
        catch (Exception e) {
            error("Error in the query: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    @OnEvent
    public void onRenderAnnotations(RenderAnnotationsEvent aEvent)
    {
        if (selectedResult != null) {
            AnnotatorState state = aEvent.getState();
            if (state.getWindowBeginOffset() <= selectedResult.getOffsetStart()
                    && selectedResult.getOffsetEnd() <= state.getWindowEndOffset()) {
                aEvent.getVDocument()
                        .add(new VTextMarker(VMarker.MATCH_FOCUS,
                                selectedResult.getOffsetStart() - state.getWindowBeginOffset(),
                                selectedResult.getOffsetEnd() - state.getWindowBeginOffset()));
            }
        }
    }

    public void actionApplyToSelectedResults(AjaxRequestTarget aTarget,
            Operation aConsumer)
    {
        if (VID.NONE_ID.equals(getModelObject().getSelection().getAnnotation())) {
            error("No annotation selected. Please select an annotation first");
        }
        else {
            AnnotationLayer layer = getModelObject().getSelectedAnnotationLayer();
            try {
                SpanAdapter adapter = (SpanAdapter) annotationService.getAdapter(layer);
                adapter.silenceEvents();
                
                // Group the results by document such that we can process one CAS at a time
                Map<Long, List<SearchResult>> resultsByDocument = groupedSearchResults.getObject()
                        .values().stream()
                        // the grouping can be based on some other strategy than the document, so
                        // we re-group here
                        .flatMap(group -> group.getResults().stream())
                        .collect(groupingBy(SearchResult::getDocumentId));
                
                BulkOperationResult bulkResult = new BulkOperationResult();
                
                AnnotatorState state = getModelObject();
                for (Entry<Long, List<SearchResult>> resultsGroup : resultsByDocument.entrySet()) {
                    long documentId = resultsGroup.getKey();
                    SourceDocument sourceDoc = documentService
                            .getSourceDocument(state.getProject().getId(), documentId);

                    AnnotationDocument annoDoc = documentService
                            .createOrGetAnnotationDocument(sourceDoc, currentUser);
                    
                    switch (annoDoc.getState()) {
                    case FINISHED: // fall-through
                    case IGNORE:
                        // Skip processing any documents which are finished or ignored
                        continue;
                    default:
                        // Do nothing
                    }
                    
                    // Load annotated document
                    CAS cas = documentService.readAnnotationCas(sourceDoc,
                            currentUser.getUsername());

                    // Apply bulk operations to all hits from this document
                    for (SearchResult result : resultsGroup.getValue()) {
                        if (result.isReadOnly() || !result.isSelectedForAnnotation()) {
                            continue;
                        }
                        
                        aConsumer.apply(sourceDoc, cas, adapter, result, bulkResult);
                    }

                    // Persist annotated document
                    writeJCasAndUpdateTimeStamp(sourceDoc, cas);
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
                        getModelObject().getProject(), currentUser.getUsername(), layer));
            }
            catch (ClassCastException e) {
                error("Can only create SPAN annotations for search results.");
                LOG.error("Can only create SPAN annotations for search results", e);
            }
            catch (Exception e) {
                error("Unable to apply action to search results: " + e.getMessage());
                LOG.error("Unable to apply action to search results: ", e);
            }
        }
        
        getAnnotationPage().actionRefreshDocument(aTarget);
    }

    private void createAnnotationAtSearchResult(SourceDocument aDocument, CAS aCas,
            SpanAdapter aAdapter, SearchResult aSearchResult, BulkOperationResult aBulkResult)
        throws AnnotationException
    {
        AnnotatorState state = getModelObject();
        AnnotationLayer layer = aAdapter.getLayer();
        List<FeatureState> featureStates = state.getFeatureStates();
        
        Type type = CasUtil.getAnnotationType(aCas, aAdapter.getAnnotationTypeName());
        AnnotationFS annoFS = selectAt(aCas, type, aSearchResult.getOffsetStart(),
            aSearchResult.getOffsetEnd()).stream().findFirst().orElse(null);

        boolean overrideExisting = createOptions.getObject().isOverrideExistingAnnotations();

        // if there is already an annotation of the same type at the target location
        // and we don't want to override it and stacking is not enabled, do nothing.
        if (annoFS != null && !overrideExisting && !layer.isAllowStacking()) {
            return;
        }

        // create a new annotation if not already there or if stacking is enabled and the
        // new annotation has different features than the existing one
        if (annoFS == null || (!overrideExisting && !featureValuesMatchCurrentState(annoFS))) {
            try {
                annoFS = aAdapter
                    .add(aDocument, currentUser.getUsername(), aCas, aSearchResult.getOffsetStart(),
                        aSearchResult.getOffsetEnd());
                aBulkResult.created++;
            }
            catch (AnnotationException e) {
                aBulkResult.conflict++;
            }
        }
        else {
            aBulkResult.updated++;
        }

        // set values for all features according to current state
        for (FeatureState featureState : featureStates) {
            Object featureValue = featureState.value;
            AnnotationFeature feature = featureState.feature;
            if (featureValue != null) {
                int addr = getAddr(annoFS);
                aAdapter.setFeatureValue(aDocument, currentUser.getUsername(), aCas, addr,
                        feature, featureValue);
            }
        }
    }

    private void deleteAnnotationAtSearchResult(SourceDocument aDocument, CAS aCas,
            SpanAdapter aAdapter, SearchResult aSearchResult, BulkOperationResult aBulkResult)
    {
        Type type = CasUtil.getAnnotationType(aCas, aAdapter.getAnnotationTypeName());
        AnnotationFS annoFS = selectAt(aCas, type, aSearchResult.getOffsetStart(),
                aSearchResult.getOffsetEnd()).stream().findFirst().orElse(null);

        if (annoFS == null || !featureValuesMatchCurrentState(annoFS)
                && deleteOptions.getObject().isDeleteOnlyMatchingFeatureValues()) {
            return;
        }
        
        aAdapter.delete(aDocument, currentUser.getUsername(), aCas, new VID(annoFS));
        aBulkResult.deleted++;
    }

    private void writeJCasAndUpdateTimeStamp(SourceDocument aSourceDoc, CAS aCas)
        throws IOException
    {
        AnnotatorState state = getModelObject();

        if (Objects.equals(state.getDocument().getId(), aSourceDoc.getId())) {
            // Updating the currently open document is done through the page in order to notify the
            // mechanism to detect concurrent modifications.
            getAnnotationPage().writeEditorCas(aCas);
        }
        else {
            documentService.writeAnnotationCas(aCas, aSourceDoc, currentUser, true);
        }
    }

    private boolean featureValuesMatchCurrentState(AnnotationFS aAnnotationFS)
    {
        SpanAdapter aAdapter = (SpanAdapter) annotationService
            .getAdapter(getModelObject().getSelectedAnnotationLayer());
        for (FeatureState state : getModelObject().getFeatureStates()) {
            Object featureValue = state.value;
            AnnotationFeature feature = state.feature;
            Object valueAtFS = aAdapter.getFeatureValue(feature, aAnnotationFS);
            if (!Objects.equals(valueAtFS, featureValue)) {
                return false;
            }
        }
        return true;
    }

    private class ResultsGroup implements Serializable
    {
        private static final long serialVersionUID = -4448435773623997560L;
        private final String groupKey;
        private final List<SearchResult> results;
        
        public ResultsGroup(String aGroupKey, List<SearchResult> aResults)
        {
            groupKey = aGroupKey;
            results = aResults;
        }
        
        public String getGroupKey()
        {
            return groupKey;
        }
        
        public List<SearchResult> getResults()
        {
            return results;
        }
    }
    
    private class SearchResultGroup
        extends Fragment
    {
        private static final long serialVersionUID = 3540041356505975132L;

        public SearchResultGroup(String aId, String aMarkupId, MarkupContainer aMarkupProvider,
            String groupKey, IModel<ResultsGroup> aModel)
        {
            super(aId, aMarkupId, aMarkupProvider, aModel);
            
            ListView<SearchResult> statementList = new ListView<SearchResult>("results")
            {
                private static final long serialVersionUID = 5811425707843441458L;
    
                @Override
                protected void populateItem(ListItem<SearchResult> aItem)
                {
                    Project currentProject = SearchAnnotationSidebar.this.getModel().getObject()
                            .getProject();
                    SearchResult result = aItem.getModelObject();
                    
                    LambdaAjaxLink lambdaAjaxLink = new LambdaAjaxLink("showSelectedDocument",
                        t -> {
                            selectedResult = aItem.getModelObject();
                            actionShowSelectedDocument(t,
                                    documentService.getSourceDocument(currentProject,
                                            selectedResult.getDocumentTitle()),
                                    selectedResult.getOffsetStart(),
                                    selectedResult.getOffsetEnd());
                        });
                    aItem.add(lambdaAjaxLink);

                    AjaxCheckBox selected = new AjaxCheckBox("selected",
                        Model.of(result.isSelectedForAnnotation()))
                    {
                        private static final long serialVersionUID = -6955396602403459129L;

                        @Override
                        protected void onUpdate(AjaxRequestTarget target)
                        {
                            SearchResult modelObject = aItem.getModelObject();
                            modelObject.setSelectedForAnnotation(getModelObject());
                            if (!getModelObject()) {
                                // not all results in the document are selected, so set document
                                // level selection to false
                                groupLevelSelections.put(groupKey, false);
                                target.add(resultsGroupContainer);
                            }
                        }
                    };
                    selected.setVisible(!result.isReadOnly());
                    aItem.add(selected);

                    String sentence = new String();

                    sentence = sentence + aItem.getModel().getObject().getLeftContext() + "<mark>"
                            + aItem.getModel().getObject().getText() + "</mark>"
                            + aItem.getModel().getObject().getRightContext();

                    lambdaAjaxLink
                            .add(new Label("sentence", sentence).setEscapeModelStrings(false));
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

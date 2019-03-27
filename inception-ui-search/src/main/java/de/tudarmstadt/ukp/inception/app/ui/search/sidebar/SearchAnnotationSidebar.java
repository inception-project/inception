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
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectSingleFsAt;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.event.RenderAnnotationsEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VMarker;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VTextMarker;
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

    private IModel<String> targetQuery = Model.of("");
    private IModel<SearchOptions> searchOptions = CompoundPropertyModel.of(new SearchOptions());
    private IModel<List<SearchResult>> searchResults;
    private Map<String, Boolean> documentLevelSelections = initDocumentLevelSelections();
    private IModel<CreateAnnotationsOptions> createOptions = CompoundPropertyModel
        .of(new CreateAnnotationsOptions());
    private IModel<DeleteAnnotationsOptions> deleteOptions = CompoundPropertyModel
        .of(new DeleteAnnotationsOptions());


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
        searchForm.add(new TextArea<String>("queryInput", targetQuery));
        LambdaAjaxButton<Void> searchButton = new LambdaAjaxButton<>("search", this::actionSearch);
        searchForm.add(searchButton);
        searchForm.setDefaultButton(searchButton);
        mainContainer.add(searchForm);

        Form<SearchOptions> searchOptionsForm = new Form<>("searchOptionsForm", searchOptions);
        searchOptionsForm.add(new CheckBox("limitedToCurrentDocument"));
        searchOptionsForm.add(visibleWhen(() -> searchOptionsForm.getModelObject().isVisible()));
        searchOptionsForm.setOutputMarkupPlaceholderTag(true);
        searchForm.add(searchOptionsForm);

        searchForm.add(new LambdaAjaxLink("toggleOptionsVisibility", _target -> {
            searchOptionsForm.getModelObject().toggleVisibility();
            _target.add(searchOptionsForm);
        }));

        searchResults = LambdaModel.of(this::getSearchResults);
        
        // Add link for re-indexing the project
        searchOptionsForm.add(new LambdaAjaxLink("reindexProject", t -> {
            Project project = ((IModel<AnnotatorState>) t.getPage().getDefaultModel()).getObject()
                    .getProject();
            searchService.reindex(project);
        }));

        ListView<String> searchResultGroups = new ListView<String>("searchResultGroups")
        {
            private static final long serialVersionUID = -631500052426449048L;

            @Override
            protected void populateItem(ListItem<String> item)
            {
                item.add(new Label("documentTitle", LambdaModel.of(() -> item.getModelObject())));
                item.add(createDocumentLevelSelectionCheckBox("selectAllInDoc", Model.of(
                    documentLevelSelections.get(item.getModelObject())),
                    item.getModelObject()));
                item.add(new SearchResultGroup("group", "resultGroup", 
                        SearchAnnotationSidebar.this,
                        LambdaModel.of(() -> searchResults.getObject().stream().filter((result) -> {
                            if (result.getDocumentTitle() == null) {
                                return true;
                            }
                            else {
                                return result.getDocumentTitle().equals(item.getModelObject());
                            }
                        }).collect(Collectors.toList()))));
            }
        };
        searchResultGroups.setModel(LambdaModel.of(() -> 
                searchResults.getObject().stream().map((result -> result.getDocumentTitle()))
                        .distinct().collect(Collectors.toList())));

        mainContainer.add(searchResultGroups);

        Form<Void> annotationForm = new Form("annotateForm");
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
        annotationForm.add(visibleWhen(() -> !searchResults.getObject().isEmpty()));

        mainContainer.add(annotationForm);
    }

    private Map<String, Boolean> initDocumentLevelSelections()
    {
        Map<String, Boolean> docLevelSelections = new HashMap();
        Project project = getModelObject().getProject();
        for (SourceDocument document : documentService.listSourceDocuments(project)) {
            docLevelSelections.put(document.getName(), true);
        }
        return docLevelSelections;
    }

    private AjaxCheckBox createDocumentLevelSelectionCheckBox(String aId, IModel<Boolean> aModel,
        String aDocumentTitle)
    {
        AjaxCheckBox selectAllCheckBox = new AjaxCheckBox(aId, aModel)
        {
            @Override
            protected void onUpdate(AjaxRequestTarget target)
            {
                searchResults.getObject().stream()
                    .filter(r -> r.getDocumentTitle().equals(aDocumentTitle))
                    .forEach(r -> r.setSelectedForAnnotation(getModelObject()));
                documentLevelSelections.put(aDocumentTitle, getModelObject());
                target.add(mainContainer);
            }
        };
        return selectAllCheckBox;
    }

    private void actionSearch(AjaxRequestTarget aTarget, Form<Void> aForm) {
        selectedResult = null;
        searchResults.detach();
        aTarget.add(mainContainer);
        aTarget.addChildren(getPage(), IFeedback.class);
    }
    
    private List<SearchResult> getSearchResults()
    {
        if (isBlank(targetQuery.getObject())) {
            return Collections.emptyList();
        }
        
        try {
            AnnotatorState state = getModelObject();
            Project project = state.getProject();
            SourceDocument limitToDocument = searchOptions.getObject().isLimitedToCurrentDocument()
                    ? state.getDocument()
                    : null;
            applicationEventPublisher.get().publishEvent(new SearchQueryEvent(this, project,
                    currentUser.getUsername(), targetQuery.getObject(), limitToDocument));
            return searchService.query(currentUser, project, targetQuery.getObject(),
                    limitToDocument);
        }
        catch (Exception e) {
            error("Error in the query: " + e.getMessage());
            return Collections.emptyList();
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
        BiConsumer<SearchResult, SpanAdapter> aConsumer)
    {
        AnnotationLayer layer = getModelObject().getSelectedAnnotationLayer();
        try {
            SpanAdapter adapter = (SpanAdapter) annotationService.getAdapter(layer);
            for (SearchResult result : searchResults.getObject()) {
                if (result.isSelectedForAnnotation()) {
                    aConsumer.accept(result, adapter);
                }
            }
        }
        catch (ClassCastException e) {
            error(
                "Can only create SPAN annotations for search results: " + e.getLocalizedMessage());
            LOG.error("Can only create SPAN annotations for search results", e);
        }
        getAnnotationPage().actionRefreshDocument(aTarget);
    }

    private void createAnnotationAtSearchResult(SearchResult searchResult, SpanAdapter aAdapter)
    {
        AnnotatorState state = getModelObject();
        AnnotationLayer layer = aAdapter.getLayer();
        List<FeatureState> featureStates = state.getFeatureStates();
        SourceDocument sourceDoc = documentService
            .getSourceDocument(state.getProject(), searchResult.getDocumentTitle());
        try {
            JCas jCas = documentService.readAnnotationCas(sourceDoc, currentUser.getUsername());

            Type type = CasUtil.getAnnotationType(jCas.getCas(), aAdapter.getAnnotationTypeName());
            AnnotationFS annoFS = selectSingleFsAt(jCas, type, searchResult.getOffsetStart(),
                searchResult.getOffsetEnd());

            boolean overrideExisting = createOptions.getObject().isOverrideExistingAnnotations();

            // if there is already an annotation of the same type at the target location
            // and we don't want to override it and stacking is not enabled, do nothing.
            if (annoFS != null && !overrideExisting && !layer.isAllowStacking()) {
                return;
            }

            // create a new annotation if not already there or if stacking is enabled and the
            // new annotation has different features than the existing one
            if (annoFS == null || !featureValuesMatchCurrentState(annoFS) && !overrideExisting) {
                annoFS = aAdapter
                    .add(sourceDoc, currentUser.getUsername(), jCas, searchResult.getOffsetStart(),
                        searchResult.getOffsetEnd());
            }

            // set values for all features according to current state
            for (FeatureState featureState : featureStates) {
                Object featureValue = featureState.value;
                AnnotationFeature feature = featureState.feature;
                if (featureValue != null) {
                    int addr = getAddr(annoFS);
                    aAdapter
                        .setFeatureValue(sourceDoc, currentUser.getUsername(), jCas, addr, feature,
                            featureValue);
                }
            }

            writeJCasAndUpdateTimeStamp(sourceDoc, jCas);
        }
        catch (IOException | AnnotationException e) {
            error(
                "Unable to create annotation for search result [" + searchResult.toString() + " ]: "
                    + e.getLocalizedMessage());
            LOG.error("Unable to create annotation for search result [" + searchResult.toString()
                + " ]: ", e);
        }
    }

    private void deleteAnnotationAtSearchResult(SearchResult searchResult, SpanAdapter aAdapter)
    {
        AnnotatorState state = getModelObject();
        SourceDocument sourceDoc = documentService
            .getSourceDocument(state.getProject(), searchResult.getDocumentTitle());
        try {
            JCas jCas = documentService.readAnnotationCas(sourceDoc, currentUser.getUsername());

            Type type = CasUtil.getAnnotationType(jCas.getCas(), aAdapter.getAnnotationTypeName());
            AnnotationFS annoFS = selectSingleFsAt(jCas, type, searchResult.getOffsetStart(),
                searchResult.getOffsetEnd());

            if (annoFS == null
                || !featureValuesMatchCurrentState(annoFS) && deleteOptions.getObject()
                .isDeleteOnlyMatchingFeatureValues()) {
                return;
            }
            aAdapter.delete(sourceDoc, currentUser.getUsername(), jCas, new VID(annoFS));

            writeJCasAndUpdateTimeStamp(sourceDoc, jCas);
        }
        catch (IOException e) {
            error(
                "Unable to delete annotation for search result [" + searchResult.toString() + " ]: "
                    + e.getLocalizedMessage());
            LOG.error("Unable to delete annotation for search result [" + searchResult.toString()
                + " ]: ", e);
        }
    }

    private void writeJCasAndUpdateTimeStamp(SourceDocument aSourceDoc, JCas aJCas)
        throws IOException
    {
        if (!documentService.existsAnnotationDocument(aSourceDoc, currentUser)) {
            documentService.createOrGetAnnotationDocument(aSourceDoc, currentUser);
        }
        documentService.writeAnnotationCas(aJCas, aSourceDoc, currentUser, true);

        updateTimestamp(aSourceDoc);
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

    private void updateTimestamp(SourceDocument aModifiedDocument) throws IOException
    {
        // If the currently displayed document is the same one where the annotation was created,
        // then update timestamp in state to avoid concurrent modification errors
        AnnotatorState state = getModelObject();
        if (Objects.equals(state.getDocument().getId(), aModifiedDocument.getId())) {
            Optional<Long> diskTimestamp = documentService
                .getAnnotationCasTimestamp(aModifiedDocument, currentUser.getUsername());
            if (diskTimestamp.isPresent()) {
                state.setAnnotationDocumentTimestamp(diskTimestamp.get());
            }
        }
    }

    private class SearchResultGroup
        extends Fragment
    {
        private static final long serialVersionUID = 3540041356505975132L;

        public SearchResultGroup(String aId, String aMarkupId, MarkupContainer aMarkupProvider,
                IModel<List<SearchResult>> aModel)
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
                        Model.of(aItem.getModelObject().isSelectedForAnnotation()))
                    {
                        @Override
                        protected void onUpdate(AjaxRequestTarget target)
                        {
                            SearchResult modelObject = aItem.getModelObject();
                            modelObject.setSelectedForAnnotation(getModelObject());
                            if (getModelObject() == false) {
                                // not all results in the document are selected, so set document
                                // level selection to false
                                documentLevelSelections.put(modelObject.getDocumentTitle(), false);
                                target.add(mainContainer);
                            }
                        }
                    };
                    aItem.add(selected);

                    String sentence = new String();

                    sentence = sentence + aItem.getModel().getObject().getLeftContext() + "<mark>"
                            + aItem.getModel().getObject().getText() + "</mark>"
                            + aItem.getModel().getObject().getRightContext();

                    lambdaAjaxLink
                            .add(new Label("sentence", sentence).setEscapeModelStrings(false));
                }
            };
            statementList.setModel(aModel);
            add(statementList);        
        }
    }
}

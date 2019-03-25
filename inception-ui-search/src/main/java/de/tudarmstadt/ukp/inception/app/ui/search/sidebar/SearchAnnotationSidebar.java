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

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
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
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.event.RenderAnnotationsEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VMarker;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VTextMarker;
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
import de.tudarmstadt.ukp.inception.search.SearchResult;
import de.tudarmstadt.ukp.inception.search.SearchService;
import de.tudarmstadt.ukp.inception.search.event.SearchQueryEvent;

public class SearchAnnotationSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = -3358207848681467993L;

    private @SpringBean DocumentService documentService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean SearchService searchService;
    private @SpringBean UserDao userRepository;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisher;

    private User currentUser;

    private final WebMarkupContainer mainContainer;

    private IModel<String> targetQuery = Model.of("");
    private IModel<Options> options = CompoundPropertyModel.of(new Options());
    private IModel<List<SearchResult>> searchResults;
    
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

        Form<Options> searchOptionsForm = new Form<>("searchOptionsForm", options);
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
            SourceDocument limitToDocument = options.getObject().isLimitedToCurrentDocument()
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
    
    public static class Options
        implements Serializable
    {
        private static final long serialVersionUID = 3030323391922717647L;
        
        private boolean visible = false;
        
        private boolean limitedToCurrentDocument = false;

        /**
         * Whether or not the options form should be displayed
         */
        public boolean isVisible()
        {
            return visible;
        }

        public void toggleVisibility()
        {
            visible = !visible;
        }

        public boolean isLimitedToCurrentDocument()
        {
            return limitedToCurrentDocument;
        }

        public void setLimitedToCurrentDocument(boolean aLimitedToCurrentDocument)
        {
            limitedToCurrentDocument = aLimitedToCurrentDocument;
        }
    }
}

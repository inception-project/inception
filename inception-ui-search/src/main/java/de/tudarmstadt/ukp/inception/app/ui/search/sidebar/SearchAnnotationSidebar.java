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

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.JCasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.event.RenderAnnotationsEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VMarker;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VTextMarker;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
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
import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;

public class SearchAnnotationSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = -3358207848681467993L;

    private @SpringBean DocumentService documentService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean SearchService searchService;
    private @SpringBean UserDao userRepository;
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisher;

    private Project currentProject;
    private User currentUser;

    final WebMarkupContainer mainContainer;

    private Model<String> targetQuery = Model.of("");
    private IModel<List<SearchResult>> searchResults;
    
    private SearchResult selectedResult;

    public SearchAnnotationSidebar(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, JCasProvider aJCasProvider,
            AnnotationPage aAnnotationPage)
    {
        super(aId, aModel, aActionHandler, aJCasProvider, aAnnotationPage);

        currentProject = getModel().getObject().getProject();

        currentUser = userRepository.getCurrentUser();
        
        mainContainer = new WebMarkupContainer("mainContainer");
        mainContainer.setOutputMarkupId(true);
        add(mainContainer);

        Form<Void> searchForm = new Form<Void>("searchForm");
        searchForm.add(new TextArea<String>("queryInput", targetQuery));
        searchForm.add(new LambdaAjaxButton<>("search", this::actionSearch));
        mainContainer.add(searchForm);

        
        searchResults = LambdaModel.of(this::getSearchResults);
        
        // Add link for reindexing the project
        mainContainer.add(new LambdaAjaxLink("reindexProject", t -> {
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
            currentProject = getModel().getObject().getProject();
            applicationEventPublisher.get().publishEvent(new SearchQueryEvent(this, currentProject,
                    currentUser.getUsername(), targetQuery.getObject()));
            return searchService.query(currentUser, currentProject, targetQuery.getObject());
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
                    LambdaAjaxLink lambdaAjaxLink;
                    if (aItem.getModel().getObject().getOffsetStart() != -1) {
                        // When the offset exists, use it. Mtas indexes.
                        lambdaAjaxLink = new LambdaAjaxLink("showSelectedDocument", t -> {
                            selectedResult = aItem.getModelObject();
                            actionShowSelectedDocument(t,
                                    documentService.getSourceDocument(currentProject,
                                            selectedResult.getDocumentTitle()),
                                    selectedResult.getOffsetStart(), selectedResult.getOffsetEnd());
                        });
                    }
                    else {
                        // If the offset doesn't exist, use the token position Mimir indexes.
                        lambdaAjaxLink = new LambdaAjaxLink("showSelectedDocument", t -> {
                            selectedResult = aItem.getModelObject();
                            getAnnotationPage().actionShowSelectedDocumentByTokenPosition(t,
                                    documentService.getSourceDocument(currentProject,
                                            aItem.getModel().getObject().getDocumentTitle()),
                                    aItem.getModel().getObject().getTokenStart());
                            ;
                        });

                    }
                    lambdaAjaxLink.add(new InputBehavior(
                            new KeyType[] { KeyType.Shift, KeyType.Page_up }, EventType.click));
                    aItem.add(lambdaAjaxLink);

                    String sentence = new String();

                    sentence = sentence + aItem.getModel().getObject().getLeftContext() + "<strong>"
                            + aItem.getModel().getObject().getText() + "</strong>"
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

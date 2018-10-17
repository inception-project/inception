/*
 * Copyright 2018
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

package de.tudarmstadt.ukp.inception.ui.kb;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.search.SearchResult;
import de.tudarmstadt.ukp.inception.search.SearchService;
import de.tudarmstadt.ukp.inception.ui.kb.search.ConceptFeatureIndexingSupport;

public class AnnotatedListIdentifiers
    extends Panel
{
    private static final long serialVersionUID = -2431507947235476294L;
    private static final Logger LOG = LoggerFactory.getLogger(AnnotatedListIdentifiers.class);

    private @SpringBean DocumentService documentService;
    private @SpringBean KnowledgeBaseService kbService;
    private @SpringBean SearchService searchService;
    private @SpringBean UserDao userRepository;

    private IModel<KnowledgeBase> kbModel;
    private IModel<KBHandle> conceptModel;

    private Project currentProject;
    private User currentUser;

    private Model<String> targetQuery = Model.of("");

    public AnnotatedListIdentifiers(String aId, IModel<KnowledgeBase> aKbModel,
            IModel<KBHandle> aConcept, IModel<KBHandle> aInstance, boolean flagInstanceSelect)
    {
        super(aId, aConcept);
        setOutputMarkupId(true);
        kbModel = aKbModel;
        conceptModel = aConcept;
        currentUser = userRepository.getCurrentUser();
        
        String queryIri = flagInstanceSelect ? aInstance.getObject().getIdentifier()
                : aConcept.getObject().getIdentifier();
        // MTAS internally escapes certain characters, so we need to escape them here as well.
        // Cf. MtasToken.createAutomatonMap()
        queryIri = queryIri.replaceAll("([\\\"\\)\\(\\<\\>\\.\\@\\#\\]\\[\\{\\}])", "\\\\$1");
        targetQuery = Model.of(
                String.format("<%s=\"%s\"/>", ConceptFeatureIndexingSupport.KB_ENTITY, queryIri));
        
        LoadableDetachableModel<List<SearchResult>> searchResults = LoadableDetachableModel
                .of(this::getSearchResults);
        LOG.trace("SearchResult count : {}" , searchResults.getObject().size());
        ListView<String> overviewList = new ListView<String>("searchResultGroups")
        {
            private static final long serialVersionUID = -122960232588575731L;

            @Override protected void onConfigure()
            {
                super.onConfigure();
                
                setVisible(!searchResults.getObject().isEmpty());
            }

            @Override
            protected void populateItem(ListItem<String> aItem)
            {
                aItem.add(new Label("documentTitle", aItem.getModel()));
                aItem.add(new SearchResultGroup("group", "resultGroup",
                        AnnotatedListIdentifiers.this, getSearchResultsFormattedForDocument(
                                searchResults, aItem.getModelObject())));
            }
        };
        overviewList.setList(
            searchResults.getObject().stream().map(res -> res.getDocumentTitle()).distinct()
                .collect(Collectors.toList()));
        
        add(overviewList);
        add(new Label("count", LambdaModel.of(() -> searchResults.getObject().size())));
    }

    public List<String> getSearchResultsFormattedForDocument(
            IModel<List<SearchResult>> searchResults, String documentTitle)
    {
        List<String> searchResultList = new ArrayList<String>();
        for (SearchResult x : searchResults.getObject()) {
            if (x.getDocumentTitle().equals(documentTitle)) {
                String sentence = x.getLeftContext() + "<strong>" + x.getText() + "</strong>"
                        + x.getRightContext();

                searchResultList.add(sentence);
                LOG.debug("Sentence search : {}", sentence);
            }
        }
        return searchResultList;
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();
        
        setVisible(conceptModel.getObject() != null
                && isNotEmpty(conceptModel.getObject().getIdentifier()));
    }

    private List<SearchResult> getSearchResults()
    {
        if (isBlank(targetQuery.getObject())) {
            return Collections.emptyList();
        }
        try {
            currentProject = kbModel.getObject().getProject();
            return searchService.query(currentUser, currentProject, targetQuery.getObject());
        }
        catch (Exception e) {
            LOG.debug("Error in the query.", e);
            error("Error in the query: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private class SearchResultGroup
        extends Fragment
    {
        private static final long serialVersionUID = 3540041356505975132L;

        public SearchResultGroup(String aId, String aMarkupId, MarkupContainer aMarkupProvider,
            List<String> aResultList)
        {
            super(aId, aMarkupId, aMarkupProvider);

            ListView<String> statementList = new ListView<String>("results")
            {
                private static final long serialVersionUID = 5811425707843441458L;

                @Override protected void populateItem(ListItem<String> aItem)
                {
                    aItem.add(
                        new Label("sentence", aItem.getModelObject())
                            .setEscapeModelStrings(false));
                }
            };
            statementList.setList(aResultList);
            add(statementList);
        }
    }

}

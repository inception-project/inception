/*
 * Copyright 2019
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.search.ResultsGroup;
import de.tudarmstadt.ukp.inception.search.SearchResult;

public class SearchResultsProviderWrapper
    implements IDataProvider<ResultsGroup>
{
    private static final long serialVersionUID = 4339947719820231592L;

    private SearchResultsProvider searchResultsProvider;
    private List<ResultsGroup> resultGroups;
    private boolean groupingActivated;
    private IModel<Boolean> lowLevelPaging;

    public SearchResultsProviderWrapper(SearchResultsProvider aSearchResultsProvider,
            IModel<Boolean> aLowLevelPaging)
    {
        searchResultsProvider = aSearchResultsProvider;
        lowLevelPaging = aLowLevelPaging;
    }

    @Override
    public Iterator<ResultsGroup> iterator(long first, long count)
    {
        // If the grouping is activated we first fetch all results sorted by group and then apply
        // the paging. This is done because if we apply paging at query level and grouping together,
        // members of a group might me scattered over multiple pages. (Grouping by document is an
        // exception because the query iterates over documents. So results from the same document
        // appear in a sequence)
        // 
        // If the grouping is not activated (defaults to grouping by document) we can apply paging
        // at query level. So we fetch them directly from the searchResultsProvider.
        if (applyLowLevelPaging()) {
            return searchResultsProvider.iterator(first, count);
        }

        List<ResultsGroup> subList = resultsGroupsSublist(first, count);
        searchResultsProvider.getPagesCacheModel().getObject().putPage(first, count, subList);
        return subList.iterator();
    }

    public boolean applyLowLevelPaging()
    {
        return !groupingActivated && lowLevelPaging.getObject();
    }

    /**
     * Iterate over a List of ResultGroups and return a sublist that contains the SearchResults
     * first to (first + count).
     *
     * e.g.: sublist of elements 2 - 5
     *
     * ResultsGroup1----------ResultsGroup2 ResultsGroup1----------ResultsGroup2 | | | |
     * SearchResult-1 SearchResult-4 SearchResult-3 SearchResult-4 SearchResult-2 SearchResult-5
     * ---> SearchResult-5 SearchResult-3 SearchResult-6
     *
     */
    private List<ResultsGroup> resultsGroupsSublist(long first, long count)
    {
        List<ResultsGroup> resultsGroupsSubList = new ArrayList<>();
        int counter = 0;
        for (ResultsGroup resultsGroup : resultGroups) {
            if (counter - first + 1 > count) {
                break;
            }
            List<SearchResult> sublist = new ArrayList<>();
            for (SearchResult result : resultsGroup.getResults()) {
                if (counter < first) {
                    counter++;
                    continue;
                }
                if (counter - first + 1 > count) {
                    break;
                }
                sublist.add(result);
                counter++;
            }
            if (!(counter <= first)) {
                ResultsGroup group = new ResultsGroup(resultsGroup.getGroupKey(), sublist);
                resultsGroupsSubList.add(group);
            }
        }
        return resultsGroupsSubList;
    }

    @Override
    public long size()
    {
        return searchResultsProvider.size();
    }

    @Override
    public IModel<ResultsGroup> model(ResultsGroup object)
    {
        return searchResultsProvider.model(object);
    }

    public long groupSize(String aGroupKey)
    {
        for (ResultsGroup group : resultGroups) {
            if (group.getGroupKey().equals(aGroupKey)) {
                return group.getResults().size();
            }
        }
        return -1;
    }

    public void initializeQuery(User aUser, Project aProject, String aQuery,
            SourceDocument aDocument, AnnotationLayer aAnnotationLayer,
            AnnotationFeature aAnnotationFeature)
    {
        searchResultsProvider.initializeQuery(aUser, aProject, aQuery, aDocument, aAnnotationLayer,
                aAnnotationFeature);

        groupingActivated = !(searchResultsProvider.getAnnotationFeature() == null
                && searchResultsProvider.getAnnotationLayer() == null);

        if (!applyLowLevelPaging()) {
            resultGroups = getAllResults();
        }

    }

    private List<ResultsGroup> getAllResults()
    {
        Iterator<ResultsGroup> resultsIterator = searchResultsProvider.iterator(0, Long.MAX_VALUE);
        ArrayList<ResultsGroup> resultsList = new ArrayList<>();
        resultsIterator.forEachRemaining(r -> resultsList.add(r));
        return resultsList;
    }

    public void emptyQuery()
    {
        searchResultsProvider.emptyQuery();
        groupingActivated = false;
    }
}

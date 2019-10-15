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

public class SearchResultsProviderWrapper implements IDataProvider<ResultsGroup>
{
    private SearchResultsProvider searchResultsProvider;
    private List<ResultsGroup> resultGroups;
    private boolean groupingActivated;


    public SearchResultsProviderWrapper(SearchResultsProvider aSearchResultsProvider)
    {
        searchResultsProvider = aSearchResultsProvider;
    }

    @Override
    public Iterator<ResultsGroup> iterator(long first, long count)
    {
        /*
        If the grouping is activated we first fetch all results sorted them by group and then apply
        the paging.
        We do this because if we apply paging at query level and grouping together, members of a
        group might me scattered over multiple pages. (Grouping by document is
        an exception because the query iterates over documents. So results from the same document
        appear in a sequence)

        If the grouping is not activated we can apply paging directly when fetching the results
        since it defaults to grouping by document. So we fetch them from the searchResultsProvider.
         */
        if (groupingActivated) {
            List<ResultsGroup> resultsGroupsSubList = new ArrayList<>();
            int counter = 0;
            for (ResultsGroup resultsGroup : resultGroups) {
                if (counter - first + 1 > count) {
                    break;
                }
                List<SearchResult> sublist = new ArrayList<>();
                for (SearchResult result : resultsGroup.getResults()) {
                    if (counter < first) {
                        counter ++;
                        continue;
                    }
                    if (counter - first + 1 > count) {
                        break;
                    }
                    sublist.add(result);
                    counter++;
                }
                if (! (counter <= first)) {
                    ResultsGroup group = new ResultsGroup(resultsGroup.getGroupKey(), sublist);
                    resultsGroupsSubList.add(group);
                }
            }
            searchResultsProvider.getCurrentPageCache().setObject(resultsGroupsSubList);
            return resultsGroupsSubList.iterator();
        }

        return searchResultsProvider.iterator(first, count);
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


    public void initializeQuery(User aUser, Project aProject, String aQuery,
        SourceDocument aDocument, AnnotationLayer aAnnotationLayer,
        AnnotationFeature aAnnotationFeature)
    {
        searchResultsProvider.initializeQuery(aUser, aProject, aQuery, aDocument, aAnnotationLayer,
            aAnnotationFeature);

        groupingActivated = !(searchResultsProvider.getAnnotationFeature() == null
            && searchResultsProvider.getAnnotationLayer() == null);

        if (groupingActivated) {
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

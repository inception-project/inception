package de.tudarmstadt.ukp.inception.search;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class SearchResultsProvider implements IDataProvider
{
    private SearchService searchService;

    private User user;
    private Project project;
    private String query;
    private SourceDocument document;
    private AnnotationLayer annotationLayer;
    private AnnotationFeature annotationFeature;

    private long totalResults = 0;
    private IModel<List<ResultsGroup>> currentPageCache;
    private long pageCacheState = -1; // -1 if a new query has been initialized, different otherwise
    private long currentoffset = -1; //
    private long currentcount = -1;

    public SearchResultsProvider(SearchService aSearchService, IModel<List<ResultsGroup>> aCurrentPageCache) {
        searchService = aSearchService;
        currentPageCache = aCurrentPageCache;
    }

    public Iterator iterator(long first, long count)
    {
        if (query == null) {
            currentPageCache.setObject(Collections.emptyList());
            return currentPageCache.getObject().iterator();
        }
        // Query if:
        // We just initialized a new query (pageCacheState = -1) (might be replaced with currentPageCache.getObject() == null)
        // or
        // we want to retrieve a new page of the current query (currentoffset != first, currentcount != count)
        if (pageCacheState == -1 || (currentoffset != first || currentcount != count)) {
            try {
                List<ResultsGroup> queryResults = searchService
                    .query(user, project, query, document, annotationLayer, annotationFeature, first,
                        count).entrySet().stream().map(e -> new ResultsGroup(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
                currentPageCache.setObject(queryResults);
                //groupLevelSelectionsCache = initGroupLevelSelections(queryResults.stream().map(rg -> rg.getGroupKey()).collect(Collectors.toSet()));
                pageCacheState = 1;
                currentcount = count;
                currentoffset = first;
                return queryResults.iterator();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        else {
            return currentPageCache.getObject().iterator();
        }
        return null;
    }

    public long size()
    {
        if (totalResults == -1) {
            try {
                totalResults = searchService
                    .determineNumOfQueryResults(user, project, query, document, annotationLayer,
                        annotationFeature);
                return totalResults;
            }
            catch (ExecutionException e) {
                e.printStackTrace();
                return 0;
            }
        }
        else {
            return totalResults;
        }
    }

    public IModel model(Object object)
    {
        return new Model((ResultsGroup) object);
    }

    /**
     * Sets the query parameters in the SearchResultsProvider.
     * Calling the {@link #iterator(long, long)} method of the SearchResultsProvider will then
     * execute the query.
     */
    public void initializeQuery(User aUser, Project aProject, String aQuery,
        SourceDocument aDocument, AnnotationLayer aAnnotationLayer,
        AnnotationFeature aAnnotationFeature) {
        user = aUser;
        project = aProject;
        query = aQuery;
        document = aDocument;
        annotationLayer = aAnnotationLayer;
        annotationFeature = aAnnotationFeature;

        totalResults = -1;  // reset size cache
        pageCacheState = -1; // reset page cache

    }

    public void emptyQuery() {
        query = null;
        totalResults = 0;
    }
}


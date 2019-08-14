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

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.search.ExecutionException;
import de.tudarmstadt.ukp.inception.search.ResultsGroup;
import de.tudarmstadt.ukp.inception.search.SearchService;

public class SearchResultsProvider implements IDataProvider
{
    private static final long serialVersionUID = -4937781923274074722L;

    private SearchService searchService;

    // Query settings
    private User user;
    private Project project;
    private String query;
    private SourceDocument document;
    private AnnotationLayer annotationLayer;
    private AnnotationFeature annotationFeature;

    // Cache
    private long totalResults = 0;
    private IModel<List<ResultsGroup>> currentPageCache;
    private long currentOffset = -1;
    private long currentCount = -1;

    public SearchResultsProvider(SearchService aSearchService,
            IModel<List<ResultsGroup>> aCurrentPageCache)
    {
        searchService = aSearchService;
        currentPageCache = aCurrentPageCache;
    }

    @Override
    public Iterator iterator(long first, long count)
    {
        if (query == null) {
            currentPageCache.setObject(Collections.emptyList());
            return currentPageCache.getObject().iterator();
        }
        // Query if we just initialized a new query currentPageCache.getObject() == null or we want
        // to retrieve a new page of the current query (currentOffset != first,
        // currentCount != count)
        if (currentPageCache.getObject() == null || (currentOffset != first
            || currentCount != count)) {
            try {
                List<ResultsGroup> queryResults = searchService
                    .query(user, project, query, document, annotationLayer, annotationFeature,
                        first, count).entrySet().stream()
                    .map(e -> new ResultsGroup(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
                currentPageCache.setObject(queryResults);
                currentCount = count;
                currentOffset = first;
                return queryResults.iterator();
            }
            catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            catch (ExecutionException e) {
                e.printStackTrace();
                return null;
            }
        }
        else {
            return currentPageCache.getObject().iterator();
        }
    }

    @Override
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

    @Override
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
        currentPageCache.setObject(null); // reset page cache

    }

    public void emptyQuery() {
        query = null;
        totalResults = 0;
    }
}


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

import static java.util.Collections.emptyIterator;

import java.io.IOException;
import java.util.Iterator;
import java.util.stream.Collectors;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.wicket.Session;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.search.ExecutionException;
import de.tudarmstadt.ukp.inception.search.ResultsGroup;
import de.tudarmstadt.ukp.inception.search.SearchService;

public class SearchResultsProvider
    implements IDataProvider<ResultsGroup>
{
    private static final long serialVersionUID = -4937781923274074722L;

    private static final Logger LOG = LoggerFactory.getLogger(SearchResultsProvider.class);

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
    private SearchResultsPagesCache cache;

    public SearchResultsProvider(SearchService aSearchService)
    {
        searchService = aSearchService;
        cache = new SearchResultsPagesCache();
    }

    @Override
    public Iterator<ResultsGroup> iterator(long first, long count)
    {
        if (query == null) {
            cache.clear();
            return IteratorUtils.emptyIterator();
        }

        if (cache.containsPage(first, count)) {
            return cache.getPage(first, count).iterator();
        }

        // Query if the results in the given range are not in the cache i.e. if we need to fetch
        // a new page
        try {
            var queryResults = searchService
                    .query(user, project, query, document, annotationLayer, annotationFeature,
                            first, count) //
                    .entrySet().stream() //
                    .map(e -> new ResultsGroup(e.getKey(), e.getValue())) //
                    .collect(Collectors.toList());

            cache.putPage(first, count, queryResults);

            return queryResults.iterator();
        }
        catch (ExecutionException e) {
            if (Session.exists()) {
                Session.get().error(e.getMessage());
            }
            return emptyIterator();
        }
        catch (IOException e) {
            if (Session.exists()) {
                Session.get().error("Unable to retrieve results: " + e.getMessage());
            }
            LOG.error("Unable to retrieve results", e);
            return emptyIterator();
        }
    }

    @Override
    public long size()
    {
        if (totalResults != -1) {
            return totalResults;
        }

        try {
            totalResults = searchService.determineNumOfQueryResults(user, project, query, document,
                    annotationLayer, annotationFeature);
            return totalResults;
        }
        catch (ExecutionException | IOException e) {
            return 0;
        }
    }

    @Override
    public IModel<ResultsGroup> model(ResultsGroup object)
    {
        return Model.of(object);
    }

    /**
     * Sets the query parameters in the SearchResultsProvider. Calling the
     * {@link #iterator(long, long)} method of the SearchResultsProvider will then execute the
     * query.
     */
    @SuppressWarnings("javadoc")
    public void initializeQuery(SearchRequest aRequest)
    {
        user = aRequest.dataOwner();
        project = aRequest.project();
        query = aRequest.query();
        document = aRequest.limitToDocument();
        annotationLayer = aRequest.groupingLayer();
        annotationFeature = aRequest.groupingFeature();

        totalResults = -1; // reset size cache
        cache.clear(); // reset page cache
    }

    public void emptyQuery()
    {
        query = null;
        totalResults = 0;
        cache.clear();
    }

    public AnnotationLayer getAnnotationLayer()
    {
        return annotationLayer;
    }

    public AnnotationFeature getAnnotationFeature()
    {
        return annotationFeature;
    }

    public SearchResultsPagesCache getCache()
    {
        return cache;
    }
}

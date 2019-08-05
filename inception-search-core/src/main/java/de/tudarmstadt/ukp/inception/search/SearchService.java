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
package de.tudarmstadt.ukp.inception.search;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.search.model.Index;

public interface SearchService
{
    static final String SERVICE_NAME = "searchService";

    List<SearchResult> query(User aUser, Project aProject, String aQuery)
        throws IOException, ExecutionException;

    /**
     * Receive the search results un-grouped as a list.
     * See {@link #query(User, Project, String, SourceDocument, AnnotationLayer, AnnotationFeature, long, long)}
     */
    List<SearchResult> query(User aUser, Project aProject, String aQuery, SourceDocument aDocument)
        throws IOException, ExecutionException;

    /**
     * Receive the search results grouped by the feature values of the given AnnotationFeature of
     * the given AnnotationLayer. Both AnnotationLayer and AnnotationFeature must be not null for
     * this, otherwise the search results will be grouped by document title.
     * @param aUser the current user
     * @param aProject the project to search in
     * @param aDocument limit search to this document or search in the whole project if null
     * @param aAnnotationLayer the layer that the grouping feature belongs to
     * @param aAnnotationFeature the feature that is used to group the results
     * @param aOffset offset used for the paging of the search results i.e. the index of the first
     *                search result of the page
     * @param aCount number of search results to be returned, starting from aOffset
     * @return a Map where the keys are the group-keys (e.g. feature-values) and the values are
     * lists of search results that belong to this group.
     * @throws IOException
     * @throws ExecutionException
     */
    Map<String, List<SearchResult>> query(User aUser, Project aProject, String aQuery,
        SourceDocument aDocument, AnnotationLayer aAnnotationLayer,
        AnnotationFeature aAnnotationFeature, long aOffset, long aCount) throws IOException, ExecutionException;

    void reindex(Project aproject) throws IOException;

    Index getIndex(Project aProject);

    boolean isIndexValid(Project aProject);
    
    void indexDocument(SourceDocument aSourceDocument, CAS aJCas);

    void indexDocument(AnnotationDocument aAnnotationDocument, CAS aJCas);

    boolean isIndexInProgress(Project aProject);

    long determineNumOfQueryResults(User aUser, Project aProject, String aQuery,
        SourceDocument aDocument, AnnotationLayer aAnnotationLayer,
        AnnotationFeature aAnnotationFeature) throws ExecutionException;
}

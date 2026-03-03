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
package de.tudarmstadt.ukp.inception.search;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.scheduling.Progress;

public interface SearchService
{
    List<SearchResult> query(User aUser, Project aProject, String aQuery)
        throws IOException, ExecutionException;

    Map<String, List<SearchResult>> query(SearchQueryRequest aRequest)
        throws ExecutionException, IOException;

    /**
     * @param aUser
     *            the current user
     * @param aProject
     *            the project to search in
     * @param aQuery
     *            the search query
     * @param aDocument
     *            limit search to this document or search in the whole project if null
     * @return the search results un-grouped as a list.
     * @see #query(User, Project, String, SourceDocument, AnnotationLayer, AnnotationFeature, long,
     *      long)
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws ExecutionException
     *             if there was a search-level problem
     */
    List<SearchResult> query(User aUser, Project aProject, String aQuery, SourceDocument aDocument)
        throws IOException, ExecutionException;

    /**
     * Receive the search results grouped by the feature values of the given AnnotationFeature of
     * the given AnnotationLayer. Both AnnotationLayer and AnnotationFeature must be not null for
     * this, otherwise the search results will be grouped by document title.
     * 
     * @param aUser
     *            the current user
     * @param aProject
     *            the project to search in
     * @param aQuery
     *            the search query
     * @param aDocument
     *            limit search to this document or search in the whole project if null
     * @param aAnnotationLayer
     *            the layer that the grouping feature belongs to
     * @param aAnnotationFeature
     *            the feature that is used to group the results
     * @param aOffset
     *            offset used for the paging of the search results i.e. the index of the first
     *            search result of the page
     * @param aCount
     *            number of search results to be returned, starting from aOffset
     * @return a Map where the keys are the group-keys (e.g. feature-values) and the values are
     *         lists of search results that belong to this group.
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws ExecutionException
     *             if there was a search-level problem
     */
    Map<String, List<SearchResult>> query(User aUser, Project aProject, String aQuery,
            SourceDocument aDocument, AnnotationLayer aAnnotationLayer,
            AnnotationFeature aAnnotationFeature, long aOffset, long aCount)
        throws IOException, ExecutionException;

    /**
     * This method is only for testing. It allows waiting until the indexing process has completed
     * before issuing a query.
     * 
     * @param aProject
     *            the project
     * @return whether the index is flagged as valid
     */
    boolean isIndexValid(Project aProject);

    void indexDocument(SourceDocument aSourceDocument, byte[] aBinaryCas);

    void indexDocument(AnnotationDocument aAnnotationDocument, byte[] aBinaryCas);

    Optional<Progress> getIndexProgress(Project aProject);

    long determineNumOfQueryResults(User aUser, Project aProject, String aQuery,
            SourceDocument aDocument, AnnotationLayer aAnnotationLayer,
            AnnotationFeature aAnnotationFeature)
        throws ExecutionException, IOException;

    /**
     *
     * @param aUser
     *            the current user, not all users have access to all documents
     * @param aProject
     *            the current project
     * @param aMinTokenPerDoc
     *            only documents which have more tokens than this are considered null means we don't
     *            have a lower bound
     * @param aMaxTokenPerDoc
     *            only documents which have less tokens than this are considered null means we don't
     *            have an upper bound
     * @param aFeatures
     *            for which features the stats shall be calculated
     * @return the method cycles through each layer and each feature from the DocumentService and
     *         gets all the statistics from aStatistic
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws ExecutionException
     *             if there was a search-level problem
     */
    public StatisticsResult getProjectStatistics(User aUser, Project aProject, int aMinTokenPerDoc,
            int aMaxTokenPerDoc, Set<AnnotationFeature> aFeatures)
        throws IOException, ExecutionException;

    /**
     *
     * @param aUser
     *            the current user, not all users have access to all documents
     * @param aProject
     *            the current project
     * @param aQuery
     *            a normal query e.g. the name of a specific token
     * @param aMinTokenPerDoc
     *            only documents which have more tokens than this are considered null means we don't
     *            have a lower bound
     * @param aMaxTokenPerDoc
     *            only documents which have less tokens than this are considered null means we don't
     *            have an upper bound
     * @param aFeatures
     *            for which features the stats shall be calculated
     * @return the statistics from aStatistic for aQuery, e.g. what is the maximal amount of
     *         occurrences per document of aQuery across all documents
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws ExecutionException
     *             if there was a search-level problem
     */
    public StatisticsResult getQueryStatistics(User aUser, Project aProject, String aQuery,
            int aMinTokenPerDoc, int aMaxTokenPerDoc, Set<AnnotationFeature> aFeatures)
        throws ExecutionException, IOException;

    void enqueueReindexTask(Project aProject, User aUser, String aTrigger);
}

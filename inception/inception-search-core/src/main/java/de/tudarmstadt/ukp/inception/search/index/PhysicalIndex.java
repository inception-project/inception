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
package de.tudarmstadt.ukp.inception.search.index;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.search.ExecutionException;
import de.tudarmstadt.ukp.inception.search.LayerStatistics;
import de.tudarmstadt.ukp.inception.search.SearchQueryRequest;
import de.tudarmstadt.ukp.inception.search.SearchResult;
import de.tudarmstadt.ukp.inception.search.StatisticRequest;
import de.tudarmstadt.ukp.inception.search.StatisticsResult;
import de.tudarmstadt.ukp.inception.search.model.AnnotationSearchState;

public interface PhysicalIndex
{
    /**
     * @return if whether the index has data that can be deleted, e.g. index files on disk.
     */
    boolean isCreated();

    /**
     * Deletes the index data, e.g. by removing the index files from disk. If necessary, the index
     * is closed before.
     * 
     * @throws IOException
     *             if there was an I/O-level problem
     */
    void delete() throws IOException;

    boolean isOpen();

    void open() throws IOException;

    void close();

    Map<String, List<SearchResult>> executeQuery(SearchQueryRequest aRequest,
            AnnotationSearchState aPrefs)
        throws IOException, ExecutionException;

    long numberOfQueryResults(SearchQueryRequest aSearchQueryRequest, AnnotationSearchState aPrefs)
        throws IOException, ExecutionException;

    public LayerStatistics getLayerStatistics(StatisticRequest aStatisticRequest,
            String aFeatureQuery, List<Integer> aFullDocSet)
        throws IOException, ExecutionException;

    public List<Integer> getUniqueDocuments(StatisticRequest aStatisticRequest) throws IOException;

    public StatisticsResult getAnnotationStatistics(StatisticRequest aStatisticRequest)
        throws IOException, ExecutionException;

    void deindexDocument(SourceDocument aDocument) throws IOException;

    void deindexDocument(AnnotationDocument aDocument) throws IOException;

    @Deprecated
    void deindexDocument(AnnotationDocument aDocument, String aTimestamp) throws IOException;

    void indexDocument(AnnotationDocument aDocument, byte[] aBinaryCas) throws IOException;

    void clear() throws IOException;

    /**
     * Retrieve the timestamp of this annotation document
     * 
     * @param aSrcDocId
     *            the source document's ID
     * @param aAnnoDocId
     *            the annotation document's ID
     * @return The first found document timestamp field value. Empty string if document is not
     *         found.
     * @throws IOException
     *             if there was an I/O-level problem
     */
    public Optional<String> getTimestamp(long aSrcDocId, long aAnnoDocId) throws IOException;

    void indexDocument(SourceDocument aSourceDocument, byte[] aBinaryCas) throws IOException;
}

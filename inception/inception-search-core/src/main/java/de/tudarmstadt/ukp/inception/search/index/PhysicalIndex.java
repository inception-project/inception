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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.search.DocumentStatistics;
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
     * Schema version of the data shape this physical index implementation currently produces.
     * Compared at index-open time against the value stamped on the {@code Index} entity by the last
     * successful (re)build; a mismatch triggers a lazy rebuild. The numbering is private to each
     * physical provider, so different providers may reuse the same numbers without conflict.
     *
     * @return the current schema version (must be a non-negative integer; bump when the on-disk
     *         data shape changes in a way that requires a rebuild).
     */
    int getCurrentSchemaVersion();

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

    /**
     * Count the number of annotations on the given layer per source document. Each annotation is
     * counted once regardless of its feature values. The returned map is keyed by
     * {@link SourceDocument} id; source documents that contain no annotations on the layer may be
     * omitted (callers should treat a missing key as zero).
     *
     * @param aStatisticRequest
     *            scope of the count (project, user, token-per-doc bounds, etc.)
     * @param aLayer
     *            the annotation layer to count, identified by its UI name
     * @return a map from {@link SourceDocument} id to match count.
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws ExecutionException
     *             if there was a search-level problem
     */
    public Map<Long, Long> getAnnotationCountsPerSourceDocument(StatisticRequest aStatisticRequest,
            AnnotationLayer aLayer)
        throws IOException, ExecutionException;

    /**
     * Token and sentence counts per source document for the given annotation set. For each
     * requested source document the row owned by {@code aSet} is preferred; if no such row is
     * indexed, the {@link AnnotationSet#INITIAL_SET} row is used as fallback. Source documents that
     * have no row in either are omitted.
     *
     * @param aSet
     *            which CAS the counts should reflect
     * @param aDocuments
     *            source documents to look up; if {@code null} or empty, the result is empty
     * @return map from {@link SourceDocument} id to its {@link DocumentStatistics}
     * @throws IOException
     *             if there was an I/O-level problem
     * @throws ExecutionException
     *             if there was a search-level problem
     */
    public Map<Long, DocumentStatistics> getAnnotationCountsPerDocument(AnnotationSet aSet,
            Collection<SourceDocument> aDocuments, AnnotationSearchState aSearchSettings)
        throws IOException, ExecutionException;

    void deindexDocument(SourceDocument aDocument) throws IOException;

    void deindexDocument(AnnotationDocument aDocument) throws IOException;

    @Deprecated
    void deindexDocument(AnnotationDocument aDocument, String aTimestamp) throws IOException;

    void indexDocument(AnnotationDocument aDocument, byte[] aBinaryCas) throws IOException;

    void clear() throws IOException;

    /**
     * Upgrades the on-disk index to the current Lucene format without re-indexing the data. This
     * only works if the existing segments are still readable by the running Lucene version. If the
     * index format is too old, callers must fall back to a full rebuild via {@link #clear()} +
     * re-index.
     *
     * @throws IOException
     *             if there was an I/O-level problem
     */
    void upgrade() throws IOException;

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

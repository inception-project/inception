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
package de.tudarmstadt.ukp.inception.search.index;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.search.ExecutionException;
import de.tudarmstadt.ukp.inception.search.SearchQueryRequest;
import de.tudarmstadt.ukp.inception.search.SearchResult;

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
     */
    void delete() throws IOException;

    boolean isOpen();

    void close();

    Map<String, List<SearchResult>> executeQuery(SearchQueryRequest aRequest)
        throws IOException, ExecutionException;

    long numberOfQueryResults(SearchQueryRequest aSearchQueryRequest)
        throws IOException, ExecutionException;

    void deindexDocument(SourceDocument aDocument) throws IOException;

    void deindexDocument(AnnotationDocument aDocument) throws IOException;

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
     */
    public Optional<String> getTimestamp(long aSrcDocId, long aAnnoDocId) throws IOException;

    void indexDocument(SourceDocument aSourceDocument, byte[] aBinaryCas) throws IOException;
}

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

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.search.ExecutionException;
import de.tudarmstadt.ukp.inception.search.SearchQueryRequest;
import de.tudarmstadt.ukp.inception.search.SearchResult;

public interface PhysicalIndex
{
    public boolean connect(String aUrl, String aUser, String aPassword);

    void createPhysicalIndex();

    void dropPhysicalIndex() throws IOException;

    void openPhysicalIndex();

    void closePhysicalIndex();

    boolean isCreated();

    boolean isOpen();

    Map<String, List<SearchResult>> executeQuery(SearchQueryRequest aRequest)
            throws IOException, ExecutionException;

    public void indexDocument(SourceDocument aDocument, CAS aJCas) throws IOException;

    public void indexDocument(AnnotationDocument aDocument, CAS aJCas) throws IOException;

    public void deindexDocument(SourceDocument aDocument) throws IOException;

    public void deindexDocument(AnnotationDocument aDocument) throws IOException;

    public void deindexDocument(AnnotationDocument aDocument, String aTimestamp) throws IOException;

    /**
     * Retrieve the timestamp of this annotation document
     * @param aDocument
     *          The annotation document
     * @return
     *          The document timestamp field value. Empty string if document is not found.
     * @throws IOException
     */
    public Optional<String> getTimestamp(AnnotationDocument aDocument) throws IOException;

    long numberofQueryResults(SearchQueryRequest aSearchQueryRequest) throws ExecutionException;
}

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
package de.tudarmstadt.ukp.inception.externalsearch;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;

public interface ExternalSearchProvider<T extends Object>
{
    List<ExternalSearchResult> executeQuery(DocumentRepository aRepository, T aTraits,
            String aQuery)
        throws IOException;

    /**
     * @return the document in plain text as a string.
     * @param aRepository
     *            the repository from which to fetch the document
     * @param aTraits
     *            the repository traits
     * @param aCollectionId
     *            the ID of the collection containing the document
     * @param aDocumentId
     *            the document ID
     * @throws IOException
     *             if there was a problem obtaining the document
     */
    String getDocumentText(DocumentRepository aRepository, T aTraits, String aCollectionId,
            String aDocumentId)
        throws IOException;

    /**
     * @return the document in the format returned by {@link #getDocumentFormat} as a stream.
     * @param aRepository
     *            the repository from which to fetch the document
     * @param aTraits
     *            the repository traits
     * @param aCollectionId
     *            the ID of the collection containing the document
     * @param aDocumentId
     *            the document ID
     * @throws IOException
     *             if there was a problem obtaining the document
     */
    InputStream getDocumentAsStream(DocumentRepository aRepository, T aTraits, String aCollectionId,
            String aDocumentId)
        throws IOException;

    String getDocumentFormat(DocumentRepository aRepository, T aTraits, String aCollectionId,
            String aDocumentId)
        throws IOException;

    @Deprecated
    ExternalSearchResult getDocumentResult(DocumentRepository aRepository, T aTraits,
            String aCollectionId, String aDocumentId)
        throws IOException;
}

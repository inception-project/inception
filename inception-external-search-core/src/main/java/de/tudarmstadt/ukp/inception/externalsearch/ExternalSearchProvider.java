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

    String getDocumentText(DocumentRepository aRepository, T aTraits, String aSource,
            String aDocumentId)
        throws IOException;

    InputStream getDocumentAsStream(DocumentRepository aRepository, T aTraits, String aCollectionId,
            String aDocumentId)
        throws IOException;

    String getDocumentFormat(DocumentRepository aRepository, T aTraits, String aCollectionId,
            String aDocumentId)
        throws IOException;

    ExternalSearchResult getDocumentResult(DocumentRepository aRepository,
            T aTraits, String aCollectionId, String aDocumentId)
        throws IOException;
}

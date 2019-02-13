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

import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.externalsearch.model.DocumentRepository;

/**
 * External search service API.
 */
public interface ExternalSearchService
{
    List<ExternalSearchResult> query(User aUser, DocumentRepository aDocumentRepository,
            String aQuery);

    String getDocumentText(DocumentRepository aDocumentRepository, String aCollectionId,
            String aId);

    List<DocumentRepository> listDocumentRepositories(Project aProject);

    void createOrUpdateDocumentRepository(DocumentRepository aDocumentRepository);

    void deleteDocumentRepository(DocumentRepository object);
    
    DocumentRepository getRepository(long aId);
}

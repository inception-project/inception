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
package de.tudarmstadt.ukp.inception.documents.config;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.documents.DocumentAccess;
import de.tudarmstadt.ukp.inception.documents.DocumentServiceImpl;
import de.tudarmstadt.ukp.inception.documents.exporters.SourceDocumentExporter;

@Configuration
public class DocumentServiceAutoConfiguration
{
    private @PersistenceContext EntityManager entityManager;

    @Bean
    public DocumentService documentService(RepositoryProperties aRepositoryProperties,
            CasStorageService aCasStorageService, DocumentImportExportService aImportExportService,
            ProjectService aProjectService, ApplicationEventPublisher aApplicationEventPublisher)
    {
        return new DocumentServiceImpl(aRepositoryProperties, aCasStorageService,
                aImportExportService, aProjectService, aApplicationEventPublisher, entityManager);
    }

    @Bean
    public DocumentAccess documentAccess(ProjectService aProjectService, UserDao aUserService,
            DocumentService aDocumentService)
    {
        return new DocumentAccess(aProjectService, aUserService, aDocumentService);
    }

    @Bean
    public SourceDocumentExporter sourceDocumentExporter(DocumentService aDocumentService,
            RepositoryProperties aRepositoryProperties)
    {
        return new SourceDocumentExporter(aDocumentService, aRepositoryProperties);
    }
}

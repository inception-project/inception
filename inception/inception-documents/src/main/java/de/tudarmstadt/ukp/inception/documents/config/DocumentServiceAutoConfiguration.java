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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.documents.DocumentAccessImpl;
import de.tudarmstadt.ukp.inception.documents.DocumentFootprintProvider;
import de.tudarmstadt.ukp.inception.documents.DocumentServiceImpl;
import de.tudarmstadt.ukp.inception.documents.DocumentStorageServiceImpl;
import de.tudarmstadt.ukp.inception.documents.api.DocumentAccess;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.DocumentStorageService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.documents.api.export.CrossDocumentExporter;
import de.tudarmstadt.ukp.inception.documents.api.export.CrossDocumentExporterRegistry;
import de.tudarmstadt.ukp.inception.documents.exporters.CrossDocumentExporterRegistryImpl;
import de.tudarmstadt.ukp.inception.documents.exporters.SourceDocumentExporter;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Configuration
public class DocumentServiceAutoConfiguration
{
    private @PersistenceContext EntityManager entityManager;

    @Bean
    public DocumentService documentService(RepositoryProperties aRepositoryProperties,
            CasStorageService aCasStorageService, DocumentImportExportService aImportExportService,
            ProjectService aProjectService, ApplicationEventPublisher aApplicationEventPublisher,
            DocumentStorageService aDocumentStorageService)
    {
        return new DocumentServiceImpl(aRepositoryProperties, aCasStorageService,
                aImportExportService, aProjectService, aApplicationEventPublisher, entityManager,
                aDocumentStorageService);
    }

    @Bean
    public DocumentAccess documentAccess(ProjectService aProjectService, UserDao aUserService,
            DocumentService aDocumentService)
    {
        return new DocumentAccessImpl(aProjectService, aUserService, aDocumentService);
    }

    @Bean
    public SourceDocumentExporter sourceDocumentExporter(DocumentService aDocumentService,
            RepositoryProperties aRepositoryProperties,
            DocumentStorageService aDocumentStorageService)
    {
        return new SourceDocumentExporter(aDocumentService, aDocumentStorageService,
                aRepositoryProperties);
    }

    @Bean
    public DocumentStorageService documentStorageService(RepositoryProperties aRepositoryProperties)
    {
        return new DocumentStorageServiceImpl(aRepositoryProperties);
    }

    @Bean
    public DocumentFootprintProvider sourceDocumentFootprintProvider(
            DocumentStorageServiceImpl aDocumentStorageService)
    {
        return new DocumentFootprintProvider(aDocumentStorageService);
    }

    @Bean
    public CrossDocumentExporterRegistry crossDocumentExporterRegistry(
            @Lazy @Autowired(required = false) List<CrossDocumentExporter> aExtensions)
    {
        return new CrossDocumentExporterRegistryImpl(aExtensions);
    }
}

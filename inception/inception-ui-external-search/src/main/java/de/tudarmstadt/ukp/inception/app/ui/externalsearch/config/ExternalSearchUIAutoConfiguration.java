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
package de.tudarmstadt.ukp.inception.app.ui.externalsearch.config;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.inception.app.ui.externalsearch.SearchPageMenuItem;
import de.tudarmstadt.ukp.inception.app.ui.externalsearch.project.DocumentRepositoryProjectSettingsPanelFactory;
import de.tudarmstadt.ukp.inception.app.ui.externalsearch.project.ProjectDocumentRepositoriesMenuItem;
import de.tudarmstadt.ukp.inception.app.ui.externalsearch.sidebar.ExternalSearchAnnotationSidebarFactory;
import de.tudarmstadt.ukp.inception.app.ui.externalsearch.utils.DocumentImporter;
import de.tudarmstadt.ukp.inception.app.ui.externalsearch.utils.DocumentImporterImpl;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchService;
import de.tudarmstadt.ukp.inception.externalsearch.config.ExternalSearchAutoConfiguration;

/**
 * Provides all UI-level Spring beans for the external search functionality.
 */
@ConditionalOnWebApplication
@Configuration
@AutoConfigureAfter(ExternalSearchAutoConfiguration.class)
@ConditionalOnBean(ExternalSearchService.class)
public class ExternalSearchUIAutoConfiguration
{
    @Bean
    public SearchPageMenuItem searchPageMenuItem()
    {
        return new SearchPageMenuItem();
    }

    @Bean
    public DocumentRepositoryProjectSettingsPanelFactory documentRepositoryProjectSettingsPanelFactory()
    {
        return new DocumentRepositoryProjectSettingsPanelFactory();
    }

    @Bean
    public ExternalSearchAnnotationSidebarFactory externalSearchAnnotationSidebarFactory(
            ExternalSearchService aExternalSearchService)
    {
        return new ExternalSearchAnnotationSidebarFactory(aExternalSearchService);
    }

    @Bean
    public DocumentImporter documentImporter(DocumentService aDocumentService,
            ExternalSearchService aExternalSearchService)
    {
        return new DocumentImporterImpl(aDocumentService, aExternalSearchService);
    }

    @Bean
    public ProjectDocumentRepositoriesMenuItem projectDocumentRepositoriesMenuItem()
    {
        return new ProjectDocumentRepositoriesMenuItem();
    }
}

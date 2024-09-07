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
package de.tudarmstadt.ukp.inception.versioning.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.curation.service.CurationDocumentService;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.versioning.VersioningService;
import de.tudarmstadt.ukp.inception.versioning.VersioningServiceImpl;
import de.tudarmstadt.ukp.inception.versioning.ui.VersioningMenuItem;
import de.tudarmstadt.ukp.inception.versioning.ui.VersioningSettingsPanelFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Configuration
@ConditionalOnProperty(prefix = "versioning", name = "enabled", havingValue = "true", matchIfMissing = false)
public class VersioningServiceAutoConfiguration
{
    private @PersistenceContext EntityManager entityManager;

    @Bean
    public VersioningService versioningService(RepositoryProperties aRepositoryProperties,
            AnnotationSchemaService aAnnotationSchemaService, DocumentService aDocumentService,
            CurationDocumentService aCurationDocumentService, CasStorageService aCasStorageService,
            UserDao aUserDao)
    {
        return new VersioningServiceImpl(aRepositoryProperties, aAnnotationSchemaService,
                aDocumentService, aCurationDocumentService, aCasStorageService, aUserDao);
    }

    @Bean
    public VersioningSettingsPanelFactory versioningSettingsPanelFactory()
    {
        return new VersioningSettingsPanelFactory();
    }

    @Bean
    public VersioningMenuItem versioningMenuItem()
    {
        return new VersioningMenuItem();
    }
}

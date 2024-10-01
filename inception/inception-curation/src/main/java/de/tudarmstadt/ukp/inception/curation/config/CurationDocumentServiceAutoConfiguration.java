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
package de.tudarmstadt.ukp.inception.curation.config;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasStorageService;
import de.tudarmstadt.ukp.inception.curation.service.CurationDocumentService;
import de.tudarmstadt.ukp.inception.curation.service.CurationDocumentServiceImpl;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import jakarta.persistence.EntityManager;

@Configuration
@AutoConfigureAfter(name = {
        "de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration",
        "de.tudarmstadt.ukp.inception.schema.config.AnnotationSchemaServiceAutoConfiguration",
        "de.tudarmstadt.ukp.inception.curation.config.CurationServiceAutoConfiguration" })
@ConditionalOnBean({ CasStorageService.class, AnnotationSchemaService.class, ProjectService.class })
public class CurationDocumentServiceAutoConfiguration
{
    @Bean(CurationDocumentService.SERVICE_NAME)
    public CurationDocumentService curationDocumentService(CasStorageService aCasStorageService,
            AnnotationSchemaService aAnnotationService, ProjectService aProjectService,
            EntityManager aEntityManager)
    {
        return new CurationDocumentServiceImpl(aCasStorageService, aAnnotationService,
                aProjectService, aEntityManager);
    }
}

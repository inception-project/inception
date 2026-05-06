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
package de.tudarmstadt.ukp.inception.kb.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseServiceImpl;
import de.tudarmstadt.ukp.inception.kb.exporter.KnowledgeBaseExporter;
import de.tudarmstadt.ukp.inception.kb.footprint.KnowledgeBaseFootprintProvider;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Configuration
@ConditionalOnProperty(prefix = "knowledge-base", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(KnowledgeBasePropertiesImpl.class)
public class KnowledgeBaseServiceAutoConfiguration
{
    private @PersistenceContext EntityManager entityManager;

    @Bean
    public KnowledgeBaseExporter knowledgeBaseExporter(KnowledgeBaseService aKbService,
            KnowledgeBaseProperties aKbProperties, AnnotationSchemaService aSchemaService)
    {
        return new KnowledgeBaseExporter(aKbService, aKbProperties, aSchemaService);
    }

    @Bean
    public KnowledgeBaseService knowledgeBaseService(RepositoryProperties aRepoProperties,
            KnowledgeBaseProperties aKbProperties)
    {
        return new KnowledgeBaseServiceImpl(aRepoProperties, aKbProperties, entityManager);
    }

    @Bean
    public KnowledgeBaseFootprintProvider KnowledgeBaseFootprintProvider(
            KnowledgeBaseService aKnowledgeBaseService)
    {
        return new KnowledgeBaseFootprintProvider(aKnowledgeBaseService);
    }
}

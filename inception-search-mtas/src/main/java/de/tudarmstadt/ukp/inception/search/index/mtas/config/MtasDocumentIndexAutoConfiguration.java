/*
 * Copyright 2020
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
package de.tudarmstadt.ukp.inception.search.index.mtas.config;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.search.FeatureIndexingSupportRegistry;
import de.tudarmstadt.ukp.inception.search.config.SearchServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.search.index.PhysicalIndexRegistry;
import de.tudarmstadt.ukp.inception.search.index.mtas.MtasDocumentIndexFactory;

@AutoConfigureAfter(SearchServiceAutoConfiguration.class)
@ConditionalOnBean(PhysicalIndexRegistry.class)
public class MtasDocumentIndexAutoConfiguration
{
    @Bean
    public MtasDocumentIndexFactory mtasDocumentIndexFactory(AnnotationSchemaService aSchemaService,
            DocumentService aDocumentService, RepositoryProperties aRepositoryProperties,
            FeatureIndexingSupportRegistry aFeatureIndexingSupportRegistry,
            FeatureSupportRegistry aFeatureSupportRegistry)
    {
        return new MtasDocumentIndexFactory(aSchemaService, aDocumentService, aRepositoryProperties,
                aFeatureIndexingSupportRegistry, aFeatureSupportRegistry);
    }
}

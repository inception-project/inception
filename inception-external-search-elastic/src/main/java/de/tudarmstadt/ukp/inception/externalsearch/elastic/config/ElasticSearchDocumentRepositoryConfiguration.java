/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.inception.externalsearch.elastic.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchService;
import de.tudarmstadt.ukp.inception.externalsearch.elastic.ElasticSearchProviderFactory;

@Configuration
@ConditionalOnBean(ExternalSearchService.class)
public class ElasticSearchDocumentRepositoryConfiguration
{
    @Bean
    public ElasticSearchProviderFactory elasticSearchProviderFactory()
    {
        return new ElasticSearchProviderFactory();
    }
}

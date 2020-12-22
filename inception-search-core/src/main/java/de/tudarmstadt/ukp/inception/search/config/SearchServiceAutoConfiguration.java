/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.inception.search.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.search.FeatureIndexingSupport;
import de.tudarmstadt.ukp.inception.search.FeatureIndexingSupportRegistry;
import de.tudarmstadt.ukp.inception.search.FeatureIndexingSupportRegistryImpl;
import de.tudarmstadt.ukp.inception.search.PrimitiveUimaIndexingSupport;
import de.tudarmstadt.ukp.inception.search.SearchService;
import de.tudarmstadt.ukp.inception.search.SearchServiceImpl;
import de.tudarmstadt.ukp.inception.search.index.PhysicalIndexFactory;
import de.tudarmstadt.ukp.inception.search.index.PhysicalIndexRegistry;
import de.tudarmstadt.ukp.inception.search.index.PhysicalIndexRegistryImpl;
import de.tudarmstadt.ukp.inception.search.log.SearchQueryEventAdapter;
import de.tudarmstadt.ukp.inception.search.scheduling.IndexScheduler;
import de.tudarmstadt.ukp.inception.search.scheduling.IndexSchedulerImpl;

@Configuration
@EnableConfigurationProperties(SearchServicePropertiesImpl.class)
@ConditionalOnProperty(prefix = "search", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SearchServiceAutoConfiguration
{
    @Bean
    public SearchService searchService(DocumentService aDocumentService,
            ProjectService aProjectService, PhysicalIndexRegistry aPhysicalIndexRegistry,
            IndexScheduler aIndexScheduler, SearchServiceProperties aProperties)
    {
        return new SearchServiceImpl(aDocumentService, aProjectService, aPhysicalIndexRegistry,
                aIndexScheduler, aProperties);
    }

    @Bean
    public SearchQueryEventAdapter searchQueryEventAdapter()
    {
        return new SearchQueryEventAdapter();
    }

    @Bean
    public PhysicalIndexRegistry physicalIndexRegistry(
            @Lazy @Autowired(required = false) List<PhysicalIndexFactory> aExtensions)
    {
        return new PhysicalIndexRegistryImpl(aExtensions);
    }

    @Bean
    public FeatureIndexingSupportRegistry featureIndexingSupportRegistry(
            @Lazy @Autowired(required = false) List<FeatureIndexingSupport> aIndexingSupports)
    {
        return new FeatureIndexingSupportRegistryImpl(aIndexingSupports);
    }

    @Bean
    public SearchServiceProperties searchServiceProperties()
    {
        return new SearchServicePropertiesImpl();
    }

    @Bean
    public IndexScheduler indexScheduler()
    {
        return new IndexSchedulerImpl();
    }

    @Bean
    public PrimitiveUimaIndexingSupport primitiveUimaIndexingSupport(
            @Autowired FeatureSupportRegistry aFeatureSupportRegistry)
    {
        return new PrimitiveUimaIndexingSupport(aFeatureSupportRegistry);
    }
}

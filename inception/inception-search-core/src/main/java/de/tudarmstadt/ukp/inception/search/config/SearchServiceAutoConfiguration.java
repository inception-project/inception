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
package de.tudarmstadt.ukp.inception.search.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
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

@Configuration
@EnableConfigurationProperties(SearchServicePropertiesImpl.class)
@ConditionalOnProperty(prefix = "search", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SearchServiceAutoConfiguration
{
    @Bean
    public SearchService searchService(DocumentService aDocumentService,
            ProjectService aProjectService, PhysicalIndexRegistry aPhysicalIndexRegistry,
            SchedulingService aSchedulingService, SearchServiceProperties aProperties,
            PreferencesService aPreferencesService)
    {
        return new SearchServiceImpl(aDocumentService, aProjectService, aPhysicalIndexRegistry,
                aSchedulingService, aProperties, aPreferencesService);
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
    public PrimitiveUimaIndexingSupport primitiveUimaIndexingSupport(
            FeatureSupportRegistry aFeatureSupportRegistry)
    {
        return new PrimitiveUimaIndexingSupport(aFeatureSupportRegistry);
    }
}

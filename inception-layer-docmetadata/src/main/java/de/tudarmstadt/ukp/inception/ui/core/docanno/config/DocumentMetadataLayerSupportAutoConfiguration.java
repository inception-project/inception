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
package de.tudarmstadt.ukp.inception.ui.core.docanno.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerType;
import de.tudarmstadt.ukp.inception.ui.core.docanno.layer.DocumentMetadataLayerSupport;
import de.tudarmstadt.ukp.inception.ui.core.docanno.sidebar.DocumentMetadataSidebarFactory;

/**
 * Provides support for document-level annotations.
 */
@Configuration
@EnableConfigurationProperties(DocumentMetadataLayerSupportPropertiesImpl.class)
public class DocumentMetadataLayerSupportAutoConfiguration
{
    @Bean
    @ConditionalOnProperty(prefix = "documentmetadata", name = "enabled", havingValue = "true", matchIfMissing = true)
    public DocumentMetadataSidebarFactory documentMetadataSidebarFactory()
    {
        return new DocumentMetadataSidebarFactory();
    }

    /**
     * This bean remains enabled so we don't break existing projects when disabling metadata
     * support. Instead we return {@code true} from {@link LayerType#isInternal()} to prevent the
     * use from creating new layers of this type.
     */
    @Bean
    public DocumentMetadataLayerSupport documentMetadataLayerSupport(
            FeatureSupportRegistry aFeatureSupportRegistry,
            ApplicationEventPublisher aEventPublisher,
            DocumentMetadataLayerSupportProperties aProperties)
    {
        return new DocumentMetadataLayerSupport(aFeatureSupportRegistry, aEventPublisher,
                aProperties);
    }
}

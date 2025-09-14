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
package de.tudarmstadt.ukp.inception.assistant.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.assistant.documents.DocumentContextRetriever;
import de.tudarmstadt.ukp.inception.assistant.tool.ClockToolLibrary;
import de.tudarmstadt.ukp.inception.assistant.tool.RecommenderToolLibrary;
import de.tudarmstadt.ukp.inception.assistant.tool.RetrieverToolLibrary;
import de.tudarmstadt.ukp.inception.documents.api.DocumentAccess;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;

@ConditionalOnWebApplication
@Configuration
@ConditionalOnProperty(prefix = "assistant", name = "enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties({ AssistantPropertiesImpl.class,
        AssistantDocumentIndexPropertiesImpl.class, })
public class AssistantToolsAutoConfiguration
{
    @Bean
    public ClockToolLibrary clockToolLibrary()
    {
        return new ClockToolLibrary();
    }

    @Bean
    public RetrieverToolLibrary retrieverToolLibrary(
            DocumentContextRetriever aDocumentContextRetriever)
    {
        return new RetrieverToolLibrary(aDocumentContextRetriever);
    }

    @Bean
    public RecommenderToolLibrary recommenderToolLibrary(
            RecommendationService aRecommendationService, UserDao aUserService,
            DocumentAccess aDocumentAccess, SchedulingService aSchedulingService)
    {
        return new RecommenderToolLibrary(aRecommendationService, aUserService, aDocumentAccess,
                aSchedulingService);
    }
}

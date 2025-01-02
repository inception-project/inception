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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.session.SessionRegistry;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.EncodingRegistry;

import de.tudarmstadt.ukp.inception.assistant.AssistantService;
import de.tudarmstadt.ukp.inception.assistant.AssistantServiceImpl;
import de.tudarmstadt.ukp.inception.assistant.documents.DocumentQueryService;
import de.tudarmstadt.ukp.inception.assistant.documents.DocumentQueryServiceImpl;
import de.tudarmstadt.ukp.inception.assistant.embedding.EmbeddingService;
import de.tudarmstadt.ukp.inception.assistant.embedding.EmbeddingServiceImpl;
import de.tudarmstadt.ukp.inception.assistant.sidebar.AssistantSidebarFactory;
import de.tudarmstadt.ukp.inception.assistant.userguide.UserGuideQueryService;
import de.tudarmstadt.ukp.inception.assistant.userguide.UserGuideQueryServiceImpl;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaClient;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;

@ConditionalOnWebApplication
@Configuration
@ConditionalOnProperty(prefix = "assistant", name = "enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties({ AssistantPropertiesImpl.class,
        AssistantDocumentIndexPropertiesImpl.class, })
public class AssistantAutoConfiguration
{
    @Bean
    public AssistantService assistantService(SessionRegistry aSessionRegistry,
            SimpMessagingTemplate aMsgTemplate, OllamaClient aOllamaClient,
            AssistantProperties aProperties, UserGuideQueryService aDocumentationIndexingService,
            DocumentQueryService aDocumentQueryService, EncodingRegistry aEncodingRegistry)
    {
        return new AssistantServiceImpl(aSessionRegistry, aMsgTemplate, aOllamaClient, aProperties,
                aDocumentationIndexingService, aDocumentQueryService, aEncodingRegistry);
    }

    @Bean
    public AssistantSidebarFactory assistantSidebarFactory()
    {
        return new AssistantSidebarFactory();
    }

    @Bean
    public UserGuideQueryService userManualQueryService(AssistantProperties aProperties,
            SchedulingService aSchedulingService, EmbeddingService aEmbeddingService)
    {
        return new UserGuideQueryServiceImpl(aProperties, aSchedulingService, aEmbeddingService);
    }
    
    @Bean
    public EncodingRegistry encodingRegistry() {
        return Encodings.newLazyEncodingRegistry();
    }

    @Bean
    public EmbeddingService EmbeddingService(AssistantProperties aProperties, OllamaClient aOllamaClient) {
        return new EmbeddingServiceImpl(aProperties, aOllamaClient);
    }

    @Bean
    public DocumentQueryService documentQueryService(AssistantProperties aProperties,
            RepositoryProperties aRepositoryProperties,
            AssistantDocumentIndexProperties aIndexProperties, SchedulingService aSchedulingService,
            OllamaClient aOllamaClient, EmbeddingService aEmbeddingService)
    {
        return new DocumentQueryServiceImpl(aProperties, aRepositoryProperties, aIndexProperties,
                aSchedulingService, aEmbeddingService);
    }
}

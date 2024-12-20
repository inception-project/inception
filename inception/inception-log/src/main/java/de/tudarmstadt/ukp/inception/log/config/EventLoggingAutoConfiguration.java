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
package de.tudarmstadt.ukp.inception.log.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.log.EventLoggingListener;
import de.tudarmstadt.ukp.inception.log.EventRepository;
import de.tudarmstadt.ukp.inception.log.EventRepositoryImpl;
import de.tudarmstadt.ukp.inception.log.adapter.EventLoggingAdapter;
import de.tudarmstadt.ukp.inception.log.adapter.EventLoggingAdapterRegistry;
import de.tudarmstadt.ukp.inception.log.adapter.EventLoggingAdapterRegistryImpl;
import de.tudarmstadt.ukp.inception.log.exporter.LoggedEventExporter;
import jakarta.persistence.EntityManager;

/**
 * Provides support event logging.
 */
@Configuration
@ConditionalOnProperty(prefix = "event-logging", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(EventLoggingPropertiesImpl.class)
public class EventLoggingAutoConfiguration
{
    @Bean
    public EventRepository eventRepository(EntityManager aEntityManager)
    {
        return new EventRepositoryImpl(aEntityManager);
    }

    @Bean
    public EventLoggingAdapterRegistry eventLoggingAdapterRegistry(
            @Lazy @Autowired(required = false) List<EventLoggingAdapter<?>> aAdapters)
    {
        return new EventLoggingAdapterRegistryImpl(aAdapters);
    }

    // When running in CLI mode, we usually perform bulk actions. We should not log these all.
    // Also, the CLI may shut down very fast (e.g. when displaying help) and close the DB before all
    // pending events would have been flushed which would create an exception.
    @ConditionalOnWebApplication
    @Bean
    public EventLoggingListener eventLoggingListener(EventRepository aRepo,
            EventLoggingAdapterRegistry aAdapterRegistry, EventLoggingProperties aProperties)
    {
        return new EventLoggingListener(aRepo, aProperties, aAdapterRegistry);
    }

    @Bean
    public LoggedEventExporter loggedEventExporter(EventRepository aEventRepository,
            DocumentService aDocumentService)
    {
        return new LoggedEventExporter(aEventRepository, aDocumentService);
    }
}

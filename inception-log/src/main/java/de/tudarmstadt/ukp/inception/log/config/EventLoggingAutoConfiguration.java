/*
 * Copyright 2021
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
package de.tudarmstadt.ukp.inception.log.config;

import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.inception.log.EventLoggingListener;
import de.tudarmstadt.ukp.inception.log.EventRepository;
import de.tudarmstadt.ukp.inception.log.EventRepositoryImpl;
import de.tudarmstadt.ukp.inception.log.adapter.EventLoggingAdapter;

/**
 * Provides support event logging.
 */
@Configuration
@ConditionalOnProperty(prefix = "event-logging", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(EventLoggingPropertiesImpl.class)
public class EventLoggingAutoConfiguration
{
    @Bean
    @Autowired
    public EventRepository eventRepository(EntityManager aEntityManager)
    {
        return new EventRepositoryImpl(aEntityManager);
    }

    @Bean
    @Autowired
    public EventLoggingListener eventLoggingListener(EventRepository aRepo,
            @Lazy @Autowired(required = false) List<EventLoggingAdapter<?>> aAdapters,
            EventLoggingProperties aProperties)
    {
        return new EventLoggingListener(aRepo, aAdapters, aProperties);
    }
}

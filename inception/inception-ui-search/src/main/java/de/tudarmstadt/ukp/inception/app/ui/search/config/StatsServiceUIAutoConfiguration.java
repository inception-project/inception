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
package de.tudarmstadt.ukp.inception.app.ui.search.config;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.inception.app.ui.search.sidebar.StatisticsAnnotationSidebarFactory;
import de.tudarmstadt.ukp.inception.search.SearchService;
import de.tudarmstadt.ukp.inception.search.config.SearchServiceAutoConfiguration;

@ConditionalOnWebApplication
@Configuration
@AutoConfigureAfter(SearchServiceAutoConfiguration.class)
@ConditionalOnBean(SearchService.class)
public class StatsServiceUIAutoConfiguration
{
    @ConditionalOnProperty(prefix = "search.statistics-sidebar", name = "enabled", //
            havingValue = "true", matchIfMissing = true)
    @Bean
    public StatisticsAnnotationSidebarFactory statisticsAnnotationSidebarFactory()
    {
        return new StatisticsAnnotationSidebarFactory();
    }
}

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
package de.tudarmstadt.ukp.inception.processing.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.processing.BulkProcessingPageMenuItem;
import de.tudarmstadt.ukp.inception.processing.BulkProcessor;
import de.tudarmstadt.ukp.inception.processing.BulkProcessorRegistry;
import de.tudarmstadt.ukp.inception.processing.BulkProcessorRegistryImpl;
import de.tudarmstadt.ukp.inception.processing.curation.BulkCurationProcessor;
import de.tudarmstadt.ukp.inception.processing.recommender.BulkRecommenderProcessor;
import de.tudarmstadt.ukp.inception.processing.tagset.TagSetExtractionProcessor;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import jakarta.servlet.ServletContext;

@ConditionalOnProperty(prefix = "bulk-processing", name = "enabled", havingValue = "true", matchIfMissing = true)
@Configuration
public class ProcessingAutoConfiguration
{
    @ConditionalOnWebApplication
    @Bean
    @ConditionalOnExpression("${websocket.enabled:true} and ${recommender.enabled:true}")
    public BulkProcessingPageMenuItem bulkProcessingPageMenuItem(UserDao aUserRepo,
            ProjectService aProjectService, ServletContext aServletContext)
    {
        return new BulkProcessingPageMenuItem(aUserRepo, aProjectService, aServletContext);
    }

    @Bean
    public BulkProcessorRegistry bulkProcessorRegistry(
            @Lazy @Autowired(required = false) List<BulkProcessor> aExtensions)
    {
        return new BulkProcessorRegistryImpl(aExtensions);
    }

    @ConditionalOnProperty(prefix = "bulk-processing.process.auto-curation", //
            name = "enabled", havingValue = "true", matchIfMissing = false)
    @Bean
    public BulkCurationProcessor bulkCurationProcessor()
    {
        return new BulkCurationProcessor();
    }

    @ConditionalOnProperty(prefix = "bulk-processing.process.apply-recommender", //
            name = "enabled", havingValue = "true", matchIfMissing = false)
    @Bean
    public BulkRecommenderProcessor bulkRecommenderProcessor()
    {
        return new BulkRecommenderProcessor();
    }

    @ConditionalOnProperty(prefix = "bulk-processing.process.extract-tagset", //
            name = "enabled", havingValue = "true", matchIfMissing = true)
    @Bean
    public TagSetExtractionProcessor tagSetExtractionProcessor()
    {
        return new TagSetExtractionProcessor();
    }
}

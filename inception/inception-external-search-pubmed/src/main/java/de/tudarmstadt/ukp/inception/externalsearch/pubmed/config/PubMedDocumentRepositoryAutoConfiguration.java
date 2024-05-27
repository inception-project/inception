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
package de.tudarmstadt.ukp.inception.externalsearch.pubmed.config;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.inception.externalsearch.ExternalSearchService;
import de.tudarmstadt.ukp.inception.externalsearch.config.ExternalSearchAutoConfiguration;
import de.tudarmstadt.ukp.inception.externalsearch.pubmed.PubMedCentralProviderFactory;
import de.tudarmstadt.ukp.inception.externalsearch.pubmed.entrez.EntrezClient;
import de.tudarmstadt.ukp.inception.externalsearch.pubmed.pmcoa.PmcOaClient;
import de.tudarmstadt.ukp.inception.io.bioc.BioCFormatSupport;
import de.tudarmstadt.ukp.inception.io.bioc.config.BioCAutoConfiguration;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

@Configuration
@AutoConfigureAfter({ ExternalSearchAutoConfiguration.class, BioCAutoConfiguration.class,
        PubMedServicesAutoConfiguration.class })
@ConditionalOnProperty(prefix = "external-search.pmc", //
        name = "enabled", havingValue = "true", matchIfMissing = false)
@ConditionalOnBean({ ExternalSearchService.class, BioCFormatSupport.class })
public class PubMedDocumentRepositoryAutoConfiguration
{
    @Bean
    public PubMedCentralProviderFactory pubMedCentralProviderFactory(EntrezClient aEntrezClient,
            PmcOaClient aPmcOaClient, AnnotationSchemaService aSchemaService)
    {
        return new PubMedCentralProviderFactory(aEntrezClient, aPmcOaClient, aSchemaService);
    }
}

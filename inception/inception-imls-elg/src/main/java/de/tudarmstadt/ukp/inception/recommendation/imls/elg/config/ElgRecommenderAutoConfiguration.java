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
package de.tudarmstadt.ukp.inception.recommendation.imls.elg.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.inception.recommendation.imls.elg.ElgRecommenderFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.client.ElgAuthenticationClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.client.ElgAuthenticationClientImpl;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.client.ElgCatalogClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.client.ElgCatalogClientImpl;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.client.ElgServiceClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.client.ElgServiceClientImpl;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.service.ElgService;
import de.tudarmstadt.ukp.inception.recommendation.imls.elg.service.ElgServiceImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Configuration
@ConditionalOnProperty(prefix = "recommender.elg", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ElgRecommenderAutoConfiguration
{
    private @PersistenceContext EntityManager entityManager;

    @Bean
    public ElgRecommenderFactory elgRecommenderFactory(ElgService aElgService)
    {
        return new ElgRecommenderFactory(aElgService);
    }

    @Bean
    public ElgCatalogClient elgCatalogClient()
    {
        return new ElgCatalogClientImpl();
    }

    @Bean
    public ElgServiceClient elgServiceClient()
    {
        return new ElgServiceClientImpl();
    }

    @Bean
    public ElgAuthenticationClient elgAuthenticationClient()
    {
        return new ElgAuthenticationClientImpl();
    }

    @Bean
    public ElgService elgService(ElgAuthenticationClient aElgAuthenticationClient,
            ElgServiceClient aElgServiceClient, EntityManager aEntityManager)
    {
        return new ElgServiceImpl(aElgAuthenticationClient, aElgServiceClient, entityManager);
    }
}

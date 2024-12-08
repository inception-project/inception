/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
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
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.config;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.config.RecommenderServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.relation.StringMatchingRelationRecommenderFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.StringMatchingRecommenderFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazeteer.GazeteerService;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazeteer.GazeteerServiceImpl;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazeteer.exporter.GazeteerExporter;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.ner.StringMatchingNerClassificationToolFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.pos.StringMatchingPosClassificationToolFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Configuration
@AutoConfigureAfter(RecommenderServiceAutoConfiguration.class)
@ConditionalOnBean(RecommendationService.class)
public class StringMatchingRecommenderAutoConfiguration
{
    private @PersistenceContext EntityManager entityManager;

    @Bean
    public GazeteerExporter gazeteerExporter(RecommendationService aRecommendationService,
            GazeteerService aGazeteerService)
    {
        return new GazeteerExporter(aRecommendationService, aGazeteerService);
    }

    @Bean
    public GazeteerService gazeteerService(RepositoryProperties aRepositoryProperties)
    {
        return new GazeteerServiceImpl(aRepositoryProperties, entityManager);
    }

    @Bean
    public StringMatchingNerClassificationToolFactory stringMatchingNerClassificationToolFactory()
    {
        return new StringMatchingNerClassificationToolFactory();
    }

    @Bean
    public StringMatchingPosClassificationToolFactory stringMatchingPosClassificationToolFactory()
    {
        return new StringMatchingPosClassificationToolFactory();
    }

    @Bean
    public StringMatchingRecommenderFactory stringMatchingRecommenderFactory(
            GazeteerService aGazeteerService)
    {
        return new StringMatchingRecommenderFactory(aGazeteerService);
    }

    @Bean
    @ConditionalOnProperty(prefix = "recommender.string-matching.relation", name = "enabled", havingValue = "true")
    public StringMatchingRelationRecommenderFactory stringMatchingRelationRecommenderFactory()
    {
        return new StringMatchingRelationRecommenderFactory();
    }
}

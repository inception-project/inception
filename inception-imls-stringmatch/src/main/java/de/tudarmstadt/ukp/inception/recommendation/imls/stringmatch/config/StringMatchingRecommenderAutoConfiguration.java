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

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.StringMatchingRecommenderFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.exporter.GazeteerExporter;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.gazeteer.GazeteerService;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.gazeteer.GazeteerServiceImpl;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.ner.StringMatchingNerClassificationToolFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.pos.StringMatchingPosClassificationToolFactory;

@Configuration
@ConditionalOnBean(RecommendationService.class)
public class StringMatchingRecommenderAutoConfiguration
{
    private @PersistenceContext EntityManager entityManager;

    @Bean
    @Autowired
    public GazeteerExporter gazeteerExporter(RecommendationService aRecommendationService,
            GazeteerService aGazeteerService)
    {
        return new GazeteerExporter(aRecommendationService, aGazeteerService);
    }

    @Bean
    @Autowired
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
    @Autowired
    public StringMatchingRecommenderFactory stringMatchingRecommenderFactory(
            GazeteerService aGazeteerService)
    {
        return new StringMatchingRecommenderFactory(aGazeteerService);
    }
}

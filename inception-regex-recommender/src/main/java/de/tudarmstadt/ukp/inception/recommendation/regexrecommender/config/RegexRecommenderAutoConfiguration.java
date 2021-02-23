/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tudarmstadt.ukp.inception.recommendation.regexrecommender.config;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.gazeteer.GazeteerService;
import de.tudarmstadt.ukp.inception.recommendation.regexrecommender.RegexRecommenderFactory;
import de.tudarmstadt.ukp.inception.recommendation.regexrecommender.RegexCounter;
import de.tudarmstadt.ukp.inception.recommendation.regexrecommender.exporter.GazeteerExporter;
import de.tudarmstadt.ukp.inception.recommendation.regexrecommender.gazeteer.GazeteerServiceImpl;
import de.tudarmstadt.ukp.inception.recommendation.regexrecommender.listener.RecommendationAcceptedListener;
import de.tudarmstadt.ukp.inception.recommendation.regexrecommender.listener.RecommendationRejectedListener;

@Configuration
@ConditionalOnBean(RecommendationService.class)
public class RegexRecommenderAutoConfiguration
{
    private @PersistenceContext EntityManager entityManager;
    
    @Bean
    @Autowired
    public GazeteerExporter gazeteerExporterRegex(RecommendationService aRecommendationService,
            GazeteerServiceImpl aGazeteerService)
    {
        return new GazeteerExporter(aRecommendationService, aGazeteerService);
    }
    
    @Bean
    @Autowired
    public GazeteerServiceImpl gazeteerServiceRegex(RepositoryProperties aRepositoryProperties)
    {   
        return new GazeteerServiceImpl(aRepositoryProperties, entityManager);
    }

    @Bean
    @Autowired
    public RecommendationAcceptedListener recommendationAcceptedListener() {
        return new RecommendationAcceptedListener();
    }
    
    @Bean
    @Autowired
    public RecommendationRejectedListener recommendationRejectedListener() {
        return new RecommendationRejectedListener();
    }
    
  
    @Bean
    @Autowired
    public RegexRecommenderFactory regexRecommenderFactory(GazeteerServiceImpl aGazeteerService,
                                                       RecommendationAcceptedListener aAccListener,
                                                       RecommendationRejectedListener aRejListener)
    {   
        return new RegexRecommenderFactory(aGazeteerService, aAccListener, aRejListener);
    }
}

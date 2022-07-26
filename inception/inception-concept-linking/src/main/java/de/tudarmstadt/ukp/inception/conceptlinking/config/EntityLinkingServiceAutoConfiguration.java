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
package de.tudarmstadt.ukp.inception.conceptlinking.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.inception.conceptlinking.feature.CasingFeatureGenerator;
import de.tudarmstadt.ukp.inception.conceptlinking.feature.EntityRankingFeatureGenerator;
import de.tudarmstadt.ukp.inception.conceptlinking.feature.FrequencyFeatureGenerator;
import de.tudarmstadt.ukp.inception.conceptlinking.feature.LevenshteinFeatureGenerator;
import de.tudarmstadt.ukp.inception.conceptlinking.feature.SemanticSignatureFeatureGenerator;
import de.tudarmstadt.ukp.inception.conceptlinking.feature.WikidataIdRankFeatureGenerator;
import de.tudarmstadt.ukp.inception.conceptlinking.recommender.NamedEntityLinkerFactory;
import de.tudarmstadt.ukp.inception.conceptlinking.service.ConceptLinkingService;
import de.tudarmstadt.ukp.inception.conceptlinking.service.ConceptLinkingServiceImpl;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.config.RecommenderServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureSupportRegistry;

@Configuration
@AutoConfigureAfter({ KnowledgeBaseServiceAutoConfiguration.class,
        RecommenderServiceAutoConfiguration.class })
@ConditionalOnBean(KnowledgeBaseService.class)
@ConditionalOnProperty(prefix = "knowledge-base.entity-linking", //
        name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(EntityLinkingPropertiesImpl.class)
public class EntityLinkingServiceAutoConfiguration
{
    @Bean
    @Autowired
    public ConceptLinkingService conceptLinkingService(KnowledgeBaseService aKbService,
            EntityLinkingPropertiesImpl aProperties, RepositoryProperties aRepoProperties,
            @Lazy @Autowired(required = false) List<EntityRankingFeatureGenerator> aFeatureGenerators)
    {
        return new ConceptLinkingServiceImpl(aKbService, aProperties, aRepoProperties,
                aFeatureGenerators);
    }

    @Bean
    public EntityLinkingProperties entityLinkingProperties()
    {
        return new EntityLinkingPropertiesImpl();
    }

    @Bean
    public CasingFeatureGenerator casingFeatureGenerator()
    {
        return new CasingFeatureGenerator();
    }

    @Bean
    public LevenshteinFeatureGenerator levenshteinFeatureGenerator()
    {
        return new LevenshteinFeatureGenerator();
    }

    @Bean
    @Autowired
    public WikidataIdRankFeatureGenerator wikidataIdRankFeatureGenerator(
            KnowledgeBaseService aKbService)
    {
        return new WikidataIdRankFeatureGenerator(aKbService);
    }

    @ConditionalOnBean(RecommendationService.class)
    @Bean
    @Autowired
    public NamedEntityLinkerFactory namedEntityLinkerFactory(KnowledgeBaseService aKbService,
            ConceptLinkingService aClService, FeatureSupportRegistry aFsRegistry)
    {
        return new NamedEntityLinkerFactory(aKbService, aClService, aFsRegistry);
    }

    // @Bean
    // @Autowired
    public FrequencyFeatureGenerator frequencyFeatureGenerator(RepositoryProperties aRepoProperties)
    {
        return new FrequencyFeatureGenerator(aRepoProperties);
    }

    // @Bean
    // @Autowired
    public SemanticSignatureFeatureGenerator semanticSignatureFeatureGenerator(
            KnowledgeBaseService aKbService, RepositoryProperties aRepoProperties,
            EntityLinkingProperties aProperties)
    {
        return new SemanticSignatureFeatureGenerator(aKbService, aRepoProperties, aProperties);
    }
}

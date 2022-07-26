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
package de.tudarmstadt.ukp.inception.project.initializers.wikidatalinking.config;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseProperties;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.project.initializers.wikidatalinking.EntityLinkingProjectInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.wikidatalinking.NamedEntityIdentifierStringRecommenderInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.wikidatalinking.WikiDataKnowledgeBaseInitializer;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.config.RecommenderServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.config.StringMatchingRecommenderAutoConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.StringMatchingRecommenderFactory;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

@AutoConfigureAfter({ //
        KnowledgeBaseServiceAutoConfiguration.class, //
        RecommenderServiceAutoConfiguration.class, //
        StringMatchingRecommenderAutoConfiguration.class })
@Configuration
public class WikiDataLinkingProjectInitializersAutoConfiguration
{
    @ConditionalOnBean(RecommendationService.class)
    @Bean
    public NamedEntityIdentifierStringRecommenderInitializer namedEntityIdentifierStringRecommenderInitializer(
            RecommendationService aRecommenderService, AnnotationSchemaService aAnnotationService,
            StringMatchingRecommenderFactory aRecommenderFactory)
    {
        return new NamedEntityIdentifierStringRecommenderInitializer(aRecommenderService,
                aAnnotationService, aRecommenderFactory);
    }

    @ConditionalOnBean(KnowledgeBaseService.class)
    @Bean
    public WikiDataKnowledgeBaseInitializer wikiDataKnowledgeBaseInitializer(
            KnowledgeBaseService aKbService, KnowledgeBaseProperties aKbProperties)
    {
        return new WikiDataKnowledgeBaseInitializer(aKbService, aKbProperties);
    }

    @ConditionalOnBean(WikiDataKnowledgeBaseInitializer.class)
    @Bean
    public EntityLinkingProjectInitializer entityLinkingProjectInitializer(
            ApplicationContext aContext, AnnotationSchemaService aAnnotationService)
    {
        return new EntityLinkingProjectInitializer(aContext, aAnnotationService);
    }
}

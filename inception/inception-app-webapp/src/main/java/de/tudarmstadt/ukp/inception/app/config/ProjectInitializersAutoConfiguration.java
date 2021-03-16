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
package de.tudarmstadt.ukp.inception.app.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.inception.initializers.BasicProjectInitializer;
import de.tudarmstadt.ukp.inception.initializers.BasicRelationLayerInitializer;
import de.tudarmstadt.ukp.inception.initializers.BasicRelationTagSetInitializer;
import de.tudarmstadt.ukp.inception.initializers.BasicSpanLayerInitializer;
import de.tudarmstadt.ukp.inception.initializers.BasicSpanRecommenderInitializer;
import de.tudarmstadt.ukp.inception.initializers.BasicSpanTagSetInitializer;
import de.tudarmstadt.ukp.inception.initializers.EntityLinkingProjectInitializer;
import de.tudarmstadt.ukp.inception.initializers.NamedEntityIdentifierStringRecommenderInitializer;
import de.tudarmstadt.ukp.inception.initializers.StandardProjectInitializer;
import de.tudarmstadt.ukp.inception.initializers.WikiDataKnowledgeBaseInitializer;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseProperties;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.config.RecommenderServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.StringMatchingRecommenderFactory;

@AutoConfigureAfter({ KnowledgeBaseServiceAutoConfiguration.class,
        RecommenderServiceAutoConfiguration.class })
// @AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Configuration
public class ProjectInitializersAutoConfiguration
{
    @Autowired
    @Bean
    public BasicProjectInitializer basicProjectInitializer(ApplicationContext aContext)
    {
        return new BasicProjectInitializer(aContext);
    }

    @Autowired
    @Bean
    public BasicRelationLayerInitializer basicRelationLayerInitializer(
            AnnotationSchemaService aAnnotationSchemaService)
    {
        return new BasicRelationLayerInitializer(aAnnotationSchemaService);
    }

    @Autowired
    @Bean
    public BasicRelationTagSetInitializer basicRelationTagSetInitializer(
            AnnotationSchemaService aAnnotationSchemaService)
    {
        return new BasicRelationTagSetInitializer(aAnnotationSchemaService);
    }

    @Autowired
    @Bean
    public BasicSpanLayerInitializer basicSpanLayerInitializer(
            AnnotationSchemaService aAnnotationSchemaService)
    {
        return new BasicSpanLayerInitializer(aAnnotationSchemaService);
    }

    @ConditionalOnBean(RecommendationService.class)
    @Autowired
    @Bean
    public BasicSpanRecommenderInitializer basicSpanRecommenderInitializer(
            RecommendationService aRecommenderService, AnnotationSchemaService aAnnotationService,
            StringMatchingRecommenderFactory aRecommenderFactory)
    {
        return new BasicSpanRecommenderInitializer(aRecommenderService, aAnnotationService,
                aRecommenderFactory);
    }

    @Autowired
    @Bean
    public BasicSpanTagSetInitializer basicSpanTagSetInitializer(
            AnnotationSchemaService aAnnotationSchemaService)
    {
        return new BasicSpanTagSetInitializer(aAnnotationSchemaService);
    }

    @ConditionalOnBean(RecommendationService.class)
    @Autowired
    @Bean
    public NamedEntityIdentifierStringRecommenderInitializer namedEntityIdentifierStringRecommenderInitializer(
            RecommendationService aRecommenderService, AnnotationSchemaService aAnnotationService,
            StringMatchingRecommenderFactory aRecommenderFactory)
    {
        return new NamedEntityIdentifierStringRecommenderInitializer(aRecommenderService,
                aAnnotationService, aRecommenderFactory);
    }

    @Autowired
    @Bean
    public StandardProjectInitializer standardProjectInitializer(
            @Lazy ProjectService aProjectService)
    {
        return new StandardProjectInitializer(aProjectService);
    }

    @ConditionalOnBean(KnowledgeBaseService.class)
    @Autowired
    @Bean
    public WikiDataKnowledgeBaseInitializer wikiDataKnowledgeBaseInitializer(
            KnowledgeBaseService aKbService, KnowledgeBaseProperties aKbProperties)
    {
        return new WikiDataKnowledgeBaseInitializer(aKbService, aKbProperties);
    }

    @ConditionalOnBean(WikiDataKnowledgeBaseInitializer.class)
    @Autowired
    @Bean
    public EntityLinkingProjectInitializer entityLinkingProjectInitializer(
            ApplicationContext aContext, AnnotationSchemaService aAnnotationService)
    {
        return new EntityLinkingProjectInitializer(aContext, aAnnotationService);
    }
}

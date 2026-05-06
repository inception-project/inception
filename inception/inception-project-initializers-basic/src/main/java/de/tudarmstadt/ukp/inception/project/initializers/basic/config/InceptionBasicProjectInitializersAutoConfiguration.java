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
package de.tudarmstadt.ukp.inception.project.initializers.basic.config;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureSupport;
import de.tudarmstadt.ukp.inception.project.initializers.basic.BasicProjectInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.basic.BasicRelationLayerInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.basic.BasicRelationRecommenderInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.basic.BasicRelationTagSetInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.basic.BasicSpanLayerInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.basic.BasicSpanRecommenderInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.basic.BasicSpanTagSetInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.basic.CommentFeatureInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.basic.RelationSourceFeatureInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.basic.RelationTargetFeatureInitializer;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.config.RecommenderServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.config.StringMatchingRecommenderAutoConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.relation.StringMatchingRelationRecommenderFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.StringMatchingRecommenderFactory;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

@AutoConfigureAfter({ //
        RecommenderServiceAutoConfiguration.class, //
        StringMatchingRecommenderAutoConfiguration.class })
@Configuration
public class InceptionBasicProjectInitializersAutoConfiguration
{
    @Bean
    public BasicProjectInitializer basicProjectInitializer(ApplicationContext aContext)
    {
        return new BasicProjectInitializer(aContext);
    }

    @Bean
    public BasicRelationLayerInitializer basicRelationLayerInitializer(
            AnnotationSchemaService aAnnotationSchemaService)
    {
        return new BasicRelationLayerInitializer(aAnnotationSchemaService);
    }

    @Bean
    public BasicRelationTagSetInitializer basicRelationTagSetInitializer(
            AnnotationSchemaService aAnnotationSchemaService)
    {
        return new BasicRelationTagSetInitializer(aAnnotationSchemaService);
    }

    @Bean
    public BasicSpanLayerInitializer basicSpanLayerInitializer(
            AnnotationSchemaService aAnnotationSchemaService)
    {
        return new BasicSpanLayerInitializer(aAnnotationSchemaService);
    }

    @Bean
    public CommentFeatureInitializer commentFeatureInitializer(
            AnnotationSchemaService aAnnotationSchemaService,
            StringFeatureSupport aStringFeatureSupport)
    {
        return new CommentFeatureInitializer(aAnnotationSchemaService, aStringFeatureSupport);
    }

    @ConditionalOnBean({ RecommendationService.class, StringMatchingRecommenderFactory.class })
    @Bean
    public BasicSpanRecommenderInitializer basicSpanRecommenderInitializer(
            RecommendationService aRecommenderService, AnnotationSchemaService aAnnotationService,
            StringMatchingRecommenderFactory aRecommenderFactory)
    {
        return new BasicSpanRecommenderInitializer(aRecommenderService, aAnnotationService,
                aRecommenderFactory);
    }

    @ConditionalOnBean({ RecommendationService.class,
            StringMatchingRelationRecommenderFactory.class, BasicSpanRecommenderInitializer.class })
    @Bean
    public BasicRelationRecommenderInitializer basicRelationRecommenderInitializer(
            RecommendationService aRecommenderService, AnnotationSchemaService aAnnotationService,
            StringMatchingRelationRecommenderFactory aRecommenderFactory)
    {
        return new BasicRelationRecommenderInitializer(aRecommenderService, aAnnotationService,
                aRecommenderFactory);
    }

    @Bean
    public BasicSpanTagSetInitializer basicSpanTagSetInitializer(
            AnnotationSchemaService aAnnotationSchemaService)
    {
        return new BasicSpanTagSetInitializer(aAnnotationSchemaService);
    }

    @Bean
    public RelationSourceFeatureInitializer relationSourceFeatureInitializer(
            AnnotationSchemaService aAnnotationSchemaService)
    {
        return new RelationSourceFeatureInitializer(aAnnotationSchemaService);
    }

    @Bean
    public RelationTargetFeatureInitializer relationTargetFeatureInitializer(
            AnnotationSchemaService aAnnotationSchemaService)
    {
        return new RelationTargetFeatureInitializer(aAnnotationSchemaService);
    }
}

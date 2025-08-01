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
package de.tudarmstadt.ukp.inception.annotation.layer.relation.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.LayerBehaviorRegistry;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationAttachmentBehavior;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationEndpointChangeListener;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationEndpointFeatureSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationLayerSupportImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.behavior.RelationCrossSentenceBehavior;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.behavior.RelationOverlapBehavior;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.recommender.RelationSuggestionSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.undo.RelationAnnotationActionUndoSupport;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;

@Configuration
public class RelationLayerAutoConfiguration
{
    @Bean
    public RelationLayerSupport relationLayerSupport(FeatureSupportRegistry aFeatureSupportRegistry,
            ApplicationEventPublisher aEventPublisher,
            LayerBehaviorRegistry aLayerBehaviorsRegistry, ConstraintsService aConstraintsService)
    {
        return new RelationLayerSupportImpl(aFeatureSupportRegistry, aEventPublisher,
                aLayerBehaviorsRegistry, aConstraintsService);
    }

    @Bean
    public RelationEndpointChangeListener relationEndpointChangeListener(
            AnnotationSchemaService aSchemaService)
    {
        return new RelationEndpointChangeListener(aSchemaService);
    }
    
    @Bean
    public RelationEndpointFeatureSupport relationEndpointFeatureSupport()
    {
        return new RelationEndpointFeatureSupport();
    }

    @Bean
    public RelationAnnotationActionUndoSupport relationAnnotationActionUndoSupport()
    {
        return new RelationAnnotationActionUndoSupport();
    }

    @Bean
    public RelationAttachmentBehavior relationAttachmentBehavior()
    {
        return new RelationAttachmentBehavior();
    }

    @Bean
    public RelationCrossSentenceBehavior relationCrossSentenceBehavior()
    {
        return new RelationCrossSentenceBehavior();
    }

    @Bean
    public RelationOverlapBehavior relationOverlapBehavior()
    {
        return new RelationOverlapBehavior();
    }

    @ConditionalOnProperty(prefix = "recommender", name = "enabled", havingValue = "true", matchIfMissing = true)
    @Bean
    public RelationSuggestionSupport relationSuggestionSupport(
            RecommendationService aRecommendationService,
            LearningRecordService aLearningRecordService,
            ApplicationEventPublisher aApplicationEventPublisher,
            AnnotationSchemaService aSchemaService, FeatureSupportRegistry aFeatureSupportRegistry)
    {
        return new RelationSuggestionSupport(aRecommendationService, aLearningRecordService,
                aApplicationEventPublisher, aSchemaService, aFeatureSupportRegistry);
    }
}

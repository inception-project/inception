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
package de.tudarmstadt.ukp.clarin.webanno.api.dao.annotationservice.config;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.LayerBehavior;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.RelationAttachmentBehavior;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.RelationCrossSentenceBehavior;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.RelationOverlapBehavior;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAnchoringModeBehavior;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanCrossSentenceBehavior;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanOverlapBehavior;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.BooleanFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.NumberFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.SlotFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.StringFeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.config.PrimitiveUimaFeatureSupportProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.ChainLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerBehaviorRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerBehaviorRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.RelationLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.SpanLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.AnnotationSchemaServiceImpl;

@Configuration
public class AnnotationSchemaServiceAutoConfiguration
{
    private @PersistenceContext EntityManager entityManager;

    @Bean(AnnotationSchemaService.SERVICE_NAME)
    public AnnotationSchemaService annotationSchemaService(
            LayerSupportRegistry aLayerSupportRegistry,
            FeatureSupportRegistry aFeatureSupportRegistry,
            ApplicationEventPublisher aApplicationEventPublisher)
    {
        return new AnnotationSchemaServiceImpl(aLayerSupportRegistry, aFeatureSupportRegistry,
                aApplicationEventPublisher, entityManager);
    }

    @Bean
    public FeatureSupportRegistry featureSupportRegistry(
            @Lazy @Autowired(required = false) List<FeatureSupport<?>> aFeatureSupports)
    {
        return new FeatureSupportRegistryImpl(aFeatureSupports);
    }

    @Bean
    public BooleanFeatureSupport booleanFeaturesupport()
    {
        return new BooleanFeatureSupport();
    }

    @Bean
    public NumberFeatureSupport numberFeaturesupport()
    {
        return new NumberFeatureSupport();
    }

    @Bean
    public StringFeatureSupport stringFeaturesupport(
            PrimitiveUimaFeatureSupportProperties aProperties,
            AnnotationSchemaService aSchemaService)
    {
        return new StringFeatureSupport(aProperties, aSchemaService);
    }

    @Bean
    public SlotFeatureSupport slotFeaturesupport(AnnotationSchemaService aAnnotationService)
    {
        return new SlotFeatureSupport(aAnnotationService);
    }

    @Bean
    public LayerSupportRegistry layerSupportRegistry(
            @Lazy @Autowired(required = false) List<LayerSupport<?, ?>> aLayerSupports)
    {
        return new LayerSupportRegistryImpl(aLayerSupports);
    }

    @Bean
    public SpanLayerSupport spanLayerSupport(FeatureSupportRegistry aFeatureSupportRegistry,
            ApplicationEventPublisher aEventPublisher,
            LayerBehaviorRegistry aLayerBehaviorsRegistry)
    {
        return new SpanLayerSupport(aFeatureSupportRegistry, aEventPublisher,
                aLayerBehaviorsRegistry);
    }

    @Bean
    public RelationLayerSupport relationLayerSupport(FeatureSupportRegistry aFeatureSupportRegistry,
            ApplicationEventPublisher aEventPublisher,
            LayerBehaviorRegistry aLayerBehaviorsRegistry)
    {
        return new RelationLayerSupport(aFeatureSupportRegistry, aEventPublisher,
                aLayerBehaviorsRegistry);
    }

    @Bean
    public ChainLayerSupport chainLayerSupport(FeatureSupportRegistry aFeatureSupportRegistry,
            ApplicationEventPublisher aEventPublisher,
            LayerBehaviorRegistry aLayerBehaviorsRegistry)
    {
        return new ChainLayerSupport(aFeatureSupportRegistry, aEventPublisher,
                aLayerBehaviorsRegistry);
    }

    @Bean
    public LayerBehaviorRegistry LayerBehaviorRegistry(
            @Lazy @Autowired(required = false) List<LayerBehavior> aLayerSupports)
    {
        return new LayerBehaviorRegistryImpl(aLayerSupports);
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

    @Bean
    public SpanAnchoringModeBehavior spanAnchoringModeBehavior()
    {
        return new SpanAnchoringModeBehavior();
    }

    @Bean
    public SpanCrossSentenceBehavior spanCrossSentenceBehavior()
    {
        return new SpanCrossSentenceBehavior();
    }

    @Bean
    public SpanOverlapBehavior spanOverlapBehavior()
    {
        return new SpanOverlapBehavior();
    }
}

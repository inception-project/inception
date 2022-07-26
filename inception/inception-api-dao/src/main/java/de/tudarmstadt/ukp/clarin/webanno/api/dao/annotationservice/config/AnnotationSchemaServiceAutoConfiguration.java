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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.LayerBehavior;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.config.LinkFeatureSupportPropertiesImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.config.StringFeatureSupportProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.config.StringFeatureSupportPropertiesImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.ChainLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerBehaviorRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerBehaviorRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.RelationLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.SpanLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.AnnotationSchemaServiceEventAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.AnnotationSchemaServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.annotationservice.exporters.AnnotationDocumentExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.annotationservice.exporters.LayerExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.annotationservice.exporters.TagSetExporter;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.feature.bool.BooleanFeatureSupport;
import de.tudarmstadt.ukp.inception.feature.link.LinkFeatureSupport;
import de.tudarmstadt.ukp.inception.feature.multistring.MultiValueStringFeatureSupport;
import de.tudarmstadt.ukp.inception.feature.number.NumberFeatureSupport;
import de.tudarmstadt.ukp.inception.feature.string.StringFeatureSupport;
import de.tudarmstadt.ukp.inception.layer.relation.RelationAttachmentBehavior;
import de.tudarmstadt.ukp.inception.layer.relation.RelationCrossSentenceBehavior;
import de.tudarmstadt.ukp.inception.layer.relation.RelationOverlapBehavior;
import de.tudarmstadt.ukp.inception.layer.span.SpanAnchoringModeBehavior;
import de.tudarmstadt.ukp.inception.layer.span.SpanCrossSentenceBehavior;
import de.tudarmstadt.ukp.inception.layer.span.SpanOverlapBehavior;
import de.tudarmstadt.ukp.inception.rendering.config.AnnotationEditorProperties;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.layer.LayerSupport;
import de.tudarmstadt.ukp.inception.schema.layer.LayerSupportRegistry;

@Configuration
@EnableConfigurationProperties({ //
        StringFeatureSupportPropertiesImpl.class, //
        LinkFeatureSupportPropertiesImpl.class, //
        AnnotationEditorPropertiesImpl.class })
public class AnnotationSchemaServiceAutoConfiguration
{
    private @PersistenceContext EntityManager entityManager;

    @Bean(AnnotationSchemaService.SERVICE_NAME)
    public AnnotationSchemaService annotationSchemaService(
            LayerSupportRegistry aLayerSupportRegistry,
            FeatureSupportRegistry aFeatureSupportRegistry,
            AnnotationEditorProperties aAnnotationEditorProperties,
            ApplicationEventPublisher aApplicationEventPublisher)
    {
        return new AnnotationSchemaServiceImpl(aLayerSupportRegistry, aFeatureSupportRegistry,
                aApplicationEventPublisher, aAnnotationEditorProperties, entityManager);
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
    public StringFeatureSupport stringFeaturesupport(StringFeatureSupportProperties aProperties,
            AnnotationSchemaService aSchemaService)
    {
        return new StringFeatureSupport(aProperties, aSchemaService);
    }

    @Bean
    public MultiValueStringFeatureSupport multiValueStringFeatureSupport(
            StringFeatureSupportProperties aProperties, AnnotationSchemaService aSchemaService)
    {
        return new MultiValueStringFeatureSupport(aProperties, aSchemaService);
    }

    @Bean
    public LinkFeatureSupport linkFeatureSupport(AnnotationSchemaService aAnnotationService)
    {
        return new LinkFeatureSupport(aAnnotationService);
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

    @Bean
    public AnnotationSchemaServiceEventAdapter annotationSchemaServiceEventAdapter(
            AnnotationSchemaService aService)
    {
        return new AnnotationSchemaServiceEventAdapter(aService);
    }

    @Bean
    public AnnotationDocumentExporter annotationDocumentExporter(DocumentService aDocumentService,
            UserDao aUserRepository, DocumentImportExportService aImportExportService,
            RepositoryProperties aRepositoryProperties)
    {
        return new AnnotationDocumentExporter(aDocumentService, aUserRepository,
                aImportExportService, aRepositoryProperties);
    }

    @Bean
    public LayerExporter layerExporter(AnnotationSchemaService aAnnotationService)
    {
        return new LayerExporter(aAnnotationService);
    }

    @Bean
    public TagSetExporter tagSetExporter(AnnotationSchemaService aAnnotationService)
    {
        return new TagSetExporter(aAnnotationService);
    }
}

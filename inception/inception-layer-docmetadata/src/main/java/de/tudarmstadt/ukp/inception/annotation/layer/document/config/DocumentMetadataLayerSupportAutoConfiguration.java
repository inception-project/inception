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
package de.tudarmstadt.ukp.inception.annotation.layer.document.config;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.LayerBehaviorRegistry;
import de.tudarmstadt.ukp.inception.annotation.layer.document.DocumentMetadataLayerSingletonCreatingWatcher;
import de.tudarmstadt.ukp.inception.annotation.layer.document.DocumentMetadataLayerSupportImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.document.curation.DocumentMetadataDiffSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.document.export.DocumentLayerToCsvExporter;
import de.tudarmstadt.ukp.inception.annotation.layer.document.export.DocumentLayerToJsonExporter;
import de.tudarmstadt.ukp.inception.annotation.layer.document.recommender.MetadataSuggestionSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.document.sidebar.DocumentMetadataSidebarFactory;
import de.tudarmstadt.ukp.inception.annotation.layer.document.undo.DocumentMetadataAnnotationActionUndoSupport;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.config.RecommenderServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerType;

/**
 * Provides support for document-level annotations.
 */
@Configuration
@AutoConfigureAfter(RecommenderServiceAutoConfiguration.class)
@EnableConfigurationProperties(DocumentMetadataLayerSupportPropertiesImpl.class)
public class DocumentMetadataLayerSupportAutoConfiguration
{
    @Bean
    @ConditionalOnProperty(prefix = "documentmetadata", name = "enabled", havingValue = "true", matchIfMissing = true)
    public DocumentMetadataSidebarFactory documentMetadataSidebarFactory(
            AnnotationSchemaService aSchemaService)
    {
        return new DocumentMetadataSidebarFactory(aSchemaService);
    }

    /**
     * This bean remains enabled so we don't break existing projects when disabling metadata
     * support. Instead we return {@code true} from {@link LayerType#isInternal()} to prevent the
     * use from creating new layers of this type.
     */
    @SuppressWarnings("javadoc")
    @Bean
    public DocumentMetadataLayerSupport documentMetadataLayerSupport(
            FeatureSupportRegistry aFeatureSupportRegistry,
            ApplicationEventPublisher aEventPublisher,
            DocumentMetadataLayerSupportProperties aProperties,
            LayerBehaviorRegistry aLayerBehaviorRegistry, ConstraintsService aConstraintsService)
    {
        return new DocumentMetadataLayerSupportImpl(aFeatureSupportRegistry, aEventPublisher,
                aProperties, aLayerBehaviorRegistry, aConstraintsService);
    }

    @Bean
    public DocumentMetadataLayerSingletonCreatingWatcher documentMetadataLayerSingletonCreatingWatcher(
            DocumentService aDocumentService, AnnotationSchemaService aAnnotationService,
            LayerSupportRegistry aLayerRegistry)
    {
        return new DocumentMetadataLayerSingletonCreatingWatcher(aDocumentService,
                aAnnotationService, aLayerRegistry);
    }

    @Bean
    @ConditionalOnProperty(prefix = "documentmetadata", name = "enabled", havingValue = "true", matchIfMissing = true)
    public DocumentMetadataAnnotationActionUndoSupport documentMetadataAnnotationActionUndoSupport()
    {
        return new DocumentMetadataAnnotationActionUndoSupport();
    }

    @Bean
    public DocumentLayerToJsonExporter documentLayerToJsonExporter(
            AnnotationSchemaService aSchemaService, DocumentService aDocumentService)
    {
        return new DocumentLayerToJsonExporter(aSchemaService, aDocumentService);
    }

    @Bean
    public DocumentLayerToCsvExporter documentLayerToCsvExporter(
            AnnotationSchemaService aSchemaService, DocumentService aDocumentService)
    {
        return new DocumentLayerToCsvExporter(aSchemaService, aDocumentService);
    }

    @Bean
    public DocumentMetadataDiffSupport documentMetadataDiffSupport()
    {
        return new DocumentMetadataDiffSupport();
    }

    @Bean
    @ConditionalOnProperty(prefix = "documentmetadata", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnBean(RecommendationService.class)
    public MetadataSuggestionSupport metadataSuggestionSupport(
            RecommendationService aRecommendationService,
            LearningRecordService aLearningRecordService,
            ApplicationEventPublisher aApplicationEventPublisher,
            AnnotationSchemaService aSchemaService)
    {
        return new MetadataSuggestionSupport(aRecommendationService, aLearningRecordService,
                aApplicationEventPublisher, aSchemaService);
    }
}

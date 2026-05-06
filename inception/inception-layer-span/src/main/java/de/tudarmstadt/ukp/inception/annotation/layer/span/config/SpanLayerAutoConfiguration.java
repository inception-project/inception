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
package de.tudarmstadt.ukp.inception.annotation.layer.span.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.LayerBehaviorRegistry;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanLayerSupportImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.span.TokenAttachedSpanChangeListener;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.curation.SpanDiffSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.export.SpanLayerToCsvExporter;
import de.tudarmstadt.ukp.inception.annotation.layer.span.export.SpanLayerToJsonExporter;
import de.tudarmstadt.ukp.inception.annotation.layer.span.log.SpanEventAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.span.pivot.SpanCoveredTextExtractorSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.pivot.SpanRangeExtractorSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.recommender.SpanSuggestionSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.undo.SpanAnnotationActionUndoSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.undo.UnitAnnotationActionUndoSupport;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.recommendation.api.LearningRecordService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;

@Configuration
@EnableConfigurationProperties({ SpanRecommenderPropertiesImpl.class })
public class SpanLayerAutoConfiguration
{
    @Bean
    public SpanLayerSupport spanLayerSupport(FeatureSupportRegistry aFeatureSupportRegistry,
            ApplicationEventPublisher aEventPublisher,
            LayerBehaviorRegistry aLayerBehaviorsRegistry, ConstraintsService aConstraintsService)
    {
        return new SpanLayerSupportImpl(aFeatureSupportRegistry, aEventPublisher,
                aLayerBehaviorsRegistry, aConstraintsService);
    }

    @Bean
    public TokenAttachedSpanChangeListener tokenAttachedSpanChangeListener(
            AnnotationSchemaService aSchemaService)
    {
        return new TokenAttachedSpanChangeListener(aSchemaService);
    }

    @Bean
    public SpanAnnotationActionUndoSupport spanAnnotationActionUndoSupport()
    {
        return new SpanAnnotationActionUndoSupport();
    }

    @Bean
    public UnitAnnotationActionUndoSupport unitAnnotationActionUndoSupport()
    {
        return new UnitAnnotationActionUndoSupport();
    }

    @Bean
    public SpanLayerToJsonExporter spanLayerToJsonExporter(AnnotationSchemaService aSchemaService,
            DocumentService aDocumentService)
    {
        return new SpanLayerToJsonExporter(aSchemaService, aDocumentService);
    }

    @Bean
    public SpanLayerToCsvExporter spanLayerToCsvExporter(AnnotationSchemaService aSchemaService,
            DocumentService aDocumentService)
    {
        return new SpanLayerToCsvExporter(aSchemaService, aDocumentService);
    }

    @Bean
    public SpanEventAdapter spanEventAdapter()
    {
        return new SpanEventAdapter();
    }

    @Bean
    public SpanDiffSupport spanDiffSupport()
    {
        return new SpanDiffSupport();
    }

    @ConditionalOnProperty(prefix = "recommender", name = "enabled", havingValue = "true", matchIfMissing = true)
    @Bean
    public SpanSuggestionSupport spanSuggestionSupport(RecommendationService aRecommendationService,
            LearningRecordService aLearningRecordService,
            ApplicationEventPublisher aApplicationEventPublisher,
            AnnotationSchemaService aSchemaService, FeatureSupportRegistry aFeatureSupportRegistry,
            SpanRecommenderProperties aRecommenderProperties)
    {
        return new SpanSuggestionSupport(aRecommendationService, aLearningRecordService,
                aApplicationEventPublisher, aSchemaService, aFeatureSupportRegistry,
                aRecommenderProperties);
    }

    @Bean
    public SpanCoveredTextExtractorSupport spanCoveredTextExtractorSupport()
    {
        return new SpanCoveredTextExtractorSupport();
    }

    @Bean
    public SpanRangeExtractorSupport spanRangeExtractorSupport()
    {
        return new SpanRangeExtractorSupport();
    }
}

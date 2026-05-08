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
package de.tudarmstadt.ukp.inception.annotation.layer.chain.config;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.LayerBehaviorRegistry;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.ChainLayerSupportImpl;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.export.ChainLayerToCsvExporter;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.export.ChainLayerToJsonExporter;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.pivot.ChainCoveredTextExtractorSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.pivot.ChainRangeExtractorSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.undo.ChainAnnotationActionUndoSupport;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;

@Configuration
public class ChainLayerAutoConfiguration
{
    @Bean
    public ChainLayerSupport chainLayerSupport(FeatureSupportRegistry aFeatureSupportRegistry,
            ApplicationEventPublisher aEventPublisher,
            LayerBehaviorRegistry aLayerBehaviorsRegistry, ConstraintsService aConstraintsService)
    {
        return new ChainLayerSupportImpl(aFeatureSupportRegistry, aEventPublisher,
                aLayerBehaviorsRegistry, aConstraintsService);
    }

    @Bean
    public ChainAnnotationActionUndoSupport chainAnnotationActionUndoSupport()
    {
        return new ChainAnnotationActionUndoSupport();
    }

    @Bean
    public ChainLayerToJsonExporter chainLayerToJsonExporter(AnnotationSchemaService aSchemaService,
            DocumentService aDocumentService)
    {
        return new ChainLayerToJsonExporter(aSchemaService, aDocumentService);
    }

    @Bean
    public ChainLayerToCsvExporter chainLayerToCsvExporter(AnnotationSchemaService aSchemaService,
            DocumentService aDocumentService)
    {
        return new ChainLayerToCsvExporter(aSchemaService, aDocumentService);
    }

    @Bean
    public ChainCoveredTextExtractorSupport chainCoveredTextExtractorSupport()
    {
        return new ChainCoveredTextExtractorSupport();
    }

    @Bean
    public ChainRangeExtractorSupport chainRangeExtractorSupport()
    {
        return new ChainRangeExtractorSupport();
    }
}

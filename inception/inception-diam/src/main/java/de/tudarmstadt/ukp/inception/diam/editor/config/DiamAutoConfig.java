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
package de.tudarmstadt.ukp.inception.diam.editor.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.inception.diam.editor.actions.CreateRelationAnnotationHandler;
import de.tudarmstadt.ukp.inception.diam.editor.actions.CreateSpanAnnotationHandler;
import de.tudarmstadt.ukp.inception.diam.editor.actions.DeleteAnnotationHandler;
import de.tudarmstadt.ukp.inception.diam.editor.actions.EditorAjaxRequestHandler;
import de.tudarmstadt.ukp.inception.diam.editor.actions.EditorAjaxRequestHandlerExtensionPoint;
import de.tudarmstadt.ukp.inception.diam.editor.actions.EditorAjaxRequestHandlerExtensionPointImpl;
import de.tudarmstadt.ukp.inception.diam.editor.actions.ExtensionActionHandler;
import de.tudarmstadt.ukp.inception.diam.editor.actions.FillSlotWithExistingAnnotationHandler;
import de.tudarmstadt.ukp.inception.diam.editor.actions.FillSlotWithNewAnnotationHandler;
import de.tudarmstadt.ukp.inception.diam.editor.actions.ImplicitUnarmSlotHandler;
import de.tudarmstadt.ukp.inception.diam.editor.actions.LazyDetailsHandler;
import de.tudarmstadt.ukp.inception.diam.editor.actions.LoadAnnotationsHandler;
import de.tudarmstadt.ukp.inception.diam.editor.actions.SelectAnnotationHandler;
import de.tudarmstadt.ukp.inception.diam.editor.lazydetails.LazyDetailsLookupService;
import de.tudarmstadt.ukp.inception.diam.editor.lazydetails.LazyDetailsLookupServiceImpl;
import de.tudarmstadt.ukp.inception.diam.model.compact.CompactSerializer;
import de.tudarmstadt.ukp.inception.diam.model.compact.CompactSerializerImpl;
import de.tudarmstadt.ukp.inception.diam.model.compactv2.CompactSerializerV2;
import de.tudarmstadt.ukp.inception.diam.model.compactv2.CompactSerializerV2Impl;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.inception.rendering.config.AnnotationEditorProperties;
import de.tudarmstadt.ukp.inception.rendering.pipeline.RenderingPipeline;
import de.tudarmstadt.ukp.inception.rendering.vmodel.serialization.VDocumentSerializerExtensionPoint;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.layer.LayerSupportRegistry;

@Configuration
public class DiamAutoConfig
{
    @Bean
    public EditorAjaxRequestHandlerExtensionPoint editorAjaxRequestHandlerExtensionPoint(
            @Lazy @Autowired(required = false) List<EditorAjaxRequestHandler> aExtensions)
    {
        return new EditorAjaxRequestHandlerExtensionPointImpl(aExtensions);
    }

    @Bean
    public SelectAnnotationHandler selectAnnotationHandler(AnnotationSchemaService aSchemaService)
    {
        return new SelectAnnotationHandler(aSchemaService);
    }

    @Bean
    public ExtensionActionHandler extensionActionHandler(
            AnnotationEditorExtensionRegistry aExtensionRegistry)
    {
        return new ExtensionActionHandler(aExtensionRegistry);
    }

    @Bean
    public CreateSpanAnnotationHandler createSpanAnnotationHandler()
    {
        return new CreateSpanAnnotationHandler();
    }

    @Bean
    public CreateRelationAnnotationHandler createRelationAnnotationHandler()
    {
        return new CreateRelationAnnotationHandler();
    }

    @Bean
    public DeleteAnnotationHandler deleteAnnotationHandler(
            AnnotationSchemaService aAnnotationService)
    {
        return new DeleteAnnotationHandler(aAnnotationService);
    }

    @Bean
    public FillSlotWithExistingAnnotationHandler fillSlotWithExistingAnnotationHandler()
    {
        return new FillSlotWithExistingAnnotationHandler();
    }

    @Bean
    public FillSlotWithNewAnnotationHandler fillSlotWithNewAnnotationHandler()
    {
        return new FillSlotWithNewAnnotationHandler();
    }

    @Bean
    public LazyDetailsHandler lazyDetailHandler(LazyDetailsLookupService aLazyDetailsLookupService)
    {
        return new LazyDetailsHandler(aLazyDetailsLookupService);
    }

    @Bean
    public ImplicitUnarmSlotHandler implicitUnarmSlotHandler()
    {
        return new ImplicitUnarmSlotHandler();
    }

    @Bean
    public LazyDetailsLookupService lazyDetailsLookupService(
            AnnotationSchemaService aAnnotationService,
            AnnotationEditorExtensionRegistry aExtensionRegistry,
            LayerSupportRegistry aLayerSupportRegistry,
            FeatureSupportRegistry aFeatureSupportRegistry)
    {
        return new LazyDetailsLookupServiceImpl(aAnnotationService, aExtensionRegistry,
                aLayerSupportRegistry, aFeatureSupportRegistry);
    }

    @Bean
    public LoadAnnotationsHandler loadAnnotationsHandler(RenderingPipeline aRenderingPipeline,
            VDocumentSerializerExtensionPoint aVDocumentSerializerExtensionPoint)
    {
        return new LoadAnnotationsHandler(aRenderingPipeline, aVDocumentSerializerExtensionPoint);
    }

    @Bean
    public CompactSerializer compactSerializer(AnnotationEditorProperties aProperties)
    {
        return new CompactSerializerImpl(aProperties);
    }

    @Bean
    public CompactSerializerV2 compactSerializerV2(AnnotationEditorProperties aProperties)
    {
        return new CompactSerializerV2Impl(aProperties);
    }
}

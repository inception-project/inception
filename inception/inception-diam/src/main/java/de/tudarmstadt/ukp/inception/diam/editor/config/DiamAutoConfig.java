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

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.annotation.menu.ContextMenuItemRegistry;
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
import de.tudarmstadt.ukp.inception.diam.editor.actions.LinkToContextMenuItem;
import de.tudarmstadt.ukp.inception.diam.editor.actions.LoadAnnotationsHandler;
import de.tudarmstadt.ukp.inception.diam.editor.actions.LoadPreferences;
import de.tudarmstadt.ukp.inception.diam.editor.actions.MoveSpanAnnotationHandler;
import de.tudarmstadt.ukp.inception.diam.editor.actions.SavePreferences;
import de.tudarmstadt.ukp.inception.diam.editor.actions.ScrollToHandler;
import de.tudarmstadt.ukp.inception.diam.editor.actions.SelectAnnotationHandler;
import de.tudarmstadt.ukp.inception.diam.editor.actions.ShowContextMenuHandler;
import de.tudarmstadt.ukp.inception.diam.editor.lazydetails.LazyDetailsLookupService;
import de.tudarmstadt.ukp.inception.diam.editor.lazydetails.LazyDetailsLookupServiceImpl;
import de.tudarmstadt.ukp.inception.diam.model.compact.CompactSerializer;
import de.tudarmstadt.ukp.inception.diam.model.compact.CompactSerializerImpl;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.inception.preferences.ClientSiderUserPreferencesProviderRegistry;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.rendering.pipeline.RenderingPipeline;
import de.tudarmstadt.ukp.inception.rendering.vmodel.serialization.VDocumentSerializerExtensionPoint;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.config.AnnotationSchemaProperties;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;

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
    public ScrollToHandler scrollToHandler(DocumentService aDocumentService)
    {
        return new ScrollToHandler(aDocumentService);
    }

    @Bean
    public ExtensionActionHandler extensionActionHandler(
            AnnotationEditorExtensionRegistry aExtensionRegistry)
    {
        return new ExtensionActionHandler(aExtensionRegistry);
    }

    @Bean
    public CreateSpanAnnotationHandler createSpanAnnotationHandler(
            AnnotationSchemaService aSchemaService)
    {
        return new CreateSpanAnnotationHandler(aSchemaService);
    }

    @Bean
    public MoveSpanAnnotationHandler moveSpanAnnotationHandler(
            AnnotationSchemaService aAnnotationService)
    {
        return new MoveSpanAnnotationHandler(aAnnotationService);
    }

    @Bean
    public CreateRelationAnnotationHandler createRelationAnnotationHandler(
            AnnotationSchemaService aSchemaService,
            AnnotationSchemaProperties aAnnotationSchemaProperties)
    {
        return new CreateRelationAnnotationHandler(aSchemaService, aAnnotationSchemaProperties);
    }

    @Bean
    public ShowContextMenuHandler showContextMenuHandler(
            ContextMenuItemRegistry aContextMenuItemRegistry)
    {
        return new ShowContextMenuHandler(aContextMenuItemRegistry);
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
            VDocumentSerializerExtensionPoint aVDocumentSerializerExtensionPoint,
            UserDao aUserService)
    {
        return new LoadAnnotationsHandler(aRenderingPipeline, aVDocumentSerializerExtensionPoint,
                aUserService);
    }

    @Bean
    public CompactSerializer compactSerializer(AnnotationSchemaProperties aProperties)
    {
        return new CompactSerializerImpl(aProperties);
    }

    @Bean
    public LoadPreferences loadPreferences(UserDao aUserService,
            PreferencesService aPreferencesService)
    {
        return new LoadPreferences(aUserService, aPreferencesService);
    }

    @Bean
    public SavePreferences savePreferences(UserDao aUserService,
            PreferencesService aPreferencesService,
            ClientSiderUserPreferencesProviderRegistry aClientSiderUserPreferencesProviderRegistry)
    {
        return new SavePreferences(aUserService, aPreferencesService,
                aClientSiderUserPreferencesProviderRegistry);
    }

    @Bean
    public LinkToContextMenuItem linkToContextMenuItem(AnnotationSchemaService aSchemaService,
            CreateRelationAnnotationHandler aCreateRelationAnnotationHandler)
    {
        return new LinkToContextMenuItem(aSchemaService, aCreateRelationAnnotationHandler);
    }
}

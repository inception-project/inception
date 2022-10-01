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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences.AnnotationEditorDefaultPreferencesProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences.AnnotationEditorDefaultPreferencesPropertiesImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences.UserPreferencesService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences.UserPreferencesServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.ColorRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.FocusMarkerRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.LabelRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.PreRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.PreRendererImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.RenderNotificationRenderStep;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.rendering.coloring.ColoringService;
import de.tudarmstadt.ukp.inception.rendering.config.AnnotationEditorProperties;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.layer.LayerSupportRegistry;

@Configuration
@EnableConfigurationProperties(AnnotationEditorDefaultPreferencesPropertiesImpl.class)
public class AnnotationAutoConfiguration
{
    @Bean
    public ColoringService coloringService(AnnotationSchemaService aSchemaService)
    {
        return new ColoringServiceImpl(aSchemaService);
    }

    @Bean
    public PreRenderer preRenderer(LayerSupportRegistry aLayerSupportRegistry,
            AnnotationSchemaService aAnnotationService)
    {
        return new PreRendererImpl(aLayerSupportRegistry, aAnnotationService);
    }

    @Bean
    public LabelRenderer labelRenderer()
    {
        return new LabelRenderer();
    }

    @Bean
    public ColorRenderer colorRenderer(AnnotationSchemaService aSchemaService,
            ColoringService aColoringService,
            @Autowired(required = false) UserPreferencesService aUserPreferencesService)
    {
        return new ColorRenderer(aSchemaService, aColoringService, aUserPreferencesService);
    }

    @Bean
    public RenderNotificationRenderStep renderNotificationRenderStep()
    {
        return new RenderNotificationRenderStep();
    }

    @Bean
    public FocusMarkerRenderer focusMarkerRenderer()
    {
        return new FocusMarkerRenderer();
    }

    @Bean
    public UserPreferencesService userPreferencesService(
            AnnotationEditorDefaultPreferencesProperties aDefaultPreferences,
            AnnotationSchemaService aAnnotationService, RepositoryProperties aRepositoryProperties,
            ColoringService aColoringService,
            AnnotationEditorProperties aAnnotationEditorProperties,
            PreferencesService aPreferencesService)
    {
        return new UserPreferencesServiceImpl(aDefaultPreferences, aAnnotationService,
                aRepositoryProperties, aColoringService, aAnnotationEditorProperties,
                aPreferencesService);
    }
}

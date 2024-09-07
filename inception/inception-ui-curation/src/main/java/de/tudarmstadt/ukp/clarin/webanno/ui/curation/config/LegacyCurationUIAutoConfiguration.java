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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.PreRenderer;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.actionbar.CurationDocumentNavigatorActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.actionbar.CurationUndoActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.actionbar.CurationWorkflowActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.render.CurationRenderer;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.render.CurationRendererImpl;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.page.LegacyCurationPageMenuItem;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.rendering.coloring.ColoringService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.config.AnnotationSchemaProperties;
import jakarta.servlet.ServletContext;

@ConditionalOnWebApplication
@Configuration
public class LegacyCurationUIAutoConfiguration
{
    @Bean
    public LegacyCurationPageMenuItem legacyCurationPageMenuItem(UserDao aUserRepo,
            ProjectService aProjectService, ServletContext aServletContext,
            PreferencesService aPreferencesService)
    {
        return new LegacyCurationPageMenuItem(aUserRepo, aProjectService, aServletContext,
                aPreferencesService);
    }

    @Bean
    public CurationUndoActionBarExtension curationUndoActionBarExtension()
    {
        return new CurationUndoActionBarExtension();
    }

    @Bean
    public CurationDocumentNavigatorActionBarExtension curationDocumentNavigatorActionBarExtension()
    {
        return new CurationDocumentNavigatorActionBarExtension();
    }

    @Bean
    public CurationWorkflowActionBarExtension curationWorkflowActionBarExtension()
    {
        return new CurationWorkflowActionBarExtension();
    }

    @Bean
    public CurationRenderer curationRenderer(PreRenderer aPreRenderer,
            AnnotationSchemaService aSchemaService, ColoringService aColoringService,
            AnnotationSchemaProperties aAnnotationEditorProperties, UserDao aUserService)
    {
        return new CurationRendererImpl(aPreRenderer, aSchemaService, aColoringService,
                aAnnotationEditorProperties, aUserService);
    }
}

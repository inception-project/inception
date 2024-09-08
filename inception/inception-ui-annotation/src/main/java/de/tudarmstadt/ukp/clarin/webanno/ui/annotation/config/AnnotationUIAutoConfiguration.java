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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageMenuItem;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.closesession.CloseSessionActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.AnnotationUndoActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions.ChainAnnotationActionUndoSupport;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions.FeatureValueActionUndoSupport;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions.RelationAnnotationActionUndoSupport;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions.SpanAnnotationActionUndoSupport;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions.UndoableActionSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions.UndoableAnnotationActionSupport;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.layer.LayerVisibilitySidebarFactory;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import jakarta.servlet.ServletContext;

@ConditionalOnWebApplication
@Configuration
public class AnnotationUIAutoConfiguration
{
    @Bean
    public AnnotationPageMenuItem annotationPageMenuItem(UserDao aUserRepo,
            ProjectService aProjectService, ServletContext aServletContext)
    {
        return new AnnotationPageMenuItem(aUserRepo, aProjectService, aServletContext);
    }

    @Bean
    public AnnotationUndoActionBarExtension annotationUndoActionBarExtension()
    {
        return new AnnotationUndoActionBarExtension();
    }

    @Bean
    public UndoableActionSupportRegistryImpl undoableActionSupportRegistry(
            @Lazy @Autowired(required = false) List<UndoableAnnotationActionSupport> aExtensions)
    {
        return new UndoableActionSupportRegistryImpl(aExtensions);
    }

    @Bean
    public SpanAnnotationActionUndoSupport spanAnnotationActionUndoSupport()
    {
        return new SpanAnnotationActionUndoSupport();
    }

    @Bean
    public RelationAnnotationActionUndoSupport relationAnnotationActionUndoSupport()
    {
        return new RelationAnnotationActionUndoSupport();
    }

    @Bean
    public ChainAnnotationActionUndoSupport chainAnnotationActionUndoSupport()
    {
        return new ChainAnnotationActionUndoSupport();
    }

    @Bean
    public FeatureValueActionUndoSupport featureValueActionUndoSupport()
    {
        return new FeatureValueActionUndoSupport();
    }

    @Bean
    public CloseSessionActionBarExtension closeSessionActionBarExtension()
    {
        return new CloseSessionActionBarExtension();
    }

    @Bean
    public LayerVisibilitySidebarFactory layerVisibilitySidebarFactory()
    {
        return new LayerVisibilitySidebarFactory();
    }
}

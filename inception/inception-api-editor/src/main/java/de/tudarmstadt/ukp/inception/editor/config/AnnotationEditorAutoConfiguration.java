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
package de.tudarmstadt.ukp.inception.editor.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.inception.editor.AnnotationEditorExtension;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorExtensionRegistryImpl;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorFactory;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorRegistry;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorRegistryImpl;
import de.tudarmstadt.ukp.inception.editor.view.DocumentViewExtensionPoint;
import de.tudarmstadt.ukp.inception.editor.view.DocumentViewExtensionPointImpl;
import de.tudarmstadt.ukp.inception.editor.view.DocumentViewFactory;

@Configuration
public class AnnotationEditorAutoConfiguration
{
    @Bean
    public AnnotationEditorExtensionRegistry annotationEditorExtensionRegistry(
            @Lazy @Autowired(required = false) List<AnnotationEditorExtension> aExtensions)
    {
        return new AnnotationEditorExtensionRegistryImpl(aExtensions);
    }

    @Bean
    public AnnotationEditorRegistry annotationEditorRegistry(
            @Lazy @Autowired(required = false) List<AnnotationEditorFactory> aExtensions)
    {
        return new AnnotationEditorRegistryImpl(aExtensions);
    }

    @Bean
    public DocumentViewExtensionPoint documentViewExtensionPoint(
            @Lazy @Autowired(required = false) List<DocumentViewFactory> aExtensions)
    {
        return new DocumentViewExtensionPointImpl(aExtensions);
    }
}

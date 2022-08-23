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
package de.tudarmstadt.ukp.inception.pdfeditor.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.inception.pdfeditor.PdfAnnotationEditorFactory;
import de.tudarmstadt.ukp.inception.pdfeditor.PdfFormatSupport;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.PdfDocumentIFrameViewFactory;

/**
 * Provides support for an PDF-oriented annotation editor.
 * 
 * @deprecated Superseded by the new PDF editor
 */
@Deprecated
@Configuration
public class PdfAnnotationEditorSupportAutoConfiguration
{
    @Bean
    public PdfAnnotationEditorFactory pdfAnnotationEditorFactory()
    {
        return new PdfAnnotationEditorFactory();
    }

    @ConditionalOnProperty(prefix = "ui.pdf-legacy", name = "enabled", havingValue = "true", matchIfMissing = false)
    @Bean
    public PdfFormatSupport pdfFormatSupport()
    {
        return new PdfFormatSupport();
    }

    @Bean
    public PdfDocumentIFrameViewFactory pdfDocumentIFrameViewFactory()
    {
        return new PdfDocumentIFrameViewFactory();
    }
}

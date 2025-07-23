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
package de.tudarmstadt.ukp.inception.pdfeditor2.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.inception.pdfeditor2.PdfAnnotationEditorFactory;
import de.tudarmstadt.ukp.inception.pdfeditor2.format.PdfFormatSupport;
import de.tudarmstadt.ukp.inception.pdfeditor2.format.PdfJsonCasFormatSupport;
import de.tudarmstadt.ukp.inception.pdfeditor2.format.PdfXmiCasFormatSupport;
import de.tudarmstadt.ukp.inception.pdfeditor2.view.PdfDocumentIFrameViewFactory;

/**
 * Provides support for an PDF-oriented annotation editor.
 */
@Configuration
@EnableConfigurationProperties(PdfFormatPropertiesImpl.class)
public class PdfAnnotationEditor2SupportAutoConfiguration
{
    @ConditionalOnProperty(prefix = "ui.pdf", name = "enabled", havingValue = "true", matchIfMissing = true)
    @Bean
    public PdfAnnotationEditorFactory pdfAnnotationEditor2Factory()
    {
        return new PdfAnnotationEditorFactory();
    }

    @ConditionalOnProperty(prefix = "ui.pdf", name = "enabled", havingValue = "true", matchIfMissing = true)
    @Bean
    public PdfDocumentIFrameViewFactory pdfDocument2IFrameViewFactory()
    {
        return new PdfDocumentIFrameViewFactory();
    }

    @ConditionalOnProperty(prefix = "format.pdf", name = "enabled", havingValue = "true", matchIfMissing = true)
    @Bean
    public PdfFormatSupport pdfFormat2Support(PdfFormatProperties aProperties)
    {
        return new PdfFormatSupport(aProperties);
    }

    @ConditionalOnProperty(prefix = "format.pdf-json-cas", name = "enabled", havingValue = "true", matchIfMissing = false)
    @Bean
    public PdfJsonCasFormatSupport pdfJsonCasFormatSupport()
    {
        return new PdfJsonCasFormatSupport();
    }

    @ConditionalOnProperty(prefix = "format.pdf-xmi-cas", name = "enabled", havingValue = "true", matchIfMissing = false)
    @Bean
    public PdfXmiCasFormatSupport pdfXmiCasFormatSupport()
    {
        return new PdfXmiCasFormatSupport();
    }
}

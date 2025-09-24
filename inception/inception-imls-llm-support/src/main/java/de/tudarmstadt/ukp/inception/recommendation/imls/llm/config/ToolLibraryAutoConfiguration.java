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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.inception.recommendation.imls.llm.AnnotationTaskCodecExtensionPoint;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.AnnotationTaskCodecExtensionPointImpl;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolLibrary;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolLibraryExtensionPoint;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolLibraryExtensionPointImpl;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response.AnnotationTaskCodec;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response.LabellingAnnotationTaskCodec;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response.SpanJsonAnnotationTaskCodec;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response.SpanJsonSchemaAnnotationTaskCodec;

@Configuration
public class ToolLibraryAutoConfiguration
{
    @Bean
    public ToolLibraryExtensionPoint toolLibraryExtensionPoint(
            @Lazy @Autowired(required = false) List<ToolLibrary> aExtensions)
    {
        return new ToolLibraryExtensionPointImpl(aExtensions);
    }

    @Bean
    public AnnotationTaskCodecExtensionPoint ResponseExtractorExtensionPointImpl(
            @Lazy @Autowired(required = false) List<AnnotationTaskCodec> aExtensions)
    {
        return new AnnotationTaskCodecExtensionPointImpl(aExtensions);
    }

    @Bean
    public SpanJsonAnnotationTaskCodec mentionsFromJsonExtractor()
    {
        return new SpanJsonAnnotationTaskCodec();
    }

    @Bean
    public SpanJsonSchemaAnnotationTaskCodec mentionsFromStructuredOutputExtractor()
    {
        return new SpanJsonSchemaAnnotationTaskCodec();
    }

    @Bean
    public LabellingAnnotationTaskCodec responseAsLabelExtractor()
    {
        return new LabellingAnnotationTaskCodec();
    }
}

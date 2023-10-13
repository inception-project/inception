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
package de.tudarmstadt.ukp.inception.rendering.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.inception.rendering.pipeline.RenderStep;
import de.tudarmstadt.ukp.inception.rendering.pipeline.RenderStepExtensionPoint;
import de.tudarmstadt.ukp.inception.rendering.pipeline.RenderStepExtensionPointImpl;
import de.tudarmstadt.ukp.inception.rendering.pipeline.RenderingPipeline;
import de.tudarmstadt.ukp.inception.rendering.pipeline.RenderingPipelineImpl;
import de.tudarmstadt.ukp.inception.rendering.vmodel.serialization.VDocumentSerializer;
import de.tudarmstadt.ukp.inception.rendering.vmodel.serialization.VDocumentSerializerExtensionPoint;
import de.tudarmstadt.ukp.inception.rendering.vmodel.serialization.VDocumentSerializerExtensionPointImpl;

@Configuration
public class RenderingAutoConfig
{
    @Bean
    public VDocumentSerializerExtensionPoint vDocumentSerializerExtensionPoint(
            @Lazy @Autowired(required = false) List<VDocumentSerializer<?>> aExtensions)
    {
        return new VDocumentSerializerExtensionPointImpl(aExtensions);
    }

    @Bean
    public RenderingPipeline renderingPipeline(RenderStepExtensionPoint aRenderStepExtensionPoint)
    {
        return new RenderingPipelineImpl(aRenderStepExtensionPoint);
    }

    @Bean
    public RenderStepExtensionPoint renderStepExtensionPoint(
            @Lazy @Autowired(required = false) List<RenderStep> aExtensions)
    {
        return new RenderStepExtensionPointImpl(aExtensions);
    }
}

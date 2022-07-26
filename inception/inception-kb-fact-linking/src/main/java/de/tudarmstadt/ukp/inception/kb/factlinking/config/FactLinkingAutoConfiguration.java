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
package de.tudarmstadt.ukp.inception.kb.factlinking.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.kb.factlinking.feature.FactLinkingService;
import de.tudarmstadt.ukp.inception.kb.factlinking.feature.FactLinkingServiceImpl;
import de.tudarmstadt.ukp.inception.kb.factlinking.feature.PropertyFeatureSupport;
import de.tudarmstadt.ukp.inception.kb.factlinking.feature.SubjectObjectFeatureSupport;
import de.tudarmstadt.ukp.inception.kb.factlinking.initializers.FactLayerInitializer;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

@Deprecated
@Configuration
@AutoConfigureAfter(KnowledgeBaseServiceAutoConfiguration.class)
@ConditionalOnBean(KnowledgeBaseService.class)
@ConditionalOnProperty(prefix = "knowledge-base.fact-linking", //
        name = "enabled", havingValue = "true", matchIfMissing = false)
public class FactLinkingAutoConfiguration
{
    @Deprecated
    @Bean
    @Autowired
    public PropertyFeatureSupport propertyFeatureSupport(KnowledgeBaseService aKbService)
    {
        return new PropertyFeatureSupport(aKbService);
    }

    @Deprecated
    @Bean
    public SubjectObjectFeatureSupport subjectObjectFeatureSupport()
    {
        return new SubjectObjectFeatureSupport();
    }

    @Deprecated
    @Bean
    public FactLinkingService factLinkingService()
    {
        return new FactLinkingServiceImpl();
    }

    @Deprecated
    @Bean
    @Autowired
    public FactLayerInitializer factLayerInitializer(
            AnnotationSchemaService aAnnotationSchemaService)
    {
        return new FactLayerInitializer(aAnnotationSchemaService);
    }
}

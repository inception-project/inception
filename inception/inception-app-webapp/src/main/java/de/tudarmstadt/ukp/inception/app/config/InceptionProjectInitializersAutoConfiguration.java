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
package de.tudarmstadt.ukp.inception.app.config;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.inception.initializers.BasicDocumentLabelLayerInitializer;
import de.tudarmstadt.ukp.inception.initializers.BasicDocumentLabelTagSetInitializer;
import de.tudarmstadt.ukp.inception.initializers.BasicDocumentLabelingProjectInitializer;
import de.tudarmstadt.ukp.inception.initializers.EntityLinkingProjectInitializer;
import de.tudarmstadt.ukp.inception.initializers.NamedEntityIdentifierStringRecommenderInitializer;
import de.tudarmstadt.ukp.inception.initializers.StandardProjectInitializer;
import de.tudarmstadt.ukp.inception.initializers.WikiDataKnowledgeBaseInitializer;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseProperties;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.config.RecommenderServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.config.StringMatchingRecommenderAutoConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.StringMatchingRecommenderFactory;
import de.tudarmstadt.ukp.inception.ui.core.docanno.config.DocumentMetadataLayerSupportAutoConfiguration;
import de.tudarmstadt.ukp.inception.ui.core.docanno.layer.DocumentMetadataLayerSupport;
import de.tudarmstadt.ukp.inception.ui.core.docanno.sidebar.DocumentMetadataSidebarFactory;
import de.tudarmstadt.ukp.inception.workload.matrix.MatrixWorkloadExtension;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

@AutoConfigureAfter({ //
        KnowledgeBaseServiceAutoConfiguration.class, //
        RecommenderServiceAutoConfiguration.class, //
        StringMatchingRecommenderAutoConfiguration.class, //
        DocumentMetadataLayerSupportAutoConfiguration.class })
// @AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Configuration
public class InceptionProjectInitializersAutoConfiguration
{
    @ConditionalOnBean(RecommendationService.class)
    @Bean
    public NamedEntityIdentifierStringRecommenderInitializer namedEntityIdentifierStringRecommenderInitializer(
            RecommendationService aRecommenderService, AnnotationSchemaService aAnnotationService,
            StringMatchingRecommenderFactory aRecommenderFactory)
    {
        return new NamedEntityIdentifierStringRecommenderInitializer(aRecommenderService,
                aAnnotationService, aRecommenderFactory);
    }

    @Bean
    public StandardProjectInitializer standardProjectInitializer(
            @Lazy ProjectService aProjectService)
    {
        return new StandardProjectInitializer(aProjectService);
    }

    @ConditionalOnBean(KnowledgeBaseService.class)
    @Bean
    public WikiDataKnowledgeBaseInitializer wikiDataKnowledgeBaseInitializer(
            KnowledgeBaseService aKbService, KnowledgeBaseProperties aKbProperties)
    {
        return new WikiDataKnowledgeBaseInitializer(aKbService, aKbProperties);
    }

    @ConditionalOnBean(WikiDataKnowledgeBaseInitializer.class)
    @Bean
    public EntityLinkingProjectInitializer entityLinkingProjectInitializer(
            ApplicationContext aContext, AnnotationSchemaService aAnnotationService)
    {
        return new EntityLinkingProjectInitializer(aContext, aAnnotationService);
    }

    @ConditionalOnBean(DocumentMetadataLayerSupport.class)
    @Bean
    public BasicDocumentLabelLayerInitializer basicDocumentTagLayerInitializer(
            AnnotationSchemaService aAnnotationSchemaService,
            DocumentMetadataLayerSupport aDocLayerSupport)
    {
        return new BasicDocumentLabelLayerInitializer(aAnnotationSchemaService, aDocLayerSupport);
    }

    @ConditionalOnBean(DocumentMetadataLayerSupport.class)
    @Bean
    BasicDocumentLabelingProjectInitializer basicDocumentLabelingProjectInitializer(
            PreferencesService aPreferencesService, DocumentMetadataSidebarFactory aDocMetaSidebar,
            WorkloadManagementService aWorkloadManagementService,
            MatrixWorkloadExtension aMatrixWorkloadExtension)
    {
        return new BasicDocumentLabelingProjectInitializer(aPreferencesService, aDocMetaSidebar,
                aWorkloadManagementService, aMatrixWorkloadExtension);
    }

    @ConditionalOnBean(DocumentMetadataLayerSupport.class)
    @Bean
    BasicDocumentLabelTagSetInitializer basicDocumentLabelTagSetInitializer(
            AnnotationSchemaService aAnnotationSchemaService)
    {
        return new BasicDocumentLabelTagSetInitializer(aAnnotationSchemaService);
    }
}

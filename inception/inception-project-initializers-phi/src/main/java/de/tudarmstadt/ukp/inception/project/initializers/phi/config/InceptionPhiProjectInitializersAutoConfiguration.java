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
package de.tudarmstadt.ukp.inception.project.initializers.phi.config;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.project.initializers.phi.PhiProjectInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.phi.PhiSpanLayerInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.phi.PhiSpanOpenNlpNerRecommenderInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.phi.PhiSpanStringMatchingRecommenderInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.phi.PhiSpanTagSetInitializer;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.config.RecommenderServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.config.OpenNlpRecommenderAutoConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.ner.OpenNlpNerRecommenderFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.config.StringMatchingRecommenderAutoConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.StringMatchingRecommenderFactory;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

@AutoConfigureAfter({ //
        RecommenderServiceAutoConfiguration.class, //
        OpenNlpRecommenderAutoConfiguration.class, //
        StringMatchingRecommenderAutoConfiguration.class })
@Configuration
public class InceptionPhiProjectInitializersAutoConfiguration
{
    @Bean
    public PhiProjectInitializer phiProjectInitializer(ApplicationContext aContext,
            DocumentService aDocumentService, UserDao aUserService,
            PreferencesService aPreferencesService)
    {
        return new PhiProjectInitializer(aContext, aDocumentService, aUserService,
                aPreferencesService);
    }

    @Bean
    public PhiSpanLayerInitializer phiSpanLayerInitializer(
            AnnotationSchemaService aAnnotationSchemaService)
    {
        return new PhiSpanLayerInitializer(aAnnotationSchemaService);
    }

    @ConditionalOnBean({ RecommendationService.class, StringMatchingRecommenderFactory.class })
    @Bean
    public PhiSpanStringMatchingRecommenderInitializer phiSpanStringMatchingRecommenderInitializer(
            RecommendationService aRecommenderService, AnnotationSchemaService aAnnotationService,
            StringMatchingRecommenderFactory aRecommenderFactory)
    {
        return new PhiSpanStringMatchingRecommenderInitializer(aRecommenderService,
                aAnnotationService, aRecommenderFactory);
    }

    @ConditionalOnBean({ RecommendationService.class, OpenNlpNerRecommenderFactory.class })
    @Bean
    public PhiSpanOpenNlpNerRecommenderInitializer phiSpanOpenNlpNerRecommenderInitializer(
            RecommendationService aRecommenderService, AnnotationSchemaService aAnnotationService,
            OpenNlpNerRecommenderFactory aRecommenderFactory)
    {
        return new PhiSpanOpenNlpNerRecommenderInitializer(aRecommenderService, aAnnotationService,
                aRecommenderFactory);
    }

    @Bean
    public PhiSpanTagSetInitializer phiSpanTagSetInitializer(
            AnnotationSchemaService aAnnotationSchemaService)
    {
        return new PhiSpanTagSetInitializer(aAnnotationSchemaService);
    }
}

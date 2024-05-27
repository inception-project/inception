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
package de.tudarmstadt.ukp.inception.project.initializers.sentencelabeling.config;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.inception.project.initializers.sentencelabeling.SentenceLabelLayerInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.sentencelabeling.SentenceLabelRecommenderInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.sentencelabeling.SentenceLabelTagSetInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.sentencelabeling.SentenceLabelingProjectInitializer;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.config.RecommenderServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.doccat.OpenNlpDoccatRecommenderFactory;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

@AutoConfigureAfter({ //
        RecommenderServiceAutoConfiguration.class })
@Configuration
public class InceptionSentenceLabelingProjectInitializersAutoConfiguration
{
    @Bean
    public SentenceLabelLayerInitializer sentenceTagLayerInitializer(
            AnnotationSchemaService aAnnotationSchemaService)
    {
        return new SentenceLabelLayerInitializer(aAnnotationSchemaService);
    }

    @Bean
    public SentenceLabelingProjectInitializer sentenceLabelingProjectInitializer()
    {
        return new SentenceLabelingProjectInitializer();
    }

    @Bean
    public SentenceLabelTagSetInitializer sentenceLabelTagSetInitializer(
            AnnotationSchemaService aAnnotationSchemaService)
    {
        return new SentenceLabelTagSetInitializer(aAnnotationSchemaService);
    }

    @Bean
    public SentenceLabelRecommenderInitializer sentenceLabelRecommenderInitializer(
            RecommendationService aRecommenderService, AnnotationSchemaService aAnnotationService,
            OpenNlpDoccatRecommenderFactory aRecommenderFactory)
    {
        return new SentenceLabelRecommenderInitializer(aRecommenderService, aAnnotationService,
                aRecommenderFactory);
    }
}

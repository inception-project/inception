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
package de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.doccat.OpenNlpDoccatMetadataRecommenderFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.doccat.OpenNlpDoccatRecommenderFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.ner.OpenNlpNerRecommenderFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.opennlp.pos.OpenNlpPosRecommenderFactory;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

@Configuration
public class OpenNlpRecommenderAutoConfiguration
{
    @Bean
    public OpenNlpDoccatRecommenderFactory openNlpDoccatRecommenderFactory()
    {
        return new OpenNlpDoccatRecommenderFactory();
    }

    @Bean
    @ConditionalOnProperty(prefix = "documentmetadata", name = "enabled", havingValue = "true", matchIfMissing = true)
    public OpenNlpDoccatMetadataRecommenderFactory openNlpDoccatMetadataRecommenderFactory(
            AnnotationSchemaService aSchemaService)
    {
        return new OpenNlpDoccatMetadataRecommenderFactory();
    }

    @Bean
    public OpenNlpNerRecommenderFactory openNlpNerRecommenderFactory()
    {
        return new OpenNlpNerRecommenderFactory();
    }

    @Bean
    public OpenNlpPosRecommenderFactory openNlpPosRecommenderFactory()
    {
        return new OpenNlpPosRecommenderFactory();
    }
}

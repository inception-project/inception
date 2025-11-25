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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.config;

import java.net.http.HttpClient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.inception.recommendation.imls.llm.AnnotationTaskCodecExtensionPoint;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.OllamaRecommenderFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaClientImpl;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaMetrics;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaMetricsImpl;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

@Configuration
public class OllamaRecommenderAutoConfiguration
{
    // We need this for the assistant atm, so it's not covered by the feature flag
    @Bean
    public OllamaClient ollamaClient(OllamaMetrics aMetrics)
    {
        return new OllamaClientImpl(HttpClient.newBuilder().build(), aMetrics);
    }

    @Bean
    public OllamaMetrics ollamaMetrics()
    {
        return new OllamaMetricsImpl();
    }

    @ConditionalOnProperty(prefix = "recommender.ollama", name = "enabled", havingValue = "true", //
            matchIfMissing = false)
    @Bean
    public OllamaRecommenderFactory ollamaRecommenderFactory(OllamaClient aClient,
            AnnotationSchemaService aSchemaService,
            AnnotationTaskCodecExtensionPoint aResponseExtractorExtensionPoint)
    {
        return new OllamaRecommenderFactory(aClient, aSchemaService,
                aResponseExtractorExtensionPoint);
    }
}

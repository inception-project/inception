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
package de.tudarmstadt.ukp.inception.recommendation.imls.hf.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.inception.recommendation.imls.hf.HfRecommenderFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.hf.client.HfHubClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.hf.client.HfHubClientImpl;
import de.tudarmstadt.ukp.inception.recommendation.imls.hf.client.HfInferenceClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.hf.client.HfInferenceClientImpl;

@Configuration
@ConditionalOnProperty(prefix = "recommender.hf", name = "enabled", havingValue = "true", matchIfMissing = false)
public class HfRecommenderAutoConfiguration
{
    @Bean
    public HfRecommenderFactory hfRecommenderFactory(HfInferenceClient aHfInferenceClient)
    {
        return new HfRecommenderFactory(aHfInferenceClient);
    }

    @Bean
    public HfHubClient hfHubClient()
    {
        return new HfHubClientImpl();
    }

    @Bean
    public HfInferenceClient hfInferenceClient()
    {
        return new HfInferenceClientImpl();
    }
}

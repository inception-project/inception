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
package de.tudarmstadt.ukp.inception.recommendation.imls.chatgpt.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.inception.recommendation.imls.chatgpt.ChatGptRecommenderFactory;
import de.tudarmstadt.ukp.inception.recommendation.imls.chatgpt.client.ChatGptClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.chatgpt.client.ChatGptClientImpl;

@Configuration
@ConditionalOnProperty(prefix = "recommender.chatgpt", name = "enabled", //
        havingValue = "true", matchIfMissing = false)
public class ChatGptRecommenderAutoConfiguration
{
    @Bean
    public ChatGptClient chatGptClient()
    {
        return new ChatGptClientImpl();
    }

    @Bean
    public ChatGptRecommenderFactory chatGptRecommenderFactory(ChatGptClient aClient)
    {
        return new ChatGptRecommenderFactory(aClient);
    }
}

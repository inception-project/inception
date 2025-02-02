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
package de.tudarmstadt.ukp.inception.recommendation.imls.azureaiopenai.client;

import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.ChatGptRecommenderTraits.OPENAI_API_URL;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.client.ChatGptResponseFormatType.JSON_OBJECT;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.lang.invoke.MethodHandles;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.client.ChatCompletionRequest;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.client.ChatGptClientImpl;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.client.ChatGptResponseFormat;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.client.ListModelsRequest;

class OpenAiClientTest
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String CHATGPT_BASE_URL = System.getProperty("chatgpt-base-url",
            OPENAI_API_URL);
    private static final String CHATGPT_API_KEY = System.getProperty("chatgpt-api-key");

    private ChatGptClientImpl sut = new ChatGptClientImpl();

    @BeforeAll
    static void setupClass()
    {
        assumeThat(CHATGPT_BASE_URL).isNotBlank();
        assumeThat(CHATGPT_API_KEY).isNotBlank();
    }

    @Test
    void testNonStream() throws Exception
    {
        var response = sut.chat(CHATGPT_BASE_URL, ChatCompletionRequest.builder() //
                .withApiKey(CHATGPT_API_KEY) //
                .withPrompt("Tell me a joke.") //
                .build());
        LOG.info("Response: [{}]", response.trim());
    }

    @Test
    void testJson() throws Exception
    {
        var response = sut.chat(CHATGPT_BASE_URL, ChatCompletionRequest.builder() //
                .withApiKey(CHATGPT_API_KEY) //
                .withPrompt("Generate a JSON map with the key/value pairs `a = 1` and `b = 2`") //
                .withResponseFormat(ChatGptResponseFormat.builder().withType(JSON_OBJECT).build()) //
                .build());
        LOG.info("Response: [{}]", response.trim());
    }

    @Test
    void testListModel() throws Exception
    {
        var response = sut.listModels(CHATGPT_BASE_URL, ListModelsRequest.builder() //
                .withApiKey(CHATGPT_API_KEY) //
                .build());
        LOG.info("Response: [{}]", response);
    }
}

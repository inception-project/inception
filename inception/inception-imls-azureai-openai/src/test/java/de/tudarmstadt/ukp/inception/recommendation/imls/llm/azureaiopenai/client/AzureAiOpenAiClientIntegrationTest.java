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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.azureaiopenai.client;

import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.azureaiopenai.client.AzureAiGenerateResponseFormat.JSON_OBJECT;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.lang.invoke.MethodHandles;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Live-server tests for {@link AzureAiOpenAiClientImpl}. Guarded by the {@code azure-base-url} and
 * {@code azure-api-key} system properties so they are skipped unless a real Azure OpenAI deployment
 * is configured. Offline behavior is covered by {@link AzureAiOpenAiClientTest}.
 */
class AzureAiOpenAiClientIntegrationTest
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String AZURE_BASE_URL = System.getProperty("azure-base-url");
    private static final String AZURE_API_KEY = System.getProperty("azure-api-key");

    private AzureAiOpenAiClientImpl sut = new AzureAiOpenAiClientImpl();

    @BeforeAll
    static void setupClass()
    {
        assumeThat(AZURE_BASE_URL).isNotBlank();
        assumeThat(AZURE_API_KEY).isNotBlank();
    }

    @Test
    void testNonStream() throws Exception
    {
        var response = sut.generate(AZURE_BASE_URL, AzureAiChatCompletionRequest.builder() //
                // .withModel("gpt-35-turbo-0301") //
                .withApiKey(AZURE_API_KEY) //
                .withPrompt("Tell me a joke.") //
                .build());
        LOG.info("Response: [{}]", response.getChoices().get(0).getMessage().getContent().trim());
    }

    @Test
    void testJson() throws Exception
    {
        var response = sut.generate(AZURE_BASE_URL, AzureAiChatCompletionRequest.builder() //
                // .withModel("gpt-35-turbo-0301") //
                .withApiKey(AZURE_API_KEY) //
                .withPrompt("Generate a JSON map with the key/value pairs `a = 1` and `b = 2`") //
                .withFormat(JSON_OBJECT) //
                .build());
        LOG.info("Response: [{}]", response.getChoices().get(0).getMessage().getContent().trim());
    }
}

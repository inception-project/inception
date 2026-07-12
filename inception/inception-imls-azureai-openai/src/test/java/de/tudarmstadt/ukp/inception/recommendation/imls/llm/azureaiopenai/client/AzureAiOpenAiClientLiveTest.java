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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ToolDescriptor;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import de.tudarmstadt.ukp.inception.support.test.http.HttpTestUtils;
import de.tudarmstadt.ukp.inception.support.test.llm.OllamazureContainer;

/**
 * Exercises {@link AzureAiOpenAiClientImpl} end-to-end against the Azure OpenAI wire protocol,
 * served by an {@link OllamazureContainer} (ollamazure) that forwards to a locally running Ollama.
 * This covers the Azure-specific request route
 * ({@code /openai/deployments/{deployment}/chat/completions?api-version=}), the SSE stream
 * assembly, and streamed tool-call handling against a real wire — none of which the offline
 * {@link AzureAiOpenAiClientTest} can reach. For a real Azure OpenAI deployment (via system
 * properties) see {@link AzureAiOpenAiClientIntegrationTest}.
 * <p>
 * Requires Docker (for ollamazure) and a local Ollama with {@code nemotron-3-nano:4b} pulled;
 * skipped otherwise. nemotron-3-nano:4b reliably fills visible content and calls tools, so one
 * model covers the chat, stream and tool tests. It reaches the host's Ollama via
 * {@code host.docker.internal}.
 */
@Testcontainers(disabledWithoutDocker = true)
class AzureAiOpenAiClientLiveTest
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // A small hybrid model that reliably fills visible content and calls tools, so one model serves
    // the chat, stream and tool tests.
    private static final String MODEL = "nemotron-3-nano:4b";
    private static final String OLLAMA_URL = "http://localhost:11434";

    @Container
    private static final OllamazureContainer ollamazure = new OllamazureContainer(MODEL);

    private final AzureAiOpenAiClientImpl sut = new AzureAiOpenAiClientImpl();

    @BeforeAll
    static void requireLocalOllama()
    {
        // ollamazure forwards to the host's Ollama; the container only provides the Azure front.
        assumeThat(HttpTestUtils.checkURL(OLLAMA_URL)) //
                .as("local Ollama reachable at " + OLLAMA_URL) //
                .isTrue();
    }

    private AzureAiChatCompletionRequest.Builder request(String aModel)
    {
        return AzureAiChatCompletionRequest.builder() //
                .withModel(aModel) //
                .withApiKey("not-used-by-ollamazure");
    }

    @Test
    void testNonStream() throws Exception
    {
        var response = sut.generate(ollamazure.getAzureBaseUrl(MODEL), request(MODEL) //
                .withPrompt("Tell me a joke in one sentence.") //
                .build());

        var choice = response.getChoices().get(0);
        LOG.info("Response: [{}], finish=[{}], usage=[{}]", choice.getMessage().getContent(),
                choice.getFinishReason(), response.getUsage());
        assertThat(choice.getMessage().getContent()).isNotBlank();
        assertThat(choice.getFinishReason()).isNotNull();
        assertThat(response.getUsage()).isNotNull();
        assertThat(response.getUsage().getTotalTokens()).isPositive();
    }

    @Test
    void testStream() throws Exception
    {
        var deltas = new ArrayList<String>();

        var response = sut.generate(ollamazure.getAzureBaseUrl(MODEL), request(MODEL) //
                .withPrompt("Count from 1 to 5.") //
                .withStream(true) //
                .build(), deltas::add);

        var choice = response.getChoices().get(0);
        LOG.info("Got {} deltas, final: [{}], finish=[{}], usage=[{}]", deltas.size(),
                choice.getMessage().getContent(), choice.getFinishReason(), response.getUsage());
        assertThat(deltas).isNotEmpty();
        assertThat(choice.getMessage().getContent()).isNotBlank();
        assertThat(choice.getFinishReason()).isNotNull();
        // usage arrives as a trailing SSE chunk (stream_options.include_usage); its capture is part
        // of what this test exercises.
        assertThat(response.getUsage()).isNotNull();
        assertThat(response.getUsage().getTotalTokens()).isPositive();
    }

    @Test
    void testStreamWithTool() throws Exception
    {
        var response = sut.generate(ollamazure.getAzureBaseUrl(MODEL), request(MODEL) //
                .withPrompt("What is the current weather in Berlin? Use the tool.") //
                .withTools(List.of(weatherTool())) //
                .withStream(true) //
                .build(), delta -> {
                });

        var choice = response.getChoices().get(0);
        var toolCalls = choice.getMessage().getToolCalls();
        LOG.info("finish=[{}], tool calls=[{}]", choice.getFinishReason(), toolCalls);

        // Exercises the streamed delta.tool_calls path (accumulateToolCalls) end-to-end.
        assertThat(choice.getFinishReason()).isEqualTo("tool_calls");
        assertThat(toolCalls).hasSize(1);
        assertThat(toolCalls.get(0).getFunction().getName()).isEqualTo("get_current_weather");
        assertThat(toolCalls.get(0).getFunction().getArguments()).contains("Berlin");
    }

    private static ToolDescriptor weatherTool()
    {
        var schema = JSONUtil.getObjectMapper().createObjectNode();
        schema.put("type", "object");
        schema.putObject("properties").putObject("location").put("type", "string");
        schema.putArray("required").add("location");
        return new ToolDescriptor("get_current_weather", "Get the current weather for a city",
                schema);
    }
}

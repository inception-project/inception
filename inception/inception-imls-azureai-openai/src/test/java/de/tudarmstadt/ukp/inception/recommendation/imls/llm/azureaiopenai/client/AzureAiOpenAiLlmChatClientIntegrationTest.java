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

import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ChatMessage.Role.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ChatMessage;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.Tool;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolParam;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ChatChunk;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ChatOptions;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.LlmEndpoint;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ToolDescriptor;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response.ResponseFormat;
import de.tudarmstadt.ukp.inception.security.client.auth.apikey.ApiKeyAuthenticationTraits;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import de.tudarmstadt.ukp.inception.support.test.http.HttpTestUtils;

/**
 * Exercises {@link AzureAiOpenAiLlmChatClient} end-to-end against a locally running
 * <a href="https://github.com/sinedied/ollamazure">ollamazure</a>, a local emulator of the Azure
 * OpenAI REST API (deployment name in the URL path, {@code api-version} query parameter) backed by
 * Ollama. ollamazure needs no Azure credentials - the {@code api-key} header must merely be
 * present, and its value is ignored.
 * <p>
 * The test is skipped (JUnit assumption, not a failure) unless ollamazure is reachable, mirroring
 * {@code OllamaLlmChatClientIntegrationTest}. Start it yourself before running - the test does not:
 *
 * <pre>
 * npm install -g ollamazure
 * ollamazure
 * </pre>
 *
 * with the default completions model {@code phi3} and embeddings model {@code all-minilm:l6-v2} on
 * the default port 4041. Overridable via system properties: {@code ollamazure-base-url} (the server
 * root used for the reachability probe), {@code ollamazure-deployment-url} (the Azure-shaped base
 * URL handed to the client, deployment path included), and {@code ollamazure-chat-model}. Offline
 * request/response mapping is covered by {@link AzureAiOpenAiLlmChatClientTest}.
 */
class AzureAiOpenAiLlmChatClientIntegrationTest
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /** ollamazure server root - used only for the reachability probe. */
    private static final String BASE_URL = System.getProperty("ollamazure-base-url",
            "http://localhost:4041/");

    /** ollamazure's default completions model, doubling as the Azure "deployment" name. */
    private static final String CHAT_MODEL = System.getProperty("ollamazure-chat-model", "phi3");

    /**
     * Azure-shaped base URL that already embeds the deployment path, matching what
     * {@link AzureAiOpenAiClientImpl} expects (it appends
     * {@code chat/completions?api-version=...}).
     */
    private static final String DEPLOYMENT_URL = System.getProperty("ollamazure-deployment-url",
            "http://localhost:4041/openai/deployments/" + CHAT_MODEL);

    private final AzureAiOpenAiLlmChatClient sut = new AzureAiOpenAiLlmChatClient(
            new AzureAiOpenAiClientImpl());

    @BeforeAll
    static void checkIfOllamazureIsRunning()
    {
        assumeThat(HttpTestUtils.checkURL(BASE_URL)).isTrue();
    }

    private static LlmEndpoint endpoint()
    {
        // ollamazure ignores the key value but the client requires a non-blank key to be present.
        var auth = new ApiKeyAuthenticationTraits();
        auth.setApiKey("dummy-key");
        return new LlmEndpoint(AzureAiOpenAiLlmChatClient.ID, DEPLOYMENT_URL, CHAT_MODEL, auth);
    }

    @Test
    void testChat() throws Exception
    {
        var messages = List.of(new ChatMessage(USER, "Tell me a joke in one sentence."));

        var result = sut.chat(endpoint(), messages, ChatOptions.defaults());

        LOG.info("Response: [{}]", result.message().content());
        LOG.info("Usage: {}", result.usage());
        assertThat(result.message().content()).isNotBlank();
        assertThat(result.message().role()).isEqualTo(ChatMessage.Role.ASSISTANT);
        assertThat(result.finishReason()).isNotNull();
    }

    @Test
    void testChatWithJsonResponseFormat() throws Exception
    {
        var messages = List.of(new ChatMessage(USER,
                "Return a JSON object with the keys `a` set to 1 and `b` set to 2."));
        var options = new ChatOptions(ResponseFormat.JSON, null, List.of(), null);

        var result = sut.chat(endpoint(), messages, options);

        LOG.info("Response: [{}]", result.message().content());
        assertThat(JSONUtil.getObjectMapper().readTree(result.message().content())).isNotNull();
    }

    @Test
    void testChatStream() throws Exception
    {
        var messages = List.of(new ChatMessage(USER, "Count from 1 to 5."));
        var chunks = new ArrayList<ChatChunk>();

        var result = sut.chatStream(endpoint(), messages, ChatOptions.defaults(), chunks::add);

        LOG.info("Got {} chunks, final: [{}]", chunks.size(), result.message().content());
        assertThat(chunks).isNotEmpty();
        assertThat(result.message().content()).isNotBlank();
    }

    static class WeatherService
    {
        @Tool(value = "get_current_weather", description = "Get the current weather for a given location.")
        @SuppressWarnings("unused")
        String getCurrentWeather(@ToolParam(value = "location", //
                description = "The city to get the weather for.") String aLocation)
        {
            return "sunny";
        }
    }

    @Test
    void testChatWithTool() throws Exception
    {
        var weatherTool = ToolDescriptor.fromMethod(
                WeatherService.class.getDeclaredMethod("getCurrentWeather", String.class));

        var messages = List
                .of(new ChatMessage(USER, "What is the current weather in Berlin? Use the tool."));
        var options = new ChatOptions(null, null, List.of(weatherTool), null);

        var result = sut.chat(endpoint(), messages, options);

        LOG.info("Finish reason: {}, tool calls: {}", result.finishReason(), result.toolCalls());

        assertThat(result.toolCalls()).as("tool calls").isNotEmpty();

        var call = result.toolCalls().get(0);
        assertThat(call.name()).isEqualTo("get_current_weather");
        assertThat(call.arguments()).as("arguments JsonNode").isNotNull();
        assertThat(call.arguments().isObject()).as("arguments is an object node").isTrue();
    }
}

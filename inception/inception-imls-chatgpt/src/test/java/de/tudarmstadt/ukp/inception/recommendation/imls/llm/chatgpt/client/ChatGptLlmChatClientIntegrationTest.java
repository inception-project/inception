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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.client;

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
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.FinishReason;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.LlmEndpoint;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ToolDescriptor;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response.ResponseFormat;
import de.tudarmstadt.ukp.inception.security.client.auth.apikey.ApiKeyAuthenticationTraits;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import de.tudarmstadt.ukp.inception.support.test.http.HttpTestUtils;

/**
 * Exercises {@link ChatGptLlmChatClient} against Ollama's OpenAI-compatible endpoint. The
 * {@code /v1} path segment is appended by the client, so the endpoint URL points at the Ollama
 * root. Requires a local Ollama with {@code nemotron-3-nano:4b} pulled; skipped otherwise.
 */
class ChatGptLlmChatClientIntegrationTest
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String OLLAMA_URL = "http://localhost:11434";
    private static final String MODEL = "nemotron-3-nano:4b";

    private final ChatGptLlmChatClient sut = new ChatGptLlmChatClient(new ChatGptClientImpl());

    @BeforeAll
    static void checkIfOllamaIsRunning()
    {
        assumeThat(HttpTestUtils.checkURL(OLLAMA_URL)).isTrue();
    }

    private static LlmEndpoint endpoint(String aModel)
    {
        var auth = new ApiKeyAuthenticationTraits();
        auth.setApiKey("not-used-by-ollama");
        return new LlmEndpoint(ChatGptLlmChatClient.ID, OLLAMA_URL, aModel, auth);
    }

    @Test
    void testChat() throws Exception
    {
        var messages = List.of(new ChatMessage(USER, "Tell me a joke in one sentence."));

        var result = sut.chat(endpoint(MODEL), messages, ChatOptions.defaults());

        LOG.info("Response: [{}]", result.message().content());
        assertThat(result.message().content()).isNotBlank();
        assertThat(result.message().role()).isEqualTo(ChatMessage.Role.ASSISTANT);
    }

    @Test
    void testChatWithJsonResponseFormat() throws Exception
    {
        var messages = List.of(new ChatMessage(USER,
                "Return a JSON object with the keys `a` set to 1 and `b` set to 2."));
        var options = new ChatOptions(ResponseFormat.JSON, null, List.of(), null);

        var result = sut.chat(endpoint(MODEL), messages, options);

        LOG.info("Response: [{}]", result.message().content());
        // Provider promised JSON object response; verify it parses.
        assertThat(JSONUtil.getObjectMapper().readTree(result.message().content())).isNotNull();
    }

    @Test
    void testChatStream() throws Exception
    {
        var messages = List.of(new ChatMessage(USER, "Count from 1 to 5."));
        var chunks = new ArrayList<ChatChunk>();

        var result = sut.chatStream(endpoint(MODEL), messages, ChatOptions.defaults(), chunks::add);

        LOG.info("Got {} chunks, final: [{}]", chunks.size(), result.message().content());
        assertThat(chunks).isNotEmpty();
        assertThat(result.message().content()).isNotBlank();
    }

    @Test
    void testListModels() throws Exception
    {
        var models = sut.listModels(endpoint(null));

        LOG.info("Models: {}", models);
        assertThat(models).extracting("id").contains(MODEL);
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

        var result = sut.chat(endpoint(MODEL), messages, options);

        LOG.info("Finish reason: {}, tool calls: {}", result.finishReason(), result.toolCalls());

        assertThat(result.toolCalls()).as("tool calls").isNotEmpty();
        assertThat(result.finishReason()).isEqualTo(FinishReason.TOOL_CALLS);

        var call = result.toolCalls().get(0);
        assertThat(call.name()).isEqualTo("get_current_weather");
        assertThat(call.arguments()).as("arguments JsonNode").isNotNull();
        assertThat(call.arguments().isObject()).as("arguments is an object node").isTrue();

        var location = call.arguments().get("location");
        assertThat(location).as("location argument node").isNotNull();
        assertThat(location.isTextual()).as("location node is TextNode").isTrue();
        assertThat(location.asText()).containsIgnoringCase("berlin");
    }
}

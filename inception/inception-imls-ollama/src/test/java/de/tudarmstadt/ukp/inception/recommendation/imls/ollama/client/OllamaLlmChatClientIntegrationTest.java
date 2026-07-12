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
package de.tudarmstadt.ukp.inception.recommendation.imls.ollama.client;

import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ChatMessage.Role.USER;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.OllamaRecommenderTraits.DEFAULT_OLLAMA_URL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.lang.invoke.MethodHandles;
import java.net.http.HttpClient;
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
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ReasoningEffort;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.FinishReason;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.LlmEndpoint;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ToolDescriptor;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaClientImpl;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaLlmChatClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaMetricsImpl;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response.ResponseFormat;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import de.tudarmstadt.ukp.inception.support.test.http.HttpTestUtils;

/**
 * Exercises {@link OllamaLlmChatClient} against a locally running Ollama. Requires {@code
 * nemotron-3-nano:4b} (chat/tool/JSON/thinking) and {@code granite-embedding:278m-fp16}
 * (embeddings) to be pulled; skipped if no Ollama is reachable.
 */
class OllamaLlmChatClientIntegrationTest
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String CHAT_MODEL = "nemotron-3-nano:4b";
    private static final String EMBED_MODEL = "granite-embedding:278m-fp16";
    // nemotron-3-nano is a hybrid model: the same model serves the chat/tool/JSON tests and the
    // thinking tests (it has a toggleable thinking channel).
    private static final String THINKING_MODEL = CHAT_MODEL;

    private final OllamaLlmChatClient sut = new OllamaLlmChatClient(
            new OllamaClientImpl(HttpClient.newBuilder().build(), new OllamaMetricsImpl()));

    @BeforeAll
    static void checkIfOllamaIsRunning()
    {
        assumeThat(HttpTestUtils.checkURL(DEFAULT_OLLAMA_URL)).isTrue();
    }

    private static LlmEndpoint endpoint(String aModel)
    {
        return new LlmEndpoint(OllamaLlmChatClient.ID, DEFAULT_OLLAMA_URL, aModel, null);
    }

    @Test
    void testChat() throws Exception
    {
        var messages = List.of(new ChatMessage(USER, "Tell me a joke in one sentence."));

        var result = sut.chat(endpoint(CHAT_MODEL), messages, ChatOptions.defaults());

        LOG.info("Response: [{}]", result.message().content());
        LOG.info("Usage: {}", result.usage());
        assertThat(result.message().content()).isNotBlank();
        assertThat(result.message().role()).isEqualTo(ChatMessage.Role.ASSISTANT);
        assertThat(result.finishReason()).isNotNull();
        assertThat(result.usage()).isNotNull();
        assertThat(result.usage().totalTokens()).isPositive();
    }

    /**
     * A reasoning model, left at its default thinking behavior (the client no longer forces
     * {@code think=false}), must still return a non-empty final answer in {@code content} — the
     * reasoning goes into {@code thinking}, not instead of the answer.
     */
    @Test
    void testChatWithThinkingModelDefault() throws Exception
    {
        var messages = List.of(new ChatMessage(USER, "What is 17 + 25? Answer briefly."));

        var result = sut.chat(endpoint(THINKING_MODEL), messages, ChatOptions.defaults());

        LOG.info("Content: [{}]", result.message().content());
        LOG.info("Thinking: [{}]", result.message().thinking());
        assertThat(result.message().content()) //
                .as("final answer is not swallowed by the thinking output") //
                .isNotBlank();
    }

    /**
     * Explicitly requesting reasoning via {@link ChatOptions#reasoningEffort()} must surface the
     * model's reasoning in {@code thinking} while still returning the final answer in
     * {@code content}.
     */
    @Test
    void testChatWithThinkingEnabled() throws Exception
    {
        var messages = List.of(new ChatMessage(USER, "What is 17 + 25? Answer briefly."));
        var options = ChatOptions.builder().withReasoningEffort(ReasoningEffort.HIGH).build();

        var result = sut.chat(endpoint(THINKING_MODEL), messages, options);

        LOG.info("Content: [{}]", result.message().content());
        LOG.info("Thinking: [{}]", result.message().thinking());
        assertThat(result.message().thinking()) //
                .as("reasoning is surfaced separately when reasoning effort is requested") //
                .isNotBlank();
        assertThat(result.message().content()) //
                .as("final answer is still present alongside the thinking") //
                .isNotBlank();
    }

    @Test
    void testChatWithJsonResponseFormat() throws Exception
    {
        var messages = List.of(new ChatMessage(USER,
                "Return a JSON object with the keys `a` set to 1 and `b` set to 2."));
        var options = new ChatOptions(ResponseFormat.JSON, null, List.of(), null);

        var result = sut.chat(endpoint(CHAT_MODEL), messages, options);

        LOG.info("Response: [{}]", result.message().content());
        assertThat(JSONUtil.getObjectMapper().readTree(result.message().content())).isNotNull();
    }

    @Test
    void testChatStream() throws Exception
    {
        var messages = List.of(new ChatMessage(USER, "Count from 1 to 5."));
        var chunks = new ArrayList<ChatChunk>();

        var result = sut.chatStream(endpoint(CHAT_MODEL), messages, ChatOptions.defaults(),
                chunks::add);

        LOG.info("Got {} chunks, final: [{}]", chunks.size(), result.message().content());
        assertThat(chunks).isNotEmpty();
        assertThat(result.message().content()).isNotBlank();
    }

    @Test
    void testEmbed() throws Exception
    {
        var vectors = sut.embed(endpoint(EMBED_MODEL), List.of("hello world", "good morning"),
                null);

        assertThat(vectors).hasSize(2);
        assertThat(vectors.get(0)).isNotEmpty();
        assertThat(vectors.get(0).length).isEqualTo(vectors.get(1).length);
        LOG.info("Embedding dim: {}", vectors.get(0).length);
    }

    @Test
    void testListModels() throws Exception
    {
        var models = sut.listModels(endpoint(null));

        LOG.info("Models: {}", models);
        assertThat(models).extracting("id").contains(CHAT_MODEL);
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

        var result = sut.chat(endpoint(CHAT_MODEL), messages, options);

        LOG.info("Finish reason: {}, tool calls: {}", result.finishReason(), result.toolCalls());

        assertThat(result.toolCalls()).as("tool calls").isNotEmpty();
        assertThat(result.finishReason()).isEqualTo(FinishReason.TOOL_CALLS);

        var call = result.toolCalls().get(0);
        assertThat(call.name()).isEqualTo("get_current_weather");
        assertThat(call.arguments()).as("arguments JsonNode").isNotNull();
        assertThat(call.arguments().isObject()).as("arguments is an object node").isTrue();

        // valueToTree should yield proper value nodes — not POJONode wrappers — so the
        // JSON-tree-style type predicates report correctly.
        var location = call.arguments().get("location");
        assertThat(location).as("location argument node").isNotNull();
        assertThat(location.isTextual()).as("location node is TextNode").isTrue();
        assertThat(location.asText()).containsIgnoringCase("berlin");
    }
}

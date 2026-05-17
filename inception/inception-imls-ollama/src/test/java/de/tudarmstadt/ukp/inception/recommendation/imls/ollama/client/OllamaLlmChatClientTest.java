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
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ChatChunk;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ChatOptions;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.LlmEndpoint;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaClientImpl;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaLlmChatClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaMetricsImpl;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response.ResponseFormat;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import de.tudarmstadt.ukp.inception.support.test.http.HttpTestUtils;

/**
 * Exercises {@link OllamaLlmChatClient} against a locally running Ollama. Requires {@code
 * ministral-3:8b} and {@code granite-embedding:278m-fp16} to be pulled; skipped if no Ollama is
 * reachable.
 */
class OllamaLlmChatClientTest
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String CHAT_MODEL = "ministral-3:8b";
    private static final String EMBED_MODEL = "granite-embedding:278m-fp16";

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
        var vectors = sut.embed(endpoint(EMBED_MODEL), List.of("hello world", "good morning"));

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
}

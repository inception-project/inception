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
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ChatMessage;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ChatOptions;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.LlmEndpoint;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response.ResponseFormat;
import de.tudarmstadt.ukp.inception.security.client.auth.apikey.ApiKeyAuthenticationTraits;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import de.tudarmstadt.ukp.inception.support.test.http.HttpTestUtils;

/**
 * Exercises {@link ChatGptLlmChatClient} against Ollama's OpenAI-compatible endpoint. The
 * {@code /v1} path segment is appended by the client, so the endpoint URL points at the Ollama
 * root. Requires a local Ollama with {@code ministral-3:8b} pulled; skipped otherwise.
 */
class ChatGptLlmChatClientTest
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String OLLAMA_URL = "http://localhost:11434";
    private static final String MODEL = "ministral-3:8b";

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
    void testListModels() throws Exception
    {
        var models = sut.listModels(endpoint(null));

        LOG.info("Models: {}", models);
        assertThat(models).extracting("id").contains(MODEL);
    }
}

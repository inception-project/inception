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
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpClient;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ChatMessage;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ChatOptions;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.LlmEndpoint;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ModelCapability;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaChatRequest;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaChatResponse;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaClientImpl;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaEmbedRequest;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaGenerateRequest;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaGenerateResponse;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaLlmChatClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaMetricsImpl;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaModelInfo;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaShowRequest;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaShowResponse;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaTag;

/**
 * Offline unit tests for {@link OllamaLlmChatClient} that exercise request building without a
 * running Ollama. Live-server behavior is covered by {@link OllamaLlmChatClientIntegrationTest}.
 */
class OllamaLlmChatClientTest
{
    private final OllamaLlmChatClient sut = new OllamaLlmChatClient(
            new OllamaClientImpl(HttpClient.newBuilder().build(), new OllamaMetricsImpl()));

    private static LlmEndpoint endpoint()
    {
        return new LlmEndpoint(OllamaLlmChatClient.ID, "http://localhost:11434", "some-model",
                null);
    }

    @Test
    void testTypedOptionsAreTranslatedToOllamaWireNames() throws Exception
    {
        var options = ChatOptions.builder() //
                .withContextLength(4096) //
                .withTopK(40) //
                .withRepeatPenalty(1.1) //
                .build();

        var request = buildChatRequest(options);

        // The neutral typed fields must reach Ollama under its own wire names, byte-identical to
        // the previous behavior where AgentLoop put these keys into the free-form options map.
        assertThat(request.getOptions()) //
                .containsEntry("num_ctx", 4096) //
                .containsEntry("top_k", 40) //
                .containsEntry("repeat_penalty", 1.1);
    }

    @Test
    void testUnsetTypedOptionsDoNotLeakToTheWire() throws Exception
    {
        var request = buildChatRequest(ChatOptions.defaults());

        assertThat(request.getOptions()) //
                .doesNotContainKeys("num_ctx", "top_k", "repeat_penalty");
    }

    /**
     * Invokes the private {@code buildChatRequest} via reflection so the assertion does not require
     * a running Ollama.
     */
    private OllamaChatRequest buildChatRequest(ChatOptions aOptions) throws Exception
    {
        var method = OllamaLlmChatClient.class.getDeclaredMethod("buildChatRequest",
                LlmEndpoint.class, List.class, ChatOptions.class, boolean.class);
        method.setAccessible(true);
        return (OllamaChatRequest) method.invoke(sut, endpoint(), of(new ChatMessage(USER, "Hi")),
                aOptions, false);
    }

    @Test
    void testDescribeModelMapsContextLengthAndCapabilities() throws Exception
    {
        var info = new OllamaModelInfo();
        info.setProperty("llama.context_length", 8192);

        // "insert" has no neutral equivalent and must be dropped; the rest must be translated.
        var showResponse = new OllamaShowResponse(null,
                of("completion", "tools", "vision", "thinking", "embedding", "insert"), null, info);

        var describingSut = new OllamaLlmChatClient(new StubOllamaClient(showResponse));

        var details = describingSut.describeModel(endpoint());

        assertThat(details).isPresent();
        assertThat(details.get().contextLength()).isEqualTo(8192);
        assertThat(details.get().capabilities()) //
                .containsExactlyInAnyOrder(ModelCapability.CHAT, ModelCapability.TOOLS,
                        ModelCapability.VISION, ModelCapability.THINKING,
                        ModelCapability.EMBEDDINGS);
    }

    @Test
    void testDescribeModelWithoutModelInfoYieldsNullContextLength() throws Exception
    {
        var showResponse = new OllamaShowResponse(null, of("completion"), null, null);

        var describingSut = new OllamaLlmChatClient(new StubOllamaClient(showResponse));

        var details = describingSut.describeModel(endpoint());

        assertThat(details).isPresent();
        assertThat(details.get().contextLength()).isNull();
        assertThat(details.get().capabilities()).containsExactly(ModelCapability.CHAT);
    }

    /**
     * Minimal hand-written {@link OllamaClient} stub (no Mockito in this module) that only answers
     * {@code getModelInfo}; all other operations are unsupported.
     */
    private static final class StubOllamaClient
        implements OllamaClient
    {
        private final OllamaShowResponse showResponse;

        StubOllamaClient(OllamaShowResponse aShowResponse)
        {
            showResponse = aShowResponse;
        }

        @Override
        public OllamaShowResponse getModelInfo(String aUrl, OllamaShowRequest aRequest)
        {
            return showResponse;
        }

        @Override
        public String generate(String aUrl, OllamaGenerateRequest aRequest)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public String generate(String aUrl, OllamaGenerateRequest aRequest,
                Consumer<OllamaGenerateResponse> aCallback)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public OllamaChatResponse chat(String aUrl, OllamaChatRequest aRequest)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public OllamaChatResponse chat(String aUrl, OllamaChatRequest aRequest,
                Consumer<OllamaChatResponse> aCallback)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<OllamaTag> listModels(String aUrl, String aApiKey)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Pair<String, float[]>> embed(String aUrl, OllamaEmbedRequest aRequest)
        {
            throw new UnsupportedOperationException();
        }
    }
}

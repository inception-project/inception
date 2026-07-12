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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ServerSentEventReader;

/**
 * Offline unit tests for {@link AzureAiOpenAiClientImpl}'s SSE stream assembly, driven by canned
 * {@code data:} lines (parsed via {@link ServerSentEventReader}) with no live server. Live-server
 * behavior is covered by {@link AzureAiOpenAiClientIntegrationTest}.
 */
class AzureAiOpenAiClientTest
{
    private final AzureAiOpenAiClientImpl sut = new AzureAiOpenAiClientImpl();

    @Test
    void testSseContentAssemblyAndCallbacks() throws Exception
    {
        var lines = List.of( //
                "data: {\"model\":\"gpt-4o\",\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\",\"content\":\"Hel\"}}]}", //
                "", //
                "data: {\"choices\":[{\"index\":0,\"delta\":{\"content\":\"lo\"}}]}", //
                "", //
                "data: {\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"stop\"}]}", //
                "", //
                "data: {\"choices\":[],\"usage\":{\"prompt_tokens\":5,\"completion_tokens\":2,\"total_tokens\":7}}", //
                "", //
                "data: [DONE]");

        var deltas = new ArrayList<String>();
        var response = sut.assembleSseStream(ServerSentEventReader.parse(lines.stream()),
                "fallback-model", deltas::add);

        assertThat(deltas).containsExactly("Hel", "lo");
        assertThat(response.getModel()).isEqualTo("gpt-4o");
        assertThat(response.getChoices().get(0).getMessage().getContent()).isEqualTo("Hello");
        assertThat(response.getChoices().get(0).getFinishReason()).isEqualTo("stop");
        assertThat(response.getUsage().getPromptTokens()).isEqualTo(5);
        assertThat(response.getUsage().getCompletionTokens()).isEqualTo(2);
        assertThat(response.getUsage().getTotalTokens()).isEqualTo(7);
    }

    @Test
    void testSseToolCallFragmentsAreMergedByIndex() throws Exception
    {
        var lines = List.of( //
                "data: {\"choices\":[{\"index\":0,\"delta\":{\"tool_calls\":[{\"index\":0,\"id\":\"call_1\",\"type\":\"function\",\"function\":{\"name\":\"get_weather\",\"arguments\":\"\"}}]}}]}", //
                "", //
                "data: {\"choices\":[{\"index\":0,\"delta\":{\"tool_calls\":[{\"index\":0,\"function\":{\"arguments\":\"{\\\"city\\\":\"}}]}}]}", //
                "", //
                "data: {\"choices\":[{\"index\":0,\"delta\":{\"tool_calls\":[{\"index\":0,\"function\":{\"arguments\":\"\\\"Berlin\\\"}\"}}]}}]}", //
                "", //
                "data: {\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"tool_calls\"}]}", //
                "", //
                "data: [DONE]");

        var response = sut.assembleSseStream(ServerSentEventReader.parse(lines.stream()), "m",
                null);

        var toolCalls = response.getChoices().get(0).getMessage().getToolCalls();
        assertThat(toolCalls).hasSize(1);
        assertThat(toolCalls.get(0).getId()).isEqualTo("call_1");
        assertThat(toolCalls.get(0).getFunction().getName()).isEqualTo("get_weather");
        assertThat(toolCalls.get(0).getFunction().getArguments())
                .isEqualTo("{\"city\":\"Berlin\"}");
        assertThat(response.getChoices().get(0).getFinishReason()).isEqualTo("tool_calls");
    }
}

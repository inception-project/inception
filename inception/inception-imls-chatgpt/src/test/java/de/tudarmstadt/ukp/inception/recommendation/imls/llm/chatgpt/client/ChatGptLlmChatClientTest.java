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

import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ChatMessage.Role.ASSISTANT;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ChatMessage.Role.TOOL;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ChatMessage.Role.USER;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ChatMessage;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ChatOptions;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.FinishReason;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.LlmEndpoint;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ToolCall;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ToolDescriptor;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

/**
 * Offline unit tests for {@link ChatGptLlmChatClient} exercising request building and wire-response
 * mapping without a live OpenAI server. Live-server behavior is covered by
 * {@link ChatGptLlmChatClientIntegrationTest}.
 */
class ChatGptLlmChatClientTest
{
    private final CapturingChatGptClient client = new CapturingChatGptClient();
    private final ChatGptLlmChatClient sut = new ChatGptLlmChatClient(client);

    private static LlmEndpoint endpoint()
    {
        return new LlmEndpoint(ChatGptLlmChatClient.ID, "http://localhost", "some-model", null);
    }

    private static ToolDescriptor weatherTool()
    {
        var schema = JSONUtil.getObjectMapper().createObjectNode();
        schema.put("type", "object");
        schema.putObject("properties").putObject("city").put("type", "string");
        schema.putArray("required").add("city");
        return new ToolDescriptor("get_weather", "Get the weather for a city", schema);
    }

    @Test
    void testToolsAreSerializedIntoTheRequest() throws Exception
    {
        var options = ChatOptions.builder().withTools(of(weatherTool())).build();
        client.response = cannedTextResponse("hi");

        sut.chat(endpoint(), of(new ChatMessage(USER, "Weather in Berlin?")), options);

        var tools = client.lastRequest.getTools();
        assertThat(tools).hasSize(1);
        assertThat(tools.get(0).getType()).isEqualTo("function");
        assertThat(tools.get(0).getFunction().getName()).isEqualTo("get_weather");
        assertThat(tools.get(0).getFunction().getParameters().get("type").asText())
                .isEqualTo("object");
    }

    @Test
    void testStreamFlagAndStreamOptionsAreSetOnlyWhenStreaming() throws Exception
    {
        client.response = cannedTextResponse("ok");
        sut.chat(endpoint(), of(new ChatMessage(USER, "Hi")), ChatOptions.defaults());
        assertThat(client.lastRequest.getStream()).isNull();
        assertThat(client.lastRequest.getStreamOptions()).isNull();

        client.streamResponse = cannedTextResponse("ok");
        sut.chatStream(endpoint(), of(new ChatMessage(USER, "Hi")), ChatOptions.defaults(),
                chunk -> {
                });
        assertThat(client.lastStreamRequest.getStream()).isTrue();
        assertThat(client.lastStreamRequest.getStreamOptions().isIncludeUsage()).isTrue();
    }

    @Test
    void testToolResultMessageCarriesToolCallId() throws Exception
    {
        client.response = cannedTextResponse("done");

        var toolResult = new ChatMessage(TOOL, "sunny", null, "call_123");
        sut.chat(endpoint(), of(toolResult), ChatOptions.defaults());

        var wireMessage = client.lastRequest.getMessages().get(0);
        assertThat(wireMessage.getRole()).isEqualTo("tool");
        assertThat(wireMessage.getToolCallId()).isEqualTo("call_123");
    }

    @Test
    void testAssistantToolCallsAreSerialized() throws Exception
    {
        client.response = cannedTextResponse("done");

        var args = JSONUtil.getObjectMapper().createObjectNode();
        args.put("city", "Berlin");
        var assistant = new ChatMessage(ASSISTANT, "", null, null,
                of(new ToolCall("call_1", "get_weather", args)));

        sut.chat(endpoint(), of(assistant), ChatOptions.defaults());

        var wireMessage = client.lastRequest.getMessages().get(0);
        assertThat(wireMessage.getToolCalls()).hasSize(1);
        assertThat(wireMessage.getToolCalls().get(0).getId()).isEqualTo("call_1");
        assertThat(wireMessage.getToolCalls().get(0).getFunction().getName())
                .isEqualTo("get_weather");
        assertThat(wireMessage.getToolCalls().get(0).getFunction().getArguments())
                .contains("Berlin");
    }

    @Test
    void testResponseMappingWithToolCallsUsageAndFinishReason() throws Exception
    {
        var json = """
                {
                  "model": "gpt-4o",
                  "choices": [
                    {
                      "index": 0,
                      "finish_reason": "tool_calls",
                      "message": {
                        "role": "assistant",
                        "content": null,
                        "tool_calls": [
                          {
                            "id": "call_abc",
                            "type": "function",
                            "function": {
                              "name": "get_weather",
                              "arguments": "{\\"city\\":\\"Berlin\\"}"
                            }
                          }
                        ]
                      }
                    }
                  ],
                  "usage": { "prompt_tokens": 11, "completion_tokens": 7, "total_tokens": 18 }
                }
                """;
        client.response = JSONUtil.getObjectMapper().readValue(json, ChatCompletionResponse.class);

        var result = sut.chat(endpoint(), of(new ChatMessage(USER, "Weather?")),
                ChatOptions.defaults());

        assertThat(result.finishReason()).isEqualTo(FinishReason.TOOL_CALLS);
        assertThat(result.toolCalls()).hasSize(1);
        assertThat(result.toolCalls().get(0).id()).isEqualTo("call_abc");
        assertThat(result.toolCalls().get(0).name()).isEqualTo("get_weather");
        assertThat(result.toolCalls().get(0).arguments().get("city").asText()).isEqualTo("Berlin");
        assertThat(result.usage().promptTokens()).isEqualTo(11);
        assertThat(result.usage().completionTokens()).isEqualTo(7);
        assertThat(result.usage().totalTokens()).isEqualTo(18);
    }

    @Test
    void testMalformedToolArgumentsDoNotThrow() throws Exception
    {
        var json = """
                {
                  "choices": [
                    {
                      "index": 0,
                      "finish_reason": "tool_calls",
                      "message": {
                        "role": "assistant",
                        "tool_calls": [
                          { "id": "c1", "type": "function",
                            "function": { "name": "f", "arguments": "not-json" } },
                          { "id": "c2", "type": "function",
                            "function": { "name": "g", "arguments": "[1,2,3]" } }
                        ]
                      }
                    }
                  ]
                }
                """;
        client.response = JSONUtil.getObjectMapper().readValue(json, ChatCompletionResponse.class);

        var result = sut.chat(endpoint(), of(new ChatMessage(USER, "Go")), ChatOptions.defaults());

        // A bad payload must degrade to an empty object node, not abort the turn.
        assertThat(result.toolCalls()).hasSize(2);
        assertThat(result.toolCalls().get(0).arguments().isObject()).isTrue();
        assertThat(result.toolCalls().get(0).arguments().size()).isZero();
        assertThat(result.toolCalls().get(1).arguments().isObject()).isTrue();
        assertThat(result.toolCalls().get(1).arguments().size()).isZero();
    }

    @Test
    void testFinishReasonMapping() throws Exception
    {
        assertThat(mapFinish("stop")).isEqualTo(FinishReason.STOP);
        assertThat(mapFinish("length")).isEqualTo(FinishReason.LENGTH);
        assertThat(mapFinish("content_filter")).isEqualTo(FinishReason.CONTENT_FILTER);
        assertThat(mapFinish("something_else")).isEqualTo(FinishReason.OTHER);
    }

    private FinishReason mapFinish(String aReason) throws Exception
    {
        var response = cannedTextResponse("x");
        response.getChoices().get(0).setFinishReason(aReason);
        client.response = response;
        return sut.chat(endpoint(), of(new ChatMessage(USER, "Hi")), ChatOptions.defaults())
                .finishReason();
    }

    private static ChatCompletionResponse cannedTextResponse(String aContent)
    {
        var message = new ChatCompletionMessage("assistant", aContent);
        var choice = new ChatCompletionChoice();
        choice.setIndex(0);
        choice.setMessage(message);
        choice.setFinishReason("stop");
        var response = new ChatCompletionResponse();
        response.setChoices(asList(choice));
        return response;
    }

    /**
     * Records the last request and returns a canned response, so the adapter can be exercised with
     * no live server.
     */
    private static final class CapturingChatGptClient
        implements ChatGptClient
    {
        private ChatCompletionRequest lastRequest;
        private ChatCompletionRequest lastStreamRequest;
        private ChatCompletionResponse response;
        private ChatCompletionResponse streamResponse;

        @Override
        public ChatCompletionResponse chat(String aUrl, ChatCompletionRequest aRequest)
            throws IOException
        {
            lastRequest = aRequest;
            return response;
        }

        @Override
        public ChatCompletionResponse chat(String aUrl, ChatCompletionRequest aRequest,
                Consumer<String> aContentCallback)
            throws IOException
        {
            lastStreamRequest = aRequest;
            return streamResponse;
        }

        @Override
        public List<ChatGptModel> listModels(String aUrl, ListModelsRequest aRequest)
            throws IOException
        {
            return emptyList();
        }
    }
}

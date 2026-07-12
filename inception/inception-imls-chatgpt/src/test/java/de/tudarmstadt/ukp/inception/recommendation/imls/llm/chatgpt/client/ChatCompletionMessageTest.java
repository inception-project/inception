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

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

class ChatCompletionMessageTest
{
    /**
     * OpenAI/Azure return HTTP 400 if a {@code role:tool} message in a multi-round tool loop is
     * sent without the {@code tool_call_id} of the call it answers. This guards that the id
     * survives serialization (it is {@code @JsonInclude(NON_NULL)}).
     */
    @Test
    void thatToolResultMessageSerializesToolCallId() throws Exception
    {
        var message = new ChatCompletionMessage("tool", "sunny", null, "call_abc123");

        var json = JSONUtil.getObjectMapper().valueToTree(message);

        assertThat(json.get("tool_call_id")).isNotNull();
        assertThat(json.get("tool_call_id").asText()).isEqualTo("call_abc123");
    }

    /**
     * When re-sending the conversation history, the assistant message that requested the tool must
     * echo the call {@code id} so the following {@code role:tool} result can be correlated.
     */
    @Test
    void thatAssistantToolCallSerializesId() throws Exception
    {
        var function = new ChatCompletionToolCall.Function();
        function.setName("get_current_weather");
        function.setArguments("{\"location\":\"Berlin\"}");
        var toolCall = new ChatCompletionToolCall();
        toolCall.setId("call_abc123");
        toolCall.setType("function");
        toolCall.setFunction(function);

        var message = new ChatCompletionMessage("assistant", "", List.of(toolCall), null);

        var json = JSONUtil.getObjectMapper().valueToTree(message);

        assertThat(json.get("tool_calls")).isNotNull();
        assertThat(json.get("tool_calls").get(0).get("id").asText()).isEqualTo("call_abc123");
        // A pure tool-request assistant message carries no tool_call_id itself.
        assertThat(json.get("tool_call_id")).isNull();
    }

    /**
     * Non-tool messages must not emit a {@code tool_call_id} field at all - an explicit
     * {@code null} would be rejected by strict validators.
     */
    @Test
    void thatPlainMessageOmitsToolCallId() throws Exception
    {
        var message = new ChatCompletionMessage("user", "Hello", emptyList(), null);

        var json = JSONUtil.getObjectMapper().valueToTree(message);

        assertThat(json.get("tool_call_id")).isNull();
    }
}

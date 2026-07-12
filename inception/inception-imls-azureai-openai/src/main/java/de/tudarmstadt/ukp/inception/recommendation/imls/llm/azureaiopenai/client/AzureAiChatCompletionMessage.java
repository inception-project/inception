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

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureAiChatCompletionMessage
{
    private @JsonProperty("role") String role;
    private @JsonProperty("content") String content;
    private @JsonInclude(NON_EMPTY) @JsonProperty("tool_calls") List<AzureAiChatCompletionToolCall> toolCalls;
    private @JsonInclude(NON_NULL) @JsonProperty("tool_call_id") String toolCallId;

    public AzureAiChatCompletionMessage()
    {
        // No args
    }

    public AzureAiChatCompletionMessage(String aRole, String aContent)
    {
        role = aRole;
        content = aContent;
    }

    public AzureAiChatCompletionMessage(String aRole, String aContent,
            List<AzureAiChatCompletionToolCall> aToolCalls, String aToolCallId)
    {
        role = aRole;
        content = aContent;
        toolCalls = aToolCalls;
        toolCallId = aToolCallId;
    }

    public String getRole()
    {
        return role;
    }

    public void setRole(String aRole)
    {
        role = aRole;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String aContent)
    {
        content = aContent;
    }

    public List<AzureAiChatCompletionToolCall> getToolCalls()
    {
        return toolCalls;
    }

    public void setToolCalls(List<AzureAiChatCompletionToolCall> aToolCalls)
    {
        toolCalls = aToolCalls;
    }

    public String getToolCallId()
    {
        return toolCallId;
    }

    public void setToolCallId(String aToolCallId)
    {
        toolCallId = aToolCallId;
    }
}

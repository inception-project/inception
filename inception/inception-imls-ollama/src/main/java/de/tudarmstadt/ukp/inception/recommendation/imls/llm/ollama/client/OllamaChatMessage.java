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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.util.Collections.emptyList;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @param role
 *            The role of the message: {@code system}, {@code user}, {@code assistant}, or
 *            {@code tool}.
 * @param content
 *            The content of the message.
 * @param thinking
 *            (for thinking models) the model's thinking process
 * @param toolName
 *            (optional): add the name of the tool that was executed to inform the model of the
 *            result
 * @param toolCalls
 *            (optional): a list of tools in JSON that the model wants to use
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaChatMessage( //
        @JsonProperty("role") String role, //
        @JsonProperty("content") @JsonInclude(NON_NULL) String content, //
        @JsonProperty("thinking") @JsonInclude(NON_NULL) String thinking, //
        @JsonProperty("tool_name") @JsonInclude(NON_NULL) String toolName, //
        @JsonProperty("tool_calls") @JsonInclude(NON_EMPTY) List<OllamaToolCall> toolCalls)
{
    public OllamaChatMessage(String aRole, String aContent)
    {
        this(aRole, aContent, null, null, emptyList());
    }

    public OllamaChatMessage(String aRole, String aContent, String aThinking, String aTooName)
    {
        this(aRole, aContent, aThinking, aTooName, emptyList());
    }
}

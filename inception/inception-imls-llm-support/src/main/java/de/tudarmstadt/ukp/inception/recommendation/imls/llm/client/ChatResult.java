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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.client;

import static java.util.Collections.emptyList;

import java.util.List;

import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ChatMessage;

/**
 * Result of a single chat exchange.
 *
 * @param message
 *            the assistant message returned by the model (may have empty content if the model only
 *            requested tool calls)
 * @param toolCalls
 *            tool invocations requested by the model; empty when none
 * @param finishReason
 *            why the model stopped, or {@code null} if the provider did not report one
 * @param usage
 *            token usage reported by the provider, or {@code null} if unavailable
 */
public record ChatResult( //
        ChatMessage message, //
        List<ToolCall> toolCalls, //
        FinishReason finishReason, //
        UsageInfo usage)
{
    public static ChatResult of(ChatMessage aMessage)
    {
        return new ChatResult(aMessage, emptyList(), null, null);
    }
}

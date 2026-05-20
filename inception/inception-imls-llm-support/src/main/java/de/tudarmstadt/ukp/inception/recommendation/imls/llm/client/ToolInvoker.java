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

import tools.jackson.databind.JsonNode;

/**
 * Dispatch handle for one tool the LLM may call: pairs the wire-side {@link ToolDescriptor} with
 * the logic that runs when the model picks the tool. Implementations are caller-defined since the
 * dispatch typically depends on caller-specific runtime context (e.g. the assistant binds
 * {@code Project}/{@code SourceDocument}/... captured at construction time). Registered into a
 * {@link ToolRegistry} and looked up by name when a {@link ToolCall} comes back from the model.
 */
public interface ToolInvoker
{
    /**
     * The provider-neutral, wire-side description of the tool this invoker handles. Sent to the
     * model via {@link ChatOptions#tools()}.
     */
    ToolDescriptor descriptor();

    /**
     * Run the tool.
     *
     * @param aArguments
     *            arguments the model supplied; typically an {@code ObjectNode} matching the tool's
     *            parameter schema. May be {@code null} if the tool has no parameters.
     * @return the value the tool produced; the caller decides how to map it into the next
     *         conversation turn.
     */
    Object invoke(JsonNode aArguments) throws Exception;
}

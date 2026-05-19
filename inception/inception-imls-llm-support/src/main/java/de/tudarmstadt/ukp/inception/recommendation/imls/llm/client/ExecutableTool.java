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
 * A tool the LLM may call, paired with the dispatch logic for actually invoking it. Concrete
 * implementations adapt different tool sources:
 * <ul>
 * <li>{@link MethodTool} for {@code @Tool}-annotated Java methods whose parameters are all
 * {@code @ToolParam}-annotated (model-supplied arguments only, no runtime injection).
 * <li>Caller-specific impls when the dispatch needs runtime context the abstraction layer should
 * not know about (e.g. the assistant binds {@code Project}/{@code SourceDocument}/...). Such impls
 * typically capture their context at construction time, since the registry is rebuilt per chat
 * turn.
 * <li>Future: an MCP-proxy tool that round-trips invocations through an MCP transport.
 * </ul>
 * Registered into a {@link ToolRegistry} and looked up by name when a {@link ToolCall} is returned
 * by the model.
 */
public interface ExecutableTool
{
    /**
     * The provider-neutral, wire-side description of this tool. Sent to the model via
     * {@link ChatOptions#tools()}.
     */
    ToolDescriptor descriptor();

    /**
     * Dispatch the tool.
     *
     * @param aArguments
     *            arguments the model supplied; typically an {@code ObjectNode} matching the tool's
     *            parameter schema. May be {@code null} if the tool has no parameters.
     * @return the value the tool produced; the caller decides how to map it into the next
     *         conversation turn.
     */
    Object invoke(JsonNode aArguments) throws Exception;
}

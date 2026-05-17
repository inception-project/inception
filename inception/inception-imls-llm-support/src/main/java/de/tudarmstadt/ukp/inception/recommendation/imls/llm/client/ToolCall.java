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
 * An invocation requested by the model. Returned as part of {@link ChatResult#toolCalls()} when the
 * model decides to call a tool. The provider-specific {@code id} (when present) must be echoed back
 * with the tool result so the model can correlate.
 *
 * @param id
 *            provider-assigned call id, may be {@code null} for providers that do not use one
 * @param name
 *            function name as declared via {@link ToolDescriptor#name()}
 * @param arguments
 *            JSON object with the arguments the model wants to pass
 */
public record ToolCall( //
        String id, //
        String name, //
        JsonNode arguments)
{}

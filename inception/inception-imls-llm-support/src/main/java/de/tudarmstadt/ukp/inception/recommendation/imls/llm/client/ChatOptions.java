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
import static java.util.Collections.emptyMap;

import java.util.List;
import java.util.Map;

import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response.ResponseFormat;
import tools.jackson.databind.JsonNode;

/**
 * Per-call generation parameters for {@link LlmChatClient#chat}. Provider-specific knobs
 * (temperature, top_p, seed, ...) ride in {@link #options}.
 *
 * @param responseFormat
 *            requested response format, or {@code null} for unconstrained
 * @param jsonSchema
 *            JSON schema for structured output; honored only when supported by the provider
 * @param tools
 *            tools the model may call; empty list disables tool calling
 * @param options
 *            free-form provider-specific options (temperature, top_p, seed, ...)
 */
public record ChatOptions( //
        ResponseFormat responseFormat, //
        JsonNode jsonSchema, //
        List<ToolDescriptor> tools, //
        Map<String, Object> options)
{
    public static ChatOptions defaults()
    {
        return new ChatOptions(null, null, emptyList(), emptyMap());
    }
}

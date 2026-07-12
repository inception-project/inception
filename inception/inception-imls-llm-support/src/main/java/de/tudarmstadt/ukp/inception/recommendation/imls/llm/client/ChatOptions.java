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
import static java.util.Collections.unmodifiableMap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response.ResponseFormat;
import tools.jackson.databind.JsonNode;

/**
 * Per-call generation parameters for {@link LlmChatClient#chat}. Provider-neutral knobs that most
 * providers share ({@link #temperature}, {@link #topP}, {@link #topK}, {@link #repeatPenalty},
 * {@link #contextLength}) are first-class fields; anything without a neutral equivalent rides in
 * {@link #options}.
 * <p>
 * Each adapter is responsible for translating the neutral fields into its backend's parameters (the
 * wire name is the adapter's to decide, not assumed here) and for letting matching {@link #options}
 * entries override them.
 *
 * @param responseFormat
 *            requested response format, or {@code null} for unconstrained
 * @param jsonSchema
 *            JSON schema for structured output; honored only when supported by the provider
 * @param tools
 *            tools the model may call; empty list disables tool calling
 * @param options
 *            free-form provider-specific options; keys must match the provider's wire API. Override
 *            the typed fields on key collision.
 * @param temperature
 *            sampling temperature, or {@code null} to leave the provider default
 * @param topP
 *            nucleus-sampling threshold, or {@code null} to leave the provider default
 * @param topK
 *            top-k sampling limit, or {@code null} to leave the provider default
 * @param repeatPenalty
 *            penalty applied to repeated tokens, or {@code null} to leave the provider default
 * @param contextLength
 *            size of the context window to use, or {@code null} to leave the provider default
 */
public record ChatOptions( //
        ResponseFormat responseFormat, //
        JsonNode jsonSchema, //
        List<ToolDescriptor> tools, //
        Map<String, Object> options, //
        Double temperature, //
        Double topP, //
        Integer topK, //
        Double repeatPenalty, //
        Integer contextLength)
{
    public ChatOptions
    {
        // Defensively copy so the record owns immutable collections regardless of how it was built
        // (canonical/convenience constructor or builder) and cannot be mutated through a retained
        // caller reference.
        tools = tools != null ? List.copyOf(tools) : emptyList();
        options = options != null ? unmodifiableMap(new LinkedHashMap<>(options)) : emptyMap();
    }

    public ChatOptions(ResponseFormat aResponseFormat, JsonNode aJsonSchema,
            List<ToolDescriptor> aTools, Map<String, Object> aOptions)
    {
        this(aResponseFormat, aJsonSchema, aTools, aOptions, null, null, null, null, null);
    }

    public static ChatOptions defaults()
    {
        return new ChatOptions(null, null, emptyList(), emptyMap());
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private ResponseFormat responseFormat;
        private JsonNode jsonSchema;
        private List<ToolDescriptor> tools = emptyList();
        private Map<String, Object> options = emptyMap();
        private Double temperature;
        private Double topP;
        private Integer topK;
        private Double repeatPenalty;
        private Integer contextLength;

        private Builder()
        {
        }

        public Builder withResponseFormat(ResponseFormat aResponseFormat)
        {
            responseFormat = aResponseFormat;
            return this;
        }

        public Builder withJsonSchema(JsonNode aJsonSchema)
        {
            jsonSchema = aJsonSchema;
            return this;
        }

        public Builder withTools(List<ToolDescriptor> aTools)
        {
            tools = aTools != null ? aTools : emptyList();
            return this;
        }

        public Builder withOptions(Map<String, Object> aOptions)
        {
            options = aOptions != null ? aOptions : emptyMap();
            return this;
        }

        public Builder withTemperature(Double aTemperature)
        {
            temperature = aTemperature;
            return this;
        }

        public Builder withTopP(Double aTopP)
        {
            topP = aTopP;
            return this;
        }

        public Builder withTopK(Integer aTopK)
        {
            topK = aTopK;
            return this;
        }

        public Builder withRepeatPenalty(Double aRepeatPenalty)
        {
            repeatPenalty = aRepeatPenalty;
            return this;
        }

        public Builder withContextLength(Integer aContextLength)
        {
            contextLength = aContextLength;
            return this;
        }

        public ChatOptions build()
        {
            return new ChatOptions(responseFormat, jsonSchema, tools, options, temperature, topP,
                    topK, repeatPenalty, contextLength);
        }
    }
}

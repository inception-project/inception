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
package de.tudarmstadt.ukp.inception.assistant.model;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolUtils;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

@JsonTypeName(MCallResponse.TYPE_CALL_RESPONSE)
public record MCallResponse<T>(UUID id, String role, String actor, boolean internal,
        boolean ephemeral, MPerformanceMetrics performance, List<MReference> references,
        String toolName, UUID context, Map<String, Object> arguments, T payload)
    implements MChatMessage
{

    static final String TYPE_CALL_RESPONSE = "callResponse";

    private MCallResponse(Builder<T> aBuilder)
    {
        this(aBuilder.id, aBuilder.role, aBuilder.actor, aBuilder.internal, aBuilder.ephemeral,
                aBuilder.performance, aBuilder.references.values().stream().toList(),
                aBuilder.toolName, aBuilder.context, aBuilder.arguments, aBuilder.payload);
    }

    @JsonProperty(MMessage.TYPE_FIELD)
    public String getType()
    {
        return TYPE_CALL_RESPONSE;
    }

    public static <T> Builder<T> builder(Class<T> aType)
    {
        return new Builder<>();
    }

    @Override
    public String textRepresentation()
    {
        try {
            return JSONUtil.toJsonString(payload);
        }
        catch (IOException e) {
            return "ERROR";
        }
    }

    public static final class Builder<T>
    {
        private UUID id;
        private String actor;
        private String role;
        private boolean internal = false;
        private boolean ephemeral = false;
        private MPerformanceMetrics performance;
        private final Map<String, MReference> references = new LinkedHashMap<>();
        private final Map<String, Object> arguments = new LinkedHashMap<>();
        private String toolName;
        private T payload;
        private UUID context;

        private Builder()
        {
        }

        public Builder<T> withId(UUID aId)
        {
            id = aId;
            return this;
        }

        public Builder<T> internal()
        {
            internal = true;
            return this;
        }

        public Builder<T> ephemeral()
        {
            ephemeral = true;
            return this;
        }

        public Builder<T> withRole(String aRole)
        {
            role = aRole;
            return this;
        }

        public Builder<T> withActor(String aActor)
        {
            actor = aActor;
            return this;
        }

        public Builder<T> withPayload(T aPayload)
        {
            payload = aPayload;
            return this;
        }

        public Builder<T> withPerformance(MPerformanceMetrics aPerformance)
        {
            performance = aPerformance;
            return this;
        }

        public Builder<T> withContext(UUID aContext)
        {
            context = aContext;
            return this;
        }

        public Builder<T> withReferences(MReference... aReferences)
        {
            if (aReferences != null) {
                for (var ref : aReferences) {
                    references.put(ref.id(), ref);
                }
            }
            return this;
        }

        public Builder<T> withReferences(Iterable<MReference> aReferences)
        {
            if (aReferences != null) {
                for (var ref : aReferences) {
                    references.put(ref.id(), ref);
                }
            }
            return this;
        }

        public Builder<T> withToolCall(MToolCall aToolCall)
        {
            arguments.clear();

            if (aToolCall == null) {
                toolName = null;
                return this;
            }

            toolName = ToolUtils.getFunctionName(aToolCall.method());
            for (var arg : aToolCall.arguments().entrySet()) {
                arguments.put(arg.getKey(),
                        arg.getValue() != null ? arg.getValue().toString() : null);
            }

            return this;
        }

        public MCallResponse<T> build()
        {
            if (id == null) {
                id = UUID.randomUUID();
            }

            return new MCallResponse<>(this);
        }
    }
}

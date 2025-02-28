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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MCallResponse<T>(UUID id, String role, String actor, boolean internal,
        boolean ephemeral, MPerformanceMetrics performance, List<MReference> references, T payload)
    implements MChatMessage
{

    static final String TYPE_TEXT_MESSAGE = "callResponse";

    private MCallResponse(Builder<T> aBuilder)
    {
        this(aBuilder.id, aBuilder.role, aBuilder.actor, aBuilder.internal, aBuilder.ephemeral,
                aBuilder.performance, aBuilder.references.values().stream().toList(),
                aBuilder.payload);
    }

    @JsonProperty(MMessage.TYPE_FIELD)
    public String getType()
    {
        return TYPE_TEXT_MESSAGE;
    }

    public static <T> Builder<T> builder(Class<T> aType)
    {
        return new Builder<>();
    }

    public static final class Builder<T>
    {
        private UUID id;
        private String actor;
        private String role;
        private boolean internal = false;
        private boolean ephemeral = false;
        private MPerformanceMetrics performance;
        private Map<String, MReference> references = new LinkedHashMap<>();
        private T payload;

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

        public MCallResponse<T> build()
        {
            if (id == null) {
                id = UUID.randomUUID();
            }

            return new MCallResponse<>(this);
        }
    }

}

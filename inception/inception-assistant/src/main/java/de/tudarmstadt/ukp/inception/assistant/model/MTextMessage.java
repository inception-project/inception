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

import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.Validate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * @param id
 *            the ID of the message, important when streaming as the UI needs to concatenate all
 *            fragments with the same ID
 * @param done
 *            when streaming indicates if the fragment is the last fragment of the message
 * @param message
 *            the message or message fragment
 * @param role
 *            the message role
 * @param actor
 *            name of the actor sending the message
 * @param internal
 *            if the message is part of the inner monolog, RAG or similarly not normally exposed to
 *            the user
 * @param performance
 *            optional performance metrics
 */
@JsonTypeName(MTextMessage.TYPE_TEXT_MESSAGE)
public record MTextMessage(UUID id, String role, String actor, String message, boolean done, boolean internal,
        MPerformanceMetrics performance)
    implements MChatMessage
{
    static final String TYPE_TEXT_MESSAGE = "textMessage";

    private MTextMessage(Builder aBuilder)
    {
        this(aBuilder.id, aBuilder.role, aBuilder.actor, aBuilder.message, aBuilder.done, aBuilder.internal,
                aBuilder.performance);
    }

    public MTextMessage append(MTextMessage aMessage)
    {
        Objects.nonNull(id());
        Objects.nonNull(id());
        Validate.isTrue(Objects.equals(aMessage.id(), id()));
        Validate.isTrue(Objects.equals(aMessage.role(), role()));
        Validate.isTrue(Objects.equals(aMessage.internal(), internal()));
        Validate.isTrue(!done());

        var perf = performance() != null ? performance().merge(aMessage.performance())
                : aMessage.performance();

        return new MTextMessage(id(), role(), actor(), message() + aMessage.message(), aMessage.done(),
                internal(), perf);
    }

    @JsonProperty(MMessage.TYPE_FIELD)
    public String getType()
    {
        return TYPE_TEXT_MESSAGE;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private UUID id;
        private String actor;
        private String role;
        private String message;
        private boolean done = true;
        private boolean internal = false;
        private MPerformanceMetrics performance;

        private Builder()
        {
        }

        public Builder withId(UUID aId)
        {
            id = aId;
            return this;
        }

        public Builder done()
        {
            done = true;
            return this;
        }

        public Builder notDone()
        {
            done = false;
            return this;
        }

        public Builder internal()
        {
            internal = true;
            return this;
        }

        public Builder withRole(String aRole)
        {
            role = aRole;
            return this;
        }

        public Builder withActor(String aActor)
        {
            actor = aActor;
            return this;
        }

        public Builder withMessage(String aMessage)
        {
            message = aMessage;
            return this;
        }

        public Builder withPerformance(MPerformanceMetrics aPerformance)
        {
            performance = aPerformance;
            return this;
        }

        public MTextMessage build()
        {
            if (id == null) {
                id = UUID.randomUUID();
            }

            return new MTextMessage(this);
        }
    }
}

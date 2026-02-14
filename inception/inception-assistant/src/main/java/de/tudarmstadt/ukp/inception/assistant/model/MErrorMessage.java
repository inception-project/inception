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

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static de.tudarmstadt.ukp.inception.assistant.model.MChatRoles.SYSTEM;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * @param id
 *            the ID of the message, important when streaming as the UI needs to concatenate all
 *            fragments with the same ID
 * @param content
 *            the message or message fragment
 * @param role
 *            the message role
 * @param actor
 *            name of the actor sending the message
 */
@JsonTypeName(MErrorMessage.TYPE_ERROR_MESSAGE)
public record MErrorMessage( //
        UUID id, //
        String role, //
        String actor, //
        @JsonInclude(NON_NULL) String content)//
    implements MChatMessage
{
    static final String TYPE_ERROR_MESSAGE = "errorMessage";

    private MErrorMessage(Builder aBuilder)
    {
        this(aBuilder.id, aBuilder.role, aBuilder.actor, aBuilder.content);
    }

    @JsonProperty(MMessage.TYPE_FIELD)
    public String getType()
    {
        return TYPE_ERROR_MESSAGE;
    }

    @Override
    public String textRepresentation()
    {
        return content;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    @Override
    public MPerformanceMetrics performance()
    {
        return null;
    }

    @Override
    public boolean internal()
    {
        return false;
    }

    @Override
    public boolean ephemeral()
    {
        return false;
    }

    @Override
    public UUID context()
    {
        return null;
    }

    @Override
    public List<MReference> references()
    {
        return Collections.emptyList();
    }

    public static final class Builder
    {
        private UUID id;
        private String actor = "Error";
        private String role = SYSTEM;
        private String content;

        private Builder()
        {
        }

        public Builder withId(UUID aId)
        {
            id = aId;
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

        public Builder withContent(String aContent)
        {
            content = aContent;
            return this;
        }

        public MErrorMessage build()
        {
            if (id == null) {
                id = UUID.randomUUID();
            }

            return new MErrorMessage(this);
        }
    }
}

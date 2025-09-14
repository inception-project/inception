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
import static java.util.Collections.emptyList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.Validate;

import com.fasterxml.jackson.annotation.JsonInclude;
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
 * @param thinking
 *            the thoughts or though fragment
 * @param role
 *            the message role
 * @param actor
 *            name of the actor sending the message
 * @param internal
 *            if the message is part of the inner monolog, RAG or similarly not normally exposed to
 *            the user
 * @param ephemeral
 *            if the message should disappear at once (i.e. it is not recorded)
 * @param performance
 *            optional performance metrics
 * @param references
 *            optional list of references
 * @param context
 *            ID of a previous message this message relates to. The message should be rendered after
 *            the context message.
 * 
 */
@JsonTypeName(MTextMessage.TYPE_TEXT_MESSAGE)
public record MTextMessage( //
        UUID id, //
        String role, //
        String actor, //
        @JsonInclude(NON_NULL) String message, //
        @JsonInclude(NON_NULL) String thinking, //
        boolean done, //
        boolean internal, //
        boolean ephemeral, //
        MPerformanceMetrics performance, //
        List<MReference> references, //
        @JsonInclude(NON_NULL) UUID context)//
    implements MChatMessage
{
    static final String TYPE_TEXT_MESSAGE = "textMessage";

    private MTextMessage(Builder aBuilder)
    {
        this(aBuilder.id, aBuilder.role, aBuilder.actor, aBuilder.message, aBuilder.thinking,
                aBuilder.done, aBuilder.internal, aBuilder.ephemeral, aBuilder.performance,
                aBuilder.references.values().stream().toList(), aBuilder.context);
    }

    public MTextMessage append(MTextMessage aMessage)
    {
        Objects.requireNonNull(id());
        Validate.isTrue(Objects.equals(aMessage.id(), id()));
        Validate.isTrue(Objects.equals(aMessage.role(), role()));
        Validate.isTrue(Objects.equals(aMessage.internal(), internal()));
        Validate.isTrue(Objects.equals(aMessage.ephemeral(), ephemeral()));
        Validate.isTrue(!done());

        if (context() != null) {
            Objects.requireNonNull(aMessage.context());
            Validate.isTrue(Objects.equals(context(), aMessage.context()));
        }

        var perf = performance() != null //
                ? performance().merge(aMessage.performance()) //
                : aMessage.performance();

        var refs = new LinkedHashMap<String, MReference>();
        if (references() != null) {
            references().forEach(r -> refs.put(r.id(), r));
        }
        if (aMessage.references() != null) {
            aMessage.references().forEach(r -> refs.put(r.id(), r));
        }

        var msg = new StringBuilder();
        if (message() != null) {
            msg.append(message());
        }
        if (aMessage.message() != null) {
            msg.append(aMessage.message());
        }

        var tnk = new StringBuilder();
        if (thinking() != null) {
            tnk.append(thinking());
        }
        if (aMessage.thinking() != null) {
            tnk.append(aMessage.thinking());
        }

        return new MTextMessage(id(), role(), actor(), msg.toString(), tnk.toString(),
                aMessage.done(), internal(), ephemeral(), perf, refs.values().stream().toList(),
                context());
    }

    public MTextMessage appendReferences(List<MReference> aReferences)
    {
        if (aReferences == null || aReferences.isEmpty()) {
            return this;
        }

        var refs = new LinkedHashMap<String, MReference>();
        if (references() != null) {
            references().forEach(r -> refs.put(r.id(), r));
        }
        aReferences.forEach(r -> refs.put(r.id(), r));

        return new MTextMessage(id(), role(), actor(), message(), thinking(), done(), internal(),
                ephemeral(), performance(), refs.values().stream().toList(), context());
    }

    public MTextMessage withoutContent()
    {
        return new MTextMessage(id(), role(), actor(), "", "", done(), internal(), ephemeral(),
                performance(), emptyList(), context());
    }

    @JsonProperty(MMessage.TYPE_FIELD)
    public String getType()
    {
        return TYPE_TEXT_MESSAGE;
    }

    @Override
    public String textRepresentation()
    {
        return message;
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
        private String thinking;
        private boolean done = true;
        private boolean internal = false;
        private boolean ephemeral = false;
        private MPerformanceMetrics performance;
        private Map<String, MReference> references = new LinkedHashMap<>();
        private UUID context;

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

        public Builder withInternal(boolean aInternal)
        {
            internal = aInternal;
            return this;
        }

        public Builder ephemeral()
        {
            ephemeral = true;
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

        public Builder withThinking(String aThinking)
        {
            thinking = aThinking;
            return this;
        }

        public Builder withPerformance(MPerformanceMetrics aPerformance)
        {
            performance = aPerformance;
            return this;
        }

        public Builder withReferences(MReference... aReferences)
        {
            if (aReferences != null) {
                for (var ref : aReferences) {
                    references.put(ref.id(), ref);
                }
            }
            return this;
        }

        public Builder withReferences(Iterable<MReference> aReferences)
        {
            if (aReferences != null) {
                for (var ref : aReferences) {
                    references.put(ref.id(), ref);
                }
            }
            return this;
        }

        public Builder withContext(UUID aContext)
        {
            context = aContext;
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

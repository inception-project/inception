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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A single chat message exchanged with the LLM.
 *
 * @param role
 *            who is speaking
 * @param content
 *            the message text; may be empty for assistant messages that only carry tool calls
 * @param thinking
 *            reasoning / chain-of-thought emitted by the model alongside {@code content}, when the
 *            model exposes it; {@code null} otherwise
 * @param toolCallId
 *            for {@link Role#TOOL} messages, the id of the call this is the result of (set by the
 *            provider in the preceding assistant turn); {@code null} for all other roles
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatMessage(Role role, String content, String thinking, String toolCallId) {
    public ChatMessage(Role aRole, String aContent)
    {
        this(aRole, aContent, null, null);
    }

    public static enum Role
    {
        SYSTEM("system"), //
        ASSISTANT("assistant"), //
        USER("user"), //
        TOOL("tool");

        private final String name;

        private Role(String aName)
        {
            name = aName;
        }

        public String getName()
        {
            return name;
        }
    }
}

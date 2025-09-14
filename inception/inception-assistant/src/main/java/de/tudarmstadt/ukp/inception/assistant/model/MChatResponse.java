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

import java.util.ArrayList;
import java.util.List;

public record MChatResponse(MTextMessage message, List<MToolCall> toolCalls) {

    // static final String TYPE_TEXT_MESSAGE = "toolOfferResponse";

    private MChatResponse(Builder aBuilder)
    {
        this(aBuilder.message, new ArrayList<>(aBuilder.toolCalls));
    }

    // @JsonProperty(MMessage.TYPE_FIELD)
    // public String getType()
    // {
    // return TYPE_TEXT_MESSAGE;
    // }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private MTextMessage message;
        private final List<MToolCall> toolCalls = new ArrayList<>();

        private Builder()
        {
        }

        public Builder withMessage(MTextMessage aMessage)
        {
            message = aMessage;
            return this;
        }

        public Builder withToolCalls(List<MToolCall> aToolCalls)
        {
            toolCalls.clear();
            if (aToolCalls != null) {
                toolCalls.addAll(aToolCalls);
            }
            return this;
        }

        public MChatResponse build()
        {
            return new MChatResponse(this);
        }
    }
}

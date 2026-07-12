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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One entry in the OpenAI request {@code tools[]} array:
 * {@code {"type":"function","function":{...}}}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatCompletionTool
{
    private final @JsonProperty("type") String type;
    private final @JsonProperty("function") ChatCompletionFunction function;

    private ChatCompletionTool(Builder aBuilder)
    {
        type = aBuilder.type;
        function = aBuilder.function;
    }

    public String getType()
    {
        return type;
    }

    public ChatCompletionFunction getFunction()
    {
        return function;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private String type = "function";
        private ChatCompletionFunction function;

        private Builder()
        {
        }

        public Builder withType(String aType)
        {
            type = aType;
            return this;
        }

        public Builder withFunction(ChatCompletionFunction aFunction)
        {
            function = aFunction;
            return this;
        }

        public ChatCompletionTool build()
        {
            return new ChatCompletionTool(this);
        }
    }
}

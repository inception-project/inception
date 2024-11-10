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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.azureaiopenai.client;

import static java.util.Arrays.asList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.traits.Option;

public class ChatCompletionRequest
{
    // See https://platform.openai.com/docs/api-reference/chat/create
    public static final Option<Integer> MAX_TOKENS = new Option<>(Integer.class, "max_tokens");
    public static final Option<Integer> SEED = new Option<>(Integer.class, "seed");
    public static final Option<Integer> N = new Option<>(Integer.class, "n");

    public static List<Option<?>> getAllOptions()
    {
        return asList(MAX_TOKENS, SEED, N);
    }

    private final @JsonIgnore String apiKey;
    private final @JsonIgnore String model;
    private final List<ChatCompletionMessage> messages;
    private final @JsonInclude(Include.NON_NULL) GenerateResponseFormat format;

    private ChatCompletionRequest(Builder builder)
    {
        messages = asList(new ChatCompletionMessage("user", builder.prompt));
        format = builder.format;
        model = builder.model;
        apiKey = builder.apiKey;
    }

    public String getApiKey()
    {
        return apiKey;
    }

    public String getModel()
    {
        return model;
    }

    public GenerateResponseFormat getFormat()
    {
        return format;
    }

    public List<ChatCompletionMessage> getMessages()
    {
        return messages;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private String model;
        private String apiKey;
        private String prompt;
        private GenerateResponseFormat format;
        private Map<String, Object> options = new HashMap<>();

        private Builder()
        {
        }

        public Builder withModel(String aModel)
        {
            this.model = aModel;
            return this;
        }

        public Builder withApiKey(String aApiKey)
        {
            this.apiKey = aApiKey;
            return this;
        }

        public Builder withPrompt(String aPrompt)
        {
            this.prompt = aPrompt;
            return this;
        }

        public Builder withFormat(GenerateResponseFormat aFormat)
        {
            this.format = aFormat;
            return this;
        }

        public <T> Builder withOption(Option<T> aOption, T aValue)
        {
            if (aValue != null) {
                this.options.put(aOption.getName(), aValue);
            }
            else {
                this.options.remove(aOption.getName());
            }
            return this;
        }

        public ChatCompletionRequest build()
        {
            return new ChatCompletionRequest(this);
        }
    }
}

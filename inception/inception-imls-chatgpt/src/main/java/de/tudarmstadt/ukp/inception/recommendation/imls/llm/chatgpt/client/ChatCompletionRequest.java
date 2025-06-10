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

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.traits.Option;

public class ChatCompletionRequest
{
    // See https://platform.openai.com/docs/api-reference/chat/create
    public static final Option<Integer> MAX_TOKENS = new Option<>(Integer.class, "max_tokens");
    public static final Option<Integer> SEED = new Option<>(Integer.class, "seed");
    public static final Option<Integer> N = new Option<>(Integer.class, "n");
    public static final Option<Double> TEMPERATURE = new Option<>(Double.class, "temperature");
    public static final Option<Double> TOP_P = new Option<>(Double.class, "top_p");

    public static List<Option<?>> getAllOptions()
    {
        return asList(SEED, TEMPERATURE, TOP_P);
    }

    private final @JsonIgnore String apiKey;
    private final String model;
    private final List<ChatCompletionMessage> messages;
    private final @JsonProperty("response_format") @JsonInclude(NON_NULL) ChatGptResponseFormat format;
    private final @JsonInclude(NON_NULL) @JsonProperty("temperature") Double temperature;
    private final @JsonInclude(NON_NULL) @JsonProperty("top_p") Double topP;
    private final @JsonInclude(NON_NULL) @JsonProperty("seed") Integer seed;

    private ChatCompletionRequest(Builder builder)
    {
        messages = builder.messages;
        format = builder.format;
        model = builder.model;
        apiKey = builder.apiKey;
        temperature = TEMPERATURE.get(builder.options);
        seed = SEED.get(builder.options);
        topP = TOP_P.get(builder.options);
    }

    public String getApiKey()
    {
        return apiKey;
    }

    public String getModel()
    {
        return model;
    }

    public ChatGptResponseFormat getFormat()
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
        private ChatGptResponseFormat format;
        private final List<ChatCompletionMessage> messages = new ArrayList<>();
        private final Map<Option<?>, Object> options = new HashMap<>();

        private Builder()
        {
        }

        public Builder withModel(String aModel)
        {
            model = aModel;
            return this;
        }

        public Builder withApiKey(String aApiKey)
        {
            apiKey = aApiKey;
            return this;
        }

        public Builder withPrompt(String aPrompt)
        {
            withMessages(new ChatCompletionMessage("user", aPrompt));
            return this;
        }

        public Builder withMessages(Collection<ChatCompletionMessage> aChatCompletionMessages)
        {
            messages.clear();
            if (aChatCompletionMessages != null) {
                messages.addAll(aChatCompletionMessages);
            }
            return this;
        }

        public Builder withMessages(ChatCompletionMessage... aChatCompletionMessages)
        {
            messages.clear();
            addMessages(aChatCompletionMessages);
            return this;
        }

        public Builder addMessages(ChatCompletionMessage... aChatCompletionMessages)
        {
            if (aChatCompletionMessages != null) {
                messages.addAll(asList(aChatCompletionMessages));
            }
            return this;
        }

        public Builder withResponseFormat(ChatGptResponseFormat aFormat)
        {
            format = aFormat;
            return this;
        }

        public <T> Builder withOption(Option<T> aOption, T aValue)
        {
            if (aValue != null) {
                options.put(aOption, aValue);
            }
            else {
                options.remove(aOption);
            }
            return this;
        }

        public <T> Builder withExtraOptions(Map<String, Object> aOptions)
        {
            if (aOptions != null) {
                for (var setting : aOptions.entrySet()) {
                    var opt = getAllOptions().stream()
                            .filter(o -> o.getName().equals(setting.getKey())).findFirst();
                    if (opt.isPresent()) {
                        withOption((Option) opt.get(), setting.getValue());
                    }
                }
            }

            return this;
        }

        public ChatCompletionRequest build()
        {
            return new ChatCompletionRequest(this);
        }
    }
}

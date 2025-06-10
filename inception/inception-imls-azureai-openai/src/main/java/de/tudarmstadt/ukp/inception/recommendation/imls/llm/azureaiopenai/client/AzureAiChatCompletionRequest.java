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

import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.traits.DoubleOption;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.traits.Option;

public class AzureAiChatCompletionRequest
{
    // See https://platform.openai.com/docs/api-reference/chat/create
    public static final Option<Integer> MAX_TOKENS = new Option<>(Integer.class, "max_tokens");
    public static final Option<Integer> SEED = new Option<>(Integer.class, "seed");
    public static final Option<Double> FREQUENCY_PENALTY = new DoubleOption("frequency_penalty",
            -2.0d, 2.0d);
    public static final Option<Double> TEMPERATURE = new DoubleOption("temperature", 0.0d, 2.0d);
    public static final Option<Double> TOP_P = new DoubleOption("top_p", 0.0d, 1.0d);

    public static List<Option<?>> getAllOptions()
    {
        return asList(MAX_TOKENS, SEED, FREQUENCY_PENALTY, TEMPERATURE);
    }

    private final @JsonIgnore String apiKey;
    private final @JsonIgnore String model;

    private final @JsonInclude(NON_NULL) AzureAiGenerateResponseFormat format;
    private final @JsonInclude(NON_NULL) @JsonProperty("frequency_penalty") Double frequencyPenalty;
    private final @JsonInclude(NON_NULL) @JsonProperty("temperature") Double temperature;
    private final @JsonInclude(NON_NULL) @JsonProperty("top_p") Double topP;
    private final @JsonInclude(NON_NULL) @JsonProperty("seed") Integer seed;

    private final List<AzureAiChatCompletionMessage> messages;

    private AzureAiChatCompletionRequest(Builder builder)
    {
        messages = builder.messages;
        model = builder.model;
        apiKey = builder.apiKey;
        format = builder.format;
        frequencyPenalty = FREQUENCY_PENALTY.get(builder.options);
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

    public AzureAiGenerateResponseFormat getFormat()
    {
        return format;
    }

    public List<AzureAiChatCompletionMessage> getMessages()
    {
        return messages;
    }

    public Double getFrequencyPenalty()
    {
        return frequencyPenalty;
    }

    public Double getTemperature()
    {
        return temperature;
    }

    public Double getTopP()
    {
        return topP;
    }

    public Integer getSeed()
    {
        return seed;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private String model;
        private String apiKey;
        private AzureAiGenerateResponseFormat format;
        private Map<Option<?>, Object> options = new HashMap<>();
        private List<AzureAiChatCompletionMessage> messages = new ArrayList<>();

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
            messages.clear();
            messages.add(new AzureAiChatCompletionMessage("user", aPrompt));
            return this;
        }

        public Builder withMessages(AzureAiChatCompletionMessage... aMessages)
        {
            messages.clear();
            if (aMessages != null) {
                messages.addAll(asList(aMessages));
            }
            return this;
        }

        public Builder withMessages(Collection<AzureAiChatCompletionMessage> aMessages)
        {
            messages.clear();
            if (aMessages != null) {
                messages.addAll(aMessages);
            }
            return this;
        }

        public Builder withFormat(AzureAiGenerateResponseFormat aFormat)
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

        public AzureAiChatCompletionRequest build()
        {
            return new AzureAiChatCompletionRequest(this);
        }
    }
}

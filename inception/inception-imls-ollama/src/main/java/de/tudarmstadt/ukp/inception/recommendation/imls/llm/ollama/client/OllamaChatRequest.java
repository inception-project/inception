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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.traits.Option;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OllamaChatRequest
{
    private String model;
    private List<OllamaChatMessage> messages;
    private boolean stream;
    private @JsonInclude(Include.NON_NULL) OllamaGenerateResponseFormat format;
    private @JsonInclude(Include.NON_DEFAULT) boolean raw;
    private @JsonInclude(Include.NON_EMPTY) Map<String, Object> options = new HashMap<>();

    private OllamaChatRequest(Builder builder)
    {
        model = builder.model;
        messages = builder.messages;
        format = builder.format;
        stream = builder.stream;
        raw = builder.raw;
        options = builder.options;
    }

    public OllamaGenerateResponseFormat getFormat()
    {
        return format;
    }

    public String getModel()
    {
        return model;
    }

    public List<OllamaChatMessage> getMessages()
    {
        return messages;
    }

    public boolean isRaw()
    {
        return raw;
    }

    public boolean isStream()
    {
        return stream;
    }

    public Map<String, Object> getOptions()
    {
        return options;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private String model;
        private List<OllamaChatMessage> messages = new ArrayList<>();
        private OllamaGenerateResponseFormat format;
        private boolean raw;
        private boolean stream;
        private Map<String, Object> options = new HashMap<>();

        private Builder()
        {
        }

        public Builder withModel(String aModel)
        {
            model = aModel;
            return this;
        }

        public Builder withMessages(OllamaChatMessage... aMessages)
        {
            if (aMessages != null) {
                messages.addAll(asList(aMessages));
            }
            return this;
        }

        public Builder withMessages(Collection<OllamaChatMessage> aMessages)
        {
            if (aMessages != null) {
                messages.addAll(aMessages);
            }
            return this;
        }

        public Builder withFormat(OllamaGenerateResponseFormat aFormat)
        {
            format = aFormat;
            return this;
        }

        public Builder withStream(boolean aStream)
        {
            stream = aStream;
            return this;
        }

        public Builder withRaw(boolean aRaw)
        {
            raw = aRaw;
            return this;
        }

        public <T> Builder withOption(Option<T> aOption, T aValue)
        {
            if (aValue != null) {
                options.put(aOption.getName(), aValue);
            }
            else {
                options.remove(aOption.getName());
            }
            return this;
        }

        public <T> Builder withOptions(Map<String, Object> aOptions)
        {
            if (aOptions != null) {
                options.putAll(aOptions);
            }
            return this;
        }

        public OllamaChatRequest build()
        {
            return new OllamaChatRequest(this);
        }
    }
}

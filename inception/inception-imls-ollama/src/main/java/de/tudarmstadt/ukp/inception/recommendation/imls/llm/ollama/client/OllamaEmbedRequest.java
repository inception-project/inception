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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.traits.Option;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaEmbedRequest(String model, List<String> input, boolean truncate,
        @JsonInclude(Include.NON_EMPTY) Map<String, Object> options)
{

    private OllamaEmbedRequest(Builder builder)
    {
        this(builder.model, builder.input, builder.truncate, builder.options);
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private String model;
        private boolean truncate = true;
        private List<String> input = new ArrayList<>();
        private Map<String, Object> options = new HashMap<>();

        private Builder()
        {
        }

        public Builder withModel(String aModel)
        {
            model = aModel;
            return this;
        }

        public Builder withInput(String... aInput)
        {
            input.addAll(asList(aInput));
            return this;
        }

        public Builder withInput(List<String> aInput)
        {
            input.addAll(aInput);
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

        public OllamaEmbedRequest build()
        {
            return new OllamaEmbedRequest(this);
        }
    }

}

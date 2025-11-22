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

import com.fasterxml.jackson.annotation.JsonIgnore;

public record OllamaShowRequest(String model, @JsonIgnore String apiKey) {

    private OllamaShowRequest(Builder builder)
    {
        this(builder.model, builder.apiKey);
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private String apiKey;
        private String model;

        private Builder()
        {
        }

        public Builder withApiKey(String aApiKey)
        {
            apiKey = aApiKey;
            return this;
        }

        public Builder withModel(String aModel)
        {
            model = aModel;
            return this;
        }

        public OllamaShowRequest build()
        {
            return new OllamaShowRequest(this);
        }
    }
}

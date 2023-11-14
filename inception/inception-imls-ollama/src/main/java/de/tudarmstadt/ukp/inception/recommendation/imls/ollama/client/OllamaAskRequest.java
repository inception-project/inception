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
package de.tudarmstadt.ukp.inception.recommendation.imls.ollama.client;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class OllamaAskRequest
{
    private String model;
    private String prompt;
    private @JsonInclude(Include.NON_NULL) OllamaResponseFormat format;
    private @JsonInclude(Include.NON_DEFAULT) boolean raw;
    private boolean stream;

    private OllamaAskRequest(Builder builder)
    {
        model = builder.model;
        prompt = builder.prompt;
        format = builder.format;
        stream = builder.stream;
        raw = builder.raw;
    }

    public OllamaResponseFormat getFormat()
    {
        return format;
    }

    public String getModel()
    {
        return model;
    }

    public String getPrompt()
    {
        return prompt;
    }

    public boolean isRaw()
    {
        return raw;
    }

    public boolean isStream()
    {
        return stream;
    }

    @Generated("SparkTools")
    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private String model;
        private String prompt;
        private OllamaResponseFormat format;
        private boolean raw;
        private boolean stream;

        private Builder()
        {
        }

        public Builder withModel(String aModel)
        {
            this.model = aModel;
            return this;
        }

        public Builder withPrompt(String aPrompt)
        {
            this.prompt = aPrompt;
            return this;
        }

        public Builder withFormat(OllamaResponseFormat aFormat)
        {
            this.format = aFormat;
            return this;
        }

        public Builder withStream(boolean aStream)
        {
            this.stream = aStream;
            return this;
        }

        public Builder withRaw(boolean aRaw)
        {
            this.raw = aRaw;
            return this;
        }

        public OllamaAskRequest build()
        {
            return new OllamaAskRequest(this);
        }
    }
}

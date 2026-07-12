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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.JsonNode;

/**
 * The {@code function} object nested inside an Azure OpenAI {@code tools[]} entry: a name, an
 * optional description, and the JSON-schema {@code parameters} passed straight through from the
 * neutral {@code ToolDescriptor.parametersSchema()}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class AzureAiChatCompletionFunction
{
    private final @JsonProperty("name") String name;
    private final @JsonProperty("description") String description;
    private final @JsonProperty("parameters") JsonNode parameters;

    private AzureAiChatCompletionFunction(Builder aBuilder)
    {
        name = aBuilder.name;
        description = aBuilder.description;
        parameters = aBuilder.parameters;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public JsonNode getParameters()
    {
        return parameters;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private String name;
        private String description;
        private JsonNode parameters;

        private Builder()
        {
        }

        public Builder withName(String aName)
        {
            name = aName;
            return this;
        }

        public Builder withDescription(String aDescription)
        {
            description = aDescription;
            return this;
        }

        public Builder withParameters(JsonNode aParameters)
        {
            parameters = aParameters;
            return this;
        }

        public AzureAiChatCompletionFunction build()
        {
            return new AzureAiChatCompletionFunction(this);
        }
    }
}

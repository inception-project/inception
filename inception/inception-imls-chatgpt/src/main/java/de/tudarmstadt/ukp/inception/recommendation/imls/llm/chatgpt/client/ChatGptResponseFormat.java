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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(NON_NULL)
public class ChatGptResponseFormat
{
    private final @JsonProperty("type") ChatGptResponseFormatType type;
    private final @JsonProperty("json_schema") ChatGptSchemaResponseDefinition schema;

    private ChatGptResponseFormat(Builder aBuilder)
    {
        type = aBuilder.type;
        if (aBuilder.schema != null) {
            schema = new ChatGptSchemaResponseDefinition(aBuilder.schemaName, aBuilder.schema);
        }
        else {
            schema = null;
        }
    }

    public ChatGptResponseFormatType getType()
    {
        return type;
    }

    public ChatGptSchemaResponseDefinition getSchema()
    {
        return schema;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private ChatGptResponseFormatType type;
        private String schemaName;
        private JsonNode schema;

        private Builder()
        {
        }

        public Builder withType(ChatGptResponseFormatType aType)
        {
            type = aType;
            return this;
        }

        public Builder withSchema(String aName, JsonNode aSchema)
        {
            schemaName = aName;
            schema = aSchema;
            return this;
        }

        public ChatGptResponseFormat build()
        {
            return new ChatGptResponseFormat(this);
        }
    }
}

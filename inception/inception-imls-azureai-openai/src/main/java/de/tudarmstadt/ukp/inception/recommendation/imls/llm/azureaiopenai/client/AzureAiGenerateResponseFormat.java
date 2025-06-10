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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(NON_NULL)
public class AzureAiGenerateResponseFormat
{
    public static final AzureAiGenerateResponseFormat JSON_OBJECT = AzureAiGenerateResponseFormat
            .builder() //
            .withType(AzureAiResponseFormatType.JSON_OBJECT) //
            .build();

    private final AzureAiResponseFormatType type;
    private final @JsonProperty("json_schema") AzureAiSchemaResponseDefinition schema;

    private AzureAiGenerateResponseFormat(Builder aBuilder)
    {
        type = aBuilder.type;
        if (aBuilder.schema != null) {
            schema = new AzureAiSchemaResponseDefinition(aBuilder.schemaName, aBuilder.schema);
        }
        else {
            schema = null;
        }
    }

    public AzureAiResponseFormatType getType()
    {
        return type;
    }

    public AzureAiSchemaResponseDefinition getSchema()
    {
        return schema;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private AzureAiResponseFormatType type;
        private String schemaName;
        private JsonNode schema;

        private Builder()
        {
        }

        public Builder withType(AzureAiResponseFormatType aType)
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

        public AzureAiGenerateResponseFormat build()
        {
            return new AzureAiGenerateResponseFormat(this);
        }
    }
}

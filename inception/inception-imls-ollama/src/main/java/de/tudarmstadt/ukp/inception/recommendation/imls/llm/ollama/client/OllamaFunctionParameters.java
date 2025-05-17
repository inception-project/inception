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

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

@JsonInclude(NON_EMPTY)
public class OllamaFunctionParameters
{
    private @JsonProperty("type") String type;
    private final @JsonProperty("required") List<String> required = new ArrayList<String>();
    private final @JsonProperty("properties") Map<String, ObjectNode> properties = new LinkedHashMap<>();

    private OllamaFunctionParameters(Builder builder)
    {
        type = builder.type;
        required.addAll(builder.required);
        properties.putAll(builder.properties);
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private String type = "object";
        private final List<String> required = new ArrayList<String>();
        private final Map<String, ObjectNode> properties = new LinkedHashMap<>();

        private Builder()
        {
        }

        public Builder withType(String aType)
        {
            type = aType;
            return this;
        }

        public Builder withRequired(String... aRequired)
        {
            required.clear();
            if (aRequired != null) {
                required.addAll(asList(aRequired));
            }
            return this;
        }

        public Builder addRequired(String aRequired)
        {
            required.add(aRequired);
            return this;
        }

        public Builder addProperty(String aName, ObjectNode aDefinition)
        {
            properties.put(aName, aDefinition);
            return this;
        }

        public Builder withProperty(String aName, String aType, String aDescripton)
        {
            var definition = JsonNodeFactory.instance.objectNode();
            definition.put("type", aType);
            if (aDescripton != null) {
                definition.put("description", aDescripton);
            }
            properties.put(aName, definition);
            return this;
        }

        public OllamaFunctionParameters build()
        {
            return new OllamaFunctionParameters(this);
        }
    }
}

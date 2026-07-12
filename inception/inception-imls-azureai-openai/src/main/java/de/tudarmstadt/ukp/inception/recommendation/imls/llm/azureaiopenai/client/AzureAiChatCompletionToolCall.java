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

/**
 * A tool call requested by the model on an assistant message, or echoed back when we resend the
 * assistant turn. Azure OpenAI carries the function arguments as a JSON <em>string</em>, not an
 * object; in streaming responses the {@code arguments} string arrives fragmented across chunks and
 * must be concatenated by {@code index}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class AzureAiChatCompletionToolCall
{
    private @JsonProperty("index") Integer index;
    private @JsonProperty("id") String id;
    private @JsonProperty("type") String type;
    private @JsonProperty("function") Function function;

    public Integer getIndex()
    {
        return index;
    }

    public void setIndex(Integer aIndex)
    {
        index = aIndex;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String aId)
    {
        id = aId;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String aType)
    {
        type = aType;
    }

    public Function getFunction()
    {
        return function;
    }

    public void setFunction(Function aFunction)
    {
        function = aFunction;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(NON_NULL)
    public static class Function
    {
        private @JsonProperty("name") String name;
        private @JsonProperty("arguments") String arguments;

        public String getName()
        {
            return name;
        }

        public void setName(String aName)
        {
            name = aName;
        }

        public String getArguments()
        {
            return arguments;
        }

        public void setArguments(String aArguments)
        {
            arguments = aArguments;
        }
    }
}

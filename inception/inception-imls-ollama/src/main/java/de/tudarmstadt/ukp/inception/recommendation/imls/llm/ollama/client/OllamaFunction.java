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

import java.lang.reflect.Method;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(NON_EMPTY)
public class OllamaFunction
{
    private final @JsonProperty("name") String name;
    private final @JsonProperty("description") String description;
    private final @JsonProperty("parameters") OllamaFunctionParameters parameters;
    private final @JsonIgnore String actor;
    private final @JsonIgnore Object service;
    private final @JsonIgnore Method implementation;

    private OllamaFunction(Builder builder)
    {
        name = builder.name;
        description = builder.description;
        actor = builder.actor;
        parameters = builder.parameters;
        service = builder.service;
        implementation = builder.implementation;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public OllamaFunctionParameters getParameters()
    {
        return parameters;
    }

    public Method getImplementation()
    {
        return implementation;
    }

    public Object getService()
    {
        return service;
    }

    public String getActor()
    {
        return actor;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private String name;
        private String actor;
        private String description;
        private OllamaFunctionParameters parameters;
        private Object service;
        private Method implementation;

        private Builder()
        {
        }

        public Builder withImplementation(Object aService, Method aImplementation)
        {
            service = aService;
            implementation = aImplementation;
            return this;
        }

        public Builder withName(String aName)
        {
            name = aName;
            return this;
        }

        public Builder withActor(String aActor)
        {
            actor = aActor;
            return this;
        }

        public Builder withDescription(String aDescription)
        {
            description = aDescription;
            return this;
        }

        public Builder withParameters(OllamaFunctionParameters aParameter)
        {
            parameters = aParameter;
            return this;
        }

        public OllamaFunction build()
        {
            return new OllamaFunction(this);
        }
    }
}

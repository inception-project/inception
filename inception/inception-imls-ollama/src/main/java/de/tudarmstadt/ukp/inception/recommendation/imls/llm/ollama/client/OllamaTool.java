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

import static com.github.victools.jsonschema.generator.OptionPreset.PLAIN_JSON;
import static com.github.victools.jsonschema.generator.SchemaVersion.DRAFT_2020_12;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolUtils.getFunctionActor;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolUtils.getFunctionDescription;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolUtils.getFunctionName;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolUtils.getParameterDescription;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolUtils.getStop;
import static org.apache.commons.lang3.reflect.MethodUtils.getMethodsListWithAnnotation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;

import de.tudarmstadt.ukp.inception.recommendation.imls.llm.Tool;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolUtils;

public class OllamaTool
{
    private @JsonIgnore boolean stop;
    private @JsonProperty("type") String type;
    private @JsonProperty("function") OllamaFunction function;

    private OllamaTool(Builder aBuilder)
    {
        type = aBuilder.type;
        function = aBuilder.function;
        stop = aBuilder.stop;
    }

    public OllamaFunction getFunction()
    {
        return function;
    }

    public String getType()
    {
        return type;
    }

    public boolean isStop()
    {
        return stop;
    }

    public Object invoke(OllamaToolCall toolCall)
        throws JsonProcessingException, IllegalAccessException, InvocationTargetException
    {
        if (!toolCall.getFunction().getName().equals(getFunction().getName())) {
            throw new IllegalArgumentException(
                    "Expected function call for [" + getFunction().getName() + "] but call is for ["
                            + toolCall.getFunction().getName() + "]");
        }

        var toolService = getFunction().getService();
        var toolMethod = getFunction().getImplementation();

        var mapper = new ObjectMapper();
        var params = new ArrayList<Object>();
        for (var param : toolMethod.getParameters()) {
            var paramName = ToolUtils.getParameterName(param);
            var paramValue = toolCall.getFunction().getArguments().get(paramName);
            if (paramValue instanceof JsonNode jsonValue) {
                paramValue = mapper.treeToValue(jsonValue, param.getType());
            }

            params.add(paramValue);
        }

        return toolMethod.invoke(toolService, params.toArray());
    }

    public static List<OllamaTool> forService(Object aService)
    {
        var clazz = aService.getClass();
        var tools = new ArrayList<OllamaTool>();
        for (var toolMethod : getMethodsListWithAnnotation(clazz, Tool.class, true, true)) {
            toolMethod.setAccessible(true);
            var tool = forMethod(aService, toolMethod);

            tools.add(tool);
        }
        return tools;
    }

    public static OllamaTool forMethod(Object aService, Method aToolMethod)
    {
        var generator = new SchemaGenerator(
                new SchemaGeneratorConfigBuilder(DRAFT_2020_12, PLAIN_JSON) //
                        .with(Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT) //
                        .with(new JacksonModule(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED)) //
                        .build());

        var function = OllamaFunction.builder() //
                .withName(getFunctionName(aToolMethod)) //
                .withImplementation(aService, aToolMethod);

        getFunctionDescription(aToolMethod).ifPresent(function::withDescription);
        getFunctionActor(aToolMethod).ifPresent(function::withActor);

        var parameters = OllamaFunctionParameters.builder();
        for (var param : aToolMethod.getParameters()) {
            if (!ToolUtils.isParameter(param)) {
                continue;
            }

            var paramName = ToolUtils.getParameterName(param);

            var schemaBuilder = generator.buildMultipleSchemaDefinitions();
            var propertySchema = schemaBuilder.createSchemaReference(param.getType());

            getParameterDescription(param)
                    .ifPresent(description -> propertySchema.put("description", description));

            parameters.addProperty(paramName, propertySchema) //
                    .addRequired(paramName);
        }

        function.withParameters(parameters.build());

        var tool = OllamaTool.builder() //
                .withFunction(function.build()) //
                .withStop(getStop(aToolMethod)) //
                .build();

        return tool;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private String type;
        private OllamaFunction function;
        private boolean stop;

        private Builder()
        {
        }

        public Builder withType(String aType)
        {
            type = aType;
            return this;
        }

        public Builder withFunction(OllamaFunction aFunction)
        {
            type = "function";
            function = aFunction;
            return this;
        }

        public Builder withStop(boolean aStop)
        {
            stop = aStop;
            return this;
        }

        public OllamaTool build()
        {
            return new OllamaTool(this);
        }
    }
}

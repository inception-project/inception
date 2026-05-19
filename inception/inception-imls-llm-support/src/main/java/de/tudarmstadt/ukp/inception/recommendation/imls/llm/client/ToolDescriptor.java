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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.client;

import static com.github.victools.jsonschema.generator.OptionPreset.PLAIN_JSON;
import static com.github.victools.jsonschema.generator.SchemaVersion.DRAFT_2020_12;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolUtils.getFunctionDescription;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolUtils.getFunctionName;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolUtils.getParameterDescription;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolUtils.getParameterName;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolUtils.isParameter;

import java.lang.reflect.Method;

import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;

import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Provider-neutral description of a tool the model may call. Carried in {@link ChatOptions#tools()}
 * on the request side. The execution-side counterpart (with the actual Java method to invoke) lives
 * in the assistant/tool-library layer.
 *
 * @param name
 *            function name as exposed to the model
 * @param description
 *            free-text description used by the model to decide when to call
 * @param parametersSchema
 *            JSON schema describing the function parameters
 */
public record ToolDescriptor( //
        String name, //
        String description, //
        JsonNode parametersSchema)
{
    /**
     * Builds a {@link ToolDescriptor} from a {@code @Tool}-annotated method by deriving the JSON
     * schema of its parameters via the victools schema generator. The method itself is not carried
     * in the result; the invocation-side {@code (instance, method)} pair stays with the caller
     * (typically in a parallel name-keyed map).
     */
    public static ToolDescriptor fromMethod(Method aMethod)
    {
        var generator = new SchemaGenerator(
                new SchemaGeneratorConfigBuilder(DRAFT_2020_12, PLAIN_JSON) //
                        .with(Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT) //
                        .with(new JacksonModule(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED)) //
                        .build());

        var schema = JSONUtil.getObjectMapper().createObjectNode();
        schema.put("type", "object");
        var properties = (ObjectNode) schema.putObject("properties");
        var required = schema.putArray("required");

        for (var param : aMethod.getParameters()) {
            if (!isParameter(param)) {
                continue;
            }

            var paramName = getParameterName(param);
            var propertySchema = generator.generateSchema(param.getParameterizedType());
            getParameterDescription(param).ifPresent(
                    description -> propertySchema.put("description", description.strip()));

            properties.set(paramName, JSONUtil.adaptJackson2To3(propertySchema));
            required.add(paramName);
        }

        return new ToolDescriptor(getFunctionName(aMethod),
                getFunctionDescription(aMethod).orElse(null), schema);
    }
}

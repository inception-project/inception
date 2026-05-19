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

import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolUtils.getParameterName;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolUtils.isParameter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import tools.jackson.databind.JsonNode;

/**
 * {@link ExecutableTool} backed by a {@code @Tool}-annotated Java {@link Method} whose parameters
 * are <em>all</em> {@code @ToolParam}-annotated — i.e., entirely bound from the LLM-supplied
 * arguments JSON. The descriptor is generated once at construction via
 * {@link ToolDescriptor#fromMethod(Method)}.
 * <p>
 * If the target method declares any parameter that is <em>not</em> {@code @ToolParam}-annotated,
 * construction fails: this class deliberately has no notion of runtime context injection. Tools
 * that need runtime objects (project, document, session owner, ...) bound into their parameters
 * should use a caller-specific {@link ExecutableTool} implementation that captures the required
 * context at construction time.
 * <p>
 * Exceptions thrown by the target method are unwrapped from {@link InvocationTargetException} so
 * callers see the original cause.
 */
public class MethodTool
    implements ExecutableTool
{
    private final Object instance;
    private final Method method;
    private final ToolDescriptor descriptor;

    public MethodTool(Object aInstance, Method aMethod)
    {
        for (var param : aMethod.getParameters()) {
            if (!isParameter(param)) {
                throw new IllegalArgumentException("MethodTool only supports methods whose "
                        + "parameters are all @ToolParam-annotated. Parameter [" + param.getName()
                        + "] of type [" + param.getType().getName() + "] on method ["
                        + aMethod.toGenericString()
                        + "] is not annotated. Provide a custom ExecutableTool implementation if "
                        + "runtime context injection is required.");
            }
        }

        instance = aInstance;
        method = aMethod;
        descriptor = ToolDescriptor.fromMethod(aMethod);
    }

    @Override
    public ToolDescriptor descriptor()
    {
        return descriptor;
    }

    @Override
    public Object invoke(JsonNode aArguments) throws Exception
    {
        var mapper = JSONUtil.getObjectMapper();
        var typeFactory = mapper.getTypeFactory();
        var args = new ArrayList<>();

        for (var param : method.getParameters()) {
            var paramName = getParameterName(param);
            var raw = aArguments != null ? aArguments.get(paramName) : null;
            var type = typeFactory.constructType(param.getParameterizedType());
            args.add(mapper.convertValue(raw, type));
        }

        try {
            return method.invoke(instance, args.toArray());
        }
        catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw e;
        }
    }

    @Override
    public String toString()
    {
        return "MethodTool[" + descriptor.name() + " -> " + method.toGenericString() + "]";
    }
}

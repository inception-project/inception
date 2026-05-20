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
package de.tudarmstadt.ukp.inception.assistant.tools;

import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolUtils.getParameterName;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolUtils.isParameter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.assistant.CommandDispatcher;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.AnnotationEditorContext;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ToolInvoker;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ToolDescriptor;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import tools.jackson.databind.JsonNode;

/**
 * {@link ToolInvoker} backed by a {@code @Tool}-annotated Java {@link Method}, with binding for the
 * assistant's runtime context. Captures the per-turn {@link AssistantRuntimeContext} at
 * construction so {@link #invoke} is a pure (arguments) → result call.
 * <p>
 * Parameter binding mirrors what {@code MToolCall.invoke} did pre-abstraction:
 * <ul>
 * <li>{@code @ToolParam} parameters → Jackson-converted from the LLM-supplied JSON arguments.
 * <li>{@link AnnotationEditorContext} → built per-call from the captured runtime context.
 * <li>{@link Project} / {@link SourceDocument} / {@link CommandDispatcher} → captured runtime
 * context.
 * <li>Anything else → {@link IllegalStateException} at invocation time.
 * </ul>
 * Exceptions thrown by the target method are unwrapped from {@link InvocationTargetException} so
 * callers see the original cause.
 */
public class AssistantToolInvoker
    implements ToolInvoker
{
    private final Object instance;
    private final Method method;
    private final ToolDescriptor descriptor;
    private final AssistantRuntimeContext context;

    public AssistantToolInvoker(Object aInstance, Method aMethod, AssistantRuntimeContext aContext)
    {
        instance = aInstance;
        method = aMethod;
        descriptor = ToolDescriptor.fromMethod(aMethod);
        context = aContext;
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
            if (isParameter(param)) {
                var paramName = getParameterName(param);
                var raw = aArguments != null ? aArguments.get(paramName) : null;
                var type = typeFactory.constructType(param.getParameterizedType());
                args.add(mapper.convertValue(raw, type));
                continue;
            }

            args.add(resolveContextParameter(param.getType(), param.getName()));
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

    private Object resolveContextParameter(Class<?> aType, String aParamName)
    {
        // Strict direction: the parameter type IS-A injectable type. Using the loose direction
        // (param.getType().isAssignableFrom(KnownType.class)) — as the pre-abstraction
        // MToolCall.invoke did — silently matched parameters declared as Object or other
        // supertypes against whichever known type appeared first in the chain.
        if (AnnotationEditorContext.class.isAssignableFrom(aType)) {
            return AnnotationEditorContext.builder() //
                    .withSessionOwner(context.sessionOwner()) //
                    .withProject(context.project()) //
                    .withDocument(context.document()) //
                    .withDataOwner(context.dataOwner()) //
                    .build();
        }
        if (CommandDispatcher.class.isAssignableFrom(aType)) {
            return context.commandDispatcher();
        }
        if (Project.class.isAssignableFrom(aType)) {
            return context.project();
        }
        if (SourceDocument.class.isAssignableFrom(aType)) {
            return context.document();
        }
        throw new IllegalStateException("Tool [" + descriptor.name() + "] declares parameter ["
                + aParamName + "] of unsupported type [" + aType.getName()
                + "]. Supported context types are: AnnotationEditorContext, "
                + "CommandDispatcher, Project, SourceDocument.");
    }

    @Override
    public String toString()
    {
        return "AssistantToolInvoker[" + descriptor.name() + " -> " + method.toGenericString()
                + "]";
    }
}

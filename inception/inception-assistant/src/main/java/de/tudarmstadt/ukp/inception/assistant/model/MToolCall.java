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
package de.tudarmstadt.ukp.inception.assistant.model;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.AnnotationEditorContext;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolUtils;

public record MToolCall(String actor, Object instance, Method method, boolean stop,
        Map<String, Object> arguments)
{

    private MToolCall(Builder builder)
    {
        this(builder.actor, builder.instance, builder.method, builder.stop,
                new LinkedHashMap<>(builder.arguments));
    }

    public Object invoke(String aSessionOwner, Project aProject, SourceDocument aDocument,
            String aDataOwner)
        throws Exception
    {
        var params = new ArrayList<Object>();
        for (var param : method().getParameters()) {
            if (ToolUtils.isParameter(param)) {
                var paramName = ToolUtils.getParameterName(param);
                var paramValue = arguments().get(paramName);
                params.add(paramValue);
                continue;
            }

            if (param.getType().isAssignableFrom(AnnotationEditorContext.class)) {
                params.add(AnnotationEditorContext.builder() //
                        .withDataOwner(aDataOwner) //
                        .withProject(aProject) //
                        .withDocument(aDocument) //
                        .build());
            }
            else if (param.getType().isAssignableFrom(Project.class)) {
                params.add(aProject);
            }
            else if (param.getType().isAssignableFrom(SourceDocument.class)) {
                params.add(aDocument);
            }
            else {
                throw new IllegalStateException("Injection of parameter [" + param.getName()
                        + "] of type [" + param.getType() + "] not supported.");
            }
        }

        return method.invoke(instance, params.toArray(Object[]::new));
    }

    @Override
    public final String toString()
    {
        return method.toGenericString();
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private Object instance;
        private Method method;
        private String actor;
        private boolean stop;
        private Map<String, Object> arguments = new LinkedHashMap<>();

        private Builder()
        {
        }

        public Builder withActor(String aActor)
        {
            actor = aActor;
            return this;
        }

        public Builder withInstance(Object aInstance)
        {
            instance = aInstance;
            return this;
        }

        public Builder withMethod(Method aMethod)
        {
            this.method = aMethod;
            return this;
        }

        public Builder withStop(boolean aStop)
        {
            this.stop = aStop;
            return this;
        }

        public Builder withArguments(Map<String, Object> aArguments)
        {
            arguments.clear();
            if (aArguments != null) {
                arguments.putAll(aArguments);
            }
            return this;
        }

        public MToolCall build()
        {
            return new MToolCall(this);
        }
    }
}

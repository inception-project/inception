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

import java.util.LinkedHashMap;
import java.util.Map;

public class OllamaFunctionCall
{
    private String name;
    private final Map<String, Object> arguments = new LinkedHashMap<>();

    public String getName()
    {
        return name;
    }

    public void setName(String aName)
    {
        name = aName;
    }

    public void setArguments(Map<String, Object> aArguments)
    {
        arguments.clear();
        if (aArguments != null) {
            arguments.putAll(aArguments);
        }
    }

    public Map<String, Object> getArguments()
    {
        return arguments;
    }

    @Override
    public String toString()
    {
        var builder = new StringBuilder();
        builder.append(name).append("(");
        var first = true;
        for (var e : arguments.entrySet()) {
            if (!first) {
                builder.append(", ");
            }
            builder.append(e.getKey());
            builder.append(": ");
            builder.append(e.getValue());
            first = false;
        }
        builder.append(")");
        return builder.toString();
    }
}

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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;

public final class ToolUtils
{
    private ToolUtils()
    {
        // No instances
    }

    public static String getFunctionName(Method aToolMethod)
    {
        var ann = aToolMethod.getAnnotation(Tool.class);

        if (ann != null && isNotBlank(ann.value())) {
            return ann.value();
        }

        return aToolMethod.getName();
    }

    public static Optional<String> getFunctionDescription(Method aToolMethod)
    {
        var ann = aToolMethod.getAnnotation(Tool.class);

        if (ann != null && isNotBlank(ann.description())) {
            return Optional.of(ann.description());
        }

        return Optional.empty();
    }

    public static Optional<String> getFunctionActor(Method aToolMethod)
    {
        var ann = aToolMethod.getAnnotation(Tool.class);

        if (ann != null && isNotBlank(ann.description())) {
            return Optional.of(ann.actor());
        }

        return Optional.empty();
    }

    public static boolean getStop(Method aToolMethod)
    {
        var ann = aToolMethod.getAnnotation(Tool.class);

        if (ann != null) {
            return ann.stop();
        }

        return false;
    }

    public static boolean isParameter(Parameter aToolParmeter)
    {
        var ann = aToolParmeter.getAnnotation(ToolParam.class);
        return ann != null;
    }

    public static String getParameterName(Parameter aToolParmeter)
    {
        var ann = aToolParmeter.getAnnotation(ToolParam.class);

        if (ann != null && isNotBlank(ann.value())) {
            return ann.value();
        }

        return aToolParmeter.getName();
    }

    public static Optional<String> getParameterDescription(Parameter aToolParmeter)
    {
        var ann = aToolParmeter.getAnnotation(ToolParam.class);

        if (ann != null && isNotBlank(ann.description())) {
            return Optional.of(ann.description());
        }

        return Optional.empty();
    }
}

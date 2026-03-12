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
package de.tudarmstadt.ukp.inception.schema.api.adapter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Simple mutable Java bean representing an annotation instance for JSON serialization.
 */
public class AnnotationRepresentation
{
    private String id;
    private String span;
    private String context;
    private final Map<String, Object> annotation = new LinkedHashMap<>();

    public AnnotationRepresentation()
    {
        // no-arg for frameworks
    }

    public void setId(String aId)
    {
        id = aId;
    }

    public String getId()
    {
        return id;
    }

    public String getSpan()
    {
        return span;
    }

    public void setSpan(String aSpan)
    {
        span = aSpan;
    }

    public String getContext()
    {
        return context;
    }

    public void setContext(String aContext)
    {
        context = aContext;
    }

    public Map<String, Object> getAnnotation()
    {
        return annotation;
    }

    public void setFeatureValues(Map<String, Object> aFeatureValues)
    {
        annotation.clear();
        if (aFeatureValues != null) {
            annotation.putAll(aFeatureValues);
        }
    }

    public void setFeatureValue(String aKey, Object aValue)
    {
        annotation.put(aKey, aValue);
    }

    public Object getFeatureValue(String aKey)
    {
        return annotation.get(aKey);
    }

    public void deleteFeatureValue(String aKey)
    {
        annotation.remove(aKey);
    }

    public void keepFeatureValues(String... aKeys)
    {
        var keep = Set.of(aKeys);
        annotation.keySet().removeIf(key -> !keep.contains(key));
    }
}

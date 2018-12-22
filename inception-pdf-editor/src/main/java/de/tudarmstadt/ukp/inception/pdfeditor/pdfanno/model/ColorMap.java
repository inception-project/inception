/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model;

import java.util.HashMap;
import java.util.Map;

public class ColorMap
{

    private String defaultColor;
    private Map<String, String> span;
    private Map<String, String> relation;

    public ColorMap(String defaultColor)
    {
        this.span = new HashMap<>();
        this.relation = new HashMap<>();
        this.defaultColor = defaultColor;
    }

    public String getDefaultColor()
    {
        return defaultColor;
    }

    public Map<String, String> getSpan()
    {
        return span;
    }

    public void addSpan(String name, String color)
    {
        span.put(name, color);
    }

    public Map<String, String> getRelation()
    {
        return relation;
    }

    public void addRelation(String name, String color)
    {
        relation.put(name, color);
    }

    private String mapToString(Map<String, String> map)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        map.forEach((k,v) -> sb.append("'").append(k).append("':'").append(v).append("',"));
        sb.append("}");
        return sb.toString();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("'default':'").append(defaultColor).append("',");
        sb.append("'span':").append(mapToString(span)).append(",");
        sb.append("'relation':").append(mapToString(relation)).append(",");
        sb.append("}");
        return sb.toString();
    }
}

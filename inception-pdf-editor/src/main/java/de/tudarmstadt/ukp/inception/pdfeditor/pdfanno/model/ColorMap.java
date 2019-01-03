/*
 * Copyright 2018
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

import com.fasterxml.jackson.annotation.JsonProperty;

public class ColorMap
{

    @JsonProperty("default")
    private String defaultColor;

    @JsonProperty("span")
    private Map<String, String> span;

    @JsonProperty("relation")
    private Map<String, String> relation;

    public ColorMap(String aDefaultColor)
    {
        span = new HashMap<>();
        relation = new HashMap<>();
        defaultColor = aDefaultColor;
    }

    public String getDefaultColor()
    {
        return defaultColor;
    }

    public Map<String, String> getSpan()
    {
        return span;
    }

    public void addSpan(String aName, String aColor)
    {
        span.put(aName, aColor);
    }

    public Map<String, String> getRelation()
    {
        return relation;
    }

    public void addRelation(String aName, String aColor)
    {
        relation.put(aName, aColor);
    }
}

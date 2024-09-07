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
package de.tudarmstadt.ukp.inception.support.xml.sanitizer;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ExternalPolicy
{
    private @JsonProperty("elements") List<String> elements;
    private @JsonProperty("attributes") List<String> attributes;
    private @JsonProperty("attribute_patterns") List<String> attributePatterns;
    private @JsonProperty("on_elements") List<String> onElements;
    private @JsonProperty("action") String action;
    private @JsonProperty("matching") String pattern;

    @JsonCreator
    public ExternalPolicy( //
            @JsonProperty("elements") List<String> aElements,
            @JsonProperty("attributes") List<String> aAttributes,
            @JsonProperty("attribute_patterns") List<String> aAttributePatterns,
            @JsonProperty("on_elements") List<String> aOnElements,
            @JsonProperty(value = "action", required = true) String aAction,
            @JsonProperty("matching") String aPattern)
    {
        elements = aElements;
        attributes = aAttributes;
        attributePatterns = aAttributePatterns;
        onElements = aOnElements;
        action = aAction;
        pattern = aPattern;
    }

    public List<String> getElements()
    {
        return elements;
    }

    public void setElements(List<String> aElements)
    {
        elements = aElements;
    }

    public List<String> getAttributes()
    {
        return attributes;
    }

    public void setAttributes(List<String> aAttributes)
    {
        attributes = aAttributes;
    }

    public List<String> getOnElements()
    {
        return onElements;
    }

    public void setOnElements(List<String> aOnElements)
    {
        onElements = aOnElements;
    }

    public String getAction()
    {
        return action;
    }

    public void setAction(String aAction)
    {
        action = aAction;
    }

    public String getPattern()
    {
        return pattern;
    }

    public void setPattern(String aMatching)
    {
        pattern = aMatching;
    }

    public void setAttributePatterns(List<String> aPatterns)
    {
        attributePatterns = aPatterns;
    }

    public List<String> getAttributePatterns()
    {
        return attributePatterns;
    }
}

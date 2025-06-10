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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.prompt;

import java.util.HashMap;
import java.util.Map;

import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.inception.rendering.model.Range;

public class PromptContext
{
    private final Range range;
    private final String text;
    private final Map<String, Object> bindings;

    public PromptContext(AnnotationFS aCandidate)
    {
        range = new Range(aCandidate);
        text = aCandidate.getCoveredText().replace("\r\n", "\n");
        bindings = new HashMap<>();
    }

    public PromptContext(int aBegin, int aEnd, String aText)
    {
        range = new Range(aBegin, aEnd);
        text = aText;
        bindings = new HashMap<>();
    }

    public Range getRange()
    {
        return range;
    }

    public String getText()
    {
        return text;
    }

    public void set(String aKey, Object aValue)
    {
        bindings.put(aKey, aValue);
    }

    public void setAll(Map<String, ? extends Object> aBindings)
    {
        bindings.putAll(aBindings);
    }

    public Map<String, Object> getBindings()
    {
        return bindings;
    }
}

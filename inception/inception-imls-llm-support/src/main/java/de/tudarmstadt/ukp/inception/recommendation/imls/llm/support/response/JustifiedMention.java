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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JustifiedMention
{
    private final @JsonProperty(required = true) String coveredText;
    private final @JsonProperty(required = true) String label;
    private final @JsonProperty(required = true) String justification;

    @JsonCreator
    public JustifiedMention( //
            @JsonProperty("coveredText") String aCoveredText, //
            @JsonProperty("label") String aLabel, //
            @JsonProperty("justification") String aJustification)
    {
        coveredText = aCoveredText;
        label = aLabel;
        justification = aJustification;
    }

    public String getCoveredText()
    {
        return coveredText;
    }

    public String getLabel()
    {
        return label;
    }

    public String getJustification()
    {
        return justification;
    }
}

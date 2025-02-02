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

import com.fasterxml.jackson.annotation.JsonProperty;

public enum PromptingMode
{
    @JsonProperty("per-annotation")
    PER_ANNOTATION("""
            Template variables:

            * `text`: annotation text
            * `tags`: tagset (if available)
            * `sentence`: sentence containing annotation
            * `examples`: labeled annotations"""),

    @JsonProperty("per-sentence")
    PER_SENTENCE("""
            Template variables:

            * `text`: sentence text
            * `tags`: tagset (if available)
            * `examples`: labeled annotations"""),

    @JsonProperty("per-paragraph")
    PER_PARAGRAPH("""
            Template variables:

            * `text`: paragraph text
            * `tags`: tagset (if available)
            * `examples`: labeled annotations"""),

    @JsonProperty("per-document")
    PER_DOCUMENT("""
            Template variables:

            * `text`: document text,
            * `tags`: tagset (if available)""");

    private final String hints;

    private PromptingMode(String aHints)
    {
        hints = aHints;
    }

    public String getHints()
    {
        return hints;
    }
}

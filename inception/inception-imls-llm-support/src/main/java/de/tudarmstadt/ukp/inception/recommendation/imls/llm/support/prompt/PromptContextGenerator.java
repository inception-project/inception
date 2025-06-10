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

import java.util.Map;
import java.util.stream.Stream;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;

public interface PromptContextGenerator
{
    static final String VAR_TEXT = "text";
    static final String VAR_SENTENCE = "sentence";
    static final String VAR_DOCUMENT = "document";
    static final String VAR_EXAMPLES = "examples";
    static final String VAR_CAS = "cas";
    static final String VAR_TAGS = "tags";

    Stream<PromptContext> generate(RecommendationEngine aEngine, CAS aCas, int aBegin, int aEnd,
            Map<String, ? extends Object> aBindings);

    static PromptContextGenerator getPromptContextGenerator(PromptingMode aMode)
    {
        switch (aMode) {
        case PER_ANNOTATION:
            return new PerAnnotationContextGenerator();
        case PER_SENTENCE:
            return new PerSentenceContextGenerator();
        case PER_PARAGRAPH:
            return new PerParagraphContextGenerator();
        case PER_DOCUMENT:
            return new PerDocumentContextGenerator();
        default:
            throw new IllegalArgumentException("Unsupported mode [" + aMode + "]");
        }
    }
}

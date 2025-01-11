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

import java.util.List;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.prompt.PromptContext;

public interface ResponseExtractor
{
    void extract(RecommendationEngine aEngine, CAS aCas, PromptContext aCandidate,
            String aResponse);

    List<MentionsSample> generate(RecommendationEngine aEngine, CAS aCas, int aNum);

    static ResponseExtractor getResponseExtractor(ExtractionMode aMode)
    {
        switch (aMode) {
        case RESPONSE_AS_LABEL:
            return new ResponseAsLabelExtractor();
        case MENTIONS_FROM_JSON:
            return new MentionsFromJsonExtractor();
        default:
            throw new IllegalArgumentException("Unsupported extraction mode [" + aMode + "]");
        }
    }
}

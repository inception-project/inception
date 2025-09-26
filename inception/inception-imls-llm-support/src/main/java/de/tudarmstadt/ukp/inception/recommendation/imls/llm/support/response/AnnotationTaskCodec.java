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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.uima.cas.CAS;

import com.fasterxml.jackson.databind.JsonNode;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.AnnotationTaskCodecQuery;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ChatMessage;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.prompt.PromptContext;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.traits.LlmRecommenderTraits;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.extensionpoint.Extension;

public sealed interface AnnotationTaskCodec
    extends Extension<AnnotationTaskCodecQuery>
    permits SpanJsonSchemaAnnotationTaskCodec, SpanJsonAnnotationTaskCodec,
    LabellingAnnotationTaskCodec
{
    List<? extends ChatMessage> encode(PromptContext aPromptContext, String aPrompt);

    void decode(RecommendationEngine aEngine, CAS aCas, PromptContext aCandidate, String aResponse)
        throws IOException;

    Map<String, ?> generateExamples(RecommendationEngine aEngine, CAS aCas, int aNum);

    default Optional<JsonNode> getJsonSchema(Recommender aRecommender,
            AnnotationSchemaService aSchemaService, LlmRecommenderTraits aTraits)
    {
        return Optional.empty();
    }

    List<? extends ChatMessage> getFormatDefiningMessages(Recommender aRecommender,
            AnnotationSchemaService aSchemaService);

    Optional<ResponseFormat> getResponseFormat();
}

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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.AnnotationTaskCodecExtensionPoint;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ChatBasedLlmRecommenderImplBase;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.LlmChatClientExtensionPoint;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaLlmChatClient;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

public class OllamaRecommender
    extends ChatBasedLlmRecommenderImplBase<OllamaRecommenderTraits>
{
    public OllamaRecommender(Recommender aRecommender, OllamaRecommenderTraits aTraits,
            AnnotationSchemaService aSchemaService,
            AnnotationTaskCodecExtensionPoint aResponseExtractorExtensionPoint,
            LlmChatClientExtensionPoint aChatClientExtensionPoint)
    {
        super(aRecommender, aTraits, aSchemaService, aResponseExtractorExtensionPoint,
                aChatClientExtensionPoint);
    }

    @Override
    protected String getProviderId()
    {
        return OllamaLlmChatClient.ID;
    }
}

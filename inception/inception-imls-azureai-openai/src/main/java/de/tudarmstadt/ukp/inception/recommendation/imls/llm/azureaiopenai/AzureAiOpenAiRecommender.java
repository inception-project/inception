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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.azureaiopenai;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.azureaiopenai.client.AzureAiOpenAiClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.azureaiopenai.client.ChatCompletionRequest;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.azureaiopenai.client.GenerateResponseFormat;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.traits.LlmRecommenderImplBase;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.security.client.auth.apikey.ApiKeyAuthenticationTraits;

public class AzureAiOpenAiRecommender
    extends LlmRecommenderImplBase<AzureAiOpenAiRecommenderTraits>
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final AzureAiOpenAiClient client;

    public AzureAiOpenAiRecommender(Recommender aRecommender,
            AzureAiOpenAiRecommenderTraits aTraits, AzureAiOpenAiClient aClient,
            AnnotationSchemaService aSchemaService)
    {
        super(aRecommender, aTraits, aSchemaService);

        client = aClient;
    }

    @Override
    protected String exchange(String aPrompt) throws IOException
    {
        GenerateResponseFormat format = null;
        if (traits.getFormat() != null) {
            format = switch (traits.getFormat()) {
            case JSON -> GenerateResponseFormat.JSON;
            default -> null;
            };
        }

        LOG.trace("Querying Azure AI OpenAI: [{}]", aPrompt);
        var request = ChatCompletionRequest.builder() //
                .withApiKey(((ApiKeyAuthenticationTraits) traits.getAuthentication()).getApiKey()) //
                .withPrompt(aPrompt) //
                .withFormat(format) //
                .build();
        var response = client.generate(traits.getUrl(), request).trim();
        LOG.trace("Azure AI OpenAI responds: [{}]", response);
        return response;
    }
}

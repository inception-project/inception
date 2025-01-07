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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt;

import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.client.ResponseFormatType.JSON_OBJECT;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response.ResponseFormat.JSON;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.client.ChatCompletionRequest;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.client.ChatGptClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.client.ResponseFormat;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.traits.LlmRecommenderImplBase;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.security.client.auth.apikey.ApiKeyAuthenticationTraits;

public class ChatGptRecommender
    extends LlmRecommenderImplBase<ChatGptRecommenderTraits>
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ChatGptClient client;

    public ChatGptRecommender(Recommender aRecommender, ChatGptRecommenderTraits aTraits,
            ChatGptClient aClient, AnnotationSchemaService aSchemaService)
    {
        super(aRecommender, aTraits, aSchemaService);

        client = aClient;
    }

    @Override
    protected String exchange(String aPrompt) throws IOException
    {
        LOG.trace("Query: [{}]", aPrompt);
        var request = ChatCompletionRequest.builder() //
                .withApiKey(((ApiKeyAuthenticationTraits) traits.getAuthentication()).getApiKey()) //
                .withPrompt(aPrompt) //
                .withOptions(traits.getOptions()) //
                .withModel(traits.getModel());

        if (traits.getFormat() == JSON) {
            request.withPrompt(
                    "Respond with a JSON object using the words as the key and the label as the value.\n\n"
                            + aPrompt);
            request.withResponseFormat(ResponseFormat.builder().withType(JSON_OBJECT).build());
        }

        var response = client.generate(traits.getUrl(), request.build()).trim();
        LOG.trace("Response: [{}]", response);
        return response;
    }
}

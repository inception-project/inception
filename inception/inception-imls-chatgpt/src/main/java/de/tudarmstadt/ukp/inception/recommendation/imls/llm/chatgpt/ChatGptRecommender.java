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

import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.client.ChatCompletionRequest.SEED;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.client.ChatCompletionRequest.TEMPERATURE;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.client.ChatCompletionRequest.TOP_P;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.client.ChatGptResponseFormatType.JSON_SCHEMA;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.AnnotationTaskCodecExtensionPoint;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ChatBasedLlmRecommenderImplBase;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ChatMessage;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.client.ChatCompletionMessage;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.client.ChatCompletionRequest;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.client.ChatGptClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.client.ChatGptResponseFormat;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.client.ChatGptResponseFormatType;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response.ResponseFormat;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.security.client.auth.apikey.ApiKeyAuthenticationTraits;

public class ChatGptRecommender
    extends ChatBasedLlmRecommenderImplBase<ChatGptRecommenderTraits>
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ChatGptClient client;

    public ChatGptRecommender(Recommender aRecommender, ChatGptRecommenderTraits aTraits,
            ChatGptClient aClient, AnnotationSchemaService aSchemaService,
            AnnotationTaskCodecExtensionPoint aResponseExtractorExtensionPoint)
    {
        super(aRecommender, aTraits, aSchemaService, aResponseExtractorExtensionPoint);

        client = aClient;
    }

    @Override
    protected String exchange(List<ChatMessage> aMessages,
            de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response.ResponseFormat aFormat,
            JsonNode aJsonSchema)
        throws IOException
    {
        var messages = aMessages.stream() //
                .map(m -> new ChatCompletionMessage(m.role().getName(), m.content())) //
                .toList();

        var request = ChatCompletionRequest.builder() //
                .withApiKey(((ApiKeyAuthenticationTraits) traits.getAuthentication()).getApiKey()) //
                .withMessages(messages) //
                .withResponseFormat(getResponseFormat(aFormat, aJsonSchema)) //
                .withModel(traits.getModel());

        var options = traits.getOptions();
        // https://platform.openai.com/docs/api-reference/chat/create recommends to set temperature
        // or top_p but not both.
        if (!options.containsKey(TEMPERATURE.getName()) && !options.containsKey(TOP_P.getName())) {
            request.withOption(TEMPERATURE, 0.0d);
        }
        request.withOption(SEED, 0xdeadbeef);
        request.withExtraOptions(options);

        var response = client.chat(traits.getUrl(), request.build()).trim();
        LOG.trace("Response: [{}]", response);
        return response;
    }

    private ChatGptResponseFormat getResponseFormat(ResponseFormat aFormat, JsonNode aSchema)
    {
        if (aSchema != null) {
            return ChatGptResponseFormat.builder() //
                    .withType(JSON_SCHEMA) //
                    .withSchema("response", aSchema) //
                    .build();
        }

        if (aFormat == ResponseFormat.JSON) {
            return ChatGptResponseFormat.builder() //
                    .withType(ChatGptResponseFormatType.JSON_OBJECT) //
                    .build();
        }

        return null;
    }
}

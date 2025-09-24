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

import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.azureaiopenai.client.AzureAiResponseFormatType.JSON_OBJECT;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.azureaiopenai.client.AzureAiResponseFormatType.JSON_SCHEMA;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response.ResponseFormat.JSON;

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
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.azureaiopenai.client.AzureAiChatCompletionMessage;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.azureaiopenai.client.AzureAiChatCompletionRequest;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.azureaiopenai.client.AzureAiGenerateResponseFormat;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.azureaiopenai.client.AzureAiOpenAiClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response.ResponseFormat;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.security.client.auth.apikey.ApiKeyAuthenticationTraits;

public class AzureAiOpenAiRecommender
    extends ChatBasedLlmRecommenderImplBase<AzureAiOpenAiRecommenderTraits>
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final AzureAiOpenAiClient client;

    public AzureAiOpenAiRecommender(Recommender aRecommender,
            AzureAiOpenAiRecommenderTraits aTraits, AzureAiOpenAiClient aClient,
            AnnotationSchemaService aSchemaService,
            AnnotationTaskCodecExtensionPoint aResponseExtractorExtensionPoint)
    {
        super(aRecommender, aTraits, aSchemaService, aResponseExtractorExtensionPoint);

        client = aClient;
    }

    @Override
    protected String exchange(List<ChatMessage> aMessages, ResponseFormat aResponseformat,
            JsonNode aJsonSchema)
        throws IOException
    {
        var format = getResponseFormat(aResponseformat, aJsonSchema);

        var messages = aMessages.stream() //
                .map(m -> new AzureAiChatCompletionMessage(m.role().getName(), m.content())) //
                .toList();

        LOG.trace("Querying Azure AI OpenAI: [{}]", aMessages);
        var request = AzureAiChatCompletionRequest.builder() //
                .withApiKey(((ApiKeyAuthenticationTraits) traits.getAuthentication()).getApiKey()) //
                .withMessages(messages) //
                .withFormat(format);

        var options = traits.getOptions();
        // https://platform.openai.com/docs/api-reference/chat/create recommends to set temperature
        // or top_p but not both.
        if (!options.containsKey(AzureAiChatCompletionRequest.TEMPERATURE.getName())
                && !options.containsKey(AzureAiChatCompletionRequest.TOP_P.getName())) {
            request.withOption(AzureAiChatCompletionRequest.TEMPERATURE, 0.0d);
        }
        request.withOption(AzureAiChatCompletionRequest.SEED, 0xdeadbeef);
        request.withExtraOptions(options);

        var response = client.generate(traits.getUrl(), request.build()).trim();
        LOG.trace("Azure AI OpenAI responds: [{}]", response);
        return response;
    }

    private AzureAiGenerateResponseFormat getResponseFormat(ResponseFormat aResponseformat,
            JsonNode aSchema)
    {
        if (aSchema != null) {
            return AzureAiGenerateResponseFormat.builder() //
                    .withType(JSON_SCHEMA) //
                    .withSchema("response", aSchema) //
                    .build();
        }

        if (aResponseformat == JSON) {
            return AzureAiGenerateResponseFormat.builder() //
                    .withType(JSON_OBJECT) //
                    .build();
        }

        return null;
    }
}

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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.client;

import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.client.ChatGptResponseFormatType.JSON_OBJECT;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.client.ChatGptResponseFormatType.JSON_SCHEMA;

import java.io.IOException;
import java.util.List;

import java.util.EnumSet;
import java.util.Set;

import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ChatMessage;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ChatOptions;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ChatResult;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.LlmChatClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.LlmEndpoint;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ModelCapability;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ModelInfo;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.response.ResponseFormat;
import de.tudarmstadt.ukp.inception.security.client.auth.apikey.ApiKeyAuthenticationTraits;

/**
 * {@link LlmChatClient} adapter for the OpenAI chat-completions API (and OpenAI-compatible
 * endpoints such as Groq, Cerebras, or local Ollama in OpenAI mode). Delegates to the existing
 * {@link ChatGptClient}; behaviour is a faithful translation, with no recommender-specific defaults
 * applied here.
 */
public class ChatGptLlmChatClient
    implements LlmChatClient
{
    public static final String ID = "openai";

    private final ChatGptClient client;

    public ChatGptLlmChatClient(ChatGptClient aClient)
    {
        client = aClient;
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public Set<ModelCapability> supportedCapabilities()
    {
        return EnumSet.of(ModelCapability.CHAT, ModelCapability.JSON_SCHEMA);
    }

    @Override
    public ChatResult chat(LlmEndpoint aEndpoint, List<ChatMessage> aMessages, ChatOptions aOptions)
        throws IOException
    {
        var messages = aMessages.stream() //
                .map(m -> new ChatCompletionMessage(m.role().getName(), m.content())) //
                .toList();

        var builder = ChatCompletionRequest.builder() //
                .withApiKey(apiKey(aEndpoint)) //
                .withModel(aEndpoint.model()) //
                .withMessages(messages) //
                .withResponseFormat(toResponseFormat(aOptions));

        if (aOptions.options() != null) {
            builder.withExtraOptions(aOptions.options());
        }

        var responseText = client.chat(aEndpoint.url(), builder.build()).trim();

        return ChatResult.of(new ChatMessage(ChatMessage.Role.ASSISTANT, responseText));
    }

    @Override
    public List<ModelInfo> listModels(LlmEndpoint aEndpoint) throws IOException
    {
        var request = ListModelsRequest.builder() //
                .withApiKey(apiKey(aEndpoint)) //
                .build();

        return client.listModels(aEndpoint.url(), request).stream() //
                .map(m -> new ModelInfo(m.getId())) //
                .toList();
    }

    private static String apiKey(LlmEndpoint aEndpoint)
    {
        var auth = aEndpoint.auth();
        if (auth instanceof ApiKeyAuthenticationTraits apiKeyAuth) {
            return apiKeyAuth.getApiKey();
        }
        if (auth == null) {
            return null;
        }
        throw new IllegalArgumentException(
                "ChatGPT client requires " + ApiKeyAuthenticationTraits.class.getSimpleName()
                        + " but got [" + auth.getClass().getName() + "]");
    }

    private static ChatGptResponseFormat toResponseFormat(ChatOptions aOptions)
    {
        if (aOptions.jsonSchema() != null) {
            return ChatGptResponseFormat.builder() //
                    .withType(JSON_SCHEMA) //
                    .withSchema("response", aOptions.jsonSchema()) //
                    .build();
        }

        if (aOptions.responseFormat() == ResponseFormat.JSON) {
            return ChatGptResponseFormat.builder() //
                    .withType(JSON_OBJECT) //
                    .build();
        }

        return null;
    }
}

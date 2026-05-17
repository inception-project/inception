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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.client;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ChatMessage;
import de.tudarmstadt.ukp.inception.support.extensionpoint.Extension;

/**
 * Provider-neutral chat client. One implementation per provider (OpenAI / OpenAI-compatible, Azure
 * OpenAI, Ollama, ...) registered as a Spring bean and selected via
 * {@link LlmChatClientExtensionPoint}.
 * <p>
 * {@link #getId()} returns the provider id (e.g. {@code openai}, {@code azure-openai},
 * {@code ollama}) and must match the {@code providerId} carried by {@link LlmEndpoint}.
 * <p>
 * Capability flags let callers and UIs gracefully degrade when a provider does not support a given
 * feature.
 */
public interface LlmChatClient
    extends Extension<LlmEndpoint>
{
    @Override
    String getId();

    @Override
    default boolean accepts(LlmEndpoint aEndpoint)
    {
        return aEndpoint != null && getId().equals(aEndpoint.providerId());
    }

    default boolean supportsTools()
    {
        return false;
    }

    default boolean supportsJsonSchema()
    {
        return false;
    }

    default boolean supportsStreaming()
    {
        return false;
    }

    default boolean supportsEmbeddings()
    {
        return false;
    }

    /**
     * Perform a non-streaming chat exchange.
     */
    ChatResult chat(LlmEndpoint aEndpoint, List<ChatMessage> aMessages, ChatOptions aOptions)
        throws IOException;

    /**
     * Perform a streaming chat exchange. {@code aOnChunk} is invoked for each delivered fragment
     * (deltas, not cumulative); the returned {@link ChatResult} carries the assembled final message
     * together with usage and finish reason.
     */
    default ChatResult chatStream(LlmEndpoint aEndpoint, List<ChatMessage> aMessages,
            ChatOptions aOptions, Consumer<ChatChunk> aOnChunk)
        throws IOException
    {
        throw new UnsupportedOperationException(
                "Provider [" + getId() + "] does not support streaming");
    }

    /**
     * Compute embeddings for the given inputs. Result order matches {@code aInputs}.
     */
    default List<float[]> embed(LlmEndpoint aEndpoint, List<String> aInputs) throws IOException
    {
        throw new UnsupportedOperationException(
                "Provider [" + getId() + "] does not support embeddings");
    }

    /**
     * List models available at the endpoint. Returns an empty list if the provider does not expose
     * a discovery API.
     */
    default List<ModelInfo> listModels(LlmEndpoint aEndpoint) throws IOException
    {
        return List.of();
    }
}

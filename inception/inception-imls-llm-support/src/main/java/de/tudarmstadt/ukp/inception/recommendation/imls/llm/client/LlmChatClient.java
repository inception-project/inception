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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    /**
     * Capabilities <em>this adapter is currently able to translate</em> on its wire protocol. This
     * is an adapter-implementation property, not a model property:
     * <ul>
     * <li>{@link ModelCapability#STREAMING} / {@link ModelCapability#EMBEDDINGS} indicate that
     * {@link #chatStream} / {@link #embed} are implemented (instead of throwing
     * {@code UnsupportedOperationException}).</li>
     * <li>{@link ModelCapability#TOOLS} / {@link ModelCapability#JSON_SCHEMA} /
     * {@link ModelCapability#VISION} / {@link ModelCapability#THINKING} indicate that the adapter
     * translates the corresponding {@link ChatOptions} field or {@link ChatMessage} content onto
     * the wire — silently ignored otherwise.</li>
     * </ul>
     * This set is typically a constant per adapter and reflects adapter maturity.
     * <p>
     * Distinct from {@link LlmEndpoint#capabilities()}, which describes what <em>the configured
     * model</em> is declared to support. The endpoint set is what the caller wants to use; this set
     * is the upper bound of what the adapter can deliver. A valid configuration has
     * {@code endpoint.capabilities() ⊆ adapter.supportedCapabilities()}; declaring a capability the
     * adapter cannot deliver has no effect (best case) or is silently ignored.
     * <p>
     * UIs should use this set to gate which capability checkboxes are even sensible to show for a
     * given provider.
     */
    default Set<ModelCapability> supportedCapabilities()
    {
        return EnumSet.of(ModelCapability.CHAT);
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
     *
     * @param aOptions
     *            provider-specific options (e.g. {@code num_ctx}, {@code seed} for Ollama;
     *            {@code dimensions}, {@code encoding_format} for OpenAI). May be {@code null} or
     *            empty.
     */
    default List<float[]> embed(LlmEndpoint aEndpoint, List<String> aInputs,
            Map<String, Object> aOptions)
        throws IOException
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

    /**
     * Describe the model configured on the endpoint, so the assistant can auto-detect its context
     * window and declared capabilities instead of assuming a specific provider. Returns
     * {@link Optional#empty()} when the provider exposes no such introspection API (the caller then
     * keeps its configured defaults). Adapters translate their backend's own capability tokens into
     * {@link ModelCapability} values.
     */
    default Optional<ModelDetails> describeModel(LlmEndpoint aEndpoint) throws IOException
    {
        return Optional.empty();
    }
}

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
package de.tudarmstadt.ukp.inception.assistant.config;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

public interface AssistantProperties
{
    /**
     * @return the default LLM provider id, used when the chat/embedding sections do not override
     *         it.
     */
    String getProvider();

    /**
     * @return the default endpoint URL, used when the chat/embedding sections do not override it.
     */
    String getUrl();

    /**
     * @return the default API key, used when the chat/embedding sections do not override it.
     */
    String getApiKey();

    AssistantChatProperties getChat();

    AssistantEmbeddingProperties getEmbedding();

    String getNickname();

    boolean isSummarizeThoughts();

    AssitantUserGuideProperties getUserGuide();

    AssistantDocumentIndexProperties getDocumentIndex();

    /**
     * @return the effective chat provider id: the chat-section override, or the top-level default.
     */
    default String getChatProvider()
    {
        return defaultIfBlank(getChat().getProvider(), getProvider());
    }

    /**
     * @return the effective chat endpoint URL: the chat-section override, or the top-level default.
     */
    default String getChatUrl()
    {
        return defaultIfBlank(getChat().getUrl(), getUrl());
    }

    /**
     * @return the effective chat API key: the chat-section override, or the top-level default.
     */
    default String getChatApiKey()
    {
        return defaultIfBlank(getChat().getApiKey(), getApiKey());
    }

    /**
     * @return the effective embedding provider id: the embedding-section override, or the top-level
     *         default.
     */
    default String getEmbeddingProvider()
    {
        return defaultIfBlank(getEmbedding().getProvider(), getProvider());
    }

    /**
     * @return the effective embedding endpoint URL: the embedding-section override, or the
     *         top-level default.
     */
    default String getEmbeddingUrl()
    {
        return defaultIfBlank(getEmbedding().getUrl(), getUrl());
    }

    /**
     * @return the effective embedding API key: the embedding-section override, or the top-level
     *         default.
     */
    default String getEmbeddingApiKey()
    {
        return defaultIfBlank(getEmbedding().getApiKey(), getApiKey());
    }
}

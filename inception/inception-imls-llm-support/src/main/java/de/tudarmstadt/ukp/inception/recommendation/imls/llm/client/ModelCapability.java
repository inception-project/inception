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

/**
 * Capabilities a configured model+endpoint supports. The source varies by provider: Ollama can
 * probe via {@code ollama show}, OpenAI-compatible endpoints (incl. LM Studio, vLLM, Groq, ...)
 * have no probe so the user declares them in the traits/config UI.
 * <p>
 * Distinct from {@code LlmChatClient.supportsX()}, which describes what the adapter can translate
 * <em>at all</em> on its wire protocol (a static, adapter-level capability). The adapter flag gates
 * which UI controls are sensible to show; this enum gates what the caller actually sends per
 * request.
 */
public enum ModelCapability
{
    CHAT, TOOLS, JSON_SCHEMA, STREAMING, EMBEDDINGS, VISION, THINKING
}

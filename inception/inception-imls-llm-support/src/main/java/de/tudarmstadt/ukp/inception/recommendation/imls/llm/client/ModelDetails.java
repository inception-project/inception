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

import static java.util.Collections.emptySet;

import java.util.Set;

/**
 * Provider-neutral description of a configured model, as reported by
 * {@link LlmChatClient#describeModel}. Distinct from {@link ModelInfo}, which is a lightweight
 * discovery entry (id/display name) returned by {@link LlmChatClient#listModels}: this type carries
 * the richer per-model properties the assistant uses to auto-configure itself (context window size
 * and the capabilities the model declares).
 * <p>
 * Any field may be absent when the provider does not expose it: {@code contextLength} is
 * {@code null} when unknown, and {@code capabilities} is empty when the provider does not report a
 * capability list. Adapters translate their backend's own capability tokens into
 * {@link ModelCapability} values.
 *
 * @param contextLength
 *            the model's context window in tokens, or {@code null} if the provider did not report
 *            it
 * @param capabilities
 *            capabilities the model declares; empty when the provider does not report them
 */
public record ModelDetails( //
        Integer contextLength, //
        Set<ModelCapability> capabilities)
{
    public ModelDetails
    {
        capabilities = capabilities != null ? Set.copyOf(capabilities) : emptySet();
    }
}

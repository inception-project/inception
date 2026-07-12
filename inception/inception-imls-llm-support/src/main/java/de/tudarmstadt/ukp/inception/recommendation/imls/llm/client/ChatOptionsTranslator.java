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

import java.util.function.Consumer;

/**
 * Helpers shared by the {@link LlmChatClient} adapters when translating the provider-neutral
 * {@link ChatOptions} fields into their backend's parameters.
 * <p>
 * The wire names differ per provider (and some neutral knobs have no equivalent at all on a given
 * backend), so this helper deliberately does not know any wire names. It only factors out the
 * null-check-and-apply pattern that every adapter repeats: each adapter drives it with its own
 * builder setter and wire name.
 */
public final class ChatOptionsTranslator
{
    private ChatOptionsTranslator()
    {
        // No instances
    }

    /**
     * Invokes {@code aSetter} with {@code aValue} only when the value is present
     * (non-{@code null}), mirroring "leave the provider default when the neutral field was not
     * set".
     *
     * @param aValue
     *            the neutral option value, possibly {@code null}
     * @param aSetter
     *            the adapter-specific action applying the value to its request builder
     */
    public static <T> void applyIfPresent(T aValue, Consumer<T> aSetter)
    {
        if (aValue != null) {
            aSetter.accept(aValue);
        }
    }
}

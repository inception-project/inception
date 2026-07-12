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
 * Provider-neutral requested reasoning/thinking effort for a chat call. Adapters translate this to
 * whatever their backend supports and degrade levels the backend lacks (see the per-adapter
 * translation). Only meaningful for models that support reasoning; ignored otherwise.
 * <p>
 * {@link #MODEL_DEFAULT} (or a {@code null} {@code ChatOptions.reasoningEffort()}) leaves the
 * choice to the model — the adapter sends no effort/thinking field on the wire.
 */
public enum ReasoningEffort
{
    /** Leave the effort to the model default; the adapter sends nothing on the wire. */
    MODEL_DEFAULT,

    /**
     * Explicitly suppress reasoning where the backend supports turning it off (e.g. Ollama sends
     * {@code think: false}). Backends without an "off" switch degrade this to the model default:
     * the OpenAI/Azure {@code reasoning_effort} field only accepts {@code low}/{@code medium}/
     * {@code high}, so {@code NONE} there behaves like {@link #MODEL_DEFAULT} (the field is
     * omitted) and a reasoning model still reasons at its default.
     */
    NONE,

    LOW,

    MEDIUM,

    HIGH,

    /**
     * Highest available reasoning; adapters clamp to their maximum when they have no {@code max}.
     */
    MAX;
}

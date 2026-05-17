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
 * Why the model stopped generating. Maps to the union of finish/stop reasons exposed by common
 * providers (OpenAI {@code finish_reason}, Ollama {@code done_reason}, Azure OpenAI).
 */
public enum FinishReason
{
    /** Natural stop, end-of-turn or stop sequence hit. */
    STOP,

    /** Reached the maximum token limit before completing. */
    LENGTH,

    /** Model wants to invoke one or more tools; see {@code ChatResult.toolCalls}. */
    TOOL_CALLS,

    /** Output was blocked by a content filter / safety system. */
    CONTENT_FILTER,

    /** Generation aborted due to an error reported by the provider. */
    ERROR,

    /** Provider returned a reason that does not map to any of the above. */
    OTHER
}

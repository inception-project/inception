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

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public interface ChatGptClient
{
    /**
     * Perform a non-streaming chat completion. Returns the parsed wire response (choices, usage,
     * finish reason); the neutral mapping to {@code ChatResult} is the adapter's job.
     */
    ChatCompletionResponse chat(String aUrl, ChatCompletionRequest aRequest) throws IOException;

    /**
     * Perform a streaming (SSE) chat completion. {@code aContentCallback} receives each content
     * delta as it arrives; the returned {@link ChatCompletionResponse} is assembled from the stream
     * and carries the full content, tool calls, finish reason, and (when
     * {@code stream_options.include_usage} was requested) usage.
     */
    ChatCompletionResponse chat(String aUrl, ChatCompletionRequest aRequest,
            Consumer<String> aContentCallback)
        throws IOException;

    List<ChatGptModel> listModels(String aUrl, ListModelsRequest aRequest) throws IOException;
}

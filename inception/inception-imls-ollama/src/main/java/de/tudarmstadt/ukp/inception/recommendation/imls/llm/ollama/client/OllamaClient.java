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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.lang3.tuple.Pair;

public interface OllamaClient
{
    String generate(String aUrl, OllamaGenerateRequest aRequest) throws IOException;

    String generate(String aUrl, OllamaGenerateRequest aRequest,
            Consumer<OllamaGenerateResponse> aCallback)
        throws IOException;

    OllamaChatResponse chat(String aUrl, OllamaChatRequest aRequest) throws IOException;

    OllamaChatResponse chat(String aUrl, OllamaChatRequest aRequest,
            Consumer<OllamaChatResponse> aCallback)
        throws IOException;

    List<OllamaTag> listModels(String aUrl) throws IOException;

    OllamaShowResponse getModelInfo(String aUrl, String aModel) throws IOException;

    List<Pair<String, float[]>> embed(String aUrl, OllamaEmbedRequest aRequest) throws IOException;
}

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
package de.tudarmstadt.ukp.inception.recommendation.imls.ollama.client;

import static de.tudarmstadt.ukp.inception.recommendation.imls.ollama.client.OllamaResponseFormat.JSON;

import java.lang.invoke.MethodHandles;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Disabled("Requires locally running ollama")
class OllamaClientImplTest
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String OLLAMA_LOCAL = "http://localhost:11434";
    private OllamaClientImpl sut = new OllamaClientImpl();

    @Test
    void testStream() throws Exception
    {
        var response = sut.generate(OLLAMA_LOCAL, OllamaAskRequest.builder() //
                .withModel("mistral") //
                .withPrompt("Tell me a joke.") //
                .withStream(true) //
                .build());
        LOG.info("Response: [{}]", response.trim());
    }

    @Test
    void testNonStream() throws Exception
    {
        var response = sut.generate(OLLAMA_LOCAL, OllamaAskRequest.builder() //
                .withModel("mistral") //
                .withPrompt("Tell me a joke.") //
                .withStream(false) //
                .build());
        LOG.info("Response: [{}]", response.trim());
    }

    @Test
    void testJson() throws Exception
    {
        var response = sut.generate(OLLAMA_LOCAL, OllamaAskRequest.builder() //
                .withModel("mistral") //
                .withPrompt("Generate a JSON map with the key/value pairs `a = 1` and `b = 2`") //
                .withStream(false) //
                .withFormat(JSON) //
                .build());
        LOG.info("Response: [{}]", response.trim());
    }
}

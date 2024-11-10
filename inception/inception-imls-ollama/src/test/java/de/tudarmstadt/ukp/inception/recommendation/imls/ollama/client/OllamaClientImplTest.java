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

import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.OllamaRecommenderTraits.DEFAULT_OLLAMA_URL;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaGenerateResponseFormat.JSON;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.lang.invoke.MethodHandles;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaClientImpl;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaGenerateRequest;
import de.tudarmstadt.ukp.inception.support.test.http.HttpTestUtils;

class OllamaClientImplTest
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private OllamaClientImpl sut = new OllamaClientImpl();

    @BeforeAll
    static void checkIfOllamaIsRunning()
    {
        assumeThat(HttpTestUtils.checkURL(DEFAULT_OLLAMA_URL)).isTrue();
    }

    @Test
    void testStream() throws Exception
    {
        var response = sut.generate(DEFAULT_OLLAMA_URL, OllamaGenerateRequest.builder() //
                .withModel("mistral") //
                .withPrompt("Tell me a joke.") //
                .withStream(true) //
                .build());
        LOG.info("Response: [{}]", response.trim());
    }

    @Test
    void testNonStream() throws Exception
    {
        var response = sut.generate(DEFAULT_OLLAMA_URL, OllamaGenerateRequest.builder() //
                .withModel("mistral") //
                .withPrompt("Tell me a joke.") //
                .withStream(false) //
                .build());
        LOG.info("Response: [{}]", response.trim());
    }

    @Test
    void testJson() throws Exception
    {
        var response = sut.generate(DEFAULT_OLLAMA_URL, OllamaGenerateRequest.builder() //
                .withModel("mistral") //
                .withPrompt("Generate a JSON map with the key/value pairs `a = 1` and `b = 2`") //
                .withStream(false) //
                .withFormat(JSON) //
                .build());
        LOG.info("Response: [{}]", response.trim());
    }

    @Test
    void testListModels() throws Exception
    {
        var response = sut.listModels(DEFAULT_OLLAMA_URL);
        LOG.info("Response: [{}]", response);
    }
}

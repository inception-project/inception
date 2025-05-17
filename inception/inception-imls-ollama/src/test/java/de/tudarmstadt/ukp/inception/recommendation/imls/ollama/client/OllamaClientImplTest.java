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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import de.tudarmstadt.ukp.inception.recommendation.imls.llm.Tool;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolParam;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaChatMessage;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaChatRequest;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaChatResponse;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaClientImpl;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaFunction;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaFunctionParameters;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaGenerateRequest;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaGenerateResponse;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaTag;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaTool;
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
        var request = OllamaGenerateRequest.builder() //
                .withModel("mistral") //
                .withPrompt("Tell me a joke.") //
                .withStream(true) //
                .build();
        var response = sut.generate(DEFAULT_OLLAMA_URL, request);
        LOG.info("Response: [{}]", response.trim());
    }

    @Test
    void testStreamWithCallback() throws Exception
    {
        var request = OllamaGenerateRequest.builder() //
                .withModel("mistral") //
                .withPrompt("Tell me a joke.") //
                .withStream(true) //
                .build();

        Consumer<OllamaGenerateResponse> callback = response -> {
            LOG.info("Callback: [{}]", response.getResponse());
        };

        var response = sut.generate(DEFAULT_OLLAMA_URL, request, callback);
        LOG.info("Response: [{}]", response.trim());
    }

    @Test
    void testChatStreamWithCallback() throws Exception
    {
        var request = OllamaChatRequest.builder() //
                .withModel("mistral") //
                .withMessages( //
                        new OllamaChatMessage("system",
                                "You are Donald Duck and end each sentence with 'quack'."),
                        new OllamaChatMessage("user", "What is your name?")) //
                .withStream(true) //
                .build();

        Consumer<OllamaChatResponse> callback = response -> {
            LOG.info("Callback: [{}]", response.getMessage().content());
        };

        var response = sut.chat(DEFAULT_OLLAMA_URL, request, callback);
        LOG.info("Response: [{}]", response.getMessage().content());
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
                .withFormat(JsonNodeFactory.instance.textNode("json")) //
                .build());
        LOG.info("Response: [{}]", response.trim());
    }

    @Test
    void testListModels() throws Exception
    {
        var response = sut.listModels(DEFAULT_OLLAMA_URL);
        LOG.info("Response: [{}]", response);
    }

    @Test
    void testShowModel() throws Exception
    {
        var response = sut.getModelInfo(DEFAULT_OLLAMA_URL, "gemma3");
        LOG.info("Response: [{}]", response);
    }

    @Test
    void testFunctionCall() throws Exception
    {
        var function = OllamaFunction.builder() //
                .withName("get_current_weather") //
                .withDescription("Get the current weather for a city") //
                .withParameters(OllamaFunctionParameters.builder() //
                        .withProperty("city", "string", "Name of the city") //
                        .withRequired("city") //
                        .build())
                .build();

        var addFunction = OllamaTool.builder() //
                .withFunction(function) //
                .build();

        var call = OllamaChatRequest.builder() //
                // .withModel("phi4-mini") //
                .withModel("llama3.2") //
                .withMessages(new OllamaChatMessage("user", "What is the weather in Toronto?")) //
                .withStream(false) //
                // .withFormat(JsonNodeFactory.instance.textNode("json")) //
                .withTools(addFunction) //
                .build();

        var response = sut.chat(DEFAULT_OLLAMA_URL, call).getMessage();

        assertThat(response.toolCalls()).as("toolCalls").hasSize(1);
        assertThat(response.toolCalls().get(0).getFunction().getName())
                .isEqualTo(function.getName());
    }

    static class TestWeatherTool
    {
        @Tool("get_weather")
        public String getWeather(
                @ToolParam(value = "city", description = "A city name") String aCity)
        {
            return "sunny";
        }
    }

    static List<OllamaTag> models() throws IOException
    {
        var sut = new OllamaClientImpl();
        return sut.listModels(DEFAULT_OLLAMA_URL);
    }

    @MethodSource("models")
    @ParameterizedTest
    void testFunctionCallReflection(OllamaTag aModel) throws Exception
    {
        var weatherTool = new TestWeatherTool();

        var tools = OllamaTool.forService(weatherTool);

        var call = OllamaChatRequest.builder() //
                .withModel(aModel.name()) //
                .withMessages(new OllamaChatMessage("user", "What is the weather in Toronto?")) //
                .withStream(false) //
                .withTools(tools) //
                .build();

        var response = sut.chat(DEFAULT_OLLAMA_URL, call).getMessage();

        assertThat(response.toolCalls()).as("toolCalls").hasSize(1);

        var toolCall = response.toolCalls().get(0);
        assertThat(toolCall.getFunction().getName()) //
                .isEqualTo("get_weather");
        assertThat(toolCall.getFunction().getArguments()) //
                .containsExactly(entry("city", "Toronto"));

        var maybeTool = call.getTool(toolCall);
        assertThat(maybeTool).isPresent();

        var tool = maybeTool.get();
        var result = tool.invoke(toolCall);

        assertThat(result).isEqualTo("sunny");
    }
}

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
package de.tudarmstadt.ukp.inception.assistant;

import static de.tudarmstadt.ukp.inception.assistant.model.MChatRoles.ASSISTANT;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.OllamaRecommenderTraits.DEFAULT_OLLAMA_URL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;

import de.tudarmstadt.ukp.inception.assistant.config.AssistantDocumentIndexPropertiesImpl;
import de.tudarmstadt.ukp.inception.assistant.config.AssistantPropertiesImpl;
import de.tudarmstadt.ukp.inception.assistant.model.MTextMessage;
import de.tudarmstadt.ukp.inception.assistant.tool.ClockToolLibrary;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaClientImpl;
import de.tudarmstadt.ukp.inception.support.test.http.HttpTestUtils;

class AgentLoopTest
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private EncodingRegistry encodingRegistry = Encodings.newLazyEncodingRegistry();
    private OllamaClientImpl client = new OllamaClientImpl();

    private AssistantPropertiesImpl props;
    private Memory memory;
    private Encoding encoding;

    @BeforeAll
    static void checkIfOllamaIsRunning()
    {
        assumeThat(HttpTestUtils.checkURL(DEFAULT_OLLAMA_URL)).isTrue();
    }

    @BeforeEach
    void setUp()
    {
        props = new AssistantPropertiesImpl(new AssistantDocumentIndexPropertiesImpl());
        props.setUrl(DEFAULT_OLLAMA_URL);
        props.getChat().setModel("gpt-oss:120b-cloud");

        encoding = encodingRegistry.getEncoding(props.getChat().getEncoding())
                .orElseThrow(() -> new IllegalStateException("Unknown encoding"));

        memory = new Memory();
    }

    @Test
    void chatAgainstOllama() throws Exception
    {
        var sut = new AgentLoop(props, client, "integration-test", null, memory, encoding);

        var input = List.of(MTextMessage.builder() //
                .withContent("Hello") //
                .withRole("user") //
                .withActor("tester") //
                .build());

        var response = sut.chat(input, null);

        LOG.debug(response.toString());

        assertThat(response).isNotNull();
        assertThat(response.message()).isNotNull();
        assertThat(response.message().content()).isNotBlank();
    }

    @Test
    void testToolCalling_ClockTool() throws Exception
    {
        // Setup AgentLoop with ClockToolLibrary
        props.getChat().setCapabilities(Set.of("tools"));
        var sut = new AgentLoop(props, client, "integration-test", null, memory, encoding);
        sut.addToolLibrary(new ClockToolLibrary());
        sut.setToolCallingEnabled(true);

        // Create user message asking for time
        var input = List.of(MTextMessage.builder() //
                .withContent("What is the current time?") //
                .withRole("user") //
                .withActor("tester") //
                .build());

        // Execute
        var response = sut.chat(input, null);

        LOG.debug("Response: {}", response.toString());

        // Verify response structure
        assertThat(response).isNotNull();
        assertThat(response.message()).isNotNull();

        // Verify tool was called
        assertThat(response.message().toolCalls()).isNotEmpty();

        var toolCall = response.message().toolCalls().get(0);
        LOG.debug("Tool call: {}", toolCall);

        assertThat(toolCall.actor()).isEqualTo("Clock");
        assertThat(toolCall.method().getName()).isEqualTo("getTime");

        // Invoke the tool and verify result structure
        var result = toolCall.invoke("integration-test", null, null, null, null);
        assertThat(result).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        var timeMap = (Map<String, String>) result;
        LOG.debug("Tool result: {}", timeMap);

        // Verify all expected time components are present
        assertThat(timeMap).containsKeys("year", "month", "day", "hour", "minute", "second",
                "day-of-week", "week-of-year");

        // Verify values are reasonable (basic sanity checks)
        assertThat(Integer.parseInt(timeMap.get("year"))).isGreaterThan(2020);
        assertThat(Integer.parseInt(timeMap.get("month"))).isBetween(1, 12);
        assertThat(Integer.parseInt(timeMap.get("day"))).isBetween(1, 31);
        assertThat(Integer.parseInt(timeMap.get("hour"))).isBetween(0, 23);
        assertThat(Integer.parseInt(timeMap.get("minute"))).isBetween(0, 59);
        assertThat(Integer.parseInt(timeMap.get("second"))).isBetween(0, 59);
    }

    @Test
    void testStreamingMessagesAreAccumulatedInMemory() throws Exception
    {
        var message = MTextMessage.builder() //
                .withContent("Tell me a very short story about a robot.") //
                .withRole("user") //
                .withActor("tester") //
                .build();

        var sut = new AgentLoop(props, client, "integration-test", null, memory, encoding);

        sut.loop(null, "integration-test", message);

        var responseMessages = memory.getInternalChatHistory().stream()
                .filter(m -> ASSISTANT.equals(m.role())).toList();
        assertThat(responseMessages).satisfiesExactly(m -> {
            assertThat(m).isInstanceOf(MTextMessage.class);
            assertThat(((MTextMessage) m).done()).isTrue();
            assertThat(((MTextMessage) m).content()).isNotBlank();
        });
    }
}

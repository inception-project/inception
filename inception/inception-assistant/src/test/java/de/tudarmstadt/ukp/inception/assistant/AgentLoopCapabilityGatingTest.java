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

import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.ChatMessage.Role.ASSISTANT;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ModelCapability.CHAT;
import static de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ModelCapability.STREAMING;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;

import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.assistant.config.AssistantDocumentIndexPropertiesImpl;
import de.tudarmstadt.ukp.inception.assistant.config.AssistantPropertiesImpl;
import de.tudarmstadt.ukp.inception.assistant.model.MTextMessage;
import de.tudarmstadt.ukp.inception.assistant.tool.ClockToolLibrary;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ChatMessage;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ChatChunk;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ChatOptions;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ChatResult;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.LlmChatClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.LlmEndpoint;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.client.ModelCapability;

/**
 * Offline unit tests for {@link AgentLoop}'s graceful degradation based on the adapter's declared
 * {@link LlmChatClient#supportedCapabilities()}. Uses a recording stub client, so it does not
 * require a running LLM server (unlike the integration-oriented {@link AgentLoopTest}).
 */
class AgentLoopCapabilityGatingTest
{
    private EncodingRegistry encodingRegistry = Encodings.newLazyEncodingRegistry();

    private AssistantPropertiesImpl props;
    private Memory memory;
    private Encoding encoding;
    private User user;

    @BeforeEach
    void setUp()
    {
        props = new AssistantPropertiesImpl();
        props.setDocumentIndex(new AssistantDocumentIndexPropertiesImpl());
        props.setUrl("http://localhost:11434");
        props.getChat().setModel("some-model");

        encoding = encodingRegistry.getEncoding(props.getChat().getEncoding())
                .orElseThrow(() -> new IllegalStateException("Unknown encoding"));

        memory = new Memory();
        user = User.builder().withUsername("unit-test").build();
    }

    @Test
    void testFallsBackToNonStreamingWhenStreamingUnsupported() throws Exception
    {
        var client = new RecordingChatClient(EnumSet.of(CHAT));
        var sut = new AgentLoop(props, client, user, null, memory, encoding);

        var response = sut.turn(List.of(userMessage("Hello")), null);

        assertThat(client.chatCalled).isTrue();
        assertThat(client.chatStreamCalled).isFalse();
        assertThat(response.message().content()).isEqualTo("pong");
    }

    @Test
    void testUsesStreamingWhenSupported() throws Exception
    {
        var client = new RecordingChatClient(EnumSet.of(CHAT, STREAMING));
        var sut = new AgentLoop(props, client, user, null, memory, encoding);

        var response = sut.turn(List.of(userMessage("Hello")), null);

        assertThat(client.chatStreamCalled).isTrue();
        assertThat(client.chatCalled).isFalse();
        assertThat(response.message().content()).isEqualTo("pong");
    }

    @Test
    void testDropsToolsWhenAdapterDoesNotSupportTools() throws Exception
    {
        // The assistant config wants tools, but the adapter does not declare TOOLS support.
        props.getChat().setCapabilities(Set.of("tools"));

        var client = new RecordingChatClient(EnumSet.of(CHAT, STREAMING));
        var sut = new AgentLoop(props, client, user, null, memory, encoding);
        sut.addToolLibrary(new ClockToolLibrary());
        sut.setToolCallingEnabled(true);

        sut.turn(List.of(userMessage("What time is it?")), null);

        // No tools must reach the wire because the adapter cannot deliver them.
        assertThat(client.lastOptions.tools()).isEmpty();
    }

    @Test
    void testPassesToolsWhenAdapterSupportsTools() throws Exception
    {
        props.getChat().setCapabilities(Set.of("tools"));

        var client = new RecordingChatClient(EnumSet.of(CHAT, STREAMING, ModelCapability.TOOLS));
        var sut = new AgentLoop(props, client, user, null, memory, encoding);
        sut.addToolLibrary(new ClockToolLibrary());
        sut.setToolCallingEnabled(true);

        sut.turn(List.of(userMessage("What time is it?")), null);

        assertThat(client.lastOptions.tools()).isNotEmpty();
    }

    private static MTextMessage userMessage(String aContent)
    {
        return MTextMessage.builder() //
                .withContent(aContent) //
                .withRole("user") //
                .withActor("tester") //
                .build();
    }

    /**
     * Recording {@link LlmChatClient} stub that answers with a fixed reply and remembers which
     * transport path was taken and which options were passed.
     */
    private static final class RecordingChatClient
        implements LlmChatClient
    {
        private final Set<ModelCapability> capabilities;

        private boolean chatCalled = false;
        private boolean chatStreamCalled = false;
        private ChatOptions lastOptions;

        RecordingChatClient(Set<ModelCapability> aCapabilities)
        {
            capabilities = aCapabilities;
        }

        @Override
        public String getId()
        {
            return "stub";
        }

        @Override
        public Set<ModelCapability> supportedCapabilities()
        {
            return capabilities;
        }

        @Override
        public ChatResult chat(LlmEndpoint aEndpoint, List<ChatMessage> aMessages,
                ChatOptions aOptions)
        {
            chatCalled = true;
            lastOptions = aOptions;
            return reply();
        }

        @Override
        public ChatResult chatStream(LlmEndpoint aEndpoint, List<ChatMessage> aMessages,
                ChatOptions aOptions, Consumer<ChatChunk> aOnChunk)
        {
            chatStreamCalled = true;
            lastOptions = aOptions;
            return reply();
        }

        private static ChatResult reply()
        {
            return new ChatResult(new ChatMessage(ASSISTANT, "pong"), emptyList(), null, null);
        }
    }
}

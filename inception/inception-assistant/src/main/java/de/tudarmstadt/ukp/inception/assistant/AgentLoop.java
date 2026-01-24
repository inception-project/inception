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

import static com.github.victools.jsonschema.generator.OptionPreset.PLAIN_JSON;
import static com.github.victools.jsonschema.generator.SchemaVersion.DRAFT_2020_12;
import static de.tudarmstadt.ukp.inception.assistant.config.AssistantChatProperties.CAP_TOOLS;
import static de.tudarmstadt.ukp.inception.assistant.model.MChatRoles.ASSISTANT;
import static de.tudarmstadt.ukp.inception.assistant.model.MChatRoles.SYSTEM;
import static de.tudarmstadt.ukp.inception.assistant.model.MChatRoles.TOOL;
import static java.lang.Math.floorDiv;
import static java.lang.System.currentTimeMillis;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.knuddels.jtokkit.api.Encoding;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.assistant.config.AssistantProperties;
import de.tudarmstadt.ukp.inception.assistant.model.MCallResponse;
import de.tudarmstadt.ukp.inception.assistant.model.MChatMessage;
import de.tudarmstadt.ukp.inception.assistant.model.MChatResponse;
import de.tudarmstadt.ukp.inception.assistant.model.MMessage;
import de.tudarmstadt.ukp.inception.assistant.model.MPerformanceMetrics;
import de.tudarmstadt.ukp.inception.assistant.model.MReference;
import de.tudarmstadt.ukp.inception.assistant.model.MTextMessage;
import de.tudarmstadt.ukp.inception.assistant.model.MTextMessage.Builder;
import de.tudarmstadt.ukp.inception.assistant.model.MToolCall;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolLibrary;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaChatMessage;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaChatRequest;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaChatResponse;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaFunctionCall;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaOptions;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaTool;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaToolCall;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

/**
 * The {@code AgentLoop} class manages the main interaction loop for an AI agent within the
 * assistant framework. It coordinates the flow of messages between the user, the agent, and
 * external tools, handling the lifecycle of agent interactions.
 */
public class AgentLoop
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * The maximum number of iterations the agent loop will perform before stopping. This limit is
     * set to 10 to prevent infinite loops and excessive resource usage. When this limit is reached,
     * the agent loop will stop and return control to the user.
     */
    private static final int MAX_REPEAT = 20;

    /**
     * How much of the available context window to use (in percent) before cutting messages.
     */
    private static final int CONTEXT_LENGTH_SAFETY_PERCENTAGE = 90;

    private final AssistantProperties properties;
    private final OllamaClient ollamaClient;
    private final String sessionOwner;
    private final Project project;
    private final List<ToolLibrary> tools = new ArrayList<>();
    private final Memory memory;
    private final Encoding encoding;

    private boolean ignoreMemory = false;
    private boolean toolCallingEnabled = true;
    private SchemaGenerator generator;
    private MessageStreamHandler messageStreamHandler;
    private CommandDispatcher commandDispatcher;

    private List<MTextMessage> systemMessages = new ArrayList<>();
    private List<? extends MChatMessage> ephemeralMessages = new ArrayList<>();

    public interface MessageRecordedCallback
    {
        void callback(String aSessionOwner, Project aProject, MMessage aMessage);
    }

    public interface MessageStreamHandler
    {
        void handleMessage(String aSessionOwner, Project aProject, UUID responseId,
                MChatMessage responseMessage);
    }

    public AgentLoop(AssistantProperties aProperties, OllamaClient aOllamaClient,
            String aSessionOwner, Project aProject, Memory aMemory, Encoding aEncoding)
    {
        properties = aProperties;
        ollamaClient = aOllamaClient;
        sessionOwner = aSessionOwner;
        project = aProject;
        memory = aMemory;
        encoding = aEncoding;
        generator = new SchemaGenerator(new SchemaGeneratorConfigBuilder(DRAFT_2020_12, PLAIN_JSON) //
                .with(Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT) //
                .with(new JacksonModule(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED)) //
                .build());
    }

    public void setEphemeralMessages(List<? extends MChatMessage> aEphemeralMessages)
    {
        ephemeralMessages = aEphemeralMessages;
    }

    public List<? extends MChatMessage> getEphemeralMessages()
    {
        return ephemeralMessages;
    }

    public void setSystemMessages(List<MTextMessage> aSystemMessages)
    {
        systemMessages = aSystemMessages;
    }

    public void setMessageStreamHandler(MessageStreamHandler aHandler)
    {
        messageStreamHandler = aHandler;
    }

    public void setCommandDispatcher(CommandDispatcher aCommandDispatcher)
    {
        commandDispatcher = aCommandDispatcher;
    }

    public List<MTextMessage> getSystemMessages()
    {
        return systemMessages;
    }

    public void recordAndSendMessage(MChatMessage aMessage)
    {
        Validate.isTrue(aMessage.done(), "Message must be done");

        memory.recordMessage(aMessage);

        if (messageStreamHandler != null) {
            messageStreamHandler.handleMessage(sessionOwner, project, UUID.randomUUID(), aMessage);
        }
    }

    public void setToolCallingEnabled(boolean aToolCallingEnabled)
    {
        toolCallingEnabled = aToolCallingEnabled;
    }

    public boolean isToolCallingEnabled()
    {
        return toolCallingEnabled;
    }

    public void addToolLibrary(ToolLibrary aTool)
    {
        tools.add(aTool);
    }

    public List<ToolLibrary> getTools()
    {
        return tools;
    }

    public Project getProject()
    {
        return project;
    }

    public String getSessionOwner()
    {
        return sessionOwner;
    }

    public MChatResponse chat(List<? extends MChatMessage> aMessages)
        throws IOException, ToolNotFoundException
    {
        return chat(aMessages, null);
    }

    MChatResponse chat(List<? extends MChatMessage> aMessages, UUID aResponseId)
        throws IOException, ToolNotFoundException
    {
        var responseId = aResponseId != null ? aResponseId : UUID.randomUUID();
        var chatProperties = properties.getChat();
        var requestBuilder = OllamaChatRequest.builder() //
                .withApiKey(properties.getApiKey()) //
                .withModel(chatProperties.getModel()) //
                .withStream(true) //
                .withMessages(aMessages.stream() //
                        .map(this::toOllama) //
                        .toList()) //
                .withOption(OllamaOptions.NUM_CTX, chatProperties.getContextLength()) //
                .withOption(OllamaOptions.TOP_P, chatProperties.getTopP()) //
                .withOption(OllamaOptions.TOP_K, chatProperties.getTopK()) //
                .withOption(OllamaOptions.REPEAT_PENALTY, chatProperties.getRepeatPenalty()) //
                .withOption(OllamaOptions.TEMPERATURE, chatProperties.getTemperature());

        if (isToolCallingEnabled() && chatProperties.getCapabilities().contains(CAP_TOOLS)
                && !tools.isEmpty()) {
            try {
                var ollamaTools = tools.stream() //
                        .flatMap(o -> OllamaTool.forService(o).stream()) //
                        .toList();
                requestBuilder.withTools(ollamaTools);

            }
            catch (Exception e) {
                LOG.error("Unable to map tools", e);
            }
        }

        var references = new LinkedHashMap<String, MReference>();
        aMessages.stream() //
                .flatMap(msg -> msg.references().stream()) //
                .forEach(r -> references.put(r.id(), r));

        // Send initial message with the assistant nickname and the references
        recordAndStreamMessage(responseId, newMessage(responseId) //
                .withReferences(references.values()) //
                .notDone() //
                .build());

        // Generate the actual response
        var startTime = currentTimeMillis();
        var firstTokenTime = new AtomicLong(0l);
        var request = requestBuilder.build();
        var response = ollamaClient.chat(properties.getUrl(), request, msg -> {
            firstTokenTime.compareAndSet(0l, currentTimeMillis());
            recordAndStreamMessage(responseId, msg);
        });
        var tokens = response.getEvalCount();
        var endTime = currentTimeMillis();

        var toolCalls = new ArrayList<MToolCall>();
        if (isNotEmpty(response.getMessage().toolCalls()) && isNotEmpty(request.getTools())) {
            for (var call : response.getMessage().toolCalls()) {
                var maybeTool = request.getTool(call);
                if (maybeTool.isEmpty()) {
                    throw new ToolNotFoundException(call.getFunction().getName());
                }

                var tool = maybeTool.get();
                toolCalls.add(MToolCall.builder() //
                        .withActor(tool.getFunction().getActor()) //
                        .withInstance(tool.getFunction().getService()) //
                        .withName(tool.getFunction().getName()) //
                        .withMethod(tool.getFunction().getImplementation()) //
                        .withArguments(call.getFunction().getArguments()) //
                        .withStop(tool.isStop()) //
                        .build());
            }
        }

        // Send a final and complete message also including final metrics
        return MChatResponse.builder() //
                .withMessage(newMessage(responseId) //
                        .withContent(response.getMessage().content()) //
                        .withThinking(response.getMessage().thinking()) //
                        .withPerformance(MPerformanceMetrics.builder() //
                                .withDelay(firstTokenTime.get() - startTime) //
                                .withDuration(endTime - firstTokenTime.get()) //
                                .withTokens(tokens) //
                                .build()) //
                        // Include all references in the final message again just to be sure
                        .withReferences(references.values()) //
                        .withToolCalls(toolCalls) //
                        .build()) //
                .build();
    }

    private OllamaChatMessage toOllama(MChatMessage aMsg)
    {
        List<OllamaToolCall> toolCalls = null;

        if (isNotEmpty(aMsg.toolCalls())) {
            toolCalls = new ArrayList<OllamaToolCall>();
            var i = 0;
            for (var tc : aMsg.toolCalls()) {
                var functionCall = new OllamaFunctionCall();
                functionCall.setIndex(i);
                functionCall.setName(tc.name());
                functionCall.setArguments(tc.arguments());
                var toolCall = new OllamaToolCall();
                toolCall.setFunction(functionCall);
                toolCalls.add(toolCall);
                i++;
            }
        }

        return new OllamaChatMessage(aMsg.role(), aMsg.textRepresentation(), aMsg.thinking(),
                toolCalls);
    }

    public <T> MCallResponse<T> call(Class<T> aResult, List<MTextMessage> aMessages)
        throws IOException
    {
        var schema = generator.generateSchema(aResult);

        var responseId = UUID.randomUUID();
        var chatProperties = properties.getChat();
        var requestBuilder = OllamaChatRequest.builder() //
                .withApiKey(properties.getApiKey()) //
                .withModel(chatProperties.getModel()) //
                .withStream(false) //
                .withMessages(aMessages.stream() //
                        .map(this::toOllama) //
                        .toList()) //
                .withFormat(JSONUtil.adaptJackson2To3(schema)) //
                .withOption(OllamaOptions.NUM_CTX, chatProperties.getContextLength()) //
                .withOption(OllamaOptions.TOP_P, chatProperties.getTopP()) //
                .withOption(OllamaOptions.TOP_K, chatProperties.getTopK()) //
                .withOption(OllamaOptions.REPEAT_PENALTY, chatProperties.getRepeatPenalty()) //
                .withOption(OllamaOptions.TEMPERATURE, chatProperties.getTemperature());

        var references = new LinkedHashMap<String, MReference>();
        aMessages.stream() //
                .flatMap(msg -> msg.references().stream()) //
                .forEach(r -> references.put(r.id(), r));

        // Generate the actual response
        var startTime = currentTimeMillis();
        var request = requestBuilder.build();
        var response = ollamaClient.chat(properties.getUrl(), request, null);
        var tokens = response.getEvalCount();
        var endTime = currentTimeMillis();

        var payload = JSONUtil.fromJsonString(aResult, response.getMessage().content());

        // Send a final and complete message also including final metrics
        return MCallResponse.builder(aResult) //
                .withId(responseId) //
                .withActor(properties.getNickname()) //
                .withRole(ASSISTANT) //
                .withPayload(payload) //
                .withPerformance(MPerformanceMetrics.builder() //
                        .withDuration(endTime - startTime) //
                        .withTokens(tokens) //
                        .build()) //
                // Include all references in the final message again just to be sure
                .withReferences(references.values()) //
                .build();
    }

    private void recordAndStreamMessage(UUID aResponseId, MChatMessage aMessage)
    {
        memory.recordMessage(aMessage);

        if (messageStreamHandler != null) {
            // When the message is done, we send it without content because the content
            // should already have been streamed before. So we only send the final "done" signal.
            var message = aMessage;
            if (message instanceof MTextMessage textMessage && textMessage.done()) {
                message = textMessage.withoutContent();
            }
            messageStreamHandler.handleMessage(sessionOwner, project, aResponseId, message);
        }
    }

    private void recordAndStreamMessage(UUID responseId, OllamaChatResponse aMessage)
    {
        if (isEmpty(aMessage.getMessage().content()) && isEmpty(aMessage.getMessage().thinking())) {
            return;
        }

        var message = newMessage(responseId) //
                .withContent(aMessage.getMessage().content()) //
                .withThinking(aMessage.getMessage().thinking()) //
                .notDone() //
                .build();

        recordAndStreamMessage(responseId, message);
    }

    private Builder newMessage(UUID responseId)
    {
        return MTextMessage.builder() //
                .withId(responseId) //
                .withActor(properties.getNickname()) //
                .withRole(ASSISTANT);
    }

    MChatMessage callTool(String aSessionOwner, Project aProject, SourceDocument aDocument,
            String aDataOwner, MChatMessage aContext, MToolCall call)
    {
        try {
            var result = call.invoke(aSessionOwner, aProject, aDocument, aDataOwner,
                    commandDispatcher);

            if (result instanceof MCallResponse.Builder responseBuilder) {
                responseBuilder //
                        .withRole(TOOL) //
                        .withToolCall(call);
                return responseBuilder.build();
            }

            return MCallResponse.builder(Object.class) //
                    .withActor(defaultIfBlank(call.actor(), "Tool")) //
                    .withRole(TOOL) //
                    .withToolCall(call) //
                    .withPayload(result) //
                    .build();
        }
        catch (Exception e) {
            LOG.error("Error calling tool", e);
            return MTextMessage.builder() //
                    .withActor("Error").withRole(SYSTEM) //
                    .withContent("Error: " + e.getMessage()) //
                    .build();
        }
    }

    /**
     * Runs the agent loop, processing the given message and managing conversation state.
     * <p>
     * The loop may repeat multiple times if the agent determines that further tool calls are
     * required to fulfill the user's request. Each iteration may result in tool calls, and the loop
     * will continue until no further tool calls are needed or the repeat limit is reached.
     * <p>
     * Tool calls are handled by invoking the appropriate tool and recording the result in the
     * conversation history. The repeat limit prevents infinite loops by capping the number of
     * consecutive tool call iterations.
     *
     * @param aDocument
     *            the source document associated with the conversation
     * @param aDataOwner
     *            the owner of the annotations
     * @param aMessage
     *            the (user) message triggering the loop
     * @throws IOException
     *             if an I/O error occurs during processing
     */
    public void loop(SourceDocument aDocument, String aDataOwner, MTextMessage aMessage)
        throws IOException
    {
        var conversationMessages = memory.getInternalChatHistory();

        // Recording the message only here so it does not yet appear in the initial conversation
        // messages
        recordAndSendMessage(aMessage);
        getEphemeralMessages().forEach(this::recordAndSendMessage);

        var headMessage = aMessage;

        UUID responseId = null;
        var repeat = false;
        var repeatCount = 0;
        var repeatMax = MAX_REPEAT;
        var accumulatedReferences = new ArrayList<MReference>();
        LOG.debug("Start of turn");
        do {
            repeat = false;

            var recentConversation = limitConversationToContextLength(encoding, getSystemMessages(),
                    getEphemeralMessages(), conversationMessages, headMessage,
                    properties.getChat().getContextLength());

            LOG.debug("Selected {} of {} to be included in the context window",
                    recentConversation.size(), memory.getMessages().size());

            MChatResponse response;
            try {
                response = chat(recentConversation, responseId);
            }
            catch (ToolNotFoundException e) {
                repeat = true;
                response = MChatResponse.builder() //
                        .withMessage(MTextMessage.builder() //
                                .withId(responseId) // if null builder sets ID
                                .withActor("Error") //
                                .withRole(SYSTEM) //
                                .withContent("Error: " + e.getMessage()) //
                                .build())
                        .build();
            }

            // If the message we started rendering has no content yet, we re-use the message ID
            // for the next message.
            if (isBlank(response.message().content()) && response.message().toolCalls().isEmpty()) {
                responseId = response.message().id();
            }
            else {
                // If we have any accumulated references from the tool calls, we need
                // to inject them here so they get stored/dispatched
                var msg = response.message();
                msg = msg.appendReferences(accumulatedReferences);

                // Recording final message without content because the content has already
                // been streamed before. This is really only to mark the message as done.
                recordAndStreamMessage(responseId, msg);
                accumulatedReferences.clear();
            }

            if (!response.message().toolCalls().isEmpty()) {
                for (var call : response.message().toolCalls()) {
                    var msg = callTool(sessionOwner, project, aDocument, aDataOwner,
                            response.message(), call);
                    if (msg.references() != null) {
                        accumulatedReferences.addAll(msg.references());
                    }

                    recordAndStreamMessage(responseId, msg);
                    repeat |= !call.stop();
                }
            }

            // Consume the user message so it does not stay at conversation head
            headMessage = null;
            conversationMessages = memory.getInternalChatHistory();

            if (repeat) {
                LOG.debug("Continuing turn");
                repeatCount++;
            }

            if (repeatCount >= repeatMax) {
                // TODO Have some mechanism to allow the user to continue the churn
                LOG.debug("Emergency break");
                repeat = false;
                recordAndSendMessage(MTextMessage.builder() //
                        .withId(responseId) // if null builder sets ID
                        .withRole(ASSISTANT) //
                        .withContent(
                                "The assistant has worked on this for a while. Should it continue?") //
                        .build());
            }
        }
        while (repeat);

        LOG.debug("End of turn");
    }

    private List<MChatMessage> limitConversationToContextLength(Encoding aEncoding,
            List<MTextMessage> aSystemMessages, List<? extends MChatMessage> aEphemeralMessages,
            List<? extends MChatMessage> aRecentMessages, MTextMessage aLatestUserMessage,
            int aContextLength)
        throws IOException
    {
        var limit = floorDiv(aContextLength * CONTEXT_LENGTH_SAFETY_PERCENTAGE, 100);

        var headMessages = new ArrayList<MChatMessage>();
        var tailMessages = new LinkedList<MChatMessage>();

        var totalMessages = aSystemMessages.size() + aEphemeralMessages.size()
                + aRecentMessages.size() + 1;
        var allTokens = 0;
        var systemTokens = 0;
        var recentTokens = 0;
        var ephemeralTokens = 0;

        // System prompts from the start as context limit allows
        var systemMsgIterator = aSystemMessages.iterator();

        while (systemMsgIterator.hasNext() && allTokens < limit) {
            var msg = systemMsgIterator.next();
            if (SYSTEM.equals(msg.role())) {
                var msgTokens = aEncoding.countTokensOrdinary(msg.content());
                if (allTokens + msgTokens > limit) {
                    LOG.trace("System message exceeds remaining token limit ({}): [{}]", msgTokens,
                            msg.content());
                    continue;
                }

                allTokens += msgTokens;
                systemTokens += msgTokens;
                headMessages.add(msg);
            }
        }

        // Unconditionally the latest user message
        if (aLatestUserMessage != null) {
            var latestUserMsgTokens = aEncoding.countTokensOrdinary(aLatestUserMessage.content());
            recentTokens += latestUserMsgTokens;
            allTokens += latestUserMsgTokens;
            tailMessages.addLast(aLatestUserMessage);
        }

        // Add ephemeral messages as context limit allows
        var ephemeralMsgIterator = aEphemeralMessages.listIterator(aEphemeralMessages.size());
        while (ephemeralMsgIterator.hasPrevious() && allTokens < limit) {
            var msg = ephemeralMsgIterator.previous();
            var textRepresentation = msg.textRepresentation();
            var msgTokens = aEncoding.countTokensOrdinary(textRepresentation);
            if (allTokens + msgTokens > limit) {
                LOG.trace("Ephemeral message exceeds remaining token limit ({}): [{}]", msgTokens,
                        textRepresentation);
                continue;
            }

            allTokens += msgTokens;
            ephemeralTokens += msgTokens;
            tailMessages.addFirst(msg);
        }

        // Add most recent user/assistant conversation as context limit allows
        var recentMsgIterator = aRecentMessages.listIterator(aRecentMessages.size());
        while (recentMsgIterator.hasPrevious() && allTokens < limit) {
            var msg = recentMsgIterator.previous();
            var textRepresentation = msg.textRepresentation();
            var msgTokens = aEncoding.countTokensOrdinary(textRepresentation);
            if (allTokens + msgTokens > limit) {
                break;
            }

            allTokens += msgTokens;
            recentTokens += msgTokens;
            tailMessages.addFirst(msg);
        }

        var allMessages = new ArrayList<>(headMessages);
        allMessages.addAll(tailMessages);

        LOG.trace(
                "Reduced from {} to {} messages with a total of {} / {} tokens (system: {}, ephemeral: {}, recent: {} )",
                totalMessages, headMessages.size() + tailMessages.size(), allTokens, limit,
                systemTokens, ephemeralTokens, recentTokens);

        return allMessages;
    }

    public void setIgnoreMemory(boolean aIgnoreMemory)
    {
        ignoreMemory = aIgnoreMemory;
    }

    public boolean isIgnoreMemory()
    {
        return ignoreMemory;
    }
}

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

import static de.tudarmstadt.ukp.inception.assistant.config.AssistantChatProperties.AUTO_DETECT_CAPABILITIES;
import static de.tudarmstadt.ukp.inception.assistant.config.AssistantChatProperties.CAP_COMPLETION;
import static de.tudarmstadt.ukp.inception.assistant.config.AssistantChatProperties.CAP_TOOLS;
import static de.tudarmstadt.ukp.inception.assistant.model.MChatRoles.SYSTEM;
import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.toPrettyJsonString;
import static java.lang.Math.floorDiv;
import static java.lang.String.join;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.security.core.session.SessionRegistry;

import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.networknt.schema.Schema;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.assistant.config.AssistantProperties;
import de.tudarmstadt.ukp.inception.assistant.config.AssistantPropertiesImpl.AssistantChatPropertiesImpl;
import de.tudarmstadt.ukp.inception.assistant.model.MCallResponse;
import de.tudarmstadt.ukp.inception.assistant.model.MChatMessage;
import de.tudarmstadt.ukp.inception.assistant.model.MClearCommand;
import de.tudarmstadt.ukp.inception.assistant.model.MMessage;
import de.tudarmstadt.ukp.inception.assistant.model.MRefreshCommand;
import de.tudarmstadt.ukp.inception.assistant.model.MTextMessage;
import de.tudarmstadt.ukp.inception.assistant.retriever.RetrieverExtensionPoint;
import de.tudarmstadt.ukp.inception.preferences.ClientSidePreferenceKey;
import de.tudarmstadt.ukp.inception.preferences.ClientSidePreferenceMapValue;
import de.tudarmstadt.ukp.inception.preferences.ClientSideUserPreferencesProvider;
import de.tudarmstadt.ukp.inception.project.api.event.AfterProjectRemovedEvent;
import de.tudarmstadt.ukp.inception.project.api.event.BeforeProjectRemovedEvent;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ToolLibraryExtensionPoint;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaShowRequest;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaShowResponse;
import de.tudarmstadt.ukp.inception.support.io.WatchedResourceFile;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

public class AssistantServiceImpl
    implements AssistantService, ClientSideUserPreferencesProvider
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final SessionRegistry sessionRegistry;
    private final SimpMessagingTemplate msgTemplate;
    private final MemoryManager memoryManager;
    private final OllamaClient ollamaClient;
    private final AssistantProperties properties;
    private final EncodingRegistry encodingRegistry;
    private final RetrieverExtensionPoint retrieverExtensionPoint;
    private final ToolLibraryExtensionPoint toolLibraryExtensionPoint;

    private static final ClientSidePreferenceKey<ClientSidePreferenceMapValue> KEY_ASSISTANT_PREFS = //
            new ClientSidePreferenceKey<>(ClientSidePreferenceMapValue.class, "assistant/general");

    private WatchedResourceFile<Schema> userPreferencesSchema;

    public AssistantServiceImpl(SessionRegistry aSessionRegistry,
            SimpMessagingTemplate aMsgTemplate, OllamaClient aOllamaClient,
            AssistantProperties aProperties, EncodingRegistry aEncodingRegistry,
            RetrieverExtensionPoint aRetrieverExtensionPoint,
            ToolLibraryExtensionPoint aToolLibraryExtensionPoint)
    {
        sessionRegistry = aSessionRegistry;
        msgTemplate = aMsgTemplate;
        memoryManager = new MemoryManager();
        ollamaClient = aOllamaClient;
        properties = aProperties;
        encodingRegistry = aEncodingRegistry;
        retrieverExtensionPoint = aRetrieverExtensionPoint;
        toolLibraryExtensionPoint = aToolLibraryExtensionPoint;

        var userPreferencesSchemaFile = getClass()
                .getResource("AssistantServiceUserPreferences.schema.json");
        userPreferencesSchema = new WatchedResourceFile<>(userPreferencesSchemaFile,
                JSONUtil::loadJsonSchema);
    }

    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        autoDetectModelCapabilities();
    }

    // Set order so this is handled before session info is removed from sessionRegistry
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EventListener
    public void onSessionDestroyed(SessionDestroyedEvent event)
    {
        var info = sessionRegistry.getSessionInformation(event.getId());
        // Could be an anonymous session without information.
        if (info == null) {
            return;
        }

        String username = null;
        if (info.getPrincipal() instanceof String) {
            username = (String) info.getPrincipal();
        }

        if (info.getPrincipal() instanceof User) {
            username = ((User) info.getPrincipal()).getUsername();
        }

        if (username != null) {
            memoryManager.clearMemories(username);
        }
    }

    @EventListener
    public void onBeforeProjectRemoved(BeforeProjectRemovedEvent aEvent)
    {
        memoryManager.clearMemories(aEvent.getProject());
    }

    @EventListener
    public void onAfterProjectRemoved(AfterProjectRemovedEvent aEvent)
    {
        memoryManager.clearMemories(aEvent.getProject());
    }

    @Override
    public List<MChatMessage> getUserChatHistory(String aSessionOwner, Project aProject)
    {
        var memory = memoryManager.getMemory(aSessionOwner, aProject);
        return memory.getUserChatHistory();
    }

    @Override
    public void dispatchMessage(String aSessionOwner, Project aProject, MMessage aMessage)
    {
        LOG.trace("Dispatching {}", aMessage);

        if (aMessage instanceof MChatMessage chatMessage) {
            if (!isDebugMode(aSessionOwner, aProject) && chatMessage.internal()) {
                return;
            }
        }

        // LOG.trace("Dispatching assistant message: {}", aMessage);
        var topic = AssistantWebsocketController.getChannel(aProject);
        msgTemplate.convertAndSend("/topic" + topic, aMessage);
    }

    @Override
    public void clearConversation(String aSessionOwner, Project aProject)
    {
        memoryManager.clearMemories(aSessionOwner, aProject);

        dispatchMessage(aSessionOwner, aProject, new MClearCommand());
    }

    @Override
    public void refreshAnnotations(String aSessionOwner, Project aProject)
    {
        dispatchMessage(aSessionOwner, aProject, new MRefreshCommand());
    }

    @Override
    public void setDebugMode(String aSessionOwner, Project aProject, boolean aOnOff)
    {
        memoryManager.setDebugMode(aSessionOwner, aProject, aOnOff);
    }

    @Override
    public boolean isDebugMode(String aSessionOwner, Project aProject)
    {
        return memoryManager.isDebugMode(aSessionOwner, aProject);
    }

    @Override
    public MTextMessage processInternalMessageSync(String aSessionOwner, Project aProject,
            MTextMessage aMessage)
        throws IOException
    {
        Validate.isTrue(aMessage.internal());

        try {
            var memory = memoryManager.getMemory(aSessionOwner, aProject);
            var assistant = new AgentLoop(properties, ollamaClient, aSessionOwner, aProject, memory,
                    getEncoding());
            assistant.setSystemMessages(generateSystemMessages(aProject));
            assistant.setMessageStreamHandler(this::handleStreamedMessageFragment);
            assistant.setToolCallingEnabled(false);

            memory.recordMessage(aMessage);
            var responseMessage = assistant.chat(asList(aMessage)).message();

            // Record the final complete message to replace the accumulated fragments
            memory.recordMessage(responseMessage);

            return responseMessage;
        }
        catch (ToolNotFoundException e) {
            // Shouldn't happen because we disabled tool calling above
            throw new IOException(e);
        }
    }

    @Override
    public <T> MCallResponse<T> processInternalCallSync(String aSessionOwner, Project aProject,
            Class<T> aType, MTextMessage aMessage)
        throws IOException
    {
        Validate.isTrue(aMessage.internal());

        var memory = memoryManager.getMemory(aSessionOwner, aProject);

        if (memory.isDebugMode()) {
            memory.recordMessage(aMessage);
            dispatchMessage(aSessionOwner, aProject, aMessage);
        }

        var assistant = new AgentLoop(properties, ollamaClient, aSessionOwner, aProject, memory,
                getEncoding());
        assistant.setSystemMessages(generateSystemMessages(aProject));
        assistant.setMessageStreamHandler(this::handleStreamedMessageFragment);
        var result = assistant.call(aType, asList(aMessage));

        if (isDebugMode(aSessionOwner, aProject)) {
            var resultMessage = MTextMessage.builder() //
                    .withRole(SYSTEM).internal().ephemeral() //
                    .withActor(aMessage.actor()) //
                    .withContent("```json\n" + toPrettyJsonString(result.payload()) + "\n```") //
                    .withPerformance(result.performance()) //
                    .build();
            memory.recordMessage(resultMessage);
            dispatchMessage(aSessionOwner, aProject, resultMessage);
        }

        return result;
    }

    @Override
    public void processInternalMessage(String aSessionOwner, Project aProject,
            SourceDocument aDocument, String aDataOwner, MTextMessage aMessage,
            MTextMessage... aContextMessages)
    {
        var memory = memoryManager.getMemory(aSessionOwner, aProject);
        var assistant = new AgentLoop(properties, ollamaClient, aSessionOwner, aProject, memory,
                getEncoding());
        assistant.setIgnoreMemory(true);
        assistant.setSystemMessages(generateSystemMessages(aProject));
        assistant.setEphemeralMessages(asList(aContextMessages));
        assistant.setMessageStreamHandler(this::handleStreamedMessageFragment);
        assistant.setToolCallingEnabled(false);

        try {
            assistant.loop(aDocument, aDataOwner, aMessage);
        }
        catch (Exception e) {
            handleAsyncException(aSessionOwner, aProject, e);
        }
    }

    @Override
    public void processUserMessage(String aSessionOwner, Project aProject, SourceDocument aDocument,
            String aDataOwner, MTextMessage aMessage)
    {
        var memory = memoryManager.getMemory(aSessionOwner, aProject);
        var assistant = new AgentLoop(properties, ollamaClient, aSessionOwner, aProject, memory,
                getEncoding());
        assistant.setSystemMessages(generateSystemMessages(aProject));
        assistant.setEphemeralMessages(generateEphemeralMessages(aProject, aMessage));
        assistant.setMessageStreamHandler(this::handleStreamedMessageFragment);
        assistant.setCommandDispatcher(command -> dispatchMessage(aSessionOwner, aProject, command));

        toolLibraryExtensionPoint.getExtensions(aProject).forEach(toolLibrary -> {
            assistant.addToolLibrary(toolLibrary);
        });

        try {
            assistant.loop(aDocument, aDataOwner, aMessage);
        }
        catch (Exception e) {
            handleAsyncException(aSessionOwner, aProject, e);
        }
    }

    private void handleAsyncException(String aSessionOwner, Project aProject, Exception e)
    {
        LOG.error("Error processing user message", e);
        var errorMessage = MTextMessage.builder() //
                .withActor("Error") //
                .withRole(SYSTEM) //
                .internal() //
                .ephemeral() //
                .withContent("Error: " + e.getMessage()) //
                .build();
        memoryManager.getMemory(aSessionOwner, aProject).recordMessage(errorMessage);
        dispatchMessage(aSessionOwner, aProject, errorMessage);
    }

    private void handleStreamedMessageFragment(String aSessionOwner, Project aProject,
            UUID responseId, MChatMessage responseMessage)
    {
        dispatchMessage(aSessionOwner, aProject, responseMessage);
    }

    private List<MTextMessage> generateEphemeralMessages(Project aProject, MTextMessage aMessage)
    {
        var messages = new ArrayList<MTextMessage>();

        if (!properties.getChat().getCapabilities().contains(CAP_TOOLS)) {
            for (var retriever : retrieverExtensionPoint.getExtensions(aProject)) {
                messages.addAll(retriever.retrieve(aProject, aMessage));
            }
        }

        return messages;
    }

    private List<MTextMessage> generateSystemMessages(Project aProject)
    {
        var establishIdentity = join("\n\n", "Your name is " + properties.getNickname() + ".",
                "You are a helpful assistant within the annotation tool INCEpTION.",
                "INCEpTION always refers to the annotation tool, never anything else such as the movie.",
                "Do not include references to INCEpTION unless the user explicitly asks about the environment itself.",
                "When reasoning, you must explicitly state the exact name of the tool you intend to call. "
                        + "Do not say 'I will read the file'. Say 'I will call read_document to read the file'.");

        // If you use information from the user manual in your response, prepend it with
        // "According to the user manual".
        // """,
        // """
        // Respectfully decline to respond to questions or instructions that do not seem to
        // be
        // related to INCEpTION or text annotation as that would be beyond your scope.
        // """

        var primeDirectives = new ArrayList<String>();
        primeDirectives.add(establishIdentity);

        if (!properties.getChat().getCapabilities().contains(CAP_TOOLS)) {
            for (var retriever : retrieverExtensionPoint.getExtensions(aProject)) {
                primeDirectives.addAll(retriever.getSystemPrompts());
            }
        }

        return asList(MTextMessage.builder() //
                .withRole(SYSTEM).internal().ephemeral() //
                .withContent(join("\n\n", primeDirectives)) //
                .build());
    }

    private List<MChatMessage> limitConversationToContextLength(List<MTextMessage> aSystemMessages,
            List<? extends MChatMessage> aEphemeralMessages,
            List<? extends MChatMessage> aRecentMessages, MTextMessage aLatestUserMessage,
            int aContextLength)
        throws IOException
    {
        var encoding = getEncoding();
        var limit = floorDiv(aContextLength * 90, 100);

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
                var msgTokens = encoding.countTokensOrdinary(msg.content());
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
            var latestUserMsgTokens = encoding.countTokensOrdinary(aLatestUserMessage.content());
            recentTokens += latestUserMsgTokens;
            allTokens += latestUserMsgTokens;
            tailMessages.addLast(aLatestUserMessage);
        }

        // Add ephemeral messages as context limit allows
        var ephemeralMsgIterator = aEphemeralMessages.listIterator(aEphemeralMessages.size());
        while (ephemeralMsgIterator.hasPrevious() && allTokens < limit) {
            var msg = ephemeralMsgIterator.previous();
            var textRepresentation = msg.textRepresentation();
            var msgTokens = encoding.countTokensOrdinary(textRepresentation);
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
            var msgTokens = encoding.countTokensOrdinary(textRepresentation);
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

    @SuppressWarnings({ "unchecked" })
    @Override
    public Optional<ClientSidePreferenceKey<ClientSidePreferenceMapValue>> getUserPreferencesKey()
    {
        return Optional.of(KEY_ASSISTANT_PREFS);
    }

    @Override
    public Optional<Schema> getUserPreferencesSchema() throws IOException
    {
        return userPreferencesSchema.get();
    }

    private Encoding getEncoding()
    {
        // We don't really know which tokenizer the LLM uses. In case
        // the tokenizer we use counts fewer tokens than the one user by
        // the model and also to cover for message encoding JSON overhead,
        // we try to use only 90% of the context window.
        var encoding = encodingRegistry.getEncoding(properties.getChat().getEncoding())
                .orElseThrow(() -> new IllegalStateException(
                        "Unknown encoding: " + properties.getChat().getEncoding()));
        return encoding;
    }

    private void autoDetectModelCapabilities()
    {
        var chatProperties = (AssistantChatPropertiesImpl) properties.getChat();

        var autoDetectCapabilities = chatProperties.getCapabilities()
                .contains(AUTO_DETECT_CAPABILITIES);
        var autoDetectContextLength = chatProperties.getContextLength() <= 0;

        synchronized (chatProperties) {
            if (autoDetectCapabilities || autoDetectContextLength) {
                OllamaShowResponse modelInfo = null;
                try {
                    LOG.info("Contacting [{}] to retrieve information about model [{}]...",
                            properties.getUrl(), chatProperties.getModel());
                    modelInfo = ollamaClient.getModelInfo(properties.getUrl(),
                            OllamaShowRequest.builder().withModel(chatProperties.getModel()) //
                                    .withApiKey(properties.getApiKey()) //
                                    .build());
                }
                catch (Exception e) {
                    if (LOG.isDebugEnabled()) {
                        LOG.warn("Unable to retrieve model information - using defaults", e);
                    }
                    else {
                        LOG.warn("Unable to retrieve model information - using defaults");
                    }
                }

                if (autoDetectContextLength) {
                    if (modelInfo != null && modelInfo.info() != null
                            && modelInfo.info().getContextLength() != null) {
                        chatProperties.setContextLength(modelInfo.info().getContextLength());
                        LOG.info("Auto-detected context length: {}",
                                chatProperties.getContextLength());
                    }
                    else {
                        chatProperties.setContextLength(4096);
                        LOG.info(
                                "Unable to auto-detect context length - using default of {} tokens",
                                chatProperties.getContextLength());
                    }
                }

                if (autoDetectCapabilities) {
                    if (modelInfo != null && CollectionUtils.isNotEmpty(modelInfo.capabilities())) {
                        chatProperties.setCapabilities(modelInfo.capabilities());
                        LOG.info("Auto-detected capabilties dimension of model [{}]: {}",
                                chatProperties.getModel(), chatProperties.getCapabilities());
                    }
                    else {
                        chatProperties.setCapabilities(Set.of(CAP_COMPLETION));
                        LOG.info("Unable to auto-detect capabilities - using default: {}",
                                chatProperties.getCapabilities());
                    }
                }
            }
        }
    }

    static record AssistantStateKey(String user, long projectId) {}
}

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

import static de.tudarmstadt.ukp.inception.assistant.model.MChatRoles.SYSTEM;
import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.toPrettyJsonString;
import static java.lang.Math.floorDiv;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.security.core.session.SessionRegistry;

import com.knuddels.jtokkit.api.EncodingRegistry;
import com.networknt.schema.JsonSchema;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.assistant.config.AssistantProperties;
import de.tudarmstadt.ukp.inception.assistant.model.MCallResponse;
import de.tudarmstadt.ukp.inception.assistant.model.MChatMessage;
import de.tudarmstadt.ukp.inception.assistant.model.MMessage;
import de.tudarmstadt.ukp.inception.assistant.model.MRemoveConversationCommand;
import de.tudarmstadt.ukp.inception.assistant.model.MTextMessage;
import de.tudarmstadt.ukp.inception.assistant.retriever.RetrieverExtensionPoint;
import de.tudarmstadt.ukp.inception.preferences.ClientSidePreferenceKey;
import de.tudarmstadt.ukp.inception.preferences.ClientSidePreferenceMapValue;
import de.tudarmstadt.ukp.inception.preferences.ClientSideUserPreferencesProvider;
import de.tudarmstadt.ukp.inception.project.api.event.AfterProjectRemovedEvent;
import de.tudarmstadt.ukp.inception.project.api.event.BeforeProjectRemovedEvent;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaClient;
import de.tudarmstadt.ukp.inception.support.io.WatchedResourceFile;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

public class AssistantServiceImpl
    implements AssistantService, ClientSideUserPreferencesProvider
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final SessionRegistry sessionRegistry;
    private final SimpMessagingTemplate msgTemplate;
    private final ConcurrentMap<AssistentStateKey, AssistentState> states;
    private final OllamaClient ollamaClient;
    private final AssistantProperties properties;
    private final EncodingRegistry encodingRegistry;
    private final RetrieverExtensionPoint retrieverExtensionPoint;

    private static final ClientSidePreferenceKey<ClientSidePreferenceMapValue> KEY_ASSISTANT_PREFS = //
            new ClientSidePreferenceKey<>(ClientSidePreferenceMapValue.class, "assistant/general");

    private WatchedResourceFile<JsonSchema> userPreferencesSchema;

    public AssistantServiceImpl(SessionRegistry aSessionRegistry,
            SimpMessagingTemplate aMsgTemplate, OllamaClient aOllamaClient,
            AssistantProperties aProperties, EncodingRegistry aEncodingRegistry,
            RetrieverExtensionPoint aRetrieverExtensionPoint)
    {
        sessionRegistry = aSessionRegistry;
        msgTemplate = aMsgTemplate;
        states = new ConcurrentHashMap<>();
        ollamaClient = aOllamaClient;
        properties = aProperties;
        encodingRegistry = aEncodingRegistry;
        retrieverExtensionPoint = aRetrieverExtensionPoint;

        var userPreferencesSchemaFile = getClass()
                .getResource("AssistantServiceUserPreferences.schema.json");
        userPreferencesSchema = new WatchedResourceFile<>(userPreferencesSchemaFile,
                JSONUtil::loadJsonSchema);
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
            clearState(username);
        }
    }

    @EventListener
    public void onBeforeProjectRemoved(BeforeProjectRemovedEvent aEvent)
    {
        clearState(aEvent.getProject());
    }

    @EventListener
    public void onAfterProjectRemoved(AfterProjectRemovedEvent aEvent)
    {
        clearState(aEvent.getProject());
    }

    @Override
    public List<MTextMessage> getAllChatMessages(String aSessionOwner, Project aProject)
    {
        var state = getState(aSessionOwner, aProject);

        return state.getMessages().stream() //
                .filter(MTextMessage.class::isInstance) //
                .map(MTextMessage.class::cast) //
                .filter(msg -> state.isDebugMode() || !msg.internal()) //
                .toList();
    }

    @Override
    public List<MTextMessage> getChatMessages(String aSessionOwner, Project aProject)
    {
        var state = getState(aSessionOwner, aProject);

        // In dev mode, we also record internal messages, so we need to filter them out again here
        return state.getMessages().stream() //
                .filter(MTextMessage.class::isInstance) //
                .map(MTextMessage.class::cast) //
                .filter(msg -> !msg.internal()) //
                .toList();
    }

    void recordMessage(String aSessionOwner, Project aProject, MChatMessage aMessage)
    {
        if (!isDebugMode(aSessionOwner, aProject) && aMessage.ephemeral()) {
            return;
        }

        var state = getState(aSessionOwner, aProject);
        state.upsertMessage(aMessage);
    }

    @Override
    public void dispatchMessage(String aSessionOwner, Project aProject, MMessage aMessage)
    {
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
        synchronized (states) {
            states.entrySet().stream() //
                    .filter(e -> aSessionOwner.equals(e.getKey().user())
                            && Objects.equals(aProject.getId(), e.getKey().projectId)) //
                    .map(Entry::getValue) //
                    .forEach(state -> state.clearMessages());
        }

        dispatchMessage(aSessionOwner, aProject, new MRemoveConversationCommand());
    }

    @Override
    public void setDebugMode(String aSessionOwner, Project aProject, boolean aOnOff)
    {
        synchronized (states) {
            getState(aSessionOwner, aProject).setDebugMode(aOnOff);
        }
    }

    @Override
    public boolean isDebugMode(String aSessionOwner, Project aProject)
    {
        synchronized (states) {
            return getState(aSessionOwner, aProject).isDebugMode();
        }
    }

    @Override
    public MTextMessage processInternalMessageSync(String aSessionOwner, Project aProject,
            MTextMessage aMessage)
        throws IOException
    {
        Validate.isTrue(aMessage.internal());

        if (isDebugMode(aSessionOwner, aProject)) {
            recordMessage(aSessionOwner, aProject, aMessage);
            dispatchMessage(aSessionOwner, aProject, aMessage);
        }

        var assistant = new ChatContext(properties, ollamaClient, aSessionOwner, aProject);
        return assistant.chat(asList(aMessage));
    }

    @Override
    public <T> MCallResponse<T> processInternalCallSync(String aSessionOwner, Project aProject,
            Class<T> aType, MTextMessage aMessage)
        throws IOException
    {
        Validate.isTrue(aMessage.internal());

        if (isDebugMode(aSessionOwner, aProject)) {
            recordMessage(aSessionOwner, aProject, aMessage);
            dispatchMessage(aSessionOwner, aProject, aMessage);
        }

        var assistant = new ChatContext(properties, ollamaClient, aSessionOwner, aProject);
        var result = assistant.call(aType, asList(aMessage));

        if (isDebugMode(aSessionOwner, aProject)) {
            var resultMessage = MTextMessage.builder() //
                    .withRole(SYSTEM).internal().ephemeral() //
                    .withActor(aMessage.actor()) //
                    .withMessage("```json\n" + toPrettyJsonString(result.payload()) + "\n```") //
                    .withPerformance(result.performance()) //
                    .build();
            recordMessage(aSessionOwner, aProject, resultMessage);
            dispatchMessage(aSessionOwner, aProject, resultMessage);
        }

        return result;
    }

    @Override
    public void processAgentMessage(String aSessionOwner, Project aProject, MTextMessage aMessage,
            MTextMessage... aContextMessages)
    {
        var assistant = new ChatContext(properties, ollamaClient, aSessionOwner, aProject);

        // Dispatch message early so the front-end can enter waiting state
        dispatchMessage(aSessionOwner, aProject, aMessage);

        try {
            var systemMessages = generateSystemMessages();

            recordMessage(aSessionOwner, aProject, aMessage);

            var recentConversation = limitConversationToContextLength(systemMessages, emptyList(),
                    emptyList(), aMessage, properties.getChat().getContextLength());

            var responseMessage = assistant.chat(recentConversation,
                    (id, r) -> handleStreamedMessageFragment(aSessionOwner, aProject, id, r));

            recordMessage(aSessionOwner, aProject, responseMessage);

            dispatchMessage(aSessionOwner, aProject, responseMessage.withoutContent());
        }
        catch (IOException e) {
            var errorMessage = MTextMessage.builder() //
                    .withActor("Error").withRole(SYSTEM).internal().ephemeral() //
                    .withMessage("Error: " + e.getMessage()) //
                    .build();
            recordMessage(aSessionOwner, aProject, errorMessage);
            dispatchMessage(aSessionOwner, aProject, errorMessage);
        }
    }

    @Override
    public void processUserMessage(String aSessionOwner, Project aProject, MTextMessage aMessage,
            MTextMessage... aContextMessages)
    {
        var assistant = new ChatContext(properties, ollamaClient, aSessionOwner, aProject);

        // Dispatch message early so the front-end can enter waiting state
        dispatchMessage(aSessionOwner, aProject, aMessage);

        try {
            var systemMessages = generateSystemMessages();

            List<MTextMessage> contextMessages = isNotEmpty(aContextMessages)
                    ? asList(aContextMessages)
                    : emptyList();
            List<MTextMessage> ephemeralMessages = contextMessages.isEmpty()
                    ? generateEphemeralMessages(assistant, aMessage)
                    : contextMessages;
            List<MTextMessage> conversationMessages = contextMessages.isEmpty()
                    ? getChatMessages(aSessionOwner, aProject)
                    : emptyList();

            // We record the message only now to ensure it is not included in the listMessages above
            recordMessage(aSessionOwner, aProject, aMessage);

            if (isDebugMode(aSessionOwner, aProject)) {
                for (var msg : ephemeralMessages) {
                    recordMessage(aSessionOwner, aProject, msg);
                    dispatchMessage(aSessionOwner, aProject, msg);
                }
            }

            var recentConversation = limitConversationToContextLength(systemMessages,
                    ephemeralMessages, conversationMessages, aMessage,
                    properties.getChat().getContextLength());

            var responseMessage = assistant.chat(recentConversation,
                    (id, r) -> handleStreamedMessageFragment(aSessionOwner, aProject, id, r));

            recordMessage(aSessionOwner, aProject, responseMessage);

            dispatchMessage(aSessionOwner, aProject, responseMessage.withoutContent());
        }
        catch (IOException e) {
            var errorMessage = MTextMessage.builder() //
                    .withActor("Error").withRole(SYSTEM).internal().ephemeral() //
                    .withMessage("Error: " + e.getMessage()) //
                    .build();
            recordMessage(aSessionOwner, aProject, errorMessage);
            dispatchMessage(aSessionOwner, aProject, errorMessage);
        }
    }

    private void handleStreamedMessageFragment(String aSessionOwner, Project aProject,
            UUID responseId, MTextMessage responseMessage)
    {
        recordMessage(aSessionOwner, aProject, responseMessage);
        dispatchMessage(aSessionOwner, aProject, responseMessage);
    }

    private List<MTextMessage> generateEphemeralMessages(ChatContext aAssistant,
            MTextMessage aMessage)
    {
        var messages = new ArrayList<MTextMessage>();

        for (var retriever : retrieverExtensionPoint.getExtensions(aAssistant.getProject())) {
            messages.addAll(retriever.retrieve(aAssistant, aMessage));
        }

        return messages;
    }

    private List<MTextMessage> generateSystemMessages()
    {
        var primeDirectives = asList("Your name is " + properties.getNickname() + ".",
                "You are a helpful assistant within the annotation tool INCEpTION.",
                "INCEpTION always refers to the annotation tool, never anything else such as the movie.",
                "Do not include references to INCEpTION unless the user explicitly asks about the environment itself."
        // If you use information from the user manual in your response, prepend it with
        // "According to the user manual".
        // """,
        // """
        // Respectfully decline to respond to questions or instructions that do not seem to be
        // related to INCEpTION or text annotation as that would be beyond your scope.
        // """
        );

        return asList(MTextMessage.builder() //
                .withRole(SYSTEM).internal().ephemeral() //
                .withMessage(join("\n\n", primeDirectives)) //
                .build());
    }

    private List<MTextMessage> limitConversationToContextLength(List<MTextMessage> aSystemMessages,
            List<MTextMessage> aEphemeralMessages, List<MTextMessage> aRecentMessages,
            MTextMessage aLatestUserMessage, int aContextLength)
    {
        // We don't really know which tokenizer the LLM uses. In case
        // the tokenizer we use counts fewer tokens than the one user by
        // the model and also to cover for message encoding JSON overhead,
        // we try to use only 90% of the context window.
        var encoding = encodingRegistry.getEncoding(properties.getChat().getEncoding())
                .orElseThrow(() -> new IllegalStateException(
                        "Unknown encoding: " + properties.getChat().getEncoding()));
        var limit = floorDiv(aContextLength * 90, 100);

        var headMessages = new ArrayList<MTextMessage>();
        var tailMessages = new LinkedList<MTextMessage>();

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
                var msgTokens = encoding.countTokensOrdinary(msg.message());
                if (allTokens + msgTokens > limit) {
                    LOG.trace("System message exceeds remaining token limit ({}): [{}]", msgTokens,
                            msg.message());
                    continue;
                }

                allTokens += msgTokens;
                systemTokens += msgTokens;
                headMessages.add(msg);
            }
        }

        // Unconditionally the latest user message
        var latestUserMsgTokens = encoding.countTokensOrdinary(aLatestUserMessage.message());
        recentTokens += latestUserMsgTokens;
        allTokens += latestUserMsgTokens;
        tailMessages.addLast(aLatestUserMessage);

        // Add ephemeral messages as context limit allows
        var ephemeralMsgIterator = aEphemeralMessages.listIterator(aEphemeralMessages.size());
        while (ephemeralMsgIterator.hasPrevious() && allTokens < limit) {
            var msg = ephemeralMsgIterator.previous();
            var msgTokens = encoding.countTokensOrdinary(msg.message());
            if (allTokens + msgTokens > limit) {
                LOG.trace("Ephemeral message exceeds remaining token limit ({}): [{}]", msgTokens,
                        msg.message());
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
            var msgTokens = encoding.countTokensOrdinary(msg.message());
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
    public Optional<JsonSchema> getUserPreferencesSchema() throws IOException
    {
        return userPreferencesSchema.get();
    }

    private AssistentState getState(String aSessionOwner, Project aProject)
    {
        synchronized (states) {
            return states.computeIfAbsent(new AssistentStateKey(aSessionOwner, aProject.getId()),
                    (v) -> newState());
        }
    }

    private AssistentState newState()
    {
        var state = new AssistentState();
        // state.upsertMessage(MTextMessage.builder() //
        // .withActor(properties.getNickname()) //
        // .withRole(SYSTEM) //
        // .withMessage("Hi") //
        // .build());
        return state;
    }

    private void clearState(Project aProject)
    {
        Validate.notNull(aProject, "Project must be specified");

        synchronized (states) {
            states.keySet().removeIf(key -> Objects.equals(aProject.getId(), key.projectId()));
        }
    }

    private void clearState(String aSessionOwner)
    {
        Validate.notNull(aSessionOwner, "Username must be specified");

        synchronized (states) {
            states.keySet().removeIf(key -> aSessionOwner.equals(key.user()));
        }
    }

    private static class AssistentState
    {
        private LinkedList<MMessage> messages = new LinkedList<>();
        private boolean debugMode;

        public List<MMessage> getMessages()
        {
            return unmodifiableList(new ArrayList<>(messages));
        }

        public void clearMessages()
        {
            synchronized (messages) {
                messages.clear();
            }
        }

        public void setDebugMode(boolean aOnOff)
        {
            debugMode = aOnOff;
        }

        public boolean isDebugMode()
        {
            return debugMode;
        }

        public void upsertMessage(MMessage aMessage)
        {
            synchronized (messages) {
                var found = false;
                if (aMessage instanceof MTextMessage textMsg) {
                    var i = messages.listIterator(messages.size());

                    // If a message with the same ID already exists, update it
                    while (i.hasPrevious() && !found) {
                        var m = i.previous();
                        if (m instanceof MTextMessage existingTextMsg) {
                            if (Objects.equals(existingTextMsg.id(), textMsg.id())) {
                                if (textMsg.done()) {
                                    i.set(textMsg);
                                }
                                else {
                                    i.set(existingTextMsg.append(textMsg));
                                }
                                found = true;
                            }
                        }
                    }
                }

                // Otherwise add it
                if (!found) {
                    messages.add(aMessage);
                }
            }
        }
    }

    private static record AssistentStateKey(String user, long projectId) {}
}

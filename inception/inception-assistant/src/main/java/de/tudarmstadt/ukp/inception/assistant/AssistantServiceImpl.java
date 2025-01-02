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

import static de.tudarmstadt.ukp.inception.assistant.model.MAssistantChatRoles.ASSISTANT;
import static de.tudarmstadt.ukp.inception.assistant.model.MAssistantChatRoles.SYSTEM;
import static java.lang.Math.floorDiv;
import static java.lang.String.join;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
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

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.assistant.config.AssistantProperties;
import de.tudarmstadt.ukp.inception.assistant.model.MAssistantClearCommand;
import de.tudarmstadt.ukp.inception.assistant.model.MAssistantMessage;
import de.tudarmstadt.ukp.inception.assistant.model.MAssistantTextMessage;
import de.tudarmstadt.ukp.inception.assistant.retriever.RetrieverExtensionPoint;
import de.tudarmstadt.ukp.inception.project.api.event.AfterProjectRemovedEvent;
import de.tudarmstadt.ukp.inception.project.api.event.BeforeProjectRemovedEvent;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaChatMessage;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaChatRequest;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaChatResponse;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaClient;
import de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client.OllamaOptions;

public class AssistantServiceImpl
    implements AssistantService
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final SessionRegistry sessionRegistry;
    private final SimpMessagingTemplate msgTemplate;
    private final ConcurrentMap<AssistentStateKey, AssistentState> states;
    private final OllamaClient ollamaClient;
    private final AssistantProperties properties;
    private final EncodingRegistry encodingRegistry;
    private final RetrieverExtensionPoint retrieverExtensionPoint;

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
    public List<MAssistantTextMessage> getAllChatMessages(String aSessionOwner, Project aProject)
    {
        var state = getState(aSessionOwner, aProject);

        return state.getMessages().stream() //
                .filter(MAssistantTextMessage.class::isInstance) //
                .map(MAssistantTextMessage.class::cast) //
                .toList();
    }

    @Override
    public List<MAssistantTextMessage> getChatMessages(String aSessionOwner, Project aProject)
    {
        var state = getState(aSessionOwner, aProject);

        // In dev mode, we also record internal messages, so we need to filter them out again here
        return state.getMessages().stream() //
                .filter(MAssistantTextMessage.class::isInstance) //
                .map(MAssistantTextMessage.class::cast) //
                .filter(msg -> !msg.internal()) //
                .toList();
    }

    void recordMessage(String aSessionOwner, Project aProject, MAssistantMessage aMessage)
    {
        var state = getState(aSessionOwner, aProject);
        state.addMessage(aMessage);
    }

    void dispatchMessage(String aSessionOwner, Project aProject, MAssistantMessage aMessage)
    {
        // LOG.trace("Dispatching assistant message: {}", aMessage);
        var topic = AssistantWebsocketController.getChannel(aProject);
        msgTemplate.convertAndSend("/topic" + topic, aMessage);
    }

    @Override
    public void clearConversation(String aSessionOwner, Project aProject)
    {
        synchronized (states) {
            states.keySet().removeIf(key -> aSessionOwner.equals(key.user())
                    && Objects.equals(aProject.getId(), key.projectId));
        }

        dispatchMessage(aSessionOwner, aProject, new MAssistantClearCommand());
    }

    @Override
    public void processUserMessage(String aSessionOwner, Project aProject,
            MAssistantTextMessage aMessage)
    {
        // Dispatch message early so the front-end can enter waiting state
        dispatchMessage(aSessionOwner, aProject, aMessage);

        var responseId = UUID.randomUUID();
        try {
            var systemMessages = generateSystemMessages(aSessionOwner, aProject, aMessage);
            var transientMessages = generateTransientMessages(aSessionOwner, aProject, aMessage);
            var conversationMessages = getChatMessages(aSessionOwner, aProject);

            // We record the message only now to ensure it is not included in the listMessages above
            recordMessage(aSessionOwner, aProject, aMessage);

            if (properties.isDevMode()) {
                for (var msg : transientMessages) {
                    recordMessage(aSessionOwner, aProject, msg);
                    dispatchMessage(aSessionOwner, aProject, msg);
                }
            }

            var recentConversation = limitConversationToContextLength(systemMessages,
                    transientMessages, conversationMessages, aMessage,
                    properties.getChat().getContextLength());

            var request = OllamaChatRequest.builder() //
                    .withModel(properties.getChat().getModel()) //
                    .withStream(true) //
                    .withMessages(recentConversation.stream() //
                            .map(msg -> new OllamaChatMessage(msg.role(), msg.message())) //
                            .toList()) //
                    .withOption(OllamaOptions.NUM_CTX, properties.getChat().getContextLength()) //
                    .withOption(OllamaOptions.TOP_P, properties.getChat().getTopP()) //
                    .withOption(OllamaOptions.TOP_K, properties.getChat().getTopK()) //
                    .withOption(OllamaOptions.REPEAT_PENALTY,
                            properties.getChat().getRepeatPenalty()) //
                    .withOption(OllamaOptions.TEMPERATURE, properties.getChat().getTemperature()) //
                    .build();

            var response = ollamaClient.generate(properties.getUrl(), request,
                    r -> handleStreamedMessageFragment(aSessionOwner, aProject, responseId, r));

            var responseMessage = MAssistantTextMessage.builder() //
                    .withId(responseId) //
                    .withRole(ASSISTANT) //
                    .withMessage(response) //
                    .build();
            recordMessage(aSessionOwner, aProject, responseMessage);
            dispatchMessage(aSessionOwner, aProject, responseMessage);
        }
        catch (IOException e) {
            var errorMessage = MAssistantTextMessage.builder() //
                    .withId(responseId) //
                    .withRole(SYSTEM) //
                    .withMessage("Error: " + e.getMessage()) //
                    .build();
            recordMessage(aSessionOwner, aProject, errorMessage);
            dispatchMessage(aSessionOwner, aProject, errorMessage);
        }
    }

    private void handleStreamedMessageFragment(String aSessionOwner, Project aProject,
            UUID responseId, OllamaChatResponse r)
    {
        var responseMessage = MAssistantTextMessage.builder() //
                .withId(responseId) //
                .withRole(ASSISTANT) //
                .withMessage(r.getMessage().content()) //
                .notDone() //
                .build();
        recordMessage(aSessionOwner, aProject, responseMessage);
        dispatchMessage(aSessionOwner, aProject, responseMessage);
    }

    private List<MAssistantTextMessage> generateTransientMessages(String aSessionOwner,
            Project aProject, MAssistantTextMessage aMessage)
    {
        var transientMessages = new ArrayList<MAssistantTextMessage>();

        for (var retriever : retrieverExtensionPoint.getExtensions(aProject)) {
            transientMessages.addAll(retriever.retrieve(aSessionOwner, aProject, aMessage));
        }

        return transientMessages;
    }

    private List<MAssistantTextMessage> generateSystemMessages(String aSessionOwner,
            Project aProject, MAssistantTextMessage aMessage)
    {
        var primeDirectives = asList(
                "You are Dominick, a helpful assistant within the annotation tool INCEpTION.",
                "INCEpTION always refers to the annotation tool, never anything else such as the movie.",
                "Do not include references to INCEpTION unless the user explicitly asks about the environment itself.",
                "If the source of an information is known, provide it in your response."
        // If you use information from the user manual in your response, prepend it with
        // "According to the user manual".
        // """,
        // """
        // Respectfully decline to respond to questions or instructions that do not seem to be
        // related to INCEpTION or text annotation as that would be beyond your scope.
        // """
        );

        return asList(MAssistantTextMessage.builder() //
                .withRole(SYSTEM).internal() //
                .withMessage(join("\n\n", primeDirectives)) //
                .build());
    }

    private List<MAssistantTextMessage> limitConversationToContextLength(
            List<MAssistantTextMessage> aSystemMessages,
            List<MAssistantTextMessage> aTransientMessages,
            List<MAssistantTextMessage> aRecentMessages, MAssistantTextMessage aLatestUserMessage,
            int aContextLength)
    {
        // We don't really know which tokenizer the LLM uses. In case
        // the tokenizer we use counts fewer tokens than the one user by
        // the model and also to cover for message encoding JSON overhead,
        // we try to use only 90% of the context window.
        var encoding = encodingRegistry.getEncoding(properties.getChat().getEncoding())
                .orElseThrow(() -> new IllegalStateException(
                        "Unknown encoding: " + properties.getChat().getEncoding()));
        var limit = floorDiv(aContextLength * 90, 100);

        var headMessages = new ArrayList<MAssistantTextMessage>();
        var tailMessages = new LinkedList<MAssistantTextMessage>();

        var totalMessages = aSystemMessages.size() + aTransientMessages.size()
                + aRecentMessages.size() + 1;
        var allTokens = 0;
        var systemTokens = 0;
        var recentTokens = 0;
        var transientTokens = 0;

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

        // Add transient messages as context limit allows
        var transientMsgIterator = aTransientMessages.listIterator(aTransientMessages.size());
        while (transientMsgIterator.hasPrevious() && allTokens < limit) {
            var msg = transientMsgIterator.previous();
            var msgTokens = encoding.countTokensOrdinary(msg.message());
            if (allTokens + msgTokens > limit) {
                LOG.trace("Transient message exceeds remaining token limit ({}): [{}]", msgTokens,
                        msg.message());
                continue;
            }

            allTokens += msgTokens;
            transientTokens += msgTokens;
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
                "Reduced from {} to {} messages with a total of {} / {} tokens (system: {}, transient: {}, recent: {} )",
                totalMessages, headMessages.size() + tailMessages.size(), allTokens, limit,
                systemTokens, transientTokens, recentTokens);

        return allMessages;
    }

    private AssistentState getState(String aSessionOwner, Project aProject)
    {
        synchronized (states) {
            return states.computeIfAbsent(new AssistentStateKey(aSessionOwner, aProject.getId()),
                    (v) -> new AssistentState());
        }
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
        private LinkedList<MAssistantMessage> messages = new LinkedList<>();

        public List<MAssistantMessage> getMessages()
        {
            return new ArrayList<>(messages);
        }

        public void addMessage(MAssistantMessage aMessage)
        {
            synchronized (messages) {
                var found = false;
                if (aMessage instanceof MAssistantTextMessage textMsg) {
                    var i = messages.listIterator(messages.size());

                    // If a message with the same ID already exists, update it
                    while (i.hasPrevious() && !found) {
                        var m = i.previous();
                        if (m instanceof MAssistantTextMessage existingTextMsg) {
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

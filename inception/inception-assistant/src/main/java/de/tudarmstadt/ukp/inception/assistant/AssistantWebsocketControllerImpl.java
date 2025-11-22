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

import static de.tudarmstadt.ukp.inception.assistant.model.MChatRoles.USER;
import static de.tudarmstadt.ukp.inception.support.logging.Logging.KEY_PROJECT_ID;
import static de.tudarmstadt.ukp.inception.support.logging.Logging.KEY_REPOSITORY_PATH;
import static de.tudarmstadt.ukp.inception.support.logging.Logging.KEY_USERNAME;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.PARAM_DOCUMENT;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.PARAM_PROJECT;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.PARAM_USER;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.assistant.model.MChatMessage;
import de.tudarmstadt.ukp.inception.assistant.model.MTextMessage;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import jakarta.servlet.ServletContext;

@Controller
@RequestMapping(AssistantWebsocketController.BASE_URL)
@ConditionalOnWebApplication
@ConditionalOnExpression("${websocket.enabled:true} and ${assistant.enabled:false}")
public class AssistantWebsocketControllerImpl
    implements AssistantWebsocketController
{
    private final AssistantService assistantService;
    private final ProjectService projectService;
    private final DocumentService documentService;
    private final UserDao userService;
    private final RepositoryProperties repositoryProperties;

    @Autowired
    public AssistantWebsocketControllerImpl(ServletContext aServletContext,
            SimpMessagingTemplate aMsgTemplate, AssistantService aAssistantService,
            ProjectService aProjectService, UserDao aUserService, DocumentService aDocumentService,
            RepositoryProperties aRepositoryProperties)
    {
        assistantService = aAssistantService;
        projectService = aProjectService;
        userService = aUserService;
        documentService = aDocumentService;
        repositoryProperties = aRepositoryProperties;
    }

    @SubscribeMapping(PROJECT_ASSISTANT_TOPIC_TEMPLATE)
    public List<MChatMessage> onSubscribeToAssistantMessages(
            SimpMessageHeaderAccessor aHeaderAccessor, Principal aPrincipal, //
            @DestinationVariable(PARAM_PROJECT) long aProjectId)
        throws IOException
    {
        var project = projectService.getProject(aProjectId);
        return assistantService.getUserChatHistory(aPrincipal.getName(), project);
    }

    @MessageMapping(PROJECT_ASSISTANT_TOPIC_TEMPLATE)
    public void onUserMessage(SimpMessageHeaderAccessor aHeaderAccessor, Principal aPrincipal, //
            @DestinationVariable(PARAM_PROJECT) long aProjectId, //
            @Header(PARAM_USER) String dataOwner, //
            @Header(PARAM_DOCUMENT) long documentId, //
            @Payload String aMessage)
        throws IOException
    {
        var project = projectService.getProject(aProjectId);
        var document = documentService.getSourceDocument(aProjectId, documentId);
        var sessionOwner = userService.get(aPrincipal.getName());
        var message = MTextMessage.builder() //
                .withActor(sessionOwner.getUiName()) //
                .withRole(USER) //
                .withMessage(aMessage) //
                .build();

        try {
            // We are in a new thread. Set up thread-specific MDC
            if (repositoryProperties != null) {
                MDC.put(KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
            }

            MDC.put(KEY_USERNAME, sessionOwner.getUsername());

            if (project != null) {
                MDC.put(KEY_PROJECT_ID, String.valueOf(project.getId()));
            }

            assistantService.processUserMessage(aPrincipal.getName(), project, document, dataOwner,
                    message);
        }
        finally {
            MDC.remove(KEY_REPOSITORY_PATH);
            MDC.remove(KEY_USERNAME);
            MDC.remove(KEY_PROJECT_ID);
        }
    }

    @SendTo(PROJECT_ASSISTANT_TOPIC_TEMPLATE)
    public MTextMessage send(MTextMessage aUpdate)
    {
        return aUpdate;
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Throwable exception)
    {
        return exception.getMessage();
    }
}

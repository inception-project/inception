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
package de.tudarmstadt.ukp.inception.experimental.api.websocket;

import java.io.IOException;
import java.util.Objects;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.slf4j.MDC;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;
import de.tudarmstadt.ukp.inception.experimental.api.message.AnnotationMessage;
import de.tudarmstadt.ukp.inception.experimental.api.message.ConnectionEstablishedMessage;
import de.tudarmstadt.ukp.inception.experimental.api.message.NewDocumentMessage;
import de.tudarmstadt.ukp.inception.experimental.api.message.ViewportMessage;

@Controller
public class WebsocketControllerImpl
    implements WebsocketController
{
    private final DocumentService documentService;
    private final ProjectService projectService;
    private final UserDao userDao;
    private final RepositoryProperties repositoryProperties;
    private final SimpMessagingTemplate simpMessagingTemplate;

    /**
     * ----------------- PUB / SUB CHANNELS ---------------
     **/
    private static final String SERVER_SEND_CLIENT_CONNECTION_MESSAGE = "/queue/connection_message/";

    private static final String SERVER_RECEIVE_CLIENT_NEW_DOCUMENT = "/new_document_by_client";
    private static final String SERVER_SEND_CLIENT_NEW_DOCUMENT = "/queue/new_document_for_client/";

    private static final String SERVER_RECEIVE_CLIENT_NEW_VIEWPORT = "/new_viewport_by_client";
    private static final String SERVER_SEND_CLIENT_NEW_VIEWPORT = "/queue/new_viewport_for_client/";

    private static final String SERVER_RECEIVE_CLIENT_SELECTED_ANNOTATION = "/select_annotation_by_client";
    private static final String SERVER_SEND_CLIENT_SELECTED_ANNOTATION = "/queue/selected_annotation_for_client/";

    private static final String SERVER_RECEIVE_CLIENT_NEW_ANNOTATION = "/new_annotation_by_client/";
    private static final String SERVER_SEND_CLIENT_NEW_ANNOTATION = "/topic/annotation_created_for_clients/";

    private static final String SERVER_RECEIVE_CLIENT_DELETE_ANNOTATION = "/delete_annotation_by_client";
    private static final String SERVER_SEND_CLIENT_DELETE_ANNOTATION = "/topic/annotation_deleted_for_clients/";

    /**
     * -----------------------------------------------------
     **/

    public WebsocketControllerImpl(ProjectService aProjectService, DocumentService aDocumentService,
            UserDao aUserDao, RepositoryProperties aRepositoryProperties,
            SimpMessagingTemplate aSimpMessagingTemplate)
    {
        this.projectService = aProjectService;
        this.documentService = aDocumentService;
        this.userDao = aUserDao;
        this.repositoryProperties = aRepositoryProperties;
        this.simpMessagingTemplate = aSimpMessagingTemplate;

        MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
    }

    // ----------- ERROR HANDLER ------------- //
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Throwable aException)
    {
        return aException.getMessage();
    }
    // ---------------------------------------- //

    // ------- TO BE REMOVED, INFO ONLY --------- //
    @EventListener
    public void connectionEstablished(SessionConnectedEvent aSce) throws IOException, CASException
    {
        System.out.println("CONNECTION ESTABLISHED");
        String user = Objects.requireNonNull(aSce.getUser()).getName();
        ConnectionEstablishedMessage connectionEstablishedMessage = new ConnectionEstablishedMessage(
                user, "Doc4", "Annotation Study");
        String msg = JSONUtil.toJsonString(connectionEstablishedMessage);
        simpMessagingTemplate.convertAndSend(SERVER_SEND_CLIENT_CONNECTION_MESSAGE + user, msg);
    }

    /**
     * ------------- PUB / SUB HANDLING -------------
     **/

    @MessageMapping(SERVER_RECEIVE_CLIENT_NEW_DOCUMENT)
    @Override
    public void handleDocumentRequest(Message<String> aMessage) throws IOException
    {
        System.out.println("RECEIVED NEW DOCUMENT BY CLIENT, Message: " + aMessage);
        String[] payload = purifyPayload(aMessage.getPayload());
        NewDocumentMessage newDocumentMessage = new NewDocumentMessage();
        // TODO retrieve desired content and fill NewDocumentMessage
        String msg = JSONUtil.toJsonString(newDocumentMessage);
        simpMessagingTemplate
                .convertAndSend(SERVER_SEND_CLIENT_NEW_DOCUMENT + payload[0].split(":")[1], msg);
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_NEW_VIEWPORT)
    public void handleViewportRequest(Message<String> aMessage) throws IOException
    {
        System.out.println("RECEIVED NEW VIEWPORT BY CLIENT, Message: " + aMessage);
        String[] payload = purifyPayload(aMessage.getPayload());
        ViewportMessage viewportMessage = new ViewportMessage(
                Integer.parseInt(payload[3].split(":")[1]),
                Integer.parseInt(payload[4].split(":")[1]));
        // TODO retrieve desired content and fill ViewportMessage
        String msg = JSONUtil.toJsonString(viewportMessage);
        simpMessagingTemplate
                .convertAndSend(SERVER_SEND_CLIENT_NEW_VIEWPORT + payload[0].split(":")[1], msg);
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_SELECTED_ANNOTATION)
    public void handleSelectAnnotation(Message<String> aMessage) throws IOException
    {
        System.out.println("RECEIVED SELECT_ANNOTATION BY CLIENT, Message: " + aMessage);
        String[] payload = purifyPayload(aMessage.getPayload());
        CAS cas = getCasForDocument(payload[1].split(":")[1], payload[2].split(":")[1],
                payload[0].split(":")[1]);
        AnnotationMessage annotationMessage = new AnnotationMessage();
        // TODO retrieve desired content and fill AnnotationMessage
        String msg = JSONUtil.toJsonString(annotationMessage);
        simpMessagingTemplate.convertAndSend(
                SERVER_SEND_CLIENT_SELECTED_ANNOTATION + payload[0].split(":")[1], msg);
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_NEW_ANNOTATION)
    public void handleNewAnnotation(Message<String> aMessage) throws IOException
    {
        System.out.println("RECEIVED NEW ANNOTATION BY CLIENT");
        String[] payload = purifyPayload(aMessage.getPayload());
        CAS cas = getCasForDocument(payload[1].split(":")[1], payload[2].split(":")[1],
                payload[0].split(":")[1]);
        // TODO cas.createAnnotation()
        AnnotationMessage annotationMessage = new AnnotationMessage();
        // TODO retrieve desired content and fill AnnotationMessage
        String msg = JSONUtil.toJsonString(annotationMessage);
        simpMessagingTemplate.convertAndSend(SERVER_SEND_CLIENT_NEW_ANNOTATION
                + payload[1].split(":")[1] + payload[2].split(":")[1] + payload[3].split(":")[1],
                msg);
    }

    @Override
    @MessageMapping(SERVER_RECEIVE_CLIENT_DELETE_ANNOTATION)
    public void handleDeleteAnnotation(Message<String> aMessage) throws IOException
    {
        String[] payload = purifyPayload(aMessage.getPayload());
        CAS cas = getCasForDocument(payload[1].split(":")[1], payload[2].split(":")[1],
                payload[0].split(":")[1]);
        // TODO cas.deleteAnnotation()
        AnnotationMessage annotationMessage = new AnnotationMessage();
        // TODO retrieve desired content and fill AnnotationMessage
        String msg = JSONUtil.toJsonString(annotationMessage);
        simpMessagingTemplate.convertAndSend(SERVER_SEND_CLIENT_DELETE_ANNOTATION
                + payload[1].split(":")[1] + payload[2].split(":")[1] + payload[3].split(":")[1],
                msg);
    }

    /**
     * -----------------------------------------------
     **/

    // --------------- SUPPORT METHODS ---------------- //
    @Override
    public CAS getCasForDocument(String aProject, String aDocument, String aUser)
    {
        try {
            Project project = projectService.getProject(aProject);
            SourceDocument sourceDocument = documentService.getSourceDocument(project, aDocument);
            return documentService.readAnnotationCas(sourceDocument, aUser);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String[] purifyPayload(String aPayload)
    {
        return aPayload.replace("\"", "").replace("{", "").replace("}", "").split(",");
    }
}
